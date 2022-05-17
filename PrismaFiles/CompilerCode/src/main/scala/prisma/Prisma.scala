package prisma

import prisma.meta.BangNotation.{DSL, IF, WHILE}
import prisma.meta.MacroImpl

import java.math.BigInteger
import scala.annotation.{StaticAnnotation, compileTimeOnly, showAsInfix}
import scala.collection.mutable
import scala.language.experimental.macros
import scala.reflect.ClassTag

object Prisma {

  class Contract[X] /*private[MacroDef]*/ (val value: X)
  class Client[X] private[prisma](val value: () => (U256, X))

  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
  // annots: @prisma, @co, @cl, @cross, @view
  // blocks: contract { ... }; client("name", "0".p) { 1 + 2 }
  // contract functionality: keccak256, ecrecover, now, sender
  // client helper: abiEncode, abiDecode

  @compileTimeOnly("enable macro paradise to expand macro annotations")
  final class prisma extends StaticAnnotation {
    def macroTransform(annottees: Any*): Unit = macro MacroImpl.annotationEvm
  }

  // ignore this one...
  @compileTimeOnly("enable macro paradise to expand macro annotations")
  final class halfPrisma extends StaticAnnotation {
    def macroTransform(annottees: Any*): Unit = macro MacroImpl.annotationJvm
  }

  final class co extends StaticAnnotation
  final class cl extends StaticAnnotation
  final class cross extends StaticAnnotation
  final class view extends StaticAnnotation
  final class event extends StaticAnnotation

  object contract extends DSL with IF with WHILE /*with IF_DSL*/ {
    override type M[A] = Contract[A]
    override type N[A] = Client[A]

    override def make[X](x: X): Contract[X] =
      new Contract(x)

    override def flatMap[X, Y](x: Client[X], f: X => Contract[Y]): Contract[Y] =
      f(x.value()._2)

    override def IF_[A](x: Boolean)(y: () => Contract[A])(z: () => Contract[A]): Client[A] =
      new Client(() => ("0".u, if (x) y().value else z().value))

    override def WHILE_(x: () => Contract[Boolean])(y: () => Contract[Unit]): Client[Unit] =
      new Client(() => ("0".u, while (x().value) y()))
  }

  object client { def apply[A](name: String, i: AddressPayable, f: () => (U256, A)): Client[A] = new Client(f) }
  object raceClient { def apply[A](name: String, f: () => (U256, A)): Client[A] = new Client(f) }

  object abiEncode { def apply[X](a: X): Array[Byte] = ??? }
  object abiEncodePacked { def apply[X](a: X): Array[Byte] = ??? }
  object abiDecode { def apply[X](a: Array[Byte]): X = ??? }

  object keccak256 { def apply(s: AnyRef*): U256 = ??? }
  object keccak256Packed { def apply(s: AnyRef*): U256 = ??? }
  object ecrecover { def apply(hash: U256, v: U8, r: U256, s: U256): Address = ??? }
  object ecrecoverPrefixed { def apply(hash: U256, v: U8, r: U256, s: U256): Address = ??? }
  val keccak256_PREFIX: String ="\u0019Ethereum Signed Message:\n32" // TODO actually not always 32 but message length!
  val keccak256_PREFIX_STRING: String = "\"\\x19Ethereum Signed Message:\\n32\""

  // these implementations are not given in scala but will be added by compilation to solidity:
  object now { def apply(): U256 = ??? }
  object sender { def apply(): AddressPayable = ??? }
  object value { def apply(): U256 = ??? }
  object balance { def apply(): U256 = ??? }
  object thisAddress { def apply(): Address = ???}
  object revert { def apply(string: String): Unit = ???}
  object contractCall {
    def apply(destination: Address, Value: U256, data: AnyRef): (Boolean, AnyRef) = ???
    def apply(destination: Address, Value: U256, functionSignature: String, inputs: Any*): (Boolean, AnyRef) = ???
  }


  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  sealed trait UINT { def bint: BigInteger }

  class U8(val bint: BigInteger) extends UINT {
    assert(BigInteger.ZERO.compareTo(bint) <= 0 && bint.compareTo(CONSTANTS.U8_MOD) <= 0)
    def +(y: U8): U8 = new U8(bint.add(y.bint).mod(CONSTANTS.U8_MOD))
    def -(y: U8): U8 = new U8(bint.subtract(y.bint).mod(CONSTANTS.U8_MOD))
    def *(y: U8): U8 = new U8(bint.multiply(y.bint).mod(CONSTANTS.U8_MOD))
    def /(y: U8): U8 = new U8(bint.divide(y.bint).mod(CONSTANTS.U8_MOD))
    def %(y: U8): U8 = new U8(bint.mod(y.bint).mod(CONSTANTS.U8_MOD))
    def <=(y: U8): Boolean = bint.compareTo(y.bint) <= 0
    def >=(y: U8): Boolean = bint.compareTo(y.bint) >= 0
    def <(y: U8): Boolean = bint.compareTo(y.bint) < 0
    def >(y: U8): Boolean = bint.compareTo(y.bint) > 0
    def ===(y: U8): Boolean = bint.compareTo(y.bint) == 0
    override def equals(y: Any): Boolean = y match { case y: U8 => bint.compareTo(y.bint) == 0 case _ => ??? }
    override def toString = s"$bint.u8"
  }
  object U8 { def from(x: UINT): U8 = new U8(x.bint) }

  class U16(val bint: BigInteger) extends UINT {
    assert(BigInteger.ZERO.compareTo(bint) <= 0 && bint.compareTo(CONSTANTS.U16_MOD) <= 0)
    def +(y: U16): U16 = new U16(bint.add(y.bint).mod(CONSTANTS.U16_MOD))
    def -(y: U16): U16 = new U16(bint.subtract(y.bint).mod(CONSTANTS.U16_MOD))
    def *(y: U16): U16 = new U16(bint.multiply(y.bint).mod(CONSTANTS.U16_MOD))
    def /(y: U16): U16 = new U16(bint.divide(y.bint).mod(CONSTANTS.U16_MOD))
    def %(y: U16): U16 = new U16(bint.mod(y.bint).mod(CONSTANTS.U16_MOD))
    def <=(y: U16): Boolean = bint.compareTo(y.bint) <= 0
    def >=(y: U16): Boolean = bint.compareTo(y.bint) >= 0
    def <(y: U16): Boolean = bint.compareTo(y.bint) < 0
    def >(y: U16): Boolean = bint.compareTo(y.bint) > 0
    def ===(y: U16): Boolean = bint.compareTo(y.bint) == 0
    override def equals(y: Any): Boolean = y match { case y: U16 => bint.compareTo(y.bint) == 0 case _ => ??? }
    override def toString = s"$bint.u16"
  }
  object U16 { def from(x: UINT): U16 = new U16(x.bint) }

  class U32(val bint: BigInteger) extends UINT {
    assert(BigInteger.ZERO.compareTo(bint) <= 0 && bint.compareTo(CONSTANTS.U32_MOD) <= 0)
    def +(y: U32): U32 = new U32(bint.add(y.bint).mod(CONSTANTS.U32_MOD))
    def -(y: U32): U32 = new U32(bint.subtract(y.bint).mod(CONSTANTS.U32_MOD))
    def *(y: U32): U32 = new U32(bint.multiply(y.bint).mod(CONSTANTS.U32_MOD))
    def /(y: U32): U32 = new U32(bint.divide(y.bint).mod(CONSTANTS.U32_MOD))
    def %(y: U32): U32 = new U32(bint.mod(y.bint).mod(CONSTANTS.U32_MOD))
    def <=(y: U32): Boolean = bint.compareTo(y.bint) <= 0
    def >=(y: U32): Boolean = bint.compareTo(y.bint) >= 0
    def <(y: U32): Boolean = bint.compareTo(y.bint) < 0
    def >(y: U32): Boolean = bint.compareTo(y.bint) > 0
    def ===(y: U32): Boolean = bint.compareTo(y.bint) == 0
    override def equals(y: Any): Boolean = y match { case y: U32 => bint.compareTo(y.bint) == 0 case _ => ??? }
    override def toString = s"$bint.u32"
  }
  object U32 { def from(x: UINT): U32 = new U32(x.bint) }

  class U64(val bint: BigInteger) extends UINT {
    assert(BigInteger.ZERO.compareTo(bint) <= 0 && bint.compareTo(CONSTANTS.U64_MOD) <= 0)
    def +(y: U64): U64 = new U64(bint.add(y.bint).mod(CONSTANTS.U64_MOD))
    def -(y: U64): U64 = new U64(bint.subtract(y.bint).mod(CONSTANTS.U64_MOD))
    def *(y: U64): U64 = new U64(bint.multiply(y.bint).mod(CONSTANTS.U64_MOD))
    def /(y: U64): U64 = new U64(bint.divide(y.bint).mod(CONSTANTS.U64_MOD))
    def %(y: U64): U64 = new U64(bint.mod(y.bint).mod(CONSTANTS.U64_MOD))
    def <=(y: U64): Boolean = bint.compareTo(y.bint) <= 0
    def >=(y: U64): Boolean = bint.compareTo(y.bint) >= 0
    def <(y: U64): Boolean = bint.compareTo(y.bint) < 0
    def >(y: U64): Boolean = bint.compareTo(y.bint) > 0
    def ===(y: U64): Boolean = bint.compareTo(y.bint) == 0
    override def equals(y: Any): Boolean = y match { case y: U64 => bint.compareTo(y.bint) == 0 case _ => ??? }
    override def toString = s"$bint.u64"
  }
  object U64 { def from(x: UINT): U64 = new U64(x.bint) }

  class U160(val bint: BigInteger) extends UINT {
    assert(BigInteger.ZERO.compareTo(bint) <= 0 && bint.compareTo(CONSTANTS.U160_MOD) <= 0)
    def +(y: U160): U160 = new U160(bint.add(y.bint).mod(CONSTANTS.U160_MOD))
    def -(y: U160): U160 = new U160(bint.subtract(y.bint).mod(CONSTANTS.U160_MOD))
    def *(y: U160): U160 = new U160(bint.multiply(y.bint).mod(CONSTANTS.U160_MOD))
    def /(y: U160): U160 = new U160(bint.divide(y.bint).mod(CONSTANTS.U160_MOD))
    def %(y: U160): U160 = new U160(bint.mod(y.bint).mod(CONSTANTS.U160_MOD))
    def <=(y: U160): Boolean = bint.compareTo(y.bint) <= 0
    def >=(y: U160): Boolean = bint.compareTo(y.bint) >= 0
    def <(y: U160): Boolean = bint.compareTo(y.bint) < 0
    def >(y: U160): Boolean = bint.compareTo(y.bint) > 0
    def ===(y: U160): Boolean = bint.compareTo(y.bint) == 0
    override def equals(y: Any): Boolean = y match { case y: U160 => bint.compareTo(y.bint) == 0 case _ => ??? }
    override def toString = s"$bint.u160"
  }
  object U160 { def from(x: UINT): U160 = new U160(x.bint) }

  class U256(val bint: BigInteger) extends UINT {
    assert(BigInteger.ZERO.compareTo(bint) <= 0 && bint.compareTo(CONSTANTS.U256_MOD) <= 0)
    def +(y: U256): U256 = new U256(bint.add(y.bint).mod(CONSTANTS.U256_MOD))
    def -(y: U256): U256 = new U256(bint.subtract(y.bint).mod(CONSTANTS.U256_MOD))
    def *(y: U256): U256 = new U256(bint.multiply(y.bint).mod(CONSTANTS.U256_MOD))
    def /(y: U256): U256 = new U256(bint.divide(y.bint).mod(CONSTANTS.U256_MOD))
    def %(y: U256): U256 = new U256(bint.mod(y.bint).mod(CONSTANTS.U256_MOD))
    def <=(y: U256): Boolean = bint.compareTo(y.bint) <= 0
    def >=(y: U256): Boolean = bint.compareTo(y.bint) >= 0
    def <(y: U256): Boolean = bint.compareTo(y.bint) < 0
    def >(y: U256): Boolean = bint.compareTo(y.bint) > 0
    def ===(y: U256): Boolean = bint.compareTo(y.bint) == 0
    override def equals(y: Any): Boolean = y match { case y: U256 => bint.compareTo(y.bint) == 0 case _ => ??? }
    override def toString = s"$bint.u"
  }
  object U256 { def from(x: UINT): U256 = new U256(x.bint) }

  class Address(bint: BigInteger) extends U160(bint)
  object Address { def from(x: UINT): Address = new Address(x.bint) }

  class AddressPayable(bint: BigInteger) extends Address(bint) {
    def transfer(value: U256) : Unit = ???
    def send(value: U256) : Boolean = ???
  }
  object AddressPayable { def from(x: UINT): AddressPayable = new AddressPayable(x.bint) }

  implicit class ToUint(str: String) {
    private def parse(str: String): BigInteger =
      if (str.startsWith("0x")) new BigInteger(str.substring(2), 16) else new BigInteger(str)
    def u: U256 = new U256(parse(str))
    def u8: U8 = new U8(parse(str))
    def u16: U16 = new U16(parse(str))
    def u32: U32 = new U32(parse(str))
    def u64: U64 = new U64(parse(str))
    def u160: U160 = new U160(parse(str))
    def u256: U256 = new U256(parse(str))
    def a: Address = new Address(parse(str))
    def p: AddressPayable = new AddressPayable(parse(str))
  }

  object CONSTANTS {
    val U8_MOD: BigInteger = BigInteger.ONE.shiftLeft(8)
    val U16_MOD: BigInteger = BigInteger.ONE.shiftLeft(16)
    val U32_MOD: BigInteger = BigInteger.ONE.shiftLeft(32)
    val U64_MOD: BigInteger = BigInteger.ONE.shiftLeft(64)
    val U160_MOD: BigInteger = BigInteger.ONE.shiftLeft(160)
    val U256_MOD: BigInteger = BigInteger.ONE.shiftLeft(256)
  }

  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  class Event
  object emit { def apply(in: Event): Unit = ??? }

  @showAsInfix type >=>[K,V] = Mapping[K,V]
  class Mapping[K, V]() {
    def update(k: K, v: V): Unit = ???
    def get(k: K): V = ???
  }
  object Mapping { def apply[K,V](): K Mapping V = ??? }

  @showAsInfix type x[X, N <: Int] = StaticArray[X, N]
  class StaticArray[X, N <: Int](args: X*)(implicit vt: ValueOf[N]) {
    require(args.length == valueOf[N])
    val theArgs: mutable.Buffer[X] = args.toBuffer
    def apply(y: U256): X = theArgs(y.bint.intValue()) // TODO intValue can fail
    def update(y: U256, x: X): Unit = theArgs(y.bint.intValue()) = x // TODO intValue can fail
    def length: U256 = new U256(BigInteger.valueOf(valueOf[N]))
    override def toString: String = theArgs.toString
    override def equals(obj: Any): Boolean = obj match {
      case obj: StaticArray[X, N] => theArgs == obj.theArgs
      case _ => false }
  }
  object StaticArray {
    def fromSeq[X, N <: Int](args: Seq[X])(implicit vt: ValueOf[N]): StaticArray[X, N] = new StaticArray[X, N](args : _*)
  }

  class Arr[X](args: X*) {
    val theArgs: mutable.Buffer[X] = args.toBuffer
    def apply(y: U256): X = theArgs(y.bint.intValue()) // TODO intValue can fail
    def update(y: U256, x: X): Unit = theArgs(y.bint.intValue()) = x // TODO intValue can fail
    def length: U256 = new U256(BigInteger.valueOf(theArgs.length))
    def push(x: X): Unit = theArgs.append(x)
    override def toString: String = theArgs.toString
    override def equals(obj: Any): Boolean = obj match {
      case obj: Arr[_] => theArgs == obj.theArgs
      case _ => false }
  }

  object Arr {
    def ofDim[X: ClassTag](size: U256): Arr[X] = new Arr[X](Array.ofDim[X](size.bint.intValueExact()): _*)
    def apply[X](args: X*): Arr[X] = new Arr[X](args: _*)
    def fromSeq[X](args: Seq[X]): Arr[X] = new Arr[X](args: _*)
  }

}
