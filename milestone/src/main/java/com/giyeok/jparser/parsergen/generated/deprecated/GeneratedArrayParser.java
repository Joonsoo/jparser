package com.giyeok.jparser.parsergen.generated.deprecated;

public class GeneratedArrayParser {
    static class Node {
        public final int nodeTypeId;
        public final Node parent;

        public Node(int nodeTypeId, Node parent) {
            this.nodeTypeId = nodeTypeId;
            this.parent = parent;
        }
    }

    static class Node2 extends Node {
        public final int nodeTypeId;
        public final Node hold1, hold2;

        public Node2(int nodeTypeId, Node hold1, Node hold2) {
            super(nodeTypeId, hold1);
            this.nodeTypeId = nodeTypeId;
            this.hold1 = hold1;
            this.hold2 = hold2;
        }
    }

    private int location;
    private Node last;
    private boolean pendingFinish;

    public GeneratedArrayParser() {
        last = new Node(1, null);
    }

    private boolean canHandle(int nodeTypeId, char c) {
        switch (nodeTypeId) {
            case 1:
                return (c == '[');
            case 2:
                return (c == ' ') || (c == 'a');
            case 3:
                return (c == ' ') || (c == ']') || (c == 'a');
            case 4:
                return (c == ']');
            case 5:
                return (c == 'a');
            case 6:
                return (c == ' ') || (c == ',');
            case 7:
                return (c == ' ');
            case 8:
                return (c == ' ');
            case 10:
                return (c == ' ') || (c == ']');
            case 11:
                return (c == ' ') || (c == ',') || (c == 'a');
            case 12:
                return (c == 'a');
            case 13:
                return (c == ',');
            case 14:
                return (c == ' ');
            case 15:
                return (c == ' ') || (c == 'a');
            case 16:
                return (c == ' ');
            case 17:
                return (c == ' ');
            case 19:
                return (c == ',') || (c == ']');
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
        int lastNodeType = last.nodeTypeId;
        if (lastNodeType == 18) {
            Node2 holder = (Node2) last;
            last = new Node2(19, holder.hold1, holder.hold2);
            pendingFinish = false;
        } else {
            int prevNodeType = last.parent.nodeTypeId;

            if (prevNodeType == 1 && lastNodeType == 2) {
                last = new Node(3, last.parent);
                pendingFinish = false;
            } else if (prevNodeType == 1 && lastNodeType == 3) {
                last = new Node(4, last.parent);
                pendingFinish = true;
            } else if (prevNodeType == 1 && lastNodeType == 4) {
                last = last.parent;
                finish();
            } else if (prevNodeType == 1 && lastNodeType == 5) {
                last = new Node(10, last.parent);
                pendingFinish = false;
            } else if (prevNodeType == 1 && lastNodeType == 7) {
                last = new Node(4, last.parent);
                pendingFinish = false;
            } else if (prevNodeType == 1 && lastNodeType == 10) {
                last = new Node(4, last.parent);
                pendingFinish = true;
            } else if (prevNodeType == 1 && lastNodeType == 17) {
                last = new Node(5, last.parent);
                pendingFinish = false;
            } else if (prevNodeType == 4 && lastNodeType == 9) {
                last = last.parent;
                finish();
            } else if (prevNodeType == 5 && lastNodeType == 6) {
                last = new Node(11, last.parent);
                pendingFinish = false;
            } else if (prevNodeType == 5 && lastNodeType == 11) {
                last = new Node(12, last.parent);
                pendingFinish = true;
            } else if (prevNodeType == 5 && lastNodeType == 12) {
                last = last.parent;
                finish();
            } else if (prevNodeType == 5 && lastNodeType == 13) {
                last = new Node(15, last.parent);
                pendingFinish = false;
            } else if (prevNodeType == 5 && lastNodeType == 14) {
                last = new Node(12, last.parent);
                pendingFinish = false;
            } else if (prevNodeType == 5 && lastNodeType == 15) {
                last = new Node(12, last.parent);
                pendingFinish = true;
            } else if (prevNodeType == 5 && lastNodeType == 16) {
                last = new Node(13, last.parent);
                pendingFinish = false;
            } else if (prevNodeType == 7 && lastNodeType == 8) {
                last = last.parent;
                finish();
            } else if (prevNodeType == 8 && lastNodeType == 8) {
                last = last.parent;
                finish();
            } else if (prevNodeType == 12 && lastNodeType == 6) {
                last = new Node(11, last.parent);
                pendingFinish = false;
            } else if (prevNodeType == 12 && lastNodeType == 11) {
                last = new Node(12, last.parent);
                pendingFinish = false;
            } else if (prevNodeType == 12 && lastNodeType == 12) {
                last = last.parent;
                finish();
            } else if (prevNodeType == 12 && lastNodeType == 13) {
                last = new Node(15, last.parent);
                pendingFinish = false;
            } else if (prevNodeType == 12 && lastNodeType == 14) {
                last = new Node(12, last.parent);
                pendingFinish = false;
            } else if (prevNodeType == 12 && lastNodeType == 15) {
                last = new Node(12, last.parent);
                pendingFinish = false;
            } else if (prevNodeType == 12 && lastNodeType == 16) {
                last = new Node(13, last.parent);
                pendingFinish = false;
            } else if (prevNodeType == 13 && lastNodeType == 9) {
                last = last.parent;
                finish();
            } else if (prevNodeType == 14 && lastNodeType == 8) {
                last = last.parent;
                finish();
            } else if (prevNodeType == 16 && lastNodeType == 8) {
                last = last.parent;
                finish();
            } else if (prevNodeType == 17 && lastNodeType == 8) {
                last = last.parent;
                finish();
            } else if (prevNodeType == 18 && lastNodeType == 8) {
                Node2 holder = (Node2) last.parent;
                last = new Node2(19, holder.hold1, holder.hold2);
                pendingFinish = false;
            } else
                throw new RuntimeException("Unknown edge, " + prevNodeType + " -> " + lastNodeType + ", " + nodeDesc(prevNodeType) + " -> " + nodeDesc(lastNodeType));
        }
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
        System.out.println(nodeString() + " " + nodeDescString());
        switch (last.nodeTypeId) {
            case 1: // *<start>
                if ((next == '[')) {
                    append(2, false);
                    return true;
                }
                break;
            case 2: // [*WS elems WS ]|[ WS*elems WS ]
                if ((next == ' ')) {
                    replace(17);
                    append(8, true);
                    return true;
                } else if ((next == 'a')) {
                    replace(5);
                    append(6, true);
                    return true;
                }
                break;
            case 3: // [ WS*elems WS ]|[ WS elems*WS ]|[ WS elems WS*]
                if ((next == ' ')) {
                    replace(7);
                    append(8, true);
                    return true;
                } else if ((next == ']')) {
                    replace(4);
                    append(9, true);
                    return true;
                } else if ((next == 'a')) {
                    replace(5);
                    append(6, true);
                    return true;
                }
                break;
            case 4: // [ WS elems WS*]
                if ((next == ']')) {
                    finish();
                    return true;
                }
                break;
            case 5: // [ WS*elems WS ]
                if ((next == 'a')) {
                    append(6, true);
                    return true;
                }
                break;
            case 6: // elem*WS , WS elems|elem WS*, WS elems -- from 5
                if ((next == ' ')) {
                    // TODO Node2로 replace/append? nodetype 5를 하나 진행시킨 것쪽으로의 가능성, 컴마 받으러 가는 쪽으로의 가능성
                    // 즉, elem*WS , WS elems 및 [ WS elems*WS ] 가 진행중일 가능성을 보아야 함.
                    // 새로운 노드 18: Node2, { elem*WS , WS elems|[ WS elems*WS ] } 로 replace + append(8, true)
                    //   ',' -> last=last.hold1; replace(13); finish();
                    //   ']' -> last=last.hold2; replace(4); finish();
                    // TODO hold2=find nearest node 5
                    // TODO temporary: (should save the nearest nodes in Node)
                    Node nearest5 = last.parent;
                    while (nearest5.nodeTypeId != 5) {
                        nearest5 = nearest5.parent;
                    }
                    last = new Node2(18, last, nearest5);
                    append(8, true);
                    // replace(16);
                    // append(8, true);
                    return true;
                } else if ((next == ',')) {
                    replace(13);
                    append(9, true);
                    return true;
                }
                break;
            case 19:
                Node2 holder = (Node2) last;
                if (next == ',') {
                    last = holder.hold1;
                    replace(13);
                    finish();
                    return true;
                } else if (next == ']') {
                    last = holder.hold2;
                    replace(4);
                    finish();
                    return true;
                }
                break;
            case 7: // [ WS elems*WS ]
                if ((next == ' ')) {
                    append(8, true);
                    return true;
                }
                break;
            case 8: // \u0020*WS
                if ((next == ' ')) {
                    append(8, true);
                    return true;
                }
                break;
            case 10: // [ WS elems*WS ]|[ WS elems WS*]
                if ((next == ' ')) {
                    replace(7);
                    append(8, true);
                    return true;
                } else if ((next == ']')) {
                    replace(4);
                    append(9, true);
                    return true;
                }
                break;
            case 11: // elem WS*, WS elems|elem WS ,*WS elems|elem WS , WS*elems
                if ((next == ' ')) {
                    replace(14);
                    append(8, true);
                    return true;
                } else if ((next == ',')) {
                    replace(13);
                    append(9, true);
                    return true;
                } else if ((next == 'a')) {
                    replace(12);
                    append(6, true);
                    return true;
                }
                break;
            case 12: // elem WS , WS*elems
                if ((next == 'a')) {
                    append(6, true);
                    return true;
                }
                break;
            case 13: // elem WS*, WS elems
                if ((next == ',')) {
                    finish();
                    return true;
                }
                break;
            case 14: // elem WS ,*WS elems
                if ((next == ' ')) {
                    append(8, true);
                    return true;
                }
                break;
            case 15: // elem WS ,*WS elems|elem WS , WS*elems
                if ((next == ' ')) {
                    replace(14);
                    append(8, true);
                    return true;
                } else if ((next == 'a')) {
                    replace(12);
                    append(6, true);
                    return true;
                }
                break;
            case 16: // elem*WS , WS elems
                if ((next == ' ')) {
                    append(8, true);
                    return true;
                }
                break;
            case 17: // [*WS elems WS ]
                if ((next == ' ')) {
                    append(8, true);
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
                return "XXX{[ WS•elems WS ]|[ WS elems•WS ]|[ WS elems WS•]}";
            case 4:
                return "{[ WS elems WS•]}";
            case 5:
                return "{[ WS•elems WS ]}";
            case 6:
                return "{elem•WS , WS elems|elem WS•, WS elems}";
            case 7:
                return "{[ WS elems•WS ]}";
            case 8:
                return "{\\\\u0020•WS}";
            case 9:
                return "{}";
            case 10:
                return "{[ WS elems•WS ]|[ WS elems WS•]}";
            case 11:
                return "XXX{elem WS•, WS elems|elem WS ,•WS elems|elem WS , WS•elems}";
            case 12:
                return "{elem WS , WS•elems}";
            case 13:
                return "{elem WS•, WS elems}";
            case 14:
                return "{elem WS ,•WS elems}";
            case 15:
                return "{elem WS ,•WS elems|elem WS , WS•elems}";
            case 16:
                return "{elem•WS , WS elems}";
            case 17:
                return "{[•WS elems WS ]}";
            case 18:
                return "{elem•WS , WS elems|[ WS elems•WS ]}";
            case 19:
                return "{elem WS•, WS elems|[ WS elems WS•]}";
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
            System.out.println(nodeString() + " " + nodeDescString() + " " + pendingFinish);
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
        GeneratedArrayParser parser = new GeneratedArrayParser();
        if (parser.proceed("[a,a,a,a    ,   a   ]")) {
            boolean result = parser.eof();
            System.out.println(result);
        } else {
            System.out.println("Failed");
        }
    }
}
