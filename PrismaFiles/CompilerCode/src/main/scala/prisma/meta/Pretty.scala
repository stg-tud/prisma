package prisma.meta

import prisma.FLAGS

import java.nio.file.{Files, Paths}
import java.util.regex.{MatchResult, Pattern}
import scala.reflect.runtime.universe.showRaw

trait Pretty extends MacroTop {
  import c._

  val pat = Pattern.compile(raw"" +
    raw"(class|object)\s(\S*)\sextends\s(\S*)\s(with\s\S*\s)*" +
    raw"[{](.*?)def <init>([(].*?[)])(:\s\S*)?\s=\s[{].*?[}]", Pattern.DOTALL)

  def fix(str: String): String = { //return str;
    "// NOTE: annotations on accessors (val/var) are not printed\n"+
    pat.matcher(str).replaceAll({ m: MatchResult =>
      val objektClass = m.group(1)
      val name = m.group(2)
      val parent = if (m.group(3) == null) "" else m.group(3)
      val parents = if (m.group(4) == null) "" else m.group(4)
      // val blank = m.group(5)
      val args = if (objektClass != "class") "" else m.group(6)
        .drop(1).dropRight(1)
        .split(",").filter(!_.isBlank)
        .map(x => x.trim.replaceAll("^(@.* )(\\S*): (.*)$", "$1var $2: $3")).mkString(sep=", ", start="(", end=")")
      f"$objektClass $name$args extends $parent $parents{".replace("$", "\\$").replace("\\n", "\n")
    }).replaceAll("@new ", "@")
      .replaceAll("<stable> <accessor> <paramaccessor> val ", "val ")
      .replaceAll("<stable> <accessor> val ", "val ")
      .replaceAll("<accessor> <paramaccessor> val ", "var ")
      .replaceAll("<accessor> val ", "var ")
      .replaceAll("<[a-zA-Z_.]*?> ", "")
      .replaceAll("<[a-zA-Z_.]*?>", "")
      .replaceAll("[.](\\S*)_=[(]", ".$1 = (")

      .replaceAll("ToUint[.]", "")
      .replaceAll("@prisma[.]Prisma[.]", "@")
      .replaceAll("@prisma[.]meta[.]MacroDef[.]", "@")
      .replaceAll("prisma[.]Prisma[.]", "")
      .replaceAll("prisma[.]meta[.]MacroDef[.]", "")
      .replaceAll("prisma[.]meta[.]BangNotation", "")
      .replaceAll("[.]apply", "")
      .replaceAll("[$]less[$]times[$]greater", "<*>")
      .replaceAll("_root_[.]", "")
  }

  var debugCtr = 0
  def debugLog(tree: Tree): Tree = if (!FLAGS.PRINT_INTERMEDIATE_COMPILER_PHASES) {debugCtr+=1; tree} else {
    //println(tree.collect { case x: RefTree if x.toString.endsWith("hashedWord") => x }.map(x=>(x, x.pos.line, if (x.pos.isDefined) x.pos.start, if (x.pos.isDefined) x.pos.end)))
    Files.writeString(Paths.get(s"out/$filename-ph${debugCtr+=1; debugCtr}.scala"), fix(tree.toString))
    //Files.writeString(Paths.get(s"out/$filename-ph${debugCtr+=1; debugCtr}.scala"), showRaw(tree, printTypes=true))
    tree
  }

  def debugPhase(phase: Tree => Tree): Tree => Tree = { trees =>
    Files.writeString(Paths.get("out/before.scala"), trees.toString)
    val trees2 = phase(trees)
    Files.writeString(Paths.get("out/after.scala"), trees2.toString)
    import sys.process._
    "meld out/before.scala out/after.scala".run()
    trees2
  }

}
