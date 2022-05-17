package prisma.meta

import prisma.FLAGS

import java.nio.file.{Files, Path, Paths}

trait Phases extends MacroTop with Util with Pretty {
  import c.universe._

  type Phase = Tree => Tree

  @inline implicit class PhaseUtils(ph: Phase) {
    @inline def **(other: Phase): Phase = (tree: Tree) =>
      (ph andThen debugLog andThen retypecheck andThen other)(tree)
    @inline def *(other: Phase): Phase = (tree: Tree) =>
      (ph andThen other)(tree)
  }

  def compilerPhase(annottees: Seq[Expr[Any]], phases: Phase): Expr[Unit] = {
    // guard against infinite macro expansion...
    val isRecursiveExpansion = c.openMacros.count { other =>
      c.enclosingPosition == other.enclosingPosition &&
      c.macroApplication.toString == other.macroApplication.toString
    } > 2
    //println(s"attempt ${isRecursiveExpansion} ${annottees.length} ${annottees.toString.substring(0, 100)}")
    if (isRecursiveExpansion) return c.Expr[Unit](asBlock(annottees.map(_.tree).toList))

    // ensure out folder exists and is empty
    val outFolder = Paths.get("out/")
    if (!Files.exists(outFolder)) Files.createDirectory(outFolder)
    Files.list(outFolder).toArray().toSeq.foreach { case path: Path =>
      //println(s"check ${path.toString} ${s"out/$filename-"} ${path.toString.startsWith(s"out/$filename-")}")
      if (path.toString.startsWith(s"out/$filename-")) {
        if (path.toString.endsWith(".txt")
        || path.toString.endsWith(".scala")
        || path.toString.endsWith(".sol")) {
          println(s"clean $path")
          Files.delete(path)
        }
      }
    }

    val (potKlass, objekt) = annottees map { _.tree } match {
      case List(objekt: ModuleDef) => (None, objekt)
    }

    var stats: Tree = if (potKlass.isEmpty) objekt else potKlass.get
    //Files.writeString(Paths.get(s"out/$filename-ph0-0untyped.scala"), fix(stats.toString))
    stats = typecheck(stats)
    stats = retypecheck(stats)
    if (FLAGS.PRINT_SCALA_INPUT) Files.writeString(Paths.get(s"out/$filename-ph0-1typed.scala"), fix(stats.toString))
    //stats = initializeSymbols(stats)
    stats = phases(stats)
    //println(stats.collect { case x: RefTree if x.toString.endsWith("hashedWord") => x }.map(x=>(x, if (x.pos.isDefined) x.pos.line, if (x.pos.isDefined) x.pos.start, if (x.pos.isDefined) x.pos.end)))
    stats = untypecheck(stats)
    println()
    if (FLAGS.PRINT_SCALA_OUTPUT) Files.writeString(Paths.get(s"out/$filename-ph${debugCtr=debugCtr+1; debugCtr}-final.scala"), fix(stats.toString))

    c.Expr[Unit](stats)
  }

  private val asBlock = (stats: List[Tree]) => Block(stats, q"()")

}