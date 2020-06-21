package com.giyeok.jparser.metalang3.analysis

import com.giyeok.jparser.metalang2.generated.MetaGrammar3Ast
import com.giyeok.jparser.metalang3.symbols.Escapes._
import com.giyeok.jparser.metalang3.types.TypeFunc
import com.giyeok.jparser.metalang3.types.TypeFunc._
import com.giyeok.jparser.metalang3.valueify.ValueifyExpr

class GrammarInfoCollector {
    var nonterminalTypes: Map[String, List[TypeFunc]] = Map()
    // (클래스 이름, param index)에 지정된 파라메터 타입 목록
    var classParamSpecs: Map[String, List[List[(String, Option[TypeFunc])]]] = Map()
    // (클래스 이름, param index)가 받는 valueifyexpr들의 목록
    var classParamExprs: Map[(String, Int), List[ValueifyExpr]] = Map()
    var classConstructs: Map[String, List[List[ValueifyExpr]]] = Map()
    var exprParamExprs: Map[(ValueifyExpr, Int), List[ValueifyExpr]] = Map()

    // `symbol`이 `typeFunc`의 값을 accept함
    def addNonterminalType(nonterminalName: String, typeFunc: TypeFunc): Unit = {
        nonterminalTypes += nonterminalName -> (nonterminalTypes.getOrElse(nonterminalName, List()) :+ typeFunc)
    }

    def addConcreteClassParamValues(className: String, paramExprs: List[ValueifyExpr]): Unit = {
        classConstructs += className -> (classConstructs.getOrElse(className, List()) :+ paramExprs)
        paramExprs.zipWithIndex.foreach(pi => {
            val pos = (className, pi._2)
            classParamExprs += pos -> (classParamExprs.getOrElse(pos, List()) :+ pi._1)
        })
    }

    def addFuncCallParamValues(funcCallExpr: ValueifyExpr, paramExprs: List[ValueifyExpr]): Unit = {
        paramExprs.zipWithIndex.foreach(pi => addExprParamExpr(funcCallExpr, pi._2, pi._1))
    }

    def addClassParamSpecs(className: String, params: List[(String, Option[TypeFunc])]): Unit = {
        classParamSpecs += className -> (classParamSpecs.getOrElse(className, List()) :+ params)
    }

    // `callee`의 `index`번 파라메터는 `value`의 값을 받게 됨
    def addExprParamExpr(callee: ValueifyExpr, index: Int, value: ValueifyExpr): Unit = {
        val pos = (callee, index)
        exprParamExprs += pos -> (exprParamExprs.getOrElse(pos, List()) :+ value)
    }

    private def addAbstractClass(name: String): Unit = {
        // TODO
    }

    private def addConcreteClass(name: String): Unit = {
        // TODO
    }

    private def addTypeRelation(superType: String, subType: String): Unit = {
        // TODO
    }

    var classSuperTypes: Map[String, List[List[String]]] = Map()
    var classSubTypes: Map[String, List[List[String]]] = Map()

    def addSuperTypes(className: String, superClassNames: List[String]): Unit = {
        classSuperTypes += className -> (classSuperTypes.getOrElse(className, List()) :+ superClassNames)
    }

    def addSubTypes(className: String, subClassNames: List[String]): Unit = {
        classSubTypes += className -> (classSubTypes.getOrElse(className, List()) :+ subClassNames)
    }

    def typeFuncOf(typeDesc: MetaGrammar3Ast.TypeDesc): TypeFunc = {
        val valType: TypeFunc = typeDesc.typ match {
            case typeDef: MetaGrammar3Ast.TypeDef =>
                addTypeDef(typeDef)
                typeDef match {
                    case classDef: MetaGrammar3Ast.ClassDef =>
                        classDef match {
                            case MetaGrammar3Ast.AbstractClassDef(_, name, supers) => ClassType(name.stringName)
                            case MetaGrammar3Ast.ConcreteClassDef(_, name, supers, _) => ClassType(name.stringName)
                        }
                    case MetaGrammar3Ast.SuperDef(_, typeName, _, _) => ClassType(typeName.stringName)
                    case MetaGrammar3Ast.EnumTypeDef(_, name, _) => EnumType(name.stringName)
                }
            case MetaGrammar3Ast.ArrayTypeDesc(_, elemType) => ArrayOf(typeFuncOf(elemType))
            case valueType: MetaGrammar3Ast.ValueType => valueType match {
                case MetaGrammar3Ast.BooleanType(_) => BoolType
                case MetaGrammar3Ast.CharType(_) => CharType
                case MetaGrammar3Ast.StringType(_) => StringType
            }
            case MetaGrammar3Ast.AnyType(_) => AnyType
            case name: MetaGrammar3Ast.EnumTypeName => EnumType(name.stringName)
            case name: MetaGrammar3Ast.TypeName => ClassType(name.stringName)
        }
        if (typeDesc.optional.isEmpty) valType else OptionalOf(valType)
    }

    def addTypeDef(typeDef: MetaGrammar3Ast.TypeDef): Unit = typeDef match {
        case classDef: MetaGrammar3Ast.ClassDef => classDef match {
            case MetaGrammar3Ast.AbstractClassDef(_, name, supers0) =>
                val supers = supers0.getOrElse(List())
                addSuperTypes(name.stringName, supers.map(_.stringName))
            case MetaGrammar3Ast.ConcreteClassDef(_, name, supers0, params0) =>
                val className = name.stringName
                val supers = supers0.getOrElse(List())
                val params = params0.getOrElse(List())
                addSuperTypes(className, supers.map(_.stringName))
                addClassParamSpecs(className, params.map(p => (p.name.stringName, p.typeDesc.map(typeFuncOf))))
        }
        case MetaGrammar3Ast.SuperDef(_, typeName, subs0, supers0) =>
            // typeName의 subtype은 subs로 한정됨. 그 외의 타입이 들어오면 error
            val className = typeName.stringName
            val subs = subs0.getOrElse(List())
            val supers = supers0.flatten.getOrElse(List())
            addSuperTypes(className, supers.map(_.stringName))
            addSubTypes(className, subs.map {
                case classDef: MetaGrammar3Ast.ClassDef =>
                    addTypeDef(classDef)
                    classDef match {
                        case MetaGrammar3Ast.AbstractClassDef(astNode, name, supers) => name.stringName
                        case MetaGrammar3Ast.ConcreteClassDef(astNode, name, supers, params) => name.stringName
                    }
                case sup: MetaGrammar3Ast.SuperDef =>
                    addTypeDef(sup)
                    sup.typeName.stringName
                case name: MetaGrammar3Ast.TypeName => name.stringName
            })
        case MetaGrammar3Ast.EnumTypeDef(_, name, values) =>
        // 이 enum type의 value는 values만 있음. 그 외의 값이 들어오면 error
        // TODO
    }

    def validate(): Unit = {
        // SuperDef들 사이에 conflict이 없는지 확인
        // EnumTypeDef들 사이에 conflict이 없는지 확인
    }
}
