package prisma.meta

trait Patterns extends MacroTop {

  import c.universe._

  object MyModuleDef {
    def unapply(klass: ModuleDef): Option[(Modifiers, TermName, List[Tree], ValDef, List[Tree], Tree, List[Tree])] = {
      val ModuleDef(mods, name, Template(parents, self, stats)) = klass
      val (vparams, ctor, rest) = splitTemplate(stats)
      Some((mods, name, parents, self, vparams, ctor, rest))
    }
  }

  object MyClassDef {
    def unapply(klass: ClassDef): Option[(Modifiers, TypeName, List[TypeDef], List[Tree], ValDef, List[Tree], Tree, List[Tree])] = {
      val ClassDef(mods, name, tparams, Template(parents, self, stats)) = klass
      val (vparams, ctor, rest) = splitTemplate(stats)
      Some((mods, name, tparams, parents, self, vparams, ctor, rest))
    }
  }

  def splitTemplate(stats: List[Tree]): (List[ValDef], DefDef, List[Tree]) = {
    val ctorIdx = stats.indexWhere { case DefDef(_, termNames.CONSTRUCTOR, _, _, _, _) => true; case _ => false }
    val (args: List[ValDef @unchecked], (ctor: DefDef) :: rest) = stats.splitAt(ctorIdx)
    (args, ctor, rest)
  }

  object AsBlock {
    def unapply(x: Tree): Option[(List[Tree], Tree)] = Some(x match {
      case Block(b, e) =>
        val AsBlock(eb, ee) = e
        (b ++ eb, ee)
      case e =>
        (List(), e)
    })
  }

  object While {
    def apply(nme: TermName, cnd: Tree, thn: Tree): Tree =
      LabelDef(nme, List(), If(cnd, Block(List(thn), Apply(Ident(nme), List())), q"()"))

    def unapply(tree: Tree): Option[(TermName, Tree, List[Tree])] = tree match {
      case LabelDef(nme, List(), If(cnd, Block(thn, Apply(Ident(nme2), List())), q"()")) if nme.toString == nme2.toString =>
        Some((nme, cnd, thn))
      case _ => None
    }
  }

  /** Example:
   *
   * q"hello bello" match {
   * case tree withAttrs (s,t,p) =>
   * tree withAttrs (s,t,p)
   * }
   *
   * */
  implicit class WithAttrs(val tree: Tree) {
    def withAttrs(stp: (Symbol, Type, Position)): tree.type =
      atPos(stp._3)(c.internal.setType(c.internal.setSymbol(tree, stp._1), stp._2))
    def withAttrs(tp: (Type, Position)): tree.type =
      atPos(tp._2)(c.internal.setType(tree, tp._1))
    def withAttr(symbol: Symbol, tpe: Type): tree.type =
      c.internal.setType(c.internal.setSymbol(tree, symbol), tpe)
  }

  object withAttrs {
    def unapply(tree: Tree): Option[(Tree, Symbol, Type, Position)] =
      Some((tree, tree.symbol, tree.tpe, tree.pos))
  }

}
