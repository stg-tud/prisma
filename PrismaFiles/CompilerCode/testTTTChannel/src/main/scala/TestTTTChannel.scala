import org.web3j.crypto.Hash.sha3
import prisma.meta.BangNotation.↓
import org.web3j.crypto.{Hash, Sign}
import prisma.runner.{Account, ganache}
import prisma.Prisma._
import prisma.meta.MacroDef.ContractInterface

import java.math.BigInteger
import scala.collection.mutable.ArrayBuffer

// !!! LoC Counted !!!

object TestTTTChannel {

  def main(args: Array[String]): Unit = {

    val players = new Arr(ganache.defaultAccount2.credentials.getAddress.p, ganache.defaultAccount3.credentials.getAddress.p)

    val messagesToA: ArrayBuffer[Contracts.Msg] = ArrayBuffer()
    val messagesToB: ArrayBuffer[Contracts.Msg] = ArrayBuffer()
    val deployed = new Contracts.TTTChannel(ganache.defaultAccount1, "0".a, null, null, null, players) // acc1 deploys

    val a = new Thread({ () =>
      new Contracts.TTTChannel(ganache.defaultAccount2, deployed.addr, messagesToA, messagesToB, "2".u8, null) {
        override def clInit(): Unit = {     //Party A sends initial move
          println("Initial board-off")
          printIt(offChain)

          //Execute next Move
          val nextMove = input("Initial move: ", false)
          offChain.moves = offChain.moves + "1".u8
          offChain.board(U256.from(nextMove.x))(U256.from(nextMove.y)) = id
          offChain.version = offChain.version + "1".u

          signAndSend(1)
        }
      }.run()
    })

    val b = new Thread({ () =>
      new Contracts.TTTChannel(ganache.defaultAccount3, deployed.addr, messagesToB, messagesToA, "3".u8, null).run()
    })

    a.start(); b.start()
    a.join(); b.join()
  }
}

@prisma object Contracts {
  //Tuples
  @cl class Msg(val msgType: Int, val state: State, val sig: Sig)
  @co @cl class State(var version: U256, var moves: U8, var board: Arr[Arr[U8]])
  @co @cl class Sig(val v: U8, val r: U256, val s: U256)
  @co @cl class SignedState(val state: State, val sig1: Sig, val sig2: Sig)
  @co @cl class UU(var x: U8, var y: U8)

  class TTTChannel(@cl acc$: Account, @cl addr$: Address, @cl in: ArrayBuffer[Msg], @cl out: ArrayBuffer[Msg], @cl id$: U8, @co parties$: Arr[AddressPayable]) extends ContractInterface { @cl val acc: Account = acc$;  @cl val id: U8 = id$; @co @cross var parties: Arr[AddressPayable] = parties$

    //Application state: onChain/offChain
    @co @cross var onChain = new State("0".u, "0".u8, Arr(Arr("0".u8,"0".u8,"0".u8),Arr("0".u8,"0".u8,"0".u8),Arr("0".u8,"0".u8,"0".u8)))
    @cl var offChain = new State("0".u, "0".u8, Arr(Arr("0".u8,"0".u8,"0".u8),Arr("0".u8,"0".u8,"0".u8),Arr("0".u8,"0".u8,"0".u8)))

    //Channel state: onChain/offChain
    @co @cross var timeout: U256 = "0xffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff".u
    @cl var latestCommitment: SignedState = new SignedState(new State("0".u, "0".u8, null), null, null) //Just a dummy
    @cl var lastSignature: Sig = null
    @cl var startedOnChain: U256 = "0".u

    //Testing {
    //@cl var idx = 0
    //@cl val inputS = Map(
    //  ganache.defaultAccount2.credentials.getAddress -> Seq(new UU("0".u8, "0".u8), new UU("0".u8, "2".u8), new UU("2".u8, "0".u8), new UU("1".u8, "0".u8)),
    //  ganache.defaultAccount3.credentials.getAddress ->          Seq(new UU("2".u8, "2".u8), new UU("0".u8, "1".u8), new UU("1".u8, "1".u8))
    //)
    @cl def input(question: String, on: Boolean): UU = {
      if (on) printIt(onChain)
      new UU(new U8(BigInteger.valueOf(scala.io.StdIn.readLine("x-position?: ").toInt)), new U8(BigInteger.valueOf(scala.io.StdIn.readLine("y-position?: ").toInt)))
    }

    //Helpers {
    //@co @cross @view def keccakView(v: U256, m: U8, b: Arr[Arr[U8]]): U256 = keccak256(v, m, b)
    // @co def ecrecoverSigPrefixed(hash: U256, sig: Sig): Address = ecrecoverPrefixed(hash, sig.v, sig.r, sig.s)
    @cl def signatureToSolidity(signature: Sign.SignatureData): Sig = new Sig(
      new U8(new java.math.BigInteger(1, signature.getV)),
      new U256(new java.math.BigInteger(1, signature.getR)),
      new U256(new java.math.BigInteger(1, signature.getS))
    )
    //}

    // returns null if not ready, otherwise returns SignedState
    @cl def readyForDispute(): (U256, SignedState) = {
      //Check if commitment should be submitted
      //  - Party 1 acts if more current version, and timeout not elapsed
      //  - Party 2 acts if more current version, and timeout not elapsed, and program has started some time ago (we do not want concurrent submissions)
      //  - Either party acts if timeout elapsed and it is the party's turn to continue with a move
      val nowSecs = (System.currentTimeMillis() / 1000).toString.u

      val hasNewerVersion = onChain.version < latestCommitment.state.version
      val beforeTimeout = nowSecs < timeout
      val ready =
        if (me() == parties("1".u)) true // FIXME should it not be party 1 according to description above?
        else nowSecs > startedOnChain + "10".u / "2".u
      val myturn = me() === parties(U256.from(onChain.moves % "2".u8))

      //println(id, beforeTimeout, hasNewerVersion, ready, myturn)

      if (beforeTimeout && hasNewerVersion && ready) {
        ("0".u, latestCommitment)
      } else if (!beforeTimeout && myturn) {
        val nextMove = input("First move is: ", true)
        ("0".u, new SignedState(latestCommitment.state, new Sig(nextMove.x, "0".u, "0".u), new Sig(nextMove.y, "0".u, "0".u)))
      } else {
        null
      }
    }

    //Contract
    @co val init: Unit = {
      //Dispute period (starting at initialization)
      while ({
        timeout > now()
      }) {
        val latestState: SignedState = ↓(raceClient("dispute", readyForDispute _))

        //If timeout has not elapsed execute dispute - otherwise just continue
        if (timeout > now()) theDispute(latestState)
        else theMove(new UU(latestState.sig1.v, latestState.sig2.v))
      }

      while (onChain.moves < "9".u8) {
        val incoming: UU = ↓(client("move", parties(U256.from(onChain.moves % "2".u8)), { () =>
          ("0".u, input("Next move is: ", true))
        }))
        theMove(incoming)
      }

      ()
    }

    @co def theDispute(cp: SignedState): Unit = {

      require(cp.state.version > onChain.version, "Old version")
      //require(now() < timeout, "Timeout elapsed")

      val theHash: U256 = keccak256(new State(cp.state.version, cp.state.moves, cp.state.board))
      require(ecrecoverPrefixed(theHash, cp.sig1.v, cp.sig1.r, cp.sig1.s) === parties("0".u), "Wrong P-0 signature")
      require(ecrecoverPrefixed(theHash, cp.sig2.v, cp.sig2.r, cp.sig2.s) === parties("1".u), "Wrong P-1 signature")

      if (timeout === "0xffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff".u)
        timeout = now() + "10".u

      onChain = cp.state
    }

    //final class storage extends StaticAnnotation // IDEA: maybe like this?
    //@co @cl def public$won(x: U8, y: U8, pos: U8, board: Arr[Arr[U8] @storage] @storage): Boolean = {

    @co @cl def public$won(x: U8, y: U8, pos: U8, board: Arr[Arr[U8]]): Boolean = {
      /*
      val hor = board(x)("0".u) === board(x)("1".u) && board(x)("1".u) === board(x)("2".u)
      val ver = board("0".u)(y) === board("1".u)(y) && board("1".u)(y) === board("2".u)(y)
      val diagdown = board("0".u)("2".u) === board("1".u)("1".u) && board("2".u)("0".u) === board("1".u)("1".u)
      val diagup = board("0".u)("0".u) === board("1".u)("1".u) && board("2".u)("2".u) === board("1".u)("1".u)
      hor || ver || board("1".u)("1".u) === pos && (diagdown || diagup)
      */
      (
         board(U256.from(x))("0".u) === board(U256.from(x))("1".u) && board(U256.from(x))("1".u) === board(U256.from(x))("2".u)
      || board("0".u)(U256.from(y)) === board("1".u)(U256.from(y)) && board("1".u)(U256.from(y)) === board("2".u)(U256.from(y))
      || board("1".u)("1".u) === pos && ((board("0".u)("2".u) === board("1".u)("1".u) && board("2".u)("0".u) === board("1".u)("1".u))
                                      || (board("0".u)("0".u) === board("1".u)("1".u) && board("2".u)("2".u) === board("1".u)("1".u)))
      )
    }

    @co def theMove(pair: UU): Unit = {
      require(onChain.board(U256.from(pair.x))(U256.from(pair.y)) === "0".u8, "Field already occupied")

      val pos = (onChain.moves % "2".u8) + "2".u8
      onChain.board(U256.from(pair.x))(U256.from(pair.y)) = pos
      val cond = public$won(pair.x, pair.y, pos, onChain.board)
      onChain.moves = if (cond) "10".u8 + pos else onChain.moves + "1".u8
    }

    def moveOff(x: U8, y: U8): Boolean = {
      if (offChain.moves > "8".u8 || offChain.board(U256.from(x))(U256.from(y)) != "0".u)
        return false

      val pos = (offChain.moves % "2".u8) + "2".u8
      offChain.board(U256.from(x))(U256.from(y)) = pos
      val cond = public$won(x, y, pos, offChain.board)
      offChain.moves = if (cond) "10".u8 + pos else offChain.moves + "1".u8

      true
    }

    @cl def printIt(state: State): Unit = {
      println("moves:   %s\nboard:   %s|%s|%s\n         %s|%s|%s\n         %s|%s|%s".format(state.moves,
        state.board("0".u)("0".u).bint.intValueExact(), state.board("1".u)("0".u).bint.intValueExact(), state.board("2".u)("0".u).bint.intValueExact(),
        state.board("0".u)("1".u).bint.intValueExact(), state.board("1".u)("1".u).bint.intValueExact(), state.board("2".u)("1".u).bint.intValueExact(),
        state.board("0".u)("2".u).bint.intValueExact(), state.board("1".u)("2".u).bint.intValueExact(), state.board("2".u)("2".u).bint.intValueExact()))
    }

    @cl def clInit(): Unit = { }

    @cl def signAndSend(msgType:Int): Unit = {
      lastSignature = signatureToSolidity(Sign.signMessage(keccak256_PREFIX.getBytes ++ sha3(abiEncode(new State(offChain.version, offChain.moves, offChain.board))), acc$.credentials.getEcKeyPair, true))
      out += new Msg(msgType, offChain, lastSignature)
    }

    @cl def run(): Unit = {

      clInit()

      var break: Boolean = false
      while (!break) {
        while (in.isEmpty) { println("."); Thread.sleep(2 * 1000) }

        //As we do not want to implement the full offline protocol in a secure way, we do not validate messages

        val nextMessage: Msg = in(0)
        in.remove(0)
        println(s"Party-$id: (${nextMessage.msgType}, ${nextMessage.state.version}, ${nextMessage.state.moves}, ${if (nextMessage.state.board == null) null else nextMessage.state.board.theArgs.map(_.theArgs.map(_.bint.intValueExact()).mkString("[", ",", "]")).mkString("[", ",", "]")})")

        if (nextMessage.msgType == 1) {
          offChain = nextMessage.state //Update state
          signAndSend(2) //Confirm message

          latestCommitment = new SignedState(offChain,
            if (id === "2".u8) lastSignature else nextMessage.sig,
            if (id === "2".u8) nextMessage.sig else lastSignature)

          if (offChain.moves === "4".u8) {
            //Notify both parties that dispute starts now
            out += new Msg(3 , new State(null, null, null), null)
            in += new Msg(3, new State(null, null, null), null)

          } else {
            //Execute next move
            val nextMove = input("Offchain move: ", false)
            offChain.moves = offChain.moves + "1".u8
            offChain.board(U256.from(nextMove.x))(U256.from(nextMove.y)) = id
            offChain.version = offChain.version + "1".u

            //Send next move
            signAndSend(1)
          }

        } else if (nextMessage.msgType == 2) {

          latestCommitment = new SignedState(offChain,
            if (id === "2".u8) lastSignature else nextMessage.sig,
            if (id === "2".u8) nextMessage.sig else lastSignature)

          println(s"New commitment - ${latestCommitment}")

        } else {

          break = true

        }
      }

      println(s"Party-$id: moves onchain")
      println("With commitment")
      println(latestCommitment.state.version)
      println(latestCommitment.state.moves)
      println(latestCommitment.state.board)
      startedOnChain = (System.currentTimeMillis() / 1000).toString.u

      //initialise()
      trampoline("()")

      printIt(onChain)

    }
  }
}
