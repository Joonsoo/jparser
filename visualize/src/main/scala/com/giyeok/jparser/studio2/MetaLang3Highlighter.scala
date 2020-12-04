package com.giyeok.jparser.studio2

import com.giyeok.jparser.metalang3a.generated.MetaLang3Ast
import com.giyeok.jparser.metalang3a.generated.MetaLang3Ast.PExpr
import com.giyeok.jparser.studio2.CodeEditor.CodeStyle

class MetaLang3Highlighter(val editor: CodeEditor) {
  def highlightType(typ: MetaLang3Ast.TypeDesc): Unit = {

  }

  def highlightPExpr(pexpr: PExpr): Unit = pexpr match {
    case MetaLang3Ast.TypedPExpr(body, typ) =>
      highlightPExpr(body)
      highlightType(typ)
    case MetaLang3Ast.TernaryOp(cond, ifTrue, ifFalse) =>
      highlightPExpr(cond)
      highlightPExpr(ifTrue)
      highlightPExpr(ifFalse)
    case MetaLang3Ast.BinOp(op, lhs, rhs) =>
      highlightPExpr(lhs)
      highlightPExpr(rhs)
    case value: MetaLang3Ast.AbstractEnumValue =>
    case MetaLang3Ast.ArrayExpr(elems) =>
      elems.foreach(highlightPExpr)
    case MetaLang3Ast.BindExpr(ctx, binder) =>
      highlightPExpr(ctx)
      binder match {
        case bindExpr: MetaLang3Ast.BindExpr => highlightPExpr(bindExpr)
        case MetaLang3Ast.ProcessorBlock(body) => highlightPExpr(body)
        case ref: MetaLang3Ast.Ref => highlightPExpr(ref)
      }
    case MetaLang3Ast.ExprParen(body) => highlightPExpr(body)
    case MetaLang3Ast.FuncCallOrConstructExpr(funcName, params) =>
      editor.setStyle(CodeStyle.CLASS_NAME, funcName.parseNode)
      params.foreach(highlightPExpr)
    case literal: MetaLang3Ast.Literal =>
      editor.setStyle(CodeStyle.LITERAL, literal.parseNode)
    case MetaLang3Ast.NamedConstructExpr(typeName, params, supers) =>
      editor.setStyle(CodeStyle.CLASS_NAME, typeName.parseNode)
      params.foreach { p =>
        editor.setStyle(CodeStyle.PARAM_NAME, p.name.parseNode)
        p.typeDesc.foreach(highlightType)
        highlightPExpr(p.expr)
      }
    case ref: MetaLang3Ast.Ref =>
      editor.setStyle(CodeStyle.REF, ref.parseNode)
    case MetaLang3Ast.PrefixOp(op, expr) =>
      highlightPExpr(expr)
    case MetaLang3Ast.ElvisOp(value, ifNull) =>
      highlightPExpr(value)
      highlightPExpr(ifNull)
  }

  def highlightElem(elem: MetaLang3Ast.Elem): Unit = elem match {
    case ref: MetaLang3Ast.Ref =>
      editor.setStyle(CodeStyle.REF, ref.parseNode)
    case MetaLang3Ast.ProcessorBlock(body) =>
      editor.setStyle(CodeStyle.PROCESSOR_BLOCK, elem.parseNode)
      highlightPExpr(body)
    case symbol: MetaLang3Ast.Symbol =>
      symbol match {
        case MetaLang3Ast.Sequence(seq) =>
          seq.foreach(highlightElem)
        case _ => // do nothing
      }
  }

  def highlightGrammar(grammar: MetaLang3Ast.Grammar): Unit = {
    editor.clearStyles()

    grammar.defs.foreach {
      case MetaLang3Ast.Rule(lhs, rhs) =>
        editor.setStyle(CodeStyle.LHS, lhs.name.parseNode)
        rhs.foreach { r =>
          editor.setStyle(CodeStyle.RHS, r.parseNode)
          r.seq.foreach(highlightElem)
        }
      case typeDef: MetaLang3Ast.TypeDef => // do nothing
    }
  }
}
