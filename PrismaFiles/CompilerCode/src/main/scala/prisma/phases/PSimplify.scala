package prisma.phases

import prisma.Prisma.contract
import prisma.meta.MacroDef.ClosureString
import prisma.meta.{BangNotation, Phases, Util}

trait PSimplify extends Phases with Util with PTranslateExpr {
  import c.universe._
  import internal.decorators._

  val selectiveCPS: Phase = (tree: Tree) => presTransform(tree)(that => {
    case term@q"$ctor.apply[..$ts]($x)"
    if ctor.tpe.baseClasses.contains(symbolOf[BangNotation.DSL]) =>
      macroInside(term)

    case term@ValDef(mod, name, tpt, expr) withAttrs (s,t,p)
    if isContractSym(s) && expr.exists {
      case t@q"$f.apply[..$ts]($x)" if isType[BangNotation.↓.type](f) => true
      case _ => false
    } =>
      val q"$typedctor($_)" = retermcheck(q"$termContract.apply[$tpt](???)")
      val expr2 = macroInside(q"$typedctor($expr)")
      ValDef(mod, name, tpt, q"$expr2; ()").withAttrs((s,t,p))
  })

  def macroInside(tree: Tree): Tree = {
    val downArrowTerm = termSymbolOf[BangNotation.↓.type]  //retermcheck(q"_root_.macros.BangNotation.↓")
    val q"$ctorapply[$_]($_)" = tree
    val q"$ctor.apply[$_]($_)" = tree

    //println("macroinside", ctor, ctorapply)
    val transformer = new ContinuationTransformer {
      override val subtransform: PartialFunction[Tree, (Symbol, (Tree => Tree)) => Tree] = {
        case t@q"$f.apply[..$ts]($x)"
        if isType[BangNotation.↓.type](f) => { (symbol, kontinue) =>
          val (s,id) =
            if (symbol == null) freshSymbol("tmp", t.tpe, t.pos)
            else (symbol, Ident(symbol.name) setType symbol.info setSymbol symbol)
          q"$ctor.flatMap($x, { (${internal.valDef(s)}) => ${kontinue(id)}})"
        }

        //case t@q"$f.apply2[..$ts]($x, $n)"
        //if isType[BangNotation.↓.type](f) => { (symbol, kontinue) =>
        //  val (s,id) =
        //    if (symbol == null) freshSymbol("tmp", t.tpe, t.pos)
        //    else (symbol, Ident(symbol.name) setType symbol.info setSymbol symbol)
        //  val Literal(Constant(ns: String)) = n
        //  q"$ctor.flatMap($x, { (${internal.valDef(s)}) => $ns; ${kontinue(id)}})"
        //}
      }

      /** X -> M[X] */ override def wrapit(t: Tree): Tree = q"$ctorapply($t)"
      /** M[X] -> X */ override def unwrap(t: Tree): Tree = q"$downArrowTerm.apply($t)"
      def contractSym: ModuleSymbol = ctor.symbol.asModule
      def contractTerm: Tree = ctor

      //treeCopy.Apply(treeCopy.TypeApply(tree, tree.asInstanceOf[Apply].fun.asInstanceOf[TypeApply].fun, List(TypeTree(t.tpe))), tree.asInstanceOf[Apply].fun, List(t))
    }

    val res = transformer.unwrappingTransformation(tree)
    //println("before = " + tree)
    //println("after  = " + res)
    res
  }

  def replaceLastFlatMapWith(tree: Tree, callt: Tree): Tree = tree match {
    case q"..$as; $f.flatMap[..$ts]($b, ($c) => $inside)" if isType[contract.type](f) =>
      q"..$as; $termContract.flatMap($b, ($c) => ${ replaceLastFlatMapWith(inside, callt) })" //setType callt.tpe
    case q"..$as; $f.apply[..$ts]({$inside; ()})" if isType[contract.type](f) =>
      q"..$as; $callt" //setType callt.tpe
    case q"..$as; $f.apply[..$ts]($inside)" if isType[contract.type](f) && inside.tpe =:= typeOf[Unit] =>
      q"..$as; $callt" //setType callt.tpe
  }

  val whiletoif: Phase = (tree: Tree) => presTransformPlusRest(tree) (that => {
    //   like:
    // flatMap(While(() => cond)(() => body), (zz) => cont)
    // --> def f() = { if (x) ${y +++ f) else $z }; f()
    case tt@q"$f.flatMap[$_, $_]($g.WHILE_[..$_](() => $cond)(() => $body), ($_) => { ..$cont })"
    if isType[contract.type](f) && isType[contract.type](g) =>
      val (sym, ident) = freshSymbol("private$whil", tt)
      internal.setAnnotations(sym, symbolOf[prisma.meta.MacroDef.markedContract.type].annotations.head, symbolOf[prisma.meta.MacroDef.markedTop.type].annotations.head)
      val (deft, callt) = tearVal(that.transform(q"..${cont.drop(1)}"), sym)
      val yy = replaceLastFlatMapWith(body, q"$callt")

      val q"$f.apply[$boolean]($u)" = cond
      assert(isType[contract.type](f))
      val q"def $_(..$args): $t = $body2" withAttrs (s,ttt,p) = deft
      that.appendOwner(q"""
        def ${sym.name.toTermName}(..$args): $t = {
          if ($u) $yy
          else    $body2
        }
      """ withAttrs ((s,ttt,p)))
      callt

    case tt@q"$f.flatMap[$_, $_]($g.IF_[$_]($cond)(() => $body)(() => $alt), $cont)"
    if isType[contract.type](f) && isType[contract.type](g) =>
      val (sym, ident) = freshSymbol("private$iff", tt)
      internal.setAnnotations(sym, symbolOf[prisma.meta.MacroDef.markedContract.type].annotations.head, symbolOf[prisma.meta.MacroDef.markedTop.type].annotations.head)
      val q"($_) => { ..$i }" = cont
      val (deft, callt) = tearVal(that.transform(q"..${i.drop(1)}"), sym)
      val body2 = replaceLastFlatMapWith(body, callt)
      val alt2  = replaceLastFlatMapWith(alt, callt)
      that.appendOwner(deft)
      q"if ($cond) $body2 else $alt2"
  })

  // closure conversion + defunctionalization //  "exponential elimination" ?
  val liftLambda: (String, List[Annotation], List[Annotation]) => Phase = (prefix, typeAnnots, termAnnots) => trees => presTransformPlusRest(trees) (that => {
    // q"(args => body)"      -->       class tmpclass(free-variables-of(body)) def tmpfunc(args) = body;
    //                                  (tmpfunc, new tmpclass(free-variables-of(body)))
    case t@q"((..$xs) => {${Literal(Constant(name: String))}; ..$body})" =>
      that.tearFuns(q"((..$xs) => ${that.rec(q"..$body" setType body.last.tpe)})", name, typeAnnots, termAnnots, freshen=false)
  })

  // C((x:t)=>f(x),y) -> ClosureString('f(t):typeof(f(x))', y:AnyRef)            where C: Closure
  val defunct: Phase = (tree: Tree) => {
    val myphase: Phase = (tree: Tree) => presTransform(tree) (that => {
      //case t@q"$_.flatMap[$_, $_]($_, $closure.apply[..$_]($x, $y))"
      case t@q"$closure.apply[..$_]({(..$args1) => $f(..$args2)}, $y)"
      if isType[prisma.meta.MacroDef.Closure.type](closure) =>
        //println(securityIdToFuncname, f.symbol.name.toString)
        val str = "0x" + securityIdToFuncname.map { case(k,v) => (v,k) }.apply(f.symbol.name.toString)
        q"${termSymbolOf[ClosureString.type]}.apply($termToUint(${Literal(Constant(str))}).u32, $y.asInstanceOf[_root_.scala.AnyRef])"
    })
    transformContractCode(myphase)(tree)
  }

}
