package com.giyeok.jparser.parsergen;

public class JsonParser {
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

  public JsonParser(boolean verbose) {
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
            || (c == '0')
            || ('1' <= c && c <= '9')
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
        return (c == '"')
            || (c == '-')
            || (c == '0')
            || ('1' <= c && c <= '9')
            || (c == '[')
            || (c == 'f')
            || (c == 'n')
            || (c == 't')
            || (c == '{');
      case 13:
        return (c == '.') || (c == '0') || ('1' <= c && c <= '9') || (c == 'E') || (c == 'e');
      case 14:
        return (c == '.') || (c == 'E') || (c == 'e');
      case 15:
        return ('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ');
      case 17:
        return ('0' <= c && c <= '9');
      case 18:
        return (c == 'E') || (c == 'e');
      case 19:
        return (c == '+') || (c == '-') || ('0' <= c && c <= '9');
      case 20:
        return (c == '+') || (c == '-') || ('0' <= c && c <= '9');
      case 22:
        return (c == '"')
            || (c == '/')
            || (c == '\\')
            || (c == 'b')
            || (c == 'n')
            || (c == 'r')
            || (c == 't')
            || (c == 'u');
      case 23:
        return (c == '"');
      case 24:
        return (c == ' ')
            || ('0' <= c && c <= '9')
            || ('A' <= c && c <= 'Z')
            || ('a' <= c && c <= 'z')
            || (c == '\\');
      case 26:
        return (c == '0') || ('1' <= c && c <= '9');
      case 29:
        return ('\t' <= c && c <= '\n')
            || (c == '\r')
            || (c == ' ')
            || (c == ',')
            || (c == '.')
            || (c == '0')
            || ('1' <= c && c <= '9')
            || (c == 'E')
            || (c == 'e');
      case 31:
        return ('\t' <= c && c <= '\n')
            || (c == '\r')
            || (c == ' ')
            || (c == ',')
            || (c == '.')
            || (c == 'E')
            || (c == 'e');
      case 34:
        return ('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ') || (c == '"');
      case 39:
        return ('0' <= c && c <= '9');
      case 40:
        return ('0' <= c && c <= '9');
      case 42:
        return ('0' <= c && c <= '9');
      case 43:
        return (c == '"');
      case 45:
        return (c == ',');
      case 46:
        return (c == '0')
            || ('1' <= c && c <= '9')
            || ('A' <= c && c <= 'F')
            || ('a' <= c && c <= 'f');
      case 47:
        return ('\t' <= c && c <= '\n')
            || (c == '\r')
            || (c == ' ')
            || (c == ',')
            || (c == '.')
            || (c == '0')
            || ('1' <= c && c <= '9')
            || (c == 'E')
            || (c == 'e');
      case 48:
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
      case 49:
        return ('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ') || (c == ']');
      case 50:
        return ('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ') || (c == ':');
      case 51:
        return (c == 'l');
      case 52:
        return (c == 'l');
      case 53:
        return (c == 'u');
      case 54:
        return (c == '.') || (c == '0') || ('1' <= c && c <= '9') || (c == 'E') || (c == 'e');
      case 55:
        return ('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ') || (c == ',');
      case 56:
        return ('\t' <= c && c <= '\n')
            || (c == '\r')
            || (c == ' ')
            || (c == '.')
            || (c == '0')
            || ('1' <= c && c <= '9')
            || (c == 'E')
            || (c == 'e');
      case 57:
        return ('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ') || (c == '"') || (c == '}');
      case 59:
        return (c == ':');
      case 62:
        return (c == ']');
      case 63:
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
      case 65:
        return (c == '"')
            || (c == '-')
            || (c == '0')
            || ('1' <= c && c <= '9')
            || (c == '[')
            || (c == 'f')
            || (c == 'n')
            || (c == 't')
            || (c == '{');
      case 66:
        return (c == 's');
      case 67:
        return (c == 'l');
      case 68:
        return (c == 'e');
      case 69:
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
      case 70:
        return (c == '0')
            || ('1' <= c && c <= '9')
            || ('A' <= c && c <= 'F')
            || ('a' <= c && c <= 'f');
      case 71:
        return ('\t' <= c && c <= '\n')
            || (c == '\r')
            || (c == ' ')
            || (c == ',')
            || (c == 'E')
            || (c == 'e');
      case 72:
        return ('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ') || (c == 'E') || (c == 'e');
      case 74:
        return (c == '"')
            || (c == '-')
            || (c == '0')
            || ('1' <= c && c <= '9')
            || (c == '[')
            || (c == 'f')
            || (c == 'n')
            || (c == 't')
            || (c == '{');
      case 75:
        return (c == 'e');
      case 76:
        return ('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ') || (c == ',');
      case 77:
        return (c == '0')
            || ('1' <= c && c <= '9')
            || ('A' <= c && c <= 'F')
            || ('a' <= c && c <= 'f');
      case 79:
        return (c == ',');
      case 80:
        return ('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ') || (c == '}');
      case 82:
        return (c == '}');
      case 83:
        return ('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ') || (c == '"');
      case 84:
        return (c == '0')
            || ('1' <= c && c <= '9')
            || ('A' <= c && c <= 'F')
            || ('a' <= c && c <= 'f');
      case 86:
        return ('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ') || (c == '"');
    }
    throw new AssertionError("Unknown nodeId: " + stack.nodeId);
  }

  public String nodeDescriptionOf(int nodeId) {
    switch (nodeId) {
      case 0:
        return "{•<start>}";
      case 1:
        return "{ws•element ws|{\\t-\\n\\r\\u0020}•ws}";
      case 2:
        return "{'\"'•characters '\"'|'\"' characters•'\"'}";
      case 3:
        return "{'['•ws ']'|'[' ws•']'|'['•ws elements ws ']'|'[' ws•elements ws ']'}";
      case 4:
        return "{ws element•ws|int•frac exp|int frac•exp|onenine•digits}";
      case 5:
        return "{'t'•'r' 'u' 'e'}";
      case 6:
        return "{'-'•digit|'-'•onenine digits}";
      case 7:
        return "{'n'•'u' 'l' 'l'}";
      case 8:
        return "{ws element•ws|int•frac exp|int frac•exp}";
      case 9:
        return "{'{'•ws '}'|'{' ws•'}'|'{'•ws members ws '}'|'{' ws•members ws '}'}";
      case 10:
        return "{'f'•'a' 'l' 's' 'e'}";
      case 11:
        return "{{\\t-\\n\\r\\u0020}•ws}";
      case 12:
        return "{ws•element ws}";
      case 13:
        return "{int•frac exp|int frac•exp|onenine•digits}";
      case 14:
        return "{int•frac exp|int frac•exp}";
      case 15:
        return "{ws element•ws}";
      case 16:
        return "{int•frac exp}";
      case 17:
        return "{'.'•{0-9}+}";
      case 18:
        return "{int frac•exp}";
      case 19:
        return "{'e'•sign {0-9}+|'e' sign•{0-9}+}";
      case 20:
        return "{'E'•sign {0-9}+|'E' sign•{0-9}+}";
      case 21:
        return "{'\"'•characters '\"'}";
      case 22:
        return "{'\\'•escape}";
      case 23:
        return "{'\"' characters•'\"'}";
      case 24:
        return "{character•characters}";
      case 25:
        return "{onenine•digits}";
      case 26:
        return "{digit•digits}";
      case 27:
        return "{'['•ws ']'|'['•ws elements ws ']'}";
      case 28:
        return "{'[' ws•elements ws ']'}";
      case 29:
        return "{element•ws ',' ws elements|element ws•',' ws elements|int•frac exp|int frac•exp|onenine•digits}";
      case 30:
        return "{'[' ws•']'}";
      case 31:
        return "{element•ws ',' ws elements|element ws•',' ws elements|int•frac exp|int frac•exp}";
      case 32:
        return "{'-'•digit}";
      case 33:
        return "{'{'•ws '}'|'{'•ws members ws '}'|'{' ws•members ws '}'}";
      case 34:
        return "{{\\t-\\n\\r\\u0020}•ws|ws•string ws ':' ws element}";
      case 35:
        return "{'{'•ws '}'|'{'•ws members ws '}'}";
      case 36:
        return "{'{' ws•members ws '}'}";
      case 37:
        return "{'{' ws•'}'}";
      case 38:
        return "{'e'•sign {0-9}+}";
      case 39:
        return "{'e' sign•{0-9}+}";
      case 40:
        return "{{0-9}+•{0-9}}";
      case 41:
        return "{'E'•sign {0-9}+}";
      case 42:
        return "{'E' sign•{0-9}+}";
      case 43:
        return "{ws•string ws ':' ws element}";
      case 44:
        return "{element•ws ',' ws elements}";
      case 45:
        return "{element ws•',' ws elements}";
      case 46:
        return "{'u'•hex hex hex hex}";
      case 47:
        return "{element•ws ',' ws elements|element ws•',' ws elements|int•frac exp|int frac•exp|'-' onenine•digits}";
      case 48:
        return "{'[' ws•']'|'[' ws•elements ws ']'}";
      case 49:
        return "{'[' ws elements•ws ']'|'[' ws elements ws•']'}";
      case 50:
        return "{ws string•ws ':' ws element|ws string ws•':' ws element}";
      case 51:
        return "{'n' 'u'•'l' 'l'}";
      case 52:
        return "{'f' 'a'•'l' 's' 'e'}";
      case 53:
        return "{'t' 'r'•'u' 'e'}";
      case 54:
        return "{int•frac exp|int frac•exp|'-' onenine•digits}";
      case 55:
        return "{element•ws ',' ws elements|element ws•',' ws elements}";
      case 56:
        return "{ws element•ws|int•frac exp|int frac•exp|'-' onenine•digits}";
      case 57:
        return "{'{' ws•'}'|'{' ws•members ws '}'}";
      case 58:
        return "{ws string•ws ':' ws element}";
      case 59:
        return "{ws string ws•':' ws element}";
      case 60:
        return "{'-' onenine•digits}";
      case 61:
        return "{'[' ws elements•ws ']'}";
      case 62:
        return "{'[' ws elements ws•']'}";
      case 63:
        return "{element ws ','•ws elements|element ws ',' ws•elements}";
      case 64:
        return "{element ws ','•ws elements}";
      case 65:
        return "{element ws ',' ws•elements}";
      case 66:
        return "{'f' 'a' 'l'•'s' 'e'}";
      case 67:
        return "{'n' 'u' 'l'•'l'}";
      case 68:
        return "{'t' 'r' 'u'•'e'}";
      case 69:
        return "{ws string ws ':'•ws element|ws string ws ':' ws•element}";
      case 70:
        return "{'u' hex•hex hex hex}";
      case 71:
        return "{element•ws ',' ws elements|element ws•',' ws elements|int frac•exp}";
      case 72:
        return "{ws element•ws|int frac•exp}";
      case 73:
        return "{ws string ws ':'•ws element}";
      case 74:
        return "{ws string ws ':' ws•element}";
      case 75:
        return "{'f' 'a' 'l' 's'•'e'}";
      case 76:
        return "{member•ws ',' ws members|member ws•',' ws members}";
      case 77:
        return "{'u' hex hex•hex hex}";
      case 78:
        return "{member•ws ',' ws members}";
      case 79:
        return "{member ws•',' ws members}";
      case 80:
        return "{'{' ws members•ws '}'|'{' ws members ws•'}'}";
      case 81:
        return "{'{' ws members•ws '}'}";
      case 82:
        return "{'{' ws members ws•'}'}";
      case 83:
        return "{member ws ','•ws members|member ws ',' ws•members}";
      case 84:
        return "{'u' hex hex hex•hex}";
      case 85:
        return "{member ws ','•ws members}";
      case 86:
        return "{member ws ',' ws•members}";
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
    if (prev == 0 && last == 5) { // (0,5)
      // ReplaceEdge(0,53,None)
      replace(53);
      pendingFinish = -1;
      return false;
    }
    if (prev == 0 && last == 6) { // (0,6)
      // ReplaceEdge(0,56,Some(0))
      replace(56);
      pendingFinish = 0;
      return false;
    }
    if (prev == 0 && last == 7) { // (0,7)
      // ReplaceEdge(0,51,None)
      replace(51);
      pendingFinish = -1;
      return false;
    }
    if (prev == 0 && last == 10) { // (0,10)
      // ReplaceEdge(0,52,None)
      replace(52);
      pendingFinish = -1;
      return false;
    }
    if (prev == 0 && last == 11) { // (0,11)
      // ReplaceEdge(0,12,None)
      replace(12);
      pendingFinish = -1;
      return false;
    }
    if (prev == 0 && last == 12) { // (0,12)
      // ReplaceEdge(0,15,Some(0))
      replace(15);
      pendingFinish = 0;
      return false;
    }
    if (prev == 0 && last == 15) { // (0,15)
      // DropLast(0)
      dropLast();
      return true;
    }
    if (prev == 0 && last == 16) { // (0,16)
      // ReplaceEdge(0,72,Some(0))
      replace(72);
      pendingFinish = 0;
      return false;
    }
    if (prev == 0 && last == 18) { // (0,18)
      // ReplaceEdge(0,15,Some(0))
      replace(15);
      pendingFinish = 0;
      return false;
    }
    if (prev == 0 && last == 21) { // (0,21)
      // ReplaceEdge(0,23,None)
      replace(23);
      pendingFinish = -1;
      return false;
    }
    if (prev == 0 && last == 23) { // (0,23)
      // ReplaceEdge(0,15,Some(0))
      replace(15);
      pendingFinish = 0;
      return false;
    }
    if (prev == 0 && last == 25) { // (0,25)
      // ReplaceEdge(0,8,Some(0))
      replace(8);
      pendingFinish = 0;
      return false;
    }
    if (prev == 0 && last == 27) { // (0,27)
      // ReplaceEdge(0,48,None)
      replace(48);
      pendingFinish = -1;
      return false;
    }
    if (prev == 0 && last == 28) { // (0,28)
      // ReplaceEdge(0,49,None)
      replace(49);
      pendingFinish = -1;
      return false;
    }
    if (prev == 0 && last == 30) { // (0,30)
      // ReplaceEdge(0,15,Some(0))
      replace(15);
      pendingFinish = 0;
      return false;
    }
    if (prev == 0 && last == 32) { // (0,32)
      // ReplaceEdge(0,8,Some(0))
      replace(8);
      pendingFinish = 0;
      return false;
    }
    if (prev == 0 && last == 35) { // (0,35)
      // ReplaceEdge(0,57,None)
      replace(57);
      pendingFinish = -1;
      return false;
    }
    if (prev == 0 && last == 36) { // (0,36)
      // ReplaceEdge(0,80,None)
      replace(80);
      pendingFinish = -1;
      return false;
    }
    if (prev == 0 && last == 37) { // (0,37)
      // ReplaceEdge(0,15,Some(0))
      replace(15);
      pendingFinish = 0;
      return false;
    }
    if (prev == 0 && last == 51) { // (0,51)
      // ReplaceEdge(0,67,None)
      replace(67);
      pendingFinish = -1;
      return false;
    }
    if (prev == 0 && last == 52) { // (0,52)
      // ReplaceEdge(0,66,None)
      replace(66);
      pendingFinish = -1;
      return false;
    }
    if (prev == 0 && last == 53) { // (0,53)
      // ReplaceEdge(0,68,None)
      replace(68);
      pendingFinish = -1;
      return false;
    }
    if (prev == 0 && last == 60) { // (0,60)
      // ReplaceEdge(0,8,Some(0))
      replace(8);
      pendingFinish = 0;
      return false;
    }
    if (prev == 0 && last == 61) { // (0,61)
      // ReplaceEdge(0,62,None)
      replace(62);
      pendingFinish = -1;
      return false;
    }
    if (prev == 0 && last == 62) { // (0,62)
      // ReplaceEdge(0,15,Some(0))
      replace(15);
      pendingFinish = 0;
      return false;
    }
    if (prev == 0 && last == 66) { // (0,66)
      // ReplaceEdge(0,75,None)
      replace(75);
      pendingFinish = -1;
      return false;
    }
    if (prev == 0 && last == 67) { // (0,67)
      // ReplaceEdge(0,15,Some(0))
      replace(15);
      pendingFinish = 0;
      return false;
    }
    if (prev == 0 && last == 68) { // (0,68)
      // ReplaceEdge(0,15,Some(0))
      replace(15);
      pendingFinish = 0;
      return false;
    }
    if (prev == 0 && last == 75) { // (0,75)
      // ReplaceEdge(0,15,Some(0))
      replace(15);
      pendingFinish = 0;
      return false;
    }
    if (prev == 0 && last == 81) { // (0,81)
      // ReplaceEdge(0,82,None)
      replace(82);
      pendingFinish = -1;
      return false;
    }
    if (prev == 0 && last == 82) { // (0,82)
      // ReplaceEdge(0,15,Some(0))
      replace(15);
      pendingFinish = 0;
      return false;
    }
    if (prev == 11 && last == 11) { // (11,11)
      // DropLast(11)
      dropLast();
      return true;
    }
    if (prev == 12 && last == 5) { // (12,5)
      // ReplaceEdge(12,53,None)
      replace(53);
      pendingFinish = -1;
      return false;
    }
    if (prev == 12 && last == 6) { // (12,6)
      // ReplaceEdge(12,54,Some(12))
      replace(54);
      pendingFinish = 12;
      return false;
    }
    if (prev == 12 && last == 7) { // (12,7)
      // ReplaceEdge(12,51,None)
      replace(51);
      pendingFinish = -1;
      return false;
    }
    if (prev == 12 && last == 10) { // (12,10)
      // ReplaceEdge(12,52,None)
      replace(52);
      pendingFinish = -1;
      return false;
    }
    if (prev == 12 && last == 16) { // (12,16)
      // ReplaceEdge(12,18,Some(12))
      replace(18);
      pendingFinish = 12;
      return false;
    }
    if (prev == 12 && last == 18) { // (12,18)
      // DropLast(12)
      dropLast();
      return true;
    }
    if (prev == 12 && last == 21) { // (12,21)
      // ReplaceEdge(12,23,None)
      replace(23);
      pendingFinish = -1;
      return false;
    }
    if (prev == 12 && last == 23) { // (12,23)
      // DropLast(12)
      dropLast();
      return true;
    }
    if (prev == 12 && last == 25) { // (12,25)
      // ReplaceEdge(12,14,Some(12))
      replace(14);
      pendingFinish = 12;
      return false;
    }
    if (prev == 12 && last == 27) { // (12,27)
      // ReplaceEdge(12,48,None)
      replace(48);
      pendingFinish = -1;
      return false;
    }
    if (prev == 12 && last == 28) { // (12,28)
      // ReplaceEdge(12,49,None)
      replace(49);
      pendingFinish = -1;
      return false;
    }
    if (prev == 12 && last == 30) { // (12,30)
      // DropLast(12)
      dropLast();
      return true;
    }
    if (prev == 12 && last == 32) { // (12,32)
      // ReplaceEdge(12,14,Some(12))
      replace(14);
      pendingFinish = 12;
      return false;
    }
    if (prev == 12 && last == 35) { // (12,35)
      // ReplaceEdge(12,57,None)
      replace(57);
      pendingFinish = -1;
      return false;
    }
    if (prev == 12 && last == 36) { // (12,36)
      // ReplaceEdge(12,80,None)
      replace(80);
      pendingFinish = -1;
      return false;
    }
    if (prev == 12 && last == 37) { // (12,37)
      // DropLast(12)
      dropLast();
      return true;
    }
    if (prev == 12 && last == 51) { // (12,51)
      // ReplaceEdge(12,67,None)
      replace(67);
      pendingFinish = -1;
      return false;
    }
    if (prev == 12 && last == 52) { // (12,52)
      // ReplaceEdge(12,66,None)
      replace(66);
      pendingFinish = -1;
      return false;
    }
    if (prev == 12 && last == 53) { // (12,53)
      // ReplaceEdge(12,68,None)
      replace(68);
      pendingFinish = -1;
      return false;
    }
    if (prev == 12 && last == 60) { // (12,60)
      // ReplaceEdge(12,14,Some(12))
      replace(14);
      pendingFinish = 12;
      return false;
    }
    if (prev == 12 && last == 61) { // (12,61)
      // ReplaceEdge(12,62,None)
      replace(62);
      pendingFinish = -1;
      return false;
    }
    if (prev == 12 && last == 62) { // (12,62)
      // DropLast(12)
      dropLast();
      return true;
    }
    if (prev == 12 && last == 66) { // (12,66)
      // ReplaceEdge(12,75,None)
      replace(75);
      pendingFinish = -1;
      return false;
    }
    if (prev == 12 && last == 67) { // (12,67)
      // DropLast(12)
      dropLast();
      return true;
    }
    if (prev == 12 && last == 68) { // (12,68)
      // DropLast(12)
      dropLast();
      return true;
    }
    if (prev == 12 && last == 75) { // (12,75)
      // DropLast(12)
      dropLast();
      return true;
    }
    if (prev == 12 && last == 81) { // (12,81)
      // ReplaceEdge(12,82,None)
      replace(82);
      pendingFinish = -1;
      return false;
    }
    if (prev == 12 && last == 82) { // (12,82)
      // DropLast(12)
      dropLast();
      return true;
    }
    if (prev == 15 && last == 11) { // (15,11)
      // DropLast(15)
      dropLast();
      return true;
    }
    if (prev == 16 && last == 17) { // (16,17)
      // DropLast(16)
      dropLast();
      return true;
    }
    if (prev == 17 && last == 40) { // (17,40)
      // ReplaceEdge(17,40,Some(17))
      pendingFinish = 17;
      return false;
    }
    if (prev == 18 && last == 38) { // (18,38)
      // ReplaceEdge(18,39,None)
      replace(39);
      pendingFinish = -1;
      return false;
    }
    if (prev == 18 && last == 39) { // (18,39)
      // DropLast(18)
      dropLast();
      return true;
    }
    if (prev == 18 && last == 41) { // (18,41)
      // ReplaceEdge(18,42,None)
      replace(42);
      pendingFinish = -1;
      return false;
    }
    if (prev == 18 && last == 42) { // (18,42)
      // DropLast(18)
      dropLast();
      return true;
    }
    if (prev == 21 && last == 22) { // (21,22)
      // ReplaceEdge(21,24,Some(21))
      replace(24);
      pendingFinish = 21;
      return false;
    }
    if (prev == 21 && last == 24) { // (21,24)
      // DropLast(21)
      dropLast();
      return true;
    }
    if (prev == 22 && last == 46) { // (22,46)
      // ReplaceEdge(22,70,None)
      replace(70);
      pendingFinish = -1;
      return false;
    }
    if (prev == 22 && last == 70) { // (22,70)
      // ReplaceEdge(22,77,None)
      replace(77);
      pendingFinish = -1;
      return false;
    }
    if (prev == 22 && last == 77) { // (22,77)
      // ReplaceEdge(22,84,None)
      replace(84);
      pendingFinish = -1;
      return false;
    }
    if (prev == 22 && last == 84) { // (22,84)
      // DropLast(22)
      dropLast();
      return true;
    }
    if (prev == 24 && last == 22) { // (24,22)
      // ReplaceEdge(24,24,Some(24))
      replace(24);
      pendingFinish = 24;
      return false;
    }
    if (prev == 24 && last == 24) { // (24,24)
      // DropLast(24)
      dropLast();
      return true;
    }
    if (prev == 25 && last == 26) { // (25,26)
      // DropLast(25)
      dropLast();
      return true;
    }
    if (prev == 26 && last == 26) { // (26,26)
      // DropLast(26)
      dropLast();
      return true;
    }
    if (prev == 27 && last == 11) { // (27,11)
      // DropLast(27)
      dropLast();
      return true;
    }
    if (prev == 28 && last == 5) { // (28,5)
      // ReplaceEdge(28,53,None)
      replace(53);
      pendingFinish = -1;
      return false;
    }
    if (prev == 28 && last == 6) { // (28,6)
      // ReplaceEdge(28,47,Some(28))
      replace(47);
      pendingFinish = 28;
      return false;
    }
    if (prev == 28 && last == 7) { // (28,7)
      // ReplaceEdge(28,51,None)
      replace(51);
      pendingFinish = -1;
      return false;
    }
    if (prev == 28 && last == 10) { // (28,10)
      // ReplaceEdge(28,52,None)
      replace(52);
      pendingFinish = -1;
      return false;
    }
    if (prev == 28 && last == 16) { // (28,16)
      // ReplaceEdge(28,71,Some(28))
      replace(71);
      pendingFinish = 28;
      return false;
    }
    if (prev == 28 && last == 18) { // (28,18)
      // ReplaceEdge(28,55,Some(28))
      replace(55);
      pendingFinish = 28;
      return false;
    }
    if (prev == 28 && last == 21) { // (28,21)
      // ReplaceEdge(28,23,None)
      replace(23);
      pendingFinish = -1;
      return false;
    }
    if (prev == 28 && last == 23) { // (28,23)
      // ReplaceEdge(28,55,Some(28))
      replace(55);
      pendingFinish = 28;
      return false;
    }
    if (prev == 28 && last == 25) { // (28,25)
      // ReplaceEdge(28,31,Some(28))
      replace(31);
      pendingFinish = 28;
      return false;
    }
    if (prev == 28 && last == 27) { // (28,27)
      // ReplaceEdge(28,48,None)
      replace(48);
      pendingFinish = -1;
      return false;
    }
    if (prev == 28 && last == 28) { // (28,28)
      // DropLast(28)
      dropLast();
      return true;
    }
    if (prev == 28 && last == 30) { // (28,30)
      // ReplaceEdge(28,55,Some(28))
      replace(55);
      pendingFinish = 28;
      return false;
    }
    if (prev == 28 && last == 32) { // (28,32)
      // ReplaceEdge(28,31,Some(28))
      replace(31);
      pendingFinish = 28;
      return false;
    }
    if (prev == 28 && last == 35) { // (28,35)
      // ReplaceEdge(28,57,None)
      replace(57);
      pendingFinish = -1;
      return false;
    }
    if (prev == 28 && last == 36) { // (28,36)
      // ReplaceEdge(28,80,None)
      replace(80);
      pendingFinish = -1;
      return false;
    }
    if (prev == 28 && last == 37) { // (28,37)
      // ReplaceEdge(28,55,Some(28))
      replace(55);
      pendingFinish = 28;
      return false;
    }
    if (prev == 28 && last == 44) { // (28,44)
      // ReplaceEdge(28,45,None)
      replace(45);
      pendingFinish = -1;
      return false;
    }
    if (prev == 28 && last == 45) { // (28,45)
      // ReplaceEdge(28,63,None)
      replace(63);
      pendingFinish = -1;
      return false;
    }
    if (prev == 28 && last == 51) { // (28,51)
      // ReplaceEdge(28,67,None)
      replace(67);
      pendingFinish = -1;
      return false;
    }
    if (prev == 28 && last == 52) { // (28,52)
      // ReplaceEdge(28,66,None)
      replace(66);
      pendingFinish = -1;
      return false;
    }
    if (prev == 28 && last == 53) { // (28,53)
      // ReplaceEdge(28,68,None)
      replace(68);
      pendingFinish = -1;
      return false;
    }
    if (prev == 28 && last == 60) { // (28,60)
      // ReplaceEdge(28,31,Some(28))
      replace(31);
      pendingFinish = 28;
      return false;
    }
    if (prev == 28 && last == 64) { // (28,64)
      // ReplaceEdge(28,65,None)
      replace(65);
      pendingFinish = -1;
      return false;
    }
    if (prev == 28 && last == 65) { // (28,65)
      // DropLast(28)
      dropLast();
      return true;
    }
    if (prev == 28 && last == 66) { // (28,66)
      // ReplaceEdge(28,75,None)
      replace(75);
      pendingFinish = -1;
      return false;
    }
    if (prev == 28 && last == 67) { // (28,67)
      // ReplaceEdge(28,55,Some(28))
      replace(55);
      pendingFinish = 28;
      return false;
    }
    if (prev == 28 && last == 68) { // (28,68)
      // ReplaceEdge(28,55,Some(28))
      replace(55);
      pendingFinish = 28;
      return false;
    }
    if (prev == 28 && last == 75) { // (28,75)
      // ReplaceEdge(28,55,Some(28))
      replace(55);
      pendingFinish = 28;
      return false;
    }
    if (prev == 28 && last == 81) { // (28,81)
      // ReplaceEdge(28,82,None)
      replace(82);
      pendingFinish = -1;
      return false;
    }
    if (prev == 28 && last == 82) { // (28,82)
      // ReplaceEdge(28,55,Some(28))
      replace(55);
      pendingFinish = 28;
      return false;
    }
    if (prev == 33 && last == 11) { // (33,11)
      // ReplaceEdge(33,43,Some(35))
      replace(43);
      pendingFinish = 35;
      return false;
    }
    if (prev == 33 && last == 43) { // (33,43)
      // ReplaceEdge(36,50,None)
      dropLast();
      replace(36);
      append(50);
      pendingFinish = -1;
      return false;
    }
    if (prev == 36 && last == 11) { // (36,11)
      // ReplaceEdge(36,43,None)
      replace(43);
      pendingFinish = -1;
      return false;
    }
    if (prev == 36 && last == 21) { // (36,21)
      // ReplaceEdge(36,23,None)
      replace(23);
      pendingFinish = -1;
      return false;
    }
    if (prev == 36 && last == 23) { // (36,23)
      // ReplaceEdge(36,50,None)
      replace(50);
      pendingFinish = -1;
      return false;
    }
    if (prev == 36 && last == 43) { // (36,43)
      // ReplaceEdge(36,50,None)
      replace(50);
      pendingFinish = -1;
      return false;
    }
    if (prev == 36 && last == 58) { // (36,58)
      // ReplaceEdge(36,59,None)
      replace(59);
      pendingFinish = -1;
      return false;
    }
    if (prev == 36 && last == 59) { // (36,59)
      // ReplaceEdge(36,69,None)
      replace(69);
      pendingFinish = -1;
      return false;
    }
    if (prev == 36 && last == 73) { // (36,73)
      // ReplaceEdge(36,74,None)
      replace(74);
      pendingFinish = -1;
      return false;
    }
    if (prev == 36 && last == 74) { // (36,74)
      // ReplaceEdge(36,76,Some(36))
      replace(76);
      pendingFinish = 36;
      return false;
    }
    if (prev == 36 && last == 78) { // (36,78)
      // ReplaceEdge(36,79,None)
      replace(79);
      pendingFinish = -1;
      return false;
    }
    if (prev == 36 && last == 79) { // (36,79)
      // ReplaceEdge(36,83,None)
      replace(83);
      pendingFinish = -1;
      return false;
    }
    if (prev == 36 && last == 85) { // (36,85)
      // ReplaceEdge(36,86,None)
      replace(86);
      pendingFinish = -1;
      return false;
    }
    if (prev == 36 && last == 86) { // (36,86)
      // DropLast(36)
      dropLast();
      return true;
    }
    if (prev == 39 && last == 40) { // (39,40)
      // ReplaceEdge(39,40,Some(39))
      pendingFinish = 39;
      return false;
    }
    if (prev == 42 && last == 40) { // (42,40)
      // ReplaceEdge(42,40,Some(42))
      pendingFinish = 42;
      return false;
    }
    if (prev == 43 && last == 21) { // (43,21)
      // ReplaceEdge(43,23,None)
      replace(23);
      pendingFinish = -1;
      return false;
    }
    if (prev == 43 && last == 23) { // (43,23)
      // DropLast(43)
      dropLast();
      return true;
    }
    if (prev == 44 && last == 11) { // (44,11)
      // DropLast(44)
      dropLast();
      return true;
    }
    if (prev == 58 && last == 11) { // (58,11)
      // DropLast(58)
      dropLast();
      return true;
    }
    if (prev == 60 && last == 26) { // (60,26)
      // DropLast(60)
      dropLast();
      return true;
    }
    if (prev == 61 && last == 11) { // (61,11)
      // DropLast(61)
      dropLast();
      return true;
    }
    if (prev == 64 && last == 11) { // (64,11)
      // DropLast(64)
      dropLast();
      return true;
    }
    if (prev == 65 && last == 5) { // (65,5)
      // ReplaceEdge(65,53,None)
      replace(53);
      pendingFinish = -1;
      return false;
    }
    if (prev == 65 && last == 6) { // (65,6)
      // ReplaceEdge(65,47,Some(65))
      replace(47);
      pendingFinish = 65;
      return false;
    }
    if (prev == 65 && last == 7) { // (65,7)
      // ReplaceEdge(65,51,None)
      replace(51);
      pendingFinish = -1;
      return false;
    }
    if (prev == 65 && last == 10) { // (65,10)
      // ReplaceEdge(65,52,None)
      replace(52);
      pendingFinish = -1;
      return false;
    }
    if (prev == 65 && last == 16) { // (65,16)
      // ReplaceEdge(65,71,Some(65))
      replace(71);
      pendingFinish = 65;
      return false;
    }
    if (prev == 65 && last == 18) { // (65,18)
      // ReplaceEdge(65,55,Some(65))
      replace(55);
      pendingFinish = 65;
      return false;
    }
    if (prev == 65 && last == 21) { // (65,21)
      // ReplaceEdge(65,23,None)
      replace(23);
      pendingFinish = -1;
      return false;
    }
    if (prev == 65 && last == 23) { // (65,23)
      // ReplaceEdge(65,55,Some(65))
      replace(55);
      pendingFinish = 65;
      return false;
    }
    if (prev == 65 && last == 25) { // (65,25)
      // ReplaceEdge(65,31,Some(65))
      replace(31);
      pendingFinish = 65;
      return false;
    }
    if (prev == 65 && last == 27) { // (65,27)
      // ReplaceEdge(65,48,None)
      replace(48);
      pendingFinish = -1;
      return false;
    }
    if (prev == 65 && last == 28) { // (65,28)
      // ReplaceEdge(65,49,None)
      replace(49);
      pendingFinish = -1;
      return false;
    }
    if (prev == 65 && last == 30) { // (65,30)
      // ReplaceEdge(65,55,Some(65))
      replace(55);
      pendingFinish = 65;
      return false;
    }
    if (prev == 65 && last == 32) { // (65,32)
      // ReplaceEdge(65,31,Some(65))
      replace(31);
      pendingFinish = 65;
      return false;
    }
    if (prev == 65 && last == 35) { // (65,35)
      // ReplaceEdge(65,57,None)
      replace(57);
      pendingFinish = -1;
      return false;
    }
    if (prev == 65 && last == 36) { // (65,36)
      // ReplaceEdge(65,80,None)
      replace(80);
      pendingFinish = -1;
      return false;
    }
    if (prev == 65 && last == 37) { // (65,37)
      // ReplaceEdge(65,55,Some(65))
      replace(55);
      pendingFinish = 65;
      return false;
    }
    if (prev == 65 && last == 44) { // (65,44)
      // ReplaceEdge(65,45,None)
      replace(45);
      pendingFinish = -1;
      return false;
    }
    if (prev == 65 && last == 45) { // (65,45)
      // ReplaceEdge(65,63,None)
      replace(63);
      pendingFinish = -1;
      return false;
    }
    if (prev == 65 && last == 51) { // (65,51)
      // ReplaceEdge(65,67,None)
      replace(67);
      pendingFinish = -1;
      return false;
    }
    if (prev == 65 && last == 52) { // (65,52)
      // ReplaceEdge(65,66,None)
      replace(66);
      pendingFinish = -1;
      return false;
    }
    if (prev == 65 && last == 53) { // (65,53)
      // ReplaceEdge(65,68,None)
      replace(68);
      pendingFinish = -1;
      return false;
    }
    if (prev == 65 && last == 60) { // (65,60)
      // ReplaceEdge(65,31,Some(65))
      replace(31);
      pendingFinish = 65;
      return false;
    }
    if (prev == 65 && last == 61) { // (65,61)
      // ReplaceEdge(65,62,None)
      replace(62);
      pendingFinish = -1;
      return false;
    }
    if (prev == 65 && last == 62) { // (65,62)
      // ReplaceEdge(65,55,Some(65))
      replace(55);
      pendingFinish = 65;
      return false;
    }
    if (prev == 65 && last == 64) { // (65,64)
      // ReplaceEdge(65,65,None)
      replace(65);
      pendingFinish = -1;
      return false;
    }
    if (prev == 65 && last == 65) { // (65,65)
      // DropLast(65)
      dropLast();
      return true;
    }
    if (prev == 65 && last == 66) { // (65,66)
      // ReplaceEdge(65,75,None)
      replace(75);
      pendingFinish = -1;
      return false;
    }
    if (prev == 65 && last == 67) { // (65,67)
      // ReplaceEdge(65,55,Some(65))
      replace(55);
      pendingFinish = 65;
      return false;
    }
    if (prev == 65 && last == 68) { // (65,68)
      // ReplaceEdge(65,55,Some(65))
      replace(55);
      pendingFinish = 65;
      return false;
    }
    if (prev == 65 && last == 75) { // (65,75)
      // ReplaceEdge(65,55,Some(65))
      replace(55);
      pendingFinish = 65;
      return false;
    }
    if (prev == 65 && last == 81) { // (65,81)
      // ReplaceEdge(65,82,None)
      replace(82);
      pendingFinish = -1;
      return false;
    }
    if (prev == 65 && last == 82) { // (65,82)
      // ReplaceEdge(65,55,Some(65))
      replace(55);
      pendingFinish = 65;
      return false;
    }
    if (prev == 73 && last == 11) { // (73,11)
      // DropLast(73)
      dropLast();
      return true;
    }
    if (prev == 74 && last == 5) { // (74,5)
      // ReplaceEdge(74,53,None)
      replace(53);
      pendingFinish = -1;
      return false;
    }
    if (prev == 74 && last == 6) { // (74,6)
      // ReplaceEdge(74,54,Some(74))
      replace(54);
      pendingFinish = 74;
      return false;
    }
    if (prev == 74 && last == 7) { // (74,7)
      // ReplaceEdge(74,51,None)
      replace(51);
      pendingFinish = -1;
      return false;
    }
    if (prev == 74 && last == 10) { // (74,10)
      // ReplaceEdge(74,52,None)
      replace(52);
      pendingFinish = -1;
      return false;
    }
    if (prev == 74 && last == 16) { // (74,16)
      // ReplaceEdge(74,18,Some(74))
      replace(18);
      pendingFinish = 74;
      return false;
    }
    if (prev == 74 && last == 18) { // (74,18)
      // DropLast(74)
      dropLast();
      return true;
    }
    if (prev == 74 && last == 21) { // (74,21)
      // ReplaceEdge(74,23,None)
      replace(23);
      pendingFinish = -1;
      return false;
    }
    if (prev == 74 && last == 23) { // (74,23)
      // DropLast(74)
      dropLast();
      return true;
    }
    if (prev == 74 && last == 25) { // (74,25)
      // ReplaceEdge(74,14,Some(74))
      replace(14);
      pendingFinish = 74;
      return false;
    }
    if (prev == 74 && last == 27) { // (74,27)
      // ReplaceEdge(74,48,None)
      replace(48);
      pendingFinish = -1;
      return false;
    }
    if (prev == 74 && last == 28) { // (74,28)
      // ReplaceEdge(74,49,None)
      replace(49);
      pendingFinish = -1;
      return false;
    }
    if (prev == 74 && last == 30) { // (74,30)
      // DropLast(74)
      dropLast();
      return true;
    }
    if (prev == 74 && last == 32) { // (74,32)
      // ReplaceEdge(74,14,Some(74))
      replace(14);
      pendingFinish = 74;
      return false;
    }
    if (prev == 74 && last == 35) { // (74,35)
      // ReplaceEdge(74,57,None)
      replace(57);
      pendingFinish = -1;
      return false;
    }
    if (prev == 74 && last == 36) { // (74,36)
      // ReplaceEdge(74,80,None)
      replace(80);
      pendingFinish = -1;
      return false;
    }
    if (prev == 74 && last == 37) { // (74,37)
      // DropLast(74)
      dropLast();
      return true;
    }
    if (prev == 74 && last == 51) { // (74,51)
      // ReplaceEdge(74,67,None)
      replace(67);
      pendingFinish = -1;
      return false;
    }
    if (prev == 74 && last == 52) { // (74,52)
      // ReplaceEdge(74,66,None)
      replace(66);
      pendingFinish = -1;
      return false;
    }
    if (prev == 74 && last == 53) { // (74,53)
      // ReplaceEdge(74,68,None)
      replace(68);
      pendingFinish = -1;
      return false;
    }
    if (prev == 74 && last == 60) { // (74,60)
      // ReplaceEdge(74,14,Some(74))
      replace(14);
      pendingFinish = 74;
      return false;
    }
    if (prev == 74 && last == 61) { // (74,61)
      // ReplaceEdge(74,62,None)
      replace(62);
      pendingFinish = -1;
      return false;
    }
    if (prev == 74 && last == 62) { // (74,62)
      // DropLast(74)
      dropLast();
      return true;
    }
    if (prev == 74 && last == 66) { // (74,66)
      // ReplaceEdge(74,75,None)
      replace(75);
      pendingFinish = -1;
      return false;
    }
    if (prev == 74 && last == 67) { // (74,67)
      // DropLast(74)
      dropLast();
      return true;
    }
    if (prev == 74 && last == 68) { // (74,68)
      // DropLast(74)
      dropLast();
      return true;
    }
    if (prev == 74 && last == 75) { // (74,75)
      // DropLast(74)
      dropLast();
      return true;
    }
    if (prev == 74 && last == 81) { // (74,81)
      // ReplaceEdge(74,82,None)
      replace(82);
      pendingFinish = -1;
      return false;
    }
    if (prev == 74 && last == 82) { // (74,82)
      // DropLast(74)
      dropLast();
      return true;
    }
    if (prev == 78 && last == 11) { // (78,11)
      // DropLast(78)
      dropLast();
      return true;
    }
    if (prev == 81 && last == 11) { // (81,11)
      // DropLast(81)
      dropLast();
      return true;
    }
    if (prev == 83 && last == 11) { // (83,11)
      // ReplaceEdge(83,43,Some(85))
      replace(43);
      pendingFinish = 85;
      return false;
    }
    if (prev == 83 && last == 43) { // (83,43)
      // ReplaceEdge(86,50,None)
      dropLast();
      replace(86);
      append(50);
      pendingFinish = -1;
      return false;
    }
    if (prev == 86 && last == 11) { // (86,11)
      // ReplaceEdge(86,43,None)
      replace(43);
      pendingFinish = -1;
      return false;
    }
    if (prev == 86 && last == 21) { // (86,21)
      // ReplaceEdge(86,23,None)
      replace(23);
      pendingFinish = -1;
      return false;
    }
    if (prev == 86 && last == 23) { // (86,23)
      // ReplaceEdge(86,50,None)
      replace(50);
      pendingFinish = -1;
      return false;
    }
    if (prev == 86 && last == 43) { // (86,43)
      // ReplaceEdge(86,50,None)
      replace(50);
      pendingFinish = -1;
      return false;
    }
    if (prev == 86 && last == 58) { // (86,58)
      // ReplaceEdge(86,59,None)
      replace(59);
      pendingFinish = -1;
      return false;
    }
    if (prev == 86 && last == 59) { // (86,59)
      // ReplaceEdge(86,69,None)
      replace(69);
      pendingFinish = -1;
      return false;
    }
    if (prev == 86 && last == 73) { // (86,73)
      // ReplaceEdge(86,74,None)
      replace(74);
      pendingFinish = -1;
      return false;
    }
    if (prev == 86 && last == 74) { // (86,74)
      // ReplaceEdge(86,76,Some(86))
      replace(76);
      pendingFinish = 86;
      return false;
    }
    if (prev == 86 && last == 78) { // (86,78)
      // ReplaceEdge(86,79,None)
      replace(79);
      pendingFinish = -1;
      return false;
    }
    if (prev == 86 && last == 79) { // (86,79)
      // ReplaceEdge(86,83,None)
      replace(83);
      pendingFinish = -1;
      return false;
    }
    if (prev == 86 && last == 85) { // (86,85)
      // ReplaceEdge(86,86,None)
      replace(86);
      pendingFinish = -1;
      return false;
    }
    if (prev == 86 && last == 86) { // (86,86)
      // DropLast(86)
      dropLast();
      return true;
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
          // Append(11,11,Some(11))
          replace(11);
          append(11);
          pendingFinish = 11;
          if (verbose) printStack();
          return true;
        }
        if ((c == '"')) {
          // Append(12,2,None)
          replace(12);
          append(2);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == '-')) {
          // Append(12,6,None)
          replace(12);
          append(6);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == '0')) {
          // Append(12,14,Some(12))
          replace(12);
          append(14);
          pendingFinish = 12;
          if (verbose) printStack();
          return true;
        }
        if (('1' <= c && c <= '9')) {
          // Append(12,13,Some(12))
          replace(12);
          append(13);
          pendingFinish = 12;
          if (verbose) printStack();
          return true;
        }
        if ((c == '[')) {
          // Append(12,3,None)
          replace(12);
          append(3);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == 'f')) {
          // Append(12,10,None)
          replace(12);
          append(10);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == 'n')) {
          // Append(12,7,None)
          replace(12);
          append(7);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == 't')) {
          // Append(12,5,None)
          replace(12);
          append(5);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == '{')) {
          // Append(12,9,None)
          replace(12);
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
          // Append(21,24,Some(21))
          replace(21);
          append(24);
          pendingFinish = 21;
          if (verbose) printStack();
          return true;
        }
        if ((c == '"')) {
          // Finish(23)
          replace(23);
          if (verbose) printStack();
          finish();
          if (verbose) printStack();
          return true;
        }
        if ((c == '\\')) {
          // Append(21,22,None)
          replace(21);
          append(22);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        return false;
      case 3:
        if (('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ')) {
          // Append(27,11,Some(27))
          replace(27);
          append(11);
          pendingFinish = 27;
          if (verbose) printStack();
          return true;
        }
        if ((c == '"')) {
          // Append(28,2,None)
          replace(28);
          append(2);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == '-')) {
          // Append(28,6,None)
          replace(28);
          append(6);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == '0')) {
          // Append(28,31,Some(28))
          replace(28);
          append(31);
          pendingFinish = 28;
          if (verbose) printStack();
          return true;
        }
        if (('1' <= c && c <= '9')) {
          // Append(28,29,Some(28))
          replace(28);
          append(29);
          pendingFinish = 28;
          if (verbose) printStack();
          return true;
        }
        if ((c == '[')) {
          // Append(28,3,None)
          replace(28);
          append(3);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == ']')) {
          // Finish(30)
          replace(30);
          if (verbose) printStack();
          finish();
          if (verbose) printStack();
          return true;
        }
        if ((c == 'f')) {
          // Append(28,10,None)
          replace(28);
          append(10);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == 'n')) {
          // Append(28,7,None)
          replace(28);
          append(7);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == 't')) {
          // Append(28,5,None)
          replace(28);
          append(5);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == '{')) {
          // Append(28,9,None)
          replace(28);
          append(9);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        return false;
      case 4:
        if (('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ')) {
          // Append(15,11,Some(15))
          replace(15);
          append(11);
          pendingFinish = 15;
          if (verbose) printStack();
          return true;
        }
        if ((c == '.')) {
          // Append(16,17,None)
          replace(16);
          append(17);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == '0')) {
          // Append(25,26,Some(25))
          replace(25);
          append(26);
          pendingFinish = 25;
          if (verbose) printStack();
          return true;
        }
        if (('1' <= c && c <= '9')) {
          // Append(25,26,Some(25))
          replace(25);
          append(26);
          pendingFinish = 25;
          if (verbose) printStack();
          return true;
        }
        if ((c == 'E')) {
          // Append(18,20,None)
          replace(18);
          append(20);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == 'e')) {
          // Append(18,19,None)
          replace(18);
          append(19);
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
          // Finish(32)
          replace(32);
          if (verbose) printStack();
          finish();
          if (verbose) printStack();
          return true;
        }
        if (('1' <= c && c <= '9')) {
          // Finish(6)
          finish();
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
          // Append(15,11,Some(15))
          replace(15);
          append(11);
          pendingFinish = 15;
          if (verbose) printStack();
          return true;
        }
        if ((c == '.')) {
          // Append(16,17,None)
          replace(16);
          append(17);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == 'E')) {
          // Append(18,20,None)
          replace(18);
          append(20);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == 'e')) {
          // Append(18,19,None)
          replace(18);
          append(19);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        return false;
      case 9:
        if (('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ')) {
          // Append(33,34,Some(35))
          replace(33);
          append(34);
          pendingFinish = 35;
          if (verbose) printStack();
          return true;
        }
        if ((c == '"')) {
          // Append(36,2,None)
          replace(36);
          append(2);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == '}')) {
          // Finish(37)
          replace(37);
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
          // Append(11,11,Some(11))
          append(11);
          pendingFinish = 11;
          if (verbose) printStack();
          return true;
        }
        return false;
      case 12:
        if ((c == '"')) {
          // Append(12,2,None)
          append(2);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == '-')) {
          // Append(12,6,None)
          append(6);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == '0')) {
          // Append(12,14,Some(12))
          append(14);
          pendingFinish = 12;
          if (verbose) printStack();
          return true;
        }
        if (('1' <= c && c <= '9')) {
          // Append(12,13,Some(12))
          append(13);
          pendingFinish = 12;
          if (verbose) printStack();
          return true;
        }
        if ((c == '[')) {
          // Append(12,3,None)
          append(3);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == 'f')) {
          // Append(12,10,None)
          append(10);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == 'n')) {
          // Append(12,7,None)
          append(7);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == 't')) {
          // Append(12,5,None)
          append(5);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == '{')) {
          // Append(12,9,None)
          append(9);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        return false;
      case 13:
        if ((c == '.')) {
          // Append(16,17,None)
          replace(16);
          append(17);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == '0')) {
          // Append(25,26,Some(25))
          replace(25);
          append(26);
          pendingFinish = 25;
          if (verbose) printStack();
          return true;
        }
        if (('1' <= c && c <= '9')) {
          // Append(25,26,Some(25))
          replace(25);
          append(26);
          pendingFinish = 25;
          if (verbose) printStack();
          return true;
        }
        if ((c == 'E')) {
          // Append(18,20,None)
          replace(18);
          append(20);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == 'e')) {
          // Append(18,19,None)
          replace(18);
          append(19);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        return false;
      case 14:
        if ((c == '.')) {
          // Append(16,17,None)
          replace(16);
          append(17);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == 'E')) {
          // Append(18,20,None)
          replace(18);
          append(20);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == 'e')) {
          // Append(18,19,None)
          replace(18);
          append(19);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        return false;
      case 15:
        if (('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ')) {
          // Append(15,11,Some(15))
          append(11);
          pendingFinish = 15;
          if (verbose) printStack();
          return true;
        }
        return false;
      case 17:
        if (('0' <= c && c <= '9')) {
          // Append(17,40,Some(17))
          append(40);
          pendingFinish = 17;
          if (verbose) printStack();
          return true;
        }
        return false;
      case 18:
        if ((c == 'E')) {
          // Append(18,20,None)
          append(20);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == 'e')) {
          // Append(18,19,None)
          append(19);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        return false;
      case 19:
        if ((c == '+')) {
          // Finish(38)
          replace(38);
          if (verbose) printStack();
          finish();
          if (verbose) printStack();
          return true;
        }
        if ((c == '-')) {
          // Finish(38)
          replace(38);
          if (verbose) printStack();
          finish();
          if (verbose) printStack();
          return true;
        }
        if (('0' <= c && c <= '9')) {
          // Append(39,40,Some(39))
          replace(39);
          append(40);
          pendingFinish = 39;
          if (verbose) printStack();
          return true;
        }
        return false;
      case 20:
        if ((c == '+')) {
          // Finish(41)
          replace(41);
          if (verbose) printStack();
          finish();
          if (verbose) printStack();
          return true;
        }
        if ((c == '-')) {
          // Finish(41)
          replace(41);
          if (verbose) printStack();
          finish();
          if (verbose) printStack();
          return true;
        }
        if (('0' <= c && c <= '9')) {
          // Append(42,40,Some(42))
          replace(42);
          append(40);
          pendingFinish = 42;
          if (verbose) printStack();
          return true;
        }
        return false;
      case 22:
        if ((c == '"')
            || (c == '/')
            || (c == '\\')
            || (c == 'b')
            || (c == 'n')
            || (c == 'r')
            || (c == 't')) {
          // Finish(22)
          finish();
          if (verbose) printStack();
          return true;
        }
        if ((c == 'u')) {
          // Append(22,46,None)
          append(46);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        return false;
      case 23:
        if ((c == '"')) {
          // Finish(23)
          finish();
          if (verbose) printStack();
          return true;
        }
        return false;
      case 24:
        if ((c == ' ')
            || ('0' <= c && c <= '9')
            || ('A' <= c && c <= 'Z')
            || ('a' <= c && c <= 'z')) {
          // Append(24,24,Some(24))
          append(24);
          pendingFinish = 24;
          if (verbose) printStack();
          return true;
        }
        if ((c == '\\')) {
          // Append(24,22,None)
          append(22);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        return false;
      case 26:
        if ((c == '0')) {
          // Append(26,26,Some(26))
          append(26);
          pendingFinish = 26;
          if (verbose) printStack();
          return true;
        }
        if (('1' <= c && c <= '9')) {
          // Append(26,26,Some(26))
          append(26);
          pendingFinish = 26;
          if (verbose) printStack();
          return true;
        }
        return false;
      case 29:
        if (('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ')) {
          // Append(44,11,Some(44))
          replace(44);
          append(11);
          pendingFinish = 44;
          if (verbose) printStack();
          return true;
        }
        if ((c == ',')) {
          // Finish(45)
          replace(45);
          if (verbose) printStack();
          finish();
          if (verbose) printStack();
          return true;
        }
        if ((c == '.')) {
          // Append(16,17,None)
          replace(16);
          append(17);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == '0')) {
          // Append(25,26,Some(25))
          replace(25);
          append(26);
          pendingFinish = 25;
          if (verbose) printStack();
          return true;
        }
        if (('1' <= c && c <= '9')) {
          // Append(25,26,Some(25))
          replace(25);
          append(26);
          pendingFinish = 25;
          if (verbose) printStack();
          return true;
        }
        if ((c == 'E')) {
          // Append(18,20,None)
          replace(18);
          append(20);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == 'e')) {
          // Append(18,19,None)
          replace(18);
          append(19);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        return false;
      case 31:
        if (('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ')) {
          // Append(44,11,Some(44))
          replace(44);
          append(11);
          pendingFinish = 44;
          if (verbose) printStack();
          return true;
        }
        if ((c == ',')) {
          // Finish(45)
          replace(45);
          if (verbose) printStack();
          finish();
          if (verbose) printStack();
          return true;
        }
        if ((c == '.')) {
          // Append(16,17,None)
          replace(16);
          append(17);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == 'E')) {
          // Append(18,20,None)
          replace(18);
          append(20);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == 'e')) {
          // Append(18,19,None)
          replace(18);
          append(19);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        return false;
      case 34:
        if (('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ')) {
          // Append(11,11,Some(11))
          replace(11);
          append(11);
          pendingFinish = 11;
          if (verbose) printStack();
          return true;
        }
        if ((c == '"')) {
          // Append(43,2,None)
          replace(43);
          append(2);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        return false;
      case 39:
        if (('0' <= c && c <= '9')) {
          // Append(39,40,Some(39))
          append(40);
          pendingFinish = 39;
          if (verbose) printStack();
          return true;
        }
        return false;
      case 40:
        if (('0' <= c && c <= '9')) {
          // Finish(40)
          finish();
          if (verbose) printStack();
          return true;
        }
        return false;
      case 42:
        if (('0' <= c && c <= '9')) {
          // Append(42,40,Some(42))
          append(40);
          pendingFinish = 42;
          if (verbose) printStack();
          return true;
        }
        return false;
      case 43:
        if ((c == '"')) {
          // Append(43,2,None)
          append(2);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        return false;
      case 45:
        if ((c == ',')) {
          // Finish(45)
          finish();
          if (verbose) printStack();
          return true;
        }
        return false;
      case 46:
        if ((c == '0')) {
          // Finish(46)
          finish();
          if (verbose) printStack();
          return true;
        }
        if (('1' <= c && c <= '9')) {
          // Finish(46)
          finish();
          if (verbose) printStack();
          return true;
        }
        if (('A' <= c && c <= 'F') || ('a' <= c && c <= 'f')) {
          // Finish(46)
          finish();
          if (verbose) printStack();
          return true;
        }
        return false;
      case 47:
        if (('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ')) {
          // Append(44,11,Some(44))
          replace(44);
          append(11);
          pendingFinish = 44;
          if (verbose) printStack();
          return true;
        }
        if ((c == ',')) {
          // Finish(45)
          replace(45);
          if (verbose) printStack();
          finish();
          if (verbose) printStack();
          return true;
        }
        if ((c == '.')) {
          // Append(16,17,None)
          replace(16);
          append(17);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == '0')) {
          // Append(60,26,Some(60))
          replace(60);
          append(26);
          pendingFinish = 60;
          if (verbose) printStack();
          return true;
        }
        if (('1' <= c && c <= '9')) {
          // Append(60,26,Some(60))
          replace(60);
          append(26);
          pendingFinish = 60;
          if (verbose) printStack();
          return true;
        }
        if ((c == 'E')) {
          // Append(18,20,None)
          replace(18);
          append(20);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == 'e')) {
          // Append(18,19,None)
          replace(18);
          append(19);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        return false;
      case 48:
        if ((c == '"')) {
          // Append(28,2,None)
          replace(28);
          append(2);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == '-')) {
          // Append(28,6,None)
          replace(28);
          append(6);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == '0')) {
          // Append(28,31,Some(28))
          replace(28);
          append(31);
          pendingFinish = 28;
          if (verbose) printStack();
          return true;
        }
        if (('1' <= c && c <= '9')) {
          // Append(28,29,Some(28))
          replace(28);
          append(29);
          pendingFinish = 28;
          if (verbose) printStack();
          return true;
        }
        if ((c == '[')) {
          // Append(28,3,None)
          replace(28);
          append(3);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == ']')) {
          // Finish(30)
          replace(30);
          if (verbose) printStack();
          finish();
          if (verbose) printStack();
          return true;
        }
        if ((c == 'f')) {
          // Append(28,10,None)
          replace(28);
          append(10);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == 'n')) {
          // Append(28,7,None)
          replace(28);
          append(7);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == 't')) {
          // Append(28,5,None)
          replace(28);
          append(5);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == '{')) {
          // Append(28,9,None)
          replace(28);
          append(9);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        return false;
      case 49:
        if (('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ')) {
          // Append(61,11,Some(61))
          replace(61);
          append(11);
          pendingFinish = 61;
          if (verbose) printStack();
          return true;
        }
        if ((c == ']')) {
          // Finish(62)
          replace(62);
          if (verbose) printStack();
          finish();
          if (verbose) printStack();
          return true;
        }
        return false;
      case 50:
        if (('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ')) {
          // Append(58,11,Some(58))
          replace(58);
          append(11);
          pendingFinish = 58;
          if (verbose) printStack();
          return true;
        }
        if ((c == ':')) {
          // Finish(59)
          replace(59);
          if (verbose) printStack();
          finish();
          if (verbose) printStack();
          return true;
        }
        return false;
      case 51:
        if ((c == 'l')) {
          // Finish(51)
          finish();
          if (verbose) printStack();
          return true;
        }
        return false;
      case 52:
        if ((c == 'l')) {
          // Finish(52)
          finish();
          if (verbose) printStack();
          return true;
        }
        return false;
      case 53:
        if ((c == 'u')) {
          // Finish(53)
          finish();
          if (verbose) printStack();
          return true;
        }
        return false;
      case 54:
        if ((c == '.')) {
          // Append(16,17,None)
          replace(16);
          append(17);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == '0')) {
          // Append(60,26,Some(60))
          replace(60);
          append(26);
          pendingFinish = 60;
          if (verbose) printStack();
          return true;
        }
        if (('1' <= c && c <= '9')) {
          // Append(60,26,Some(60))
          replace(60);
          append(26);
          pendingFinish = 60;
          if (verbose) printStack();
          return true;
        }
        if ((c == 'E')) {
          // Append(18,20,None)
          replace(18);
          append(20);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == 'e')) {
          // Append(18,19,None)
          replace(18);
          append(19);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        return false;
      case 55:
        if (('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ')) {
          // Append(44,11,Some(44))
          replace(44);
          append(11);
          pendingFinish = 44;
          if (verbose) printStack();
          return true;
        }
        if ((c == ',')) {
          // Finish(45)
          replace(45);
          if (verbose) printStack();
          finish();
          if (verbose) printStack();
          return true;
        }
        return false;
      case 56:
        if (('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ')) {
          // Append(15,11,Some(15))
          replace(15);
          append(11);
          pendingFinish = 15;
          if (verbose) printStack();
          return true;
        }
        if ((c == '.')) {
          // Append(16,17,None)
          replace(16);
          append(17);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == '0')) {
          // Append(60,26,Some(60))
          replace(60);
          append(26);
          pendingFinish = 60;
          if (verbose) printStack();
          return true;
        }
        if (('1' <= c && c <= '9')) {
          // Append(60,26,Some(60))
          replace(60);
          append(26);
          pendingFinish = 60;
          if (verbose) printStack();
          return true;
        }
        if ((c == 'E')) {
          // Append(18,20,None)
          replace(18);
          append(20);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == 'e')) {
          // Append(18,19,None)
          replace(18);
          append(19);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        return false;
      case 57:
        if (('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ')) {
          // Append(36,34,None)
          replace(36);
          append(34);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == '"')) {
          // Append(36,2,None)
          replace(36);
          append(2);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == '}')) {
          // Finish(37)
          replace(37);
          if (verbose) printStack();
          finish();
          if (verbose) printStack();
          return true;
        }
        return false;
      case 59:
        if ((c == ':')) {
          // Finish(59)
          finish();
          if (verbose) printStack();
          return true;
        }
        return false;
      case 62:
        if ((c == ']')) {
          // Finish(62)
          finish();
          if (verbose) printStack();
          return true;
        }
        return false;
      case 63:
        if (('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ')) {
          // Append(64,11,Some(64))
          replace(64);
          append(11);
          pendingFinish = 64;
          if (verbose) printStack();
          return true;
        }
        if ((c == '"')) {
          // Append(65,2,None)
          replace(65);
          append(2);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == '-')) {
          // Append(65,6,None)
          replace(65);
          append(6);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == '0')) {
          // Append(65,31,Some(65))
          replace(65);
          append(31);
          pendingFinish = 65;
          if (verbose) printStack();
          return true;
        }
        if (('1' <= c && c <= '9')) {
          // Append(65,29,Some(65))
          replace(65);
          append(29);
          pendingFinish = 65;
          if (verbose) printStack();
          return true;
        }
        if ((c == '[')) {
          // Append(65,3,None)
          replace(65);
          append(3);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == 'f')) {
          // Append(65,10,None)
          replace(65);
          append(10);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == 'n')) {
          // Append(65,7,None)
          replace(65);
          append(7);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == 't')) {
          // Append(65,5,None)
          replace(65);
          append(5);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == '{')) {
          // Append(65,9,None)
          replace(65);
          append(9);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        return false;
      case 65:
        if ((c == '"')) {
          // Append(65,2,None)
          append(2);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == '-')) {
          // Append(65,6,None)
          append(6);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == '0')) {
          // Append(65,31,Some(65))
          append(31);
          pendingFinish = 65;
          if (verbose) printStack();
          return true;
        }
        if (('1' <= c && c <= '9')) {
          // Append(65,29,Some(65))
          append(29);
          pendingFinish = 65;
          if (verbose) printStack();
          return true;
        }
        if ((c == '[')) {
          // Append(65,3,None)
          append(3);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == 'f')) {
          // Append(65,10,None)
          append(10);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == 'n')) {
          // Append(65,7,None)
          append(7);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == 't')) {
          // Append(65,5,None)
          append(5);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == '{')) {
          // Append(65,9,None)
          append(9);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        return false;
      case 66:
        if ((c == 's')) {
          // Finish(66)
          finish();
          if (verbose) printStack();
          return true;
        }
        return false;
      case 67:
        if ((c == 'l')) {
          // Finish(67)
          finish();
          if (verbose) printStack();
          return true;
        }
        return false;
      case 68:
        if ((c == 'e')) {
          // Finish(68)
          finish();
          if (verbose) printStack();
          return true;
        }
        return false;
      case 69:
        if (('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ')) {
          // Append(73,11,Some(73))
          replace(73);
          append(11);
          pendingFinish = 73;
          if (verbose) printStack();
          return true;
        }
        if ((c == '"')) {
          // Append(74,2,None)
          replace(74);
          append(2);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == '-')) {
          // Append(74,6,None)
          replace(74);
          append(6);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == '0')) {
          // Append(74,14,Some(74))
          replace(74);
          append(14);
          pendingFinish = 74;
          if (verbose) printStack();
          return true;
        }
        if (('1' <= c && c <= '9')) {
          // Append(74,13,Some(74))
          replace(74);
          append(13);
          pendingFinish = 74;
          if (verbose) printStack();
          return true;
        }
        if ((c == '[')) {
          // Append(74,3,None)
          replace(74);
          append(3);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == 'f')) {
          // Append(74,10,None)
          replace(74);
          append(10);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == 'n')) {
          // Append(74,7,None)
          replace(74);
          append(7);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == 't')) {
          // Append(74,5,None)
          replace(74);
          append(5);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == '{')) {
          // Append(74,9,None)
          replace(74);
          append(9);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        return false;
      case 70:
        if ((c == '0')) {
          // Finish(70)
          finish();
          if (verbose) printStack();
          return true;
        }
        if (('1' <= c && c <= '9')) {
          // Finish(70)
          finish();
          if (verbose) printStack();
          return true;
        }
        if (('A' <= c && c <= 'F') || ('a' <= c && c <= 'f')) {
          // Finish(70)
          finish();
          if (verbose) printStack();
          return true;
        }
        return false;
      case 71:
        if (('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ')) {
          // Append(44,11,Some(44))
          replace(44);
          append(11);
          pendingFinish = 44;
          if (verbose) printStack();
          return true;
        }
        if ((c == ',')) {
          // Finish(45)
          replace(45);
          if (verbose) printStack();
          finish();
          if (verbose) printStack();
          return true;
        }
        if ((c == 'E')) {
          // Append(18,20,None)
          replace(18);
          append(20);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == 'e')) {
          // Append(18,19,None)
          replace(18);
          append(19);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        return false;
      case 72:
        if (('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ')) {
          // Append(15,11,Some(15))
          replace(15);
          append(11);
          pendingFinish = 15;
          if (verbose) printStack();
          return true;
        }
        if ((c == 'E')) {
          // Append(18,20,None)
          replace(18);
          append(20);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == 'e')) {
          // Append(18,19,None)
          replace(18);
          append(19);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        return false;
      case 74:
        if ((c == '"')) {
          // Append(74,2,None)
          append(2);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == '-')) {
          // Append(74,6,None)
          append(6);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == '0')) {
          // Append(74,14,Some(74))
          append(14);
          pendingFinish = 74;
          if (verbose) printStack();
          return true;
        }
        if (('1' <= c && c <= '9')) {
          // Append(74,13,Some(74))
          append(13);
          pendingFinish = 74;
          if (verbose) printStack();
          return true;
        }
        if ((c == '[')) {
          // Append(74,3,None)
          append(3);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == 'f')) {
          // Append(74,10,None)
          append(10);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == 'n')) {
          // Append(74,7,None)
          append(7);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == 't')) {
          // Append(74,5,None)
          append(5);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == '{')) {
          // Append(74,9,None)
          append(9);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        return false;
      case 75:
        if ((c == 'e')) {
          // Finish(75)
          finish();
          if (verbose) printStack();
          return true;
        }
        return false;
      case 76:
        if (('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ')) {
          // Append(78,11,Some(78))
          replace(78);
          append(11);
          pendingFinish = 78;
          if (verbose) printStack();
          return true;
        }
        if ((c == ',')) {
          // Finish(79)
          replace(79);
          if (verbose) printStack();
          finish();
          if (verbose) printStack();
          return true;
        }
        return false;
      case 77:
        if ((c == '0')) {
          // Finish(77)
          finish();
          if (verbose) printStack();
          return true;
        }
        if (('1' <= c && c <= '9')) {
          // Finish(77)
          finish();
          if (verbose) printStack();
          return true;
        }
        if (('A' <= c && c <= 'F') || ('a' <= c && c <= 'f')) {
          // Finish(77)
          finish();
          if (verbose) printStack();
          return true;
        }
        return false;
      case 79:
        if ((c == ',')) {
          // Finish(79)
          finish();
          if (verbose) printStack();
          return true;
        }
        return false;
      case 80:
        if (('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ')) {
          // Append(81,11,Some(81))
          replace(81);
          append(11);
          pendingFinish = 81;
          if (verbose) printStack();
          return true;
        }
        if ((c == '}')) {
          // Finish(82)
          replace(82);
          if (verbose) printStack();
          finish();
          if (verbose) printStack();
          return true;
        }
        return false;
      case 82:
        if ((c == '}')) {
          // Finish(82)
          finish();
          if (verbose) printStack();
          return true;
        }
        return false;
      case 83:
        if (('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ')) {
          // Append(83,34,Some(85))
          append(34);
          pendingFinish = 85;
          if (verbose) printStack();
          return true;
        }
        if ((c == '"')) {
          // Append(86,2,None)
          replace(86);
          append(2);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        return false;
      case 84:
        if ((c == '0')) {
          // Finish(84)
          finish();
          if (verbose) printStack();
          return true;
        }
        if (('1' <= c && c <= '9')) {
          // Finish(84)
          finish();
          if (verbose) printStack();
          return true;
        }
        if (('A' <= c && c <= 'F') || ('a' <= c && c <= 'f')) {
          // Finish(84)
          finish();
          if (verbose) printStack();
          return true;
        }
        return false;
      case 86:
        if (('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ')) {
          // Append(86,34,None)
          append(34);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == '"')) {
          // Append(86,2,None)
          append(2);
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
    JsonParser parser = new JsonParser(false);
    for (int i = 0; i < s.length(); i++) {
      if (!parser.proceed(s.charAt(i))) {
        return false;
      }
    }
    return parser.proceedEof();
  }

  public static boolean parseVerbose(String s) {
    JsonParser parser = new JsonParser(true);
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

    log("Test \"{\"abcd\": [\"hello\", 123, {\"xyz\": 1}]}\"");
    succeed = parseVerbose("{\"abcd\": [\"hello\", 123, {\"xyz\": 1}]}");
    log("Parsing " + (succeed ? "succeeded" : "failed"));
  }
}
