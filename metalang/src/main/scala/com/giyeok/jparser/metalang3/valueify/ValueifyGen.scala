package com.giyeok.jparser.metalang3.valueify

import com.giyeok.jparser.ParseResultTree.Node
import com.giyeok.jparser.examples.metalang3.MetaLang3Grammar
import com.giyeok.jparser.metalang2.generated.MetaGrammar3Ast
import com.giyeok.jparser.metalang2.generated.MetaGrammar3Ast.{Elem, InPlaceSequence, Rule, ngrammar}
import com.giyeok.jparser.metalang3.AnalysisResult
import com.giyeok.jparser.metalang3.codegen.ScalaGen
import com.giyeok.jparser.metalang3.symbols.Escapes
import com.giyeok.jparser.metalang3.symbols.Escapes.NonterminalName
import com.giyeok.jparser.metalang3.types.TypeFunc
import com.giyeok.jparser.metalang3.types.TypeFunc._
import com.giyeok.jparser.metalang3.valueify.ValueifyGen.IllegalGrammar
import com.giyeok.jparser.nparser.ParseTreeConstructor
import com.giyeok.jparser.{NGrammar, ParseForestFunc, ParseResultTree, ParsingErrors}

import scala.annotation.tailrec

class ValueifyGen {

    private def check(cond: Boolean, msg: String): Unit = {
        if (!cond) throw IllegalGrammar(msg)
    }

    private def condSymPathOf(condSymPath: List[Node]): String = ???

    private def unifyTypes(types: Iterable[TypeFunc]): TypeFunc = if (types.size == 1) types.head else UnionOf(types.toList)

    private def valueifyInPlaceSequence(choices: List[MetaGrammar3Ast.InPlaceSequence], condSymPath: String, input: ValueifyExpr): ValueifyExpr = {
        // TODO 이 안에서 `input`들 잘 들어가는지 확인
        def valueifyChoice(choice: InPlaceSequence, input: ValueifyExpr): ValueifyExpr = {
            // 마지막 element가 symbol이면 SeqElemAt이 들어가야 한다.
            assert(choice.seq.nonEmpty)
            val bodyInput = choice.seq.last match {
                case symbol: MetaGrammar3Ast.Symbol => SeqElemAt(Unbind(choice, input), choice.seq.size - 1, TypeOfSymbol(symbol))
                case _: MetaGrammar3Ast.Processor => Unbind(choice, input)
            }
            valueify(choice.seq, choice.seq.last, condSymPath, bodyInput)
        }

        if (choices.size == 1) {
            valueifyChoice(choices.head, input)
        } else {
            check(condSymPath.isEmpty, "")
            val vChoices = choices.map { choice =>
                choice -> valueifyChoice(choice, input)
            }
            val choicesMap: Map[DerivationChoice, ValueifyExpr] = vChoices.map(choice => InPlaceSequenceChoice(choice._1) -> choice._2).toMap
            val resultType = unifyTypes(vChoices.map(_._2.resultType))
            UnrollChoices(input, choicesMap, resultType)
        }
    }

    // _1는 elem 속의 refCtx
    // _2는 elem이 Longest 등인 경우 그걸 어떻게 발라먹을지
    @tailrec private def getBindingContext(elem: MetaGrammar3Ast.Elem, input: ValueifyExpr): (List[Elem], ValueifyExpr) = elem match {
        case symbol@MetaGrammar3Ast.Longest(astNode, choices) if choices.choices.size == 1 =>
            getBindingContext(choices.choices.head, Unbind(symbol, input))
        case MetaGrammar3Ast.InPlaceChoices(astNode, choices) if choices.size == 1 =>
            (choices.head.seq, input)
        case _ => throw IllegalGrammar("Bind expression only can refer to Longest or InPlaceChoices with only choice")
    }

    private def typeOfElem(elem: MetaGrammar3Ast.Elem) = elem match {
        case symbol: MetaGrammar3Ast.Symbol => TypeOfSymbol(symbol)
        case processor: MetaGrammar3Ast.Processor => TypeOfProcessor(processor)
    }

    private var idCounter: Int = 0

    private def nextId(): Int = {
        idCounter += 1
        idCounter
    }

    def valueify(refCtx: List[Elem], elem: Elem, condSymPath: String, input: ValueifyExpr): ValueifyExpr = elem match {
        case symbol: MetaGrammar3Ast.Symbol =>
            symbol match {
                case symbol@MetaGrammar3Ast.JoinSymbol(astNode, body, join) =>
                    if (condSymPath.isEmpty) {
                        valueify(refCtx, body, condSymPath, JoinBodyOf(symbol, Unbind(symbol, input), TypeOfSymbol(body)))
                    } else {
                        // TODO condSymPath.head 방향 봐서 JoinBodyOf/JoinCondOf, valueify 재귀호출 할 때는 condSymPath.tail
                        ???
                    }
                case symbol@MetaGrammar3Ast.ExceptSymbol(astNode, body, except) =>
                    if (condSymPath.isEmpty) {
                        valueify(refCtx, body, condSymPath, ExceptBodyOf(symbol, Unbind(symbol, input), TypeOfSymbol(body)))
                    } else {
                        // TODO condSymPath
                        ???
                    }
                case MetaGrammar3Ast.FollowedBy(astNode, followedBy) =>
                    // TODO condSymPath
                    InputNode
                case MetaGrammar3Ast.NotFollowedBy(astNode, notFollowedBy) =>
                    // TODO condSymPath
                    InputNode
                case MetaGrammar3Ast.Optional(astNode, body) =>
                    check(condSymPath.isEmpty, "Optional cannot be referred with condSymPath")
                    val vBody = valueify(refCtx, body, condSymPath, InputNode)
                    UnrollChoices(Unbind(symbol, input), Map(
                        EmptySeqChoice -> NullLiteral,
                        SymbolChoice(body) -> vBody),
                        OptionalOf(vBody.resultType))
                case MetaGrammar3Ast.RepeatFromZero(astNode, body) =>
                    check(condSymPath.isEmpty, "Repeat* cannot be referred with condSymPath")
                    val vBody = valueify(refCtx, body, condSymPath, InputNode)
                    UnrollRepeat(0, input, vBody, ArrayOf(vBody.resultType))
                case MetaGrammar3Ast.RepeatFromOne(astNode, body) =>
                    check(condSymPath.isEmpty, "Repeat+ cannot be referred with condSymPath")
                    val vBody = valueify(refCtx, body, condSymPath, input)
                    UnrollRepeat(1, input, vBody, ArrayOf(vBody.resultType))
                case MetaGrammar3Ast.InPlaceChoices(astNode, choices) =>
                    check(condSymPath.isEmpty, "InPlaceChoices cannot be referred with condSymPath")
                    valueifyInPlaceSequence(choices, condSymPath, input)
                case symbol@MetaGrammar3Ast.Longest(astNode, choices) =>
                    check(condSymPath.isEmpty, "Longest cannot be referred with condSymPath")
                    val vChoices = valueifyInPlaceSequence(choices.choices, condSymPath, input)
                    Unbind(symbol, vChoices) //vChoices.resultType)
                case _: MetaGrammar3Ast.Terminal =>
                    check(condSymPath.isEmpty, "Terminal cannot be referred with condSymPath")
                    InputNode
                case MetaGrammar3Ast.TerminalChoice(astNode, choices) =>
                    check(condSymPath.isEmpty, "TerminalChoice cannot be referred with condSymPath")
                    InputNode
                case MetaGrammar3Ast.StringSymbol(astNode, value) =>
                    check(condSymPath.isEmpty, "String cannot be referred with condSymPath")
                    StringLiteral(StringLiteral.escape(value))
                case nonterm@MetaGrammar3Ast.Nonterminal(astNode, name) =>
                    check(condSymPath.isEmpty, "Nonterminal cannot be referred with condSymPath")
                    MatchNonterminal(nonterm, input, TypeOfSymbol(symbol))
                case MetaGrammar3Ast.EmptySeq(astNode) =>
                    check(condSymPath.isEmpty, "EmptySeq cannot be referred with condSymPath")
                    InputNode
            }
        case processor: MetaGrammar3Ast.Processor =>
            // TODO share ValueifyExpr
            check(condSymPath.isEmpty, "")
            processor match {
                //                case MetaGrammar3Ast.TernaryOp(astNode, cond, ifTrue, ifFalse) =>
                //                    val vCond = valueify(refCtx, cond, "", InputNode)
                //                    val vIfTrue = valueify(refCtx, ??? /*ifTrue*/ , "", InputNode)
                //                    val vIfFalse = valueify(refCtx, ??? /*ifFalse*/ , "", InputNode)
                //                    TernaryExpr(vCond, vIfTrue, vIfFalse, unifyTypes(List(vIfTrue.resultType, vIfFalse.resultType)))
                case MetaGrammar3Ast.ElvisOp(astNode, value, ifNull) =>
                    val vValue = valueify(refCtx, value, "", input)
                    val vIfNull = valueify(refCtx, ifNull, "", input)
                    ElvisOp(vValue, vIfNull, ElvisType(vValue.resultType, vIfNull.resultType))
                case MetaGrammar3Ast.BinOp(astNode, op, lhs, rhs) =>
                    val vLhs = valueify(refCtx, lhs, "", input)
                    val vRhs = valueify(refCtx, rhs, "", input)
                    op.sourceText match {
                        case "&&" => BinOp(Op.BOOL_AND, vLhs, vRhs, BoolType)
                        case "||" => BinOp(Op.BOOL_OR, vLhs, vRhs, BoolType)
                        case "==" => BinOp(Op.EQ, vLhs, vRhs, BoolType)
                        case "!=" => BinOp(Op.NE, vLhs, vRhs, BoolType)
                        case "+" => BinOp(Op.ADD, vLhs, vRhs, BoolType)
                    }
                case MetaGrammar3Ast.PrefixOp(astNode, expr, op) =>
                    val vExpr = valueify(refCtx, expr, "", input)
                    op.sourceText match {
                        case "!" => PrefixOp(PreOp.NOT, vExpr, BoolType)
                    }
                case ref: MetaGrammar3Ast.Ref => ref match {
                    case MetaGrammar3Ast.ValRef(astNode, idx, condSymPath) =>
                        val idxValue = idx.sourceText.toInt
                        if (idxValue >= refCtx.size) throw new IllegalGrammar("")
                        val symbolIdx = idxValue // TODO idxValue - refCtx(0~idxValue 전)까지 symbol의 갯수
                        val referredElem = refCtx(idxValue)
                        valueify(refCtx, referredElem, condSymPath.map(condSymPathOf).getOrElse(""),
                            SeqElemAt(input, symbolIdx, typeOfElem(referredElem)))
                    case MetaGrammar3Ast.RawRef(astNode, idx, condSymPath) => ???
                }
                case MetaGrammar3Ast.ExprParen(astNode, body) =>
                    valueify(refCtx, body, "", input)
                case MetaGrammar3Ast.BindExpr(astNode, ctx, binder) =>
                    check(ctx.condSymPath.getOrElse(List()).isEmpty, "Binding context cannot have condSymPath")
                    val idx = ctx.idx.sourceText.toInt
                    check(idx < refCtx.size, "")
                    val bindingCtx = getBindingContext(refCtx(idx), input)
                    assert(bindingCtx._1.nonEmpty)
                    valueify(bindingCtx._1, bindingCtx._1.last, "", bindingCtx._2)
                case MetaGrammar3Ast.NamedConstructExpr(astNode, typeName, params) =>
                    val vParams = params.map(param => (param, valueify(refCtx, param.expr, "", input)))
                    NamedConstructCall(typeName, vParams, ClassType(typeName))
                case MetaGrammar3Ast.FuncCallOrConstructExpr(astNode, funcName, params) =>
                    val vParams = params.getOrElse(List()).map(param => valueify(refCtx, param, "", input))
                    // TODO FuncCall or UnnamedConstructCall depending on its name
                    FuncCall(funcName, vParams, FuncCallResultType(funcName, vParams))
                case MetaGrammar3Ast.ArrayExpr(astNode, elems) =>
                    val vElems = elems.getOrElse(List()).map(elem => valueify(refCtx, elem, "", input))
                    val arrayElemType = unifyTypes(vElems.map(_.resultType).toSet)
                    ArrayExpr(vElems, ArrayOf(arrayElemType))
                case literal: MetaGrammar3Ast.Literal => literal match {
                    case MetaGrammar3Ast.NullLiteral(astNode) => NullLiteral
                    case MetaGrammar3Ast.BoolLiteral(astNode, value) => BoolLiteral(value.sourceText.toBoolean)
                    // TODO Fix StringLiteral and CharLiteral value
                    case MetaGrammar3Ast.CharLiteral(astNode, value) => CharLiteral(value.astNode.sourceText.charAt(0))
                    case MetaGrammar3Ast.StringLiteral(astNode, value) => StringLiteral(value.map(_.astNode.sourceText).mkString)
                }
                case MetaGrammar3Ast.CanonicalEnumValue(astNode, enumName, valueName) =>
                    CanonicalEnumValue(enumName, valueName, EnumType(enumName))
                case MetaGrammar3Ast.ShortenedEnumValue(astNode, valueName) =>
                    ShortenedEnumValue(valueName, UnspecifiedEnum(nextId()))
            }
    }

    def valueifyRule(rule: Rule): UnrollChoices = {
        val mappers = rule.rhs.map { rhs =>
            RightHandSideChoice(rhs) -> valueify(rhs.elems, rhs.elems.last, "", InputNode)
        }.toMap
        val mappings = mappers.map(mapper => mapper._1 -> mapper._2)
        val returnType = unifyTypes(mappers.map(_._2.resultType))
        UnrollChoices(InputNode, mappings.toMap, returnType)
    }

    def parse(text: String): ParseResultTree.Node = {
        MetaGrammar3Ast.parse(text) match {
            case Left(ctx) =>
                val tree = new ParseTreeConstructor(ParseForestFunc)(ngrammar)(ctx.inputs, ctx.history, ctx.conditionFinal).reconstruct()
                tree match {
                    case Some(forest) if forest.trees.size == 1 =>
                        forest.trees.head
                    case Some(forest) =>
                        throw new Exception(ParsingErrors.AmbiguousParse("Ambiguous Parse: " + forest.trees.size).toString)
                    case None =>
                        val expectedTerms = ctx.nextGraph.nodes.flatMap { node =>
                            node.kernel.symbol match {
                                case NGrammar.NTerminal(_, term) => Some(term)
                                case _ => None
                            }
                        }
                        throw new Exception(ParsingErrors.UnexpectedEOF(expectedTerms, text.length).toString)
                }
            case Right(error) => throw new Exception(error.toString)
        }
    }
}

object ValueifyGen {

    case class IllegalGrammar(msg: String) extends Exception

    def main(args: Array[String]): Unit = {
        MetaLang3Grammar.inMetaLang3
        val example =
            """A = B&C
              |""".stripMargin
        val defs = MetaGrammar3Ast.parseAst(example).left.get.defs
        val rule = defs.head.asInstanceOf[Rule]
        val v = new ValueifyGen().valueifyRule(rule)

        val analysis = new AnalysisResult(rule.lhs.name.name.stringName, Map(rule.lhs.name.name.stringName -> v))
        val scalaGen = new ScalaGen(analysis)
        scalaGen.matchFuncFor(rule.lhs.name, v).codes.foreach(println)
    }
}
