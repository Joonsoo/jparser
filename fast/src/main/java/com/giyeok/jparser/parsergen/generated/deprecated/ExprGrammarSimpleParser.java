package com.giyeok.jparser.parsergen.generated.deprecated;

public class ExprGrammarSimpleParser {
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

    public ExprGrammarSimpleParser() {
        last = new Node(1, null);
    }

    private boolean canHandle(int nodeTypeId, char c) {
        switch (nodeTypeId) {
            case 1:
                return (c == '(') || ('0' <= c && c <= '9');
            case 2:
                return ('*' <= c && c <= '+');
            case 3:
                return ('*' <= c && c <= '+') || ('0' <= c && c <= '9');
            case 4:
                return (c == '(') || ('0' <= c && c <= '9');
            case 5:
                return ('0' <= c && c <= '9');
            case 6:
                return (c == '(') || ('0' <= c && c <= '9');
            case 7:
                return (c == '(') || ('0' <= c && c <= '9');
            case 8:
                return ('0' <= c && c <= '9');
            case 9:
                return (c == '*');
            case 10:
                return (c == '*') || ('0' <= c && c <= '9');
            case 11:
                return (c == '+');
            case 12:
                return (c == ')');
            case 13:
                return (c == '*') || ('0' <= c && c <= '9');
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

    private void inreplaceAppend(int replaceNodeType, int appendNodeType, boolean pendingFinish) {
        last = new Node(appendNodeType, new Node(replaceNodeType, last.parent));
        this.pendingFinish = pendingFinish;
    }

    private void finish() {
        System.out.println(nodeString() + " " + nodeDescString());
        int prevNodeType = last.parent.nodeTypeId, lastNodeType = last.nodeTypeId;

        if (prevNodeType == 1 && lastNodeType == 4) {
            last = new Node(12, last.parent);
            pendingFinish = false;
        } else if (prevNodeType == 1 && lastNodeType == 6) {
            last = new Node(2, last.parent);
            pendingFinish = true;
        } else if (prevNodeType == 1 && lastNodeType == 7) {
            last = new Node(2, last.parent);
            pendingFinish = true;
        } else if (prevNodeType == 1 && lastNodeType == 8) {
            last = new Node(3, last.parent);
            pendingFinish = true;
        } else if (prevNodeType == 1 && lastNodeType == 12) {
            last = new Node(2, last.parent);
            pendingFinish = true;
        } else if (prevNodeType == 4 && lastNodeType == 4) {
            last = new Node(12, last.parent);
            pendingFinish = false;
        } else if (prevNodeType == 4 && lastNodeType == 6) {
            last = new Node(2, last.parent);
            pendingFinish = true;
        } else if (prevNodeType == 4 && lastNodeType == 7) {
            last = new Node(11, last.parent);
            pendingFinish = true;
        } else if (prevNodeType == 4 && lastNodeType == 8) {
            last = new Node(3, last.parent);
            pendingFinish = true;
        } else if (prevNodeType == 4 && lastNodeType == 11) {
            last = last.parent;
            finish();
        } else if (prevNodeType == 4 && lastNodeType == 12) {
            last = new Node(2, last.parent);
            pendingFinish = true;
        } else if (prevNodeType == 6 && lastNodeType == 4) {
            last = new Node(12, last.parent);
            pendingFinish = false;
        } else if (prevNodeType == 6 && lastNodeType == 8) {
            last = last.parent;
            finish();
        } else if (prevNodeType == 6 && lastNodeType == 12) {
            last = last.parent;
            finish();
        } else if (prevNodeType == 7 && lastNodeType == 4) {
            last = new Node(12, last.parent);
            pendingFinish = false;
        } else if (prevNodeType == 7 && lastNodeType == 5) {
            last = new Node(13, last.parent);
            pendingFinish = true;
        } else if (prevNodeType == 7 && lastNodeType == 6) {
            last = new Node(9, last.parent);
            pendingFinish = true;
        } else if (prevNodeType == 7 && lastNodeType == 8) {
            last = new Node(9, last.parent);
            pendingFinish = true;
        } else if (prevNodeType == 7 && lastNodeType == 9) {
            last = new Node(9, last.parent);
            pendingFinish = false;
        } else if (prevNodeType == 7 && lastNodeType == 10) {
            last = last.parent;
            finish();
        } else if (prevNodeType == 7 && lastNodeType == 12) {
            last = new Node(9, last.parent);
            pendingFinish = true;
        } else if (prevNodeType == 8 && lastNodeType == 5) {
            last = new Node(5, last.parent);
            pendingFinish = true;
        } else
            throw new RuntimeException("Unknown edge, " + prevNodeType + " -> " + lastNodeType + ", " + nodeDesc(prevNodeType) + " -> " + nodeDesc(lastNodeType));
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
            case 1:
                if ((next == '(')) {
                    append(4, false);
                    return true;
                } else if ((next == '0')) {
                    append(2, true);
                    return true;
                } else if (('1' <= next && next <= '9')) {
                    append(3, true);
                    return true;
                }
                break;
            case 2:
                if ((next == '*')) {
                    replace(6);
                    return true;
                } else if ((next == '+')) {
                    replace(7);
                    return true;
                }
                break;
            case 3:
                if ((next == '*')) {
                    replace(6);
                    return true;
                } else if ((next == '+')) {
                    replace(7);
                    return true;
                } else if (('0' <= next && next <= '9')) {
                    inreplaceAppend(8, 5, true);
                    return true;
                }
                break;
            case 4:
                if ((next == '(')) {
                    append(4, false);
                    return true;
                } else if ((next == '0')) {
                    append(2, false);
                    return true;
                } else if (('1' <= next && next <= '9')) {
                    append(3, false);
                    return true;
                }
                break;
            case 5:
                if (('0' <= next && next <= '9')) {
                    finish();
                    return true;
                }
                break;
            case 6:
                if ((next == '(')) {
                    append(4, false);
                    return true;
                } else if ((next == '0')) {
                    finish();
                    return true;
                } else if (('1' <= next && next <= '9')) {
                    append(8, true);
                    return true;
                }
                break;
            case 7:
                if ((next == '(')) {
                    append(4, false);
                    return true;
                } else if ((next == '0')) {
                    append(9, true);
                    return true;
                } else if (('1' <= next && next <= '9')) {
                    append(10, true);
                    return true;
                }
                break;
            case 8:
                if (('0' <= next && next <= '9')) {
                    append(5, true);
                    return true;
                }
                break;
            case 9:
                if ((next == '*')) {
                    replace(6);
                    return true;
                }
                break;
            case 10:
                if ((next == '*')) {
                    replace(6);
                    return true;
                } else if (('0' <= next && next <= '9')) {
                    inreplaceAppend(8, 5, true);
                    return true;
                }
                break;
            case 11:
                if ((next == '+')) {
                    replace(7);
                    return true;
                }
                break;
            case 12:
                if ((next == ')')) {
                    finish();
                    return true;
                }
                break;
            case 13:
                if ((next == '*')) {
                    replace(6);
                    return true;
                } else if (('0' <= next && next <= '9')) {
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
            case 5:
                return "{{0-9}[0-].{0-9}}";
            case 10:
                return "{{1-9}.{0-9}[0-],T.* F}";
            case 1:
                return "{.<start>}";
            case 6:
                return "{T *.F}";
            case 9:
                return "{T.* F}";
            case 13:
                return "{{0-9}[0-].{0-9},T.* F}";
            case 2:
                return "{T.* F,E.+ T}";
            case 12:
                return "{( E.)}";
            case 7:
                return "{E +.T}";
            case 3:
                return "{{1-9}.{0-9}[0-],T.* F,E.+ T}";
            case 11:
                return "{E.+ T}";
            case 8:
                return "{{1-9}.{0-9}[0-]}";
            case 4:
                return "{(.E )}";
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
            if (i == 10) {
                System.out.println();
            }
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
        ExprGrammarSimpleParser p = new ExprGrammarSimpleParser();
        p.proceed("123+456+234+567*987+654*321+(0+123)");
        boolean result = p.eof();
        // TODO p.finalizeParsing: pendingFinish여야하고, finish()를 계속 해서 1 노드만 남아있을 때까지 줄일 수 있어야 하고, 줄인 뒤에 pendingFinish여야 함
        System.out.println(result);
    }
}
