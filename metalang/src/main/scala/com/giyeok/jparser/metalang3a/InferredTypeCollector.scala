package com.giyeok.jparser.metalang3a

import com.giyeok.jparser.metalang3a.ClassRelationCollector.Super
import com.giyeok.jparser.metalang3a.Type._
import com.giyeok.jparser.utils.{AbstractEdge, AbstractGraph}

import scala.collection.MapView

class InferredTypeCollector(private val startNonterminalName: String,
                            private val classInfo: ClassInfoCollector,
                            private val allNonterms: Set[String],
                            private val nonterminalInfo: NonterminalInfoCollector) {
    private var _classRelations: ClassRelationCollector = ClassRelationCollector.empty
    private var _nontermTypes: Map[String, Type] = nonterminalInfo.specifiedTypes
    private var _classParamTypes: Map[String, List[Option[Type]]] = classInfo.classParamSpecs.map(kv =>
        kv._1 -> kv._2.params.map(param => param.typ))

    def classRelations: ClassRelationCollector = _classRelations

    def classParamTypes: MapView[String, List[Type]] = _classParamTypes.view.mapValues(_.flatten)

    private def initialize(): Unit = {
        classInfo.allClasses.foreach(className => _classRelations = _classRelations.addNode(className))
        classInfo.classSuperTypes.foreach(target => target._2.foreach(sup =>
            _classRelations = _classRelations.addEdge(Super(sup, target._1))))
        classInfo.classSubTypes.foreach(target => target._2.foreach(sub =>
            _classRelations = _classRelations.addEdge(Super(target._1, sub))))
    }

    initialize()

    def isComplete(): Boolean = _nontermTypes.keySet == allNonterms && _classParamTypes.forall(!_._2.contains(None))

    // 새로 얻어낸 정보가 있으면 true, 없으면 false
    def tryInference(): Boolean = {
        var newInfo = false

        val typeInferer = new TypeInferer(startNonterminalName, _nontermTypes)
        _classParamTypes = _classParamTypes.map { kv =>
            val (className, paramTypes) = kv
            className -> paramTypes.zipWithIndex.map {
                case (fixedType@Some(_), _) => fixedType
                case (None, index) =>
                    val calls = classInfo.classConstructCalls(className).map(_ (index))
                    val callTypes = calls.flatMap(typeInferer.typeOfValuefyExpr)
                    if (callTypes.isEmpty) None else {
                        newInfo = true
                        Some(unifyTypes(callTypes.toSet))
                    }
            }
        }
        allNonterms.foreach { nontermName =>
            val exprTypes = nonterminalInfo.exprs(nontermName).flatMap(typeInferer.typeOfValuefyExpr)
            if (exprTypes.nonEmpty) {
                nonterminalInfo.specifiedTypes.get(nontermName) match {
                    case Some(specifiedType) =>
                        exprTypes.foreach { receivingType =>
                            if (_classRelations.isNewTypeRelation(specifiedType, receivingType)) {
                                newInfo = true
                                _classRelations = _classRelations.addTypeRelation(specifiedType, receivingType)
                            }
                        }
                    case None =>
                        val inferredNontermType = unifyTypes(exprTypes.toSet)
                        newInfo ||= !_nontermTypes.get(nontermName).contains(inferredNontermType)
                        _nontermTypes += nontermName -> inferredNontermType
                }
            }
        }
        newInfo
    }
}

class ClassRelationCollector private(override val nodes: Set[String], override val edges: Set[Super],
                                     override val edgesByStart: Map[String, Set[Super]], override val edgesByEnd: Map[String, Set[Super]])
    extends AbstractGraph[String, Super, ClassRelationCollector] {
    override def createGraph(nodes: Set[String], edges: Set[Super], edgesByStart: Map[String, Set[Super]], edgesByEnd: Map[String, Set[Super]]): ClassRelationCollector =
        new ClassRelationCollector(nodes, edges, edgesByStart, edgesByEnd)

    def isNewTypeRelation(supertype: Type, subtype: Type): Boolean = (supertype, subtype) match {
        case (NodeType, NodeType) => false
        case (ClassType(superclass), ClassType(subclass)) => isNewClassRelation(superclass, subclass)
        case (OptionalOf(superopt), OptionalOf(subopt)) => isNewTypeRelation(superopt, subopt)
        case (ArrayOf(superelem), ArrayOf(subelem)) => isNewTypeRelation(superelem, subelem)
        case (EnumType(superEnumName), EnumType(subEnumName)) if superEnumName == subEnumName => false
        case (EnumType(_), UnspecifiedEnumType(_)) => false
        case (UnspecifiedEnumType(_), EnumType(_)) => false
        case (UnspecifiedEnumType(_), UnspecifiedEnumType(_)) => false
        case (OptionalOf(_), NullType) => false
        case (AnyType, _) => false
        case (BoolType, BoolType) => false
        case (CharType, CharType) => false
        case (StringType, StringType) => false
    }

    def addTypeRelation(supertype: Type, subtype: Type): ClassRelationCollector = (supertype, subtype) match {
        case (NodeType, NodeType) => this
        case (ClassType(superclass), ClassType(subclass)) => addClassRelation(superclass, subclass)
        case (OptionalOf(superopt), OptionalOf(subopt)) => addTypeRelation(superopt, subopt)
        case (ArrayOf(superelem), ArrayOf(subelem)) => addTypeRelation(superelem, subelem)
        case (EnumType(superEnumName), EnumType(subEnumName)) if superEnumName == subEnumName => this
        case (EnumType(_), UnspecifiedEnumType(_)) => this
        case (UnspecifiedEnumType(_), EnumType(_)) => this
        case (UnspecifiedEnumType(_), UnspecifiedEnumType(_)) => this
        case (OptionalOf(_), NullType) => this
        case (AnyType, _) => this
        case (BoolType, BoolType) => this
        case (CharType, CharType) => this
        case (StringType, StringType) => this
    }

    def isNewClassRelation(superclass: String, subclass: String): Boolean =
        !edges.contains(Super(superclass, subclass))

    def addClassRelation(superclass: String, subclass: String): ClassRelationCollector =
        addEdge(Super(superclass, subclass))

    def hasCycle: Boolean = {
        var visited = Set[String]()

        def traverse(curr: String, path: List[String]): Boolean = {
            visited += curr
            edgesByStart(curr).foldLeft(false) { (m, i) =>
                if (m) true
                else if (path contains i.end) true
                else traverse(i.end, curr +: path)
            }
        }

        nodes.foldLeft(false) { (m, i) => if (m) true else if (visited.contains(i)) false else traverse(i, List(i)) }
    }

    def toHierarchy: ClassHierarchyTree = {
        val allItems = nodes.toList.map(className =>
            ClassHierarchyItem(className, edgesByEnd(className).map(_.superclass), edgesByStart(className).map(_.subclass)))
        val rootItems = allItems.filter(_.superclasses.isEmpty)
        ClassHierarchyTree(allItems.map(i => i.className -> i).toMap, rootItems)
    }
}

object ClassRelationCollector {
    val empty = new ClassRelationCollector(Set(), Set(), Map(), Map())

    case class Super(superclass: String, subclass: String) extends AbstractEdge[String] {
        override val start: String = superclass
        override val end: String = subclass
    }

}

case class ClassHierarchyTree(allTypes: Map[String, ClassHierarchyItem], rootTypes: List[ClassHierarchyItem])

case class ClassHierarchyItem(className: String, superclasses: Set[String], subclasses: Set[String])