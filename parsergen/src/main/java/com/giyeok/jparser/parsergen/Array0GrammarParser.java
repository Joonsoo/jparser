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
      case 2:
        return (c == ' ') || (c == ']') || (c == 'a');
      case 3:
        return (c == ' ') || (c == ',');
      case 4:
        return (c == ']');
      case 5:
        return (c == ' ') || (c == ']') || (c == 'a');
      case 6:
        return (c == ' ') || (c == 'a');
      case 7:
        return (c == ' ') || (c == ']');
      case 8:
        return (c == ' ') || (c == ']');
      case 10:
        return (c == ' ');
      case 11:
        return (c == 'a');
      case 12:
        return (c == ' ') || (c == 'a');
      case 13:
        return (c == ' ') || (c == ',');
      case 14:
        return (c == ' ') || (c == ']') || (c == 'a');
      case 15:
        return (c == ' ') || (c == ',') || (c == 'a');
      case 16:
        return (c == ' ') || (c == ',');
      case 17:
        return (c == 'a');
      case 18:
        return (c == ' ') || (c == 'a');
      case 19:
        return (c == ',');
      case 20:
        return (c == ' ') || (c == ',') || (c == ']') || (c == 'a');
      case 21:
        return (c == ' ') || (c == ',') || (c == 'a');
      case 22:
        return (c == ' ') || (c == ',') || (c == 'a');
      case 23:
        return (c == ' ') || (c == 'a');
      case 24:
        return (c == ' ') || (c == ',');
      case 25:
        return (c == ' ') || (c == ',') || (c == 'a');
      case 26:
        return (c == ' ') || (c == ',');
      case 27:
        return (c == ' ') || (c == ',') || (c == 'a');
      case 28:
        return (c == ' ') || (c == ',') || (c == 'a');
      case 29:
        return (c == ' ') || (c == ',') || (c == 'a');
      case 30:
        return (c == ' ') || (c == ',') || (c == 'a');
      case 31:
        return (c == ' ') || (c == ',') || (c == 'a');
      case 32:
        return (c == ' ') || (c == ',');
      case 33:
        return (c == ' ') || (c == ',');
      case 34:
        return (c == ' ') || (c == ',') || (c == 'a');
    }
    throw new AssertionError("Unknown nodeId: " + stack.nodeId);
  }

  public String nodeDescriptionOf(int nodeId) {
    switch (nodeId) {
      case 0:
        return "{•<start>}";
      case 1:
        return "{[•([WS E ([WS , WS E])*])? WS ] | [ ([WS E ([WS , WS E])*])?•WS ] | [ ([WS E ([WS , WS E])*])? WS•]}";
      case 2:
        return "{[•([WS E ([WS , WS E])*])? WS ]}";
      case 3:
        return "{WS E•([WS , WS E])*}";
      case 4:
        return "{[ ([WS E ([WS , WS E])*])? WS•]}";
      case 5:
        return "{[•([WS E ([WS , WS E])*])? WS ] | [ ([WS E ([WS , WS E])*])?•WS ]}";
      case 6:
        return "{WS•E ([WS , WS E])* | \\u0020*•\\u0020}";
      case 7:
        return "{[ ([WS E ([WS , WS E])*])?•WS ]}";
      case 8:
        return "{[ ([WS E ([WS , WS E])*])?•WS ] | [ ([WS E ([WS , WS E])*])? WS•]}";
      case 9:
        return "{}";
      case 10:
        return "{\\u0020*•\\u0020}";
      case 11:
        return "{WS•E ([WS , WS E])*}";
      case 12:
        return "{WS ,•WS E | WS , WS•E}";
      case 13:
        return "{\\u0020*•\\u0020 | WS•, WS E}";
      case 14:
        return "{[ ([WS E ([WS , WS E])*])? WS•] | WS•E ([WS , WS E])* | \\u0020*•\\u0020}";
      case 15:
        return "{WS•E ([WS , WS E])* | WS E•([WS , WS E])* | \\u0020*•\\u0020}";
      case 16:
        return "{WS E•([WS , WS E])* | \\u0020*•\\u0020}";
      case 17:
        return "{WS , WS•E}";
      case 18:
        return "{WS ,•WS E}";
      case 19:
        return "{WS•, WS E}";
      case 20:
        return "{[ ([WS E ([WS , WS E])*])? WS•] | WS•E ([WS , WS E])* | WS E•([WS , WS E])* | \\u0020*•\\u0020}";
      case 21:
        return "{\\u0020*•\\u0020 | WS•, WS E | WS ,•WS E | WS , WS•E}";
      case 22:
        return "{([WS , WS E])*•([WS , WS E]) | WS , WS•E}";
      case 23:
        return "{\\u0020*•\\u0020 | WS ,•WS E}";
      case 24:
        return "{([WS , WS E])*•([WS , WS E])}";
      case 25:
        return "{\\u0020*•\\u0020 | ([WS , WS E])*•([WS , WS E]) | WS•, WS E | WS ,•WS E | WS , WS•E}";
      case 26:
        return "{([WS , WS E])*•([WS , WS E]) | WS•, WS E}";
      case 27:
        return "{\\u0020*•\\u0020 | ([WS , WS E])*•([WS , WS E]) | WS ,•WS E}";
      case 28:
        return "{\\u0020*•\\u0020 | WS•, WS E | WS , WS•E}";
      case 29:
        return "{([WS , WS E])*•([WS , WS E]) | WS ,•WS E | WS , WS•E}";
      case 30:
        return "{\\u0020*•\\u0020 | ([WS , WS E])*•([WS , WS E]) | WS•, WS E | WS , WS•E}";
      case 31:
        return "{([WS , WS E])*•([WS , WS E]) | WS ,•WS E}";
      case 32:
        return "{\\u0020*•\\u0020 | ([WS , WS E])*•([WS , WS E])}";
      case 33:
        return "{\\u0020*•\\u0020 | ([WS , WS E])*•([WS , WS E]) | WS•, WS E}";
      case 34:
        return "{\\u0020*•\\u0020 | WS•, WS E | WS ,•WS E}";
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
    if (prev == 0 && last == 1) { // (0,1)
      // ReplaceEdge(0,8,Some(0))
      replace(8);
      pendingFinish = 0;
      return false;
    }
    if (prev == 0 && last == 2) { // (0,2)
      // ReplaceEdge(0,8,None)
      replace(8);
      pendingFinish = -1;
      return false;
    }
    if (prev == 0 && last == 4) { // (0,4)
      // DropLast(0)
      dropLast();
      return true;
    }
    if (prev == 0 && last == 5) { // (0,5)
      // ReplaceEdge(0,8,None)
      replace(8);
      pendingFinish = -1;
      return false;
    }
    if (prev == 0 && last == 7) { // (0,7)
      // ReplaceEdge(0,4,None)
      replace(4);
      pendingFinish = -1;
      return false;
    }
    if (prev == 0 && last == 8) { // (0,8)
      // ReplaceEdge(0,4,Some(0))
      replace(4);
      pendingFinish = 0;
      return false;
    }
    if (prev == 0 && last == 9) { // (0,9)
      // DropLast(9)
      dropLast();
      replace(9);
      return true;
    }
    if (prev == 2 && last == 3) { // (2,3)
      // DropLast(2)
      dropLast();
      return true;
    }
    if (prev == 2 && last == 4) { // (2,4)
      // DropLast(9)
      dropLast();
      replace(9);
      return true;
    }
    if (prev == 2 && last == 9) { // (2,9)
      // DropLast(9)
      dropLast();
      replace(9);
      return true;
    }
    if (prev == 2 && last == 10) { // (2,10)
      // ReplaceEdge(2,14,None)
      replace(14);
      pendingFinish = -1;
      return false;
    }
    if (prev == 2 && last == 11) { // (2,11)
      // ReplaceEdge(2,3,Some(2))
      replace(3);
      pendingFinish = 2;
      return false;
    }
    if (prev == 2 && last == 14) { // (2,14)
      // ReplaceEdge(2,20,Some(2))
      replace(20);
      pendingFinish = 2;
      return false;
    }
    if (prev == 2 && last == 16) { // (2,16)
      // ReplaceEdge(2,14,Some(2))
      replace(14);
      pendingFinish = 2;
      return false;
    }
    if (prev == 2 && last == 20) { // (2,20)
      // ReplaceEdge(2,20,Some(2))
      replace(20);
      pendingFinish = 2;
      return false;
    }
    if (prev == 3 && last == 9) { // (3,9)
      // DropLast(9)
      dropLast();
      replace(9);
      return true;
    }
    if (prev == 3 && last == 10) { // (3,10)
      // ReplaceEdge(3,13,None)
      replace(13);
      pendingFinish = -1;
      return false;
    }
    if (prev == 3 && last == 12) { // (3,12)
      // ReplaceEdge(3,22,Some(3))
      replace(22);
      pendingFinish = 3;
      return false;
    }
    if (prev == 3 && last == 13) { // (3,13)
      // ReplaceEdge(3,21,None)
      replace(21);
      pendingFinish = -1;
      return false;
    }
    if (prev == 3 && last == 17) { // (3,17)
      // ReplaceEdge(3,24,Some(3))
      replace(24);
      pendingFinish = 3;
      return false;
    }
    if (prev == 3 && last == 18) { // (3,18)
      // ReplaceEdge(3,17,None)
      replace(17);
      pendingFinish = -1;
      return false;
    }
    if (prev == 3 && last == 19) { // (3,19)
      // ReplaceEdge(3,12,None)
      replace(12);
      pendingFinish = -1;
      return false;
    }
    if (prev == 3 && last == 21) { // (3,21)
      // ReplaceEdge(3,25,Some(3))
      replace(25);
      pendingFinish = 3;
      return false;
    }
    if (prev == 3 && last == 22) { // (3,22)
      // ReplaceEdge(3,24,Some(3))
      replace(24);
      pendingFinish = 3;
      return false;
    }
    if (prev == 3 && last == 23) { // (3,23)
      // ReplaceEdge(3,28,None)
      replace(28);
      pendingFinish = -1;
      return false;
    }
    if (prev == 3 && last == 24) { // (3,24)
      // ReplaceEdge(3,24,Some(3))
      replace(24);
      pendingFinish = 3;
      return false;
    }
    if (prev == 3 && last == 25) { // (3,25)
      // ReplaceEdge(3,25,Some(3))
      replace(25);
      pendingFinish = 3;
      return false;
    }
    if (prev == 3 && last == 26) { // (3,26)
      // ReplaceEdge(3,29,Some(3))
      replace(29);
      pendingFinish = 3;
      return false;
    }
    if (prev == 3 && last == 27) { // (3,27)
      // ReplaceEdge(3,30,Some(3))
      replace(30);
      pendingFinish = 3;
      return false;
    }
    if (prev == 3 && last == 28) { // (3,28)
      // ReplaceEdge(3,25,Some(3))
      replace(25);
      pendingFinish = 3;
      return false;
    }
    if (prev == 3 && last == 29) { // (3,29)
      // ReplaceEdge(3,22,Some(3))
      replace(22);
      pendingFinish = 3;
      return false;
    }
    if (prev == 3 && last == 30) { // (3,30)
      // ReplaceEdge(3,25,Some(3))
      replace(25);
      pendingFinish = 3;
      return false;
    }
    if (prev == 3 && last == 31) { // (3,31)
      // ReplaceEdge(3,22,Some(3))
      replace(22);
      pendingFinish = 3;
      return false;
    }
    if (prev == 3 && last == 32) { // (3,32)
      // ReplaceEdge(3,33,Some(3))
      replace(33);
      pendingFinish = 3;
      return false;
    }
    if (prev == 3 && last == 33) { // (3,33)
      // ReplaceEdge(3,25,Some(3))
      replace(25);
      pendingFinish = 3;
      return false;
    }
    if (prev == 5 && last == 3) { // (5,3)
      // DropLast(2)
      dropLast();
      replace(2);
      return true;
    }
    if (prev == 5 && last == 6) { // (5,6)
      // ReplaceEdge(5,15,Some(5))
      replace(15);
      pendingFinish = 5;
      return false;
    }
    if (prev == 5 && last == 9) { // (5,9)
      // DropLast(9)
      dropLast();
      replace(9);
      return true;
    }
    if (prev == 5 && last == 10) { // (5,10)
      // ReplaceEdge(5,6,Some(7))
      replace(6);
      pendingFinish = 7;
      return false;
    }
    if (prev == 5 && last == 11) { // (5,11)
      // ReplaceEdge(2,3,Some(2))
      dropLast();
      replace(2);
      append(3);
      pendingFinish = 2;
      return false;
    }
    if (prev == 5 && last == 15) { // (5,15)
      // ReplaceEdge(5,15,Some(5))
      replace(15);
      pendingFinish = 5;
      return false;
    }
    if (prev == 5 && last == 16) { // (5,16)
      // ReplaceEdge(5,6,Some(5))
      replace(6);
      pendingFinish = 5;
      return false;
    }
    if (prev == 7 && last == 10) { // (7,10)
      // ReplaceEdge(7,10,Some(7))
      replace(10);
      pendingFinish = 7;
      return false;
    }
    if (prev == 18 && last == 10) { // (18,10)
      // ReplaceEdge(18,10,Some(18))
      replace(10);
      pendingFinish = 18;
      return false;
    }
    if (prev == 24 && last == 9) { // (24,9)
      // DropLast(9)
      dropLast();
      replace(9);
      return true;
    }
    if (prev == 24 && last == 10) { // (24,10)
      // ReplaceEdge(24,13,None)
      replace(13);
      pendingFinish = -1;
      return false;
    }
    if (prev == 24 && last == 12) { // (24,12)
      // ReplaceEdge(24,17,Some(24))
      replace(17);
      pendingFinish = 24;
      return false;
    }
    if (prev == 24 && last == 13) { // (24,13)
      // ReplaceEdge(24,21,None)
      replace(21);
      pendingFinish = -1;
      return false;
    }
    if (prev == 24 && last == 17) { // (24,17)
      // DropLast(24)
      dropLast();
      return true;
    }
    if (prev == 24 && last == 18) { // (24,18)
      // ReplaceEdge(24,17,None)
      replace(17);
      pendingFinish = -1;
      return false;
    }
    if (prev == 24 && last == 19) { // (24,19)
      // ReplaceEdge(24,12,None)
      replace(12);
      pendingFinish = -1;
      return false;
    }
    if (prev == 24 && last == 21) { // (24,21)
      // ReplaceEdge(24,21,Some(24))
      replace(21);
      pendingFinish = 24;
      return false;
    }
    if (prev == 24 && last == 23) { // (24,23)
      // ReplaceEdge(24,28,None)
      replace(28);
      pendingFinish = -1;
      return false;
    }
    if (prev == 24 && last == 28) { // (24,28)
      // ReplaceEdge(24,21,Some(24))
      replace(21);
      pendingFinish = 24;
      return false;
    }
    if (prev == 31 && last == 9) { // (31,9)
      // DropLast(9)
      dropLast();
      replace(9);
      return true;
    }
    if (prev == 31 && last == 10) { // (31,10)
      // ReplaceEdge(31,13,Some(18))
      replace(13);
      pendingFinish = 18;
      return false;
    }
    if (prev == 31 && last == 13) { // (31,13)
      // ReplaceEdge(31,34,Some(18))
      replace(34);
      pendingFinish = 18;
      return false;
    }
    if (prev == 31 && last == 19) { // (31,19)
      // ReplaceEdge(24,12,None)
      dropLast();
      replace(24);
      append(12);
      pendingFinish = -1;
      return false;
    }
    if (prev == 31 && last == 23) { // (31,23)
      // ReplaceEdge(31,13,Some(18))
      replace(13);
      pendingFinish = 18;
      return false;
    }
    if (prev == 31 && last == 34) { // (31,34)
      // ReplaceEdge(31,34,Some(18))
      replace(34);
      pendingFinish = 18;
      return false;
    }
    throw new AssertionError("Unknown edge to finish: " + stackIds());
  }

  private boolean finish() {
    if (stack.prev == null) {
      return false;
    }
    while (finishStep()) {
      if (verbose) {
        printStack();
      }
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
      if (verbose) {
        log("  - already finished");
      }
      return false;
    }
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
      case 2:
        if ((c == ' ')) {
          // Append(2,14,None)
          append(14);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == ']')) {
          // Finish(9)
          replace(9);
          if (verbose) printStack();
          finish();
          if (verbose) printStack();
          return true;
        }
        if ((c == 'a')) {
          // Append(2,3,Some(2))
          append(3);
          pendingFinish = 2;
          if (verbose) printStack();
          return true;
        }
        return false;
      case 3:
        if ((c == ' ')) {
          // Append(3,13,None)
          append(13);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == ',')) {
          // Append(3,12,None)
          append(12);
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
      case 5:
        if ((c == ' ')) {
          // Append(5,6,Some(7))
          append(6);
          pendingFinish = 7;
          if (verbose) printStack();
          return true;
        }
        if ((c == ']')) {
          // Finish(9)
          replace(9);
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
      case 6:
        if ((c == ' ')) {
          // Finish(10)
          replace(10);
          if (verbose) printStack();
          finish();
          if (verbose) printStack();
          return true;
        }
        if ((c == 'a')) {
          // Finish(11)
          replace(11);
          if (verbose) printStack();
          finish();
          if (verbose) printStack();
          return true;
        }
        return false;
      case 7:
        if ((c == ' ')) {
          // Append(7,10,Some(7))
          append(10);
          pendingFinish = 7;
          if (verbose) printStack();
          return true;
        }
        if ((c == ']')) {
          // Finish(9)
          replace(9);
          if (verbose) printStack();
          finish();
          if (verbose) printStack();
          return true;
        }
        return false;
      case 8:
        if ((c == ' ')) {
          // Append(7,10,Some(7))
          replace(7);
          append(10);
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
      case 10:
        if ((c == ' ')) {
          // Finish(10)
          finish();
          if (verbose) printStack();
          return true;
        }
        return false;
      case 11:
        if ((c == 'a')) {
          // Finish(11)
          finish();
          if (verbose) printStack();
          return true;
        }
        return false;
      case 12:
        if ((c == ' ')) {
          // Append(18,10,Some(18))
          replace(18);
          append(10);
          pendingFinish = 18;
          if (verbose) printStack();
          return true;
        }
        if ((c == 'a')) {
          // Finish(17)
          replace(17);
          if (verbose) printStack();
          finish();
          if (verbose) printStack();
          return true;
        }
        return false;
      case 13:
        if ((c == ' ')) {
          // Finish(10)
          replace(10);
          if (verbose) printStack();
          finish();
          if (verbose) printStack();
          return true;
        }
        if ((c == ',')) {
          // Finish(19)
          replace(19);
          if (verbose) printStack();
          finish();
          if (verbose) printStack();
          return true;
        }
        return false;
      case 14:
        if ((c == ' ')) {
          // Finish(10)
          replace(10);
          if (verbose) printStack();
          finish();
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
          // Finish(11)
          replace(11);
          if (verbose) printStack();
          finish();
          if (verbose) printStack();
          return true;
        }
        return false;
      case 15:
        if ((c == ' ')) {
          // Finish(16)
          replace(16);
          if (verbose) printStack();
          finish();
          if (verbose) printStack();
          return true;
        }
        if ((c == ',')) {
          // Append(3,12,None)
          replace(3);
          append(12);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == 'a')) {
          // Finish(11)
          replace(11);
          if (verbose) printStack();
          finish();
          if (verbose) printStack();
          return true;
        }
        return false;
      case 16:
        if ((c == ' ')) {
          // Finish(16)
          finish();
          if (verbose) printStack();
          return true;
        }
        if ((c == ',')) {
          // Append(3,12,None)
          replace(3);
          append(12);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        return false;
      case 17:
        if ((c == 'a')) {
          // Finish(17)
          finish();
          if (verbose) printStack();
          return true;
        }
        return false;
      case 18:
        if ((c == ' ')) {
          // Append(18,10,Some(18))
          append(10);
          pendingFinish = 18;
          if (verbose) printStack();
          return true;
        }
        if ((c == 'a')) {
          // Finish(9)
          replace(9);
          if (verbose) printStack();
          finish();
          if (verbose) printStack();
          return true;
        }
        return false;
      case 19:
        if ((c == ',')) {
          // Finish(19)
          finish();
          if (verbose) printStack();
          return true;
        }
        return false;
      case 20:
        if ((c == ' ')) {
          // Finish(16)
          replace(16);
          if (verbose) printStack();
          finish();
          if (verbose) printStack();
          return true;
        }
        if ((c == ',')) {
          // Append(3,12,None)
          replace(3);
          append(12);
          pendingFinish = -1;
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
          // Finish(11)
          replace(11);
          if (verbose) printStack();
          finish();
          if (verbose) printStack();
          return true;
        }
        return false;
      case 21:
        if ((c == ' ')) {
          // Finish(23)
          replace(23);
          if (verbose) printStack();
          finish();
          if (verbose) printStack();
          return true;
        }
        if ((c == ',')) {
          // Finish(19)
          replace(19);
          if (verbose) printStack();
          finish();
          if (verbose) printStack();
          return true;
        }
        if ((c == 'a')) {
          // Finish(17)
          replace(17);
          if (verbose) printStack();
          finish();
          if (verbose) printStack();
          return true;
        }
        return false;
      case 22:
        if ((c == ' ')) {
          // Append(24,13,None)
          replace(24);
          append(13);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == ',')) {
          // Append(24,12,None)
          replace(24);
          append(12);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == 'a')) {
          // Finish(17)
          replace(17);
          if (verbose) printStack();
          finish();
          if (verbose) printStack();
          return true;
        }
        return false;
      case 23:
        if ((c == ' ')) {
          // Finish(23)
          finish();
          if (verbose) printStack();
          return true;
        }
        if ((c == 'a')) {
          // Finish(9)
          replace(9);
          if (verbose) printStack();
          finish();
          if (verbose) printStack();
          return true;
        }
        return false;
      case 24:
        if ((c == ' ')) {
          // Append(24,13,None)
          append(13);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == ',')) {
          // Append(24,12,None)
          append(12);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        return false;
      case 25:
        if ((c == ' ')) {
          // Finish(27)
          replace(27);
          if (verbose) printStack();
          finish();
          if (verbose) printStack();
          return true;
        }
        if ((c == ',')) {
          // Finish(26)
          replace(26);
          if (verbose) printStack();
          finish();
          if (verbose) printStack();
          return true;
        }
        if ((c == 'a')) {
          // Finish(17)
          replace(17);
          if (verbose) printStack();
          finish();
          if (verbose) printStack();
          return true;
        }
        return false;
      case 26:
        if ((c == ' ')) {
          // Append(24,13,None)
          replace(24);
          append(13);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == ',')) {
          // Finish(26)
          finish();
          if (verbose) printStack();
          return true;
        }
        return false;
      case 27:
        if ((c == ' ')) {
          // Finish(27)
          finish();
          if (verbose) printStack();
          return true;
        }
        if ((c == ',')) {
          // Append(24,12,None)
          replace(24);
          append(12);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == 'a')) {
          // Finish(9)
          replace(9);
          if (verbose) printStack();
          finish();
          if (verbose) printStack();
          return true;
        }
        return false;
      case 28:
        if ((c == ' ')) {
          // Finish(10)
          replace(10);
          if (verbose) printStack();
          finish();
          if (verbose) printStack();
          return true;
        }
        if ((c == ',')) {
          // Finish(19)
          replace(19);
          if (verbose) printStack();
          finish();
          if (verbose) printStack();
          return true;
        }
        if ((c == 'a')) {
          // Finish(17)
          replace(17);
          if (verbose) printStack();
          finish();
          if (verbose) printStack();
          return true;
        }
        return false;
      case 29:
        if ((c == ' ')) {
          // Append(31,13,Some(18))
          replace(31);
          append(13);
          pendingFinish = 18;
          if (verbose) printStack();
          return true;
        }
        if ((c == ',')) {
          // Append(24,12,None)
          replace(24);
          append(12);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == 'a')) {
          // Finish(17)
          replace(17);
          if (verbose) printStack();
          finish();
          if (verbose) printStack();
          return true;
        }
        return false;
      case 30:
        if ((c == ' ')) {
          // Finish(32)
          replace(32);
          if (verbose) printStack();
          finish();
          if (verbose) printStack();
          return true;
        }
        if ((c == ',')) {
          // Finish(26)
          replace(26);
          if (verbose) printStack();
          finish();
          if (verbose) printStack();
          return true;
        }
        if ((c == 'a')) {
          // Finish(17)
          replace(17);
          if (verbose) printStack();
          finish();
          if (verbose) printStack();
          return true;
        }
        return false;
      case 31:
        if ((c == ' ')) {
          // Append(31,13,Some(18))
          append(13);
          pendingFinish = 18;
          if (verbose) printStack();
          return true;
        }
        if ((c == ',')) {
          // Append(24,12,None)
          replace(24);
          append(12);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == 'a')) {
          // Finish(9)
          replace(9);
          if (verbose) printStack();
          finish();
          if (verbose) printStack();
          return true;
        }
        return false;
      case 32:
        if ((c == ' ')) {
          // Finish(32)
          finish();
          if (verbose) printStack();
          return true;
        }
        if ((c == ',')) {
          // Append(24,12,None)
          replace(24);
          append(12);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        return false;
      case 33:
        if ((c == ' ')) {
          // Finish(32)
          replace(32);
          if (verbose) printStack();
          finish();
          if (verbose) printStack();
          return true;
        }
        if ((c == ',')) {
          // Finish(26)
          replace(26);
          if (verbose) printStack();
          finish();
          if (verbose) printStack();
          return true;
        }
        return false;
      case 34:
        if ((c == ' ')) {
          // Finish(23)
          replace(23);
          if (verbose) printStack();
          finish();
          if (verbose) printStack();
          return true;
        }
        if ((c == ',')) {
          // Finish(19)
          replace(19);
          if (verbose) printStack();
          finish();
          if (verbose) printStack();
          return true;
        }
        if ((c == 'a')) {
          // Finish(9)
          replace(9);
          if (verbose) printStack();
          finish();
          if (verbose) printStack();
          return true;
        }
        return false;
    }
    throw new AssertionError("Unknown nodeId: " + stack.nodeId);
  }

  public boolean proceedEof() {
    if (stack == null) {
      if (verbose) {
        log("  - already finished");
        return true;
      }
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
    boolean succeed = parseVerbose("[ a,a, a, a,  a]");
    log("Parsing " + (succeed ? "succeeded" : "failed"));
  }
}
