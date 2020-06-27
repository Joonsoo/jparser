package com.giyeok.jparser.metalang3a

import com.giyeok.jparser.metalang3a.ClassInfoCollector.ClassSpec
import com.giyeok.jparser.metalang3a.MetaLanguage3.check

case class ClassInfoCollector(allClasses: Set[String],
                              classParamSpecs: Map[String, ClassSpec],
                              classConstructCalls: Map[String, List[List[ValuefyExpr]]],
                              classSuperTypes: Map[String, Set[String]],
                              classSubTypes: Map[String, Set[String]],
                              allEnumTypes: Set[String],
                              enumTypes: Map[String, Set[String]]) {
    def addClassName(className: String): ClassInfoCollector = copy(allClasses = allClasses + className)

    def addClassNames(classNames: List[String]): ClassInfoCollector = copy(allClasses = allClasses ++ classNames.toSet)

    def addClassParamSpecs(className: String, classSpec: ClassSpec): ClassInfoCollector = {
        // className의 파라메터들이 classSpec이라고 정의
        // TODO 만약 두 번 이상 호출됐는데 classSpec이 기존 것과 일치하지 않으면 throw IllegalGrammar

        addClassName(className).copy(classParamSpecs = classParamSpecs + (className -> classSpec))
    }

    def addClassConstructCall(className: String, params: List[ValuefyExpr]): ClassInfoCollector =
        addClassName(className).copy(classConstructCalls = classConstructCalls +
            (className -> (classConstructCalls.getOrElse(className, List()) :+ params)))

    def addClassSuperTypes(className: String, superTypes: List[String]): ClassInfoCollector = {
        // className의 상위 클래스가 superTypes라고 정의
        // TODO superTypes에 duplicate 있으면 오류
        // TODO 만약 두 번 이상 호출됐는데 superTypes가 기존 것과 일치하지 않으면 throw IllegalGrammar

        addClassName(className).addClassNames(superTypes)
            .copy(classSuperTypes = classSuperTypes + (className -> superTypes.toSet))
    }

    def addClassSubTypes(className: String, subTypes: List[String]): ClassInfoCollector = {
        // className의 하위 클래스가 subTypes만 있다고 정의
        // TODO subTypes에 duplicate 있으면 오류
        // TODO 만약 두 번 이상 호출됐는데 subTypes가 기존 것과 일치하지 않으면 throw IllegalGrammar

        addClassName(className).addClassNames(subTypes)
            .copy(classSubTypes = classSubTypes + (className -> subTypes.toSet))
    }

    def addEnumTypeName(enumTypeName: String): ClassInfoCollector =
        copy(allEnumTypes = allEnumTypes + enumTypeName)

    def addEnumType(enumTypeName: String, values: List[String]): ClassInfoCollector = {
        // TODO values에 duplicate 있으면 오류
        // TODO 두번 이상 호출시 기존것과 일치하지 않으면 throw IllegalGrammar
        addEnumTypeName(enumTypeName).copy(enumTypes = enumTypes + (enumTypeName -> values.toSet))
    }

    def validate(): Unit = {
        // TODO supertype과 subtype이 불일치하는 경우 오류 발생
        // 모든 constructcall의 파라메터 갯수가 일치하는지
    }
}

object ClassInfoCollector {
    val empty = new ClassInfoCollector(Set(), Map(), Map(), Map(), Map(), Set(), Map())

    case class ClassSpec(params: List[ClassParamSpec])

    case class ClassParamSpec(name: String, typ: Option[Type])

}

case class NonterminalInfoCollector(specifiedTypes: Map[String, Type],
                                    exprs: Map[String, List[ValuefyExpr]]) {
    def setNonterminalType(nonterminalName: String, specifiedType: Type): NonterminalInfoCollector = {
        check(!specifiedTypes.contains(nonterminalName), "??")
        copy(specifiedTypes = specifiedTypes + (nonterminalName -> specifiedType))
    }

    def addNonterminalExpr(nonterminalName: String, expr: ValuefyExpr): NonterminalInfoCollector = {
        copy(exprs = exprs + (nonterminalName -> (exprs.getOrElse(nonterminalName, List()) :+ expr)))
    }
}

object NonterminalInfoCollector {
    val empty = new NonterminalInfoCollector(Map(), Map())
}
