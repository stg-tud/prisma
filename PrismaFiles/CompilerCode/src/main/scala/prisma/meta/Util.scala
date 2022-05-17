package prisma.meta

import prisma.Prisma.Arr
import prisma.meta.MacroDef.Closure

import scala.collection.mutable

trait Util extends Patterns {
  import c.universe._
  import internal.decorators._

  lazy val UNIT = retermcheck(q"()")

  // eta-expanding functions with constanttype returntype seems to inline them,
  //   so we hide constanttypes, or else the next stage would mess up
  // variances get broken
  def nonConstantType(tpe: Type): Type = tpe match {
    case x: ConstantType => x.value.tpe
    case x => x
  }

  private val containsCache: mutable.Map[PartialFunction[Tree, _], mutable.Map[Tree, Boolean]] = mutable.WeakHashMap()
  private def containsUnbound(f: PartialFunction[Tree, _])(t: Tree): Boolean = containsCache
    .getOrElseUpdate(f, mutable.WeakHashMap())
    .getOrElseUpdate(t,
      t match {
        case _ if f.isDefinedAt(t)                                   => true
        //case q"$f.apply[$ts]($xss)" if isType[BangNotation.lift.type](f) => false // TODO
        case _                                                       => t.children.exists(containsUnbound(f))
      })

  def tearVal(body: Tree, funcname: Symbol): (Tree, Tree) = {
    //def tpeName(x: Type) = if (x.typeSymbol.isModuleClass) tq"$x.type" else q"$x"
    val free = frees(body).filter(s => !(rooted(s) || isTopSym(s))).toList
    //frees(body).map(s => (s, rooted(s) || isTopSym(s), ownerList(s).map(s => (s, s.isTerm, s.isModuleClass, s.isPackage)))).foreach(println)
    //println("")

    //val misser = (refs(body) -- dots(body) ++ thises(body)).collect {
    //  case x if x.symbol == NoSymbol => (showRaw(x), x.symbol) }
    //if (misser.nonEmpty) assert(false, "missing? " + misser + "\n" + body)
    val theMods = (free.toSet & mods(body)).toList
    if (theMods.nonEmpty) c.abort(body.pos,
      "cannot tear expression, because it contains an assigment. did you eliminateVariables before this?")

    //free.filter(_.isMethod).foreach{ x => body collect {
    //  case y: RefTree if y.symbol == x =>
    //    c.abort(y.pos, "cannot yet lift methods, please use lambda instead") } }
    // val his = body.collect { case t: This if !t.tpe.typeSymbol.isModuleClass => t.tpe }.distinct
    // his.map { x => q"val ${TermName(x.typeSymbol.name.toString.toLowerCase)}: $x" } ++
    // his.map(x=>This(x.typeSymbol)) ++
    // presTransform(body) { case (t: This, _) if his.contains(t.tpe) => Ident(TermName(t.tpe.typeSymbol.name.toString.toLowerCase)) }

    val tr = mutable.Map[Symbol, (Symbol, Ident)]()
    val formalenv = free.flatMap { x =>
      if (x.toString.startsWith("object "))
        None
      else if (x.isClass && !x.fullName.startsWith("that$macro$")) {
        println(x.asType.toType)
        println(internal.thisType(x))
        tr(x) = freshSymbol("that", x.asType.toType, x.pos)
        //println("olala", tr(x)._2.tpe)
        Some(internal.valDef(tr(x)._1))
      } else Some(internal.valDef(x)) }
    //println(body)
    //println(formalenv)
    val concreteenv = free.flatMap { x =>
      if (x.toString.startsWith("object ")) None
      else if (x.isClass) Some(This(x))
      else if (!x.asTerm.isAccessor && x.isMethod) Some(q"(..${x.asMethod.info.typeArgs.map{ x => internal.valDef(x.typeSymbol) }}) => $x()")
      else Some(Ident(x) setSymbol x) }
    //println(concreteenv)
    val newbody = presTransform(body) (that => {
      case x: This if tr.contains(x.symbol) => tr(x.symbol)._2
    })
    val deft = internal.defDef(funcname.symbol, NoMods, List(formalenv), newbody)
    val callt = q"$funcname(..$concreteenv)" setType body.tpe
    val res = (deft, callt)
    //println(res)
    res
  }
  private def tearFun(fun: Tree, classSym: Symbol, funcSym: Symbol): (Tree, Tree, Tree) = {
    val free = frees(fun).filter(s => !(rooted(s) || isTopSym(s))).toList
    val theMods = (free.toSet & mods(fun)).toList
    if (theMods.nonEmpty) c.abort(fun.pos,
      "cannot tear expression, because it contains an assigment")

    //free.filter(_.isMethod).foreach{ x => fun collect {
    //  case y: RefTree if y.symbol == x =>
    //    c.abort(y.pos, "cannot yet lift methods, please use lambda instead") } }
    val formalenv = free.flatMap { x =>
      //if (x.isModule || x.isModuleClass || x.isClass) None
      if (x.isType) None
      else if (x.asTerm.isSetter)
        Some(internal.valDef(x.asTerm.setter))
      else if (!x.asTerm.isAccessor && x.isMethod) {
        assert(false, "error " + x + " inside " + fun)
        Some(internal.valDef(x.asMethod))
      } else
        Some(internal.valDef(x)) }

    val concreteenv = free.flatMap { x =>
      //if (x.isModule || x.isModuleClass || x.isClass) None
      if (x.isType) None
      else if (x.asTerm.isSetter) {
        assert(x.asTerm.setter.info.paramLists.length == 1)
        Some(q"""(..${ x.asTerm.setter.info.paramLists.head.map(x => internal.valDef(x.symbol)) }) =>
             $x(..${ x.asTerm.setter.asTerm.info.paramLists.head.map(x => Ident(x.symbol)) })""")
      } else if (!x.asTerm.isAccessor && x.isMethod)
        Some(q"""(..${ x.asMethod.info.paramLists.head.map(x => internal.valDef(x.symbol)) }) =>
            $x(..${ x.asMethod.info.paramLists.head.map(x => Ident(x.symbol)) })""")
      else
        Some(Ident(x) setSymbol x) }

    val newfun = presTransform(fun) (that => {
      case x@Select(y: This, n) if free.contains(x.symbol) => Ident(n)
    })

    val q"((..$args) => $body)" = newfun
    val unpack_args = if (args.length == 1) {
      val ValDef(mods, name, tpe, body) = args.head
      List(treeCopy.ValDef(args.head, mods, name, tpe, q"args"))
    } else {
      args.zipWithIndex.map { case (x@ValDef(mods, name, tpe, body), i) =>
        treeCopy.ValDef(x, mods, name, tpe, q"args.${ TermName("_" + (i+1).toString) }") }
    }
    val unpack_userdata = formalenv.map { case x@ValDef(mods, name, tpe, body) =>
      treeCopy.ValDef(x, mods, name, tpe, q"userdata.${ x.symbol.name.toTermName }") }
      //treeCopy.ValDef(x, mods, name, tpe, q"(userdata.asInstanceOf[$classname]).${ x.symbol.name.toTermName }") }

    val tpe = nonConstantType(body.tpe)

    // def apply(..$args): ${body.tpe} = $body
    val classt = q"class ${classSym.name.toTypeName}(..$formalenv)" setSymbol classSym
    val callt = q"""${termSymbolOf[Closure.type]}.apply(
                      (${Ident(funcSym.name.toTermName)} _),
                      new $classSym(..$concreteenv) )"""

    val deft = (if (args.isEmpty) {
      // c.internal.defDef(funcSym, List(List(q"userdata: $classSym")),  q"..$unpack_userdata; ..${flat(body)}")
      q"""def ${funcSym.name.toTermName}(userdata: $classSym): $tpe = {
             ..$unpack_userdata
             ..${flat(body)}
         }"""
    } else if (args.length == 1) {
        q"""def ${funcSym.name.toTermName}(${args.head}, userdata: $classSym): $tpe = {
             ..$unpack_userdata
             ..${flat(body)}
         }"""
    } else {
        q"""def ${funcSym.name.toTermName}(args: (..${args.map{_.symbol.info}}), userdata: $classSym): ${tpe} = {
             ..$unpack_args
             ..$unpack_userdata
             ..${flat(body)}
         }"""
    }) setSymbol funcSym

    (classt, deft, callt)

    /*q"""val $funcname:
             ((..${args.map{_.symbol.info}}), Any) => ${body.tpe} =
             (args: (..${args.map{_.symbol.info}}), userdata: Any) => {
               ..$unpack_args;
               ..$unpack_userdata;
               $body
           }""",*/
  }

  // NOTE internal.freeTerms(x) does something different: seems to only gather globally(?) undefined terms
  // we need locally undefined terms, e.g. frees(val x = y; x + z) --> Set(y, z)
  // TODO should 'this' be considered free? what about private variables
  def getSym(x: Tree): Symbol = if (x.symbol == NoSymbol) { println(s"$x - ${x.symbol}"); x.symbol } else x.symbol
  def frees(tree: Tree): Set[Symbol] = (refs(tree) -- dots(tree)).map(getSym) -- defs(tree) ++ thises(tree).map(getSym)
  def refs(tree: Tree): Set[RefTree] = tree.collect { case x: RefTree if isNotRoot(x) => x } .toSet
  def dots(tree: Tree): Set[Select] = tree.collect { case x: Select  => x } .toSet
  def defs(tree: Tree): Set[Symbol] = tree.collect { case x: DefTree => x.symbol } .toSet.flatMap { s: Symbol => if (s.isTerm) Set(s, s.asTerm.setter) else Set(s) }
  def thises(tree: Tree): Set[This] = tree.collect { case x: This => x } .toSet
  def thisdots(tree: Tree): Set[Symbol] = tree.collect { case x@Select(y: This, n)  => x.symbol } .toSet
  def isNotRoot(x: Tree): Boolean = x.symbol != rootMirror.RootClass && x.symbol != rootMirror.RootPackage && x.toString != "_root_"
  def mods(tree: Tree): Set[Symbol] = tree.collect { case Assign(lhs, rhs) => lhs.symbol } .toSet
  def rooted(s: Symbol): Boolean =
    if (s == NoSymbol) false
    else if (s == c.mirror.RootClass) true
    else if (s.owner.isTerm && s.owner.asTerm.isStable ||  s.owner.isModuleClass || s.owner.isModule || s.owner.isPackage) rooted(s.owner)
    else false
  //def isStrongType[T: TypeTag](t: Tree): Boolean = t.tpe == typeOf[T]
  def isType[T: WeakTypeTag](t: Tree): Boolean = t.tpe.dealias.typeSymbol == symbolOf[T]
  def ownerList(s: Symbol): List[Symbol] = if (s == NoSymbol) Nil else s :: ownerList(s.owner)

  @inline implicit class TreeUtils(tree: Tree) {
    @inline def +++(others: List[Tree]): Tree = tree match {
      case MyClassDef(mods, name, tparams, parents, self, vparams, ctor, rest) withAttrs (s,t,p) =>
        ClassDef(mods, name, tparams, Template(parents, self, (vparams :+ ctor) ++ rest ++ others)) withAttrs (s,t,p)
      case MyModuleDef(mods, name, parents, self, vparams, ctor, stats) withAttrs (s,t,p) =>
        ModuleDef(mods, name, Template(parents, self, vparams ++ List(ctor) ++ stats ++ others)) withAttrs (s,t,p)
      case tree withAttrs (_,t,p) =>
        val AsBlock(bs, e) = tree
        Block(others ++ bs, e) withAttrs (t,p)
    }
  }

  // define a Tree Transformer as an inline partial function and be done with it.
  def presTransforms(trees: List[Tree])(f: PreservingTransformer => PartialFunction[Tree, Tree]): List[Tree] = {
    val transformer = new PreservingTransformer { override val doPreserveType = true; override val ff = f(this) }; trees.map(transformer.transform) }
  def presTransformsPlusRest(trees: List[Tree])(f: PreservingTransformer => PartialFunction[Tree, Tree]): List[Tree] = {
    val transformer = new PreservingTransformer { override val doPreserveType = true; override val ff = f(this) }; trees.map(transformer.transform) ++ transformer.store }
  def presTransform(tree: Tree)(f: PreservingTransformer => PartialFunction[Tree, Tree]): Tree =
    new PreservingTransformer { override val doPreserveType = true; override val ff = f(this) }.transform(tree)
  def nopresTransform(tree: Tree)(f: PreservingTransformer => PartialFunction[Tree, Tree]): Tree =
    new PreservingTransformer { override val doPreserveType = false; override val ff = f(this) }.transform(tree)
  def presTransformPlusRest(tree: Tree)(f: PreservingTransformer => PartialFunction[Tree, Tree]): Tree = {
    val transformer = new PreservingTransformer { override val doPreserveType = true; override val ff = f(this) }
    transformer.transform(tree) +++ transformer.store.toList
  }

  /* preserves types and positions, see case _ in transform */
  trait PreservingTransformer extends Transformer {
    val ff: PartialFunction[Tree, Tree]
    val doPreserveType: Boolean

    //val store: mutable.Map[Symbol, mutable.Buffer[Tree]] = mutable.WeakHashMap()
    //def appendOwner(tree: Tree): Unit = store(currentSymbol.get).append(tree)

    val store: mutable.Buffer[Tree] = mutable.Buffer()
    def appendOwner(tree: Tree): Unit = store.append(tree)

    /** cutout takes an expression `body` and a function name,
     * and returns an expression defining an function `def`; and an expression to call it `call`,
     * such that body == call, where def is defined previously.
     *
     * ex. it will tell you that x and y are free in println, but z is not.
     * ~~~
     *   val body = c.typecheck(q"def f(): Unit = { val x=1; val y=2; println { val z = 5; x + y + z * 2 }}")
     *   println(cutOut(body.children(1).children(2).children(1), "cutted"))
     *   //  (q"def cutted(y: Int, x: Int) = { val z: Int = 5; x.+(y).+(z.*(2)) }",
     *   //   q"cutted(y, x)")
     * ~~~ */
    def tearVals(tree: Tree, name: String, annots: List[Annotation], freshen: Boolean = true): Tree = {
      val sym = freshSymbol(name, NoType, tree.pos, freshen=freshen)._1
      internal.setAnnotations(sym.symbol, (annots ++ symbolOf[MacroDef.markedTop.type].annotations):_*)
      val (deft, callt) = tearVal(tree, sym)
      appendOwner(deft)
      callt
    }
    def tearFuns(tree: Tree, name: String, typeAnnots: List[Annotation], termAnnots: List[Annotation], freshen: Boolean = true): Tree = {
      //val termName = if (freshen) c.freshName(TermName(name)) else TermName(name)
      val q"(..$x) => $body" = tree
      val termSym = freshSymbol(name, NoType, body.pos, freshen=freshen)._1
      val typeSym = c.internal.newClassSymbol(NoSymbol, TypeName(termSym.name.toString + "t"), tree.pos)
      internal.setInfo(typeSym, NoType)
      internal.setAnnotations(termSym, (termAnnots ++ symbolOf[MacroDef.markedTop.type].annotations):_*)
      internal.setAnnotations(typeSym, (typeAnnots ++ symbolOf[MacroDef.markedTop.type].annotations):_*)

      //println(fdeft, fdeft.symbol, termSym, fdeft.symbol == termSym, termSym.annotations, fdeft.symbol.annotations)
      //println(cdeft, cdeft.symbol, typeSym, cdeft.symbol == typeSym, typeSym.annotations, cdeft.symbol.annotations)
      //println()

      val (cdeft, fdeft, callt) = tearFun(tree, typeSym, termSym)
      appendOwner(cdeft)
      appendOwner(fdeft)
      callt
    }

    override def transform(tree: Tree): Tree = tree match {
      case _ withAttrs (_,t,p) if ff.isDefinedAt(tree) =>
        val res = ff.apply(tree)
        if (doPreserveType) res withAttrs (t,p) else res
      case tree@Block(b, e) => // flatten nested blocks { a { b c } d e { f } } -> { a b c d e f }
        val bb = flatten((b :+ e).map(transform))
        treeCopy.Block(tree, bb.init, bb.last)
      case _ => superRecurse(tree)
    }

    def superRecurse(tree: Tree): Tree = super.transform(tree)
    def rec(tree: Tree): Tree = transform(tree)
  }

  val liftDynArrays: Tree => Tree = (tree: Tree) => presTransform(tree) (that => {
    case q"$f.apply[$t](..$xs)" if isType[Arr.type](f) =>
      val tmp = TermName(c.freshName("tmp"))
      val init = xs.zipWithIndex.map { case (x,i) =>
        q"$tmp(${Literal(Constant(i.toString))}.u) = ${that.rec(x)}" }
      q"val $tmp = $termArrOfDim[$t](${Literal(Constant(xs.length.toString))}.u); ..$init; $tmp"
  })

  // (block) normalisation
  val liftBlocks: Tree => Tree = (tree: Tree) => {
    val transformer = new ContinuationTransformer {
      override def neverShift: Boolean = true
      override val subtransform: PartialFunction[Tree, (Symbol, (Tree => Tree)) => Tree] = {
        //case ttt@If(a,b,c) => { tail => tail(ttt) } // TODO good idea?
        case ttt@Block(bs, e) => { (sym, tail) =>
          val (s,id) =
            if (sym == null) freshSymbol("x", e.tpe, e.pos)
            else (sym, Ident(sym.name) setSymbol sym setType sym.info)
          q"..$bs; ${internal.valDef(s, e)}; ${tail(id)}"
        }
      }
      /** X -> M[X] */ override def wrapit(t: Tree): Tree = t
      /** M[X] -> X */ override def unwrap(t: Tree): Tree = t
      override def contractSym: ModuleSymbol = ???
      override def contractTerm: Tree = ???
    }

    val x = presTransform(tree)(that => {
      case q"(..$xs) => $body" withAttrs (_,t,p) =>
        q"(..$xs) => ${transformer.unwrappingTransformation(that.rec(body))}" withAttrs (t,p)
      case vd@ValDef(mods, name, tpe, rhs) if vd.symbol.isMethod && vd.symbol.asMethod.isAccessor =>
        treeCopy.ValDef(vd, mods, name, tpe, transformer.unwrappingTransformation(that.rec(rhs)))
      case dd@DefDef(mods, name, tparams, vparams, tpe, rhs) =>
        treeCopy.DefDef(dd, mods, name, tparams, vparams, tpe, transformer.unwrappingTransformation(that.rec(rhs)))
    })
    x
  }

  /** instances need to implement
   *  - subtransform
   *  - wrapit :: X -> M[X]
   *  - unwrap :: M[X] -> X
   *
   *  then you can use
   *  - unwrappingTransformation :: M[X] -> M[X]
   *
   *  which will remove all (undefined) unwraps inside the expression. */
  trait ContinuationTransformer extends PreservingTransformer {
    override val doPreserveType = true
    def neverShift = false

    val subtransform: PartialFunction[Tree, (Symbol, (Tree => Tree)) => Tree]
    /** X -> M[X] */ def wrapit(t: Tree): Tree
    /** M[X] -> X */ def unwrap(t: Tree): Tree
    def contractSym: ModuleSymbol
    def contractTerm: Tree

    private def IF = {
      val IF = contractSym.info.member(TermName("IF"))
      q"$contractTerm.$IF".withAttr(IF.symbol, IF.info)
    }
    private def WHILE = {
      val WHILE = contractSym.info.member(TermName("WHILE"))
      q"$contractTerm.$WHILE".withAttr(WHILE.symbol,WHILE.info)
    }

    private val predicate = containsUnbound(subtransform) _
    override def transform(tree: Tree): Tree = {
      val result: Tree = tree match {
        case _ if !predicate(tree) => tree

        // special case for scala && ||, as they lie about their laziness!
        case q"$x && $y" withAttrs (_,t,p) if !neverShift =>
          transform(q"if ($x) $y else ${retermcheck(q"false")}" withAttrs (t,p))
        case q"$x || $y" withAttrs (_,t,p) if !neverShift =>
          transform(q"if ($x) ${retermcheck(q"true")} else $y" withAttrs (t,p))

        // reduce control flow forms to function application
        case q"if ($z) $x else $y" withAttrs (_,t,p) if !neverShift =>
          transform(q"$IF($z)(() => $x)(() => $y)" withAttrs (t,p))
        case While(labelname, z, xs) withAttrs (_,t,p) if !neverShift =>
          val zz = q"() => $z" setType internal.typeRef(internal.thisType(definitions.ScalaPackageClass), symbolOf[scala.Function0[_]], List(z.tpe))
          val xx = q"{() => ..$xs}" setType internal.typeRef(internal.thisType(definitions.ScalaPackageClass), symbolOf[scala.Function0[_]], List(xs.last.tpe))
          transform(q"$WHILE($zz)($xx)" withAttrs (t,p))
        //case q"try $z catch $pf finally $f" withAttrs (_,t,p) => // TODO this does not catch anything
        //  transform(q"TRY(() => $z)($pf)(() => $f)" withAttrs (t,p))

        case Block(b, e) withAttrs (_,t,p) => // semicolon associativity
          // flatten nested blocks { a { b c } d e { f } } -> { a b c d e f } // TODO flattening two blocks with same variable (shadowing) okay?
          val flattened = filterUnit((b:+e).flatMap(x => flat(transform(x))), UNIT)
          q"..$flattened" withAttrs (t,p)

        // TODO handle paramaccessor and accessor differently, to avoid defining new fields?
        // ValDef with x==EmptyTree: function argument declaration
        case ValDef(mods, n, tt, x) if x != EmptyTree && !tree.symbol.asTerm.isAccessor =>
          val AsBlock(xb, xe) = transform(x)
          q"..$xb; ${ treeCopy.ValDef(tree, mods, n, tt, xe) }"
        case Assign(x, y) =>
          val AsBlock(xb, xe) = transform(x)
          val AsBlock(yb, ye) = transform(y)
          q"..$xb; ..$yb; ${ treeCopy.Assign(tree, xe, ye) }"

        case q"$x.$n" withAttrs (s,t,p) =>
          val (xb, xe) = asBlockVar(transform(x), predicate=predicate)
          q"..$xb; ${q"$xe.$n" withAttrs (s,t,p)}"

        // TODO merge with function call below?
        case q"new $f[..$ts](...$xss)" withAttrs (s,t,p) =>
          val (fb, fe) = asBlockVar(transform(f), predicate=predicate)
          if (xss.exists(_.exists(predicate))) {
            val byNamess = f.symbol.asClass.primaryConstructor.info.paramLists.map(_.map(_.asTerm.isByNameParam).toList).toList
            val (ebs, ees) = mkCall(xss, byNamess)
            q"..$fb; ..${ebs.flatten.flatten}; ${q"new $fe[..$ts](...$ees)" withAttrs (s,t,p) }"
          } else
            q"..$fb; ${q"new $fe[..$ts](...$xss)" withAttrs (s,t,p)}"

        case q"$f[..$ts](...$xss)" withAttrs(s,t,p) if ts.nonEmpty || xss.nonEmpty =>
          val (fb, fe) = asBlockVar(transform(f), predicate=predicate)
          if (!xss.exists(_.exists(predicate))) {
            // only f was predicate, so its not necessary to touch the arguments.
            q"..$fb; ${q"$fe[..$ts](...$xss)" withAttrs (s,t,p)}"
          } else {
            // if any of the arguments contains an transformable,
            // we need to normalise all arguments,
            // else the evaluation order would not be preserved.

            // shifting is necessary,
            // if there is any transformable thing inside of a byname or lambda argument.
//            println(tree +" -- "+ f)
//            println(showRaw(f, printTypes=true))
//            println(f.tpe)
//            println(f.tpe.paramLists)
//            println(f.tpe.paramLists.map(_.map(_.info)))
//            assert(!f.tpe.paramLists.map(_.map(_.info)).contains("*"), "cannot handle var-arg functions")
            val byNamess = f.tpe.paramLists.map(_.map(_.asTerm.isByNameParam).toList).toList
//            println(xss, byNamess)
            val shiftNecessaryss = zipMapMap[Tree, Boolean, Boolean](xss, byNamess) {
              case (e, true) => predicate(e)
              case (Function(xs, body), false) => predicate(body)
              case _ => false
            }
            val shiftNecessary = shiftNecessaryss.exists(x => x.exists(y => y))

            //if (shiftNecessary) println("shift necessary " + tree + " " + shiftNecessary)

            //val ft = try { val q"$ft" = fe; ft } catch { case _: MatchError => null }
            //if (ft == null) {
            //  assert(!shiftNecessary)
            //  mkCall(tree, fb, fe, ts, ess, byNamess)
            //} else {
            val q"$ft.$fn" = f
            //val q"$ftt.$fnn" = ft
            val fn2 = TermName(s"${fn}_")
            val f2 = ft.tpe.decl(fn2)
            val originalTps = f.tpe.paramLists.map(_.map(_.info))
            val shiftedTps = f2.info.paramLists.map(_.map(_.info))
            val shiftPossible = originalTps.length == shiftedTps.length

            val res = if (!shiftNecessary || neverShift) {
              val (ebs, ees) = mkCall(xss, byNamess)
              q"..$fb; ..${ebs.flatten.flatten}; ${q"$fe[..$ts](...$ees)" withAttrs (s,t,p) }"
            } else if (!shiftPossible) c.abort(fe.pos, s"""
              |the following function does not have a shift version (name + underscore) in scope
              |(did you define it? did you import it?)
              |
              |$ft.$fn(${shiftNecessaryss.map(_.mkString(",")).mkString(")(")}) : $originalTps
              |$ft.$fn2(${shiftNecessaryss.map(_.mkString(",")).mkString(")(")}) : $shiftedTps
              |${ft.tpe.decls}
              |""".stripMargin('|'))
              // ${ftt.tpe.decl(TermName(fnn + "_")).symbol.info.decls}
            else {
              val shiftingPos = zipMapMap(originalTps, shiftedTps) { case (x, y) => x != y }
              val bynameZipShiftss = zipMapMap(byNamess, shiftingPos)((x, y) => (x, y))
              val (ebs, ees) = zipMapMap(xss, bynameZipShiftss) {
                case (e, (true, true)) => // by name and shift!
                  (List(), unwrappingTransformation(wrapit(e))) // transform if shifted
                case (e, (false, true)) => // by value and shift! --> (lambda)
                  val q"(..$xs) => $x" = e
                  // NOTE: retermcheck/retypecheck at non-toplevel turns X.this into _root_.<empty> where X is the annottee
                  // this is a workaround, by retermchecking, but then using the untypecked tree...
                  val tmp = q"(..$xs) => ${unwrappingTransformation(wrapit(x))}"
                  (List(), tmp setType retermcheck(tmp).tpe)
                case (e, (false, false)) if predicate(e) => // by value and no shift! --> normalize
                  asBlockVar(transform(e))
                case (e, (_, _)) =>
                  (List(), e)
              }.map(_.unzip).unzip
              import scala.language.existentials
              val enclosed = unwrap(q"$f2[..$ts](...$ees)") withAttrs (t,p)
              val (tmpsymb, tmp) = freshSymbol("tmp", t, p)
              q"..$fb; ..${ebs.flatten.flatten}; ${internal.valDef(tmpsymb, enclosed) withAttrs (t,p)}; $tmp"
            }
            res
            //}
          }

        case _: DefDef | _: ClassDef | _: Function =>
          tree // stop at code that is not evaluated, also see lazy arguments in the function application case
        case _ =>
          //println(s"default to super ${tree.tpe} -- $tree")
          //c.abort(tree.pos, s"match error normalisation not defined for this type of tree, yet ${tree.getClass}")
          superRecurse(tree)
      }

      //if (predicate(tree)) println(s"$tree ---> $result ||| ${tree.tpe} -> ${flat(result).last.tpe}\n")

      def UnitAndNullAsNotype(x: Type) = if (x == null || x == definitions.UnitTpe) NoType else x
      if (UnitAndNullAsNotype(flat(tree).last.tpe) != UnitAndNullAsNotype(flat(result).last.tpe))
        c.abort(tree.pos, s"\nWARNING, PLEASE FIX ME. THIS TRANSFORMATION IS NOT TYPE PRESERVING: ${flat(tree).last.tpe} -> ${flat(result).last.tpe}\n$tree\n--->\n$result\n")

      result
    }

    def unwrappingTransformation(lst: Tree): Tree = {
      val tmp = this.store.toList; this.store.clear()
      val result = flat(postTransformList(flat(transform(lst))))
      val middle = this.store.toList; this.store.clear(); this.store.appendAll(tmp)
      q"..${middle ++ result}"
    }

    private def postTransformList(lst: List[Tree]): Tree = {
      val tpe = lst.last.tpe

      //println("transformTheBlock " + lst)
      //println("-- -- -- ->")
      //lst.foreach(x => println((if (predicate(x)) "*" else "-") + " " + x ))

      val idx = lst.indexWhere(predicate) // find first occurrence
      if (idx == -1) q"..$lst" else {
        @unchecked val (before, scrut :: tail) = lst.splitAt(idx)
        val tail2 = if (tail.isEmpty) Nil else flat(postTransformList(tail))

        scrut match {
          case t@q"$mods val $n: $tp = $scrut match { case $m => $x }" if subtransform.isDefinedAt(scrut) =>
            assert(tp.tpe.typeSymbol.fullName == "scala.Tuple2", tp.tpe.typeSymbol.fullName)
            val tail3 = tail2
            val AsBlock(b, e) = subtransform(scrut)(t.symbol, { bb => val (b, e) = asBlockVar(bb)
              q"..$b; ..${tail3.map(x => replace(x, t.symbol, e))}" setType tpe })
            q"..$before; ..$b; ${e setType tpe}"
            //q"..$before; ..$b; ${q"$e match { case $m => $x }" setType tpe}"

          case t@q"$mods val $n: $tp = $scrut" if subtransform.isDefinedAt(scrut) =>
            val AsBlock(b, e) = subtransform(scrut)(t.symbol, { bb => val (b, e) = asBlockVar(bb)
              q"..$b; ..${tail2.map(x => replace(x, t.symbol, e))}" setType tpe })
            q"..$before; ..$b; ${e setType tpe}"

          case x if subtransform.isDefinedAt(scrut) =>
            // sys.error(s"matchy error: $x")
            val AsBlock(b, e) = subtransform(scrut)(null, { case AsBlock(b, e) =>
              q"..$b; $e; ..$tail2" setType tpe })
            q"..$before; ..$b; ${e setType tpe}"

          case x if neverShift =>
            q"..$before; ..$scrut; ..$tail2"
        }
      }
    }

    private def mkCall(ess: List[List[Tree]], byNamess: List[List[Boolean]]): (List[List[Seq[Tree]]], List[List[Tree]]) = {
      val (ebs, ees) = zipMapMap[Tree, Boolean, (Seq[Tree], Tree)](ess, byNamess) {
        case (e, true) => (List(), e) // leave byname params untouched
        case (e, _) => asBlockVar(transform(e)) // normalize rest
      }.map(_.unzip).unzip
      (ebs, ees)
    }

    override val ff: PartialFunction[Tree, Tree] = { case t: Tree if false => t } /* ignoree ff */
  }

  private def zipMapMap[X, Y, Z](xss: List[List[X]], yss: List[List[Y]])(f: (X, Y) => Z): List[List[Z]] = {
    assert(xss.length == yss.length, xss -> yss)
    (xss zip yss).map { case (xs, ys) =>
      assert(xs.length == ys.length)
      (xs zip ys).map { case (x, y) => f(x, y) } }
  }

  def replace(tree: Tree, s1: Symbol, y: Tree): Tree =
    presTransform(tree) (that => { case x: RefTree if x.symbol == s1 => y })

  def hasSym(tree: Tree, expected: String): Boolean = if (tree.symbol == null) false else {
    var sym = tree.symbol.fullName.toString
    if (sym.startsWith("_root_.")) sym = sym.substring(7)
    if (sym.endsWith(".apply")) sym = sym.substring(0, sym.length - 6)
    sym == expected
  }

  private def isAtom(t: Tree): Boolean = t match { case _: RefTree | _: This | _: Super | _: Literal => false; case _ => true }
  def asBlockVar(x: Tree, ident: Option[Ident] = None, predicate: Tree => Boolean = isAtom): (List[Tree], Tree) = x match {
    case Block(b, e) =>
      val (eb, ee) = asBlockVar(e, ident=ident, predicate=predicate)
      (b ++ eb, ee)
    case _ if !predicate(x) =>
      assert(ident.isEmpty, "TODO need to use ident if given !?")
      (List(), x)
    case e: Tree if ident.isEmpty =>
      val (sym, ident2) = freshSymbol("x", e)
      //println("wtf2?" + q"val ${TermName(sym.name.toString)}: Boolean = $e")
      (List(internal.valDef(sym, e)), ident2)
    case e =>
      (List(internal.valDef(ident.get.symbol, e)), ident.get)
  }

  // WARNING: these freshSymbols do not uphold common invariant that owner should not be NoSymbol...
  // retypecheck fixes that for us.
  def freshSymbol(name: String, blame: Tree): (Symbol, Ident) = {
    assert(blame.tpe != null, s"type($blame) must not be null")
    freshSymbol(name, blame.tpe, blame.pos, true)
  }
  def freshSymbol(name: String, blame: Tree, freshen: Boolean): (Symbol, Ident) =
    freshSymbol(name, blame.tpe, blame.pos, freshen)
  def freshSymbol(name: String, tpe: Type, pos: Position): (Symbol, Ident) =
    freshSymbol(name, tpe, pos, true)
  def freshSymbol(name: String, tpe: Type, pos: Position, freshen: Boolean): (Symbol, Ident) = {
    val term = TermName(if (freshen) c.freshName(name) else name)
    val sym = c.internal.newTermSymbol(NoSymbol, term) //; sym.setOwner(sym)
    assert(tpe != null, s"$tpe must not be null")
    sym.setInfo(tpe)
    (sym, atPos(pos)(Ident(term)) setSymbol sym setType tpe)
  }

  def filterUnit(b: List[Tree], default: Tree): List[Tree] =
    if (b.isEmpty) List(default) else b.init.filter { case q"()" => false; case _ => true } :+ b.last
  def flatten(stats: List[Tree]): List[Tree] =
    if (stats.isEmpty) List() else filterUnit(stats.flatMap(flat), UNIT)
  def flat(x: Tree): List[Tree] = x match {
    case Block(b, e) => b :+ e
    case e => List(e)
  }

  def splitAfter[T](ts: List[T])(f: T => Boolean): List[List[T]] = if (ts.isEmpty) Nil else {
    ts.indexWhere(f) match {
      case -1 => List(ts)
      case i =>
        val (ts1, ts2) = ts.splitAt(i + 1)
        ts1 :: splitAfter(ts2)(f)
    }
  }

  import c.internal._
  case class TypingContext(api: TypingTransformApi) {
    def currentOwner: Symbol = api.currentOwner
    def typecheck(tree: Tree, owner: Symbol = c.internal.enclosingOwner): Tree =
      api.atOwner(tree, owner)(api.typecheck(tree))
  }
  def withApi[X](f: TypingContext => X): X = {
    var theApi: TypingTransformApi = null
    typingTransform(Literal(Constant(()))){(tree, api) => theApi = api; tree}
    f(TypingContext(theApi))
  }

  object Stable {
    def unapply(t: Tree): Option[Tree] = t match {
      case _: Literal => Some(t)
      case _: TypeApply => Some(t)
      case _: Ident => Some(t)
      case _: Function => Some(t)
      case Select(Stable(_), _) => Some(t)
      case _ => None
    }
  }

  object PotBlock {
    def fromList(t: List[Tree]): Tree = t match {
      case List(x) => x
      case xs => Block(xs.init, xs.last)
    }
    def apply(b: List[Tree], e: Tree): Tree = b match {
      case Nil => e
      case xs => Block(xs.init, xs.last)
    }
    def unapply(t: Tree): Option[(List[Tree], Tree)] = t match {
      case Block(bs, e) => Some((bs, t))
      case e => Some((Nil, e))
    }
  }

}
