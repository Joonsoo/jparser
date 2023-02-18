package com.giyeok.jparser.parsergen.generated.deprecated;

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
                return (c == 's');
            case 14:
                return (c == 'e');
            case 15:
                return ('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ');
            case 16:
                return ('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ');
            case 17:
                return ('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ') || (c == '"');
            case 18:
                return ('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ') || (c == '"');
            case 19:
                return (c == '}');
            case 21:
                return ('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ') || (c == '"');
            case 22:
                return ('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ') || (c == '}');
            case 23:
                return ('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ');
            case 24:
                return (c == '}');
            case 25:
                return (c == '"');
            case 26:
                return ('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ') || (c == ':');
            case 27:
                return ('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ');
            case 28:
                return (c == ':');
            case 29:
                return ('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ') || (c == '"') || (c == '-') || ('0' <= c && c <= '9') || (c == '[') || (c == 'f') || (c == 'n') || (c == 't') || (c == '{');
            case 30:
                return ('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ');
            case 31:
                return (c == '"') || (c == '-') || ('0' <= c && c <= '9') || (c == '[') || (c == 'f') || (c == 'n') || (c == 't') || (c == '{');
            case 32:
                return (c == '.') || ('0' <= c && c <= '9') || (c == 'E') || (c == 'e');
            case 33:
                return (c == '.') || (c == 'E') || (c == 'e');
            case 34:
                return (c == 'E') || (c == 'e');
            case 35:
                return (c == '+') || (c == '-') || ('0' <= c && c <= '9');
            case 36:
                return (c == '+') || (c == '-') || ('0' <= c && c <= '9');
            case 37:
                return (c == '.');
            case 38:
                return ('0' <= c && c <= '9');
            case 39:
                return ('0' <= c && c <= '9');
            case 40:
                return (c == '+') || (c == '-');
            case 41:
                return ('0' <= c && c <= '9');
            case 42:
                return (c == '+') || (c == '-');
            case 43:
                return ('0' <= c && c <= '9');
            case 44:
                return ('0' <= c && c <= '9');
            case 45:
                return ('0' <= c && c <= '9');
            case 46:
                return ('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ') || (c == ',');
            case 47:
                return ('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ');
            case 48:
                return (c == ',');
            case 49:
                return ('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ') || (c == '"');
            case 50:
                return ('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ') || (c == '"');
            case 51:
                return ('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ') || (c == '"') || (c == '}');
            case 52:
                return (c == '}');
            case 53:
                return ('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ') || (c == '"');
            case 54:
                return ('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ') || (c == 'E') || (c == 'e');
            case 55:
                return (c == 'l');
            case 56:
                return (c == 'l');
            case 57:
                return ('0' <= c && c <= '9');
            case 58:
                return (c == '.') || ('0' <= c && c <= '9') || (c == 'E') || (c == 'e');
            case 59:
                return ('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ') || (c == '.') || ('0' <= c && c <= '9') || (c == 'E') || (c == 'e');
            case 60:
                return ('0' <= c && c <= '9');
            case 61:
                return (c == 'u');
            case 62:
                return (c == 'e');
            case 63:
                return ('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ');
            case 64:
                return (c == '"') || (c == '-') || ('0' <= c && c <= '9') || (c == '[') || (c == 'f') || (c == 'n') || (c == 't') || (c == '{');
            case 65:
                return ('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ') || (c == ',') || (c == '.') || ('0' <= c && c <= '9') || (c == 'E') || (c == 'e');
            case 66:
                return (c == ']');
            case 67:
                return ('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ') || (c == ',') || (c == '.') || (c == 'E') || (c == 'e');
            case 68:
                return ('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ');
            case 69:
                return (c == ',');
            case 70:
                return ('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ') || (c == ']');
            case 71:
                return ('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ') || (c == ',') || (c == '.') || ('0' <= c && c <= '9') || (c == 'E') || (c == 'e');
            case 72:
                return ('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ') || (c == ',');
            case 73:
                return ('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ') || (c == '"') || (c == '-') || ('0' <= c && c <= '9') || (c == '[') || (c == 'f') || (c == 'n') || (c == 't') || (c == '{');
            case 74:
                return ('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ') || (c == ',') || (c == 'E') || (c == 'e');
            case 75:
                return ('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ');
            case 76:
                return (c == '"') || (c == '-') || ('0' <= c && c <= '9') || (c == '[') || (c == 'f') || (c == 'n') || (c == 't') || (c == '{');
            case 77:
                return ('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ');
            case 78:
                return (c == ']');
            case 79:
                return (c == '"') || (c == '-') || ('0' <= c && c <= '9') || (c == '[') || (c == ']') || (c == 'f') || (c == 'n') || (c == 't') || (c == '{');
            case 80:
                return (c == ' ') || ('0' <= c && c <= '9') || ('A' <= c && c <= 'Z') || (c == '\\') || ('a' <= c && c <= 'z');
            case 81:
                return (c == '"') || (c == '/') || (c == '\\') || (c == 'b') || (c == 'n') || (c == 'r') || ('t' <= c && c <= 'u');
            case 82:
                return (c == '"');
            case 83:
                return (c == ' ') || ('0' <= c && c <= '9') || ('A' <= c && c <= 'Z') || (c == '\\') || ('a' <= c && c <= 'z');
            case 84:
                return ('0' <= c && c <= '9') || ('A' <= c && c <= 'F') || ('a' <= c && c <= 'f');
            case 85:
                return ('0' <= c && c <= '9') || ('A' <= c && c <= 'F') || ('a' <= c && c <= 'f');
            case 86:
                return ('0' <= c && c <= '9') || ('A' <= c && c <= 'F') || ('a' <= c && c <= 'f');
            case 87:
                return ('0' <= c && c <= '9') || ('A' <= c && c <= 'F') || ('a' <= c && c <= 'f');
            case 88:
                return (c == '"') || (c == '-') || ('0' <= c && c <= '9') || (c == '[') || (c == 'f') || (c == 'n') || (c == 't') || (c == '{');
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

        if (prevNodeType == 1 && lastNodeType == 6) {
            last = new Node(61, last.parent);
            pendingFinish = false;
        } else if (prevNodeType == 1 && lastNodeType == 7) {
            last = new Node(59, last.parent);
            pendingFinish = true;
        } else if (prevNodeType == 1 && lastNodeType == 8) {
            last = new Node(55, last.parent);
            pendingFinish = false;
        } else if (prevNodeType == 1 && lastNodeType == 11) {
            last = new Node(12, last.parent);
            pendingFinish = false;
        } else if (prevNodeType == 1 && lastNodeType == 12) {
            last = new Node(13, last.parent);
            pendingFinish = false;
        } else if (prevNodeType == 1 && lastNodeType == 13) {
            last = new Node(14, last.parent);
            pendingFinish = false;
        } else if (prevNodeType == 1 && lastNodeType == 14) {
            last = new Node(15, last.parent);
            pendingFinish = true;
        } else if (prevNodeType == 1 && lastNodeType == 15) {
            last = last.parent;
            finish();
        } else if (prevNodeType == 1 && lastNodeType == 16) {
            last = new Node(88, last.parent);
            pendingFinish = false;
        } else if (prevNodeType == 1 && lastNodeType == 17) {
            last = new Node(51, last.parent);
            pendingFinish = false;
        } else if (prevNodeType == 1 && lastNodeType == 19) {
            last = new Node(15, last.parent);
            pendingFinish = true;
        } else if (prevNodeType == 1 && lastNodeType == 21) {
            last = new Node(22, last.parent);
            pendingFinish = false;
        } else if (prevNodeType == 1 && lastNodeType == 23) {
            last = new Node(24, last.parent);
            pendingFinish = false;
        } else if (prevNodeType == 1 && lastNodeType == 24) {
            last = new Node(15, last.parent);
            pendingFinish = true;
        } else if (prevNodeType == 1 && lastNodeType == 34) {
            last = new Node(15, last.parent);
            pendingFinish = true;
        } else if (prevNodeType == 1 && lastNodeType == 37) {
            last = new Node(54, last.parent);
            pendingFinish = true;
        } else if (prevNodeType == 1 && lastNodeType == 44) {
            last = new Node(9, last.parent);
            pendingFinish = true;
        } else if (prevNodeType == 1 && lastNodeType == 52) {
            last = new Node(15, last.parent);
            pendingFinish = true;
        } else if (prevNodeType == 1 && lastNodeType == 53) {
            last = new Node(24, last.parent);
            pendingFinish = false;
        } else if (prevNodeType == 1 && lastNodeType == 55) {
            last = new Node(56, last.parent);
            pendingFinish = false;
        } else if (prevNodeType == 1 && lastNodeType == 56) {
            last = new Node(15, last.parent);
            pendingFinish = true;
        } else if (prevNodeType == 1 && lastNodeType == 57) {
            last = new Node(9, last.parent);
            pendingFinish = true;
        } else if (prevNodeType == 1 && lastNodeType == 60) {
            last = new Node(9, last.parent);
            pendingFinish = true;
        } else if (prevNodeType == 1 && lastNodeType == 61) {
            last = new Node(62, last.parent);
            pendingFinish = false;
        } else if (prevNodeType == 1 && lastNodeType == 62) {
            last = new Node(15, last.parent);
            pendingFinish = true;
        } else if (prevNodeType == 1 && lastNodeType == 63) {
            last = new Node(79, last.parent);
            pendingFinish = false;
        } else if (prevNodeType == 1 && lastNodeType == 64) {
            last = new Node(70, last.parent);
            pendingFinish = false;
        } else if (prevNodeType == 1 && lastNodeType == 66) {
            last = new Node(15, last.parent);
            pendingFinish = true;
        } else if (prevNodeType == 1 && lastNodeType == 77) {
            last = new Node(78, last.parent);
            pendingFinish = false;
        } else if (prevNodeType == 1 && lastNodeType == 78) {
            last = new Node(15, last.parent);
            pendingFinish = true;
        } else if (prevNodeType == 1 && lastNodeType == 80) {
            last = new Node(82, last.parent);
            pendingFinish = false;
        } else if (prevNodeType == 1 && lastNodeType == 82) {
            last = new Node(15, last.parent);
            pendingFinish = true;
        } else if (prevNodeType == 1 && lastNodeType == 88) {
            last = new Node(15, last.parent);
            pendingFinish = true;
        } else if (prevNodeType == 15 && lastNodeType == 16) {
            last = last.parent;
            finish();
        } else if (prevNodeType == 16 && lastNodeType == 16) {
            last = last.parent;
            finish();
        } else if (prevNodeType == 17 && lastNodeType == 16) {
            last = new Node(25, last.parent);
            pendingFinish = true;
        } else if (prevNodeType == 17 && lastNodeType == 25) {
            last = new Node(26, new Node(21, last.parent.parent));
            pendingFinish = false;
        } else if (prevNodeType == 21 && lastNodeType == 16) {
            last = new Node(25, last.parent);
            pendingFinish = false;
        } else if (prevNodeType == 21 && lastNodeType == 25) {
            last = new Node(26, last.parent);
            pendingFinish = false;
        } else if (prevNodeType == 21 && lastNodeType == 27) {
            last = new Node(28, last.parent);
            pendingFinish = false;
        } else if (prevNodeType == 21 && lastNodeType == 28) {
            last = new Node(29, last.parent);
            pendingFinish = false;
        } else if (prevNodeType == 21 && lastNodeType == 30) {
            last = new Node(31, last.parent);
            pendingFinish = false;
        } else if (prevNodeType == 21 && lastNodeType == 31) {
            last = new Node(46, last.parent);
            pendingFinish = true;
        } else if (prevNodeType == 21 && lastNodeType == 47) {
            last = new Node(48, last.parent);
            pendingFinish = false;
        } else if (prevNodeType == 21 && lastNodeType == 48) {
            last = new Node(49, last.parent);
            pendingFinish = false;
        } else if (prevNodeType == 21 && lastNodeType == 49) {
            last = new Node(50, last.parent);
            pendingFinish = true;
        } else if (prevNodeType == 21 && lastNodeType == 50) {
            last = last.parent;
            finish();
        } else if (prevNodeType == 21 && lastNodeType == 80) {
            last = new Node(82, last.parent);
            pendingFinish = false;
        } else if (prevNodeType == 21 && lastNodeType == 82) {
            last = new Node(26, last.parent);
            pendingFinish = false;
        } else if (prevNodeType == 23 && lastNodeType == 16) {
            last = last.parent;
            finish();
        } else if (prevNodeType == 25 && lastNodeType == 80) {
            last = new Node(82, last.parent);
            pendingFinish = false;
        } else if (prevNodeType == 25 && lastNodeType == 82) {
            last = last.parent;
            finish();
        } else if (prevNodeType == 27 && lastNodeType == 16) {
            last = last.parent;
            finish();
        } else if (prevNodeType == 30 && lastNodeType == 16) {
            last = last.parent;
            finish();
        } else if (prevNodeType == 31 && lastNodeType == 6) {
            last = new Node(61, last.parent);
            pendingFinish = false;
        } else if (prevNodeType == 31 && lastNodeType == 7) {
            last = new Node(58, last.parent);
            pendingFinish = true;
        } else if (prevNodeType == 31 && lastNodeType == 8) {
            last = new Node(55, last.parent);
            pendingFinish = false;
        } else if (prevNodeType == 31 && lastNodeType == 11) {
            last = new Node(12, last.parent);
            pendingFinish = false;
        } else if (prevNodeType == 31 && lastNodeType == 12) {
            last = new Node(13, last.parent);
            pendingFinish = false;
        } else if (prevNodeType == 31 && lastNodeType == 13) {
            last = new Node(14, last.parent);
            pendingFinish = false;
        } else if (prevNodeType == 31 && lastNodeType == 14) {
            last = last.parent;
            finish();
        } else if (prevNodeType == 31 && lastNodeType == 17) {
            last = new Node(51, last.parent);
            pendingFinish = false;
        } else if (prevNodeType == 31 && lastNodeType == 19) {
            last = last.parent;
            finish();
        } else if (prevNodeType == 31 && lastNodeType == 21) {
            last = new Node(22, last.parent);
            pendingFinish = false;
        } else if (prevNodeType == 31 && lastNodeType == 23) {
            last = new Node(24, last.parent);
            pendingFinish = false;
        } else if (prevNodeType == 31 && lastNodeType == 24) {
            last = last.parent;
            finish();
        } else if (prevNodeType == 31 && lastNodeType == 34) {
            last = last.parent;
            finish();
        } else if (prevNodeType == 31 && lastNodeType == 37) {
            last = new Node(34, last.parent);
            pendingFinish = true;
        } else if (prevNodeType == 31 && lastNodeType == 44) {
            last = new Node(33, last.parent);
            pendingFinish = true;
        } else if (prevNodeType == 31 && lastNodeType == 52) {
            last = last.parent;
            finish();
        } else if (prevNodeType == 31 && lastNodeType == 53) {
            last = new Node(24, last.parent);
            pendingFinish = false;
        } else if (prevNodeType == 31 && lastNodeType == 55) {
            last = new Node(56, last.parent);
            pendingFinish = false;
        } else if (prevNodeType == 31 && lastNodeType == 56) {
            last = last.parent;
            finish();
        } else if (prevNodeType == 31 && lastNodeType == 57) {
            last = new Node(33, last.parent);
            pendingFinish = true;
        } else if (prevNodeType == 31 && lastNodeType == 60) {
            last = new Node(33, last.parent);
            pendingFinish = true;
        } else if (prevNodeType == 31 && lastNodeType == 61) {
            last = new Node(62, last.parent);
            pendingFinish = false;
        } else if (prevNodeType == 31 && lastNodeType == 62) {
            last = last.parent;
            finish();
        } else if (prevNodeType == 31 && lastNodeType == 63) {
            last = new Node(79, last.parent);
            pendingFinish = false;
        } else if (prevNodeType == 31 && lastNodeType == 64) {
            last = new Node(70, last.parent);
            pendingFinish = false;
        } else if (prevNodeType == 31 && lastNodeType == 66) {
            last = last.parent;
            finish();
        } else if (prevNodeType == 31 && lastNodeType == 77) {
            last = new Node(78, last.parent);
            pendingFinish = false;
        } else if (prevNodeType == 31 && lastNodeType == 78) {
            last = last.parent;
            finish();
        } else if (prevNodeType == 31 && lastNodeType == 80) {
            last = new Node(82, last.parent);
            pendingFinish = false;
        } else if (prevNodeType == 31 && lastNodeType == 82) {
            last = last.parent;
            finish();
        } else if (prevNodeType == 34 && lastNodeType == 40) {
            last = new Node(41, last.parent);
            pendingFinish = false;
        } else if (prevNodeType == 34 && lastNodeType == 41) {
            last = last.parent;
            finish();
        } else if (prevNodeType == 34 && lastNodeType == 42) {
            last = new Node(43, last.parent);
            pendingFinish = false;
        } else if (prevNodeType == 34 && lastNodeType == 43) {
            last = last.parent;
            finish();
        } else if (prevNodeType == 37 && lastNodeType == 38) {
            last = last.parent;
            finish();
        } else if (prevNodeType == 38 && lastNodeType == 39) {
            last = new Node(39, last.parent);
            pendingFinish = true;
        } else if (prevNodeType == 41 && lastNodeType == 39) {
            last = new Node(39, last.parent);
            pendingFinish = true;
        } else if (prevNodeType == 43 && lastNodeType == 39) {
            last = new Node(39, last.parent);
            pendingFinish = true;
        } else if (prevNodeType == 44 && lastNodeType == 45) {
            last = last.parent;
            finish();
        } else if (prevNodeType == 45 && lastNodeType == 45) {
            last = last.parent;
            finish();
        } else if (prevNodeType == 47 && lastNodeType == 16) {
            last = last.parent;
            finish();
        } else if (prevNodeType == 49 && lastNodeType == 16) {
            last = new Node(25, last.parent);
            pendingFinish = true;
        } else if (prevNodeType == 49 && lastNodeType == 25) {
            last = new Node(26, new Node(50, last.parent.parent));
            pendingFinish = false;
        } else if (prevNodeType == 50 && lastNodeType == 16) {
            last = new Node(25, last.parent);
            pendingFinish = false;
        } else if (prevNodeType == 50 && lastNodeType == 25) {
            last = new Node(26, last.parent);
            pendingFinish = false;
        } else if (prevNodeType == 50 && lastNodeType == 27) {
            last = new Node(28, last.parent);
            pendingFinish = false;
        } else if (prevNodeType == 50 && lastNodeType == 28) {
            last = new Node(29, last.parent);
            pendingFinish = false;
        } else if (prevNodeType == 50 && lastNodeType == 30) {
            last = new Node(31, last.parent);
            pendingFinish = false;
        } else if (prevNodeType == 50 && lastNodeType == 31) {
            last = new Node(46, last.parent);
            pendingFinish = true;
        } else if (prevNodeType == 50 && lastNodeType == 47) {
            last = new Node(48, last.parent);
            pendingFinish = false;
        } else if (prevNodeType == 50 && lastNodeType == 48) {
            last = new Node(49, last.parent);
            pendingFinish = false;
        } else if (prevNodeType == 50 && lastNodeType == 49) {
            last = new Node(50, last.parent);
            pendingFinish = false;
        } else if (prevNodeType == 50 && lastNodeType == 50) {
            last = last.parent;
            finish();
        } else if (prevNodeType == 50 && lastNodeType == 80) {
            last = new Node(82, last.parent);
            pendingFinish = false;
        } else if (prevNodeType == 50 && lastNodeType == 82) {
            last = new Node(26, last.parent);
            pendingFinish = false;
        } else if (prevNodeType == 53 && lastNodeType == 16) {
            last = new Node(25, last.parent);
            pendingFinish = true;
        } else if (prevNodeType == 53 && lastNodeType == 25) {
            last = new Node(26, new Node(21, last.parent.parent));
            pendingFinish = false;
        } else if (prevNodeType == 60 && lastNodeType == 45) {
            last = last.parent;
            finish();
        } else if (prevNodeType == 63 && lastNodeType == 16) {
            last = last.parent;
            finish();
        } else if (prevNodeType == 64 && lastNodeType == 6) {
            last = new Node(61, last.parent);
            pendingFinish = false;
        } else if (prevNodeType == 64 && lastNodeType == 7) {
            last = new Node(71, last.parent);
            pendingFinish = true;
        } else if (prevNodeType == 64 && lastNodeType == 8) {
            last = new Node(55, last.parent);
            pendingFinish = false;
        } else if (prevNodeType == 64 && lastNodeType == 11) {
            last = new Node(12, last.parent);
            pendingFinish = false;
        } else if (prevNodeType == 64 && lastNodeType == 12) {
            last = new Node(13, last.parent);
            pendingFinish = false;
        } else if (prevNodeType == 64 && lastNodeType == 13) {
            last = new Node(14, last.parent);
            pendingFinish = false;
        } else if (prevNodeType == 64 && lastNodeType == 14) {
            last = new Node(72, last.parent);
            pendingFinish = true;
        } else if (prevNodeType == 64 && lastNodeType == 17) {
            last = new Node(51, last.parent);
            pendingFinish = false;
        } else if (prevNodeType == 64 && lastNodeType == 19) {
            last = new Node(72, last.parent);
            pendingFinish = true;
        } else if (prevNodeType == 64 && lastNodeType == 21) {
            last = new Node(22, last.parent);
            pendingFinish = false;
        } else if (prevNodeType == 64 && lastNodeType == 23) {
            last = new Node(24, last.parent);
            pendingFinish = false;
        } else if (prevNodeType == 64 && lastNodeType == 24) {
            last = new Node(72, last.parent);
            pendingFinish = true;
        } else if (prevNodeType == 64 && lastNodeType == 34) {
            last = new Node(72, last.parent);
            pendingFinish = true;
        } else if (prevNodeType == 64 && lastNodeType == 37) {
            last = new Node(74, last.parent);
            pendingFinish = true;
        } else if (prevNodeType == 64 && lastNodeType == 44) {
            last = new Node(67, last.parent);
            pendingFinish = true;
        } else if (prevNodeType == 64 && lastNodeType == 52) {
            last = new Node(72, last.parent);
            pendingFinish = true;
        } else if (prevNodeType == 64 && lastNodeType == 53) {
            last = new Node(24, last.parent);
            pendingFinish = false;
        } else if (prevNodeType == 64 && lastNodeType == 55) {
            last = new Node(56, last.parent);
            pendingFinish = false;
        } else if (prevNodeType == 64 && lastNodeType == 56) {
            last = new Node(72, last.parent);
            pendingFinish = true;
        } else if (prevNodeType == 64 && lastNodeType == 57) {
            last = new Node(67, last.parent);
            pendingFinish = true;
        } else if (prevNodeType == 64 && lastNodeType == 60) {
            last = new Node(67, last.parent);
            pendingFinish = true;
        } else if (prevNodeType == 64 && lastNodeType == 61) {
            last = new Node(62, last.parent);
            pendingFinish = false;
        } else if (prevNodeType == 64 && lastNodeType == 62) {
            last = new Node(72, last.parent);
            pendingFinish = true;
        } else if (prevNodeType == 64 && lastNodeType == 63) {
            last = new Node(79, last.parent);
            pendingFinish = false;
        } else if (prevNodeType == 64 && lastNodeType == 64) {
            last = new Node(70, last.parent);
            pendingFinish = false;
        } else if (prevNodeType == 64 && lastNodeType == 66) {
            last = new Node(72, last.parent);
            pendingFinish = true;
        } else if (prevNodeType == 64 && lastNodeType == 68) {
            last = new Node(69, last.parent);
            pendingFinish = false;
        } else if (prevNodeType == 64 && lastNodeType == 69) {
            last = new Node(73, last.parent);
            pendingFinish = false;
        } else if (prevNodeType == 64 && lastNodeType == 75) {
            last = new Node(76, last.parent);
            pendingFinish = false;
        } else if (prevNodeType == 64 && lastNodeType == 76) {
            last = last.parent;
            finish();
        } else if (prevNodeType == 64 && lastNodeType == 77) {
            last = new Node(78, last.parent);
            pendingFinish = false;
        } else if (prevNodeType == 64 && lastNodeType == 78) {
            last = new Node(72, last.parent);
            pendingFinish = true;
        } else if (prevNodeType == 64 && lastNodeType == 80) {
            last = new Node(82, last.parent);
            pendingFinish = false;
        } else if (prevNodeType == 64 && lastNodeType == 82) {
            last = new Node(72, last.parent);
            pendingFinish = true;
        } else if (prevNodeType == 68 && lastNodeType == 16) {
            last = last.parent;
            finish();
        } else if (prevNodeType == 75 && lastNodeType == 16) {
            last = last.parent;
            finish();
        } else if (prevNodeType == 76 && lastNodeType == 6) {
            last = new Node(61, last.parent);
            pendingFinish = false;
        } else if (prevNodeType == 76 && lastNodeType == 7) {
            last = new Node(71, last.parent);
            pendingFinish = true;
        } else if (prevNodeType == 76 && lastNodeType == 8) {
            last = new Node(55, last.parent);
            pendingFinish = false;
        } else if (prevNodeType == 76 && lastNodeType == 11) {
            last = new Node(12, last.parent);
            pendingFinish = false;
        } else if (prevNodeType == 76 && lastNodeType == 12) {
            last = new Node(13, last.parent);
            pendingFinish = false;
        } else if (prevNodeType == 76 && lastNodeType == 13) {
            last = new Node(14, last.parent);
            pendingFinish = false;
        } else if (prevNodeType == 76 && lastNodeType == 14) {
            last = new Node(72, last.parent);
            pendingFinish = true;
        } else if (prevNodeType == 76 && lastNodeType == 17) {
            last = new Node(51, last.parent);
            pendingFinish = false;
        } else if (prevNodeType == 76 && lastNodeType == 19) {
            last = new Node(72, last.parent);
            pendingFinish = true;
        } else if (prevNodeType == 76 && lastNodeType == 21) {
            last = new Node(22, last.parent);
            pendingFinish = false;
        } else if (prevNodeType == 76 && lastNodeType == 23) {
            last = new Node(24, last.parent);
            pendingFinish = false;
        } else if (prevNodeType == 76 && lastNodeType == 24) {
            last = new Node(72, last.parent);
            pendingFinish = true;
        } else if (prevNodeType == 76 && lastNodeType == 34) {
            last = new Node(72, last.parent);
            pendingFinish = true;
        } else if (prevNodeType == 76 && lastNodeType == 37) {
            last = new Node(74, last.parent);
            pendingFinish = true;
        } else if (prevNodeType == 76 && lastNodeType == 44) {
            last = new Node(67, last.parent);
            pendingFinish = true;
        } else if (prevNodeType == 76 && lastNodeType == 52) {
            last = new Node(72, last.parent);
            pendingFinish = true;
        } else if (prevNodeType == 76 && lastNodeType == 53) {
            last = new Node(24, last.parent);
            pendingFinish = false;
        } else if (prevNodeType == 76 && lastNodeType == 55) {
            last = new Node(56, last.parent);
            pendingFinish = false;
        } else if (prevNodeType == 76 && lastNodeType == 56) {
            last = new Node(72, last.parent);
            pendingFinish = true;
        } else if (prevNodeType == 76 && lastNodeType == 57) {
            last = new Node(67, last.parent);
            pendingFinish = true;
        } else if (prevNodeType == 76 && lastNodeType == 60) {
            last = new Node(67, last.parent);
            pendingFinish = true;
        } else if (prevNodeType == 76 && lastNodeType == 61) {
            last = new Node(62, last.parent);
            pendingFinish = false;
        } else if (prevNodeType == 76 && lastNodeType == 62) {
            last = new Node(72, last.parent);
            pendingFinish = true;
        } else if (prevNodeType == 76 && lastNodeType == 63) {
            last = new Node(79, last.parent);
            pendingFinish = false;
        } else if (prevNodeType == 76 && lastNodeType == 64) {
            last = new Node(70, last.parent);
            pendingFinish = false;
        } else if (prevNodeType == 76 && lastNodeType == 66) {
            last = new Node(72, last.parent);
            pendingFinish = true;
        } else if (prevNodeType == 76 && lastNodeType == 68) {
            last = new Node(69, last.parent);
            pendingFinish = false;
        } else if (prevNodeType == 76 && lastNodeType == 69) {
            last = new Node(73, last.parent);
            pendingFinish = false;
        } else if (prevNodeType == 76 && lastNodeType == 75) {
            last = new Node(76, last.parent);
            pendingFinish = false;
        } else if (prevNodeType == 76 && lastNodeType == 76) {
            last = last.parent;
            finish();
        } else if (prevNodeType == 76 && lastNodeType == 77) {
            last = new Node(78, last.parent);
            pendingFinish = false;
        } else if (prevNodeType == 76 && lastNodeType == 78) {
            last = new Node(72, last.parent);
            pendingFinish = true;
        } else if (prevNodeType == 76 && lastNodeType == 80) {
            last = new Node(82, last.parent);
            pendingFinish = false;
        } else if (prevNodeType == 76 && lastNodeType == 82) {
            last = new Node(72, last.parent);
            pendingFinish = true;
        } else if (prevNodeType == 77 && lastNodeType == 16) {
            last = last.parent;
            finish();
        } else if (prevNodeType == 80 && lastNodeType == 81) {
            last = new Node(83, last.parent);
            pendingFinish = true;
        } else if (prevNodeType == 80 && lastNodeType == 83) {
            last = last.parent;
            finish();
        } else if (prevNodeType == 81 && lastNodeType == 84) {
            last = new Node(85, last.parent);
            pendingFinish = false;
        } else if (prevNodeType == 81 && lastNodeType == 85) {
            last = new Node(86, last.parent);
            pendingFinish = false;
        } else if (prevNodeType == 81 && lastNodeType == 86) {
            last = new Node(87, last.parent);
            pendingFinish = false;
        } else if (prevNodeType == 81 && lastNodeType == 87) {
            last = last.parent;
            finish();
        } else if (prevNodeType == 83 && lastNodeType == 81) {
            last = new Node(83, last.parent);
            pendingFinish = true;
        } else if (prevNodeType == 83 && lastNodeType == 83) {
            last = last.parent;
            finish();
        } else if (prevNodeType == 88 && lastNodeType == 6) {
            last = new Node(61, last.parent);
            pendingFinish = false;
        } else if (prevNodeType == 88 && lastNodeType == 7) {
            last = new Node(58, last.parent);
            pendingFinish = true;
        } else if (prevNodeType == 88 && lastNodeType == 8) {
            last = new Node(55, last.parent);
            pendingFinish = false;
        } else if (prevNodeType == 88 && lastNodeType == 11) {
            last = new Node(12, last.parent);
            pendingFinish = false;
        } else if (prevNodeType == 88 && lastNodeType == 12) {
            last = new Node(13, last.parent);
            pendingFinish = false;
        } else if (prevNodeType == 88 && lastNodeType == 13) {
            last = new Node(14, last.parent);
            pendingFinish = false;
        } else if (prevNodeType == 88 && lastNodeType == 14) {
            last = last.parent;
            finish();
        } else if (prevNodeType == 88 && lastNodeType == 17) {
            last = new Node(51, last.parent);
            pendingFinish = false;
        } else if (prevNodeType == 88 && lastNodeType == 19) {
            last = last.parent;
            finish();
        } else if (prevNodeType == 88 && lastNodeType == 21) {
            last = new Node(22, last.parent);
            pendingFinish = false;
        } else if (prevNodeType == 88 && lastNodeType == 23) {
            last = new Node(24, last.parent);
            pendingFinish = false;
        } else if (prevNodeType == 88 && lastNodeType == 24) {
            last = last.parent;
            finish();
        } else if (prevNodeType == 88 && lastNodeType == 34) {
            last = last.parent;
            finish();
        } else if (prevNodeType == 88 && lastNodeType == 37) {
            last = new Node(34, last.parent);
            pendingFinish = true;
        } else if (prevNodeType == 88 && lastNodeType == 44) {
            last = new Node(33, last.parent);
            pendingFinish = true;
        } else if (prevNodeType == 88 && lastNodeType == 52) {
            last = last.parent;
            finish();
        } else if (prevNodeType == 88 && lastNodeType == 53) {
            last = new Node(24, last.parent);
            pendingFinish = false;
        } else if (prevNodeType == 88 && lastNodeType == 55) {
            last = new Node(56, last.parent);
            pendingFinish = false;
        } else if (prevNodeType == 88 && lastNodeType == 56) {
            last = last.parent;
            finish();
        } else if (prevNodeType == 88 && lastNodeType == 57) {
            last = new Node(33, last.parent);
            pendingFinish = true;
        } else if (prevNodeType == 88 && lastNodeType == 60) {
            last = new Node(33, last.parent);
            pendingFinish = true;
        } else if (prevNodeType == 88 && lastNodeType == 61) {
            last = new Node(62, last.parent);
            pendingFinish = false;
        } else if (prevNodeType == 88 && lastNodeType == 62) {
            last = last.parent;
            finish();
        } else if (prevNodeType == 88 && lastNodeType == 63) {
            last = new Node(79, last.parent);
            pendingFinish = false;
        } else if (prevNodeType == 88 && lastNodeType == 64) {
            last = new Node(70, last.parent);
            pendingFinish = false;
        } else if (prevNodeType == 88 && lastNodeType == 66) {
            last = last.parent;
            finish();
        } else if (prevNodeType == 88 && lastNodeType == 77) {
            last = new Node(78, last.parent);
            pendingFinish = false;
        } else if (prevNodeType == 88 && lastNodeType == 78) {
            last = last.parent;
            finish();
        } else if (prevNodeType == 88 && lastNodeType == 80) {
            last = new Node(82, last.parent);
            pendingFinish = false;
        } else if (prevNodeType == 88 && lastNodeType == 82) {
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
                    replace(16);
                    append(16, true);
                    return true;
                } else if ((next == '"')) {
                    replace(88);
                    append(3, false);
                    return true;
                } else if ((next == '-')) {
                    replace(88);
                    append(7, false);
                    return true;
                } else if ((next == '0')) {
                    replace(88);
                    append(33, true);
                    return true;
                } else if (('1' <= next && next <= '9')) {
                    replace(88);
                    append(32, true);
                    return true;
                } else if ((next == '[')) {
                    replace(88);
                    append(4, false);
                    return true;
                } else if ((next == 'f')) {
                    replace(88);
                    append(11, false);
                    return true;
                } else if ((next == 'n')) {
                    replace(88);
                    append(8, false);
                    return true;
                } else if ((next == 't')) {
                    replace(88);
                    append(6, false);
                    return true;
                } else if ((next == '{')) {
                    replace(88);
                    append(10, false);
                    return true;
                }
                break;
            case 3: // "*characters "|" characters*"
                if ((next == ' ') || ('0' <= next && next <= '9') || ('A' <= next && next <= 'Z') || ('a' <= next && next <= 'z')) {
                    replace(80);
                    append(83, true);
                    return true;
                } else if ((next == '"')) {
                    replace(82);
                    append(20, true);
                    return true;
                } else if ((next == '\\')) {
                    replace(80);
                    append(81, false);
                    return true;
                }
                break;
            case 4: // [*ws elements ws ]|[ ws*elements ws ]|[*ws ]|[ ws*]
                if (('\t' <= next && next <= '\n') || (next == '\r') || (next == ' ')) {
                    replace(63);
                    append(16, true);
                    return true;
                } else if ((next == '"')) {
                    replace(64);
                    append(3, false);
                    return true;
                } else if ((next == '-')) {
                    replace(64);
                    append(7, false);
                    return true;
                } else if ((next == '0')) {
                    replace(64);
                    append(67, true);
                    return true;
                } else if (('1' <= next && next <= '9')) {
                    replace(64);
                    append(65, true);
                    return true;
                } else if ((next == '[')) {
                    replace(64);
                    append(63, true);
                    return true;
                } else if ((next == ']')) {
                    replace(66);
                    append(20, true);
                    return true;
                } else if ((next == 'f')) {
                    replace(64);
                    append(11, false);
                    return true;
                } else if ((next == 'n')) {
                    replace(64);
                    append(8, false);
                    return true;
                } else if ((next == 't')) {
                    replace(64);
                    append(6, false);
                    return true;
                } else if ((next == '{')) {
                    replace(64);
                    append(10, false);
                    return true;
                }
                break;
            case 5: // onenine*digits|int*frac exp|int frac*exp|ws element*ws
                if (('\t' <= next && next <= '\n') || (next == '\r') || (next == ' ')) {
                    replace(15);
                    append(16, true);
                    return true;
                } else if ((next == '.')) {
                    replace(37);
                    append(38, false);
                    return true;
                } else if ((next == '0')) {
                    replace(44);
                    append(45, true);
                    return true;
                } else if (('1' <= next && next <= '9')) {
                    replace(44);
                    append(45, true);
                    return true;
                } else if ((next == 'E')) {
                    replace(34);
                    append(36, false);
                    return true;
                } else if ((next == 'e')) {
                    replace(34);
                    append(35, false);
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
                    replace(57);
                    append(20, true);
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
                    replace(15);
                    append(16, true);
                    return true;
                } else if ((next == '.')) {
                    replace(37);
                    append(38, false);
                    return true;
                } else if ((next == 'E')) {
                    replace(34);
                    append(36, false);
                    return true;
                } else if ((next == 'e')) {
                    replace(34);
                    append(35, false);
                    return true;
                }
                break;
            case 10: // {*ws }|{ ws*}|{*ws members ws }|{ ws*members ws }
                if (('\t' <= next && next <= '\n') || (next == '\r') || (next == ' ')) {
                    replace(17);
                    append(18, true);
                    return true;
                } else if ((next == '"')) {
                    replace(21);
                    append(3, false);
                    return true;
                } else if ((next == '}')) {
                    replace(19);
                    append(20, true);
                    return true;
                }
                break;
            case 11: // f*a l s e
                if ((next == 'a')) {
                    finish();
                    return true;
                }
                break;
            case 12: // f a*l s e
                if ((next == 'l')) {
                    finish();
                    return true;
                }
                break;
            case 13: // f a l*s e
                if ((next == 's')) {
                    finish();
                    return true;
                }
                break;
            case 14: // f a l s*e
                if ((next == 'e')) {
                    finish();
                    return true;
                }
                break;
            case 15: // ws element*ws
                if (('\t' <= next && next <= '\n') || (next == '\r') || (next == ' ')) {
                    append(16, true);
                    return true;
                }
                break;
            case 16: // {\t-\n\r\u0020}*ws
                if (('\t' <= next && next <= '\n') || (next == '\r') || (next == ' ')) {
                    append(16, true);
                    return true;
                }
                break;
            case 17: // {*ws }|{*ws members ws }|{ ws*members ws }
                if (('\t' <= next && next <= '\n') || (next == '\r') || (next == ' ')) {
                    append(18, true);
                    return true;
                } else if ((next == '"')) {
                    replace(21);
                    append(3, false);
                    return true;
                }
                break;
            case 18: // {\t-\n\r\u0020}*ws|ws*string ws : ws element
                if (('\t' <= next && next <= '\n') || (next == '\r') || (next == ' ')) {
                    replace(16);
                    append(16, true);
                    return true;
                } else if ((next == '"')) {
                    replace(25);
                    append(3, false);
                    return true;
                }
                break;
            case 19: // { ws*}
                if ((next == '}')) {
                    finish();
                    return true;
                }
                break;
            case 21: // { ws*members ws }
                if (('\t' <= next && next <= '\n') || (next == '\r') || (next == ' ')) {
                    append(18, false);
                    return true;
                } else if ((next == '"')) {
                    append(3, false);
                    return true;
                }
                break;
            case 22: // { ws members*ws }|{ ws members ws*}
                if (('\t' <= next && next <= '\n') || (next == '\r') || (next == ' ')) {
                    replace(23);
                    append(16, true);
                    return true;
                } else if ((next == '}')) {
                    replace(24);
                    append(20, true);
                    return true;
                }
                break;
            case 23: // { ws members*ws }
                if (('\t' <= next && next <= '\n') || (next == '\r') || (next == ' ')) {
                    append(16, true);
                    return true;
                }
                break;
            case 24: // { ws members ws*}
                if ((next == '}')) {
                    finish();
                    return true;
                }
                break;
            case 25: // ws*string ws : ws element
                if ((next == '"')) {
                    append(3, false);
                    return true;
                }
                break;
            case 26: // ws string*ws : ws element|ws string ws*: ws element
                if (('\t' <= next && next <= '\n') || (next == '\r') || (next == ' ')) {
                    replace(27);
                    append(16, true);
                    return true;
                } else if ((next == ':')) {
                    replace(28);
                    append(20, true);
                    return true;
                }
                break;
            case 27: // ws string*ws : ws element
                if (('\t' <= next && next <= '\n') || (next == '\r') || (next == ' ')) {
                    append(16, true);
                    return true;
                }
                break;
            case 28: // ws string ws*: ws element
                if ((next == ':')) {
                    finish();
                    return true;
                }
                break;
            case 29: // ws string ws :*ws element|ws string ws : ws*element
                if (('\t' <= next && next <= '\n') || (next == '\r') || (next == ' ')) {
                    replace(30);
                    append(16, true);
                    return true;
                } else if ((next == '"')) {
                    replace(31);
                    append(3, false);
                    return true;
                } else if ((next == '-')) {
                    replace(31);
                    append(7, false);
                    return true;
                } else if ((next == '0')) {
                    replace(31);
                    append(33, true);
                    return true;
                } else if (('1' <= next && next <= '9')) {
                    replace(31);
                    append(32, true);
                    return true;
                } else if ((next == '[')) {
                    replace(31);
                    append(4, false);
                    return true;
                } else if ((next == 'f')) {
                    replace(31);
                    append(11, false);
                    return true;
                } else if ((next == 'n')) {
                    replace(31);
                    append(8, false);
                    return true;
                } else if ((next == 't')) {
                    replace(31);
                    append(6, false);
                    return true;
                } else if ((next == '{')) {
                    replace(31);
                    append(10, false);
                    return true;
                }
                break;
            case 30: // ws string ws :*ws element
                if (('\t' <= next && next <= '\n') || (next == '\r') || (next == ' ')) {
                    append(16, true);
                    return true;
                }
                break;
            case 31: // ws string ws : ws*element
                if ((next == '"')) {
                    append(3, false);
                    return true;
                } else if ((next == '-')) {
                    append(7, false);
                    return true;
                } else if ((next == '0')) {
                    append(33, true);
                    return true;
                } else if (('1' <= next && next <= '9')) {
                    append(32, true);
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
            case 32: // onenine*digits|int*frac exp|int frac*exp
                if ((next == '.')) {
                    replace(37);
                    append(38, false);
                    return true;
                } else if ((next == '0')) {
                    replace(44);
                    append(45, true);
                    return true;
                } else if (('1' <= next && next <= '9')) {
                    replace(44);
                    append(45, true);
                    return true;
                } else if ((next == 'E')) {
                    replace(34);
                    append(36, false);
                    return true;
                } else if ((next == 'e')) {
                    replace(34);
                    append(35, false);
                    return true;
                }
                break;
            case 33: // int*frac exp|int frac*exp
                if ((next == '.')) {
                    replace(37);
                    append(38, false);
                    return true;
                } else if ((next == 'E')) {
                    replace(34);
                    append(36, false);
                    return true;
                } else if ((next == 'e')) {
                    replace(34);
                    append(35, false);
                    return true;
                }
                break;
            case 34: // int frac*exp
                if ((next == 'E')) {
                    append(36, false);
                    return true;
                } else if ((next == 'e')) {
                    append(35, false);
                    return true;
                }
                break;
            case 35: // e*sign {0-9}[1-]|e sign*{0-9}[1-]
                if ((next == '+')) {
                    replace(42);
                    append(20, true);
                    return true;
                } else if ((next == '-')) {
                    replace(42);
                    append(20, true);
                    return true;
                } else if (('0' <= next && next <= '9')) {
                    replace(43);
                    append(39, true);
                    return true;
                }
                break;
            case 36: // E*sign {0-9}[1-]|E sign*{0-9}[1-]
                if ((next == '+')) {
                    replace(40);
                    append(20, true);
                    return true;
                } else if ((next == '-')) {
                    replace(40);
                    append(20, true);
                    return true;
                } else if (('0' <= next && next <= '9')) {
                    replace(41);
                    append(39, true);
                    return true;
                }
                break;
            case 37: // int*frac exp
                if ((next == '.')) {
                    append(38, false);
                    return true;
                }
                break;
            case 38: // .*{0-9}[1-]
                if (('0' <= next && next <= '9')) {
                    append(39, true);
                    return true;
                }
                break;
            case 39: // {0-9}[1-]*{0-9}
                if (('0' <= next && next <= '9')) {
                    finish();
                    return true;
                }
                break;
            case 40: // E*sign {0-9}[1-]
                if ((next == '+')) {
                    finish();
                    return true;
                } else if ((next == '-')) {
                    finish();
                    return true;
                }
                break;
            case 41: // E sign*{0-9}[1-]
                if (('0' <= next && next <= '9')) {
                    append(39, true);
                    return true;
                }
                break;
            case 42: // e*sign {0-9}[1-]
                if ((next == '+')) {
                    finish();
                    return true;
                } else if ((next == '-')) {
                    finish();
                    return true;
                }
                break;
            case 43: // e sign*{0-9}[1-]
                if (('0' <= next && next <= '9')) {
                    append(39, true);
                    return true;
                }
                break;
            case 44: // onenine*digits
                if ((next == '0')) {
                    append(45, true);
                    return true;
                } else if (('1' <= next && next <= '9')) {
                    append(45, true);
                    return true;
                }
                break;
            case 45: // digit*digits
                if ((next == '0')) {
                    append(45, true);
                    return true;
                } else if (('1' <= next && next <= '9')) {
                    append(45, true);
                    return true;
                }
                break;
            case 46: // member*ws , ws members|member ws*, ws members
                if (('\t' <= next && next <= '\n') || (next == '\r') || (next == ' ')) {
                    replace(47);
                    append(16, true);
                    return true;
                } else if ((next == ',')) {
                    replace(48);
                    append(20, true);
                    return true;
                }
                break;
            case 47: // member*ws , ws members
                if (('\t' <= next && next <= '\n') || (next == '\r') || (next == ' ')) {
                    append(16, true);
                    return true;
                }
                break;
            case 48: // member ws*, ws members
                if ((next == ',')) {
                    finish();
                    return true;
                }
                break;
            case 49: // member ws ,*ws members|member ws , ws*members
                if (('\t' <= next && next <= '\n') || (next == '\r') || (next == ' ')) {
                    append(18, true);
                    return true;
                } else if ((next == '"')) {
                    replace(50);
                    append(3, false);
                    return true;
                }
                break;
            case 50: // member ws , ws*members
                if (('\t' <= next && next <= '\n') || (next == '\r') || (next == ' ')) {
                    append(18, false);
                    return true;
                } else if ((next == '"')) {
                    append(3, false);
                    return true;
                }
                break;
            case 51: // { ws*}|{ ws*members ws }|{ ws members*ws }|{ ws members ws*}
                if (('\t' <= next && next <= '\n') || (next == '\r') || (next == ' ')) {
                    replace(53);
                    append(18, true);
                    return true;
                } else if ((next == '"')) {
                    replace(21);
                    append(3, false);
                    return true;
                } else if ((next == '}')) {
                    replace(52);
                    append(20, true);
                    return true;
                }
                break;
            case 52: // { ws*}|{ ws members ws*}
                if ((next == '}')) {
                    finish();
                    return true;
                }
                break;
            case 53: // { ws*members ws }|{ ws members*ws }
                if (('\t' <= next && next <= '\n') || (next == '\r') || (next == ' ')) {
                    append(18, true);
                    return true;
                } else if ((next == '"')) {
                    replace(21);
                    append(3, false);
                    return true;
                }
                break;
            case 54: // int frac*exp|ws element*ws
                if (('\t' <= next && next <= '\n') || (next == '\r') || (next == ' ')) {
                    replace(15);
                    append(16, true);
                    return true;
                } else if ((next == 'E')) {
                    replace(34);
                    append(36, false);
                    return true;
                } else if ((next == 'e')) {
                    replace(34);
                    append(35, false);
                    return true;
                }
                break;
            case 55: // n u*l l
                if ((next == 'l')) {
                    finish();
                    return true;
                }
                break;
            case 56: // n u l*l
                if ((next == 'l')) {
                    finish();
                    return true;
                }
                break;
            case 57: // -*digit
                if ((next == '0')) {
                    finish();
                    return true;
                } else if (('1' <= next && next <= '9')) {
                    finish();
                    return true;
                }
                break;
            case 58: // - onenine*digits|int*frac exp|int frac*exp
                if ((next == '.')) {
                    replace(37);
                    append(38, false);
                    return true;
                } else if ((next == '0')) {
                    replace(60);
                    append(45, true);
                    return true;
                } else if (('1' <= next && next <= '9')) {
                    replace(60);
                    append(45, true);
                    return true;
                } else if ((next == 'E')) {
                    replace(34);
                    append(36, false);
                    return true;
                } else if ((next == 'e')) {
                    replace(34);
                    append(35, false);
                    return true;
                }
                break;
            case 59: // - onenine*digits|int*frac exp|int frac*exp|ws element*ws
                if (('\t' <= next && next <= '\n') || (next == '\r') || (next == ' ')) {
                    replace(15);
                    append(16, true);
                    return true;
                } else if ((next == '.')) {
                    replace(37);
                    append(38, false);
                    return true;
                } else if ((next == '0')) {
                    replace(60);
                    append(45, true);
                    return true;
                } else if (('1' <= next && next <= '9')) {
                    replace(60);
                    append(45, true);
                    return true;
                } else if ((next == 'E')) {
                    replace(34);
                    append(36, false);
                    return true;
                } else if ((next == 'e')) {
                    replace(34);
                    append(35, false);
                    return true;
                }
                break;
            case 60: // - onenine*digits
                if ((next == '0')) {
                    append(45, true);
                    return true;
                } else if (('1' <= next && next <= '9')) {
                    append(45, true);
                    return true;
                }
                break;
            case 61: // t r*u e
                if ((next == 'u')) {
                    finish();
                    return true;
                }
                break;
            case 62: // t r u*e
                if ((next == 'e')) {
                    finish();
                    return true;
                }
                break;
            case 63: // [*ws elements ws ]|[*ws ]
                if (('\t' <= next && next <= '\n') || (next == '\r') || (next == ' ')) {
                    append(16, true);
                    return true;
                }
                break;
            case 64: // [ ws*elements ws ]
                if ((next == '"')) {
                    append(3, false);
                    return true;
                } else if ((next == '-')) {
                    append(7, false);
                    return true;
                } else if ((next == '0')) {
                    append(67, true);
                    return true;
                } else if (('1' <= next && next <= '9')) {
                    append(65, true);
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
            case 65: // int frac*exp|int*frac exp|onenine*digits|element ws*, ws elements|element*ws , ws elements
                if (('\t' <= next && next <= '\n') || (next == '\r') || (next == ' ')) {
                    replace(68);
                    append(16, true);
                    return true;
                } else if ((next == ',')) {
                    replace(69);
                    append(20, true);
                    return true;
                } else if ((next == '.')) {
                    replace(37);
                    append(38, false);
                    return true;
                } else if ((next == '0')) {
                    replace(44);
                    append(45, true);
                    return true;
                } else if (('1' <= next && next <= '9')) {
                    replace(44);
                    append(45, true);
                    return true;
                } else if ((next == 'E')) {
                    replace(34);
                    append(36, false);
                    return true;
                } else if ((next == 'e')) {
                    replace(34);
                    append(35, false);
                    return true;
                }
                break;
            case 66: // [ ws*]
                if ((next == ']')) {
                    finish();
                    return true;
                }
                break;
            case 67: // int*frac exp|int frac*exp|element*ws , ws elements|element ws*, ws elements
                if (('\t' <= next && next <= '\n') || (next == '\r') || (next == ' ')) {
                    replace(68);
                    append(16, true);
                    return true;
                } else if ((next == ',')) {
                    replace(69);
                    append(20, true);
                    return true;
                } else if ((next == '.')) {
                    replace(37);
                    append(38, false);
                    return true;
                } else if ((next == 'E')) {
                    replace(34);
                    append(36, false);
                    return true;
                } else if ((next == 'e')) {
                    replace(34);
                    append(35, false);
                    return true;
                }
                break;
            case 68: // element*ws , ws elements
                if (('\t' <= next && next <= '\n') || (next == '\r') || (next == ' ')) {
                    append(16, true);
                    return true;
                }
                break;
            case 69: // element ws*, ws elements
                if ((next == ',')) {
                    finish();
                    return true;
                }
                break;
            case 70: // [ ws elements*ws ]|[ ws elements ws*]
                if (('\t' <= next && next <= '\n') || (next == '\r') || (next == ' ')) {
                    replace(77);
                    append(16, true);
                    return true;
                } else if ((next == ']')) {
                    replace(78);
                    append(20, true);
                    return true;
                }
                break;
            case 71: // - onenine*digits|int frac*exp|int*frac exp|element ws*, ws elements|element*ws , ws elements
                if (('\t' <= next && next <= '\n') || (next == '\r') || (next == ' ')) {
                    replace(68);
                    append(16, true);
                    return true;
                } else if ((next == ',')) {
                    replace(69);
                    append(20, true);
                    return true;
                } else if ((next == '.')) {
                    replace(37);
                    append(38, false);
                    return true;
                } else if ((next == '0')) {
                    replace(60);
                    append(45, true);
                    return true;
                } else if (('1' <= next && next <= '9')) {
                    replace(60);
                    append(45, true);
                    return true;
                } else if ((next == 'E')) {
                    replace(34);
                    append(36, false);
                    return true;
                } else if ((next == 'e')) {
                    replace(34);
                    append(35, false);
                    return true;
                }
                break;
            case 72: // element*ws , ws elements|element ws*, ws elements
                if (('\t' <= next && next <= '\n') || (next == '\r') || (next == ' ')) {
                    replace(68);
                    append(16, true);
                    return true;
                } else if ((next == ',')) {
                    replace(69);
                    append(20, true);
                    return true;
                }
                break;
            case 73: // element ws ,*ws elements|element ws , ws*elements
                if (('\t' <= next && next <= '\n') || (next == '\r') || (next == ' ')) {
                    replace(75);
                    append(16, true);
                    return true;
                } else if ((next == '"')) {
                    replace(76);
                    append(3, false);
                    return true;
                } else if ((next == '-')) {
                    replace(76);
                    append(7, false);
                    return true;
                } else if ((next == '0')) {
                    replace(76);
                    append(67, true);
                    return true;
                } else if (('1' <= next && next <= '9')) {
                    replace(76);
                    append(65, true);
                    return true;
                } else if ((next == '[')) {
                    replace(76);
                    append(4, false);
                    return true;
                } else if ((next == 'f')) {
                    replace(76);
                    append(11, false);
                    return true;
                } else if ((next == 'n')) {
                    replace(76);
                    append(8, false);
                    return true;
                } else if ((next == 't')) {
                    replace(76);
                    append(6, false);
                    return true;
                } else if ((next == '{')) {
                    replace(76);
                    append(10, false);
                    return true;
                }
                break;
            case 74: // int frac*exp|element*ws , ws elements|element ws*, ws elements
                if (('\t' <= next && next <= '\n') || (next == '\r') || (next == ' ')) {
                    replace(68);
                    append(16, true);
                    return true;
                } else if ((next == ',')) {
                    replace(69);
                    append(20, true);
                    return true;
                } else if ((next == 'E')) {
                    replace(34);
                    append(36, false);
                    return true;
                } else if ((next == 'e')) {
                    replace(34);
                    append(35, false);
                    return true;
                }
                break;
            case 75: // element ws ,*ws elements
                if (('\t' <= next && next <= '\n') || (next == '\r') || (next == ' ')) {
                    append(16, true);
                    return true;
                }
                break;
            case 76: // element ws , ws*elements
                if ((next == '"')) {
                    append(3, false);
                    return true;
                } else if ((next == '-')) {
                    append(7, false);
                    return true;
                } else if ((next == '0')) {
                    append(67, true);
                    return true;
                } else if (('1' <= next && next <= '9')) {
                    append(65, true);
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
            case 77: // [ ws elements*ws ]
                if (('\t' <= next && next <= '\n') || (next == '\r') || (next == ' ')) {
                    append(16, true);
                    return true;
                }
                break;
            case 78: // [ ws elements ws*]
                if ((next == ']')) {
                    finish();
                    return true;
                }
                break;
            case 79: // [ ws*elements ws ]|[ ws*]
                if ((next == '"')) {
                    replace(64);
                    append(3, false);
                    return true;
                } else if ((next == '-')) {
                    replace(64);
                    append(7, false);
                    return true;
                } else if ((next == '0')) {
                    replace(64);
                    append(67, true);
                    return true;
                } else if (('1' <= next && next <= '9')) {
                    replace(64);
                    append(65, true);
                    return true;
                } else if ((next == '[')) {
                    replace(64);
                    append(4, false);
                    return true;
                } else if ((next == ']')) {
                    replace(66);
                    append(20, true);
                    return true;
                } else if ((next == 'f')) {
                    replace(64);
                    append(11, false);
                    return true;
                } else if ((next == 'n')) {
                    replace(64);
                    append(8, false);
                    return true;
                } else if ((next == 't')) {
                    replace(64);
                    append(6, false);
                    return true;
                } else if ((next == '{')) {
                    replace(64);
                    append(10, false);
                    return true;
                }
                break;
            case 80: // "*characters "
                if ((next == ' ') || ('0' <= next && next <= '9') || ('A' <= next && next <= 'Z') || ('a' <= next && next <= 'z')) {
                    append(83, true);
                    return true;
                } else if ((next == '\\')) {
                    append(81, false);
                    return true;
                }
                break;
            case 81: // \*escape
                if ((next == '"') || (next == '/') || (next == '\\') || (next == 'b') || (next == 'n') || (next == 'r') || (next == 't')) {
                    finish();
                    return true;
                } else if ((next == 'u')) {
                    append(84, false);
                    return true;
                }
                break;
            case 82: // " characters*"
                if ((next == '"')) {
                    finish();
                    return true;
                }
                break;
            case 83: // character*characters
                if ((next == ' ') || ('0' <= next && next <= '9') || ('A' <= next && next <= 'Z') || ('a' <= next && next <= 'z')) {
                    append(83, true);
                    return true;
                } else if ((next == '\\')) {
                    append(81, false);
                    return true;
                }
                break;
            case 84: // u*hex hex hex hex
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
            case 85: // u hex*hex hex hex
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
            case 86: // u hex hex*hex hex
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
            case 87: // u hex hex hex*hex
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
            case 88: // ws*element ws
                if ((next == '"')) {
                    append(3, false);
                    return true;
                } else if ((next == '-')) {
                    append(7, false);
                    return true;
                } else if ((next == '0')) {
                    append(33, true);
                    return true;
                } else if (('1' <= next && next <= '9')) {
                    append(32, true);
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
                return "{{\\\\t-\\\\n\\\\r\\\\u0020}•ws|ws•element ws}";
            case 3:
                return "{\\\"•characters \\\"|\\\" characters•\\\"}";
            case 4:
                return "{[•ws elements ws ]|[ ws•elements ws ]|[•ws ]|[ ws•]}";
            case 5:
                return "{onenine•digits|int•frac exp|int frac•exp|ws element•ws}";
            case 6:
                return "{t•r u e}";
            case 7:
                return "{-•digit|-•onenine digits}";
            case 8:
                return "{n•u l l}";
            case 9:
                return "{int•frac exp|int frac•exp|ws element•ws}";
            case 10:
                return "{{•ws }|{ ws•}|{•ws members ws }|{ ws•members ws }}";
            case 11:
                return "{f•a l s e}";
            case 12:
                return "{f a•l s e}";
            case 13:
                return "{f a l•s e}";
            case 14:
                return "{f a l s•e}";
            case 15:
                return "{ws element•ws}";
            case 16:
                return "{{\\\\t-\\\\n\\\\r\\\\u0020}•ws}";
            case 17:
                return "{{•ws }|{•ws members ws }|{ ws•members ws }}";
            case 18:
                return "{{\\\\t-\\\\n\\\\r\\\\u0020}•ws|ws•string ws : ws element}";
            case 19:
                return "{{ ws•}}";
            case 20:
                return "{}";
            case 21:
                return "{{ ws•members ws }}";
            case 22:
                return "{{ ws members•ws }|{ ws members ws•}}";
            case 23:
                return "{{ ws members•ws }}";
            case 24:
                return "{{ ws members ws•}}";
            case 25:
                return "{ws•string ws : ws element}";
            case 26:
                return "{ws string•ws : ws element|ws string ws•: ws element}";
            case 27:
                return "{ws string•ws : ws element}";
            case 28:
                return "{ws string ws•: ws element}";
            case 29:
                return "{ws string ws :•ws element|ws string ws : ws•element}";
            case 30:
                return "{ws string ws :•ws element}";
            case 31:
                return "{ws string ws : ws•element}";
            case 32:
                return "{onenine•digits|int•frac exp|int frac•exp}";
            case 33:
                return "{int•frac exp|int frac•exp}";
            case 34:
                return "{int frac•exp}";
            case 35:
                return "{e•sign {0-9}[1-]|e sign•{0-9}[1-]}";
            case 36:
                return "{E•sign {0-9}[1-]|E sign•{0-9}[1-]}";
            case 37:
                return "{int•frac exp}";
            case 38:
                return "{.•{0-9}[1-]}";
            case 39:
                return "{{0-9}[1-]•{0-9}}";
            case 40:
                return "{E•sign {0-9}[1-]}";
            case 41:
                return "{E sign•{0-9}[1-]}";
            case 42:
                return "{e•sign {0-9}[1-]}";
            case 43:
                return "{e sign•{0-9}[1-]}";
            case 44:
                return "{onenine•digits}";
            case 45:
                return "{digit•digits}";
            case 46:
                return "{member•ws , ws members|member ws•, ws members}";
            case 47:
                return "{member•ws , ws members}";
            case 48:
                return "{member ws•, ws members}";
            case 49:
                return "{member ws ,•ws members|member ws , ws•members}";
            case 50:
                return "{member ws , ws•members}";
            case 51:
                return "{{ ws•}|{ ws•members ws }|{ ws members•ws }|{ ws members ws•}}";
            case 52:
                return "{{ ws•}|{ ws members ws•}}";
            case 53:
                return "{{ ws•members ws }|{ ws members•ws }}";
            case 54:
                return "{int frac•exp|ws element•ws}";
            case 55:
                return "{n u•l l}";
            case 56:
                return "{n u l•l}";
            case 57:
                return "{-•digit}";
            case 58:
                return "{- onenine•digits|int•frac exp|int frac•exp}";
            case 59:
                return "{- onenine•digits|int•frac exp|int frac•exp|ws element•ws}";
            case 60:
                return "{- onenine•digits}";
            case 61:
                return "{t r•u e}";
            case 62:
                return "{t r u•e}";
            case 63:
                return "{[•ws elements ws ]|[•ws ]}";
            case 64:
                return "{[ ws•elements ws ]}";
            case 65:
                return "{int frac•exp|element•ws , ws elements|int•frac exp|onenine•digits|element ws•, ws elements}";
            case 66:
                return "{[ ws•]}";
            case 67:
                return "{int•frac exp|int frac•exp|element•ws , ws elements|element ws•, ws elements}";
            case 68:
                return "{element•ws , ws elements}";
            case 69:
                return "{element ws•, ws elements}";
            case 70:
                return "{[ ws elements•ws ]|[ ws elements ws•]}";
            case 71:
                return "{- onenine•digits|int frac•exp|element•ws , ws elements|int•frac exp|element ws•, ws elements}";
            case 72:
                return "{element•ws , ws elements|element ws•, ws elements}";
            case 73:
                return "{element ws ,•ws elements|element ws , ws•elements}";
            case 74:
                return "{int frac•exp|element•ws , ws elements|element ws•, ws elements}";
            case 75:
                return "{element ws ,•ws elements}";
            case 76:
                return "{element ws , ws•elements}";
            case 77:
                return "{[ ws elements•ws ]}";
            case 78:
                return "{[ ws elements ws•]}";
            case 79:
                return "{[ ws•elements ws ]|[ ws•]}";
            case 80:
                return "{\\\"•characters \\\"}";
            case 81:
                return "{\\\\•escape}";
            case 82:
                return "{\\\" characters•\\\"}";
            case 83:
                return "{character•characters}";
            case 84:
                return "{u•hex hex hex hex}";
            case 85:
                return "{u hex•hex hex hex}";
            case 86:
                return "{u hex hex•hex hex}";
            case 87:
                return "{u hex hex hex•hex}";
            case 88:
                return "{ws•element ws}";
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
        GeneratedJsonParser parser = new GeneratedJsonParser();
        if (parser.proceed("{\"abcdefg\": [1, 2, 3, 4, 567e89]}")) {
            boolean result = parser.eof();
            System.out.println(result);
        } else {
            System.out.println("Failed");
        }
    }
}
