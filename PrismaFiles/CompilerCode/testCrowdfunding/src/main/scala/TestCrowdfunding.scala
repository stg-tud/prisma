import prisma.meta.BangNotation.↓
import prisma.runner.{Account, ganache}
import prisma.Prisma._

// !!! LoC Counted !!!

object TestCrowdfunding {
  def run(ctor: (Account, Address, Int, U256, U256) => Contract.Crowdfunding, pl1: Account, pl2: Account): (Contract.Crowdfunding, Contract.Crowdfunding) = {
    val deploy = ctor(pl1, "0".a, 0, "100".u,  ((System.currentTimeMillis() / 1000)+20).toString.u)

    val decision = scala.io.StdIn.readLine("Should the goal be reached? [0: No, 1:Yes]?").toInt

    val x1 = ctor(pl1, deploy.addr, decision, null,null) // load from addr
    val x2 = ctor(pl2, deploy.addr, decision, null,null) // load from addr
    //val x3 = ctor(pl3, deploy.addr, null,null) // load from addr
    //val x4 = ctor(pl4, deploy.addr, null,null) // load from addr
    val a1: Thread = new Thread({ () => x1.trampoline("()") })
    val a2: Thread = new Thread({ () => x2.trampoline("()") })
    //val a3: Thread = new Thread({ () => x3.trampoline("()") })
    //val a4: Thread = new Thread({ () => x4.trampoline("()") })

    a1.start(); a2.start(); //a3.start(); a4.start()
    a1.join(); a2.join(); //a3.join(); a4.join()
    (x1, x2)
  }

  def main(args: Array[String]): Unit = run(new Contract.Crowdfunding(_,_,_,_,_), ganache.defaultAccount1, ganache.defaultAccount2)
}

@prisma object Contract {
  class Crowdfunding(@cl val acc$: Account, @cl val addr$: Address,  @cl val decision$: Int, @co goal$: U256, @co deadline$: U256) { @cross @co var goal: U256 = goal$; @cross @co var deadline: U256 = deadline$
    @cross @co var owner: AddressPayable = sender()
    @co val fundings: Address >=> U256 = Mapping()

    @cl def printIt(): Unit =
      println("owner: %s\n  goal: %s\n  balance: %s\n  deadline: %s".format(this.owner, this.goal, 0, this.deadline)) //FIXME: Access balance

    @cl var funded = 0 //0: Not decided, 1: Funded, 2: Not funded

    @cl def act(): (U256, U256) = {
      val nowSecs = System.currentTimeMillis() / 1000
      printIt()

      if (funded == 0 & deadline - "5".u > nowSecs.toString.u) {
        funded = 1
        if (acc$.credentials.getAddress.p != owner){
          println("Second party always funds 10 wei")
          ("10".u, "0".u)
        } else if (decision$ == 1){
          println("Owner funds up to the goal")
          (goal - "10".u, "0".u)
        } else null
      } else if (deadline < nowSecs.toString.u){
        if (acc$.credentials.getAddress.p != owner){
          println("Second party triggers payout (to owner or to itself depending on the total funding)")
          ("0".u, "0".u)
        } else null
      } else null
    }

    @co val init: Unit = {
      while (deadline > now() || balance() > "0".u ) {
        val dummy: U256 = ↓(raceClient("allInOne_payable", act _))
        if (deadline > now()) {
          fundings.update(sender(), fundings.get(sender()) + value())
        } else if (balance() >= goal) {
          owner.transfer(balance())
        } else {
          sender().transfer(fundings.get(sender()))
          fundings.update(sender(), "0".u)
        }
      }
      ()
    }
  }
}

