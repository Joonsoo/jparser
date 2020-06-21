package com.giyeok.jparser.metalang3.analysis

import com.giyeok.jparser.Symbols
import com.giyeok.jparser.metalang2.generated.MetaGrammar3Ast
import com.giyeok.jparser.metalang3.MetaLanguage3.IllegalGrammar
import com.giyeok.jparser.metalang3.types.{ConcreteType, TypeFunc}

class BottomUpTypeInferer(val startSymbolName: String,
                          val symbolsMap: Map[MetaGrammar3Ast.Symbol, Symbols.Symbol],
                          val nonterminalTypeFuncs: Map[String, List[TypeFunc]]) {
    private def unifyTypes(types: Iterable[ConcreteType]): ConcreteType =
        if (types.size == 1) types.head else ConcreteType.UnionOf(types.toList)

    def nonterminalType(nonterminalName: String): ConcreteType = ???

    def typeOf(symbol: MetaGrammar3Ast.Symbol): ConcreteType = typeOf(symbolsMap(symbol))

    def typeOf(symbol: Symbols.Symbol): ConcreteType = symbol match {
        case symbol: Symbols.AtomicSymbol =>
            symbol match {
                case _: Symbols.Terminal => ConcreteType.NodeType
                case Symbols.Start => typeOf(Symbols.Nonterminal(startSymbolName))
                case Symbols.Nonterminal(name) => nonterminalType(name)
                case Symbols.OneOf(syms) => unifyTypes(syms.map(typeOf))
                case Symbols.Repeat(sym, lower) => typeOf(sym)
                case Symbols.Except(sym, except) => typeOf(sym)
                case lookahead: Symbols.Lookahead => ConcreteType.NodeType
                case Symbols.Proxy(sym) => typeOf(sym)
                case Symbols.Join(sym, join) => typeOf(sym)
                case Symbols.Longest(sym) => typeOf(sym)
            }
        case Symbols.Sequence(seq) => typeOf(seq.last)
    }

    def typeOf(typeFunc: TypeFunc): ConcreteType = typeFunc match {
        case TypeFunc.NodeType => ConcreteType.NodeType
        case TypeFunc.TypeOfSymbol(symbol) => typeOf(symbol)
        case TypeFunc.ClassType(name) => ConcreteType.ClassType(name)
        case TypeFunc.OptionalOf(typ) => ConcreteType.OptionalOf(typeOf(typ))
        case TypeFunc.ArrayOf(elemType) => ConcreteType.ArrayOf(typeOf(elemType))
        case TypeFunc.ElvisType(value, ifNull) =>
            val valueType = typeOf(value)
            valueType match {
                case ConcreteType.OptionalOf(valueType) => unifyTypes(Set(valueType, typeOf(ifNull)))
                case _ => throw IllegalGrammar("Only nullable type can be used with ?: operator")
            }
        case TypeFunc.AddOpType(lhs, rhs) =>
            val lhsType = typeOf(lhs)
            val rhsType = typeOf(rhs)
            (lhsType, rhsType) match {
                case (ConcreteType.StringType, ConcreteType.StringType) => ConcreteType.StringType
                case (ConcreteType.ArrayOf(lhsElemType), ConcreteType.ArrayOf(rhsElemType)) =>
                    ConcreteType.ArrayOf(unifyTypes(Set(lhsElemType, rhsElemType)))
                case _ => throw IllegalGrammar("Invalid + operator")
            }
        case TypeFunc.UnionOf(types) => ConcreteType.UnionOf(types.map(typeOf))
        case TypeFunc.EnumType(enumTypeName) => ConcreteType.EnumType(enumTypeName)
        case TypeFunc.UnspecifiedEnum(uniqueId) => ??? // TODO
        case TypeFunc.NullType => ConcreteType.NullType
        case TypeFunc.AnyType => ConcreteType.AnyType
        case TypeFunc.BoolType => ConcreteType.BoolType
        case TypeFunc.CharType => ConcreteType.CharType
        case TypeFunc.StringType => ConcreteType.StringType
    }
}
