package com.giyeok.jparser.parsergen.generated.deprecated;

public class GeneratedExprStringInterpolation0GrammarParser {
  static class Node {
    public final int nodeTypeId;
    public final Node parent;

    public Node(int nodeTypeId, Node parent) {
      this.nodeTypeId = nodeTypeId;
      this.parent = parent;
    }
  }

  private int location;
  private Node last;
  private boolean pendingFinish;

  public GeneratedExprStringInterpolation0GrammarParser() {
    last = new Node(1, null);
  }

  private boolean canHandle(int nodeTypeId, char c) {
    switch (nodeTypeId) {
      case 1:
        return (c == '"')
            || (c == '(')
            || ('0' <= c && c <= '9')
            || ('A' <= c && c <= 'Z')
            || ('a' <= c && c <= 'z');
      case 2:
        return (c == ' ')
            || (c == '"')
            || (c == '$')
            || ('0' <= c && c <= '9')
            || ('A' <= c && c <= 'Z')
            || (c == '\\')
            || ('a' <= c && c <= 'z');
      case 3:
        return ('*' <= c && c <= '+') || ('A' <= c && c <= 'Z') || ('a' <= c && c <= 'z');
      case 4:
        return (c == '"')
            || (c == '(')
            || ('0' <= c && c <= '9')
            || ('A' <= c && c <= 'Z')
            || ('a' <= c && c <= 'z');
      case 5:
        return ('*' <= c && c <= '+') || ('0' <= c && c <= '9');
      case 6:
        return ('*' <= c && c <= '+');
      case 7:
        return (c == '*');
      case 9:
        return (c == '+');
      case 10:
        return (c == '"')
            || (c == '(')
            || ('0' <= c && c <= '9')
            || ('A' <= c && c <= 'Z')
            || ('a' <= c && c <= 'z');
      case 11:
        return (c == '"')
            || (c == '(')
            || ('0' <= c && c <= '9')
            || ('A' <= c && c <= 'Z')
            || ('a' <= c && c <= 'z');
      case 12:
        return ('A' <= c && c <= 'Z') || ('a' <= c && c <= 'z');
      case 13:
        return ('0' <= c && c <= '9');
      case 14:
        return ('0' <= c && c <= '9');
      case 15:
        return (c == '*') || ('A' <= c && c <= 'Z') || ('a' <= c && c <= 'z');
      case 16:
        return (c == '*') || ('0' <= c && c <= '9');
      case 17:
        return (c == ')');
      case 18:
        return (c == '"');
      case 19:
        return (c == ' ')
            || (c == '$')
            || ('0' <= c && c <= '9')
            || ('A' <= c && c <= 'Z')
            || (c == '\\')
            || ('a' <= c && c <= 'z');
      case 20:
        return (c == '$') || (c == '\\') || (c == 'n');
      case 21:
        return (c == ' ')
            || (c == '$')
            || ('0' <= c && c <= '9')
            || ('A' <= c && c <= 'Z')
            || (c == '\\')
            || ('a' <= c && c <= 'z');
      case 22:
        return (c == '{');
      case 23:
        return (c == '"')
            || (c == '(')
            || ('0' <= c && c <= '9')
            || ('A' <= c && c <= 'Z')
            || ('a' <= c && c <= 'z');
      case 24:
        return (c == '}');
    }
    throw new RuntimeException("Unknown nodeTypeId=" + nodeTypeId);
  }

  private void append(int newNodeType, boolean pendingFinish) {
    last = new Node(newNodeType, last);
    this.pendingFinish = pendingFinish;
  }

  private void replace(int newNodeType) {
    last = new Node(newNodeType, last.parent);
    this.pendingFinish = false;
  }

  private void finish() {
    System.out.println(nodeString() + " " + nodeDescString());
    int prevNodeType = last.parent.nodeTypeId, lastNodeType = last.nodeTypeId;

    if (prevNodeType == 1 && lastNodeType == 4) {
      last = new Node(17, last.parent);
      pendingFinish = false;
    } else if (prevNodeType == 1 && lastNodeType == 7) {
      last = new Node(11, last.parent);
      pendingFinish = false;
    } else if (prevNodeType == 1 && lastNodeType == 9) {
      last = new Node(10, last.parent);
      pendingFinish = false;
    } else if (prevNodeType == 1 && lastNodeType == 10) {
      last = new Node(9, last.parent);
      pendingFinish = true;
    } else if (prevNodeType == 1 && lastNodeType == 11) {
      last = new Node(6, last.parent);
      pendingFinish = true;
    } else if (prevNodeType == 1 && lastNodeType == 12) {
      last = new Node(3, last.parent);
      pendingFinish = true;
    } else if (prevNodeType == 1 && lastNodeType == 13) {
      last = new Node(6, last.parent);
      pendingFinish = true;
    } else if (prevNodeType == 1 && lastNodeType == 17) {
      last = new Node(6, last.parent);
      pendingFinish = true;
    } else if (prevNodeType == 1 && lastNodeType == 18) {
      last = new Node(6, last.parent);
      pendingFinish = true;
    } else if (prevNodeType == 1 && lastNodeType == 19) {
      last = new Node(18, last.parent);
      pendingFinish = false;
    } else if (prevNodeType == 4 && lastNodeType == 4) {
      last = new Node(17, last.parent);
      pendingFinish = false;
    } else if (prevNodeType == 4 && lastNodeType == 7) {
      last = new Node(11, last.parent);
      pendingFinish = false;
    } else if (prevNodeType == 4 && lastNodeType == 9) {
      last = new Node(10, last.parent);
      pendingFinish = false;
    } else if (prevNodeType == 4 && lastNodeType == 10) {
      last = new Node(9, last.parent);
      pendingFinish = true;
    } else if (prevNodeType == 4 && lastNodeType == 11) {
      last = new Node(6, last.parent);
      pendingFinish = true;
    } else if (prevNodeType == 4 && lastNodeType == 12) {
      last = new Node(3, last.parent);
      pendingFinish = true;
    } else if (prevNodeType == 4 && lastNodeType == 13) {
      last = new Node(6, last.parent);
      pendingFinish = true;
    } else if (prevNodeType == 4 && lastNodeType == 17) {
      last = new Node(6, last.parent);
      pendingFinish = true;
    } else if (prevNodeType == 4 && lastNodeType == 18) {
      last = new Node(6, last.parent);
      pendingFinish = true;
    } else if (prevNodeType == 4 && lastNodeType == 19) {
      last = new Node(18, last.parent);
      pendingFinish = false;
    } else if (prevNodeType == 10 && lastNodeType == 4) {
      last = new Node(17, last.parent);
      pendingFinish = false;
    } else if (prevNodeType == 10 && lastNodeType == 7) {
      last = new Node(11, last.parent);
      pendingFinish = false;
    } else if (prevNodeType == 10 && lastNodeType == 11) {
      last = new Node(7, last.parent);
      pendingFinish = true;
    } else if (prevNodeType == 10 && lastNodeType == 12) {
      last = new Node(15, last.parent);
      pendingFinish = true;
    } else if (prevNodeType == 10 && lastNodeType == 13) {
      last = new Node(7, last.parent);
      pendingFinish = true;
    } else if (prevNodeType == 10 && lastNodeType == 17) {
      last = new Node(7, last.parent);
      pendingFinish = true;
    } else if (prevNodeType == 10 && lastNodeType == 18) {
      last = new Node(7, last.parent);
      pendingFinish = true;
    } else if (prevNodeType == 10 && lastNodeType == 19) {
      last = new Node(18, last.parent);
      pendingFinish = false;
    } else if (prevNodeType == 11 && lastNodeType == 4) {
      last = new Node(17, last.parent);
      pendingFinish = false;
    } else if (prevNodeType == 11 && lastNodeType == 12) {
      last = new Node(12, last.parent);
      pendingFinish = true;
    } else if (prevNodeType == 11 && lastNodeType == 13) {
      last = last.parent;
      finish();
    } else if (prevNodeType == 11 && lastNodeType == 17) {
      last = last.parent;
      finish();
    } else if (prevNodeType == 11 && lastNodeType == 18) {
      last = last.parent;
      finish();
    } else if (prevNodeType == 11 && lastNodeType == 19) {
      last = new Node(18, last.parent);
      pendingFinish = false;
    } else if (prevNodeType == 13 && lastNodeType == 14) {
      last = new Node(14, last.parent);
      pendingFinish = true;
    } else if (prevNodeType == 19 && lastNodeType == 20) {
      last = new Node(21, last.parent);
      pendingFinish = true;
    } else if (prevNodeType == 19 && lastNodeType == 21) {
      last = new Node(21, last.parent);
      pendingFinish = true;
    } else if (prevNodeType == 19 && lastNodeType == 22) {
      last = new Node(23, last.parent);
      pendingFinish = false;
    } else if (prevNodeType == 19 && lastNodeType == 23) {
      last = new Node(24, last.parent);
      pendingFinish = false;
    } else if (prevNodeType == 19 && lastNodeType == 24) {
      last = new Node(21, last.parent);
      pendingFinish = true;
    } else if (prevNodeType == 21 && lastNodeType == 20) {
      last = last.parent;
      finish();
    } else if (prevNodeType == 21 && lastNodeType == 22) {
      last = new Node(23, last.parent);
      pendingFinish = false;
    } else if (prevNodeType == 21 && lastNodeType == 23) {
      last = new Node(24, last.parent);
      pendingFinish = false;
    } else if (prevNodeType == 21 && lastNodeType == 24) {
      last = last.parent;
      finish();
    } else if (prevNodeType == 23 && lastNodeType == 4) {
      last = new Node(17, last.parent);
      pendingFinish = false;
    } else if (prevNodeType == 23 && lastNodeType == 7) {
      last = new Node(11, last.parent);
      pendingFinish = false;
    } else if (prevNodeType == 23 && lastNodeType == 9) {
      last = new Node(10, last.parent);
      pendingFinish = false;
    } else if (prevNodeType == 23 && lastNodeType == 10) {
      last = new Node(9, last.parent);
      pendingFinish = true;
    } else if (prevNodeType == 23 && lastNodeType == 11) {
      last = new Node(6, last.parent);
      pendingFinish = true;
    } else if (prevNodeType == 23 && lastNodeType == 12) {
      last = new Node(3, last.parent);
      pendingFinish = true;
    } else if (prevNodeType == 23 && lastNodeType == 13) {
      last = new Node(6, last.parent);
      pendingFinish = true;
    } else if (prevNodeType == 23 && lastNodeType == 17) {
      last = new Node(6, last.parent);
      pendingFinish = true;
    } else if (prevNodeType == 23 && lastNodeType == 18) {
      last = new Node(6, last.parent);
      pendingFinish = true;
    } else if (prevNodeType == 23 && lastNodeType == 19) {
      last = new Node(18, last.parent);
      pendingFinish = false;
    } else
      throw new RuntimeException(
          "Unknown edge, "
              + prevNodeType
              + " -> "
              + lastNodeType
              + ", "
              + nodeDesc(prevNodeType)
              + " -> "
              + nodeDesc(lastNodeType));
  }

  private boolean tryFinishable(char next) {
    if (pendingFinish) {
      while (pendingFinish) {
        last = last.parent;
        finish();
        if (canHandle(last.nodeTypeId, next)) {
          return proceed1(next);
        }
      }
      return proceed1(next);
    } else {
      return false;
    }
  }

  private boolean proceed1(char next) {
    switch (last.nodeTypeId) {
      case 1: // *<start>
        if ((next == '"')) {
          append(2, false);
          return true;
        } else if ((next == '(')) {
          append(4, false);
          return true;
        } else if ((next == '0')) {
          append(6, true);
          return true;
        } else if (('1' <= next && next <= '9')) {
          append(5, true);
          return true;
        } else if (('A' <= next && next <= 'Z') || ('a' <= next && next <= 'z')) {
          append(3, true);
          return true;
        }
        break;
      case 2: // "*stringElem[0-] "|" stringElem[0-]*"
        if ((next == ' ')
            || ('0' <= next && next <= '9')
            || ('A' <= next && next <= 'Z')
            || ('a' <= next && next <= 'z')) {
          replace(19);
          append(21, true);
          return true;
        } else if ((next == '"')) {
          replace(18);
          append(8, true);
          return true;
        } else if ((next == '$')) {
          replace(19);
          append(22, false);
          return true;
        } else if ((next == '\\')) {
          replace(19);
          append(20, false);
          return true;
        }
        break;
      case 3: // {A-Za-z}[1-]*{A-Za-z}|term** factor|expression*+ term
        if ((next == '*')) {
          replace(7);
          append(8, true);
          return true;
        } else if ((next == '+')) {
          replace(9);
          append(8, true);
          return true;
        } else if (('A' <= next && next <= 'Z') || ('a' <= next && next <= 'z')) {
          replace(12);
          append(8, true);
          return true;
        }
        break;
      case 4: // (*expression )
        if ((next == '"')) {
          append(2, false);
          return true;
        } else if ((next == '(')) {
          append(4, false);
          return true;
        } else if ((next == '0')) {
          append(6, true);
          return true;
        } else if (('1' <= next && next <= '9')) {
          append(5, true);
          return true;
        } else if (('A' <= next && next <= 'Z') || ('a' <= next && next <= 'z')) {
          append(3, true);
          return true;
        }
        break;
      case 5: // {1-9}*{0-9}[0-]|term** factor|expression*+ term
        if ((next == '*')) {
          replace(7);
          append(8, true);
          return true;
        } else if ((next == '+')) {
          replace(9);
          append(8, true);
          return true;
        } else if (('0' <= next && next <= '9')) {
          replace(13);
          append(14, true);
          return true;
        }
        break;
      case 6: // term** factor|expression*+ term
        if ((next == '*')) {
          replace(7);
          append(8, true);
          return true;
        } else if ((next == '+')) {
          replace(9);
          append(8, true);
          return true;
        }
        break;
      case 7: // term** factor
        if ((next == '*')) {
          finish();
          return true;
        }
        break;
      case 9: // expression*+ term
        if ((next == '+')) {
          finish();
          return true;
        }
        break;
      case 10: // expression +*term
        if ((next == '"')) {
          append(2, false);
          return true;
        } else if ((next == '(')) {
          append(4, false);
          return true;
        } else if ((next == '0')) {
          append(7, true);
          return true;
        } else if (('1' <= next && next <= '9')) {
          append(16, true);
          return true;
        } else if (('A' <= next && next <= 'Z') || ('a' <= next && next <= 'z')) {
          append(15, true);
          return true;
        }
        break;
      case 11: // term **factor
        if ((next == '"')) {
          append(2, false);
          return true;
        } else if ((next == '(')) {
          append(4, false);
          return true;
        } else if ((next == '0')) {
          finish();
          return true;
        } else if (('1' <= next && next <= '9')) {
          append(13, true);
          return true;
        } else if (('A' <= next && next <= 'Z') || ('a' <= next && next <= 'z')) {
          append(12, true);
          return true;
        }
        break;
      case 12: // {A-Za-z}[1-]*{A-Za-z}
        if (('A' <= next && next <= 'Z') || ('a' <= next && next <= 'z')) {
          finish();
          return true;
        }
        break;
      case 13: // {1-9}*{0-9}[0-]
        if (('0' <= next && next <= '9')) {
          append(14, true);
          return true;
        }
        break;
      case 14: // {0-9}[0-]*{0-9}
        if (('0' <= next && next <= '9')) {
          finish();
          return true;
        }
        break;
      case 15: // {A-Za-z}[1-]*{A-Za-z}|term** factor
        if ((next == '*')) {
          replace(7);
          append(8, true);
          return true;
        } else if (('A' <= next && next <= 'Z') || ('a' <= next && next <= 'z')) {
          replace(12);
          append(8, true);
          return true;
        }
        break;
      case 16: // {1-9}*{0-9}[0-]|term** factor
        if ((next == '*')) {
          replace(7);
          append(8, true);
          return true;
        } else if (('0' <= next && next <= '9')) {
          replace(13);
          append(14, true);
          return true;
        }
        break;
      case 17: // ( expression*)
        if ((next == ')')) {
          finish();
          return true;
        }
        break;
      case 18: // " stringElem[0-]*"
        if ((next == '"')) {
          finish();
          return true;
        }
        break;
      case 19: // "*stringElem[0-] "
        if ((next == ' ')
            || ('0' <= next && next <= '9')
            || ('A' <= next && next <= 'Z')
            || ('a' <= next && next <= 'z')) {
          append(21, true);
          return true;
        } else if ((next == '$')) {
          append(22, false);
          return true;
        } else if ((next == '\\')) {
          append(20, false);
          return true;
        }
        break;
      case 20: // \*{$\n}
        if ((next == '$') || (next == '\\') || (next == 'n')) {
          finish();
          return true;
        }
        break;
      case 21: // stringElem[0-]*stringElem
        if ((next == ' ')
            || ('0' <= next && next <= '9')
            || ('A' <= next && next <= 'Z')
            || ('a' <= next && next <= 'z')) {
          finish();
          return true;
        } else if ((next == '$')) {
          append(22, false);
          return true;
        } else if ((next == '\\')) {
          append(20, false);
          return true;
        }
        break;
      case 22: // $*{ expression }
        if ((next == '{')) {
          finish();
          return true;
        }
        break;
      case 23: // $ {*expression }
        if ((next == '"')) {
          append(2, false);
          return true;
        } else if ((next == '(')) {
          append(4, false);
          return true;
        } else if ((next == '0')) {
          append(6, true);
          return true;
        } else if (('1' <= next && next <= '9')) {
          append(5, true);
          return true;
        } else if (('A' <= next && next <= 'Z') || ('a' <= next && next <= 'z')) {
          append(3, true);
          return true;
        }
        break;
      case 24: // $ { expression*}
        if ((next == '}')) {
          finish();
          return true;
        }
        break;
    }
    return tryFinishable(next);
  }

  private String nodeString(Node node) {
    if (node.parent == null) return "" + node.nodeTypeId;
    else return nodeString(node.parent) + " " + node.nodeTypeId;
  }

  public String nodeString() {
    return nodeString(last);
  }

  public String nodeDesc(int nodeTypeId) {
    switch (nodeTypeId) {
      case 1:
        return "{•<start>}";
      case 2:
        return "{\\\"•stringElem[0-] \\\"|\\\" stringElem[0-]•\\\"}";
      case 3:
        return "{{A-Za-z}[1-]•{A-Za-z}|term•* factor|expression•+ term}";
      case 4:
        return "{(•expression )}";
      case 5:
        return "{{1-9}•{0-9}[0-]|term•* factor|expression•+ term}";
      case 6:
        return "{term•* factor|expression•+ term}";
      case 7:
        return "{term•* factor}";
      case 8:
        return "{}";
      case 9:
        return "{expression•+ term}";
      case 10:
        return "{expression +•term}";
      case 11:
        return "{term *•factor}";
      case 12:
        return "{{A-Za-z}[1-]•{A-Za-z}}";
      case 13:
        return "{{1-9}•{0-9}[0-]}";
      case 14:
        return "{{0-9}[0-]•{0-9}}";
      case 15:
        return "{{A-Za-z}[1-]•{A-Za-z}|term•* factor}";
      case 16:
        return "{{1-9}•{0-9}[0-]|term•* factor}";
      case 17:
        return "{( expression•)}";
      case 18:
        return "{\\\" stringElem[0-]•\\\"}";
      case 19:
        return "{\\\"•stringElem[0-] \\\"}";
      case 20:
        return "{\\\\•{$\\\\n}}";
      case 21:
        return "{stringElem[0-]•stringElem}";
      case 22:
        return "{$•{ expression }}";
      case 23:
        return "{$ {•expression }}";
      case 24:
        return "{$ { expression•}}";
    }
    throw new RuntimeException("Unknown nodeTypeId=" + nodeTypeId);
  }

  private String nodeDescString(Node node) {
    if (node.parent == null) return "" + nodeDesc(node.nodeTypeId);
    else return nodeDescString(node.parent) + " " + nodeDesc(node.nodeTypeId);
  }

  public String nodeDescString() {
    return nodeDescString(last);
  }

  public boolean proceed(char next) {
    location += 1;
    return proceed1(next);
  }

  public boolean proceed(String next) {
    for (int i = 0; i < next.length(); i++) {
      System.out.println(nodeString() + " " + nodeDescString());
      System.out.println(i + " " + next.charAt(i));
      if (!proceed(next.charAt(i))) {
        return false;
      }
    }
    return true;
  }

  public boolean eof() {
    while (pendingFinish) {
      last = last.parent;
      if (last.nodeTypeId == 1) {
        return pendingFinish;
      }
      finish();
    }
    return false;
  }

  public static void main(String[] args) {
    GeneratedExprStringInterpolation0GrammarParser parser =
        new GeneratedExprStringInterpolation0GrammarParser();
    if (parser.proceed("123+\"asdf ${1+45} fdsa\"+(456*789)")) {
      boolean result = parser.eof();
      System.out.println(result);
    } else {
      System.out.println("Failed");
    }
  }
}
