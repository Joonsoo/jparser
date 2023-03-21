package com.giyeok.jparser.parsergen;

import java.util.Objects;

public class ExpressionGrammarSimpleParser {
    // E = T | E '+' T
    // T = F | T '*' F
    // F = N | '(' E ')'
    // N = '0' | '1-9' '0-9'*

    // symbol -> symbolId
    // 1: <start>
    // 2: E
    // 3: T
    // 7: '1-9' '0-9'*
    // 11: '0-9'* '0-9'
    // 13: ( E )
    // 16: T * F
    // 18: E + T

    // nodeTypeId -> kernels
    // 1: {(1, 0)}
    //   = { . <start> }
    // 2: {(16, 1), (18, 1)}
    //   = { T . * F, E . + T }
    // 3: {(7, 1), (16, 1), (18, 1)}
    //   = { '1-9' . '0-9'*, T . * F, E . + T }
    // 4: {(13, 1)}
    //   = { ( . E ) }
    // 5: {(11, 1)}
    //   = { '0-9'* . '0-9' }
    // 6: {(16, 2)}
    //   = { T * . F }
    // 7: {(18, 2)}
    //   = { E + . T }
    // 8: {(7, 1)}
    //   = { '1-9' . '0-9'* }
    // 9: {(16, 1)}
    //   = { T . * F }
    // 10: {(7, 1), (16, 1)}
    //   = { '1-9' . '0-9'*, T . * F }
    // 11: {(18, 1)}
    //   = { E . + T }
    // 12: {(13, 2)}
    //   = { ( E . ) }
    // 13: {(11, 1), (16, 1)}
    //   = { '0-9'* . '0-9', T . * F }
    // 14: {(7, 1)}
    //   = { '1-9' . '0-9'* }

    // nodeTypeId, input(last 노드의 타입에 맞춰서 받을 수 있는 input)
    //      : appending node? (input이 들어왔을 때 새로 추가될 node type)
    //      : replacing node (input이 들어왔을 때 last를 교체, 같은 parent 유지하면서)
    // 1, '0': append 2 + finish
    // 1, '1-9': append 3 + finish
    // 1, '(': append 4
    // 2, '*': replace 6
    // 2, '+': replace 7
    // 3, '0-9': in-replace 14 + append 5 + finish
    // 3, '*': replace 6
    // 3, '+': replace 7
    // 4, '0': append 2
    // 4, '1-9': append 3
    // 4, '(': append 4
    // 5, '0-9': finish
    // 6, '0': finish
    // 6, '1-9': append 8 + finish
    // 6, '(': append 4
    // 7, '0': append 9 + finish
    // 7, '1-9': append 10 + finish
    // 7, '(': append 4
    // 8, '0-9': append 5 + finish
    // 9, '*': replace 6
    // 10, '0-9': in-replace 14 + append 5 + finish
    // 10, '*': replace 6
    // 11, '+': replace 7
    // 12, ')': finish
    // 13, '*': replace 6
    // 13, '0-9': finish

    // 한 글자 받으면 finishable이 될 수 있는 nodes: 1, 3, 5, 6, 7, 8, 10
    //   -> 근데 이게 정말 위 표에서 "+ finish"인 노드들을 고르면 맞는걸까? 맞는 것 같은데..?

    // nodeTypeId, nodeTypeId -> nodeTypeId (엣지 타고 올라갈 때 새로 추가될 node type)
    // 1 바로 밑에 붙을 수 있는 노드: 2, 3, 4, 6, 7, 14
    // 2 바로 밑에 붙을 수 있는 노드:  없음
    // 2에서 바뀔 수 있는 노드: 6, 7
    // 3 바로 밑에 붙을 수 있는 노드: 5
    // 3에서 바뀔 수 있는 노드: 6, 7, 14
    // 4 바로 밑에 붙을 수 있는 노드: 2, 3, 4, 6, 7, 14
    // 5 바로 밑에 붙을 수 있는 노드: 없음
    // 6 바로 밑에 붙을 수 있는 노드: 4, 8
    // 7 바로 밑에 붙을 수 있는 노드: 4, 9, 10, 5, 6, 14
    // 8 바로 밑에 붙을 수 있는 노드: 5
    // 9에서 바뀔 수 있는 노드: 6
    // 10에서 바뀔 수 있는 노드: 5, 6, 14
    // 따라서 가능한 모든 엣지 조합:
    // (그 중 target이 finishable인 엣지만 의미가 있음)
    // finishable하지 않은 엣지 조합: 1 -> 2, 1 -> 4, 4 -> 2, 4 -> 4, 6 -> 4, 7 -> 9, 7 -> 4
    // -- 1 -> 3: 2
    // 1 -> 6: 2
    // 1 -> 7: 11
    // 1 -> 14: 3
    // -- 3 -> 5: 5
    // -- 4 -> 3: (노드 3 안에서도 (16, 1), (18, 1)은 finishable이 아니라서 의미 없음) 2
    // 4 -> 6: 2
    // 4 -> 7: 11
    // 4 -> 14: ??
    // 6 -> 8: .
    // 7 -> 10: (노드 10 안에서도 (16, 1)은 finishable이 아니라서 의미 없음) .
    // 7 -> 6: 9
    // ? -> 4: 12
    // 7 -> 5: 13
    // 7 -> 14: 9
    // 8 -> 5: 5
    // -- 10 -> 5: 5
    // 14 -> 5: 5

    static class Node {
        public final int nodeTypeId;
        public final Node parent;

        public Node(int nodeTypeId, Node parent) {
            this.nodeTypeId = nodeTypeId;
            this.parent = parent;
        }

        private boolean canHandle(char next) {
            switch (nodeTypeId) {
                case 1:
                    return ('0' <= next && next <= '9') || (next == '(');
                case 2:
                    return next == '+' || next == '*';
                case 3:
                    return ('0' <= next && next <= '9') || (next == '*') || (next == '+');
                case 4:
                    return ('0' <= next && next <= '9') || (next == '(');
                case 5:
                    return ('0' <= next && next <= '9');
                case 6:
                    return ('0' <= next && next <= '9') || (next == '(');
                case 7:
                    return ('0' <= next && next <= '9') || (next == '(');
                case 8:
                    return ('0' <= next && next <= '9');
                case 9:
                    return (next == '*');
                case 10:
                    return ('0' <= next && next <= '9') || (next == '*');
                case 11:
                    return (next == '+');
                case 12:
                    return (next == ')');
                case 13:
                    return (next == '*') || ('0' <= next && next <= '9');
            }
            return false;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Node node = (Node) o;
            return nodeTypeId == node.nodeTypeId &&
                    Objects.equals(parent, node.parent);
        }

        @Override
        public int hashCode() {
            return Objects.hash(nodeTypeId, parent);
        }

        @Override
        public String toString() {
            return "Node{nodeTypeId=" + nodeTypeId + ", parent=" + parent + '}';
        }
    }

    private int location;
    private Node last;
    private boolean lastFinishable;

    ExpressionGrammarSimpleParser() {
        last = new Node(1, null);
    }

    private void append(int newNodeType, boolean lastFinishable) {
        System.out.println("append " + newNodeType + " with finishable=" + lastFinishable + " at " + location);
        last = new Node(newNodeType, last);
        this.lastFinishable = lastFinishable;
    }

    private void replace(int newNodeType) {
        System.out.println("replace " + newNodeType + " at " + location);
        last = new Node(newNodeType, last.parent);
        this.lastFinishable = false;
    }

    private void inreplace(int newNodeType) {
        System.out.println("inreplace " + newNodeType + " at " + location);
        last = new Node(newNodeType, last.parent);
        // another operation must follow
    }

    private void finish() {
        System.out.println("finish at " + location);
        // 마지막 노드가 finish되었다고 보고 한 단계 위로 lift
        // return값이 true이면 last에서 더 finish되어야 함을 의미
        //  -> 즉, finish성이면 true/progress성이면 false
        int prevNodeType = last.parent.nodeTypeId, lastNodeType = last.nodeTypeId;

        // lastFinishable은 기존의 last.parent(?)가 progress되었을 때 (점이 맨 끝에 위치하거나 점 뒤에 nullable들이 따라와서) parent를 progress할 수 있으면 true
        if (prevNodeType == 1 && lastNodeType == 6) {
            last = new Node(2, last.parent);
            lastFinishable = true;
        } else if (prevNodeType == 1 && lastNodeType == 7) {
            last = new Node(11, last.parent);
            lastFinishable = true;
        } else if (prevNodeType == 1 && lastNodeType == 14) {
            last = new Node(3, last.parent);
            lastFinishable = true;
        } else if (prevNodeType == 3 && lastNodeType == 5) {
            // 이런 경우엔 사실 ('1-9' . '0-9'*) 커널만 고려하면 된다. 왜냐면 그 외의 케이스는 replace로 처리되었기 때문
            last = new Node(5, last.parent);
            lastFinishable = true;
        } else if (prevNodeType == 4 && lastNodeType == 6) {
            last = new Node(2, last.parent);
            lastFinishable = true;
        } else if (prevNodeType == 4 && lastNodeType == 7) {
            last = new Node(11, last.parent);
            lastFinishable = true;
        } else if (prevNodeType == 4 && lastNodeType == 11) {
            last = new Node(12, last.parent);
            lastFinishable = true;
        } else if (prevNodeType == 4 && lastNodeType == 12) {
            last = new Node(12, last.parent);
            lastFinishable = true;
        } else if (prevNodeType == 4 && lastNodeType == 14) {
            last = new Node(3, last.parent);
            lastFinishable = true;
        } else if (prevNodeType == 6 && lastNodeType == 8) {
            last = last.parent;
            finish();
        } else if (prevNodeType == 7 && lastNodeType == 5) {
            last = new Node(13, last.parent);
            lastFinishable = true;
        } else if (prevNodeType == 7 && lastNodeType == 6) {
            last = new Node(9, last.parent);
            lastFinishable = true;
        } else if (prevNodeType == 7 && lastNodeType == 14) {
            last = new Node(9, last.parent);
            lastFinishable = true;
        } else if (prevNodeType == 8 && lastNodeType == 5) {
            last = new Node(5, last.parent);
            lastFinishable = true;
        } else if (prevNodeType == 10 && lastNodeType == 5) {
            last = new Node(5, last.parent);
            lastFinishable = true;
        } else if (prevNodeType == 14 && lastNodeType == 5) {
            last = new Node(5, last.parent);
            lastFinishable = true;
        } else if (lastNodeType == 4) {
            last = new Node(12, last.parent);
            lastFinishable = false;
        } else {
            throw new RuntimeException("Should not happen! prev=" + prevNodeType + " last=" + lastNodeType);
        }
    }

    private boolean tryFinishable(char next) {
        System.out.println("Try finishable, lastFinishable=" + lastFinishable);
        if (lastFinishable) {
            // TODO
            last = last.parent;
            while (lastFinishable) {
                finish();
                System.out.println(nodeString());
                if (last.canHandle(next)) {
                    return proceed1(next);
                }
                System.out.println(nodeString());
            }
            return proceed1(next);
        } else {
            return false;
        }
    }

    public boolean finalizeParse() {
        if (lastFinishable) {
            last = last.parent;
            finish();
            return true;
        } else {
            return false;
        }
    }

    private boolean proceed1(char next) {
        switch (last.nodeTypeId) {
            case 1:
                if (next == '0') {
                    append(2, true);
                    return true;
                } else if ('1' <= next && next <= '9') {
                    append(3, true);
                    return true;
                } else if (next == '(') {
                    append(4, false);
                    return true;
                }
                break;
            case 2:
                if (next == '*') {
                    replace(6);
                    return true;
                } else if (next == '+') {
                    replace(7);
                    return true;
                }
                break;
            case 3:
                if ('0' <= next && next <= '9') {
                    inreplace(14);
                    append(5, true);
                    return true;
                } else if (next == '*') {
                    replace(6);
                    return true;
                } else if (next == '+') {
                    replace(7);
                    return true;
                }
                break;
            case 4:
                if (next == '0') {
                    append(2, false);
                    return true;
                } else if ('1' <= next && next <= '9') {
                    append(3, false);
                    return true;
                } else if (next == '(') {
                    append(4, false);
                    return true;
                }
                break;
            case 5:
                if ('0' <= next && next <= '9') {
                    finish();
                    return true;
                }
                break;
            case 6:
                if (next == '0') {
                    finish();
                    return true;
                } else if ('1' <= next && next <= '9') {
                    append(8, true);
                    return true;
                } else if (next == '(') {
                    append(4, false);
                    return true;
                }
                break;
            case 7:
                if (next == '0') {
                    append(9, true);
                    return true;
                } else if ('1' <= next && next <= '9') {
                    append(10, true);
                    return true;
                } else if (next == '(') {
                    append(4, false);
                    return true;
                }
                break;
            case 8:
                if ('0' <= next && next <= '9') {
                    append(5, true);
                    return true;
                }
                break;
            case 9:
                if (next == '*') {
                    replace(6);
                    return true;
                }
                break;
            case 10:
                if ('0' <= next && next <= '9') {
                    inreplace(14);
                    append(5, true);
                    return true;
                } else if (next == '*') {
                    replace(6);
                    return true;
                }
                break;
            case 11:
                if (next == '+') {
                    replace(7);
                    return true;
                }
                break;
            case 12:
                if (next == ')') {
                    finish();
                    return true;
                }
                break;
            case 13:
                if (next == '*') {
                    replace(6);
                    return true;
                } else if ('0' <= next && next <= '9') {
                    finish();
                    return true;
                }
                break;
            default:
                throw new RuntimeException("Should not happen! last=" + last);
        }
        return tryFinishable(next);
    }

    public boolean proceed(char next) {
        location += 1;
        return proceed1(next);
    }

    @Override
    public String toString() {
        return "ExpressionGrammarSimpleParser{" +
                "location=" + location +
                ", last=" + last +
                ", lastFinishable=" + lastFinishable +
                '}';
    }

    private String nodeString(Node node) {
        if (node.parent == null) return "" + node.nodeTypeId;
        else return nodeString(node.parent) + " " + node.nodeTypeId;
    }

    public String nodeString() {
        return nodeString(last);
    }

    public static void main(String[] args) {
        ExpressionGrammarSimpleParser parser = new ExpressionGrammarSimpleParser();
        String example = "12345+(1423*4321*(223+0))";

        for (int i = 0; i < example.length(); i++) {
            char c = example.charAt(i);
            System.out.println(parser.nodeString());
            System.out.println("Proceed " + c);
            if (!parser.proceed(c)) {
                System.out.println("Failed to proceed");
                return;
            }
        }
        System.out.println(parser.nodeString());
        boolean result = parser.finalizeParse();
        System.out.println("result=" + result);
        System.out.println(parser.nodeString());
        // parser.finish();
    }
}
