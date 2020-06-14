package com.giyeok.jparser.metalang3.valueify

import com.giyeok.jparser.ParseResultTree.Node
import com.giyeok.jparser._
import com.giyeok.jparser.metalang2.generated.MetaGrammar3Ast
import com.giyeok.jparser.metalang2.generated.MetaGrammar3Ast.Elem
import com.giyeok.jparser.metalang3.MetaLanguage3.IllegalGrammar
import com.giyeok.jparser.metalang3.symbols.Escapes
import com.giyeok.jparser.metalang3.symbols.Escapes.NonterminalName
import com.giyeok.jparser.metalang3.types.TypeFunc
import com.giyeok.jparser.metalang3.types.TypeFunc._

import scala.annotation.tailrec
import scala.collection.immutable.ListSet

class ValueifyGen {

    private def check(cond: Boolean, msg: String): Unit = {
        if (!cond) throw IllegalGrammar(msg)
    }

    private var _symbolsMap = Map[MetaGrammar3Ast.Symbol, Symbols.Symbol]()

    def symbolsMap: Map[MetaGrammar3Ast.Symbol, Symbols.Symbol] = _symbolsMap

    // ID는 UnspecifiedEnum 객체 생성시 필요
    private var idCounter: Int = 0

    private def nextId(): Int = {
        idCounter += 1
        idCounter
    }

    private def collectSymbolAndReturn(symbol: MetaGrammar3Ast.Symbol, tuple: (ValueifyExpr, Symbols.Symbol)): (ValueifyExpr, Symbols.Symbol) = {
        _symbolsMap += (symbol -> tuple._2)
        tuple
    }

    private def collectSequenceAndReturn(sequence: MetaGrammar3Ast.Sequence, tuple: (ValueifyExpr, Symbols.Symbol)): (ValueifyExpr, Symbols.Symbol) = {
        _symbolsMap += (sequence -> tuple._2)
        tuple
    }

    private def condSymPathOf(condSymPath: List[Node]): String = condSymPath.map(_.sourceText).mkString

    private def unifyTypes(types: Iterable[TypeFunc]): TypeFunc = if (types.size == 1) types.head else UnionOf(types.toList)

    private def valueifySequence(elemsSeq: List[Elem], input: ValueifyExpr): (ValueifyExpr, Symbols.Symbol) = {
        val elems = elemsSeq map {
            case elem: MetaGrammar3Ast.Symbol =>
                val p = proxyIf(valueifySymbol(elemsSeq, elem, "", input))
                (p._1, Some(p._2))
            case elem: MetaGrammar3Ast.Processor =>
                (valueifyProcessor(elemsSeq, elem, input), None)
        }
        val lastProcessor = elems.last._1
        val symbols = elems.flatMap(_._2)
        val seqSymbol = if (symbols.size == 1) symbols.head else Symbols.Sequence(symbols)
        (lastProcessor, seqSymbol)
    }

    private def valueifyChoices(choices: List[MetaGrammar3Ast.Sequence], condSymPath: String, input: ValueifyExpr): (ValueifyExpr, Symbols.Symbol) = {
        // TODO 이 안에서 `input`들 잘 들어가는지 확인
        def valueifyChoice(choice: MetaGrammar3Ast.Sequence, input: ValueifyExpr): ValueifyExpr = {
            // 마지막 element가 symbol이면 SeqElemAt이 들어가야 한다.
            assert(choice.seq.nonEmpty)
            val bodyInput = choice.seq.last match {
                case symbol: MetaGrammar3Ast.Symbol => SeqElemAt(Unbind(choice, input), choice.seq.size - 1, TypeOfSymbol(symbol))
                case _: MetaGrammar3Ast.Processor => Unbind(choice, input)
            }
            choice.seq.last match {
                case lastElem: MetaGrammar3Ast.Symbol => valueifySymbol(choice.seq, lastElem, condSymPath, bodyInput)._1
                case lastElem: MetaGrammar3Ast.Processor => valueifyProcessor(choice.seq, lastElem, bodyInput)
            }
        }

        if (choices.size == 1) {
            collectSequenceAndReturn(choices.head, valueifySequence(choices.head.seq, input))
        } else {
            ???
            //            check(condSymPath.isEmpty, "")
            //            val vChoices = choices.map { choice =>
            //                choice -> valueifySequence(choice.seq, input)
            //            }
            //            val choicesMap: Map[DerivationChoice, ValueifyExpr] = vChoices.map(choice => InPlaceSequenceChoice(choice._1) -> choice._2).toMap
            //            val resultType = unifyTypes(vChoices.map(_._2._1.resultType))
            //            (UnrollChoices(input, choicesMap, resultType), Symbols.OneOf(ListSet(vChoices.map(_._1): _*)))
        }
    }

    private def typeOfElem(elem: MetaGrammar3Ast.Elem) = elem match {
        case symbol: MetaGrammar3Ast.Symbol => TypeOfSymbol(symbol)
        case processor: MetaGrammar3Ast.Processor => TypeOfProcessor(processor)
    }

    private def proxyIf(pair: (ValueifyExpr, Symbols.Symbol)): (ValueifyExpr, Symbols.AtomicSymbol) = pair._2 match {
        case symbol: Symbols.AtomicSymbol => (pair._1, symbol)
        case seq: Symbols.Sequence => (pair._1, Symbols.Proxy(seq)) // TODO pair._1에서 Unbind하는거 넣어야할듯
    }

    def valueifySymbol(refCtx: List[Elem], symbol: MetaGrammar3Ast.Symbol, condSymPath: String, input: ValueifyExpr): (ValueifyExpr, Symbols.Symbol) = collectSymbolAndReturn(symbol, symbol match {
        case symbol@MetaGrammar3Ast.JoinSymbol(astNode, body, join) =>
            if (condSymPath.isEmpty) {
                // body나 join이 SingleChoice/OneSymbol 가 아니면 Proxy + Unbind 추가
                val vBody = proxyIf(valueifySymbol(refCtx, body, "", InputNode))
                val vJoin = proxyIf(valueifySymbol(refCtx, join, "", InputNode))
                (JoinBodyOf(symbol, Unbind(symbol, input), vBody._1, TypeOfSymbol(body)), Symbols.Join(vBody._2, vJoin._2))
            } else {
                // condSymPath.head 방향 봐서 JoinBodyOf/JoinCondOf, valueify 재귀호출 할 때는 condSymPath.tail
                val vBody = proxyIf(valueifySymbol(refCtx, body, condSymPath.tail, InputNode))
                val vJoin = proxyIf(valueifySymbol(refCtx, join, condSymPath.tail, InputNode))
                condSymPath.head match {
                    case '<' => // body
                        (JoinBodyOf(symbol, Unbind(symbol, input), vBody._1, TypeOfSymbol(body)), Symbols.Join(vBody._2, vJoin._2))
                    case '>' => // cond
                        (JoinCondOf(symbol, Unbind(symbol, input), vJoin._1, TypeOfSymbol(join)), Symbols.Join(vBody._2, vJoin._2))
                }
            }
        case symbol@MetaGrammar3Ast.ExceptSymbol(astNode, body, except) =>
            check(condSymPath.isEmpty, "Except cannot be referred with condSymPath")
            val vBody = proxyIf(valueifySymbol(refCtx, body, "", input))
            val vExcept = proxyIf(valueifySymbol(refCtx, except, "", input))
            (Unbind(symbol, input), Symbols.Except(vBody._2, vExcept._2))
        case MetaGrammar3Ast.FollowedBy(astNode, followedBy) =>
            check(condSymPath.isEmpty, "FollowedBy cannot be referred with condSymPath")
            val vLookahead = proxyIf(valueifySymbol(refCtx, followedBy, "", input))
            (InputNode, Symbols.LookaheadIs(vLookahead._2))
        case MetaGrammar3Ast.NotFollowedBy(astNode, notFollowedBy) =>
            check(condSymPath.isEmpty, "NotFollowedBy cannot be referred with condSymPath")
            val vLookahead = proxyIf(valueifySymbol(refCtx, notFollowedBy, "", input))
            (InputNode, Symbols.LookaheadExcept(vLookahead._2))
        case MetaGrammar3Ast.Optional(astNode, body) =>
            check(condSymPath.isEmpty, "Optional cannot be referred with condSymPath")
            val vBody = proxyIf(valueifySymbol(refCtx, body, condSymPath, InputNode))
            val emptySymbol = Symbols.Proxy(Symbols.Sequence(Seq()))
            (UnrollChoices(Unbind(symbol, input), Map(
                GrammarSymbolChoice(emptySymbol) -> NullLiteral, AstSymbolChoice(body) -> vBody._1
            ), OptionalOf(vBody._1.resultType)), Symbols.OneOf(ListSet(emptySymbol, vBody._2)))
        case MetaGrammar3Ast.RepeatFromZero(astNode, body) =>
            check(condSymPath.isEmpty, "Repeat* cannot be referred with condSymPath")
            val vBody = valueifySymbol(refCtx, body, condSymPath, InputNode)
            (UnrollRepeat(0, input, vBody._1, ArrayOf(vBody._1.resultType)), ???)
        case MetaGrammar3Ast.RepeatFromOne(astNode, body) =>
            check(condSymPath.isEmpty, "Repeat+ cannot be referred with condSymPath")
            val vBody = valueifySymbol(refCtx, body, condSymPath, input)
            (UnrollRepeat(1, input, vBody._1, ArrayOf(vBody._1.resultType)), ???)
        case MetaGrammar3Ast.InPlaceChoices(astNode, choices) =>
            check(condSymPath.isEmpty, "InPlaceChoices cannot be referred with condSymPath")
            valueifyChoices(choices, condSymPath, input)
        case symbol@MetaGrammar3Ast.Longest(astNode, choices) =>
            check(condSymPath.isEmpty, "Longest cannot be referred with condSymPath")
            val vChoices = proxyIf(valueifyChoices(choices.choices, condSymPath, input))
            // TODO Unbind resultType이 vChoices.resultType인가..?
            (Unbind(symbol, vChoices._1), Symbols.Longest(vChoices._2))
        case terminal: MetaGrammar3Ast.Terminal =>
            check(condSymPath.isEmpty, "Terminal cannot be referred with condSymPath")
            (InputNode, Escapes.terminalToSymbol(terminal))
        case MetaGrammar3Ast.TerminalChoice(astNode, choices) =>
            check(condSymPath.isEmpty, "TerminalChoice cannot be referred with condSymPath")
            (InputNode, Escapes.terminalChoiceToSymbol(choices))
        case MetaGrammar3Ast.StringSymbol(astNode, value) =>
            check(condSymPath.isEmpty, "String cannot be referred with condSymPath")
            val stringSymbol = Symbols.Proxy(Symbols.Sequence(value.map(Escapes.stringCharToChar).map(Symbols.ExactChar)))
            (StringLiteral(Escapes.stringCharsToString(value)), stringSymbol)
        case nonterm@MetaGrammar3Ast.Nonterminal(astNode, name) =>
            check(condSymPath.isEmpty, "Nonterminal cannot be referred with condSymPath")
            (MatchNonterminal(nonterm, input, TypeOfSymbol(symbol)), Symbols.Nonterminal(name.stringName))
        case MetaGrammar3Ast.EmptySeq(astNode) =>
            check(condSymPath.isEmpty, "EmptySeq cannot be referred with condSymPath")
            (InputNode, Symbols.Sequence(Seq()))
    })

    def valueifyProcessor(refCtx: List[Elem], processor: MetaGrammar3Ast.Processor, input: ValueifyExpr): ValueifyExpr = processor match {
        //                case MetaGrammar3Ast.TernaryOp(astNode, cond, ifTrue, ifFalse) =>
        //                    val vCond = valueify(refCtx, cond, "", InputNode)
        //                    val vIfTrue = valueify(refCtx, ??? /*ifTrue*/ , "", InputNode)
        //                    val vIfFalse = valueify(refCtx, ??? /*ifFalse*/ , "", InputNode)
        //                    TernaryExpr(vCond, vIfTrue, vIfFalse, unifyTypes(List(vIfTrue.resultType, vIfFalse.resultType)))
        case MetaGrammar3Ast.ElvisOp(astNode, value, ifNull) =>
            val vValue = valueifyProcessor(refCtx, value, input)
            val vIfNull = valueifyProcessor(refCtx, ifNull, input)
            ElvisOp(vValue, vIfNull, ElvisType(vValue.resultType, vIfNull.resultType))
        case MetaGrammar3Ast.BinOp(astNode, op, lhs, rhs) =>
            val vLhs = valueifyProcessor(refCtx, lhs, input)
            val vRhs = valueifyProcessor(refCtx, rhs, input)
            op.sourceText match {
                case "&&" => BinOp(Op.BOOL_AND, vLhs, vRhs, BoolType)
                case "||" => BinOp(Op.BOOL_OR, vLhs, vRhs, BoolType)
                case "==" => BinOp(Op.EQ, vLhs, vRhs, BoolType)
                case "!=" => BinOp(Op.NE, vLhs, vRhs, BoolType)
                case "+" => BinOp(Op.ADD, vLhs, vRhs, BoolType)
            }
        case MetaGrammar3Ast.PrefixOp(astNode, expr, op) =>
            val vExpr = valueifyProcessor(refCtx, expr, input)
            op.sourceText match {
                case "!" => PrefixOp(PreOp.NOT, vExpr, BoolType)
            }
        case ref: MetaGrammar3Ast.Ref => ref match {
            case MetaGrammar3Ast.ValRef(astNode, idx, condSymPath0) =>
                val condSymPath = condSymPath0.getOrElse(List())
                val idxValue = idx.sourceText.toInt
                if (idxValue >= refCtx.size) throw new IllegalGrammar("")
                val symbolIdx = idxValue // TODO idxValue - refCtx(0~idxValue 전)까지 symbol의 갯수
                refCtx(symbolIdx) match {
                    case referredElem: MetaGrammar3Ast.Symbol =>
                        // valueifySymbol에서 나오는 symbol은 여기서는 의미 없음
                        valueifySymbol(refCtx, referredElem, condSymPathOf(condSymPath),
                            SeqElemAt(input, symbolIdx, typeOfElem(referredElem)))._1
                    case referredElem: MetaGrammar3Ast.Processor =>
                        check(condSymPath.isEmpty, "")
                        valueifyProcessor(refCtx, referredElem, SeqElemAt(input, symbolIdx, typeOfElem(referredElem)))
                }
            case MetaGrammar3Ast.RawRef(astNode, idx, condSymPath) => ???
        }
        case MetaGrammar3Ast.ExprParen(astNode, body) =>
            valueifyProcessor(refCtx, body, input)
        case MetaGrammar3Ast.BindExpr(astNode, ctx, binder) =>
            check(ctx.condSymPath.getOrElse(List()).isEmpty, "Binding context cannot have condSymPath")
            val idx = ctx.idx.sourceText.toInt
            check(idx < refCtx.size, "")

            // _1는 elem 속의 refCtx
            // _2는 elem이 Longest 등인 경우 그걸 어떻게 발라먹을지
            @tailrec def getBindingContext(elem: MetaGrammar3Ast.Elem, input: ValueifyExpr): (List[Elem], ValueifyExpr) = elem match {
                case symbol@MetaGrammar3Ast.Longest(astNode, choices) if choices.choices.size == 1 =>
                    getBindingContext(choices.choices.head, Unbind(symbol, input))
                case MetaGrammar3Ast.InPlaceChoices(astNode, choices) if choices.size == 1 =>
                    (choices.head.seq, input)
                // TODO Join symbol의 body가 Longest나 InPlaceChoices인 경우도 안될것 없는듯 "(A B C)&D {$0$2}" 하면 B나오면 되지 않을까?
                case _ => throw IllegalGrammar("Bind expression only can refer to Longest or InPlaceChoices symbol with only choice")
            }

            val bindingCtx = getBindingContext(refCtx(idx), input)
            assert(bindingCtx._1.nonEmpty)
            bindingCtx._1.last match {
                case lastElem: MetaGrammar3Ast.Symbol =>
                    // valueifySymbol에서 나오는 symbol은 여기서는 의미 없음
                    valueifySymbol(bindingCtx._1, lastElem, "", bindingCtx._2)._1
                case lastElem: MetaGrammar3Ast.Processor =>
                    valueifyProcessor(bindingCtx._1, lastElem, bindingCtx._2)
            }
        case MetaGrammar3Ast.NamedConstructExpr(astNode, typeName, params) =>
            val vParams = params.map(param => (param, valueifyProcessor(refCtx, param.expr, input)))
            NamedConstructCall(typeName, vParams, ClassType(typeName))
        case MetaGrammar3Ast.FuncCallOrConstructExpr(astNode, funcName, params) =>
            // TODO funcName이 backtick이면 무조건 ConstructExpr
            val vParams = params.getOrElse(List()).map(param => valueifyProcessor(refCtx, param, input))
            // TODO FuncCall or UnnamedConstructCall depending on its name
            FuncCall(funcName, vParams, FuncCallResultType(funcName, vParams))
        case MetaGrammar3Ast.ArrayExpr(astNode, elems) =>
            val vElems = elems.getOrElse(List()).map(elem => valueifyProcessor(refCtx, elem, input))
            val arrayElemType = unifyTypes(vElems.map(_.resultType).toSet)
            ArrayExpr(vElems, ArrayOf(arrayElemType))
        case literal: MetaGrammar3Ast.Literal => literal match {
            case MetaGrammar3Ast.NullLiteral(astNode) => NullLiteral
            case MetaGrammar3Ast.BoolLiteral(astNode, value) => BoolLiteral(value.sourceText.toBoolean)
            case MetaGrammar3Ast.CharLiteral(astNode, value) => CharLiteral(Escapes.terminalCharToChar(value))
            case MetaGrammar3Ast.StringLiteral(astNode, value) => StringLiteral(Escapes.stringCharsToString(value))
        }
        case MetaGrammar3Ast.CanonicalEnumValue(astNode, enumName, valueName) =>
            CanonicalEnumValue(enumName, valueName, EnumType(enumName))
        case MetaGrammar3Ast.ShortenedEnumValue(astNode, valueName) =>
            ShortenedEnumValue(valueName, UnspecifiedEnum(nextId()))
    }

    def valueifyRule(rhsList: List[MetaGrammar3Ast.Sequence]): (UnrollChoices, List[Symbols.Symbol]) = {
        val mappers = rhsList.map { rhs =>
            AstSymbolChoice(rhs) -> collectSequenceAndReturn(rhs, valueifySequence(rhs.seq, InputNode))
        }.toMap
        val mappings = mappers.map(mapper => mapper._1 -> mapper._2._1)
        val returnType = unifyTypes(mappers.map(_._2._1.resultType))
        (UnrollChoices(InputNode, mappings.toMap, returnType), mappers.map(_._2._2).toList)
    }
}
