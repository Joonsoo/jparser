package com.giyeok.jparser.kttestutils

import com.giyeok.jparser.NGrammar
import com.giyeok.jparser.Symbols
import com.giyeok.jparser.ktlib.*
import com.giyeok.jparser.metalang3.MetaLanguage3
import com.giyeok.jparser.metalang3.Type
import com.giyeok.jparser.metalang3.ValuefyExpr
import scala.jdk.CollectionConverters.IterableHasAsJava
import scala.jdk.CollectionConverters.MapHasAsJava

class AstifySimulator(
  val analysis: MetaLanguage3.ProcessedGrammar,
  val history: List<KernelSet>,
  val source: String
) {
  fun simulateAst(): Ast =
    simulateAst(
      ValuefyExpr.MatchNonterminal(analysis.startNonterminalName()),
      0,
      history.size - 1,
      analysis.grammar().startSymbol()
    )

  private fun simulateAst(
    expr: ValuefyExpr,
    beginGen: Int,
    endGen: Int,
    sym: Symbols.Symbol,
  ): Ast =
    when (expr) {
      is ValuefyExpr.`InputNode$` -> TODO()
      is ValuefyExpr.MatchNonterminal -> {
        val bodyExpr = analysis.nonterminalValuefyExprs()[expr.nonterminalName()].get()
        simulateAst(bodyExpr, beginGen, endGen, sym)
      }

      is ValuefyExpr.Unbind -> {
        simulateAst(expr.expr(), beginGen, endGen, expr.symbol())
      }

      is ValuefyExpr.JoinBody -> {
        val joinSymbol = sym as Symbols.Join
        simulateAst(expr.bodyProcessor(), beginGen, endGen, joinSymbol.sym())
      }

      is ValuefyExpr.JoinCond -> {
        val joinSymbol = sym as Symbols.Join
        simulateAst(expr.condProcessor(), beginGen, endGen, joinSymbol.join())
      }

      is ValuefyExpr.SeqElemAt -> {
        val seqId = analysis.ngrammar().idOf(sym)
        val seq = analysis.ngrammar().nsequences().get(seqId).get()
        val seqElems = getSequenceElems(
          history,
          seqId,
          IterableHasAsJava(seq.sequence()).asJava().toList().map { it as Int },
          beginGen,
          endGen
        )
        val (subBegin, subEnd) = seqElems[expr.index()]
        val seqSyms = IterableHasAsJava(seq.symbol().seq()).asJava().toList()
        simulateAst(
          expr.expr(),
          subBegin,
          subEnd,
          seqSyms[expr.index()]
        )
      }

      is ValuefyExpr.UnrollRepeatFromZero -> {
        val symId = analysis.ngrammar().idOf(sym)
        val repeat = analysis.ngrammar().symbolOf(symId) as NGrammar.NRepeat
        val itemSymId = analysis.ngrammar().idOf(repeat.symbol().sym())
        val elems = unrollRepeat0(
          history,
          symId,
          itemSymId,
          repeat.baseSeq(),
          repeat.repeatSeq(),
          beginGen,
          endGen
        )
        Ast.Arr(elems.map {
          simulateAst(
            expr.elemProcessor(),
            it.first,
            it.second,
            analysis.ngrammar().symbolOf(itemSymId).symbol()
          )
        })
      }

      is ValuefyExpr.UnrollRepeatFromZeroNoUnbind -> {
        TODO()
      }

      is ValuefyExpr.UnrollRepeatFromOne -> {
        val symId = analysis.ngrammar().idOf(sym)
        val repeat = analysis.ngrammar().symbolOf(symId) as NGrammar.NRepeat
        val itemSymId = analysis.ngrammar().idOf(repeat.symbol().sym())
        val elems = unrollRepeat1(
          history,
          symId,
          itemSymId,
          repeat.baseSeq(),
          repeat.repeatSeq(),
          beginGen,
          endGen
        )
        Ast.Arr(elems.map {
          simulateAst(
            expr.elemProcessor(),
            it.first,
            it.second,
            analysis.ngrammar().symbolOf(itemSymId).symbol()
          )
        })
      }

      is ValuefyExpr.UnrollRepeatFromOneNoUnbind -> {
        TODO()
      }

      is ValuefyExpr.UnrollChoices -> {
        val choices = MapHasAsJava(expr.choices()).asJava().entries
        if (choices.size == 1) {
          val (choiceSymbol, choiceExpr) = choices.single()
          simulateAst(choiceExpr, beginGen, endGen, choiceSymbol)
        } else {
          data class UnrollChoiceTry(
            val sym: Symbols.Symbol,
            val symId: Int,
            val expr: ValuefyExpr,
            val found: Kernel?
          )

          val choiceSymbols = choices.map { (sym, expr) ->
            val symId = analysis.ngrammar().idOf(sym)
            val lastPointer = analysis.ngrammar().lastPointerOf(symId)
            val found = history[endGen].findByBeginGenOpt(symId, lastPointer, beginGen)
            UnrollChoiceTry(sym, symId, expr, found)
          }
          check(hasSingleTrue(*choiceSymbols.map { it.found != null }.toBooleanArray())) {
            val possibleMatches = choiceSymbols.filter { it.found != null }
            "Ambiguity found: $beginGen..<$endGen ${sym.toShortString()} -> ${possibleMatches.map { "${it.sym.toShortString()} (${it.symId})" }}"
          }
          val choice = choiceSymbols.single { it.found != null }
          simulateAst(choice.expr, beginGen, endGen, choice.sym)
        }
      }

      is ValuefyExpr.ConstructCall -> {
        val params = IterableHasAsJava(expr.params()).asJava().toList()
        val clsParams = IterableHasAsJava(analysis.classParamTypes().get(expr.className()).get())
          .asJava().toList()
        check(params.size == clsParams.size)
        val fieldValues = clsParams.zip(params).associate { (param, paramExpr) ->
          val paramValue = simulateAst(paramExpr, beginGen, endGen, sym)
          param._1() to paramValue
        }
        Ast.Cls(expr.className(), fieldValues, beginGen, endGen)
      }

      is ValuefyExpr.FuncCall -> {
        val args = IterableHasAsJava(expr.params()).asJava().map {
          simulateAst(it, beginGen, endGen, sym)
        }
        when (expr.funcType()) {
          ValuefyExpr.`FuncType$`.`MODULE$`.IsPresent() -> {
            check(args.size == 1)
            val res = when (val v = args.single()) {
              Ast.Null -> false
              is Ast.Arr -> v.elems.isNotEmpty()
              is Ast.Str -> v.value.isNotEmpty()
              is Ast.Bool -> TODO()
              is Ast.Chr -> TODO()
              is Ast.Cls -> TODO()
              is Ast.Enum -> TODO()
            }
            Ast.Bool(res)
          }

          ValuefyExpr.`FuncType$`.`MODULE$`.IsEmpty() -> {
            check(args.size == 1)
            val res = when (val v = args.single()) {
              Ast.Null -> true
              is Ast.Arr -> v.elems.isEmpty()
              is Ast.Str -> v.value.isEmpty()
              is Ast.Bool -> TODO()
              is Ast.Chr -> TODO()
              is Ast.Cls -> TODO()
              is Ast.Enum -> TODO()
            }
            Ast.Bool(res)
          }

          ValuefyExpr.`FuncType$`.`MODULE$`.Chr() -> TODO()
          ValuefyExpr.`FuncType$`.`MODULE$`.Str() -> {
            fun Ast.stringify(): String = when (this) {
              is Ast.Arr -> {
                elems.joinToString("") { it.stringify() }
              }

              is Ast.Bool -> value.toString()
              is Ast.Chr -> value.toString()
              is Ast.Cls -> TODO()
              is Ast.Enum -> TODO()
              Ast.Null -> TODO()
              is Ast.Str -> value
            }

            val v = args.joinToString("") { it.stringify() }
            Ast.Str(v)
          }

          else -> TODO()
        }
      }

      is ValuefyExpr.ArrayExpr -> {
        val arrayType = analysis.typeInferer().typeOfValuefyExpr(expr).get()
        check(arrayType is Type.ArrayOf)
        val elemType = arrayType.elemType()
        val elemExprs = IterableHasAsJava(expr.elems()).asJava()
        val elems = elemExprs.map { simulateAst(it, beginGen, endGen, sym) }
        Ast.Arr(elems)
      }

      is ValuefyExpr.BinOp -> TODO()
      is ValuefyExpr.PreOp -> TODO()
      is ValuefyExpr.ElvisOp -> {
        val v = simulateAst(expr.expr(), beginGen, endGen, sym)
        if (v != Ast.Null) v else simulateAst(expr.ifNull(), beginGen, endGen, sym)
      }

      is ValuefyExpr.TernaryOp -> TODO()
      is ValuefyExpr.`NullLiteral$` -> Ast.Null
      is ValuefyExpr.BoolLiteral -> Ast.Bool(expr.value())
      is ValuefyExpr.CharLiteral -> Ast.Chr(expr.value())
      is ValuefyExpr.`CharFromTerminalLiteral$` -> {
        check(beginGen + 1 == endGen)
        Ast.Chr(source[beginGen])
      }

      is ValuefyExpr.StringLiteral -> Ast.Str(expr.value())

      is ValuefyExpr.CanonicalEnumValue -> Ast.Enum(expr.enumName(), expr.enumValue())
      is ValuefyExpr.ShortenedEnumValue -> {
        val enumName = analysis.shortenedEnumTypesMap().get(expr.unspecifiedEnumTypeId()).get()
        Ast.Enum(enumName, expr.enumValue())
      }

      else -> throw AssertionError()
    }
}

sealed class Ast {
  data class Cls(
    val clsName: String,
    val fields: Map<String, Ast>,
    val start: Int,
    val end: Int
  ): Ast()

  data class Arr(val elems: List<Ast>): Ast()
  data class Str(val value: String): Ast()
  data class Chr(val value: Char): Ast()
  data class Bool(val value: Boolean): Ast()
  data class Enum(val enumName: String, val enumValue: String): Ast()
  data object Null: Ast()
}
