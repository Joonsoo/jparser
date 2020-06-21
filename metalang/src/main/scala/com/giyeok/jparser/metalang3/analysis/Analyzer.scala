package com.giyeok.jparser.metalang3.analysis

import com.giyeok.jparser.metalang2.generated.MetaGrammar3Ast
import com.giyeok.jparser.metalang2.generated.MetaGrammar3Ast.Def
import com.giyeok.jparser.metalang3.valueify._
import com.giyeok.jparser.{NGrammar, Symbols}

// GrammarGraph는 Valueify를 한 문법 정의를 받아서
// - TypeFunc를 ConcreteType으로 바꿀 수 있는 정보 수집
// - 명시적으로 정의된 타입이 받을 수 없는 expression 타입을 주는 caller나 construct 찾아서 오류 발생
// - class hierarchy 계산
// 등의 목적을 위해 그래프를 구축하고 필요한 정보를 추출해준다.
// 이 그래프는 우선 문법 정의에 명시된 내용만 기록/관리하고, 여기서 inference 가능한 정보는 따로 관리한다.

// 그래프의 노드는
// - SymbolNode
// - PExprNode
//   - FuncCallOrOpNode(ispresent, isempty같은 기본 함수 및 !, +, && 등의 연산자).
//     - PExprNode 중 Ternary, BoolOr/And/Eq, Elvis, Additive, PrefixNot.
//     - FuncCallOrOpParamNode
//   - PAtomNode(FuncCallOrOpNode가 아닌 모든 PExpr)
//   - BindExpr은 어떻게 되지..?
// - TypeNode(TypeFunc가 아닌 ConcreteType 정보, 사용자가 명시적으로 inference 가능한)
//   - ClassParamNode
// 엣지는
// - SymbolNode mustResult TypeNode (사용자 지정)
// - PExprNode mustResult TypeNode (사용자 지정)
// - SymbolNode results TypeNode (inference)
// - PExprNode results TypeNode (inference)
// - TypeNode supers TypeNode
// - TypeNode takes ClassParamNode
// - FuncCallOrOpNode takes FuncCallOrOpParamNode
// - PAtomNode takes PExprNode
// - ParamNode receives PExprNode

// 제공해야 될 기능은
// - validate
//   - TypeHierarchy에 모순이 없는지
//   - ParamNode에 명시된 타입이 있는 경우, 들어오는 PExprNode 중 명시된 타입과 호환되지 않는 것은 없는지
//   - FuncCallOrOpNode에 잘못된 것은 없는지
//     - str + str, [T] + [T] 외에 다른 +가 들어오는 등의 경우
// - (TypeFunc) -> ConcreteType
//   - (UnspecifiedEnumType) -> EnumType
// - (ParamNode) -> ConcreteType
//   - 해당 파라메터의 type
// - () -> TypeHierarchy
class Analyzer(val ngrammar: NGrammar,
               val grammrRules: List[(String, (UnrollChoices, scala.List[Symbols.Symbol]))],
               val symbolsMap: Map[MetaGrammar3Ast.Symbol, Symbols.Symbol],
               val grammarInfo: GrammarInfoCollector,
               val bottomUpTypeInferer: BottomUpTypeInferer) {

    // 더이상 얻을 수 있는 정보가 없어서 내용이 변한게 없으면 true, 그렇지 않으면 false
    private def traverse(): Boolean = {
        true
    }

    // TODO 모든 TypeDef collect, graph에 추가.
    // - 만약 같은 클래스, 같은 위치에 다른 이름의 파라메터가 두개 정의되면 오류. 타입이 두개 정의되는건 뒤에서 확인

    def validate(): Unit = {
        // TODO typeGraph에 싸이클이 없는지 확인
        // TODO paramConcreteTypesMap 계산한 후, 받을 수 없는 값이 전달되는 경우가 없는지 확인
        // TODO FuncCallOrOpNode 노드들에 들어오는 타입, 나가는 타입 확인.
        // TODO FuncCallOrOpNode 노드들에 리턴 타입 inference해서 추가
        // TODO 파라메터 갯수가 맞는지 확인
    }
}

abstract sealed class GrammarValidationException extends Exception

case class CyclicType(msg: String) extends GrammarValidationException
