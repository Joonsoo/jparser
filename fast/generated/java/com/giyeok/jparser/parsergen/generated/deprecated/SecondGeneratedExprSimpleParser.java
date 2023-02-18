package com.giyeok.jparser.parsergen.generated.deprecated;

public class SecondGeneratedExprSimpleParser {
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

    public SecondGeneratedExprSimpleParser() {
        last = new Node(1, null);
    }

    private boolean canHandle(int nodeTypeId, char c) {
        switch (nodeTypeId) {
            case 1:
                return (c == '(') || ('0' <= c && c <= '9') || ('A' <= c && c <= 'Z') || ('a' <= c && c <= 'z');
            case 2:
                return ('*' <= c && c <= '+');
            case 3:
                return (c == '(') || ('0' <= c && c <= '9') || ('A' <= c && c <= 'Z') || ('a' <= c && c <= 'z');
            case 4:
                return ('*' <= c && c <= '+') || ('0' <= c && c <= '9');
            case 5:
                return ('*' <= c && c <= '+') || ('A' <= c && c <= 'Z') || ('a' <= c && c <= 'z');
            case 6:
                return (c == '(') || ('0' <= c && c <= '9') || ('A' <= c && c <= 'Z') || ('a' <= c && c <= 'z');
            case 7:
                return (c == ')');
            case 8:
                return (c == '(') || ('*' <= c && c <= '+') || ('0' <= c && c <= '9') || ('A' <= c && c <= 'Z') || ('a' <= c && c <= 'z');
            case 9:
                return (c == '(') || ('*' <= c && c <= '+') || ('0' <= c && c <= '9') || ('A' <= c && c <= 'Z') || ('a' <= c && c <= 'z');
            case 10:
                return (c == '(') || ('0' <= c && c <= '9') || ('A' <= c && c <= 'Z') || ('a' <= c && c <= 'z');
            case 11:
                return (c == '*') || ('A' <= c && c <= 'Z') || ('a' <= c && c <= 'z');
            case 12:
                return (c == '*') || ('0' <= c && c <= '9');
            case 13:
                return (c == '+');
            case 15:
                return (c == '*');
            case 16:
                return (c == '(') || ('0' <= c && c <= '9') || ('A' <= c && c <= 'Z') || ('a' <= c && c <= 'z');
            case 17:
                return (c == '(') || ('0' <= c && c <= '9') || ('A' <= c && c <= 'Z') || ('a' <= c && c <= 'z');
            case 18:
                return (c == '(') || (c == '*') || ('0' <= c && c <= '9') || ('A' <= c && c <= 'Z') || ('a' <= c && c <= 'z');
            case 19:
                return (c == '(') || (c == '*') || ('0' <= c && c <= '9') || ('A' <= c && c <= 'Z') || ('a' <= c && c <= 'z');
            case 20:
                return ('A' <= c && c <= 'Z') || ('a' <= c && c <= 'z');
            case 21:
                return ('0' <= c && c <= '9');
            case 22:
                return ('0' <= c && c <= '9');
            case 23:
                return (c == '(') || ('0' <= c && c <= '9') || ('A' <= c && c <= 'Z') || ('a' <= c && c <= 'z');
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
            last = new Node(6, last.parent);
            pendingFinish = false;
        } else if (prevNodeType == 1 && lastNodeType == 3) {
            last = new Node(7, last.parent);
            pendingFinish = false;
        } else if (prevNodeType == 1 && lastNodeType == 4) {
            last = new Node(8, last.parent);
            pendingFinish = true;
        } else if (prevNodeType == 1 && lastNodeType == 5) {
            last = new Node(9, last.parent);
            pendingFinish = true;
        } else if (prevNodeType == 1 && lastNodeType == 6) {
            last = new Node(2, last.parent);
            pendingFinish = true;
        } else if (prevNodeType == 1 && lastNodeType == 7) {
            last = new Node(2, last.parent);
            pendingFinish = true;
        } else if (prevNodeType == 1 && lastNodeType == 8) {
            last = new Node(8, last.parent);
            pendingFinish = true;
        } else if (prevNodeType == 1 && lastNodeType == 9) {
            last = new Node(9, last.parent);
            pendingFinish = true;
        } else if (prevNodeType == 1 && lastNodeType == 10) {
            last = new Node(5, last.parent);
            pendingFinish = true;
        } else if (prevNodeType == 1 && lastNodeType == 13) {
            last = new Node(16, last.parent);
            pendingFinish = false;
        } else if (prevNodeType == 1 && lastNodeType == 15) {
            last = new Node(17, last.parent);
            pendingFinish = false;
        } else if (prevNodeType == 1 && lastNodeType == 16) {
            last = new Node(13, last.parent);
            pendingFinish = true;
        } else if (prevNodeType == 1 && lastNodeType == 17) {
            last = new Node(2, last.parent);
            pendingFinish = true;
        } else if (prevNodeType == 1 && lastNodeType == 20) {
            last = new Node(5, last.parent);
            pendingFinish = true;
        } else if (prevNodeType == 1 && lastNodeType == 21) {
            last = new Node(2, last.parent);
            pendingFinish = true;
        } else if (prevNodeType == 3 && lastNodeType == 2) {
            last = new Node(6, last.parent);
            pendingFinish = false;
        } else if (prevNodeType == 3 && lastNodeType == 3) {
            last = new Node(7, last.parent);
            pendingFinish = false;
        } else if (prevNodeType == 3 && lastNodeType == 4) {
            last = new Node(8, last.parent);
            pendingFinish = true;
        } else if (prevNodeType == 3 && lastNodeType == 5) {
            last = new Node(9, last.parent);
            pendingFinish = true;
        } else if (prevNodeType == 3 && lastNodeType == 6) {
            last = new Node(2, last.parent);
            pendingFinish = true;
        } else if (prevNodeType == 3 && lastNodeType == 7) {
            last = new Node(2, last.parent);
            pendingFinish = true;
        } else if (prevNodeType == 3 && lastNodeType == 8) {
            last = new Node(8, last.parent);
            pendingFinish = true;
        } else if (prevNodeType == 3 && lastNodeType == 9) {
            last = new Node(9, last.parent);
            pendingFinish = true;
        } else if (prevNodeType == 3 && lastNodeType == 10) {
            last = new Node(5, last.parent);
            pendingFinish = true;
        } else if (prevNodeType == 3 && lastNodeType == 13) {
            last = new Node(16, last.parent);
            pendingFinish = false;
        } else if (prevNodeType == 3 && lastNodeType == 15) {
            last = new Node(17, last.parent);
            pendingFinish = false;
        } else if (prevNodeType == 3 && lastNodeType == 16) {
            last = new Node(13, last.parent);
            pendingFinish = true;
        } else if (prevNodeType == 3 && lastNodeType == 17) {
            last = new Node(2, last.parent);
            pendingFinish = true;
        } else if (prevNodeType == 3 && lastNodeType == 20) {
            last = new Node(5, last.parent);
            pendingFinish = true;
        } else if (prevNodeType == 3 && lastNodeType == 21) {
            last = new Node(2, last.parent);
            pendingFinish = true;
        } else if (prevNodeType == 6 && lastNodeType == 3) {
            last = new Node(7, last.parent);
            pendingFinish = false;
        } else if (prevNodeType == 6 && lastNodeType == 7) {
            last = new Node(15, last.parent);
            pendingFinish = true;
        } else if (prevNodeType == 6 && lastNodeType == 11) {
            last = new Node(18, new Node(16, last.parent.parent));
            pendingFinish = true;
        } else if (prevNodeType == 6 && lastNodeType == 12) {
            last = new Node(19, new Node(16, last.parent.parent));
            pendingFinish = true;
        } else if (prevNodeType == 6 && lastNodeType == 15) {
            last = new Node(17, new Node(16, last.parent.parent));
            pendingFinish = false;
        } else if (prevNodeType == 6 && lastNodeType == 20) {
            last = new Node(11, last.parent);
            pendingFinish = true;
        } else if (prevNodeType == 6 && lastNodeType == 21) {
            last = new Node(15, last.parent);
            pendingFinish = true;
        } else if (prevNodeType == 10 && lastNodeType == 11) {
            last = new Node(18, new Node(16, last.parent.parent));
            pendingFinish = true;
        } else if (prevNodeType == 10 && lastNodeType == 15) {
            last = new Node(17, new Node(16, last.parent.parent));
            pendingFinish = false;
        } else if (prevNodeType == 10 && lastNodeType == 20) {
            last = new Node(11, new Node(6, last.parent.parent));
            pendingFinish = true;
        } else if (prevNodeType == 13 && lastNodeType == 14) {
            last = last.parent;
            finish();
        } else if (prevNodeType == 15 && lastNodeType == 14) {
            last = last.parent;
            finish();
        } else if (prevNodeType == 16 && lastNodeType == 3) {
            last = new Node(7, last.parent);
            pendingFinish = false;
        } else if (prevNodeType == 16 && lastNodeType == 7) {
            last = new Node(15, last.parent);
            pendingFinish = true;
        } else if (prevNodeType == 16 && lastNodeType == 11) {
            last = new Node(18, last.parent);
            pendingFinish = true;
        } else if (prevNodeType == 16 && lastNodeType == 12) {
            last = new Node(19, last.parent);
            pendingFinish = true;
        } else if (prevNodeType == 16 && lastNodeType == 15) {
            last = new Node(17, last.parent);
            pendingFinish = false;
        } else if (prevNodeType == 16 && lastNodeType == 17) {
            last = new Node(15, last.parent);
            pendingFinish = true;
        } else if (prevNodeType == 16 && lastNodeType == 18) {
            last = new Node(18, last.parent);
            pendingFinish = true;
        } else if (prevNodeType == 16 && lastNodeType == 19) {
            last = new Node(19, last.parent);
            pendingFinish = true;
        } else if (prevNodeType == 16 && lastNodeType == 20) {
            last = new Node(11, last.parent);
            pendingFinish = true;
        } else if (prevNodeType == 16 && lastNodeType == 21) {
            last = new Node(15, last.parent);
            pendingFinish = true;
        } else if (prevNodeType == 16 && lastNodeType == 23) {
            last = new Node(11, last.parent);
            pendingFinish = true;
        } else if (prevNodeType == 17 && lastNodeType == 3) {
            last = new Node(7, last.parent);
            pendingFinish = false;
        } else if (prevNodeType == 17 && lastNodeType == 7) {
            last = last.parent;
            finish();
        } else if (prevNodeType == 17 && lastNodeType == 14) {
            last = last.parent;
            finish();
        } else if (prevNodeType == 17 && lastNodeType == 20) {
            last = new Node(20, last.parent);
            pendingFinish = true;
        } else if (prevNodeType == 17 && lastNodeType == 21) {
            last = last.parent;
            finish();
        } else if (prevNodeType == 20 && lastNodeType == 14) {
            last = last.parent;
            finish();
        } else if (prevNodeType == 21 && lastNodeType == 22) {
            last = new Node(22, last.parent);
            pendingFinish = true;
        } else if (prevNodeType == 23 && lastNodeType == 20) {
            last = new Node(20, new Node(17, last.parent.parent));
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
            case 1: // *<start>
                if ((next == '(')) {
                    append(3, false);
                    return true;
                } else if ((next == '0')) {
                    append(2, true);
                    return true;
                } else if (('1' <= next && next <= '9')) {
                    append(4, true);
                    return true;
                } else if (('A' <= next && next <= 'Z') || ('a' <= next && next <= 'z')) {
                    append(5, true);
                    return true;
                }
                break;
            case 2: // term** factor,expression*+ term
                if ((next == '*')) {
                    replace(15);
                    append(14, true);
                    return true;
                } else if ((next == '+')) {
                    replace(13);
                    append(14, true);
                    return true;
                }
                break;
            case 3: // (*expression )
                if ((next == '(')) {
                    append(3, false);
                    return true;
                } else if ((next == '0')) {
                    append(2, true);
                    return true;
                } else if (('1' <= next && next <= '9')) {
                    append(4, true);
                    return true;
                } else if (('A' <= next && next <= 'Z') || ('a' <= next && next <= 'z')) {
                    append(5, true);
                    return true;
                }
                break;
            case 4: // {1-9}*{0-9}[0-],term** factor,expression*+ term
                if ((next == '*')) {
                    replace(15);
                    append(14, true);
                    return true;
                } else if ((next == '+')) {
                    replace(13);
                    append(14, true);
                    return true;
                } else if (('0' <= next && next <= '9')) {
                    replace(21);
                    append(22, true);
                    return true;
                }
                break;
            case 5: // {A-Za-z}[1-]*{A-Za-z},term** factor,expression*+ term
                if ((next == '*')) {
                    replace(15);
                    append(14, true);
                    return true;
                } else if ((next == '+')) {
                    replace(13);
                    append(14, true);
                    return true;
                } else if (('A' <= next && next <= 'Z') || ('a' <= next && next <= 'z')) {
                    replace(20);
                    append(14, true);
                    return true;
                }
                break;
            case 6: // term **factor,expression +*term
                if ((next == '(')) {
                    append(3, false);
                    return true;
                } else if ((next == '0')) {
                    append(15, true);
                    return true;
                } else if (('1' <= next && next <= '9')) {
                    append(12, true);
                    return true;
                } else if (('A' <= next && next <= 'Z') || ('a' <= next && next <= 'z')) {
                    append(11, true);
                    return true;
                }
                break;
            case 7: // ( expression*)
                if ((next == ')')) {
                    finish();
                    return true;
                }
                break;
            case 8: // term **factor,expression +*term,term** factor,expression*+ term
                if ((next == '(')) {
                    replace(6);
                    append(3, false);
                    return true;
                } else if ((next == '*')) {
                    replace(15);
                    append(14, true);
                    return true;
                } else if ((next == '+')) {
                    replace(13);
                    append(14, true);
                    return true;
                } else if ((next == '0')) {
                    replace(6);
                    append(15, true);
                    return true;
                } else if (('1' <= next && next <= '9')) {
                    replace(6);
                    append(12, true);
                    return true;
                } else if (('A' <= next && next <= 'Z') || ('a' <= next && next <= 'z')) {
                    replace(6);
                    append(11, true);
                    return true;
                }
                break;
            case 9: // term **factor,term** factor,expression*+ term,expression +*term,{A-Za-z}[1-]*{A-Za-z}
                if ((next == '(')) {
                    replace(6);
                    append(3, false);
                    return true;
                } else if ((next == '*')) {
                    replace(15);
                    append(14, true);
                    return true;
                } else if ((next == '+')) {
                    replace(13);
                    append(14, true);
                    return true;
                } else if ((next == '0')) {
                    replace(6);
                    append(15, true);
                    return true;
                } else if (('1' <= next && next <= '9')) {
                    replace(6);
                    append(12, true);
                    return true;
                } else if (('A' <= next && next <= 'Z') || ('a' <= next && next <= 'z')) {
                    replace(10);
                    append(11, true);
                    return true;
                }
                break;
            case 10: // term **factor,expression +*term,{A-Za-z}[1-]*{A-Za-z}
                if ((next == '(')) {
                    replace(6);
                    append(3, false);
                    return true;
                } else if ((next == '0')) {
                    replace(6);
                    append(15, true);
                    return true;
                } else if (('1' <= next && next <= '9')) {
                    replace(6);
                    append(12, true);
                    return true;
                } else if (('A' <= next && next <= 'Z') || ('a' <= next && next <= 'z')) {
                    append(11, true);
                    return true;
                }
                break;
            case 11: // {A-Za-z}[1-]*{A-Za-z},term** factor
                if ((next == '*')) {
                    replace(15);
                    append(14, true);
                    return true;
                } else if (('A' <= next && next <= 'Z') || ('a' <= next && next <= 'z')) {
                    replace(20);
                    append(14, true);
                    return true;
                }
                break;
            case 12: // {1-9}*{0-9}[0-],term** factor
                if ((next == '*')) {
                    replace(15);
                    append(14, true);
                    return true;
                } else if (('0' <= next && next <= '9')) {
                    replace(21);
                    append(22, true);
                    return true;
                }
                break;
            case 13: // expression*+ term
                if ((next == '+')) {
                    finish();
                    return true;
                }
                break;
            case 15: // term** factor
                if ((next == '*')) {
                    finish();
                    return true;
                }
                break;
            case 16: // expression +*term
                if ((next == '(')) {
                    append(3, false);
                    return true;
                } else if ((next == '0')) {
                    append(15, true);
                    return true;
                } else if (('1' <= next && next <= '9')) {
                    append(12, true);
                    return true;
                } else if (('A' <= next && next <= 'Z') || ('a' <= next && next <= 'z')) {
                    append(11, true);
                    return true;
                }
                break;
            case 17: // term **factor
                if ((next == '(')) {
                    append(3, false);
                    return true;
                } else if ((next == '0')) {
                    finish();
                    return true;
                } else if (('1' <= next && next <= '9')) {
                    append(21, true);
                    return true;
                } else if (('A' <= next && next <= 'Z') || ('a' <= next && next <= 'z')) {
                    append(20, true);
                    return true;
                }
                break;
            case 18: // term **factor,{A-Za-z}[1-]*{A-Za-z},term** factor
                if ((next == '(')) {
                    replace(17);
                    append(3, false);
                    return true;
                } else if ((next == '*')) {
                    replace(15);
                    append(14, true);
                    return true;
                } else if ((next == '0')) {
                    replace(17);
                    append(14, true);
                    return true;
                } else if (('1' <= next && next <= '9')) {
                    replace(17);
                    append(21, true);
                    return true;
                } else if (('A' <= next && next <= 'Z') || ('a' <= next && next <= 'z')) {
                    replace(23);
                    append(20, true);
                    return true;
                }
                break;
            case 19: // term **factor,term** factor
                if ((next == '(')) {
                    replace(17);
                    append(3, false);
                    return true;
                } else if ((next == '*')) {
                    replace(15);
                    append(14, true);
                    return true;
                } else if ((next == '0')) {
                    replace(17);
                    append(14, true);
                    return true;
                } else if (('1' <= next && next <= '9')) {
                    replace(17);
                    append(21, true);
                    return true;
                } else if (('A' <= next && next <= 'Z') || ('a' <= next && next <= 'z')) {
                    replace(17);
                    append(20, true);
                    return true;
                }
                break;
            case 20: // {A-Za-z}[1-]*{A-Za-z}
                if (('A' <= next && next <= 'Z') || ('a' <= next && next <= 'z')) {
                    finish();
                    return true;
                }
                break;
            case 21: // {1-9}*{0-9}[0-]
                if (('0' <= next && next <= '9')) {
                    append(22, true);
                    return true;
                }
                break;
            case 22: // {0-9}[0-]*{0-9}
                if (('0' <= next && next <= '9')) {
                    finish();
                    return true;
                }
                break;
            case 23: // term **factor,{A-Za-z}[1-]*{A-Za-z}
                if ((next == '(')) {
                    replace(17);
                    append(3, false);
                    return true;
                } else if ((next == '0')) {
                    replace(17);
                    append(14, true);
                    return true;
                } else if (('1' <= next && next <= '9')) {
                    replace(17);
                    append(21, true);
                    return true;
                } else if (('A' <= next && next <= 'Z') || ('a' <= next && next <= 'z')) {
                    append(20, true);
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
                return "{{A-Za-z}[1-].{A-Za-z},term.* factor,expression.+ term}";
            case 10:
                return "{{A-Za-z}[1-].{A-Za-z},expression +.term,term *.factor}";
            case 14:
                return "{}";
            case 20:
                return "{{A-Za-z}[1-].{A-Za-z}}";
            case 1:
                return "{.<start>}";
            case 6:
                return "{term *.factor,expression +.term}";
            case 21:
                return "{{1-9}.{0-9}[0-]}";
            case 9:
                return "{{A-Za-z}[1-].{A-Za-z},expression +.term,expression.+ term,term.* factor,term *.factor}";
            case 13:
                return "{expression.+ term}";
            case 2:
                return "{term.* factor,expression.+ term}";
            case 17:
                return "{term *.factor}";
            case 22:
                return "{{0-9}[0-].{0-9}}";
            case 12:
                return "{{1-9}.{0-9}[0-],term.* factor}";
            case 7:
                return "{( expression.)}";
            case 3:
                return "{(.expression )}";
            case 18:
                return "{term *.factor,{A-Za-z}[1-].{A-Za-z},term.* factor}";
            case 16:
                return "{expression +.term}";
            case 11:
                return "{{A-Za-z}[1-].{A-Za-z},term.* factor}";
            case 23:
                return "{term *.factor,{A-Za-z}[1-].{A-Za-z}}";
            case 8:
                return "{term *.factor,expression +.term,term.* factor,expression.+ term}";
            case 19:
                return "{term *.factor,term.* factor}";
            case 4:
                return "{{1-9}.{0-9}[0-],term.* factor,expression.+ term}";
            case 15:
                return "{term.* factor}";
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
        SecondGeneratedExprSimpleParser p = new SecondGeneratedExprSimpleParser();
        p.proceed("aaa*((123+bcd))");
        boolean result = p.eof();
        System.out.println(result);
    }
}
