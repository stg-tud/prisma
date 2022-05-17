package prisma.runner

import prisma.FLAGS
import prisma.runner.ethereum.{dupDepth, instrLength, isDup, isPush, isSwap, opcodeName, opcodes, swapDepth}
import prisma.runner.web3s.Compiled

import java.math.BigInteger
import java.nio.file.{Files, Paths}
import scala.annotation.tailrec
import scala.collection.mutable

// TODO read sstore gas counting: https://eips.ethereum.org/EIPS/eip-1283

class Analysis(compiled: Compiled) {

  def disasm(): Unit = {
    val buf = Files.newBufferedWriter(Paths.get("out/disasm.txt"))
    buf.write("    |")
    compiled.instrs.indices.dropRight(1).foreach { i =>
      val start = compiled.instrs(i)
      val end = compiled.instrs(i + 1)
      val args = compiled.binRT.substring(start * 2 + 2, end * 2)

      if (opname(at(start)) == "JUMPDEST") buf.write("\n%04x|".format(start))
      else buf.write(" %s".format(if (opname(at(start)).startsWith("PUSH")) args else opname(at(start))))

      if ("JUMP JUMPI STOP REVERT RETURN INVALID ".contains(opname(at(start)) + " ") /*|| opname(at(end)) == "JUMPDEST"*/)
        buf.write("\n    |")
    }

    buf.write("\nDATA:\n")

    buf.write(s"\n${compiled.binRT.substring(compiled.instrs.last * 2 + 2)}\n")
    buf.flush()
    buf.close()
  }

  def listPaths(): Unit = {
    val res = pathsOf(SymbolicState(0, Seq(), Set(), 0, Seq(), Seq(), ""))

    println()
    println("result = {")
    res.sortBy(_.gas).foreach(println)
    println("}")
  }


  ////////////////////////////////////////////////////////////////////////////////////////////////

  def at(i: Int): Int = Integer.valueOf(iat(i), 16)
  def iat(i: Int): String = compiled.binRT.substring(i*2, i*2+2)
  def opname(i: Int): String = ethereum.opcodeName(i, "?" + i.toHexString + "?")

  private lazy val lines: Seq[Int] = {
    val result = mutable.Buffer[Int]()
    result.append(0)
    compiled.instrs.sliding(2).foreach { case List(prev, now) =>
      if (opname(at(now)) == "JUMPDEST" || "JUMP JUMPI STOP REVERT RETURN INVALID ".contains(opname(at(prev))+" "))
        result.append(now)
    }
    result.toSeq
  }

  private lazy val lineEnd: Map[Int, Int] = lines.sliding(2).toSeq.dropRight(0)
    .map { case List(fromByte, toByte) => fromByte -> toByte }.toMap

  // CITE Borzacchiello, Coppa, D'Elia, Demetrescu. STVR'19.
  // MEMSIGHT - MEMORY MODELS IN SYMBOLIC EXECUTION: KEY IDEAS AND NEW THOUGHTS.
  // doi.org/10.1002/stvr.1722
  // TODO not yet implemented: merge optimization...
  case class Event(ptr: zExpr, data: zExpr, cond: Set[zExpr]) {
    override def toString: String =
      if (cond.nonEmpty) s"$ptr|->$data if $cond"
      else s"$ptr|->$data"
  }
  case class SymbolicState(pc: Int, stack: Seq[zExpr], cond: Set[zExpr], gas: Int,
                           memTrace: Seq[Event], stoTrace: Seq[Event],
                           comment: String) {
    override def toString: String = {
        "pc = " + pc.toHexString +
      "\n  cond   = " + cond.mkString(", ") +
      "\n  mem tr = " + memTrace.mkString(", ") +
      "\n  sto tr = " + stoTrace.mkString(", ") +
      "\n  gas    = " + gas +
      "\n  comment= " + comment
    }
  }
  sealed trait zExpr
  case class Call(args: Seq[zExpr]) extends zExpr { override def toString: String = args.mkString("[", " ", "]") }
  case class Instr(s: String) extends zExpr { override def toString: String = {
    if (s == "ITE") return s
    val i = Integer.valueOf(s.substring(0, 2), 16)
    if (isPush(i)) s.substring(2) else opcodeName(i, "??")
  } }
  object Know {
    def unapply(i: Instr): Option[String] =
      if (!isPush(Integer.valueOf(i.s.substring(0, 2), 16))) None
      else Some(i.s.substring(2))
    def apply(s: String): Instr = {
      val i = 0x60 + Math.ceil(s.length / 2.0).toInt - 1
      assert(isPush(i), (s, i).toString)
      Instr(i.toHexString + s)
    }
  }

  var unknowctr = -1
  def pathsOf(state: SymbolicState): Seq[SymbolicState] = {

    val fromByte = state.pc
    val buf = mutable.Buffer.from(state.stack)
    var gas = state.gas
    var cond = state.cond
    val memTrace = mutable.Buffer.from(state.memTrace)
    val stoTrace = mutable.Buffer.from(state.stoTrace)
    def nextState(target: Int, comment: String): SymbolicState =
      SymbolicState(target,
        buf.toSeq, cond, gas,
        memTrace.toSeq, stoTrace.toSeq, comment)

    //def gasfmt = "%8d".format(gas)

    if (!lineEnd.contains(state.pc)) return Seq(nextState(state.pc, s"${state.pc} not in ${lineEnd.keys.toSeq.sorted}"))
    if (gas >= 1000) return Seq(nextState(fromByte, s"STUCK  too much gas..."))

    println(state)
    val toByte = lineEnd(state.pc)

    def newUnkn(): Unit = {
      sys.error("stack underflow?")
      unknowctr += 1
      buf.insert(0, Instr("$" + unknowctr))
    }
    @tailrec def bufGet(idx: Int): zExpr =
      if (idx >= 0) buf(idx)
      else {
        newUnkn()
        bufGet(idx + 1)
      }
    @tailrec def bufPop(): zExpr =
      if (buf.nonEmpty) buf.remove(buf.length - 1)
      else {
        newUnkn()
        bufPop()
      }
    @tailrec def bufSwap(i: Int, j: Int): Unit = {
      if ((i min j) < 0) {
        newUnkn()
        bufSwap(i + 1, j + 1)
      } else {
        val tmp = buf(i)
        buf(i) = buf(j)
        buf(j) = tmp
      }
    }

    def execInstr(j: Int): Unit = {
      val instrInt = at(j)
      val instr = opcodes(instrInt)
      if (FLAGS.PRINT_SYMBOLIC_EXECUTION_STACK)
        println(buf.mkString("buf = [", " ", "]") + " " + instr._4)

      assert(!"JUMP JUMPI STOP REVERT RETURN INVALID ".contains(instr._4 + " "))
      instr._2 match {
        case _ if instr._4 == "JUMPDEST" =>
          // pass
        case _ if isPush(instrInt) =>
          buf.append(Instr(compiled.binRT.substring(2*j, 2*(j+instrLength(instrInt)))))
        case _ if isDup(instrInt) =>
          buf.append(bufGet(buf.length - dupDepth(instrInt)))

        case _ if isSwap(instrInt) =>
          bufSwap(buf.length - swapDepth(instrInt) - 1, buf.length - 1)

        case 0 =>
          val elems = (0 until instr._1).map(_ => bufPop()).reverse
          (opname(at(j)), elems) match {
            case ("SSTORE", Seq(data, ptr)) =>
              stoTrace append Event(ptr, data, Set())
            case ("MSTORE", Seq(data, ptr)) =>
              memTrace append Event(ptr, data, Set())
            case ("CODECOPY", Seq(Know(length), Know(srcPtr), Know(dstPtr))) =>
              val src = Integer.valueOf(srcPtr, 16)
              val dst = Integer.valueOf(dstPtr, 16)
              val len = Integer.valueOf(length, 16)
              val limit = 0x40/2
              var i = 0
              while (i < len) {
                val chunk = limit min (len - i)
                val data = Know(compiled.binRT.substring(2*(src + i), 2*(src + i + chunk)).padTo(limit, '0')) // TODO zero extend right?
                memTrace append Event(Know(Integer.toHexString(dst + i)), data, Set())
                i += chunk
              }
            //case ("CALLDATACOPY") =>
            //case ("EXTCODECOPY") =>
            //case ("RETURNDATACOPY") =>
            case ("POP", Seq(_)) =>
              // pass
            case t =>
              sys.error(s"NYI execute($t)")
          }

        case 1 =>
          // TODO order, is it [SUB X Y] or [SUB Y X], test it
          val elems = (0 until instr._1).map(_ => bufPop()).reverse

          def b2u(b: Boolean): BigInteger = if (b) BigInteger.ONE else BigInteger.ZERO // TODO true=1 false=0 in evm?
          (opname(at(j)), elems) match {

            // knowledge:            callvalue -> 00
            // knowledge:      00 calldataload -> b71349f800000000000000000000000000000000000000000000000000000000
            // knowledge:   04 calldatasize lt -> 00
            //case ("CALLVALUE", Seq()) =>
            //  buf.append(Know("00"))
            //case ("CALLDATALOAD", Seq(Know("00"))) =>
            //  buf.append(Know("8fa3be8e00000000000000000000000000000000000000000000000000000000"))
            //case ("LT", Seq(Know("04"), Call(Seq(), op))) if opname(op) == "CALLDATASIZE" =>
            //  buf.append(Know("00"))

            case ("SLOAD", Seq(ptr)) =>
              // FIXME assumes storage is initially zero (not true)
              // very problematic pointer storage data (arrays, structs), and constants
              buf append trLookup(stoTrace, ptr)
            case ("MLOAD", Seq(ptr)) =>
              buf append trLookup(memTrace, ptr)

            case ("ISZERO", Seq(Know("0"))) =>
              buf.append(Know("01"))
            case ("ISZERO", Seq(Know("00"))) =>
              buf.append(Know("01"))
            case ("ISZERO", Seq(Know(_))) =>
              buf.append(Know("00"))

            // order important
            case ("SHL", Seq(Know(x), Know(y))) =>
              val (a, b) = (new BigInteger(x, 16), new BigInteger(y, 16))
              val c = a.shiftLeft(b.intValueExact())
              buf.append(Know(c.toString(16)))
            case ("SHR", Seq(Know(x), Know(y))) =>
              val (a, b) = (new BigInteger(x, 16), new BigInteger(y, 16))
              val c = a.shiftRight(b.intValueExact())
              buf.append(Know(c.toString(16)))
            case ("SUB", Seq(Know(y), Know(x))) =>
              val (a, b) = (new BigInteger(x, 16), new BigInteger(y, 16))
              val c = a.subtract(b)
              buf.append(Know(c.toString(16)))
            case ("EXP", Seq(Know(x), Know(y))) =>
              val (a, b) = (new BigInteger(x, 16), new BigInteger(y, 16))
              val c = a.pow(b.intValueExact())
              buf.append(Know(c.toString(16))) // TODO modpow u256
            case ("GT", Seq(Know(x), Know(y))) =>
              val (a, b) = (new BigInteger(x, 16), new BigInteger(y, 16))
              val c = b2u(+1 == a.compareTo(b))
              buf.append(Know(c.toString(16)))
            case ("LT", Seq(Know(x), Know(y))) =>
              val (a, b) = (new BigInteger(x, 16), new BigInteger(y, 16))
              val c = b2u(-1 == a.compareTo(b))
              buf.append(Know(c.toString(16)))

            // order not important
            case ("NOT", Seq(Know(x))) =>
              val a = new BigInteger(x, 16)
              val c = prisma.Prisma.CONSTANTS.U32_MOD.subtract(BigInteger.valueOf(1)).subtract(a) // TODO wrong
              buf.append(Know(c.toString(16)))
            case ("AND", Seq(Know(x), Know(y))) =>
              val (a, b) = (new BigInteger(x, 16), new BigInteger(y, 16))
              val c = a.and(b)
              buf.append(Know(c.toString(16)))
            case ("OR", Seq(Know(x), Know(y))) =>
              val (a, b) = (new BigInteger(x, 16), new BigInteger(y, 16))
              val c = a.or(b)
              buf.append(Know(c.toString(16)))
            case ("ADD", Seq(Know(x), Know(y))) =>
              val (a, b) = (new BigInteger(x, 16), new BigInteger(y, 16))
              val c = a.add(b)
              buf.append(Know(c.toString(16)))
            case ("MUL", Seq(Know(x), Know(y))) =>
              val (a, b) = (new BigInteger(x, 16), new BigInteger(y, 16))
              val c = a.multiply(b)
              buf.append(Know(c.toString(16)))
            case ("EQ", Seq(Know(x), Know(y))) =>
              val (a, b) = (new BigInteger(x, 16), new BigInteger(y, 16))
              val c = b2u(0 == a.compareTo(b))
              buf.append(Know(c.toString(16)))

            case _ =>
              buf.append(Call(elems :+ Instr(iat(j))))
          }

        case _ =>
          println(instr)
          ???
          //(0 until instr._1).foreach(_ => buf.remove(buf.length - 1))
          //(0 until instr._2).foreach(_ => buf.append(Instr("??")))
      }
      gas += instr._3
    }

    val opspos = compiled.instrs.dropWhile(_<fromByte).takeWhile(_<toByte)
    opspos.dropRight(1).foreach(execInstr)

    // TODO REVERT if JUMP target is not JUMPDEST ?

    val finalop = opcodes(at(opspos.last))
    val gotos: Seq[(Seq[zExpr], Int)] = finalop._4 match {
      case "JUMP" | "JUMPI" | "REVERT" | "RETURN" | "STOP" | "INVALID" =>
        if (FLAGS.PRINT_SYMBOLIC_EXECUTION_STACK)
          println(buf.mkString("buf = [", " ", "]") + " " + finalop._4 + "\n")

        val elems = (0 until finalop._1).map(_ => bufPop()).reverse
        finalop._4 match {
          case "JUMP" => elems match {
            case Seq(Know(t)) => List(Seq() -> Integer.valueOf(t, 16))
            case Seq(target) => return Seq(nextState(opspos.last, s"STUCK  JUMP to $target"))
          }
          case "JUMPI" => elems match {
            case Seq(Know(cond), Know(target)) if cond == "01" || cond == "1" =>
              List(Seq() -> Integer.valueOf(target, 16))
            case Seq(Know(_), Know(_)) =>
              List(Seq() -> toByte)
            case Seq(cond, Know(target)) =>
              List(Seq(cond) -> Integer.valueOf(target, 16), Seq(Call(Seq(cond, Instr("19")))) -> toByte)
            case Seq(Know(cond), target) if cond == "1" || cond == "01" =>
              return Seq(nextState(opspos.last, s"STUCK  JUMPI $cond $target"))
            case Seq(Know(_), _) =>
              List(Seq() -> toByte)
            case Seq(cond, target) =>
              return Seq(state.copy(comment = s"STUCK  JUMPI $cond $target"))
          }
          case "REVERT" | "RETURN" =>
            val NAME = if (finalop._4 == "REVERT") "REVERT" else "RETURN"
            return elems match {
              case Seq(Know(length), Know(offset)) =>
                val o = Integer.valueOf(offset, 16)
                val l = Integer.valueOf(length, 16)
                val res = Range(o, o + l, 0x20)
                  .map(ptr => trLookup(memTrace, Know(ptr.toHexString)).toString.padTo(0x40, '0'))
                  .mkString("[", " ", "]")
                Seq(nextState(opspos.last, s"$NAME $offset $length $res"))
              case Seq(length, offset) =>
                Seq(nextState(opspos.last, s"$NAME $offset $length"))
            }
          case "STOP" | "INVALID" =>
            val Seq() = elems
            return Seq(nextState(opspos.last, finalop._4))
        }
      case t =>
        execInstr(opspos.last)
        if (FLAGS.PRINT_SYMBOLIC_EXECUTION_STACK) println()
        List(Seq() -> toByte)
      // TODO last op gas may be counted twice?
    }

    gotos.flatMap {
      case (cond_, target) if lineEnd.contains(target) =>
        cond = cond ++ cond_
        pathsOf(nextState(target, ""))
    }
  }

  private def trLookup(tr: mutable.Buffer[Event], ptr: zExpr) = {
    tr.foldLeft(Know("00"): zExpr) { (expr, ev) =>
      (ptr, ev.ptr) match {
        case (Know(x), Know(y)) if x != y =>
          expr
        case _ =>
          val ptrEq =
            if (ptr == ev.ptr) Know("ff")
            else Call(Seq(ptr, ev.ptr, Instr("14")))
          val cond = ev.cond.fold(ptrEq) { (a, b) => Call(Seq(a, b, Instr("16"))) }
          if (cond == Know("ff")) ev.data
          else Call(Seq(expr, ev.data, cond, Instr("ITE")))
      }
    }
  }

  // TODO include memory, storage & copy fees
  def memFee(high: Int): Int = {
    // FIXME note this does (high32 ** 2) in constrast to manticore who do (delta ** 2), who is right?
    // https://github.com/trailofbits/manticore/blob/c6f457d72e1164c4c8c6d0256fe9b8b765d2cb24/manticore/platforms/evm.py#L1176
    val high32 = (high + 32 - 1) / 32 // round up integer division
    val fee  = high32 * 3 + high32 * high32 / 512
    fee
  }
}