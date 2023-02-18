package com.giyeok.jparser.metalang3.codegen

case class CodeBlob(code: String, required: Set[String]) {
  def indent(width: Int = 2): CodeBlob =
    copy(code.linesIterator.toList.map(line => (" " * width) + line).mkString("\n"))

  def +(other: CodeBlob): CodeBlob = CodeBlob(code + "\n" + other.code, required ++ other.required)

  def wrap(pre: CodeBlob, post: CodeBlob, requiredImports: Set[String]): CodeBlob =
    CodeBlob(pre.code + "\n" + indent().code + "\n" + post.code,
      requiredImports ++ pre.required ++ post.required)

  def wrap(pre: String, post: String): CodeBlob = copy(pre + "\n" + indent().code + "\n" + post)

  def wrapBrace(): CodeBlob = wrap(CodeBlob("{", Set()), CodeBlob("}", Set()), Set())
}

case class ExprBlob(prepares: List[String], result: String, required: Set[String]) {
  def withBraceIfNeeded: String =
    if (prepares.isEmpty) result else "{\n" + prepares.map("  " + _).mkString("\n") + "\n}"
}

object ExprBlob {
  def code(code: String) = ExprBlob(List(), code, Set())
}
