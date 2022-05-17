import prisma.meta.BangNotation.↓
import prisma.runner.{Account, ganache}
import prisma.Prisma._

import java.math.BigInteger

// !!! LoC Counted !!!

object TestTTT {
  def run(ctor: (Account, Address, Arr[AddressPayable]) => TTTContracts.TTT,
          pl1: Account, pl2: Account): (TTTContracts.TTT, TTTContracts.TTT) = {
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
    run(new TTTContracts.TTT(_,_,_), ganache.defaultAccount1, ganache.defaultAccount2)
}

@prisma object TTTContracts {
  class TTT(@cl acc$: Account, @cl addr$: Address, @co players$: Arr[AddressPayable])  { @co @cross var players: Arr[AddressPayable] = players$
    @co @cl class UU(var x: U8, var y: U8)

    @co @cross var moves: U8 = "0".u8
    @co @cross val board: Arr[Arr[U8]] = Arr(Arr("0".u8, "0".u8, "0".u8), Arr("0".u8, "0".u8, "0".u8), Arr("0".u8, "0".u8, "0".u8))

    @cl def input(s: String): String = scala.io.StdIn.readLine(s)
    @cl def printIt(): Unit = {
      println("parties: %s\n  moves: %s\n  %s|%s|%s\n  %s|%s|%s\n  %s|%s|%s".format(
        this.players, this.moves,
        this.board("0".u)("0".u).bint,this.board("1".u)("0".u).bint,this.board("2".u)("0".u).bint,
        this.board("0".u)("1".u).bint,this.board("1".u)("1".u).bint,this.board("2".u)("1".u).bint,
        this.board("0".u)("2".u).bint,this.board("1".u)("2".u).bint,this.board("2".u)("2".u).bint))
    }

    @co val init: Unit = {
      while (moves < "9".u8) {
        val pair: UU = ↓(client("move", players(U256.from(moves % "2".u8)), { () =>
          printIt()
          ("0".u, new UU(new U8(BigInteger.valueOf(input("x-position?: ").toInt))
            , new U8(BigInteger.valueOf(input("y-position?: ").toInt))))
        }))
        move2(pair.x, pair.y)
      }
      ()
    }

    @co def move2(x: U8, y: U8): Unit = {
      require(board(U256.from(x))(U256.from(y)) === "0".u8, "Field already occupied")
      val pos = (moves % "2".u8) + "2".u8
      board(U256.from(x))(U256.from(y)) = pos

      val (success, data) = contractCall("0x254dffcd3277C0b1660F6d42EFbB754edaBAbC2B".a, "0".u, "\\xca\\x76\\x1a\\x21", board, x, y)

      if(success) moves = "10".u8 + pos
      else moves = moves + "1".u8
    }
  }
}