package com.giyeok.jparser.metalang3a

import com.giyeok.jparser.metalang2.generated.MetaGrammar3Ast

class ErrorCollector {
  private var errors = List[ErrorMessage]()

  def isClear: Boolean = errors.isEmpty

  def addError(message: String): Unit = {
    errors :+= ErrorMessage(message, None)
  }

  def addError(message: String, location: MetaGrammar3Ast.ASTNode): Unit = {
    errors :+= ErrorMessage(message, Some(location))
  }

  def collectedErrors: CollectedErrors = CollectedErrors(errors)
}

case class CollectedErrors(errors: List[ErrorMessage]) {
  val isClear: Boolean = errors.isEmpty
}

case class ErrorMessage(message: String, astNode: Option[MetaGrammar3Ast.ASTNode])
