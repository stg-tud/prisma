package prisma.runner

import org.web3j.protocol.core.Response

object ErrHelper {
  private val msgmatch = "(<stdin>|tmp.sol):([0-9]+):([0-9]+): ([^\n]*)\n([^\n]*)\n([^\n]*)\n".r
  private val wordmatch = "/\\*([0-9]+):([0-9]+)\\*/".r

  def handleRespError(err: Response.Error, inSig: String, inst2sol: Seq[(Int, Int, Int)], byte2inst: Seq[Int], sol: String, scala: String): Unit = {
    val errString = err.getData
    println((err.getCode, err.getData, err.getMessage, err.getData))
    println("\n[ERROR %s] %s\n[MESSAGE] %s".format(err.getCode, err.getMessage.take(43), err.getMessage.drop(43)))
    print(s"trying to call '$inSig'"); println(s" - ${headlong.hexSelector(inSig)}")
    if (err.getMessage == "VM Exception while processing transaction: revert")
      println(
        """(possible causes are:
          |* require without message;
          |* function (signature) not found;
          |* array out of bounds;
          |* abi.decode failure;
          |* arithmetic overflow/underflow;
          |* or else ...?)""".stripMargin)
    if (err.getData == null) println("pc = (null)\n") else {
      val byteCounterString = web3s.getStringFromJson(errString, ",\"program_counter\":")
      if (byteCounterString == "") println("pc = (null)\n") else {
        val byteCounter = byteCounterString.init.toInt
        val instrCounter = byte2inst.indexOf(byteCounter)
        val solpos = inst2sol(instrCounter)
        if (solpos._3 == 0) {
          val line = sol.substring(0, solpos._1).count(_ == '\n')+1
          val linestart = sol.lastIndexOf('\n', solpos._1)
          val lineend   = sol.indexOf('\n', solpos._1 + solpos._2)
          //val solslice = sol.substring(solpos._1, solpos._1 + solpos._2)
          val solslice = sol.substring(linestart+1, lineend-1)
          val msg = solslice + "\n" + " ".repeat(solpos._1 - linestart - 1) + "^".repeat(solpos._2)
          println(s" ! byte: $byteCounter ! inst: $instrCounter ! solLine: $line ! solSource:\n$msg")
          ErrHelper.printError("scalaSource in:", msg, scala)
        } else
          println(s" ! byte: $byteCounter ! inst: $instrCounter")
        println()
      }
    }
    println("'%s'".format(errString))
  }

  def printSolcErrorAsNewlangError(msg: String, origSrc: String): Unit = {
    //println(msg)
    msgmatch.findAllIn(msg).matchData foreach { m =>
      //val line = m.group(2).toInt
      //val char = m.group(3).toInt
      val errmsg = m.group(4)
      val codeline = m.group(5)
      val markline = m.group(6)

      //val rest = src.split("\n")(line-1).drop(char)
      val rest = (0 until markline.length).map(i =>
        if (markline(i) != ' ') codeline(i) else ' ').mkString

      printError(errmsg, rest, origSrc)
    }
  }

  def printError(errmsg: String, srcPart: String, origSrc: String): Unit = {
    val word = wordmatch.findAllMatchIn(srcPart)
    val lines = word.map { w =>
      val i = w.group(1).toInt
      val j = w.group(2).toInt
      mkErrorPlacement(origSrc, i, j)
    }
    printErrorPlacements(errmsg, origSrc, lines)
  }

  def printErrorPlacements(errmsg: String, origSrc: String,
                           lines: Iterator[((Int, Int, Int), (Int, Int))]): Unit = {
    lines.toSeq.groupBy(x => x._1).foreach { case ((origLine, origLineStart, origLineEnd), parts) =>
      println("%d: %s".format(origLine, errmsg))
      println(origSrc.slice(origLineStart + 1, origLineEnd))
      println(0.until(origLineEnd - origLineStart).map(i =>
        if (parts.exists { case (_, (origChar, length)) =>
          origChar <= i && i <= origChar + length
        }) "^" else " "
      ).mkString(""))
    }
  }

  def mkErrorPlacement(origSrc: String, i: Int, j: Int): ((Int, Int, Int), (Int, Int)) = {
    val length = j - i - 1
    val origLineStart = origSrc.slice(0, i).lastIndexOf("\n")
    val origLineEnd = origSrc.indexOf("\n", j)
    val origLine = origSrc.slice(0, origLineStart).count(_ == '\n')
    val origChar = i - origLineStart - 1
    ((origLine, origLineStart, origLineEnd), (origChar, length))
  }
}
