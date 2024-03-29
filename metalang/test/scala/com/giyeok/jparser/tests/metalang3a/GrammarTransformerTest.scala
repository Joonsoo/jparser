package com.giyeok.jparser.tests.metalang3a

import com.giyeok.jparser.examples.metalang3.MetaLang3ExamplesCatalog
import com.giyeok.jparser.metalang3.MetaLanguage3.IllegalGrammar
import com.giyeok.jparser.metalang3.ast.MetaLang3Ast
import com.giyeok.jparser.metalang3.codegen.{KotlinOptCodeGen, ScalaCodeGen}
import com.giyeok.jparser.metalang3.{ErrorCollector, GrammarTransformer, MetaLang3Parser, MetaLanguage3, ValuefyExprSimulator}
import com.giyeok.jparser.nparser.ParseTreeConstructor2
import com.giyeok.jparser.nparser.ParseTreeConstructor2.Kernels
import com.giyeok.jparser.{Inputs, NGrammar, ParseForestFunc}
import org.scalatest.PrivateMethodTester
import org.scalatest.flatspec.AnyFlatSpec

class GrammarTransformerTest extends AnyFlatSpec with PrivateMethodTester {
  def parseGrammar(grammar: String): MetaLang3Ast.Grammar = {
    val parser = MetaLang3Parser.parser
    val inputs = Inputs.fromString(grammar)
    val parseResult = parser.parseOrThrow(inputs)
    val history = parser.kernelsHistory(parseResult)
    val reconstructor = new ParseTreeConstructor2(ParseForestFunc)(parser.parserData.grammar)(inputs, history.map(Kernels))
    reconstructor.reconstruct() match {
      case Some(forest) if forest.trees.size == 1 =>
        new MetaLang3Ast().matchStart(forest.trees.head)
      case None =>
        throw IllegalGrammar("??")
    }
  }

  "bibix-small" should "work" in {
    val ast = parseGrammar(
      """BuildScript = Def
        |
        |Def: Def = TargetDef
        |
        |TargetDef = SimpleName WS '=' WS Expr {TargetDef(name=$0)}
        |
        |Expr: Expr = "good"
        |
        |SimpleName = ('a-zA-z' {str($0)})&Tk {$0}
        |
        |Keyword = "true" | "false" | "none" | "this"
        |
        |Tk = <'a-z'+>
        |
        |WS = ' '*
        |""".stripMargin)
    val errors = new ErrorCollector()

    val transformer = new GrammarTransformer(ast, errors)
    val grammar = transformer.grammar("Grammar")
    val ngrammar = NGrammar.fromGrammar(grammar)

    val simul = new ValuefyExprSimulator(ngrammar, transformer.startNonterminalName(), transformer.nonterminalValuefyExprs, Map())
    val value = simul.valuefy("a = good")
    println(value)
  }

  "array grammar" should "work" in {
    val ast = parseGrammar(
      """E:Expr = 'a' {Literal(value=$0)} | A
        |A = '[' WS E (WS ',' WS E)* WS ']' {Arr(elems=[$2]+$3)}
        |WS = ' '*""".stripMargin)
    val errors = new ErrorCollector()

    val transformer = new GrammarTransformer(ast, errors)
    val grammar = transformer.grammar("Grammar")
    val ngrammar = NGrammar.fromGrammar(grammar)

    val simul = new ValuefyExprSimulator(ngrammar, transformer.startNonterminalName(), transformer.nonterminalValuefyExprs, Map())
    val value = simul.valuefy("[a,a,a]")
    println(value)
  }

  def test(grammarText: String, examples: Map[String, String]) = {
    val ast = parseGrammar(grammarText)

    val errors = new ErrorCollector()

    val analysis = MetaLanguage3.analyzeGrammar(grammarText)

    val simul = new ValuefyExprSimulator(
      analysis.ngrammar,
      analysis.startNonterminalName,
      analysis.nonterminalValuefyExprs,
      analysis.shortenedEnumTypesMap)
    examples.foreach { example =>
      val value = simul.valuefy(example._1)
      value match {
        case Right(value) =>
          println(value)
          throw new AssertionError(s"?? $value")
        case Left(value) =>
          println(value.prettyPrint())
          assert(value.prettyPrint() == example._2)
      }
    }

    val scalaCodeGen = new ScalaCodeGen(analysis)
    scalaCodeGen.generateParser("TestParser")
    val kotlinCodeGen = new KotlinOptCodeGen(analysis)
    kotlinCodeGen.generate("TestParser")
  }

  "simple grammar" should "work" in {
    test(
      "RefIdx = <'0' {str($0)} | '1-9' '0-9'* {str($0, $1)}>",
      Map("0" -> "\"0\""))

    test(
      """A = 'a' B&T 'z' {str($0, $1, $2)}
        |B = 'b-z'+
        |T = 't'+ | "to"
        |""".stripMargin,
      Map(
        "attttz" -> "\"attttz\"",
        "atoz" -> "\"atoz\""))

    test(
      """A = ('a' 'b' {"ab"})+
        |""".stripMargin,
      Map("abab" -> "[\"ab\",\"ab\"]")
    )
  }

  "bind expr" should "work" in {
    assertThrows[IllegalStateException] {
      test(
        """A = ('a' 'b' | 'c')+ {$0$0}""",
        Map("ab" -> "")
      )
    }

    test(
      """A = ('c-e' 'f')+ {$0$0}""",
      Map(
        "cf" -> "['c']",
        "cfdf" -> "['c','d']")
    )

    test(
      """A = ('k-m' 'n')* {$0$0}""",
      Map(
        "kn" -> "['k']",
        "knln" -> "['k','l']",
        "" -> "[]")
    )

    test(
      """A = ('g-i')? {$0$0}""",
      Map(
        "g" -> "'g'",
        "" -> "null")
    )

    assertThrows[IllegalStateException] {
      test(
        """A = 'g-i'? {$0$0}""",
        Map()
      )
    }

    test(
      """A = ('g-i' 'j')? {$0$0}""",
      Map(
        "gj" -> "'g'",
        "" -> "null")
    )
  }

  "error grammar" should "work" in {
    test(
      "RefIdx = ('0' {str($0)} | '1-9' '0-9'* {str($0, $1)})",
      Map(
        "1" -> "\"1\"")
    )
  }

  "error grammar 2" should "work" in {
    test(
      """ArrayExpr = '[' WS (PExpr (WS ',' WS PExpr)* WS)? ']' {$2{[$0] + $1} ?: []}
        |PExpr = 'a-z'
        |WS = ' '*
        |""".stripMargin,
      Map("[a, b, c]" -> "['a','b','c']")
    )
  }

  "error 3" should "be fixed" in {
    test(
      "RefIdx = ('0' {str($0)} | '1-9' '0-9'* {str($0, $1)})",
      Map("0" -> "\"0\""))
  }

  "error 4" should "be fixed" in {
    test(
      """A = ('c-e' 'f')+ {$0$0}""",
      Map(
        "cf" -> "['c']",
        "cfdf" -> "['c','d']")
    )
  }

  "metalang3 grammar" should "work" in {
    test(
      MetaLang3ExamplesCatalog.INSTANCE.getMetalang3.getGrammarText,
      Map(
        "Abc = 'a'+" ->
          "Grammar([Rule(LHS(Nonterminal(NonterminalName(\"Abc\")),null),[Sequence([RepeatFromOne(CharAsIs('a'))])])])",
        "Xyz = ('a' 'b')+ {$0$0}" ->
          "Grammar([Rule(LHS(Nonterminal(NonterminalName(\"Xyz\")),null),[Sequence([RepeatFromOne(InPlaceChoices([Sequence([CharAsIs('a'),CharAsIs('b')])])),ProcessorBlock(BindExpr(ValRef(\"0\",null),ValRef(\"0\",null)))])])])",
        "Def = 'h' 'e' 'l' 'l' 'o' {$1}" ->
          "Grammar([Rule(LHS(Nonterminal(NonterminalName(\"Def\")),null),[Sequence([CharAsIs('h'),CharAsIs('e'),CharAsIs('l'),CharAsIs('l'),CharAsIs('o'),ProcessorBlock(ValRef(\"1\",null))])])])",
        "Def = 'h' 'e' 'l' 'l' 'o' {[$1, $3]}" ->
          "Grammar([Rule(LHS(Nonterminal(NonterminalName(\"Def\")),null),[Sequence([CharAsIs('h'),CharAsIs('e'),CharAsIs('l'),CharAsIs('l'),CharAsIs('o'),ProcessorBlock(ArrayExpr([ValRef(\"1\",null),ValRef(\"3\",null)]))])])])"
      )
    )
  }

  "join grammar" should "work" in {
    test(
      """S = (A Q)&(B C D) {$0}
        |A = 'a-z'+
        |Q = 'q'+
        |B = 'b'+
        |C = 'c'+
        |D = 'd-z'+
        |""".stripMargin,
      Map("bcdq" -> "['q']")
    )

    test(
      """S = (A Q $0)&(B C D) {$0}
        |A = 'a-z'+
        |Q = 'q'+
        |B = 'b'+
        |C = 'c'+
        |D = 'd-z'+
        |""".stripMargin,
      Map("bcdq" -> "['b','c','d']")
    )

    test(
      """S = (A Q)&(B C D) {$>0}
        |A = 'a-z'+
        |Q = 'q'+
        |B = 'b'+
        |C = 'c'+
        |D = 'd-z'+
        |""".stripMargin,
      Map("bcdq" -> "['d','q']")
    )

    test(
      """S = (A Q)&(B C D $1) {$>0}
        |A = 'a-z'+
        |Q = 'q'+
        |B = 'b'+
        |C = 'c'+
        |D = 'd-z'+
        |""".stripMargin,
      Map("bcdq" -> "['c']")
    )
  }

  "join bind grammar" should "work" in {
    test(
      """S = (A Q)&(B C D) {$0$0}
        |A = 'a-z'+
        |Q = 'q'+
        |B = 'b'+
        |C = 'c'+
        |D = 'd-z'+
        |""".stripMargin,
      Map("bcdq" -> "['b','c','d']")
    )

    test(
      """S = (A Q)&(B C D) {$>0$0}
        |A = 'a-z'+
        |Q = 'q'+
        |B = 'b'+
        |C = 'c'+
        |D = 'd-z'+
        |""".stripMargin,
      Map("bcdq" -> "['b']")
    )
  }

  "multi join grammar" should "work" in {
    // A&B&C == A&(B&C)
    test(
      """S = A&B&C&D {$0}
        |A = 'a-z'+ {"this is A"}
        |B = 'b-z'+ {"this is B"}
        |C = 'c-z'+ {"this is C"}
        |D = 'd-z'+ {"this is D"}
        |""".stripMargin,
      Map("ddd" -> "\"this is A\"")
    )

    test(
      """S = A&(B&(C&D)) {$>0}
        |A = 'a-z'+ {"this is A"}
        |B = 'b-z'+ {"this is B"}
        |C = 'c-z'+ {"this is C"}
        |D = 'd-z'+ {"this is D"}
        |""".stripMargin,
      Map("ddd" -> "\"this is B\"")
    )

    test(
      """S = A&B&C&D {$>>0}
        |A = 'a-z'+ {"this is A"}
        |B = 'b-z'+ {"this is B"}
        |C = 'c-z'+ {"this is C"}
        |D = 'd-z'+ {"this is D"}
        |""".stripMargin,
      Map("ddd" -> "\"this is C\"")
    )

    test(
      """S = A&B&C&D {$>>>0}
        |A = 'a-z'+ {"this is A"}
        |B = 'b-z'+ {"this is B"}
        |C = 'c-z'+ {"this is C"}
        |D = 'd-z'+ {"this is D"}
        |""".stripMargin,
      Map("ddd" -> "\"this is D\"")
    )

    test(
      """S = A&(B&C {"this is B&C"})&D {$>0}
        |A = 'a-z'+ {"this is A"}
        |B = 'b-z'+ {"this is B"}
        |C = 'c-z'+ {"this is C"}
        |D = 'd-z'+ {"this is D"}
        |""".stripMargin,
      Map("ddd" -> "\"this is B&C\"")
    )

    test(
      """S = (A&B)&(C&D) {$>0{$>0}}
        |A = 'a-z'+ {"this is A"}
        |B = 'b-z'+ {"this is B"}
        |C = 'c-z'+ {"this is C"}
        |D = 'd-z'+ {"this is D"}
        |""".stripMargin,
      Map("dddd" -> "\"this is D\"")
    )
  }

  "error 5" should "be fixed" in {
    test(
      """S = A&B&C {$>>0}
        |A = 'a-z'+ {"this is A"}
        |B = 'b-z'+ {"this is B"}
        |C = 'c-z'+ {"this is C"}
        |""".stripMargin,
      Map("ddd" -> "\"this is C\"")
    )
  }

  "except grammar" should "work" in {
    // A-B-C == (A-B)-C
    test(
      """S = A-B-C
        |A = 'a-z'+
        |B = 'b-q'+
        |C = 'b-d'+
        |""".stripMargin,
      Map("aaa" -> "['a','a','a']")
    )
  }

  "repeat grammar" should "work" in {
    test(
      """S = A+ B {str($0, $1)}
        |A = 'a'
        |B = 'b'+
        |""".stripMargin,
      Map("aabb" -> "\"aabb\"")
    )

    test(
      """S = A* B {str($0, $1)}
        |A = 'a'
        |B = 'b'*
        |""".stripMargin,
      Map(
        "" -> "\"\"",
        "aabb" -> "\"aabb\"")
    )
  }

  "subset of autodb grammar" should "work" in {
    test(
      """EntityViewFieldSelectExpr: EntityViewFieldSelectExpr = EntityViewFieldSelectTerm
        |    | EntityViewFieldSelectTerm WS <"==" {%EQ} | "!=" {%NE}> WS EntityViewFieldSelectExpr
        |      {BinOp(op:%BinOpType=$2, lhs=$0, rhs=$4)}
        |EntityViewFieldSelectTerm: EntityViewFieldSelectTerm = "null"&Tk {NullValue()}
        |    | FieldName ((WS '?')? WS '.' WS DataFieldName {DataTypeValueSelectField(nullable=ispresent($0), fieldName=$4)})* {DataTypeValueSelect(field=$0, selectPath=$1)}
        |    | "@primaryKey" {PrimaryKeySelect()}
        |    | '(' WS EntityViewFieldSelectExpr WS ')' {Paren(expr=$2)}
        |PreDefinedEntityView = "view"&Tk WS FieldRef WS '(' WS PreDefinedEntityViewField (WS ',' WS PreDefinedEntityViewField)* (WS ',')? WS ')'
        |    {PreDefinedEntityView(definition=$2, fields=[$6]+$7)}
        |PreDefinedEntityViewField = FieldName {PreDefinedEntityViewField(originalFieldName=$0, thisEntityFieldName=$0)}
        |    | FieldName WS '=' WS FieldName {PreDefinedEntityViewField(originalFieldName=$0, thisEntityFieldName=$4)}
        |
        |
        |WS = ' '*
        |Tk = <'a-zA-Z0-9_'+> | <'+\-*/!&|=<>'+>
        |
        |FieldName = Name
        |DataTypeName = Name
        |DataFieldName = Name
        |
        |FieldRef = <Name (WS '.' WS Name)* {FieldRef(names=[$0] + $1)}>
        |
        |Name = <'a-zA-Z_' 'a-zA-Z0-9_'* {str($0, $1)}>-Keywords
        |
        |Keywords = "Int" | "Long" | "String" | "Timestamp" | "Duration" | "URI" | "Boolean"
        |  | "Empty" | "Ref" | "List" | "entity"
        |  | "autoincrement" | "sparsegenLong" | "view" | "null"
        |  | "==" | "!="
        |  | "query" | "rows" | "update"
        |  | "true" | "false"
        |""".stripMargin,
      Map(
        "verifiedIdentity != null" -> "BinOp(%BinOpType.NE,DataTypeValueSelect(\"verifiedIdentity\",[]),NullValue())"
      )
    )
  }
}
