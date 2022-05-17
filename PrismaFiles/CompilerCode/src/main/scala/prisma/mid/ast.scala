package prisma.mid

trait Necessity {
  val reqArgs: Boolean
  val storage: Boolean
  val state: Boolean
  val flatmap: Boolean
  val mk: Boolean
}
case class Module(comment: String, body: Seq[Contract])
case class Contract(name: Word, parents: Seq[Word], arg: TRecord, defs: Seq[Def])

sealed trait Def
case class Const(name: Word, tpe: Tpe, init: Expr) extends Def
case class Struct(name: Word, body: TRecord) extends Def
//case class Enum(name: Word, body: Seq[Def]) extends Def
case class Prop(name: Word, tpe: Tpe, init: Seq[Expr], mod: Seq[String]) extends Def
case class Func(name: Word, arg: TRecord, ret: TRecord, body: Seq[Expr], mod: Seq[String]) extends Def
case class Fallback(name: Word, body: Seq[Expr]) extends Def // TODO can this be expressed as a func with name equal "fallback" / "receive" ?
case class Event(name: Word, arg: TRecord) extends Def

sealed trait Tpe
case class TArray(inner: Tpe, size: Word) extends Tpe
case class TSimple(name: Word) extends Tpe
case class TRecord(lst: Seq[(Word, Tpe)]) extends Tpe
case class TMapping(from: Tpe, to: Tpe) extends Tpe

sealed trait Expr
case class Word(str: String, i: Int, j: Int) extends Expr      // x 0 1 2 3 + - * / == < > <= >= && || %
case class Default(tpe: Tpe) extends Expr                      // default<int[2]>  ==  [0,0]
case class Decl(vari: Word, tpe: Tpe, valu: Expr) extends Expr // int x = y
case class Asgn(vari: Expr, valu: Expr) extends Expr           // x := y
case class App(fun: Expr, args: Expr) extends Expr             // x(y)
case class Get(x: Expr, idx: Expr) extends Expr                // x[y]
case class Dot(x: Expr, y: Word) extends Expr                  // x.y
case class Tuple(lst: Seq[Expr]) extends Expr                  // x, y, z
case class If(cond: Expr, body: Seq[Expr], alt: Seq[Expr]) extends Expr
case class For(vari: Word, tpe:Tpe, from: Expr, to: Expr, body: Seq[Expr]) extends Expr
case class Loop(body: Seq[Expr]) extends Expr
case class Break() extends Expr
case class EmitEvent(event: Expr) extends Expr                 // emit E(a,b,c)

object helper {
  def exprMap(expr: Expr, f: PartialFunction[(Expr, Expr => Expr), Expr]): Expr = {
    def rec(e: Expr): Expr = exprMap(e, f)
    f.applyOrElse((expr, rec _), (_: (Expr, Expr => Expr)) => expr match {
      case Word(str, i, j) => Word(str, i, j)
      case Default(tpe: Tpe) => Default(tpe)
      case Tuple(exprs) => Tuple(exprs.map(rec))
      case Decl(vari, tpe, valu) => Decl(vari, tpe, rec(valu))
      case Asgn(vari, valu) => Asgn(rec(vari), rec(valu))
      case App(fun, args) => App(rec(fun), rec(args))
      case Get(x, idx) => Get(rec(x), rec(idx))
      case Dot(x, y) => Dot(rec(x), y)
      case If(cond, body, alt) => If(rec(cond), body.map(rec), alt.map(rec))
      case For(vari, tpe, from, to, body) => For(vari, tpe, rec(from), rec(to), body.map(rec))
      case Break() => ???
      case Loop(body) => ???
    })
  }

  val nam2op: Map[String, String] = Map(
    "$plus" -> "+", "$minus" -> "-", "$times" -> "*", "$div" -> "/", "$mod" -> "%",
    "===" -> "===", "$eq$eq" -> "==", "$ne" -> "!=", "$lt" -> "<", "$le" -> "<=", "$gt" -> ">", "$ge" -> ">=",
    "$amp$amp" -> "&&", "$bar$bar" -> "||")
  val ops: Set[String] = nam2op.values.toSet[String]

  //val trampolineTRecordString = "(uint32,bytes)"
  //val trampolineTRecord: TRecord = TRecord(Seq(
  //  (Word("", 0, 0), TSimple(Word("uint32", 0, 0))),
  //  (Word("", 0, 0), TSimple(Word("bytes", 0, 0))),
  //))
}
