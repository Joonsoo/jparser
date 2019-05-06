package com.giyeok.jparser.parsergen.generated.simplegen;

public class LongestPriorParser {
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

  public LongestPriorParser(boolean verbose) {
    this.verbose = verbose;
    this.stack = new Stack(0, null);
    this.pendingFinish = -1;
  }

  public boolean canAccept(char c) {
    if (stack == null) return false;
    switch (stack.nodeId) {
      case 0:
        return (c == 'a') || (c == 'b');
      case 1:
        return (c == 'a') || (c == 'b');
      case 2:
        return (c == 'a') || (c == 'b');
      case 3:
        return (c == 'a') || (c == 'b');
      case 4:
        return (c == 'a');
      case 5:
        return (c == 'b');
    }
    throw new AssertionError("Unknown nodeId: " + stack.nodeId);
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

  public String nodeDescriptionOf(int nodeId) {
    switch (nodeId) {
      case 0:
        return "{•<start>}";
      case 1:
        return "{T*•T|'b'+•'b'}";
      case 2:
        return "{T*•T|'a'+•'a'}";
      case 3:
        return "{T*•T}";
      case 4:
        return "{'a'+•'a'}";
      case 5:
        return "{'b'+•'b'}";
    }
    return null;
  }

  private void replace(int newNodeId) {
    stack = new Stack(newNodeId, stack.prev);
  }

  private void append(int newNodeId) {
    stack = new Stack(newNodeId, stack);
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

  // Returns true if further finishStep is required
  private boolean finishStep() {
    if (stack == null || stack.prev == null) {
      throw new AssertionError("No edge to finish: " + stackIds());
    }
    int prev = stack.prev.nodeId;
    int last = stack.nodeId;
    if (prev == 0 && last == 1) { // (0,1)
      // ReplaceEdge(0,1,Some(0))
      pendingFinish = 0;
      return false;
    }
    if (prev == 0 && last == 2) { // (0,2)
      // ReplaceEdge(0,2,Some(0))
      pendingFinish = 0;
      return false;
    }
    if (prev == 0 && last == 3) { // (0,3)
      // ReplaceEdge(0,3,Some(0))
      pendingFinish = 0;
      return false;
    }
    if (prev == 1 && last == 5) { // (1,5)
      // ReplaceEdge(3,5,Some(3))
      dropLast();
      replace(3);
      append(5);
      pendingFinish = 3;
      return false;
    }
    if (prev == 2 && last == 4) { // (2,4)
      // ReplaceEdge(3,4,Some(3))
      dropLast();
      replace(3);
      append(4);
      pendingFinish = 3;
      return false;
    }
    if (prev == 3 && last == 4) { // (3,4)
      // ReplaceEdge(3,4,Some(3))
      pendingFinish = 3;
      return false;
    }
    if (prev == 3 && last == 5) { // (3,5)
      // ReplaceEdge(3,5,Some(3))
      pendingFinish = 3;
      return false;
    }
    throw new AssertionError("Unknown edge to finish: " + stackIds());
  }

  private boolean proceedStep(char c) {
    switch (stack.nodeId) {
      case 0:
        if ((c == 'a')) {
          // Append(0,2,Some(0))
          append(2);
          pendingFinish = 0;
          if (verbose) printStack();
          return true;
        }
        if ((c == 'b')) {
          // Append(0,1,Some(0))
          append(1);
          pendingFinish = 0;
          if (verbose) printStack();
          return true;
        }
        return false;
      case 1:
        if ((c == 'a')) {
          // Append(3,4,Some(3))
          replace(3);
          append(4);
          pendingFinish = 3;
          if (verbose) printStack();
          return true;
        }
        if ((c == 'b')) {
          // Append(1,5,Some(1))
          append(5);
          pendingFinish = 1;
          if (verbose) printStack();
          return true;
        }
        return false;
      case 2:
        if ((c == 'a')) {
          // Append(2,4,Some(2))
          append(4);
          pendingFinish = 2;
          if (verbose) printStack();
          return true;
        }
        if ((c == 'b')) {
          // Append(3,5,Some(3))
          replace(3);
          append(5);
          pendingFinish = 3;
          if (verbose) printStack();
          return true;
        }
        return false;
      case 3:
        if ((c == 'a')) {
          // Append(3,4,Some(3))
          append(4);
          pendingFinish = 3;
          if (verbose) printStack();
          return true;
        }
        if ((c == 'b')) {
          // Append(3,5,Some(3))
          append(5);
          pendingFinish = 3;
          if (verbose) printStack();
          return true;
        }
        return false;
      case 4:
        if ((c == 'a')) {
          // Finish(4)
          finish();
          if (verbose) printStack();
          return true;
        }
        return false;
      case 5:
        if ((c == 'b')) {
          // Finish(5)
          finish();
          if (verbose) printStack();
          return true;
        }
        return false;
    }
    throw new AssertionError("Unknown nodeId: " + stack.nodeId);
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
    return proceedStep(c);
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
    LongestPriorParser parser = new LongestPriorParser(false);
    for (int i = 0; i < s.length(); i++) {
      if (!parser.proceed(s.charAt(i))) {
        return false;
      }
    }
    return parser.proceedEof();
  }

  public static boolean parseVerbose(String s) {
    LongestPriorParser parser = new LongestPriorParser(true);
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

    inputLoop();
  }
}
