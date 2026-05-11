package com.giyeok.jparser.mgroup2

import com.giyeok.jparser.{Inputs, NGrammar, ParseForest, ParseForestFunc}
import com.giyeok.jparser.metalang3.MetaLanguage3
import com.giyeok.jparser.milestone2.{AcceptConditionTemplate, AndTemplate, LongestTemplate, OnlyIfTemplate, OrTemplate, TasksSummary2, UnlessTemplate}
import com.giyeok.jparser.nparser.{NaiveParser, ParseTreeConstructor2}
import com.giyeok.jparser.nparser.ParseTreeConstructor2.Kernels
import com.giyeok.jparser.nparser2.NaiveParser2
import org.scalatest.flatspec.AnyFlatSpec

class Mgroup2Test extends AnyFlatSpec {
  "mulang grammar" should "produce the same parse forest with naive and mgroup2 parsers" in {
    testMulang()
  }

  "left recursion + NJoin with NLongest body" should "parse 1 || 2" in {
    val grammar = """
      |Grammar = E
      |E = N | E WS "||"&OpTk WS N
      |N = '0-9'+
      |OpTk = <Op>
      |Op = ('+' | '-' | "||")+
      |WS = ' '*
      |""".stripMargin
    assertSameParseResult(grammar, "1 || 2")
  }

  "mulang try_let.mu" should "dump mgroup2 state around fail point of mgroup3 (idx 840-841)" in {
    val cdgFile = new java.io.File("../mulang/grammar/mulang.cdg")
    val srcFile = new java.io.File("../mulang/examples/try_let.mu")
    if (!cdgFile.exists() || !srcFile.exists()) {
      cancel("mulang.cdg or try_let.mu not found")
    }
    val grammarText = scala.io.Source.fromFile(cdgFile).mkString
    val grammar = MetaLanguage3.analyzeGrammar(grammarText, "CompileUnit").ngrammar
    val parserData = new MilestoneGroupParserGen(grammar).parserData()
    val parser = new MilestoneGroupParser(parserData)
    val src = scala.io.Source.fromFile(srcFile).mkString
    val inputs = Inputs.fromString(src)

    // step-by-step parse
    var ctx = parser.initialCtx
    var lastIdx = -1
    try {
      inputs.zipWithIndex.foreach { case (input, idx) =>
        parser.parseStep(ctx, input) match {
          case Right(next) =>
            ctx = next
            lastIdx = idx
            // dump idx 838~842 의 state.
            if (idx >= 838 && idx <= 842) {
              val ch = input.toShortString
              println(s"=== mgroup2 after step idx=$idx ('$ch') gen=${ctx.gen} paths=${ctx.paths.size} ===")
              ctx.paths.zipWithIndex.foreach { case (path, i) =>
                println(s"  p[$i] first=(${path.first.symbolId},${path.first.pointer},${path.first.gen}) " +
                  s"tip=mg${path.tip.groupId}@${path.tip.gen} " +
                  s"cond=${path.acceptCondition.toString.take(400)}")
                path.path.zipWithIndex.foreach { case (m, j) =>
                  println(s"    path[$j] (${m.symbolId},${m.pointer},${m.gen})")
                }
              }
              // 같은 step 의 genActions 의 progressed* 도 dump.
              val ga = ctx.history.head.genActions
              if (ga.progressedRootMilestones.nonEmpty) {
                println(s"  -- progressedRootMilestones (${ga.progressedRootMilestones.size}):")
                ga.progressedRootMilestones.foreach { case (m, cond) =>
                  println(s"    (${m.symbolId},${m.pointer},${m.gen}) => ${cond.toString.take(300)}")
                }
              }
              if (ga.progressedRootMgroups.nonEmpty) {
                println(s"  -- progressedRootMgroups (${ga.progressedRootMgroups.size}):")
                ga.progressedRootMgroups.foreach { case (mg, cond) =>
                  println(s"    mg${mg.groupId}@${mg.gen} => ${cond.toString.take(300)}")
                }
              }
            }
          case Left(err) =>
            println(s"=== mgroup2 FAILED at idx=$idx: $err ===")
            throw new RuntimeException(s"mgroup2 failed at idx=$idx")
        }
      }
      println(s"mgroup2 parsed try_let.mu, ${src.length} chars, ${ctx.paths.size} final paths")
    } catch {
      case e: Exception =>
        println(s"caught: $e at lastIdx=$lastIdx")
        throw e
    }
  }

  "mulang ccgen.mu" should "be parsed by mgroup2 (path explosion check)" in {
    val cdgFile = new java.io.File("../mulang/grammar/mulang.cdg")
    val ccgenFile = new java.io.File("../mulang/examples/ccgen.mu")
    if (cdgFile.exists() && ccgenFile.exists()) {
      val grammarText = scala.io.Source.fromFile(cdgFile).mkString
      val grammar = MetaLanguage3.analyzeGrammar(grammarText, "CompileUnit").ngrammar
      val parserData = new MilestoneGroupParserGen(grammar).parserData()
      val parser = new MilestoneGroupParser(parserData)
      val src = scala.io.Source.fromFile(ccgenFile).mkString
      val start = System.currentTimeMillis()
      val ctx = parser.parse(Inputs.fromString(src))
      val end = System.currentTimeMillis()
      ctx match {
        case Right(_) => println(s"ccgen.mu parsed by mgroup2 in ${end - start} ms, ${src.length} chars")
        case Left(e) => fail(s"mgroup2 parse failed: $e")
      }
    } else {
      cancel("mulang.cdg or ccgen.mu not found")
    }
  }

  "nullable condition grammars" should "generate parser data for longest except and join" in {
    val nullableLongest = parserDataFrom(
      """
        |S = 'a' <A> 'b'
        |A = # | 'z'
        |""".stripMargin)
    assert(hasTemplate(nullableLongest) {
      case LongestTemplate(_, beginFromNextGen) => beginFromNextGen
      case _ => false
    })

    val nullableExcept = parserDataFrom(
      """
        |S = 'a' (A-B) 'b'
        |A = #
        |B = 'z'
        |""".stripMargin)
    assert(hasTemplate(nullableExcept) {
      case UnlessTemplate(_, fromNextGen) => fromNextGen
      case _ => false
    })

    val nullableJoin = parserDataFrom(
      """
        |S = 'a' (A&B) 'b'
        |A = #
        |B = # | 'z'
        |""".stripMargin)
    assert(hasTemplate(nullableJoin) {
      case OnlyIfTemplate(_, fromNextGen) => fromNextGen
      case _ => false
    })
  }

  private def parseForestWithNaiveParser(grammar: NGrammar, inputs: List[Inputs.Input]): ParseForest = {
    val parser = new NaiveParser2(grammar)
    val ctx = parser.parse(inputs) match {
      case Right(ctx) => ctx
      case Left(error) => fail(s"Naive parser failed: $error")
    }
    parser.parseTreeReconstructor2(ParseForestFunc, ctx).reconstruct()
      .getOrElse(fail("Naive parser result could not be reconstructed"))
  }

  private def parseForestWithMgroup2Parser(parser: MilestoneGroupParser, inputs: List[Inputs.Input]): ParseForest = {
    val ctx = parser.parse(inputs) match {
      case Right(ctx) => ctx
      case Left(error) => fail(s"Mgroup2 parser failed: $error")
    }
    new ParseTreeConstructor2(ParseForestFunc)(parser.parserData.grammar)(inputs, parser.kernelsHistory(ctx).map(Kernels))
      .reconstruct()
      .getOrElse(fail("Mgroup2 parser result could not be reconstructed"))
  }

  private def assertSameParseResult(grammar: NGrammar, parser: MilestoneGroupParser, source: String): Unit = {
    val inputs = Inputs.fromString(source)
    val mgroup2Forest = parseForestWithMgroup2Parser(parser, inputs)
    val naiveForest = parseForestWithNaiveParser(grammar, inputs)

    assert(
      naiveForest.trees == mgroup2Forest.trees,
      s"""Naive parser and mgroup2 parser produced different parse forests.
         |naive tree count: ${naiveForest.trees.size}
         |mgroup2 tree count: ${mgroup2Forest.trees.size}
         |naive only: ${(naiveForest.trees.toSet -- mgroup2Forest.trees.toSet).mkString("\n")}
         |mgroup2 only: ${(mgroup2Forest.trees.toSet -- naiveForest.trees.toSet).mkString("\n")}
         |""".stripMargin)
  }

  private def assertSameParseResult(grammarText: String, source: String): Unit = {
    val parserData = parserDataFrom(grammarText)
    val parser = new MilestoneGroupParser(parserData)

    assertSameParseResult(parserData.grammar, parser, source)
  }

  private def parserDataFrom(grammarText: String): MilestoneGroupParserData = {
    val grammar = MetaLanguage3.analyzeGrammar(grammarText, "Testing").ngrammar
    new MilestoneGroupParserGen(grammar).parserData()
  }

  private def hasTemplate(parserData: MilestoneGroupParserData)(pred: AcceptConditionTemplate => Boolean): Boolean =
    templatesOf(parserData).exists(template => hasTemplate(template)(pred))

  private def hasTemplate(template: AcceptConditionTemplate)(pred: AcceptConditionTemplate => Boolean): Boolean =
    pred(template) || (template match {
      case AndTemplate(conditions) => conditions.exists(hasTemplate(_)(pred))
      case OrTemplate(conditions) => conditions.exists(hasTemplate(_)(pred))
      case _ => false
    })

  private def templatesOf(tasksSummary: TasksSummary2): Iterable[AcceptConditionTemplate] =
    tasksSummary.addedKernels.keys

  private def templatesOf(termAction: TermAction): Iterable[AcceptConditionTemplate] =
    termAction.appendingMilestoneGroups.map(_._2.acceptCondition) ++
      termAction.startNodeProgress.map(_._2) ++
      templatesOf(termAction.tasksSummary) ++
      termAction.pendedAcceptConditionKernels.values.flatMap { case (appendings, condition) =>
        appendings.map(_.acceptCondition) ++ condition
      }

  private def templatesOf(edgeAction: EdgeAction): Iterable[AcceptConditionTemplate] =
    edgeAction.appendingMilestoneGroups.map(_.acceptCondition) ++
      edgeAction.startNodeProgress ++
      templatesOf(edgeAction.tasksSummary)

  private def templatesOf(parserData: MilestoneGroupParserData): Iterable[AcceptConditionTemplate] =
    templatesOf(parserData.initialTasksSummary) ++
      parserData.termActions.values.flatten.flatMap { case (_, action) => templatesOf(action) } ++
      parserData.tipEdgeProgressActions.values.flatMap(templatesOf) ++
      parserData.midEdgeProgressActions.values.flatMap(templatesOf)

  def testMulang(): Unit = {
    val grammar =
      """
        |FuncDef = "def"&Tk WS Word WS '(' WS ')' WS Block
        |  {FuncDef(name=$2, body=$8)}
        |
        |Block = '{' (WS Stmts)? WS '}' {Block(stmts=$1 ?: [])}
        |Stmts = Stmt (StmtDelim Stmt)* {[$0] + $1}
        |
        |
        |// Expr은 자체적으로 annotation 넣기
        |Stmt: Stmt = Expr
        |  | LetStmt
        |  | Assign
        |
        |LetStmt = "let"&Tk WS Word WS '=' WS Expr
        |    {LetStmt(pattern=$2, value=$6)}
        |
        |Assign = AssignableExpr WS AssignOp WS Expr {Assign(op=$2, lhs=$0, rhs=$4)}
        |
        |AssignableExpr: AssignableExpr
        |  = Name
        |  | '*' WS AssignableExpr {DerefAssign(ptr=$2)}
        |AssignOp: %AssignOps = (
        |  '=' {%ASSIGN} |
        |  "+=" {%ADD} |
        |  "-=" {%SUB} |
        |  "*=" {%MUL} |
        |  "/=" {%DIV} |
        |  "%=" {%REM})&OpTk
        |
        |Expr = AddExpr
        |
        |AddExpr: AddExprOr = <MulExpr
        |  | MulExpr (WS ("+" {%ADD} | "-" {%SUB})&OpTk WS MulExpr {AddChain(op: %AddOps=$1, rhs=$3)})+
        |    {AddExpr(lhs=$0, chain=$1)}>
        |
        |MulExpr: MulExprOr = PrefixExpr
        |  | MulExpr WS "*"&OpTk WS PrefixExpr !(WS AssignOp)
        |    {MulExpr(op: %MulOps=%MUL, lhs=$0, rhs=$4)}
        |  | MulExpr WS ("/" {%DIV} | "%" {%REM})&OpTk WS PrefixExpr
        |    {MulExpr(op: %MulOps=$2, lhs=$0, rhs=$4)}
        |
        |PrefixExpr: PrefixExprOr = PrimaryExpr
        |  | ('+' {%PLUS} | '-' {%MINUS} | '!' {%NOT})&OpTk WS PrimaryExpr
        |    {PrefixExpr(op: %PrefixOps=$0, operand=$2)}
        |
        |PrimaryExpr: PrimaryExpr
        |  = Literal
        |  | Name
        |
        |Name = Word {NameExpr(name=$0)}
        |
        |Literal: Literal = BoolLiteral | IntLiteral | FloatLiteral
        |
        |BoolLiteral = ("true" {BoolLiteral(value=true)} | "false" {BoolLiteral(false)})&Tk
        |IntLiteral = Digits IntTypeSuffix? {IntLiteral(digits=$0, typeSuffix=$1)}
        |IntTypeSuffix = ('u' {true} | 'i' {false}) '1-9' '0-9'* {IntTypeSuffix(isUnsigned=$0, bitLength=str($1, $2))}
        |FloatLiteral
        |  = Digits '.' '0-9'+ FloatExponent? FloatTypeSuffix?
        |    {FloatLiteral(intPart=$0, fracPart=$2, exp=$3, typeSuffix=$4)}
        |  | Digits FloatExponent FloatTypeSuffix?
        |    {FloatLiteral($0, null, $1, $2)}
        |FloatExponent = 'eE' ('+' {false} | '-' {true})? Digits {FloatExponent(isMinus=$1 ?: false, exponent=$2)}
        |FloatTypeSuffix = 'f' '1-9' '0-9'* {FloatTypeSuffix(bitLength=str($1, $2))}
        |
        |Digits = '0' {"0"} | '1-9' '0-9'* {str($0, $1)}
        |
        |Word = <Word_>
        |Word_
        |  = 'a-zA-Z' '0-9a-zA-Z_'* {str($0, $1)}
        |  | '_' '0-9a-zA-Z_'+ {str($0, $1)}
        |Tk = <Word | "_">
        |OpTk = <"+" | "-" | "*" | "/" | "%" | "!" | "=" | "+=" | "-=" | "*=" | "/=" | "%=">
        |
        |WS = (' \n\r\t' | Comment)* {""}
        |WS_NO_NL = (' \t' | BlockComment)* {""}
        |WS_01_NL = WS
        |// TODO WS_01_NL = WS_NO_NL 이거나 개행 한 번
        |// WS_01_NL = WS_NO_NL (('\n\r' | LineComment) WS_NO_NL)?
        |Comment = LineComment | BlockComment
        |LineComment = "//" (.-'\n')* ('\n' | EOF)
        |EOF = !.
        |BlockComment = "/*" ((. !"*/")* .)? "*/"
        |
        |StmtDelim
        |  = WS_NO_NL ('\n' | LineComment) WS
        |  | WS ';' WS
        |""".stripMargin
    val g = MetaLanguage3.analyzeGrammar(grammar, "Testing")

    val parserData = new MilestoneGroupParserGen(g.ngrammar)
      .parserData()
    val parser = new MilestoneGroupParser(parserData)

    val source2 =
      """def x() {
        |  let p = abc
        |  *p = 42
        |}
        |""".stripMargin.trim

    println(source2)
    assertSameParseResult(g.ngrammar, parser, source2)

    val source1 =
      """def x() {
        |  let x = abc
        |    + bcd
        |}
        |""".stripMargin.trim

    println(source1)
    assertSameParseResult(g.ngrammar, parser, source1)
  }
}
