import prisma.runner.{Account, ganache}
import prisma.Prisma._

import java.math.BigInteger
import java.nio.file.{Files, Paths, StandardOpenOption}
import java.text.SimpleDateFormat
import java.util.Date

class AllTests extends munit.FunSuite {

  val dt = new SimpleDateFormat("yyyy-MM-dd-HH-mm").format(new Date())
  def measure(name: String)(f: => Unit): Unit = {
    prisma.runner.web3s.txs.clear()
    f
    if (!Files.exists(Paths.get("out/measurements.txt")))
      Files.writeString(Paths.get("out/measurements.txt"), "")
    Files.writeString(
      Paths.get("out/measurements.txt"),
      Seq(dt, name, prisma.runner.web3s.txs.fold(BigInteger.ZERO)(_ add _).toString).mkString("\t") + "\n",
      StandardOpenOption.APPEND)
  }

  test("hang") { measure("hang") {

    class AutoHang(acc: Account, addr: Address, parties: Arr[AddressPayable])
    extends HangContracts.Hangman(acc, addr, parties) {
      var idx = 0
      val inputS = Map(
        ganache.defaultAccount2.credentials.getAddress -> Seq("HELLO",    "0",    "1",    "2,3",    "4",  "HELLO"),
        ganache.defaultAccount3.credentials.getAddress -> Seq(        "H",    "E",    "L",      "O"))
      override def input(s: String): String = {
        printIt()
        val tmp = inputS(acc.credentials.getAddress)(idx)
        idx+=1
        println(s + tmp)
        tmp
      }
    }

    val (hang1, hang2) = TestHangman.run(new AutoHang(_,_,_),
      ganache.defaultAccount2, ganache.defaultAccount3)
    assertEquals((hang1.missingletters, hang1.missingletters), ("0".u, "0".u))

  } }



  test("ttt") { measure("ttt") {

    class AutoTTT(acc: Account, addr2: Address, players$: Arr[AddressPayable])
    extends TTTContracts.TTT(acc, addr2, players$) {
      val inputMap = Map(
        ganache.defaultAccount2.credentials.getAddress -> Array("2","2",   "1","1",    "0","0"),
        ganache.defaultAccount3.credentials.getAddress -> Array(      "1","2",  "2","1",    "0","1"))
      var idx = 0
      override def input(s: String): String = {
        val answer = inputMap(acc.credentials.getAddress)(idx)
        idx = idx + 1
        println(s + answer)
        //val answer = scala.io.StdIn.readLine(s)
        answer
      }
    }

    val (ttt1, ttt2) = TestTTT.run(new AutoTTT(_,_,_),
      ganache.defaultAccount2, ganache.defaultAccount3)
    assertEquals((ttt1.moves, ttt2.moves), ("12".u8, "12".u8))
    // 12 = 10 (ended) + 2 (second player won).

  } }



  test("maxValues") { measure("maxValues") {
    val c = new CalcContracts.Calc(ganache.defaultAccount4, "0".a)

    assertEquals(c.tu8("0xf".u8).value, "0xf".u8)
    assertEquals(c.tu8("0xff".u8).value, "0xff".u8)
    assertEquals(c.tu16("0xffff".u16).value, "0xffff".u16)
    assertEquals(c.tu32("0xffffffff".u32).value, "0xffffffff".u32)
    assertEquals(c.tu64("0xffffffffffffffff".u64).value, "0xffffffffffffffff".u64)
    assertEquals(c.tu("0xffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff".u).value, "0xffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff".u)
  } }



  test("codec") { measure("codec") {
    val c = new CalcContracts.Calc(ganache.defaultAccount4, "0".a)

    assertEquals(c.de8(), "0xff".u8)
    assertEquals(c.de16(), "0xff".u16)
    assertEquals(c.de32(), "0xff".u32)
    assertEquals(c.de64(), "0xff".u64)

    assertEquals(c.deP(), new CalcContracts.U8Pair("0x12".u8, "0x34".u8))
    assertEquals(c.dePP(), new CalcContracts.U8PairPair(
      new CalcContracts.U8Pair("0x12".u8, "0x34".u8),
      new CalcContracts.U8Pair("0x56".u8, "0x78".u8)))
    assertEquals(c.deA(), Arr[U8]("0x12".u8, "0x34".u8, "0x56".u8))
    assertEquals(c.deAA(), Arr[Arr[U8]](
      Arr[U8]("0x12".u8, "0x34".u8),
      Arr[U8]("0x56".u8, "0x12".u8),
      Arr[U8]("0x34".u8, "0x56".u8)))
  } }



  test("calc") { measure("calc") {
    val c = new CalcContracts.Calc(ganache.defaultAccount4, "0".a)

    assertEquals(c.tu("0x0".u).value, "0x0".u)
    assertEquals(c.tu("0x1".u).value, "0x1".u)
    assertEquals(c.tu("0x2".u).value, "0x2".u)
    assertEquals(c.tu("0x256".u).value, "0x256".u)
    assertEquals(c.tu64("0x256".u64).value, "0x256".u64)

    assertEquals(c.tP().value, new CalcContracts.U8Pair("0x12".u8, "0x34".u8))
    assertEquals(c.tPP().value, new CalcContracts.U8PairPair(
      new CalcContracts.U8Pair("0x12".u8, "0x34".u8),
      new CalcContracts.U8Pair("0x56".u8, "0x78".u8)))
    assertEquals(c.tA().value, Arr[U8]("0x12".u8, "0x34".u8))
    assertEquals(c.tAA().value, Arr[Arr[U8]](
      Arr[U8]("0x12".u8, "0x34".u8),
      Arr[U8]("0x56".u8, "0x12".u8),
      Arr[U8]("0x34".u8, "0x56".u8)))

    c.add("100".u16); assertEquals(c.result, "100".u16)
    c.add("200".u16); assertEquals(c.result, "300".u16)
    c.mul("10".u16);  assertEquals(c.result, "3000".u16)
    c.div("5".u16);   assertEquals(c.result, "600".u16)
    c.clear();        assertEquals(c.result, "0".u16)

  } }


}