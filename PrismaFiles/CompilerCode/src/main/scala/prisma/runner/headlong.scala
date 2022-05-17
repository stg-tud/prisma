package prisma.runner

import com.esaulpaugh.headlong.abi
import com.esaulpaugh.headlong.abi.Tuple
import prisma.FLAGS

object headlong {

  def arrToHex(bytes: Array[Byte]): String =
    _root_.com.esaulpaugh.headlong.util.FastHex.encodeToString(bytes)

  def hexToArr(hex: String): Array[Byte] =
    com.esaulpaugh.headlong.util.FastHex.decode(hex)

  def bytesSelector(signature: String): Array[Byte] =
    new abi.Function(signature, "()").selector()

  def hexSelector(signature: String): String =
    new abi.Function(signature, "()").selectorHex()

  def tupleToArr(signature: String, arguments: AnyRef*): Array[Byte] = {
    if (FLAGS.PRINT_DE_SERIALIZATION) println(s"  tupleToHex $signature ${Tuple.of(arguments: _*)}")
    val bytes = abi.TupleType.parse(signature.drop(signature.indexOf('('))).encode(Tuple.of(arguments:_*)).array()
    if (FLAGS.PRINT_DE_SERIALIZATION) println(("    " + showHex(arrToHex(bytes))).replaceAll("\n", "\n    "))
    bytes
  }
  def tupleToArrPacked(signature: String, arguments: AnyRef*): Array[Byte] = {
    if (FLAGS.PRINT_DE_SERIALIZATION) println(s"  tupleToHexPacked $signature ${Tuple.of(arguments: _*)}")
    val bytes = abi.TupleType.parse(signature.drop(signature.indexOf('('))).encodePacked(Tuple.of(arguments:_*)).array()
    if (FLAGS.PRINT_DE_SERIALIZATION) println(("    " + showHex(arrToHex(bytes))).replaceAll("\n", "\n    "))
    bytes
  }

  def tupleToHex(signature: String, arguments: AnyRef*): String =
    arrToHex(tupleToArr(signature, arguments:_*))

  def arrToTuple(signature: String, bytes: Array[Byte]): Tuple = {
    if (FLAGS.PRINT_DE_SERIALIZATION) println(s"  hexToTuple $signature (")
    if (FLAGS.PRINT_DE_SERIALIZATION) println("    " + showHex(arrToHex(bytes)).replaceAll("\n", "\n    "))
    val result = abi.TupleType.parse(signature).decode(bytes)
    if (FLAGS.PRINT_DE_SERIALIZATION) println("  ) = " + result)
    result
  }

  def hexToTuple(signature: String, hex: String): Tuple = {
    //println(signature)
    //println(hex)
    arrToTuple(signature, hexToArr(hex))
  }

  def showHex(result: String, head: Boolean = false): String = {
    if (head) result.substring(0, 8) +"\n"+ showHex(result.substring(8), false)
    else result.replaceAll(".{64}", "$0\n").trim
  }

}
