package com.giyeok.jparser.parsergen.generated;

public class SimpleArray0GrammarParser {
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

  public SimpleArray0GrammarParser() {
    last = new Node(1, null);
  }

  private boolean canHandle(int nodeTypeId, char c) {
    switch (nodeTypeId) {
      case 1:
        return (c == '[');
      case 2:
        return (c == ' ') || (c == ']') || (c == 'a');
      case 3:
        return (c == ' ') || (c == 'a');
      case 4:
        return (c == ' ') || (c == 'a');
      case 5:
        return (c == ' ') || (c == 'a');
      case 6:
        return (c == ' ') || (c == ',');
      case 7:
        return (c == ']');
      case 9:
        return (c == ' ') || (c == ']');
      case 10:
        return (c == ' ');
      case 11:
        return (c == ' ');
      case 12:
        return (c == ' ') || (c == 'a');
      case 13:
        return (c == ' ') || (c == ',');
      case 14:
        return (c == ',');
      case 15:
        return (c == 'a');
      case 16:
        return (c == ' ');
      case 17:
        return (c == ' ') || (c == ',');
      case 18:
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
      last = new Node(9, last.parent);
      pendingFinish = false;
    } else if (prevNodeType == 1 && lastNodeType == 7) {
      last = last.parent;
      finish();
    } else if (prevNodeType == 1 && lastNodeType == 10) {
      last = new Node(7, last.parent);
      pendingFinish = false;
    } else if (prevNodeType == 3 && lastNodeType == 11) {
      last = new Node(4, last.parent);
      pendingFinish = true;
    } else if (prevNodeType == 3 && lastNodeType == 18) {
      last = new Node(6, new Node(5, last.parent.parent));
      pendingFinish = true;
    } else if (prevNodeType == 5 && lastNodeType == 6) {
      last = last.parent;
      finish();
    } else if (prevNodeType == 5 && lastNodeType == 11) {
      last = new Node(4, last.parent);
      pendingFinish = false;
    } else if (prevNodeType == 5 && lastNodeType == 18) {
      last = new Node(6, last.parent);
      pendingFinish = true;
    } else if (prevNodeType == 6 && lastNodeType == 11) {
      last = new Node(13, last.parent);
      pendingFinish = false;
    } else if (prevNodeType == 6 && lastNodeType == 14) {
      last = new Node(12, last.parent);
      pendingFinish = false;
    } else if (prevNodeType == 6 && lastNodeType == 15) {
      last = new Node(17, last.parent);
      pendingFinish = true;
    } else if (prevNodeType == 6 && lastNodeType == 16) {
      last = new Node(15, last.parent);
      pendingFinish = false;
    } else if (prevNodeType == 6 && lastNodeType == 17) {
      last = new Node(17, last.parent);
      pendingFinish = true;
    } else if (prevNodeType == 10 && lastNodeType == 11) {
      last = new Node(11, last.parent);
      pendingFinish = true;
    } else if (prevNodeType == 16 && lastNodeType == 11) {
      last = new Node(11, last.parent);
      pendingFinish = true;
    } else if (prevNodeType == 17 && lastNodeType == 11) {
      last = new Node(13, last.parent);
      pendingFinish = false;
    } else if (prevNodeType == 17 && lastNodeType == 14) {
      last = new Node(12, last.parent);
      pendingFinish = false;
    } else if (prevNodeType == 17 && lastNodeType == 15) {
      last = last.parent;
      finish();
    } else if (prevNodeType == 17 && lastNodeType == 16) {
      last = new Node(15, last.parent);
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
        if ((next == '[')) {
          append(2, false);
          return true;
        }
        break;
      case 2: // [*([WS E ([WS , WS E])*])? WS ]
              // [ ([WS E ([WS , WS E])*])?*WS ]
              // [ ([WS E ([WS , WS E])*])? WS*]
        if ((next == ' ')) {
          replace(3);
          append(4, true);
          return true;
        } else if ((next == ']')) {
          replace(7);
          append(8, true);
          return true;
        } else if ((next == 'a')) {
          replace(5);
          append(6, true);
          return true;
        }
        break;
      case 3: // [*([WS E ([WS , WS E])*])? WS ]
              // [ ([WS E ([WS , WS E])*])?*WS ]
        if ((next == ' ')) {
          append(4, true);
          return true;
        } else if ((next == 'a')) {
          replace(5);
          append(6, true);
          return true;
        }
        break;
      case 4: // \u0020**\u0020|WS*E ([WS , WS E])*
        if ((next == ' ')) {
          replace(11);
          append(8, true);
          return true;
        } else if ((next == 'a')) {
          replace(18);
          append(8, true);
          return true;
        }
        break;
      case 5: // [*([WS E ([WS , WS E])*])? WS ]
        if ((next == ' ')) {
          append(4, false);
          return true;
        } else if ((next == 'a')) {
          append(6, true);
          return true;
        }
        break;
      case 6: // WS E*([WS , WS E])*
        if ((next == ' ')) {
          append(13, false);
          return true;
        } else if ((next == ',')) {
          append(12, false);
          return true;
        }
        break;
      case 7: // [ ([WS E ([WS , WS E])*])? WS*]
        if ((next == ']')) {
          finish();
          return true;
        }
        break;
      case 9: // [ ([WS E ([WS , WS E])*])?*WS ]
              // [ ([WS E ([WS , WS E])*])? WS*]
        if ((next == ' ')) {
          replace(10);
          append(11, true);
          return true;
        } else if ((next == ']')) {
          replace(7);
          append(8, true);
          return true;
        }
        break;
      case 10: // [ ([WS E ([WS , WS E])*])?*WS ]
        if ((next == ' ')) {
          append(11, true);
          return true;
        }
        break;
      case 11: // \u0020**\u0020
        if ((next == ' ')) {
          finish();
          return true;
        }
        break;
      case 12: // WS ,*WS E|WS , WS*E
        if ((next == ' ')) {
          replace(16);
          append(11, true);
          return true;
        } else if ((next == 'a')) {
          replace(15);
          append(8, true);
          return true;
        }
        break;
      case 13: // \u0020**\u0020
               // WS*, WS E
        if ((next == ' ')) {
          replace(11);
          append(8, true);
          return true;
        } else if ((next == ',')) {
          replace(14);
          append(8, true);
          return true;
        }
        break;
      case 14: // WS*, WS E
        if ((next == ',')) {
          finish();
          return true;
        }
        break;
      case 15: // WS , WS*E
        if ((next == 'a')) {
          finish();
          return true;
        }
        break;
      case 16: // WS ,*WS E
        if ((next == ' ')) {
          append(11, true);
          return true;
        }
        break;
      case 17: // ([WS , WS E])**([WS , WS E])
        if ((next == ' ')) {
          append(13, false);
          return true;
        } else if ((next == ',')) {
          append(12, false);
          return true;
        }
        break;
      case 18: // WS*E ([WS , WS E])*
        if ((next == 'a')) {
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
        return "{[•([WS E ([WS , WS E])*])? WS ]|[ ([WS E ([WS , WS E])*])?•WS ]|[ ([WS E ([WS , WS E])*])? WS•]}";
      case 3:
        return "{[•([WS E ([WS , WS E])*])? WS ]|[ ([WS E ([WS , WS E])*])?•WS ]}";
      case 4:
        return "{\\\\u0020*•\\\\u0020|WS•E ([WS , WS E])*}";
      case 5:
        return "{[•([WS E ([WS , WS E])*])? WS ]}";
      case 6:
        return "{WS E•([WS , WS E])*}";
      case 7:
        return "{[ ([WS E ([WS , WS E])*])? WS•]}";
      case 8:
        return "{}";
      case 9:
        return "{[ ([WS E ([WS , WS E])*])?•WS ]|[ ([WS E ([WS , WS E])*])? WS•]}";
      case 10:
        return "{[ ([WS E ([WS , WS E])*])?•WS ]}";
      case 11:
        return "{\\\\u0020*•\\\\u0020}";
      case 12:
        return "{WS ,•WS E|WS , WS•E}";
      case 13:
        return "{\\\\u0020*•\\\\u0020|WS•, WS E}";
      case 14:
        return "{WS•, WS E}";
      case 15:
        return "{WS , WS•E}";
      case 16:
        return "{WS ,•WS E}";
      case 17:
        return "{([WS , WS E])*•([WS , WS E])}";
      case 18:
        return "{WS•E ([WS , WS E])*}";
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
    SimpleArray0GrammarParser parser = new SimpleArray0GrammarParser();
    if (parser.proceed("[a    ]")) {
      boolean result = parser.eof();
      System.out.println(result);
    } else {
      System.out.println("Failed");
    }
  }
}
