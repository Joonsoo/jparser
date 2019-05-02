package com.giyeok.jparser.parsergen.generated;

public class SimpleLexer2Parser {
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

  public SimpleLexer2Parser() {
    last = new Node(1, null);
  }

  private boolean canHandle(int nodeTypeId, char c) {
    switch (nodeTypeId) {
      case 1:
        return (c == ' ') || ('a' <= c && c <= 'z');
      case 2:
        return (c == ' ') || ('a' <= c && c <= 'z');
      case 3:
        return (c == ' ') || ('a' <= c && c <= 'z');
      case 4:
        return (c == ' ') || ('a' <= c && c <= 'z');
      case 5:
        return ('a' <= c && c <= 'z');
      case 6:
        return ('a' <= c && c <= 'z');
      case 8:
        return ('a' <= c && c <= 'z');
      case 9:
        return (c == ' ') || ('a' <= c && c <= 'z');
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
      last = new Node(9, last.parent);
      pendingFinish = true;
    } else if (prevNodeType == 1 && lastNodeType == 3) {
      last = new Node(3, last.parent);
      pendingFinish = true;
    } else if (prevNodeType == 1 && lastNodeType == 4) {
      last = new Node(4, last.parent);
      pendingFinish = true;
    } else if (prevNodeType == 1 && lastNodeType == 9) {
      last = new Node(3, last.parent);
      pendingFinish = true;
    } else if (prevNodeType == 2 && lastNodeType == 6) {
      last = new Node(6, new Node(4, last.parent.parent));
      pendingFinish = true;
    } else if (prevNodeType == 3 && lastNodeType == 5) {
      last = new Node(8, new Node(4, last.parent.parent));
      pendingFinish = true;
    } else if (prevNodeType == 3 && lastNodeType == 6) {
      last = new Node(6, new Node(4, last.parent.parent));
      pendingFinish = true;
    } else if (prevNodeType == 4 && lastNodeType == 5) {
      last = new Node(8, last.parent);
      pendingFinish = true;
    } else if (prevNodeType == 4 && lastNodeType == 6) {
      last = new Node(6, last.parent);
      pendingFinish = true;
    } else if (prevNodeType == 4 && lastNodeType == 8) {
      last = new Node(6, last.parent);
      pendingFinish = true;
    } else if (prevNodeType == 9 && lastNodeType == 6) {
      last = new Node(6, new Node(4, last.parent.parent));
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
        if ((next == ' ')) {
          append(4, true);
          return true;
        } else if (('a' <= next && next <= 'w')) {
          append(3, true);
          return true;
        } else if ((next == 'x')) {
          append(2, true);
          return true;
        } else if ((next == 'y')) {
          append(3, true);
          return true;
        } else if ((next == 'z')) {
          append(3, true);
          return true;
        }
        break;
      case 2: // x*y z|{a-w}|x|y|z[1-]*{a-w}|x|y|z|T[0-]*T
        if ((next == ' ')) {
          replace(4);
          append(7, true);
          return true;
        } else if (('a' <= next && next <= 'w')) {
          replace(3);
          append(6, true);
          return true;
        } else if ((next == 'x')) {
          replace(3);
          append(5, true);
          return true;
        } else if ((next == 'y')) {
          append(6, true);
          return true;
        } else if ((next == 'z')) {
          replace(3);
          append(6, true);
          return true;
        }
        break;
      case 3: // {a-w}|x|y|z[1-]*{a-w}|x|y|z|T[0-]*T
        if ((next == ' ')) {
          replace(4);
          append(7, true);
          return true;
        } else if (('a' <= next && next <= 'w')) {
          append(6, true);
          return true;
        } else if ((next == 'x')) {
          append(5, true);
          return true;
        } else if ((next == 'y')) {
          append(6, true);
          return true;
        } else if ((next == 'z')) {
          append(6, true);
          return true;
        }
        break;
      case 4: // T[0-]*T
        if ((next == ' ')) {
          finish();
          return true;
        } else if (('a' <= next && next <= 'w')) {
          append(6, true);
          return true;
        } else if ((next == 'x')) {
          append(5, true);
          return true;
        } else if ((next == 'y')) {
          append(6, true);
          return true;
        } else if ((next == 'z')) {
          append(6, true);
          return true;
        }
        break;
      case 5: // x*y z|{a-w}|x|y|z[1-]*{a-w}|x|y|z
        if (('a' <= next && next <= 'w')) {
          replace(6);
          append(7, true);
          return true;
        } else if ((next == 'x')) {
          replace(6);
          append(7, true);
          return true;
        } else if ((next == 'y')) {
          finish();
          return true;
        } else if ((next == 'z')) {
          replace(6);
          append(7, true);
          return true;
        }
        break;
      case 6: // {a-w}|x|y|z[1-]*{a-w}|x|y|z
        if (('a' <= next && next <= 'w')) {
          finish();
          return true;
        } else if ((next == 'x')) {
          finish();
          return true;
        } else if ((next == 'y')) {
          finish();
          return true;
        } else if ((next == 'z')) {
          finish();
          return true;
        }
        break;
      case 8: // x y*z|{a-w}|x|y|z[1-]*{a-w}|x|y|z
        if (('a' <= next && next <= 'w')) {
          replace(6);
          append(7, true);
          return true;
        } else if ((next == 'x')) {
          replace(6);
          append(7, true);
          return true;
        } else if ((next == 'y')) {
          replace(6);
          append(7, true);
          return true;
        } else if ((next == 'z')) {
          finish();
          return true;
        }
        break;
      case 9: // x y*z|{a-w}|x|y|z[1-]*{a-w}|x|y|z|T[0-]*T
        if ((next == ' ')) {
          replace(4);
          append(7, true);
          return true;
        } else if (('a' <= next && next <= 'w')) {
          replace(3);
          append(6, true);
          return true;
        } else if ((next == 'x')) {
          replace(3);
          append(5, true);
          return true;
        } else if ((next == 'y')) {
          replace(3);
          append(6, true);
          return true;
        } else if ((next == 'z')) {
          append(6, true);
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
        return "{x•y z|{a-w}|x|y|z[1-]•{a-w}|x|y|z|T[0-]•T}";
      case 3:
        return "{{a-w}|x|y|z[1-]•{a-w}|x|y|z|T[0-]•T}";
      case 4:
        return "{T[0-]•T}";
      case 5:
        return "{x•y z|{a-w}|x|y|z[1-]•{a-w}|x|y|z}";
      case 6:
        return "{{a-w}|x|y|z[1-]•{a-w}|x|y|z}";
      case 7:
        return "{}";
      case 8:
        return "{x y•z|{a-w}|x|y|z[1-]•{a-w}|x|y|z}";
      case 9:
        return "{x y•z|{a-w}|x|y|z[1-]•{a-w}|x|y|z|T[0-]•T}";
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
    SimpleLexer2Parser parser = new SimpleLexer2Parser();
    if (parser.proceed("  if  if  xyzfff  xyz  ")) {
      boolean result = parser.eof();
      System.out.println(result);
    } else {
      System.out.println("Failed");
    }
  }
}
