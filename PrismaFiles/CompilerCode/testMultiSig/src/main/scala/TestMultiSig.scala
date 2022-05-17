import org.web3j.crypto.Hash.sha3
import org.web3j.crypto.Sign
import prisma.runner.{Account, ganache}
import prisma.Prisma._

import java.math.BigInteger
import scala.collection.mutable.ListBuffer

// !!! LoC Counted !!!

object TestTTT {
  def run(ctor: (Account, Address, U256, Arr[AddressPayable]) => MultiSigContracts.MultiSig, players: List[Account]): Unit = {
    val deploy = ctor(players(0), "0".a, "2".u, Arr.fromSeq(players.map( x => x.credentials.getAddress.p)))

    val clients = players.map(acc => ctor(acc, deploy.addr, null, null))

    def removeSignatureBit (in: Array[Byte]): Array[Byte] = { if(in(0) == 0) in.tail else in }

    //Hardcoded target address
    val target = "0x5b1869D9A4C187F2EAa108f3062412ecf0526b24".p

    //Hard-coded sample transaction data
    var data = new ListBuffer[Array[Byte]]()
    data += removeSignatureBit(new BigInteger("276afec6", 16).toByteArray)
    data += removeSignatureBit(new BigInteger("0864c21e000000000000000000000000000000000000000000000000000000000000002a", 16).toByteArray)
    data += removeSignatureBit(new BigInteger("aec5aa5b000000000000000000000000000000000000000000000000000000000000002a0000000000000000000000000000000000000000000000000000000000000fa0", 16).toByteArray)
    data += removeSignatureBit(new BigInteger("98f1529c0000000000000000000000000000000000000000000000000000000000000020000000000000000000000000000000000000000000000000000000000000000cabcdefabcdefabcdefabcdef0000000000000000000000000000000000000000", 16).toByteArray)
    data += removeSignatureBit(new BigInteger("98f1529c00000000000000000000000000000000000000000000000000000000000000200000000000000000000000000000000000000000000000000000000000000024abcdefabcdefabcdefabcdefabcdefabcdefabcdefabcdefabcdefabcdefabcdefabcdef00000000000000000000000000000000000000000000000000000000", 16).toByteArray)
    data += removeSignatureBit(new BigInteger("83d35345", 16).toByteArray) //Contract does not have money so this will fail

    var loopIt = true

    while(loopIt){
      val action = scala.io.StdIn.readLine(s"Action? [1,2,3,4,5,6, Q = Quit] ")
      if (action == "Q") {
        loopIt = false
        println("Stopping the application")
      } else if (action.toInt > 0 && action.toInt < 7){
        val timeout = ((System.currentTimeMillis() / 1000) + (60 * 60)).toString.u
        val nonce = clients(0).nonce //Somehow this is a transaction
        val value = if(action == "6") "10000".u else "0".u
        val signatures = clients.map(client => client.sign(deploy.addr, nonce, timeout, target, value, data(action.toInt - 1))) //I have not done the ordering yet, just hope for the best
        clients(0).execute(new MultiSigContracts.Execution(Arr(signatures(0), signatures(1)), Arr("0".u, "1".u), timeout, target, value, data(action.toInt - 1)))
        println("Successful submission")
      }
    }
  }

  def main(args: Array[String]): Unit =
    run(new MultiSigContracts.MultiSig(_,_,_,_), List(ganache.defaultAccount1, ganache.defaultAccount2, ganache.defaultAccount3))
}

@prisma object MultiSigContracts {

  @co @cl class Sig(val v: U8, val r: U256, val s: U256)
  @co @cl class Execution(val signatures: Arr[Sig], val indices: Arr[U256], val timeout: U256,
                          val destination: AddressPayable, val value: U256, val data: AnyRef)

  class MultiSig(@cl acc$: Account, @cl addr$: Address, @co threshold$: U256, @co parties$: Arr[AddressPayable]) { @co @cross var parties: Arr[AddressPayable] = parties$; @co @cross var threshold: U256 = threshold$; @co @cross var nonce: U256 = "0".u

    @co @cross def $receive() = {}

    @co @cross def execute(request: Execution): Unit = {
      require(request.signatures.length == threshold, "Wrong number of signatures")
      require(request.indices.length == threshold, "Wrong number of indices")
      require(now() < request.timeout, "Too late to submit")
      require(balance() >= request.value, "Not enough coins")

      val theHash: U256 = keccak256Packed(thisAddress(), nonce, request.timeout, request.destination, request.value, request.data)

      var last:Address = "0".p
      var i = "0".u
      while (i < threshold) {
        val rec = ecrecoverPrefixed(theHash, request.signatures(i).v, request.signatures(i).r, request.signatures(i).s)
        require(rec > last, "Wrong ordering")
        require(rec == parties(request.indices(i)), "Wrong signature")
        last = rec
        i = i + "1".u
      }

      nonce = nonce + "1".u
      val (success, data) = contractCall(request.destination, request.value, request.data)
      require(success, "Call failed")
    }

    //Client logic
    @cl def signatureToSolidity(signature: Sign.SignatureData): Sig = new Sig(
      new U8(new java.math.BigInteger(1, signature.getV)),
      new U256(new java.math.BigInteger(1, signature.getR)),
      new U256(new java.math.BigInteger(1, signature.getS))
    )

    @cl def convert(x: Array[Byte]): Array[Byte] = if (x(0) == 0) x.tail else x
    @cl def convertFilled(x: Array[Byte], l: Int): Array[Byte] = Array.fill(l - convert(x).size)(0.toByte) ++ convert(x)

    @cl def sign(addr: Address, nonce: U256, timestamp: U256, target: AddressPayable, value: U256, data: Array[Byte]): Sig = {
      val x = convertFilled(addr.bint.toByteArray, 16) ++ convertFilled(nonce.bint.toByteArray, 32) ++ convertFilled(timestamp.bint.toByteArray, 32) ++ convertFilled(target.bint.toByteArray, 16) ++ convertFilled(value.bint.toByteArray, 32) ++ data
      //println(x.map("%02X" format _).mkString)
      signatureToSolidity(Sign.signMessage(keccak256_PREFIX.getBytes ++ sha3(x), acc$.credentials.getEcKeyPair, true))
    }
  }
}
