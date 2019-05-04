package com.giyeok.jparser.parsergen.nocond.codegen

import java.io.{File, PrintWriter}

import com.giyeok.jparser.Grammar
import com.giyeok.jparser.Inputs.{CharacterTermGroupDesc, CharsGroup, CharsGrouping, TermGroupDesc}
import com.giyeok.jparser.examples.{JsonGrammar, SimpleGrammars}
import com.giyeok.jparser.nparser.NGrammar
import com.giyeok.jparser.parsergen.nocond.{SimpleParser, SimpleParserGen}
import com.google.googlejavaformat.java.Formatter

class SimpleParserJavaGen(val parser: SimpleParser) {
    implicit def termGroupOrdering[A <: TermGroupDesc]: Ordering[A] = (x: A, y: A) => {
        (x, y) match {
            case (xx: CharsGroup, yy: CharsGroup) =>
                xx.chars.min - yy.chars.min
        }
    }

    private def javaChar(c: Char) = c match {
        case '\n' => "\\n"
        case '\r' => "\\r"
        case '\t' => "\\t"
        case '\\' => "\\\\"
        // TODO finish
        case c => c.toString
    }

    private def javaString(str: String) =
        "\"" + str.replaceAllLiterally("\\", "\\\\").replaceAllLiterally("\"", "\\\"") + "\""

    private def charsToCondition(chars: Set[Char], varName: String): String =
        chars.groups map { group =>
            if (group._1 == group._2) s"($varName == '${javaChar(group._1)}')"
            else s"('${javaChar(group._1)}' <= $varName && $varName <= '${javaChar(group._2)}')"
        } mkString " || "

    private def charGroupToCondition(charsGroup: CharsGroup, varName: String): String =
        charsToCondition(charsGroup.chars, varName)

    private def charGroupToCondition(termGroupDesc: CharacterTermGroupDesc, varName: String): String =
        charGroupToCondition(termGroupDesc.asInstanceOf[CharsGroup], varName)

    def generateUnformattedJavaSource(pkgName: String, className: String, testStrOpt: Option[String]): String = {
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

        val mainTestPart = testStrOpt map { testStr =>
            s"""
               |public static void main(String[] args) {
               |    boolean succeed = parseVerbose(${javaString(testStr)});
               |    log("Parsing " + (succeed? "succeeded":"failed"));
               |}""".stripMargin
        } getOrElse ""

        s"""package $pkgName;
           |
           |public class $className {
           |    static class Stack {
           |        final int nodeId;
           |        final Stack prev;
           |
           |        Stack(int nodeId, Stack prev) {
           |            this.nodeId = nodeId;
           |            this.prev = prev;
           |        }
           |    }
           |
           |    private boolean verbose;
           |    private Stack stack;
           |    private int pendingFinish;
           |
           |    public $className(boolean verbose) {
           |        this.verbose = verbose;
           |        this.stack = new Stack(${parser.startNodeId}, null);
           |        this.pendingFinish = -1;
           |    }
           |
           |    public boolean canAccept(char c) {
           |        if (stack == null) return false;
           |        switch (stack.nodeId) {
           |            ${canAcceptCases mkString "\n"}
           |        }
           |        throw new AssertionError("Unknown nodeId: " + stack.nodeId);
           |    }
           |
           |    public String nodeDescriptionOf(int nodeId) {
           |        switch (nodeId) {
           |            ${nodeDescriptionCases mkString "\n"}
           |        }
           |        return null;
           |    }
           |
           |    private void replace(int newNodeId) {
           |        stack = new Stack(newNodeId, stack.prev);
           |    }
           |
           |    private void append(int newNodeId) {
           |        stack = new Stack(newNodeId, stack);
           |    }
           |
           |    // false를 리턴하면 더이상 finishStep을 하지 않아도 되는 상황
           |    // true를 리턴하면 finishStep을 계속 해야하는 상황
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
           |    private boolean finish() {
           |        if (stack.prev == null) {
           |            return false;
           |        }
           |        while (finishStep()) {
           |            if (verbose) {
           |                printStack();
           |            }
           |            if (stack.prev == null) {
           |                stack = null;
           |                return false;
           |            }
           |        }
           |        return true;
           |    }
           |
           |    private void dropLast() {
           |        stack = stack.prev;
           |    }
           |
           |    public String stackIds() {
           |        if (stack == null) {
           |            return ".";
           |        }
           |        return stackIds(stack);
           |    }
           |
           |    private String stackIds(Stack stack) {
           |        if (stack.prev == null) return "" + stack.nodeId;
           |        else return stackIds(stack.prev) + " " + stack.nodeId;
           |    }
           |
           |    public String stackDescription() {
           |        if (stack == null) {
           |            return ".";
           |        }
           |        return stackDescription(stack);
           |    }
           |
           |    private String stackDescription(Stack stack) {
           |        if (stack.prev == null) return nodeDescriptionOf(stack.nodeId);
           |        else return stackDescription(stack.prev) + " " + nodeDescriptionOf(stack.nodeId);
           |    }
           |
           |    private static void log(String s) {
           |        System.out.println(s);
           |    }
           |
           |    private void printStack() {
           |        if (stack == null) {
           |            log("  .");
           |        } else {
           |            log("  " + stackIds() + " " + stackDescription());
           |        }
           |    }
           |
           |    public boolean proceed(char c) {
           |        if (stack == null) {
           |            if (verbose) {
           |                log("  - already finished");
           |            }
           |            return false;
           |        }
           |        if (!canAccept(c)) {
           |            if (verbose) {
           |                log("  - cannot accept " + c + ", try pendingFinish");
           |            }
           |            if (pendingFinish == -1) {
           |                if (verbose) {
           |                    log("  - pendingFinish unavailable, proceed failed");
           |                }
           |                return false;
           |            }
           |            dropLast();
           |            if (stack.nodeId != pendingFinish) {
           |                replace(pendingFinish);
           |            }
           |            if (verbose) {
           |                printStack();
           |            }
           |            if (!finish()) {
           |                return false;
           |            }
           |            return proceed(c);
           |        }
           |        switch (stack.nodeId) {
           |            ${proceedCases mkString "\n"}
           |        }
           |        throw new AssertionError("Unknown nodeId: " + stack.nodeId);
           |    }
           |
           |    public boolean proceedEof() {
           |        if (stack == null) {
           |            if (verbose) {
           |                log("  - already finished");
           |                return true;
           |            }
           |        }
           |        if (pendingFinish == -1) {
           |            if (stack.prev == null && stack.nodeId == ${parser.startNodeId}) {
           |                return true;
           |            }
           |            if (verbose) {
           |                log("  - pendingFinish unavailable, proceedEof failed");
           |            }
           |            return false;
           |        }
           |        dropLast();
           |        if (stack.nodeId != pendingFinish) {
           |            replace(pendingFinish);
           |        }
           |        if (verbose) printStack();
           |        while (stack.prev != null) {
           |            boolean finishNeeded = finishStep();
           |            if (verbose) printStack();
           |            if (!finishNeeded) {
           |                if (pendingFinish == -1) {
           |                    return false;
           |                }
           |                dropLast();
           |                replace(pendingFinish);
           |                if (verbose) {
           |                    printStack();
           |                }
           |            }
           |        }
           |        return true;
           |    }
           |
           |    public static boolean parse(String s) {
           |        $className parser = new $className(false);
           |        for (int i = 0; i < s.length(); i++) {
           |            if (!parser.proceed(s.charAt(i))) {
           |                return false;
           |            }
           |        }
           |        return parser.proceedEof();
           |    }
           |
           |    public static boolean parseVerbose(String s) {
           |        $className parser = new $className(true);
           |        for (int i = 0; i < s.length(); i++) {
           |            log("Proceed char at " + i + ": " + s.charAt(i));
           |            if (!parser.proceed(s.charAt(i))) {
           |                return false;
           |            }
           |        }
           |        log("Proceed EOF");
           |        return parser.proceedEof();
           |    }
           |    $mainTestPart
           |}
             """.stripMargin
    }

    def generateJavaSource(pkgName: String, className: String, testStr: Option[String] = None): String =
        new Formatter().formatSource(generateUnformattedJavaSource(pkgName, className, testStr))

    def generateJavaSourceToFile(file: File, pkgName: String, className: String, testStr: Option[String] = None): Unit = {
        val writer = new PrintWriter(file)
        writer.write(generateJavaSource(pkgName, className, testStr))
        writer.close()
    }

    def generateJavaSourceToDir(baseDir: File, pkgName: String, className: String, testStr: Option[String] = None): Unit = {
        val file = new File(baseDir, (pkgName.split("\\.") :+ className + ".java").mkString(File.separator))
        println(s"generate parser to ${file.getAbsolutePath}")
        generateJavaSourceToFile(file, pkgName, className, testStr)
    }
}

object SimpleParserJavaGen {
    val baseDir = new File("parsergen/src/main/java")
    val pkgName = "com.giyeok.jparser.parsergen"

    def generate(grammar: Grammar, className: String, example: Option[String]): Unit = {
        val ngrammar = NGrammar.fromGrammar(grammar)
        ngrammar.describe()
        val parser = new SimpleParserGen(ngrammar).generateParser()
        parser.describe()

        new SimpleParserJavaGen(parser).generateJavaSourceToDir(baseDir, pkgName, className, example)
    }

    def main(args: Array[String]): Unit = {
        generate(SimpleGrammars.array0Grammar, "Array0GrammarParser", Some("[a,  a, a]"))
        generate(JsonGrammar.fromJsonOrg, "JsonParser", Some("""{"abcd": ["hello", 123, {"xyz": 1}]}"""))
    }
}
