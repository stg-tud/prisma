package prisma.meta

import com.esaulpaugh.headlong.abi.Tuple
import prisma.Prisma
import prisma.runner.{Account, web3s}

import java.math.BigInteger
//import prisma.experiment.PSplitOld
import prisma.phases.{PSecurity, PSimplify, PTranslateExpr}
import prisma.Prisma._

import java.lang.Thread.sleep
import scala.annotation.{StaticAnnotation, tailrec}
import scala.collection.mutable
//import scala.language.experimental.macros
import scala.reflect.macros.blackbox

object MacroDef {

  object Closure {
    def apply[R,S](function: (S) => R, userdata: S): () => R =
      () => function(userdata)
    def apply[A0,R,S](function: (A0, S) => R, userdata: S): A0 => R =
      (a0) => function(a0, userdata)
    def apply[A0,A1,R,S](function: (A0, A1, S) => R, userdata: S): (A0, A1) => R =
      (a0, a1) => function(a0, a1, userdata)
    def apply[A0,A1,A2,R,S](function: (A0, A1, A2, S) => R, userdata: S): (A0, A1, A2) => R =
      (a0, a1, a2) => function(a0, a1, a2, userdata)
  }
  object ClosureString {
    val callfunc: mutable.Map[U32, Any => Nothing] = mutable.Map()
    def apply[R](function: U32, userdata: Any): () => R =
      () => callfunc(function)(userdata)
    def apply[A0,R](function: U32, userdata: Any): A0 => R =
      (a0) => callfunc(function)((a0, userdata))
    def apply[A0,A1,R](function: U32, userdata: Any): (A0, A1) => R =
      (a0, a1) => callfunc(function)((a0, a1, userdata))
    def apply[A0,A1,A2,R](function: U32, userdata: Any): (A0, A1, A2) => R =
      (a0, a1, a2) => callfunc(function)((a0, a1, a2, userdata))
  }

  class RemoteMapping[K, V](acc: Account, deployed: web3s.Deployed, inSig: String, outSig: String, abiEncoder: K => Tuple, abiDecoder: Tuple => V) extends Mapping[K, V] {
    override def get(k: K): V = abiDecoder(deployed.magic(BigInteger.ZERO, acc, inSig, outSig, abiEncoder(k).toArray:_*))
  }

  trait ContractInterface {
    def dispatchContract(sig: (String, U256, Tuple)): String = ???
    def dispatchClient(x: String, y: Array[Byte]): (String, U256, Tuple) = ???

    def me(): AddressPayable = ??? // user addr
    def state(): U256 = ???
    def userdata(): Array[Byte] = ???
    def stateToFuncName(state: String): String = ???

    def addr: Address = ??? // contract addr

    //var ctr = 0
    @tailrec final def trampoline(finalsig: String): Tuple = {
      val (theState, theUserdata) = (state(), userdata())
      if (theState == "0".u256) {
        val hex: String = prisma.runner.headlong.arrToHex(theUserdata)
        println(s"<- (0x$hex) as $finalsig")
        if (finalsig == "()") new com.esaulpaugh.headlong.abi.Tuple()
        else prisma.runner.headlong.hexToTuple(finalsig, hex)
      } else {
        //println(s"$me: New Security-ID: (state) = ${stateToFuncName(theState.bint.toString(16))}")
        val statenumber = theState.bint.toString(16)
        val zeropadded = "0".repeat(8 - statenumber.length) + statenumber
        val res = dispatchClient(zeropadded, theUserdata) // dispatchClient(theAction, theUserdata)
        if (res == null) {
          print(".")
          sleep(1000)
          trampoline(finalsig)
        } else {
          val statename = theState.bint.toString(16)
          val zeropadded = "0".repeat(8 - statenumber.length) + statenumber
          println(s"<- ${stateToFuncName(zeropadded)}(${theUserdata.mkString(", ")})")
          try dispatchContract(res) catch { case _: NumberFormatException => }
          trampoline(finalsig)
        }
      }
    }
  }

  final class top extends StaticAnnotation
  @top object markedTop
  @Prisma.co object markedContract
  @Prisma.cl object markedClient
  @Prisma.cross object markedCross
  @Prisma.view object markedView

  //var who: AddressPayable = "0".p
  var state: U32 = "0".u32

}

// quasiquote summary:        https://docs.scala-lang.org/overviews/quasiquotes/syntax-summary.html
// def-macros example:        https://stackoverflow.com/questions/53640045/exploring-expression-tree-within-scala-macro
// annotation-macros example: https://docs.scala-lang.org/overviews/macros/annotations.html

class MacroImpl(context: blackbox.Context)
  extends PSecurity
     with PSimplify
     //with PSplitOld
     with PTranslateExpr
{
  override val c: blackbox.Context = context
  import c.universe._

  // lift contract { ... } in val, var, def & expr
  private val liftTasks: Phase = (tree: Tree) => presTransformPlusRest(tree) (that => {
    //case q"$mods var $tname: $tpt = $f.apply[$tpe]($expr)" withAttrs (_,t,p) if isType[MacroDef.contract.type](f) =>
    //  q"${mark(mods)} var $tname: $tpt = _root_.macros.MacroDef.contract.apply[$tpe](${that.rec(expr)})"

    case q"$mods def $tname[..$tparams](...$paramss): $tpt = $f.apply[$tpe]($expr)" withAttrs (_,t,p)
    if isType[contract.type](f) =>
      val newterm = q"$f.apply[$tpe](${that.rec(expr)})"
      q"$mods def $tname[..$tparams](...$paramss): $tpt = ${that.tearVals(newterm withAttrs ((t,p)), "co$" + tname.decodedName.toString,
        symbolOf[MacroDef.markedContract.type].annotations ++ symbolOf[MacroDef.markedCross.type].annotations)}"

    /*
    case q"$mods val $tname: $tpt = $f.apply[$tpe]($expr)" withAttrs (_,t,p)
    if isType[contract.type](f) =>
      val newterm = q"$f.apply[$tpe](${that.rec(expr)})"
      q"$mods val $tname: $tpt = ${that.tearVals(newterm withAttrs (t,p), "co$" + tname.decodedName.toString,
        symbolOf[MacroDef.markedContract.type].annotations ++ symbolOf[MacroDef.markedCross.type].annotations)}"
     */

    case q"$f.apply[$tpe]($expr)" withAttrs (_,t,p)
    if isType[contract.type](f) =>
      val newterm = q"$f.apply[$tpe](${that.rec(expr)})"
      that.tearVals(newterm withAttrs ((t,p)), "co$",
        symbolOf[MacroDef.markedContract.type].annotations ++ symbolOf[MacroDef.markedCross.type].annotations)

    //case q"$mods def $tname[..$tparams](...$paramss): $tpt = $f.apply[$tpe]($expr)" withAttrs (_,t,p)
    //  if isType[MacroDef.client.type](f) =>
    //  val newterm = q"$root.macros.MacroDef.client.apply[$tpe](${that.rec(expr)})"
    //  q"$mods def $tname[..$tparams](...$paramss): $tpt = ${that.tearVal(newterm withAttrs (t,p), "client$" + tname.decodedName.toString, List(clientAnnotation))}"

    //case q"$f.apply[$tpe]($expr)" withAttrs (_,t,p)
    //  if isType[MacroDef.client.type](f) =>
    //  val newterm = q"$root.macros.MacroDef.client.apply[$tpe](${that.rec(expr)})"
    //  that.tearVal(newterm withAttrs (t, p), "client$", List(clientAnnotation))

    //case q"$f.apply[$tpe]($i, $expr, $name)" withAttrs (_,t,p)
    //if isType[MacroDef.clientN.type](f) =>
    //  val newterm = q"$root.macros.MacroDef.clientN.apply[$tpe]($i, ${that.rec(expr)})"
    //  that.tearVal(newterm withAttrs (t, p), "client$" + name, List(clientAnnotation), false)

    //case tree@q"$bang.apply[..$ts](${ q"$f.apply[$tpe]($name, $i, $expr)" withAttrs (_,t,p) })"
    //if isType[MacroDef.clientN.type](f)
    //&& isType[BangNotation.↓.type](bang) =>
    //  val Literal(Constant(nameStr: String)) = name
    //  val newterm = q"""$root.macros.MacroDef.clientN.apply[$tpe]($name, $i, ${that.rec(expr)})"""
    //  q"$bang.apply2[..$ts](${that.tearVals(newterm withAttrs (t, p), "pre" + nameStr, symbolOf[MacroDef.markedClient.type].annotations, false)}, $name)"

    case tree@q"$f.apply[$tpe]($name, $expr)" withAttrs (_,t,p)
    if isType[raceClient.type](f) =>
      val Literal(Constant(nameStr: String)) = name
      val newterm = q"""$f.apply[$tpe]($name, ${that.rec(expr)})"""
      that.tearVals(newterm withAttrs (t,p), "cl$" + nameStr,
        symbolOf[MacroDef.markedClient.type].annotations ++ symbolOf[MacroDef.markedCross.type].annotations, false)

  })

  val foreach: Phase => Phase = ph => {
    case MyModuleDef(mods, name, parents, self, vparams, ctor, rest) withAttrs (s,t,p) =>
      ModuleDef(mods, name, Template(parents, self, vparams.map(ph) ++ List(ctor) ++ rest.map(ph))).withAttrs(s,t,p)
  }

  val myliftLambda: Phase = liftLambda("contract",
    symbolOf[MacroDef.markedContract.type].annotations ++ symbolOf[MacroDef.markedClient.type].annotations,
    symbolOf[MacroDef.markedContract.type].annotations ++ symbolOf[MacroDef.markedCross.type].annotations,
  )

  def annotationJvm(annottees: Expr[Any]*): Expr[Unit] = compilerPhase(annottees,
    abiCodec **
    foreach(clientToRaceClientWithAccessGuard) **
    foreach(liftTasks) **
    (foreach(selectiveCPS) ** foreach(whiletoif)) **
    foreach(transformContractCode(myliftLambda))
  )

  // compiler pipeline consists of the following stages
  def annotationEvm(annottees: Expr[Any]*): Expr[Unit] = compilerPhase(annottees,
    abiCodec **                                            // resolve encode decode calls
    foreach(clientToRaceClientWithAccessGuard) **          // desugar client expressions
    foreach(liftTasks) **                                  // lift raceClient expressions
    (foreach(selectiveCPS) ** foreach(whiletoif)) **       // no support for deliminated control -> selectiveCPS
    foreach(transformContractCode(myliftLambda)) **        // lambdas cannot be in nested exprs -> lift to toplevel
    foreach(defunct) **                                    // cannot serialize closures -> defunctionalize
    foreach(transformContractCode(liftDynArrays)) **       // dynamic arrays be in nested exprs -> lift to variable
    foreach(transformContractCode(liftBlocks)) **          // blocks cannot be in nested exprs -> flatten
    toSolidity
  )

}
