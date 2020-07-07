package com.giyeok.jparser.metalang3a.codegen

import com.giyeok.jparser.metalang3a.MetaLanguage3.ProcessedGrammar
import com.giyeok.jparser.metalang3a.codegen.ScalaCodeGen.{CodeBlob, Options}
import com.giyeok.jparser.metalang3a.{Type, ValuefyExpr}

object ScalaCodeGen {

    /**
     *
     * @param useNull              (미구현) true이면 Option 대신 그냥 값+null을 사용한다.
     * @param looseSuperType       TODO 기본 false
     * @param assertBindTypes      Unbind할 때 unbind된 심볼의 타입을 체크한다. 기본 true
     * @param symbolComments       human readable한 심볼 설명 코멘트를 추가한다. 기본 true
     * @param astNodesInAllClasses 모든 클래스에 astNode를 기본으로 포함시킨다. 기본 true
     */
    case class Options(useNull: Boolean = false,
                       looseSuperType: Boolean = false,
                       assertBindTypes: Boolean = true,
                       symbolComments: Boolean = true,
                       astNodesInAllClasses: Boolean = true)

    case class CodeBlob(code: String, outputName: String, outputType: Type, requirements: Set[String]) {
        def indent(width: Int = 2): CodeBlob =
            copy(code.linesIterator.toList.map(line => (" " * width) + line).mkString("\n"))
    }

}

class ScalaCodeGen(val analysis: ProcessedGrammar, val options: Options = Options()) {
    // TODO
    def valuefyExprToCode(valuefyExpr: ValuefyExpr): CodeBlob = ???

    def nonterminalMatchFunc(nonterminal: String): CodeBlob = ???
}
