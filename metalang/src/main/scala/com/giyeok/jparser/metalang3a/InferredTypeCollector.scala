package com.giyeok.jparser.metalang3a

import com.giyeok.jparser.metalang3a.ClassRelationCollector.Super
import com.giyeok.jparser.metalang3a.Type._
import com.giyeok.jparser.utils.{AbstractEdge, AbstractGraph}

import scala.collection.MapView

class InferredTypeCollector(private val startNonterminalName: String,
                            private val classInfo: ClassInfoCollector,
                            private val allNonterms: Set[String],
                            private val nonterminalInfo: NonterminalInfoCollector)
                           (implicit private val errorCollector: ErrorCollector) {
    private var _typeRelations: TypeRelationCollector = TypeRelationCollector.empty
    private var _nontermTypes: Map[String, Type] = nonterminalInfo.specifiedTypes
    private var _classParamTypes: Map[String, List[Option[Type]]] = classInfo.classParamSpecs.map(kv =>
        kv._1 -> kv._2.params.map(param => param.typ))

    def typeRelations: TypeRelationCollector = _typeRelations

    def classParamTypes: MapView[String, List[Type]] = _classParamTypes.view.mapValues(_.flatten)

    private def initialize(): Unit = {
        // TODO type relations에서 enum value들 수집
        _typeRelations = _typeRelations.initializeFrom(classInfo)
    }

    initialize()

    def isComplete: Boolean = _nontermTypes.keySet == allNonterms && _classParamTypes.forall(!_._2.contains(None))

    // 새로 얻어낸 정보가 있으면 true, 없으면 false
    def tryInference(): Boolean = {
        var newInfo = false

        val typeInferer = new TypeInferer(startNonterminalName, _nontermTypes)
        _classParamTypes = _classParamTypes.map { kv =>
            val (className, paramTypes) = kv
            className -> paramTypes.zipWithIndex.map {
                case (fixedType@Some(_), _) => fixedType
                case (None, index) =>
                    val specifiedType = classInfo.classParamSpecs(className).params(index).typ
                    val calls = classInfo.classConstructCalls(className).map(_ (index))
                    val callTypes = calls.flatMap(typeInferer.typeOfValuefyExpr)
                    if (callTypes.isEmpty) None else {
                        newInfo = true
                        val inferredParamType = unifyTypes(callTypes.toSet)
                        // user specified type이 있으면 addTypeRelation
                        specifiedType.foreach(someSpecifiedType =>
                            _typeRelations = _typeRelations.addTypeRelation(someSpecifiedType, inferredParamType))
                        Some(inferredParamType)
                    }
            }
        }
        allNonterms.foreach { nontermName =>
            val exprTypes = nonterminalInfo.exprs(nontermName).flatMap(typeInferer.typeOfValuefyExpr)
            if (exprTypes.nonEmpty) {
                nonterminalInfo.specifiedTypes.get(nontermName) match {
                    case Some(specifiedType) =>
                        exprTypes.foreach { receivingType =>
                            if (_typeRelations.isNewTypeRelation(specifiedType, receivingType)) {
                                newInfo = true
                                _typeRelations = _typeRelations.addTypeRelation(specifiedType, receivingType)
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

case class TypeRelationCollector(classRelations: ClassRelationCollector, enumRelations: EnumRelationCollector) {
    def initializeFrom(classInfo: ClassInfoCollector): TypeRelationCollector = {
        var _classRelations = classRelations
        classInfo.allClasses.foreach(className => _classRelations = _classRelations.addNode(className))
        classInfo.classSuperTypes.foreach(target => target._2.foreach(sup =>
            _classRelations = _classRelations.addEdge(Super(sup, target._1))))
        classInfo.classSubTypes.foreach(target => target._2.foreach(sub =>
            _classRelations = _classRelations.addEdge(Super(target._1, sub))))

        copy(classRelations = _classRelations)
    }

    // TODO supertype이나 subtype이 union이면 풀어서 하나라도 true이면 true. add할 때도 마찬가지로 풀어서.
    def isNewTypeRelation(supertype: Type, subtype: Type): Boolean = (supertype, subtype) match {
        case (NodeType, NodeType) => false
        case (ClassType(superclass), ClassType(subclass)) => isNewClassRelation(superclass, subclass)
        case (OptionalOf(superopt), OptionalOf(subopt)) => isNewTypeRelation(superopt, subopt)
        case (ArrayOf(superelem), ArrayOf(subelem)) => isNewTypeRelation(superelem, subelem)
        case (EnumType(superEnumName), EnumType(subEnumName)) if superEnumName == subEnumName => false
        case (EnumType(enumName), UnspecifiedEnumType(unspecifiedId)) => enumRelations.isNewRelation(enumName, unspecifiedId)
        case (UnspecifiedEnumType(unspecifiedId), EnumType(enumName)) => enumRelations.isNewRelation(enumName, unspecifiedId)
        case (UnspecifiedEnumType(unspecifiedId), UnspecifiedEnumType(otherUnspecifiedId)) => enumRelations.isNewRelation(unspecifiedId, otherUnspecifiedId)
        case (OptionalOf(_), NullType) => false
        case (AnyType, _) => false
        case (BoolType, BoolType) => false
        case (CharType, CharType) => false
        case (StringType, StringType) => false
    }

    def addTypeRelation(supertype: Type, subtype: Type)(implicit errorCollector: ErrorCollector): TypeRelationCollector = (supertype, subtype) match {
        case (NodeType, NodeType) => this
        case (ClassType(superclass), ClassType(subclass)) => addClassRelation(superclass, subclass)
        case (OptionalOf(superopt), OptionalOf(subopt)) => addTypeRelation(superopt, subopt)
        case (ArrayOf(superelem), ArrayOf(subelem)) => addTypeRelation(superelem, subelem)
        case (EnumType(superEnumName), EnumType(subEnumName)) if superEnumName == subEnumName => this
        case (EnumType(enumName), UnspecifiedEnumType(unspecifiedId)) => addEnumRelation(enumName, unspecifiedId)
        case (UnspecifiedEnumType(unspecifiedId), EnumType(enumName)) => addEnumRelation(enumName, unspecifiedId)
        case (UnspecifiedEnumType(unspecifiedId), UnspecifiedEnumType(otherUnspecifiedId)) => addEnumRelation(unspecifiedId, otherUnspecifiedId)
        case (OptionalOf(_), NullType) => this
        case (AnyType, _) => this
        case (BoolType, BoolType) => this
        case (CharType, CharType) => this
        case (StringType, StringType) => this
    }

    def isNewClassRelation(superclass: String, subclass: String): Boolean =
        !classRelations.edges.contains(Super(superclass, subclass))

    def addClassRelation(superclass: String, subclass: String): TypeRelationCollector =
        copy(classRelations = classRelations.addEdge(Super(superclass, subclass)))

    def addEnumRelation(enumName: String, unspecifiedId: Int)(implicit errorCollector: ErrorCollector): TypeRelationCollector =
        copy(enumRelations = enumRelations.addEnumRelation(enumName, unspecifiedId))

    def addEnumRelation(unspecifiedId: Int, otherUnspecifiedId: Int): TypeRelationCollector =
        copy(enumRelations = enumRelations.addEnumRelation(unspecifiedId, otherUnspecifiedId))
}

object TypeRelationCollector {
    val empty = new TypeRelationCollector(ClassRelationCollector.empty, EnumRelationCollector.empty)
}

class ClassRelationCollector private(override val nodes: Set[String], override val edges: Set[Super],
                                     override val edgesByStart: Map[String, Set[Super]], override val edgesByEnd: Map[String, Set[Super]])
    extends AbstractGraph[String, Super, ClassRelationCollector] {
    override def createGraph(nodes: Set[String], edges: Set[Super], edgesByStart: Map[String, Set[Super]], edgesByEnd: Map[String, Set[Super]]): ClassRelationCollector =
        new ClassRelationCollector(nodes, edges, edgesByStart, edgesByEnd)

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

case class EnumRelationCollector(enumTypes: Set[String], unspecifiedTypes: Set[Int],
                                 directRelations: Map[Int, String], indirectRelations: Set[(Int, Int)]) {
    def isNewRelation(enumName: String, unspecifiedId: Int): Boolean =
        directRelations.get(unspecifiedId) match {
            case Some(prevName) => enumName != prevName
            case None => true
        }

    private def pairOf(id1: Int, id2: Int) = if (id1 < id2) (id1, id2) else (id2, id1)

    def isNewRelation(unspecifiedId: Int, otherUnspecifiedId: Int): Boolean =
        indirectRelations.contains(pairOf(unspecifiedId, otherUnspecifiedId))

    def addEnumRelation(enumType: String, unspecifiedEnumTypeId: Int)(implicit errorCollector: ErrorCollector): EnumRelationCollector = {
        if (directRelations.contains(unspecifiedEnumTypeId) && directRelations(unspecifiedEnumTypeId) != enumType) {
            errorCollector.addError(s"Inconsistent enum value $enumType")
        }
        copy(directRelations = directRelations + (unspecifiedEnumTypeId -> enumType))
    }

    def addEnumRelation(unspecifiedId: Int, otherUnspecifiedId: Int): EnumRelationCollector =
        copy(indirectRelations = indirectRelations + pairOf(unspecifiedId, otherUnspecifiedId))

    def checkConsistency()(implicit errorCollector: ErrorCollector): Unit = {
        // TODO unspecified enum이 두개 이상으로 맵핑될 수 있는 경우 확인
    }

    def toUnspecifiedEnumMap(implicit errorCollector: ErrorCollector): Map[Int, String] = {
        var indirectMap = Map[Int, Set[Int]]()
        indirectRelations.foreach { rel =>
            indirectMap += rel._1 -> (indirectMap.getOrElse(rel._1, Set()) + rel._2)
            indirectMap += rel._2 -> (indirectMap.getOrElse(rel._2, Set()) + rel._1)
        }

        def findConcreteNameFrom(id: Int, visited: Set[Int]): Set[String] = directRelations get id match {
            case Some(concreteName) => Set(concreteName)
            case None =>
                indirectMap.getOrElse(id, Set()).foldLeft(Set[String]()) { (m, i) =>
                    m ++ findConcreteNameFrom(i, visited + i)
                }
        }

        (directRelations.keySet ++ indirectMap.keySet).map { id =>
            val concreteTypes = findConcreteNameFrom(id, Set(id))
            if (concreteTypes.size != 1) {
                errorCollector.addError("Cannot infer enum type")
                id -> ""
            } else {
                id -> concreteTypes.head
            }
        }.toMap
    }
}

object EnumRelationCollector {
    val empty = new EnumRelationCollector(Set(), Set(), Map(), Set())
}
