package com.giyeok.jparser.parsergen.generated;

public class SuperSimpleParser {
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

  public SuperSimpleParser() {
    last = new Node(1, null);
  }

  private boolean canHandle(int nodeTypeId, char c) {
    switch (nodeTypeId) {
      case 1:
        return (c == 'x');
      case 2:
        return (c == 'a');
      case 3:
        return (c == 'b');
      case 4:
        return (c == 'y');
      case 5:
        return (c == 'a');
      case 7:
        return (c == 'y');
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
      last = new Node(4, last.parent);
      pendingFinish = false;
    } else if (prevNodeType == 1 && lastNodeType == 4) {
      last = last.parent;
      finish();
    } else if (prevNodeType == 1 && lastNodeType == 5) {
      last = new Node(7, last.parent);
      pendingFinish = false;
    } else if (prevNodeType == 1 && lastNodeType == 7) {
      last = last.parent;
      finish();
    } else if (prevNodeType == 2 && lastNodeType == 3) {
      last = new Node(6, new Node(5, last.parent.parent));
      pendingFinish = true;
    } else if (prevNodeType == 5 && lastNodeType == 3) {
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
        if ((next == 'x')) {
          append(2, false);
          return true;
        }
        break;
      case 2: // x*A y|x*B y
        if ((next == 'a')) {
          append(3, true);
          return true;
        }
        break;
      case 3: // a*b
        if ((next == 'b')) {
          finish();
          return true;
        }
        break;
      case 4: // x A*y|x B*y
        if ((next == 'y')) {
          finish();
          return true;
        }
        break;
      case 5: // x*B y
        if ((next == 'a')) {
          append(3, false);
          return true;
        }
        break;
      case 7: // x B*y
        if ((next == 'y')) {
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
        return "{x•A y|x•B y}";
      case 3:
        return "{a•b}";
      case 4:
        return "{x A•y|x B•y}";
      case 5:
        return "{x•B y}";
      case 6:
        return "{}";
      case 7:
        return "{x B•y}";
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
    SuperSimpleParser parser = new SuperSimpleParser();
    if (parser.proceed("xay")) {
      boolean result = parser.eof();
      System.out.println(result);
    } else {
      System.out.println("Failed");
    }
  }
}
