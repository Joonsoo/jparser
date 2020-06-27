package com.giyeok.jparser.metalang3a

import com.giyeok.jparser.ParseResultTree
import com.giyeok.jparser.ParseResultTree.{BindNode, JoinNode, Node, SequenceNode}
import com.giyeok.jparser.metalang3a.Type.readableNameOf
import com.giyeok.jparser.metalang3a.ValuefyExpr.{MatchNonterminal, Unbind, UnrollChoices}

class AnalysisPrinter(val startValuefyExpr: ValuefyExpr,
                      val nonterminalValuefyExprs: Map[String, UnrollChoices]) {
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
            println(s"${indent}Bind(${symbol.symbol.toShortString})")
            printNodeStructure(body, indent + "  ")
        case ParseResultTree.CyclicBindNode(start, end, symbol) =>
            println(s"${indent}Cyclic Bind")
        case JoinNode(symbol, body, join) =>
            println(s"${indent}Join(${symbol.symbol.toShortString})")
            printNodeStructure(body, indent + "  ")
            printNodeStructure(join, indent + "  ")
        case seq@SequenceNode(start, end, symbol, _children) =>
            println(s"${indent}Sequence(${symbol.symbol.toShortString})")
            seq.children.zipWithIndex.foreach { childIdx =>
                printNodeStructure(childIdx._1, indent + "  ")
            }
        case ParseResultTree.CyclicSequenceNode(start, end, symbol, pointer, _children) =>
            println(s"${indent}Cyclic Sequence")
    }

    def printValuefyStructure(valuefyExpr: ValuefyExpr, indent: String = ""): Unit = valuefyExpr match {
        case ValuefyExpr.InputNode => println(s"${indent}InputNode")
        case MatchNonterminal(nonterminalName) => println(s"${indent}match $nonterminalName")
        case Unbind(symbol, expr) =>
            println(s"${indent}Unbind ${symbol.toShortString}")
            printValuefyStructure(expr, indent + "  ")
        case ValuefyExpr.JoinBody(joinSymbol, bodyProcessor) => ???
        case ValuefyExpr.JoinCond(joinSymbol, bodyProcessor) => ???
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
                println(s"${indent}${choice._1.toShortString} -> ")
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
        case value: ValuefyExpr.EnumValue =>
    }

    def printValuefyStructure(): Unit = {
        printValuefyStructure(startValuefyExpr)
        nonterminalValuefyExprs.foreach { expr =>
            println(s"== ${expr._1}:")
            printValuefyStructure(expr._2)
        }
    }

    def printClassDef(className: String, classParams: List[(String, Type)]): Unit = {
        println(s"class $className(${classParams.map(p => s"${p._1}: ${readableNameOf(p._2)}").mkString(", ")})")
    }
}
