package com.giyeok.jparser.gramgram.meta2

import com.giyeok.jparser.gramgram.meta2.TypeDependenceGraph.SymbolNode
import com.giyeok.jparser.nparser.{NaiveParser, ParseTreeConstructor}
import com.giyeok.jparser.{ParseForestFunc, Symbols}

object MetaGrammar2 {
    val parser = new NaiveParser(GrammarDef.oldGrammar)

    def grammarSpecToAST(grammar: String): Option[AST.Grammar] = {
        val result = parser.parse(grammar)

        result match {
            case Left(ctx) =>
                val reconstructor = new ParseTreeConstructor(ParseForestFunc)(GrammarDef.oldGrammar)(ctx.inputs, ctx.history, ctx.conditionFinal)
                reconstructor.reconstruct() match {
                    case Some(parseForest) =>
                        assert(parseForest.trees.size == 1)
                        val tree = parseForest.trees.head

                        Some(ASTifier.matchGrammar(tree))
                    case None =>
                        println("Incomplete input")
                        None
                }
            case Right(error) =>
                println(error)
                None
        }
    }

    def main(args: Array[String]): Unit = {
        val expressionGrammar =
            """expression: @Expression = term
              |    | expression '+' term {@BinOp(op=$1, lhs:Expression=$0, rhs=$2)}
              |term: @Term = factor
              |    | term '*' factor {BinOp($1, $0, $2)}
              |factor: @Factor = number
              |    | variable
              |    | '(' expression ')' {@Paren(expr=$1)}
              |number: @Number = '0' {@Integer(value=[$0])}
              |    | '1-9' '0-9'* {Integer([$0, $1])}
              |variable = <'A-Za-z'+> {@Variable(name=$0)}
              |array = '[' expression (',' expression)* ']' {@Array(elems=[$1] + $2$1)}
            """.stripMargin

        val arrayGrammar =
            """expression = 'axyz0'
              |array = '[' expression (',' expression)* ']' {@Array(elems=[$1] + $2$1)}
              |""".stripMargin

        val ast = grammarSpecToAST(arrayGrammar)

        println(ast)

        val analysis = Analyzer.analyze(ast.get)

        println(analysis.typeDependenceGraph.toDotGraphModel.printDotGraph())
        println(analysis.typeHierarchyGraph.toDotGraphModel.printDotGraph())

        analysis.grammar("G").rules.foreach { rule =>
            val lhsName = rule._1
            val lhsType = analysis.typeDependenceGraph.inferType(SymbolNode(Symbols.Nonterminal(lhsName)))
            println(s"$lhsName: $lhsType")
        }

        val scaladef = new ScalaDefGenerator(analysis)
        println(scaladef.grammarDef("G", includeAstifiers = false))
        println(scaladef.classDefs())

        analysis.astifiers.foreach { astifier =>
            println(s"${astifier._1} =")
            astifier._2 foreach { r =>
                println(s"  ${r._1.toShortString}")
                println(s"  ${r._2}")
            }
        }

        // 문법이 주어지면
        // 1a. processor가 없는 문법 텍스트
        // 1b. NGrammar 정의하는 스칼라 코드(new NGrammar(...))
        // 1c. (나중엔) 제너레이트된 파서
        // 2. 정의된 타입들을 정의하는 자바 코드
        // 3. ParseForest를 주면 프로세서로 처리해서 가공한 값으로 만들어주는 자바 코드
    }
}
