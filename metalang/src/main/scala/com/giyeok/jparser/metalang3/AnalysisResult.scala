package com.giyeok.jparser.metalang3

import com.giyeok.jparser.NGrammar.NSymbol
import com.giyeok.jparser.metalang2.generated.MetaGrammar3Ast
import com.giyeok.jparser.metalang3.graphs.GrammarGraphGen
import com.giyeok.jparser.metalang3.types.{ConcreteType, TypeFunc}
import com.giyeok.jparser.metalang3.valueify.UnrollChoices
import com.giyeok.jparser.{NGrammar, Symbols}

// ruleValueifyExprs: Nonterminal name -> ValueifyExpr
class AnalysisResult(val ngrammar: NGrammar,
                     val startSymbolName: String,
                     val valueifyExprsMap: Map[String, UnrollChoices],
                     val symbolsMap: Map[MetaGrammar3Ast.Symbol, Symbols.Symbol],
                     val grammarGraph: GrammarGraphGen) {

    def symbolOf(symbol: MetaGrammar3Ast.Symbol): NSymbol = ngrammar.findSymbol(symbolsMap(symbol)).get._2

    def concreteTypeOf(typeFunc: TypeFunc): ConcreteType = grammarGraph.concreteTypeOf(typeFunc)

    // loose = true이면 union type이 들어왔을 때, 그 모든 union type 외에 다른 타입을 자식으로 가질 수 있는 super type이
    // 있는 경우, 그 타입을 반환한다. loose = false이면 이런 경우 exception 발생
    def mostSpecificSuperTypeOf(typ: ConcreteType, loose: Boolean): ConcreteType = typ match {
        case ConcreteType.UnionOf(types) => ???
        case ConcreteType.OptionalOf(typ) =>
            ConcreteType.OptionalOf(mostSpecificSuperTypeOf(typ, loose))
        case ConcreteType.ArrayOf(elemType) =>
            ConcreteType.ArrayOf(mostSpecificSuperTypeOf(elemType, loose))
        case _: ConcreteType.ClassType | _: ConcreteType.EnumType | ConcreteType.NodeType | ConcreteType.NullType |
             ConcreteType.BoolType | ConcreteType.CharType | ConcreteType.StringType => typ
    }
}
