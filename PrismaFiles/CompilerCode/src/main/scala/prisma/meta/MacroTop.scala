package prisma.meta

import java.nio.file.{Files, Paths}
import prisma.Prisma._
import prisma.meta.MacroDef.top
import retypecheck.ReTyper

import scala.reflect.macros.blackbox

trait MacroTop {
  val c: blackbox.Context

  import c.universe._
  import internal.decorators._

  def getSym2(sym: Symbol): Symbol =
    if (sym.isMethod && sym.asMethod.isAccessor) sym.asMethod.accessed else sym

  def isContractSym(sym: Symbol): Boolean = getSym2(sym).annotations.exists(_.tree.tpe.typeSymbol == typeOf[co].typeSymbol)
  def isClientSym  (sym: Symbol): Boolean = getSym2(sym).annotations.exists(_.tree.tpe.typeSymbol == typeOf[cl].typeSymbol)
  def isTopSym     (sym: Symbol): Boolean = getSym2(sym).annotations.exists(_.tree.tpe.typeSymbol == typeOf[top].typeSymbol)
  def isCrossSym   (sym: Symbol): Boolean = getSym2(sym).annotations.exists(_.tree.tpe.typeSymbol == typeOf[cross].typeSymbol)
  def isViewSym    (sym: Symbol): Boolean = getSym2(sym).annotations.exists(_.tree.tpe.typeSymbol == typeOf[view].typeSymbol)
  def isEventSym    (sym: Symbol): Boolean = getSym2(sym).annotations.exists(_.tree.tpe.typeSymbol == typeOf[event].typeSymbol)

  lazy val filename: String = c.enclosingPosition.source.path.split("/").last.split("\\.").head

  def fix(str: String): String
  def infoOnError[X](x: Tree)(f: Tree => X): X =
    try f(x) catch {
      case e: Throwable =>
        Files.writeString(Paths.get(s"out/$filename-error.scala"), fix(x.toString))
        println(s"detailed error input, see 'out/$filename-error.scala'")
        throw e
    }

  // using retypecheck // intellij thinks this is an error. it is not...
  //var ctr = 0
  val typecheck   = (tree: Tree) => infoOnError(tree) { ReTyper(c) typecheck _   }.asInstanceOf[tree.type]
  val untypecheck = (tree: Tree) => infoOnError(tree) { ReTyper(c) untypecheck _ }.asInstanceOf[tree.type]
  val retypecheck = (tree: Tree) => infoOnError(tree) { ReTyper(c) retypecheck _ }.asInstanceOf[tree.type]

  // dont introduce private[this] around valdefs
  val retermcheck = (tree: Tree) => { // TODO sadly modifies full path classnames?
//    c.info(tree.pos, s"termcheck = $tree", force = true)
    val res = (ReTyper(c) retypecheck q"new _root_.scala.AnyRef { val z = $tree }") match {
      case q"$_ class $_ extends $_ { ${ValDef(mods, name, tpt, tree)} }; $_" => tree
    }
//    c.info(tree.pos, s"termcheck : $res", force = true)
    res
    //ReTyper(c) retypecheck tree
  }
  def root: Tree = q"_root_" setSymbol c.mirror.RootPackage // TODO c.mirror vs c.universe.rootMirror

  def termSymbolOf[T: TypeTag]: Tree = {
    // internal.setSymbol(q"${weakTypeOf[T].termSymbol.asTerm.name}", weakTypeOf[T].termSymbol)
    val SingletonTypeTree(res) = ReTyper(c).createTypeTree(typeOf[T], NoPosition)
    res
  }
  def termState = q"${termSymbolOf[MacroDef.type]}.state"
  def termWho = q"${termSymbolOf[MacroDef.type]}.who"
  def termSender = q"${termSymbolOf[sender.type]}.apply()"
  def termToUint = q"_root_.prisma.Prisma.ToUint"
  def termRaceClient = q"${termSymbolOf[raceClient.type]}"
  def termClient = q"${termSymbolOf[client.type]}"
  def termContract = q"${termSymbolOf[contract.type]}"
  def termArrOfDim = q"_root_.prisma.Prisma.Arr.ofDim"


}
