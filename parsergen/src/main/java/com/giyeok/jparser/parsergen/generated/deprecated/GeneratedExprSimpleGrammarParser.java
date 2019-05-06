package com.giyeok.jparser.parsergen.generated.deprecated;

public class GeneratedExprSimpleGrammarParser {
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

  public GeneratedExprSimpleGrammarParser() {
    last = new Node(1, null);
  }

  private boolean canHandle(int nodeTypeId, char c) {
    switch (nodeTypeId) {
      case 1:
        return (c == '(') || ('0' <= c && c <= '9');
      case 2:
        return (c == '(') || ('0' <= c && c <= '9');
      case 3:
        return ('*' <= c && c <= '+') || ('0' <= c && c <= '9');
      case 4:
        return ('*' <= c && c <= '+');
      case 5:
        return (c == '*');
      case 7:
        return (c == '+');
      case 8:
        return (c == '(') || ('0' <= c && c <= '9');
      case 9:
        return (c == '(') || ('0' <= c && c <= '9');
      case 10:
        return ('0' <= c && c <= '9');
      case 11:
        return ('0' <= c && c <= '9');
      case 12:
        return (c == '*') || ('0' <= c && c <= '9');
      case 13:
        return (c == ')');
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

    if (prevNodeType == 1 && lastNodeType == 2) {
      last = new Node(13, last.parent);
      pendingFinish = false;
    } else if (prevNodeType == 1 && lastNodeType == 5) {
      last = new Node(9, last.parent);
      pendingFinish = false;
    } else if (prevNodeType == 1 && lastNodeType == 7) {
      last = new Node(8, last.parent);
      pendingFinish = false;
    } else if (prevNodeType == 1 && lastNodeType == 8) {
      last = new Node(7, last.parent);
      pendingFinish = true;
    } else if (prevNodeType == 1 && lastNodeType == 9) {
      last = new Node(4, last.parent);
      pendingFinish = true;
    } else if (prevNodeType == 1 && lastNodeType == 10) {
      last = new Node(4, last.parent);
      pendingFinish = true;
    } else if (prevNodeType == 1 && lastNodeType == 13) {
      last = new Node(4, last.parent);
      pendingFinish = true;
    } else if (prevNodeType == 2 && lastNodeType == 2) {
      last = new Node(13, last.parent);
      pendingFinish = false;
    } else if (prevNodeType == 2 && lastNodeType == 5) {
      last = new Node(9, last.parent);
      pendingFinish = false;
    } else if (prevNodeType == 2 && lastNodeType == 7) {
      last = new Node(8, last.parent);
      pendingFinish = false;
    } else if (prevNodeType == 2 && lastNodeType == 8) {
      last = new Node(7, last.parent);
      pendingFinish = true;
    } else if (prevNodeType == 2 && lastNodeType == 9) {
      last = new Node(4, last.parent);
      pendingFinish = true;
    } else if (prevNodeType == 2 && lastNodeType == 10) {
      last = new Node(4, last.parent);
      pendingFinish = true;
    } else if (prevNodeType == 2 && lastNodeType == 13) {
      last = new Node(4, last.parent);
      pendingFinish = true;
    } else if (prevNodeType == 8 && lastNodeType == 2) {
      last = new Node(13, last.parent);
      pendingFinish = false;
    } else if (prevNodeType == 8 && lastNodeType == 5) {
      last = new Node(9, last.parent);
      pendingFinish = false;
    } else if (prevNodeType == 8 && lastNodeType == 9) {
      last = new Node(5, last.parent);
      pendingFinish = true;
    } else if (prevNodeType == 8 && lastNodeType == 10) {
      last = new Node(5, last.parent);
      pendingFinish = true;
    } else if (prevNodeType == 8 && lastNodeType == 13) {
      last = new Node(5, last.parent);
      pendingFinish = true;
    } else if (prevNodeType == 9 && lastNodeType == 2) {
      last = new Node(13, last.parent);
      pendingFinish = false;
    } else if (prevNodeType == 9 && lastNodeType == 10) {
      last = last.parent;
      finish();
    } else if (prevNodeType == 9 && lastNodeType == 13) {
      last = last.parent;
      finish();
    } else if (prevNodeType == 10 && lastNodeType == 11) {
      last = new Node(11, last.parent);
      pendingFinish = true;
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
        if ((next == '(')) {
          append(2, false);
          return true;
        } else if ((next == '0')) {
          append(4, true);
          return true;
        } else if (('1' <= next && next <= '9')) {
          append(3, true);
          return true;
        }
        break;
      case 2: // (*E )
        if ((next == '(')) {
          append(2, false);
          return true;
        } else if ((next == '0')) {
          append(4, true);
          return true;
        } else if (('1' <= next && next <= '9')) {
          append(3, true);
          return true;
        }
        break;
      case 3: // {1-9}*{0-9}[0-]|T** F|E*+ T
        if ((next == '*')) {
          replace(5);
          append(6, true);
          return true;
        } else if ((next == '+')) {
          replace(7);
          append(6, true);
          return true;
        } else if (('0' <= next && next <= '9')) {
          replace(10);
          append(11, true);
          return true;
        }
        break;
      case 4: // T** F|E*+ T
        if ((next == '*')) {
          replace(5);
          append(6, true);
          return true;
        } else if ((next == '+')) {
          replace(7);
          append(6, true);
          return true;
        }
        break;
      case 5: // T** F
        if ((next == '*')) {
          finish();
          return true;
        }
        break;
      case 7: // E*+ T
        if ((next == '+')) {
          finish();
          return true;
        }
        break;
      case 8: // E +*T
        if ((next == '(')) {
          append(2, false);
          return true;
        } else if ((next == '0')) {
          append(5, true);
          return true;
        } else if (('1' <= next && next <= '9')) {
          append(12, true);
          return true;
        }
        break;
      case 9: // T **F
        if ((next == '(')) {
          append(2, false);
          return true;
        } else if ((next == '0')) {
          finish();
          return true;
        } else if (('1' <= next && next <= '9')) {
          append(10, true);
          return true;
        }
        break;
      case 10: // {1-9}*{0-9}[0-]
        if (('0' <= next && next <= '9')) {
          append(11, true);
          return true;
        }
        break;
      case 11: // {0-9}[0-]*{0-9}
        if (('0' <= next && next <= '9')) {
          finish();
          return true;
        }
        break;
      case 12: // {1-9}*{0-9}[0-]|T** F
        if ((next == '*')) {
          replace(5);
          append(6, true);
          return true;
        } else if (('0' <= next && next <= '9')) {
          replace(10);
          append(11, true);
          return true;
        }
        break;
      case 13: // ( E*)
        if ((next == ')')) {
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
        return "{(•E )}";
      case 3:
        return "{{1-9}•{0-9}[0-]|T•* F|E•+ T}";
      case 4:
        return "{T•* F|E•+ T}";
      case 5:
        return "{T•* F}";
      case 6:
        return "{}";
      case 7:
        return "{E•+ T}";
      case 8:
        return "{E +•T}";
      case 9:
        return "{T *•F}";
      case 10:
        return "{{1-9}•{0-9}[0-]}";
      case 11:
        return "{{0-9}[0-]•{0-9}}";
      case 12:
        return "{{1-9}•{0-9}[0-]|T•* F}";
      case 13:
        return "{( E•)}";
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
    GeneratedExprSimpleGrammarParser parser = new GeneratedExprSimpleGrammarParser();
    if (parser.proceed("123+(456*789+987+654)*321")) {
      boolean result = parser.eof();
      System.out.println(result);
    } else {
      System.out.println("Failed");
    }
  }
}
