package com.giyeok.jparser.metalang3a

import com.giyeok.jparser.metalang2.generated.MetaGrammar3Ast
import com.giyeok.jparser.metalang2.generated.MetaGrammar3Ast.ValRef
import com.giyeok.jparser.metalang3a.ClassInfoCollector.{ClassParamSpec, ClassSpec}
import com.giyeok.jparser.metalang3a.MetaLanguage3.check
import com.giyeok.jparser.metalang3a.ValuefyExpr._
import com.giyeok.jparser.{Grammar, Symbols}

import scala.collection.immutable.{ListMap, ListSet}

class GrammarTransformer(val grammarDef: MetaGrammar3Ast.Grammar, implicit private val errorCollector: ErrorCollector) {
    // grammar를 traverse하면서
    // nonterminal -> (Symbol, UnrollChoices)
    // className -> [class construct calls]
    // className -> [parameter specs]
    // className -> {superTypes}
    // className -> {subTypes}
    // nonterminal -> Type
    // 나중에) 사용자가 지정한 ValuefyExpr의 Type

    private var _classInfoCollector = ClassInfoCollector.empty
    private var _nonterminalInfoCollector = NonterminalInfoCollector.empty
    private var _nonterminalSymbols = Map[String, List[Symbols.Symbol]]()
    private var _nonterminalValuefyExprs = Map[String, UnrollChoices]()

    def classInfo: ClassInfoCollector = _classInfoCollector

    def nonterminalInfo: NonterminalInfoCollector = _nonterminalInfoCollector

    def nonterminalSymbols: Map[String, List[Symbols.Symbol]] = _nonterminalSymbols

    def nonterminalValuefyExprs: Map[String, UnrollChoices] = _nonterminalValuefyExprs

    def startNonterminalName(): String = grammarDef.defs.collectFirst {
        case MetaGrammar3Ast.Rule(_, lhs, _) => stringNameOf(lhs.name)
    }.get

    def grammar(grammarName: String): Grammar = new Grammar {
        val name: String = grammarName
        val rules: RuleMap = ListMap.from(_nonterminalSymbols.map(rule => rule._1 -> ListSet.from(rule._2)))
        val startSymbol: Symbols.Nonterminal = Symbols.Nonterminal(startNonterminalName())
    }

    // ID는 UnspecifiedEnum 객체 생성시 필요
    private var idCounter: Int = 0

    private def nextId(): Int = {
        idCounter += 1
        idCounter
    }

    private def stringNameOf(name: MetaGrammar3Ast.Nonterminal): String = stringNameOf(name.name)

    private def stringNameOf(name: MetaGrammar3Ast.NonterminalName): String = name.name.sourceText

    private def stringNameOf(name: MetaGrammar3Ast.TypeName): String = name.name.sourceText

    private def stringNameOf(name: MetaGrammar3Ast.ParamName): String = name.name.sourceText

    private def stringNameOf(name: MetaGrammar3Ast.TypeOrFuncName): String = name.name.sourceText

    private def stringNameOf(name: MetaGrammar3Ast.EnumTypeName): String = name.name.sourceText

    private def stringNameOf(name: MetaGrammar3Ast.EnumValueName): String = name.name.sourceText

    private def typeOf(typeDesc: MetaGrammar3Ast.NonNullTypeDesc): Type = typeDesc match {
        case typeDef: MetaGrammar3Ast.TypeDef => addTypeDef(typeDef)
        case MetaGrammar3Ast.ArrayTypeDesc(_, elemType) => Type.ArrayOf(typeOf(elemType))
        case valueType: MetaGrammar3Ast.ValueType =>
            valueType match {
                case MetaGrammar3Ast.BooleanType(_) => Type.BoolType
                case MetaGrammar3Ast.CharType(_) => Type.CharType
                case MetaGrammar3Ast.StringType(_) => Type.StringType
            }
        case MetaGrammar3Ast.AnyType(_) => Type.AnyType
        case name: MetaGrammar3Ast.EnumTypeName =>
            val enumName = stringNameOf(name)
            _classInfoCollector = _classInfoCollector.addEnumTypeName(enumName)
            Type.EnumType(enumName)
        case name: MetaGrammar3Ast.TypeName =>
            val className = stringNameOf(name)
            _classInfoCollector = _classInfoCollector.addClassName(className)
            Type.ClassType(className)
    }

    private def typeOf(typeDesc: MetaGrammar3Ast.TypeDesc): Type = {
        val t = typeOf(typeDesc.typ)
        if (typeDesc.optional.isDefined) Type.OptionalOf(t) else t
    }

    private def addTypeDef(typeDef: MetaGrammar3Ast.TypeDef): Type = {
        def addClassDef(classDef: MetaGrammar3Ast.ClassDef): Type.ClassType = classDef match {
            case MetaGrammar3Ast.AbstractClassDef(astNode, name, supers) =>
                val className = stringNameOf(name)
                supers.foreach { someSupers =>
                    _classInfoCollector = _classInfoCollector.addClassSuperTypes(className, someSupers.map(stringNameOf))
                }
                Type.ClassType(className)
            case MetaGrammar3Ast.ConcreteClassDef(astNode, name, supers, params) =>
                val className = stringNameOf(name)
                supers.foreach { someSupers =>
                    _classInfoCollector = _classInfoCollector.addClassSuperTypes(className, someSupers.map(stringNameOf))
                }
                params.foreach { someParams =>
                    _classInfoCollector = _classInfoCollector.addClassParamSpecs(className,
                        ClassSpec(someParams.map(p =>
                            ClassParamSpec(stringNameOf(p.name), p.typeDesc.map(typeOf)))))
                }
                Type.ClassType(className)
        }

        def addSuperDef(superDef: MetaGrammar3Ast.SuperDef): Type.ClassType = {
            val MetaGrammar3Ast.SuperDef(astNode, typeName, subs, supers) = superDef
            val className = stringNameOf(typeName)
            supers.flatten.foreach { someSupers =>
                _classInfoCollector = _classInfoCollector.addClassSuperTypes(className, someSupers.map(stringNameOf))
            }
            val subTypes = subs.getOrElse(List()).map {
                case classDef: MetaGrammar3Ast.ClassDef => addClassDef(classDef).name
                case superDef: MetaGrammar3Ast.SuperDef => addSuperDef(superDef).name
                case name: MetaGrammar3Ast.TypeName => stringNameOf(name)
            }
            _classInfoCollector = _classInfoCollector.addClassSubTypes(className, subTypes)
            Type.ClassType(className)
        }

        typeDef match {
            case classDef: MetaGrammar3Ast.ClassDef => addClassDef(classDef)
            case superDef: MetaGrammar3Ast.SuperDef => addSuperDef(superDef)
            case MetaGrammar3Ast.EnumTypeDef(_, name, values) =>
                val enumTypeName = stringNameOf(name)
                val enumValueNames = values.map(_.sourceText)
                _classInfoCollector = _classInfoCollector.addEnumType(enumTypeName, enumValueNames)
                Type.EnumType(enumTypeName)
        }
    }

    private def proxy(pair: (ValuefyExpr, Symbols.Symbol)): (ValuefyExpr, Symbols.AtomicSymbol) = pair._2 match {
        case symbol: Symbols.AtomicSymbol => (pair._1, symbol)
        case seq: Symbols.Sequence =>
            val proxySymbol = Symbols.Proxy(seq)
            (Unbind(proxySymbol, pair._1), proxySymbol)
    }

    private def valuefySymbol(symbolAst: MetaGrammar3Ast.Symbol, condSymPath: String, input: ValuefyExpr): (ValuefyExpr, Symbols.Symbol) = symbolAst match {
        case MetaGrammar3Ast.JoinSymbol(_, body, join) =>
            val condSymPathTail = if (condSymPath.isEmpty) "" else condSymPath.tail
            val vBody = proxy(valuefySymbol(body, condSymPathTail, input))
            val vJoin = proxy(valuefySymbol(join, condSymPathTail, input))
            val joinSymbol = Symbols.Join(vBody._2, vJoin._2)
            val useBody = condSymPath.isEmpty || condSymPath.charAt(0) == '<'
            if (useBody) (JoinBody(joinSymbol, vBody._1), joinSymbol)
            else (JoinCond(joinSymbol, vJoin._1), joinSymbol)
        case MetaGrammar3Ast.ExceptSymbol(_, body, except) =>
            check(condSymPath.isEmpty, "Except cannot be referred with condSymPath")
            val vBody = proxy(valuefySymbol(body, "", input))
            val vExcept = proxy(valuefySymbol(except, "", input))
            val exceptSymbol = Symbols.Except(vBody._2, vExcept._2)
            (Unbind(exceptSymbol, input), exceptSymbol)
        case MetaGrammar3Ast.FollowedBy(_, followedBy) =>
            check(condSymPath.isEmpty, "FollowedBy cannot be referred with condSymPath")
            ???
        case MetaGrammar3Ast.NotFollowedBy(_, notFollowedBy) =>
            check(condSymPath.isEmpty, "NotFollowedBy cannot be referred with condSymPath")
            ???
        case MetaGrammar3Ast.Optional(_, body) =>
            check(condSymPath.isEmpty, "Optional cannot be referred with condSymPath")
            val vBody = proxy(valuefySymbol(body, "", input))
            val emptySeq = Symbols.Proxy(Symbols.Sequence(Seq()))
            val oneofSymbol = Symbols.OneOf(ListSet(vBody._2, emptySeq))
            (Unbind(oneofSymbol, UnrollChoices(Map(emptySeq -> NullLiteral, vBody._2 -> vBody._1))), oneofSymbol)
        case MetaGrammar3Ast.RepeatFromZero(_, body) =>
            check(condSymPath.isEmpty, "RepeatFromZero cannot be referred with condSymPath")
            val vBody = proxy(valuefySymbol(body, "", input))
            val repeatSymbol = Symbols.Repeat(vBody._2, 0)
            (UnrollRepeatFromZero(vBody._1), repeatSymbol)
        case MetaGrammar3Ast.RepeatFromOne(_, body) =>
            check(condSymPath.isEmpty, "RepeatFromOne cannot be referred with condSymPath")
            val vBody = proxy(valuefySymbol(body, "", input))
            val repeatSymbol = Symbols.Repeat(vBody._2, 1)
            (UnrollRepeatFromOne(vBody._1), repeatSymbol)
        case MetaGrammar3Ast.InPlaceChoices(_, choices) =>
            check(condSymPath.isEmpty, "InPlaceChoices cannot be referred with condSymPath")
            val vChoices = choices.map(c => proxy(valuefySymbol(c, "", input)))
            val oneofSymbol = Symbols.OneOf(ListSet.from(vChoices.map(_._2)))
            (UnrollChoices(vChoices.map(c => oneofSymbol -> c._1).toMap), oneofSymbol)
        case MetaGrammar3Ast.Longest(_, choices) =>
            check(condSymPath.isEmpty, "Longest cannot be referred with condSymPath")
            val vChoices = proxy(valuefySymbol(choices, "", input))
            val longestSymbol = Symbols.Longest(vChoices._2)
            (Unbind(vChoices._2, vChoices._1), longestSymbol)
        case MetaGrammar3Ast.EmptySeq(_) =>
            check(condSymPath.isEmpty, "EmptySeq cannot be referred with condSymPath")
            (input, Symbols.Sequence(Seq()))
        case MetaGrammar3Ast.Sequence(_, seq) =>
            check(condSymPath.isEmpty, "Sequence cannot be referred with condSymPath")
            valuefySequence(seq, input)
        case MetaGrammar3Ast.Nonterminal(_, name) =>
            check(condSymPath.isEmpty, "Nonterminal cannot be referred with condSymPath")
            val nonterminalName = stringNameOf(name)
            val nonterminalSymbol = Symbols.Nonterminal(nonterminalName)
            (Unbind(nonterminalSymbol, MatchNonterminal(nonterminalName)), nonterminalSymbol)
        case MetaGrammar3Ast.StringSymbol(_, value) =>
            check(condSymPath.isEmpty, "StringSymbol cannot be referred with condSymPath")
            val stringSymbol = Symbols.Proxy(Symbols.Sequence(value.map(TerminalUtil.stringCharToChar).map(Symbols.ExactChar)))
            (StringLiteral(TerminalUtil.stringCharsToString(value)), stringSymbol)
        case terminal: MetaGrammar3Ast.Terminal =>
            check(condSymPath.isEmpty, "NotFollowedBy cannot be referred with condSymPath")
            (input, TerminalUtil.terminalToSymbol(terminal))
        case MetaGrammar3Ast.TerminalChoice(_, choices) =>
            check(condSymPath.isEmpty, "NotFollowedBy cannot be referred with condSymPath")
            (input, TerminalUtil.terminalChoicesToSymbol(choices))
    }

    private def symbolIndexFrom(refCtx: List[MetaGrammar3Ast.Elem], index: Int): Int = index // TODO processor 갯수 빼기

    private def valuefyProcessor(refCtx: List[MetaGrammar3Ast.Elem], processor: MetaGrammar3Ast.Processor, input: ValuefyExpr): ValuefyExpr = processor match {
        case MetaGrammar3Ast.ElvisOp(_, value, ifNull) =>
            val vValue = valuefyProcessor(refCtx, value, input)
            val vIfNull = valuefyProcessor(refCtx, ifNull, input)
            ElvisOp(vValue, vIfNull)
        case MetaGrammar3Ast.BinOp(_, op, lhs, rhs) =>
            val vLhs = valuefyProcessor(refCtx, lhs, input)
            val vRhs = valuefyProcessor(refCtx, rhs, input)
            val opType = op.sourceText match {
                case "+" => BinOpType.ADD
                case "==" => BinOpType.EQ
                case "!=" => BinOpType.NE
                case "&&" => BinOpType.BOOL_AND
                case "||" => BinOpType.BOOL_OR
            }
            BinOp(opType, vLhs, vRhs)
        case MetaGrammar3Ast.PrefixOp(_, expr, op) =>
            val vExpr = valuefyProcessor(refCtx, expr, input)
            val opType = op.sourceText match {
                case "!" => PreOpType.NOT
            }
            PreOp(opType, vExpr)
        case ref: MetaGrammar3Ast.Ref => ref match {
            case MetaGrammar3Ast.ValRef(_, idx0, condSymPath0) =>
                val idx = idx0.sourceText.toInt
                check(idx < refCtx.size, "reference index out of range")
                val condSymPath = condSymPath0.getOrElse(List())
                refCtx(idx) match {
                    case symbol: MetaGrammar3Ast.Symbol =>
                        val symbolIdx = symbolIndexFrom(refCtx, idx)
                        SeqElemAt(symbolIdx, valuefySymbol(symbol, condSymPath.map(_.sourceText).mkString, input)._1)
                    case processor: MetaGrammar3Ast.Processor =>
                        check(condSymPath.isEmpty, "Ref to processor cannot have cond sym path")
                        valuefyProcessor(refCtx, processor, input)
                }
            case MetaGrammar3Ast.RawRef(_, idx0, condSymPath0) =>
                val idx = idx0.sourceText.toInt
                check(idx < refCtx.size, "reference index out of range")
                val condSymPath = condSymPath0.getOrElse(List())
                refCtx(idx) match {
                    case _: MetaGrammar3Ast.Symbol =>
                        val symbolIdx = symbolIndexFrom(refCtx, idx)
                        check(condSymPath.isEmpty, "RawRef with cond sym path not implemented")
                        SeqElemAt(symbolIdx, input)
                    case _: MetaGrammar3Ast.Processor =>
                        errorCollector.addError("RawRef cannot refer to processor", ref)
                        // throw IllegalGrammar("RawRef cannot refer to processor")
                        InputNode
                }
        }
        case MetaGrammar3Ast.ExprParen(_, body) => valuefyProcessor(refCtx, body, input)
        case MetaGrammar3Ast.BindExpr(_, ctx, binder) =>
            val ValRef(_, idx0, condSymPath0) = ctx
            val idx = idx0.sourceText.toInt
            check(idx < refCtx.size, "reference index out of range")
            val condSymPath = condSymPath0.getOrElse(List())
            check(condSymPath.isEmpty, "BindExpr with cond sym path not implemented")
            refCtx(idx) match {
                case symbol: MetaGrammar3Ast.Symbol =>
                    // 이 부분은 valuefySequence에서 마지막 elem의 종류에 따라 SeqElem을 넣는 것 때문에
                    // valuefySymbol을 재사용하지 못하고 valuefySymbol과 거의 같은 코드가 중복돼서 들어갔음
                    // TODO 고칠 수 있으면 좋을텐데..
                    def processBindExpr(symbol: MetaGrammar3Ast.Symbol): ValuefyExpr =
                        symbol match {
                            case MetaGrammar3Ast.Optional(_, body) => ???
                            case MetaGrammar3Ast.RepeatFromZero(_, body) => ???
                            case MetaGrammar3Ast.RepeatFromOne(_, body) => ???
                            case MetaGrammar3Ast.InPlaceChoices(_, choices) if choices.size == 1 =>
                                Unbind(valuefySymbol(symbol, "", InputNode)._2, processBindExpr(choices.head))
                            case MetaGrammar3Ast.Longest(_, choices) =>
                                Unbind(valuefySymbol(symbol, "", InputNode)._2, processBindExpr(choices))
                            case MetaGrammar3Ast.Sequence(_, seq) =>
                                val (processor, seqSymbol) = valuefySequence(seq :+ binder.asInstanceOf[MetaGrammar3Ast.Processor], input)
                                proxy(Unbind(seqSymbol, processor), seqSymbol)._1
                            case _ =>
                                // throw IllegalGrammar("Unsupported binding context")
                                errorCollector.addError("Unsupported binding context", processor)
                                InputNode
                        }

                    SeqElemAt(symbolIndexFrom(refCtx, idx), processBindExpr(symbol))
                case _: MetaGrammar3Ast.Processor =>
                    // throw IllegalGrammar("BindExpr cannot refer to processor")
                    errorCollector.addError("BindExpr cannot refer to processor", processor)
                    InputNode
            }
        case MetaGrammar3Ast.NamedConstructExpr(_, typeName, params, supers0) =>
            val className = stringNameOf(typeName)
            // add className->supers
            supers0.flatten.foreach { supers =>
                _classInfoCollector = _classInfoCollector.addClassSuperTypes(className, supers.map(stringNameOf))
            }
            // add className->params
            val paramSpecs = params.map(p => ClassParamSpec(stringNameOf(p.name), p.typeDesc.map(typeOf)))
            _classInfoCollector = _classInfoCollector.addClassParamSpecs(className, ClassSpec(paramSpecs))
            // add className->callers
            val vParams = params.map(p => valuefyProcessor(refCtx, p.expr, input))
            _classInfoCollector = _classInfoCollector.addClassConstructCall(className, vParams)
            ConstructCall(className, vParams)
        case MetaGrammar3Ast.FuncCallOrConstructExpr(_, funcName, params0) =>
            val params = params0.getOrElse(List())
            val vParams = params.map(valuefyProcessor(refCtx, _, input))
            stringNameOf(funcName) match {
                case "ispresent" => FuncCall(FuncType.IsPresent, vParams)
                case "isempty" => FuncCall(FuncType.IsEmpty, vParams)
                case "chr" => FuncCall(FuncType.Chr, vParams)
                case "str" => FuncCall(FuncType.Str, vParams)
                case className =>
                    // construct이면 add className->params
                    _classInfoCollector = _classInfoCollector.addClassConstructCall(className, vParams)
                    ConstructCall(className, vParams)
            }
        case MetaGrammar3Ast.ArrayExpr(_, elems0) =>
            val elems = elems0.getOrElse(List())
            val vElems = elems.map(valuefyProcessor(refCtx, _, input))
            ArrayExpr(vElems)
        case literal: MetaGrammar3Ast.Literal => literal match {
            case MetaGrammar3Ast.NullLiteral(_) => NullLiteral
            case MetaGrammar3Ast.BoolLiteral(_, value) => BoolLiteral(value.sourceText.toBoolean)
            case MetaGrammar3Ast.CharLiteral(_, value) => CharLiteral(TerminalUtil.terminalCharToChar(value))
            case MetaGrammar3Ast.StringLiteral(_, value) => StringLiteral(TerminalUtil.stringCharsToString(value))
        }
        case enumValue: MetaGrammar3Ast.AbstractEnumValue => enumValue match {
            case MetaGrammar3Ast.CanonicalEnumValue(_, enumName, valueName) =>
                val enumTypeName = stringNameOf(enumName)
                val enumValueName = stringNameOf(valueName)
                // TODO enumTypeName의 value로 enumValueName 추가
                CanonicalEnumValue(enumTypeName, enumValueName)
            case MetaGrammar3Ast.ShortenedEnumValue(_, valueName) =>
                val enumId = nextId()
                val enumValueName = stringNameOf(valueName)
                // TODO enumId의 value로 enumValueName 추가
                ShortenedEnumValue(enumId, enumValueName)
        }
    }

    private def valuefySequence(seq: List[MetaGrammar3Ast.Elem], input: ValuefyExpr): (ValuefyExpr, Symbols.Symbol) = {
        val elems = seq map {
            case symbol: MetaGrammar3Ast.Symbol =>
                val p = proxy(valuefySymbol(symbol, "", input))
                (p._1, Some(p._2))
            case processor: MetaGrammar3Ast.Processor =>
                (valuefyProcessor(seq, processor, input), None)
        }
        val symbols = elems.flatMap(_._2)
        val lastProcessor = elems.last._1
        val seqSymbol = Symbols.Sequence(symbols)
        seq.last match {
            case _: MetaGrammar3Ast.Symbol => (SeqElemAt(symbols.size - 1, lastProcessor), seqSymbol)
            case _: MetaGrammar3Ast.Processor => (lastProcessor, seqSymbol)
        }
    }

    private def addNonterminalDerivation(nonterminalName: String, nonterminalType: Option[Type], rhs: List[MetaGrammar3Ast.Sequence]): Unit = {
        nonterminalType.foreach { someNonterminalType =>
            _nonterminalInfoCollector = _nonterminalInfoCollector.setNonterminalType(nonterminalName, someNonterminalType)
        }
        val vRhs = rhs.map { derivation =>
            val (valuefyExpr, seqSymbol) = valuefySequence(derivation.seq, InputNode)
            _nonterminalInfoCollector = _nonterminalInfoCollector.addNonterminalExpr(nonterminalName, valuefyExpr)
            (valuefyExpr, seqSymbol)
        }
        _nonterminalSymbols += nonterminalName -> vRhs.map(_._2)
        _nonterminalValuefyExprs += nonterminalName -> UnrollChoices(vRhs.map { v => v._2 -> v._1 }.toMap)
    }

    private def traverseDef(definition: MetaGrammar3Ast.Def): Unit = definition match {
        case MetaGrammar3Ast.Rule(_, lhs, rhs) =>
            addNonterminalDerivation(stringNameOf(lhs.name), lhs.typeDesc.map(typeOf), rhs)
        case typeDef: MetaGrammar3Ast.TypeDef =>
            addTypeDef(typeDef)
    }

    grammarDef.defs.foreach(traverseDef)

    def validate(): Unit = {
        // classInfoCollector와 nonterminalInfoCollector에 문제가 있으면 exception throw
        // classInfoCollector와 nonterminalInfoCollector에서 바로 throw하지 않는 것은 ParserStudio등에서 AST의 valid 여부와 관계 없이 grammar만 얻고 싶은 경우가 있기 때문
    }
}
