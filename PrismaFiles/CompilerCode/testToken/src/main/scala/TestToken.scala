import prisma.runner.{Account, ganache}
import prisma.Prisma._
import _root_.prisma.Prisma

// !!! LoC Counted !!!

object TestToken {

  def run(ctor: (Account, Address, U256) => Contract.Token, players: List[Account]): Unit = {
    val deploy = ctor(players(0), "0".a, scala.io.StdIn.readLine(s"Initial supply? ").u)
    val clients = players.map(acc => ctor(acc, deploy.addr, null))
    var loopIt = true

    while (loopIt) {
      val who = scala.io.StdIn.readLine(s"Client? [0, 1, 2, 3] ")
      val action = scala.io.StdIn.readLine(s"Action? [T: Transfer, A: Approve, F: TransferFrom, Q:Quit] ")
      val amount = scala.io.StdIn.readLine(s"Amount? ")
      val toWho = scala.io.StdIn.readLine(s"To? [0, 1, 2, 3] ")
      action match {
        case "T" =>
          clients(who.toInt).transfer(players(toWho.toInt).credentials.getAddress.p, amount.u)
          println(s"Transferring $amount tokens from Cl-$who to Cl-$toWho")
        case "A" =>
          clients(who.toInt).approve(players(toWho.toInt).credentials.getAddress.p, amount.u)
          println(s"Approving $amount tokens from Cl-$who to Cl-$toWho")
        case "F" =>
          val fromWho = scala.io.StdIn.readLine(s"From Who? [0, 1, 2, 3]")
          clients(who.toInt).transferFrom(players(fromWho.toInt).credentials.getAddress.p, players(toWho.toInt).credentials.getAddress.p, amount.u)
          println(s"Transferring $amount tokens from Cl-$fromWho to Cl-$toWho")
        case "Q" =>
          loopIt = false
          println("Stopping the application")
      }
    }
  }

  def main(args: Array[String]): Unit =
    run(new Contract.Token(_,_,_), List(ganache.defaultAccount1, ganache.defaultAccount2, ganache.defaultAccount3, ganache.defaultAccount4))
}


@prisma object Contract {
  class Token(@cl val acc$: Account, @cl val addr$: Address, @co initialSupply$: U256) {
    @co val _balances: Address >=> U256 = Mapping()
    @co val _allowances: Address >=> (Address >=> U256) = Mapping()
    @co var _totalSupply: U256 = initialSupply$
    @co @cross val name: String = "Prisma-Token";

    @co @event def Transfer($indexed_from: Address, $indexed_to: Address, value: U256): Event
    @co @event def Approval($indexed_owner: Address, $indexed_spender: Address, value: U256): Event

    @co @cross def transfer(recipient: Address, amount:U256):Boolean = {
      _transfer(sender(), recipient, amount)
      true
    }

    @co @cross def approve(spender: Address, amount: U256): Boolean = {
      _approve(sender(), spender, amount)
      true
    }

    @co @cross def transferFrom(sender: Address, recipient: Address, amount: U256): Boolean = {
      _transfer(sender, recipient, amount);
      val currentAllowance:U256 = _allowances.get(sender).get(Prisma.sender());
      require(currentAllowance >= amount, "ERC20: transfer amount exceeds allowance")
      _approve(sender, Prisma.sender(), currentAllowance - amount)
      true
    }

    @co def _transfer(sender: Address, recipient: Address, amount: U256): Unit = {
      val senderBalance: U256 = _balances.get(sender)
      require(senderBalance >= amount, "ERC20: transfer amount exceeds balance")
      _balances(sender) = senderBalance - amount
      _balances(recipient) = _balances.get(recipient) + amount

      emit(Transfer(sender, recipient, amount));
    }

    @co def _approve(owner: Address, spender: Address, amount: U256): Unit = {
      _allowances.get(owner)(spender) = amount
      emit(Approval(owner, spender, amount));
    }

    @co @cross @view def allowance(owner: Address, spender: Address): U256 =
      _allowances.get(owner).get(spender)
    @co @cross @view def totalSupply(): U256 =
      _totalSupply
    @co @cross @view def balanceOf(account: Address): U256 =
      _balances.get(account)

    @co val init: Unit = {
      _balances.update(sender(), _totalSupply)
      ()
    }
  }
}