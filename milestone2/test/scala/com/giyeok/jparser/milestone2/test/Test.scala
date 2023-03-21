package com.giyeok.jparser.milestone2.test

import com.giyeok.jparser.metalang3.MetaLanguage3
import com.giyeok.jparser.milestone2.{KernelTemplate, MilestoneParserGen}
import org.scalatest.flatspec.AnyFlatSpec

class Test extends AnyFlatSpec {
  it should "work" in {
    val grammar =
      """
        |EntityViewFieldSelectExpr: EntityViewFieldSelectExpr = EntityViewFieldSelectTerm
        |    | EntityViewFieldSelectTerm WS <"==" {%EQ} | "!=" {%NE}> WS EntityViewFieldSelectExpr
        |      {BinOp(op:%BinOpType=$2, lhs=$0, rhs=$4)}
        |
        |EntityViewFieldSelectTerm: EntityViewFieldSelectTerm = "null"&Tk {NullValue()}
        |    | FieldName ((WS '?')? WS '.' WS DataFieldName {DataTypeValueSelectField(nullable=ispresent($0), fieldName=$4)})* {DataTypeValueSelect(field=$0, selectPath=$1)}
        |
        |FieldName = Name
        |DataFieldName = Name
        |Name = <'a-zA-Z_' 'a-zA-Z0-9_'* {str($0, $1)}>-Keywords
        |Keywords = "null"
        |
        |Tk = <'a-zA-Z0-9_'+> | <'+\-*/!&|=<>'+>
        |WS = (' \n\r\t' | LineComment)*
        |LineComment = "//" (.-'\n')* (EOF | '\n')
        |EOF = !.
        |""".stripMargin

    val analysis = MetaLanguage3.analyzeGrammar(grammar)

    val parserGen = new MilestoneParserGen(analysis.ngrammar)
    val edgeAction = parserGen.edgeProgressActionBetween(KernelTemplate(1, 0), KernelTemplate(38, 1))

    println(edgeAction)
  }
}
