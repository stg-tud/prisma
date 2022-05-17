package prisma.phases

import prisma.meta.MacroDef.ClosureString
import prisma.runner.headlong
import prisma.runner.web3s.Compiled
import prisma.meta.MacroDef
import prisma.mid._
import prisma.{FLAGS, Prisma, back, mid}
import prisma.Prisma._

// note for later -- howto extract typerefs:
//   val TypeTree() withAttrs (_, TypeRef(_,_, List(xxx)), p) = retpe
//   val inner = q"$mods def $name(..$args): $xxx = $body" withAttrs (xxx,p)

trait PTranslateExpr extends PTranslateType with PSecurity {
  import c.universe._

  /*
  private def defdefToLambda(x: DefDef): Tree = {
    val fun = x.symbol
    val formal = x.vparamss.head.map(_.symbol.info)
    val concrete = x.vparamss.head.zipWithIndex.map { case (a, i) => q"x.${TermName("_" + (1 + i))}" }
    q"(x: (..$formal)) => $fun(..$concrete)"
  }
   */

  val transformContractCode: Phase => Phase = (f: Phase) => (tree: Tree) => tree match {
    case cc@ModuleDef(mods, cname, tt@Template(parents, self, stats)) => // for case class companion
      val (vparams, ctor, rest) = splitTemplate(stats)
      val (contractDefs, clientDefs) = rest.partition {
        case x: DefTree if isContractSym(x.symbol) => true
        case _ => false
      }
      val contract2 = flatten(contractDefs.map(f))
      val updatedRest = contract2 ++ clientDefs
      treeCopy.ModuleDef(cc, mods, cname,
        treeCopy.Template(tt, parents, self, vparams ++ List(ctor) ++ updatedRest))

    case cc@ClassDef(mods, cname, tparam, tt@Template(parents, self, stats)) =>
      val (vparams, ctor, rest) = splitTemplate(stats)
      val (contractDefs, clientDefs) = rest.partition {
        case x: DefTree if isContractSym(x.symbol) => true
        case _ => false
      }
      val contract2 = flatten(contractDefs.map(f))
      val updatedRest = contract2 ++ clientDefs
      treeCopy.ClassDef(cc, mods, cname, tparam,
        treeCopy.Template(tt, parents, self, vparams ++ List(ctor) ++ updatedRest))
  }

  val transformCode: (List[Tree] => List[Tree]) => Tree => ClassDef = f => tree => {
    val cc@ClassDef(mods, cname, tparam, tt@Template(parents, self, stats)) = tree
    val (vparams, ctor, rest) = splitTemplate(stats)
    val updatedRest = flatten(f(rest))
    treeCopy.ClassDef(cc, mods, cname, tparam,
      treeCopy.Template(tt, parents,
        self, vparams ++ List(ctor) ++ updatedRest))
  }

  def partitionCoClMix(trees: List[Tree]): (List[Tree], List[Tree], List[Tree]) = {
    val (contractDefs, _) = trees.partition {
      case x: DefTree if isContractSym(x.symbol) => true
      case _ => false
    }
    val (clientDefs, _) = trees.partition {
      case x: DefTree if isClientSym(x.symbol) => true
      case _ => false
    }
    val (restDefs, _) = trees.partition {
      case x: DefTree if !isClientSym(x.symbol) && !isClientSym(x.symbol) => true
      case _ => false
    }
    (contractDefs, clientDefs, restDefs)
  }

  val abiCodec: Phase = { tree: Tree =>
    val translator = new TypeTranslator(tree.collect {
      case MyClassDef(mods, name, tparams, parents, self, vparams, ctor, rest) withAttrs (s,_,_)
      if isContractSym(s) =>
        (s, vparams.asInstanceOf[List[ValDef]])
    }.toMap)
    presTransform(tree)(that => {
      case tree@q"$f.apply[$t]($x)" if isType[Prisma.abiEncode.type](f) =>
        genAbiEncode(translator, tree.pos, t.tpe, that.rec(x))
      case tree@q"$f.apply[$t]($x)" if isType[Prisma.abiEncodePacked.type](f) =>
        genAbiEncodePacked(translator, tree.pos, t.tpe, that.rec(x))
      case tree@q"$f.apply[$t]($x)" if isType[Prisma.abiDecode.type](f) =>
        genAbiDecode(translator, tree.pos, t.tpe, that.rec(x))
    })
  }

  private def genAbiDecode(translator: TypeTranslator, p: Position, t: Type, x: Tree) = {
    val sig = prisma.back.solidity.gen(translator.translateTypeFilterUnit(t), false)._1.replace("address payable", "address").replace(" ", "")
    val tup = q"${termSymbolOf[headlong.type]}.arrToTuple($sig, $x).get(0).asInstanceOf[${translator.scalaToHeadlongType(t)}]"
    translator.headlongToScala(t, tup, p)
  }
  private def genAbiEncodePacked(translator: TypeTranslator, p: Position, t: Type, x: Tree) = {
    val sig = prisma.back.solidity.gen(translator.translateTypeFilterUnit(t), false)._1.replace("address payable", "address").replace(" ", "")
    val tup = translator.scalaToHeadlong(x, true, p)
    q"${termSymbolOf[headlong.type]}.tupleToArrPacked($sig, $tup)"
  }
  private def genAbiEncode(translator: TypeTranslator, p: Position, t: Type, x: Tree) = {
    val sig = prisma.back.solidity.gen(translator.translateTypeFilterUnit(t), false)._1.replace("address payable", "address").replace(" ", "")
    val tup = translator.scalaToHeadlong(x, true, p)
    q"${termSymbolOf[headlong.type]}.tupleToArr($sig, $tup)"
  }

  def compileMix(mixStats: List[Tree]): List[Tree] = {
    //mixStats map {
    //  case q"↓(contract { () => $body })" =>
    //  case q"↓(clientN($n) { () => $body })" =>
    //}
    List()
  }

  val toSolidity: Phase = tree => {
    val mm@MyModuleDef(mods, name, parents, self, vparams, ctor, rest) withAttrs (s,t,p) = tree

    val coClasses = rest.collect {
      case (cc: ClassDef) withAttrs (s,_,_) if isContractSym(s) =>
        cc
    }
    val coClassTranslator = new TypeTranslator(tree.collect {
      case MyClassDef(mods, name, tparams, parents, self, vparams, ctor, rest) withAttrs (s,_,_)
      if isContractSym(s) =>
        (s, vparams.asInstanceOf[List[ValDef]])
    }.toMap ++ defaultTranslatorMap)

    // if   @co        -> drop
    // if   @cl        -> keep
    // if   @cl @co    -> keep
    // if   -          -> split contract + client
    val rest2 = rest.collect {
      case cd: ClassDef if isClientSym(cd.symbol) =>
        cd

      case cd@ClassDef(mods, cname, tparam, tt@Template(parents, self, rest))
      if !isContractSym(cd.symbol) && !isClientSym(cd.symbol) =>
        val updatedRest = splitCoCl(coClasses, coClassTranslator, rest)
        val newparents =
          if (parents.exists(_.toString == "prisma.meta.MacroDef.ContractInterface")) parents
          else parents ++ List(tq"${symbolOf[MacroDef.ContractInterface]}")
        treeCopy.ClassDef(cd, mods, cname, tparam,
          treeCopy.Template(tt, newparents,
            self, updatedRest))
    }
    ModuleDef(mods, name, Template(parents, self, vparams ++ (ctor :: rest2))) withAttrs (s,t,p)
  }

  var necessity: Necessity = _
  var env: Set[Symbol] = _

  private def splitCoCl(coClasses: List[c.universe.ClassDef], coClassTranslator: TypeTranslator, rest: List[c.universe.Tree]) = {
    val (args, ctor, stats) = splitTemplate(rest)
    val (coArgs: List[ValDef@unchecked], clArgs: List[ValDef@unchecked], mixArgs: List[ValDef@unchecked]) = partitionCoClMix(args)
    var (coStats, clStats, mixStats) = partitionCoClMix(stats)

    // optimize unit valdefs away
    // optimize empty contract structs away
    coStats = coStats.flatMap {
      case t@ValDef(mods, name, tpt, expr) if tpt.tpe =:= typeOf[Unit] =>
        Some(expr)
      case t@MyClassDef(mods, name, tparams, parents, self, vparams, ctor, rest) if vparams.isEmpty =>
        None
      case t =>
        Some(t)
    }

//    // optimize: inline contract constructor call
//    val (clStats2, inlinables) = clStats.partition {
//      case ValDef(mods, name, tpt, rhs @ q"$f().value.apply()") if isContractSym(f.symbol) => false
//      case _ => true
//    }
//    coStats = coStats ++ inlinables.map { case ValDef(mods, name, tpt, rhs @ q"$call.value.apply()") => call }
//    clStats = clStats2

    // translate contract defs to solidity
    val (getUserData, runnerDef) = {
      env = coStats.collect {
        case t: ValDef => Set(t.symbol, t.symbol.asTerm.setter, getSym2(t.symbol)) //++ ctorToProp.get(t.symbol)
        case t: DefDef => Set(t.symbol)
      }.flatten.toSet ++ coClasses.map(_.symbol) ++ coArgs.collect {
        case vd: ValDef if vd.symbol.asTerm.isParamAccessor && !vd.symbol.asTerm.isAccessor => Set(vd.symbol)
      }.flatten

      val name = TermName("Contract") // TODO get name from stats

      val bothCo = (coClasses ++ coStats)
      necessity = new Necessity {
        override val reqArgs: Boolean = bothCo.exists(d => d.exists {
          case t@q"$ctor.apply[$_]($x)"
          if isType[contract.type](ctor) =>
            //println("FOUND", t, !(x.tpe =:= typeOf[Unit]))
            !(x.tpe =:= typeOf[Unit])
          case t@q"$ctor.flatMap[$_, $_]($f(..$args), $closurestring.apply[$_, $_]($d, new $c(..$cargs).asInstanceOf[$_]))"
          if isType[contract.type](ctor)
          && isType[MacroDef.ClosureString.type](closurestring) =>
            args.nonEmpty
          case _ => false
        })
        override val storage: Boolean = bothCo.exists(d => d.exists {
          case t@q"$ctor.flatMap[$_, $_]($f(..$args), $closurestring.apply[$_, $_]($d, new $c(..$cargs).asInstanceOf[$_]))"
          if isType[contract.type](ctor)
          && isType[MacroDef.ClosureString.type](closurestring) =>
            cargs.nonEmpty
          case _ => false
        })
        override val flatmap: Boolean = (reqArgs || storage)
        override val mk: Boolean = (reqArgs || storage)
        override val state: Boolean = (flatmap || mk) || (bothCo.exists(d => d.exists {
          case t@q"$ctor.flatMap[$_, $_]($_, $_)"
            if isType[contract.type](ctor) =>
            true
          case t@q"$ctor.apply[$_]($x)"
            if isType[contract.type](ctor) =>
            true
          case _ => false
        }))
      }

      val contractDefsSolidity = bothCo flatMap translateDef
      val argT = coClassTranslator.translateValDef(coArgs)
      val (solSource, solMap) = prisma.back.solidity.gen(Module(
          s"compiled with ${FLAGS.compiletimeFlagsDescription}",
          Seq(Contract(Word(name.toString, 0, 0), Seq(), argT, contractDefsSolidity))),
        necessity)
      val scalaSource =
        try new String(stats.find(_.pos != NoPosition).get.pos.source.content)
        catch { case _: Throwable => "" }
      val web3Source = prisma.runner.web3s.Source(name.toString, solSource, scalaSource, solMap.map { case (a, b, c, d) => s"$a,$b,$c,$d" }.mkString(";"), filename).compile
      val ctorSig = prisma.back.solidity.gen(TRecord(argT.lst.map { case (k, v) => Word("", 0, 0) -> v }), false)._1.replace("address payable", "address").replace(" ", "")
      val ctorArgs = coArgs.map(x =>
        coClassTranslator.scalaToHeadlong(
          internal.setType(Ident(TermName(x.name.toString)), x.symbol.info),
          false, x.pos))
      val runnerDef =
        q"""val thisRunner = new ${symbolOf[Compiled]}(
              ${web3Source.className}, ${web3Source.binCT},
              ${web3Source.sol}, ${web3Source.scala},
              ${web3Source.srcmapRT}, ${web3Source.binRT}, ${web3Source.sol2Scala}
            ).loadOrDeploy(acc$$, addr$$, $ctorSig, $ctorArgs)
            override def addr = new ${symbolOf[Address]}(new _root_.java.math.BigInteger(thisRunner.addr.substring(2), 16))
            """

      val getUserData =
        if (necessity.reqArgs) coClassTranslator.generateTxCall(List(), internal.setInfo(internal.newMethodSymbol(NoSymbol, TermName("reqArgs")), internal.methodType(List(), typeOf[AnyRef])), NoPosition)
        else q"new _root_.scala.Array[_root_.scala.Byte](0)"

      (getUserData, runnerDef)
    }

    val crossStats = {
      val badAnnots = Set(
        typeOf[co].typeSymbol,
        typeOf[cl].typeSymbol,
        typeOf[cross].typeSymbol,
        typeOf[view].typeSymbol,
        typeOf[MacroDef.top].typeSymbol,
      )

      def unmark(mods: Modifiers): Modifiers =
        Modifiers(mods.flags, mods.privateWithin, mods.annotations
          .filter(s => !badAnnots.contains(s.tree.tpe.typeSymbol)))

      // contract defs = serialize . call . deserialize
      val crossAccessors = coStats.collect {
        case a@q"$mods var $name: $tpt = $expr" withAttrs (s, t, p)
        if isCrossSym(a.symbol) =>
          val methodname = if (needsAccessor(s)) TermName("get_" + name.toString) else name
          val call = coClassTranslator.generateTxCall(List(), internal.setInfo(internal.newMethodSymbol(NoSymbol, methodname), internal.methodType(List(), tpt.tpe)), p)
          atPos(p)(q"${unmark(mods)} def $name: $tpt = $call")

        case a@q"$mods val $name: $tpt = $expr" withAttrs (s, t, p)
        if isCrossSym(a.symbol) =>
          val methodname = if (needsAccessor(s)) TermName("get_" + name.toString) else name
          val call = coClassTranslator.generateTxCall(List(), internal.setInfo(internal.newMethodSymbol(NoSymbol, methodname), internal.methodType(List(), tpt.tpe)), p)
          atPos(p)(q"${unmark(mods)} def $name: $tpt = $call")

        case a@q"$mods def $name(..$vparams): $tpt = $expr" withAttrs (s, t, p)
        if isCrossSym(a.symbol) =>
          val args = vparams.map { case x: ValDef => internal.setType(Ident(x.symbol.name), x.symbol.info) }
          val call = /*s.info match {
            case TypeRef(pre, sym, List(tk, tv)) if sym.fullName == symbolOf[Mapping[Nothing,Nothing]].fullName =>
              val (inSig, outSig) = coClassTranslator.generateInOutSolidityTypeSignature(s.asMethod)
              q"""new ${termSymbolOf[RemoteMapping[Nothing,Nothing]]}[$tk,$tv](
                  acc$$, deployed, $inSig, $outSig,
                  (k: $tk) => ${genAbiEncode(coClassTranslator, NoPosition, tk, q"k")},
                  (t: Tuple) => ${genAbiDecode(coClassTranslator, NoPosition, tv, q"t")}
                )"""
            case _ =>*/
              coClassTranslator.generateTxCall(args.toList, s.asMethod, p)
          //}
          val res = q"${unmark(mods)} def $name(..$vparams): $tpt = $call"
          atPos(p)(res)
      }

      val getMe = q"new ${symbolOf[AddressPayable]}(new _root_.java.math.BigInteger(acc$$.credentials.getAddress.substring(2), 16))"
      val getState = coClassTranslator.generateTxCall(List(), internal.setInfo(internal.newMethodSymbol(NoSymbol, TermName("state")), internal.methodType(List(), typeOf[U256])), NoPosition)
      val contractMap = {
        q"private val securityIdToFuncname: _root_.scala.Predef.Map[_root_.scala.Predef.String, _root_.scala.Predef.String] = ${securityIdToFuncname.toMap}"
      }
      val dispatchContract =
        q"""override def dispatchContract(sig_data: (_root_.scala.Predef.String, _root_.prisma.Prisma.U256, _root_.com.esaulpaugh.headlong.abi.Tuple)): String = {
                val signature = sig_data._1
                val money = sig_data._2
                val userdata = sig_data._3

                println("-> "+ signature + userdata + " (with " + money + "$$)\n")
                thisRunner.magic2(money.bint, acc$$, signature, userdata.toArray():_*).substring(2)
              }"""
      val dispatchClientBody = Match(q"funcname", clStats.collect {
        case defdef: DefDef
        if isCrossSym(defdef.symbol) =>
          //val literal = Literal(Constant(defdef.symbol.name.toString))
          val scalaArgs = defdef.symbol.asMethod.paramLists.head.zipWithIndex.map {
            case (x, i) => coClassTranslator.headlongToScala(x.info, q"userdata2.get($i)", x.pos)
          }
          val TypeRef(_, _, List(outtype)) = defdef.symbol.asMethod.returnType
          val (inSig, _) = coClassTranslator.generateInOutSolidityTypeSignature(defdef.symbol.asMethod)
          val (_, outSig) = coClassTranslator.generateInOutSolidityTypeSignature(internal.setInfo(internal.newMethodSymbol(NoSymbol, TermName("")), internal.methodType(List(), outtype)))
          // assert(outSig == outSig2) -> not same
          val translated = coClassTranslator.scalaToHeadlong(internal.setType(q"result", outtype), true, defdef.pos)
          //println(s"$inSig : $outSig - $translated")
          val funcname = defdef.symbol.name.toString.split("\\$").last
          atPos(defdef.pos)(
            cq"""${securityFuncnameToId(funcname)} =>
                val userdata2 = ${termSymbolOf[headlong.type]}.arrToTuple(${inSig.substring(inSig.indexOf('('))}, userdata)
                val tmp = ${defdef.symbol}(..$scalaArgs).value()
                if (tmp == null) null
                else {
                  val (money, result) = tmp
                  ($funcname + $outSig, money, new _root_.com.esaulpaugh.headlong.abi.Tuple($translated))
                }
                """)
      })
      val dispatchClient =
        q"""override def dispatchClient(funcname: String, userdata: Array[Byte]):
                              (_root_.scala.Predef.String, _root_.prisma.Prisma.U256, _root_.com.esaulpaugh.headlong.abi.Tuple) = $dispatchClientBody"""

      val stateToFuncNameBody = Match(q"state", securityIdToFuncname.map{ case(key,value) => cq"$key => $value" }.toList)
      val stateToFuncName = q"""override def stateToFuncName(state: String): String = $stateToFuncNameBody"""

      List(
        contractMap,
        dispatchClient,
        dispatchContract,
        q"override def me(): ${symbolOf[AddressPayable]} = $getMe",
        q"override def userdata(): _root_.scala.Array[_root_.scala.Byte] = $getUserData",
        stateToFuncName,
        q"override def state(): prisma.Prisma.U256 = $getState",
      ) ++ crossAccessors
    }

    //clStats.foreach { d =>
    //  internal.setAnnotations(d.symbol,
    //    d.symbol.annotations filter (a => !badAnnots.contains(a.tree.tpe.typeSymbol)) :_*)
    //} /*.collect {
    //  case a@q"$mods val $name: $tpt = $expr" withAttrs (s, t, p) =>
    //    q"${unmark(mods)} val $name: $tpt = $expr" withAttrs(s, p)
    //  case a@q"$mods val $name: $tpt = $expr" withAttrs (s, t, p) =>
    //    q"${unmark(mods)} val $name: $tpt = $expr" withAttrs(s, p)
    //  case a@q"$mods var $name: $tpt = $expr" withAttrs (s, t, p) =>
    //    q"${unmark(mods)} var $name: $tpt = $expr" withAttrs(s, p)
    //  case a@q"$mods def $name(..$args): $tpt = $expr" withAttrs (s, t, p) =>
    //    q"${unmark(mods)} def $name(..$args): $tpt = $expr" withAttrs(s, p)
    //}*/

    val updatedRest = flatten(List(runnerDef) ++ crossStats ++ clStats ++ compileMix(mixStats))
    args ++ List(ctor) ++ updatedRest
  }

  private def needsAccessor(s: Symbol): Boolean =
    back.solidity.needsAccessor(Seq("public"), new TypeTranslator(Map()).translateType(s.info.resultType), s.name.toString)

  private def translateDef(tree: Tree): Seq[prisma.mid.Def] = tree match {
    //case tt@q"$mods val $name: (..$ts) => $ret = (..$vparams) => $expr" =>
    //  translateDef(q"$mods def $name(..$vparams): $ret = $expr")

    case vd@q"$mods val $name: $tpt = $expr" =>
      val theType = new TypeTranslator(Map()).translateType(tpt.tpe)
      val theExpr =
        if (expr == EmptyTree) Seq(mkWord(name.toString + "$", tree.pos))
        else translateExprs(flat(expr))
      val modst = translateMods(vd)
      Seq(Prop(mkWord(name.toString, tree.pos), theType, theExpr, modst))

    // TODO if simple type (uint, ...) and not complex (array/struct), then const instead of prop would be sufficient?
    // can we use needsAccessor for that?
    case vd@q"$mods var $name: $tpt = $expr" =>
      val theType = new TypeTranslator(Map()).translateType(tpt.tpe)
      val theExpr =
        if (expr == EmptyTree) Seq(mkWord(name.toString + "$", tree.pos))
        else translateExprs(flat(expr))
      val modst = translateMods(vd)
      Seq(Prop(mkWord(name.toString, tree.pos), theType, theExpr, modst))

    case dd@q"$mods def $name(..$vparams): $tpt = $expr" if isEventSym(dd.symbol) =>
      val namet = mkWord(name.toString, tree.pos)
      val in = new TypeTranslator(Map()).translateValDef(vparams)
      Seq(Event(namet, in))

    case dd@q"$mods def $name(..$vparams): $tpt = $expr" if name.toString().equals("$fallback") | name.toString().equals("$receive") && vparams.isEmpty =>   //TODO: Added by DK quick & dirty
      val namet = mkWord(name.toString.substring(1), tree.pos)
      val lst = flat(expr)
      val body = List() ++ lst map translateExpr
      Seq(Fallback(namet, wrapReturn(lst.last.tpe, body)))

    case dd@q"$mods def $name(..$vparams): $tpt = $expr" =>
      val namet = mkWord(name.toString, tree.pos)
      val in = new TypeTranslator(Map()).translateValDef(vparams)
      val out = new TypeTranslator(Map()).translateTypeFilterUnit(tpt.tpe)

      val modst = translateMods(dd)

      val body =
        (if (vparams.nonEmpty && vparams.last.asInstanceOf[ValDef].name.toString == "userdata"
             && frees(expr).map(_.name.toString).contains("userdata")
        ) List({
          val theType = new TypeTranslator(Map()).translateType(vparams.last.asInstanceOf[ValDef].symbol.info)
          Word(back.solidity.gen(theType)._1 +" userdata = abi.decode(store, (" + back.solidity.gen(theType, false)._1 + "))", 0, 0)
        }) else List()) ++ translateExprs(flat(expr))

      Seq(Func(namet, in, out, wrapReturn(expr.tpe, body), modst))

    case MyClassDef(mods, name, tparams, parents, self, vparams, ctor, rest) =>
      assert(parents.length == 1 && parents.head.symbol.fullName == "scala.AnyRef" || parents.toString == "List(AnyRef, Product, Serializable)"
        //|| parents.map(_.symbol.name.toString) == List("Object", "Product", "Serializable")
        ,
        s"NYI other parents except AnyRef: $parents")
      assert(ctor match { case DefDef(_, _, _, _, _, Block(bs, e)) => bs.length == 1 }, s"NYI non-empty ctor: $ctor")
      assert(tparams.isEmpty, s"NYI tparams non-empty for classes: $tparams")
      //assert(rest.isEmpty, s"NYI methods non-empty for classes: $rest")

      Seq(mid.Struct(mkWord(name.toString, tree.pos), TRecord(vparams.map { param =>
        (mkWord(param.symbol.name.toString, param.pos), new TypeTranslator(Map()).translateType(param.symbol.info.resultType))
      })))

    case k@MyModuleDef(mods, name, parents, self, vparams, ctor, rest) =>
      Seq() //mid.Struct()

    case expr =>
      //println(("EXPRESSION", expr))
      val theType = new TypeTranslator(Map()).translateType(expr.tpe)
      val theExpr = flat(expr) map translateExpr
      Seq(Prop(mkWord("_", expr.pos), theType, theExpr, Seq("")))

    //tree.foreach(println)
      //sys.error("??? " + tree)
  }

  // TODO pure methods should only simulated not executed, e.g., magic vs simulate
  private def translateMods(dd: c.universe.Tree): List[String] =
    (if (isViewSym(dd.symbol)) List("view") else List()) ++
    (if (isCrossSym(dd.symbol)) List("public") else List("private")) ++
    (if (dd.symbol.toString.contains("payable")) List("payable") else List())

  private def translateExprs(stats: Seq[Tree]): Seq[prisma.mid.Expr] = stats match {
    // translate destructuring assignment, e.g., (x,y) = (1,2)
    case (f@q"$_ val $_: $t = $expr match { case $_ => $_ }") :: tl =>
      val n = t.tpe.typeSymbol.toString match { case s"class Tuple$n" => n.toInt }
      val vars = tl.take(n)
      val name = mkWord(vars.map(_.symbol.name.toString).mkString("(", ", ", ")"), f.pos)
      val tpe = new TypeTranslator(Map()).translateType(t.tpe)
      val exprt = translateExpr(expr)
      Decl(name, tpe, exprt) +: translateExprs(tl.drop(n))
    case hd :: tl =>
      translateExpr(hd) +: translateExprs(tl)
    case Nil =>
      Nil
  }

  // put return(x) in front of last expression
  private def wrapReturn(tpe: Type, exprs: Seq[prisma.mid.Expr]): Seq[prisma.mid.Expr] = {
    val ttpe = tpe match {
      case x: ConstantType => x.value.tpe
      case x => x
    }
    val tttpe = new TypeTranslator(Map()).translateType(ttpe)
    if (tttpe == TRecord(Seq())) exprs else potReturnR(exprs)
  }

  private def potReturnR(exprs: Seq[prisma.mid.Expr]): Seq[prisma.mid.Expr] = exprs.last match {
    case prisma.mid.If(cond, body, alt) =>
      exprs.init :+ mid.If(cond, potReturnR(body), potReturnR(alt))
    case _ =>
      exprs.init :+ App(Word("return", 0, 0), Tuple(Seq(exprs.last)))
  }

  private def mkApp(x: prisma.mid.Expr, rest: prisma.mid.Expr*): prisma.mid.Expr = App( x, Tuple(Seq(rest:_*)))
  private def encodePacked(x: prisma.mid.Expr*): prisma.mid.Expr = mkApp( Dot(Word("abi", 0,0), Word("encodePacked", 0,0)), x:_*)

  private def translateExpr(tree: Tree): prisma.mid.Expr = tree match {
    case q"$x.length().asInstanceOf[$_]" =>
      val xt = translateExpr(x)
      mid.Dot(mkApp(mkWord("bytes", x.pos), xt), mkWord("length", x.pos))

    // meta /////////////////////////////////////////////////////////
    // client / contract requests & await (== "flatmap")
    case term@Apply(func: RefTree, args) if isClientSym(func.symbol) && !isContractSym(func.symbol) =>
      mkApp(Word("Closure", 0,0),
        mkWord(s"""0x${securityFuncnameToId(func.symbol.name.toString.substring(3))}""", func.pos),
        mkApp(Word("abi.encode",0,0), args map translateExpr:_*))
    case term: RefTree if isClientSym(term.symbol) && !isContractSym(term.symbol) =>
      mkApp(Word("Closure", 0,0),
        mkWord(s"""0x${securityFuncnameToId(term.symbol.name.toString.substring(3))}""", term.pos),
        mkApp(Word("abi.encode",0,0)))

    case term@q"$ctor.apply[$_]($x)"
    if isType[contract.type](ctor)
    && (necessity.reqArgs || necessity.storage) =>
//      println("!!! mk A")
      mkApp(Word("mk",0,0),
        mkApp(Word("Closure",0,0),
          //Word("0",0,0),
          mkWord("0x0", term.pos),
          mkApp(Word("abi.encode",0,0), translateExpr(x))))
    case term@q"$ctor.apply[$_]($x)"
    if isType[contract.type](ctor)
    && !(necessity.reqArgs || necessity.storage) =>
//      println("!!! mk B")
      mid.Asgn(Word("state",0,0), Word("0",0,0))

    case q"$f.flatMap[$x, $y]"
    if isType[contract.type](f)
    && (necessity.reqArgs || necessity.storage) =>
//      println("!!! flatmap A")
      mkWord("flatMap", f.pos)
    case q"$f.flatMap[$x, $y]($request, $closstring.apply[$_, $_]($expr, $_))"
    if isType[contract.type](f)
    && isType[ClosureString.type](closstring)
    //&& isType[contract.type](ctor)
    && !(necessity.reqArgs || necessity.storage) =>
//      println("!!! flatmap B")
      mid.Asgn(Word("state",0,0), translateExpr(expr))

    // stdlib /////////////////////////////////////////////////////////
    // require, sender, keccak256
    case t@q"new $ctor().asInstanceOf[$x]" if x.tpe =:= typeOf[AnyRef] =>
      Word(""" hex"" """,0,0)
    case t@q"new $ctor(..$args).asInstanceOf[$x]" if x.tpe =:= typeOf[AnyRef] =>
      mkApp(Word("abi.encode",0,0),
        mid.App(mkWord(ctor.symbol.name.toString, ctor.pos),
          mid.Tuple(args map translateExpr)))
    case t@q"$f.asInstanceOf[$x]" if x.tpe =:= typeOf[AnyRef] =>
      mkApp(mkWord("abi.encode", t.pos), translateExpr(f))
    case t@q"$f.asInstanceOf[$x]" =>
      val ft = translateExpr(f)
      val xt = mkApp(Word("",0,0), mkWord(back.solidity.gen(new TypeTranslator(Map()).translateType(x.tpe), false)._1, x.pos))
      mkApp(mkWord("abi.decode", t.pos), ft, xt)
    case t@q"$f.apply(..$xs)" if isType[keccak256Packed.type](f) =>
      mkApp(mkWord("keccak256Packed", f.pos), Tuple(xs.map(x => translateExpr(x))))
    case t@q"$f.apply(..$xs)" if isType[keccak256.type](f) =>
      mkApp(mkWord("keccak256", f.pos), Tuple(xs.map(x => translateExpr(x))))
    case t@q"$f.apply($h, $x, $y, $z)" if isType[ecrecover.type](f) =>
      mkApp(mkWord("ecrecover", f.pos), Tuple(Seq(
        mkApp(mkWord("bytes32", f.pos), translateExpr(h)),
        translateExpr(x),
        mkApp(mkWord("bytes32", f.pos), translateExpr(y)),
        mkApp(mkWord("bytes32", f.pos), translateExpr(z)),
      )))
    // TODO can ecrecoverPrefixed be implemented as a function using
    //      ecrecover(keccak256(abiEncodePacked("\"\\x19Ethereum Signed Message:\\n32\"", h), x, y, z) insted of here in the compiler?
    case t@q"$f.apply($h, $x, $y, $z)" if isType[ecrecoverPrefixed.type](f) =>
      mkApp(mkWord("ecrecover", f.pos), Tuple(Seq(
        mkApp(mkWord("bytes32", f.pos), Tuple(Seq(
          mkApp(mkWord("keccak256Packed", f.pos), Tuple(Seq(
              mkWord(keccak256_PREFIX_STRING, f.pos),
              translateExpr(h))))))),
        translateExpr(x),
        mkApp(mkWord("bytes32", f.pos), translateExpr(y)),
        mkApp(mkWord("bytes32", f.pos), translateExpr(z)),
      )))
    case t@q"$f.apply()" if isType[now.type](f) =>
      mkWord("block.timestamp", f.pos)
    case t@q"$f.apply()" if isType[value.type](f) =>
      mkWord("msg.value", t.pos)
    case t@q"$f.apply()" if isType[sender.type](f) =>
      mkWord("payable(msg.sender)", t.pos)
    case t@q"$f.apply()" if isType[balance.type](f) =>
      mkWord("address(this).balance", t.pos)
    case t@q"$f.apply()" if isType[thisAddress.type](f) =>
      mkWord("address(this)", t.pos)
    case t@q"$f.require($b, $str)" if isType[Predef.type](f) =>
      mkApp(mkWord("require", t.pos), translateExpr(b), translateExpr(str))
    case t@q"$f.require($b)" if isType[Predef.type](f) =>
      mkApp(mkWord("require", t.pos), translateExpr(b))
    case t@q"$f.who" if isType[MacroDef.type](f) =>
      mkWord("who", t.pos)
    case t@q"$f.state" if isType[MacroDef.type](f) =>
      mkWord("state", t.pos)
    case t@q"$f.apply($msg)" if isType[revert.type](f) =>
      mkApp(mkWord("revert", t.pos), translateExpr(msg))
    case t@q"$f.apply($contract, $money, $args)" if isType[contractCall.type](f) =>
      val moneyt = translateExpr(money)
      //val tmpvalue = c.freshName("tmpvalue").toString
      val contractt = translateExpr(contract)
      val argst = translateExpr(args)
      //Seq(
        //Decl(Word(tmpvalue, 0,0), new TypeTranslator(Map()).translateType(money), moneyt),
        mkApp(Dot(contractt, mkWord(s"call{value:${back.solidity.genExpr(moneyt)._1}}", f.pos)), argst)
      //)
    case t@q"$f.apply($contract, $money, $funcName, ..$args)" if isType[contractCall.type](f) =>
      mkApp(Dot(translateExpr(contract), mkWord(s"call{value:${back.solidity.genExpr(translateExpr(money))._1}}", t.pos)),
        mkApp(mkWord("abi.encodePacked", t.pos),
          translateExpr(funcName),
          mkApp(mkWord("abi.encode", t.pos), mid.Tuple(args.map(x => translateExpr(x))))
        )
      )

    // closure
    case q"$x.apply[..$ts](..$xs)" if isType[MacroDef.ClosureString.type](x) =>
      mid.App(Word("Closure",0,0), mid.Tuple(xs.map(x => translateExpr(x))))

    //// uptoeach
    //case q"$x.apply(...$xss)" if isType[UptoEach.type](x) =>
    //  mid.App(Word("UptoEach",0,0), mid.Tuple(xss.flatten.map(x => translateExpr(x))))

    // array ctor length get set
    case q"$f.ofDim[$t]($sz)($classTag)" if isType[Arr.type](f) =>
      val ttt = back.solidity.gen(new TypeTranslator(Map()).translateType(t.tpe), false)._1
      mkApp(Word(s"new $ttt[]",0,0), translateExpr(sz))
//    case TypeApply(tpe, args) /* if tpe.tpe.resultType =:= typeOf[Arr[_]] */=>
//      println(tpe.tpe.resultType =:= typeOf[Arr[_]]) // TODO
//      Word("Array",0,0)
    case q"$x.length" if isType[Arr[_]](x) =>
      val xt = translateExpr(x)
      mid.Dot(xt, mkWord("length", x.pos))
    case q"$x.push" if isType[Arr[_]](x) =>
      val xt = translateExpr(x)
      mid.Dot(xt, mkWord("push", x.pos))
    case q"$x.apply($arg).update($arg2,$arg3)" if isType[Arr[_]](x) =>
      val xt = translateExpr(q"$x.apply($arg)")
      val argt = translateExpr(arg2)
      val arg2t = translateExpr(arg3)
      mid.Asgn(mid.Get(xt, mid.Tuple(Seq(argt))), arg2t)
    case q"$x.apply($arg)" if isType[Arr[_]](x) =>
      val xt = translateExpr(x)
      val argt = translateExpr(arg)
      mid.Get(xt, mid.Tuple(Seq(argt)))
    case q"$x.update($arg, $arg2)" if isType[Arr[_]](x) =>
      val xt = translateExpr(x)
      val argt = translateExpr(arg)
      val arg2t = translateExpr(arg2)
      mid.Asgn(mid.Get(xt, mid.Tuple(Seq(argt))), arg2t)

    // StaticArray
//    case TypeApply(tpe, args) /*if tpe.tpe.resultType.dealias =:= typeOf[StaticArray[_,_]]*/ =>
//      println(tpe, args, tpe.tpe.resultType =:= typeOf[StaticArray[_,_]]) // TODO
//      Word("Array",0,0)
    case q"$x.length" if isType[StaticArray[_,_]](x) =>
      val xt = translateExpr(x)
      mid.Dot(xt, mkWord("length", x.pos))
    case q"$x.apply($arg).update($arg2,$arg3)" if isType[StaticArray[_,_]](x) =>
      val xt = translateExpr(q"$x.apply($arg)")
      val argt = translateExpr(arg2)
      val arg2t = translateExpr(arg3)
      mid.Asgn(mid.Get(xt, mid.Tuple(Seq(argt))), arg2t)
    case q"$x.apply($arg)" if isType[StaticArray[_,_]](x) =>
      val xt = translateExpr(x)
      val argt = translateExpr(arg)
      mid.Get(xt, mid.Tuple(Seq(argt)))
    case q"$x.update($arg, $arg2)" if isType[StaticArray[_,_]](x) =>
      val xt = translateExpr(x)
      val argt = translateExpr(arg)
      val arg2t = translateExpr(arg2)
      mid.Asgn(mid.Get(xt, mid.Tuple(Seq(argt))), arg2t)

    //Mappings
    case q"$x.get($arg).update($arg2,$arg3)" if isType[Mapping[_,_]](x) =>
      val xt = translateExpr(q"$x.get($arg)")
      val argt = translateExpr(arg2)
      val arg2t = translateExpr(arg3)
      mid.Asgn(mid.Get(xt, mid.Tuple(Seq(argt))), arg2t)
    case q"$x.update($arg,$arg2)" if isType[Mapping[_,_]](x) =>
      val xt = translateExpr(x)
      val argt = translateExpr(arg)
      val arg2t = translateExpr(arg2)
      mid.Asgn(mid.Get(xt, mid.Tuple(Seq(argt))), arg2t)
    case q"$x.get($arg)" if isType[Mapping[_,_]](x) =>
      val xt = translateExpr(x)
      val argt = translateExpr(arg)
      mid.Get(xt, mid.Tuple(Seq(argt)))

    //Event
    case x@q"$e.apply($arg)" if isType[emit.type](e) =>
      EmitEvent(translateExpr(arg))

    // language ///////////////////////////////////////////////////////////
    // literals: unit, bool, str, int, unsigned int, lambdas
    case q"(..$args1) => $f(..$args2)" =>
      mkWord(s""""$f"""", f.pos)

    case Literal(Constant(())) => mid.Word("uint8(0)", 0,0)
    case Literal(Constant(y: Boolean)) => mid.Word(if (y) "true" else "false", 0,0)
    case Literal(Constant(y: String)) =>  mkWord("\"" + y.replace("\"", "\\\"") + "\"", tree.pos)
    case Literal(Constant(y: Int)) => mkApp(Word("int32",0,0), mkWord(y.toString, tree.pos))
    case Literal(Constant(y: Long)) => mkApp(Word("int64",0,0), mkWord(y.toString, tree.pos))

    case q"$toUint.ToUint(${Literal(Constant(str: String))}).u" if isType[Prisma.type](toUint) => mkApp(Word("uint256",0,0), Word(str, 0,0))
    case q"$toUint.ToUint(${Literal(Constant(str: String))}).u8" if isType[Prisma.type](toUint) => mkApp(Word("uint8",0,0), Word(str, 0,0))
    case q"$toUint.ToUint(${Literal(Constant(str: String))}).u16" if isType[Prisma.type](toUint) => mkApp(Word("uint16",0,0), Word(str, 0,0))
    case q"$toUint.ToUint(${Literal(Constant(str: String))}).u32" if isType[Prisma.type](toUint) => mkApp(Word("uint32",0,0), Word(str, 0,0))
    case q"$toUint.ToUint(${Literal(Constant(str: String))}).u64" if isType[Prisma.type](toUint) => mkApp(Word("uint64",0,0), Word(str, 0,0))
    case q"$toUint.ToUint(${Literal(Constant(str: String))}).a" if isType[Prisma.type](toUint) => mkApp(Word("address",0,0), Word(str, 0,0))
    case q"$toUint.ToUint(${Literal(Constant(str: String))}).p" if isType[Prisma.type](toUint) => mkApp(Word("payable",0,0), Word(str, 0,0))

    case q"$fUint.from($uint)" if isType[U256.type](fUint) => mkApp(Word(s"uint256",0,0), translateExpr(uint))
    case q"$fUint.from($uint)" if isType[U8.type](fUint) => mkApp(Word(s"uint8",0,0), translateExpr(uint))
    case q"$fUint.from($uint)" if isType[U16.type](fUint) => mkApp(Word(s"uint16",0,0), translateExpr(uint))
    case q"$fUint.from($uint)" if isType[U32.type](fUint) => mkApp(Word(s"uint32",0,0), translateExpr(uint))
    case q"$fUint.from($uint)" if isType[U64.type](fUint) => mkApp(Word(s"uint64",0,0), translateExpr(uint))
    case q"$fUint.from($uint)" if isType[Address.type](fUint) => mkApp(Word(s"address",0,0), translateExpr(uint))
    case q"$fUint.from($uint)" if isType[AddressPayable.type](fUint) => mkApp(Word(s"payable",0,0), translateExpr(uint))

    // control flow: while, if
    case While(_, c, es) =>
      mid.Loop(
        mid.If(translateExpr(c),
          Seq(mid.Word("0",0,0)),
          Seq(mid.Break())) +:
        translateExprs(es))
    case If(cond, body, alt) =>
      mid.If(
        translateExpr(cond),
        translateExprs(flat(body)),
        translateExprs(flat(alt)))
    case q"$f.WHILE($x)($y)" if isType[contract.type](f) =>
      mid.Loop(
        mid.If(translateExpr(x),
          Seq(mid.Word("0",0,0)),
          Seq(mid.Break())) +:
        translateExprs(flat(y)))
    //case Match(sel, cases) =>
    //  cases foreach { case CaseDef(pat, guard, body) => }

    // references: setter, globals and identifiers
    case Apply(x: RefTree, List(expr)) if x.symbol.isTerm && x.symbol.asTerm.isSetter =>
      val a = x match {
        case Select(pre, nm) =>
          translateExpr(internal.setSymbol(Select(pre, x.symbol.asTerm.getter.name), x.symbol.asTerm.getter))
        case _ =>
          mkWord(x.symbol.asTerm.getter.name.toString, x.pos)
      }
      mid.Asgn(a, translateExpr(expr))
    case x: RefTree if env(getSym2(x.symbol)) => // shortcut: env are global names
      mkWord(x.name.toString, x.pos)
    case x: Ident =>
      mkWord(x.name.toString, x.pos)

    // unary ops, binary ops; dot, call, typecall, constructor
    case q"$that.unary_!" =>
      mkApp(Word("!",0,0), translateExpr(that))
    case q"$that.$it($other)" if isType[java.lang.String](that) && it.toString == "$plus" && isType[java.lang.String](other) =>
      val thatt = translateExpr(that)
      val othert = translateExpr(other)
      mkApp(Word("string", 0,0), encodePacked(thatt, othert))
    case q"$that.$it($other)" if isType[java.lang.String](that) && it.toString == "$plus" =>
      val thatt = translateExpr(that)
      val othert = translateExpr(other)
      mkApp(Word("string", 0,0), encodePacked(thatt, mkApp(Word("tohex", 0,0), encodePacked(othert))))
    case q"$obj.apply" => // TODO explicit x.apply() This neccessary?
      translateExpr(obj)
    case q"$obj.$it" => // if front.parser.nam2op.contains(it.toString) =>
      val objt = translateExpr(obj)
      val itt = it.decodedName.toString // front.parser.nam2op.getOrElse(it.toString, it.toString)
      if (tree.pos == NoPosition)
        println(f"!!! ${back.solidity.genExpr(objt, true)._1}.$itt in ${tree.pos} ${obj.symbol.pos} ${tree.symbol} ${env(tree.symbol)} ${isContractSym(tree.symbol)}")
      mid.Dot(objt, mkWord(itt, obj.pos))
    case q"$fun(..$args)" =>
      mkApp(translateExpr(fun), args.map(x => translateExpr(x)):_*)
    case TypeApply(f, ts) =>
      val lst = ts.map(x => back.solidity.gen(new TypeTranslator(Map()).translateType(x.tpe))._1)
      mid.App(translateExpr(f), Word("/* [" + lst.mkString(", ") + "] */", 0,0))
    case q"new $klass[..$_](..$args)" =>
      mid.App(translateExpr(klass), mid.Tuple(args map translateExpr))
    case t@q"new $klass[..$_](..$args)($len)" if isType[StaticArray[_,_]](t) =>
      mkApp(Word("Array",0,0), args.map(x => translateExpr(x)):_*)

    // constant, declaration, assignment, block
    case q"$mods val $name: $tpt = $expr" if tpt.tpe.toString == "Unit" =>
      translateExpr(expr)
    case q"$mods val $name: $tpt = $expr" =>
      mid.Decl(mkWord(name.toString, tree.symbol.pos),
        new TypeTranslator(Map()).translateType(tpt.tpe),
        translateExpr(expr))
    case q"$mods var $name: $tpt = $expr" =>
      mid.Decl(mkWord(name.toString, tree.symbol.pos),
        new TypeTranslator(Map()).translateType(tpt.tpe),
        translateExpr(expr))
    case q"$lhs = $rhs" =>
      val lhst = translateExpr(lhs)
      val rhst = translateExpr(rhs)
      mid.Asgn(lhst, rhst)
    case x@Block(bs, e) =>
      val bs2 = (bs ++ List(e)).map(e => back.solidity.genExpr(translateExpr(e))._1) mkString (";\n  ")
      mkWord(s"/* {\n  $bs2\n} */", x.pos)

    //Create new class //TODO: Make sure that we map only struct creations here
    case x@Select(This(_), klass) =>
      mkWord(klass.toString,x.pos)

    // else
    case x =>
      // throw new Exception(x.getClass + " -- " + x.toString)
      mkWord("/* " + x.getClass + " -- " + x.toString() + " */", x.pos)
  }
}
