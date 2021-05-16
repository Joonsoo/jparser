package com.giyeok.jparser.metalang3a

import com.giyeok.jparser.metalang3a.ClassInfoCollector.{ClassParamSpec, ClassSpec}
import com.giyeok.jparser.metalang3a.MetaLanguage3.check
import com.giyeok.jparser.metalang3a.ValuefyExpr._
import com.giyeok.jparser.metalang3a.generated.MetaLang3Ast
import com.giyeok.jparser.metalang3a.generated.MetaLang3Ast.CondSymDir.BODY
import com.giyeok.jparser.{Grammar, Symbols}

import scala.collection.immutable.{ListMap, ListSet}

class GrammarTransformer(val grammarDef: MetaLang3Ast.Grammar, implicit private val errorCollector: ErrorCollector) {
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
    case MetaLang3Ast.Rule(lhs, _) => stringNameOf(lhs.name)
  }.get

  def grammar(grammarName: String): Grammar = new Grammar {
    val name: String = grammarName
    val rules: RuleMap = ListMap.from(_nonterminalSymbols.map(rule => rule._1 -> rule._2))
    val startSymbol: Symbols.Nonterminal = Symbols.Nonterminal(startNonterminalName())
  }

  // ID는 UnspecifiedEnum 객체 생성시 필요
  private var idCounter: Int = 0

  private def nextId(): Int = {
    idCounter += 1
    idCounter
  }

  private def stringNameOf(name: MetaLang3Ast.Nonterminal): String = name.name.name

  private def stringNameOf(name: MetaLang3Ast.NonterminalName): String = name.name

  private def stringNameOf(name: MetaLang3Ast.TypeName): String = name.name

  private def stringNameOf(name: MetaLang3Ast.ParamName): String = name.name

  private def stringNameOf(name: MetaLang3Ast.TypeOrFuncName): String = name.name

  private def stringNameOf(name: MetaLang3Ast.EnumTypeName): String = name.name

  private def stringNameOf(name: MetaLang3Ast.EnumValueName): String = name.name

  private def typeOf(typeDesc: MetaLang3Ast.NonNullTypeDesc): Type = typeDesc match {
    case typeDef: MetaLang3Ast.TypeDef => addTypeDef(typeDef)
    case MetaLang3Ast.ArrayTypeDesc(elemType) => Type.ArrayOf(typeOf(elemType))
    case valueType: MetaLang3Ast.ValueType =>
      valueType match {
        case MetaLang3Ast.BooleanType() => Type.BoolType
        case MetaLang3Ast.CharType() => Type.CharType
        case MetaLang3Ast.StringType() => Type.StringType
      }
    case MetaLang3Ast.AnyType() => Type.AnyType
    case name: MetaLang3Ast.EnumTypeName =>
      val enumName = stringNameOf(name)
      _classInfoCollector = _classInfoCollector.addEnumTypeName(enumName)
      Type.EnumType(enumName)
    case name: MetaLang3Ast.TypeName =>
      val className = stringNameOf(name)
      _classInfoCollector = _classInfoCollector.addClassName(className)
      Type.ClassType(className)
  }

  private def typeOf(typeDesc: MetaLang3Ast.TypeDesc): Type = {
    val t = typeOf(typeDesc.typ)
    if (typeDesc.optional) Type.OptionalOf(t) else t
  }

  private def addTypeDef(typeDef: MetaLang3Ast.TypeDef): Type = {
    def addClassDef(classDef: MetaLang3Ast.ClassDef): Type.ClassType = classDef match {
      case MetaLang3Ast.AbstractClassDef(name, supers) =>
        val className = stringNameOf(name)
        _classInfoCollector = _classInfoCollector.addClassSuperTypes(className, supers.map(stringNameOf))
        Type.ClassType(className)
      case MetaLang3Ast.ConcreteClassDef(name, supers, params) =>
        val className = stringNameOf(name)
        supers.foreach { someSupers =>
          _classInfoCollector = _classInfoCollector.addClassSuperTypes(className, someSupers.map(stringNameOf))
        }
        _classInfoCollector = _classInfoCollector.addClassParamSpecs(className,
          ClassSpec(params.map(p =>
            ClassParamSpec(stringNameOf(p.name), p.typeDesc.map(typeOf)))))
        Type.ClassType(className)
    }

    def addSuperDef(superDef: MetaLang3Ast.SuperDef): Type.ClassType = {
      val MetaLang3Ast.SuperDef(typeName, subs, supers0) = superDef
      val className = stringNameOf(typeName)
      supers0 match {
        case Some(supers) =>
          _classInfoCollector = _classInfoCollector.addClassSuperTypes(className, supers.map(stringNameOf))
        case None =>
      }
      val subTypes = subs.getOrElse(List()).map {
        case classDef: MetaLang3Ast.ClassDef => addClassDef(classDef).name
        case superDef: MetaLang3Ast.SuperDef => addSuperDef(superDef).name
        case name: MetaLang3Ast.TypeName => stringNameOf(name)
      }
      _classInfoCollector = _classInfoCollector.addClassSubTypes(className, subTypes)
      Type.ClassType(className)
    }

    typeDef match {
      case classDef: MetaLang3Ast.ClassDef => addClassDef(classDef)
      case superDef: MetaLang3Ast.SuperDef => addSuperDef(superDef)
      case MetaLang3Ast.EnumTypeDef(name, enumValueNames) =>
        val enumTypeName = stringNameOf(name)
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

  private def valuefySymbol(symbolAst: MetaLang3Ast.Symbol, condSymPath: List[MetaLang3Ast.CondSymDir.Value], input: ValuefyExpr): (ValuefyExpr, Symbols.Symbol) = symbolAst match {
    case MetaLang3Ast.JoinSymbol(body, join) =>
      val condSymPathTail = if (condSymPath.isEmpty) List() else condSymPath.tail
      val vBody = proxy(valuefySymbol(body, condSymPathTail, input))
      val vJoin = proxy(valuefySymbol(join, condSymPathTail, input))
      val joinSymbol = Symbols.Join(vBody._2, vJoin._2)
      val useBody = condSymPath.isEmpty || condSymPath.head == BODY
      if (useBody) (Unbind(joinSymbol, JoinBody(vBody._1)), joinSymbol)
      else (Unbind(joinSymbol, JoinCond(vJoin._1)), joinSymbol)
    case MetaLang3Ast.ExceptSymbol(body, except) =>
      check(condSymPath.isEmpty, "Except cannot be referred with condSymPath")
      val vBody = proxy(valuefySymbol(body, List(), input))
      val vExcept = proxy(valuefySymbol(except, List(), input))
      val exceptSymbol = Symbols.Except(vBody._2, vExcept._2)
      (Unbind(exceptSymbol, vBody._1), exceptSymbol)
    case MetaLang3Ast.FollowedBy(followedBy) =>
      check(condSymPath.isEmpty, "FollowedBy cannot be referred with condSymPath")
      val vFollowedBy = proxy(valuefySymbol(followedBy, List(), input))
      (NullLiteral, Symbols.LookaheadIs(vFollowedBy._2))
    case MetaLang3Ast.NotFollowedBy(notFollowedBy) =>
      check(condSymPath.isEmpty, "NotFollowedBy cannot be referred with condSymPath")
      val vNotFollowedBy = proxy(valuefySymbol(notFollowedBy, List(), input))
      (NullLiteral, Symbols.LookaheadExcept(vNotFollowedBy._2))
    case MetaLang3Ast.Optional(body) =>
      check(condSymPath.isEmpty, "Optional cannot be referred with condSymPath")
      val vBody = proxy(valuefySymbol(body, List(), input))
      val emptySeq = Symbols.Proxy(Symbols.Sequence(Seq()))
      val oneofSymbol = Symbols.OneOf(ListSet(vBody._2, emptySeq))
      (Unbind(oneofSymbol, UnrollChoices(Map(emptySeq -> NullLiteral, vBody._2 -> vBody._1))), oneofSymbol)
    case MetaLang3Ast.RepeatFromZero(body) =>
      check(condSymPath.isEmpty, "RepeatFromZero cannot be referred with condSymPath")
      val vBody = proxy(valuefySymbol(body, List(), input))
      val repeatSymbol = Symbols.Repeat(vBody._2, 0)
      (UnrollRepeatFromZero(vBody._1), repeatSymbol)
    case MetaLang3Ast.RepeatFromOne(body) =>
      check(condSymPath.isEmpty, "RepeatFromOne cannot be referred with condSymPath")
      val vBody = proxy(valuefySymbol(body, List(), input))
      val repeatSymbol = Symbols.Repeat(vBody._2, 1)
      (UnrollRepeatFromOne(vBody._1), repeatSymbol)
    case MetaLang3Ast.InPlaceChoices(choices) =>
      check(condSymPath.isEmpty, "InPlaceChoices cannot be referred with condSymPath")
      val vChoices = choices.map(c => proxy(valuefySymbol(c, List(), input)))
      val oneofSymbol = Symbols.OneOf(ListSet.from(vChoices.map(_._2)))
      (Unbind(oneofSymbol, UnrollChoices(vChoices.map(c => c._2 -> c._1).toMap)), oneofSymbol)
    case MetaLang3Ast.Longest(choices) =>
      check(condSymPath.isEmpty, "Longest cannot be referred with condSymPath")
      val vChoices = proxy(valuefySymbol(choices, List(), input))
      val longestSymbol = Symbols.Longest(vChoices._2)
      (Unbind(longestSymbol, vChoices._1), longestSymbol)
    case MetaLang3Ast.EmptySeq() =>
      check(condSymPath.isEmpty, "EmptySeq cannot be referred with condSymPath")
      (NullLiteral, Symbols.Sequence())
    case MetaLang3Ast.Sequence(seq) =>
      check(condSymPath.isEmpty, "Sequence cannot be referred with condSymPath")
      val vSeq = valuefySequence(seq, input)
      (Unbind(vSeq._2, vSeq._1), vSeq._2)
    case MetaLang3Ast.Nonterminal(name) =>
      check(condSymPath.isEmpty, "Nonterminal cannot be referred with condSymPath")
      val nonterminalName = stringNameOf(name)
      val nonterminalSymbol = Symbols.Nonterminal(nonterminalName)
      (Unbind(nonterminalSymbol, MatchNonterminal(nonterminalName)), nonterminalSymbol)
    case MetaLang3Ast.StringSymbol(value) =>
      check(condSymPath.isEmpty, "StringSymbol cannot be referred with condSymPath")
      val stringSymbol = Symbols.Proxy(Symbols.Sequence(value.map(TerminalUtil.stringCharToChar).map(Symbols.ExactChar)))
      (StringLiteral(TerminalUtil.stringCharsToString(value)), stringSymbol)
    case terminal: MetaLang3Ast.Terminal =>
      check(condSymPath.isEmpty, "Terminal cannot be referred with condSymPath")
      val terminalSymbol = TerminalUtil.terminalToSymbol(terminal)
      (Unbind(terminalSymbol, CharFromTerminalLiteral), terminalSymbol)
    case MetaLang3Ast.TerminalChoice(choices) =>
      check(condSymPath.isEmpty, "TerminalChoice cannot be referred with condSymPath")
      val terminalSymbol = TerminalUtil.terminalChoicesToSymbol(choices)
      (Unbind(terminalSymbol, CharFromTerminalLiteral), terminalSymbol)
  }

  private def symbolIndexFrom(refCtx: List[MetaLang3Ast.Elem], index: Int): Int =
    index - refCtx.slice(0, index).count(_.isInstanceOf[MetaLang3Ast.Processor]) // index 앞에 있는 processor 갯수 빼기

  private def valuefyPExpr(refCtx: List[MetaLang3Ast.Elem], processor: MetaLang3Ast.PExpr, input: ValuefyExpr): ValuefyExpr = processor match {
    case MetaLang3Ast.TypedPExpr(body, typ) =>
      // TODO typ 처리
      valuefyPExpr(refCtx, body, input)
    case MetaLang3Ast.TernaryOp(cond, ifTrue, ifFalse) =>
      TernaryOp(valuefyPExpr(refCtx, cond, input),
        ifTrue = valuefyPExpr(refCtx, ifTrue, input),
        ifFalse = valuefyPExpr(refCtx, ifFalse, input))
    case MetaLang3Ast.ElvisOp(value, ifNull) =>
      val vValue = valuefyPExpr(refCtx, value, input)
      val vIfNull = valuefyPExpr(refCtx, ifNull, input)
      ElvisOp(vValue, vIfNull)
    case MetaLang3Ast.BinOp(op, lhs, rhs) =>
      val vLhs = valuefyPExpr(refCtx, lhs, input)
      val vRhs = valuefyPExpr(refCtx, rhs, input)
      val opType = op match {
        case com.giyeok.jparser.metalang3a.generated.MetaLang3Ast.Op.ADD => BinOpType.ADD
        case com.giyeok.jparser.metalang3a.generated.MetaLang3Ast.Op.EQ => BinOpType.EQ
        case com.giyeok.jparser.metalang3a.generated.MetaLang3Ast.Op.NE => BinOpType.NE
        case com.giyeok.jparser.metalang3a.generated.MetaLang3Ast.Op.AND => BinOpType.BOOL_AND
        case com.giyeok.jparser.metalang3a.generated.MetaLang3Ast.Op.OR => BinOpType.BOOL_OR
      }
      BinOp(opType, vLhs, vRhs)
    case MetaLang3Ast.PrefixOp(op, expr) =>
      val vExpr = valuefyPExpr(refCtx, expr, input)
      val opType = op match {
        case com.giyeok.jparser.metalang3a.generated.MetaLang3Ast.PreOp.NOT => PreOpType.NOT
      }
      PreOp(opType, vExpr)
    case ref: MetaLang3Ast.Ref => ref match {
      case MetaLang3Ast.ValRef(idx0, condSymPath0) =>
        val idx = idx0.toInt
        check(idx < refCtx.size, "reference index out of range")
        val condSymPath = condSymPath0.getOrElse(List())
        refCtx(idx) match {
          case symbol: MetaLang3Ast.Symbol =>
            val symbolIdx = symbolIndexFrom(refCtx, idx)
            SeqElemAt(symbolIdx, valuefySymbol(symbol, condSymPath, input)._1)
          case processor: MetaLang3Ast.Processor =>
            check(condSymPath.isEmpty, "Ref to processor cannot have cond sym path")
            valuefyProcessor(refCtx, processor, input)
        }
      case MetaLang3Ast.RawRef(idx0, condSymPath0) =>
        val idx = idx0.toInt
        check(idx < refCtx.size, "reference index out of range")
        val condSymPath = condSymPath0.getOrElse(List())
        refCtx(idx) match {
          case _: MetaLang3Ast.Symbol =>
            val symbolIdx = symbolIndexFrom(refCtx, idx)
            check(condSymPath.isEmpty, "RawRef with cond sym path not implemented")
            SeqElemAt(symbolIdx, input)
          case _: MetaLang3Ast.Processor =>
            errorCollector.addError("RawRef cannot refer to processor", ref.parseNode)
            // throw IllegalGrammar("RawRef cannot refer to processor")
            InputNode
        }
    }
    case MetaLang3Ast.ExprParen(body) => valuefyPExpr(refCtx, body, input)
    case MetaLang3Ast.BindExpr(ctx, binder) =>
      val MetaLang3Ast.ValRef(idx0, condSymPath0) = ctx
      val idx = idx0.toInt
      check(idx < refCtx.size, "reference index out of range")
      val condSymPath = condSymPath0.getOrElse(List())
      check(condSymPath.isEmpty, "BindExpr with cond sym path not implemented")
      refCtx(idx) match {
        case symbol: MetaLang3Ast.Symbol =>
          // 이 부분은 valuefySequence에서 마지막 elem의 종류에 따라 SeqElem을 넣는 것 때문에
          // valuefySymbol을 재사용하지 못하고 valuefySymbol과 거의 같은 코드가 중복돼서 들어갔음
          // TODO 고칠 수 있으면 좋을텐데..
          def processBindExpr(symbol: MetaLang3Ast.Symbol): ValuefyExpr =
            symbol match {
              case MetaLang3Ast.Optional(body) =>
                val vBody = proxy(valuefySymbol(body, List(), input))
                val emptySeq = Symbols.Proxy(Symbols.Sequence(Seq()))
                val oneofSymbol = Symbols.OneOf(ListSet(vBody._2, emptySeq))
                Unbind(oneofSymbol, UnrollChoices(Map(emptySeq -> NullLiteral,
                  vBody._2 -> processBindExpr(body))))
              case MetaLang3Ast.RepeatFromZero(body) =>
                UnrollRepeatFromZero(processBindExpr(body))
              case MetaLang3Ast.RepeatFromOne(body) =>
                UnrollRepeatFromOne(processBindExpr(body))
              case MetaLang3Ast.InPlaceChoices(choices) if choices.size == 1 =>
                // 어차피 choice가 하나이므로 UnrollChoices 대신 Unbind만 해도 됨
                Unbind(valuefySymbol(symbol, List(), InputNode)._2, processBindExpr(choices.head))
              case MetaLang3Ast.Longest(choices) =>
                Unbind(valuefySymbol(symbol, List(), InputNode)._2, processBindExpr(choices))
              case MetaLang3Ast.Sequence(seq) =>
                val (processor, seqSymbol) = valuefySequence(seq :+ binder.asInstanceOf[MetaLang3Ast.Processor], input)
                proxy(Unbind(seqSymbol, processor), seqSymbol)._1
              case _ =>
                // throw IllegalGrammar("Unsupported binding context")
                errorCollector.addError("Unsupported binding context", processor.parseNode)
                InputNode
            }

          SeqElemAt(symbolIndexFrom(refCtx, idx), processBindExpr(symbol))
        case _: MetaLang3Ast.Processor =>
          // throw IllegalGrammar("BindExpr cannot refer to processor")
          errorCollector.addError("BindExpr cannot refer to processor", processor.parseNode)
          InputNode
      }
    case MetaLang3Ast.NamedConstructExpr(typeName, params, supers0) =>
      val className = stringNameOf(typeName)
      // add className->supers
      supers0 match {
        case Some(supers) =>
          _classInfoCollector = _classInfoCollector.addClassSuperTypes(className, supers.map(stringNameOf))
        case None =>
      }
      // add className->params
      val paramSpecs = params.map(p => ClassParamSpec(stringNameOf(p.name), p.typeDesc.map(typeOf)))
      _classInfoCollector = _classInfoCollector.addClassParamSpecs(className, ClassSpec(paramSpecs))
      // add className->callers
      val vParams = params.map(p => valuefyPExpr(refCtx, p.expr, input))
      _classInfoCollector = _classInfoCollector.addClassConstructCall(className, vParams)
      ConstructCall(className, vParams)
    case MetaLang3Ast.FuncCallOrConstructExpr(funcName, params) =>
      val vParams = params.map(valuefyPExpr(refCtx, _, input))
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
    case MetaLang3Ast.ArrayExpr(elems) =>
      val vElems = elems.map(valuefyPExpr(refCtx, _, input))
      ArrayExpr(vElems)
    case literal: MetaLang3Ast.Literal => literal match {
      case MetaLang3Ast.NullLiteral() => NullLiteral
      case MetaLang3Ast.BoolLiteral(value) => BoolLiteral(value)
      case MetaLang3Ast.CharLiteral(value) => CharLiteral(TerminalUtil.terminalCharToChar(value))
      case MetaLang3Ast.StrLiteral(value) => StringLiteral(TerminalUtil.stringCharsToString(value))
    }
    case enumValue: MetaLang3Ast.AbstractEnumValue => enumValue match {
      case MetaLang3Ast.CanonicalEnumValue(enumName, valueName) =>
        val enumTypeName = stringNameOf(enumName)
        val enumValueName = stringNameOf(valueName)
        // enumTypeName의 value로 enumValueName 추가
        _classInfoCollector = _classInfoCollector.addCanonicalEnumValue(enumTypeName, enumValueName)
        CanonicalEnumValue(enumTypeName, enumValueName)
      case MetaLang3Ast.ShortenedEnumValue(valueName) =>
        val shortenedEnumTypeId = nextId()
        val enumValueName = stringNameOf(valueName)
        // enumId의 value로 enumValueName 추가
        _classInfoCollector = _classInfoCollector.addShortenedEnumValue(shortenedEnumTypeId, enumValueName)
        ShortenedEnumValue(shortenedEnumTypeId, enumValueName)
    }
  }

  private def valuefyProcessor(refCtx: List[MetaLang3Ast.Elem], processor: MetaLang3Ast.Processor, input: ValuefyExpr) =
    processor match {
      case MetaLang3Ast.ProcessorBlock(body) => valuefyPExpr(refCtx, body, input)
      case ref: MetaLang3Ast.Ref => valuefyPExpr(refCtx, ref, input)
    }

  private def valuefySequence(seq: List[MetaLang3Ast.Elem], input: ValuefyExpr): (ValuefyExpr, Symbols.Symbol) = {
    val elems = seq map {
      case symbol: MetaLang3Ast.Symbol =>
        val p = proxy(valuefySymbol(symbol, List(), input))
        (p._1, Some(p._2))
      case processor: MetaLang3Ast.Processor =>
        (valuefyProcessor(seq, processor, input), None)
    }
    val symbols = elems.flatMap(_._2)
    val lastProcessor = elems.last._1
    val seqSymbol = Symbols.Sequence(symbols)
    seq.last match {
      case _: MetaLang3Ast.Symbol => (SeqElemAt(symbols.size - 1, lastProcessor), seqSymbol)
      case _: MetaLang3Ast.Processor => (lastProcessor, seqSymbol)
    }
  }

  private def addNonterminalDerivation(nonterminalName: String, nonterminalType: Option[Type], rhs: List[MetaLang3Ast.Sequence]): Unit = {
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

  private def traverseDef(definition: MetaLang3Ast.Def): Unit = definition match {
    case MetaLang3Ast.Rule(lhs, rhs) =>
      addNonterminalDerivation(stringNameOf(lhs.name), lhs.typeDesc.map(typeOf), rhs)
    case typeDef: MetaLang3Ast.TypeDef =>
      addTypeDef(typeDef)
  }

  grammarDef.defs.foreach(traverseDef)

  def validate(): Unit = {
    // classInfoCollector와 nonterminalInfoCollector에 문제가 있으면 exception throw
    // classInfoCollector와 nonterminalInfoCollector에서 바로 throw하지 않는 것은 ParserStudio등에서 AST의 valid 여부와 관계 없이 grammar만 얻고 싶은 경우가 있기 때문
  }
}
