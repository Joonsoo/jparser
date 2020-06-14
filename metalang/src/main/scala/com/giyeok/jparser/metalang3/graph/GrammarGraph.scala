package com.giyeok.jparser.metalang3.graph

import com.giyeok.jparser.metalang3.types.{ConcreteType, TypeFunc, TypeHierarchy}

// GrammarGraph는 Valueify를 한 문법 정의를 받아서
// - TypeFunc를 ConcreteType으로 바꿀 수 있는 정보 수집
// - 명시적으로 정의된 타입이 받을 수 없는 expression 타입을 주는 caller나 construct 찾아서 오류 발생
// - class hierarchy 계산
// 등의 목적을 위해 그래프를 구축하고 필요한 정보를 추출해준다.
// 이 그래프는 우선 문법 정의에 명시된 내용만 기록/관리하고, 여기서 inference 가능한 정보는 따로 관리한다.

// 그래프의 노드는
// - SymbolNode
// - PExprNode
// - TypeNode(ConcreteType 정보, 사용자가 명시적으로 inference 가능한)
// - FuncCallOrOpNode(ispresent, isempty같은 기본 함수 및 !, +, && 등의 연산자)
// - ParamNode
// 엣지는
// - SymbolNode results TypeNode
// - PExprNode results TypeNode
// - TypeNode (extends의 반대) TypeNode
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
class GrammarGraph() {
    def validate(): Unit = {
        ???
    }

    def concreteTypeOf(typeFunc: TypeFunc): ConcreteType = ???

    def typeHierarchy(): TypeHierarchy = ???
}

object GrammarGraph {

    case class GrammarValidationException(msg: String) extends Exception(msg)

}
