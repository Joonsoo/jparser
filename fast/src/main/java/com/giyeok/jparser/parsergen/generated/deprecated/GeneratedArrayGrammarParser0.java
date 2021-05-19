package com.giyeok.jparser.parsergen.generated.deprecated;

public class GeneratedArrayGrammarParser0 {
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

  public GeneratedArrayGrammarParser0() {
    last = new Node(1, null);
  }

  private boolean canHandle(int nodeTypeId, char c) {
    switch (nodeTypeId) {
      case 1:
        return (c == '[');
      case 2:
        return (c == ' ') || (c == 'a');
      case 3:
        return (c == 'a');
      case 4:
        return (c == ' ') || (c == ',');
      case 5:
        return (c == ' ');
      case 6:
        return (c == ' ');
      case 7:
        return (c == ' ') || (c == ']');
      case 8:
        return (c == ' ');
      case 9:
        return (c == ']');
      case 11:
        return (c == ' ');
      case 12:
        return (c == ',');
      case 13:
        return (c == ' ') || (c == 'a');
      case 14:
        return (c == ' ');
      case 15:
        return (c == 'a');
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

    if (prevNodeType == 1 && lastNodeType == 3) {
      last = new Node(7, last.parent);
      pendingFinish = false;
    } else if (prevNodeType == 1 && lastNodeType == 5) {
      last = new Node(3, last.parent);
      pendingFinish = false;
    } else if (prevNodeType == 1 && lastNodeType == 8) {
      last = new Node(9, last.parent);
      pendingFinish = false;
    } else if (prevNodeType == 1 && lastNodeType == 9) {
      last = last.parent;
      finish();
    } else if (prevNodeType == 3 && lastNodeType == 11) {
      last = new Node(12, last.parent);
      pendingFinish = false;
    } else if (prevNodeType == 3 && lastNodeType == 12) {
      last = new Node(13, last.parent);
      pendingFinish = false;
    } else if (prevNodeType == 3 && lastNodeType == 14) {
      last = new Node(15, last.parent);
      pendingFinish = false;
    } else if (prevNodeType == 3 && lastNodeType == 15) {
      last = last.parent;
      finish();
    } else if (prevNodeType == 5 && lastNodeType == 6) {
      last = last.parent;
      finish();
    } else if (prevNodeType == 6 && lastNodeType == 6) {
      last = last.parent;
      finish();
    } else if (prevNodeType == 8 && lastNodeType == 6) {
      last = last.parent;
      finish();
    } else if (prevNodeType == 11 && lastNodeType == 6) {
      last = last.parent;
      finish();
    } else if (prevNodeType == 14 && lastNodeType == 6) {
      last = last.parent;
      finish();
    } else if (prevNodeType == 15 && lastNodeType == 11) {
      last = new Node(12, last.parent);
      pendingFinish = false;
    } else if (prevNodeType == 15 && lastNodeType == 12) {
      last = new Node(13, last.parent);
      pendingFinish = false;
    } else if (prevNodeType == 15 && lastNodeType == 14) {
      last = new Node(15, last.parent);
      pendingFinish = false;
    } else if (prevNodeType == 15 && lastNodeType == 15) {
      last = last.parent;
      finish();
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
        if ((next == '[')) {
          append(2, false);
          return true;
        }
        break;
      case 2: // [*WS elems WS ]|[ WS*elems WS ]
        if ((next == ' ')) {
          replace(5);
          append(6, true);
          return true;
        } else if ((next == 'a')) {
          replace(3);
          append(4, true);
          return true;
        }
        break;
      case 3: // [ WS*elems WS ]
        if ((next == 'a')) {
          append(4, true);
          return true;
        }
        break;
      case 4: // elem*WS , WS elems|elem WS*, WS elems
        if ((next == ' ')) {
          replace(11);
          append(6, true);
          return true;
        } else if ((next == ',')) {
          replace(12);
          append(10, true);
          return true;
        }
        break;
      case 5: // [*WS elems WS ]
        if ((next == ' ')) {
          append(6, true);
          return true;
        }
        break;
      case 6: // \u0020*WS
        if ((next == ' ')) {
          append(6, true);
          return true;
        }
        break;
      case 7: // [ WS elems*WS ]|[ WS elems WS*]
        if ((next == ' ')) {
          replace(8);
          append(6, true);
          return true;
        } else if ((next == ']')) {
          replace(9);
          append(10, true);
          return true;
        }
        break;
      case 8: // [ WS elems*WS ]
        if ((next == ' ')) {
          append(6, true);
          return true;
        }
        break;
      case 9: // [ WS elems WS*]
        if ((next == ']')) {
          finish();
          return true;
        }
        break;
      case 11: // elem*WS , WS elems
        if ((next == ' ')) {
          append(6, true);
          return true;
        }
        break;
      case 12: // elem WS*, WS elems
        if ((next == ',')) {
          finish();
          return true;
        }
        break;
      case 13: // elem WS ,*WS elems|elem WS , WS*elems
        if ((next == ' ')) {
          replace(14);
          append(6, true);
          return true;
        } else if ((next == 'a')) {
          replace(15);
          append(4, true);
          return true;
        }
        break;
      case 14: // elem WS ,*WS elems
        if ((next == ' ')) {
          append(6, true);
          return true;
        }
        break;
      case 15: // elem WS , WS*elems
        if ((next == 'a')) {
          append(4, true);
          // TODO 여기서 append(4)가 되어도 ' '을 처리할 수 있고, pendingFinish를 따라 위로 올라가다보면 (1 -> 7)에서도 ' '을 처리할 수 있음
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
        return "{[•WS elems WS ]|[ WS•elems WS ]}";
      case 3:
        return "{[ WS•elems WS ]}";
      case 4:
        return "{elem•WS , WS elems|elem WS•, WS elems}";
      case 5:
        return "{[•WS elems WS ]}";
      case 6:
        return "{\\\\u0020•WS}";
      case 7:
        return "{[ WS elems•WS ]|[ WS elems WS•]}";
      case 8:
        return "{[ WS elems•WS ]}";
      case 9:
        return "{[ WS elems WS•]}";
      case 10:
        return "{}";
      case 11:
        return "{elem•WS , WS elems}";
      case 12:
        return "{elem WS•, WS elems}";
      case 13:
        return "{elem WS ,•WS elems|elem WS , WS•elems}";
      case 14:
        return "{elem WS ,•WS elems}";
      case 15:
        return "{elem WS , WS•elems}";
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
    GeneratedArrayGrammarParser0 parser = new GeneratedArrayGrammarParser0();
    if (parser.proceed("[ a,   a,  a,a ]")) {
      boolean result = parser.eof();
      System.out.println(result);
    } else {
      System.out.println("Failed");
    }
  }
}
