import prisma.meta.BangNotation.↓
import prisma.runner.{Account, ganache}
import prisma.Prisma._

// !!! LoC Counted !!!

object TestEscrow {
  def run(ctor: (Account, Address, AddressPayable, AddressPayable, AddressPayable, U256) => EscrowContracts.Escrow,
          pl1: Account, pl2: Account, pl3: Account): (EscrowContracts.Escrow, EscrowContracts.Escrow, EscrowContracts.Escrow) = {
    val deploy = ctor(pl1, "0".a, pl1.credentials.getAddress.p, pl2.credentials.getAddress.p, pl3.credentials.getAddress.p, "100".u)

    val x = ctor(pl1, deploy.addr, null, null, null, null)
    val y = ctor(pl2, deploy.addr, null, null, null, null)
    val z = ctor(pl3, deploy.addr, null, null, null, null)

    val a: Thread = new Thread({ () => x.trampoline("()") })
    val b: Thread = new Thread({ () => y.trampoline("()") })
    val c: Thread = new Thread({ () => z.trampoline("()") })
    a.start(); b.start(); c.start()
    a.join(); b.join(); c.join()
    (x, y, z)
  }

  def main(args: Array[String]): Unit =
    run(new EscrowContracts.Escrow(_,_,_,_,_,_), ganache.defaultAccount1, ganache.defaultAccount2, ganache.defaultAccount3)
}

@prisma object EscrowContracts {
  class Escrow(@cl val acc$: Account, @cl val addr$: Address, @co buyer$: AddressPayable, @co seller$: AddressPayable, @co arbiter$: AddressPayable, @co price$: U256) { @co @cross var buyer: AddressPayable = buyer$; @co @cross var seller: AddressPayable = seller$; @co @cross var arbiter: AddressPayable = arbiter$; @co @cross var price: U256 = price$

    @cl val started: Long = System.currentTimeMillis() / 1000
    @cl val arbiterTimeout = 30

    @cl def clientAddress: AddressPayable = acc$.credentials.getAddress.p

    @cl def printIt(): Unit = {
      println("buyer: %s\n  seller: %s\n  arbiter: %s\n  price: %s".format(this.buyer, this.seller, this.arbiter, this.price))
    }

    @cl def input(s: String): String = {
      printIt()
      scala.io.StdIn.readLine(s)
    }

    @co val init: Unit = {

      val dummy: U8 = ↓(client("pay_payable", buyer, { () =>
        input("Pay? [Press anything to confirm]")
        (price, "0xff".u8)
      }))

      require(value() == price, "Wrong amount")

      val sendBack: U8 = ↓(raceClient("proceed", { () =>
        if (clientAddress == buyer) {
          val in = input("Confirm receipt? [Yes, No]")
          if(in == "Yes"){
            ("0".u, "0".u8)
          } else {
            Thread.sleep(1000*60*60)  //Stop "forever"
            null
          }
        } else if (clientAddress == arbiter && started + arbiterTimeout < System.currentTimeMillis() / 1000) {
          val in = input("Arbiter decides? [0, 1]")
          ("0".u, in.u8)
        } else null
      }))

      if (sender() == buyer || (sender() == arbiter && sendBack === "0".u8)) {
        seller.transfer(balance())
      } else if (sender() == arbiter) {
        buyer.transfer(balance())
      } else {
        revert("Wrong party")
      }

      ()
    }

  }
}