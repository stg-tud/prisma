import prisma.Prisma._
import _root_.prisma.runner.{Account, ethereum, ganache}

@prisma object CalcContracts {
  @co @cl case class U8Pair(val a: U8, val b: U8)
  @co @cl case class U8PairPair(val a: U8Pair, val b: U8Pair)

  class Calc(@cl acc$: Account,
             @cl addr$: Address)
  {
    @co @cross var result: U16 = "0".u16
    //contract { 1+2+3; result = "2".u16 }.value()
    //contract { 2+3+4; result = "3".u16 }.value()

    @cl def add(n: U16): Contract[U16] = contract { result = result + n; result }
    @cl def sub(n: U16): Contract[U16] = contract { result = result - n; result }
    @cl def mul(n: U16): Contract[U16] = contract { result = result * n; result }
    @cl def div(n: U16): Contract[U16] = contract { result = result / n; result }
    @cl def clear(): Contract[U16] = contract { result = "0".u16; result }

    @cl def tu(n: U256): Contract[U256] = contract { n }
    @cl def tu8(n: U8): Contract[U8] = contract { n }
    @cl def tu16(n: U16): Contract[U16] = contract { n }
    @cl def tu32(n: U32): Contract[U32] = contract { n }
    @cl def tu64(n: U64): Contract[U64] = contract { n }

    // client encode decode
    @cl def de8(): U8 = abiDecode[U8](abiEncode("0xff".u8))
    @cl def de16(): U16 = abiDecode[U16](abiEncode("0xff".u16))
    @cl def de32(): U32 = abiDecode[U32](abiEncode("0xff".u32))
    @cl def de64(): U64 = abiDecode[U64](abiEncode("0xff".u64))

    @cl def deP(): U8Pair = {
      val bin = abiEncode(new U8Pair("0x12".u8, "0x34".u8))
      abiDecode[U8Pair](bin)
    }
    @cl def dePP(): U8PairPair = {
      val bin = abiEncode(new U8PairPair(
        new U8Pair("0x12".u8, "0x34".u8),
        new U8Pair("0x56".u8, "0x78".u8)))
      abiDecode[U8PairPair](bin)
    }
    @cl def deA(): Arr[U8] = {
      val bin = abiEncode(
        Arr[U8]("0x12".u8, "0x34".u8, "0x56".u8))
      abiDecode[Arr[U8]](bin)
    }
    @cl def deAA(): Arr[Arr[U8]] = {
      val bin = abiEncode(Arr[Arr[U8]](
        Arr[U8]("0x12".u8, "0x34".u8),
        Arr[U8]("0x56".u8, "0x12".u8),
        Arr[U8]("0x34".u8, "0x56".u8)))
      abiDecode[Arr[Arr[U8]]](bin)
    }

    @cl def tP(): Contract[U8Pair] = contract { new U8Pair("0x12".u8, "0x34".u8) }
    @cl def tPP(): Contract[U8PairPair] = contract { new U8PairPair(
      new U8Pair("0x12".u8, "0x34".u8),
      new U8Pair("0x56".u8, "0x78".u8)) }

    @cl def tA(): Contract[Arr[U8]] = contract { Arr[U8]("0x12".u8, "0x34".u8) }
    @cl def tAA(): Contract[Arr[Arr[U8]]] = contract { Arr[Arr[U8]](
      Arr[U8]("0x12".u8, "0x34".u8),
      Arr[U8]("0x56".u8, "0x12".u8),
      Arr[U8]("0x34".u8, "0x56".u8)) }

    @co @cross def ttA(): Arr[U8] = Arr[U8]("0x12".u8, "0x34".u8)
    @co @cross def ttAA(): Arr[Arr[U8]] = Arr[Arr[U8]](
      Arr[U8]("0x12".u8, "0x34".u8),
      Arr[U8]("0x56".u8, "0x12".u8),
      Arr[U8]("0x34".u8, "0x56".u8))
  }

}

object Main {
  def main(args: Array[String]): Unit = {
    val c = new CalcContracts.Calc(
      new Account(ganache.defaultAccount1.credentials.getAddress),
      "0".a
    )

    println("-10".u + "-10".u)

    println("  Macro Ganache Headlong Calc")
    c.add("100".u16); println(c.result)
    c.add("200".u16); println(c.result)
    c.mul("10".u16);  println(c.result)
    c.div("5".u16);   println(c.result)
    c.clear();  println(c.result)
  }
}