package com.giyeok.jparser.metalang3

import com.giyeok.jparser.ParseResultTree.{BindNode, JoinNode, Node, SequenceNode}
import com.giyeok.jparser.metalang3.Type.readableNameOf
import com.giyeok.jparser.metalang3.ValuefyExpr.{MatchNonterminal, Unbind, UnrollChoices}
import com.giyeok.jparser.{NGrammar, ParseResultTree}

object AnalysisPrinter {
  def printClassHierarchy(classHierarchy: ClassHierarchyTree): Unit = {
    def printHierarchyItem(item: ClassHierarchyItem, indent: String = ""): Unit = {
      val extending = if (item.superclasses.isEmpty) "" else s" extends ${item.superclasses.toList.sorted.mkString(", ")}"
      println(s"$indent${item.className}$extending")
      item.subclasses.toList.sorted.foreach { sub =>
        printHierarchyItem(classHierarchy.allTypes(sub), indent + "  ")
      }
    }

    classHierarchy.rootTypes.foreach(printHierarchyItem(_))
  }

  def printNodeStructure(parseNode: Node, indent: String = ""): Unit = parseNode match {
    case ParseResultTree.TerminalNode(start, input) => println(s"${indent}Terminal ${input}")
    case BindNode(symbol, body) =>
      println(s"${indent}Bind ${symbol.id}:${symbol.symbol.toShortString}")
      printNodeStructure(body, indent + "  ")
    case ParseResultTree.CyclicBindNode(start, end, symbol) =>
      println(s"${indent}Cyclic Bind")
    case JoinNode(_, body, join) =>
      println(s"${indent}Join")
      printNodeStructure(body, indent + "  ")
      printNodeStructure(join, indent + "  ")
    case seq@SequenceNode(start, end, symbol, _children) =>
      println(s"${indent}Sequence ${symbol.id}:${symbol.symbol.toShortString}")
      seq.children.zipWithIndex.foreach { childIdx =>
        printNodeStructure(childIdx._1, indent + "  ")
      }
    case ParseResultTree.CyclicSequenceNode(start, end, symbol, pointer, _children) =>
      println(s"${indent}Cyclic Sequence")
  }

  def printClassDef(classHierarchy: ClassHierarchyTree, className: String, classParams: List[(String, Type)]): Unit = {
    println(s"class $className(${classParams.map(p => s"${p._1}: ${readableNameOf(classHierarchy.simplifyType(p._2))}(${readableNameOf(p._2)})").mkString(", ")})")
  }
}

class AnalysisPrinter(val grammar: NGrammar, val startValuefyExpr: ValuefyExpr,
                      val nonterminalValuefyExprs: Map[String, UnrollChoices],
                      val shortenedEnumTypesMap: Map[Int, String]) {
  def printValuefyStructure(valuefyExpr: ValuefyExpr, indent: String = ""): Unit = valuefyExpr match {
    case ValuefyExpr.InputNode => println(s"${indent}InputNode")
    case MatchNonterminal(nonterminalName) => println(s"${indent}match $nonterminalName")
    case Unbind(symbol, expr) =>
      println(s"${indent}Unbind ${grammar.idOf(symbol)}:${symbol.toShortString}")
      printValuefyStructure(expr, indent + "  ")
    case ValuefyExpr.JoinBody(bodyProcessor) =>
      println(s"${indent}JoinBody")
      printValuefyStructure(bodyProcessor, indent + "  ")
    case ValuefyExpr.JoinCond(bodyProcessor) =>
      println(s"${indent}JoinCond")
      printValuefyStructure(bodyProcessor, indent + "  ")
    case ValuefyExpr.SeqElemAt(index, expr) =>
      println(s"${indent}SeqElem $index")
      printValuefyStructure(expr, indent + "  ")
    case ValuefyExpr.UnrollRepeatFromZero(elemProcessor) =>
      println(s"${indent}Unroll*")
      printValuefyStructure(elemProcessor, indent + "  ")
    case ValuefyExpr.UnrollRepeatFromOne(elemProcessor) =>
      println(s"${indent}Unroll+")
      printValuefyStructure(elemProcessor, indent + "  ")
    case UnrollChoices(choices) =>
      println(s"${indent}choices")
      choices.foreach { choice =>
        println(s"${indent}${grammar.idOf(choice._1)}:${choice._1.toShortString} -> ")
        printValuefyStructure(choice._2, indent + "  ")
      }
    case ValuefyExpr.ConstructCall(className, params) =>
      println(s"${indent}new $className")
      params.zipWithIndex.foreach { param =>
        println(s"${indent}Param ${param._2}")
        printValuefyStructure(param._1, indent + "  ")
      }
    case ValuefyExpr.FuncCall(funcType, params) =>
    case ValuefyExpr.ArrayExpr(elems) =>
    case ValuefyExpr.BinOp(op, lhs, rhs) =>
    case ValuefyExpr.PreOp(op, expr) =>
    case ValuefyExpr.ElvisOp(expr, ifNull) =>
    case ValuefyExpr.TernaryOp(condition, ifTrue, ifFalse) =>
    case literal: ValuefyExpr.Literal =>
      val literalName = literal match {
        case ValuefyExpr.NullLiteral => "null"
        case ValuefyExpr.BoolLiteral(value) => s"bool $value"
        case ValuefyExpr.CharLiteral(value) => s"char $value"
        case ValuefyExpr.CharFromTerminalLiteral => s"char from node"
        case ValuefyExpr.StringLiteral(value) => s"string $value"
      }
      println(s"$indent$literalName")
    case value: ValuefyExpr.EnumValue =>
      val enumName = value match {
        case ValuefyExpr.CanonicalEnumValue(enumName, enumValue) => s"%$enumName.$enumValue"
        case ValuefyExpr.ShortenedEnumValue(unspecifiedEnumTypeId, enumValue) =>
          s"%${shortenedEnumTypesMap(unspecifiedEnumTypeId)}.$enumValue"
      }
      println(s"${indent}enum $enumName")
  }

  def printValuefyStructure(): Unit = {
    printValuefyStructure(startValuefyExpr)
    nonterminalValuefyExprs.foreach { expr =>
      println(s"== ${expr._1}:")
      printValuefyStructure(expr._2)
    }
  }
}
