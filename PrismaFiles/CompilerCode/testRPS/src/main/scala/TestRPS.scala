import org.web3j.crypto.Hash.sha3
import prisma.meta.BangNotation.↓
import prisma.runner.{Account, ganache}
import prisma.Prisma._

import java.math.BigInteger

// !!! LoC Counted !!!

object TestRPS {
  def run(ctor: (Account, Address, Arr[AddressPayable]) => RPSContracts.RPS,
          pl1: Account, pl2: Account): (RPSContracts.RPS, RPSContracts.RPS) = {
    val deploy = ctor(pl1, "0".a, new Arr(pl1.credentials.getAddress.p, pl2.credentials.getAddress.p))

    val x = ctor(pl1, deploy.addr, null) // load from addr
    val y = ctor(pl2, deploy.addr, null) // load from addr
    val a: Thread = new Thread({ () => x.trampoline("()") })
    val b: Thread = new Thread({ () => y.trampoline("()") })
    a.start(); b.start()
    a.join(); b.join()
    (x, y)
  }

  def main(args: Array[String]): Unit =
    run(new RPSContracts.RPS(_,_,_), ganache.defaultAccount1, ganache.defaultAccount2)
}

@prisma object RPSContracts {
  @co @cl class Opening(val rps1: U8, val nonce: U256)

  class RPS(@cl val acc$: Account, @cl val addr$: Address, @co players$: Arr[AddressPayable]) { @co @cross var players: Arr[AddressPayable] = players$

    //Contract
    @co @cross var winner: U256 = "0".u
    @co @cross var rps2: U8 = "0".u8
    @co @cross var commitment: U256 = "0".u
    @co @cross var deadline: U256 = "0".u

    //Client
    @cl var rps1: U8 = "0".u8
    @cl var nonce: U256 = "0".u256

    //Inputs
    //@cl var idx = 0
    //@cl val inputS = Map(
    //  ganache.defaultAccount1.credentials.getAddress -> Seq("1", "42"),
    //  ganache.defaultAccount2.credentials.getAddress -> Seq("0")
    //)

    //Get Input
    @cl def input(question: String): String = {
      printIt()
      scala.io.StdIn.readLine(question)
      //val tmp = scala.io.StdIn.readLine(s)
      //val tmp = inputS(acc$.credentials.getAddress)(idx); idx+=1
      //println(question + " " + tmp)
      //tmp
    }

    @cl def printIt(): Unit = {
      println("parties: %s\n  winner: %s \n commitment: %s \n move2: %s".format(
        this.players, this.winner, this.commitment, this.rps2))
    }

    @co val init: Unit = {

      commitment = ↓(client("commit", players("0".u), { () =>
        rps1 = input("Move? [1-3]").u8
        nonce = new U256(BigInteger.valueOf((Math.random() * 256).toInt))
        ("0".u, new U256(new BigInteger(+1, sha3(abiEncode(new Opening(rps1, nonce))))))
      }))

      rps2 = ↓(client("move", players("1".u), { () =>
        ("0".u, input("Move? [1-3]").u8)
      }))

      deadline = now() + "10".u

      val decommitment: Opening = ↓(raceClient("open", { () =>
        if(acc$.credentials.getAddress.a == players("0".u)){
          if(input("Open it? [Yes,  No]") == "Yes"){
            ("0".u, new Opening(rps1, nonce))
          } else {
            Thread.sleep(15 * 1000)
            null
          }
        } else {
          if(deadline < (System.currentTimeMillis() / 1000).toString.u){
            input("Trigger timeout? [Press Anything to confirm]")
            ("0".u, new Opening("0".u8, "0".u))
          } else {
            Thread.sleep(2 * 1000)
            null
          }
        }

      }))

      if(deadline > now()){
        require(sender() == players("0".u), "Wrong party to open")
        require(commitment == keccak256(decommitment), "Wrong opening")

        winner =
          if (U256.from(decommitment.rps1) % "3".u == U256.from(rps2) % "3".u) "3".u
          else if (U256.from(decommitment.rps1) % "3".u == (U256.from(rps2) + "1".u) % "3".u) "1".u
          else "2".u
        ()
      } else {
        winner = "2".u
      }

    }

  }
}
