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
      case 21:
        return (c == ' ')
            || ('0' <= c && c <= '9')
            || ('A' <= c && c <= 'Z')
            || ('a' <= c && c <= 'z')
            || (c == '"')
            || (c == '\\');
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
      case 25:
        return (c == '0') || ('1' <= c && c <= '9');
      case 26:
        return (c == '0') || ('1' <= c && c <= '9');
      case 27:
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
      case 28:
        return (c == '"')
            || (c == '-')
            || (c == '0')
            || ('1' <= c && c <= '9')
            || (c == '[')
            || (c == 'f')
            || (c == 'n')
            || (c == 't')
            || (c == '{');
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
      case 35:
        return ('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ') || (c == '"') || (c == '}');
      case 36:
        return ('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ') || (c == '"');
      case 40:
        return ('0' <= c && c <= '9');
      case 41:
        return ('0' <= c && c <= '9');
      case 43:
        return ('0' <= c && c <= '9');
      case 44:
        return ('\t' <= c && c <= '\n')
            || (c == '\r')
            || (c == ' ')
            || (c == ',')
            || (c == '.')
            || (c == '0')
            || ('1' <= c && c <= '9')
            || (c == 'E')
            || (c == ']')
            || (c == 'e');
      case 45:
        return ('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ') || (c == ',') || (c == ']');
      case 46:
        return ('\t' <= c && c <= '\n')
            || (c == '\r')
            || (c == ' ')
            || (c == ',')
            || (c == '.')
            || (c == 'E')
            || (c == ']')
            || (c == 'e');
      case 47:
        return (c == '"');
      case 48:
        return ('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ') || (c == ',');
      case 49:
        return (c == ',');
      case 50:
        return (c == '0')
            || ('1' <= c && c <= '9')
            || ('A' <= c && c <= 'F')
            || ('a' <= c && c <= 'f');
      case 51:
        return ('\t' <= c && c <= '\n')
            || (c == '\r')
            || (c == ' ')
            || (c == ',')
            || (c == '.')
            || (c == '0')
            || ('1' <= c && c <= '9')
            || (c == 'E')
            || (c == 'e');
      case 52:
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
      case 53:
        return ('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ') || (c == ']');
      case 54:
        return ('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ') || (c == ':');
      case 55:
        return (c == 'l');
      case 56:
        return (c == 'l');
      case 57:
        return (c == 'u');
      case 58:
        return (c == '.') || (c == '0') || ('1' <= c && c <= '9') || (c == 'E') || (c == 'e');
      case 59:
        return ('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ') || (c == ',');
      case 60:
        return ('\t' <= c && c <= '\n')
            || (c == '\r')
            || (c == ' ')
            || (c == '.')
            || (c == '0')
            || ('1' <= c && c <= '9')
            || (c == 'E')
            || (c == 'e');
      case 61:
        return ('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ') || (c == '"') || (c == '}');
      case 62:
        return ('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ') || (c == ':');
      case 63:
        return (c == ':');
      case 64:
        return (c == '0') || ('1' <= c && c <= '9');
      case 65:
        return ('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ') || (c == ']');
      case 66:
        return (c == ']');
      case 67:
        return ('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ') || (c == ',') || (c == ']');
      case 68:
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
        return (c == '"')
            || (c == '-')
            || (c == '0')
            || ('1' <= c && c <= '9')
            || (c == '[')
            || (c == 'f')
            || (c == 'n')
            || (c == 't')
            || (c == '{');
      case 71:
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
      case 72:
        return (c == 's');
      case 73:
        return (c == 'l');
      case 74:
        return (c == 'e');
      case 75:
        return (c == '0')
            || ('1' <= c && c <= '9')
            || ('A' <= c && c <= 'F')
            || ('a' <= c && c <= 'f');
      case 76:
        return ('\t' <= c && c <= '\n')
            || (c == '\r')
            || (c == ' ')
            || (c == ',')
            || (c == 'E')
            || (c == 'e');
      case 77:
        return ('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ') || (c == 'E') || (c == 'e');
      case 78:
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
      case 79:
        return (c == '"')
            || (c == '-')
            || (c == '0')
            || ('1' <= c && c <= '9')
            || (c == '[')
            || (c == 'f')
            || (c == 'n')
            || (c == 't')
            || (c == '{');
      case 80:
        return (c == 'e');
      case 81:
        return ('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ') || (c == ',');
      case 82:
        return (c == '0')
            || ('1' <= c && c <= '9')
            || ('A' <= c && c <= 'F')
            || ('a' <= c && c <= 'f');
      case 83:
        return ('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ') || (c == ',');
      case 84:
        return (c == ',');
      case 85:
        return ('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ') || (c == '}');
      case 86:
        return ('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ') || (c == '"');
      case 87:
        return ('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ') || (c == '}');
      case 88:
        return (c == '}');
      case 89:
        return (c == '0')
            || ('1' <= c && c <= '9')
            || ('A' <= c && c <= 'F')
            || ('a' <= c && c <= 'f');
      case 90:
        return ('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ') || (c == '"');
      case 91:
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
        return "{}";
      case 39:
        return "{'e'•sign {0-9}+}";
      case 40:
        return "{'e' sign•{0-9}+}";
      case 41:
        return "{{0-9}+•{0-9}}";
      case 42:
        return "{'E'•sign {0-9}+}";
      case 43:
        return "{'E' sign•{0-9}+}";
      case 44:
        return "{'[' ws elements•ws ']'|'[' ws elements ws•']'|element•ws ',' ws elements|element ws•',' ws elements|int•frac exp|int frac•exp|onenine•digits}";
      case 45:
        return "{'[' ws elements•ws ']'|'[' ws elements ws•']'|element•ws ',' ws elements|element ws•',' ws elements}";
      case 46:
        return "{'[' ws elements•ws ']'|'[' ws elements ws•']'|element•ws ',' ws elements|element ws•',' ws elements|int•frac exp|int frac•exp}";
      case 47:
        return "{ws•string ws ':' ws element}";
      case 48:
        return "{element•ws ',' ws elements}";
      case 49:
        return "{element ws•',' ws elements}";
      case 50:
        return "{'u'•hex hex hex hex}";
      case 51:
        return "{element•ws ',' ws elements|element ws•',' ws elements|int•frac exp|int frac•exp|'-' onenine•digits}";
      case 52:
        return "{'[' ws•']'|'[' ws•elements ws ']'}";
      case 53:
        return "{'[' ws elements•ws ']'|'[' ws elements ws•']'}";
      case 54:
        return "{ws string•ws ':' ws element|ws string ws•':' ws element}";
      case 55:
        return "{'n' 'u'•'l' 'l'}";
      case 56:
        return "{'f' 'a'•'l' 's' 'e'}";
      case 57:
        return "{'t' 'r'•'u' 'e'}";
      case 58:
        return "{int•frac exp|int frac•exp|'-' onenine•digits}";
      case 59:
        return "{element•ws ',' ws elements|element ws•',' ws elements}";
      case 60:
        return "{ws element•ws|int•frac exp|int frac•exp|'-' onenine•digits}";
      case 61:
        return "{'{' ws•'}'|'{' ws•members ws '}'}";
      case 62:
        return "{ws string•ws ':' ws element}";
      case 63:
        return "{ws string ws•':' ws element}";
      case 64:
        return "{'-' onenine•digits}";
      case 65:
        return "{'[' ws elements•ws ']'}";
      case 66:
        return "{'[' ws elements ws•']'}";
      case 67:
        return "{'[' ws elements•ws ']'|element•ws ',' ws elements}";
      case 68:
        return "{element ws ','•ws elements|element ws ',' ws•elements}";
      case 69:
        return "{element ws ','•ws elements}";
      case 70:
        return "{element ws ',' ws•elements}";
      case 71:
        return "{ws string ws ':'•ws element|ws string ws ':' ws•element}";
      case 72:
        return "{'f' 'a' 'l'•'s' 'e'}";
      case 73:
        return "{'n' 'u' 'l'•'l'}";
      case 74:
        return "{'t' 'r' 'u'•'e'}";
      case 75:
        return "{'u' hex•hex hex hex}";
      case 76:
        return "{element•ws ',' ws elements|element ws•',' ws elements|int frac•exp}";
      case 77:
        return "{ws element•ws|int frac•exp}";
      case 78:
        return "{ws string ws ':'•ws element}";
      case 79:
        return "{ws string ws ':' ws•element}";
      case 80:
        return "{'f' 'a' 'l' 's'•'e'}";
      case 81:
        return "{member•ws ',' ws members|member ws•',' ws members}";
      case 82:
        return "{'u' hex hex•hex hex}";
      case 83:
        return "{member•ws ',' ws members}";
      case 84:
        return "{member ws•',' ws members}";
      case 85:
        return "{'{' ws members•ws '}'|'{' ws members ws•'}'}";
      case 86:
        return "{member ws ','•ws members|member ws ',' ws•members}";
      case 87:
        return "{'{' ws members•ws '}'}";
      case 88:
        return "{'{' ws members ws•'}'}";
      case 89:
        return "{'u' hex hex hex•hex}";
      case 90:
        return "{member ws ','•ws members}";
      case 91:
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
      // ReplaceEdge(0,57,None)
      replace(57);
      pendingFinish = -1;
      return false;
    }
    if (prev == 0 && last == 6) { // (0,6)
      // ReplaceEdge(0,60,Some(0))
      replace(60);
      pendingFinish = 0;
      return false;
    }
    if (prev == 0 && last == 7) { // (0,7)
      // ReplaceEdge(0,55,None)
      replace(55);
      pendingFinish = -1;
      return false;
    }
    if (prev == 0 && last == 10) { // (0,10)
      // ReplaceEdge(0,56,None)
      replace(56);
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
      // ReplaceEdge(0,77,Some(0))
      replace(77);
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
      // ReplaceEdge(0,52,None)
      replace(52);
      pendingFinish = -1;
      return false;
    }
    if (prev == 0 && last == 28) { // (0,28)
      // ReplaceEdge(0,53,None)
      replace(53);
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
      // ReplaceEdge(0,61,None)
      replace(61);
      pendingFinish = -1;
      return false;
    }
    if (prev == 0 && last == 36) { // (0,36)
      // ReplaceEdge(0,85,None)
      replace(85);
      pendingFinish = -1;
      return false;
    }
    if (prev == 0 && last == 37) { // (0,37)
      // ReplaceEdge(0,15,Some(0))
      replace(15);
      pendingFinish = 0;
      return false;
    }
    if (prev == 0 && last == 38) { // (0,38)
      // DropLast(38)
      dropLast();
      replace(38);
      return true;
    }
    if (prev == 0 && last == 55) { // (0,55)
      // ReplaceEdge(0,73,None)
      replace(73);
      pendingFinish = -1;
      return false;
    }
    if (prev == 0 && last == 56) { // (0,56)
      // ReplaceEdge(0,72,None)
      replace(72);
      pendingFinish = -1;
      return false;
    }
    if (prev == 0 && last == 57) { // (0,57)
      // ReplaceEdge(0,74,None)
      replace(74);
      pendingFinish = -1;
      return false;
    }
    if (prev == 0 && last == 64) { // (0,64)
      // ReplaceEdge(0,8,Some(0))
      replace(8);
      pendingFinish = 0;
      return false;
    }
    if (prev == 0 && last == 65) { // (0,65)
      // ReplaceEdge(0,66,None)
      replace(66);
      pendingFinish = -1;
      return false;
    }
    if (prev == 0 && last == 66) { // (0,66)
      // ReplaceEdge(0,15,Some(0))
      replace(15);
      pendingFinish = 0;
      return false;
    }
    if (prev == 0 && last == 72) { // (0,72)
      // ReplaceEdge(0,80,None)
      replace(80);
      pendingFinish = -1;
      return false;
    }
    if (prev == 0 && last == 73) { // (0,73)
      // ReplaceEdge(0,15,Some(0))
      replace(15);
      pendingFinish = 0;
      return false;
    }
    if (prev == 0 && last == 74) { // (0,74)
      // ReplaceEdge(0,15,Some(0))
      replace(15);
      pendingFinish = 0;
      return false;
    }
    if (prev == 0 && last == 80) { // (0,80)
      // ReplaceEdge(0,15,Some(0))
      replace(15);
      pendingFinish = 0;
      return false;
    }
    if (prev == 0 && last == 87) { // (0,87)
      // ReplaceEdge(0,88,None)
      replace(88);
      pendingFinish = -1;
      return false;
    }
    if (prev == 0 && last == 88) { // (0,88)
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
      // ReplaceEdge(12,57,None)
      replace(57);
      pendingFinish = -1;
      return false;
    }
    if (prev == 12 && last == 6) { // (12,6)
      // ReplaceEdge(12,58,Some(12))
      replace(58);
      pendingFinish = 12;
      return false;
    }
    if (prev == 12 && last == 7) { // (12,7)
      // ReplaceEdge(12,55,None)
      replace(55);
      pendingFinish = -1;
      return false;
    }
    if (prev == 12 && last == 10) { // (12,10)
      // ReplaceEdge(12,56,None)
      replace(56);
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
      // ReplaceEdge(12,52,None)
      replace(52);
      pendingFinish = -1;
      return false;
    }
    if (prev == 12 && last == 28) { // (12,28)
      // ReplaceEdge(12,53,None)
      replace(53);
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
      // ReplaceEdge(12,61,None)
      replace(61);
      pendingFinish = -1;
      return false;
    }
    if (prev == 12 && last == 36) { // (12,36)
      // ReplaceEdge(12,85,None)
      replace(85);
      pendingFinish = -1;
      return false;
    }
    if (prev == 12 && last == 37) { // (12,37)
      // DropLast(12)
      dropLast();
      return true;
    }
    if (prev == 12 && last == 38) { // (12,38)
      // DropLast(38)
      dropLast();
      replace(38);
      return true;
    }
    if (prev == 12 && last == 55) { // (12,55)
      // ReplaceEdge(12,73,None)
      replace(73);
      pendingFinish = -1;
      return false;
    }
    if (prev == 12 && last == 56) { // (12,56)
      // ReplaceEdge(12,72,None)
      replace(72);
      pendingFinish = -1;
      return false;
    }
    if (prev == 12 && last == 57) { // (12,57)
      // ReplaceEdge(12,74,None)
      replace(74);
      pendingFinish = -1;
      return false;
    }
    if (prev == 12 && last == 64) { // (12,64)
      // ReplaceEdge(12,14,Some(12))
      replace(14);
      pendingFinish = 12;
      return false;
    }
    if (prev == 12 && last == 65) { // (12,65)
      // ReplaceEdge(12,66,None)
      replace(66);
      pendingFinish = -1;
      return false;
    }
    if (prev == 12 && last == 66) { // (12,66)
      // DropLast(12)
      dropLast();
      return true;
    }
    if (prev == 12 && last == 72) { // (12,72)
      // ReplaceEdge(12,80,None)
      replace(80);
      pendingFinish = -1;
      return false;
    }
    if (prev == 12 && last == 73) { // (12,73)
      // DropLast(12)
      dropLast();
      return true;
    }
    if (prev == 12 && last == 74) { // (12,74)
      // DropLast(12)
      dropLast();
      return true;
    }
    if (prev == 12 && last == 80) { // (12,80)
      // DropLast(12)
      dropLast();
      return true;
    }
    if (prev == 12 && last == 87) { // (12,87)
      // ReplaceEdge(12,88,None)
      replace(88);
      pendingFinish = -1;
      return false;
    }
    if (prev == 12 && last == 88) { // (12,88)
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
    if (prev == 17 && last == 41) { // (17,41)
      // ReplaceEdge(17,41,Some(17))
      pendingFinish = 17;
      return false;
    }
    if (prev == 18 && last == 39) { // (18,39)
      // ReplaceEdge(18,40,None)
      replace(40);
      pendingFinish = -1;
      return false;
    }
    if (prev == 18 && last == 40) { // (18,40)
      // DropLast(18)
      dropLast();
      return true;
    }
    if (prev == 18 && last == 42) { // (18,42)
      // ReplaceEdge(18,43,None)
      replace(43);
      pendingFinish = -1;
      return false;
    }
    if (prev == 18 && last == 43) { // (18,43)
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
    if (prev == 22 && last == 50) { // (22,50)
      // ReplaceEdge(22,75,None)
      replace(75);
      pendingFinish = -1;
      return false;
    }
    if (prev == 22 && last == 75) { // (22,75)
      // ReplaceEdge(22,82,None)
      replace(82);
      pendingFinish = -1;
      return false;
    }
    if (prev == 22 && last == 82) { // (22,82)
      // ReplaceEdge(22,89,None)
      replace(89);
      pendingFinish = -1;
      return false;
    }
    if (prev == 22 && last == 89) { // (22,89)
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
      // ReplaceEdge(28,57,None)
      replace(57);
      pendingFinish = -1;
      return false;
    }
    if (prev == 28 && last == 6) { // (28,6)
      // ReplaceEdge(28,51,Some(28))
      replace(51);
      pendingFinish = 28;
      return false;
    }
    if (prev == 28 && last == 7) { // (28,7)
      // ReplaceEdge(28,55,None)
      replace(55);
      pendingFinish = -1;
      return false;
    }
    if (prev == 28 && last == 10) { // (28,10)
      // ReplaceEdge(28,56,None)
      replace(56);
      pendingFinish = -1;
      return false;
    }
    if (prev == 28 && last == 16) { // (28,16)
      // ReplaceEdge(28,76,Some(28))
      replace(76);
      pendingFinish = 28;
      return false;
    }
    if (prev == 28 && last == 18) { // (28,18)
      // ReplaceEdge(28,59,Some(28))
      replace(59);
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
      // ReplaceEdge(28,59,Some(28))
      replace(59);
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
      // ReplaceEdge(28,52,None)
      replace(52);
      pendingFinish = -1;
      return false;
    }
    if (prev == 28 && last == 28) { // (28,28)
      // DropLast(28)
      dropLast();
      return true;
    }
    if (prev == 28 && last == 30) { // (28,30)
      // ReplaceEdge(28,59,Some(28))
      replace(59);
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
      // ReplaceEdge(28,61,None)
      replace(61);
      pendingFinish = -1;
      return false;
    }
    if (prev == 28 && last == 36) { // (28,36)
      // ReplaceEdge(28,85,None)
      replace(85);
      pendingFinish = -1;
      return false;
    }
    if (prev == 28 && last == 37) { // (28,37)
      // ReplaceEdge(28,59,Some(28))
      replace(59);
      pendingFinish = 28;
      return false;
    }
    if (prev == 28 && last == 38) { // (28,38)
      // DropLast(38)
      dropLast();
      replace(38);
      return true;
    }
    if (prev == 28 && last == 48) { // (28,48)
      // ReplaceEdge(28,49,None)
      replace(49);
      pendingFinish = -1;
      return false;
    }
    if (prev == 28 && last == 49) { // (28,49)
      // ReplaceEdge(28,68,None)
      replace(68);
      pendingFinish = -1;
      return false;
    }
    if (prev == 28 && last == 55) { // (28,55)
      // ReplaceEdge(28,73,None)
      replace(73);
      pendingFinish = -1;
      return false;
    }
    if (prev == 28 && last == 56) { // (28,56)
      // ReplaceEdge(28,72,None)
      replace(72);
      pendingFinish = -1;
      return false;
    }
    if (prev == 28 && last == 57) { // (28,57)
      // ReplaceEdge(28,74,None)
      replace(74);
      pendingFinish = -1;
      return false;
    }
    if (prev == 28 && last == 64) { // (28,64)
      // ReplaceEdge(28,31,Some(28))
      replace(31);
      pendingFinish = 28;
      return false;
    }
    if (prev == 28 && last == 69) { // (28,69)
      // ReplaceEdge(28,70,None)
      replace(70);
      pendingFinish = -1;
      return false;
    }
    if (prev == 28 && last == 70) { // (28,70)
      // DropLast(28)
      dropLast();
      return true;
    }
    if (prev == 28 && last == 72) { // (28,72)
      // ReplaceEdge(28,80,None)
      replace(80);
      pendingFinish = -1;
      return false;
    }
    if (prev == 28 && last == 73) { // (28,73)
      // ReplaceEdge(28,59,Some(28))
      replace(59);
      pendingFinish = 28;
      return false;
    }
    if (prev == 28 && last == 74) { // (28,74)
      // ReplaceEdge(28,59,Some(28))
      replace(59);
      pendingFinish = 28;
      return false;
    }
    if (prev == 28 && last == 80) { // (28,80)
      // ReplaceEdge(28,59,Some(28))
      replace(59);
      pendingFinish = 28;
      return false;
    }
    if (prev == 28 && last == 87) { // (28,87)
      // ReplaceEdge(28,88,None)
      replace(88);
      pendingFinish = -1;
      return false;
    }
    if (prev == 28 && last == 88) { // (28,88)
      // ReplaceEdge(28,59,Some(28))
      replace(59);
      pendingFinish = 28;
      return false;
    }
    if (prev == 33 && last == 11) { // (33,11)
      // ReplaceEdge(33,47,Some(35))
      replace(47);
      pendingFinish = 35;
      return false;
    }
    if (prev == 33 && last == 38) { // (33,38)
      // DropLast(38)
      dropLast();
      replace(38);
      return true;
    }
    if (prev == 33 && last == 47) { // (33,47)
      // ReplaceEdge(36,54,None)
      dropLast();
      replace(36);
      append(54);
      pendingFinish = -1;
      return false;
    }
    if (prev == 35 && last == 11) { // (35,11)
      // ReplaceEdge(35,47,Some(35))
      replace(47);
      pendingFinish = 35;
      return false;
    }
    if (prev == 35 && last == 38) { // (35,38)
      // DropLast(38)
      dropLast();
      replace(38);
      return true;
    }
    if (prev == 35 && last == 47) { // (35,47)
      // ReplaceEdge(38,54,None)
      dropLast();
      replace(38);
      append(54);
      pendingFinish = -1;
      return false;
    }
    if (prev == 36 && last == 11) { // (36,11)
      // ReplaceEdge(36,47,None)
      replace(47);
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
      // ReplaceEdge(36,54,None)
      replace(54);
      pendingFinish = -1;
      return false;
    }
    if (prev == 36 && last == 38) { // (36,38)
      // DropLast(38)
      dropLast();
      replace(38);
      return true;
    }
    if (prev == 36 && last == 47) { // (36,47)
      // ReplaceEdge(36,54,None)
      replace(54);
      pendingFinish = -1;
      return false;
    }
    if (prev == 36 && last == 62) { // (36,62)
      // ReplaceEdge(36,63,None)
      replace(63);
      pendingFinish = -1;
      return false;
    }
    if (prev == 36 && last == 63) { // (36,63)
      // ReplaceEdge(36,71,None)
      replace(71);
      pendingFinish = -1;
      return false;
    }
    if (prev == 36 && last == 78) { // (36,78)
      // ReplaceEdge(36,79,None)
      replace(79);
      pendingFinish = -1;
      return false;
    }
    if (prev == 36 && last == 79) { // (36,79)
      // ReplaceEdge(36,81,Some(36))
      replace(81);
      pendingFinish = 36;
      return false;
    }
    if (prev == 36 && last == 83) { // (36,83)
      // ReplaceEdge(36,84,None)
      replace(84);
      pendingFinish = -1;
      return false;
    }
    if (prev == 36 && last == 84) { // (36,84)
      // ReplaceEdge(36,86,None)
      replace(86);
      pendingFinish = -1;
      return false;
    }
    if (prev == 36 && last == 90) { // (36,90)
      // ReplaceEdge(36,91,None)
      replace(91);
      pendingFinish = -1;
      return false;
    }
    if (prev == 36 && last == 91) { // (36,91)
      // DropLast(36)
      dropLast();
      return true;
    }
    if (prev == 38 && last == 5) { // (38,5)
      // DropLast(38)
      dropLast();
      return true;
    }
    if (prev == 38 && last == 6) { // (38,6)
      // DropLast(38)
      dropLast();
      return true;
    }
    if (prev == 38 && last == 7) { // (38,7)
      // DropLast(38)
      dropLast();
      return true;
    }
    if (prev == 38 && last == 10) { // (38,10)
      // DropLast(38)
      dropLast();
      return true;
    }
    if (prev == 38 && last == 16) { // (38,16)
      // DropLast(38)
      dropLast();
      return true;
    }
    if (prev == 38 && last == 18) { // (38,18)
      // DropLast(38)
      dropLast();
      return true;
    }
    if (prev == 38 && last == 21) { // (38,21)
      // DropLast(38)
      dropLast();
      return true;
    }
    if (prev == 38 && last == 23) { // (38,23)
      // DropLast(38)
      dropLast();
      return true;
    }
    if (prev == 38 && last == 25) { // (38,25)
      // DropLast(38)
      dropLast();
      return true;
    }
    if (prev == 38 && last == 27) { // (38,27)
      // DropLast(38)
      dropLast();
      return true;
    }
    if (prev == 38 && last == 28) { // (38,28)
      // DropLast(38)
      dropLast();
      return true;
    }
    if (prev == 38 && last == 30) { // (38,30)
      // DropLast(38)
      dropLast();
      return true;
    }
    if (prev == 38 && last == 32) { // (38,32)
      // DropLast(38)
      dropLast();
      return true;
    }
    if (prev == 38 && last == 35) { // (38,35)
      // DropLast(38)
      dropLast();
      return true;
    }
    if (prev == 38 && last == 36) { // (38,36)
      // DropLast(38)
      dropLast();
      return true;
    }
    if (prev == 38 && last == 37) { // (38,37)
      // DropLast(38)
      dropLast();
      return true;
    }
    if (prev == 38 && last == 38) { // (38,38)
      // DropLast(38)
      dropLast();
      return true;
    }
    if (prev == 38 && last == 48) { // (38,48)
      // DropLast(38)
      dropLast();
      return true;
    }
    if (prev == 38 && last == 49) { // (38,49)
      // DropLast(38)
      dropLast();
      return true;
    }
    if (prev == 38 && last == 62) { // (38,62)
      // DropLast(38)
      dropLast();
      return true;
    }
    if (prev == 38 && last == 63) { // (38,63)
      // DropLast(38)
      dropLast();
      return true;
    }
    if (prev == 38 && last == 66) { // (38,66)
      // DropLast(38)
      dropLast();
      return true;
    }
    if (prev == 38 && last == 67) { // (38,67)
      // DropLast(38)
      dropLast();
      return true;
    }
    if (prev == 38 && last == 69) { // (38,69)
      // DropLast(38)
      dropLast();
      return true;
    }
    if (prev == 38 && last == 70) { // (38,70)
      // DropLast(38)
      dropLast();
      return true;
    }
    if (prev == 38 && last == 78) { // (38,78)
      // DropLast(38)
      dropLast();
      return true;
    }
    if (prev == 38 && last == 79) { // (38,79)
      // DropLast(38)
      dropLast();
      return true;
    }
    if (prev == 38 && last == 90) { // (38,90)
      // DropLast(38)
      dropLast();
      return true;
    }
    if (prev == 38 && last == 91) { // (38,91)
      // DropLast(38)
      dropLast();
      return true;
    }
    if (prev == 40 && last == 41) { // (40,41)
      // ReplaceEdge(40,41,Some(40))
      pendingFinish = 40;
      return false;
    }
    if (prev == 43 && last == 41) { // (43,41)
      // ReplaceEdge(43,41,Some(43))
      pendingFinish = 43;
      return false;
    }
    if (prev == 47 && last == 21) { // (47,21)
      // ReplaceEdge(47,23,None)
      replace(23);
      pendingFinish = -1;
      return false;
    }
    if (prev == 47 && last == 23) { // (47,23)
      // DropLast(47)
      dropLast();
      return true;
    }
    if (prev == 47 && last == 38) { // (47,38)
      // DropLast(38)
      dropLast();
      replace(38);
      return true;
    }
    if (prev == 48 && last == 11) { // (48,11)
      // DropLast(48)
      dropLast();
      return true;
    }
    if (prev == 62 && last == 11) { // (62,11)
      // DropLast(62)
      dropLast();
      return true;
    }
    if (prev == 64 && last == 26) { // (64,26)
      // DropLast(64)
      dropLast();
      return true;
    }
    if (prev == 65 && last == 11) { // (65,11)
      // DropLast(65)
      dropLast();
      return true;
    }
    if (prev == 67 && last == 11) { // (67,11)
      // DropLast(67)
      dropLast();
      return true;
    }
    if (prev == 69 && last == 11) { // (69,11)
      // DropLast(69)
      dropLast();
      return true;
    }
    if (prev == 70 && last == 5) { // (70,5)
      // ReplaceEdge(70,57,None)
      replace(57);
      pendingFinish = -1;
      return false;
    }
    if (prev == 70 && last == 6) { // (70,6)
      // ReplaceEdge(70,51,Some(70))
      replace(51);
      pendingFinish = 70;
      return false;
    }
    if (prev == 70 && last == 7) { // (70,7)
      // ReplaceEdge(70,55,None)
      replace(55);
      pendingFinish = -1;
      return false;
    }
    if (prev == 70 && last == 10) { // (70,10)
      // ReplaceEdge(70,56,None)
      replace(56);
      pendingFinish = -1;
      return false;
    }
    if (prev == 70 && last == 16) { // (70,16)
      // ReplaceEdge(70,76,Some(70))
      replace(76);
      pendingFinish = 70;
      return false;
    }
    if (prev == 70 && last == 18) { // (70,18)
      // ReplaceEdge(70,59,Some(70))
      replace(59);
      pendingFinish = 70;
      return false;
    }
    if (prev == 70 && last == 21) { // (70,21)
      // ReplaceEdge(70,23,None)
      replace(23);
      pendingFinish = -1;
      return false;
    }
    if (prev == 70 && last == 23) { // (70,23)
      // ReplaceEdge(70,59,Some(70))
      replace(59);
      pendingFinish = 70;
      return false;
    }
    if (prev == 70 && last == 25) { // (70,25)
      // ReplaceEdge(70,31,Some(70))
      replace(31);
      pendingFinish = 70;
      return false;
    }
    if (prev == 70 && last == 27) { // (70,27)
      // ReplaceEdge(70,52,None)
      replace(52);
      pendingFinish = -1;
      return false;
    }
    if (prev == 70 && last == 28) { // (70,28)
      // ReplaceEdge(70,53,None)
      replace(53);
      pendingFinish = -1;
      return false;
    }
    if (prev == 70 && last == 30) { // (70,30)
      // ReplaceEdge(70,59,Some(70))
      replace(59);
      pendingFinish = 70;
      return false;
    }
    if (prev == 70 && last == 32) { // (70,32)
      // ReplaceEdge(70,31,Some(70))
      replace(31);
      pendingFinish = 70;
      return false;
    }
    if (prev == 70 && last == 35) { // (70,35)
      // ReplaceEdge(70,61,None)
      replace(61);
      pendingFinish = -1;
      return false;
    }
    if (prev == 70 && last == 36) { // (70,36)
      // ReplaceEdge(70,85,None)
      replace(85);
      pendingFinish = -1;
      return false;
    }
    if (prev == 70 && last == 37) { // (70,37)
      // ReplaceEdge(70,59,Some(70))
      replace(59);
      pendingFinish = 70;
      return false;
    }
    if (prev == 70 && last == 38) { // (70,38)
      // DropLast(38)
      dropLast();
      replace(38);
      return true;
    }
    if (prev == 70 && last == 48) { // (70,48)
      // ReplaceEdge(70,49,None)
      replace(49);
      pendingFinish = -1;
      return false;
    }
    if (prev == 70 && last == 49) { // (70,49)
      // ReplaceEdge(70,68,None)
      replace(68);
      pendingFinish = -1;
      return false;
    }
    if (prev == 70 && last == 55) { // (70,55)
      // ReplaceEdge(70,73,None)
      replace(73);
      pendingFinish = -1;
      return false;
    }
    if (prev == 70 && last == 56) { // (70,56)
      // ReplaceEdge(70,72,None)
      replace(72);
      pendingFinish = -1;
      return false;
    }
    if (prev == 70 && last == 57) { // (70,57)
      // ReplaceEdge(70,74,None)
      replace(74);
      pendingFinish = -1;
      return false;
    }
    if (prev == 70 && last == 64) { // (70,64)
      // ReplaceEdge(70,31,Some(70))
      replace(31);
      pendingFinish = 70;
      return false;
    }
    if (prev == 70 && last == 65) { // (70,65)
      // ReplaceEdge(70,66,None)
      replace(66);
      pendingFinish = -1;
      return false;
    }
    if (prev == 70 && last == 66) { // (70,66)
      // ReplaceEdge(70,59,Some(70))
      replace(59);
      pendingFinish = 70;
      return false;
    }
    if (prev == 70 && last == 69) { // (70,69)
      // ReplaceEdge(70,70,None)
      replace(70);
      pendingFinish = -1;
      return false;
    }
    if (prev == 70 && last == 70) { // (70,70)
      // DropLast(70)
      dropLast();
      return true;
    }
    if (prev == 70 && last == 72) { // (70,72)
      // ReplaceEdge(70,80,None)
      replace(80);
      pendingFinish = -1;
      return false;
    }
    if (prev == 70 && last == 73) { // (70,73)
      // ReplaceEdge(70,59,Some(70))
      replace(59);
      pendingFinish = 70;
      return false;
    }
    if (prev == 70 && last == 74) { // (70,74)
      // ReplaceEdge(70,59,Some(70))
      replace(59);
      pendingFinish = 70;
      return false;
    }
    if (prev == 70 && last == 80) { // (70,80)
      // ReplaceEdge(70,59,Some(70))
      replace(59);
      pendingFinish = 70;
      return false;
    }
    if (prev == 70 && last == 87) { // (70,87)
      // ReplaceEdge(70,88,None)
      replace(88);
      pendingFinish = -1;
      return false;
    }
    if (prev == 70 && last == 88) { // (70,88)
      // ReplaceEdge(70,59,Some(70))
      replace(59);
      pendingFinish = 70;
      return false;
    }
    if (prev == 78 && last == 11) { // (78,11)
      // DropLast(78)
      dropLast();
      return true;
    }
    if (prev == 79 && last == 5) { // (79,5)
      // ReplaceEdge(79,57,None)
      replace(57);
      pendingFinish = -1;
      return false;
    }
    if (prev == 79 && last == 6) { // (79,6)
      // ReplaceEdge(79,58,Some(79))
      replace(58);
      pendingFinish = 79;
      return false;
    }
    if (prev == 79 && last == 7) { // (79,7)
      // ReplaceEdge(79,55,None)
      replace(55);
      pendingFinish = -1;
      return false;
    }
    if (prev == 79 && last == 10) { // (79,10)
      // ReplaceEdge(79,56,None)
      replace(56);
      pendingFinish = -1;
      return false;
    }
    if (prev == 79 && last == 16) { // (79,16)
      // ReplaceEdge(79,18,Some(79))
      replace(18);
      pendingFinish = 79;
      return false;
    }
    if (prev == 79 && last == 18) { // (79,18)
      // DropLast(79)
      dropLast();
      return true;
    }
    if (prev == 79 && last == 21) { // (79,21)
      // ReplaceEdge(79,23,None)
      replace(23);
      pendingFinish = -1;
      return false;
    }
    if (prev == 79 && last == 23) { // (79,23)
      // DropLast(79)
      dropLast();
      return true;
    }
    if (prev == 79 && last == 25) { // (79,25)
      // ReplaceEdge(79,14,Some(79))
      replace(14);
      pendingFinish = 79;
      return false;
    }
    if (prev == 79 && last == 27) { // (79,27)
      // ReplaceEdge(79,52,None)
      replace(52);
      pendingFinish = -1;
      return false;
    }
    if (prev == 79 && last == 28) { // (79,28)
      // ReplaceEdge(79,53,None)
      replace(53);
      pendingFinish = -1;
      return false;
    }
    if (prev == 79 && last == 30) { // (79,30)
      // DropLast(79)
      dropLast();
      return true;
    }
    if (prev == 79 && last == 32) { // (79,32)
      // ReplaceEdge(79,14,Some(79))
      replace(14);
      pendingFinish = 79;
      return false;
    }
    if (prev == 79 && last == 35) { // (79,35)
      // ReplaceEdge(79,61,None)
      replace(61);
      pendingFinish = -1;
      return false;
    }
    if (prev == 79 && last == 36) { // (79,36)
      // ReplaceEdge(79,85,None)
      replace(85);
      pendingFinish = -1;
      return false;
    }
    if (prev == 79 && last == 37) { // (79,37)
      // DropLast(79)
      dropLast();
      return true;
    }
    if (prev == 79 && last == 38) { // (79,38)
      // DropLast(38)
      dropLast();
      replace(38);
      return true;
    }
    if (prev == 79 && last == 55) { // (79,55)
      // ReplaceEdge(79,73,None)
      replace(73);
      pendingFinish = -1;
      return false;
    }
    if (prev == 79 && last == 56) { // (79,56)
      // ReplaceEdge(79,72,None)
      replace(72);
      pendingFinish = -1;
      return false;
    }
    if (prev == 79 && last == 57) { // (79,57)
      // ReplaceEdge(79,74,None)
      replace(74);
      pendingFinish = -1;
      return false;
    }
    if (prev == 79 && last == 64) { // (79,64)
      // ReplaceEdge(79,14,Some(79))
      replace(14);
      pendingFinish = 79;
      return false;
    }
    if (prev == 79 && last == 65) { // (79,65)
      // ReplaceEdge(79,66,None)
      replace(66);
      pendingFinish = -1;
      return false;
    }
    if (prev == 79 && last == 66) { // (79,66)
      // DropLast(79)
      dropLast();
      return true;
    }
    if (prev == 79 && last == 72) { // (79,72)
      // ReplaceEdge(79,80,None)
      replace(80);
      pendingFinish = -1;
      return false;
    }
    if (prev == 79 && last == 73) { // (79,73)
      // DropLast(79)
      dropLast();
      return true;
    }
    if (prev == 79 && last == 74) { // (79,74)
      // DropLast(79)
      dropLast();
      return true;
    }
    if (prev == 79 && last == 80) { // (79,80)
      // DropLast(79)
      dropLast();
      return true;
    }
    if (prev == 79 && last == 87) { // (79,87)
      // ReplaceEdge(79,88,None)
      replace(88);
      pendingFinish = -1;
      return false;
    }
    if (prev == 79 && last == 88) { // (79,88)
      // DropLast(79)
      dropLast();
      return true;
    }
    if (prev == 83 && last == 11) { // (83,11)
      // DropLast(83)
      dropLast();
      return true;
    }
    if (prev == 86 && last == 11) { // (86,11)
      // ReplaceEdge(86,47,Some(90))
      replace(47);
      pendingFinish = 90;
      return false;
    }
    if (prev == 86 && last == 38) { // (86,38)
      // DropLast(38)
      dropLast();
      replace(38);
      return true;
    }
    if (prev == 86 && last == 47) { // (86,47)
      // ReplaceEdge(91,54,None)
      dropLast();
      replace(91);
      append(54);
      pendingFinish = -1;
      return false;
    }
    if (prev == 87 && last == 11) { // (87,11)
      // DropLast(87)
      dropLast();
      return true;
    }
    if (prev == 90 && last == 11) { // (90,11)
      // ReplaceEdge(90,47,Some(90))
      replace(47);
      pendingFinish = 90;
      return false;
    }
    if (prev == 90 && last == 38) { // (90,38)
      // DropLast(38)
      dropLast();
      replace(38);
      return true;
    }
    if (prev == 90 && last == 47) { // (90,47)
      // ReplaceEdge(38,54,None)
      dropLast();
      replace(38);
      append(54);
      pendingFinish = -1;
      return false;
    }
    if (prev == 91 && last == 11) { // (91,11)
      // ReplaceEdge(91,47,None)
      replace(47);
      pendingFinish = -1;
      return false;
    }
    if (prev == 91 && last == 21) { // (91,21)
      // ReplaceEdge(91,23,None)
      replace(23);
      pendingFinish = -1;
      return false;
    }
    if (prev == 91 && last == 23) { // (91,23)
      // ReplaceEdge(91,54,None)
      replace(54);
      pendingFinish = -1;
      return false;
    }
    if (prev == 91 && last == 38) { // (91,38)
      // DropLast(38)
      dropLast();
      replace(38);
      return true;
    }
    if (prev == 91 && last == 47) { // (91,47)
      // ReplaceEdge(91,54,None)
      replace(54);
      pendingFinish = -1;
      return false;
    }
    if (prev == 91 && last == 62) { // (91,62)
      // ReplaceEdge(91,63,None)
      replace(63);
      pendingFinish = -1;
      return false;
    }
    if (prev == 91 && last == 63) { // (91,63)
      // ReplaceEdge(91,71,None)
      replace(71);
      pendingFinish = -1;
      return false;
    }
    if (prev == 91 && last == 78) { // (91,78)
      // ReplaceEdge(91,79,None)
      replace(79);
      pendingFinish = -1;
      return false;
    }
    if (prev == 91 && last == 79) { // (91,79)
      // ReplaceEdge(91,81,Some(91))
      replace(81);
      pendingFinish = 91;
      return false;
    }
    if (prev == 91 && last == 83) { // (91,83)
      // ReplaceEdge(91,84,None)
      replace(84);
      pendingFinish = -1;
      return false;
    }
    if (prev == 91 && last == 84) { // (91,84)
      // ReplaceEdge(91,86,None)
      replace(86);
      pendingFinish = -1;
      return false;
    }
    if (prev == 91 && last == 90) { // (91,90)
      // ReplaceEdge(91,91,None)
      replace(91);
      pendingFinish = -1;
      return false;
    }
    if (prev == 91 && last == 91) { // (91,91)
      // DropLast(91)
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
          // Append(17,41,Some(17))
          append(41);
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
          // Finish(39)
          replace(39);
          if (verbose) printStack();
          finish();
          if (verbose) printStack();
          return true;
        }
        if ((c == '-')) {
          // Finish(39)
          replace(39);
          if (verbose) printStack();
          finish();
          if (verbose) printStack();
          return true;
        }
        if (('0' <= c && c <= '9')) {
          // Append(40,41,Some(40))
          replace(40);
          append(41);
          pendingFinish = 40;
          if (verbose) printStack();
          return true;
        }
        return false;
      case 20:
        if ((c == '+')) {
          // Finish(42)
          replace(42);
          if (verbose) printStack();
          finish();
          if (verbose) printStack();
          return true;
        }
        if ((c == '-')) {
          // Finish(42)
          replace(42);
          if (verbose) printStack();
          finish();
          if (verbose) printStack();
          return true;
        }
        if (('0' <= c && c <= '9')) {
          // Append(43,41,Some(43))
          replace(43);
          append(41);
          pendingFinish = 43;
          if (verbose) printStack();
          return true;
        }
        return false;
      case 21:
        if ((c == ' ')
            || ('0' <= c && c <= '9')
            || ('A' <= c && c <= 'Z')
            || ('a' <= c && c <= 'z')) {
          // Append(21,24,Some(21))
          append(24);
          pendingFinish = 21;
          if (verbose) printStack();
          return true;
        }
        if ((c == '"')) {
          // Finish(38)
          replace(38);
          if (verbose) printStack();
          finish();
          if (verbose) printStack();
          return true;
        }
        if ((c == '\\')) {
          // Append(21,22,None)
          append(22);
          pendingFinish = -1;
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
          // Append(22,50,None)
          append(50);
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
      case 25:
        if ((c == '0')) {
          // Append(25,26,Some(25))
          append(26);
          pendingFinish = 25;
          if (verbose) printStack();
          return true;
        }
        if (('1' <= c && c <= '9')) {
          // Append(25,26,Some(25))
          append(26);
          pendingFinish = 25;
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
      case 27:
        if (('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ')) {
          // Append(27,11,Some(27))
          append(11);
          pendingFinish = 27;
          if (verbose) printStack();
          return true;
        }
        if ((c == '"')) {
          // Append(38,2,None)
          replace(38);
          append(2);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == '-')) {
          // Append(38,6,None)
          replace(38);
          append(6);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == '0')) {
          // Append(38,46,None)
          replace(38);
          append(46);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if (('1' <= c && c <= '9')) {
          // Append(38,44,None)
          replace(38);
          append(44);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == '[')) {
          // Append(38,3,None)
          replace(38);
          append(3);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == ']')) {
          // Append(38,45,None)
          replace(38);
          append(45);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == 'f')) {
          // Append(38,10,None)
          replace(38);
          append(10);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == 'n')) {
          // Append(38,7,None)
          replace(38);
          append(7);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == 't')) {
          // Append(38,5,None)
          replace(38);
          append(5);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == '{')) {
          // Append(38,9,None)
          replace(38);
          append(9);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        return false;
      case 28:
        if ((c == '"')) {
          // Append(28,2,None)
          append(2);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == '-')) {
          // Append(28,6,None)
          append(6);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == '0')) {
          // Append(28,31,Some(28))
          append(31);
          pendingFinish = 28;
          if (verbose) printStack();
          return true;
        }
        if (('1' <= c && c <= '9')) {
          // Append(28,29,Some(28))
          append(29);
          pendingFinish = 28;
          if (verbose) printStack();
          return true;
        }
        if ((c == '[')) {
          // Append(28,3,None)
          append(3);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == 'f')) {
          // Append(28,10,None)
          append(10);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == 'n')) {
          // Append(28,7,None)
          append(7);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == 't')) {
          // Append(28,5,None)
          append(5);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == '{')) {
          // Append(28,9,None)
          append(9);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        return false;
      case 29:
        if (('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ')) {
          // Append(48,11,Some(48))
          replace(48);
          append(11);
          pendingFinish = 48;
          if (verbose) printStack();
          return true;
        }
        if ((c == ',')) {
          // Finish(49)
          replace(49);
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
          // Append(48,11,Some(48))
          replace(48);
          append(11);
          pendingFinish = 48;
          if (verbose) printStack();
          return true;
        }
        if ((c == ',')) {
          // Finish(49)
          replace(49);
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
          // Append(47,2,None)
          replace(47);
          append(2);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        return false;
      case 35:
        if (('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ')) {
          // Append(35,34,Some(35))
          append(34);
          pendingFinish = 35;
          if (verbose) printStack();
          return true;
        }
        if ((c == '"')) {
          // Append(38,2,None)
          replace(38);
          append(2);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == '}')) {
          // Finish(38)
          replace(38);
          if (verbose) printStack();
          finish();
          if (verbose) printStack();
          return true;
        }
        return false;
      case 36:
        if (('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ')) {
          // Append(36,34,None)
          append(34);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == '"')) {
          // Append(36,2,None)
          append(2);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        return false;
      case 40:
        if (('0' <= c && c <= '9')) {
          // Append(40,41,Some(40))
          append(41);
          pendingFinish = 40;
          if (verbose) printStack();
          return true;
        }
        return false;
      case 41:
        if (('0' <= c && c <= '9')) {
          // Finish(41)
          finish();
          if (verbose) printStack();
          return true;
        }
        return false;
      case 43:
        if (('0' <= c && c <= '9')) {
          // Append(43,41,Some(43))
          append(41);
          pendingFinish = 43;
          if (verbose) printStack();
          return true;
        }
        return false;
      case 44:
        if (('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ')) {
          // Append(67,11,Some(67))
          replace(67);
          append(11);
          pendingFinish = 67;
          if (verbose) printStack();
          return true;
        }
        if ((c == ',')) {
          // Finish(49)
          replace(49);
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
        if ((c == ']')) {
          // Finish(66)
          replace(66);
          if (verbose) printStack();
          finish();
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
      case 45:
        if (('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ')) {
          // Append(67,11,Some(67))
          replace(67);
          append(11);
          pendingFinish = 67;
          if (verbose) printStack();
          return true;
        }
        if ((c == ',')) {
          // Finish(49)
          replace(49);
          if (verbose) printStack();
          finish();
          if (verbose) printStack();
          return true;
        }
        if ((c == ']')) {
          // Finish(66)
          replace(66);
          if (verbose) printStack();
          finish();
          if (verbose) printStack();
          return true;
        }
        return false;
      case 46:
        if (('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ')) {
          // Append(67,11,Some(67))
          replace(67);
          append(11);
          pendingFinish = 67;
          if (verbose) printStack();
          return true;
        }
        if ((c == ',')) {
          // Finish(49)
          replace(49);
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
        if ((c == ']')) {
          // Finish(66)
          replace(66);
          if (verbose) printStack();
          finish();
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
      case 47:
        if ((c == '"')) {
          // Append(47,2,None)
          append(2);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        return false;
      case 48:
        if (('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ')) {
          // Append(48,11,Some(48))
          append(11);
          pendingFinish = 48;
          if (verbose) printStack();
          return true;
        }
        if ((c == ',')) {
          // Append(38,68,None)
          replace(38);
          append(68);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        return false;
      case 49:
        if ((c == ',')) {
          // Finish(49)
          finish();
          if (verbose) printStack();
          return true;
        }
        return false;
      case 50:
        if ((c == '0')) {
          // Finish(50)
          finish();
          if (verbose) printStack();
          return true;
        }
        if (('1' <= c && c <= '9')) {
          // Finish(50)
          finish();
          if (verbose) printStack();
          return true;
        }
        if (('A' <= c && c <= 'F') || ('a' <= c && c <= 'f')) {
          // Finish(50)
          finish();
          if (verbose) printStack();
          return true;
        }
        return false;
      case 51:
        if (('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ')) {
          // Append(48,11,Some(48))
          replace(48);
          append(11);
          pendingFinish = 48;
          if (verbose) printStack();
          return true;
        }
        if ((c == ',')) {
          // Finish(49)
          replace(49);
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
          // Append(64,26,Some(64))
          replace(64);
          append(26);
          pendingFinish = 64;
          if (verbose) printStack();
          return true;
        }
        if (('1' <= c && c <= '9')) {
          // Append(64,26,Some(64))
          replace(64);
          append(26);
          pendingFinish = 64;
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
      case 52:
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
      case 53:
        if (('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ')) {
          // Append(65,11,Some(65))
          replace(65);
          append(11);
          pendingFinish = 65;
          if (verbose) printStack();
          return true;
        }
        if ((c == ']')) {
          // Finish(66)
          replace(66);
          if (verbose) printStack();
          finish();
          if (verbose) printStack();
          return true;
        }
        return false;
      case 54:
        if (('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ')) {
          // Append(62,11,Some(62))
          replace(62);
          append(11);
          pendingFinish = 62;
          if (verbose) printStack();
          return true;
        }
        if ((c == ':')) {
          // Finish(63)
          replace(63);
          if (verbose) printStack();
          finish();
          if (verbose) printStack();
          return true;
        }
        return false;
      case 55:
        if ((c == 'l')) {
          // Finish(55)
          finish();
          if (verbose) printStack();
          return true;
        }
        return false;
      case 56:
        if ((c == 'l')) {
          // Finish(56)
          finish();
          if (verbose) printStack();
          return true;
        }
        return false;
      case 57:
        if ((c == 'u')) {
          // Finish(57)
          finish();
          if (verbose) printStack();
          return true;
        }
        return false;
      case 58:
        if ((c == '.')) {
          // Append(16,17,None)
          replace(16);
          append(17);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == '0')) {
          // Append(64,26,Some(64))
          replace(64);
          append(26);
          pendingFinish = 64;
          if (verbose) printStack();
          return true;
        }
        if (('1' <= c && c <= '9')) {
          // Append(64,26,Some(64))
          replace(64);
          append(26);
          pendingFinish = 64;
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
      case 59:
        if (('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ')) {
          // Append(48,11,Some(48))
          replace(48);
          append(11);
          pendingFinish = 48;
          if (verbose) printStack();
          return true;
        }
        if ((c == ',')) {
          // Finish(49)
          replace(49);
          if (verbose) printStack();
          finish();
          if (verbose) printStack();
          return true;
        }
        return false;
      case 60:
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
          // Append(64,26,Some(64))
          replace(64);
          append(26);
          pendingFinish = 64;
          if (verbose) printStack();
          return true;
        }
        if (('1' <= c && c <= '9')) {
          // Append(64,26,Some(64))
          replace(64);
          append(26);
          pendingFinish = 64;
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
      case 61:
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
      case 62:
        if (('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ')) {
          // Append(62,11,Some(62))
          append(11);
          pendingFinish = 62;
          if (verbose) printStack();
          return true;
        }
        if ((c == ':')) {
          // Append(38,71,None)
          replace(38);
          append(71);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        return false;
      case 63:
        if ((c == ':')) {
          // Finish(63)
          finish();
          if (verbose) printStack();
          return true;
        }
        return false;
      case 64:
        if ((c == '0')) {
          // Append(64,26,Some(64))
          append(26);
          pendingFinish = 64;
          if (verbose) printStack();
          return true;
        }
        if (('1' <= c && c <= '9')) {
          // Append(64,26,Some(64))
          append(26);
          pendingFinish = 64;
          if (verbose) printStack();
          return true;
        }
        return false;
      case 65:
        if (('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ')) {
          // Append(65,11,Some(65))
          append(11);
          pendingFinish = 65;
          if (verbose) printStack();
          return true;
        }
        if ((c == ']')) {
          // Finish(38)
          replace(38);
          if (verbose) printStack();
          finish();
          if (verbose) printStack();
          return true;
        }
        return false;
      case 66:
        if ((c == ']')) {
          // Finish(66)
          finish();
          if (verbose) printStack();
          return true;
        }
        return false;
      case 67:
        if (('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ')) {
          // Append(67,11,Some(67))
          append(11);
          pendingFinish = 67;
          if (verbose) printStack();
          return true;
        }
        if ((c == ',')) {
          // Append(38,68,None)
          replace(38);
          append(68);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == ']')) {
          // Finish(38)
          replace(38);
          if (verbose) printStack();
          finish();
          if (verbose) printStack();
          return true;
        }
        return false;
      case 68:
        if (('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ')) {
          // Append(69,11,Some(69))
          replace(69);
          append(11);
          pendingFinish = 69;
          if (verbose) printStack();
          return true;
        }
        if ((c == '"')) {
          // Append(70,2,None)
          replace(70);
          append(2);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == '-')) {
          // Append(70,6,None)
          replace(70);
          append(6);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == '0')) {
          // Append(70,31,Some(70))
          replace(70);
          append(31);
          pendingFinish = 70;
          if (verbose) printStack();
          return true;
        }
        if (('1' <= c && c <= '9')) {
          // Append(70,29,Some(70))
          replace(70);
          append(29);
          pendingFinish = 70;
          if (verbose) printStack();
          return true;
        }
        if ((c == '[')) {
          // Append(70,3,None)
          replace(70);
          append(3);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == 'f')) {
          // Append(70,10,None)
          replace(70);
          append(10);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == 'n')) {
          // Append(70,7,None)
          replace(70);
          append(7);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == 't')) {
          // Append(70,5,None)
          replace(70);
          append(5);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == '{')) {
          // Append(70,9,None)
          replace(70);
          append(9);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        return false;
      case 69:
        if (('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ')) {
          // Append(69,11,Some(69))
          append(11);
          pendingFinish = 69;
          if (verbose) printStack();
          return true;
        }
        if ((c == '"')) {
          // Append(38,2,None)
          replace(38);
          append(2);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == '-')) {
          // Append(38,6,None)
          replace(38);
          append(6);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == '0')) {
          // Append(38,31,None)
          replace(38);
          append(31);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if (('1' <= c && c <= '9')) {
          // Append(38,29,None)
          replace(38);
          append(29);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == '[')) {
          // Append(38,3,None)
          replace(38);
          append(3);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == 'f')) {
          // Append(38,10,None)
          replace(38);
          append(10);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == 'n')) {
          // Append(38,7,None)
          replace(38);
          append(7);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == 't')) {
          // Append(38,5,None)
          replace(38);
          append(5);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == '{')) {
          // Append(38,9,None)
          replace(38);
          append(9);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        return false;
      case 70:
        if ((c == '"')) {
          // Append(70,2,None)
          append(2);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == '-')) {
          // Append(70,6,None)
          append(6);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == '0')) {
          // Append(70,31,Some(70))
          append(31);
          pendingFinish = 70;
          if (verbose) printStack();
          return true;
        }
        if (('1' <= c && c <= '9')) {
          // Append(70,29,Some(70))
          append(29);
          pendingFinish = 70;
          if (verbose) printStack();
          return true;
        }
        if ((c == '[')) {
          // Append(70,3,None)
          append(3);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == 'f')) {
          // Append(70,10,None)
          append(10);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == 'n')) {
          // Append(70,7,None)
          append(7);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == 't')) {
          // Append(70,5,None)
          append(5);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == '{')) {
          // Append(70,9,None)
          append(9);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        return false;
      case 71:
        if (('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ')) {
          // Append(78,11,Some(78))
          replace(78);
          append(11);
          pendingFinish = 78;
          if (verbose) printStack();
          return true;
        }
        if ((c == '"')) {
          // Append(79,2,None)
          replace(79);
          append(2);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == '-')) {
          // Append(79,6,None)
          replace(79);
          append(6);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == '0')) {
          // Append(79,14,Some(79))
          replace(79);
          append(14);
          pendingFinish = 79;
          if (verbose) printStack();
          return true;
        }
        if (('1' <= c && c <= '9')) {
          // Append(79,13,Some(79))
          replace(79);
          append(13);
          pendingFinish = 79;
          if (verbose) printStack();
          return true;
        }
        if ((c == '[')) {
          // Append(79,3,None)
          replace(79);
          append(3);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == 'f')) {
          // Append(79,10,None)
          replace(79);
          append(10);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == 'n')) {
          // Append(79,7,None)
          replace(79);
          append(7);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == 't')) {
          // Append(79,5,None)
          replace(79);
          append(5);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == '{')) {
          // Append(79,9,None)
          replace(79);
          append(9);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        return false;
      case 72:
        if ((c == 's')) {
          // Finish(72)
          finish();
          if (verbose) printStack();
          return true;
        }
        return false;
      case 73:
        if ((c == 'l')) {
          // Finish(73)
          finish();
          if (verbose) printStack();
          return true;
        }
        return false;
      case 74:
        if ((c == 'e')) {
          // Finish(74)
          finish();
          if (verbose) printStack();
          return true;
        }
        return false;
      case 75:
        if ((c == '0')) {
          // Finish(75)
          finish();
          if (verbose) printStack();
          return true;
        }
        if (('1' <= c && c <= '9')) {
          // Finish(75)
          finish();
          if (verbose) printStack();
          return true;
        }
        if (('A' <= c && c <= 'F') || ('a' <= c && c <= 'f')) {
          // Finish(75)
          finish();
          if (verbose) printStack();
          return true;
        }
        return false;
      case 76:
        if (('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ')) {
          // Append(48,11,Some(48))
          replace(48);
          append(11);
          pendingFinish = 48;
          if (verbose) printStack();
          return true;
        }
        if ((c == ',')) {
          // Finish(49)
          replace(49);
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
      case 77:
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
      case 78:
        if (('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ')) {
          // Append(78,11,Some(78))
          append(11);
          pendingFinish = 78;
          if (verbose) printStack();
          return true;
        }
        if ((c == '"')) {
          // Append(38,2,None)
          replace(38);
          append(2);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == '-')) {
          // Append(38,6,None)
          replace(38);
          append(6);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == '0')) {
          // Append(38,14,None)
          replace(38);
          append(14);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if (('1' <= c && c <= '9')) {
          // Append(38,13,None)
          replace(38);
          append(13);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == '[')) {
          // Append(38,3,None)
          replace(38);
          append(3);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == 'f')) {
          // Append(38,10,None)
          replace(38);
          append(10);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == 'n')) {
          // Append(38,7,None)
          replace(38);
          append(7);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == 't')) {
          // Append(38,5,None)
          replace(38);
          append(5);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == '{')) {
          // Append(38,9,None)
          replace(38);
          append(9);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        return false;
      case 79:
        if ((c == '"')) {
          // Append(79,2,None)
          append(2);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == '-')) {
          // Append(79,6,None)
          append(6);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == '0')) {
          // Append(79,14,Some(79))
          append(14);
          pendingFinish = 79;
          if (verbose) printStack();
          return true;
        }
        if (('1' <= c && c <= '9')) {
          // Append(79,13,Some(79))
          append(13);
          pendingFinish = 79;
          if (verbose) printStack();
          return true;
        }
        if ((c == '[')) {
          // Append(79,3,None)
          append(3);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == 'f')) {
          // Append(79,10,None)
          append(10);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == 'n')) {
          // Append(79,7,None)
          append(7);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == 't')) {
          // Append(79,5,None)
          append(5);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == '{')) {
          // Append(79,9,None)
          append(9);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        return false;
      case 80:
        if ((c == 'e')) {
          // Finish(80)
          finish();
          if (verbose) printStack();
          return true;
        }
        return false;
      case 81:
        if (('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ')) {
          // Append(83,11,Some(83))
          replace(83);
          append(11);
          pendingFinish = 83;
          if (verbose) printStack();
          return true;
        }
        if ((c == ',')) {
          // Finish(84)
          replace(84);
          if (verbose) printStack();
          finish();
          if (verbose) printStack();
          return true;
        }
        return false;
      case 82:
        if ((c == '0')) {
          // Finish(82)
          finish();
          if (verbose) printStack();
          return true;
        }
        if (('1' <= c && c <= '9')) {
          // Finish(82)
          finish();
          if (verbose) printStack();
          return true;
        }
        if (('A' <= c && c <= 'F') || ('a' <= c && c <= 'f')) {
          // Finish(82)
          finish();
          if (verbose) printStack();
          return true;
        }
        return false;
      case 83:
        if (('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ')) {
          // Append(83,11,Some(83))
          append(11);
          pendingFinish = 83;
          if (verbose) printStack();
          return true;
        }
        if ((c == ',')) {
          // Append(38,86,None)
          replace(38);
          append(86);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        return false;
      case 84:
        if ((c == ',')) {
          // Finish(84)
          finish();
          if (verbose) printStack();
          return true;
        }
        return false;
      case 85:
        if (('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ')) {
          // Append(87,11,Some(87))
          replace(87);
          append(11);
          pendingFinish = 87;
          if (verbose) printStack();
          return true;
        }
        if ((c == '}')) {
          // Finish(88)
          replace(88);
          if (verbose) printStack();
          finish();
          if (verbose) printStack();
          return true;
        }
        return false;
      case 86:
        if (('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ')) {
          // Append(86,34,Some(90))
          append(34);
          pendingFinish = 90;
          if (verbose) printStack();
          return true;
        }
        if ((c == '"')) {
          // Append(91,2,None)
          replace(91);
          append(2);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        return false;
      case 87:
        if (('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ')) {
          // Append(87,11,Some(87))
          append(11);
          pendingFinish = 87;
          if (verbose) printStack();
          return true;
        }
        if ((c == '}')) {
          // Finish(38)
          replace(38);
          if (verbose) printStack();
          finish();
          if (verbose) printStack();
          return true;
        }
        return false;
      case 88:
        if ((c == '}')) {
          // Finish(88)
          finish();
          if (verbose) printStack();
          return true;
        }
        return false;
      case 89:
        if ((c == '0')) {
          // Finish(89)
          finish();
          if (verbose) printStack();
          return true;
        }
        if (('1' <= c && c <= '9')) {
          // Finish(89)
          finish();
          if (verbose) printStack();
          return true;
        }
        if (('A' <= c && c <= 'F') || ('a' <= c && c <= 'f')) {
          // Finish(89)
          finish();
          if (verbose) printStack();
          return true;
        }
        return false;
      case 90:
        if (('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ')) {
          // Append(90,34,Some(90))
          append(34);
          pendingFinish = 90;
          if (verbose) printStack();
          return true;
        }
        if ((c == '"')) {
          // Append(38,2,None)
          replace(38);
          append(2);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        return false;
      case 91:
        if (('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ')) {
          // Append(91,34,None)
          append(34);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == '"')) {
          // Append(91,2,None)
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
        if (verbose) {
          printStack();
        }
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
    boolean succeed = parseVerbose("{\"abcd\": [\"hello\", 123, {\"xyz\": 1}]}");
    log("Parsing " + (succeed ? "succeeded" : "failed"));
  }
}
