package com.giyeok.jparser.parsergen;

public class SuperSimpleParser {
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

  public SuperSimpleParser(boolean verbose) {
    this.verbose = verbose;
    this.stack = new Stack(0, null);
    this.pendingFinish = -1;
  }

  public boolean canAccept(char c) {
    switch (stack.nodeId) {
      case 0:
        return (c == 'x');
      case 1:
        return (c == 'a');
      case 2:
        return (c == 'b');
      case 3:
        return (c == 'a');
      case 4:
        return (c == 'y');
      case 5:
        return (c == 'a');
      case 6:
        return (c == 'y');
      case 7:
        return (c == 'y');
    }
    throw new AssertionError("Unknown nodeId: " + stack.nodeId);
  }

  public String nodeDescriptionOf(int nodeId) {
    switch (nodeId) {
      case 0:
        return "{•<start>}";
      case 1:
        return "{x•A y | x•B y}";
      case 2:
        return "{a•b}";
      case 3:
        return "{x•A y}";
      case 4:
        return "{x A•y | x B•y}";
      case 5:
        return "{x•B y}";
      case 6:
        return "{x A•y}";
      case 7:
        return "{x B•y}";
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
    if (prev == 0 && last == 1) {
      // ReplaceEdge(0,4,None)
      replace(4);
      pendingFinish = -1;
      return false;
    }
    if (prev == 0 && last == 3) {
      // ReplaceEdge(0,6,None)
      replace(6);
      pendingFinish = -1;
      return false;
    }
    if (prev == 0 && last == 4) {
      // DropLast(0)
      dropLast();
      return true;
    }
    if (prev == 0 && last == 5) {
      // ReplaceEdge(0,7,None)
      replace(7);
      pendingFinish = -1;
      return false;
    }
    if (prev == 0 && last == 6) {
      // DropLast(0)
      dropLast();
      return true;
    }
    if (prev == 0 && last == 7) {
      // DropLast(0)
      dropLast();
      return true;
    }
    if (prev == 1 && last == 2) {
      // DropLast(5)
      dropLast();
      replace(5);
      return true;
    }
    if (prev == 5 && last == 2) {
      // DropLast(5)
      dropLast();
      return true;
    }
    throw new AssertionError("Unknown edge to finish: " + stackIds());
  }

  private boolean finish() {
    do {
      if (stack.prev == null) {
        return false;
      }
    } while (finishStep());
    return true;
  }

  private void dropLast() {
    stack = stack.prev;
  }

  public String stackIds() {
    return stackIds(stack);
  }

  private String stackIds(Stack stack) {
    if (stack.prev == null) return "" + stack.nodeId;
    else return stackIds(stack.prev) + " " + stack.nodeId;
  }

  public String stackDescription() {
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
    log("  " + stackIds() + " " + stackDescription());
  }

  public boolean proceed(char c) {
    if (!canAccept(c)) {
      if (verbose) {
        log("  - cannot accept " + c + ", try pendingFinish");
      }
      if (pendingFinish == -1) {
        if (verbose) {
          log("  - pendingFinish unavailable, proceed failed");
        }
        return false;
      }
      dropLast();
      if (stack.nodeId != pendingFinish) {
        replace(pendingFinish);
      }
      if (verbose) {
        printStack();
      }
      if (!finish()) {
        return false;
      }
      return proceed(c);
    }
    switch (stack.nodeId) {
      case 0:
        if ((c == 'x')) {
          // Append(0,1,None)
          append(1);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        return false;
      case 1:
        if ((c == 'a')) {
          // Append(1,2,Some(3))
          append(2);
          pendingFinish = 3;
          if (verbose) printStack();
          return true;
        }
        return false;
      case 2:
        if ((c == 'b')) {
          // Finish(2)
          finish();
          if (verbose) printStack();
          return true;
        }
        return false;
      case 3:
        if ((c == 'a')) {
          // Finish(3)
          finish();
          if (verbose) printStack();
          return true;
        }
        return false;
      case 4:
        if ((c == 'y')) {
          // Finish(4)
          finish();
          if (verbose) printStack();
          return true;
        }
        return false;
      case 5:
        if ((c == 'a')) {
          // Append(5,2,None)
          append(2);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        return false;
      case 6:
        if ((c == 'y')) {
          // Finish(6)
          finish();
          if (verbose) printStack();
          return true;
        }
        return false;
      case 7:
        if ((c == 'y')) {
          // Finish(7)
          finish();
          if (verbose) printStack();
          return true;
        }
        return false;
    }
    throw new AssertionError("Unknown nodeId: " + stack.nodeId);
  }

  public boolean proceedEof() {
    if (verbose) {
      log("  - proceeding eof");
    }
    if (pendingFinish == -1) {
      if (stack.prev == null && stack.nodeId == 0) {
        return true;
      }
      if (verbose) {
        log("  - pendingFinish unavailable, proceedEof failed");
      }
      return false;
    }
    dropLast();
    if (stack.nodeId != pendingFinish) {
      replace(pendingFinish);
    }
    while (stack.prev != null) {
      boolean finishNeeded = finishStep();
      if (verbose) {
        printStack();
      }
      if (!finishNeeded) {
        if (pendingFinish == -1) {
          return false;
        }
        dropLast();
        replace(pendingFinish);
        if (verbose) {
          printStack();
        }
      }
    }
    return true;
  }

  public static boolean parse(String s) {
    SuperSimpleParser parser = new SuperSimpleParser(false);
    for (int i = 0; i < s.length(); i++) {
      if (!parser.proceed(s.charAt(i))) {
        return false;
      }
    }
    return parser.proceedEof();
  }

  public static boolean parseVerbose(String s) {
    SuperSimpleParser parser = new SuperSimpleParser(true);
    for (int i = 0; i < s.length(); i++) {
      log("Proceed char at " + i + ": " + s.charAt(i));
      if (!parser.proceed(s.charAt(i))) {
        return false;
      }
    }
    return parser.proceedEof();
  }

  public static void main(String[] args) {
    boolean succeed = parseVerbose("xaby");
    log("Parsing " + (succeed ? "succeeded" : "failed"));
  }
}
