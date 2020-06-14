package com.giyeok.jparser.metalang3

import com.giyeok.jparser.NGrammar.{NNonterminal, NSequence, NSymbol}
import com.giyeok.jparser.Symbols
import com.giyeok.jparser.Symbols.Nonterminal
import com.giyeok.jparser.metalang2.generated.MetaGrammar3Ast
import com.giyeok.jparser.metalang3.types.{ConcreteType, TypeFunc}
import com.giyeok.jparser.metalang3.valueify.ValueifyExpr

// ruleValueifyExprs: Nonterminal name -> ValueifyExpr
class AnalysisResult(val startNonterminal: String, val valueifyExprsMap: Map[String, ValueifyExpr]) {
    def symbolOf(symbol: MetaGrammar3Ast.Symbol): NSymbol = NNonterminal(1, Nonterminal("Nonterm"), Set())

    def symbolOf(rhs: MetaGrammar3Ast.RHS): NSymbol = NNonterminal(1, Nonterminal("Nonterm"), Set())

    def emptySymbol(): NSymbol = NSequence(1, Symbols.Sequence(Seq()), Seq(1))

    def concreteTypeOf(typeFunc: TypeFunc): ConcreteType = typeFunc match {
        case TypeFunc.NodeType => ConcreteType.NodeType
        case TypeFunc.TypeOfSymbol(symbol) => ConcreteType.NodeType
        case TypeFunc.TypeOfProcessor(processor) => ???
        case TypeFunc.ClassType(name) => ConcreteType.ClassType(name)
        case TypeFunc.OptionalOf(typ) => ConcreteType.OptionalOf(concreteTypeOf(typ))
        case TypeFunc.ArrayOf(elemType) => ConcreteType.ArrayOf(concreteTypeOf(elemType))
        case TypeFunc.ElvisType(value, ifNull) =>
            val valueType = concreteTypeOf(value)
            val ifNullType = concreteTypeOf(ifNull)
            ???
        case TypeFunc.AddOpType(lhs, rhs) => ???
        case TypeFunc.FuncCallResultType(typeOrFuncName, params) => ???
        case TypeFunc.UnionOf(types) => ???
        case TypeFunc.EnumType(enumName) => ???
        case TypeFunc.UnspecifiedEnum(uniqueId) => ???
        case TypeFunc.NullType => ConcreteType.NullType
        case TypeFunc.BoolType => ConcreteType.BoolType
        case TypeFunc.CharType => ConcreteType.CharType
        case TypeFunc.StringType => ConcreteType.StringType
    }

    def mostSpecificSuperTypeOf(typ: ConcreteType): ConcreteType = typ match {
        case ConcreteType.UnionOf(types) => ???
        case ConcreteType.OptionalOf(typ) =>
            ConcreteType.OptionalOf(mostSpecificSuperTypeOf(typ))
        case ConcreteType.ArrayOf(elemType) =>
            ConcreteType.ArrayOf(mostSpecificSuperTypeOf(elemType))
        case _: ConcreteType.ClassType | _: ConcreteType.EnumType | ConcreteType.NodeType | ConcreteType.NullType |
             ConcreteType.BoolType | ConcreteType.CharType | ConcreteType.StringType => typ
    }
}
