package com.giyeok.jparser.parsergen.generated.simplegen;

public class CustomJsonParser {
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

  public CustomJsonParser(boolean verbose) {
    this.verbose = verbose;
    this.stack = new Stack(0, null);
    this.pendingFinish = -1;
  }

  public boolean canAccept(char c) {
    if (stack == null) return false;
    switch (stack.nodeId) {
      case 0:
        return ('\t' <= c && c <= '\n')
            || (c == '\r')
            || (c == ' ')
            || (c == '"')
            || (c == '-')
            || (c == '0')
            || ('1' <= c && c <= '9')
            || (c == '[')
            || (c == 'f')
            || (c == 'n')
            || (c == 't')
            || (c == '{');
      case 1:
        return ('\t' <= c && c <= '\n')
            || (c == '\r')
            || (c == ' ')
            || (c == '"')
            || (c == '-')
            || (c == '0')
            || ('1' <= c && c <= '9')
            || (c == '[')
            || (c == 'f')
            || (c == 'n')
            || (c == 't')
            || (c == '{');
      case 2:
        return (c == ' ')
            || ('0' <= c && c <= '9')
            || ('A' <= c && c <= 'Z')
            || ('a' <= c && c <= 'z')
            || (c == '"')
            || (c == '\\');
      case 3:
        return ('\t' <= c && c <= '\n')
            || (c == '\r')
            || (c == ' ')
            || (c == '"')
            || (c == '-')
            || (c == '0')
            || ('1' <= c && c <= '9')
            || (c == '[')
            || (c == ']')
            || (c == 'f')
            || (c == 'n')
            || (c == 't')
            || (c == '{');
      case 4:
        return ('\t' <= c && c <= '\n')
            || (c == '\r')
            || (c == ' ')
            || (c == '.')
            || ('0' <= c && c <= '9')
            || (c == 'E')
            || (c == 'e');
      case 5:
        return (c == 'r');
      case 6:
        return (c == '0') || ('1' <= c && c <= '9');
      case 7:
        return (c == 'u');
      case 8:
        return ('\t' <= c && c <= '\n')
            || (c == '\r')
            || (c == ' ')
            || (c == '.')
            || (c == 'E')
            || (c == 'e');
      case 9:
        return ('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ') || (c == '"') || (c == '}');
      case 10:
        return (c == 'a');
      case 11:
        return ('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ');
      case 12:
        return ('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ');
      case 14:
        return ('0' <= c && c <= '9');
      case 15:
        return (c == 'E') || (c == 'e');
      case 16:
        return (c == '+') || (c == '-') || ('0' <= c && c <= '9');
      case 18:
        return (c == ' ')
            || ('0' <= c && c <= '9')
            || ('A' <= c && c <= 'Z')
            || (c == 'a')
            || ('c' <= c && c <= 'm')
            || ('o' <= c && c <= 'q')
            || (c == 's')
            || ('v' <= c && c <= 'z')
            || (c == '"')
            || (c == '/')
            || (c == '\\')
            || (c == 'b')
            || (c == 'n')
            || (c == 'r')
            || (c == 't')
            || (c == 'u');
      case 19:
        return (c == '"');
      case 20:
        return (c == ' ')
            || ('0' <= c && c <= '9')
            || ('A' <= c && c <= 'Z')
            || ('a' <= c && c <= 'z')
            || (c == '\\');
      case 22:
        return (c == '.') || ('0' <= c && c <= '9') || (c == 'E') || (c == 'e');
      case 23:
        return (c == '.') || (c == 'E') || (c == 'e');
      case 24:
        return ('0' <= c && c <= '9');
      case 25:
        return ('0' <= c && c <= '9');
      case 32:
        return (c == '"')
            || (c == '/')
            || (c == '\\')
            || (c == 'b')
            || (c == 'n')
            || (c == 'r')
            || (c == 't')
            || (c == 'u');
      case 33:
        return (c == '"')
            || (c == '/')
            || (c == '\\')
            || (c == 'b')
            || (c == 'n')
            || (c == 'r')
            || (c == 't')
            || (c == 'u');
      case 34:
        return (c == '"')
            || (c == '/')
            || (c == '\\')
            || (c == 'b')
            || (c == 'n')
            || (c == 'r')
            || (c == 't')
            || (c == 'u');
      case 35:
        return ('0' <= c && c <= '9') || ('A' <= c && c <= 'F') || ('a' <= c && c <= 'f');
      case 36:
        return ('0' <= c && c <= '9');
      case 38:
        return ('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ') || (c == ',') || (c == ']');
      case 39:
        return (c == 'l');
      case 40:
        return (c == 'u');
      case 41:
        return ('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ') || (c == ':');
      case 42:
        return (c == '"')
            || (c == '-')
            || (c == '0')
            || ('1' <= c && c <= '9')
            || (c == '[')
            || (c == ']')
            || (c == 'f')
            || (c == 'n')
            || (c == 't')
            || (c == '{');
      case 43:
        return (c == 'l');
      case 44:
        return (c == '"') || (c == '}');
      case 46:
        return ('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ') || (c == ',');
      case 49:
        return ('\t' <= c && c <= '\n')
            || (c == '\r')
            || (c == ' ')
            || (c == '"')
            || (c == '-')
            || (c == '0')
            || ('1' <= c && c <= '9')
            || (c == '[')
            || (c == 'f')
            || (c == 'n')
            || (c == 't')
            || (c == '{');
      case 50:
        return (c == ']');
      case 52:
        return (c == ':');
      case 55:
        return (c == '"')
            || (c == '-')
            || (c == '0')
            || ('1' <= c && c <= '9')
            || (c == '[')
            || (c == 'f')
            || (c == 'n')
            || (c == 't')
            || (c == '{');
      case 56:
        return ('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ') || (c == 'E') || (c == 'e');
      case 57:
        return (c == 'l');
      case 58:
        return ('0' <= c && c <= '9') || ('A' <= c && c <= 'F') || ('a' <= c && c <= 'f');
      case 59:
        return (c == 's');
      case 60:
        return (c == 'e');
      case 61:
        return ('\t' <= c && c <= '\n')
            || (c == '\r')
            || (c == ' ')
            || (c == '"')
            || (c == '-')
            || (c == '0')
            || ('1' <= c && c <= '9')
            || (c == '[')
            || (c == 'f')
            || (c == 'n')
            || (c == 't')
            || (c == '{');
      case 63:
        return (c == '"')
            || (c == '-')
            || (c == '0')
            || ('1' <= c && c <= '9')
            || (c == '[')
            || (c == 'f')
            || (c == 'n')
            || (c == 't')
            || (c == '{');
      case 64:
        return ('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ') || (c == ',');
      case 65:
        return (c == 'e');
      case 66:
        return ('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ') || (c == ']');
      case 67:
        return ('0' <= c && c <= '9') || ('A' <= c && c <= 'F') || ('a' <= c && c <= 'f');
      case 68:
        return ('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ') || (c == ',') || (c == '}');
      case 70:
        return ('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ') || (c == ',');
      case 73:
        return ('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ') || (c == '"');
      case 74:
        return (c == '}');
      case 75:
        return ('0' <= c && c <= '9') || ('A' <= c && c <= 'F') || ('a' <= c && c <= 'f');
      case 78:
        return (c == '"');
      case 79:
        return ('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ') || (c == ',');
      case 80:
        return ('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ') || (c == '}');
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
        return "{WS•Elem WS|{\\t-\\n\\r\\u0020}*•{\\t-\\n\\r\\u0020}}";
      case 2:
        return "{'\"'•Char* '\"'|'\"' Char*•'\"'}";
      case 3:
        return "{'['•WS ']'|'[' WS•']'|'['•WS Elem ([WS ',' WS Elem])* WS ']'|'[' WS•Elem ([WS ',' WS Elem])* WS ']'}";
      case 4:
        return "{WS Elem•WS|int•(['.' {0-9}+])? ([{Ee} {+-}? {0-9}+])?|int (['.' {0-9}+])?•([{Ee} {+-}? {0-9}+])?|{1-9}•{0-9}+}";
      case 5:
        return "{'t'•'r' 'u' 'e'}";
      case 6:
        return "{'-'?•{0-9}|([{1-9} {0-9}+])}";
      case 7:
        return "{'n'•'u' 'l' 'l'}";
      case 8:
        return "{WS Elem•WS|int•(['.' {0-9}+])? ([{Ee} {+-}? {0-9}+])?|int (['.' {0-9}+])?•([{Ee} {+-}? {0-9}+])?}";
      case 9:
        return "{'{'•WS '}'|'{' WS•'}'|'{'•WS ObjectPair ([WS ',' WS ObjectPair])* WS '}'|'{' WS•ObjectPair ([WS ',' WS ObjectPair])* WS '}'}";
      case 10:
        return "{'f'•'a' 'l' 's' 'e'}";
      case 11:
        return "{WS Elem•WS}";
      case 12:
        return "{{\\t-\\n\\r\\u0020}*•{\\t-\\n\\r\\u0020}}";
      case 13:
        return "{int•(['.' {0-9}+])? ([{Ee} {+-}? {0-9}+])?}";
      case 14:
        return "{'.'•{0-9}+}";
      case 15:
        return "{int (['.' {0-9}+])?•([{Ee} {+-}? {0-9}+])?}";
      case 16:
        return "{{Ee}•{+-}? {0-9}+|{Ee} {+-}?•{0-9}+}";
      case 17:
        return "{'\"'•Char* '\"'}";
      case 18:
        return "{Char*•Char|'\\'•{\"/\\bnrt}|(['u' hex hex hex hex])*}";
      case 19:
        return "{'\"' Char*•'\"'}";
      case 20:
        return "{Char*•Char}";
      case 21:
        return "{WS•Elem WS}";
      case 22:
        return "{int•(['.' {0-9}+])? ([{Ee} {+-}? {0-9}+])?|int (['.' {0-9}+])?•([{Ee} {+-}? {0-9}+])?|{1-9}•{0-9}+}";
      case 23:
        return "{int•(['.' {0-9}+])? ([{Ee} {+-}? {0-9}+])?|int (['.' {0-9}+])?•([{Ee} {+-}? {0-9}+])?}";
      case 24:
        return "{{1-9}•{0-9}+}";
      case 25:
        return "{{0-9}+•{0-9}}";
      case 26:
        return "{'['•WS ']'|'['•WS Elem ([WS ',' WS Elem])* WS ']'}";
      case 27:
        return "{'[' WS•Elem ([WS ',' WS Elem])* WS ']'}";
      case 28:
        return "{'[' WS•']'}";
      case 29:
        return "{'{'•WS '}'|'{'•WS ObjectPair ([WS ',' WS ObjectPair])* WS '}'}";
      case 30:
        return "{'{' WS•ObjectPair ([WS ',' WS ObjectPair])* WS '}'}";
      case 31:
        return "{'{' WS•'}'}";
      case 32:
        return "{'\\'•{\"/\\bnrt}|(['u' hex hex hex hex])*}";
      case 33:
        return "{'\\'•{\"/\\bnrt}|(['u' hex hex hex hex])*|{\"/\\bnrt}|(['u' hex hex hex hex])*•{\"/\\bnrt}|(['u' hex hex hex hex])}";
      case 34:
        return "{{\"/\\bnrt}|(['u' hex hex hex hex])*•{\"/\\bnrt}|(['u' hex hex hex hex])}";
      case 35:
        return "{'u'•hex hex hex hex}";
      case 36:
        return "{{Ee} {+-}?•{0-9}+}";
      case 37:
        return "{{Ee}•{+-}? {0-9}+}";
      case 38:
        return "{'[' WS Elem•([WS ',' WS Elem])* WS ']'|'[' WS Elem ([WS ',' WS Elem])*•WS ']'|'[' WS Elem ([WS ',' WS Elem])* WS•']'}";
      case 39:
        return "{'f' 'a'•'l' 's' 'e'}";
      case 40:
        return "{'t' 'r'•'u' 'e'}";
      case 41:
        return "{String•WS ':' WS Elem|String WS•':' WS Elem}";
      case 42:
        return "{'[' WS•']'|'[' WS•Elem ([WS ',' WS Elem])* WS ']'}";
      case 43:
        return "{'n' 'u'•'l' 'l'}";
      case 44:
        return "{'{' WS•'}'|'{' WS•ObjectPair ([WS ',' WS ObjectPair])* WS '}'}";
      case 45:
        return "{'[' WS Elem•([WS ',' WS Elem])* WS ']'|'[' WS Elem ([WS ',' WS Elem])*•WS ']'}";
      case 46:
        return "{{\\t-\\n\\r\\u0020}*•{\\t-\\n\\r\\u0020}|WS•',' WS Elem}";
      case 47:
        return "{'[' WS Elem ([WS ',' WS Elem])*•WS ']'}";
      case 48:
        return "{'[' WS Elem•([WS ',' WS Elem])* WS ']'}";
      case 49:
        return "{WS ','•WS Elem|WS ',' WS•Elem}";
      case 50:
        return "{'[' WS Elem ([WS ',' WS Elem])* WS•']'}";
      case 51:
        return "{String•WS ':' WS Elem}";
      case 52:
        return "{String WS•':' WS Elem}";
      case 53:
        return "{WS•',' WS Elem}";
      case 54:
        return "{WS ','•WS Elem}";
      case 55:
        return "{WS ',' WS•Elem}";
      case 56:
        return "{WS Elem•WS|int (['.' {0-9}+])?•([{Ee} {+-}? {0-9}+])?}";
      case 57:
        return "{'n' 'u' 'l'•'l'}";
      case 58:
        return "{'u' hex•hex hex hex}";
      case 59:
        return "{'f' 'a' 'l'•'s' 'e'}";
      case 60:
        return "{'t' 'r' 'u'•'e'}";
      case 61:
        return "{String WS ':'•WS Elem|String WS ':' WS•Elem}";
      case 62:
        return "{String WS ':'•WS Elem}";
      case 63:
        return "{String WS ':' WS•Elem}";
      case 64:
        return "{([WS ',' WS Elem])*•([WS ',' WS Elem])}";
      case 65:
        return "{'f' 'a' 'l' 's'•'e'}";
      case 66:
        return "{'[' WS Elem ([WS ',' WS Elem])*•WS ']'|'[' WS Elem ([WS ',' WS Elem])* WS•']'}";
      case 67:
        return "{'u' hex hex•hex hex}";
      case 68:
        return "{'{' WS ObjectPair•([WS ',' WS ObjectPair])* WS '}'|'{' WS ObjectPair ([WS ',' WS ObjectPair])*•WS '}'|'{' WS ObjectPair ([WS ',' WS ObjectPair])* WS•'}'}";
      case 69:
        return "{'{' WS ObjectPair•([WS ',' WS ObjectPair])* WS '}'|'{' WS ObjectPair ([WS ',' WS ObjectPair])*•WS '}'}";
      case 70:
        return "{{\\t-\\n\\r\\u0020}*•{\\t-\\n\\r\\u0020}|WS•',' WS ObjectPair}";
      case 71:
        return "{'{' WS ObjectPair ([WS ',' WS ObjectPair])*•WS '}'}";
      case 72:
        return "{'{' WS ObjectPair•([WS ',' WS ObjectPair])* WS '}'}";
      case 73:
        return "{WS ','•WS ObjectPair|WS ',' WS•ObjectPair}";
      case 74:
        return "{'{' WS ObjectPair ([WS ',' WS ObjectPair])* WS•'}'}";
      case 75:
        return "{'u' hex hex hex•hex}";
      case 76:
        return "{WS•',' WS ObjectPair}";
      case 77:
        return "{WS ','•WS ObjectPair}";
      case 78:
        return "{WS ',' WS•ObjectPair}";
      case 79:
        return "{([WS ',' WS ObjectPair])*•([WS ',' WS ObjectPair])}";
      case 80:
        return "{'{' WS ObjectPair ([WS ',' WS ObjectPair])*•WS '}'|'{' WS ObjectPair ([WS ',' WS ObjectPair])* WS•'}'}";
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
    if (prev == 0 && last == 5) { // (0,5)
      // ReplaceEdge(0,40,None)
      replace(40);
      pendingFinish = -1;
      return false;
    }
    if (prev == 0 && last == 6) { // (0,6)
      // ReplaceEdge(0,8,Some(0))
      replace(8);
      pendingFinish = 0;
      return false;
    }
    if (prev == 0 && last == 7) { // (0,7)
      // ReplaceEdge(0,43,None)
      replace(43);
      pendingFinish = -1;
      return false;
    }
    if (prev == 0 && last == 10) { // (0,10)
      // ReplaceEdge(0,39,None)
      replace(39);
      pendingFinish = -1;
      return false;
    }
    if (prev == 0 && last == 11) { // (0,11)
      // DropLast(0)
      dropLast();
      return true;
    }
    if (prev == 0 && last == 12) { // (0,12)
      // ReplaceEdge(0,1,None)
      replace(1);
      pendingFinish = -1;
      return false;
    }
    if (prev == 0 && last == 13) { // (0,13)
      // ReplaceEdge(0,56,Some(0))
      replace(56);
      pendingFinish = 0;
      return false;
    }
    if (prev == 0 && last == 15) { // (0,15)
      // ReplaceEdge(0,11,Some(0))
      replace(11);
      pendingFinish = 0;
      return false;
    }
    if (prev == 0 && last == 17) { // (0,17)
      // ReplaceEdge(0,19,None)
      replace(19);
      pendingFinish = -1;
      return false;
    }
    if (prev == 0 && last == 19) { // (0,19)
      // ReplaceEdge(0,11,Some(0))
      replace(11);
      pendingFinish = 0;
      return false;
    }
    if (prev == 0 && last == 21) { // (0,21)
      // ReplaceEdge(0,11,Some(0))
      replace(11);
      pendingFinish = 0;
      return false;
    }
    if (prev == 0 && last == 24) { // (0,24)
      // ReplaceEdge(0,8,Some(0))
      replace(8);
      pendingFinish = 0;
      return false;
    }
    if (prev == 0 && last == 26) { // (0,26)
      // ReplaceEdge(0,42,None)
      replace(42);
      pendingFinish = -1;
      return false;
    }
    if (prev == 0 && last == 27) { // (0,27)
      // ReplaceEdge(0,38,None)
      replace(38);
      pendingFinish = -1;
      return false;
    }
    if (prev == 0 && last == 28) { // (0,28)
      // ReplaceEdge(0,11,Some(0))
      replace(11);
      pendingFinish = 0;
      return false;
    }
    if (prev == 0 && last == 29) { // (0,29)
      // ReplaceEdge(0,44,None)
      replace(44);
      pendingFinish = -1;
      return false;
    }
    if (prev == 0 && last == 30) { // (0,30)
      // ReplaceEdge(0,68,None)
      replace(68);
      pendingFinish = -1;
      return false;
    }
    if (prev == 0 && last == 31) { // (0,31)
      // ReplaceEdge(0,11,Some(0))
      replace(11);
      pendingFinish = 0;
      return false;
    }
    if (prev == 0 && last == 39) { // (0,39)
      // ReplaceEdge(0,59,None)
      replace(59);
      pendingFinish = -1;
      return false;
    }
    if (prev == 0 && last == 40) { // (0,40)
      // ReplaceEdge(0,60,None)
      replace(60);
      pendingFinish = -1;
      return false;
    }
    if (prev == 0 && last == 43) { // (0,43)
      // ReplaceEdge(0,57,None)
      replace(57);
      pendingFinish = -1;
      return false;
    }
    if (prev == 0 && last == 47) { // (0,47)
      // ReplaceEdge(0,50,None)
      replace(50);
      pendingFinish = -1;
      return false;
    }
    if (prev == 0 && last == 48) { // (0,48)
      // ReplaceEdge(0,66,None)
      replace(66);
      pendingFinish = -1;
      return false;
    }
    if (prev == 0 && last == 50) { // (0,50)
      // ReplaceEdge(0,11,Some(0))
      replace(11);
      pendingFinish = 0;
      return false;
    }
    if (prev == 0 && last == 57) { // (0,57)
      // ReplaceEdge(0,11,Some(0))
      replace(11);
      pendingFinish = 0;
      return false;
    }
    if (prev == 0 && last == 59) { // (0,59)
      // ReplaceEdge(0,65,None)
      replace(65);
      pendingFinish = -1;
      return false;
    }
    if (prev == 0 && last == 60) { // (0,60)
      // ReplaceEdge(0,11,Some(0))
      replace(11);
      pendingFinish = 0;
      return false;
    }
    if (prev == 0 && last == 65) { // (0,65)
      // ReplaceEdge(0,11,Some(0))
      replace(11);
      pendingFinish = 0;
      return false;
    }
    if (prev == 0 && last == 71) { // (0,71)
      // ReplaceEdge(0,74,None)
      replace(74);
      pendingFinish = -1;
      return false;
    }
    if (prev == 0 && last == 72) { // (0,72)
      // ReplaceEdge(0,80,None)
      replace(80);
      pendingFinish = -1;
      return false;
    }
    if (prev == 0 && last == 74) { // (0,74)
      // ReplaceEdge(0,11,Some(0))
      replace(11);
      pendingFinish = 0;
      return false;
    }
    if (prev == 6 && last == 24) { // (6,24)
      // DropLast(6)
      dropLast();
      return true;
    }
    if (prev == 11 && last == 12) { // (11,12)
      // ReplaceEdge(11,12,Some(11))
      pendingFinish = 11;
      return false;
    }
    if (prev == 13 && last == 14) { // (13,14)
      // DropLast(13)
      dropLast();
      return true;
    }
    if (prev == 14 && last == 25) { // (14,25)
      // ReplaceEdge(14,25,Some(14))
      pendingFinish = 14;
      return false;
    }
    if (prev == 15 && last == 36) { // (15,36)
      // DropLast(15)
      dropLast();
      return true;
    }
    if (prev == 15 && last == 37) { // (15,37)
      // ReplaceEdge(15,36,None)
      replace(36);
      pendingFinish = -1;
      return false;
    }
    if (prev == 17 && last == 18) { // (17,18)
      // ReplaceEdge(17,20,Some(17))
      replace(20);
      pendingFinish = 17;
      return false;
    }
    if (prev == 17 && last == 20) { // (17,20)
      // ReplaceEdge(17,20,Some(17))
      pendingFinish = 17;
      return false;
    }
    if (prev == 17 && last == 32) { // (17,32)
      // ReplaceEdge(17,20,Some(17))
      replace(20);
      pendingFinish = 17;
      return false;
    }
    if (prev == 18 && last == 33) { // (18,33)
      // ReplaceEdge(18,34,Some(32))
      replace(34);
      pendingFinish = 32;
      return false;
    }
    if (prev == 18 && last == 34) { // (18,34)
      // ReplaceEdge(32,34,Some(32))
      dropLast();
      replace(32);
      append(34);
      pendingFinish = 32;
      return false;
    }
    if (prev == 18 && last == 35) { // (18,35)
      // ReplaceEdge(32,58,None)
      dropLast();
      replace(32);
      append(58);
      pendingFinish = -1;
      return false;
    }
    if (prev == 20 && last == 32) { // (20,32)
      // DropLast(20)
      dropLast();
      return true;
    }
    if (prev == 21 && last == 5) { // (21,5)
      // ReplaceEdge(21,40,None)
      replace(40);
      pendingFinish = -1;
      return false;
    }
    if (prev == 21 && last == 6) { // (21,6)
      // ReplaceEdge(21,23,Some(21))
      replace(23);
      pendingFinish = 21;
      return false;
    }
    if (prev == 21 && last == 7) { // (21,7)
      // ReplaceEdge(21,43,None)
      replace(43);
      pendingFinish = -1;
      return false;
    }
    if (prev == 21 && last == 10) { // (21,10)
      // ReplaceEdge(21,39,None)
      replace(39);
      pendingFinish = -1;
      return false;
    }
    if (prev == 21 && last == 13) { // (21,13)
      // ReplaceEdge(21,15,Some(21))
      replace(15);
      pendingFinish = 21;
      return false;
    }
    if (prev == 21 && last == 15) { // (21,15)
      // DropLast(21)
      dropLast();
      return true;
    }
    if (prev == 21 && last == 17) { // (21,17)
      // ReplaceEdge(21,19,None)
      replace(19);
      pendingFinish = -1;
      return false;
    }
    if (prev == 21 && last == 19) { // (21,19)
      // DropLast(21)
      dropLast();
      return true;
    }
    if (prev == 21 && last == 24) { // (21,24)
      // ReplaceEdge(21,23,Some(21))
      replace(23);
      pendingFinish = 21;
      return false;
    }
    if (prev == 21 && last == 26) { // (21,26)
      // ReplaceEdge(21,42,None)
      replace(42);
      pendingFinish = -1;
      return false;
    }
    if (prev == 21 && last == 27) { // (21,27)
      // ReplaceEdge(21,38,None)
      replace(38);
      pendingFinish = -1;
      return false;
    }
    if (prev == 21 && last == 28) { // (21,28)
      // DropLast(21)
      dropLast();
      return true;
    }
    if (prev == 21 && last == 29) { // (21,29)
      // ReplaceEdge(21,44,None)
      replace(44);
      pendingFinish = -1;
      return false;
    }
    if (prev == 21 && last == 30) { // (21,30)
      // ReplaceEdge(21,68,None)
      replace(68);
      pendingFinish = -1;
      return false;
    }
    if (prev == 21 && last == 31) { // (21,31)
      // DropLast(21)
      dropLast();
      return true;
    }
    if (prev == 21 && last == 39) { // (21,39)
      // ReplaceEdge(21,59,None)
      replace(59);
      pendingFinish = -1;
      return false;
    }
    if (prev == 21 && last == 40) { // (21,40)
      // ReplaceEdge(21,60,None)
      replace(60);
      pendingFinish = -1;
      return false;
    }
    if (prev == 21 && last == 43) { // (21,43)
      // ReplaceEdge(21,57,None)
      replace(57);
      pendingFinish = -1;
      return false;
    }
    if (prev == 21 && last == 47) { // (21,47)
      // ReplaceEdge(21,50,None)
      replace(50);
      pendingFinish = -1;
      return false;
    }
    if (prev == 21 && last == 48) { // (21,48)
      // ReplaceEdge(21,66,None)
      replace(66);
      pendingFinish = -1;
      return false;
    }
    if (prev == 21 && last == 50) { // (21,50)
      // DropLast(21)
      dropLast();
      return true;
    }
    if (prev == 21 && last == 57) { // (21,57)
      // DropLast(21)
      dropLast();
      return true;
    }
    if (prev == 21 && last == 59) { // (21,59)
      // ReplaceEdge(21,65,None)
      replace(65);
      pendingFinish = -1;
      return false;
    }
    if (prev == 21 && last == 60) { // (21,60)
      // DropLast(21)
      dropLast();
      return true;
    }
    if (prev == 21 && last == 65) { // (21,65)
      // DropLast(21)
      dropLast();
      return true;
    }
    if (prev == 21 && last == 71) { // (21,71)
      // ReplaceEdge(21,74,None)
      replace(74);
      pendingFinish = -1;
      return false;
    }
    if (prev == 21 && last == 72) { // (21,72)
      // ReplaceEdge(21,80,None)
      replace(80);
      pendingFinish = -1;
      return false;
    }
    if (prev == 21 && last == 74) { // (21,74)
      // DropLast(21)
      dropLast();
      return true;
    }
    if (prev == 24 && last == 25) { // (24,25)
      // ReplaceEdge(24,25,Some(24))
      pendingFinish = 24;
      return false;
    }
    if (prev == 26 && last == 12) { // (26,12)
      // ReplaceEdge(26,12,Some(26))
      pendingFinish = 26;
      return false;
    }
    if (prev == 27 && last == 5) { // (27,5)
      // ReplaceEdge(27,40,None)
      replace(40);
      pendingFinish = -1;
      return false;
    }
    if (prev == 27 && last == 6) { // (27,6)
      // ReplaceEdge(27,23,Some(27))
      replace(23);
      pendingFinish = 27;
      return false;
    }
    if (prev == 27 && last == 7) { // (27,7)
      // ReplaceEdge(27,43,None)
      replace(43);
      pendingFinish = -1;
      return false;
    }
    if (prev == 27 && last == 10) { // (27,10)
      // ReplaceEdge(27,39,None)
      replace(39);
      pendingFinish = -1;
      return false;
    }
    if (prev == 27 && last == 13) { // (27,13)
      // ReplaceEdge(27,15,Some(27))
      replace(15);
      pendingFinish = 27;
      return false;
    }
    if (prev == 27 && last == 15) { // (27,15)
      // DropLast(27)
      dropLast();
      return true;
    }
    if (prev == 27 && last == 17) { // (27,17)
      // ReplaceEdge(27,19,None)
      replace(19);
      pendingFinish = -1;
      return false;
    }
    if (prev == 27 && last == 19) { // (27,19)
      // DropLast(27)
      dropLast();
      return true;
    }
    if (prev == 27 && last == 24) { // (27,24)
      // ReplaceEdge(27,23,Some(27))
      replace(23);
      pendingFinish = 27;
      return false;
    }
    if (prev == 27 && last == 26) { // (27,26)
      // ReplaceEdge(27,42,None)
      replace(42);
      pendingFinish = -1;
      return false;
    }
    if (prev == 27 && last == 27) { // (27,27)
      // DropLast(27)
      dropLast();
      return true;
    }
    if (prev == 27 && last == 28) { // (27,28)
      // DropLast(27)
      dropLast();
      return true;
    }
    if (prev == 27 && last == 29) { // (27,29)
      // ReplaceEdge(27,44,None)
      replace(44);
      pendingFinish = -1;
      return false;
    }
    if (prev == 27 && last == 30) { // (27,30)
      // ReplaceEdge(27,68,None)
      replace(68);
      pendingFinish = -1;
      return false;
    }
    if (prev == 27 && last == 31) { // (27,31)
      // DropLast(27)
      dropLast();
      return true;
    }
    if (prev == 27 && last == 39) { // (27,39)
      // ReplaceEdge(27,59,None)
      replace(59);
      pendingFinish = -1;
      return false;
    }
    if (prev == 27 && last == 40) { // (27,40)
      // ReplaceEdge(27,60,None)
      replace(60);
      pendingFinish = -1;
      return false;
    }
    if (prev == 27 && last == 43) { // (27,43)
      // ReplaceEdge(27,57,None)
      replace(57);
      pendingFinish = -1;
      return false;
    }
    if (prev == 27 && last == 57) { // (27,57)
      // DropLast(27)
      dropLast();
      return true;
    }
    if (prev == 27 && last == 59) { // (27,59)
      // ReplaceEdge(27,65,None)
      replace(65);
      pendingFinish = -1;
      return false;
    }
    if (prev == 27 && last == 60) { // (27,60)
      // DropLast(27)
      dropLast();
      return true;
    }
    if (prev == 27 && last == 65) { // (27,65)
      // DropLast(27)
      dropLast();
      return true;
    }
    if (prev == 27 && last == 71) { // (27,71)
      // ReplaceEdge(27,74,None)
      replace(74);
      pendingFinish = -1;
      return false;
    }
    if (prev == 27 && last == 72) { // (27,72)
      // ReplaceEdge(27,80,None)
      replace(80);
      pendingFinish = -1;
      return false;
    }
    if (prev == 27 && last == 74) { // (27,74)
      // DropLast(27)
      dropLast();
      return true;
    }
    if (prev == 29 && last == 12) { // (29,12)
      // ReplaceEdge(29,12,Some(29))
      pendingFinish = 29;
      return false;
    }
    if (prev == 30 && last == 17) { // (30,17)
      // ReplaceEdge(30,19,None)
      replace(19);
      pendingFinish = -1;
      return false;
    }
    if (prev == 30 && last == 19) { // (30,19)
      // ReplaceEdge(30,41,None)
      replace(41);
      pendingFinish = -1;
      return false;
    }
    if (prev == 30 && last == 51) { // (30,51)
      // ReplaceEdge(30,52,None)
      replace(52);
      pendingFinish = -1;
      return false;
    }
    if (prev == 30 && last == 52) { // (30,52)
      // ReplaceEdge(30,61,None)
      replace(61);
      pendingFinish = -1;
      return false;
    }
    if (prev == 30 && last == 62) { // (30,62)
      // ReplaceEdge(30,63,None)
      replace(63);
      pendingFinish = -1;
      return false;
    }
    if (prev == 30 && last == 63) { // (30,63)
      // DropLast(30)
      dropLast();
      return true;
    }
    if (prev == 32 && last == 34) { // (32,34)
      // ReplaceEdge(32,34,Some(32))
      pendingFinish = 32;
      return false;
    }
    if (prev == 32 && last == 35) { // (32,35)
      // ReplaceEdge(32,58,None)
      replace(58);
      pendingFinish = -1;
      return false;
    }
    if (prev == 32 && last == 58) { // (32,58)
      // ReplaceEdge(32,67,None)
      replace(67);
      pendingFinish = -1;
      return false;
    }
    if (prev == 32 && last == 67) { // (32,67)
      // ReplaceEdge(32,75,None)
      replace(75);
      pendingFinish = -1;
      return false;
    }
    if (prev == 32 && last == 75) { // (32,75)
      // ReplaceEdge(32,34,Some(32))
      replace(34);
      pendingFinish = 32;
      return false;
    }
    if (prev == 33 && last == 35) { // (33,35)
      // ReplaceEdge(33,58,None)
      replace(58);
      pendingFinish = -1;
      return false;
    }
    if (prev == 33 && last == 58) { // (33,58)
      // ReplaceEdge(33,67,None)
      replace(67);
      pendingFinish = -1;
      return false;
    }
    if (prev == 33 && last == 67) { // (33,67)
      // ReplaceEdge(33,75,None)
      replace(75);
      pendingFinish = -1;
      return false;
    }
    if (prev == 33 && last == 75) { // (33,75)
      // DropLast(33)
      dropLast();
      return true;
    }
    if (prev == 34 && last == 35) { // (34,35)
      // ReplaceEdge(34,58,None)
      replace(58);
      pendingFinish = -1;
      return false;
    }
    if (prev == 34 && last == 58) { // (34,58)
      // ReplaceEdge(34,67,None)
      replace(67);
      pendingFinish = -1;
      return false;
    }
    if (prev == 34 && last == 67) { // (34,67)
      // ReplaceEdge(34,75,None)
      replace(75);
      pendingFinish = -1;
      return false;
    }
    if (prev == 34 && last == 75) { // (34,75)
      // DropLast(34)
      dropLast();
      return true;
    }
    if (prev == 36 && last == 25) { // (36,25)
      // ReplaceEdge(36,25,Some(36))
      pendingFinish = 36;
      return false;
    }
    if (prev == 45 && last == 12) { // (45,12)
      // ReplaceEdge(45,46,Some(47))
      replace(46);
      pendingFinish = 47;
      return false;
    }
    if (prev == 45 && last == 53) { // (45,53)
      // ReplaceEdge(48,49,None)
      dropLast();
      replace(48);
      append(49);
      pendingFinish = -1;
      return false;
    }
    if (prev == 47 && last == 12) { // (47,12)
      // ReplaceEdge(47,12,Some(47))
      pendingFinish = 47;
      return false;
    }
    if (prev == 48 && last == 54) { // (48,54)
      // ReplaceEdge(48,55,None)
      replace(55);
      pendingFinish = -1;
      return false;
    }
    if (prev == 48 && last == 55) { // (48,55)
      // ReplaceEdge(48,64,Some(48))
      replace(64);
      pendingFinish = 48;
      return false;
    }
    if (prev == 48 && last == 64) { // (48,64)
      // ReplaceEdge(48,64,Some(48))
      pendingFinish = 48;
      return false;
    }
    if (prev == 51 && last == 12) { // (51,12)
      // ReplaceEdge(51,12,Some(51))
      pendingFinish = 51;
      return false;
    }
    if (prev == 54 && last == 12) { // (54,12)
      // ReplaceEdge(54,12,Some(54))
      pendingFinish = 54;
      return false;
    }
    if (prev == 55 && last == 5) { // (55,5)
      // ReplaceEdge(55,40,None)
      replace(40);
      pendingFinish = -1;
      return false;
    }
    if (prev == 55 && last == 6) { // (55,6)
      // ReplaceEdge(55,23,Some(55))
      replace(23);
      pendingFinish = 55;
      return false;
    }
    if (prev == 55 && last == 7) { // (55,7)
      // ReplaceEdge(55,43,None)
      replace(43);
      pendingFinish = -1;
      return false;
    }
    if (prev == 55 && last == 10) { // (55,10)
      // ReplaceEdge(55,39,None)
      replace(39);
      pendingFinish = -1;
      return false;
    }
    if (prev == 55 && last == 13) { // (55,13)
      // ReplaceEdge(55,15,Some(55))
      replace(15);
      pendingFinish = 55;
      return false;
    }
    if (prev == 55 && last == 15) { // (55,15)
      // DropLast(55)
      dropLast();
      return true;
    }
    if (prev == 55 && last == 17) { // (55,17)
      // ReplaceEdge(55,19,None)
      replace(19);
      pendingFinish = -1;
      return false;
    }
    if (prev == 55 && last == 19) { // (55,19)
      // DropLast(55)
      dropLast();
      return true;
    }
    if (prev == 55 && last == 24) { // (55,24)
      // ReplaceEdge(55,23,Some(55))
      replace(23);
      pendingFinish = 55;
      return false;
    }
    if (prev == 55 && last == 26) { // (55,26)
      // ReplaceEdge(55,42,None)
      replace(42);
      pendingFinish = -1;
      return false;
    }
    if (prev == 55 && last == 27) { // (55,27)
      // ReplaceEdge(55,38,None)
      replace(38);
      pendingFinish = -1;
      return false;
    }
    if (prev == 55 && last == 28) { // (55,28)
      // DropLast(55)
      dropLast();
      return true;
    }
    if (prev == 55 && last == 29) { // (55,29)
      // ReplaceEdge(55,44,None)
      replace(44);
      pendingFinish = -1;
      return false;
    }
    if (prev == 55 && last == 30) { // (55,30)
      // ReplaceEdge(55,68,None)
      replace(68);
      pendingFinish = -1;
      return false;
    }
    if (prev == 55 && last == 31) { // (55,31)
      // DropLast(55)
      dropLast();
      return true;
    }
    if (prev == 55 && last == 39) { // (55,39)
      // ReplaceEdge(55,59,None)
      replace(59);
      pendingFinish = -1;
      return false;
    }
    if (prev == 55 && last == 40) { // (55,40)
      // ReplaceEdge(55,60,None)
      replace(60);
      pendingFinish = -1;
      return false;
    }
    if (prev == 55 && last == 43) { // (55,43)
      // ReplaceEdge(55,57,None)
      replace(57);
      pendingFinish = -1;
      return false;
    }
    if (prev == 55 && last == 47) { // (55,47)
      // ReplaceEdge(55,50,None)
      replace(50);
      pendingFinish = -1;
      return false;
    }
    if (prev == 55 && last == 48) { // (55,48)
      // ReplaceEdge(55,66,None)
      replace(66);
      pendingFinish = -1;
      return false;
    }
    if (prev == 55 && last == 50) { // (55,50)
      // DropLast(55)
      dropLast();
      return true;
    }
    if (prev == 55 && last == 57) { // (55,57)
      // DropLast(55)
      dropLast();
      return true;
    }
    if (prev == 55 && last == 59) { // (55,59)
      // ReplaceEdge(55,65,None)
      replace(65);
      pendingFinish = -1;
      return false;
    }
    if (prev == 55 && last == 60) { // (55,60)
      // DropLast(55)
      dropLast();
      return true;
    }
    if (prev == 55 && last == 65) { // (55,65)
      // DropLast(55)
      dropLast();
      return true;
    }
    if (prev == 55 && last == 71) { // (55,71)
      // ReplaceEdge(55,74,None)
      replace(74);
      pendingFinish = -1;
      return false;
    }
    if (prev == 55 && last == 72) { // (55,72)
      // ReplaceEdge(55,80,None)
      replace(80);
      pendingFinish = -1;
      return false;
    }
    if (prev == 55 && last == 74) { // (55,74)
      // DropLast(55)
      dropLast();
      return true;
    }
    if (prev == 62 && last == 12) { // (62,12)
      // ReplaceEdge(62,12,Some(62))
      pendingFinish = 62;
      return false;
    }
    if (prev == 63 && last == 5) { // (63,5)
      // ReplaceEdge(63,40,None)
      replace(40);
      pendingFinish = -1;
      return false;
    }
    if (prev == 63 && last == 6) { // (63,6)
      // ReplaceEdge(63,23,Some(63))
      replace(23);
      pendingFinish = 63;
      return false;
    }
    if (prev == 63 && last == 7) { // (63,7)
      // ReplaceEdge(63,43,None)
      replace(43);
      pendingFinish = -1;
      return false;
    }
    if (prev == 63 && last == 10) { // (63,10)
      // ReplaceEdge(63,39,None)
      replace(39);
      pendingFinish = -1;
      return false;
    }
    if (prev == 63 && last == 13) { // (63,13)
      // ReplaceEdge(63,15,Some(63))
      replace(15);
      pendingFinish = 63;
      return false;
    }
    if (prev == 63 && last == 15) { // (63,15)
      // DropLast(63)
      dropLast();
      return true;
    }
    if (prev == 63 && last == 17) { // (63,17)
      // ReplaceEdge(63,19,None)
      replace(19);
      pendingFinish = -1;
      return false;
    }
    if (prev == 63 && last == 19) { // (63,19)
      // DropLast(63)
      dropLast();
      return true;
    }
    if (prev == 63 && last == 24) { // (63,24)
      // ReplaceEdge(63,23,Some(63))
      replace(23);
      pendingFinish = 63;
      return false;
    }
    if (prev == 63 && last == 26) { // (63,26)
      // ReplaceEdge(63,42,None)
      replace(42);
      pendingFinish = -1;
      return false;
    }
    if (prev == 63 && last == 27) { // (63,27)
      // ReplaceEdge(63,38,None)
      replace(38);
      pendingFinish = -1;
      return false;
    }
    if (prev == 63 && last == 28) { // (63,28)
      // DropLast(63)
      dropLast();
      return true;
    }
    if (prev == 63 && last == 29) { // (63,29)
      // ReplaceEdge(63,44,None)
      replace(44);
      pendingFinish = -1;
      return false;
    }
    if (prev == 63 && last == 30) { // (63,30)
      // ReplaceEdge(63,68,None)
      replace(68);
      pendingFinish = -1;
      return false;
    }
    if (prev == 63 && last == 31) { // (63,31)
      // DropLast(63)
      dropLast();
      return true;
    }
    if (prev == 63 && last == 39) { // (63,39)
      // ReplaceEdge(63,59,None)
      replace(59);
      pendingFinish = -1;
      return false;
    }
    if (prev == 63 && last == 40) { // (63,40)
      // ReplaceEdge(63,60,None)
      replace(60);
      pendingFinish = -1;
      return false;
    }
    if (prev == 63 && last == 43) { // (63,43)
      // ReplaceEdge(63,57,None)
      replace(57);
      pendingFinish = -1;
      return false;
    }
    if (prev == 63 && last == 47) { // (63,47)
      // ReplaceEdge(63,50,None)
      replace(50);
      pendingFinish = -1;
      return false;
    }
    if (prev == 63 && last == 48) { // (63,48)
      // ReplaceEdge(63,66,None)
      replace(66);
      pendingFinish = -1;
      return false;
    }
    if (prev == 63 && last == 50) { // (63,50)
      // DropLast(63)
      dropLast();
      return true;
    }
    if (prev == 63 && last == 57) { // (63,57)
      // DropLast(63)
      dropLast();
      return true;
    }
    if (prev == 63 && last == 59) { // (63,59)
      // ReplaceEdge(63,65,None)
      replace(65);
      pendingFinish = -1;
      return false;
    }
    if (prev == 63 && last == 60) { // (63,60)
      // DropLast(63)
      dropLast();
      return true;
    }
    if (prev == 63 && last == 65) { // (63,65)
      // DropLast(63)
      dropLast();
      return true;
    }
    if (prev == 63 && last == 71) { // (63,71)
      // ReplaceEdge(63,74,None)
      replace(74);
      pendingFinish = -1;
      return false;
    }
    if (prev == 63 && last == 72) { // (63,72)
      // ReplaceEdge(63,80,None)
      replace(80);
      pendingFinish = -1;
      return false;
    }
    if (prev == 63 && last == 74) { // (63,74)
      // DropLast(63)
      dropLast();
      return true;
    }
    if (prev == 64 && last == 12) { // (64,12)
      // ReplaceEdge(64,46,None)
      replace(46);
      pendingFinish = -1;
      return false;
    }
    if (prev == 64 && last == 53) { // (64,53)
      // ReplaceEdge(64,49,None)
      replace(49);
      pendingFinish = -1;
      return false;
    }
    if (prev == 64 && last == 54) { // (64,54)
      // ReplaceEdge(64,55,None)
      replace(55);
      pendingFinish = -1;
      return false;
    }
    if (prev == 64 && last == 55) { // (64,55)
      // DropLast(64)
      dropLast();
      return true;
    }
    if (prev == 69 && last == 12) { // (69,12)
      // ReplaceEdge(69,70,Some(71))
      replace(70);
      pendingFinish = 71;
      return false;
    }
    if (prev == 69 && last == 76) { // (69,76)
      // ReplaceEdge(72,73,None)
      dropLast();
      replace(72);
      append(73);
      pendingFinish = -1;
      return false;
    }
    if (prev == 71 && last == 12) { // (71,12)
      // ReplaceEdge(71,12,Some(71))
      pendingFinish = 71;
      return false;
    }
    if (prev == 72 && last == 77) { // (72,77)
      // ReplaceEdge(72,78,None)
      replace(78);
      pendingFinish = -1;
      return false;
    }
    if (prev == 72 && last == 78) { // (72,78)
      // ReplaceEdge(72,79,Some(72))
      replace(79);
      pendingFinish = 72;
      return false;
    }
    if (prev == 72 && last == 79) { // (72,79)
      // ReplaceEdge(72,79,Some(72))
      pendingFinish = 72;
      return false;
    }
    if (prev == 77 && last == 12) { // (77,12)
      // ReplaceEdge(77,12,Some(77))
      pendingFinish = 77;
      return false;
    }
    if (prev == 78 && last == 17) { // (78,17)
      // ReplaceEdge(78,19,None)
      replace(19);
      pendingFinish = -1;
      return false;
    }
    if (prev == 78 && last == 19) { // (78,19)
      // ReplaceEdge(78,41,None)
      replace(41);
      pendingFinish = -1;
      return false;
    }
    if (prev == 78 && last == 51) { // (78,51)
      // ReplaceEdge(78,52,None)
      replace(52);
      pendingFinish = -1;
      return false;
    }
    if (prev == 78 && last == 52) { // (78,52)
      // ReplaceEdge(78,61,None)
      replace(61);
      pendingFinish = -1;
      return false;
    }
    if (prev == 78 && last == 62) { // (78,62)
      // ReplaceEdge(78,63,None)
      replace(63);
      pendingFinish = -1;
      return false;
    }
    if (prev == 78 && last == 63) { // (78,63)
      // DropLast(78)
      dropLast();
      return true;
    }
    if (prev == 79 && last == 12) { // (79,12)
      // ReplaceEdge(79,70,None)
      replace(70);
      pendingFinish = -1;
      return false;
    }
    if (prev == 79 && last == 76) { // (79,76)
      // ReplaceEdge(79,73,None)
      replace(73);
      pendingFinish = -1;
      return false;
    }
    if (prev == 79 && last == 77) { // (79,77)
      // ReplaceEdge(79,78,None)
      replace(78);
      pendingFinish = -1;
      return false;
    }
    if (prev == 79 && last == 78) { // (79,78)
      // DropLast(79)
      dropLast();
      return true;
    }
    throw new AssertionError("Unknown edge to finish: " + stackIds());
  }

  private boolean proceedStep(char c) {
    switch (stack.nodeId) {
      case 0:
        if (('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ')) {
          // Append(0,1,None)
          append(1);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == '"')) {
          // Append(0,2,None)
          append(2);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == '-')) {
          // Append(0,6,None)
          append(6);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == '0')) {
          // Append(0,8,Some(0))
          append(8);
          pendingFinish = 0;
          if (verbose) printStack();
          return true;
        }
        if (('1' <= c && c <= '9')) {
          // Append(0,4,Some(0))
          append(4);
          pendingFinish = 0;
          if (verbose) printStack();
          return true;
        }
        if ((c == '[')) {
          // Append(0,3,None)
          append(3);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == 'f')) {
          // Append(0,10,None)
          append(10);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == 'n')) {
          // Append(0,7,None)
          append(7);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == 't')) {
          // Append(0,5,None)
          append(5);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == '{')) {
          // Append(0,9,None)
          append(9);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        return false;
      case 1:
        if (('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ')) {
          // Finish(12)
          replace(12);
          if (verbose) printStack();
          finish();
          if (verbose) printStack();
          return true;
        }
        if ((c == '"')) {
          // Append(21,2,None)
          replace(21);
          append(2);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == '-')) {
          // Append(21,6,None)
          replace(21);
          append(6);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == '0')) {
          // Append(21,23,Some(21))
          replace(21);
          append(23);
          pendingFinish = 21;
          if (verbose) printStack();
          return true;
        }
        if (('1' <= c && c <= '9')) {
          // Append(21,22,Some(21))
          replace(21);
          append(22);
          pendingFinish = 21;
          if (verbose) printStack();
          return true;
        }
        if ((c == '[')) {
          // Append(21,3,None)
          replace(21);
          append(3);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == 'f')) {
          // Append(21,10,None)
          replace(21);
          append(10);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == 'n')) {
          // Append(21,7,None)
          replace(21);
          append(7);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == 't')) {
          // Append(21,5,None)
          replace(21);
          append(5);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == '{')) {
          // Append(21,9,None)
          replace(21);
          append(9);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        return false;
      case 2:
        if ((c == ' ')
            || ('0' <= c && c <= '9')
            || ('A' <= c && c <= 'Z')
            || ('a' <= c && c <= 'z')) {
          // Append(17,20,Some(17))
          replace(17);
          append(20);
          pendingFinish = 17;
          if (verbose) printStack();
          return true;
        }
        if ((c == '"')) {
          // Finish(19)
          replace(19);
          if (verbose) printStack();
          finish();
          if (verbose) printStack();
          return true;
        }
        if ((c == '\\')) {
          // Append(17,18,Some(17))
          replace(17);
          append(18);
          pendingFinish = 17;
          if (verbose) printStack();
          return true;
        }
        return false;
      case 3:
        if (('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ')) {
          // Append(26,12,Some(26))
          replace(26);
          append(12);
          pendingFinish = 26;
          if (verbose) printStack();
          return true;
        }
        if ((c == '"')) {
          // Append(27,2,None)
          replace(27);
          append(2);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == '-')) {
          // Append(27,6,None)
          replace(27);
          append(6);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == '0')) {
          // Append(27,23,Some(27))
          replace(27);
          append(23);
          pendingFinish = 27;
          if (verbose) printStack();
          return true;
        }
        if (('1' <= c && c <= '9')) {
          // Append(27,22,Some(27))
          replace(27);
          append(22);
          pendingFinish = 27;
          if (verbose) printStack();
          return true;
        }
        if ((c == '[')) {
          // Append(27,3,None)
          replace(27);
          append(3);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == ']')) {
          // Finish(28)
          replace(28);
          if (verbose) printStack();
          finish();
          if (verbose) printStack();
          return true;
        }
        if ((c == 'f')) {
          // Append(27,10,None)
          replace(27);
          append(10);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == 'n')) {
          // Append(27,7,None)
          replace(27);
          append(7);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == 't')) {
          // Append(27,5,None)
          replace(27);
          append(5);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == '{')) {
          // Append(27,9,None)
          replace(27);
          append(9);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        return false;
      case 4:
        if (('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ')) {
          // Append(11,12,Some(11))
          replace(11);
          append(12);
          pendingFinish = 11;
          if (verbose) printStack();
          return true;
        }
        if ((c == '.')) {
          // Append(13,14,None)
          replace(13);
          append(14);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if (('0' <= c && c <= '9')) {
          // Append(24,25,Some(24))
          replace(24);
          append(25);
          pendingFinish = 24;
          if (verbose) printStack();
          return true;
        }
        if ((c == 'E') || (c == 'e')) {
          // Append(15,16,None)
          replace(15);
          append(16);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        return false;
      case 5:
        if ((c == 'r')) {
          // Finish(5)
          finish();
          if (verbose) printStack();
          return true;
        }
        return false;
      case 6:
        if ((c == '0')) {
          // Finish(6)
          finish();
          if (verbose) printStack();
          return true;
        }
        if (('1' <= c && c <= '9')) {
          // Append(6,24,Some(6))
          append(24);
          pendingFinish = 6;
          if (verbose) printStack();
          return true;
        }
        return false;
      case 7:
        if ((c == 'u')) {
          // Finish(7)
          finish();
          if (verbose) printStack();
          return true;
        }
        return false;
      case 8:
        if (('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ')) {
          // Append(11,12,Some(11))
          replace(11);
          append(12);
          pendingFinish = 11;
          if (verbose) printStack();
          return true;
        }
        if ((c == '.')) {
          // Append(13,14,None)
          replace(13);
          append(14);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == 'E') || (c == 'e')) {
          // Append(15,16,None)
          replace(15);
          append(16);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        return false;
      case 9:
        if (('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ')) {
          // Append(29,12,Some(29))
          replace(29);
          append(12);
          pendingFinish = 29;
          if (verbose) printStack();
          return true;
        }
        if ((c == '"')) {
          // Append(30,2,None)
          replace(30);
          append(2);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == '}')) {
          // Finish(31)
          replace(31);
          if (verbose) printStack();
          finish();
          if (verbose) printStack();
          return true;
        }
        return false;
      case 10:
        if ((c == 'a')) {
          // Finish(10)
          finish();
          if (verbose) printStack();
          return true;
        }
        return false;
      case 11:
        if (('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ')) {
          // Append(11,12,Some(11))
          append(12);
          pendingFinish = 11;
          if (verbose) printStack();
          return true;
        }
        return false;
      case 12:
        if (('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ')) {
          // Finish(12)
          finish();
          if (verbose) printStack();
          return true;
        }
        return false;
      case 14:
        if (('0' <= c && c <= '9')) {
          // Append(14,25,Some(14))
          append(25);
          pendingFinish = 14;
          if (verbose) printStack();
          return true;
        }
        return false;
      case 15:
        if ((c == 'E') || (c == 'e')) {
          // Append(15,16,None)
          append(16);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        return false;
      case 16:
        if ((c == '+') || (c == '-')) {
          // Finish(37)
          replace(37);
          if (verbose) printStack();
          finish();
          if (verbose) printStack();
          return true;
        }
        if (('0' <= c && c <= '9')) {
          // Append(36,25,Some(36))
          replace(36);
          append(25);
          pendingFinish = 36;
          if (verbose) printStack();
          return true;
        }
        return false;
      case 18:
        if ((c == ' ')
            || ('0' <= c && c <= '9')
            || ('A' <= c && c <= 'Z')
            || (c == 'a')
            || ('c' <= c && c <= 'm')
            || ('o' <= c && c <= 'q')
            || (c == 's')
            || ('v' <= c && c <= 'z')) {
          // Finish(20)
          replace(20);
          if (verbose) printStack();
          finish();
          if (verbose) printStack();
          return true;
        }
        if ((c == '"') || (c == '/')) {
          // Append(32,34,Some(32))
          replace(32);
          append(34);
          pendingFinish = 32;
          if (verbose) printStack();
          return true;
        }
        if ((c == '\\')) {
          // Append(18,33,Some(18))
          append(33);
          pendingFinish = 18;
          if (verbose) printStack();
          return true;
        }
        if ((c == 'b') || (c == 'n') || (c == 'r') || (c == 't')) {
          // Append(18,34,Some(18))
          append(34);
          pendingFinish = 18;
          if (verbose) printStack();
          return true;
        }
        if ((c == 'u')) {
          // Append(18,35,Some(20))
          append(35);
          pendingFinish = 20;
          if (verbose) printStack();
          return true;
        }
        return false;
      case 19:
        if ((c == '"')) {
          // Finish(19)
          finish();
          if (verbose) printStack();
          return true;
        }
        return false;
      case 20:
        if ((c == ' ')
            || ('0' <= c && c <= '9')
            || ('A' <= c && c <= 'Z')
            || ('a' <= c && c <= 'z')) {
          // Finish(20)
          finish();
          if (verbose) printStack();
          return true;
        }
        if ((c == '\\')) {
          // Append(20,32,Some(20))
          append(32);
          pendingFinish = 20;
          if (verbose) printStack();
          return true;
        }
        return false;
      case 22:
        if ((c == '.')) {
          // Append(13,14,None)
          replace(13);
          append(14);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if (('0' <= c && c <= '9')) {
          // Append(24,25,Some(24))
          replace(24);
          append(25);
          pendingFinish = 24;
          if (verbose) printStack();
          return true;
        }
        if ((c == 'E') || (c == 'e')) {
          // Append(15,16,None)
          replace(15);
          append(16);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        return false;
      case 23:
        if ((c == '.')) {
          // Append(13,14,None)
          replace(13);
          append(14);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == 'E') || (c == 'e')) {
          // Append(15,16,None)
          replace(15);
          append(16);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        return false;
      case 24:
        if (('0' <= c && c <= '9')) {
          // Append(24,25,Some(24))
          append(25);
          pendingFinish = 24;
          if (verbose) printStack();
          return true;
        }
        return false;
      case 25:
        if (('0' <= c && c <= '9')) {
          // Finish(25)
          finish();
          if (verbose) printStack();
          return true;
        }
        return false;
      case 32:
        if ((c == '"')
            || (c == '/')
            || (c == '\\')
            || (c == 'b')
            || (c == 'n')
            || (c == 'r')
            || (c == 't')) {
          // Append(32,34,Some(32))
          append(34);
          pendingFinish = 32;
          if (verbose) printStack();
          return true;
        }
        if ((c == 'u')) {
          // Append(32,35,None)
          append(35);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        return false;
      case 33:
        if ((c == '"')
            || (c == '/')
            || (c == '\\')
            || (c == 'b')
            || (c == 'n')
            || (c == 'r')
            || (c == 't')) {
          // Finish(33)
          finish();
          if (verbose) printStack();
          return true;
        }
        if ((c == 'u')) {
          // Append(33,35,None)
          append(35);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        return false;
      case 34:
        if ((c == '"')
            || (c == '/')
            || (c == '\\')
            || (c == 'b')
            || (c == 'n')
            || (c == 'r')
            || (c == 't')) {
          // Finish(34)
          finish();
          if (verbose) printStack();
          return true;
        }
        if ((c == 'u')) {
          // Append(34,35,None)
          append(35);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        return false;
      case 35:
        if (('0' <= c && c <= '9') || ('A' <= c && c <= 'F') || ('a' <= c && c <= 'f')) {
          // Finish(35)
          finish();
          if (verbose) printStack();
          return true;
        }
        return false;
      case 36:
        if (('0' <= c && c <= '9')) {
          // Append(36,25,Some(36))
          append(25);
          pendingFinish = 36;
          if (verbose) printStack();
          return true;
        }
        return false;
      case 38:
        if (('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ')) {
          // Append(45,46,Some(47))
          replace(45);
          append(46);
          pendingFinish = 47;
          if (verbose) printStack();
          return true;
        }
        if ((c == ',')) {
          // Append(48,49,None)
          replace(48);
          append(49);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == ']')) {
          // Finish(50)
          replace(50);
          if (verbose) printStack();
          finish();
          if (verbose) printStack();
          return true;
        }
        return false;
      case 39:
        if ((c == 'l')) {
          // Finish(39)
          finish();
          if (verbose) printStack();
          return true;
        }
        return false;
      case 40:
        if ((c == 'u')) {
          // Finish(40)
          finish();
          if (verbose) printStack();
          return true;
        }
        return false;
      case 41:
        if (('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ')) {
          // Append(51,12,Some(51))
          replace(51);
          append(12);
          pendingFinish = 51;
          if (verbose) printStack();
          return true;
        }
        if ((c == ':')) {
          // Finish(52)
          replace(52);
          if (verbose) printStack();
          finish();
          if (verbose) printStack();
          return true;
        }
        return false;
      case 42:
        if ((c == '"')) {
          // Append(27,2,None)
          replace(27);
          append(2);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == '-')) {
          // Append(27,6,None)
          replace(27);
          append(6);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == '0')) {
          // Append(27,23,Some(27))
          replace(27);
          append(23);
          pendingFinish = 27;
          if (verbose) printStack();
          return true;
        }
        if (('1' <= c && c <= '9')) {
          // Append(27,22,Some(27))
          replace(27);
          append(22);
          pendingFinish = 27;
          if (verbose) printStack();
          return true;
        }
        if ((c == '[')) {
          // Append(27,3,None)
          replace(27);
          append(3);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == ']')) {
          // Finish(28)
          replace(28);
          if (verbose) printStack();
          finish();
          if (verbose) printStack();
          return true;
        }
        if ((c == 'f')) {
          // Append(27,10,None)
          replace(27);
          append(10);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == 'n')) {
          // Append(27,7,None)
          replace(27);
          append(7);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == 't')) {
          // Append(27,5,None)
          replace(27);
          append(5);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == '{')) {
          // Append(27,9,None)
          replace(27);
          append(9);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        return false;
      case 43:
        if ((c == 'l')) {
          // Finish(43)
          finish();
          if (verbose) printStack();
          return true;
        }
        return false;
      case 44:
        if ((c == '"')) {
          // Append(30,2,None)
          replace(30);
          append(2);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == '}')) {
          // Finish(31)
          replace(31);
          if (verbose) printStack();
          finish();
          if (verbose) printStack();
          return true;
        }
        return false;
      case 46:
        if (('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ')) {
          // Finish(12)
          replace(12);
          if (verbose) printStack();
          finish();
          if (verbose) printStack();
          return true;
        }
        if ((c == ',')) {
          // Finish(53)
          replace(53);
          if (verbose) printStack();
          finish();
          if (verbose) printStack();
          return true;
        }
        return false;
      case 49:
        if (('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ')) {
          // Append(54,12,Some(54))
          replace(54);
          append(12);
          pendingFinish = 54;
          if (verbose) printStack();
          return true;
        }
        if ((c == '"')) {
          // Append(55,2,None)
          replace(55);
          append(2);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == '-')) {
          // Append(55,6,None)
          replace(55);
          append(6);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == '0')) {
          // Append(55,23,Some(55))
          replace(55);
          append(23);
          pendingFinish = 55;
          if (verbose) printStack();
          return true;
        }
        if (('1' <= c && c <= '9')) {
          // Append(55,22,Some(55))
          replace(55);
          append(22);
          pendingFinish = 55;
          if (verbose) printStack();
          return true;
        }
        if ((c == '[')) {
          // Append(55,3,None)
          replace(55);
          append(3);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == 'f')) {
          // Append(55,10,None)
          replace(55);
          append(10);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == 'n')) {
          // Append(55,7,None)
          replace(55);
          append(7);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == 't')) {
          // Append(55,5,None)
          replace(55);
          append(5);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == '{')) {
          // Append(55,9,None)
          replace(55);
          append(9);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        return false;
      case 50:
        if ((c == ']')) {
          // Finish(50)
          finish();
          if (verbose) printStack();
          return true;
        }
        return false;
      case 52:
        if ((c == ':')) {
          // Finish(52)
          finish();
          if (verbose) printStack();
          return true;
        }
        return false;
      case 55:
        if ((c == '"')) {
          // Append(55,2,None)
          append(2);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == '-')) {
          // Append(55,6,None)
          append(6);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == '0')) {
          // Append(55,23,Some(55))
          append(23);
          pendingFinish = 55;
          if (verbose) printStack();
          return true;
        }
        if (('1' <= c && c <= '9')) {
          // Append(55,22,Some(55))
          append(22);
          pendingFinish = 55;
          if (verbose) printStack();
          return true;
        }
        if ((c == '[')) {
          // Append(55,3,None)
          append(3);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == 'f')) {
          // Append(55,10,None)
          append(10);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == 'n')) {
          // Append(55,7,None)
          append(7);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == 't')) {
          // Append(55,5,None)
          append(5);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == '{')) {
          // Append(55,9,None)
          append(9);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        return false;
      case 56:
        if (('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ')) {
          // Append(11,12,Some(11))
          replace(11);
          append(12);
          pendingFinish = 11;
          if (verbose) printStack();
          return true;
        }
        if ((c == 'E') || (c == 'e')) {
          // Append(15,16,None)
          replace(15);
          append(16);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        return false;
      case 57:
        if ((c == 'l')) {
          // Finish(57)
          finish();
          if (verbose) printStack();
          return true;
        }
        return false;
      case 58:
        if (('0' <= c && c <= '9') || ('A' <= c && c <= 'F') || ('a' <= c && c <= 'f')) {
          // Finish(58)
          finish();
          if (verbose) printStack();
          return true;
        }
        return false;
      case 59:
        if ((c == 's')) {
          // Finish(59)
          finish();
          if (verbose) printStack();
          return true;
        }
        return false;
      case 60:
        if ((c == 'e')) {
          // Finish(60)
          finish();
          if (verbose) printStack();
          return true;
        }
        return false;
      case 61:
        if (('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ')) {
          // Append(62,12,Some(62))
          replace(62);
          append(12);
          pendingFinish = 62;
          if (verbose) printStack();
          return true;
        }
        if ((c == '"')) {
          // Append(63,2,None)
          replace(63);
          append(2);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == '-')) {
          // Append(63,6,None)
          replace(63);
          append(6);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == '0')) {
          // Append(63,23,Some(63))
          replace(63);
          append(23);
          pendingFinish = 63;
          if (verbose) printStack();
          return true;
        }
        if (('1' <= c && c <= '9')) {
          // Append(63,22,Some(63))
          replace(63);
          append(22);
          pendingFinish = 63;
          if (verbose) printStack();
          return true;
        }
        if ((c == '[')) {
          // Append(63,3,None)
          replace(63);
          append(3);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == 'f')) {
          // Append(63,10,None)
          replace(63);
          append(10);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == 'n')) {
          // Append(63,7,None)
          replace(63);
          append(7);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == 't')) {
          // Append(63,5,None)
          replace(63);
          append(5);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == '{')) {
          // Append(63,9,None)
          replace(63);
          append(9);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        return false;
      case 63:
        if ((c == '"')) {
          // Append(63,2,None)
          append(2);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == '-')) {
          // Append(63,6,None)
          append(6);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == '0')) {
          // Append(63,23,Some(63))
          append(23);
          pendingFinish = 63;
          if (verbose) printStack();
          return true;
        }
        if (('1' <= c && c <= '9')) {
          // Append(63,22,Some(63))
          append(22);
          pendingFinish = 63;
          if (verbose) printStack();
          return true;
        }
        if ((c == '[')) {
          // Append(63,3,None)
          append(3);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == 'f')) {
          // Append(63,10,None)
          append(10);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == 'n')) {
          // Append(63,7,None)
          append(7);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == 't')) {
          // Append(63,5,None)
          append(5);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == '{')) {
          // Append(63,9,None)
          append(9);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        return false;
      case 64:
        if (('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ')) {
          // Append(64,46,None)
          append(46);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == ',')) {
          // Append(64,49,None)
          append(49);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        return false;
      case 65:
        if ((c == 'e')) {
          // Finish(65)
          finish();
          if (verbose) printStack();
          return true;
        }
        return false;
      case 66:
        if (('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ')) {
          // Append(47,12,Some(47))
          replace(47);
          append(12);
          pendingFinish = 47;
          if (verbose) printStack();
          return true;
        }
        if ((c == ']')) {
          // Finish(50)
          replace(50);
          if (verbose) printStack();
          finish();
          if (verbose) printStack();
          return true;
        }
        return false;
      case 67:
        if (('0' <= c && c <= '9') || ('A' <= c && c <= 'F') || ('a' <= c && c <= 'f')) {
          // Finish(67)
          finish();
          if (verbose) printStack();
          return true;
        }
        return false;
      case 68:
        if (('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ')) {
          // Append(69,70,Some(71))
          replace(69);
          append(70);
          pendingFinish = 71;
          if (verbose) printStack();
          return true;
        }
        if ((c == ',')) {
          // Append(72,73,None)
          replace(72);
          append(73);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == '}')) {
          // Finish(74)
          replace(74);
          if (verbose) printStack();
          finish();
          if (verbose) printStack();
          return true;
        }
        return false;
      case 70:
        if (('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ')) {
          // Finish(12)
          replace(12);
          if (verbose) printStack();
          finish();
          if (verbose) printStack();
          return true;
        }
        if ((c == ',')) {
          // Finish(76)
          replace(76);
          if (verbose) printStack();
          finish();
          if (verbose) printStack();
          return true;
        }
        return false;
      case 73:
        if (('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ')) {
          // Append(77,12,Some(77))
          replace(77);
          append(12);
          pendingFinish = 77;
          if (verbose) printStack();
          return true;
        }
        if ((c == '"')) {
          // Append(78,2,None)
          replace(78);
          append(2);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        return false;
      case 74:
        if ((c == '}')) {
          // Finish(74)
          finish();
          if (verbose) printStack();
          return true;
        }
        return false;
      case 75:
        if (('0' <= c && c <= '9') || ('A' <= c && c <= 'F') || ('a' <= c && c <= 'f')) {
          // Finish(75)
          finish();
          if (verbose) printStack();
          return true;
        }
        return false;
      case 78:
        if ((c == '"')) {
          // Append(78,2,None)
          append(2);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        return false;
      case 79:
        if (('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ')) {
          // Append(79,70,None)
          append(70);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == ',')) {
          // Append(79,73,None)
          append(73);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        return false;
      case 80:
        if (('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ')) {
          // Append(71,12,Some(71))
          replace(71);
          append(12);
          pendingFinish = 71;
          if (verbose) printStack();
          return true;
        }
        if ((c == '}')) {
          // Finish(74)
          replace(74);
          if (verbose) printStack();
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
    CustomJsonParser parser = new CustomJsonParser(false);
    for (int i = 0; i < s.length(); i++) {
      if (!parser.proceed(s.charAt(i))) {
        return false;
      }
    }
    return parser.proceedEof();
  }

  public static boolean parseVerbose(String s) {
    CustomJsonParser parser = new CustomJsonParser(true);
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
    test("{\"abcd\": [\"hello\", 123, {\"xyz\": 1}]}");
    inputLoop();
  }
}
