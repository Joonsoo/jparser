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

  def nonterminalTypes: Map[String, Type] = _nontermTypes

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
      className -> paramTypes.zipWithIndex.map { case (prevType, index) =>
        val specifiedType = classInfo.classParamSpecs(className).params(index).typ
        val calls = classInfo.classConstructCalls(className).map(_ (index))
        val callTypes = calls.flatMap(typeInferer.typeOfValuefyExpr)
        if (callTypes.isEmpty) specifiedType else {
          val inferredParamType = unifyTypes(callTypes.toSet)
          // user specified type이 있으면 addTypeRelation
          specifiedType.foreach { someSpecifiedType =>
            if (_typeRelations.isNewTypeRelation(someSpecifiedType, inferredParamType)) {
              newInfo = true
              _typeRelations = _typeRelations.addTypeRelation(someSpecifiedType, inferredParamType)
            }
          }
          Some(specifiedType.getOrElse(inferredParamType))
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

  def isNewTypeRelation(supertype: Type, subtype: Type)(implicit errorCollector: ErrorCollector): Boolean =
    if (supertype == subtype) false else (supertype, subtype) match {
      // supertype이나 subtype이 union이면 풀어서 하나라도 true이면 true. add할 때도 마찬가지로 풀어서.
      case (UnionOf(supertypes), _) => supertypes.exists(isNewTypeRelation(_, subtype))
      case (_, UnionOf(subtypes)) => subtypes.exists(isNewTypeRelation(supertype, _))
      case (NodeType, NodeType) => false
      case (ClassType(superclass), ClassType(subclass)) => isNewClassRelation(superclass, subclass)
      case (OptionalOf(superopt), OptionalOf(subopt)) => isNewTypeRelation(superopt, subopt)
      case (OptionalOf(opttype), valuetype) if opttype == valuetype => false
      case (ArrayOf(superelem), ArrayOf(subelem)) => isNewTypeRelation(superelem, subelem)
      case (EnumType(superEnumName), EnumType(subEnumName)) if superEnumName == subEnumName => false
      case (EnumType(enumName), UnspecifiedEnumType(unspecifiedId)) => enumRelations.isNewRelation(enumName, unspecifiedId)
      case (UnspecifiedEnumType(unspecifiedId), EnumType(enumName)) => enumRelations.isNewRelation(enumName, unspecifiedId)
      case (UnspecifiedEnumType(unspecifiedId), UnspecifiedEnumType(otherUnspecifiedId)) => enumRelations.isNewRelation(unspecifiedId, otherUnspecifiedId)
      case (OptionalOf(_), NullType) => false
      case (AnyType, _) => false
      case (_, NothingType) => false
      case (BoolType, BoolType) => false
      case (CharType, CharType) => false
      case (StringType, StringType) => false
      case (sup, sub) =>
        // 있을 수 없는 type relation인 경우에는 오류 추가
        errorCollector.addError(s"Invalid type relation ${Type.readableNameOf(sup)} cannot be super of ${Type.readableNameOf(sub)}")
        false
    }

  def addTypeRelation(supertype: Type, subtype: Type)(implicit errorCollector: ErrorCollector): TypeRelationCollector =
    if (supertype == subtype) this else (supertype, subtype) match {
      case (UnionOf(supertypes), _) => supertypes.foldLeft(this) { (m, i) => m.addTypeRelation(i, subtype) }
      case (_, UnionOf(subtypes)) => subtypes.foldLeft(this) { (m, i) => m.addTypeRelation(supertype, i) }
      case (NodeType, NodeType) => this
      case (ClassType(superclass), ClassType(subclass)) => addClassRelation(superclass, subclass)
      case (OptionalOf(superopt), OptionalOf(subopt)) => addTypeRelation(superopt, subopt)
      case (OptionalOf(opttype), valuetype) if opttype == valuetype => this
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

  def checkCycle(implicit errorCollector: ErrorCollector): Boolean = {
    var visited = Set[String]()

    def traverse(curr: String, path: List[String]): Boolean = {
      visited += curr
      edgesByStart(curr).foldLeft(false) { (m, i) =>
        if (m) true
        else if (path contains i.end) {
          errorCollector.addError(s"Cyclic class relation found: ${i.end +: path}")
          true
        } else traverse(i.end, i.end +: path)
      }
    }

    nodes.foldLeft(false) { (m, i) => if (m) true else if (visited.contains(i)) false else traverse(i, List(i)) }
  }

  // A super B, B super C, A super C 이면 A super C는 필요 없기 때문에 엣지에서 제거
  // -> edge중에, 지워도 node간의 reachability에 영향이 없는 엣지들은 지워도 됨 -> 중요한건 node간의 reachability
  def removeDuplicateEdges(): ClassRelationCollector = {
    var nodesReachability = Map[(String, String), Set[List[Super]]]()

    def traverse(node: String, path: List[Super]): Unit = {
      if (path.nonEmpty) {
        val reach = (path.head.start, node)
        nodesReachability += reach -> (nodesReachability.getOrElse(reach, Set()) + path)
      }
      edgesByStart(node).foreach(next => traverse(next.end, path :+ next))
    }

    nodes.foreach(node => traverse(node, List()))

    val essentialEdges = nodesReachability.values.filter(_.size == 1).filter(_.head.size == 1).flatMap(_.map(_.head))

    (edges -- essentialEdges).foldLeft(this) { (m, i) => m.removeEdge(i) }
  }

  def toHierarchy: ClassHierarchyTree = {
    val allItems = nodes.toList.map(className =>
      ClassHierarchyItem(className, edgesByEnd(className).map(_.superclass), edgesByStart(className).map(_.subclass)))
    val rootItems = allItems.filter(_.superclasses.isEmpty)
    // TODO A가 B의 super class일 때, class C extends A, B 이면 C는 B만 상속받으면 되고, union(A, B)는 A로 바꾸면 됨
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

case class ClassHierarchyTree(allTypes: Map[String, ClassHierarchyItem], rootTypes: List[ClassHierarchyItem]) {
  def isSuperClass(superclass: String, subclass: String): Boolean =
    if (allTypes(subclass).superclasses.contains(superclass)) true
    else allTypes(subclass).superclasses.exists(isSuperClass(superclass, _))

  def simplifyType(typ: Type): Type = typ match {
    case OptionalOf(typ) => OptionalOf(simplifyType(typ))
    case ArrayOf(elemType) => ArrayOf(simplifyType(elemType))
    case union: UnionOf => simplifyUnionType(union)
    case _ => typ
  }

  def simplifyUnionType(unionType: UnionOf): Type = {
    // unionType에 있는 ClassType 중, superclass-subclass 가 같이 들어 있으면 subclass는 필요 없음
    val (classtypes0, othertypes) = unionType.types.partition(_.isInstanceOf[ClassType])
    val classtypes = classtypes0.map(_.asInstanceOf[ClassType].name)
    val simplifiedClassTypes = classtypes.filter(subtype => (classtypes - subtype).forall(isSuperClass(_, subtype)))
    if (othertypes.isEmpty && simplifiedClassTypes.size == 1) {
      ClassType(simplifiedClassTypes.head)
    } else {
      UnionOf(simplifiedClassTypes.map(ClassType) ++ othertypes)
    }
  }
}

case class ClassHierarchyItem(className: String, superclasses: Set[String], subclasses: Set[String])
  extends Ordered[ClassHierarchyItem] {
  override def compare(that: ClassHierarchyItem): Int = className.compare(that.className)
}

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
