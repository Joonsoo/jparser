package com.giyeok.jparser.parsergen.nocond.codegen

import com.giyeok.jparser.parsergen.nocond.SimpleParser

// SimpleParser를 생성하면서 만들어진 노드들로 SimpleParser처럼 스택(혹은 리스트)으로 파싱하는 것이 아니라,
// DAG로 관리하면서 파싱하면 실제 그래프 형태와 동일하게 파싱 가능함. 이렇게 파싱하는걸 GeneralParser라고 하자.
// GeneralParser는 SimpleParser의 노드들을 그대로 사용하므로 SimpleParser를 받아서 GeneralParser를 생성함.
class GeneralParserJavaGen(val parser: SimpleParser) {
}
