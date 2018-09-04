package com.giyeok.jparser.parsergen.generated;

public class GeneratedJsonParser {
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

    public GeneratedJsonParser() {
        last = new Node(1, null);
    }

    private boolean canHandle(int nodeTypeId, char c) {
        switch (nodeTypeId) {
            case 1:
                return ('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ') || (c == '"') || (c == '-') || ('0' <= c && c <= '9') || (c == '[') || (c == 'f') || (c == 'n') || (c == 't') || (c == '{');
            case 2:
                return ('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ') || (c == '"') || (c == '-') || ('0' <= c && c <= '9') || (c == '[') || (c == 'f') || (c == 'n') || (c == 't') || (c == '{');
            case 3:
                return (c == ' ') || (c == '"') || ('0' <= c && c <= '9') || ('A' <= c && c <= 'Z') || (c == '\\') || ('a' <= c && c <= 'z');
            case 4:
                return ('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ') || (c == '"') || (c == '-') || ('0' <= c && c <= '9') || (c == '[') || (c == ']') || (c == 'f') || (c == 'n') || (c == 't') || (c == '{');
            case 5:
                return ('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ') || (c == '.') || ('0' <= c && c <= '9') || (c == 'E') || (c == 'e');
            case 6:
                return (c == 'r');
            case 7:
                return ('0' <= c && c <= '9');
            case 8:
                return (c == 'u');
            case 9:
                return ('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ') || (c == '.') || (c == 'E') || (c == 'e');
            case 10:
                return ('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ') || (c == '"') || (c == '}');
            case 11:
                return (c == 'a');
            case 12:
                return (c == 'l');
            case 13:
                return (c == 'u');
            case 14:
                return (c == 'l');
            case 15:
                return ('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ') || (c == '"') || (c == '}');
            case 16:
                return ('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ') || (c == '"') || (c == '-') || ('0' <= c && c <= '9') || (c == '[') || (c == ']') || (c == 'f') || (c == 'n') || (c == 't') || (c == '{');
            case 17:
                return ('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ') || (c == '"');
            case 18:
                return ('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ') || (c == '.') || ('0' <= c && c <= '9') || (c == 'E') || (c == 'e');
            case 19:
                return ('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ') || (c == '"') || (c == '-') || ('0' <= c && c <= '9') || (c == '[') || (c == 'f') || (c == 'n') || (c == 't') || (c == '{');
            case 20:
                return (c == 'l');
            case 21:
                return (c == ']');
            case 22:
                return (c == '}');
            case 23:
                return (c == 's');
            case 24:
                return (c == 'e');
            case 25:
                return ('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ');
            case 26:
                return (c == 'e');
            case 27:
                return ('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ');
            case 28:
                return (c == '"') || (c == '-') || ('0' <= c && c <= '9') || (c == '[') || (c == 'f') || (c == 'n') || (c == 't') || (c == '{');
            case 29:
                return (c == '.') || ('0' <= c && c <= '9') || (c == 'E') || (c == 'e');
            case 30:
                return (c == '.') || (c == 'E') || (c == 'e');
            case 31:
                return ('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ') || (c == '"') || (c == '-') || ('0' <= c && c <= '9') || (c == '[') || (c == ']') || (c == 'f') || (c == 'n') || (c == 't') || (c == '{');
            case 32:
                return (c == '.') || ('0' <= c && c <= '9') || (c == 'E') || (c == 'e');
            case 33:
                return (c == '"');
            case 34:
                return ('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ') || (c == '"') || (c == '}');
            case 35:
                return (c == '}');
            case 37:
                return ('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ') || (c == '"');
            case 38:
                return ('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ') || (c == '"');
            case 39:
                return ('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ') || (c == '"');
            case 40:
                return ('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ') || (c == '}');
            case 41:
                return ('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ') || (c == '"') || (c == ':');
            case 42:
                return ('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ') || (c == '"') || (c == ':');
            case 43:
                return ('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ') || (c == '"') || (c == '-') || ('0' <= c && c <= ':') || (c == '[') || (c == 'f') || (c == 'n') || (c == 't') || (c == '{');
            case 44:
                return ('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ') || (c == '"') || (',' <= c && c <= '-') || ('0' <= c && c <= '9') || (c == '[') || (c == 'f') || (c == 'n') || (c == 't') || (c == '{');
            case 45:
                return ('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ') || (c == '"') || (c == ',');
            case 46:
                return ('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ') || (c == '"');
            case 47:
                return ('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ') || (c == '"');
            case 48:
                return (c == ',');
            case 49:
                return ('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ');
            case 50:
                return (c == '"') || (c == '-') || ('0' <= c && c <= '9') || (c == '[') || (c == 'f') || (c == 'n') || (c == 't') || (c == '{');
            case 51:
                return ('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ') || (c == ',');
            case 52:
                return ('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ');
            case 53:
                return (c == ':');
            case 54:
                return ('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ') || (c == '"') || (c == '-') || ('0' <= c && c <= '9') || (c == '[') || (c == 'f') || (c == 'n') || (c == 't') || (c == '{');
            case 55:
                return ('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ');
            case 56:
                return ('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ') || (c == ':');
            case 57:
                return (c == '"');
            case 58:
                return ('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ');
            case 59:
                return (c == '.');
            case 60:
                return ('0' <= c && c <= '9');
            case 61:
                return (c == 'E') || (c == 'e');
            case 62:
                return (c == '+') || (c == '-') || ('0' <= c && c <= '9');
            case 63:
                return ('0' <= c && c <= '9');
            case 64:
                return ('0' <= c && c <= '9');
            case 65:
                return (c == '+') || (c == '-') || ('0' <= c && c <= '9');
            case 66:
                return ('0' <= c && c <= '9');
            case 67:
                return ('0' <= c && c <= '9');
            case 68:
                return ('0' <= c && c <= '9');
            case 69:
                return (c == '+') || (c == '-');
            case 70:
                return (c == '+') || (c == '-');
            case 71:
                return ('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ');
            case 72:
                return (c == '"') || (c == '-') || ('0' <= c && c <= '9') || (c == '[') || (c == 'f') || (c == 'n') || (c == 't') || (c == '{');
            case 73:
                return ('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ') || (c == ',') || (c == '.') || ('0' <= c && c <= '9') || (c == 'E') || (c == 'e');
            case 74:
                return (c == ']');
            case 75:
                return ('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ') || (c == ',') || (c == '.') || (c == 'E') || (c == 'e');
            case 76:
                return ('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ') || (c == '"') || (c == ',');
            case 77:
                return ('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ') || (c == '"') || (',' <= c && c <= '-') || ('0' <= c && c <= '9') || (c == '[') || (c == 'f') || (c == 'n') || (c == 't') || (c == '{');
            case 78:
                return ('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ') || (c == ']');
            case 79:
                return ('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ') || (c == '"') || (c == ',') || (c == '}');
            case 80:
                return ('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ') || (c == ',') || (c == '.') || ('0' <= c && c <= '9') || (c == 'E') || (c == 'e');
            case 81:
                return ('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ') || (c == '"') || (',' <= c && c <= '-') || ('0' <= c && c <= '9') || (c == '[') || (c == ']') || (c == 'f') || (c == 'n') || (c == 't') || (c == '{');
            case 82:
                return ('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ') || (c == '"') || (',' <= c && c <= '-') || ('0' <= c && c <= '9') || (c == '[') || (c == 'f') || (c == 'n') || (c == 't') || (c == '{') || (c == '}');
            case 83:
                return (c == '"') || (c == '-') || ('0' <= c && c <= '9') || (c == '[') || (c == 'f') || (c == 'n') || (c == 't') || (c == '{');
            case 84:
                return ('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ') || (c == '"') || (',' <= c && c <= '-') || ('0' <= c && c <= '9') || (c == '[') || (c == ']') || (c == 'f') || (c == 'n') || (c == 't') || (c == '{');
            case 85:
                return ('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ') || (c == '"') || (',' <= c && c <= '-') || ('0' <= c && c <= '9') || (c == '[') || (c == 'f') || (c == 'n') || (c == 't') || (c == '{');
            case 86:
                return ('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ') || (c == ',');
            case 87:
                return (c == ',');
            case 88:
                return ('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ');
            case 89:
                return ('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ') || (c == '"') || (c == '-') || ('0' <= c && c <= '9') || (c == '[') || (c == 'f') || (c == 'n') || (c == 't') || (c == '{');
            case 90:
                return ('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ');
            case 91:
                return ('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ') || (c == '.') || ('0' <= c && c <= '9') || (c == 'E') || (c == 'e');
            case 92:
                return ('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ') || (c == '.') || (c == 'E') || (c == 'e');
            case 93:
                return ('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ') || (c == ',') || (c == 'E') || (c == 'e');
            case 94:
                return ('0' <= c && c <= '9');
            case 95:
                return ('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ');
            case 96:
                return (c == ',') || (c == ']');
            case 97:
                return ('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ') || (c == ',') || (c == ']');
            case 98:
                return ('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ') || (c == '"') || (',' <= c && c <= '-') || ('0' <= c && c <= '9') || (c == '[') || (c == 'f') || (c == 'n') || (c == 't') || (c == '{');
            case 99:
                return (c == '"') || (',' <= c && c <= '-') || ('0' <= c && c <= '9') || (c == '[') || (c == 'f') || (c == 'n') || (c == 't') || (c == '{');
            case 100:
                return ('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ');
            case 101:
                return ('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ') || (c == '"');
            case 102:
                return (c == ',') || (c == '}');
            case 103:
                return ('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ') || (c == ',') || (c == '}');
            case 104:
                return ('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ') || (c == 'E') || (c == 'e');
            case 105:
                return ('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ');
            case 106:
                return ('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ') || (c == ']');
            case 107:
                return ('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ') || (c == '"');
            case 108:
                return ('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ') || (c == '}');
            case 109:
                return ('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ') || (c == '"');
            case 110:
                return (c == '}');
            case 111:
                return ('0' <= c && c <= '9');
            case 112:
                return ('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ');
            case 113:
                return (c == ']');
            case 114:
                return (c == '"') || (c == '-') || ('0' <= c && c <= '9') || (c == '[') || (c == ']') || (c == 'f') || (c == 'n') || (c == 't') || (c == '{');
            case 115:
                return ('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ') || (c == ',') || (c == ']');
            case 116:
                return ('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ') || (c == ']');
            case 117:
                return (c == ' ') || ('0' <= c && c <= '9') || ('A' <= c && c <= 'Z') || (c == '\\') || ('a' <= c && c <= 'z');
            case 118:
                return (c == '"') || (c == '/') || (c == '\\') || (c == 'b') || (c == 'n') || (c == 'r') || ('t' <= c && c <= 'u');
            case 119:
                return (c == ' ') || ('0' <= c && c <= '9') || ('A' <= c && c <= 'Z') || (c == '\\') || ('a' <= c && c <= 'z');
            case 120:
                return ('0' <= c && c <= '9') || ('A' <= c && c <= 'F') || ('a' <= c && c <= 'f');
            case 121:
                return ('0' <= c && c <= '9') || ('A' <= c && c <= 'F') || ('a' <= c && c <= 'f');
            case 122:
                return ('0' <= c && c <= '9') || ('A' <= c && c <= 'F') || ('a' <= c && c <= 'f');
            case 123:
                return ('0' <= c && c <= '9') || ('A' <= c && c <= 'F') || ('a' <= c && c <= 'f');
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
            last = new Node(19, last.parent);
            pendingFinish = true;
        } else if (prevNodeType == 1 && lastNodeType == 3) {
            last = new Node(17, last.parent);
            pendingFinish = true;
        } else if (prevNodeType == 1 && lastNodeType == 4) {
            last = new Node(16, last.parent);
            pendingFinish = true;
        } else if (prevNodeType == 1 && lastNodeType == 5) {
            last = last.parent;
            finish();
        } else if (prevNodeType == 1 && lastNodeType == 6) {
            last = new Node(13, last.parent);
            pendingFinish = false;
        } else if (prevNodeType == 1 && lastNodeType == 7) {
            last = new Node(18, last.parent);
            pendingFinish = true;
        } else if (prevNodeType == 1 && lastNodeType == 8) {
            last = new Node(12, last.parent);
            pendingFinish = false;
        } else if (prevNodeType == 1 && lastNodeType == 9) {
            last = last.parent;
            finish();
        } else if (prevNodeType == 1 && lastNodeType == 10) {
            last = new Node(15, last.parent);
            pendingFinish = true;
        } else if (prevNodeType == 1 && lastNodeType == 11) {
            last = new Node(14, last.parent);
            pendingFinish = false;
        } else if (prevNodeType == 1 && lastNodeType == 12) {
            last = new Node(20, last.parent);
            pendingFinish = false;
        } else if (prevNodeType == 1 && lastNodeType == 13) {
            last = new Node(24, last.parent);
            pendingFinish = false;
        } else if (prevNodeType == 1 && lastNodeType == 14) {
            last = new Node(23, last.parent);
            pendingFinish = false;
        } else if (prevNodeType == 1 && lastNodeType == 15) {
            last = new Node(22, last.parent);
            pendingFinish = true;
        } else if (prevNodeType == 1 && lastNodeType == 16) {
            last = new Node(21, last.parent);
            pendingFinish = true;
        } else if (prevNodeType == 1 && lastNodeType == 17) {
            last = last.parent;
            finish();
        } else if (prevNodeType == 1 && lastNodeType == 18) {
            last = last.parent;
            finish();
        } else if (prevNodeType == 1 && lastNodeType == 19) {
            last = last.parent;
            finish();
        } else if (prevNodeType == 1 && lastNodeType == 20) {
            last = new Node(25, last.parent);
            pendingFinish = true;
        } else if (prevNodeType == 1 && lastNodeType == 21) {
            last = new Node(25, last.parent);
            pendingFinish = true;
        } else if (prevNodeType == 1 && lastNodeType == 22) {
            last = new Node(25, last.parent);
            pendingFinish = true;
        } else if (prevNodeType == 1 && lastNodeType == 23) {
            last = new Node(26, last.parent);
            pendingFinish = false;
        } else if (prevNodeType == 1 && lastNodeType == 24) {
            last = new Node(25, last.parent);
            pendingFinish = true;
        } else if (prevNodeType == 1 && lastNodeType == 25) {
            last = last.parent;
            finish();
        } else if (prevNodeType == 1 && lastNodeType == 26) {
            last = new Node(25, last.parent);
            pendingFinish = true;
        } else if (prevNodeType == 1 && lastNodeType == 27) {
            last = new Node(28, last.parent);
            pendingFinish = false;
        } else if (prevNodeType == 1 && lastNodeType == 28) {
            last = new Node(25, last.parent);
            pendingFinish = true;
        } else if (prevNodeType == 1 && lastNodeType == 33) {
            last = new Node(25, last.parent);
            pendingFinish = true;
        } else if (prevNodeType == 1 && lastNodeType == 34) {
            last = new Node(108, last.parent);
            pendingFinish = true;
        } else if (prevNodeType == 1 && lastNodeType == 35) {
            last = new Node(25, last.parent);
            pendingFinish = true;
        } else if (prevNodeType == 1 && lastNodeType == 37) {
            last = new Node(40, last.parent);
            pendingFinish = false;
        } else if (prevNodeType == 1 && lastNodeType == 38) {
            last = new Node(22, last.parent);
            pendingFinish = false;
        } else if (prevNodeType == 1 && lastNodeType == 40) {
            last = new Node(108, last.parent);
            pendingFinish = true;
        } else if (prevNodeType == 1 && lastNodeType == 58) {
            last = new Node(22, last.parent);
            pendingFinish = false;
        } else if (prevNodeType == 1 && lastNodeType == 59) {
            last = new Node(104, last.parent);
            pendingFinish = true;
        } else if (prevNodeType == 1 && lastNodeType == 61) {
            last = new Node(25, last.parent);
            pendingFinish = true;
        } else if (prevNodeType == 1 && lastNodeType == 63) {
            last = new Node(9, last.parent);
            pendingFinish = true;
        } else if (prevNodeType == 1 && lastNodeType == 71) {
            last = new Node(21, last.parent);
            pendingFinish = false;
        } else if (prevNodeType == 1 && lastNodeType == 72) {
            last = new Node(78, last.parent);
            pendingFinish = false;
        } else if (prevNodeType == 1 && lastNodeType == 74) {
            last = new Node(25, last.parent);
            pendingFinish = true;
        } else if (prevNodeType == 1 && lastNodeType == 78) {
            last = new Node(106, last.parent);
            pendingFinish = true;
        } else if (prevNodeType == 1 && lastNodeType == 94) {
            last = new Node(9, last.parent);
            pendingFinish = true;
        } else if (prevNodeType == 1 && lastNodeType == 104) {
            last = last.parent;
            finish();
        } else if (prevNodeType == 1 && lastNodeType == 105) {
            last = new Node(21, last.parent);
            pendingFinish = true;
        } else if (prevNodeType == 1 && lastNodeType == 106) {
            last = last.parent;
            finish();
        } else if (prevNodeType == 1 && lastNodeType == 107) {
            last = new Node(22, last.parent);
            pendingFinish = true;
        } else if (prevNodeType == 1 && lastNodeType == 108) {
            last = last.parent;
            finish();
        } else if (prevNodeType == 1 && lastNodeType == 109) {
            last = new Node(34, last.parent);
            pendingFinish = false;
        } else if (prevNodeType == 1 && lastNodeType == 110) {
            last = new Node(25, last.parent);
            pendingFinish = true;
        } else if (prevNodeType == 1 && lastNodeType == 111) {
            last = new Node(9, last.parent);
            pendingFinish = true;
        } else if (prevNodeType == 1 && lastNodeType == 112) {
            last = new Node(114, last.parent);
            pendingFinish = false;
        } else if (prevNodeType == 1 && lastNodeType == 113) {
            last = new Node(25, last.parent);
            pendingFinish = true;
        } else if (prevNodeType == 1 && lastNodeType == 114) {
            last = new Node(116, last.parent);
            pendingFinish = true;
        } else if (prevNodeType == 1 && lastNodeType == 116) {
            last = new Node(21, last.parent);
            pendingFinish = true;
        } else if (prevNodeType == 1 && lastNodeType == 117) {
            last = new Node(33, last.parent);
            pendingFinish = false;
        } else if (prevNodeType == 21 && lastNodeType == 36) {
            last = last.parent;
            finish();
        } else if (prevNodeType == 22 && lastNodeType == 36) {
            last = last.parent;
            finish();
        } else if (prevNodeType == 25 && lastNodeType == 27) {
            last = last.parent;
            finish();
        } else if (prevNodeType == 27 && lastNodeType == 27) {
            last = last.parent;
            finish();
        } else if (prevNodeType == 28 && lastNodeType == 3) {
            last = new Node(33, last.parent);
            pendingFinish = true;
        } else if (prevNodeType == 28 && lastNodeType == 4) {
            last = new Node(31, last.parent);
            pendingFinish = true;
        } else if (prevNodeType == 28 && lastNodeType == 6) {
            last = new Node(13, last.parent);
            pendingFinish = false;
        } else if (prevNodeType == 28 && lastNodeType == 7) {
            last = new Node(32, last.parent);
            pendingFinish = true;
        } else if (prevNodeType == 28 && lastNodeType == 8) {
            last = new Node(12, last.parent);
            pendingFinish = false;
        } else if (prevNodeType == 28 && lastNodeType == 10) {
            last = new Node(34, last.parent);
            pendingFinish = true;
        } else if (prevNodeType == 28 && lastNodeType == 11) {
            last = new Node(14, last.parent);
            pendingFinish = false;
        } else if (prevNodeType == 28 && lastNodeType == 12) {
            last = new Node(20, last.parent);
            pendingFinish = false;
        } else if (prevNodeType == 28 && lastNodeType == 13) {
            last = new Node(24, last.parent);
            pendingFinish = false;
        } else if (prevNodeType == 28 && lastNodeType == 14) {
            last = new Node(23, last.parent);
            pendingFinish = false;
        } else if (prevNodeType == 28 && lastNodeType == 20) {
            last = last.parent;
            finish();
        } else if (prevNodeType == 28 && lastNodeType == 21) {
            last = last.parent;
            finish();
        } else if (prevNodeType == 28 && lastNodeType == 22) {
            last = last.parent;
            finish();
        } else if (prevNodeType == 28 && lastNodeType == 23) {
            last = new Node(26, last.parent);
            pendingFinish = false;
        } else if (prevNodeType == 28 && lastNodeType == 24) {
            last = last.parent;
            finish();
        } else if (prevNodeType == 28 && lastNodeType == 26) {
            last = last.parent;
            finish();
        } else if (prevNodeType == 28 && lastNodeType == 29) {
            last = last.parent;
            finish();
        } else if (prevNodeType == 28 && lastNodeType == 30) {
            last = last.parent;
            finish();
        } else if (prevNodeType == 28 && lastNodeType == 31) {
            last = new Node(21, last.parent);
            pendingFinish = true;
        } else if (prevNodeType == 28 && lastNodeType == 32) {
            last = last.parent;
            finish();
        } else if (prevNodeType == 28 && lastNodeType == 33) {
            last = last.parent;
            finish();
        } else if (prevNodeType == 28 && lastNodeType == 34) {
            last = new Node(22, last.parent);
            pendingFinish = true;
        } else if (prevNodeType == 28 && lastNodeType == 35) {
            last = last.parent;
            finish();
        } else if (prevNodeType == 28 && lastNodeType == 37) {
            last = new Node(40, last.parent);
            pendingFinish = false;
        } else if (prevNodeType == 28 && lastNodeType == 38) {
            last = new Node(22, last.parent);
            pendingFinish = false;
        } else if (prevNodeType == 28 && lastNodeType == 40) {
            last = new Node(22, last.parent);
            pendingFinish = true;
        } else if (prevNodeType == 28 && lastNodeType == 58) {
            last = new Node(22, last.parent);
            pendingFinish = false;
        } else if (prevNodeType == 28 && lastNodeType == 59) {
            last = new Node(61, last.parent);
            pendingFinish = true;
        } else if (prevNodeType == 28 && lastNodeType == 61) {
            last = last.parent;
            finish();
        } else if (prevNodeType == 28 && lastNodeType == 63) {
            last = new Node(30, last.parent);
            pendingFinish = true;
        } else if (prevNodeType == 28 && lastNodeType == 71) {
            last = new Node(21, last.parent);
            pendingFinish = false;
        } else if (prevNodeType == 28 && lastNodeType == 72) {
            last = new Node(78, last.parent);
            pendingFinish = false;
        } else if (prevNodeType == 28 && lastNodeType == 74) {
            last = last.parent;
            finish();
        } else if (prevNodeType == 28 && lastNodeType == 78) {
            last = new Node(21, last.parent);
            pendingFinish = true;
        } else if (prevNodeType == 28 && lastNodeType == 94) {
            last = new Node(30, last.parent);
            pendingFinish = true;
        } else if (prevNodeType == 28 && lastNodeType == 109) {
            last = new Node(34, last.parent);
            pendingFinish = false;
        } else if (prevNodeType == 28 && lastNodeType == 110) {
            last = last.parent;
            finish();
        } else if (prevNodeType == 28 && lastNodeType == 111) {
            last = new Node(30, last.parent);
            pendingFinish = true;
        } else if (prevNodeType == 28 && lastNodeType == 112) {
            last = new Node(114, last.parent);
            pendingFinish = false;
        } else if (prevNodeType == 28 && lastNodeType == 113) {
            last = last.parent;
            finish();
        } else if (prevNodeType == 28 && lastNodeType == 114) {
            last = new Node(78, last.parent);
            pendingFinish = true;
        } else if (prevNodeType == 28 && lastNodeType == 117) {
            last = new Node(33, last.parent);
            pendingFinish = false;
        } else if (prevNodeType == 33 && lastNodeType == 36) {
            last = last.parent;
            finish();
        } else if (prevNodeType == 35 && lastNodeType == 36) {
            last = last.parent;
            finish();
        } else if (prevNodeType == 37 && lastNodeType == 3) {
            last = new Node(42, last.parent);
            pendingFinish = false;
        } else if (prevNodeType == 37 && lastNodeType == 27) {
            last = new Node(57, last.parent);
            pendingFinish = false;
        } else if (prevNodeType == 37 && lastNodeType == 33) {
            last = new Node(56, last.parent);
            pendingFinish = false;
        } else if (prevNodeType == 37 && lastNodeType == 39) {
            last = new Node(41, last.parent);
            pendingFinish = false;
        } else if (prevNodeType == 37 && lastNodeType == 41) {
            last = new Node(43, last.parent);
            pendingFinish = false;
        } else if (prevNodeType == 37 && lastNodeType == 42) {
            last = new Node(43, last.parent);
            pendingFinish = false;
        } else if (prevNodeType == 37 && lastNodeType == 43) {
            last = new Node(44, last.parent);
            pendingFinish = true;
        } else if (prevNodeType == 37 && lastNodeType == 44) {
            last = new Node(45, last.parent);
            pendingFinish = true;
        } else if (prevNodeType == 37 && lastNodeType == 45) {
            last = new Node(46, last.parent);
            pendingFinish = true;
        } else if (prevNodeType == 37 && lastNodeType == 46) {
            last = last.parent;
            finish();
        } else if (prevNodeType == 37 && lastNodeType == 47) {
            last = new Node(46, last.parent);
            pendingFinish = true;
        } else if (prevNodeType == 37 && lastNodeType == 48) {
            last = new Node(47, last.parent);
            pendingFinish = false;
        } else if (prevNodeType == 37 && lastNodeType == 49) {
            last = new Node(48, last.parent);
            pendingFinish = false;
        } else if (prevNodeType == 37 && lastNodeType == 50) {
            last = new Node(51, last.parent);
            pendingFinish = true;
        } else if (prevNodeType == 37 && lastNodeType == 51) {
            last = new Node(45, last.parent);
            pendingFinish = false;
        } else if (prevNodeType == 37 && lastNodeType == 52) {
            last = new Node(50, last.parent);
            pendingFinish = false;
        } else if (prevNodeType == 37 && lastNodeType == 53) {
            last = new Node(54, last.parent);
            pendingFinish = false;
        } else if (prevNodeType == 37 && lastNodeType == 54) {
            last = new Node(44, last.parent);
            pendingFinish = true;
        } else if (prevNodeType == 37 && lastNodeType == 55) {
            last = new Node(53, last.parent);
            pendingFinish = false;
        } else if (prevNodeType == 37 && lastNodeType == 56) {
            last = new Node(43, last.parent);
            pendingFinish = false;
        } else if (prevNodeType == 37 && lastNodeType == 57) {
            last = new Node(56, last.parent);
            pendingFinish = false;
        } else if (prevNodeType == 37 && lastNodeType == 117) {
            last = new Node(33, last.parent);
            pendingFinish = false;
        } else if (prevNodeType == 38 && lastNodeType == 27) {
            last = new Node(57, last.parent);
            pendingFinish = true;
        } else if (prevNodeType == 38 && lastNodeType == 39) {
            last = new Node(41, new Node(37, last.parent.parent));
            pendingFinish = false;
        } else if (prevNodeType == 38 && lastNodeType == 57) {
            last = new Node(56, new Node(37, last.parent.parent));
            pendingFinish = false;
        } else if (prevNodeType == 46 && lastNodeType == 3) {
            last = new Node(42, last.parent);
            pendingFinish = false;
        } else if (prevNodeType == 46 && lastNodeType == 27) {
            last = new Node(57, last.parent);
            pendingFinish = false;
        } else if (prevNodeType == 46 && lastNodeType == 33) {
            last = new Node(56, last.parent);
            pendingFinish = false;
        } else if (prevNodeType == 46 && lastNodeType == 39) {
            last = new Node(41, last.parent);
            pendingFinish = false;
        } else if (prevNodeType == 46 && lastNodeType == 41) {
            last = new Node(43, last.parent);
            pendingFinish = false;
        } else if (prevNodeType == 46 && lastNodeType == 42) {
            last = new Node(43, last.parent);
            pendingFinish = false;
        } else if (prevNodeType == 46 && lastNodeType == 43) {
            last = new Node(44, last.parent);
            pendingFinish = true;
        } else if (prevNodeType == 46 && lastNodeType == 44) {
            last = new Node(45, last.parent);
            pendingFinish = false;
        } else if (prevNodeType == 46 && lastNodeType == 45) {
            last = new Node(46, last.parent);
            pendingFinish = false;
        } else if (prevNodeType == 46 && lastNodeType == 46) {
            last = last.parent;
            finish();
        } else if (prevNodeType == 46 && lastNodeType == 47) {
            last = new Node(46, last.parent);
            pendingFinish = false;
        } else if (prevNodeType == 46 && lastNodeType == 48) {
            last = new Node(47, last.parent);
            pendingFinish = false;
        } else if (prevNodeType == 46 && lastNodeType == 49) {
            last = new Node(48, last.parent);
            pendingFinish = false;
        } else if (prevNodeType == 46 && lastNodeType == 50) {
            last = new Node(51, last.parent);
            pendingFinish = true;
        } else if (prevNodeType == 46 && lastNodeType == 51) {
            last = new Node(45, last.parent);
            pendingFinish = false;
        } else if (prevNodeType == 46 && lastNodeType == 52) {
            last = new Node(50, last.parent);
            pendingFinish = false;
        } else if (prevNodeType == 46 && lastNodeType == 53) {
            last = new Node(54, last.parent);
            pendingFinish = false;
        } else if (prevNodeType == 46 && lastNodeType == 54) {
            last = new Node(44, last.parent);
            pendingFinish = true;
        } else if (prevNodeType == 46 && lastNodeType == 55) {
            last = new Node(53, last.parent);
            pendingFinish = false;
        } else if (prevNodeType == 46 && lastNodeType == 56) {
            last = new Node(43, last.parent);
            pendingFinish = false;
        } else if (prevNodeType == 46 && lastNodeType == 57) {
            last = new Node(56, last.parent);
            pendingFinish = false;
        } else if (prevNodeType == 46 && lastNodeType == 117) {
            last = new Node(33, last.parent);
            pendingFinish = false;
        } else if (prevNodeType == 47 && lastNodeType == 27) {
            last = new Node(57, last.parent);
            pendingFinish = true;
        } else if (prevNodeType == 47 && lastNodeType == 39) {
            last = new Node(41, new Node(46, last.parent.parent));
            pendingFinish = false;
        } else if (prevNodeType == 47 && lastNodeType == 57) {
            last = new Node(56, new Node(46, last.parent.parent));
            pendingFinish = false;
        } else if (prevNodeType == 48 && lastNodeType == 36) {
            last = last.parent;
            finish();
        } else if (prevNodeType == 49 && lastNodeType == 27) {
            last = last.parent;
            finish();
        } else if (prevNodeType == 50 && lastNodeType == 3) {
            last = new Node(33, last.parent);
            pendingFinish = true;
        } else if (prevNodeType == 50 && lastNodeType == 4) {
            last = new Node(31, last.parent);
            pendingFinish = true;
        } else if (prevNodeType == 50 && lastNodeType == 6) {
            last = new Node(13, last.parent);
            pendingFinish = false;
        } else if (prevNodeType == 50 && lastNodeType == 7) {
            last = new Node(32, last.parent);
            pendingFinish = true;
        } else if (prevNodeType == 50 && lastNodeType == 8) {
            last = new Node(12, last.parent);
            pendingFinish = false;
        } else if (prevNodeType == 50 && lastNodeType == 10) {
            last = new Node(34, last.parent);
            pendingFinish = true;
        } else if (prevNodeType == 50 && lastNodeType == 11) {
            last = new Node(14, last.parent);
            pendingFinish = false;
        } else if (prevNodeType == 50 && lastNodeType == 12) {
            last = new Node(20, last.parent);
            pendingFinish = false;
        } else if (prevNodeType == 50 && lastNodeType == 13) {
            last = new Node(24, last.parent);
            pendingFinish = false;
        } else if (prevNodeType == 50 && lastNodeType == 14) {
            last = new Node(23, last.parent);
            pendingFinish = false;
        } else if (prevNodeType == 50 && lastNodeType == 20) {
            last = last.parent;
            finish();
        } else if (prevNodeType == 50 && lastNodeType == 21) {
            last = last.parent;
            finish();
        } else if (prevNodeType == 50 && lastNodeType == 22) {
            last = last.parent;
            finish();
        } else if (prevNodeType == 50 && lastNodeType == 23) {
            last = new Node(26, last.parent);
            pendingFinish = false;
        } else if (prevNodeType == 50 && lastNodeType == 24) {
            last = last.parent;
            finish();
        } else if (prevNodeType == 50 && lastNodeType == 26) {
            last = last.parent;
            finish();
        } else if (prevNodeType == 50 && lastNodeType == 29) {
            last = last.parent;
            finish();
        } else if (prevNodeType == 50 && lastNodeType == 30) {
            last = last.parent;
            finish();
        } else if (prevNodeType == 50 && lastNodeType == 31) {
            last = new Node(21, last.parent);
            pendingFinish = true;
        } else if (prevNodeType == 50 && lastNodeType == 32) {
            last = last.parent;
            finish();
        } else if (prevNodeType == 50 && lastNodeType == 33) {
            last = last.parent;
            finish();
        } else if (prevNodeType == 50 && lastNodeType == 34) {
            last = new Node(22, last.parent);
            pendingFinish = true;
        } else if (prevNodeType == 50 && lastNodeType == 35) {
            last = last.parent;
            finish();
        } else if (prevNodeType == 50 && lastNodeType == 37) {
            last = new Node(40, last.parent);
            pendingFinish = false;
        } else if (prevNodeType == 50 && lastNodeType == 38) {
            last = new Node(22, last.parent);
            pendingFinish = false;
        } else if (prevNodeType == 50 && lastNodeType == 40) {
            last = new Node(22, last.parent);
            pendingFinish = true;
        } else if (prevNodeType == 50 && lastNodeType == 58) {
            last = new Node(22, last.parent);
            pendingFinish = false;
        } else if (prevNodeType == 50 && lastNodeType == 59) {
            last = new Node(61, last.parent);
            pendingFinish = true;
        } else if (prevNodeType == 50 && lastNodeType == 61) {
            last = last.parent;
            finish();
        } else if (prevNodeType == 50 && lastNodeType == 63) {
            last = new Node(30, last.parent);
            pendingFinish = true;
        } else if (prevNodeType == 50 && lastNodeType == 71) {
            last = new Node(21, last.parent);
            pendingFinish = false;
        } else if (prevNodeType == 50 && lastNodeType == 72) {
            last = new Node(78, last.parent);
            pendingFinish = false;
        } else if (prevNodeType == 50 && lastNodeType == 74) {
            last = last.parent;
            finish();
        } else if (prevNodeType == 50 && lastNodeType == 78) {
            last = new Node(21, last.parent);
            pendingFinish = true;
        } else if (prevNodeType == 50 && lastNodeType == 94) {
            last = new Node(30, last.parent);
            pendingFinish = true;
        } else if (prevNodeType == 50 && lastNodeType == 109) {
            last = new Node(34, last.parent);
            pendingFinish = false;
        } else if (prevNodeType == 50 && lastNodeType == 110) {
            last = last.parent;
            finish();
        } else if (prevNodeType == 50 && lastNodeType == 111) {
            last = new Node(30, last.parent);
            pendingFinish = true;
        } else if (prevNodeType == 50 && lastNodeType == 112) {
            last = new Node(114, last.parent);
            pendingFinish = false;
        } else if (prevNodeType == 50 && lastNodeType == 113) {
            last = last.parent;
            finish();
        } else if (prevNodeType == 50 && lastNodeType == 114) {
            last = new Node(78, last.parent);
            pendingFinish = true;
        } else if (prevNodeType == 50 && lastNodeType == 117) {
            last = new Node(33, last.parent);
            pendingFinish = false;
        } else if (prevNodeType == 52 && lastNodeType == 27) {
            last = last.parent;
            finish();
        } else if (prevNodeType == 53 && lastNodeType == 36) {
            last = last.parent;
            finish();
        } else if (prevNodeType == 55 && lastNodeType == 27) {
            last = last.parent;
            finish();
        } else if (prevNodeType == 57 && lastNodeType == 3) {
            last = new Node(33, last.parent);
            pendingFinish = true;
        } else if (prevNodeType == 57 && lastNodeType == 33) {
            last = last.parent;
            finish();
        } else if (prevNodeType == 57 && lastNodeType == 117) {
            last = new Node(33, last.parent);
            pendingFinish = false;
        } else if (prevNodeType == 58 && lastNodeType == 27) {
            last = last.parent;
            finish();
        } else if (prevNodeType == 59 && lastNodeType == 60) {
            last = last.parent;
            finish();
        } else if (prevNodeType == 60 && lastNodeType == 68) {
            last = new Node(68, last.parent);
            pendingFinish = true;
        } else if (prevNodeType == 61 && lastNodeType == 62) {
            last = new Node(66, last.parent);
            pendingFinish = true;
        } else if (prevNodeType == 61 && lastNodeType == 65) {
            last = new Node(67, last.parent);
            pendingFinish = true;
        } else if (prevNodeType == 61 && lastNodeType == 66) {
            last = last.parent;
            finish();
        } else if (prevNodeType == 61 && lastNodeType == 67) {
            last = last.parent;
            finish();
        } else if (prevNodeType == 61 && lastNodeType == 69) {
            last = new Node(67, last.parent);
            pendingFinish = false;
        } else if (prevNodeType == 61 && lastNodeType == 70) {
            last = new Node(66, last.parent);
            pendingFinish = false;
        } else if (prevNodeType == 63 && lastNodeType == 64) {
            last = last.parent;
            finish();
        } else if (prevNodeType == 64 && lastNodeType == 64) {
            last = last.parent;
            finish();
        } else if (prevNodeType == 66 && lastNodeType == 68) {
            last = new Node(68, last.parent);
            pendingFinish = true;
        } else if (prevNodeType == 67 && lastNodeType == 68) {
            last = new Node(68, last.parent);
            pendingFinish = true;
        } else if (prevNodeType == 69 && lastNodeType == 36) {
            last = last.parent;
            finish();
        } else if (prevNodeType == 70 && lastNodeType == 36) {
            last = last.parent;
            finish();
        } else if (prevNodeType == 71 && lastNodeType == 27) {
            last = last.parent;
            finish();
        } else if (prevNodeType == 72 && lastNodeType == 3) {
            last = new Node(76, last.parent);
            pendingFinish = true;
        } else if (prevNodeType == 72 && lastNodeType == 4) {
            last = new Node(81, last.parent);
            pendingFinish = false;
        } else if (prevNodeType == 72 && lastNodeType == 6) {
            last = new Node(13, last.parent);
            pendingFinish = false;
        } else if (prevNodeType == 72 && lastNodeType == 7) {
            last = new Node(80, last.parent);
            pendingFinish = true;
        } else if (prevNodeType == 72 && lastNodeType == 8) {
            last = new Node(12, last.parent);
            pendingFinish = false;
        } else if (prevNodeType == 72 && lastNodeType == 10) {
            last = new Node(79, last.parent);
            pendingFinish = true;
        } else if (prevNodeType == 72 && lastNodeType == 11) {
            last = new Node(14, last.parent);
            pendingFinish = false;
        } else if (prevNodeType == 72 && lastNodeType == 12) {
            last = new Node(20, last.parent);
            pendingFinish = false;
        } else if (prevNodeType == 72 && lastNodeType == 13) {
            last = new Node(24, last.parent);
            pendingFinish = false;
        } else if (prevNodeType == 72 && lastNodeType == 14) {
            last = new Node(23, last.parent);
            pendingFinish = false;
        } else if (prevNodeType == 72 && lastNodeType == 20) {
            last = new Node(86, last.parent);
            pendingFinish = true;
        } else if (prevNodeType == 72 && lastNodeType == 21) {
            last = new Node(86, last.parent);
            pendingFinish = true;
        } else if (prevNodeType == 72 && lastNodeType == 22) {
            last = new Node(86, last.parent);
            pendingFinish = true;
        } else if (prevNodeType == 72 && lastNodeType == 23) {
            last = new Node(26, last.parent);
            pendingFinish = false;
        } else if (prevNodeType == 72 && lastNodeType == 24) {
            last = new Node(86, last.parent);
            pendingFinish = true;
        } else if (prevNodeType == 72 && lastNodeType == 26) {
            last = new Node(86, last.parent);
            pendingFinish = true;
        } else if (prevNodeType == 72 && lastNodeType == 33) {
            last = new Node(86, last.parent);
            pendingFinish = true;
        } else if (prevNodeType == 72 && lastNodeType == 34) {
            last = new Node(103, last.parent);
            pendingFinish = true;
        } else if (prevNodeType == 72 && lastNodeType == 35) {
            last = new Node(86, last.parent);
            pendingFinish = true;
        } else if (prevNodeType == 72 && lastNodeType == 37) {
            last = new Node(40, last.parent);
            pendingFinish = false;
        } else if (prevNodeType == 72 && lastNodeType == 38) {
            last = new Node(22, last.parent);
            pendingFinish = false;
        } else if (prevNodeType == 72 && lastNodeType == 40) {
            last = new Node(103, last.parent);
            pendingFinish = true;
        } else if (prevNodeType == 72 && lastNodeType == 58) {
            last = new Node(22, last.parent);
            pendingFinish = false;
        } else if (prevNodeType == 72 && lastNodeType == 59) {
            last = new Node(93, last.parent);
            pendingFinish = true;
        } else if (prevNodeType == 72 && lastNodeType == 61) {
            last = new Node(86, last.parent);
            pendingFinish = true;
        } else if (prevNodeType == 72 && lastNodeType == 63) {
            last = new Node(75, last.parent);
            pendingFinish = true;
        } else if (prevNodeType == 72 && lastNodeType == 71) {
            last = new Node(21, last.parent);
            pendingFinish = false;
        } else if (prevNodeType == 72 && lastNodeType == 72) {
            last = new Node(78, last.parent);
            pendingFinish = false;
        } else if (prevNodeType == 72 && lastNodeType == 73) {
            last = new Node(77, last.parent);
            pendingFinish = true;
        } else if (prevNodeType == 72 && lastNodeType == 74) {
            last = new Node(86, last.parent);
            pendingFinish = true;
        } else if (prevNodeType == 72 && lastNodeType == 75) {
            last = new Node(77, last.parent);
            pendingFinish = true;
        } else if (prevNodeType == 72 && lastNodeType == 76) {
            last = new Node(77, last.parent);
            pendingFinish = true;
        } else if (prevNodeType == 72 && lastNodeType == 77) {
            last = new Node(83, last.parent);
            pendingFinish = true;
        } else if (prevNodeType == 72 && lastNodeType == 78) {
            last = new Node(97, last.parent);
            pendingFinish = true;
        } else if (prevNodeType == 72 && lastNodeType == 79) {
            last = new Node(82, last.parent);
            pendingFinish = true;
        } else if (prevNodeType == 72 && lastNodeType == 80) {
            last = new Node(77, last.parent);
            pendingFinish = true;
        } else if (prevNodeType == 72 && lastNodeType == 81) {
            last = new Node(84, last.parent);
            pendingFinish = false;
        } else if (prevNodeType == 72 && lastNodeType == 82) {
            last = new Node(85, last.parent);
            pendingFinish = true;
        } else if (prevNodeType == 72 && lastNodeType == 83) {
            last = last.parent;
            finish();
        } else if (prevNodeType == 72 && lastNodeType == 84) {
            last = new Node(85, last.parent);
            pendingFinish = true;
        } else if (prevNodeType == 72 && lastNodeType == 85) {
            last = new Node(77, last.parent);
            pendingFinish = true;
        } else if (prevNodeType == 72 && lastNodeType == 86) {
            last = new Node(77, last.parent);
            pendingFinish = false;
        } else if (prevNodeType == 72 && lastNodeType == 87) {
            last = new Node(89, last.parent);
            pendingFinish = false;
        } else if (prevNodeType == 72 && lastNodeType == 88) {
            last = new Node(87, last.parent);
            pendingFinish = false;
        } else if (prevNodeType == 72 && lastNodeType == 89) {
            last = new Node(83, last.parent);
            pendingFinish = true;
        } else if (prevNodeType == 72 && lastNodeType == 90) {
            last = new Node(83, last.parent);
            pendingFinish = false;
        } else if (prevNodeType == 72 && lastNodeType == 91) {
            last = new Node(87, last.parent);
            pendingFinish = true;
        } else if (prevNodeType == 72 && lastNodeType == 92) {
            last = new Node(87, last.parent);
            pendingFinish = true;
        } else if (prevNodeType == 72 && lastNodeType == 93) {
            last = new Node(77, last.parent);
            pendingFinish = true;
        } else if (prevNodeType == 72 && lastNodeType == 94) {
            last = new Node(75, last.parent);
            pendingFinish = true;
        } else if (prevNodeType == 72 && lastNodeType == 95) {
            last = new Node(96, last.parent);
            pendingFinish = false;
        } else if (prevNodeType == 72 && lastNodeType == 96) {
            last = new Node(98, last.parent);
            pendingFinish = true;
        } else if (prevNodeType == 72 && lastNodeType == 97) {
            last = new Node(77, last.parent);
            pendingFinish = true;
        } else if (prevNodeType == 72 && lastNodeType == 98) {
            last = new Node(99, last.parent);
            pendingFinish = true;
        } else if (prevNodeType == 72 && lastNodeType == 99) {
            last = new Node(89, last.parent);
            pendingFinish = true;
        } else if (prevNodeType == 72 && lastNodeType == 100) {
            last = new Node(99, last.parent);
            pendingFinish = false;
        } else if (prevNodeType == 72 && lastNodeType == 101) {
            last = new Node(102, last.parent);
            pendingFinish = false;
        } else if (prevNodeType == 72 && lastNodeType == 102) {
            last = new Node(98, last.parent);
            pendingFinish = true;
        } else if (prevNodeType == 72 && lastNodeType == 103) {
            last = new Node(77, last.parent);
            pendingFinish = true;
        } else if (prevNodeType == 72 && lastNodeType == 109) {
            last = new Node(34, last.parent);
            pendingFinish = false;
        } else if (prevNodeType == 72 && lastNodeType == 110) {
            last = new Node(86, last.parent);
            pendingFinish = true;
        } else if (prevNodeType == 72 && lastNodeType == 111) {
            last = new Node(75, last.parent);
            pendingFinish = true;
        } else if (prevNodeType == 72 && lastNodeType == 112) {
            last = new Node(114, last.parent);
            pendingFinish = false;
        } else if (prevNodeType == 72 && lastNodeType == 113) {
            last = new Node(86, last.parent);
            pendingFinish = true;
        } else if (prevNodeType == 72 && lastNodeType == 114) {
            last = new Node(115, last.parent);
            pendingFinish = false;
        } else if (prevNodeType == 72 && lastNodeType == 115) {
            last = new Node(84, last.parent);
            pendingFinish = true;
        } else if (prevNodeType == 72 && lastNodeType == 117) {
            last = new Node(33, last.parent);
            pendingFinish = false;
        } else if (prevNodeType == 74 && lastNodeType == 36) {
            last = last.parent;
            finish();
        } else if (prevNodeType == 83 && lastNodeType == 3) {
            last = new Node(76, last.parent);
            pendingFinish = true;
        } else if (prevNodeType == 83 && lastNodeType == 4) {
            last = new Node(81, last.parent);
            pendingFinish = true;
        } else if (prevNodeType == 83 && lastNodeType == 6) {
            last = new Node(13, last.parent);
            pendingFinish = false;
        } else if (prevNodeType == 83 && lastNodeType == 7) {
            last = new Node(80, last.parent);
            pendingFinish = true;
        } else if (prevNodeType == 83 && lastNodeType == 8) {
            last = new Node(12, last.parent);
            pendingFinish = false;
        } else if (prevNodeType == 83 && lastNodeType == 10) {
            last = new Node(79, last.parent);
            pendingFinish = true;
        } else if (prevNodeType == 83 && lastNodeType == 11) {
            last = new Node(14, last.parent);
            pendingFinish = false;
        } else if (prevNodeType == 83 && lastNodeType == 12) {
            last = new Node(20, last.parent);
            pendingFinish = false;
        } else if (prevNodeType == 83 && lastNodeType == 13) {
            last = new Node(24, last.parent);
            pendingFinish = false;
        } else if (prevNodeType == 83 && lastNodeType == 14) {
            last = new Node(23, last.parent);
            pendingFinish = false;
        } else if (prevNodeType == 83 && lastNodeType == 20) {
            last = new Node(86, last.parent);
            pendingFinish = true;
        } else if (prevNodeType == 83 && lastNodeType == 21) {
            last = new Node(86, last.parent);
            pendingFinish = true;
        } else if (prevNodeType == 83 && lastNodeType == 22) {
            last = new Node(86, last.parent);
            pendingFinish = true;
        } else if (prevNodeType == 83 && lastNodeType == 23) {
            last = new Node(26, last.parent);
            pendingFinish = false;
        } else if (prevNodeType == 83 && lastNodeType == 24) {
            last = new Node(86, last.parent);
            pendingFinish = true;
        } else if (prevNodeType == 83 && lastNodeType == 26) {
            last = new Node(86, last.parent);
            pendingFinish = true;
        } else if (prevNodeType == 83 && lastNodeType == 33) {
            last = new Node(86, last.parent);
            pendingFinish = true;
        } else if (prevNodeType == 83 && lastNodeType == 34) {
            last = new Node(103, last.parent);
            pendingFinish = true;
        } else if (prevNodeType == 83 && lastNodeType == 35) {
            last = new Node(86, last.parent);
            pendingFinish = true;
        } else if (prevNodeType == 83 && lastNodeType == 37) {
            last = new Node(40, last.parent);
            pendingFinish = false;
        } else if (prevNodeType == 83 && lastNodeType == 38) {
            last = new Node(22, last.parent);
            pendingFinish = false;
        } else if (prevNodeType == 83 && lastNodeType == 40) {
            last = new Node(103, last.parent);
            pendingFinish = true;
        } else if (prevNodeType == 83 && lastNodeType == 58) {
            last = new Node(22, last.parent);
            pendingFinish = false;
        } else if (prevNodeType == 83 && lastNodeType == 59) {
            last = new Node(93, last.parent);
            pendingFinish = true;
        } else if (prevNodeType == 83 && lastNodeType == 61) {
            last = new Node(86, last.parent);
            pendingFinish = true;
        } else if (prevNodeType == 83 && lastNodeType == 63) {
            last = new Node(75, last.parent);
            pendingFinish = true;
        } else if (prevNodeType == 83 && lastNodeType == 71) {
            last = new Node(21, last.parent);
            pendingFinish = false;
        } else if (prevNodeType == 83 && lastNodeType == 72) {
            last = new Node(78, last.parent);
            pendingFinish = false;
        } else if (prevNodeType == 83 && lastNodeType == 73) {
            last = new Node(77, last.parent);
            pendingFinish = false;
        } else if (prevNodeType == 83 && lastNodeType == 74) {
            last = new Node(86, last.parent);
            pendingFinish = true;
        } else if (prevNodeType == 83 && lastNodeType == 75) {
            last = new Node(77, last.parent);
            pendingFinish = false;
        } else if (prevNodeType == 83 && lastNodeType == 76) {
            last = new Node(77, last.parent);
            pendingFinish = false;
        } else if (prevNodeType == 83 && lastNodeType == 77) {
            last = new Node(83, last.parent);
            pendingFinish = false;
        } else if (prevNodeType == 83 && lastNodeType == 78) {
            last = new Node(97, last.parent);
            pendingFinish = true;
        } else if (prevNodeType == 83 && lastNodeType == 79) {
            last = new Node(82, last.parent);
            pendingFinish = false;
        } else if (prevNodeType == 83 && lastNodeType == 80) {
            last = new Node(77, last.parent);
            pendingFinish = false;
        } else if (prevNodeType == 83 && lastNodeType == 81) {
            last = new Node(84, last.parent);
            pendingFinish = false;
        } else if (prevNodeType == 83 && lastNodeType == 82) {
            last = new Node(85, last.parent);
            pendingFinish = false;
        } else if (prevNodeType == 83 && lastNodeType == 83) {
            last = last.parent;
            finish();
        } else if (prevNodeType == 83 && lastNodeType == 84) {
            last = new Node(85, last.parent);
            pendingFinish = false;
        } else if (prevNodeType == 83 && lastNodeType == 85) {
            last = new Node(77, last.parent);
            pendingFinish = false;
        } else if (prevNodeType == 83 && lastNodeType == 86) {
            last = new Node(77, last.parent);
            pendingFinish = false;
        } else if (prevNodeType == 83 && lastNodeType == 87) {
            last = new Node(89, last.parent);
            pendingFinish = false;
        } else if (prevNodeType == 83 && lastNodeType == 88) {
            last = new Node(87, last.parent);
            pendingFinish = false;
        } else if (prevNodeType == 83 && lastNodeType == 89) {
            last = new Node(83, last.parent);
            pendingFinish = false;
        } else if (prevNodeType == 83 && lastNodeType == 90) {
            last = new Node(83, last.parent);
            pendingFinish = false;
        } else if (prevNodeType == 83 && lastNodeType == 91) {
            last = new Node(87, last.parent);
            pendingFinish = true;
        } else if (prevNodeType == 83 && lastNodeType == 92) {
            last = new Node(87, last.parent);
            pendingFinish = true;
        } else if (prevNodeType == 83 && lastNodeType == 93) {
            last = new Node(77, last.parent);
            pendingFinish = false;
        } else if (prevNodeType == 83 && lastNodeType == 94) {
            last = new Node(75, last.parent);
            pendingFinish = true;
        } else if (prevNodeType == 83 && lastNodeType == 95) {
            last = new Node(96, last.parent);
            pendingFinish = false;
        } else if (prevNodeType == 83 && lastNodeType == 96) {
            last = new Node(98, last.parent);
            pendingFinish = false;
        } else if (prevNodeType == 83 && lastNodeType == 97) {
            last = new Node(77, last.parent);
            pendingFinish = false;
        } else if (prevNodeType == 83 && lastNodeType == 98) {
            last = new Node(99, last.parent);
            pendingFinish = false;
        } else if (prevNodeType == 83 && lastNodeType == 99) {
            last = new Node(89, last.parent);
            pendingFinish = false;
        } else if (prevNodeType == 83 && lastNodeType == 100) {
            last = new Node(99, last.parent);
            pendingFinish = false;
        } else if (prevNodeType == 83 && lastNodeType == 101) {
            last = new Node(102, last.parent);
            pendingFinish = false;
        } else if (prevNodeType == 83 && lastNodeType == 102) {
            last = new Node(98, last.parent);
            pendingFinish = false;
        } else if (prevNodeType == 83 && lastNodeType == 103) {
            last = new Node(77, last.parent);
            pendingFinish = false;
        } else if (prevNodeType == 83 && lastNodeType == 109) {
            last = new Node(34, last.parent);
            pendingFinish = false;
        } else if (prevNodeType == 83 && lastNodeType == 110) {
            last = new Node(86, last.parent);
            pendingFinish = true;
        } else if (prevNodeType == 83 && lastNodeType == 111) {
            last = new Node(75, last.parent);
            pendingFinish = true;
        } else if (prevNodeType == 83 && lastNodeType == 112) {
            last = new Node(114, last.parent);
            pendingFinish = false;
        } else if (prevNodeType == 83 && lastNodeType == 113) {
            last = new Node(86, last.parent);
            pendingFinish = true;
        } else if (prevNodeType == 83 && lastNodeType == 114) {
            last = new Node(115, last.parent);
            pendingFinish = true;
        } else if (prevNodeType == 83 && lastNodeType == 115) {
            last = new Node(84, last.parent);
            pendingFinish = false;
        } else if (prevNodeType == 83 && lastNodeType == 117) {
            last = new Node(33, last.parent);
            pendingFinish = false;
        } else if (prevNodeType == 87 && lastNodeType == 36) {
            last = last.parent;
            finish();
        } else if (prevNodeType == 88 && lastNodeType == 27) {
            last = last.parent;
            finish();
        } else if (prevNodeType == 90 && lastNodeType == 27) {
            last = last.parent;
            finish();
        } else if (prevNodeType == 94 && lastNodeType == 64) {
            last = last.parent;
            finish();
        } else if (prevNodeType == 95 && lastNodeType == 27) {
            last = last.parent;
            finish();
        } else if (prevNodeType == 100 && lastNodeType == 27) {
            last = last.parent;
            finish();
        } else if (prevNodeType == 101 && lastNodeType == 27) {
            last = new Node(57, last.parent);
            pendingFinish = true;
        } else if (prevNodeType == 101 && lastNodeType == 39) {
            last = new Node(41, new Node(37, last.parent.parent));
            pendingFinish = false;
        } else if (prevNodeType == 101 && lastNodeType == 57) {
            last = new Node(56, new Node(37, last.parent.parent));
            pendingFinish = false;
        } else if (prevNodeType == 105 && lastNodeType == 27) {
            last = last.parent;
            finish();
        } else if (prevNodeType == 107 && lastNodeType == 27) {
            last = new Node(57, last.parent);
            pendingFinish = true;
        } else if (prevNodeType == 107 && lastNodeType == 39) {
            last = new Node(41, new Node(37, last.parent.parent));
            pendingFinish = false;
        } else if (prevNodeType == 107 && lastNodeType == 57) {
            last = new Node(56, new Node(37, last.parent.parent));
            pendingFinish = false;
        } else if (prevNodeType == 109 && lastNodeType == 27) {
            last = new Node(57, last.parent);
            pendingFinish = true;
        } else if (prevNodeType == 109 && lastNodeType == 39) {
            last = new Node(41, new Node(37, last.parent.parent));
            pendingFinish = false;
        } else if (prevNodeType == 109 && lastNodeType == 57) {
            last = new Node(56, new Node(37, last.parent.parent));
            pendingFinish = false;
        } else if (prevNodeType == 110 && lastNodeType == 36) {
            last = last.parent;
            finish();
        } else if (prevNodeType == 111 && lastNodeType == 36) {
            last = last.parent;
            finish();
        } else if (prevNodeType == 112 && lastNodeType == 27) {
            last = last.parent;
            finish();
        } else if (prevNodeType == 113 && lastNodeType == 36) {
            last = last.parent;
            finish();
        } else if (prevNodeType == 117 && lastNodeType == 118) {
            last = new Node(119, last.parent);
            pendingFinish = true;
        } else if (prevNodeType == 117 && lastNodeType == 119) {
            last = last.parent;
            finish();
        } else if (prevNodeType == 118 && lastNodeType == 120) {
            last = new Node(121, last.parent);
            pendingFinish = false;
        } else if (prevNodeType == 118 && lastNodeType == 121) {
            last = new Node(122, last.parent);
            pendingFinish = false;
        } else if (prevNodeType == 118 && lastNodeType == 122) {
            last = new Node(123, last.parent);
            pendingFinish = false;
        } else if (prevNodeType == 118 && lastNodeType == 123) {
            last = last.parent;
            finish();
        } else if (prevNodeType == 119 && lastNodeType == 118) {
            last = new Node(119, last.parent);
            pendingFinish = true;
        } else if (prevNodeType == 119 && lastNodeType == 119) {
            last = last.parent;
            finish();
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
                if (('\t' <= next && next <= '\n') || (next == '\r') || (next == ' ')) {
                    append(2, false);
                    return true;
                } else if ((next == '"')) {
                    append(3, false);
                    return true;
                } else if ((next == '-')) {
                    append(7, false);
                    return true;
                } else if ((next == '0')) {
                    append(9, true);
                    return true;
                } else if (('1' <= next && next <= '9')) {
                    append(5, true);
                    return true;
                } else if ((next == '[')) {
                    append(4, false);
                    return true;
                } else if ((next == 'f')) {
                    append(11, false);
                    return true;
                } else if ((next == 'n')) {
                    append(8, false);
                    return true;
                } else if ((next == 't')) {
                    append(6, false);
                    return true;
                } else if ((next == '{')) {
                    append(10, false);
                    return true;
                }
                break;
            case 2: // {\t-\n\r\u0020}*ws|ws*element ws
                if (('\t' <= next && next <= '\n') || (next == '\r') || (next == ' ')) {
                    replace(27);
                    append(27, true);
                    return true;
                } else if ((next == '"')) {
                    replace(28);
                    append(3, false);
                    return true;
                } else if ((next == '-')) {
                    replace(28);
                    append(7, false);
                    return true;
                } else if ((next == '0')) {
                    replace(28);
                    append(30, true);
                    return true;
                } else if (('1' <= next && next <= '9')) {
                    replace(28);
                    append(29, true);
                    return true;
                } else if ((next == '[')) {
                    replace(28);
                    append(4, false);
                    return true;
                } else if ((next == 'f')) {
                    replace(28);
                    append(11, false);
                    return true;
                } else if ((next == 'n')) {
                    replace(28);
                    append(8, false);
                    return true;
                } else if ((next == 't')) {
                    replace(28);
                    append(6, false);
                    return true;
                } else if ((next == '{')) {
                    replace(28);
                    append(10, false);
                    return true;
                }
                break;
            case 3: // "*characters "|" characters*"
                if ((next == ' ') || ('0' <= next && next <= '9') || ('A' <= next && next <= 'Z') || ('a' <= next && next <= 'z')) {
                    replace(117);
                    append(119, true);
                    return true;
                } else if ((next == '"')) {
                    replace(33);
                    append(36, true);
                    return true;
                } else if ((next == '\\')) {
                    replace(117);
                    append(118, false);
                    return true;
                }
                break;
            case 4: // [*ws elements ws ]|[ ws*elements ws ]|[*ws ]|[ ws*]
                if (('\t' <= next && next <= '\n') || (next == '\r') || (next == ' ')) {
                    replace(112);
                    append(27, true);
                    return true;
                } else if ((next == '"')) {
                    replace(72);
                    append(3, false);
                    return true;
                } else if ((next == '-')) {
                    replace(72);
                    append(7, false);
                    return true;
                } else if ((next == '0')) {
                    replace(72);
                    append(75, true);
                    return true;
                } else if (('1' <= next && next <= '9')) {
                    replace(72);
                    append(73, true);
                    return true;
                } else if ((next == '[')) {
                    replace(72);
                    append(112, true);
                    return true;
                } else if ((next == ']')) {
                    replace(113);
                    append(36, true);
                    return true;
                } else if ((next == 'f')) {
                    replace(72);
                    append(11, false);
                    return true;
                } else if ((next == 'n')) {
                    replace(72);
                    append(8, false);
                    return true;
                } else if ((next == 't')) {
                    replace(72);
                    append(6, false);
                    return true;
                } else if ((next == '{')) {
                    replace(72);
                    append(10, false);
                    return true;
                }
                break;
            case 5: // onenine*digits|int*frac exp|int frac*exp|ws element*ws
                if (('\t' <= next && next <= '\n') || (next == '\r') || (next == ' ')) {
                    replace(25);
                    append(27, true);
                    return true;
                } else if ((next == '.')) {
                    replace(59);
                    append(60, false);
                    return true;
                } else if ((next == '0')) {
                    replace(94);
                    append(64, true);
                    return true;
                } else if (('1' <= next && next <= '9')) {
                    replace(94);
                    append(64, true);
                    return true;
                } else if ((next == 'E')) {
                    replace(61);
                    append(65, false);
                    return true;
                } else if ((next == 'e')) {
                    replace(61);
                    append(62, false);
                    return true;
                }
                break;
            case 6: // t*r u e
                if ((next == 'r')) {
                    finish();
                    return true;
                }
                break;
            case 7: // -*digit|-*onenine digits
                if ((next == '0')) {
                    replace(111);
                    append(36, true);
                    return true;
                } else if (('1' <= next && next <= '9')) {
                    finish();
                    return true;
                }
                break;
            case 8: // n*u l l
                if ((next == 'u')) {
                    finish();
                    return true;
                }
                break;
            case 9: // int*frac exp|int frac*exp|ws element*ws
                if (('\t' <= next && next <= '\n') || (next == '\r') || (next == ' ')) {
                    replace(25);
                    append(27, true);
                    return true;
                } else if ((next == '.')) {
                    replace(59);
                    append(60, false);
                    return true;
                } else if ((next == 'E')) {
                    replace(61);
                    append(65, false);
                    return true;
                } else if ((next == 'e')) {
                    replace(61);
                    append(62, false);
                    return true;
                }
                break;
            case 10: // {*ws }|{ ws*}|{*ws members ws }|{ ws*members ws }
                if (('\t' <= next && next <= '\n') || (next == '\r') || (next == ' ')) {
                    replace(109);
                    append(39, true);
                    return true;
                } else if ((next == '"')) {
                    replace(37);
                    append(3, false);
                    return true;
                } else if ((next == '}')) {
                    replace(110);
                    append(36, true);
                    return true;
                }
                break;
            case 11: // f*a l s e
                if ((next == 'a')) {
                    finish();
                    return true;
                }
                break;
            case 12: // n u*l l
                if ((next == 'l')) {
                    finish();
                    return true;
                }
                break;
            case 13: // t r*u e
                if ((next == 'u')) {
                    finish();
                    return true;
                }
                break;
            case 14: // f a*l s e
                if ((next == 'l')) {
                    finish();
                    return true;
                }
                break;
            case 15: // { ws*}|{ ws*members ws }|{ ws members ws*}|{ ws members*ws }|ws element*ws
                if (('\t' <= next && next <= '\n') || (next == '\r') || (next == ' ')) {
                    replace(107);
                    append(39, true);
                    return true;
                } else if ((next == '"')) {
                    replace(37);
                    append(3, false);
                    return true;
                } else if ((next == '}')) {
                    replace(35);
                    append(36, true);
                    return true;
                }
                break;
            case 16: // [ ws*]|[ ws elements ws*]|ws element*ws|[ ws*elements ws ]|[ ws elements*ws ]
                if (('\t' <= next && next <= '\n') || (next == '\r') || (next == ' ')) {
                    replace(105);
                    append(27, true);
                    return true;
                } else if ((next == '"')) {
                    replace(72);
                    append(3, false);
                    return true;
                } else if ((next == '-')) {
                    replace(72);
                    append(7, false);
                    return true;
                } else if ((next == '0')) {
                    replace(72);
                    append(75, true);
                    return true;
                } else if (('1' <= next && next <= '9')) {
                    replace(72);
                    append(73, true);
                    return true;
                } else if ((next == '[')) {
                    replace(72);
                    append(4, false);
                    return true;
                } else if ((next == ']')) {
                    replace(74);
                    append(36, true);
                    return true;
                } else if ((next == 'f')) {
                    replace(72);
                    append(11, false);
                    return true;
                } else if ((next == 'n')) {
                    replace(72);
                    append(8, false);
                    return true;
                } else if ((next == 't')) {
                    replace(72);
                    append(6, false);
                    return true;
                } else if ((next == '{')) {
                    replace(72);
                    append(10, false);
                    return true;
                }
                break;
            case 17: // " characters*"|ws element*ws
                if (('\t' <= next && next <= '\n') || (next == '\r') || (next == ' ')) {
                    replace(25);
                    append(27, true);
                    return true;
                } else if ((next == '"')) {
                    replace(33);
                    append(36, true);
                    return true;
                }
                break;
            case 18: // - onenine*digits|int*frac exp|int frac*exp|ws element*ws
                if (('\t' <= next && next <= '\n') || (next == '\r') || (next == ' ')) {
                    replace(25);
                    append(27, true);
                    return true;
                } else if ((next == '.')) {
                    replace(59);
                    append(60, false);
                    return true;
                } else if ((next == '0')) {
                    replace(63);
                    append(64, true);
                    return true;
                } else if (('1' <= next && next <= '9')) {
                    replace(63);
                    append(64, true);
                    return true;
                } else if ((next == 'E')) {
                    replace(61);
                    append(65, false);
                    return true;
                } else if ((next == 'e')) {
                    replace(61);
                    append(62, false);
                    return true;
                }
                break;
            case 19: // ws element*ws|ws*element ws
                if (('\t' <= next && next <= '\n') || (next == '\r') || (next == ' ')) {
                    replace(25);
                    append(27, true);
                    return true;
                } else if ((next == '"')) {
                    replace(28);
                    append(3, false);
                    return true;
                } else if ((next == '-')) {
                    replace(28);
                    append(7, false);
                    return true;
                } else if ((next == '0')) {
                    replace(28);
                    append(30, true);
                    return true;
                } else if (('1' <= next && next <= '9')) {
                    replace(28);
                    append(29, true);
                    return true;
                } else if ((next == '[')) {
                    replace(28);
                    append(4, false);
                    return true;
                } else if ((next == 'f')) {
                    replace(28);
                    append(11, false);
                    return true;
                } else if ((next == 'n')) {
                    replace(28);
                    append(8, false);
                    return true;
                } else if ((next == 't')) {
                    replace(28);
                    append(6, false);
                    return true;
                } else if ((next == '{')) {
                    replace(28);
                    append(10, false);
                    return true;
                }
                break;
            case 20: // n u l*l
                if ((next == 'l')) {
                    finish();
                    return true;
                }
                break;
            case 21: // [ ws elements ws*]
                if ((next == ']')) {
                    finish();
                    return true;
                }
                break;
            case 22: // { ws members ws*}
                if ((next == '}')) {
                    finish();
                    return true;
                }
                break;
            case 23: // f a l*s e
                if ((next == 's')) {
                    finish();
                    return true;
                }
                break;
            case 24: // t r u*e
                if ((next == 'e')) {
                    finish();
                    return true;
                }
                break;
            case 25: // ws element*ws
                if (('\t' <= next && next <= '\n') || (next == '\r') || (next == ' ')) {
                    append(27, true);
                    return true;
                }
                break;
            case 26: // f a l s*e
                if ((next == 'e')) {
                    finish();
                    return true;
                }
                break;
            case 27: // {\t-\n\r\u0020}*ws
                if (('\t' <= next && next <= '\n') || (next == '\r') || (next == ' ')) {
                    append(27, true);
                    return true;
                }
                break;
            case 28: // ws*element ws
                if ((next == '"')) {
                    append(3, false);
                    return true;
                } else if ((next == '-')) {
                    append(7, false);
                    return true;
                } else if ((next == '0')) {
                    append(30, true);
                    return true;
                } else if (('1' <= next && next <= '9')) {
                    append(29, true);
                    return true;
                } else if ((next == '[')) {
                    append(4, false);
                    return true;
                } else if ((next == 'f')) {
                    append(11, false);
                    return true;
                } else if ((next == 'n')) {
                    append(8, false);
                    return true;
                } else if ((next == 't')) {
                    append(6, false);
                    return true;
                } else if ((next == '{')) {
                    append(10, false);
                    return true;
                }
                break;
            case 29: // onenine*digits|int*frac exp|int frac*exp
                if ((next == '.')) {
                    replace(59);
                    append(60, false);
                    return true;
                } else if ((next == '0')) {
                    replace(94);
                    append(64, true);
                    return true;
                } else if (('1' <= next && next <= '9')) {
                    replace(94);
                    append(64, true);
                    return true;
                } else if ((next == 'E')) {
                    replace(61);
                    append(65, false);
                    return true;
                } else if ((next == 'e')) {
                    replace(61);
                    append(62, false);
                    return true;
                }
                break;
            case 30: // int*frac exp|int frac*exp
                if ((next == '.')) {
                    replace(59);
                    append(60, false);
                    return true;
                } else if ((next == 'E')) {
                    replace(61);
                    append(65, false);
                    return true;
                } else if ((next == 'e')) {
                    replace(61);
                    append(62, false);
                    return true;
                }
                break;
            case 31: // [ ws*elements ws ]|[ ws elements*ws ]|[ ws elements ws*]|[ ws*]
                if (('\t' <= next && next <= '\n') || (next == '\r') || (next == ' ')) {
                    replace(71);
                    append(27, true);
                    return true;
                } else if ((next == '"')) {
                    replace(72);
                    append(3, false);
                    return true;
                } else if ((next == '-')) {
                    replace(72);
                    append(7, false);
                    return true;
                } else if ((next == '0')) {
                    replace(72);
                    append(75, true);
                    return true;
                } else if (('1' <= next && next <= '9')) {
                    replace(72);
                    append(73, true);
                    return true;
                } else if ((next == '[')) {
                    replace(72);
                    append(4, false);
                    return true;
                } else if ((next == ']')) {
                    replace(74);
                    append(36, true);
                    return true;
                } else if ((next == 'f')) {
                    replace(72);
                    append(11, false);
                    return true;
                } else if ((next == 'n')) {
                    replace(72);
                    append(8, false);
                    return true;
                } else if ((next == 't')) {
                    replace(72);
                    append(6, false);
                    return true;
                } else if ((next == '{')) {
                    replace(72);
                    append(10, false);
                    return true;
                }
                break;
            case 32: // - onenine*digits|int*frac exp|int frac*exp
                if ((next == '.')) {
                    replace(59);
                    append(60, false);
                    return true;
                } else if ((next == '0')) {
                    replace(63);
                    append(64, true);
                    return true;
                } else if (('1' <= next && next <= '9')) {
                    replace(63);
                    append(64, true);
                    return true;
                } else if ((next == 'E')) {
                    replace(61);
                    append(65, false);
                    return true;
                } else if ((next == 'e')) {
                    replace(61);
                    append(62, false);
                    return true;
                }
                break;
            case 33: // " characters*"
                if ((next == '"')) {
                    finish();
                    return true;
                }
                break;
            case 34: // { ws*}|{ ws*members ws }|{ ws members*ws }|{ ws members ws*}
                if (('\t' <= next && next <= '\n') || (next == '\r') || (next == ' ')) {
                    replace(38);
                    append(39, true);
                    return true;
                } else if ((next == '"')) {
                    replace(37);
                    append(3, false);
                    return true;
                } else if ((next == '}')) {
                    replace(35);
                    append(36, true);
                    return true;
                }
                break;
            case 35: // { ws*}|{ ws members ws*}
                if ((next == '}')) {
                    finish();
                    return true;
                }
                break;
            case 37: // { ws*members ws }
                if (('\t' <= next && next <= '\n') || (next == '\r') || (next == ' ')) {
                    append(39, false);
                    return true;
                } else if ((next == '"')) {
                    append(3, false);
                    return true;
                }
                break;
            case 38: // { ws*members ws }|{ ws members*ws }
                if (('\t' <= next && next <= '\n') || (next == '\r') || (next == ' ')) {
                    append(39, true);
                    return true;
                } else if ((next == '"')) {
                    replace(37);
                    append(3, false);
                    return true;
                }
                break;
            case 39: // {\t-\n\r\u0020}*ws|ws*string ws : ws element
                if (('\t' <= next && next <= '\n') || (next == '\r') || (next == ' ')) {
                    replace(27);
                    append(27, true);
                    return true;
                } else if ((next == '"')) {
                    replace(57);
                    append(3, false);
                    return true;
                }
                break;
            case 40: // { ws members*ws }|{ ws members ws*}
                if (('\t' <= next && next <= '\n') || (next == '\r') || (next == ' ')) {
                    replace(58);
                    append(27, true);
                    return true;
                } else if ((next == '}')) {
                    replace(22);
                    append(36, true);
                    return true;
                }
                break;
            case 41: // ws string*ws : ws element|ws string ws*: ws element|ws*string ws : ws element
                if (('\t' <= next && next <= '\n') || (next == '\r') || (next == ' ')) {
                    replace(55);
                    append(27, true);
                    return true;
                } else if ((next == '"')) {
                    replace(57);
                    append(3, false);
                    return true;
                } else if ((next == ':')) {
                    replace(53);
                    append(36, true);
                    return true;
                }
                break;
            case 42: // " characters*"|ws string*ws : ws element|ws string ws*: ws element
                if (('\t' <= next && next <= '\n') || (next == '\r') || (next == ' ')) {
                    replace(55);
                    append(27, true);
                    return true;
                } else if ((next == '"')) {
                    replace(33);
                    append(36, true);
                    return true;
                } else if ((next == ':')) {
                    replace(53);
                    append(36, true);
                    return true;
                }
                break;
            case 43: // ws string ws*: ws element|ws string ws :*ws element|ws string ws : ws*element
                if (('\t' <= next && next <= '\n') || (next == '\r') || (next == ' ')) {
                    replace(52);
                    append(27, true);
                    return true;
                } else if ((next == '"')) {
                    replace(50);
                    append(3, false);
                    return true;
                } else if ((next == '-')) {
                    replace(50);
                    append(7, false);
                    return true;
                } else if ((next == '0')) {
                    replace(50);
                    append(30, true);
                    return true;
                } else if (('1' <= next && next <= '9')) {
                    replace(50);
                    append(29, true);
                    return true;
                } else if ((next == ':')) {
                    replace(53);
                    append(36, true);
                    return true;
                } else if ((next == '[')) {
                    replace(50);
                    append(4, false);
                    return true;
                } else if ((next == 'f')) {
                    replace(50);
                    append(11, false);
                    return true;
                } else if ((next == 'n')) {
                    replace(50);
                    append(8, false);
                    return true;
                } else if ((next == 't')) {
                    replace(50);
                    append(6, false);
                    return true;
                } else if ((next == '{')) {
                    replace(50);
                    append(10, false);
                    return true;
                }
                break;
            case 44: // ws string ws : ws*element|member*ws , ws members|member ws*, ws members
                if (('\t' <= next && next <= '\n') || (next == '\r') || (next == ' ')) {
                    replace(49);
                    append(27, true);
                    return true;
                } else if ((next == '"')) {
                    replace(50);
                    append(3, false);
                    return true;
                } else if ((next == ',')) {
                    replace(48);
                    append(36, true);
                    return true;
                } else if ((next == '-')) {
                    replace(50);
                    append(7, false);
                    return true;
                } else if ((next == '0')) {
                    replace(50);
                    append(30, true);
                    return true;
                } else if (('1' <= next && next <= '9')) {
                    replace(50);
                    append(29, true);
                    return true;
                } else if ((next == '[')) {
                    replace(50);
                    append(4, false);
                    return true;
                } else if ((next == 'f')) {
                    replace(50);
                    append(11, false);
                    return true;
                } else if ((next == 'n')) {
                    replace(50);
                    append(8, false);
                    return true;
                } else if ((next == 't')) {
                    replace(50);
                    append(6, false);
                    return true;
                } else if ((next == '{')) {
                    replace(50);
                    append(10, false);
                    return true;
                }
                break;
            case 45: // member ws*, ws members|member ws ,*ws members|member ws , ws*members
                if (('\t' <= next && next <= '\n') || (next == '\r') || (next == ' ')) {
                    replace(47);
                    append(39, true);
                    return true;
                } else if ((next == '"')) {
                    replace(46);
                    append(3, false);
                    return true;
                } else if ((next == ',')) {
                    replace(48);
                    append(36, true);
                    return true;
                }
                break;
            case 46: // member ws , ws*members
                if (('\t' <= next && next <= '\n') || (next == '\r') || (next == ' ')) {
                    append(39, false);
                    return true;
                } else if ((next == '"')) {
                    append(3, false);
                    return true;
                }
                break;
            case 47: // member ws ,*ws members|member ws , ws*members
                if (('\t' <= next && next <= '\n') || (next == '\r') || (next == ' ')) {
                    append(39, true);
                    return true;
                } else if ((next == '"')) {
                    replace(46);
                    append(3, false);
                    return true;
                }
                break;
            case 48: // member ws*, ws members
                if ((next == ',')) {
                    finish();
                    return true;
                }
                break;
            case 49: // member*ws , ws members
                if (('\t' <= next && next <= '\n') || (next == '\r') || (next == ' ')) {
                    append(27, true);
                    return true;
                }
                break;
            case 50: // ws string ws : ws*element
                if ((next == '"')) {
                    append(3, false);
                    return true;
                } else if ((next == '-')) {
                    append(7, false);
                    return true;
                } else if ((next == '0')) {
                    append(30, true);
                    return true;
                } else if (('1' <= next && next <= '9')) {
                    append(29, true);
                    return true;
                } else if ((next == '[')) {
                    append(4, false);
                    return true;
                } else if ((next == 'f')) {
                    append(11, false);
                    return true;
                } else if ((next == 'n')) {
                    append(8, false);
                    return true;
                } else if ((next == 't')) {
                    append(6, false);
                    return true;
                } else if ((next == '{')) {
                    append(10, false);
                    return true;
                }
                break;
            case 51: // member*ws , ws members|member ws*, ws members
                if (('\t' <= next && next <= '\n') || (next == '\r') || (next == ' ')) {
                    replace(49);
                    append(27, true);
                    return true;
                } else if ((next == ',')) {
                    replace(48);
                    append(36, true);
                    return true;
                }
                break;
            case 52: // ws string ws :*ws element
                if (('\t' <= next && next <= '\n') || (next == '\r') || (next == ' ')) {
                    append(27, true);
                    return true;
                }
                break;
            case 53: // ws string ws*: ws element
                if ((next == ':')) {
                    finish();
                    return true;
                }
                break;
            case 54: // ws string ws :*ws element|ws string ws : ws*element
                if (('\t' <= next && next <= '\n') || (next == '\r') || (next == ' ')) {
                    replace(52);
                    append(27, true);
                    return true;
                } else if ((next == '"')) {
                    replace(50);
                    append(3, false);
                    return true;
                } else if ((next == '-')) {
                    replace(50);
                    append(7, false);
                    return true;
                } else if ((next == '0')) {
                    replace(50);
                    append(30, true);
                    return true;
                } else if (('1' <= next && next <= '9')) {
                    replace(50);
                    append(29, true);
                    return true;
                } else if ((next == '[')) {
                    replace(50);
                    append(4, false);
                    return true;
                } else if ((next == 'f')) {
                    replace(50);
                    append(11, false);
                    return true;
                } else if ((next == 'n')) {
                    replace(50);
                    append(8, false);
                    return true;
                } else if ((next == 't')) {
                    replace(50);
                    append(6, false);
                    return true;
                } else if ((next == '{')) {
                    replace(50);
                    append(10, false);
                    return true;
                }
                break;
            case 55: // ws string*ws : ws element
                if (('\t' <= next && next <= '\n') || (next == '\r') || (next == ' ')) {
                    append(27, true);
                    return true;
                }
                break;
            case 56: // ws string*ws : ws element|ws string ws*: ws element
                if (('\t' <= next && next <= '\n') || (next == '\r') || (next == ' ')) {
                    replace(55);
                    append(27, true);
                    return true;
                } else if ((next == ':')) {
                    replace(53);
                    append(36, true);
                    return true;
                }
                break;
            case 57: // ws*string ws : ws element
                if ((next == '"')) {
                    append(3, false);
                    return true;
                }
                break;
            case 58: // { ws members*ws }
                if (('\t' <= next && next <= '\n') || (next == '\r') || (next == ' ')) {
                    append(27, true);
                    return true;
                }
                break;
            case 59: // int*frac exp
                if ((next == '.')) {
                    append(60, false);
                    return true;
                }
                break;
            case 60: // .*{0-9}[1-]
                if (('0' <= next && next <= '9')) {
                    append(68, true);
                    return true;
                }
                break;
            case 61: // int frac*exp
                if ((next == 'E')) {
                    append(65, false);
                    return true;
                } else if ((next == 'e')) {
                    append(62, false);
                    return true;
                }
                break;
            case 62: // e*sign {0-9}[1-]|e sign*{0-9}[1-]
                if ((next == '+')) {
                    replace(70);
                    append(36, true);
                    return true;
                } else if ((next == '-')) {
                    replace(70);
                    append(36, true);
                    return true;
                } else if (('0' <= next && next <= '9')) {
                    replace(66);
                    append(68, true);
                    return true;
                }
                break;
            case 63: // - onenine*digits
                if ((next == '0')) {
                    append(64, true);
                    return true;
                } else if (('1' <= next && next <= '9')) {
                    append(64, true);
                    return true;
                }
                break;
            case 64: // digit*digits
                if ((next == '0')) {
                    append(64, true);
                    return true;
                } else if (('1' <= next && next <= '9')) {
                    append(64, true);
                    return true;
                }
                break;
            case 65: // E*sign {0-9}[1-]|E sign*{0-9}[1-]
                if ((next == '+')) {
                    replace(69);
                    append(36, true);
                    return true;
                } else if ((next == '-')) {
                    replace(69);
                    append(36, true);
                    return true;
                } else if (('0' <= next && next <= '9')) {
                    replace(67);
                    append(68, true);
                    return true;
                }
                break;
            case 66: // e sign*{0-9}[1-]
                if (('0' <= next && next <= '9')) {
                    append(68, true);
                    return true;
                }
                break;
            case 67: // E sign*{0-9}[1-]
                if (('0' <= next && next <= '9')) {
                    append(68, true);
                    return true;
                }
                break;
            case 68: // {0-9}[1-]*{0-9}
                if (('0' <= next && next <= '9')) {
                    finish();
                    return true;
                }
                break;
            case 69: // E*sign {0-9}[1-]
                if ((next == '+')) {
                    finish();
                    return true;
                } else if ((next == '-')) {
                    finish();
                    return true;
                }
                break;
            case 70: // e*sign {0-9}[1-]
                if ((next == '+')) {
                    finish();
                    return true;
                } else if ((next == '-')) {
                    finish();
                    return true;
                }
                break;
            case 71: // [ ws elements*ws ]
                if (('\t' <= next && next <= '\n') || (next == '\r') || (next == ' ')) {
                    append(27, true);
                    return true;
                }
                break;
            case 72: // [ ws*elements ws ]
                if ((next == '"')) {
                    append(3, false);
                    return true;
                } else if ((next == '-')) {
                    append(7, false);
                    return true;
                } else if ((next == '0')) {
                    append(75, true);
                    return true;
                } else if (('1' <= next && next <= '9')) {
                    append(73, true);
                    return true;
                } else if ((next == '[')) {
                    append(4, false);
                    return true;
                } else if ((next == 'f')) {
                    append(11, false);
                    return true;
                } else if ((next == 'n')) {
                    append(8, false);
                    return true;
                } else if ((next == 't')) {
                    append(6, false);
                    return true;
                } else if ((next == '{')) {
                    append(10, false);
                    return true;
                }
                break;
            case 73: // int frac*exp|int*frac exp|onenine*digits|element ws*, ws elements|element*ws , ws elements
                if (('\t' <= next && next <= '\n') || (next == '\r') || (next == ' ')) {
                    replace(88);
                    append(27, true);
                    return true;
                } else if ((next == ',')) {
                    replace(87);
                    append(36, true);
                    return true;
                } else if ((next == '.')) {
                    replace(59);
                    append(60, false);
                    return true;
                } else if ((next == '0')) {
                    replace(94);
                    append(64, true);
                    return true;
                } else if (('1' <= next && next <= '9')) {
                    replace(94);
                    append(64, true);
                    return true;
                } else if ((next == 'E')) {
                    replace(61);
                    append(65, false);
                    return true;
                } else if ((next == 'e')) {
                    replace(61);
                    append(62, false);
                    return true;
                }
                break;
            case 74: // [ ws elements ws*]|[ ws*]
                if ((next == ']')) {
                    finish();
                    return true;
                }
                break;
            case 75: // int*frac exp|int frac*exp|element*ws , ws elements|element ws*, ws elements
                if (('\t' <= next && next <= '\n') || (next == '\r') || (next == ' ')) {
                    replace(88);
                    append(27, true);
                    return true;
                } else if ((next == ',')) {
                    replace(87);
                    append(36, true);
                    return true;
                } else if ((next == '.')) {
                    replace(59);
                    append(60, false);
                    return true;
                } else if ((next == 'E')) {
                    replace(61);
                    append(65, false);
                    return true;
                } else if ((next == 'e')) {
                    replace(61);
                    append(62, false);
                    return true;
                }
                break;
            case 76: // " characters*"|element*ws , ws elements|element ws*, ws elements
                if (('\t' <= next && next <= '\n') || (next == '\r') || (next == ' ')) {
                    replace(88);
                    append(27, true);
                    return true;
                } else if ((next == '"')) {
                    replace(33);
                    append(36, true);
                    return true;
                } else if ((next == ',')) {
                    replace(87);
                    append(36, true);
                    return true;
                }
                break;
            case 77: // element ws ,*ws elements|element ws , ws*elements|element ws*, ws elements
                if (('\t' <= next && next <= '\n') || (next == '\r') || (next == ' ')) {
                    replace(90);
                    append(27, true);
                    return true;
                } else if ((next == '"')) {
                    replace(83);
                    append(3, false);
                    return true;
                } else if ((next == ',')) {
                    replace(87);
                    append(36, true);
                    return true;
                } else if ((next == '-')) {
                    replace(83);
                    append(7, false);
                    return true;
                } else if ((next == '0')) {
                    replace(83);
                    append(75, true);
                    return true;
                } else if (('1' <= next && next <= '9')) {
                    replace(83);
                    append(73, true);
                    return true;
                } else if ((next == '[')) {
                    replace(83);
                    append(4, false);
                    return true;
                } else if ((next == 'f')) {
                    replace(83);
                    append(11, false);
                    return true;
                } else if ((next == 'n')) {
                    replace(83);
                    append(8, false);
                    return true;
                } else if ((next == 't')) {
                    replace(83);
                    append(6, false);
                    return true;
                } else if ((next == '{')) {
                    replace(83);
                    append(10, false);
                    return true;
                }
                break;
            case 78: // [ ws elements*ws ]|[ ws elements ws*]
                if (('\t' <= next && next <= '\n') || (next == '\r') || (next == ' ')) {
                    replace(71);
                    append(27, true);
                    return true;
                } else if ((next == ']')) {
                    replace(21);
                    append(36, true);
                    return true;
                }
                break;
            case 79: // { ws*}|{ ws*members ws }|{ ws members ws*}|{ ws members*ws }|element ws*, ws elements|element*ws , ws elements
                if (('\t' <= next && next <= '\n') || (next == '\r') || (next == ' ')) {
                    replace(101);
                    append(39, true);
                    return true;
                } else if ((next == '"')) {
                    replace(37);
                    append(3, false);
                    return true;
                } else if ((next == ',')) {
                    replace(87);
                    append(36, true);
                    return true;
                } else if ((next == '}')) {
                    replace(35);
                    append(36, true);
                    return true;
                }
                break;
            case 80: // - onenine*digits|int frac*exp|int*frac exp|element ws*, ws elements|element*ws , ws elements
                if (('\t' <= next && next <= '\n') || (next == '\r') || (next == ' ')) {
                    replace(88);
                    append(27, true);
                    return true;
                } else if ((next == ',')) {
                    replace(87);
                    append(36, true);
                    return true;
                } else if ((next == '.')) {
                    replace(59);
                    append(60, false);
                    return true;
                } else if ((next == '0')) {
                    replace(63);
                    append(64, true);
                    return true;
                } else if (('1' <= next && next <= '9')) {
                    replace(63);
                    append(64, true);
                    return true;
                } else if ((next == 'E')) {
                    replace(61);
                    append(65, false);
                    return true;
                } else if ((next == 'e')) {
                    replace(61);
                    append(62, false);
                    return true;
                }
                break;
            case 81: // [ ws*]|[ ws elements ws*]|[ ws*elements ws ]|[ ws elements*ws ]|element ws*, ws elements|element*ws , ws elements
                if (('\t' <= next && next <= '\n') || (next == '\r') || (next == ' ')) {
                    replace(95);
                    append(27, true);
                    return true;
                } else if ((next == '"')) {
                    replace(72);
                    append(3, false);
                    return true;
                } else if ((next == ',')) {
                    replace(87);
                    append(36, true);
                    return true;
                } else if ((next == '-')) {
                    replace(72);
                    append(7, false);
                    return true;
                } else if ((next == '0')) {
                    replace(72);
                    append(92, true);
                    return true;
                } else if (('1' <= next && next <= '9')) {
                    replace(72);
                    append(91, true);
                    return true;
                } else if ((next == '[')) {
                    replace(72);
                    append(4, false);
                    return true;
                } else if ((next == ']')) {
                    replace(74);
                    append(36, true);
                    return true;
                } else if ((next == 'f')) {
                    replace(72);
                    append(11, false);
                    return true;
                } else if ((next == 'n')) {
                    replace(72);
                    append(8, false);
                    return true;
                } else if ((next == 't')) {
                    replace(72);
                    append(6, false);
                    return true;
                } else if ((next == '{')) {
                    replace(72);
                    append(10, false);
                    return true;
                }
                break;
            case 82: // { ws members ws*}|element ws ,*ws elements|element ws , ws*elements|element ws*, ws elements
                if (('\t' <= next && next <= '\n') || (next == '\r') || (next == ' ')) {
                    replace(90);
                    append(27, true);
                    return true;
                } else if ((next == '"')) {
                    replace(83);
                    append(3, false);
                    return true;
                } else if ((next == ',')) {
                    replace(87);
                    append(36, true);
                    return true;
                } else if ((next == '-')) {
                    replace(83);
                    append(7, false);
                    return true;
                } else if ((next == '0')) {
                    replace(83);
                    append(75, true);
                    return true;
                } else if (('1' <= next && next <= '9')) {
                    replace(83);
                    append(73, true);
                    return true;
                } else if ((next == '[')) {
                    replace(83);
                    append(4, false);
                    return true;
                } else if ((next == 'f')) {
                    replace(83);
                    append(11, false);
                    return true;
                } else if ((next == 'n')) {
                    replace(83);
                    append(8, false);
                    return true;
                } else if ((next == 't')) {
                    replace(83);
                    append(6, false);
                    return true;
                } else if ((next == '{')) {
                    replace(83);
                    append(10, false);
                    return true;
                } else if ((next == '}')) {
                    replace(22);
                    append(36, true);
                    return true;
                }
                break;
            case 83: // element ws , ws*elements
                if ((next == '"')) {
                    append(3, false);
                    return true;
                } else if ((next == '-')) {
                    append(7, false);
                    return true;
                } else if ((next == '0')) {
                    append(75, true);
                    return true;
                } else if (('1' <= next && next <= '9')) {
                    append(73, true);
                    return true;
                } else if ((next == '[')) {
                    append(4, false);
                    return true;
                } else if ((next == 'f')) {
                    append(11, false);
                    return true;
                } else if ((next == 'n')) {
                    append(8, false);
                    return true;
                } else if ((next == 't')) {
                    append(6, false);
                    return true;
                } else if ((next == '{')) {
                    append(10, false);
                    return true;
                }
                break;
            case 84: // [ ws elements ws*]|element ws ,*ws elements|element ws , ws*elements|element ws*, ws elements
                if (('\t' <= next && next <= '\n') || (next == '\r') || (next == ' ')) {
                    replace(90);
                    append(27, true);
                    return true;
                } else if ((next == '"')) {
                    replace(83);
                    append(3, false);
                    return true;
                } else if ((next == ',')) {
                    replace(87);
                    append(36, true);
                    return true;
                } else if ((next == '-')) {
                    replace(83);
                    append(7, false);
                    return true;
                } else if ((next == '0')) {
                    replace(83);
                    append(75, true);
                    return true;
                } else if (('1' <= next && next <= '9')) {
                    replace(83);
                    append(73, true);
                    return true;
                } else if ((next == '[')) {
                    replace(83);
                    append(4, false);
                    return true;
                } else if ((next == ']')) {
                    replace(21);
                    append(36, true);
                    return true;
                } else if ((next == 'f')) {
                    replace(83);
                    append(11, false);
                    return true;
                } else if ((next == 'n')) {
                    replace(83);
                    append(8, false);
                    return true;
                } else if ((next == 't')) {
                    replace(83);
                    append(6, false);
                    return true;
                } else if ((next == '{')) {
                    replace(83);
                    append(10, false);
                    return true;
                }
                break;
            case 85: // element ws , ws*elements|element*ws , ws elements|element ws*, ws elements
                if (('\t' <= next && next <= '\n') || (next == '\r') || (next == ' ')) {
                    replace(88);
                    append(27, true);
                    return true;
                } else if ((next == '"')) {
                    replace(83);
                    append(3, false);
                    return true;
                } else if ((next == ',')) {
                    replace(87);
                    append(36, true);
                    return true;
                } else if ((next == '-')) {
                    replace(83);
                    append(7, false);
                    return true;
                } else if ((next == '0')) {
                    replace(83);
                    append(92, true);
                    return true;
                } else if (('1' <= next && next <= '9')) {
                    replace(83);
                    append(91, true);
                    return true;
                } else if ((next == '[')) {
                    replace(83);
                    append(4, false);
                    return true;
                } else if ((next == 'f')) {
                    replace(83);
                    append(11, false);
                    return true;
                } else if ((next == 'n')) {
                    replace(83);
                    append(8, false);
                    return true;
                } else if ((next == 't')) {
                    replace(83);
                    append(6, false);
                    return true;
                } else if ((next == '{')) {
                    replace(83);
                    append(10, false);
                    return true;
                }
                break;
            case 86: // element*ws , ws elements|element ws*, ws elements
                if (('\t' <= next && next <= '\n') || (next == '\r') || (next == ' ')) {
                    replace(88);
                    append(27, true);
                    return true;
                } else if ((next == ',')) {
                    replace(87);
                    append(36, true);
                    return true;
                }
                break;
            case 87: // element ws*, ws elements
                if ((next == ',')) {
                    finish();
                    return true;
                }
                break;
            case 88: // element*ws , ws elements
                if (('\t' <= next && next <= '\n') || (next == '\r') || (next == ' ')) {
                    append(27, true);
                    return true;
                }
                break;
            case 89: // element ws ,*ws elements|element ws , ws*elements
                if (('\t' <= next && next <= '\n') || (next == '\r') || (next == ' ')) {
                    replace(90);
                    append(27, true);
                    return true;
                } else if ((next == '"')) {
                    replace(83);
                    append(3, false);
                    return true;
                } else if ((next == '-')) {
                    replace(83);
                    append(7, false);
                    return true;
                } else if ((next == '0')) {
                    replace(83);
                    append(75, true);
                    return true;
                } else if (('1' <= next && next <= '9')) {
                    replace(83);
                    append(73, true);
                    return true;
                } else if ((next == '[')) {
                    replace(83);
                    append(4, false);
                    return true;
                } else if ((next == 'f')) {
                    replace(83);
                    append(11, false);
                    return true;
                } else if ((next == 'n')) {
                    replace(83);
                    append(8, false);
                    return true;
                } else if ((next == 't')) {
                    replace(83);
                    append(6, false);
                    return true;
                } else if ((next == '{')) {
                    replace(83);
                    append(10, false);
                    return true;
                }
                break;
            case 90: // element ws ,*ws elements
                if (('\t' <= next && next <= '\n') || (next == '\r') || (next == ' ')) {
                    append(27, true);
                    return true;
                }
                break;
            case 91: // onenine*digits|int*frac exp|int frac*exp|element*ws , ws elements
                if (('\t' <= next && next <= '\n') || (next == '\r') || (next == ' ')) {
                    replace(88);
                    append(27, true);
                    return true;
                } else if ((next == '.')) {
                    replace(59);
                    append(60, false);
                    return true;
                } else if ((next == '0')) {
                    replace(94);
                    append(64, true);
                    return true;
                } else if (('1' <= next && next <= '9')) {
                    replace(94);
                    append(64, true);
                    return true;
                } else if ((next == 'E')) {
                    replace(61);
                    append(65, false);
                    return true;
                } else if ((next == 'e')) {
                    replace(61);
                    append(62, false);
                    return true;
                }
                break;
            case 92: // int*frac exp|int frac*exp|element*ws , ws elements
                if (('\t' <= next && next <= '\n') || (next == '\r') || (next == ' ')) {
                    replace(88);
                    append(27, true);
                    return true;
                } else if ((next == '.')) {
                    replace(59);
                    append(60, false);
                    return true;
                } else if ((next == 'E')) {
                    replace(61);
                    append(65, false);
                    return true;
                } else if ((next == 'e')) {
                    replace(61);
                    append(62, false);
                    return true;
                }
                break;
            case 93: // int frac*exp|element*ws , ws elements|element ws*, ws elements
                if (('\t' <= next && next <= '\n') || (next == '\r') || (next == ' ')) {
                    replace(88);
                    append(27, true);
                    return true;
                } else if ((next == ',')) {
                    replace(87);
                    append(36, true);
                    return true;
                } else if ((next == 'E')) {
                    replace(61);
                    append(65, false);
                    return true;
                } else if ((next == 'e')) {
                    replace(61);
                    append(62, false);
                    return true;
                }
                break;
            case 94: // onenine*digits
                if ((next == '0')) {
                    append(64, true);
                    return true;
                } else if (('1' <= next && next <= '9')) {
                    append(64, true);
                    return true;
                }
                break;
            case 95: // [ ws elements*ws ]|element*ws , ws elements
                if (('\t' <= next && next <= '\n') || (next == '\r') || (next == ' ')) {
                    append(27, true);
                    return true;
                }
                break;
            case 96: // [ ws elements ws*]|element ws*, ws elements
                if ((next == ',')) {
                    replace(87);
                    append(36, true);
                    return true;
                } else if ((next == ']')) {
                    replace(21);
                    append(36, true);
                    return true;
                }
                break;
            case 97: // [ ws elements ws*]|element*ws , ws elements|element ws*, ws elements
                if (('\t' <= next && next <= '\n') || (next == '\r') || (next == ' ')) {
                    replace(88);
                    append(27, true);
                    return true;
                } else if ((next == ',')) {
                    replace(87);
                    append(36, true);
                    return true;
                } else if ((next == ']')) {
                    replace(21);
                    append(36, true);
                    return true;
                }
                break;
            case 98: // element ws ,*ws elements|element ws , ws*elements|element*ws , ws elements|element ws*, ws elements
                if (('\t' <= next && next <= '\n') || (next == '\r') || (next == ' ')) {
                    replace(100);
                    append(27, true);
                    return true;
                } else if ((next == '"')) {
                    replace(83);
                    append(3, false);
                    return true;
                } else if ((next == ',')) {
                    replace(87);
                    append(36, true);
                    return true;
                } else if ((next == '-')) {
                    replace(83);
                    append(7, false);
                    return true;
                } else if ((next == '0')) {
                    replace(83);
                    append(92, true);
                    return true;
                } else if (('1' <= next && next <= '9')) {
                    replace(83);
                    append(91, true);
                    return true;
                } else if ((next == '[')) {
                    replace(83);
                    append(4, false);
                    return true;
                } else if ((next == 'f')) {
                    replace(83);
                    append(11, false);
                    return true;
                } else if ((next == 'n')) {
                    replace(83);
                    append(8, false);
                    return true;
                } else if ((next == 't')) {
                    replace(83);
                    append(6, false);
                    return true;
                } else if ((next == '{')) {
                    replace(83);
                    append(10, false);
                    return true;
                }
                break;
            case 99: // element ws , ws*elements|element ws*, ws elements
                if ((next == '"')) {
                    replace(83);
                    append(3, false);
                    return true;
                } else if ((next == ',')) {
                    replace(87);
                    append(36, true);
                    return true;
                } else if ((next == '-')) {
                    replace(83);
                    append(7, false);
                    return true;
                } else if ((next == '0')) {
                    replace(83);
                    append(75, true);
                    return true;
                } else if (('1' <= next && next <= '9')) {
                    replace(83);
                    append(73, true);
                    return true;
                } else if ((next == '[')) {
                    replace(83);
                    append(4, false);
                    return true;
                } else if ((next == 'f')) {
                    replace(83);
                    append(11, false);
                    return true;
                } else if ((next == 'n')) {
                    replace(83);
                    append(8, false);
                    return true;
                } else if ((next == 't')) {
                    replace(83);
                    append(6, false);
                    return true;
                } else if ((next == '{')) {
                    replace(83);
                    append(10, false);
                    return true;
                }
                break;
            case 100: // element ws ,*ws elements|element*ws , ws elements
                if (('\t' <= next && next <= '\n') || (next == '\r') || (next == ' ')) {
                    append(27, true);
                    return true;
                }
                break;
            case 101: // { ws*members ws }|{ ws members*ws }|element*ws , ws elements
                if (('\t' <= next && next <= '\n') || (next == '\r') || (next == ' ')) {
                    append(39, true);
                    return true;
                } else if ((next == '"')) {
                    replace(37);
                    append(3, false);
                    return true;
                }
                break;
            case 102: // { ws members ws*}|element ws*, ws elements
                if ((next == ',')) {
                    replace(87);
                    append(36, true);
                    return true;
                } else if ((next == '}')) {
                    replace(22);
                    append(36, true);
                    return true;
                }
                break;
            case 103: // { ws members ws*}|element*ws , ws elements|element ws*, ws elements
                if (('\t' <= next && next <= '\n') || (next == '\r') || (next == ' ')) {
                    replace(88);
                    append(27, true);
                    return true;
                } else if ((next == ',')) {
                    replace(87);
                    append(36, true);
                    return true;
                } else if ((next == '}')) {
                    replace(22);
                    append(36, true);
                    return true;
                }
                break;
            case 104: // int frac*exp|ws element*ws
                if (('\t' <= next && next <= '\n') || (next == '\r') || (next == ' ')) {
                    replace(25);
                    append(27, true);
                    return true;
                } else if ((next == 'E')) {
                    replace(61);
                    append(65, false);
                    return true;
                } else if ((next == 'e')) {
                    replace(61);
                    append(62, false);
                    return true;
                }
                break;
            case 105: // ws element*ws|[ ws elements*ws ]
                if (('\t' <= next && next <= '\n') || (next == '\r') || (next == ' ')) {
                    append(27, true);
                    return true;
                }
                break;
            case 106: // [ ws elements ws*]|ws element*ws
                if (('\t' <= next && next <= '\n') || (next == '\r') || (next == ' ')) {
                    replace(25);
                    append(27, true);
                    return true;
                } else if ((next == ']')) {
                    replace(21);
                    append(36, true);
                    return true;
                }
                break;
            case 107: // { ws*members ws }|{ ws members*ws }|ws element*ws
                if (('\t' <= next && next <= '\n') || (next == '\r') || (next == ' ')) {
                    append(39, true);
                    return true;
                } else if ((next == '"')) {
                    replace(37);
                    append(3, false);
                    return true;
                }
                break;
            case 108: // { ws members ws*}|ws element*ws
                if (('\t' <= next && next <= '\n') || (next == '\r') || (next == ' ')) {
                    replace(25);
                    append(27, true);
                    return true;
                } else if ((next == '}')) {
                    replace(22);
                    append(36, true);
                    return true;
                }
                break;
            case 109: // {*ws }|{*ws members ws }|{ ws*members ws }
                if (('\t' <= next && next <= '\n') || (next == '\r') || (next == ' ')) {
                    append(39, true);
                    return true;
                } else if ((next == '"')) {
                    replace(37);
                    append(3, false);
                    return true;
                }
                break;
            case 110: // { ws*}
                if ((next == '}')) {
                    finish();
                    return true;
                }
                break;
            case 111: // -*digit
                if ((next == '0')) {
                    finish();
                    return true;
                } else if (('1' <= next && next <= '9')) {
                    finish();
                    return true;
                }
                break;
            case 112: // [*ws elements ws ]|[*ws ]
                if (('\t' <= next && next <= '\n') || (next == '\r') || (next == ' ')) {
                    append(27, true);
                    return true;
                }
                break;
            case 113: // [ ws*]
                if ((next == ']')) {
                    finish();
                    return true;
                }
                break;
            case 114: // [ ws*elements ws ]|[ ws*]
                if ((next == '"')) {
                    replace(72);
                    append(3, false);
                    return true;
                } else if ((next == '-')) {
                    replace(72);
                    append(7, false);
                    return true;
                } else if ((next == '0')) {
                    replace(72);
                    append(75, true);
                    return true;
                } else if (('1' <= next && next <= '9')) {
                    replace(72);
                    append(73, true);
                    return true;
                } else if ((next == '[')) {
                    replace(72);
                    append(4, false);
                    return true;
                } else if ((next == ']')) {
                    replace(113);
                    append(36, true);
                    return true;
                } else if ((next == 'f')) {
                    replace(72);
                    append(11, false);
                    return true;
                } else if ((next == 'n')) {
                    replace(72);
                    append(8, false);
                    return true;
                } else if ((next == 't')) {
                    replace(72);
                    append(6, false);
                    return true;
                } else if ((next == '{')) {
                    replace(72);
                    append(10, false);
                    return true;
                }
                break;
            case 115: // [ ws elements*ws ]|[ ws elements ws*]|element*ws , ws elements|element ws*, ws elements
                if (('\t' <= next && next <= '\n') || (next == '\r') || (next == ' ')) {
                    replace(95);
                    append(27, true);
                    return true;
                } else if ((next == ',')) {
                    replace(87);
                    append(36, true);
                    return true;
                } else if ((next == ']')) {
                    replace(21);
                    append(36, true);
                    return true;
                }
                break;
            case 116: // [ ws elements*ws ]|[ ws elements ws*]|ws element*ws
                if (('\t' <= next && next <= '\n') || (next == '\r') || (next == ' ')) {
                    replace(105);
                    append(27, true);
                    return true;
                } else if ((next == ']')) {
                    replace(21);
                    append(36, true);
                    return true;
                }
                break;
            case 117: // "*characters "
                if ((next == ' ') || ('0' <= next && next <= '9') || ('A' <= next && next <= 'Z') || ('a' <= next && next <= 'z')) {
                    append(119, true);
                    return true;
                } else if ((next == '\\')) {
                    append(118, false);
                    return true;
                }
                break;
            case 118: // \*escape
                if ((next == '"') || (next == '/') || (next == '\\') || (next == 'b') || (next == 'n') || (next == 'r') || (next == 't')) {
                    finish();
                    return true;
                } else if ((next == 'u')) {
                    append(120, false);
                    return true;
                }
                break;
            case 119: // character*characters
                if ((next == ' ') || ('0' <= next && next <= '9') || ('A' <= next && next <= 'Z') || ('a' <= next && next <= 'z')) {
                    append(119, true);
                    return true;
                } else if ((next == '\\')) {
                    append(118, false);
                    return true;
                }
                break;
            case 120: // u*hex hex hex hex
                if ((next == '0')) {
                    finish();
                    return true;
                } else if (('1' <= next && next <= '9')) {
                    finish();
                    return true;
                } else if (('A' <= next && next <= 'F') || ('a' <= next && next <= 'f')) {
                    finish();
                    return true;
                }
                break;
            case 121: // u hex*hex hex hex
                if ((next == '0')) {
                    finish();
                    return true;
                } else if (('1' <= next && next <= '9')) {
                    finish();
                    return true;
                } else if (('A' <= next && next <= 'F') || ('a' <= next && next <= 'f')) {
                    finish();
                    return true;
                }
                break;
            case 122: // u hex hex*hex hex
                if ((next == '0')) {
                    finish();
                    return true;
                } else if (('1' <= next && next <= '9')) {
                    finish();
                    return true;
                } else if (('A' <= next && next <= 'F') || ('a' <= next && next <= 'f')) {
                    finish();
                    return true;
                }
                break;
            case 123: // u hex hex hex*hex
                if ((next == '0')) {
                    finish();
                    return true;
                } else if (('1' <= next && next <= '9')) {
                    finish();
                    return true;
                } else if (('A' <= next && next <= 'F') || ('a' <= next && next <= 'f')) {
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
            case 69:
                return "{E•sign {0-9}[1-]}";
            case 101:
                return "{element•ws , ws elements|{ ws members•ws }|{ ws•members ws }}";
            case 88:
                return "{element•ws , ws elements}";
            case 115:
                return "{[ ws elements•ws ]|[ ws elements ws•]|element•ws , ws elements|element ws•, ws elements}";
            case 5:
                return "{onenine•digits|int•frac exp|int frac•exp|ws element•ws}";
            case 120:
                return "{u•hex hex hex hex}";
            case 10:
                return "{{•ws }|{ ws•}|{•ws members ws }|{ ws•members ws }}";
            case 56:
                return "{ws string•ws : ws element|ws string ws•: ws element}";
            case 42:
                return "{\\\" characters•\\\"|ws string•ws : ws element|ws string ws•: ws element}";
            case 24:
                return "{t r u•e}";
            case 37:
                return "{{ ws•members ws }}";
            case 25:
                return "{ws element•ws}";
            case 52:
                return "{ws string ws :•ws element}";
            case 14:
                return "{f a•l s e}";
            case 110:
                return "{{ ws•}}";
            case 20:
                return "{n u l•l}";
            case 46:
                return "{member ws , ws•members}";
            case 93:
                return "{int frac•exp|element•ws , ws elements|element ws•, ws elements}";
            case 57:
                return "{ws•string ws : ws element}";
            case 78:
                return "{[ ws elements•ws ]|[ ws elements ws•]}";
            case 29:
                return "{onenine•digits|int•frac exp|int frac•exp}";
            case 106:
                return "{[ ws elements ws•]|ws element•ws}";
            case 121:
                return "{u hex•hex hex hex}";
            case 84:
                return "{[ ws elements ws•]|element ws ,•ws elements|element ws , ws•elements|element ws•, ws elements}";
            case 61:
                return "{int frac•exp}";
            case 89:
                return "{element ws ,•ws elements|element ws , ws•elements}";
            case 116:
                return "{[ ws elements•ws ]|[ ws elements ws•]|ws element•ws}";
            case 1:
                return "{•<start>}";
            case 74:
                return "{[ ws elements ws•]|[ ws•]}";
            case 6:
                return "{t•r u e}";
            case 60:
                return "{.•{0-9}[1-]}";
            case 117:
                return "{\\\"•characters \\\"}";
            case 85:
                return "{element ws , ws•elements|element•ws , ws elements|element ws•, ws elements}";
            case 102:
                return "{{ ws members ws•}|element ws•, ws elements}";
            case 28:
                return "{ws•element ws}";
            case 38:
                return "{{ ws•members ws }|{ ws members•ws }}";
            case 70:
                return "{e•sign {0-9}[1-]}";
            case 21:
                return "{[ ws elements ws•]}";
            case 33:
                return "{\\\" characters•\\\"}";
            case 92:
                return "{int•frac exp|int frac•exp|element•ws , ws elements}";
            case 65:
                return "{E•sign {0-9}[1-]|E sign•{0-9}[1-]}";
            case 97:
                return "{[ ws elements ws•]|element•ws , ws elements|element ws•, ws elements}";
            case 9:
                return "{int•frac exp|int frac•exp|ws element•ws}";
            case 53:
                return "{ws string ws•: ws element}";
            case 109:
                return "{{•ws }|{•ws members ws }|{ ws•members ws }}";
            case 77:
                return "{element ws ,•ws elements|element ws , ws•elements|element ws•, ws elements}";
            case 96:
                return "{[ ws elements ws•]|element ws•, ws elements}";
            case 13:
                return "{t r•u e}";
            case 41:
                return "{ws string•ws : ws element|ws string ws•: ws element|ws•string ws : ws element}";
            case 73:
                return "{int frac•exp|element•ws , ws elements|int•frac exp|onenine•digits|element ws•, ws elements}";
            case 105:
                return "{[ ws elements•ws ]|ws element•ws}";
            case 2:
                return "{{\\\\t-\\\\n\\\\r\\\\u0020}•ws|ws•element ws}";
            case 32:
                return "{- onenine•digits|int•frac exp|int frac•exp}";
            case 34:
                return "{{ ws•}|{ ws•members ws }|{ ws members•ws }|{ ws members ws•}}";
            case 45:
                return "{member ws•, ws members|member ws ,•ws members|member ws , ws•members}";
            case 64:
                return "{digit•digits}";
            case 17:
                return "{\\\" characters•\\\"|ws element•ws}";
            case 22:
                return "{{ ws members ws•}}";
            case 44:
                return "{ws string ws : ws•element|member•ws , ws members|member ws•, ws members}";
            case 59:
                return "{int•frac exp}";
            case 118:
                return "{\\\\•escape}";
            case 27:
                return "{{\\\\t-\\\\n\\\\r\\\\u0020}•ws}";
            case 71:
                return "{[ ws elements•ws ]}";
            case 12:
                return "{n u•l l}";
            case 54:
                return "{ws string ws :•ws element|ws string ws : ws•element}";
            case 49:
                return "{member•ws , ws members}";
            case 86:
                return "{element•ws , ws elements|element ws•, ws elements}";
            case 113:
                return "{[ ws•]}";
            case 81:
                return "{[ ws elements•ws ]|[ ws•]|element•ws , ws elements|[ ws•elements ws ]|[ ws elements ws•]|element ws•, ws elements}";
            case 76:
                return "{\\\" characters•\\\"|element•ws , ws elements|element ws•, ws elements}";
            case 7:
                return "{-•digit|-•onenine digits}";
            case 39:
                return "{{\\\\t-\\\\n\\\\r\\\\u0020}•ws|ws•string ws : ws element}";
            case 98:
                return "{element ws ,•ws elements|element ws , ws•elements|element•ws , ws elements|element ws•, ws elements}";
            case 103:
                return "{{ ws members ws•}|element•ws , ws elements|element ws•, ws elements}";
            case 91:
                return "{onenine•digits|int•frac exp|int frac•exp|element•ws , ws elements}";
            case 66:
                return "{e sign•{0-9}[1-]}";
            case 108:
                return "{{ ws members ws•}|ws element•ws}";
            case 3:
                return "{\\\"•characters \\\"|\\\" characters•\\\"}";
            case 80:
                return "{- onenine•digits|int frac•exp|element•ws , ws elements|int•frac exp|element ws•, ws elements}";
            case 35:
                return "{{ ws•}|{ ws members ws•}}";
            case 112:
                return "{[•ws elements ws ]|[•ws ]}";
            case 123:
                return "{u hex hex hex•hex}";
            case 48:
                return "{member ws•, ws members}";
            case 63:
                return "{- onenine•digits}";
            case 18:
                return "{- onenine•digits|int•frac exp|int frac•exp|ws element•ws}";
            case 95:
                return "{[ ws elements•ws ]|element•ws , ws elements}";
            case 50:
                return "{ws string ws : ws•element}";
            case 67:
                return "{E sign•{0-9}[1-]}";
            case 16:
                return "{[ ws elements•ws ]|[ ws•]|[ ws•elements ws ]|ws element•ws|[ ws elements ws•]}";
            case 31:
                return "{[ ws•elements ws ]|[ ws elements•ws ]|[ ws elements ws•]|[ ws•]}";
            case 11:
                return "{f•a l s e}";
            case 72:
                return "{[ ws•elements ws ]}";
            case 43:
                return "{ws string ws•: ws element|ws string ws :•ws element|ws string ws : ws•element}";
            case 99:
                return "{element ws , ws•elements|element ws•, ws elements}";
            case 87:
                return "{element ws•, ws elements}";
            case 104:
                return "{int frac•exp|ws element•ws}";
            case 40:
                return "{{ ws members•ws }|{ ws members ws•}}";
            case 26:
                return "{f a l s•e}";
            case 55:
                return "{ws string•ws : ws element}";
            case 114:
                return "{[ ws•elements ws ]|[ ws•]}";
            case 23:
                return "{f a l•s e}";
            case 8:
                return "{n•u l l}";
            case 75:
                return "{int•frac exp|int frac•exp|element•ws , ws elements|element ws•, ws elements}";
            case 119:
                return "{character•characters}";
            case 58:
                return "{{ ws members•ws }}";
            case 82:
                return "{{ ws members ws•}|element ws ,•ws elements|element ws , ws•elements|element ws•, ws elements}";
            case 36:
                return "{}";
            case 30:
                return "{int•frac exp|int frac•exp}";
            case 51:
                return "{member•ws , ws members|member ws•, ws members}";
            case 19:
                return "{ws element•ws|ws•element ws}";
            case 107:
                return "{{ ws members•ws }|{ ws•members ws }|ws element•ws}";
            case 4:
                return "{[•ws elements ws ]|[ ws•elements ws ]|[•ws ]|[ ws•]}";
            case 79:
                return "{{ ws•}|{ ws members ws•}|element•ws , ws elements|{ ws members•ws }|{ ws•members ws }|element ws•, ws elements}";
            case 94:
                return "{onenine•digits}";
            case 47:
                return "{member ws ,•ws members|member ws , ws•members}";
            case 15:
                return "{{ ws•}|{ ws members ws•}|{ ws members•ws }|{ ws•members ws }|ws element•ws}";
            case 68:
                return "{{0-9}[1-]•{0-9}}";
            case 62:
                return "{e•sign {0-9}[1-]|e sign•{0-9}[1-]}";
            case 90:
                return "{element ws ,•ws elements}";
            case 111:
                return "{-•digit}";
            case 122:
                return "{u hex hex•hex hex}";
            case 83:
                return "{element ws , ws•elements}";
            case 100:
                return "{element ws ,•ws elements|element•ws , ws elements}";
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
        GeneratedJsonParser p = new GeneratedJsonParser();
        boolean result;
        result = p.proceed("[null ]");
        if (!result) {
            System.out.println("Failed to parse");
        } else {
            result = p.eof();
            System.out.println(result);
        }
    }
}
