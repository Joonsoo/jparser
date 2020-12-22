package com.giyeok.jparser.parsergen.deprecated.nocond.codegen

import java.io.{File, PrintWriter}

import com.giyeok.jparser.Inputs.{CharacterTermGroupDesc, CharsGroup, CharsGrouping, TermGroupDesc}
import JavaGenTemplates.MainFunc
import com.google.googlejavaformat.java.Formatter

trait JavaParserGenerator {
    def generateUnformattedJavaSource(pkgName: String, className: String, mainFunc: MainFunc): String

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

object JavaGenUtils {
    implicit def termGroupOrdering[A <: TermGroupDesc]: Ordering[A] = (x: A, y: A) => {
        (x, y) match {
            case (xx: CharsGroup, yy: CharsGroup) =>
                xx.chars.min - yy.chars.min
        }
    }

    def isPrintableChar(char: Char): Boolean = {
        val block = Character.UnicodeBlock.of(char)
        (!Character.isISOControl(char)) && block != null && block != Character.UnicodeBlock.SPECIALS
    }

    def javaChar(char: Char): String = char match {
        case '\n' => "\\n"
        case '\r' => "\\r"
        case '\t' => "\\t"
        case '\\' => "\\\\"
        case c if !isPrintableChar(c) && c.toInt < 65536 =>
            val c1 = (c.toInt >> 8) % 256
            val c2 = c.toInt % 256
            val hexChars = "0123456789abcdef"
            s"\\u${hexChars(c1 >> 4)}${hexChars(c1 & 15)}${hexChars(c2 >> 4)}${hexChars(c2 & 15)}"
        case c => c.toString
    }

    def escapeToJavaString(str: String): String =
        (str flatMap javaChar).replaceAllLiterally("\"", "\\\"")

    def javaString(str: String): String =
        "\"" + escapeToJavaString(str) + "\""

    def charsToCondition(chars: Set[Char], varName: String): String =
        chars.groups map { group =>
            if (group._1 == group._2) s"($varName == '${javaChar(group._1)}')"
            else s"('${javaChar(group._1)}' <= $varName && $varName <= '${javaChar(group._2)}')"
        } mkString " || "

    def charGroupToCondition(charsGroup: CharsGroup, varName: String): String =
        charsToCondition(charsGroup.chars, varName)

    def charGroupToCondition(termGroupDesc: CharacterTermGroupDesc, varName: String): String =
        charGroupToCondition(termGroupDesc.asInstanceOf[CharsGroup], varName)
}

object JavaGenTemplates {

    sealed trait MainFunc

    object NoMainFunc extends MainFunc

    case class InputLoop(tests: List[String]) extends MainFunc

    case class TestInputs(tests: List[String]) extends MainFunc

    // stackIds(), stackDescription(), log(), printStack() 함수 내용.
    // nodeDescriptionOf를 구현해야 한다.
    def logFuncs: String =
        """public String stackIds() {
          |    if (stack == null) {
          |        return ".";
          |    }
          |    return stackIds(stack);
          |}
          |
          |private String stackIds(Stack stack) {
          |    if (stack.prev == null) return "" + stack.nodeId;
          |    else return stackIds(stack.prev) + " " + stack.nodeId;
          |}
          |
          |public String stackDescription() {
          |    if (stack == null) {
          |        return ".";
          |    }
          |    return stackDescription(stack);
          |}
          |
          |private String stackDescription(Stack stack) {
          |    if (stack.prev == null) return nodeDescriptionOf(stack.nodeId);
          |    else return stackDescription(stack.prev) + " " + nodeDescriptionOf(stack.nodeId);
          |}
          |
          |private static void log(String s) {
          |    System.out.println(s);
          |}
          |
          |private void printStack() {
          |    if (stack == null) {
          |        log("  .");
          |    } else {
          |        log("  " + stackIds() + "  pf=" + pendingFinish + "  " + stackDescription());
          |    }
          |}""".stripMargin

    // replace(), append(), finish(), dropLast() 함수 구현
    // finishStep 함수를 별도로 구현해야 한다.
    def basicStackOpFuncs: String =
        """private void replace(int newNodeId) {
          |    stack = new Stack(newNodeId, stack.prev);
          |}
          |
          |private void append(int newNodeId) {
          |    stack = new Stack(newNodeId, stack);
          |}
          |
          |private boolean finish() {
          |    if (stack.prev == null) {
          |        return false;
          |    }
          |    while (finishStep()) {
          |        if (verbose) printStack();
          |        if (stack.prev == null) {
          |            stack = null;
          |            return false;
          |        }
          |    }
          |    return true;
          |}
          |
          |private void dropLast() {
          |    stack = stack.prev;
          |}""".stripMargin

    def proceedFunc: String =
        """public boolean proceed(char c) {
          |    if (stack == null) {
          |        if (verbose) log("  - already finished");
          |        return false;
          |    }
          |    if (!canAccept(c)) {
          |        if (verbose) log("  - cannot accept " + c + ", try pendingFinish");
          |        if (pendingFinish == -1) {
          |            if (verbose) log("  - pendingFinish unavailable, proceed failed");
          |            return false;
          |        }
          |        dropLast();
          |        if (stack.nodeId != pendingFinish) {
          |            replace(pendingFinish);
          |        }
          |        if (verbose) printStack();
          |        if (!finish()) {
          |            return false;
          |        }
          |        return proceed(c);
          |    }
          |    return proceedStep(c);
          |}""".stripMargin

    def proceedEofFunc(startNodeId: Int): String =
        s"""public boolean proceedEof() {
           |    if (stack == null) {
           |        if (verbose) log("  - already finished");
           |        return true;
           |    }
           |    if (pendingFinish == -1) {
           |        if (stack.prev == null && stack.nodeId == $startNodeId) {
           |            return true;
           |        }
           |        if (verbose) log("  - pendingFinish unavailable, proceedEof failed");
           |        return false;
           |    }
           |    dropLast();
           |    if (stack.nodeId != pendingFinish) {
           |        replace(pendingFinish);
           |    }
           |    if (verbose) printStack();
           |    while (stack.prev != null) {
           |        boolean finishNeeded = finishStep();
           |        if (verbose) printStack();
           |        if (!finishNeeded) {
           |            if (pendingFinish == -1) {
           |                return false;
           |            }
           |            dropLast();
           |            replace(pendingFinish);
           |            if (verbose) printStack();
           |        }
           |    }
           |    return true;
           |}""".stripMargin

    def classDef(className: String, startNodeId: Int): String =
        s"""static class Stack {
           |    final int nodeId;
           |    final Stack prev;
           |    Stack(int nodeId, Stack prev) {
           |        this.nodeId = nodeId;
           |        this.prev = prev;
           |    }
           |}
           |
           |private boolean verbose;
           |private Stack stack;
           |private int pendingFinish;
           |
           |public $className(boolean verbose) {
           |    this.verbose = verbose;
           |    this.stack = new Stack($startNodeId, null);
           |    this.pendingFinish = -1;
           |}""".stripMargin

    def parseFunc(className: String): String =
        s"""public static boolean parse(String s) {
           |    $className parser = new $className(false);
           |    for (int i = 0; i < s.length(); i++) {
           |        if (!parser.proceed(s.charAt(i))) {
           |            return false;
           |        }
           |    }
           |    return parser.proceedEof();
           |}
           |
           |public static boolean parseVerbose(String s) {
           |    $className parser = new $className(true);
           |    for (int i = 0; i < s.length(); i++) {
           |        log("Proceed char at " + i + ": " + s.charAt(i));
           |        if (!parser.proceed(s.charAt(i))) {
           |            return false;
           |        }
           |    }
           |    log("Proceed EOF");
           |    return parser.proceedEof();
           |}""".stripMargin

    def mainTestFunc: String =
        """private static void test(String input) {
          |    log("Test \"" + input + "\"");
          |    boolean succeed = parseVerbose(input);
          |    log("Parsing " + (succeed? "succeeded":"failed"));
          |}""".stripMargin

    def inputLoopFunc: String =
        """private static void inputLoop() {
          |    java.util.Scanner scanner = new java.util.Scanner(System.in);
          |    while (true) {
          |        System.out.print("> ");
          |        String input = scanner.nextLine();
          |        if (input.isEmpty()) break;
          |        test(input);
          |    }
          |    System.out.println("Bye~");
          |}""".stripMargin

    def mainFunc(mainFunc: MainFunc): String = mainFunc match {
        case NoMainFunc => ""
        case InputLoop(tests) =>
            val testCalls = tests map { t => s"test(${JavaGenUtils.javaString(t)});" }
            s"""$mainTestFunc
               |
               |$inputLoopFunc
               |
               |public static void main(String[] args) {
               |    ${testCalls mkString "\n"}
               |    inputLoop();
               |}
               |""".stripMargin
        case TestInputs(tests) =>
            val testCalls = tests map { t => s"test(${JavaGenUtils.javaString(t)});" }
            s"""$mainTestFunc
               |
               |public static void main(String[] args) {
               |    ${testCalls mkString "\n"}
               |}""".stripMargin
    }
}
