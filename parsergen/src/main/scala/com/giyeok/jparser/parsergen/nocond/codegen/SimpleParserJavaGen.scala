package com.giyeok.jparser.parsergen.nocond.codegen

import java.io.{File, PrintWriter}

import com.giyeok.jparser.Grammar
import com.giyeok.jparser.examples.{ExpressionGrammars, JsonGrammar, SimpleGrammars}
import com.giyeok.jparser.gramgram.MetaGrammar
import com.giyeok.jparser.nparser.NGrammar
import com.giyeok.jparser.parsergen.nocond.codegen.JavaGenTemplates.{InputLoop, MainFunc, TestInputs}
import com.giyeok.jparser.parsergen.nocond.codegen.JavaGenUtils._
import com.giyeok.jparser.parsergen.nocond.{SimpleParser, SimpleParserGen}
import com.google.googlejavaformat.java.Formatter

class SimpleParserJavaGen(val parser: SimpleParser) {
    def generateUnformattedJavaSource(pkgName: String, className: String, mainFunc: MainFunc): String = {
        // (nodeId, TermGroup) -> Action 의 형태를 (nodeId) -> (TermGroup -> Action) 으로 바꿈
        val termActions = (parser.termActions groupBy (_._1._1)
            mapValues (m => m map (p => p._1._2 -> p._2))).toList sortBy (_._1)

        val canAcceptCases = termActions map { kv =>
            val (nodeId, actions) = kv
            val termGroups = actions.keys.toList.sorted
            val condition = termGroups map (term => charGroupToCondition(term, "c")) mkString " || "

            s"case $nodeId: return $condition;"
        }

        val nodeDescriptionCases = parser.nodes.toList.sortBy(_._1) map { node =>
            val (nodeId, kernelSet) = node
            val nodeDescription = s"{${kernelSet.toReadableString(parser.grammar)}}"

            s"case $nodeId: return ${javaString(nodeDescription)};"
        }

        val finishCases = parser.edgeActions.toList.sortBy(_._1) map { kv =>
            val (edge, edgeAction) = kv
            // pendingFinish를 세팅을 하든지, true를 리턴하든지
            val actions = edgeAction match {
                case SimpleParser.DropLast(replace) if replace == edge._1 =>
                    List("dropLast();",
                        "return true;")
                case SimpleParser.DropLast(replace) =>
                    List("dropLast();",
                        s"replace($replace);",
                        "return true;")
                case SimpleParser.ReplaceEdge(replacePrev, replaceLast, pendingFinish) if (replacePrev, replaceLast) == edge =>
                    List(s"pendingFinish = ${pendingFinish.getOrElse(-1)};",
                        "return false;")
                case SimpleParser.ReplaceEdge(replacePrev, replaceLast, pendingFinish) if replacePrev == edge._1 =>
                    List(s"replace($replaceLast);",
                        s"pendingFinish = ${pendingFinish.getOrElse(-1)};",
                        "return false;")
                case SimpleParser.ReplaceEdge(replacePrev, replaceLast, pendingFinish) =>
                    List("dropLast();",
                        s"replace($replacePrev);",
                        s"append($replaceLast);",
                        s"pendingFinish = ${pendingFinish.getOrElse(-1)};",
                        "return false;")
            }

            s"""if (prev == ${edge._1} && last == ${edge._2}) {  // $edge
               |    // $edgeAction
               |    ${actions mkString "\n"}
               |}""".stripMargin
        }

        val proceedCases = termActions map { kv =>
            val (nodeId, nodeTermActions) = kv
            val termCases = nodeTermActions.toList.sortBy(_._1) map { termCase =>
                val condition = charGroupToCondition(termCase._1, "c")
                val printStack = "if (verbose) printStack();"
                val actions: List[String] = termCase._2 match {
                    case SimpleParser.Finish(replace) if replace == nodeId =>
                        // Finish
                        List("finish();",
                            printStack)
                    case SimpleParser.Finish(replace) =>
                        // ReplaceAndFinish
                        List(s"replace($replace);",
                            printStack,
                            "finish();",
                            printStack)
                    case SimpleParser.Append(replace, append, pendingFinish) if replace == nodeId =>
                        // Append
                        List(s"append($append);",
                            s"pendingFinish = ${pendingFinish.getOrElse(-1)};",
                            printStack)
                    case SimpleParser.Append(replace, append, pendingFinish) =>
                        // ReplaceAndAppend
                        List(s"replace($replace);", s"append($append);",
                            s"pendingFinish = ${pendingFinish.getOrElse(-1)};",
                            printStack)
                }
                s"""if ($condition) {
                   |    // ${termCase._2}
                   |    ${actions mkString "\n"}
                   |    return true;
                   |}""".stripMargin
            }
            s"""case $nodeId:
               |    ${termCases mkString "\n"}
               |    return false;""".stripMargin
        }

        s"""package $pkgName;
           |
           |public class $className {
           |    ${JavaGenTemplates.classDef(className, parser.startNodeId)}
           |
           |    public boolean canAccept(char c) {
           |        if (stack == null) return false;
           |        switch (stack.nodeId) {
           |            ${canAcceptCases mkString "\n"}
           |        }
           |        throw new AssertionError("Unknown nodeId: " + stack.nodeId);
           |    }
           |
           |    ${JavaGenTemplates.logFuncs}
           |
           |    public String nodeDescriptionOf(int nodeId) {
           |        switch (nodeId) {
           |            ${nodeDescriptionCases mkString "\n"}
           |        }
           |        return null;
           |    }
           |
           |    ${JavaGenTemplates.basicStackOpFuncs}
           |
           |    // Returns true if further finishStep is required
           |    private boolean finishStep() {
           |        if (stack == null || stack.prev == null) {
           |            throw new AssertionError("No edge to finish: " + stackIds());
           |        }
           |        int prev = stack.prev.nodeId;
           |        int last = stack.nodeId;
           |        ${finishCases mkString "\n"}
           |        throw new AssertionError("Unknown edge to finish: " + stackIds());
           |    }
           |
           |    private boolean proceedStep(char c) {
           |        switch (stack.nodeId) {
           |            ${proceedCases mkString "\n"}
           |        }
           |        throw new AssertionError("Unknown nodeId: " + stack.nodeId);
           |    }
           |
           |    ${JavaGenTemplates.proceedFunc}
           |
           |    ${JavaGenTemplates.proceedEofFunc(parser.startNodeId)}
           |
           |    ${JavaGenTemplates.parseFunc(className)}
           |
           |    ${JavaGenTemplates.mainFunc(mainFunc)}
           |}""".stripMargin
    }

    def generateJavaSource(pkgName: String, className: String, mainFunc: MainFunc): String =
        new Formatter().formatSource(generateUnformattedJavaSource(pkgName, className, mainFunc))

    def generateJavaSourceToFile(file: File, pkgName: String, className: String, mainFunc: MainFunc): Unit = {
        val writer = new PrintWriter(file)
        writer.write(generateJavaSource(pkgName, className, mainFunc))
        writer.close()
    }

    def generateJavaSourceToDir(baseDir: File, pkgName: String, className: String, mainFunc: MainFunc): Unit = {
        val file = new File(baseDir, (pkgName.split("\\.") :+ className + ".java").mkString(File.separator))
        println(s"generate parser to ${file.getAbsolutePath}")
        generateJavaSourceToFile(file, pkgName, className, mainFunc)
    }
}

object SimpleParserJavaGen {
    val baseDir = new File("parsergen/src/main/java")
    val pkgName = "com.giyeok.jparser.parsergen.generated.simplegen"

    def generateParser(grammar: Grammar): SimpleParser = {
        val ngrammar = NGrammar.fromGrammar(grammar)
        ngrammar.describe()
        val parser = new SimpleParserGen(ngrammar).generateParser()
        parser.describe()

        parser
    }

    def generate(grammar: Grammar, className: String, mainFunc: MainFunc): Unit = {
        new SimpleParserJavaGen(generateParser(grammar)).generateJavaSourceToDir(baseDir, pkgName, className, mainFunc)
    }

    def main(args: Array[String]): Unit = {
        generate(MetaGrammar.translateForce("LongestPrior", "S = T*\nT = 'a'+|'b'+"),
            "LongestPriorParser", InputLoop(List()))
        generate(ExpressionGrammars.simple, "ExprGrammarSimpleParser", InputLoop(List("123+456")))
        generate(SimpleGrammars.array0Grammar, "Array0GrammarParser", InputLoop(List("[a,a,a]")))
        generate(MetaGrammar.translateForce("Super Simple", "S='x' A 'y'|'x' B 'y'\nA=['a']\nB=['a' 'b']"),
            "SuperSimpleGrammar", TestInputs(List("xy", "xay", "xaby")))
        generate(JsonGrammar.fromJsonOrg, "JsonParser", InputLoop(List(
            """{"abcd": ["hello", 123, {"xyz": 1}]}""")))
        generate(JsonGrammar.custom, "CustomJsonParser", InputLoop(List(
            """{"abcd": ["hello", 123, {"xyz": 1}]}""")))
    }
}
