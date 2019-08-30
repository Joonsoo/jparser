package com.giyeok.jparser.gramgram.meta2

import com.giyeok.jparser.ParseResultTree.{BindNode, SequenceNode, TerminalNode}
import com.giyeok.jparser.{Grammar, GrammarHelper, Inputs, Symbols}
import com.giyeok.jparser.Symbols.Nonterminal
import com.giyeok.jparser.nparser.NGrammar

import scala.collection.immutable.{ListMap, ListSet}

class AstAnalysis(val astifiers: List[(String, List[(Symbols.Symbol, AstifierExpr)])]) {
    val astifierMap: ListMap[String, List[(Symbols.Symbol, AstifierExpr)]] = ListMap(astifiers: _*)

    def grammar(grammarName: String): Grammar = new Grammar {
        val name: String = grammarName
        val startSymbol: Nonterminal = Symbols.Nonterminal(astifiers.head._1)
        val rules: RuleMap = ListMap[String, ListSet[Symbols.Symbol]](
            astifierMap.toList.map { p => p._1 -> ListSet(p._2 map (_._1): _*) }: _*
        )
    }

    lazy val ngrammar: NGrammar = NGrammar.fromGrammar(grammar("G"))
}

class AstAnalyzer(val grammarAst: AST.Grammar) {
    private val _astToSymbols = scala.collection.mutable.Map[AST.Symbol, Symbols.Symbol]()

    def astToSymbols(ast: AST.Symbol): Symbols.Symbol = _astToSymbols(ast)

    private def addSymbol[T <: Symbols.Symbol](ast: AST.Symbol, symbol: T): T = {
        _astToSymbols get ast match {
            case Some(existing) =>
                assert(symbol == existing)
                existing.asInstanceOf[T]
            case None =>
                _astToSymbols(ast) = symbol
                symbol
        }
    }

    private def unicodeCharToChar(charNode: AST.Node): Char = charNode.node match {
        case BindNode(_, seq: SequenceNode) =>
            assert(seq.children.size == 6)
            Integer.parseInt(s"${seq.children(2).toString}${seq.children(3).toString}${seq.children(4).toString}${seq.children(5).toString}", 16).toChar
    }

    private def charNodeToChar(charNode: AST.Node): Char = charNode.node match {
        case BindNode(_, BindNode(_, TerminalNode(c))) =>
            c.asInstanceOf[Inputs.Character].char
        case BindNode(_, SequenceNode(_, List(BindNode(_, TerminalNode(escapeCode)), _))) =>
            escapeCode.asInstanceOf[Inputs.Character].char match {
                case '\'' => '\''
                case '\\' => '\\'
                case 'b' => '\b'
                case 'n' => '\n'
                case 'r' => '\r'
                case 't' => '\t'
            }
        case _ => unicodeCharToChar(charNode)
    }

    private def charChoiceNodeToChar(charNode: AST.Node): Char = charNode.node match {
        case BindNode(_, BindNode(_, TerminalNode(c))) =>
            c.asInstanceOf[Inputs.Character].char
        case BindNode(_, BindNode(_, BindNode(_, TerminalNode(c)))) =>
            c.asInstanceOf[Inputs.Character].char
        case BindNode(_, SequenceNode(_, List(BindNode(_, TerminalNode(escapeCode)), _))) =>
            escapeCode.asInstanceOf[Inputs.Character].char match {
                case '\'' => '\''
                case '-' => '-'
                case '\\' => '\\'
                case 'b' => '\b'
                case 'n' => '\n'
                case 'r' => '\r'
                case 't' => '\t'
            }
        case _ => unicodeCharToChar(charNode)
    }

    private def stringCharToChar(stringCharNode: AST.Node): Char = stringCharNode.node match {
        case BindNode(_, BindNode(_, TerminalNode(c))) =>
            c.asInstanceOf[Inputs.Character].char
        case BindNode(_, SequenceNode(_, List(BindNode(_, TerminalNode(escapeCode)), _))) =>
            escapeCode.asInstanceOf[Inputs.Character].char match {
                case '"' => '"'
                case '\\' => '\\'
                case 'b' => '\b'
                case 'n' => '\n'
                case 'r' => '\r'
                case 't' => '\t'
            }
        case _ => unicodeCharToChar(stringCharNode)
    }

    private implicit class AstifiedProxyIfNeeded(astified: Astified) {
        def proxyNeeded: Boolean = astified.symbol.isInstanceOf[Symbols.Sequence]

        def proxyIfNeeded: Astified =
            astified.symbol match {
                case seq: Symbols.Sequence =>
                    val p = Symbols.Proxy(seq)
                    Astified(p, astified.astifierExpr.replaceThisNode(Unbind(ThisNode, p)), astified.insideCtx)
                case _: Symbols.AtomicSymbol =>
                    astified
            }
    }

    private implicit class SymbolIsAtomic(symbol: Symbols.Symbol) {
        def asAtomic: Symbols.AtomicSymbol = symbol.asInstanceOf[Symbols.AtomicSymbol]
    }

    private def astProcessorToAstifier(ctx: AstifiedCtx, processor: AST.Processor): AstifierExpr = processor match {
        case AST.Ref(_, idx) =>
            val idxNum = idx.toString.toInt
            ctx.refs(idxNum).astifierExpr
        case AST.BoundPExpr(_, ctxRef, expr) =>
            val referred = ctx.refs(ctxRef.idx.toString.toInt)
            // TODO .getOrElse 해서 명시적 insideCtx가 없으면 $0을 전체를 가리키도록
            val referredCtx = referred.insideCtx.get
            val mapFn = astProcessorToAstifier(referredCtx, expr)
            // TODO referred.symbol의 종류에 따라서 UnrollMapper를 다른 형태로 바꿔야될 수도?
            EachMap(referred.astifierExpr, mapFn)
        case AST.BinOpExpr(_, op, lhs, rhs) =>
            op.toString match {
                case "+" => ConcatList(astProcessorToAstifier(ctx, lhs), astProcessorToAstifier(ctx, rhs))
            }
        case AST.ConstructExpr(_, typ, params) =>
            CreateObj(typ.name.toString, params map (astProcessorToAstifier(ctx, _)))
        case AST.OnTheFlyTypeDefConstructExpr(_, typeDef, params) =>
            CreateObj(typeDef.name.name.toString, params map { p => astProcessorToAstifier(ctx, p.expr) })
        case AST.PTermParen(_, expr) =>
            astProcessorToAstifier(ctx, expr)
        case AST.PTermSeq(_, elems) =>
            CreateList(elems map (astProcessorToAstifier(ctx, _)))
    }

    private def astSymbolToSymbol(ast: AST.Symbol): Astified = ast match {
        case AST.JoinSymbol(_, symbol1, symbol2) =>
            val s1 = astSymbolToSymbol(symbol1).proxyIfNeeded
            val s2 = astSymbolToSymbol(symbol2).proxyIfNeeded
            val s = addSymbol(ast, Symbols.Join(s1.symbol.asAtomic, s2.symbol.asAtomic))
            Astified(s, Unbind(ThisNode, s), Some(AstifiedCtx(List(s1, s2))))
        case AST.ExceptSymbol(_, symbol1, symbol2) =>
            val s1 = astSymbolToSymbol(symbol1).proxyIfNeeded
            val s2 = astSymbolToSymbol(symbol2).proxyIfNeeded
            val s = addSymbol(ast, Symbols.Except(s1.symbol.asAtomic, s2.symbol.asAtomic))
            Astified(s, Unbind(ThisNode, s), Some(AstifiedCtx(List(s1, s2))))
        case AST.FollowedBy(_, expr) =>
            val l = astSymbolToSymbol(expr).proxyIfNeeded
            val s = addSymbol(ast, Symbols.LookaheadIs(l.symbol.asAtomic))
            Astified(s, Unbind(ThisNode, s), None)
        case AST.NotFollowedBy(_, expr) =>
            val l = astSymbolToSymbol(expr).proxyIfNeeded
            val s = addSymbol(ast, Symbols.LookaheadExcept(l.symbol.asAtomic))
            Astified(s, Unbind(ThisNode, s), None)
        case AST.Repeat(_, repeatingSymbol, repeatSpec) =>
            val r = astSymbolToSymbol(repeatingSymbol).proxyIfNeeded
            val insideCtx = Some(r.insideCtx.getOrElse(AstifiedCtx(List(
                // TODO 이거 다시 확인
                Astified(Symbols.Start, ThisNode, None)
            ))))
            repeatSpec.toString match {
                case "?" =>
                    val empty = Symbols.Proxy(Symbols.Sequence(Seq()))
                    val content = r.symbol.asAtomic
                    val s = addSymbol(ast, Symbols.OneOf(ListSet(empty, content)))
                    Astified(s, UnrollOptional(ThisNode, r.astifierExpr, empty, content), insideCtx)
                case "*" =>
                    val s = addSymbol(ast, Symbols.Repeat(r.symbol.asAtomic, 0))
                    Astified(s, UnrollRepeat(0, ThisNode, r.astifierExpr), insideCtx)
                case "+" =>
                    val s = addSymbol(ast, Symbols.Repeat(r.symbol.asAtomic, 1))
                    Astified(s, UnrollRepeat(1, ThisNode, r.astifierExpr), insideCtx)
            }
        case AST.Paren(_, choices) =>
            val r = astSymbolToSymbol(choices)
            addSymbol(ast, r.symbol)
            r
        case AST.Longest(_, choices) =>
            val r = if (choices.choices.size == 1) astSymbolToSymbol(choices.choices.head).proxyIfNeeded else
                astSymbolToSymbol(choices)
            val s = addSymbol(ast, Symbols.Longest(r.symbol.asAtomic))
            Astified(s, r.astifierExpr.replaceThisNode(Unbind(ThisNode, s)), r.insideCtx)
        case AST.EmptySeq(_) =>
            val s = addSymbol(ast, Symbols.Proxy(Symbols.Sequence(Seq())))
            Astified(s, ThisNode, None)
        case AST.InPlaceChoices(_, choices) =>
            if (choices.size == 1) {
                val r = astSymbolToSymbol(choices.head)
                addSymbol(ast, r.symbol)
                r
            } else {
                val ss = choices map astSymbolToSymbol map (_.proxyIfNeeded)
                val s = addSymbol(ast, Symbols.OneOf(ListSet(ss map (_.symbol.asAtomic): _*)))
                val astifier = UnrollChoices((ss map { r => r.symbol -> r.astifierExpr }).toMap)
                Astified(s, astifier, Some(AstifiedCtx(ss)))
            }
        case AST.InPlaceSequence(_, seq) =>
            if (seq.size == 1) {
                val r = astSymbolToSymbol(seq.head)
                addSymbol(ast, r.symbol)
                r
            } else {
                val r = astElemSequence(seq)
                Astified(addSymbol(ast, r.symbol), r.astifierExpr, r.insideCtx)
            }
        case AST.Nonterminal(_, name) =>
            val s = addSymbol(ast, Symbols.Nonterminal(name.toString))
            Astified(s, Unbind(ThisNode, s), None)
        case AST.TerminalChar(_, char) =>
            val s = addSymbol(ast, Symbols.ExactChar(charNodeToChar(char)))
            Astified(s, ThisNode, None)
        case AST.AnyTerminal(_) =>
            val s = addSymbol(ast, Symbols.AnyChar)
            Astified(s, ThisNode, None)
        case AST.TerminalChoice(_, choices) =>
            val s = addSymbol(ast, Symbols.Chars((choices flatMap {
                case AST.TerminalChoiceChar(_, char) => Seq(charChoiceNodeToChar(char))
                case AST.TerminalChoiceRange(_, start, end) =>
                    charChoiceNodeToChar(start.char) to charChoiceNodeToChar(end.char)
            }).toSet))
            Astified(s, ThisNode, None)
        case AST.StringLiteral(_, value) =>
            // TODO stringCharToChar 사용하도록 수정
            val s = addSymbol(ast, Symbols.Proxy(GrammarHelper.i(value.toString)))
            Astified(s, ThisNode, None)
    }

    private def astElemSequence(seq: List[AST.Elem]): Astified = {
        var syms = List[Symbols.AtomicSymbol]()
        var refs = List[Astified]()

        seq.zipWithIndex foreach {
            case (processor: AST.Processor, _) =>
                val astifier = astProcessorToAstifier(AstifiedCtx(refs), processor)
                // TODO 여기서 임의의 심볼(Symbols.Start) 넣지 않아도 되도록 Astified/AstifeidCtx 구조 개선
                refs :+= Astified(Symbols.Start, astifier, None)
            case (symbol: AST.Symbol, idx) =>
                val astified = astSymbolToSymbol(symbol).proxyIfNeeded
                syms :+= astified.symbol.asAtomic
                refs :+= Astified(astified.symbol, astified.astifierExpr.replaceThisNode(SeqRef(ThisNode, idx)), astified.insideCtx)
        }
        val s = Symbols.Sequence(syms)
        Astified(s, Unbind(ThisNode, s), Some(AstifiedCtx(refs)))
    }

    def analyzeAstifiers(): AstAnalysis = {
        // Check duplicate definitions of nonterminal
        val ruleNames = grammarAst.defs.collect { case r: AST.Rule => r }.groupBy(_.lhs.name.name.toString)
        if (ruleNames exists (_._2.size >= 2)) {
            val duplicates = (ruleNames filter (_._2.size >= 2)).keys
            throw new Exception(s"Duplicate definitions of nonterminal ${duplicates mkString ", "}")
        }

        // grammar를 만들면서 Node -> AST 변환 코드도 생성
        val astifiers = grammarAst.defs collect {
            case AST.Rule(_, lhs, rhs) =>
                val processedR = rhs map { r =>
                    val astified = astElemSequence(r.elems)
                    val symbol = astified.symbol.asInstanceOf[Symbols.Sequence]
                    val lastExpr = astified.insideCtx.get.refs.last.astifierExpr
                    (symbol, lastExpr)
                }
                lhs.name.name.toString -> processedR
        }
        new AstAnalysis(astifiers)
    }
}
