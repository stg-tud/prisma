import org.web3j.crypto.Hash.sha3
import prisma.runner.{Account, ganache}
import prisma.Prisma._

import java.math.BigInteger

// !!! LoC Counted !!!

object TestNotary {
  def run(ctor: (Account, Address) => Contract.Notary, pl1: Account, pl2: Account): Unit = {
    val deploy = ctor(pl1, "0".a)
    val clients = List(ctor(pl1, deploy.addr), ctor(pl2, deploy.addr))
    for (i <- 1 to 10)  clients(i % 2).clSubmit("Cl-" + i % 2)
  }

  def main(args: Array[String]): Unit = run(new Contract.Notary(_,_), ganache.defaultAccount1, ganache.defaultAccount2)
}

@prisma object Contract {

  @co @cl class Data(val owner: Address, val timestamp: U256, val data: AnyRef)

  class Notary(@cl val acc$: Account, @cl val addr$: Address) {
    @co val notaryStorage: U256 >=> Data = Mapping()
    @co @cross @view def getData(index: U256): Data = notaryStorage.get(index)

    @cl def dropLeadingZero(x: Array[Byte]): Array[Byte] = if (x(0) == 0) x.tail else x
    @cl def clSubmit(who: String): Unit = {
      val inArray = dropLeadingZero(new BigInteger(scala.io.StdIn.readLine(s"Please provide some hex-encoded input for $who: "), 16).toByteArray)
      val newIndex = new U256(new BigInteger(+1, sha3(dropLeadingZero(acc$.credentials.getAddress.p.bint.toByteArray) ++ inArray)))
      submit(inArray)

      println(newIndex)
      println("Input-Array: " + inArray.map("%02X" format _).mkString)
      println(s"Stored Certificate: " + getData(newIndex).data.asInstanceOf[Array[Byte]].map("%02X" format _).mkString)
    }

    @co @cross def submit(theData: AnyRef): Unit = {
      //notaryStorage.update(keccak256(new Index(sender(), theData)), new Data(sender(), now(), theData))
      notaryStorage.update(keccak256Packed(sender(), theData), new Data(sender(), now(), theData))
      ()
    }
  }

}

