package com.giyeok.jparser.metalang3

import com.giyeok.jparser.metalang3.ClassInfoCollector.{ClassParamSpec, ClassSpec}
import com.giyeok.jparser.metalang3.MetaLanguage3.check
import com.giyeok.jparser.metalang3.ValuefyExpr._
import com.giyeok.jparser.metalang3.ast.MetaLang3Ast
import com.giyeok.jparser.metalang3.ast.MetaLang3Ast.CondSymDir.BODY
import com.giyeok.jparser.{Grammar, Symbols}

import scala.collection.immutable.{ListMap, ListSet}
import scala.collection.mutable

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

  private def proxy(pair: (ValuefyExpr, Symbols.Symbol)): (ValuefyExpr, Symbols.PlainAtomicSymbol) = pair._2 match {
    case symbol: Symbols.PlainAtomicSymbol => (pair._1, symbol)
    case nonAtomic =>
      val proxySymbol = Symbols.Proxy(nonAtomic)
      (Unbind(proxySymbol, pair._1), proxySymbol)
  }

  case class BindCtx(refCtx: List[ReferrableExpr], postProcess: ValuefyExpr => ValuefyExpr = x => x) {
    def withPostProcess(postProcess: ValuefyExpr => ValuefyExpr): BindCtx =
      BindCtx(refCtx, x => postProcess(this.postProcess(x)))

    def unbind(symbol: Symbols.Symbol): BindCtx =
      BindCtx(refCtx.map(_.unbind(symbol)), postProcess)

    def seqElemAt(seqIdx: Int): BindCtx =
      BindCtx(refCtx.map(_.seqElemAt(seqIdx)), postProcess)
  }

  case class ValuefiableSymbol[+T <: Symbols.Symbol](
    symbol: T,
    expr: ValuefyExpr,
    // bindCtx는 이 element에 대해 bind expr을 사용하려고 시도하는 경우 refCtx로 사용할 context. None이면 bind expr을 사용할 수 없음을 의미
    bindCtx: Option[BindCtx],
    joinExpr: Option[ValuefyExpr] = None,
  ) {
    def postProcess(postProcess: ValuefyExpr => ValuefyExpr): ValuefiableSymbol[T] =
      ValuefiableSymbol(symbol, postProcess(expr), bindCtx.map(_.withPostProcess(postProcess)), joinExpr)

    def unbind: ValuefiableSymbol[T] = {
      ValuefiableSymbol(symbol, Unbind(symbol, expr), bindCtx.map(_.unbind(symbol)), joinExpr.map(Unbind(symbol, _)))
    }

    def unbindIf(needed: Boolean): ValuefiableSymbol[T] = if (needed) this.unbind else this

    def proxy: ValuefiableSymbol[Symbols.PlainAtomicSymbol] = symbol match {
      case plainAtomic: Symbols.PlainAtomicSymbol =>
        // 실은 같은거지만 타입 맞추려고..
        ValuefiableSymbol(plainAtomic, expr, bindCtx, joinExpr)
      case nonPlainAtomic =>
        val proxySymbol = Symbols.Proxy(nonPlainAtomic)
        ValuefiableSymbol(proxySymbol, expr, bindCtx, joinExpr).unbind
    }
  }

  case class ReferrableExpr(expr: ValuefyExpr, bindCtx: Option[BindCtx], joinExpr: Option[ValuefyExpr]) {
    def unbind(symbol: Symbols.Symbol): ReferrableExpr =
      ReferrableExpr(Unbind(symbol, expr), bindCtx.map(_.unbind(symbol)), joinExpr.map(Unbind(symbol, _)))

    def seqElemAt(seqIdx: Int): ReferrableExpr =
      ReferrableExpr(SeqElemAt(seqIdx, expr), bindCtx.map(_.seqElemAt(seqIdx)), joinExpr.map(SeqElemAt(seqIdx, _)))
  }

  // symbolAst가 나타내는 심볼과, 해당 심볼을 파싱한 결과에 해당하는 값이 input으로 들어왔을 때 값으로 가공하는 valuefy expr을 반환하는 함수
  // unbindNeeded가 true이면 input이 생성되는 심볼로 bind된 노드이고, false이면 input이 생성되는 심볼의 body이라고 가정(최종 생성되는 심볼로 bind되지 않은)
  def valuefySymbol(
    symbolAst: MetaLang3Ast.Symbol,
    input: ValuefyExpr,
    unbindNeeded: Boolean = true,
  ): ValuefiableSymbol[Symbols.Symbol] = symbolAst match {
    case MetaLang3Ast.JoinSymbol(body, join) =>
      val vBody = valuefySymbol(body, input).proxy
      val vJoin = valuefySymbol(join, input).proxy
      val joinSymbol = Symbols.Join(vBody.symbol, vJoin.symbol)
      // A&B {Dual($<0, $>0)}
      // $>>0 같은건 지원하지 말자. A&(B&C) 에서 $>>0하면 C가 나오게 하고 싶은.. 그런 거였는데
      // 사용빈도도 엄청 떨어지는데다 A&(B&C $>0) $>0 같은식으로 해결하면 될 것 같음
      ValuefiableSymbol(joinSymbol,
        JoinBody(vBody.expr),
        None,
        joinExpr = Some(JoinCond(vJoin.expr))).unbindIf(unbindNeeded)
    case MetaLang3Ast.ExceptSymbol(body, except) =>
      val vBody = valuefySymbol(body, input).proxy
      val vExcept = valuefySymbol(except, input).proxy
      val exceptSymbol = Symbols.Except(vBody.symbol, vExcept.symbol)
      ValuefiableSymbol(exceptSymbol, vBody.expr, None).unbindIf(unbindNeeded)
    case MetaLang3Ast.FollowedBy(followedBy) =>
      val vFollowedBy = valuefySymbol(followedBy, input).proxy
      ValuefiableSymbol(Symbols.LookaheadIs(vFollowedBy.symbol), NullLiteral, None)
    case MetaLang3Ast.NotFollowedBy(notFollowedBy) =>
      val vNotFollowedBy = valuefySymbol(notFollowedBy, input).proxy
      ValuefiableSymbol(Symbols.LookaheadExcept(vNotFollowedBy.symbol), NullLiteral, None)
    case MetaLang3Ast.Optional(body) =>
      val vBody = valuefySymbol(body, input, unbindNeeded = false)
      // vBody.proxy를 하면 기본적으로 unbind까지 딸려 오기 때문에.. 그 부분을 제거하기 위해 이렇게 처리 - 다른데서 쓰는데가 없는 것 같아서 여기에 정의
      val vBodyProxy: ValuefiableSymbol[Symbols.PlainAtomicSymbol] = vBody.symbol match {
        case _: Symbols.PlainAtomicSymbol =>
          // proxy는 타입 때문
          vBody.proxy
        case nonPlainAtomic =>
          val proxySymbol = Symbols.Proxy(nonPlainAtomic)
          val vBodyUnbind = vBody.unbind
          ValuefiableSymbol(proxySymbol, vBodyUnbind.expr, vBodyUnbind.bindCtx, vBodyUnbind.joinExpr)
      }
      val emptySeq = Symbols.Proxy(Symbols.Sequence(Seq()))
      val oneofSymbol = Symbols.OneOf(ListSet(vBodyProxy.symbol, emptySeq))
      ValuefiableSymbol(
        oneofSymbol,
        vBodyProxy.expr,
        vBodyProxy.bindCtx
      ).postProcess { expr =>
        UnrollChoices(Map(emptySeq -> NullLiteral, vBodyProxy.symbol -> expr))
      }.unbindIf(unbindNeeded)
    case MetaLang3Ast.RepeatFromZero(body) =>
      val vBody = valuefySymbol(body, input).proxy
      val repeatSymbol = Symbols.Repeat(vBody.symbol, 0)
      if (unbindNeeded) {
        ValuefiableSymbol(repeatSymbol, vBody.expr, vBody.bindCtx)
          .postProcess(UnrollRepeatFromZero)
      } else {
        ValuefiableSymbol(repeatSymbol, vBody.expr, vBody.bindCtx)
          .postProcess(UnrollRepeatFromZeroNoUnbind(repeatSymbol, _))
      }
    case MetaLang3Ast.RepeatFromOne(body) =>
      val vBody = valuefySymbol(body, input).proxy
      val repeatSymbol = Symbols.Repeat(vBody.symbol, 1)
      if (unbindNeeded) {
        ValuefiableSymbol(repeatSymbol, vBody.expr, vBody.bindCtx)
          .postProcess(UnrollRepeatFromOne)
      } else {
        ValuefiableSymbol(repeatSymbol, vBody.expr, vBody.bindCtx)
          .postProcess(UnrollRepeatFromOneNoUnbind(repeatSymbol, _))
      }
    case MetaLang3Ast.InPlaceChoices(choices) =>
      if (choices.size == 1) {
        valuefySymbol(choices.head, input, unbindNeeded = unbindNeeded)
      } else {
        val vChoices = choices.map(c => valuefySymbol(c, input, unbindNeeded = false).proxy)
        val oneofSymbol = Symbols.OneOf(ListSet.from(vChoices.map(_.symbol)))
        ValuefiableSymbol(
          oneofSymbol,
          UnrollChoices(vChoices.map(c => c.symbol -> c.expr).toMap),
          // choice가 여러개인 경우 bind 불가
          None).unbindIf(unbindNeeded)
      }
    case MetaLang3Ast.Longest(choices) =>
      val vChoices = valuefySymbol(choices, input).proxy
      val longestSymbol = Symbols.Longest(vChoices.symbol)
      ValuefiableSymbol(longestSymbol, vChoices.expr, vChoices.bindCtx)
        .unbindIf(unbindNeeded)
    case MetaLang3Ast.EmptySeq() =>
      ValuefiableSymbol(Symbols.Sequence(), NullLiteral, None)
    case MetaLang3Ast.Sequence(seq) =>
      valuefySequence(seq, input, List()).unbindIf(unbindNeeded)
    case MetaLang3Ast.Nonterminal(name) =>
      val nonterminalName = stringNameOf(name)
      val nonterminalSymbol = Symbols.Nonterminal(nonterminalName)
      ValuefiableSymbol(nonterminalSymbol, MatchNonterminal(nonterminalName), None).unbindIf(unbindNeeded)
    case MetaLang3Ast.StringSymbol(value) =>
      val stringSymbol = Symbols.Proxy(Symbols.Sequence(value.map(TerminalUtil.stringCharToChar).map(Symbols.ExactChar)))
      ValuefiableSymbol(stringSymbol, StringLiteral(TerminalUtil.stringCharsToString(value)), None)
    case terminal: MetaLang3Ast.Terminal =>
      val terminalSymbol = TerminalUtil.terminalToSymbol(terminal)
      ValuefiableSymbol(terminalSymbol, CharFromTerminalLiteral, None).unbindIf(unbindNeeded)
    case MetaLang3Ast.TerminalChoice(choices) =>
      val terminalSymbol = TerminalUtil.terminalChoicesToSymbol(choices)
      ValuefiableSymbol(terminalSymbol, CharFromTerminalLiteral, None).unbindIf(unbindNeeded)
  }

  // input은 전체 시퀀스에 대한 bind node가 없다고 가정
  def valuefySequence(
    sequence: List[MetaLang3Ast.Elem],
    input: ValuefyExpr,
    callCtx: List[MetaLang3Ast.PExpr],
  ): ValuefiableSymbol[Symbols.Symbol] = {
    val symbolsCount = sequence.count(_.isInstanceOf[MetaLang3Ast.Symbol])
    // sequence에 속한 심볼이 하나뿐이면 Sequence 심볼을 거치지 않도록
    if (symbolsCount == 1) {
      var rhsSymbol: Symbols.Symbol = null
      val refCtx = mutable.ListBuffer[ReferrableExpr]()
      sequence.foreach { elem =>
        val expr: ReferrableExpr = elem match {
          case processor: MetaLang3Ast.Processor =>
            ReferrableExpr(valuefyProcessor(refCtx.toList, processor, input, callCtx), None, None)
          case symbol: MetaLang3Ast.Symbol =>
            val valuefiable = valuefySymbol(symbol, input, unbindNeeded = false)
            // valuefiable.symbol 가 대응되는 symbols와 일치해야 함
            rhsSymbol = valuefiable.symbol
            ReferrableExpr(valuefiable.expr, valuefiable.bindCtx, valuefiable.joinExpr)
        }
        refCtx += expr
      }
      ValuefiableSymbol(rhsSymbol, refCtx.last.expr, Some(BindCtx(refCtx.toList)))
    } else {
      val vElems = sequence.collect {
        case symbol: MetaLang3Ast.Symbol =>
          valuefySymbol(symbol, input).proxy
      }
      val sequenceSymbol = Symbols.Sequence(vElems.map(_.symbol))
      val refCtx = mutable.ListBuffer[ReferrableExpr]()
      var symbolIdx = 0
      sequence.foreach {
        case processor: MetaLang3Ast.Processor =>
          refCtx += ReferrableExpr(valuefyProcessor(refCtx.toList, processor, input, callCtx), None, None)
        case _: MetaLang3Ast.Symbol =>
          assert(symbolIdx < symbolsCount)
          val vElem = vElems(symbolIdx)
          refCtx += ReferrableExpr(vElem.expr, vElem.bindCtx, vElem.joinExpr).seqElemAt(symbolIdx)
          symbolIdx += 1
      }
      ValuefiableSymbol(sequenceSymbol, refCtx.last.expr, Some(BindCtx(refCtx.toList)))
    }
  }

  def valuefyProcessor(
    refCtx: List[ReferrableExpr],
    processor: MetaLang3Ast.Processor,
    input: ValuefyExpr,
    callCtx: List[MetaLang3Ast.PExpr]
  ): ValuefyExpr = processor match {
    case MetaLang3Ast.ProcessorBlock(body) =>
      valuefyPExpr(refCtx, body, input, body +: callCtx)
    case ref: MetaLang3Ast.Ref =>
      valuefyPExpr(refCtx, ref, input, ref +: callCtx)
  }

  def valuefyPExpr(
    refCtx: List[ReferrableExpr],
    expr: MetaLang3Ast.PExpr,
    input: ValuefyExpr,
    callCtx: List[MetaLang3Ast.PExpr]
  ): ValuefyExpr = {
    if (callCtx.tail.contains(callCtx.head)) {
      throw new IllegalStateException(s"Recursive reference - $callCtx")
    }
    if (callCtx.size > 200) {
      throw new IllegalStateException(s"Too deep calls - $callCtx")
    }
    expr match {
      case MetaLang3Ast.TypedPExpr(body, typ) =>
        // TODO typ 처리
        ???
      case MetaLang3Ast.TernaryOp(cond, ifTrue, ifFalse) =>
        TernaryOp(valuefyPExpr(refCtx, cond, input, cond +: callCtx),
          ifTrue = valuefyPExpr(refCtx, ifTrue, input, ifTrue +: callCtx),
          ifFalse = valuefyPExpr(refCtx, ifFalse, input, ifFalse +: callCtx))
      case MetaLang3Ast.ElvisOp(value, ifNull) =>
        val vValue = valuefyPExpr(refCtx, value, input, value +: callCtx)
        val vIfNull = valuefyPExpr(refCtx, ifNull, input, ifNull +: callCtx)
        ElvisOp(vValue, vIfNull)
      case MetaLang3Ast.BinOp(op, lhs, rhs) =>
        val vLhs = valuefyPExpr(refCtx, lhs, input, lhs +: callCtx)
        val vRhs = valuefyPExpr(refCtx, rhs, input, rhs +: callCtx)
        val opType = op match {
          case MetaLang3Ast.Op.ADD => BinOpType.ADD
          case MetaLang3Ast.Op.EQ => BinOpType.EQ
          case MetaLang3Ast.Op.NE => BinOpType.NE
          case MetaLang3Ast.Op.AND => BinOpType.BOOL_AND
          case MetaLang3Ast.Op.OR => BinOpType.BOOL_OR
        }
        BinOp(opType, vLhs, vRhs)
      case MetaLang3Ast.PrefixOp(op, expr) =>
        val vExpr = valuefyPExpr(refCtx, expr, input, expr +: callCtx)
        val opType = op match {
          case MetaLang3Ast.PreOp.NOT => PreOpType.NOT
        }
        PreOp(opType, vExpr)
      case ref: MetaLang3Ast.Ref => ref match {
        case MetaLang3Ast.ValRef(idx0, condSymPath0) =>
          val idx = idx0.toInt
          check(idx < refCtx.size, s"reference index out of range around ${ref.parseNode.start}")
          val referred = refCtx(idx)
          val condSymPath = condSymPath0.getOrElse(List())
          check(condSymPath.size <= 1, "join path ref cannot be more than one")
          if (condSymPath.isEmpty || condSymPath.head == BODY) referred.expr else {
            check(referred.joinExpr.isDefined, "Invalid reference to join condition")
            referred.joinExpr.get
          }
        case MetaLang3Ast.RawRef(idx0, condSymPath0) =>
          val idx = idx0.toInt
          check(idx < refCtx.size, "reference index out of range")
          ???
      }
      case MetaLang3Ast.ExprParen(body) =>
        valuefyPExpr(refCtx, body, input, body +: callCtx)
      case expr: MetaLang3Ast.BindExpr =>
        valuefyBindExpr(refCtx, expr, input, expr +: callCtx)
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
        val vParams = params.map(p => valuefyPExpr(refCtx, p.expr, input, p.expr +: callCtx))
        _classInfoCollector = _classInfoCollector.addClassConstructCall(className, vParams)
        ConstructCall(className, vParams)
      case MetaLang3Ast.FuncCallOrConstructExpr(funcName, params) =>
        val vParams = params.map(param => valuefyPExpr(refCtx, param, input, param +: callCtx))
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
        val vElems = elems.map(elem => valuefyPExpr(refCtx, elem, input, elem +: callCtx))
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
  }

  private def valuefyBindExpr(
    refCtx: List[ReferrableExpr],
    bindExpr: MetaLang3Ast.BindExpr,
    input: ValuefyExpr,
    callCtx: List[MetaLang3Ast.PExpr]
  ): ValuefyExpr = {
    val MetaLang3Ast.ValRef(idx0, condSymPath0) = bindExpr.ctx
    val condSymPath = condSymPath0.getOrElse(List())
    check(condSymPath.isEmpty, "BindExpr with cond sym path not implemented")
    val idx = idx0.toInt
    check(idx < refCtx.size, "reference index out of range")
    val referred = refCtx(idx)
    println(referred)
    referred.bindCtx match {
      case Some(bindCtx) =>
        val vExpr = bindExpr.binder match {
          case anotherBindExpr: MetaLang3Ast.BindExpr =>
            valuefyBindExpr(bindCtx.refCtx, anotherBindExpr, input, anotherBindExpr +: callCtx)
          case MetaLang3Ast.ProcessorBlock(body) =>
            valuefyPExpr(bindCtx.refCtx, body, input, body +: callCtx)
          case ref: MetaLang3Ast.Ref =>
            valuefyPExpr(bindCtx.refCtx, ref, input, ref +: callCtx)
        }
        bindCtx.postProcess(vExpr)
      case None =>
        throw new IllegalStateException(s"Bind expression is not supported for ${bindExpr.ctx}")
    }
  }

  private def addNonterminalDerivation(nonterminalName: String, nonterminalType: Option[Type], rhs: List[MetaLang3Ast.Sequence]): Unit = {
    nonterminalType.foreach { someNonterminalType =>
      _nonterminalInfoCollector = _nonterminalInfoCollector.setNonterminalType(nonterminalName, someNonterminalType)
    }
    val vRhs = rhs.map { derivation =>
      val ValuefiableSymbol(rhsSymbol, valuefyExpr, _, _) = valuefySequence(derivation.seq, InputNode, List())
      _nonterminalInfoCollector = _nonterminalInfoCollector.addNonterminalExpr(nonterminalName, valuefyExpr)
      (rhsSymbol, valuefyExpr)
    }
    _nonterminalSymbols += nonterminalName -> vRhs.map(_._1)
    _nonterminalValuefyExprs += nonterminalName -> UnrollChoices(vRhs.toMap)
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
