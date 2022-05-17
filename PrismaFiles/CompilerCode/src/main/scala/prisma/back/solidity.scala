package prisma.back

import prisma.FLAGS
import prisma.mid._

import scala.collection.mutable

object solidity {

  var fresh = 0

  type SrcMap = Seq[(Int, Int, Int, Int)]
  def gen(expr: Module, necessity: Necessity): (String, SrcMap) = {
    val w = new Writer()
    w.gen(expr, necessity)
    (w.b.toString, w.srcmap.toSeq)
  }
  def genExpr(expr: Expr): (String, SrcMap) = genExpr(expr, false)
  def genExpr(expr: Expr, stmt: Boolean): (String, SrcMap) = {
    val w = new Writer()
    w.genExpr(expr, stmt)
    (w.b.toString, w.srcmap.toSeq)
  }
  def gen(expr: Tpe, first: Boolean = true): (String, SrcMap) = {
    val w = new Writer()
    w.gen(expr, first)
    (w.b.toString, w.srcmap.toSeq)
  }

  private[solidity] class Writer {
    val b = new StringBuffer()
    val srcmap: mutable.Buffer[(Int, Int, Int, Int)] = mutable.Buffer() // sol(i - j) <-> scala(i - j)
    def foreachSep[X](xs: Iterable[X], sep: => Unit)(f: X => Unit): Unit = {
      val it = xs.iterator
      if (it.hasNext) f(it.next())
      while (it.hasNext) { sep; f(it.next()) }
    }
    def fmt(s: String, fs: () => Unit *) = {
      val ss = s.split("%s")
      ss.indices.dropRight(1) foreach { i =>
        b append ss(i)
        fs(i)()
      }
      b append ss.last
    }
    var indent = 0
    def newline(): Unit = b append ("\n" + "  " * indent)
    def potline[X](f: X => Unit): X => Unit = x => { val tmp = b.length(); f(x); if (tmp != b.length()) newline() }
    def bracket(f: => Unit): Unit = {
      b append " {"
      indent += 1
      newline()
      f
      newline()
      indent -= 1
      b.setCharAt(b.length()-2, '}')
    }

    def gen(expr: Module, necessity: Necessity): Unit = {
      b append s"""// SPDX-License-Identifier: UNLICENSED
                 |// ${expr.comment}
                 |pragma solidity >=0.8.0;
                 |pragma abicoder v2;
                 |""".stripMargin + "\n"
      foreachSep(expr.body, b append "\n\n")(c => gen(c, necessity))
    }

    private def gen(expr: Contract, necessity: Necessity): Unit = {
      b append "contract "
      genExpr(expr.name)
      b append " "
      bracket {
        expr.defs foreach potline(gen)
        genCtor(expr.arg, expr.defs); newline()
        expr.defs foreach potline(genAccessor)
        b append utils(necessity)
        if (FLAGS.GUARD_ERROR_MESSAGES)
          b append debugUtils
      }
    }

    private def utils(necessity: Necessity): String =
      s"""
        |  ${if (necessity.state) "uint32 public state;" else ""}
        |  ${if (necessity.reqArgs) "bytes public reqArgs;" else ""}
        |  ${if (necessity.storage) "bytes public store;" else ""}
        |  ${if (necessity.mk && necessity.flatmap) "struct Closure { uint32 id; bytes data; }" else ""}
        |  ${if (necessity.mk) s"""
        |  function mk(Closure memory response) private {
        |    ${if (necessity.reqArgs) "reqArgs = response.data;" else ""}
        |    ${if (necessity.storage) "store = hex\"\";" else ""}
        |    state = 0;
        |  }""".stripMargin else ""}
        |  ${if (necessity.flatmap) s"""
        |  function flatMap(Closure memory request, Closure memory callback) private {
        |    ${if(necessity.reqArgs) "reqArgs = request.data;" else ""}
        |    ${if(necessity.storage) "store = callback.data;" else ""}
        |    state = callback.id;
        |  }""".stripMargin else ""}
        |""".stripMargin

    private def debugUtils: String =
      """
        |  function toascii(uint8 x) public pure returns (uint8) { return 48 + (x >= 10? x + 7 : x); }
        |  function tohex(bytes memory input) public pure returns (string memory) {
        |    bytes memory output = new bytes(input.length * 2);
        |    uint j = 0; while (j < input.length) {
        |      output[j*2+1] = bytes1(toascii(uint8(input[j] & 0x0f) >> 0));
        |      output[j*2+0] = bytes1(toascii(uint8(input[j] & 0xf0) >> 4));
        |      j++;
        |    }
        |    return string(output);
        |  }
        |""".stripMargin

    private def genCtor(arg: TRecord, defs: Seq[Def]): Unit = {
      b append "constructor"
      gen(arg)
      bracket { defs foreach {
        case Prop(name, TMapping(_, _), _, _) => // TODO
          // nothing if mapping
        case Prop(name, TArray(_, sz), Seq(Default(_)), _) if sz.str != "" =>
          // nothing if static array
        case Prop(name, tpe, Seq(App(Word("Array", _,_), Tuple(Seq()))), _) =>
          // nothing if dynamic EMPTY array
        case Prop(name, tpe, init, mods) =>
          init.init foreach { expr =>
            genExpr(expr)
            b append ";"
            newline()
          }
          if (name.str != "_") {
            genExpr(name)
            b append " = "
          }
          genExpr(init.last)
          b append ";"
          newline()
        case _ => false
      } }
    }

    private def genAccessor(defs: Def): Unit = defs match {
      case Prop(name, tpe, init, mods) if needsAccessor(mods, tpe, name.str) =>
        gen(Func(name.copy(str = "get_" + name.str),
          TRecord(Seq()),
          TRecord(Seq((Word("result",0,0), tpe))),
          Seq(
            Decl(Word("tmp", 0,0), tpe, name),
            App(Word("return",0,0), Word("tmp", 0,0))
          ),
          Seq("public", "view")))
      case _ =>
    }

    private def gen(expr: Def): Unit = expr match {
      case Struct(name, defs) =>
        b append "struct "
        genExpr(name)
        bracket { foreachSep(defs.lst, newline()) { case (name, tpe) =>
          gen(tpe, false)
          b append " "
          genExpr(name)
          b append ";"
        } }
      case Const(name, tpe, init) =>
        gen(tpe, false)
        b append " constant public "
        genExpr(name)
        b append " = "
        genExpr(init)
        b append ";"
        newline()
      case Prop(Word("_", _, _), tpe, _, _) =>
        // nothing
      case Prop(name, tpe, _, mods) =>
        gen(tpe, false)
        b append (if (needsAccessor(mods, tpe, name.str)) " private " else mods.mkString(" ", " ", " "))
        genExpr(name)
        b append ";"
      case Func(name, arg, ret, body, mod) =>
        b append "function "
        genExpr(name)
        gen(arg)
        b append " " + mod.map(" " + _).mkString
        if (ret.lst.nonEmpty) { b append " returns "; gen(ret) }
        genBracket(body)
      case Fallback(name, body) =>
        b append genExpr(name)
        b append " external payable "
        genBracket(body)
      case Event(name, arg) =>
        b append "event "
        genExpr(name)
        gen(arg)
        b append ";"
    }

    private def genBracket(body: Seq[Expr]): Unit =
      bracket { foreachSep(body, newline()) (genExpr(_, true)) }

    private def genOp(op: String): Unit = op match {
      case "+" | "-" | "*" | "/" | "%" => b append op
      case "==" | "!=" | "<" | "<=" | ">" | ">=" => b append op
      case "===" => b append "=="
      case "&&" | "||" => b append op
      case _ => sys.error("missing implementation for binop " + op)
    }

    private def default(expr: Tpe): Unit = expr match {
      case TSimple(Word("uint", i, j)) => genExpr(Word("0", i, j))
      case TSimple(n) => genExpr(n); b append "()"
      //case TComplex(mod, tpe) => default(tpe)
      case TArray(tpe, Word("", _, _)) => sys.error("default of array without size") // "[]"
      case TArray(tpe, sz) =>
        b append "new "; gen(tpe, false); b append "[]("; genExpr(sz); b append ")"
        // "[" + (1 to sz.str.toInt).map(x => default(tpe)).mkString(", ") + "]"
      case TRecord(lst) => ???
      case TMapping(from, to) => sys.error("default of mapping does not exist")
    }

    def genExpr(expr: Expr, stmt: Boolean = false): Unit = {
      expr match {
        case Default(t) => default(t)
        case Tuple(exprs) => foreachSep(exprs, b append ", ")(genExpr(_, false))
        case Decl(name, tpe, Default(_)) =>
          gen(tpe); b append " "; genExpr(name, false)
        case Decl(Word(s"($names)",_,_), TRecord(tpelst), body) =>
          b append "("
          foreachSep(names.split(", ") zip tpelst, b append ", ") { case (n, (_, t)) =>
            gen(t)
            b append " "
            b append n
          }
          b append ") = "
          genExpr(body)

        case Decl(name, tpe, body) =>
          gen(tpe); b append " "; genExpr(name, false); b append " = "; genExpr(body, false)
        case Asgn(Tuple(Seq()), body) => genExpr(body, false)
        case Asgn(name, body) =>
          b append "("; genExpr(name); b append ") = "; genExpr(body, false)
        case Dot(expr, word) =>
          genExpr(expr, false); b append "."; genExpr(word, false)
        case App(Dot(x, op), y) if prisma.mid.helper.ops.contains(op.str) =>
          b append "("; genExpr(x, false); b append " "; genOp(op.str); b append " "; genExpr(y, false); b append ")"
        case App(r@Word("require", _, _), args) =>
          genExpr(r, false); b append "("; genExpr(args, false); b append ")"
        case App(k@Word("keccak256",_,_), args) =>
          b append "uint256("; genExpr(k, false); b append "(abi.encode("; genExpr(args, false); b append ")))"
        case App(k@Word("keccak256Packed",x1,x2), args) =>
          b append "uint256("; genExpr(Word("keccak256", x1, x2), false); b append "(abi.encodePacked("; genExpr(args, false); b append ")))"
        case App(Word("Array", _,_), args) =>
          b append "["; genExpr(args, false); b append "]"
        case App(fun, args) =>
          genExpr(fun, false); b append "("; genExpr(args, false); b append ")"
        case Get(x, idx) =>
          genExpr(x, false); b append "["; genExpr(idx, false); b append "]"
        case If(cond, body, Seq(i@If(_,_,_))) if stmt =>
          b append "if ("; genExpr(cond, false); b append ")"; genBracket(body); b append "else "; genExpr(i, true)
        case If(cond, Seq(Word("0",_,_)), alt) if stmt =>
          b append "if (!("; genExpr(cond, false); b append "))"; genBracket(alt)
        case If(cond, body, Seq(Word("0",_,_))) if stmt =>
          b append "if ("; genExpr(cond, false); b append ")"; genBracket(body)
        case If(cond, body, alt) if stmt =>
          b append "if ("; genExpr(cond, false); b append ")"; genBracket(body); b append "else"; genBracket(alt)
        case If(cond, Seq(body), Seq(alt)) if !stmt =>
          b append "("; genExpr(cond, false); b append " ? "; genExpr(body, false); b append " : "; genExpr(alt, false); b append ")"
        //    case If(cond, body, alt) => "if (%s)%s else%s".format(
        //      genExpr(cond), genBracket(body), genBracket(alt))
        case For(vari, tpe, from, to, body) =>
          fresh+=1
          gen(tpe)
          b append " upperMax" + fresh + " = "
          genExpr(to, false)
          b append "; for ("
          gen(tpe)
          genExpr(vari, false)
          b append " = "
          genExpr(from, false)
          b append "; "
          genExpr(vari, false)
          b append " < upperMax" + fresh + "; "
          genExpr(vari, false)
          b append " = "
          genExpr(vari, false)
          b append " + 1) "
          genBracket(body)
        case Loop(body) =>
          b append "while (true)"
          genBracket(body)
        case Break() => b append "break"
        case Word(name, 0, 0) =>
          if(name.startsWith("$indexed_")) b append name.replace("$indexed_","")
          else b append name
        case Word(name, i, j) =>
          if(name.startsWith("$indexed_")) {
            b append name.replace("$indexed_","")
            srcmap append ((b.length, b.length + name.length - 9, i, j))
          } else {
            b append name
            srcmap append ((b.length, b.length + name.length, i, j))
          }

          //b append "%s/*%s:%s*/".format(name, i, j)
        case EmitEvent(event) =>
          b append "emit "
          genExpr(event)
      }
      if (stmt && !expr.isInstanceOf[If] && !expr.isInstanceOf[For] && !expr.isInstanceOf[Loop])
        b append ";"
    }

    def gen(expr: Tpe, first: Boolean = true): Unit = expr match {
      case TRecord(lst) =>
        b append "("
        foreachSep(lst, b append ", ") { case (name, tpe) =>
          gen(tpe, first)
          if(name.str.startsWith("$indexed_")) { b append " indexed "; genExpr(name)}
          else if (name.str != "") { b append " "; genExpr(name) }
        }
        b append ")" // true false?
      case TArray(x, sz) if first => fmt("%s[%s] memory",
        ()=> gen(x, false), ()=> genExpr(sz))
      case TMapping(from, to) =>
        b append "mapping ("
        gen(from, false)
        b append "=>"
        gen(to, false)
        b append ")"
      case TArray(x, sz) => fmt("%s[%s]",
        ()=> gen(x, false), ()=> genExpr(sz))
      case TSimple(n) if n.str(0).isUpper => // TODO better guessing what is struct
        genExpr(n); if (first) b append " memory"
      case TSimple(n) if n.str == "uint" => genExpr(n.copy(str="uint256"))
      case TSimple(n) if n.str == "int" => genExpr(n.copy(str="int256"))
      case TSimple(n) if n.str.startsWith("uint") => genExpr(n)
      case TSimple(n) if n.str.startsWith("int") => genExpr(n)
      case TSimple(n) if n.str == "bool" => genExpr(n)
      case TSimple(n) if n.str == "payable" => genExpr(n.copy("address payable"))
      case TSimple(n) if n.str == "address" => genExpr(n)
      case TSimple(n) if n.str == "bytes" => genExpr(n); if (first) b append " memory"
      case TSimple(n) if n.str == "string" => genExpr(n); if (first) b append " memory"
      case TSimple(n) if n.str.startsWith("/*") =>
        b append n.str.substring(0,n.str.indexOf("*/ ")+3)
        try gen(TSimple(n.copy(str=n.str.substring(n.str.indexOf("*/ ")+3))), true)
        catch { case e: MatchError => b append s"/* ${e.getMessage()}  */" }
      // "ERROR /* only uint or struct-types supported */"
      case TSimple(n) => genExpr(n); if (first) b append " memory"
    }  }

  def needsAccessor(mods: Seq[String], tpe: Tpe, name: String): Boolean =
    mods.contains("public") && (tpe match {
      case TSimple(n) if n.str.startsWith("uint") || n.str == "string" => false
      case TMapping(_, _)   => true
      case _ if name == "_" => false
      case _ => true
    })
}
