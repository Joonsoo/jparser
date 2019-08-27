package com.giyeok.jparser.gramgram.meta2

import com.giyeok.jparser.ParseResultTree.{BindNode, SequenceNode, TerminalNode}
import com.giyeok.jparser.Symbols.Nonterminal
import com.giyeok.jparser.gramgram.meta2.TypeDependenceGraph._
import com.giyeok.jparser.nparser.NGrammar
import com.giyeok.jparser.{Grammar, GrammarHelper, Inputs, Symbols}

import scala.collection.immutable.{ListMap, ListSet}

object Analyzer {

    sealed trait UserDefinedType {
        val name: String
    }

    case class UserClassType(name: String, params: List[AST.NamedParam]) extends UserDefinedType

    case class UserAbstractType(name: String) extends UserDefinedType

    class Analysis private[Analyzer](val grammarAst: AST.Grammar,
                                     val astifiers: List[(String, List[(Symbols.Symbol, AstifierExpr)])],
                                     val typeDependenceGraph: TypeDependenceGraph,
                                     val typeHierarchyGraph: TypeHierarchyGraph,
                                     val classDefs: List[ClassDef]) {
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

    class Analyzer(val grammarAst: AST.Grammar) {
        val ruleDefs: List[AST.Rule] = grammarAst.defs collect { case r: AST.Rule => r }
        val typeDefs: List[AST.TypeDef] = grammarAst.defs collect { case r: AST.TypeDef => r }

        private var userDefinedTypes = List[UserDefinedType]()
        private var superTypes = Map[UserAbstractType, Set[String]]()

        def collectTypeDefs(): Unit = {
            // TODO typeDefs
            def addTypeDefsLhs(typ: AST.ValueTypeDesc): Unit = typ match {
                case AST.OnTheFlyTypeDef(_, name, supers) =>
                    val newType = UserAbstractType(name.name.toString)
                    val newSupers = superTypes.getOrElse(newType, List()) ++ (supers map {
                        _.name.toString
                    })
                    superTypes += newType -> newSupers.toSet
                    userDefinedTypes +:= newType
                case AST.ArrayTypeDesc(_, elemType) =>
                    addTypeDefsLhs(elemType.typ)
                case _ => // do nothing
            }

            def addTypeDefsOnTheFlyTypeDefConstructExpr(expr: AST.OnTheFlyTypeDefConstructExpr): Unit = expr match {
                case AST.OnTheFlyTypeDefConstructExpr(_, tdef, params) =>
                    val newType = UserClassType(tdef.name.name.toString, params)
                    userDefinedTypes +:= newType
                    params foreach { p =>
                        if (p.typeDesc.isDefined) {
                            addTypeDefsLhs(p.typeDesc.get.typ)
                        }
                        addTypeDefsExpr(p.expr)
                    }
            }

            def addTypeDefsBoundedPExpr(expr: AST.BoundedPExpr): Unit = expr match {
                case AST.BoundPExpr(_, _, e) => addTypeDefsBoundedPExpr(e)
                case e: AST.OnTheFlyTypeDefConstructExpr =>
                    addTypeDefsOnTheFlyTypeDefConstructExpr(e)
                case e: AST.PExpr => addTypeDefsExpr(e)
            }

            def addTypeDefsExpr(expr: AST.PExpr): Unit = expr match {
                case AST.PTermParen(_, e) => addTypeDefsExpr(e)
                case AST.BoundPExpr(_, _, e) => addTypeDefsBoundedPExpr(e)
                case AST.ConstructExpr(_, _, params) => params foreach addTypeDefsExpr
                case AST.PTermSeq(_, elems) => elems foreach addTypeDefsExpr
                case e: AST.OnTheFlyTypeDefConstructExpr =>
                    addTypeDefsOnTheFlyTypeDefConstructExpr(e)
                case AST.BinOpExpr(_, _, lhs, rhs) =>
                    addTypeDefsExpr(lhs)
                    addTypeDefsExpr(rhs)
                case _: AST.Ref => // do nothing
            }

            ruleDefs foreach { rule =>
                rule.lhs.typeDesc foreach { lhsType =>
                    addTypeDefsLhs(lhsType.typ)
                }
                rule.rhs foreach { rhs =>
                    rhs.elems foreach {
                        case processor: AST.PExpr =>
                            addTypeDefsExpr(processor)
                        case _ => // do nothing
                    }
                }
            }
        }

        private val astToSymbols = scala.collection.mutable.Map[AST.Symbol, Symbols.Symbol]()

        private def addSymbol[T <: Symbols.Symbol](ast: AST.Symbol, symbol: T): T = {
            astToSymbols get ast match {
                case Some(existing) =>
                    assert(symbol == existing)
                    existing.asInstanceOf[T]
                case None =>
                    astToSymbols(ast) = symbol
                    symbol
            }
        }

        private class GrammarAndAstifierGenerator() {
            private def astProcessorToAstifier(ctx: BoundRefs, processor: AST.Processor): AstifierExpr = processor match {
                case AST.Ref(_, idx) =>
                    val idxNum = idx.toString.toInt
                    ctx.refs(idxNum)._1.replaceThisNode(SeqRef(ThisNode, idxNum))
                // ctx.refs(idx.toString.toInt)._1
                case AST.BinOpExpr(_, op, lhs, rhs) =>
                    op.toString match {
                        case "+" =>
                            ConcatList(astProcessorToAstifier(ctx, lhs), astProcessorToAstifier(ctx, rhs))
                    }
                case AST.BoundPExpr(_, ctxRef, expr) =>
                    val refIdxNum = ctxRef.idx.toString.toInt
                    val referrer = SeqRef(ThisNode, refIdxNum)
                    val referred = ctx.refs(refIdxNum)
                    val target = referred._1
                    if (referred._2.isEmpty) {
                        throw new Exception("Invalid bound expr")
                    }
                    val boundCtx = referred._2.get
                    if (boundCtx.boundType == BoundType.Repeat0 || boundCtx.boundType == BoundType.Repeat1) {
                        // TODO target에서 가장 바깥 Unbinder는 제거해야함(repeat symbol)
                    }
                    UnrollMapper(boundCtx.boundType, referrer, target, astProcessorToAstifier(boundCtx, expr))
                case AST.OnTheFlyTypeDefConstructExpr(_, typeDef, params) =>
                    CreateObj(typeDef.name.name.toString, params map { p => astProcessorToAstifier(ctx, p.expr) })
                case AST.ConstructExpr(_, typ, params) =>
                    CreateObj(typ.name.toString, params map (astProcessorToAstifier(ctx, _)))
                case AST.PTermParen(_, expr) => astProcessorToAstifier(ctx, expr)
                case AST.PTermSeq(_, elems) =>
                    CreateList(elems map (astProcessorToAstifier(ctx, _)))
            }

            private def addSymbol1(symbol: AST.Symbol, r: (Symbols.Symbol, AstifierExpr, Option[BoundRefs])) = {
                addSymbol(symbol, r._1)
                r
            }

            def astSymbolToSymbol(symbol: AST.Symbol): (Symbols.Symbol, AstifierExpr, Option[BoundRefs]) = symbol match {
                case symbol: AST.BinSymbol =>
                    symbol match {
                        case AST.JoinSymbol(nodeId, symbol1, symbol2) => ???
                        case AST.ExceptSymbol(nodeId, symbol1, symbol2) => ???
                        case AST.FollowedBy(nodeId, expr) => ???
                        case AST.NotFollowedBy(nodeId, expr) => ???
                        case AST.Repeat(_, repeatingSymbol, repeatSpec) =>
                            val repeating = astSymbolToSymbol(repeatingSymbol)
                            // TODO repeatSpec 종류에 따라서 BoundRef의 boundType 변경
                            // TODO br.refs가 그대로 가면 안될듯 함
                            val (repeatingBody, astifier) = repeating._1 match {
                                case atom: Symbols.AtomicSymbol =>
                                    (atom, repeating._2)
                                case seq: Symbols.Sequence =>
                                    val p = Symbols.Proxy(seq)
                                    (p, repeating._2.replaceThisNode(Unbinder(ThisNode, p)))
                            }
                            repeatSpec.toString match {
                                case "?" =>
                                    val e = Symbols.Proxy(Symbols.Sequence(Seq()))
                                    val s = addSymbol(symbol, Symbols.OneOf(ListSet(e, repeatingBody)))
                                    (s, astifier.replaceThisNode(Unbinder(ThisNode, s)), repeating._3 map (_.changeBoundType(BoundType.Optional)))
                                case "*" =>
                                    val s = addSymbol(symbol, Symbols.Repeat(repeatingBody, 0))
                                    (s, astifier.replaceThisNode(Unbinder(ThisNode, s)), repeating._3 map (_.changeBoundType(BoundType.Repeat0)))
                                case "+" =>
                                    val s = addSymbol(symbol, Symbols.Repeat(repeatingBody, 1))
                                    (s, astifier.replaceThisNode(Unbinder(ThisNode, s)), repeating._3 map (_.changeBoundType(BoundType.Repeat1)))
                            }
                        case AST.Paren(_, choices) =>
                            addSymbol1(symbol, astSymbolToSymbol(choices))
                        case AST.Longest(_, choices) =>
                            val r = astSymbolToSymbol(choices)
                            val (longestBody, astifier) = r._1 match {
                                case atom: Symbols.AtomicSymbol => (atom, r._2)
                                case seq: Symbols.Sequence =>
                                    val s = Symbols.Proxy(seq)
                                    (s, r._2.replaceThisNode(Unbinder(ThisNode, s)))
                            }
                            val s = addSymbol(symbol, Symbols.Longest(longestBody))
                            (s, astifier.replaceThisNode(Unbinder(ThisNode, s)), r._3)
                        case AST.EmptySeq(_) =>
                            val s = addSymbol(symbol, Symbols.Proxy(Symbols.Sequence(Seq())))
                            (s, ThisNode, None)
                        case AST.InPlaceChoices(_, choices) =>
                            if (choices.size == 1) {
                                addSymbol1(symbol, astSymbolToSymbol(choices.head))
                            } else {
                                val choiceBodies = choices map astSymbolToSymbol
                                val proxied = choiceBodies.map { b =>
                                    b._1 match {
                                        case atom: Symbols.AtomicSymbol =>
                                            (atom, b._2, b._3)
                                        case seq: Symbols.Sequence =>
                                            val p = Symbols.Proxy(seq)
                                            (p, b._2.replaceThisNode(Unbinder(ThisNode, p)), b._3)
                                    }
                                }
                                val s = addSymbol(symbol, Symbols.OneOf(ListSet(proxied map (_._1): _*)))
                                val astifier = UnrollChoices(proxied.map { p => p._1 -> p._2 }.toMap)
                                val refs = BoundRefs(BoundType.Choice, proxied.map { p => (p._2, p._3) })
                                (s, astifier, Some(refs))
                            }
                        case AST.InPlaceSequence(_, seq) =>
                            if (seq.size == 1) {
                                addSymbol1(symbol, astSymbolToSymbol(seq.head))
                            } else {
                                val r = astElemSequence(seq)
                                (addSymbol(symbol, r._1), r._2.replaceThisNode(Unbinder(ThisNode, r._1)), r._3)
                            }
                        case AST.Nonterminal(_, name) =>
                            val s = addSymbol(symbol, Symbols.Nonterminal(name.toString))
                            (s, Unbinder(ThisNode, s), None)
                        case AST.TerminalChar(_, char) =>
                            val s = addSymbol(symbol, Symbols.ExactChar(charNodeToChar(char)))
                            (s, ThisNode, None)
                        case AST.AnyTerminal(_) =>
                            val s = addSymbol(symbol, Symbols.Any)
                            (s, ThisNode, None)
                        case AST.TerminalChoice(_, choices) =>
                            val s = choices flatMap {
                                case AST.TerminalChoiceChar(_, char) => Seq(charChoiceNodeToChar(char))
                                case AST.TerminalChoiceRange(_, start, end) =>
                                    charChoiceNodeToChar(start.char) to charChoiceNodeToChar(end.char)
                            }
                            (addSymbol(symbol, Symbols.Chars(s.toSet)), ThisNode, None)
                        case AST.StringLiteral(_, value) =>
                            val s = addSymbol(symbol, Symbols.Proxy(GrammarHelper.i(value.toString)))
                            (s, ThisNode, None)
                    }
            }

            def astElemToSymbolAndElemRefMap(ctx: BoundRefs, ast: AST.Elem): (Option[Symbols.Symbol], AstifierExpr, Option[BoundRefs]) = ast match {
                case processor: AST.Processor =>
                    (None, astProcessorToAstifier(ctx, processor), None)
                case symbol: AST.Symbol =>
                    val r = astSymbolToSymbol(symbol)
                    (Some(r._1), r._2, r._3)
            }

            def astElemSequence(seq: List[AST.Elem]): (Symbols.Sequence, AstifierExpr, Option[BoundRefs]) = {
                var syms = List[Symbols.AtomicSymbol]()
                var ctx = BoundRefs(BoundType.Sequence, List())
                seq foreach { subelem =>
                    val r = astElemToSymbolAndElemRefMap(ctx, subelem)

                    r._1 match {
                        case Some(atom: Symbols.AtomicSymbol) =>
                            syms :+= atom
                            ctx = ctx.appendRef(r._2, r._3)
                        case Some(seqSym: Symbols.Sequence) =>
                            val proxySym = Symbols.Proxy(seqSym)
                            syms :+= proxySym
                            ctx = ctx.appendRef(Unbinder(r._2, proxySym), r._3)
                        case None =>
                            // no symbol
                            ctx = ctx.appendRef(r._2, r._3)
                    }
                }
                val seqSym = Symbols.Sequence(syms)
                (seqSym, Unbinder(ThisNode, seqSym), Some(ctx))
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

        class TypeDependenceGraphBuilder() {
            private var graph = TypeDependenceGraph.emptyGraph

            private def addNode[T <: Node](node: T): T = {
                graph = graph.addNode(node)
                node
            }

            private def addEdge(edge: Edge): Edge = {
                graph = graph.addEdge(edge)
                edge
            }

            private def typeDescToTypeNode(typeDesc: AST.TypeDesc): TypeNode = {
                val valueTypeNode = typeDesc.typ match {
                    case AST.ArrayTypeDesc(_, elemType) =>
                        val elemTypeNode = typeDescToTypeNode(elemType)
                        addNode(TypeArray(elemTypeNode))
                    case AST.TypeName(_, typeName) =>
                        ClassTypeNode(typeName.toString)
                    case AST.OnTheFlyTypeDef(_, name, _) =>
                        ClassTypeNode(name.name.toString)
                }
                if (typeDesc.optional) addNode(TypeOptional(valueTypeNode)) else valueTypeNode
            }

            private var classParamNodes = Map[String, List[ParamNode]]()

            def analyze(): TypeDependenceGraph = {
                userDefinedTypes foreach {
                    case UserClassType(className, params) =>
                        val classNode = addNode(ClassTypeNode(className))
                        val paramNodes = params.zipWithIndex map { case (paramAst, paramIdx) =>
                            val paramNode = addNode(ParamNode(className, paramIdx, paramAst.name.name.toString))
                            // ClassNode --has--> ParamNode
                            addEdge(Edge(classNode, paramNode, EdgeTypes.Has))
                            paramAst.typeDesc foreach { typeDesc =>
                                val paramType = addNode(typeDescToTypeNode(typeDesc))
                                // ParamNode --is--> TypeNode
                                addEdge(Edge(paramNode, paramType, EdgeTypes.Is))
                            }
                            paramNode
                        }
                        classParamNodes += className -> paramNodes
                    case typ@UserAbstractType(name) =>
                        val abstractType = addNode(ClassTypeNode(name))
                        superTypes(typ) foreach { superTyp =>
                            val subType = addNode(ClassTypeNode(superTyp))
                            // ClassTypeNode --extends--> ClassTypeNode
                            addEdge(Edge(subType, abstractType, EdgeTypes.Extends))
                        }
                }
                ruleDefs foreach { ruleDef =>
                    val lhsName = ruleDef.lhs.name.name.toString
                    val lhsNontermNode = addNode(SymbolNode(Symbols.Nonterminal(lhsName)))

                    ruleDef.lhs.typeDesc foreach { lhsType =>
                        val lhsTypeNode = typeDescToTypeNode(lhsType)
                        // SymbolNode --is--> TypeNode
                        addEdge(Edge(lhsNontermNode, lhsTypeNode, EdgeTypes.Is))
                    }

                    // TODO ruleDef.rhs 를 하나씩 순회하면서 Param accept expr
                    ruleDef.rhs foreach { rhs =>
                        def visitBoundExpr(node: ExprNode, ctx: List[AST.Elem], expr: AST.BoundPExpr): ExprNode = expr match {
                            case AST.BoundPExpr(_, ctxRef, boundedExpr) =>
                                ctx(ctxRef.idx.toString.toInt) match {
                                    case _: AST.Processor =>
                                        throw new Exception("Invalid bound context")
                                    case symbol: AST.Symbol =>
                                        symbol match {
                                            case AST.Repeat(_, repeatingSymbol, repeatSpec) =>
                                                // val repeatingSymbolNode = addNode(SymbolNode(astToSymbol(repeatingSymbol)))
                                                // TODO ctx 처리
                                                val bound = repeatingSymbol match {
                                                    case AST.InPlaceChoices(_, List(choice)) => choice.seq
                                                    case AST.Longest(_, AST.InPlaceChoices(_, List(choice))) => choice.seq
                                                    case symbol => List(symbol)
                                                }
                                                val elemNode = boundedExpr match {
                                                    case expr: AST.OnTheFlyTypeDefConstructExpr => visitExpr(bound, expr)
                                                    case expr: AST.PExpr => visitExpr(bound, expr)
                                                    case expr: AST.BoundPExpr =>
                                                        // TODO 첫번째 인자 node 수정
                                                        visitBoundExpr(node, bound, expr)
                                                }
                                                repeatSpec.toString match {
                                                    case "?" =>
                                                        val typeNode = addNode(TypeGenOptional(elemNode))
                                                        // ExprNode --is--> TypeNode
                                                        addEdge(Edge(node, typeNode, EdgeTypes.Is))
                                                        // TypeNode --accepts--> ElemNode (informative)
                                                        addEdge(Edge(typeNode, elemNode, EdgeTypes.Accepts))
                                                    case "*" | "+" =>
                                                        val typeNode = addNode(TypeGenArray(elemNode))
                                                        // ExprNode --is--> TypeNode
                                                        addEdge(Edge(node, typeNode, EdgeTypes.Is))
                                                        // TypeNode --accepts--> ElemNode (informative)
                                                        addEdge(Edge(typeNode, elemNode, EdgeTypes.Accepts))
                                                }
                                                node
                                            case AST.Paren(_, AST.InPlaceChoices(_, choices)) =>
                                                // TODO
                                                ???
                                            case AST.Longest(_, AST.InPlaceChoices(_, choices)) =>
                                                // TODO
                                                ???
                                            case AST.InPlaceSequence(_, seq) =>
                                                // TODO
                                                ???
                                            case _ =>
                                                throw new Exception("Invalid bound context")
                                        }
                                }
                        }

                        def visitExpr(ctx: List[AST.Elem], expr: AST.PExpr): ExprNode = {
                            val node = addNode(ExprNode(expr))
                            expr match {
                                case AST.BinOpExpr(_, operator, operand1, operand2) =>
                                    operator.toString match {
                                        case "+" =>
                                            val op1 = visitExpr(ctx, operand1)
                                            val op2 = visitExpr(ctx, operand2)
                                            val typeNode = addNode(TypeGenArrayConcatOp(operator.toString, op1, op2))
                                            // ExprNode --is--> TypeNode
                                            addEdge(Edge(node, typeNode, EdgeTypes.Is))
                                            // TypeNode --accepts--> ExprNode (informative)
                                            addEdge(Edge(typeNode, op1, EdgeTypes.Accepts))
                                            addEdge(Edge(typeNode, op2, EdgeTypes.Accepts))
                                    }
                                case AST.Ref(_, idx) =>
                                    // ExprNode --accepts--> ElemNode
                                    addEdge(Edge(node, visitElem(ctx, ctx(idx.toString.toInt)), EdgeTypes.Accepts))
                                case bound: AST.BoundPExpr => visitBoundExpr(node, ctx, bound)
                                case AST.ConstructExpr(_, typ, params) =>
                                    val className = typ.name.toString
                                    // ExprNode --is--> ClassTypeNode
                                    addEdge(Edge(node, ClassTypeNode(className), EdgeTypes.Is))
                                    // ParamNode --accepts--> ExprNode
                                    params.zipWithIndex foreach { case (paramExpr, paramIdx) =>
                                        addEdge(Edge(classParamNodes(className)(paramIdx), visitExpr(ctx, paramExpr), EdgeTypes.Accepts))
                                    }
                                case AST.OnTheFlyTypeDefConstructExpr(_, typeDef, params) =>
                                    val className = typeDef.name.name.toString
                                    // ExprNode --is--> ClassTypeNode
                                    addEdge(Edge(node, ClassTypeNode(className), EdgeTypes.Is))
                                    params.zipWithIndex foreach { case (paramExpr, paramIdx) =>
                                        val paramNode = classParamNodes(className)(paramIdx)
                                        assert(paramNode.name == paramExpr.name.name.toString)
                                        if (paramExpr.typeDesc.isDefined) {
                                            val typeDesc = typeDescToTypeNode(paramExpr.typeDesc.get)
                                            // ParamNode --is--> ExprNode
                                            addEdge(Edge(paramNode, typeDesc, EdgeTypes.Is))
                                        }
                                        // ParamNode --accepts--> ExprNode
                                        addEdge(Edge(paramNode, visitExpr(ctx, paramExpr.expr), EdgeTypes.Accepts))
                                    }
                                case AST.PTermParen(_, parenExpr) =>
                                    // ExprNode --accepts--> ExprNode
                                    addEdge(Edge(node, visitExpr(ctx, parenExpr), EdgeTypes.Accepts))
                                case AST.PTermSeq(_, elems) =>
                                    val elemNodes = elems map {
                                        visitExpr(ctx, _)
                                    }
                                    val typeNode = addNode(TypeGenArrayElemsUnion(elemNodes))
                                    // ExprNode --is--> TypeNode
                                    addEdge(Edge(node, typeNode, EdgeTypes.Is))
                                    // TypeNode --accepts--> ExprNode (informative)
                                    elemNodes foreach { seqElem =>
                                        addEdge(Edge(typeNode, seqElem, EdgeTypes.Accepts))
                                    }
                            }
                            node
                        }

                        def visitElem(ctx: List[AST.Elem], elem: AST.Elem): ElemNode = elem match {
                            case processor: AST.Processor =>
                                processor match {
                                    case AST.Ref(_, idx) =>
                                        visitElem(ctx, ctx(idx.toString.toInt))
                                    case expr: AST.PExpr =>
                                        visitExpr(ctx, expr)
                                        addNode(ExprNode(expr))
                                }
                            case symbol: AST.Symbol => addNode(SymbolNode(astToSymbols(symbol)))
                        }

                        val elemNodes: List[Node] = rhs.elems map { e => visitElem(rhs.elems, e) }

                        // SymbolNode --accepts--> ExprNode|SymbolNode
                        val lastElem = elemNodes.last
                        addEdge(Edge(lhsNontermNode, lastElem, EdgeTypes.Accepts))
                    }
                }
                graph
            }
        }

        def generateClassDefs(typeDependenceGraph: TypeDependenceGraph, typeHierarchyGraph: TypeHierarchyGraph): List[ClassDef] = {
            userDefinedTypes.reverse map { typ =>
                val supers = typeHierarchyGraph.edgesByEnd(ClassType(typ.name)) map {
                    _.start match {
                        case ClassType(className) => className
                        case t => throw new Exception(s"Invalid hierarchy: class ${typ.name} extends $t")
                    }
                }
                typ match {
                    case UserAbstractType(name) =>
                        ClassDef(name, isAbstract = true, List(), supers.toList.sorted)
                    case UserClassType(name, paramsAst) =>
                        val paramNodes = (typeDependenceGraph.edgesByStart(TypeDependenceGraph.ClassTypeNode(typ.name)) map {
                            _.end.asInstanceOf[TypeDependenceGraph.ParamNode]
                        }).toList.sortBy(_.paramIdx)
                        assert(paramNodes.size == paramsAst.size && paramNodes.map(_.name) == paramsAst.map(_.name.name.toString))
                        val params = paramNodes map { p =>
                            val nodeType = typeDependenceGraph.inferType(p)
                            val paramType = nodeType.fixedType.getOrElse {
                                val types = typeHierarchyGraph.removeRedundantTypesFrom(nodeType.inferredTypes) map typeHierarchyGraph.cleanType
                                if (types.size == 1) types.head else UnionType(types)
                            }
                            ClassParam(p.name, paramType)
                        }
                        ClassDef(name, isAbstract = false, params, supers.toList.sorted)
                }
            }
        }

        def analyze(grammarName: String = "Intermediate"): Analysis = {
            collectTypeDefs()
            // TODO check name conflict in userDefinedTypes
            // TODO make sure no type has name the name "Node"

            // Check duplicate definitions of nonterminal
            val ruleNames = grammarAst.defs.collect { case r: AST.Rule => r }.groupBy(_.lhs.name.name.toString)
            if (ruleNames exists (_._2.size >= 2)) {
                val duplicates = (ruleNames filter (_._2.size >= 2)).keys
                throw new Exception(s"Duplicate definitions of nonterminal ${duplicates mkString ", "}")
            }

            // grammar를 만들면서 Node -> AST 변환 코드도 생성
            val astifierGenerator = new GrammarAndAstifierGenerator()
            val astifiers = grammarAst.defs collect {
                case AST.Rule(_, lhs, rhs) =>
                    val processedR = rhs map { r =>
                        val (symbol, _, boundRef) = astifierGenerator.astElemSequence(r.elems)
                        val lastExpr = boundRef.get.refs.last._1
                        (symbol, lastExpr)
                    }
                    lhs.name.name.toString -> processedR
            }

            val typeDependenceGraph = new TypeDependenceGraphBuilder().analyze()

            val typeHierarchyGraph0 = typeDependenceGraph.typeHierarchyGraph
            val typeHierarchyGraph = typeHierarchyGraph0.pruneRedundantEdges

            // typeHierarchyGraph에서 ParseNodeType을 subtype으로 갖고있는 노드가 있으면 오류 발생
            if (typeHierarchyGraph.edgesByEnd(ParseNodeType).nonEmpty) {
                throw new Exception(s"Incomplete hierarchy from ${typeHierarchyGraph.edgesByEnd(ParseNodeType) map (_.start)}")
            }
            // TODO typeHierarchyGraph에 unrollArrayAndOptionals

            val classDefs = generateClassDefs(typeDependenceGraph, typeHierarchyGraph)

            new Analysis(grammarAst, astifiers, typeDependenceGraph, typeHierarchyGraph, classDefs)
        }
    }

    def analyze(grammar: AST.Grammar): Analysis = {
        new Analyzer(grammar).analyze()
    }
}
