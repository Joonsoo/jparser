package com.giyeok.jparser.parsergen;

public class Array0GrammarParser {
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

  public Array0GrammarParser(boolean verbose) {
    this.verbose = verbose;
    this.stack = new Stack(0, null);
    this.pendingFinish = -1;
  }

  public boolean canAccept(char c) {
    if (stack == null) return false;
    switch (stack.nodeId) {
      case 0:
        return (c == '[');
      case 1:
        return (c == ' ') || (c == ']') || (c == 'a');
      case 3:
        return (c == ' ') || (c == ',');
      case 4:
        return (c == ']');
      case 6:
        return (c == ' ') || (c == 'a');
      case 8:
        return (c == ' ') || (c == 'a');
      case 9:
        return (c == ' ') || (c == ',');
      case 11:
        return (c == ' ');
      case 12:
        return (c == ' ') || (c == ']');
      case 13:
        return (c == 'a');
      case 16:
        return (c == ' ') || (c == ',');
    }
    throw new AssertionError("Unknown nodeId: " + stack.nodeId);
  }

  public String nodeDescriptionOf(int nodeId) {
    switch (nodeId) {
      case 0:
        return "{•<start>}";
      case 1:
        return "{'['•([WS E ([WS ',' WS E])*])? WS ']'|'[' ([WS E ([WS ',' WS E])*])?•WS ']'|'[' ([WS E ([WS ',' WS E])*])? WS•']'}";
      case 2:
        return "{'['•([WS E ([WS ',' WS E])*])? WS ']'}";
      case 3:
        return "{WS E•([WS ',' WS E])*}";
      case 4:
        return "{'[' ([WS E ([WS ',' WS E])*])? WS•']'}";
      case 5:
        return "{'['•([WS E ([WS ',' WS E])*])? WS ']'|'[' ([WS E ([WS ',' WS E])*])?•WS ']'}";
      case 6:
        return "{WS•E ([WS ',' WS E])*|'\\u0020'*•'\\u0020'}";
      case 7:
        return "{'[' ([WS E ([WS ',' WS E])*])?•WS ']'}";
      case 8:
        return "{WS ','•WS E|WS ',' WS•E}";
      case 9:
        return "{'\\u0020'*•'\\u0020'|WS•',' WS E}";
      case 10:
        return "{WS•E ([WS ',' WS E])*}";
      case 11:
        return "{'\\u0020'*•'\\u0020'}";
      case 12:
        return "{'[' ([WS E ([WS ',' WS E])*])?•WS ']'|'[' ([WS E ([WS ',' WS E])*])? WS•']'}";
      case 13:
        return "{WS ',' WS•E}";
      case 14:
        return "{WS ','•WS E}";
      case 15:
        return "{WS•',' WS E}";
      case 16:
        return "{([WS ',' WS E])*•([WS ',' WS E])}";
    }
    return null;
  }

  private void replace(int newNodeId) {
    stack = new Stack(newNodeId, stack.prev);
  }

  private void append(int newNodeId) {
    stack = new Stack(newNodeId, stack);
  }

  // false를 리턴하면 더이상 finishStep을 하지 않아도 되는 상황
  // true를 리턴하면 finishStep을 계속 해야하는 상황
  private boolean finishStep() {
    if (stack == null || stack.prev == null) {
      throw new AssertionError("No edge to finish: " + stackIds());
    }
    int prev = stack.prev.nodeId;
    int last = stack.nodeId;
    if (prev == 0 && last == 2) { // (0,2)
      // ReplaceEdge(0,12,None)
      replace(12);
      pendingFinish = -1;
      return false;
    }
    if (prev == 0 && last == 4) { // (0,4)
      // DropLast(0)
      dropLast();
      return true;
    }
    if (prev == 0 && last == 7) { // (0,7)
      // ReplaceEdge(0,4,None)
      replace(4);
      pendingFinish = -1;
      return false;
    }
    if (prev == 2 && last == 3) { // (2,3)
      // DropLast(2)
      dropLast();
      return true;
    }
    if (prev == 3 && last == 11) { // (3,11)
      // ReplaceEdge(3,9,None)
      replace(9);
      pendingFinish = -1;
      return false;
    }
    if (prev == 3 && last == 13) { // (3,13)
      // ReplaceEdge(3,16,Some(3))
      replace(16);
      pendingFinish = 3;
      return false;
    }
    if (prev == 3 && last == 14) { // (3,14)
      // ReplaceEdge(3,13,None)
      replace(13);
      pendingFinish = -1;
      return false;
    }
    if (prev == 3 && last == 15) { // (3,15)
      // ReplaceEdge(3,8,None)
      replace(8);
      pendingFinish = -1;
      return false;
    }
    if (prev == 3 && last == 16) { // (3,16)
      // ReplaceEdge(3,16,Some(3))
      pendingFinish = 3;
      return false;
    }
    if (prev == 5 && last == 10) { // (5,10)
      // ReplaceEdge(2,3,Some(2))
      dropLast();
      replace(2);
      append(3);
      pendingFinish = 2;
      return false;
    }
    if (prev == 5 && last == 11) { // (5,11)
      // ReplaceEdge(5,6,Some(7))
      replace(6);
      pendingFinish = 7;
      return false;
    }
    if (prev == 7 && last == 11) { // (7,11)
      // ReplaceEdge(7,11,Some(7))
      pendingFinish = 7;
      return false;
    }
    if (prev == 14 && last == 11) { // (14,11)
      // ReplaceEdge(14,11,Some(14))
      pendingFinish = 14;
      return false;
    }
    if (prev == 16 && last == 11) { // (16,11)
      // ReplaceEdge(16,9,None)
      replace(9);
      pendingFinish = -1;
      return false;
    }
    if (prev == 16 && last == 13) { // (16,13)
      // DropLast(16)
      dropLast();
      return true;
    }
    if (prev == 16 && last == 14) { // (16,14)
      // ReplaceEdge(16,13,None)
      replace(13);
      pendingFinish = -1;
      return false;
    }
    if (prev == 16 && last == 15) { // (16,15)
      // ReplaceEdge(16,8,None)
      replace(8);
      pendingFinish = -1;
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
      log("  " + stackIds() + " " + stackDescription());
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
        if ((c == '[')) {
          // Append(0,1,None)
          append(1);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        return false;
      case 1:
        if ((c == ' ')) {
          // Append(5,6,Some(7))
          replace(5);
          append(6);
          pendingFinish = 7;
          if (verbose) printStack();
          return true;
        }
        if ((c == ']')) {
          // Finish(4)
          replace(4);
          if (verbose) printStack();
          finish();
          if (verbose) printStack();
          return true;
        }
        if ((c == 'a')) {
          // Append(2,3,Some(2))
          replace(2);
          append(3);
          pendingFinish = 2;
          if (verbose) printStack();
          return true;
        }
        return false;
      case 3:
        if ((c == ' ')) {
          // Append(3,9,None)
          append(9);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == ',')) {
          // Append(3,8,None)
          append(8);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        return false;
      case 4:
        if ((c == ']')) {
          // Finish(4)
          finish();
          if (verbose) printStack();
          return true;
        }
        return false;
      case 6:
        if ((c == ' ')) {
          // Finish(11)
          replace(11);
          if (verbose) printStack();
          finish();
          if (verbose) printStack();
          return true;
        }
        if ((c == 'a')) {
          // Finish(10)
          replace(10);
          if (verbose) printStack();
          finish();
          if (verbose) printStack();
          return true;
        }
        return false;
      case 8:
        if ((c == ' ')) {
          // Append(14,11,Some(14))
          replace(14);
          append(11);
          pendingFinish = 14;
          if (verbose) printStack();
          return true;
        }
        if ((c == 'a')) {
          // Finish(13)
          replace(13);
          if (verbose) printStack();
          finish();
          if (verbose) printStack();
          return true;
        }
        return false;
      case 9:
        if ((c == ' ')) {
          // Finish(11)
          replace(11);
          if (verbose) printStack();
          finish();
          if (verbose) printStack();
          return true;
        }
        if ((c == ',')) {
          // Finish(15)
          replace(15);
          if (verbose) printStack();
          finish();
          if (verbose) printStack();
          return true;
        }
        return false;
      case 11:
        if ((c == ' ')) {
          // Finish(11)
          finish();
          if (verbose) printStack();
          return true;
        }
        return false;
      case 12:
        if ((c == ' ')) {
          // Append(7,11,Some(7))
          replace(7);
          append(11);
          pendingFinish = 7;
          if (verbose) printStack();
          return true;
        }
        if ((c == ']')) {
          // Finish(4)
          replace(4);
          if (verbose) printStack();
          finish();
          if (verbose) printStack();
          return true;
        }
        return false;
      case 13:
        if ((c == 'a')) {
          // Finish(13)
          finish();
          if (verbose) printStack();
          return true;
        }
        return false;
      case 16:
        if ((c == ' ')) {
          // Append(16,9,None)
          append(9);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == ',')) {
          // Append(16,8,None)
          append(8);
          pendingFinish = -1;
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
    Array0GrammarParser parser = new Array0GrammarParser(false);
    for (int i = 0; i < s.length(); i++) {
      if (!parser.proceed(s.charAt(i))) {
        return false;
      }
    }
    return parser.proceedEof();
  }

  public static boolean parseVerbose(String s) {
    Array0GrammarParser parser = new Array0GrammarParser(true);
    for (int i = 0; i < s.length(); i++) {
      log("Proceed char at " + i + ": " + s.charAt(i));
      if (!parser.proceed(s.charAt(i))) {
        return false;
      }
    }
    log("Proceed EOF");
    return parser.proceedEof();
  }

  public static void main(String[] args) {
    boolean succeed;

    log("Test \"[a,a,a]\"");
    succeed = parseVerbose("[a,a,a]");
    log("Parsing " + (succeed ? "succeeded" : "failed"));

    java.util.Scanner scanner = new java.util.Scanner(System.in);
    String input;
    while (true) {
      System.out.print("> ");
      input = scanner.nextLine();
      if (input.isEmpty()) break;
      System.out.println("Input: \"" + input + "\"");
      succeed = parseVerbose(input);
      log("Parsing " + (succeed ? "succeeded" : "failed"));
    }
    System.out.println("Bye~");
  }
}
