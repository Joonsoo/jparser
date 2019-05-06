package com.giyeok.jparser.parsergen;

public class ExprGrammarSimpleParser {
  static class Stack {
    final int nodeId;
    final Stack prev;

    Stack(int nodeId, Stack prev) {
      this.nodeId = nodeId;
      this.prev = prev;
    }
  }

  private boolean verbose;
  private Stack stack;
  private int pendingFinish;

  public ExprGrammarSimpleParser(boolean verbose) {
    this.verbose = verbose;
    this.stack = new Stack(0, null);
    this.pendingFinish = -1;
  }

  public boolean canAccept(char c) {
    if (stack == null) return false;
    switch (stack.nodeId) {
      case 0:
        return (c == '(') || (c == '0') || ('1' <= c && c <= '9');
      case 1:
        return (c == '(') || (c == '0') || ('1' <= c && c <= '9');
      case 2:
        return (c == '*') || (c == '+');
      case 3:
        return (c == '*') || (c == '+') || ('0' <= c && c <= '9');
      case 4:
        return (c == '+');
      case 5:
        return (c == '*');
      case 6:
        return ('0' <= c && c <= '9');
      case 7:
        return ('0' <= c && c <= '9');
      case 8:
        return (c == '(') || (c == '0') || ('1' <= c && c <= '9');
      case 9:
        return (c == ')');
      case 10:
        return (c == '(') || (c == '0') || ('1' <= c && c <= '9');
      case 11:
        return (c == '*') || ('0' <= c && c <= '9');
    }
    throw new AssertionError("Unknown nodeId: " + stack.nodeId);
  }

  public String nodeDescriptionOf(int nodeId) {
    switch (nodeId) {
      case 0:
        return "{•<start>}";
      case 1:
        return "{'('•E ')'}";
      case 2:
        return "{T•'*' F|E•'+' T}";
      case 3:
        return "{{1-9}•{0-9}*|T•'*' F|E•'+' T}";
      case 4:
        return "{E•'+' T}";
      case 5:
        return "{T•'*' F}";
      case 6:
        return "{{1-9}•{0-9}*}";
      case 7:
        return "{{0-9}*•{0-9}}";
      case 8:
        return "{T '*'•F}";
      case 9:
        return "{'(' E•')'}";
      case 10:
        return "{E '+'•T}";
      case 11:
        return "{{1-9}•{0-9}*|T•'*' F}";
    }
    return null;
  }

  private void replace(int newNodeId) {
    stack = new Stack(newNodeId, stack.prev);
  }

  private void append(int newNodeId) {
    stack = new Stack(newNodeId, stack);
  }

  // Returns true if further finishStep is required
  private boolean finishStep() {
    if (stack == null || stack.prev == null) {
      throw new AssertionError("No edge to finish: " + stackIds());
    }
    int prev = stack.prev.nodeId;
    int last = stack.nodeId;
    if (prev == 0 && last == 1) { // (0,1)
      // ReplaceEdge(0,9,None)
      replace(9);
      pendingFinish = -1;
      return false;
    }
    if (prev == 0 && last == 4) { // (0,4)
      // ReplaceEdge(0,10,None)
      replace(10);
      pendingFinish = -1;
      return false;
    }
    if (prev == 0 && last == 5) { // (0,5)
      // ReplaceEdge(0,8,None)
      replace(8);
      pendingFinish = -1;
      return false;
    }
    if (prev == 0 && last == 6) { // (0,6)
      // ReplaceEdge(0,2,Some(0))
      replace(2);
      pendingFinish = 0;
      return false;
    }
    if (prev == 0 && last == 8) { // (0,8)
      // ReplaceEdge(0,2,Some(0))
      replace(2);
      pendingFinish = 0;
      return false;
    }
    if (prev == 0 && last == 9) { // (0,9)
      // ReplaceEdge(0,2,Some(0))
      replace(2);
      pendingFinish = 0;
      return false;
    }
    if (prev == 0 && last == 10) { // (0,10)
      // ReplaceEdge(0,4,Some(0))
      replace(4);
      pendingFinish = 0;
      return false;
    }
    if (prev == 1 && last == 1) { // (1,1)
      // DropLast(1)
      dropLast();
      return true;
    }
    if (prev == 1 && last == 4) { // (1,4)
      // ReplaceEdge(1,10,None)
      replace(10);
      pendingFinish = -1;
      return false;
    }
    if (prev == 1 && last == 5) { // (1,5)
      // ReplaceEdge(1,8,None)
      replace(8);
      pendingFinish = -1;
      return false;
    }
    if (prev == 1 && last == 6) { // (1,6)
      // ReplaceEdge(1,2,Some(1))
      replace(2);
      pendingFinish = 1;
      return false;
    }
    if (prev == 1 && last == 8) { // (1,8)
      // ReplaceEdge(1,2,Some(1))
      replace(2);
      pendingFinish = 1;
      return false;
    }
    if (prev == 1 && last == 10) { // (1,10)
      // ReplaceEdge(1,4,Some(1))
      replace(4);
      pendingFinish = 1;
      return false;
    }
    if (prev == 6 && last == 7) { // (6,7)
      // ReplaceEdge(6,7,Some(6))
      pendingFinish = 6;
      return false;
    }
    if (prev == 8 && last == 1) { // (8,1)
      // ReplaceEdge(8,9,None)
      replace(9);
      pendingFinish = -1;
      return false;
    }
    if (prev == 8 && last == 6) { // (8,6)
      // DropLast(8)
      dropLast();
      return true;
    }
    if (prev == 8 && last == 9) { // (8,9)
      // DropLast(8)
      dropLast();
      return true;
    }
    if (prev == 10 && last == 1) { // (10,1)
      // ReplaceEdge(10,9,None)
      replace(9);
      pendingFinish = -1;
      return false;
    }
    if (prev == 10 && last == 5) { // (10,5)
      // ReplaceEdge(10,8,None)
      replace(8);
      pendingFinish = -1;
      return false;
    }
    if (prev == 10 && last == 6) { // (10,6)
      // ReplaceEdge(10,5,Some(10))
      replace(5);
      pendingFinish = 10;
      return false;
    }
    if (prev == 10 && last == 8) { // (10,8)
      // ReplaceEdge(10,5,Some(10))
      replace(5);
      pendingFinish = 10;
      return false;
    }
    if (prev == 10 && last == 9) { // (10,9)
      // ReplaceEdge(10,5,Some(10))
      replace(5);
      pendingFinish = 10;
      return false;
    }
    throw new AssertionError("Unknown edge to finish: " + stackIds());
  }

  private boolean finish() {
    if (stack.prev == null) {
      return false;
    }
    while (finishStep()) {
      if (verbose) printStack();
      if (stack.prev == null) {
        stack = null;
        return false;
      }
    }
    return true;
  }

  private void dropLast() {
    stack = stack.prev;
  }

  public String stackIds() {
    if (stack == null) {
      return ".";
    }
    return stackIds(stack);
  }

  private String stackIds(Stack stack) {
    if (stack.prev == null) return "" + stack.nodeId;
    else return stackIds(stack.prev) + " " + stack.nodeId;
  }

  public String stackDescription() {
    if (stack == null) {
      return ".";
    }
    return stackDescription(stack);
  }

  private String stackDescription(Stack stack) {
    if (stack.prev == null) return nodeDescriptionOf(stack.nodeId);
    else return stackDescription(stack.prev) + " " + nodeDescriptionOf(stack.nodeId);
  }

  private static void log(String s) {
    System.out.println(s);
  }

  private void printStack() {
    if (stack == null) {
      log("  .");
    } else {
      log("  " + stackIds() + "  pf=" + pendingFinish + "  " + stackDescription());
    }
  }

  public boolean proceed(char c) {
    if (stack == null) {
      if (verbose) log("  - already finished");
      return false;
    }
    if (!canAccept(c)) {
      if (verbose) log("  - cannot accept " + c + ", try pendingFinish");
      if (pendingFinish == -1) {
        if (verbose) log("  - pendingFinish unavailable, proceed failed");
        return false;
      }
      dropLast();
      if (stack.nodeId != pendingFinish) {
        replace(pendingFinish);
      }
      if (verbose) printStack();
      if (!finish()) {
        return false;
      }
      return proceed(c);
    }
    switch (stack.nodeId) {
      case 0:
        if ((c == '(')) {
          // Append(0,1,None)
          append(1);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == '0')) {
          // Append(0,2,Some(0))
          append(2);
          pendingFinish = 0;
          if (verbose) printStack();
          return true;
        }
        if (('1' <= c && c <= '9')) {
          // Append(0,3,Some(0))
          append(3);
          pendingFinish = 0;
          if (verbose) printStack();
          return true;
        }
        return false;
      case 1:
        if ((c == '(')) {
          // Append(1,1,None)
          append(1);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == '0')) {
          // Append(1,2,Some(1))
          append(2);
          pendingFinish = 1;
          if (verbose) printStack();
          return true;
        }
        if (('1' <= c && c <= '9')) {
          // Append(1,3,Some(1))
          append(3);
          pendingFinish = 1;
          if (verbose) printStack();
          return true;
        }
        return false;
      case 2:
        if ((c == '*')) {
          // Finish(5)
          replace(5);
          if (verbose) printStack();
          finish();
          if (verbose) printStack();
          return true;
        }
        if ((c == '+')) {
          // Finish(4)
          replace(4);
          if (verbose) printStack();
          finish();
          if (verbose) printStack();
          return true;
        }
        return false;
      case 3:
        if ((c == '*')) {
          // Finish(5)
          replace(5);
          if (verbose) printStack();
          finish();
          if (verbose) printStack();
          return true;
        }
        if ((c == '+')) {
          // Finish(4)
          replace(4);
          if (verbose) printStack();
          finish();
          if (verbose) printStack();
          return true;
        }
        if (('0' <= c && c <= '9')) {
          // Append(6,7,Some(6))
          replace(6);
          append(7);
          pendingFinish = 6;
          if (verbose) printStack();
          return true;
        }
        return false;
      case 4:
        if ((c == '+')) {
          // Finish(4)
          finish();
          if (verbose) printStack();
          return true;
        }
        return false;
      case 5:
        if ((c == '*')) {
          // Finish(5)
          finish();
          if (verbose) printStack();
          return true;
        }
        return false;
      case 6:
        if (('0' <= c && c <= '9')) {
          // Append(6,7,Some(6))
          append(7);
          pendingFinish = 6;
          if (verbose) printStack();
          return true;
        }
        return false;
      case 7:
        if (('0' <= c && c <= '9')) {
          // Finish(7)
          finish();
          if (verbose) printStack();
          return true;
        }
        return false;
      case 8:
        if ((c == '(')) {
          // Append(8,1,None)
          append(1);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == '0')) {
          // Finish(8)
          finish();
          if (verbose) printStack();
          return true;
        }
        if (('1' <= c && c <= '9')) {
          // Append(8,6,Some(8))
          append(6);
          pendingFinish = 8;
          if (verbose) printStack();
          return true;
        }
        return false;
      case 9:
        if ((c == ')')) {
          // Finish(9)
          finish();
          if (verbose) printStack();
          return true;
        }
        return false;
      case 10:
        if ((c == '(')) {
          // Append(10,1,None)
          append(1);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == '0')) {
          // Append(10,5,Some(10))
          append(5);
          pendingFinish = 10;
          if (verbose) printStack();
          return true;
        }
        if (('1' <= c && c <= '9')) {
          // Append(10,11,Some(10))
          append(11);
          pendingFinish = 10;
          if (verbose) printStack();
          return true;
        }
        return false;
      case 11:
        if ((c == '*')) {
          // Finish(5)
          replace(5);
          if (verbose) printStack();
          finish();
          if (verbose) printStack();
          return true;
        }
        if (('0' <= c && c <= '9')) {
          // Append(6,7,Some(6))
          replace(6);
          append(7);
          pendingFinish = 6;
          if (verbose) printStack();
          return true;
        }
        return false;
    }
    throw new AssertionError("Unknown nodeId: " + stack.nodeId);
  }

  public boolean proceedEof() {
    if (stack == null) {
      if (verbose) log("  - already finished");
      return true;
    }
    if (pendingFinish == -1) {
      if (stack.prev == null && stack.nodeId == 0) {
        return true;
      }
      if (verbose) log("  - pendingFinish unavailable, proceedEof failed");
      return false;
    }
    dropLast();
    if (stack.nodeId != pendingFinish) {
      replace(pendingFinish);
    }
    if (verbose) printStack();
    while (stack.prev != null) {
      boolean finishNeeded = finishStep();
      if (verbose) printStack();
      if (!finishNeeded) {
        if (pendingFinish == -1) {
          return false;
        }
        dropLast();
        replace(pendingFinish);
        if (verbose) printStack();
      }
    }
    return true;
  }

  public static boolean parse(String s) {
    ExprGrammarSimpleParser parser = new ExprGrammarSimpleParser(false);
    for (int i = 0; i < s.length(); i++) {
      if (!parser.proceed(s.charAt(i))) {
        return false;
      }
    }
    return parser.proceedEof();
  }

  public static boolean parseVerbose(String s) {
    ExprGrammarSimpleParser parser = new ExprGrammarSimpleParser(true);
    for (int i = 0; i < s.length(); i++) {
      log("Proceed char at " + i + ": " + s.charAt(i));
      if (!parser.proceed(s.charAt(i))) {
        return false;
      }
    }
    log("Proceed EOF");
    return parser.proceedEof();
  }

  private static void test(String input) {
    log("Test \"" + input + "\"");
    boolean succeed = parseVerbose(input);
    log("Parsing " + (succeed ? "succeeded" : "failed"));
  }

  private static void inputLoop() {
    java.util.Scanner scanner = new java.util.Scanner(System.in);
    while (true) {
      System.out.print("> ");
      String input = scanner.nextLine();
      if (input.isEmpty()) break;
      test(input);
    }
    System.out.println("Bye~");
  }

  public static void main(String[] args) {
    test("123+456");

    inputLoop();
  }
}
