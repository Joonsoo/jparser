package com.giyeok.jparser.metalang2

import java.io.{BufferedWriter, File, FileWriter}

import com.giyeok.jparser.Inputs.CharsGrouping
import com.giyeok.jparser.Symbols.Terminals
import com.giyeok.jparser.examples.metalang2.{ExpressionGrammarsMetaLang2, MetaLang2Grammar}
import com.giyeok.jparser.metalang.MetaGrammar
import com.giyeok.jparser.nparser.{NaiveParser, ParseTreeConstructor}
import com.giyeok.jparser.{Grammar, NGrammar, ParseForestFunc, Symbols}

import scala.io.Source

object MetaLanguage2 {

    object GrammarDef {
        lazy val oldGrammar: NGrammar = NGrammar.fromGrammar(
            MetaLang2Grammar.inMetaLang1.toGrammar(MetaGrammar.translateForce))

        val newGrammar: String = MetaLang2Grammar.inMetaLang2.grammar
    }

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

    def grammarSpecToGrammar(grammarName: String, grammar: String): Option[Grammar] = grammarSpecToAST(grammar) map { ast =>
        new AstAnalyzer(ast).analyzeAstifiers().grammar(grammarName)
    }

    class Analysis(val astAnalysis: AstAnalysis, val typeAnalysis: TypeAnalysis)

    def analyze(ast: AST.Grammar): Analysis = {
        val astAnalyzer = new AstAnalyzer(ast)
        val astAnalysis = astAnalyzer.analyzeAstifiers()
        val typeAnalysis = new TypeAnalyzer(astAnalyzer).analyzeTypeHierarchy()

        new Analysis(astAnalysis, typeAnalysis)
    }

    def stringify(grammar: Grammar): String = {
        def symbolStringOf(symbol: Symbols.Symbol, outerPrec: Int): String = {
            val (string: String, prec: Int) = symbol match {
                case terminal: Symbols.Terminal =>
                    val s = terminal match {
                        case Terminals.Any | Terminals.AnyChar => "."
                        case Terminals.ExactChar(char) =>
                            val s = char match {
                                case '\n' => "\\n"
                                case '\\' => "\\\\"
                                case '\'' => "\\'"
                                // TODO complete escape
                                case _ => s"$char"
                            }
                            s"'$s'"
                        case chars: Terminals.Chars =>
                            def escapeChar(char: Char): String =
                                char match {
                                    case '-' => "\\-"
                                    case '}' => "\\}"
                                    case '\n' => "\\n"
                                    case '\t' => "\\t"
                                    case '\r' => "\\r"
                                    case '\\' => "\\\\"
                                    // TODO complete escape
                                    case _ => "" + char
                                }

                            "'" + (chars.chars.groups.sorted map { pair =>
                                if (pair._1 == pair._2) s"${escapeChar(pair._1)}"
                                else if (pair._1 + 1 == pair._2) s"${escapeChar(pair._1)}${escapeChar(pair._2)}"
                                else s"${escapeChar(pair._1)}-${escapeChar(pair._2)}"
                            } mkString "") + "'"
                        // case Terminals.Unicode(categories) =>
                        // case Terminals.ExactVirtual(name) =>
                        // case Terminals.Virtuals(names) =>
                    }
                    (s, 0)
                case Symbols.Nonterminal(name) =>
                    (name, 0)
                case Symbols.Sequence(seq) =>
                    val isString = seq forall (_.isInstanceOf[Symbols.Terminals.ExactChar])
                    if (isString) {
                        // string인 경우
                        // TODO escape
                        ("\"" + (seq map (_.asInstanceOf[Symbols.Terminals.ExactChar].char)).mkString + "\"", 0)
                    } else {
                        (seq map {
                            symbolStringOf(_, 5)
                        } mkString " ", 5)
                    }
                case Symbols.OneOf(syms) =>
                    val isOptional = syms.size == 2 && (syms contains Symbols.Proxy(Symbols.Sequence(Seq())))
                    if (isOptional) {
                        val optContent = (syms - Symbols.Proxy(Symbols.Sequence(Seq()))).head
                        (symbolStringOf(optContent, 2) + "?", 2)
                    } else {
                        ("(" + (syms.toSeq map {
                            symbolStringOf(_, 5)
                        } mkString " | ") + ")", 0)
                    }
                case Symbols.Repeat(sym, 0) =>
                    (symbolStringOf(sym, 2) + "*", 2)
                case Symbols.Repeat(sym, 1) =>
                    (symbolStringOf(sym, 2) + "+", 2)
                case Symbols.Repeat(sym, more) =>
                    ("(" + ((symbolStringOf(sym, 5) + " ") * more) + (symbolStringOf(sym, 5) + "*") + ")", 0)
                case Symbols.Except(sym, except) =>
                    (symbolStringOf(sym, 4) + "-" + symbolStringOf(except, 4), 4)
                case Symbols.LookaheadIs(lookahead) =>
                    ("^" + symbolStringOf(lookahead, 0), 1)
                case Symbols.LookaheadExcept(except) =>
                    ("!" + symbolStringOf(except, 0), 1)
                case Symbols.Proxy(sym) =>
                    ("(" + symbolStringOf(sym, 5) + ")", 0)
                case Symbols.Join(sym, join) =>
                    (symbolStringOf(sym, 3) + "&" + symbolStringOf(join, 3), 3)
                case Symbols.Longest(sym) =>
                    ("<" + symbolStringOf(sym, 5) + ">", 0)
            }
            if (outerPrec < prec) "(" + string + ")" else string
        }

        def nonterminalNameOf(name: String): String = {
            val charArray = name.toCharArray
            if (charArray forall { c => ('0' <= c && c <= '9') || ('A' <= c && c <= 'Z') || ('a' <= c && c <= 'z') || (c == '_') }) {
                name
            } else {
                // TODO \하고 ` escape
                s"`$name`"
            }
        }

        def ruleStringOf(lhs: String, rhs: List[Symbols.Symbol]): String = {
            nonterminalNameOf(lhs) + " = " + ((rhs map { symbol => symbolStringOf(symbol, 6) }) mkString "\n    | ")
        }

        val startRule = grammar.rules(grammar.startSymbol.name).toList
        val restRules = grammar.rules - grammar.startSymbol.name

        ruleStringOf(grammar.startSymbol.name, startRule) + "\n" +
            (restRules.toList.map { kv => ruleStringOf(kv._1, kv._2.toList) } mkString "\n")
    }

    // 문법이 주어지면
    // 1a. processor가 없는 문법 텍스트
    // 1b. NGrammar 정의하는 스칼라 코드(new NGrammar(...))
    // 1c. (나중엔) 제너레이트된 파서
    // 2. 정의된 타입들을 정의하는 자바 코드
    // 3. ParseForest를 주면 프로세서로 처리해서 가공한 값으로 만들어주는 자바 코드

    case class TestGrammar(packageName: Option[String], name: String, grammar: String, parseUtils: Boolean = true, astPrettyPrinter: Boolean = true)

    def readFile(path: String): String = {
        val s = Source.fromFile(new File(path))
        try s.getLines().mkString("\n") finally s.close()
    }

    def main(args: Array[String]): Unit = {
        val pkgName = Some("com.giyeok.jparser.metalang2.generated")
        val expressionGrammar = TestGrammar(pkgName, "ExpressionGrammar", ExpressionGrammarsMetaLang2.basic.grammar)
        val metaGrammar2 = TestGrammar(pkgName, "MetaGrammar2Ast", MetaLang2Grammar.inMetaLang2.grammar)
        val pyobjGrammar = TestGrammar(pkgName, "PyObjGrammar", readFile("./examples/src/main/resources/pyobj.cdg"))
        // val protobufTextFormatGrammar = TestGrammar(pkgName, "ProtobufTextFormatGrammar", readFile("./protobuf_textformat.cdg"))
        val argsGrammar = TestGrammar(pkgName, "ArgsGrammar", readFile("./examples/src/main/resources/args.cdg"))

        List(argsGrammar) foreach { grammar =>
            val analysis = analyze(grammarSpecToAST(grammar.grammar).get)

            analysis.astAnalysis.astifiers.foreach { case (lhs, rhs) =>
                println(s"$lhs =")
                rhs.foreach { r =>
                    println(s"  ${r._1.toShortString}")
                    println(s"  ${r._2}")
                }
            }
            println(analysis.typeAnalysis.typeDependenceGraph.toDotGraphModel.printDotGraph())

            val scalaCode = s"package ${grammar.packageName.get}\n\n" +
                new ScalaDefGenerator(analysis, parseUtils = grammar.parseUtils).toGrammarObject(grammar.name)
            val filepath = s"./metalang/src/main/scala/com/giyeok/jparser/metalang2/generated/${grammar.name}.scala"
            val writer = new BufferedWriter(new FileWriter(filepath))
            writer.write(scalaCode)
            writer.close()

            println(s"Written parser to $filepath")
        }
    }
}
