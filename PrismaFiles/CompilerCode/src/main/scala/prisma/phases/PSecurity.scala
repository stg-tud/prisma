package prisma.phases

import prisma.{FLAGS, Prisma}
import prisma.meta.{BangNotation, Phases}

import scala.collection.mutable

trait PSecurity extends Phases {
  import c.universe._

  private def doit(state_name: (String, String), cond: Option[Tree], tree: Tree, symbol: Symbol): Tree = {
    securityCtr += 1

    val (state, name) = state_name
    securityIdToFuncname(state.substring(2)) = name
    securityFuncnameToId(name) = state.substring(2)

    val controlGuard = q"$termState == $termToUint($state).u32"
    val guard =
      if (cond.isEmpty) q"$controlGuard"
      else q"$controlGuard && ${cond.get} == $termSender"

    if (FLAGS.GUARD_ERROR_MESSAGES)
      q"""${internal.valDef(symbol, tree)}
          ${Literal(Constant(name))};
          if (!$guard) _root_.scala.Predef.require(false,
             "got (state=" + $termState + ", who=" + ${cond.getOrElse(q""""*"""")} + "), " +
             "but expected (" + ${Literal(Constant(state))} + ", msg.sender=" + $termSender + ")"
          )
          ${Ident(symbol.name.toTermName)}"""
    else
      q"""${internal.valDef(symbol, tree)}
          ${Literal(Constant(name))};
          _root_.scala.Predef.require($guard, "access control or flow guard failed")
          ${Ident(symbol.name.toTermName)}"""

  }

  val securityIdToFuncname: mutable.Map[String, String] = mutable.Map[String, String]()
  val securityFuncnameToId: mutable.Map[String, String] = mutable.Map[String, String]()
  var securityCtr = 0
  protected val clientToRaceClientWithAccessGuard: Phase = (tree: Tree) => {
    presTransform(tree) (that => {
      case tree@ValDef(mods, varname, tt, q"$bang.apply[$ta,$tb]($raceClient.apply[$tc](${Literal(Constant(name: String))}, $body))")
      if isType[Prisma.raceClient.type](raceClient)
      && isType[BangNotation.↓.type](bang) =>
        val state = org.web3j.crypto.Hash.sha3(name).substring(0, 2+8)
        val body2 = genClientCheck(None, body)
        val Block(b,e) = doit(
          state -> name,
          None,
          q"""$bang.apply[$ta,$tb]($termRaceClient.apply[$tc](${Literal(Constant(name))}, $body2))""",
          tree.symbol)
        q"..$b"

      case tree@ValDef(mods, varname, tt, q"$bang.apply[$ta,$tb]($client.apply[$tc](${Literal(Constant(name: String))}, $cond, $body))")
      if isType[Prisma.client.type](client)
      && isType[BangNotation.↓.type](bang) =>
        val state = org.web3j.crypto.Hash.sha3(name).substring(0, 2+8)
        val body2 = genClientCheck(Some(cond), body)
        val Block(b,e) = doit(
          state -> name,
          Some(cond),
          q"""$bang.apply[$ta,$tb]($termRaceClient.apply[$tc](${Literal(Constant(name))}, $body2))""",
          tree.symbol)
        q"..$b"

//      case tree@ValDef(mods, varname, tt, q"$bang.apply[$ta,$tb]($client.apply[$tc](${Literal(Constant(name: String))}, $cond, $body))")
//      if isType[Prisma.moneyClientN.type](client) && isType[BangNotation.↓.type](bang)
//      =>
//        val name = "step_payable_" + securityCtr.toHexString
//        that.rec(treeCopy.ValDef(tree, mods, varname, tt, q"$bang.apply[$ta,$tb]($client.apply[$tc](${Literal(Constant(name: String))}, $cond, $body))"))

//      case tree@ValDef(mods, varname, tt, q"$bang.apply[$ta,$tb]($client.apply[$tc]($cond, $body))")
//      if isType[Prisma.client.type](client) && isType[BangNotation.↓.type](bang)
//      =>
//        val name = "step_" + securityCtr.toHexString
//        that.rec(treeCopy.ValDef(tree, mods, varname, tt, q"$bang.apply[$ta,$tb]($client.apply[$tc](${Literal(Constant(name: String))}, $cond, $body))"))

      case tree@q"$bang.apply[$ta,$tb]($raceClient.apply[$tc](${Literal(Constant(name: String))}, $body))"
      if isType[Prisma.raceClient.type](raceClient)
      && isType[BangNotation.↓.type](bang) =>
        val state = org.web3j.crypto.Hash.sha3(name).substring(0, 2+8)
        val body2 = genClientCheck(None, body)
        doit(
          state -> name,
          None,
          q"""$bang.apply[$ta,$tb]($termRaceClient.apply[$tc](${Literal(Constant(name))}, $body2))""",
          freshSymbol("tmp", tree, freshen=false)._1)

      case tree@q"$bang.apply[$ta,$tb]($client.apply[$tc](${Literal(Constant(name: String))}, $cond, $body))"
      if isType[Prisma.client.type](client)
      && isType[BangNotation.↓.type](bang) =>
        val state = org.web3j.crypto.Hash.sha3(name).substring(0, 2+8)
        val body2 = genClientCheck(Some(cond), body)
        doit(
          state -> name,
          Some(cond),
          q"""$bang.apply[$ta,$tb]($termRaceClient.apply[$tc](${Literal(Constant(name))}, $body2))""",
          freshSymbol("tmp", tree, freshen=false)._1)

//      case q"$bang.apply[$ta,$tb]($client.apply[$tc]($cond, $body))" withAttrs (s,t,p)
//      if isType[Prisma.client.type](client) && isType[BangNotation.↓.type](bang)
//      =>
//        val name = "step_" + securityCtr.toHexString
//        that.rec(q"$bang.apply[$ta,$tb]($client.apply[$tc](${Literal(Constant(name: String))}, $cond, $body))" withAttrs (s,t,p))
    })
  }

  private def genClientCheck(cond: Option[Tree], fun: Tree): Tree =
    if (cond.isEmpty) fun
    else {
      val q"{(..$args) => ..$body}" = fun
      q"""(..$args) => { if (${cond.get} === this.acc$$.credentials.getAddress.p) { ..$body } else null } """
    }
}
