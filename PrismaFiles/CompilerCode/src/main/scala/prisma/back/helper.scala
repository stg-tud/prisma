package prisma.back

object helper {

  def bracket(s: Seq[String]): String =
    " {\n  " + s.filter(_.nonEmpty).mkString("\n").replace("\n", "\n  ") + "\n}"

}
