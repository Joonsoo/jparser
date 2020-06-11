package com.giyeok.jparser.metalang2

import com.giyeok.jparser.Symbols
import com.giyeok.jparser.metalang2.TypeDependenceGraph._

case class ClassParam(name: String, typ: TypeSpec)

case class ClassDef(name: String, isAbstract: Boolean, params: List[ClassParam], supers: List[String]) {
    // isAbstract --> params must be empty
    assert(!isAbstract || params.isEmpty)
}

class TypeAnalysis(val typeDependenceGraph: TypeDependenceGraph,
                   val typeHierarchyGraph: TypeHierarchyGraph,
                   val classDefs: List[ClassDef])

class TypeAnalyzer(val astAnalyzer: AstAnalyzer) {
    val ruleDefs: List[AST.Rule] = astAnalyzer.grammarAst.defs collect { case r: AST.Rule => r }

    sealed trait UserDefinedType {
        val name: String
    }

    case class UserClassType(name: String, params: List[AST.NamedParam]) extends UserDefinedType

    case class UserAbstractType(name: String) extends UserDefinedType

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
                ArrayTypeOf(elemTypeNode)
            case AST.TypeName(_, typeName) if typeName.toString == "Node" =>
                TypeSpecNode(ParseNodeType)
            case AST.TypeName(_, typeName) =>
                TypeSpecNode(ClassType(typeName.toString))
            case AST.OnTheFlyTypeDef(_, name, _) =>
                TypeSpecNode(ClassType(name.name.toString))
        }
        if (typeDesc.optional) addNode(OptionalTypeOf(valueTypeNode)) else valueTypeNode
    }

    private var classParamNodes = Map[String, List[ParamNode]]()

    def createTypeDependencyGraph(): TypeDependenceGraph = {
        userDefinedTypes foreach {
            case UserClassType(className, params) =>
                val classNode = addNode(TypeSpecNode(ClassType(className)))
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
                val abstractType = addNode(TypeSpecNode(ClassType(name)))
                superTypes(typ) foreach { superTyp =>
                    val subType = addNode(TypeSpecNode(ClassType(superTyp)))
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
                                                val typeNode = addNode(OptionalTypeOf(addNode(TypeOfElemNode(elemNode))))
                                                // ExprNode --is--> TypeNode
                                                addEdge(Edge(node, typeNode, EdgeTypes.Is))
                                                // TypeNode --accepts--> ElemNode (informative)
                                                addEdge(Edge(typeNode, elemNode, EdgeTypes.Accepts))
                                            case "*" | "+" =>
                                                val typeNode = addNode(ArrayTypeOf(TypeOfElemNode(elemNode)))
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
                                        println(symbol)
                                        throw new Exception("Invalid bound context")
                                }
                        }
                }

                def mergeTypes(typeNodes: List[TypeNode]): TypeNode =
                    if (typeNodes.size == 1) typeNodes.head else UnionTypeOf(typeNodes.toSet)

                def visitExpr(ctx: List[AST.Elem], expr: AST.PExpr): ExprNode = {
                    val node = addNode(ExprNode(expr))
                    expr match {
                        case AST.BinOpExpr(_, operator, operand1, operand2) =>
                            operator.toString match {
                                case "+" =>
                                    val op1 = visitExpr(ctx, operand1)
                                    val op2 = visitExpr(ctx, operand2)
                                    val typeNode = addNode(mergeTypes(List(TypeOfElemNode(op1), TypeOfElemNode(op2))))
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
                            addEdge(Edge(node, TypeSpecNode(ClassType(className)), EdgeTypes.Is))
                            // ParamNode --accepts--> ExprNode
                            params.zipWithIndex foreach { case (paramExpr, paramIdx) =>
                                addEdge(Edge(classParamNodes(className)(paramIdx), visitExpr(ctx, paramExpr), EdgeTypes.Accepts))
                            }
                        case AST.OnTheFlyTypeDefConstructExpr(_, typeDef, params) =>
                            val className = typeDef.name.name.toString
                            // ExprNode --is--> ClassTypeNode
                            addEdge(Edge(node, TypeSpecNode(ClassType(className)), EdgeTypes.Is))
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
                            val typeNode = addNode(ArrayTypeOf(mergeTypes(elemNodes.map(TypeOfElemNode))))
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
                    case symbol: AST.Symbol =>
                        val symbolNode = addNode(SymbolNode(astAnalyzer.astToSymbols(symbol)))
                        symbol match {
                            case AST.Repeat(_, repeatingSymbolAst, repeatSpec) =>
                                val repeatingSymbolNode = addNode(SymbolNode(astAnalyzer.astToSymbols(repeatingSymbolAst)))
                                val typeNode = repeatSpec.toString match {
                                    case "?" =>
                                        OptionalTypeOf(TypeOfElemNode(repeatingSymbolNode))
                                    case "*" | "+" =>
                                        ArrayTypeOf(TypeOfElemNode(repeatingSymbolNode))
                                }
                                // SymbolNode --is--> TypeNode
                                addEdge(Edge(symbolNode, addNode(typeNode), EdgeTypes.Is))
                                // TypeNode --accepts--> SymbolNode (informative)
                                addEdge(Edge(typeNode, repeatingSymbolNode, EdgeTypes.Accepts))
                            case _ => // do nothing
                        }
                        symbolNode
                }

                val elemNodes: List[Node] = rhs.elems map { e => visitElem(rhs.elems, e) }

                // SymbolNode --accepts--> ExprNode|SymbolNode
                val lastElem = elemNodes.last
                addEdge(Edge(lhsNontermNode, lastElem, EdgeTypes.Accepts))
            }
        }
        graph
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
                    val paramNodes = (typeDependenceGraph.edgesByStart(TypeDependenceGraph.TypeSpecNode(ClassType(typ.name))) map {
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

    def analyzeTypeHierarchy(): TypeAnalysis = {
        collectTypeDefs()
        // TODO check name conflict in userDefinedTypes
        // TODO make sure no type has name the name "Node"

        val typeDependenceGraph = createTypeDependencyGraph()

        println(typeDependenceGraph.toDotGraphModel.printDotGraph())

        val typeHierarchyGraph0 = typeDependenceGraph.createTypeHierarchyGraph()
        val typeHierarchyGraph = typeHierarchyGraph0.pruneRedundantEdges

        // typeHierarchyGraph에서 ParseNodeType을 subtype으로 갖고있는 노드가 있으면 오류 발생
        if (typeHierarchyGraph.edgesByEnd(ParseNodeType).nonEmpty) {
            throw new Exception(s"Incomplete hierarchy from ${typeHierarchyGraph.edgesByEnd(ParseNodeType) map (_.start)}")
        }
        // TODO typeHierarchyGraph에 unrollArrayAndOptionals

        val classDefs = generateClassDefs(typeDependenceGraph, typeHierarchyGraph)

        new TypeAnalysis(typeDependenceGraph, typeHierarchyGraph, classDefs)
    }
}
