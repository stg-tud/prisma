package prisma.phases

import prisma.meta.Phases
import prisma.mid.{TArray, TMapping, TRecord, TSimple, Word}
import prisma.Prisma._

trait PTranslateType extends Phases {
  import c.universe._

  var intermediateCtr = 0
  def mkWord(str: String, pos: Position): Word =
    // if (pos != NoPosition) // TODO
    if (pos.isDefined) Word(str, pos.start, pos.start + str.length)
    else Word(str, 0, 0)

  class TypeTranslator(val structs: Map[Symbol, List[ValDef]]) {

    def generateTxCall(scalaArgs: List[Tree], m: MethodSymbol, pos: Position): Tree = {
      val scalaType = m.returnType
      val headlong = TermName(c.freshName("headlong"))
      val headlongArgs = scalaArgs.map(i => scalaToHeadlong(i, true, pos))
      val headlongType = scalaToHeadlongType(scalaType)
      val (inSig, outSig) = generateInOutSolidityTypeSignature(m)
      val call = q"thisRunner.magic(_root_.java.math.BigInteger.ZERO, acc$$, $inSig, $outSig, ..$headlongArgs)"
      val res =
        if (headlongType.toString == "_root_.scala.Unit")
          q"""$call
              ..${ flat(headlongToScala(scalaType, q"()", pos)) }"""
        else
          q"""val $headlong = $call.get(0).asInstanceOf[$headlongType]
              ..${ flat(headlongToScala(scalaType, q"$headlong", pos)) }"""
      atPos(pos)(res)
    }

    def scalaToHeadlong(value: Tree, frst: Boolean = false, pos: Position): Tree = atPos(pos)(value.tpe match {
      case _ if value.tpe =:= typeOf[AnyRef] => q"$value"
      case _ if value.tpe =:= typeOf[String] => q"$value"
      case _ if value.tpe =:= typeOf[Unit] => q"()"
      //case _ if value.tpe =:= typeOf[Int] && frst => q"java.lang.Integer.valueOf($value)"
      //case _ if value.tpe =:= typeOf[Int] => q"$value"
      case _ if value.tpe =:= typeOf[Boolean] && frst => q"_root_.java.lang.Boolean.valueOf($value)"
      case _ if value.tpe =:= typeOf[Boolean] => q"$value"
      case _ if value.tpe =:= typeOf[U8] && frst => q"_root_.java.lang.Integer.valueOf($value.bint.intValueExact())"
      case _ if value.tpe =:= typeOf[U8] => q"$value.bint.intValueExact()"
      case _ if value.tpe =:= typeOf[U16] && frst => q"_root_.java.lang.Integer.valueOf($value.bint.intValueExact())"
      case _ if value.tpe =:= typeOf[U16] => q"$value.bint.intValueExact()"
      case _ if value.tpe =:= typeOf[U32] && frst => q"_root_.java.lang.Long.valueOf($value.bint.longValueExact())"
      case _ if value.tpe =:= typeOf[U32] => q"$value.bint.longValueExact()"
      case _ if value.tpe =:= typeOf[U64] => q"$value.bint"
      case _ if value.tpe =:= typeOf[U256] => q"$value.bint"
      case _ if value.tpe =:= typeOf[Address] => q"$value.bint"
      case _ if value.tpe =:= typeOf[AddressPayable] => q"$value.bint"
      case TypeRef(pre, sym, List(arg)) if sym.fullName == symbolOf[Arr.type].fullName => q"_root_.scala.Array.from( $value.theArgs.map(x => ${ scalaToHeadlong(internal.setType(q"x", arg), false, pos) }) )"
      case TypeRef(pre, sym, List()) if structs.contains(sym) =>
        val tup = TermName("tup$macro$" + {intermediateCtr += 1; intermediateCtr})
        q"val $tup = $value; new _root_.com.esaulpaugh.headlong.abi.Tuple( ..${ structs(sym).map { vd =>
          scalaToHeadlong(internal.setType(q"$tup.${vd.symbol.name.toTermName}", vd.symbol.info.resultType), true, NoPosition)
        } } )"
      case TypeRef(pre, sym, args) =>
        //q"$value.toString()" // TODO
        sys.error(s"fullname (${sym.fullName}) ${value.tpe}")
    })

    def scalaToHeadlongType(tpe: Type): Tree = tpe.dealias match {
      case _ if tpe =:= typeOf[AnyRef] => tq"_root_.scala.Array[_root_.scala.Byte]"
      case _ if tpe =:= typeOf[String] => tq"_root_.scala.Predef.String"
      case _ if tpe =:= typeOf[Unit] => tq"_root_.scala.Unit"
      //case _ if tpe =:= typeOf[scala.Int] => tq"_root_.scala.Int"
      case _ if tpe =:= typeOf[Boolean] => tq"_root_.scala.Boolean"
      case _ if tpe =:= typeOf[U8] => tq"_root_.scala.Int" //tq"_root_.java.lang.Integer"
      case _ if tpe =:= typeOf[U16] => tq"_root_.scala.Int"
      case _ if tpe =:= typeOf[U32] => tq"_root_.scala.Long"
      case _ if tpe =:= typeOf[U64] => tq"_root_.java.math.BigInteger"
      case _ if tpe =:= typeOf[U256] => tq"_root_.java.math.BigInteger"
      case _ if tpe =:= typeOf[Address] => tq"_root_.java.math.BigInteger"
      case _ if tpe =:= typeOf[AddressPayable] => tq"_root_.java.math.BigInteger"

      case TypeRef(pre, sym, List(typ, len)) if sym.fullName == symbolOf[StaticArray[_,_]].fullName => tq"_root_.scala.Array[${scalaToHeadlongType(typ)}]"
      case TypeRef(pre, sym, List(arg)) if sym.fullName == symbolOf[Arr.type].fullName => tq"_root_.scala.Array[${scalaToHeadlongType(arg)}]"
      case TypeRef(pre, sym, List(arg)) if sym.fullName == symbolOf[contract.type].fullName + ".M" => tq"_root_.scala.Unit" //tq"_root_.com.esaulpaugh.headlong.abi.Tuple"
      case TypeRef(pre, sym, List(arg)) if sym.fullName == symbolOf[Contract[_]].fullName => tq"_root_.scala.Unit" //tq"_root_.com.esaulpaugh.headlong.abi.Tuple"
      case TypeRef(pre, sym, List(arg)) if sym.fullName == symbolOf[Client[_]].fullName => tq"_root_.scala.Unit" //tq"_root_.com.esaulpaugh.headlong.abi.Tuple" // NOPE ?
      case TypeRef(_, sym, _) if structs.contains(sym) => tq"_root_.com.esaulpaugh.headlong.abi.Tuple" // scalaToHeadlongType(arg)
      case TypeRef(_, sym, _) => sys.error(s"fullname (${sym.fullName}) $tpe")
    }

    def headlongToScala(resultType: Type, intermediateValue: Tree, pos: Position): Tree = resultType.dealias match {
      case TypeRef(pre, sym, List(arg)) if sym.fullName == symbolOf[Client[_]].fullName =>
        q"${headlongToScala(arg, intermediateValue, pos)}" // NOPE
      case TypeRef(pre, sym, List(argTpe))
      if sym.fullName == symbolOf[Contract[_]].fullName
      || sym.fullName == symbolOf[contract.M[_]].fullName =>

        //val outSig  = back.solidity.gen(TRecord(Seq((Word("",0,0), translateBareType(resultType)))), false)
        //  .replaceAllLiterally("address payable", "address").replaceAllLiterally(" ", "")
        //val argSigl = back.solidity.gen(TRecord(Seq((Word("",0,0), translateBareType(argType)))), false)
        //  .replaceAllLiterally("address payable", "address").replaceAllLiterally(" ", "")
        //val argSig  = argSigl.substring(1, argSigl.length-1)
        val sig = prisma.back.solidity.gen(translateTypeFilterUnit(argTpe), false)._1.replace("address payable", "address").replace(" ", "")
        val tuple = TermName(c.freshName("tuple"))
        val tuple0 = q"""$tuple.get(0).asInstanceOf[${ scalaToHeadlongType(argTpe) }]"""

        val res = q"""
            ..${flat(intermediateValue)}
            val $tuple: _root_.com.esaulpaugh.headlong.abi.Tuple = trampoline($sig)
            new _root_.prisma.Prisma.Contract(${headlongToScala(argTpe, tuple0, pos)})"""
        atPos(pos)(res)

      //case TypeRef(pre, sym, List(arg)) if sym.fullName == "macros.MacroDef.Box" => q"_root_.macros.MacroDef.Box(${HeadlongToScala(arg, i)})"
      case _ if resultType =:= typeOf[AnyRef] => q"$intermediateValue"
      case _ if resultType =:= typeOf[String] => q"new String($intermediateValue)"
      case _ if resultType =:= typeOf[Unit] => q"()"
      //case _ if resultType =:= typeOf[scala.Int] => q"$intermediateValue"
      case _ if resultType =:= typeOf[Boolean] => q"$intermediateValue"
      case _ if resultType =:= typeOf[U8] => q"new ${symbolOf[U8]}(_root_.java.math.BigInteger.valueOf($intermediateValue.toLong))"
      case _ if resultType =:= typeOf[U16] => q"new ${symbolOf[U16]}(_root_.java.math.BigInteger.valueOf($intermediateValue.toLong))"
      case _ if resultType =:= typeOf[U32] => q"new ${symbolOf[U32]}(_root_.java.math.BigInteger.valueOf($intermediateValue.toLong))"
      case _ if resultType =:= typeOf[U64] => q"new ${symbolOf[U64]}($intermediateValue)"
      case _ if resultType =:= typeOf[U256] => q"new ${symbolOf[U256]}($intermediateValue)"
      case _ if resultType =:= typeOf[Address] => q"new ${symbolOf[Address]}($intermediateValue)"
      case _ if resultType =:= typeOf[AddressPayable] => q"new ${symbolOf[AddressPayable]}($intermediateValue)"

      //case TypeRef(pre, sym, List(k,v)) if sym.fullName == symbolOf[Mapping[_,_]].fullName => q"$intermediateValue"
      case TypeRef(pre, sym, List(arg, len)) if sym.fullName == symbolOf[StaticArray[_,_]].fullName =>
        val ConstantType(Constant(vlen: Int)) = len
        q"${termSymbolOf[StaticArray.type]}.fromSeq( $intermediateValue .map(x => ${ headlongToScala(arg, q"x", pos) }).toSeq )(new ValueOf[$len]($vlen))"
      case TypeRef(pre, sym, List(arg)) if sym.fullName == symbolOf[Arr[_]].fullName => q"${termSymbolOf[Arr.type]}.fromSeq( $intermediateValue .map(x => ${ headlongToScala(arg, q"x", pos) }).toSeq )"
      case TypeRef(pre, sym, args) if structs.contains(sym) =>
        val tup = TermName("tup$macro$" + {intermediateCtr += 1; intermediateCtr})
        q"val $tup = $intermediateValue; new ${sym.name.toTypeName}(..${ structs(sym).zipWithIndex.map { case (valdef, i) =>
          headlongToScala(valdef.symbol.info.resultType, q"$tup.get($i).asInstanceOf[${scalaToHeadlongType(valdef.symbol.info.resultType)}]", pos)
        }})"
      case TypeRef(pre, sym, args) => sys.error(s"fullname (${sym.fullName}) $resultType")
    }

    def generateInOutSolidityTypeSignature(function: MethodSymbol): (String, String) = {
      val outtype = TRecord(translateTypeFilterUnit(function.returnType).lst)
      val fun = function.name.toString
      val inSig = s"$fun(${function.paramLists.head.map(x => prisma.back.solidity.gen(translateType(x.info), false)._1) mkString ","})"
      val outSig = prisma.back.solidity.gen(outtype, false)._1 // term.tpe
      (inSig.replace("address payable", "address").replace(" ", ""),
       outSig.replace("address payable", "address").replace(" ", ""))
    }

    def translateTypeFilterUnit(tpe: Type): TRecord = {
      val tpe1 = translateType(tpe)
      if (tpe1 == TRecord(Seq())) TRecord(Seq())
      else TRecord(Seq((Word("",0,0), tpe1)))
    }

    def translateType(tpe: Type): prisma.mid.Tpe = tpe.dealias match {
      case _ if structs.contains(tpe.typeSymbol) => TRecord(structs(tpe.typeSymbol).map { vd => (Word("",0,0), translateType(if (vd.symbol.isMethod) vd.symbol.asMethod.returnType else vd.symbol.info)) })
      case TypeRef(pre, sym, List()) if isContractSym(sym) => TSimple(Word(sym.name.toString, 0,0)) // TODO should be top methinks?
      case _ if tpe =:= typeOf[Unit] => TRecord(Seq())
      case _ if tpe =:= typeOf[Boolean] => TSimple(Word("bool",0,0))
      //case _ if tpe =:= typeOf[Int] => TSimple(Word("int32",0,0))
      case _ if tpe =:= typeOf[AnyRef] => TSimple(Word("bytes",0,0))
      case _ if tpe =:= typeOf[String] => TSimple(Word("string",0,0))
      case _ if tpe =:= typeOf[U8] => TSimple(Word("uint8",0,0))
      case _ if tpe =:= typeOf[U16] => TSimple(Word("uint16",0,0))
      case _ if tpe =:= typeOf[U32] => TSimple(Word("uint32",0,0))
      case _ if tpe =:= typeOf[U64] => TSimple(Word("uint64",0,0))
      case _ if tpe =:= typeOf[U256] => TSimple(Word("uint",0,0))
      case _ if tpe =:= typeOf[Address] => TSimple(Word("address",0,0))
      case _ if tpe =:= typeOf[AddressPayable] => TSimple(Word("payable",0,0))
      case TypeRef(pre, sym, args) if sym.fullName == symbolOf[Mapping[_,_]].fullName => TMapping(translateType(args(0)), translateType(args(1)))
      case TypeRef(pre, sym, List(arg, len)) if sym.fullName == symbolOf[StaticArray.type].fullName => TArray(translateType(arg.asInstanceOf[Type]), Word(len.toString,0,0))
      case TypeRef(pre, sym, List(arg)) if sym.fullName == symbolOf[Arr.type].fullName => TArray(translateType(arg.asInstanceOf[Type]), Word("",0,0))
      case TypeRef(pre, sym, List(arg)) if sym.fullName == symbolOf[prisma.Prisma.contract.M[_]].fullName => TRecord(Seq()) // TSimple(Word("Closure",0,0))
      case TypeRef(pre, sym, List(arg)) if sym.fullName == symbolOf[prisma.Prisma.Contract[_]].fullName => TRecord(Seq()) // TSimple(Word("Closure",0,0))
      case TypeRef(pre, sym, List(arg)) if sym.fullName == symbolOf[prisma.Prisma.contract.N[_]].fullName => TRecord(Seq()) // TSimple(Word("Closure",0,0))
      case TypeRef(pre, sym, List(arg)) if sym.fullName == symbolOf[prisma.Prisma.Client[_]].fullName => TRecord(Seq()) // TSimple(Word("Closure",0,0))
      //case _ => println(sym.fullName); ???
      case TypeRef(pre, sym, List(a, b)) if sym.fullName == "scala.Tuple2" =>
        TRecord(List(
          (Word("",0,0), translateType(a)),
          (Word("",0,0), translateType(b))
        ))
      case TypeRef(pre, sym, List(from, to)) if sym.fullName == "scala.Function1" => TSimple(Word("Closure",0,0))
      case TypeRef(pre, sym, List(arg)) => TSimple(Word(s"/* ($pre . ${sym.fullName} [$arg] */",0,0))
      case TypeRef(pre, sym, List(arg)) => TSimple(Word(s"/* $pre . ${sym.fullName} */",0,0))
      case x@TypeRef(pre, sym, args) =>
        println((x, args, sym.fullName,
                 symbolOf[prisma.Prisma.contract.type].fullName + ".N",
                 sym.fullName == symbolOf[prisma.Prisma.Client[_]].fullName))
        TSimple(Word(s"/* ${showRaw(x)} // $x // ${x.sym.toString} */",0,0))
    }

//    case TypeRef(pre, sym, List(arg)) if sym.fullName == "macros.MacroDef.contract.M" || sym.fullName == "macros.MacroDef.Contract" =>
//    mid.helper.trampolineTRecord
//    case TypeRef(pre, sym, List(arg)) if sym.fullName == "macros.MacroDef.contract.N" || sym.fullName == "macros.MacroDef.Client" =>
//    mid.helper.trampolineTRecord
//    case TypeRef(pre, sym, List(arg)) if structs.isEmpty &&
//      (sym.fullName == "macros.MacroDef.contract.M" || sym.fullName == "macros.MacroDef.Contract" ||
//        sym.fullName == "macros.MacroDef.contract.N" || sym.fullName == "macros.MacroDef.Client") =>
//    mid.helper.trampolineTRecord

    def translateBareType(tpe: Type): TRecord = tpe match {
      case _ => TRecord(Seq((Word("",0,0), translateType(tpe))))
    }

    def translateValDef(vparams: Seq[ValDef]): TRecord = TRecord(vparams.collect {
      case vd@ValDef(mods, name, tpt, rhs) if name.toString != "userdata" =>
        (mkWord(name.toString, vd.pos), translateType(tpt.tpe))
    })
  }

  lazy val defaultTranslatorMap: Map[Symbol, List[ValDef]] = Map(
    /*
    symbolOf[contract.type].info.decl(TypeName("M")) -> List(
      internal.valDef(freshSymbol("action", typeOf[U32], NoPosition, true)._1),
      internal.valDef(freshSymbol("userdata", typeOf[Any], NoPosition, true)._1)
    ),
    symbolOf[contract.type].info.decl(TypeName("N")) -> List(
      internal.valDef(freshSymbol("action", typeOf[U32], NoPosition, true)._1),
      internal.valDef(freshSymbol("userdata", typeOf[Any], NoPosition, true)._1)
    ),
    */
    typeOf[Client[_]].typeSymbol -> List(), /*List(
      internal.valDef(freshSymbol("action", typeOf[U32], NoPosition, true)._1),
      internal.valDef(freshSymbol("userdata", typeOf[Any], NoPosition, true)._1)
    ),*/
    typeOf[Contract[_]].typeSymbol -> List(), /*List(
      internal.valDef(freshSymbol("action", typeOf[U32], NoPosition, true)._1),
      internal.valDef(freshSymbol("userdata", typeOf[Any], NoPosition, true)._1)
    ),*/ // mid.helper.trampolineTRecord,
  )

}
