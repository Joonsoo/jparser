package com.giyeok.jparser.metalang3a

import com.giyeok.jparser.metalang3a.ClassInfoCollector.{ClassParamSpec, ClassSpec}
import com.giyeok.jparser.metalang3a.MetaLanguage3.check

case class ClassInfoCollector(allClasses: Set[String],
                              classParamSpecs: Map[String, ClassSpec],
                              classConstructCalls: Map[String, List[List[ValuefyExpr]]],
                              classSuperTypes: Map[String, Set[String]],
                              classSubTypes: Map[String, Set[String]],
                              allEnumTypes: Set[String],
                              enumTypes: Map[String, Set[String]],
                              canonicalEnumValues: Map[String, Set[String]],
                              shortenedEnumValues: Map[Int, Set[String]]) {
    def addClassName(className: String): ClassInfoCollector = copy(allClasses = allClasses + className)

    def addClassNames(classNames: List[String]): ClassInfoCollector = copy(allClasses = allClasses ++ classNames.toSet)

    def addClassParamSpecs(className: String, classSpec: ClassSpec)(implicit errorCollector: ErrorCollector): ClassInfoCollector = {
        // className의 파라메터들이 classSpec이라고 정의
        // 만약 두 번 이상 호출됐는데 classSpec이 기존 것과 일치하지 않으면 error

        val newClassSpec = classParamSpecs get className match {
            case Some(prevSpec) =>
                if (classSpec.params.size != prevSpec.params.size) {
                    errorCollector.addError(s"Inconsistent class params(size mismatch) for $className")
                }
                if (classSpec.params.map(_.name) != prevSpec.params.map(_.name)) {
                    errorCollector.addError(s"Inconsistent class params(name mismatch) for $className")
                }
                // merged class spec
                val mergedParams = classSpec.params.zip(prevSpec.params).map { p =>
                    val name = p._1.name
                    (p._1.typ, p._2.typ) match {
                        case (Some(typ1), Some(typ2)) =>
                            if (typ1 != typ2) {
                                errorCollector.addError(s"Inconsistent class params(type mismatch) for $className")
                            }
                            ClassParamSpec(name, Some(typ1))
                        case (Some(typ), None) => ClassParamSpec(name, Some(typ))
                        case (None, Some(typ)) => ClassParamSpec(name, Some(typ))
                        case (None, None) => ClassParamSpec(name, None)
                    }
                }
                ClassSpec(mergedParams)
            case None => classSpec
        }

        addClassName(className).copy(classParamSpecs = classParamSpecs + (className -> newClassSpec))
    }

    def addClassConstructCall(className: String, params: List[ValuefyExpr]): ClassInfoCollector =
        addClassName(className).copy(classConstructCalls = classConstructCalls +
            (className -> (classConstructCalls.getOrElse(className, List()) :+ params)))

    def addClassSuperTypes(className: String, superTypes: List[String])(implicit errorCollector: ErrorCollector): ClassInfoCollector = {
        // className의 상위 클래스가 superTypes라고 정의
        if (superTypes.toSet.size != superTypes.size) {
            // superTypes에 duplicate 있으면 오류
            errorCollector.addError(s"Super types of $className has duplicates")
            this
        } else if ((classSuperTypes contains className) && (classSuperTypes(className) != superTypes.toSet)) {
            // 만약 두 번 이상 호출됐는데 superTypes가 기존 것과 일치하지 않으면 throw IllegalGrammar
            errorCollector.addError(s"Inconsistent super types of $className")
            this
        } else {
            addClassName(className).addClassNames(superTypes)
                .copy(classSuperTypes = classSuperTypes + (className -> superTypes.toSet))
        }
    }

    def addClassSubTypes(className: String, subTypes: List[String])(implicit errorCollector: ErrorCollector): ClassInfoCollector = {
        // className의 하위 클래스가 subTypes만 있다고 정의
        if (subTypes.toSet.size != subTypes.size) {
            // subTypes에 duplicate 있으면 오류
            errorCollector.addError(s"Subtypes of $className has duplicates")
            this
        } else if ((classSubTypes contains className) && (classSubTypes(className) != subTypes.toSet)) {
            // 만약 두 번 이상 호출됐는데 subTypes가 기존 것과 일치하지 않으면 throw IllegalGrammar
            errorCollector.addError(s"Inconsistent subtypes of $className")
            this
        } else {
            addClassName(className).addClassNames(subTypes)
                .copy(classSubTypes = classSubTypes + (className -> subTypes.toSet))
        }
    }

    def addEnumTypeName(enumTypeName: String): ClassInfoCollector =
        copy(allEnumTypes = allEnumTypes + enumTypeName)

    def addEnumType(enumTypeName: String, values: List[String])(implicit errorCollector: ErrorCollector): ClassInfoCollector = {
        if (values.toSet.size != values.size) {
            // values에 duplicate 있으면 오류
            errorCollector.addError(s"Values of $enumTypeName has duplicates")
            this
        } else if ((enumTypes contains enumTypeName) && (enumTypes(enumTypeName) != values.toSet)) {
            // 두번 이상 호출시 기존것과 일치하지 않으면 throw IllegalGrammar
            errorCollector.addError(s"Inconsistent values of $enumTypeName")
            this
        } else {
            addEnumTypeName(enumTypeName).copy(enumTypes = enumTypes + (enumTypeName -> values.toSet))
        }
    }

    def addCanonicalEnumValue(enumTypeName: String, valueName: String): ClassInfoCollector =
        addEnumTypeName(enumTypeName).copy(canonicalEnumValues = canonicalEnumValues +
            (enumTypeName -> (canonicalEnumValues.getOrElse(enumTypeName, Set()) + valueName)))

    def addShortenedEnumValue(unspecifiedEnumTypeId: Int, valueName: String): ClassInfoCollector =
        copy(shortenedEnumValues = shortenedEnumValues +
            (unspecifiedEnumTypeId -> (shortenedEnumValues.getOrElse(unspecifiedEnumTypeId, Set()) + valueName)))

    def validate()(implicit errorCollector: ErrorCollector): Unit = {
        // supertype과 subtype이 불일치하는 경우는 나중에 TypeRelations에서 확인
        // 모든 constructcall의 파라메터 갯수가 일치하는지 확인
        classConstructCalls.foreach { kv =>
            val (className, calls) = kv
            val callArities = calls.map(_.size).distinct
            if (callArities.size != 1) {
                errorCollector.addError(s"Inconsistent construct call params count on $className")
            }
        }
    }
}

object ClassInfoCollector {
    val empty = new ClassInfoCollector(Set(), Map(), Map(), Map(), Map(), Set(), Map(), Map(), Map())

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
