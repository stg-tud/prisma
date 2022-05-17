package prisma.runner

import com.esaulpaugh.headlong.abi.Tuple
import org.web3j.crypto.{ContractUtils, Credentials, RawTransaction}
import org.web3j.protocol.Web3j
import org.web3j.protocol.core.methods.request.Transaction
import org.web3j.protocol.core.methods.response.{EthGetTransactionReceipt, TransactionReceipt}
import org.web3j.protocol.core.{DefaultBlockParameter, DefaultBlockParameterName, Response}
import org.web3j.protocol.http.HttpService
import org.web3j.tx.gas.DefaultGasProvider
import org.web3j.tx.{RawTransactionManager, Transfer}
import org.web3j.utils.Convert
import prisma.FLAGS
import prisma.Prisma.Address
import prisma.runner.ethereum.instrLength
import prisma.runner.headlong.{hexSelector, hexToTuple, tupleToHex}

import java.math.BigInteger
import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Paths}
import java.security.interfaces.ECPrivateKey
import java.security.spec.ECGenParameterSpec
import java.security.{KeyPairGenerator, SecureRandom}
import scala.collection.mutable

class ChainException(val err: Response.Error) extends RuntimeException

object web3s {
  def bytesToString(data: Array[Byte]) = new String(data, StandardCharsets.UTF_8)
  def stringToBytes(data: String): Array[Byte] = data.getBytes

  case class Source(className: String, sol: String, scala: String, sol2Scala: String, filename: String) {
    def compile: Compiled = {
      // "--optimize-yul", "--revert-strings", "verboseDebug",

      val (stdout1, stderr1) = exec(className, filename, "", "--version")
      val version = stdout1.split("\n")(1)
      require(version.startsWith("Version: 0.8.4"), s"solc version is $version but should be Version: 0.8.4+xxxxxxxxx")

      val (stdout, stderr) = exec(className, filename, sol, "--optimize", "--combined-json", "bin,srcmap,bin-runtime,srcmap-runtime,storage-layout")

      println(stderr)
      ErrHelper.printSolcErrorAsNewlangError(stderr, scala)
      if (stderr.contains("Error: ")) sys.error(stderr)

      val binCT = getStringFromJson(stdout, "\"bin\":\"")
      val srcmapCT = getStringFromJson(stdout, "\"srcmap\":\"")
      val binRT = getStringFromJson(stdout, "\"bin-runtime\":\"")
      val srcmapRT = getStringFromJson(stdout, "\"srcmap-runtime\":\"")
      //val storageLayout = getStringFromJson(stdout, "\"storage-layout\":\"")
      val res = Compiled(className, binCT, sol, scala, srcmapRT, binRT, sol2Scala)
      if (FLAGS.PRINT_EVM_BYTECODE) new Analysis(res.copy(binRT = binCT)).disasm()
      //if (FLAGS.PRINT_BYTECODE) new Analysis(res).disasm()
      if (FLAGS.PRINT_SYMBOLIC_EXECUTION) new Analysis(res.copy(binRT = binCT)).listPaths()
      //if (FLAGS.SYMBOLIC_EXECUTION) new Analysis(res).listPaths()
      res
    }
  }

  private def deriveKeypair(bytes: String) = {
    // TODO ponder security problem if key is derived from program bytecode
    //      if not enough, include PBKDF2 and additional nonce
    //      or, compare other deterministic keypair generation, like
    //      https://stackoverflow.com/questions/49201637/how-to-generate-deterministic-keys-for-ethereum-using-java
    // ---> actually people need to agree with each other that they want to create a contract,
    //      therefore a multisig / create2-proxy-contract is neccessary
    val initNonce = org.web3j.crypto.Hash.sha3(stringToBytes(bytes))
    val random = SecureRandom.getInstance("SHA1PRNG", "SUN")
    random.setSeed(initNonce)

    // web3j.create() --> ClassCastException BCECKeyPair cannot be cast to BCECKeyPair ... ???
    // Workaround {
    val keyPairGenerator = KeyPairGenerator.getInstance("ECDSA"/*, "BC"*/)
    val ecGenParameterSpec = new ECGenParameterSpec("secp256k1")
    keyPairGenerator.initialize(ecGenParameterSpec, random)
    val strPrivate = "0x" + keyPairGenerator.generateKeyPair().getPrivate.asInstanceOf[ECPrivateKey].getS.toString(16)
    // }

    val tempAcc = new Account(strPrivate)
    tempAcc
  }

  case class Compiled(className: String, binCT: String, sol: String, scala: String, srcmapRT: String, binRT: String, sol2Scala: String) {
    def deployDeterministic(moneyAcc: Account): Deployed = {
      // only deploy if not yet exists
      //   (via contract: https://github.com/miguelmota/solidity-create2-example)
      //   via client ->

      // a contract address depends on the creator's keypair and number of contracts the creator has created before.
      // to deploy a contract with a deterministic address, we need to fix both numbers.
      // we ensure that the counter is 0 by creating a new account, and ensure that we know the creator's address,
      // seeding the (random) account creator with the to be deployed contract's bytecode.
      // then we transfer the money to create the contract from the original acc to this tempAcc,
      // and the deploy the contract through the tempAcc.

      val expectedCode = web3s.Tx(moneyAcc, null, binCT).estimate.simulate

      val tempAcc = deriveKeypair(expectedCode)
      val nonce = tempAcc.getNonce
      val expectedAddr = ContractUtils.generateContractAddress(tempAcc.credentials.getAddress, BigInteger.ZERO)

      if (nonce != BigInteger.ZERO) { // already deployed
        // checks
        val actualCode = moneyAcc.web3j.ethGetCode(expectedAddr, DefaultBlockParameterName.LATEST).send.getCode
        assert(actualCode == expectedCode, f"code at this addr should equal expected code")

        println("LOAD")
        Deployed(expectedAddr, this)

      } else try { // not yet deployed
        val deploy = web3s.Tx(tempAcc, null, binCT).estimate
        val balance = tempAcc.getBalance
        Transfer.sendFunds(moneyAcc.web3j,
          moneyAcc.credentials, tempAcc.credentials.getAddress,
          new java.math.BigDecimal(deploy.gas multiply DefaultGasProvider.GAS_PRICE), Convert.Unit.WEI).send()
        val actualAddr = deploy.execute.getContractAddress

        // checks
        val actualCode = moneyAcc.web3j.ethGetCode(expectedAddr, DefaultBlockParameterName.LATEST).send.getCode
        assert(balance == tempAcc.getBalance, f"tempAcc should have received and spent he same amount of money ($balance == ${tempAcc.getBalance} ?)")
        assert(actualAddr == expectedAddr, "deployed addr should equal expected addr")
        assert(actualCode == expectedCode, "code at this addr should equal expected code")

        println("DEPLOY")
        Deployed(actualAddr, this)

      } catch {
        case x: ChainException =>
          ErrHelper.handleRespError(x.err, "<deploy>()", instr2sol, instrs, sol, scala)
          throw x
      }
    }

    def loadOrDeploy(acc: Account, addr: Address, sig: String, args: => Seq[AnyRef]): Deployed = {
      try {
        //val expectedCode = web3s.Tx(acc, null, binCT + headlong.tupleToHex(sig, args:_*)).estimate.simulate
        val res =
          if (addr.bint == BigInteger.ZERO) { deploy(acc, sig, args) }
          else { Deployed("0x" + addr.bint.toString(16), this) }
        //val actualCode = acc.web3j.ethGetCode(res.addr, DefaultBlockParameterName.LATEST).send.getCode
        //assert(actualCode == expectedCode, s"code at addr ${res.addr} should equal expected code\n$actualCode\n$expectedCode")
        res
      } catch {
        case x: ChainException =>
          ErrHelper.handleRespError(x.err, "<deploy>()", instr2sol, instrs, sol, scala)
          throw x
      }
    }

    def deploy(acc: Account, sig: String, args: Seq[AnyRef]): Deployed =
      try Deployed(web3s.Tx(acc, null, binCT + headlong.tupleToHex(sig, args:_*)).estimate.execute.getContractAddress, this)
      catch {
        case x: ChainException =>
          println((this.binCT, x.err.getMessage))
          ErrHelper.handleRespError(x.err, "<deploy>()", instr2sol, instrs, sol, scala)
          throw x
      }

    // https://ethereum.stackexchange.com/questions/25479/how-to-map-evm-trace-to-contract-source
    lazy val instrs: Seq[Int] = {
      val result = mutable.Buffer[Int]()
      var byteIndex = 0
      var continue = byteIndex < binRT.length
      while (continue) {
        val str = binRT.substring(byteIndex, byteIndex + 2)
        val int = Integer.parseInt(str, 16)
        val length = instrLength(int)
        //println(byteIndex/2, LowLevel.opcodeName(int, int.toHexString))
        result.append(byteIndex / 2)
        byteIndex += length * 2;
        continue = byteIndex < binRT.length && str != "fe" // 0xfe == INVALID, which is END OF CODE marker
      }
      result.toSeq
    }

    def instr2sol: Seq[(Int, Int, Int)] = {
      var (i, j, f) = (0, 0, 0)
      srcmapRT.split(";").map { x =>
        val xs: Array[String] = x.split(":")

        def tryGetIntElse(i: Int, default: Int) = try {
          xs(i).toInt
        } catch {
          case _: Throwable => default
        }

        i = tryGetIntElse(0, i)
        j = tryGetIntElse(1, j)
        f = tryGetIntElse(2, f)
        (i, j, f)
      }.toSeq
    }

    lazy val sol2Scala2: Seq[(Int, Int, Int, Int)] =
      sol2Scala.split(";").map { l => val Array(a,b,c,d) = l.split(","); (a.toInt,b.toInt,c.toInt,d.toInt) }.toSeq

  }

  val txs: mutable.Buffer[BigInteger] = mutable.Buffer()

  case class Deployed(addr: String, compiled: Compiled) {

    def magic(money: BigInteger, acc: Account, inSig: String, outSig: String, args: AnyRef*): Tuple =
      hexToTuple(outSig, magic2(money, acc, inSig, args: _*).substring(2))

    def magic2(money: BigInteger, acc: Account, inSig: String, args: AnyRef*): String =
      try magic3(money, acc, inSig, tupleToHex(inSig, args: _*))
      catch { case x: IllegalArgumentException =>
        println(s"$inSig $args")
        throw x
      }

    private def magic3(money: BigInteger, acc: Account, inSig: String, argumentHex: String): String =
      magic4(money, acc, hexSelector(inSig) + argumentHex, inSig)

    private def magic4(money: BigInteger, acc: Account, selectorAndArgumentHex: String, inSig: String): String = {
      try Tx(acc, addr, selectorAndArgumentHex, money).magic
      catch { case x: ChainException =>
        ErrHelper.handleRespError(x.err, inSig, compiled.instr2sol, compiled.instrs, compiled.sol, compiled.scala)
        println(f"acc $acc\nargumentHex $selectorAndArgumentHex")
        sys.error(f"$acc $inSig $selectorAndArgumentHex")
      }
    }

  }

  case class Tx(acc: Account, to: String, data: String, value: BigInteger = BigInteger.ZERO) {
    // COOL execute and get result w/o events! (at the cost of executing code once remote and twice locally ^^')
    def magic: String = {
      val blocknum = this.estimate.execute.getBlockNumber.subtract(BigInteger.ONE)
      acc.getNonce // linebreak in debug
      this.simulate(blocknum)
    }

    def estimate: TxGas = {
      val raw = new Transaction(acc.credentials.getAddress, null, null, null, to, value, data)
      val resp = acc.web3j.ethEstimateGas(raw).send
      if (resp.hasError) throw new ChainException(resp.getError)
      TxGas(acc, to, data, value, resp.getAmountUsed)
    }

    def simulate(no: BigInteger): String = TxGas(acc, to, data, value, null).simulate(no)
    def simulate: String = TxGas(acc, to, data, value, null).simulate
  }

  case class TxGas(acc: Account, to: String, data: String, value: BigInteger, gas: BigInteger) {
    def execute: TransactionReceipt = {
      txs append gas

      val raw = RawTransaction.createTransaction(acc.getNonce, DefaultGasProvider.GAS_PRICE, gas, to, value, data)
      val resp2 = acc.mgr.signAndSend(raw)
      if (resp2.hasError) throw new ChainException(resp2.getError)
      val hash = resp2.getTransactionHash

      val result = retry(10, 500) { () => acc.web3j.ethGetTransactionReceipt(hash).send }
        .getTransactionReceipt.get
//      val result = LowLevel.web3j.ethGetTransactionReceipt(hash)
//        .sendAsync.get(5, TimeUnit.SECONDS)
//        .getTransactionReceipt.get

      // if status is zero, rerun in 'debug mode' to get more info and throw
      if (result.getStatus == "0x0") {
        simulate
        sys.error("must error TODO ???")
      }
      result
    }

    def retry(times: Int, ms: Int)(test: () => EthGetTransactionReceipt): EthGetTransactionReceipt = {
      var i = 0
      while (true) {
        val resp: EthGetTransactionReceipt = test()
        if (resp.hasError) throw new ChainException(resp.getError)
        if (!resp.getTransactionReceipt.isEmpty) return resp
        i += 1
        assert(i <= times)
        Thread.sleep(ms)
      }
      null
    }

    def simulate: String = {
      val raw = new Transaction(acc.credentials.getAddress, acc.getNonce, null, gas, to, value, data)
      val result = acc.web3j.ethCall(raw, DefaultBlockParameterName.LATEST).send
      if (result.hasError) throw new ChainException(result.getError)
      if (result.isReverted) sys.error(result.getRevertReason)
      result.getValue
    }

    def simulate(no: BigInteger): String = {
      val raw = new Transaction(acc.credentials.getAddress, acc.getNonce, null, gas, to, value, data)
      val result = acc.web3j.ethCall(raw, DefaultBlockParameter.valueOf(no)).send
      if (result.hasError) throw new ChainException(result.getError)
      if (result.isReverted) sys.error(result.getRevertReason)
      result.getValue
    }
  }

  private def exec(className: String, filename: String, sol: String, cmds: String*): (String, String) = {
    val tmpfile = s"out/$filename-q-solidity.sol"
    Files.writeString(Paths.get(tmpfile), sol)
    val solcA = Paths.get("/usr/bin/solc-static-linux")
    val solcB = Paths.get("solc-static-linux")
    val solc = (if (Files.exists(solcA)) solcA else solcB).toAbsolutePath.toString
    val cmd = Runtime.getRuntime.exec(Array(solc, tmpfile) ++ cmds)
    val stdout = new String(cmd.getInputStream.readAllBytes()).trim()
    val stderr = new String(cmd.getErrorStream.readAllBytes()).trim()
    (stdout, stderr)
  }

  def getStringFromJson(src: String, startingWith: String): String = {
    val start = src.indexOf(startingWith)
    val end = src.indexOf("\"", start + startingWith.length)
    val bin2 = src.substring(start + startingWith.length, end)
    bin2
  }

  //private def listAllSecurityProvidersAndAlgorithsm(): Unit = {
  //  import scala.jdk.CollectionConverters._
  //  println(java.security.Security.getProviders.map{ x => x + ":\n    " + x.keys.asScala.mkString("\n    ") }.mkString("\n"))
  //}
}

class Account(privKey: String) {
  val web3j: Web3j = Web3j.build(new HttpService()) // defaults to http://localhost:8545/
  val credentials: Credentials = Credentials.create(privKey)
  val mgr = new RawTransactionManager(web3j, credentials)
  def getNonce: BigInteger = web3j.ethGetTransactionCount(credentials.getAddress, DefaultBlockParameterName.PENDING).send.getTransactionCount
  def getBalance: BigInteger = web3j.ethGetBalance(credentials.getAddress, DefaultBlockParameterName.LATEST).send().getBalance
  override def toString: String = "Account(priv=???, pub=" + credentials.getAddress + ")"
}

