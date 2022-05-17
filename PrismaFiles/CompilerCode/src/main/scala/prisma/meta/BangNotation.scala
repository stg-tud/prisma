package prisma.meta

import prisma.phases.PSimplify

import scala.annotation.{StaticAnnotation, compileTimeOnly}
import scala.language.experimental.macros
import scala.reflect.api.Trees
import scala.reflect.macros.blackbox

object BangNotation {
  @compileTimeOnly("enable macro paradise to expand macro annotations")
  final class BangNotation extends StaticAnnotation {
    def macroTransform(annottees: Any*): Unit = macro BangNotationImpl.macroOutside
  }

  @compileTimeOnly("enable macro paradise to expand macro annotations")
  final class BangNotation2 extends StaticAnnotation {
    def macroTransform(annottees: Any*): Unit =
      macro BangNotation2Impl.macroOutside
  }

  /** extend this to create a DSL */
  trait DSL {
    type M[X]
    type N[X]
    def make[X](x: X): M[X]
    def flatMap[X,Y](x: N[X], f: X => M[Y]): M[Y]
    def apply[X](x: X): M[X] = make(x)
    def <*>[A,B](f: N[A=>B], x: N[A]): M[B] = flatMap(f, (ff: A=>B) => flatMap(x, (xx: A) => apply(ff(xx))))
  }
  trait IF extends DSL {
    def IF[A](x: Boolean)(y: () => A)(z: () => A): A = if (x) y() else z()
    def IF_[A](x: Boolean)(y: () => M[A])(z: () => M[A]): N[A]
  }
  trait WHILE extends DSL {
    def WHILE(x: () => Boolean)(y: () => Unit): Unit = while (x()) y()
    def WHILE_(x: () => M[Boolean])(y: () => M[Unit]): N[Unit]
  }
  trait TRY extends DSL {
    def TRY[A](x: () => A)(pf: PartialFunction[Throwable, A])(fin: () => Unit): A =
      try x() catch { case e: Throwable if pf.isDefinedAt(e) => pf(e) } finally fin()
    def TRY_[A](x: () => M[A])(pf: PartialFunction[Throwable, M[A]])(fin: () => M[Unit]): M[A]
  }
  object ↓ {
    def apply[X, M[_]](x: M[X]): X = theError
    //def apply2[X, M[_]](x: M[X], name: String): X = theError
  }

  private def theError: Nothing = sys.error(
    "'↓' can only be used inside DSL { ... } brackets,\n" +
    "which must be inside an annotated object like '@BangNotation object Blabla { ... }'")

  @compileTimeOnly("enable macro paradise to expand macro annotations")
  final def printIt[X](expr: X): X = macro BangNotationImpl.printItMacro[X]
}

class BangNotationImpl(context: blackbox.Context)
  extends MacroTop with Util with PSimplify
{
  override val c: blackbox.Context = context
  import c.universe._

  def macroOutside(annottees: c.Expr[Any]*): c.Expr[Unit] = {
    val expr2 = typecheck(q"..$annottees")
    val expr3 = expr2 //presTransform(expr2) (that => {
    //  case tree@q"$ctor.apply[..$ts]($x)"
    //  if ctor.tpe.baseClasses.map(_.fullName).contains("macros.BangNotation.DSL") =>
    //    eliminateControlFlow(tree)
    //})
    val expr4 = expr3 //retermcheck(expr3)
    val expr5 = selectiveCPS(expr4)
    //val empty: Symbol = weakTypeOf[F].typeSymbol.companion.typeSignature.member(TermName("empty"))
    //val result = q"$empty[${weakTypeOf[X].typeSymbol}]"
    c.Expr[Unit](untypecheck(expr5))
  }

  def printItMacro[X](expr: Expr[X]): Expr[X] = {
    println(expr)
    expr
  }
}

class BangNotation2Impl(context: blackbox.Context)
  extends MacroTop with Util with PSimplify
{
  override val c: blackbox.Context = context

  import c.universe._
  import internal.decorators._
  import internal._

  def macroOutside(annottees: c.Expr[Any]*): c.Expr[Unit] = {
    val expr2 = typecheck(q"..$annottees")
    val expr3 = presTransform(expr2) (that => {
      case tree@q"$ctor.apply[..$ts]($x)"
      if ctor.tpe.baseClasses.map(_.fullName).contains("prisma.meta.BangNotation.DSL") =>
        bangMacro(tree)
    })
    c.Expr[Unit](untypecheck(expr3))
  }

  private def bangMacro(tree: Tree): Tree = {
    //val dtorTerm = termSymbolOf[BangNotation.↓.type]
    val q"$ctorTerm.apply[$_]($arg)" = tree
    val q"$ctorApply[$_]($_)" = tree
    val flatMapSymbol = ctorTerm.symbol.info.member(TermName("flatMap"))

    def IF_(): Tree = {
      val IF_ = ctorTerm.symbol.info.member(TermName("IF_"))
      q"$ctorTerm.$IF_".withAttr(IF_.symbol, IF_.info)
    }
    def WHILE_(): Tree = {
      val WHILE_ = ctorTerm.symbol.info.member(TermName("WHILE_"))
      q"$ctorTerm.$WHILE_".withAttr(WHILE_.symbol,WHILE_.info)
    }

    def subst(body: Tree, paramSymbol: Symbol, arg: Tree): Tree = nopresTransform(body)(that => {
      case q"$f.apply(${param2: Ident})" if param2.symbol == paramSymbol =>
        call(f, arg)
      case q"$f(${param2: Ident})" if param2.symbol == paramSymbol =>
        call(f, arg)
      case q"${param2: Ident}.apply($x)" if param2.symbol == paramSymbol =>
        call(arg, x)
      case q"${param2: Ident}($x)" if param2.symbol == paramSymbol =>
        call(arg, x)
      case it: Ident if it.symbol == paramSymbol =>
        arg
    })

    def count(body: Tree, paramSymbol: Symbol): Int = {
      var ctr = 0
      nopresTransform(body)(that => {case it: Ident if it.symbol == paramSymbol => ctr += 1; it })
      ctr
    }

    def call(f: Tree, x: Tree): Tree = f /*PotBlock.fromList(filterUnit(flat(f), q"???"))*/ match {
      case q"(($param) => $body)" =>
        // optim: dont duplicate code TODO consider using valdef always
        val ctr = count(body, param.symbol)
        if (ctr <= 1) subst(body, param.symbol, x)
        else q"${internal.valDef(param.symbol, x)}; $body"
      case f =>
        q"$f($x)"
    }

    def pure(x: Tree): Tree =
      retermcheck(q"$ctorApply[${nonConstantType(x.tpe)}]($x)")

    def join(x: Tree): Tree = {
      val (sym,id) = freshSymbol("it", x.tpe.typeArgs.head, x.pos)
      fmap(x, q"((${internal.valDef(sym)}) => $id)")
    }

    def fmap(x: Tree, f: Tree): Tree = retermcheck(x match {
      // left pure:  pure a >>= f     =     f a
      case PotBlock(bs, q"$ctor($arg)") if ctor.symbol == ctorApply.symbol =>
        PotBlock(bs, call(f, arg))
      // assoc:  (a >>= b) >>= c      =     a >>= (\x. b x >>= c)
      case PotBlock(bs, q"$ctor($x2, $f2)") if ctor.symbol == flatMapSymbol =>
        val (sym, id) = freshSymbol("tmp", x2.tpe.typeArgs.head, x2.pos)
        PotBlock(bs, q"$ctorTerm.flatMap($x2, ((${internal.valDef(sym)}) => ${fmap(call(f2, id), f)}))")
      case x =>
        q"$ctorTerm.flatMap($x, $f)"
    })

    def appl(m1: Tree, m2: Tree): Tree = {
      //  m1 match {
      //      case q"$ctor($m1)" if hasSym(ctor, ctorApply.symbol) =>
      //        map(m1)
      //      case m1 => q"$m1 <*> $m2"
      //}
      // println("<*>: " + m1 + " <*> " + m2)
      val (s1, s1v) = freshSymbol("tmp", m1.tpe.typeArgs.head, m1.pos)
      val (s2, s2v) = freshSymbol("tmp", m2.tpe.typeArgs.head, m2.pos)
      fmap(m1, q"((${internal.valDef(s1)}) => ${
        fmap(m2, q"((${internal.valDef(s2)}) => ${
          pure(call(s1v, s2v)) })") })")
    }

    // appl m1 m2 = liftM2 ($) f x = do { x1 <- m1; x2 <- m2; return (x1 x2) }
    //           = m1 >>= (\x1. m2 >>= (\x2. return x1 x2))
    //           = fmap m1 (\x1. fmap m2 (\x2. return (x1 x2)))

    def curryAndEtaExpand(e: Tree): Tree = if (e.symbol == null) e else {
      val func = if (e.symbol.isMethod && e.symbol.asMethod.paramLists.nonEmpty) {
        retermcheck(q"$e _") match {
          // remove synthetic eta$_$_ variables (why are they even created, and only sometimes?)
          case Block(List(vd@ValDef(mods, name, tpt, rhs)), t) => subst(t, vd.symbol, rhs)
          case t => t
        }
      } else e
      val arglen = if (e.symbol.isMethod && e.symbol.asMethod.paramLists.nonEmpty) e.symbol.asMethod.paramLists.head.length else 0 // tpe.typeArgs.length - 1
      retermcheck(
        if (arglen > 1) q"$func.curried"
        else func)
    }

    val res = nopresTransform(arg) (that => {
      // pure v = pure v
      case Stable(e) =>
        pure(curryAndEtaExpand(e))
      case tt@Typed(e,t) =>
        treeCopy.Typed(tt, that.rec(e), t)

      // pure (!l e) = join (pure e)
      case q"$f.apply[..$ts]($x)"
      if isType[BangNotation.↓.type](f) =>
        join(that.rec(x))

      // pure (f e) = (pure f) <> (pure e)
      case q"$f($e1)" =>
        appl(that.rec(f), that.rec(e1))
      case q"$f(...$ess)" if ess.nonEmpty =>
        val zero = that.rec(curryAndEtaExpand(f))
        ess.foldLeft(zero) { case (fun, es) =>
          es.foldLeft(fun) { case (fun, arg) =>
            appl(fun, that.rec(arg)) } }
      case ttt@q"$it.$m" =>
        val (s2,id) = freshSymbol("tmp", it)
        val thing = curryAndEtaExpand(internal.setSymbol(q"$id.$m", ttt.symbol))
        appl(pure(q"((${internal.valDef(s2)}) => $thing)"), that.rec(it))

      // pure (val x=e; f[x]) = pure (f[e])       = (pure e) `fmap` (pure . f)
      // pure (e; f)          = pure ((\(). f) e) = (pure e) `fmap` (\(). pure f)
      // pure e               = pure e
      case Block((d@ValDef(a, b, c, rhs))::bs, e) =>
        fmap(that.rec(rhs), q"(${treeCopy.ValDef(d, a, b, c, EmptyTree)}) => ${that.rec(Block(bs, e))}")
      case Block(b::bs, e) =>
        fmap(that.rec(b), q"(_: Unit) => ${that.rec(Block(bs, e))}")
      case Block(Nil, e) =>
        that.rec(e)
      case t@Assign(lhs, rhs) =>
        val (sym,id) = freshSymbol("tmp", rhs)
        val lll = q"(${internal.valDef(sym)}) => { ${treeCopy.Assign(t, lhs, id)}; ${pure(UNIT)} }"
        fmap(that.rec(rhs), lll)

      // pure (if x f g) = if' (pure x) (pure . f) (pure . g)
      case If(cond, thenf, elsef) =>
        val mcond = that.rec(cond)
        val mthenf = that.rec(thenf)
        val melsef = that.rec(elsef)
        val (sym,id) = freshSymbol("tmp", mcond.tpe.typeArgs.head, mcond.pos)
        fmap(mcond, q"((${internal.valDef(sym)}) => $IF_($id)(() => $mthenf)(() => $melsef))")
      case While(_, cond, body) =>
        val mcond = q"() => ${that.rec(cond)}" setType internal.typeRef(internal.thisType(definitions.ScalaPackageClass), symbolOf[scala.Function0[_]], List(cond.tpe))
        val mbody = q"() => ${that.rec(q"..$body")}" setType internal.typeRef(internal.thisType(definitions.ScalaPackageClass), symbolOf[scala.Function0[_]], List(body.last.tpe))
        q"$WHILE_($mcond)($mbody)"
      //case q"try $z catch $pf finally $f" withAttrs (_,t,p) => // TODO this does not catch anything
      //  transform(q"TRY(() => $z)($pf)(() => $f)" withAttrs (t,p))

      case t =>
        c.abort(t.pos, "\nNYI " ++ show(t) ++ "\nNYI " ++ showRaw(t))
    })

//    println("before = " + fix(tree.toString()))
//    println("after  = " + fix(res.toString()))
    res
  }

}