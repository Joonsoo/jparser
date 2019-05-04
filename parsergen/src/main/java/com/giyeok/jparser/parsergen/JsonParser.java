package com.giyeok.jparser.parsergen;

public class JsonParser {
  static class Stack {
    final int nodeId;
    final Stack prev;

    Stack(int nodeId, Stack prev) {
      this.nodeId = nodeId;
      this.prev = prev;
    }
  }

  private boolean verbose;
  private Stack stack;
  private int pendingFinish;

  public JsonParser(boolean verbose) {
    this.verbose = verbose;
    this.stack = new Stack(0, null);
    this.pendingFinish = -1;
  }

  public boolean canAccept(char c) {
    if (stack == null) return false;
    switch (stack.nodeId) {
      case 0:
        return ('\t' <= c && c <= '\n')
            || (c == '\r')
            || (c == ' ')
            || (c == '"')
            || (c == '-')
            || (c == '0')
            || ('1' <= c && c <= '9')
            || (c == '[')
            || (c == 'f')
            || (c == 'n')
            || (c == 't')
            || (c == '{');
      case 1:
        return ('\t' <= c && c <= '\n')
            || (c == '\r')
            || (c == ' ')
            || (c == '"')
            || (c == '-')
            || (c == '0')
            || ('1' <= c && c <= '9')
            || (c == '[')
            || (c == 'f')
            || (c == 'n')
            || (c == 't')
            || (c == '{');
      case 2:
        return (c == ' ')
            || ('0' <= c && c <= '9')
            || ('A' <= c && c <= 'Z')
            || ('a' <= c && c <= 'z')
            || (c == '"')
            || (c == '\\');
      case 3:
        return ('\t' <= c && c <= '\n')
            || (c == '\r')
            || (c == ' ')
            || (c == '"')
            || (c == '-')
            || (c == '0')
            || ('1' <= c && c <= '9')
            || (c == '[')
            || (c == ']')
            || (c == 'f')
            || (c == 'n')
            || (c == 't')
            || (c == '{');
      case 4:
        return ('\t' <= c && c <= '\n')
            || (c == '\r')
            || (c == ' ')
            || (c == '.')
            || (c == '0')
            || ('1' <= c && c <= '9')
            || (c == 'E')
            || (c == 'e');
      case 5:
        return (c == 'r');
      case 6:
        return (c == '0') || ('1' <= c && c <= '9');
      case 7:
        return (c == 'u');
      case 8:
        return ('\t' <= c && c <= '\n')
            || (c == '\r')
            || (c == ' ')
            || (c == '.')
            || (c == 'E')
            || (c == 'e');
      case 9:
        return ('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ') || (c == '"') || (c == '}');
      case 10:
        return (c == 'a');
      case 11:
        return ('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ');
      case 12:
        return (c == '"')
            || (c == '-')
            || (c == '0')
            || ('1' <= c && c <= '9')
            || (c == '[')
            || (c == 'f')
            || (c == 'n')
            || (c == 't')
            || (c == '{');
      case 13:
        return (c == '.') || (c == '0') || ('1' <= c && c <= '9') || (c == 'E') || (c == 'e');
      case 14:
        return (c == '.') || (c == 'E') || (c == 'e');
      case 15:
        return ('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ');
      case 16:
        return (c == '.') || (c == 'E') || (c == 'e');
      case 17:
        return ('0' <= c && c <= '9');
      case 18:
        return (c == 'E') || (c == 'e');
      case 19:
        return (c == '+') || (c == '-') || ('0' <= c && c <= '9');
      case 20:
        return (c == '+') || (c == '-') || ('0' <= c && c <= '9');
      case 21:
        return (c == ' ')
            || ('0' <= c && c <= '9')
            || ('A' <= c && c <= 'Z')
            || ('a' <= c && c <= 'z')
            || (c == '"')
            || (c == '\\');
      case 22:
        return (c == '"')
            || (c == '/')
            || (c == '\\')
            || (c == 'b')
            || (c == 'n')
            || (c == 'r')
            || (c == 't')
            || (c == 'u');
      case 23:
        return (c == '"');
      case 24:
        return (c == ' ')
            || ('0' <= c && c <= '9')
            || ('A' <= c && c <= 'Z')
            || ('a' <= c && c <= 'z')
            || (c == '\\');
      case 25:
        return (c == '0') || ('1' <= c && c <= '9');
      case 26:
        return (c == '0') || ('1' <= c && c <= '9');
      case 27:
        return ('\t' <= c && c <= '\n')
            || (c == '\r')
            || (c == ' ')
            || (c == '"')
            || (c == '-')
            || (c == '0')
            || ('1' <= c && c <= '9')
            || (c == '[')
            || (c == ']')
            || (c == 'f')
            || (c == 'n')
            || (c == 't')
            || (c == '{');
      case 28:
        return (c == '"')
            || (c == '-')
            || (c == '0')
            || ('1' <= c && c <= '9')
            || (c == '[')
            || (c == 'f')
            || (c == 'n')
            || (c == 't')
            || (c == '{');
      case 29:
        return ('\t' <= c && c <= '\n')
            || (c == '\r')
            || (c == ' ')
            || (c == ',')
            || (c == '.')
            || (c == '0')
            || ('1' <= c && c <= '9')
            || (c == 'E')
            || (c == 'e');
      case 30:
        return (c == ']');
      case 31:
        return ('\t' <= c && c <= '\n')
            || (c == '\r')
            || (c == ' ')
            || (c == ',')
            || (c == '.')
            || (c == 'E')
            || (c == 'e');
      case 32:
        return (c == '0') || ('1' <= c && c <= '9');
      case 33:
        return ('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ') || (c == '"') || (c == '}');
      case 34:
        return ('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ') || (c == '"');
      case 35:
        return ('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ') || (c == '"') || (c == '}');
      case 36:
        return ('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ') || (c == '"');
      case 37:
        return (c == '}');
      case 38:
        return ('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ') || (c == '"') || (c == '}');
      case 39:
        return ('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ') || (c == 'E') || (c == 'e');
      case 40:
        return (c == 'l');
      case 41:
        return ('\t' <= c && c <= '\n')
            || (c == '\r')
            || (c == ' ')
            || (c == '.')
            || (c == '0')
            || ('1' <= c && c <= '9')
            || (c == 'E')
            || (c == 'e');
      case 42:
        return (c == 'u');
      case 43:
        return (c == 'l');
      case 44:
        return ('\t' <= c && c <= '\n')
            || (c == '\r')
            || (c == ' ')
            || (c == '"')
            || (c == '-')
            || (c == '0')
            || ('1' <= c && c <= '9')
            || (c == '[')
            || (c == ']')
            || (c == 'f')
            || (c == 'n')
            || (c == 't')
            || (c == '{');
      case 45:
        return ('\t' <= c && c <= '\n')
            || (c == '\r')
            || (c == ' ')
            || (c == '"')
            || (c == '-')
            || (c == '0')
            || ('1' <= c && c <= '9')
            || (c == '[')
            || (c == 'f')
            || (c == 'n')
            || (c == 't')
            || (c == '{');
      case 46:
        return ('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ') || (c == '"');
      case 48:
        return (c == '+') || (c == '-') || ('0' <= c && c <= '9');
      case 49:
        return ('0' <= c && c <= '9');
      case 50:
        return ('0' <= c && c <= '9');
      case 51:
        return (c == '+') || (c == '-') || ('0' <= c && c <= '9');
      case 52:
        return ('0' <= c && c <= '9');
      case 53:
        return ('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ') || (c == ']');
      case 54:
        return (c == ']');
      case 55:
        return ('\t' <= c && c <= '\n')
            || (c == '\r')
            || (c == ' ')
            || (c == ',')
            || (c == '.')
            || (c == '0')
            || ('1' <= c && c <= '9')
            || (c == 'E')
            || (c == ']')
            || (c == 'e');
      case 56:
        return ('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ') || (c == ',') || (c == ']');
      case 57:
        return ('\t' <= c && c <= '\n')
            || (c == '\r')
            || (c == ' ')
            || (c == ',')
            || (c == '.')
            || (c == 'E')
            || (c == ']')
            || (c == 'e');
      case 58:
        return (c == '0') || ('1' <= c && c <= '9');
      case 59:
        return (c == '"');
      case 60:
        return ('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ') || (c == ',');
      case 61:
        return (c == ',');
      case 62:
        return ('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ') || (c == '"') || (c == '}');
      case 63:
        return ('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ') || (c == '}');
      case 64:
        return (c == '}');
      case 65:
        return (c == '0')
            || ('1' <= c && c <= '9')
            || ('A' <= c && c <= 'F')
            || ('a' <= c && c <= 'f');
      case 66:
        return (c == 's');
      case 67:
        return ('\t' <= c && c <= '\n')
            || (c == '\r')
            || (c == ' ')
            || (c == ',')
            || (c == '.')
            || (c == '0')
            || ('1' <= c && c <= '9')
            || (c == 'E')
            || (c == 'e');
      case 68:
        return (c == '"')
            || (c == '-')
            || (c == '0')
            || ('1' <= c && c <= '9')
            || (c == '[')
            || (c == ']')
            || (c == 'f')
            || (c == 'n')
            || (c == 't')
            || (c == '{');
      case 69:
        return ('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ') || (c == '}');
      case 70:
        return ('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ') || (c == '"') || (c == ':');
      case 71:
        return ('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ') || (c == ']');
      case 72:
        return ('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ') || (c == ':');
      case 73:
        return (c == '.') || (c == '0') || ('1' <= c && c <= '9') || (c == 'E') || (c == 'e');
      case 74:
        return (c == 'l');
      case 75:
        return ('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ') || (c == ',');
      case 76:
        return ('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ') || (c == ']');
      case 77:
        return ('\t' <= c && c <= '\n')
            || (c == '\r')
            || (c == ' ')
            || (c == '"')
            || (c == '-')
            || (c == '0')
            || ('1' <= c && c <= '9')
            || (c == '[')
            || (c == ']')
            || (c == 'f')
            || (c == 'n')
            || (c == 't')
            || (c == '{');
      case 78:
        return ('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ') || (c == '"') || (c == ':');
      case 79:
        return ('\t' <= c && c <= '\n')
            || (c == '\r')
            || (c == ' ')
            || (c == '"')
            || (c == ',')
            || (c == '}');
      case 80:
        return (c == 'e');
      case 81:
        return ('\t' <= c && c <= '\n')
            || (c == '\r')
            || (c == ' ')
            || (c == '"')
            || (c == ',')
            || (c == '-')
            || (c == '0')
            || ('1' <= c && c <= '9')
            || (c == 'E')
            || (c == '[')
            || (c == 'e')
            || (c == 'f')
            || (c == 'n')
            || (c == 't')
            || (c == '{');
      case 82:
        return ('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ') || (c == '}');
      case 83:
        return ('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ') || (c == '"') || (c == '}');
      case 84:
        return ('\t' <= c && c <= '\n')
            || (c == '\r')
            || (c == ' ')
            || (c == '"')
            || (c == ',')
            || (c == '-')
            || (c == '.')
            || (c == '0')
            || ('1' <= c && c <= '9')
            || (c == 'E')
            || (c == '[')
            || (c == 'e')
            || (c == 'f')
            || (c == 'n')
            || (c == 't')
            || (c == '{');
      case 85:
        return ('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ') || (c == '"') || (c == ',');
      case 86:
        return ('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ') || (c == '"') || (c == '}');
      case 87:
        return ('\t' <= c && c <= '\n')
            || (c == '\r')
            || (c == ' ')
            || (c == '"')
            || (c == ',')
            || (c == '-')
            || (c == '0')
            || ('1' <= c && c <= '9')
            || (c == '[')
            || (c == ']')
            || (c == 'f')
            || (c == 'n')
            || (c == 't')
            || (c == '{');
      case 88:
        return ('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ') || (c == '"') || (c == '}');
      case 89:
        return ('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ') || (c == '}');
      case 90:
        return ('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ') || (c == ':');
      case 91:
        return (c == ':');
      case 92:
        return (c == '}');
      case 93:
        return ('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ') || (c == ']');
      case 94:
        return (c == ']');
      case 95:
        return ('\t' <= c && c <= '\n')
            || (c == '\r')
            || (c == ' ')
            || (c == '"')
            || (c == ',')
            || (c == '-')
            || (c == '0')
            || ('1' <= c && c <= '9')
            || (c == '[')
            || (c == 'f')
            || (c == 'n')
            || (c == 't')
            || (c == '{');
      case 96:
        return (c == '"')
            || (c == '-')
            || (c == '0')
            || ('1' <= c && c <= '9')
            || (c == '[')
            || (c == 'f')
            || (c == 'n')
            || (c == 't')
            || (c == '{');
      case 97:
        return ('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ') || (c == ',') || (c == ']');
      case 98:
        return ('\t' <= c && c <= '\n')
            || (c == '\r')
            || (c == ' ')
            || (c == '"')
            || (c == ',')
            || (c == '}');
      case 99:
        return ('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ') || (c == ',') || (c == '}');
      case 100:
        return ('\t' <= c && c <= '\n')
            || (c == '\r')
            || (c == ' ')
            || (c == '"')
            || (c == '-')
            || (c == '0')
            || ('1' <= c && c <= '9')
            || (c == '[')
            || (c == 'f')
            || (c == 'n')
            || (c == 't')
            || (c == '{');
      case 101:
        return ('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ') || (c == ',') || (c == '}');
      case 102:
        return ('\t' <= c && c <= '\n')
            || (c == '\r')
            || (c == ' ')
            || (c == '"')
            || (c == '-')
            || (c == '0')
            || ('1' <= c && c <= '9')
            || (c == ':')
            || (c == '[')
            || (c == 'f')
            || (c == 'n')
            || (c == 't')
            || (c == '{');
      case 103:
        return ('\t' <= c && c <= '\n')
            || (c == '\r')
            || (c == ' ')
            || (c == '"')
            || (c == ',')
            || (c == '-')
            || (c == '0')
            || ('1' <= c && c <= '9')
            || (c == '[')
            || (c == 'f')
            || (c == 'n')
            || (c == 't')
            || (c == '{');
      case 104:
        return (c == 'e');
      case 105:
        return ('\t' <= c && c <= '\n')
            || (c == '\r')
            || (c == ' ')
            || (c == '"')
            || (c == ',')
            || (c == '-')
            || (c == '0')
            || ('1' <= c && c <= '9')
            || (c == '[')
            || (c == 'f')
            || (c == 'n')
            || (c == 't')
            || (c == '{');
      case 106:
        return ('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ') || (c == ']');
      case 107:
        return ('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ') || (c == ',') || (c == '}');
      case 108:
        return ('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ') || (c == '}');
      case 109:
        return (c == '0')
            || ('1' <= c && c <= '9')
            || ('A' <= c && c <= 'F')
            || ('a' <= c && c <= 'f');
      case 110:
        return ('\t' <= c && c <= '\n')
            || (c == '\r')
            || (c == ' ')
            || (c == '"')
            || (c == ',')
            || (c == '-')
            || (c == '0')
            || ('1' <= c && c <= '9')
            || (c == '[')
            || (c == 'f')
            || (c == 'n')
            || (c == 't')
            || (c == '{')
            || (c == '}');
      case 111:
        return ('\t' <= c && c <= '\n')
            || (c == '\r')
            || (c == ' ')
            || (c == '"')
            || (c == '-')
            || (c == '0')
            || ('1' <= c && c <= '9')
            || (c == ':')
            || (c == '[')
            || (c == 'f')
            || (c == 'n')
            || (c == 't')
            || (c == '{');
      case 112:
        return ('\t' <= c && c <= '\n')
            || (c == '\r')
            || (c == ' ')
            || (c == ',')
            || (c == 'E')
            || (c == 'e');
      case 113:
        return ('\t' <= c && c <= '\n')
            || (c == '\r')
            || (c == ' ')
            || (c == '"')
            || (c == ',')
            || (c == '-')
            || (c == '0')
            || ('1' <= c && c <= '9')
            || (c == '[')
            || (c == 'f')
            || (c == 'n')
            || (c == 't')
            || (c == '{')
            || (c == '}');
      case 114:
        return ('\t' <= c && c <= '\n')
            || (c == '\r')
            || (c == ' ')
            || (c == '"')
            || (c == '-')
            || (c == '0')
            || ('1' <= c && c <= '9')
            || (c == '[')
            || (c == 'f')
            || (c == 'n')
            || (c == 't')
            || (c == '{');
      case 115:
        return ('\t' <= c && c <= '\n')
            || (c == '\r')
            || (c == ' ')
            || (c == '"')
            || (c == '-')
            || (c == '0')
            || ('1' <= c && c <= '9')
            || (c == ':')
            || (c == '[')
            || (c == 'f')
            || (c == 'n')
            || (c == 't')
            || (c == '{');
      case 116:
        return (c == '"')
            || (c == '-')
            || (c == '0')
            || ('1' <= c && c <= '9')
            || (c == '[')
            || (c == 'f')
            || (c == 'n')
            || (c == 't')
            || (c == '{');
      case 117:
        return ('\t' <= c && c <= '\n')
            || (c == '\r')
            || (c == ' ')
            || (c == '"')
            || (c == '-')
            || (c == '0')
            || ('1' <= c && c <= '9')
            || (c == '[')
            || (c == 'f')
            || (c == 'n')
            || (c == 't')
            || (c == '{');
      case 118:
        return ('\t' <= c && c <= '\n')
            || (c == '\r')
            || (c == ' ')
            || (c == '"')
            || (c == '-')
            || (c == '0')
            || ('1' <= c && c <= '9')
            || (c == '[')
            || (c == 'f')
            || (c == 'n')
            || (c == 't')
            || (c == '{');
      case 119:
        return (c == '"')
            || (c == ',')
            || (c == '-')
            || (c == '0')
            || ('1' <= c && c <= '9')
            || (c == '[')
            || (c == 'f')
            || (c == 'n')
            || (c == 't')
            || (c == '{');
      case 120:
        return ('\t' <= c && c <= '\n')
            || (c == '\r')
            || (c == ' ')
            || (c == '"')
            || (c == ',')
            || (c == '-')
            || (c == '0')
            || ('1' <= c && c <= '9')
            || (c == '[')
            || (c == ']')
            || (c == 'f')
            || (c == 'n')
            || (c == 't')
            || (c == '{');
      case 121:
        return ('\t' <= c && c <= '\n')
            || (c == '\r')
            || (c == ' ')
            || (c == '"')
            || (c == ',')
            || (c == '-')
            || (c == '0')
            || ('1' <= c && c <= '9')
            || (c == ':')
            || (c == '[')
            || (c == 'f')
            || (c == 'n')
            || (c == 't')
            || (c == '{');
      case 122:
        return (c == ',') || (c == '}');
      case 123:
        return ('\t' <= c && c <= '\n')
            || (c == '\r')
            || (c == ' ')
            || (c == '"')
            || (c == ',')
            || (c == '-')
            || (c == '0')
            || ('1' <= c && c <= '9')
            || (c == '[')
            || (c == 'f')
            || (c == 'n')
            || (c == 't')
            || (c == '{');
      case 124:
        return (c == '0')
            || ('1' <= c && c <= '9')
            || ('A' <= c && c <= 'F')
            || ('a' <= c && c <= 'f');
      case 125:
        return ('\t' <= c && c <= '\n')
            || (c == '\r')
            || (c == ' ')
            || (c == '"')
            || (c == ',')
            || (c == '-')
            || (c == '0')
            || ('1' <= c && c <= '9')
            || (c == '[')
            || (c == 'f')
            || (c == 'n')
            || (c == 't')
            || (c == '{')
            || (c == '}');
      case 126:
        return ('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ') || (c == ',') || (c == '}');
      case 127:
        return ('\t' <= c && c <= '\n')
            || (c == '\r')
            || (c == ' ')
            || (c == '"')
            || (c == ',')
            || (c == '-')
            || (c == '0')
            || ('1' <= c && c <= '9')
            || (c == '[')
            || (c == 'f')
            || (c == 'n')
            || (c == 't')
            || (c == '{');
      case 128:
        return (c == ',');
      case 129:
        return (c == '"')
            || (c == ',')
            || (c == '-')
            || (c == '0')
            || ('1' <= c && c <= '9')
            || (c == '[')
            || (c == 'f')
            || (c == 'n')
            || (c == 't')
            || (c == '{')
            || (c == '}');
      case 130:
        return ('\t' <= c && c <= '\n')
            || (c == '\r')
            || (c == ' ')
            || (c == '"')
            || (c == ',')
            || (c == '-')
            || (c == '0')
            || ('1' <= c && c <= '9')
            || (c == '[')
            || (c == ']')
            || (c == 'f')
            || (c == 'n')
            || (c == 't')
            || (c == '{');
      case 131:
        return (c == '"')
            || (c == '-')
            || (c == '0')
            || ('1' <= c && c <= '9')
            || (c == ':')
            || (c == '[')
            || (c == 'f')
            || (c == 'n')
            || (c == 't')
            || (c == '{');
      case 132:
        return ('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ') || (c == ',');
      case 133:
        return ('\t' <= c && c <= '\n')
            || (c == '\r')
            || (c == ' ')
            || (c == '"')
            || (c == ',')
            || (c == '-')
            || (c == '0')
            || ('1' <= c && c <= '9')
            || (c == '[')
            || (c == 'f')
            || (c == 'n')
            || (c == 't')
            || (c == '{');
      case 134:
        return ('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ') || (c == ',') || (c == ']');
      case 135:
        return ('\t' <= c && c <= '\n')
            || (c == '\r')
            || (c == ' ')
            || (c == '"')
            || (c == ',')
            || (c == '-')
            || (c == '0')
            || ('1' <= c && c <= '9')
            || (c == '[')
            || (c == 'f')
            || (c == 'n')
            || (c == 't')
            || (c == '{');
      case 136:
        return ('\t' <= c && c <= '\n')
            || (c == '\r')
            || (c == ' ')
            || (c == '"')
            || (c == ',')
            || (c == '-')
            || (c == '0')
            || ('1' <= c && c <= '9')
            || (c == '[')
            || (c == 'f')
            || (c == 'n')
            || (c == 't')
            || (c == '{');
      case 137:
        return (c == '0')
            || ('1' <= c && c <= '9')
            || ('A' <= c && c <= 'F')
            || ('a' <= c && c <= 'f');
      case 138:
        return ('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ') || (c == '"');
      case 139:
        return ('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ') || (c == ',');
      case 140:
        return ('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ') || (c == '"') || (c == ',');
      case 141:
        return ('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ') || (c == '"') || (c == ',');
      case 142:
        return ('\t' <= c && c <= '\n')
            || (c == '\r')
            || (c == ' ')
            || (c == '"')
            || (c == '-')
            || (c == '0')
            || ('1' <= c && c <= '9')
            || (c == '[')
            || (c == 'f')
            || (c == 'n')
            || (c == 't')
            || (c == '{');
      case 143:
        return ('\t' <= c && c <= '\n')
            || (c == '\r')
            || (c == ' ')
            || (c == '"')
            || (c == ',')
            || (c == '-')
            || (c == '0')
            || ('1' <= c && c <= '9')
            || (c == '[')
            || (c == ']')
            || (c == 'f')
            || (c == 'n')
            || (c == 't')
            || (c == '{');
      case 144:
        return ('\t' <= c && c <= '\n')
            || (c == '\r')
            || (c == ' ')
            || (c == '"')
            || (c == ',')
            || (c == '-')
            || (c == '0')
            || ('1' <= c && c <= '9')
            || (c == '[')
            || (c == 'f')
            || (c == 'n')
            || (c == 't')
            || (c == '{');
      case 145:
        return ('\t' <= c && c <= '\n')
            || (c == '\r')
            || (c == ' ')
            || (c == '"')
            || (c == ',')
            || (c == '-')
            || (c == '0')
            || ('1' <= c && c <= '9')
            || (c == '[')
            || (c == 'f')
            || (c == 'n')
            || (c == 't')
            || (c == '{');
      case 146:
        return ('\t' <= c && c <= '\n')
            || (c == '\r')
            || (c == ' ')
            || (c == '"')
            || (c == ',')
            || (c == '-')
            || (c == '0')
            || ('1' <= c && c <= '9')
            || (c == '[')
            || (c == ']')
            || (c == 'f')
            || (c == 'n')
            || (c == 't')
            || (c == '{');
      case 147:
        return ('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ') || (c == '"') || (c == ',');
      case 148:
        return ('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ') || (c == '"') || (c == ',');
      case 149:
        return (c == ',') || (c == ']');
      case 150:
        return (c == '"')
            || (c == ',')
            || (c == '-')
            || (c == '0')
            || ('1' <= c && c <= '9')
            || (c == '[')
            || (c == 'f')
            || (c == 'n')
            || (c == 't')
            || (c == '{');
      case 151:
        return ('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ') || (c == '"');
      case 152:
        return ('\t' <= c && c <= '\n')
            || (c == '\r')
            || (c == ' ')
            || (c == ',')
            || (c == '.')
            || (c == '0')
            || ('1' <= c && c <= '9')
            || (c == 'E')
            || (c == 'e');
      case 153:
        return ('\t' <= c && c <= '\n')
            || (c == '\r')
            || (c == ' ')
            || (c == ',')
            || (c == '.')
            || (c == 'E')
            || (c == 'e');
      case 154:
        return ('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ') || (c == '"');
      case 155:
        return (c == '"')
            || (c == ',')
            || (c == '-')
            || (c == '0')
            || ('1' <= c && c <= '9')
            || (c == '[')
            || (c == ']')
            || (c == 'f')
            || (c == 'n')
            || (c == 't')
            || (c == '{');
      case 156:
        return ('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ') || (c == '"') || (c == ',');
      case 157:
        return ('\t' <= c && c <= '\n')
            || (c == '\r')
            || (c == ' ')
            || (c == '"')
            || (c == ',')
            || (c == '-')
            || (c == '0')
            || ('1' <= c && c <= '9')
            || (c == '[')
            || (c == 'f')
            || (c == 'n')
            || (c == 't')
            || (c == '{');
    }
    throw new AssertionError("Unknown nodeId: " + stack.nodeId);
  }

  public String nodeDescriptionOf(int nodeId) {
    switch (nodeId) {
      case 0:
        return "{•<start>}";
      case 1:
        return "{ws•element ws|{\\t-\\n\\r\\u0020}•ws}";
      case 2:
        return "{'\"'•characters '\"'|'\"' characters•'\"'}";
      case 3:
        return "{'['•ws ']'|'[' ws•']'|'['•ws elements ws ']'|'[' ws•elements ws ']'}";
      case 4:
        return "{ws element•ws|int•frac exp|int frac•exp|onenine•digits}";
      case 5:
        return "{'t'•'r' 'u' 'e'}";
      case 6:
        return "{'-'•digit|'-'•onenine digits}";
      case 7:
        return "{'n'•'u' 'l' 'l'}";
      case 8:
        return "{ws element•ws|int•frac exp|int frac•exp}";
      case 9:
        return "{'{'•ws '}'|'{' ws•'}'|'{'•ws members ws '}'|'{' ws•members ws '}'}";
      case 10:
        return "{'f'•'a' 'l' 's' 'e'}";
      case 11:
        return "{{\\t-\\n\\r\\u0020}•ws}";
      case 12:
        return "{ws•element ws}";
      case 13:
        return "{int•frac exp|int frac•exp|onenine•digits}";
      case 14:
        return "{int•frac exp|int frac•exp}";
      case 15:
        return "{ws element•ws}";
      case 16:
        return "{int•frac exp}";
      case 17:
        return "{'.'•{0-9}+}";
      case 18:
        return "{int frac•exp}";
      case 19:
        return "{'e'•sign {0-9}+|'e' sign•{0-9}+}";
      case 20:
        return "{'E'•sign {0-9}+|'E' sign•{0-9}+}";
      case 21:
        return "{'\"'•characters '\"'}";
      case 22:
        return "{'\\'•escape}";
      case 23:
        return "{'\"' characters•'\"'}";
      case 24:
        return "{character•characters}";
      case 25:
        return "{onenine•digits}";
      case 26:
        return "{digit•digits}";
      case 27:
        return "{'['•ws ']'|'['•ws elements ws ']'}";
      case 28:
        return "{'[' ws•elements ws ']'}";
      case 29:
        return "{element•ws ',' ws elements|element ws•',' ws elements|int•frac exp|int frac•exp|onenine•digits}";
      case 30:
        return "{'[' ws•']'}";
      case 31:
        return "{element•ws ',' ws elements|element ws•',' ws elements|int•frac exp|int frac•exp}";
      case 32:
        return "{'-'•digit}";
      case 33:
        return "{'{'•ws '}'|'{'•ws members ws '}'|'{' ws•members ws '}'}";
      case 34:
        return "{{\\t-\\n\\r\\u0020}•ws|ws•string ws ':' ws element}";
      case 35:
        return "{'{'•ws '}'|'{'•ws members ws '}'}";
      case 36:
        return "{'{' ws•members ws '}'}";
      case 37:
        return "{'{' ws•'}'}";
      case 38:
        return "{ws element•ws|'{' ws•'}'|'{' ws•members ws '}'|'{' ws members•ws '}'|'{' ws members ws•'}'}";
      case 39:
        return "{ws element•ws|int frac•exp}";
      case 40:
        return "{'f' 'a'•'l' 's' 'e'}";
      case 41:
        return "{ws element•ws|int•frac exp|int frac•exp|'-' onenine•digits}";
      case 42:
        return "{'t' 'r'•'u' 'e'}";
      case 43:
        return "{'n' 'u'•'l' 'l'}";
      case 44:
        return "{ws element•ws|'[' ws•']'|'[' ws•elements ws ']'|'[' ws elements•ws ']'|'[' ws elements ws•']'}";
      case 45:
        return "{ws•element ws|ws element•ws}";
      case 46:
        return "{ws element•ws|'\"' characters•'\"'}";
      case 47:
        return "{}";
      case 48:
        return "{'e'•sign {0-9}+}";
      case 49:
        return "{'e' sign•{0-9}+}";
      case 50:
        return "{{0-9}+•{0-9}}";
      case 51:
        return "{'E'•sign {0-9}+}";
      case 52:
        return "{'E' sign•{0-9}+}";
      case 53:
        return "{ws element•ws|'[' ws elements•ws ']'}";
      case 54:
        return "{'[' ws•']'|'[' ws elements ws•']'}";
      case 55:
        return "{'[' ws elements•ws ']'|'[' ws elements ws•']'|element•ws ',' ws elements|element ws•',' ws elements|int•frac exp|int frac•exp|onenine•digits}";
      case 56:
        return "{'[' ws elements•ws ']'|'[' ws elements ws•']'|element•ws ',' ws elements|element ws•',' ws elements}";
      case 57:
        return "{'[' ws elements•ws ']'|'[' ws elements ws•']'|element•ws ',' ws elements|element ws•',' ws elements|int•frac exp|int frac•exp}";
      case 58:
        return "{'-' onenine•digits}";
      case 59:
        return "{ws•string ws ':' ws element}";
      case 60:
        return "{element•ws ',' ws elements}";
      case 61:
        return "{element ws•',' ws elements}";
      case 62:
        return "{ws element•ws|'{' ws•members ws '}'|'{' ws members•ws '}'}";
      case 63:
        return "{ws element•ws|'{' ws members•ws '}'}";
      case 64:
        return "{'{' ws•'}'|'{' ws members ws•'}'}";
      case 65:
        return "{'u'•hex hex hex hex}";
      case 66:
        return "{'f' 'a' 'l'•'s' 'e'}";
      case 67:
        return "{element•ws ',' ws elements|element ws•',' ws elements|int•frac exp|int frac•exp|'-' onenine•digits}";
      case 68:
        return "{'[' ws•']'|'[' ws•elements ws ']'}";
      case 69:
        return "{'{' ws members•ws '}'|'{' ws members ws•'}'}";
      case 70:
        return "{ws•string ws ':' ws element|ws string•ws ':' ws element|ws string ws•':' ws element}";
      case 71:
        return "{'[' ws elements•ws ']'|'[' ws elements ws•']'}";
      case 72:
        return "{ws string•ws ':' ws element|ws string ws•':' ws element}";
      case 73:
        return "{int•frac exp|int frac•exp|'-' onenine•digits}";
      case 74:
        return "{'n' 'u' 'l'•'l'}";
      case 75:
        return "{element•ws ',' ws elements|element ws•',' ws elements}";
      case 76:
        return "{ws element•ws|'[' ws elements•ws ']'|'[' ws elements ws•']'}";
      case 77:
        return "{'[' ws•']'|'[' ws•elements ws ']'|'[' ws elements•ws ']'|'[' ws elements ws•']'}";
      case 78:
        return "{ws string•ws ':' ws element|ws string ws•':' ws element|'\"' characters•'\"'}";
      case 79:
        return "{'{' ws•'}'|'{' ws•members ws '}'|'{' ws members•ws '}'|'{' ws members ws•'}'|element•ws ',' ws elements|element ws•',' ws elements}";
      case 80:
        return "{'t' 'r' 'u'•'e'}";
      case 81:
        return "{element•ws ',' ws elements|element ws•',' ws elements|element ws ','•ws elements|element ws ',' ws•elements|int frac•exp}";
      case 82:
        return "{ws element•ws|'{' ws members•ws '}'|'{' ws members ws•'}'}";
      case 83:
        return "{'{' ws•'}'|'{' ws•members ws '}'|'{' ws members•ws '}'|'{' ws members ws•'}'}";
      case 84:
        return "{element•ws ',' ws elements|element ws•',' ws elements|element ws ','•ws elements|element ws ',' ws•elements|int•frac exp|int frac•exp}";
      case 85:
        return "{'\"' characters•'\"'|element•ws ',' ws elements|element ws•',' ws elements}";
      case 86:
        return "{'{' ws•'}'|'{' ws•members ws '}'}";
      case 87:
        return "{'[' ws•']'|'[' ws•elements ws ']'|element•ws ',' ws elements|element ws•',' ws elements}";
      case 88:
        return "{'{' ws•members ws '}'|'{' ws members•ws '}'}";
      case 89:
        return "{'{' ws members•ws '}'}";
      case 90:
        return "{ws string•ws ':' ws element}";
      case 91:
        return "{ws string ws•':' ws element}";
      case 92:
        return "{'{' ws members ws•'}'}";
      case 93:
        return "{'[' ws elements•ws ']'}";
      case 94:
        return "{'[' ws elements ws•']'}";
      case 95:
        return "{element•ws ',' ws elements|element ws ','•ws elements}";
      case 96:
        return "{element ws ',' ws•elements}";
      case 97:
        return "{'[' ws elements•ws ']'|element•ws ',' ws elements}";
      case 98:
        return "{'{' ws•members ws '}'|'{' ws members•ws '}'|element•ws ',' ws elements}";
      case 99:
        return "{'{' ws members•ws '}'|element•ws ',' ws elements}";
      case 100:
        return "{element ws ','•ws elements|element ws ',' ws•elements}";
      case 101:
        return "{'{' ws members•ws '}'|'{' ws members ws•'}'|element•ws ',' ws elements|element ws•',' ws elements}";
      case 102:
        return "{ws string•ws ':' ws element|ws string ws•':' ws element|ws string ws ':'•ws element|ws string ws ':' ws•element}";
      case 103:
        return "{element•ws ',' ws elements|element ws•',' ws elements|element ws ','•ws elements|element ws ',' ws•elements}";
      case 104:
        return "{'f' 'a' 'l' 's'•'e'}";
      case 105:
        return "{element ws•',' ws elements|element ws ','•ws elements|element ws ',' ws•elements}";
      case 106:
        return "{ws element•ws|'[' ws elements ws•']'}";
      case 107:
        return "{'{' ws members ws•'}'|element•ws ',' ws elements|element ws•',' ws elements}";
      case 108:
        return "{ws element•ws|'{' ws members ws•'}'}";
      case 109:
        return "{'u' hex•hex hex hex}";
      case 110:
        return "{'{' ws members•ws '}'|'{' ws members ws•'}'|element•ws ',' ws elements|element ws•',' ws elements|element ws ','•ws elements|element ws ',' ws•elements}";
      case 111:
        return "{ws string ws•':' ws element|ws string ws ':'•ws element|ws string ws ':' ws•element}";
      case 112:
        return "{element•ws ',' ws elements|element ws•',' ws elements|int frac•exp}";
      case 113:
        return "{'{' ws members•ws '}'|element•ws ',' ws elements|element ws ','•ws elements}";
      case 114:
        return "{element ws ','•ws elements}";
      case 115:
        return "{ws string•ws ':' ws element|ws string ws ':'•ws element}";
      case 116:
        return "{ws string ws ':' ws•element}";
      case 117:
        return "{ws string ws ':'•ws element|ws string ws ':' ws•element}";
      case 118:
        return "{ws string ws ':'•ws element}";
      case 119:
        return "{element ws•',' ws elements|element ws ',' ws•elements}";
      case 120:
        return "{'[' ws•']'|'[' ws•elements ws ']'|'[' ws elements•ws ']'|'[' ws elements ws•']'|element•ws ',' ws elements|element ws•',' ws elements}";
      case 121:
        return "{ws string ws•':' ws element|ws string ws ':'•ws element|ws string ws ':' ws•element|member•ws ',' ws members|member ws•',' ws members}";
      case 122:
        return "{'{' ws members ws•'}'|element ws•',' ws elements}";
      case 123:
        return "{ws string ws ':'•ws element|ws string ws ':' ws•element|member•ws ',' ws members|member ws•',' ws members}";
      case 124:
        return "{'u' hex hex•hex hex}";
      case 125:
        return "{'{' ws members ws•'}'|element•ws ',' ws elements|element ws•',' ws elements|element ws ','•ws elements|element ws ',' ws•elements}";
      case 126:
        return "{'{' ws members•ws '}'|'{' ws members ws•'}'|element ws•',' ws elements}";
      case 127:
        return "{ws string ws ':'•ws element|member•ws ',' ws members}";
      case 128:
        return "{member ws•',' ws members}";
      case 129:
        return "{'{' ws members ws•'}'|element ws•',' ws elements|element ws ',' ws•elements}";
      case 130:
        return "{'[' ws elements•ws ']'|'[' ws elements ws•']'|element•ws ',' ws elements|element ws•',' ws elements|element ws ','•ws elements|element ws ',' ws•elements}";
      case 131:
        return "{ws string ws•':' ws element|ws string ws ':' ws•element}";
      case 132:
        return "{member•ws ',' ws members|member ws•',' ws members}";
      case 133:
        return "{ws string ws ':' ws•element|member•ws ',' ws members|member ws•',' ws members}";
      case 134:
        return "{'[' ws elements ws•']'|element•ws ',' ws elements|element ws•',' ws elements}";
      case 135:
        return "{ws string ws ':'•ws element|ws string ws ':' ws•element|member•ws ',' ws members|member ws•',' ws members|member ws ','•ws members|member ws ',' ws•members}";
      case 136:
        return "{ws string ws ':' ws•element|member•ws ',' ws members|member ws•',' ws members|member ws ','•ws members|member ws ',' ws•members}";
      case 137:
        return "{'u' hex hex hex•hex}";
      case 138:
        return "{member ws ','•ws members|member ws ',' ws•members}";
      case 139:
        return "{member•ws ',' ws members}";
      case 140:
        return "{member•ws ',' ws members|member ws ','•ws members|member ws ',' ws•members}";
      case 141:
        return "{member•ws ',' ws members|member ws ','•ws members}";
      case 142:
        return "{ws string ws ':' ws•element|member ws ',' ws•members}";
      case 143:
        return "{'[' ws elements•ws ']'|element•ws ',' ws elements|element ws ','•ws elements}";
      case 144:
        return "{ws string ws ':'•ws element|member•ws ',' ws members|member ws ','•ws members|member ws ',' ws•members}";
      case 145:
        return "{ws string ws ':'•ws element|member•ws ',' ws members|member ws ','•ws members}";
      case 146:
        return "{'[' ws elements ws•']'|element•ws ',' ws elements|element ws•',' ws elements|element ws ','•ws elements|element ws ',' ws•elements}";
      case 147:
        return "{member•ws ',' ws members|member ws•',' ws members|member ws ','•ws members|member ws ',' ws•members}";
      case 148:
        return "{member ws•',' ws members|member ws ','•ws members|member ws ',' ws•members}";
      case 149:
        return "{'[' ws elements ws•']'|element ws•',' ws elements}";
      case 150:
        return "{ws string ws ':' ws•element|member ws•',' ws members}";
      case 151:
        return "{member ws ',' ws•members}";
      case 152:
        return "{member•ws ',' ws members|member ws•',' ws members|int•frac exp|int frac•exp|onenine•digits}";
      case 153:
        return "{member•ws ',' ws members|member ws•',' ws members|int•frac exp|int frac•exp}";
      case 154:
        return "{member ws ','•ws members}";
      case 155:
        return "{'[' ws elements ws•']'|element ws•',' ws elements|element ws ',' ws•elements}";
      case 156:
        return "{member ws•',' ws members|member ws ',' ws•members}";
      case 157:
        return "{ws string ws ':' ws•element|member ws•',' ws members|member ws ',' ws•members}";
    }
    return null;
  }

  private void replace(int newNodeId) {
    stack = new Stack(newNodeId, stack.prev);
  }

  private void append(int newNodeId) {
    stack = new Stack(newNodeId, stack);
  }

  // false를 리턴하면 더이상 finishStep을 하지 않아도 되는 상황
  // true를 리턴하면 finishStep을 계속 해야하는 상황
  private boolean finishStep() {
    if (stack == null || stack.prev == null) {
      throw new AssertionError("No edge to finish: " + stackIds());
    }
    int prev = stack.prev.nodeId;
    int last = stack.nodeId;
    if (prev == 0 && last == 1) { // (0,1)
      // ReplaceEdge(0,45,Some(0))
      replace(45);
      pendingFinish = 0;
      return false;
    }
    if (prev == 0 && last == 2) { // (0,2)
      // ReplaceEdge(0,46,Some(0))
      replace(46);
      pendingFinish = 0;
      return false;
    }
    if (prev == 0 && last == 3) { // (0,3)
      // ReplaceEdge(0,44,Some(0))
      replace(44);
      pendingFinish = 0;
      return false;
    }
    if (prev == 0 && last == 4) { // (0,4)
      // ReplaceEdge(0,8,Some(0))
      replace(8);
      pendingFinish = 0;
      return false;
    }
    if (prev == 0 && last == 5) { // (0,5)
      // ReplaceEdge(0,42,None)
      replace(42);
      pendingFinish = -1;
      return false;
    }
    if (prev == 0 && last == 6) { // (0,6)
      // ReplaceEdge(0,41,Some(0))
      replace(41);
      pendingFinish = 0;
      return false;
    }
    if (prev == 0 && last == 7) { // (0,7)
      // ReplaceEdge(0,43,None)
      replace(43);
      pendingFinish = -1;
      return false;
    }
    if (prev == 0 && last == 8) { // (0,8)
      // ReplaceEdge(0,39,Some(0))
      replace(39);
      pendingFinish = 0;
      return false;
    }
    if (prev == 0 && last == 9) { // (0,9)
      // ReplaceEdge(0,38,Some(0))
      replace(38);
      pendingFinish = 0;
      return false;
    }
    if (prev == 0 && last == 10) { // (0,10)
      // ReplaceEdge(0,40,None)
      replace(40);
      pendingFinish = -1;
      return false;
    }
    if (prev == 0 && last == 11) { // (0,11)
      // ReplaceEdge(0,12,None)
      replace(12);
      pendingFinish = -1;
      return false;
    }
    if (prev == 0 && last == 12) { // (0,12)
      // ReplaceEdge(0,15,Some(0))
      replace(15);
      pendingFinish = 0;
      return false;
    }
    if (prev == 0 && last == 15) { // (0,15)
      // DropLast(0)
      dropLast();
      return true;
    }
    if (prev == 0 && last == 16) { // (0,16)
      // ReplaceEdge(0,39,Some(0))
      replace(39);
      pendingFinish = 0;
      return false;
    }
    if (prev == 0 && last == 18) { // (0,18)
      // ReplaceEdge(0,15,Some(0))
      replace(15);
      pendingFinish = 0;
      return false;
    }
    if (prev == 0 && last == 21) { // (0,21)
      // ReplaceEdge(0,23,None)
      replace(23);
      pendingFinish = -1;
      return false;
    }
    if (prev == 0 && last == 23) { // (0,23)
      // ReplaceEdge(0,15,Some(0))
      replace(15);
      pendingFinish = 0;
      return false;
    }
    if (prev == 0 && last == 25) { // (0,25)
      // ReplaceEdge(0,8,Some(0))
      replace(8);
      pendingFinish = 0;
      return false;
    }
    if (prev == 0 && last == 27) { // (0,27)
      // ReplaceEdge(0,68,None)
      replace(68);
      pendingFinish = -1;
      return false;
    }
    if (prev == 0 && last == 28) { // (0,28)
      // ReplaceEdge(0,71,None)
      replace(71);
      pendingFinish = -1;
      return false;
    }
    if (prev == 0 && last == 30) { // (0,30)
      // ReplaceEdge(0,15,Some(0))
      replace(15);
      pendingFinish = 0;
      return false;
    }
    if (prev == 0 && last == 32) { // (0,32)
      // ReplaceEdge(0,8,Some(0))
      replace(8);
      pendingFinish = 0;
      return false;
    }
    if (prev == 0 && last == 33) { // (0,33)
      // ReplaceEdge(0,83,None)
      replace(83);
      pendingFinish = -1;
      return false;
    }
    if (prev == 0 && last == 35) { // (0,35)
      // ReplaceEdge(0,86,None)
      replace(86);
      pendingFinish = -1;
      return false;
    }
    if (prev == 0 && last == 36) { // (0,36)
      // ReplaceEdge(0,69,None)
      replace(69);
      pendingFinish = -1;
      return false;
    }
    if (prev == 0 && last == 37) { // (0,37)
      // ReplaceEdge(0,15,Some(0))
      replace(15);
      pendingFinish = 0;
      return false;
    }
    if (prev == 0 && last == 38) { // (0,38)
      // ReplaceEdge(0,82,Some(0))
      replace(82);
      pendingFinish = 0;
      return false;
    }
    if (prev == 0 && last == 39) { // (0,39)
      // ReplaceEdge(0,15,Some(0))
      replace(15);
      pendingFinish = 0;
      return false;
    }
    if (prev == 0 && last == 40) { // (0,40)
      // ReplaceEdge(0,66,None)
      replace(66);
      pendingFinish = -1;
      return false;
    }
    if (prev == 0 && last == 41) { // (0,41)
      // ReplaceEdge(0,8,Some(0))
      replace(8);
      pendingFinish = 0;
      return false;
    }
    if (prev == 0 && last == 42) { // (0,42)
      // ReplaceEdge(0,80,None)
      replace(80);
      pendingFinish = -1;
      return false;
    }
    if (prev == 0 && last == 43) { // (0,43)
      // ReplaceEdge(0,74,None)
      replace(74);
      pendingFinish = -1;
      return false;
    }
    if (prev == 0 && last == 44) { // (0,44)
      // ReplaceEdge(0,76,Some(0))
      replace(76);
      pendingFinish = 0;
      return false;
    }
    if (prev == 0 && last == 45) { // (0,45)
      // ReplaceEdge(0,15,Some(0))
      replace(15);
      pendingFinish = 0;
      return false;
    }
    if (prev == 0 && last == 46) { // (0,46)
      // ReplaceEdge(0,15,Some(0))
      replace(15);
      pendingFinish = 0;
      return false;
    }
    if (prev == 0 && last == 47) { // (0,47)
      // DropLast(47)
      dropLast();
      replace(47);
      return true;
    }
    if (prev == 0 && last == 53) { // (0,53)
      // ReplaceEdge(0,94,Some(0))
      replace(94);
      pendingFinish = 0;
      return false;
    }
    if (prev == 0 && last == 54) { // (0,54)
      // ReplaceEdge(0,15,Some(0))
      replace(15);
      pendingFinish = 0;
      return false;
    }
    if (prev == 0 && last == 58) { // (0,58)
      // ReplaceEdge(0,8,Some(0))
      replace(8);
      pendingFinish = 0;
      return false;
    }
    if (prev == 0 && last == 62) { // (0,62)
      // ReplaceEdge(0,69,Some(0))
      replace(69);
      pendingFinish = 0;
      return false;
    }
    if (prev == 0 && last == 63) { // (0,63)
      // ReplaceEdge(0,92,Some(0))
      replace(92);
      pendingFinish = 0;
      return false;
    }
    if (prev == 0 && last == 64) { // (0,64)
      // ReplaceEdge(0,15,Some(0))
      replace(15);
      pendingFinish = 0;
      return false;
    }
    if (prev == 0 && last == 66) { // (0,66)
      // ReplaceEdge(0,104,None)
      replace(104);
      pendingFinish = -1;
      return false;
    }
    if (prev == 0 && last == 68) { // (0,68)
      // ReplaceEdge(0,76,Some(0))
      replace(76);
      pendingFinish = 0;
      return false;
    }
    if (prev == 0 && last == 69) { // (0,69)
      // ReplaceEdge(0,108,Some(0))
      replace(108);
      pendingFinish = 0;
      return false;
    }
    if (prev == 0 && last == 71) { // (0,71)
      // ReplaceEdge(0,106,Some(0))
      replace(106);
      pendingFinish = 0;
      return false;
    }
    if (prev == 0 && last == 74) { // (0,74)
      // ReplaceEdge(0,15,Some(0))
      replace(15);
      pendingFinish = 0;
      return false;
    }
    if (prev == 0 && last == 76) { // (0,76)
      // ReplaceEdge(0,106,Some(0))
      replace(106);
      pendingFinish = 0;
      return false;
    }
    if (prev == 0 && last == 80) { // (0,80)
      // ReplaceEdge(0,15,Some(0))
      replace(15);
      pendingFinish = 0;
      return false;
    }
    if (prev == 0 && last == 82) { // (0,82)
      // ReplaceEdge(0,108,Some(0))
      replace(108);
      pendingFinish = 0;
      return false;
    }
    if (prev == 0 && last == 83) { // (0,83)
      // ReplaceEdge(0,82,Some(0))
      replace(82);
      pendingFinish = 0;
      return false;
    }
    if (prev == 0 && last == 86) { // (0,86)
      // ReplaceEdge(0,82,Some(0))
      replace(82);
      pendingFinish = 0;
      return false;
    }
    if (prev == 0 && last == 88) { // (0,88)
      // ReplaceEdge(0,69,None)
      replace(69);
      pendingFinish = -1;
      return false;
    }
    if (prev == 0 && last == 89) { // (0,89)
      // ReplaceEdge(0,92,None)
      replace(92);
      pendingFinish = -1;
      return false;
    }
    if (prev == 0 && last == 92) { // (0,92)
      // ReplaceEdge(0,15,Some(0))
      replace(15);
      pendingFinish = 0;
      return false;
    }
    if (prev == 0 && last == 93) { // (0,93)
      // ReplaceEdge(0,94,None)
      replace(94);
      pendingFinish = -1;
      return false;
    }
    if (prev == 0 && last == 94) { // (0,94)
      // ReplaceEdge(0,15,Some(0))
      replace(15);
      pendingFinish = 0;
      return false;
    }
    if (prev == 0 && last == 104) { // (0,104)
      // ReplaceEdge(0,15,Some(0))
      replace(15);
      pendingFinish = 0;
      return false;
    }
    if (prev == 0 && last == 106) { // (0,106)
      // ReplaceEdge(0,15,Some(0))
      replace(15);
      pendingFinish = 0;
      return false;
    }
    if (prev == 0 && last == 108) { // (0,108)
      // ReplaceEdge(0,15,Some(0))
      replace(15);
      pendingFinish = 0;
      return false;
    }
    if (prev == 11 && last == 11) { // (11,11)
      // DropLast(11)
      dropLast();
      return true;
    }
    if (prev == 12 && last == 2) { // (12,2)
      // ReplaceEdge(12,23,Some(12))
      replace(23);
      pendingFinish = 12;
      return false;
    }
    if (prev == 12 && last == 3) { // (12,3)
      // ReplaceEdge(12,77,Some(12))
      replace(77);
      pendingFinish = 12;
      return false;
    }
    if (prev == 12 && last == 5) { // (12,5)
      // ReplaceEdge(12,42,None)
      replace(42);
      pendingFinish = -1;
      return false;
    }
    if (prev == 12 && last == 6) { // (12,6)
      // ReplaceEdge(12,73,Some(12))
      replace(73);
      pendingFinish = 12;
      return false;
    }
    if (prev == 12 && last == 7) { // (12,7)
      // ReplaceEdge(12,43,None)
      replace(43);
      pendingFinish = -1;
      return false;
    }
    if (prev == 12 && last == 9) { // (12,9)
      // ReplaceEdge(12,83,Some(12))
      replace(83);
      pendingFinish = 12;
      return false;
    }
    if (prev == 12 && last == 10) { // (12,10)
      // ReplaceEdge(12,40,None)
      replace(40);
      pendingFinish = -1;
      return false;
    }
    if (prev == 12 && last == 13) { // (12,13)
      // ReplaceEdge(12,14,Some(12))
      replace(14);
      pendingFinish = 12;
      return false;
    }
    if (prev == 12 && last == 14) { // (12,14)
      // ReplaceEdge(12,18,Some(12))
      replace(18);
      pendingFinish = 12;
      return false;
    }
    if (prev == 12 && last == 16) { // (12,16)
      // ReplaceEdge(12,18,Some(12))
      replace(18);
      pendingFinish = 12;
      return false;
    }
    if (prev == 12 && last == 18) { // (12,18)
      // DropLast(12)
      dropLast();
      return true;
    }
    if (prev == 12 && last == 21) { // (12,21)
      // ReplaceEdge(12,23,None)
      replace(23);
      pendingFinish = -1;
      return false;
    }
    if (prev == 12 && last == 23) { // (12,23)
      // DropLast(12)
      dropLast();
      return true;
    }
    if (prev == 12 && last == 25) { // (12,25)
      // ReplaceEdge(12,14,Some(12))
      replace(14);
      pendingFinish = 12;
      return false;
    }
    if (prev == 12 && last == 27) { // (12,27)
      // ReplaceEdge(12,68,None)
      replace(68);
      pendingFinish = -1;
      return false;
    }
    if (prev == 12 && last == 28) { // (12,28)
      // ReplaceEdge(12,71,None)
      replace(71);
      pendingFinish = -1;
      return false;
    }
    if (prev == 12 && last == 30) { // (12,30)
      // DropLast(12)
      dropLast();
      return true;
    }
    if (prev == 12 && last == 32) { // (12,32)
      // ReplaceEdge(12,14,Some(12))
      replace(14);
      pendingFinish = 12;
      return false;
    }
    if (prev == 12 && last == 33) { // (12,33)
      // ReplaceEdge(12,83,None)
      replace(83);
      pendingFinish = -1;
      return false;
    }
    if (prev == 12 && last == 35) { // (12,35)
      // ReplaceEdge(12,86,None)
      replace(86);
      pendingFinish = -1;
      return false;
    }
    if (prev == 12 && last == 36) { // (12,36)
      // ReplaceEdge(12,69,None)
      replace(69);
      pendingFinish = -1;
      return false;
    }
    if (prev == 12 && last == 37) { // (12,37)
      // DropLast(12)
      dropLast();
      return true;
    }
    if (prev == 12 && last == 40) { // (12,40)
      // ReplaceEdge(12,66,None)
      replace(66);
      pendingFinish = -1;
      return false;
    }
    if (prev == 12 && last == 42) { // (12,42)
      // ReplaceEdge(12,80,None)
      replace(80);
      pendingFinish = -1;
      return false;
    }
    if (prev == 12 && last == 43) { // (12,43)
      // ReplaceEdge(12,74,None)
      replace(74);
      pendingFinish = -1;
      return false;
    }
    if (prev == 12 && last == 47) { // (12,47)
      // DropLast(47)
      dropLast();
      replace(47);
      return true;
    }
    if (prev == 12 && last == 54) { // (12,54)
      // DropLast(12)
      dropLast();
      return true;
    }
    if (prev == 12 && last == 58) { // (12,58)
      // ReplaceEdge(12,14,Some(12))
      replace(14);
      pendingFinish = 12;
      return false;
    }
    if (prev == 12 && last == 64) { // (12,64)
      // DropLast(12)
      dropLast();
      return true;
    }
    if (prev == 12 && last == 66) { // (12,66)
      // ReplaceEdge(12,104,None)
      replace(104);
      pendingFinish = -1;
      return false;
    }
    if (prev == 12 && last == 68) { // (12,68)
      // ReplaceEdge(12,71,Some(12))
      replace(71);
      pendingFinish = 12;
      return false;
    }
    if (prev == 12 && last == 69) { // (12,69)
      // ReplaceEdge(12,92,Some(12))
      replace(92);
      pendingFinish = 12;
      return false;
    }
    if (prev == 12 && last == 71) { // (12,71)
      // ReplaceEdge(12,94,Some(12))
      replace(94);
      pendingFinish = 12;
      return false;
    }
    if (prev == 12 && last == 73) { // (12,73)
      // ReplaceEdge(12,14,Some(12))
      replace(14);
      pendingFinish = 12;
      return false;
    }
    if (prev == 12 && last == 74) { // (12,74)
      // DropLast(12)
      dropLast();
      return true;
    }
    if (prev == 12 && last == 77) { // (12,77)
      // ReplaceEdge(12,71,Some(12))
      replace(71);
      pendingFinish = 12;
      return false;
    }
    if (prev == 12 && last == 80) { // (12,80)
      // DropLast(12)
      dropLast();
      return true;
    }
    if (prev == 12 && last == 83) { // (12,83)
      // ReplaceEdge(12,69,Some(12))
      replace(69);
      pendingFinish = 12;
      return false;
    }
    if (prev == 12 && last == 86) { // (12,86)
      // ReplaceEdge(12,69,Some(12))
      replace(69);
      pendingFinish = 12;
      return false;
    }
    if (prev == 12 && last == 88) { // (12,88)
      // ReplaceEdge(12,69,None)
      replace(69);
      pendingFinish = -1;
      return false;
    }
    if (prev == 12 && last == 89) { // (12,89)
      // ReplaceEdge(12,92,None)
      replace(92);
      pendingFinish = -1;
      return false;
    }
    if (prev == 12 && last == 92) { // (12,92)
      // DropLast(12)
      dropLast();
      return true;
    }
    if (prev == 12 && last == 93) { // (12,93)
      // ReplaceEdge(12,94,None)
      replace(94);
      pendingFinish = -1;
      return false;
    }
    if (prev == 12 && last == 94) { // (12,94)
      // DropLast(12)
      dropLast();
      return true;
    }
    if (prev == 12 && last == 104) { // (12,104)
      // DropLast(12)
      dropLast();
      return true;
    }
    if (prev == 15 && last == 11) { // (15,11)
      // DropLast(15)
      dropLast();
      return true;
    }
    if (prev == 16 && last == 17) { // (16,17)
      // DropLast(16)
      dropLast();
      return true;
    }
    if (prev == 17 && last == 50) { // (17,50)
      // ReplaceEdge(17,50,Some(17))
      pendingFinish = 17;
      return false;
    }
    if (prev == 18 && last == 19) { // (18,19)
      // ReplaceEdge(18,49,Some(18))
      replace(49);
      pendingFinish = 18;
      return false;
    }
    if (prev == 18 && last == 20) { // (18,20)
      // ReplaceEdge(18,52,Some(18))
      replace(52);
      pendingFinish = 18;
      return false;
    }
    if (prev == 18 && last == 47) { // (18,47)
      // DropLast(47)
      dropLast();
      replace(47);
      return true;
    }
    if (prev == 18 && last == 48) { // (18,48)
      // ReplaceEdge(18,49,None)
      replace(49);
      pendingFinish = -1;
      return false;
    }
    if (prev == 18 && last == 49) { // (18,49)
      // DropLast(18)
      dropLast();
      return true;
    }
    if (prev == 18 && last == 51) { // (18,51)
      // ReplaceEdge(18,52,None)
      replace(52);
      pendingFinish = -1;
      return false;
    }
    if (prev == 18 && last == 52) { // (18,52)
      // DropLast(18)
      dropLast();
      return true;
    }
    if (prev == 21 && last == 22) { // (21,22)
      // ReplaceEdge(21,24,Some(21))
      replace(24);
      pendingFinish = 21;
      return false;
    }
    if (prev == 21 && last == 24) { // (21,24)
      // DropLast(21)
      dropLast();
      return true;
    }
    if (prev == 22 && last == 65) { // (22,65)
      // ReplaceEdge(22,109,None)
      replace(109);
      pendingFinish = -1;
      return false;
    }
    if (prev == 22 && last == 109) { // (22,109)
      // ReplaceEdge(22,124,None)
      replace(124);
      pendingFinish = -1;
      return false;
    }
    if (prev == 22 && last == 124) { // (22,124)
      // ReplaceEdge(22,137,None)
      replace(137);
      pendingFinish = -1;
      return false;
    }
    if (prev == 22 && last == 137) { // (22,137)
      // DropLast(22)
      dropLast();
      return true;
    }
    if (prev == 24 && last == 22) { // (24,22)
      // ReplaceEdge(24,24,Some(24))
      replace(24);
      pendingFinish = 24;
      return false;
    }
    if (prev == 24 && last == 24) { // (24,24)
      // DropLast(24)
      dropLast();
      return true;
    }
    if (prev == 25 && last == 26) { // (25,26)
      // DropLast(25)
      dropLast();
      return true;
    }
    if (prev == 26 && last == 26) { // (26,26)
      // DropLast(26)
      dropLast();
      return true;
    }
    if (prev == 27 && last == 11) { // (27,11)
      // DropLast(27)
      dropLast();
      return true;
    }
    if (prev == 28 && last == 2) { // (28,2)
      // ReplaceEdge(28,85,Some(28))
      replace(85);
      pendingFinish = 28;
      return false;
    }
    if (prev == 28 && last == 3) { // (28,3)
      // ReplaceEdge(28,87,Some(28))
      replace(87);
      pendingFinish = 28;
      return false;
    }
    if (prev == 28 && last == 5) { // (28,5)
      // ReplaceEdge(28,42,None)
      replace(42);
      pendingFinish = -1;
      return false;
    }
    if (prev == 28 && last == 6) { // (28,6)
      // ReplaceEdge(28,67,Some(28))
      replace(67);
      pendingFinish = 28;
      return false;
    }
    if (prev == 28 && last == 7) { // (28,7)
      // ReplaceEdge(28,43,None)
      replace(43);
      pendingFinish = -1;
      return false;
    }
    if (prev == 28 && last == 9) { // (28,9)
      // ReplaceEdge(28,79,Some(28))
      replace(79);
      pendingFinish = 28;
      return false;
    }
    if (prev == 28 && last == 10) { // (28,10)
      // ReplaceEdge(28,40,None)
      replace(40);
      pendingFinish = -1;
      return false;
    }
    if (prev == 28 && last == 16) { // (28,16)
      // ReplaceEdge(28,112,Some(28))
      replace(112);
      pendingFinish = 28;
      return false;
    }
    if (prev == 28 && last == 18) { // (28,18)
      // ReplaceEdge(28,75,Some(28))
      replace(75);
      pendingFinish = 28;
      return false;
    }
    if (prev == 28 && last == 21) { // (28,21)
      // ReplaceEdge(28,23,None)
      replace(23);
      pendingFinish = -1;
      return false;
    }
    if (prev == 28 && last == 23) { // (28,23)
      // ReplaceEdge(28,75,Some(28))
      replace(75);
      pendingFinish = 28;
      return false;
    }
    if (prev == 28 && last == 25) { // (28,25)
      // ReplaceEdge(28,31,Some(28))
      replace(31);
      pendingFinish = 28;
      return false;
    }
    if (prev == 28 && last == 27) { // (28,27)
      // ReplaceEdge(28,68,None)
      replace(68);
      pendingFinish = -1;
      return false;
    }
    if (prev == 28 && last == 28) { // (28,28)
      // DropLast(28)
      dropLast();
      return true;
    }
    if (prev == 28 && last == 29) { // (28,29)
      // ReplaceEdge(28,84,Some(28))
      replace(84);
      pendingFinish = 28;
      return false;
    }
    if (prev == 28 && last == 30) { // (28,30)
      // ReplaceEdge(28,75,Some(28))
      replace(75);
      pendingFinish = 28;
      return false;
    }
    if (prev == 28 && last == 31) { // (28,31)
      // ReplaceEdge(28,81,Some(28))
      replace(81);
      pendingFinish = 28;
      return false;
    }
    if (prev == 28 && last == 32) { // (28,32)
      // ReplaceEdge(28,31,Some(28))
      replace(31);
      pendingFinish = 28;
      return false;
    }
    if (prev == 28 && last == 33) { // (28,33)
      // ReplaceEdge(28,83,None)
      replace(83);
      pendingFinish = -1;
      return false;
    }
    if (prev == 28 && last == 35) { // (28,35)
      // ReplaceEdge(28,86,None)
      replace(86);
      pendingFinish = -1;
      return false;
    }
    if (prev == 28 && last == 36) { // (28,36)
      // ReplaceEdge(28,69,None)
      replace(69);
      pendingFinish = -1;
      return false;
    }
    if (prev == 28 && last == 37) { // (28,37)
      // ReplaceEdge(28,75,Some(28))
      replace(75);
      pendingFinish = 28;
      return false;
    }
    if (prev == 28 && last == 40) { // (28,40)
      // ReplaceEdge(28,66,None)
      replace(66);
      pendingFinish = -1;
      return false;
    }
    if (prev == 28 && last == 42) { // (28,42)
      // ReplaceEdge(28,80,None)
      replace(80);
      pendingFinish = -1;
      return false;
    }
    if (prev == 28 && last == 43) { // (28,43)
      // ReplaceEdge(28,74,None)
      replace(74);
      pendingFinish = -1;
      return false;
    }
    if (prev == 28 && last == 47) { // (28,47)
      // DropLast(47)
      dropLast();
      replace(47);
      return true;
    }
    if (prev == 28 && last == 58) { // (28,58)
      // ReplaceEdge(28,31,Some(28))
      replace(31);
      pendingFinish = 28;
      return false;
    }
    if (prev == 28 && last == 60) { // (28,60)
      // ReplaceEdge(28,61,None)
      replace(61);
      pendingFinish = -1;
      return false;
    }
    if (prev == 28 && last == 61) { // (28,61)
      // ReplaceEdge(28,100,None)
      replace(100);
      pendingFinish = -1;
      return false;
    }
    if (prev == 28 && last == 64) { // (28,64)
      // ReplaceEdge(28,75,Some(28))
      replace(75);
      pendingFinish = 28;
      return false;
    }
    if (prev == 28 && last == 66) { // (28,66)
      // ReplaceEdge(28,104,None)
      replace(104);
      pendingFinish = -1;
      return false;
    }
    if (prev == 28 && last == 67) { // (28,67)
      // ReplaceEdge(28,84,Some(28))
      replace(84);
      pendingFinish = 28;
      return false;
    }
    if (prev == 28 && last == 68) { // (28,68)
      // ReplaceEdge(28,75,Some(28))
      replace(75);
      pendingFinish = 28;
      return false;
    }
    if (prev == 28 && last == 69) { // (28,69)
      // ReplaceEdge(28,107,Some(28))
      replace(107);
      pendingFinish = 28;
      return false;
    }
    if (prev == 28 && last == 74) { // (28,74)
      // ReplaceEdge(28,75,Some(28))
      replace(75);
      pendingFinish = 28;
      return false;
    }
    if (prev == 28 && last == 75) { // (28,75)
      // ReplaceEdge(28,105,None)
      replace(105);
      pendingFinish = -1;
      return false;
    }
    if (prev == 28 && last == 79) { // (28,79)
      // ReplaceEdge(28,110,Some(28))
      replace(110);
      pendingFinish = 28;
      return false;
    }
    if (prev == 28 && last == 80) { // (28,80)
      // ReplaceEdge(28,75,Some(28))
      replace(75);
      pendingFinish = 28;
      return false;
    }
    if (prev == 28 && last == 81) { // (28,81)
      // ReplaceEdge(28,103,Some(28))
      replace(103);
      pendingFinish = 28;
      return false;
    }
    if (prev == 28 && last == 83) { // (28,83)
      // ReplaceEdge(28,101,Some(28))
      replace(101);
      pendingFinish = 28;
      return false;
    }
    if (prev == 28 && last == 84) { // (28,84)
      // ReplaceEdge(28,81,Some(28))
      replace(81);
      pendingFinish = 28;
      return false;
    }
    if (prev == 28 && last == 85) { // (28,85)
      // ReplaceEdge(28,103,Some(28))
      replace(103);
      pendingFinish = 28;
      return false;
    }
    if (prev == 28 && last == 86) { // (28,86)
      // ReplaceEdge(28,101,Some(28))
      replace(101);
      pendingFinish = 28;
      return false;
    }
    if (prev == 28 && last == 87) { // (28,87)
      // ReplaceEdge(28,103,Some(28))
      replace(103);
      pendingFinish = 28;
      return false;
    }
    if (prev == 28 && last == 88) { // (28,88)
      // ReplaceEdge(28,69,None)
      replace(69);
      pendingFinish = -1;
      return false;
    }
    if (prev == 28 && last == 89) { // (28,89)
      // ReplaceEdge(28,92,None)
      replace(92);
      pendingFinish = -1;
      return false;
    }
    if (prev == 28 && last == 92) { // (28,92)
      // ReplaceEdge(28,75,Some(28))
      replace(75);
      pendingFinish = 28;
      return false;
    }
    if (prev == 28 && last == 95) { // (28,95)
      // ReplaceEdge(28,119,None)
      replace(119);
      pendingFinish = -1;
      return false;
    }
    if (prev == 28 && last == 96) { // (28,96)
      // DropLast(28)
      dropLast();
      return true;
    }
    if (prev == 28 && last == 98) { // (28,98)
      // ReplaceEdge(28,126,None)
      replace(126);
      pendingFinish = -1;
      return false;
    }
    if (prev == 28 && last == 99) { // (28,99)
      // ReplaceEdge(28,122,None)
      replace(122);
      pendingFinish = -1;
      return false;
    }
    if (prev == 28 && last == 100) { // (28,100)
      // ReplaceEdge(28,96,Some(28))
      replace(96);
      pendingFinish = 28;
      return false;
    }
    if (prev == 28 && last == 101) { // (28,101)
      // ReplaceEdge(28,125,Some(28))
      replace(125);
      pendingFinish = 28;
      return false;
    }
    if (prev == 28 && last == 103) { // (28,103)
      // ReplaceEdge(28,105,Some(28))
      replace(105);
      pendingFinish = 28;
      return false;
    }
    if (prev == 28 && last == 104) { // (28,104)
      // ReplaceEdge(28,75,Some(28))
      replace(75);
      pendingFinish = 28;
      return false;
    }
    if (prev == 28 && last == 105) { // (28,105)
      // ReplaceEdge(28,100,Some(28))
      replace(100);
      pendingFinish = 28;
      return false;
    }
    if (prev == 28 && last == 107) { // (28,107)
      // ReplaceEdge(28,103,Some(28))
      replace(103);
      pendingFinish = 28;
      return false;
    }
    if (prev == 28 && last == 110) { // (28,110)
      // ReplaceEdge(28,125,Some(28))
      replace(125);
      pendingFinish = 28;
      return false;
    }
    if (prev == 28 && last == 112) { // (28,112)
      // ReplaceEdge(28,103,Some(28))
      replace(103);
      pendingFinish = 28;
      return false;
    }
    if (prev == 28 && last == 113) { // (28,113)
      // ReplaceEdge(28,129,None)
      replace(129);
      pendingFinish = -1;
      return false;
    }
    if (prev == 28 && last == 114) { // (28,114)
      // ReplaceEdge(28,96,None)
      replace(96);
      pendingFinish = -1;
      return false;
    }
    if (prev == 28 && last == 119) { // (28,119)
      // ReplaceEdge(28,100,Some(28))
      replace(100);
      pendingFinish = 28;
      return false;
    }
    if (prev == 28 && last == 122) { // (28,122)
      // ReplaceEdge(28,103,Some(28))
      replace(103);
      pendingFinish = 28;
      return false;
    }
    if (prev == 28 && last == 125) { // (28,125)
      // ReplaceEdge(28,103,Some(28))
      replace(103);
      pendingFinish = 28;
      return false;
    }
    if (prev == 28 && last == 126) { // (28,126)
      // ReplaceEdge(28,125,Some(28))
      replace(125);
      pendingFinish = 28;
      return false;
    }
    if (prev == 28 && last == 129) { // (28,129)
      // ReplaceEdge(28,103,Some(28))
      replace(103);
      pendingFinish = 28;
      return false;
    }
    if (prev == 33 && last == 11) { // (33,11)
      // ReplaceEdge(33,59,Some(35))
      replace(59);
      pendingFinish = 35;
      return false;
    }
    if (prev == 33 && last == 34) { // (33,34)
      // ReplaceEdge(33,70,Some(35))
      replace(70);
      pendingFinish = 35;
      return false;
    }
    if (prev == 33 && last == 47) { // (33,47)
      // DropLast(47)
      dropLast();
      replace(47);
      return true;
    }
    if (prev == 33 && last == 59) { // (33,59)
      // ReplaceEdge(36,72,None)
      dropLast();
      replace(36);
      append(72);
      pendingFinish = -1;
      return false;
    }
    if (prev == 33 && last == 70) { // (33,70)
      // ReplaceEdge(36,102,None)
      dropLast();
      replace(36);
      append(102);
      pendingFinish = -1;
      return false;
    }
    if (prev == 33 && last == 90) { // (33,90)
      // ReplaceEdge(36,91,None)
      dropLast();
      replace(36);
      append(91);
      pendingFinish = -1;
      return false;
    }
    if (prev == 33 && last == 91) { // (33,91)
      // ReplaceEdge(36,117,None)
      dropLast();
      replace(36);
      append(117);
      pendingFinish = -1;
      return false;
    }
    if (prev == 35 && last == 11) { // (35,11)
      // ReplaceEdge(35,59,Some(35))
      replace(59);
      pendingFinish = 35;
      return false;
    }
    if (prev == 35 && last == 34) { // (35,34)
      // ReplaceEdge(35,70,Some(35))
      replace(70);
      pendingFinish = 35;
      return false;
    }
    if (prev == 35 && last == 47) { // (35,47)
      // DropLast(47)
      dropLast();
      replace(47);
      return true;
    }
    if (prev == 35 && last == 59) { // (35,59)
      // ReplaceEdge(47,72,None)
      dropLast();
      replace(47);
      append(72);
      pendingFinish = -1;
      return false;
    }
    if (prev == 35 && last == 70) { // (35,70)
      // ReplaceEdge(47,102,None)
      dropLast();
      replace(47);
      append(102);
      pendingFinish = -1;
      return false;
    }
    if (prev == 35 && last == 90) { // (35,90)
      // ReplaceEdge(47,91,None)
      dropLast();
      replace(47);
      append(91);
      pendingFinish = -1;
      return false;
    }
    if (prev == 35 && last == 91) { // (35,91)
      // ReplaceEdge(47,117,None)
      dropLast();
      replace(47);
      append(117);
      pendingFinish = -1;
      return false;
    }
    if (prev == 36 && last == 2) { // (36,2)
      // ReplaceEdge(36,78,None)
      replace(78);
      pendingFinish = -1;
      return false;
    }
    if (prev == 36 && last == 11) { // (36,11)
      // ReplaceEdge(36,59,None)
      replace(59);
      pendingFinish = -1;
      return false;
    }
    if (prev == 36 && last == 21) { // (36,21)
      // ReplaceEdge(36,23,None)
      replace(23);
      pendingFinish = -1;
      return false;
    }
    if (prev == 36 && last == 23) { // (36,23)
      // ReplaceEdge(36,72,None)
      replace(72);
      pendingFinish = -1;
      return false;
    }
    if (prev == 36 && last == 34) { // (36,34)
      // ReplaceEdge(36,70,None)
      replace(70);
      pendingFinish = -1;
      return false;
    }
    if (prev == 36 && last == 47) { // (36,47)
      // DropLast(47)
      dropLast();
      replace(47);
      return true;
    }
    if (prev == 36 && last == 59) { // (36,59)
      // ReplaceEdge(36,72,None)
      replace(72);
      pendingFinish = -1;
      return false;
    }
    if (prev == 36 && last == 70) { // (36,70)
      // ReplaceEdge(36,102,None)
      replace(102);
      pendingFinish = -1;
      return false;
    }
    if (prev == 36 && last == 72) { // (36,72)
      // ReplaceEdge(36,111,None)
      replace(111);
      pendingFinish = -1;
      return false;
    }
    if (prev == 36 && last == 78) { // (36,78)
      // ReplaceEdge(36,102,None)
      replace(102);
      pendingFinish = -1;
      return false;
    }
    if (prev == 36 && last == 90) { // (36,90)
      // ReplaceEdge(36,91,None)
      replace(91);
      pendingFinish = -1;
      return false;
    }
    if (prev == 36 && last == 91) { // (36,91)
      // ReplaceEdge(36,117,None)
      replace(117);
      pendingFinish = -1;
      return false;
    }
    if (prev == 36 && last == 102) { // (36,102)
      // ReplaceEdge(36,121,Some(36))
      replace(121);
      pendingFinish = 36;
      return false;
    }
    if (prev == 36 && last == 111) { // (36,111)
      // ReplaceEdge(36,123,Some(36))
      replace(123);
      pendingFinish = 36;
      return false;
    }
    if (prev == 36 && last == 115) { // (36,115)
      // ReplaceEdge(36,131,None)
      replace(131);
      pendingFinish = -1;
      return false;
    }
    if (prev == 36 && last == 116) { // (36,116)
      // ReplaceEdge(36,132,Some(36))
      replace(132);
      pendingFinish = 36;
      return false;
    }
    if (prev == 36 && last == 117) { // (36,117)
      // ReplaceEdge(36,133,Some(36))
      replace(133);
      pendingFinish = 36;
      return false;
    }
    if (prev == 36 && last == 118) { // (36,118)
      // ReplaceEdge(36,116,None)
      replace(116);
      pendingFinish = -1;
      return false;
    }
    if (prev == 36 && last == 121) { // (36,121)
      // ReplaceEdge(36,135,Some(36))
      replace(135);
      pendingFinish = 36;
      return false;
    }
    if (prev == 36 && last == 123) { // (36,123)
      // ReplaceEdge(36,136,Some(36))
      replace(136);
      pendingFinish = 36;
      return false;
    }
    if (prev == 36 && last == 127) { // (36,127)
      // ReplaceEdge(36,150,None)
      replace(150);
      pendingFinish = -1;
      return false;
    }
    if (prev == 36 && last == 128) { // (36,128)
      // ReplaceEdge(36,138,None)
      replace(138);
      pendingFinish = -1;
      return false;
    }
    if (prev == 36 && last == 131) { // (36,131)
      // ReplaceEdge(36,123,Some(36))
      replace(123);
      pendingFinish = 36;
      return false;
    }
    if (prev == 36 && last == 132) { // (36,132)
      // ReplaceEdge(36,148,None)
      replace(148);
      pendingFinish = -1;
      return false;
    }
    if (prev == 36 && last == 133) { // (36,133)
      // ReplaceEdge(36,147,Some(36))
      replace(147);
      pendingFinish = 36;
      return false;
    }
    if (prev == 36 && last == 135) { // (36,135)
      // ReplaceEdge(36,136,Some(36))
      replace(136);
      pendingFinish = 36;
      return false;
    }
    if (prev == 36 && last == 136) { // (36,136)
      // ReplaceEdge(36,147,Some(36))
      replace(147);
      pendingFinish = 36;
      return false;
    }
    if (prev == 36 && last == 138) { // (36,138)
      // ReplaceEdge(36,151,Some(36))
      replace(151);
      pendingFinish = 36;
      return false;
    }
    if (prev == 36 && last == 139) { // (36,139)
      // ReplaceEdge(36,128,None)
      replace(128);
      pendingFinish = -1;
      return false;
    }
    if (prev == 36 && last == 140) { // (36,140)
      // ReplaceEdge(36,156,Some(36))
      replace(156);
      pendingFinish = 36;
      return false;
    }
    if (prev == 36 && last == 141) { // (36,141)
      // ReplaceEdge(36,156,None)
      replace(156);
      pendingFinish = -1;
      return false;
    }
    if (prev == 36 && last == 142) { // (36,142)
      // ReplaceEdge(36,132,Some(36))
      replace(132);
      pendingFinish = 36;
      return false;
    }
    if (prev == 36 && last == 144) { // (36,144)
      // ReplaceEdge(36,157,Some(36))
      replace(157);
      pendingFinish = 36;
      return false;
    }
    if (prev == 36 && last == 145) { // (36,145)
      // ReplaceEdge(36,157,None)
      replace(157);
      pendingFinish = -1;
      return false;
    }
    if (prev == 36 && last == 147) { // (36,147)
      // ReplaceEdge(36,148,Some(36))
      replace(148);
      pendingFinish = 36;
      return false;
    }
    if (prev == 36 && last == 148) { // (36,148)
      // ReplaceEdge(36,138,Some(36))
      replace(138);
      pendingFinish = 36;
      return false;
    }
    if (prev == 36 && last == 150) { // (36,150)
      // ReplaceEdge(36,147,Some(36))
      replace(147);
      pendingFinish = 36;
      return false;
    }
    if (prev == 36 && last == 151) { // (36,151)
      // DropLast(36)
      dropLast();
      return true;
    }
    if (prev == 36 && last == 154) { // (36,154)
      // ReplaceEdge(36,151,None)
      replace(151);
      pendingFinish = -1;
      return false;
    }
    if (prev == 36 && last == 156) { // (36,156)
      // ReplaceEdge(36,138,Some(36))
      replace(138);
      pendingFinish = 36;
      return false;
    }
    if (prev == 36 && last == 157) { // (36,157)
      // ReplaceEdge(36,147,Some(36))
      replace(147);
      pendingFinish = 36;
      return false;
    }
    if (prev == 47 && last == 2) { // (47,2)
      // DropLast(47)
      dropLast();
      return true;
    }
    if (prev == 47 && last == 3) { // (47,3)
      // DropLast(47)
      dropLast();
      return true;
    }
    if (prev == 47 && last == 5) { // (47,5)
      // DropLast(47)
      dropLast();
      return true;
    }
    if (prev == 47 && last == 6) { // (47,6)
      // DropLast(47)
      dropLast();
      return true;
    }
    if (prev == 47 && last == 7) { // (47,7)
      // DropLast(47)
      dropLast();
      return true;
    }
    if (prev == 47 && last == 9) { // (47,9)
      // DropLast(47)
      dropLast();
      return true;
    }
    if (prev == 47 && last == 10) { // (47,10)
      // DropLast(47)
      dropLast();
      return true;
    }
    if (prev == 47 && last == 13) { // (47,13)
      // DropLast(47)
      dropLast();
      return true;
    }
    if (prev == 47 && last == 14) { // (47,14)
      // DropLast(47)
      dropLast();
      return true;
    }
    if (prev == 47 && last == 16) { // (47,16)
      // DropLast(47)
      dropLast();
      return true;
    }
    if (prev == 47 && last == 18) { // (47,18)
      // DropLast(47)
      dropLast();
      return true;
    }
    if (prev == 47 && last == 19) { // (47,19)
      // DropLast(47)
      dropLast();
      return true;
    }
    if (prev == 47 && last == 20) { // (47,20)
      // DropLast(47)
      dropLast();
      return true;
    }
    if (prev == 47 && last == 21) { // (47,21)
      // DropLast(47)
      dropLast();
      return true;
    }
    if (prev == 47 && last == 23) { // (47,23)
      // DropLast(47)
      dropLast();
      return true;
    }
    if (prev == 47 && last == 25) { // (47,25)
      // DropLast(47)
      dropLast();
      return true;
    }
    if (prev == 47 && last == 27) { // (47,27)
      // DropLast(47)
      dropLast();
      return true;
    }
    if (prev == 47 && last == 28) { // (47,28)
      // DropLast(47)
      dropLast();
      return true;
    }
    if (prev == 47 && last == 29) { // (47,29)
      // DropLast(47)
      dropLast();
      return true;
    }
    if (prev == 47 && last == 30) { // (47,30)
      // DropLast(47)
      dropLast();
      return true;
    }
    if (prev == 47 && last == 31) { // (47,31)
      // DropLast(47)
      dropLast();
      return true;
    }
    if (prev == 47 && last == 32) { // (47,32)
      // DropLast(47)
      dropLast();
      return true;
    }
    if (prev == 47 && last == 33) { // (47,33)
      // DropLast(47)
      dropLast();
      return true;
    }
    if (prev == 47 && last == 35) { // (47,35)
      // DropLast(47)
      dropLast();
      return true;
    }
    if (prev == 47 && last == 36) { // (47,36)
      // DropLast(47)
      dropLast();
      return true;
    }
    if (prev == 47 && last == 37) { // (47,37)
      // DropLast(47)
      dropLast();
      return true;
    }
    if (prev == 47 && last == 47) { // (47,47)
      // DropLast(47)
      dropLast();
      return true;
    }
    if (prev == 47 && last == 48) { // (47,48)
      // DropLast(47)
      dropLast();
      return true;
    }
    if (prev == 47 && last == 49) { // (47,49)
      // DropLast(47)
      dropLast();
      return true;
    }
    if (prev == 47 && last == 50) { // (47,50)
      // DropLast(47)
      dropLast();
      return true;
    }
    if (prev == 47 && last == 51) { // (47,51)
      // DropLast(47)
      dropLast();
      return true;
    }
    if (prev == 47 && last == 52) { // (47,52)
      // DropLast(47)
      dropLast();
      return true;
    }
    if (prev == 47 && last == 55) { // (47,55)
      // DropLast(47)
      dropLast();
      return true;
    }
    if (prev == 47 && last == 56) { // (47,56)
      // DropLast(47)
      dropLast();
      return true;
    }
    if (prev == 47 && last == 57) { // (47,57)
      // DropLast(47)
      dropLast();
      return true;
    }
    if (prev == 47 && last == 60) { // (47,60)
      // DropLast(47)
      dropLast();
      return true;
    }
    if (prev == 47 && last == 61) { // (47,61)
      // DropLast(47)
      dropLast();
      return true;
    }
    if (prev == 47 && last == 72) { // (47,72)
      // DropLast(47)
      dropLast();
      return true;
    }
    if (prev == 47 && last == 75) { // (47,75)
      // DropLast(47)
      dropLast();
      return true;
    }
    if (prev == 47 && last == 90) { // (47,90)
      // DropLast(47)
      dropLast();
      return true;
    }
    if (prev == 47 && last == 91) { // (47,91)
      // DropLast(47)
      dropLast();
      return true;
    }
    if (prev == 47 && last == 94) { // (47,94)
      // DropLast(47)
      dropLast();
      return true;
    }
    if (prev == 47 && last == 96) { // (47,96)
      // DropLast(47)
      dropLast();
      return true;
    }
    if (prev == 47 && last == 97) { // (47,97)
      // DropLast(47)
      dropLast();
      return true;
    }
    if (prev == 47 && last == 100) { // (47,100)
      // DropLast(47)
      dropLast();
      return true;
    }
    if (prev == 47 && last == 102) { // (47,102)
      // DropLast(47)
      dropLast();
      return true;
    }
    if (prev == 47 && last == 114) { // (47,114)
      // DropLast(47)
      dropLast();
      return true;
    }
    if (prev == 47 && last == 115) { // (47,115)
      // DropLast(47)
      dropLast();
      return true;
    }
    if (prev == 47 && last == 116) { // (47,116)
      // DropLast(47)
      dropLast();
      return true;
    }
    if (prev == 47 && last == 117) { // (47,117)
      // DropLast(47)
      dropLast();
      return true;
    }
    if (prev == 47 && last == 118) { // (47,118)
      // DropLast(47)
      dropLast();
      return true;
    }
    if (prev == 47 && last == 128) { // (47,128)
      // DropLast(47)
      dropLast();
      return true;
    }
    if (prev == 47 && last == 138) { // (47,138)
      // DropLast(47)
      dropLast();
      return true;
    }
    if (prev == 47 && last == 139) { // (47,139)
      // DropLast(47)
      dropLast();
      return true;
    }
    if (prev == 47 && last == 151) { // (47,151)
      // DropLast(47)
      dropLast();
      return true;
    }
    if (prev == 47 && last == 152) { // (47,152)
      // DropLast(47)
      dropLast();
      return true;
    }
    if (prev == 47 && last == 153) { // (47,153)
      // DropLast(47)
      dropLast();
      return true;
    }
    if (prev == 47 && last == 154) { // (47,154)
      // DropLast(47)
      dropLast();
      return true;
    }
    if (prev == 49 && last == 50) { // (49,50)
      // ReplaceEdge(49,50,Some(49))
      pendingFinish = 49;
      return false;
    }
    if (prev == 52 && last == 50) { // (52,50)
      // ReplaceEdge(52,50,Some(52))
      pendingFinish = 52;
      return false;
    }
    if (prev == 53 && last == 11) { // (53,11)
      // DropLast(53)
      dropLast();
      return true;
    }
    if (prev == 58 && last == 26) { // (58,26)
      // DropLast(58)
      dropLast();
      return true;
    }
    if (prev == 59 && last == 2) { // (59,2)
      // ReplaceEdge(59,23,Some(59))
      replace(23);
      pendingFinish = 59;
      return false;
    }
    if (prev == 59 && last == 21) { // (59,21)
      // ReplaceEdge(59,23,None)
      replace(23);
      pendingFinish = -1;
      return false;
    }
    if (prev == 59 && last == 23) { // (59,23)
      // DropLast(59)
      dropLast();
      return true;
    }
    if (prev == 59 && last == 47) { // (59,47)
      // DropLast(47)
      dropLast();
      replace(47);
      return true;
    }
    if (prev == 60 && last == 11) { // (60,11)
      // DropLast(60)
      dropLast();
      return true;
    }
    if (prev == 62 && last == 11) { // (62,11)
      // ReplaceEdge(62,59,Some(63))
      replace(59);
      pendingFinish = 63;
      return false;
    }
    if (prev == 62 && last == 34) { // (62,34)
      // ReplaceEdge(62,70,Some(63))
      replace(70);
      pendingFinish = 63;
      return false;
    }
    if (prev == 62 && last == 47) { // (62,47)
      // DropLast(47)
      dropLast();
      replace(47);
      return true;
    }
    if (prev == 62 && last == 59) { // (62,59)
      // ReplaceEdge(36,72,None)
      dropLast();
      replace(36);
      append(72);
      pendingFinish = -1;
      return false;
    }
    if (prev == 62 && last == 70) { // (62,70)
      // ReplaceEdge(36,102,None)
      dropLast();
      replace(36);
      append(102);
      pendingFinish = -1;
      return false;
    }
    if (prev == 62 && last == 90) { // (62,90)
      // ReplaceEdge(36,91,None)
      dropLast();
      replace(36);
      append(91);
      pendingFinish = -1;
      return false;
    }
    if (prev == 62 && last == 91) { // (62,91)
      // ReplaceEdge(36,117,None)
      dropLast();
      replace(36);
      append(117);
      pendingFinish = -1;
      return false;
    }
    if (prev == 63 && last == 11) { // (63,11)
      // DropLast(63)
      dropLast();
      return true;
    }
    if (prev == 88 && last == 11) { // (88,11)
      // ReplaceEdge(88,59,Some(89))
      replace(59);
      pendingFinish = 89;
      return false;
    }
    if (prev == 88 && last == 34) { // (88,34)
      // ReplaceEdge(88,70,Some(89))
      replace(70);
      pendingFinish = 89;
      return false;
    }
    if (prev == 88 && last == 47) { // (88,47)
      // DropLast(47)
      dropLast();
      replace(47);
      return true;
    }
    if (prev == 88 && last == 59) { // (88,59)
      // ReplaceEdge(36,72,None)
      dropLast();
      replace(36);
      append(72);
      pendingFinish = -1;
      return false;
    }
    if (prev == 88 && last == 70) { // (88,70)
      // ReplaceEdge(36,102,None)
      dropLast();
      replace(36);
      append(102);
      pendingFinish = -1;
      return false;
    }
    if (prev == 88 && last == 90) { // (88,90)
      // ReplaceEdge(36,91,None)
      dropLast();
      replace(36);
      append(91);
      pendingFinish = -1;
      return false;
    }
    if (prev == 88 && last == 91) { // (88,91)
      // ReplaceEdge(36,117,None)
      dropLast();
      replace(36);
      append(117);
      pendingFinish = -1;
      return false;
    }
    if (prev == 89 && last == 11) { // (89,11)
      // DropLast(89)
      dropLast();
      return true;
    }
    if (prev == 90 && last == 11) { // (90,11)
      // DropLast(90)
      dropLast();
      return true;
    }
    if (prev == 93 && last == 11) { // (93,11)
      // DropLast(93)
      dropLast();
      return true;
    }
    if (prev == 95 && last == 11) { // (95,11)
      // DropLast(95)
      dropLast();
      return true;
    }
    if (prev == 96 && last == 2) { // (96,2)
      // ReplaceEdge(96,85,Some(96))
      replace(85);
      pendingFinish = 96;
      return false;
    }
    if (prev == 96 && last == 3) { // (96,3)
      // ReplaceEdge(96,120,Some(96))
      replace(120);
      pendingFinish = 96;
      return false;
    }
    if (prev == 96 && last == 5) { // (96,5)
      // ReplaceEdge(96,42,None)
      replace(42);
      pendingFinish = -1;
      return false;
    }
    if (prev == 96 && last == 6) { // (96,6)
      // ReplaceEdge(96,67,Some(96))
      replace(67);
      pendingFinish = 96;
      return false;
    }
    if (prev == 96 && last == 7) { // (96,7)
      // ReplaceEdge(96,43,None)
      replace(43);
      pendingFinish = -1;
      return false;
    }
    if (prev == 96 && last == 9) { // (96,9)
      // ReplaceEdge(96,79,Some(96))
      replace(79);
      pendingFinish = 96;
      return false;
    }
    if (prev == 96 && last == 10) { // (96,10)
      // ReplaceEdge(96,40,None)
      replace(40);
      pendingFinish = -1;
      return false;
    }
    if (prev == 96 && last == 16) { // (96,16)
      // ReplaceEdge(96,112,Some(96))
      replace(112);
      pendingFinish = 96;
      return false;
    }
    if (prev == 96 && last == 18) { // (96,18)
      // ReplaceEdge(96,75,Some(96))
      replace(75);
      pendingFinish = 96;
      return false;
    }
    if (prev == 96 && last == 21) { // (96,21)
      // ReplaceEdge(96,23,None)
      replace(23);
      pendingFinish = -1;
      return false;
    }
    if (prev == 96 && last == 23) { // (96,23)
      // ReplaceEdge(96,75,Some(96))
      replace(75);
      pendingFinish = 96;
      return false;
    }
    if (prev == 96 && last == 25) { // (96,25)
      // ReplaceEdge(96,31,Some(96))
      replace(31);
      pendingFinish = 96;
      return false;
    }
    if (prev == 96 && last == 27) { // (96,27)
      // ReplaceEdge(96,68,None)
      replace(68);
      pendingFinish = -1;
      return false;
    }
    if (prev == 96 && last == 28) { // (96,28)
      // ReplaceEdge(96,71,None)
      replace(71);
      pendingFinish = -1;
      return false;
    }
    if (prev == 96 && last == 29) { // (96,29)
      // ReplaceEdge(96,84,Some(96))
      replace(84);
      pendingFinish = 96;
      return false;
    }
    if (prev == 96 && last == 30) { // (96,30)
      // ReplaceEdge(96,75,Some(96))
      replace(75);
      pendingFinish = 96;
      return false;
    }
    if (prev == 96 && last == 31) { // (96,31)
      // ReplaceEdge(96,81,Some(96))
      replace(81);
      pendingFinish = 96;
      return false;
    }
    if (prev == 96 && last == 32) { // (96,32)
      // ReplaceEdge(96,31,Some(96))
      replace(31);
      pendingFinish = 96;
      return false;
    }
    if (prev == 96 && last == 33) { // (96,33)
      // ReplaceEdge(96,83,None)
      replace(83);
      pendingFinish = -1;
      return false;
    }
    if (prev == 96 && last == 35) { // (96,35)
      // ReplaceEdge(96,86,None)
      replace(86);
      pendingFinish = -1;
      return false;
    }
    if (prev == 96 && last == 36) { // (96,36)
      // ReplaceEdge(96,69,None)
      replace(69);
      pendingFinish = -1;
      return false;
    }
    if (prev == 96 && last == 37) { // (96,37)
      // ReplaceEdge(96,75,Some(96))
      replace(75);
      pendingFinish = 96;
      return false;
    }
    if (prev == 96 && last == 40) { // (96,40)
      // ReplaceEdge(96,66,None)
      replace(66);
      pendingFinish = -1;
      return false;
    }
    if (prev == 96 && last == 42) { // (96,42)
      // ReplaceEdge(96,80,None)
      replace(80);
      pendingFinish = -1;
      return false;
    }
    if (prev == 96 && last == 43) { // (96,43)
      // ReplaceEdge(96,74,None)
      replace(74);
      pendingFinish = -1;
      return false;
    }
    if (prev == 96 && last == 47) { // (96,47)
      // DropLast(47)
      dropLast();
      replace(47);
      return true;
    }
    if (prev == 96 && last == 54) { // (96,54)
      // ReplaceEdge(96,75,Some(96))
      replace(75);
      pendingFinish = 96;
      return false;
    }
    if (prev == 96 && last == 56) { // (96,56)
      // ReplaceEdge(96,146,Some(96))
      replace(146);
      pendingFinish = 96;
      return false;
    }
    if (prev == 96 && last == 58) { // (96,58)
      // ReplaceEdge(96,31,Some(96))
      replace(31);
      pendingFinish = 96;
      return false;
    }
    if (prev == 96 && last == 60) { // (96,60)
      // ReplaceEdge(96,61,None)
      replace(61);
      pendingFinish = -1;
      return false;
    }
    if (prev == 96 && last == 61) { // (96,61)
      // ReplaceEdge(96,100,None)
      replace(100);
      pendingFinish = -1;
      return false;
    }
    if (prev == 96 && last == 64) { // (96,64)
      // ReplaceEdge(96,75,Some(96))
      replace(75);
      pendingFinish = 96;
      return false;
    }
    if (prev == 96 && last == 66) { // (96,66)
      // ReplaceEdge(96,104,None)
      replace(104);
      pendingFinish = -1;
      return false;
    }
    if (prev == 96 && last == 67) { // (96,67)
      // ReplaceEdge(96,84,Some(96))
      replace(84);
      pendingFinish = 96;
      return false;
    }
    if (prev == 96 && last == 68) { // (96,68)
      // ReplaceEdge(96,56,Some(96))
      replace(56);
      pendingFinish = 96;
      return false;
    }
    if (prev == 96 && last == 69) { // (96,69)
      // ReplaceEdge(96,107,Some(96))
      replace(107);
      pendingFinish = 96;
      return false;
    }
    if (prev == 96 && last == 71) { // (96,71)
      // ReplaceEdge(96,134,Some(96))
      replace(134);
      pendingFinish = 96;
      return false;
    }
    if (prev == 96 && last == 74) { // (96,74)
      // ReplaceEdge(96,75,Some(96))
      replace(75);
      pendingFinish = 96;
      return false;
    }
    if (prev == 96 && last == 75) { // (96,75)
      // ReplaceEdge(96,105,None)
      replace(105);
      pendingFinish = -1;
      return false;
    }
    if (prev == 96 && last == 79) { // (96,79)
      // ReplaceEdge(96,110,Some(96))
      replace(110);
      pendingFinish = 96;
      return false;
    }
    if (prev == 96 && last == 80) { // (96,80)
      // ReplaceEdge(96,75,Some(96))
      replace(75);
      pendingFinish = 96;
      return false;
    }
    if (prev == 96 && last == 81) { // (96,81)
      // ReplaceEdge(96,103,Some(96))
      replace(103);
      pendingFinish = 96;
      return false;
    }
    if (prev == 96 && last == 83) { // (96,83)
      // ReplaceEdge(96,101,Some(96))
      replace(101);
      pendingFinish = 96;
      return false;
    }
    if (prev == 96 && last == 84) { // (96,84)
      // ReplaceEdge(96,81,Some(96))
      replace(81);
      pendingFinish = 96;
      return false;
    }
    if (prev == 96 && last == 85) { // (96,85)
      // ReplaceEdge(96,103,Some(96))
      replace(103);
      pendingFinish = 96;
      return false;
    }
    if (prev == 96 && last == 86) { // (96,86)
      // ReplaceEdge(96,101,Some(96))
      replace(101);
      pendingFinish = 96;
      return false;
    }
    if (prev == 96 && last == 88) { // (96,88)
      // ReplaceEdge(96,69,None)
      replace(69);
      pendingFinish = -1;
      return false;
    }
    if (prev == 96 && last == 89) { // (96,89)
      // ReplaceEdge(96,92,None)
      replace(92);
      pendingFinish = -1;
      return false;
    }
    if (prev == 96 && last == 92) { // (96,92)
      // ReplaceEdge(96,75,Some(96))
      replace(75);
      pendingFinish = 96;
      return false;
    }
    if (prev == 96 && last == 93) { // (96,93)
      // ReplaceEdge(96,94,None)
      replace(94);
      pendingFinish = -1;
      return false;
    }
    if (prev == 96 && last == 94) { // (96,94)
      // ReplaceEdge(96,75,Some(96))
      replace(75);
      pendingFinish = 96;
      return false;
    }
    if (prev == 96 && last == 95) { // (96,95)
      // ReplaceEdge(96,119,None)
      replace(119);
      pendingFinish = -1;
      return false;
    }
    if (prev == 96 && last == 96) { // (96,96)
      // DropLast(96)
      dropLast();
      return true;
    }
    if (prev == 96 && last == 97) { // (96,97)
      // ReplaceEdge(96,149,None)
      replace(149);
      pendingFinish = -1;
      return false;
    }
    if (prev == 96 && last == 98) { // (96,98)
      // ReplaceEdge(96,126,None)
      replace(126);
      pendingFinish = -1;
      return false;
    }
    if (prev == 96 && last == 99) { // (96,99)
      // ReplaceEdge(96,122,None)
      replace(122);
      pendingFinish = -1;
      return false;
    }
    if (prev == 96 && last == 100) { // (96,100)
      // ReplaceEdge(96,96,Some(96))
      replace(96);
      pendingFinish = 96;
      return false;
    }
    if (prev == 96 && last == 101) { // (96,101)
      // ReplaceEdge(96,125,Some(96))
      replace(125);
      pendingFinish = 96;
      return false;
    }
    if (prev == 96 && last == 103) { // (96,103)
      // ReplaceEdge(96,105,Some(96))
      replace(105);
      pendingFinish = 96;
      return false;
    }
    if (prev == 96 && last == 104) { // (96,104)
      // ReplaceEdge(96,75,Some(96))
      replace(75);
      pendingFinish = 96;
      return false;
    }
    if (prev == 96 && last == 105) { // (96,105)
      // ReplaceEdge(96,100,Some(96))
      replace(100);
      pendingFinish = 96;
      return false;
    }
    if (prev == 96 && last == 107) { // (96,107)
      // ReplaceEdge(96,103,Some(96))
      replace(103);
      pendingFinish = 96;
      return false;
    }
    if (prev == 96 && last == 110) { // (96,110)
      // ReplaceEdge(96,125,Some(96))
      replace(125);
      pendingFinish = 96;
      return false;
    }
    if (prev == 96 && last == 112) { // (96,112)
      // ReplaceEdge(96,103,Some(96))
      replace(103);
      pendingFinish = 96;
      return false;
    }
    if (prev == 96 && last == 113) { // (96,113)
      // ReplaceEdge(96,129,None)
      replace(129);
      pendingFinish = -1;
      return false;
    }
    if (prev == 96 && last == 114) { // (96,114)
      // ReplaceEdge(96,96,None)
      replace(96);
      pendingFinish = -1;
      return false;
    }
    if (prev == 96 && last == 119) { // (96,119)
      // ReplaceEdge(96,100,Some(96))
      replace(100);
      pendingFinish = 96;
      return false;
    }
    if (prev == 96 && last == 120) { // (96,120)
      // ReplaceEdge(96,130,Some(96))
      replace(130);
      pendingFinish = 96;
      return false;
    }
    if (prev == 96 && last == 122) { // (96,122)
      // ReplaceEdge(96,103,Some(96))
      replace(103);
      pendingFinish = 96;
      return false;
    }
    if (prev == 96 && last == 125) { // (96,125)
      // ReplaceEdge(96,103,Some(96))
      replace(103);
      pendingFinish = 96;
      return false;
    }
    if (prev == 96 && last == 126) { // (96,126)
      // ReplaceEdge(96,125,Some(96))
      replace(125);
      pendingFinish = 96;
      return false;
    }
    if (prev == 96 && last == 129) { // (96,129)
      // ReplaceEdge(96,103,Some(96))
      replace(103);
      pendingFinish = 96;
      return false;
    }
    if (prev == 96 && last == 130) { // (96,130)
      // ReplaceEdge(96,146,Some(96))
      replace(146);
      pendingFinish = 96;
      return false;
    }
    if (prev == 96 && last == 134) { // (96,134)
      // ReplaceEdge(96,103,Some(96))
      replace(103);
      pendingFinish = 96;
      return false;
    }
    if (prev == 96 && last == 143) { // (96,143)
      // ReplaceEdge(96,155,None)
      replace(155);
      pendingFinish = -1;
      return false;
    }
    if (prev == 96 && last == 146) { // (96,146)
      // ReplaceEdge(96,103,Some(96))
      replace(103);
      pendingFinish = 96;
      return false;
    }
    if (prev == 96 && last == 149) { // (96,149)
      // ReplaceEdge(96,103,Some(96))
      replace(103);
      pendingFinish = 96;
      return false;
    }
    if (prev == 96 && last == 155) { // (96,155)
      // ReplaceEdge(96,103,Some(96))
      replace(103);
      pendingFinish = 96;
      return false;
    }
    if (prev == 97 && last == 11) { // (97,11)
      // DropLast(97)
      dropLast();
      return true;
    }
    if (prev == 98 && last == 11) { // (98,11)
      // ReplaceEdge(98,59,Some(99))
      replace(59);
      pendingFinish = 99;
      return false;
    }
    if (prev == 98 && last == 34) { // (98,34)
      // ReplaceEdge(98,70,Some(99))
      replace(70);
      pendingFinish = 99;
      return false;
    }
    if (prev == 98 && last == 47) { // (98,47)
      // DropLast(47)
      dropLast();
      replace(47);
      return true;
    }
    if (prev == 98 && last == 59) { // (98,59)
      // ReplaceEdge(36,72,None)
      dropLast();
      replace(36);
      append(72);
      pendingFinish = -1;
      return false;
    }
    if (prev == 98 && last == 70) { // (98,70)
      // ReplaceEdge(36,102,None)
      dropLast();
      replace(36);
      append(102);
      pendingFinish = -1;
      return false;
    }
    if (prev == 98 && last == 90) { // (98,90)
      // ReplaceEdge(36,91,None)
      dropLast();
      replace(36);
      append(91);
      pendingFinish = -1;
      return false;
    }
    if (prev == 98 && last == 91) { // (98,91)
      // ReplaceEdge(36,117,None)
      dropLast();
      replace(36);
      append(117);
      pendingFinish = -1;
      return false;
    }
    if (prev == 99 && last == 11) { // (99,11)
      // DropLast(99)
      dropLast();
      return true;
    }
    if (prev == 113 && last == 11) { // (113,11)
      // DropLast(113)
      dropLast();
      return true;
    }
    if (prev == 114 && last == 11) { // (114,11)
      // DropLast(114)
      dropLast();
      return true;
    }
    if (prev == 115 && last == 11) { // (115,11)
      // DropLast(115)
      dropLast();
      return true;
    }
    if (prev == 116 && last == 2) { // (116,2)
      // ReplaceEdge(116,23,Some(116))
      replace(23);
      pendingFinish = 116;
      return false;
    }
    if (prev == 116 && last == 3) { // (116,3)
      // ReplaceEdge(116,77,Some(116))
      replace(77);
      pendingFinish = 116;
      return false;
    }
    if (prev == 116 && last == 5) { // (116,5)
      // ReplaceEdge(116,42,None)
      replace(42);
      pendingFinish = -1;
      return false;
    }
    if (prev == 116 && last == 6) { // (116,6)
      // ReplaceEdge(116,73,Some(116))
      replace(73);
      pendingFinish = 116;
      return false;
    }
    if (prev == 116 && last == 7) { // (116,7)
      // ReplaceEdge(116,43,None)
      replace(43);
      pendingFinish = -1;
      return false;
    }
    if (prev == 116 && last == 9) { // (116,9)
      // ReplaceEdge(116,83,Some(116))
      replace(83);
      pendingFinish = 116;
      return false;
    }
    if (prev == 116 && last == 10) { // (116,10)
      // ReplaceEdge(116,40,None)
      replace(40);
      pendingFinish = -1;
      return false;
    }
    if (prev == 116 && last == 13) { // (116,13)
      // ReplaceEdge(116,14,Some(116))
      replace(14);
      pendingFinish = 116;
      return false;
    }
    if (prev == 116 && last == 14) { // (116,14)
      // ReplaceEdge(116,18,Some(116))
      replace(18);
      pendingFinish = 116;
      return false;
    }
    if (prev == 116 && last == 16) { // (116,16)
      // ReplaceEdge(116,18,Some(116))
      replace(18);
      pendingFinish = 116;
      return false;
    }
    if (prev == 116 && last == 18) { // (116,18)
      // DropLast(116)
      dropLast();
      return true;
    }
    if (prev == 116 && last == 21) { // (116,21)
      // ReplaceEdge(116,23,None)
      replace(23);
      pendingFinish = -1;
      return false;
    }
    if (prev == 116 && last == 23) { // (116,23)
      // DropLast(116)
      dropLast();
      return true;
    }
    if (prev == 116 && last == 25) { // (116,25)
      // ReplaceEdge(116,14,Some(116))
      replace(14);
      pendingFinish = 116;
      return false;
    }
    if (prev == 116 && last == 27) { // (116,27)
      // ReplaceEdge(116,68,None)
      replace(68);
      pendingFinish = -1;
      return false;
    }
    if (prev == 116 && last == 28) { // (116,28)
      // ReplaceEdge(116,71,None)
      replace(71);
      pendingFinish = -1;
      return false;
    }
    if (prev == 116 && last == 30) { // (116,30)
      // DropLast(116)
      dropLast();
      return true;
    }
    if (prev == 116 && last == 32) { // (116,32)
      // ReplaceEdge(116,14,Some(116))
      replace(14);
      pendingFinish = 116;
      return false;
    }
    if (prev == 116 && last == 33) { // (116,33)
      // ReplaceEdge(116,83,None)
      replace(83);
      pendingFinish = -1;
      return false;
    }
    if (prev == 116 && last == 35) { // (116,35)
      // ReplaceEdge(116,86,None)
      replace(86);
      pendingFinish = -1;
      return false;
    }
    if (prev == 116 && last == 36) { // (116,36)
      // ReplaceEdge(116,69,None)
      replace(69);
      pendingFinish = -1;
      return false;
    }
    if (prev == 116 && last == 37) { // (116,37)
      // DropLast(116)
      dropLast();
      return true;
    }
    if (prev == 116 && last == 40) { // (116,40)
      // ReplaceEdge(116,66,None)
      replace(66);
      pendingFinish = -1;
      return false;
    }
    if (prev == 116 && last == 42) { // (116,42)
      // ReplaceEdge(116,80,None)
      replace(80);
      pendingFinish = -1;
      return false;
    }
    if (prev == 116 && last == 43) { // (116,43)
      // ReplaceEdge(116,74,None)
      replace(74);
      pendingFinish = -1;
      return false;
    }
    if (prev == 116 && last == 47) { // (116,47)
      // DropLast(47)
      dropLast();
      replace(47);
      return true;
    }
    if (prev == 116 && last == 54) { // (116,54)
      // DropLast(116)
      dropLast();
      return true;
    }
    if (prev == 116 && last == 58) { // (116,58)
      // ReplaceEdge(116,14,Some(116))
      replace(14);
      pendingFinish = 116;
      return false;
    }
    if (prev == 116 && last == 64) { // (116,64)
      // DropLast(116)
      dropLast();
      return true;
    }
    if (prev == 116 && last == 66) { // (116,66)
      // ReplaceEdge(116,104,None)
      replace(104);
      pendingFinish = -1;
      return false;
    }
    if (prev == 116 && last == 68) { // (116,68)
      // ReplaceEdge(116,71,Some(116))
      replace(71);
      pendingFinish = 116;
      return false;
    }
    if (prev == 116 && last == 69) { // (116,69)
      // ReplaceEdge(116,92,Some(116))
      replace(92);
      pendingFinish = 116;
      return false;
    }
    if (prev == 116 && last == 71) { // (116,71)
      // ReplaceEdge(116,94,Some(116))
      replace(94);
      pendingFinish = 116;
      return false;
    }
    if (prev == 116 && last == 73) { // (116,73)
      // ReplaceEdge(116,14,Some(116))
      replace(14);
      pendingFinish = 116;
      return false;
    }
    if (prev == 116 && last == 74) { // (116,74)
      // DropLast(116)
      dropLast();
      return true;
    }
    if (prev == 116 && last == 77) { // (116,77)
      // ReplaceEdge(116,71,Some(116))
      replace(71);
      pendingFinish = 116;
      return false;
    }
    if (prev == 116 && last == 80) { // (116,80)
      // DropLast(116)
      dropLast();
      return true;
    }
    if (prev == 116 && last == 83) { // (116,83)
      // ReplaceEdge(116,69,Some(116))
      replace(69);
      pendingFinish = 116;
      return false;
    }
    if (prev == 116 && last == 86) { // (116,86)
      // ReplaceEdge(116,69,Some(116))
      replace(69);
      pendingFinish = 116;
      return false;
    }
    if (prev == 116 && last == 88) { // (116,88)
      // ReplaceEdge(116,69,None)
      replace(69);
      pendingFinish = -1;
      return false;
    }
    if (prev == 116 && last == 89) { // (116,89)
      // ReplaceEdge(116,92,None)
      replace(92);
      pendingFinish = -1;
      return false;
    }
    if (prev == 116 && last == 92) { // (116,92)
      // DropLast(116)
      dropLast();
      return true;
    }
    if (prev == 116 && last == 93) { // (116,93)
      // ReplaceEdge(116,94,None)
      replace(94);
      pendingFinish = -1;
      return false;
    }
    if (prev == 116 && last == 94) { // (116,94)
      // DropLast(116)
      dropLast();
      return true;
    }
    if (prev == 116 && last == 104) { // (116,104)
      // DropLast(116)
      dropLast();
      return true;
    }
    if (prev == 118 && last == 11) { // (118,11)
      // DropLast(118)
      dropLast();
      return true;
    }
    if (prev == 127 && last == 11) { // (127,11)
      // DropLast(127)
      dropLast();
      return true;
    }
    if (prev == 138 && last == 11) { // (138,11)
      // ReplaceEdge(138,59,Some(154))
      replace(59);
      pendingFinish = 154;
      return false;
    }
    if (prev == 138 && last == 34) { // (138,34)
      // ReplaceEdge(138,70,Some(154))
      replace(70);
      pendingFinish = 154;
      return false;
    }
    if (prev == 138 && last == 47) { // (138,47)
      // DropLast(47)
      dropLast();
      replace(47);
      return true;
    }
    if (prev == 138 && last == 59) { // (138,59)
      // ReplaceEdge(151,72,None)
      dropLast();
      replace(151);
      append(72);
      pendingFinish = -1;
      return false;
    }
    if (prev == 138 && last == 70) { // (138,70)
      // ReplaceEdge(151,102,None)
      dropLast();
      replace(151);
      append(102);
      pendingFinish = -1;
      return false;
    }
    if (prev == 138 && last == 90) { // (138,90)
      // ReplaceEdge(151,91,None)
      dropLast();
      replace(151);
      append(91);
      pendingFinish = -1;
      return false;
    }
    if (prev == 138 && last == 91) { // (138,91)
      // ReplaceEdge(151,117,None)
      dropLast();
      replace(151);
      append(117);
      pendingFinish = -1;
      return false;
    }
    if (prev == 139 && last == 11) { // (139,11)
      // DropLast(139)
      dropLast();
      return true;
    }
    if (prev == 140 && last == 11) { // (140,11)
      // ReplaceEdge(140,59,Some(141))
      replace(59);
      pendingFinish = 141;
      return false;
    }
    if (prev == 140 && last == 34) { // (140,34)
      // ReplaceEdge(140,70,Some(141))
      replace(70);
      pendingFinish = 141;
      return false;
    }
    if (prev == 140 && last == 47) { // (140,47)
      // DropLast(47)
      dropLast();
      replace(47);
      return true;
    }
    if (prev == 140 && last == 59) { // (140,59)
      // ReplaceEdge(151,72,None)
      dropLast();
      replace(151);
      append(72);
      pendingFinish = -1;
      return false;
    }
    if (prev == 140 && last == 70) { // (140,70)
      // ReplaceEdge(151,102,None)
      dropLast();
      replace(151);
      append(102);
      pendingFinish = -1;
      return false;
    }
    if (prev == 140 && last == 90) { // (140,90)
      // ReplaceEdge(151,91,None)
      dropLast();
      replace(151);
      append(91);
      pendingFinish = -1;
      return false;
    }
    if (prev == 140 && last == 91) { // (140,91)
      // ReplaceEdge(151,117,None)
      dropLast();
      replace(151);
      append(117);
      pendingFinish = -1;
      return false;
    }
    if (prev == 141 && last == 11) { // (141,11)
      // ReplaceEdge(141,59,Some(141))
      replace(59);
      pendingFinish = 141;
      return false;
    }
    if (prev == 141 && last == 34) { // (141,34)
      // ReplaceEdge(141,70,Some(141))
      replace(70);
      pendingFinish = 141;
      return false;
    }
    if (prev == 141 && last == 47) { // (141,47)
      // DropLast(47)
      dropLast();
      replace(47);
      return true;
    }
    if (prev == 141 && last == 59) { // (141,59)
      // ReplaceEdge(47,72,None)
      dropLast();
      replace(47);
      append(72);
      pendingFinish = -1;
      return false;
    }
    if (prev == 141 && last == 70) { // (141,70)
      // ReplaceEdge(47,102,None)
      dropLast();
      replace(47);
      append(102);
      pendingFinish = -1;
      return false;
    }
    if (prev == 141 && last == 90) { // (141,90)
      // ReplaceEdge(47,91,None)
      dropLast();
      replace(47);
      append(91);
      pendingFinish = -1;
      return false;
    }
    if (prev == 141 && last == 91) { // (141,91)
      // ReplaceEdge(47,117,None)
      dropLast();
      replace(47);
      append(117);
      pendingFinish = -1;
      return false;
    }
    if (prev == 142 && last == 2) { // (142,2)
      // ReplaceEdge(142,78,Some(116))
      replace(78);
      pendingFinish = 116;
      return false;
    }
    if (prev == 142 && last == 21) { // (142,21)
      // ReplaceEdge(142,23,None)
      replace(23);
      pendingFinish = -1;
      return false;
    }
    if (prev == 142 && last == 23) { // (142,23)
      // ReplaceEdge(142,72,Some(116))
      replace(72);
      pendingFinish = 116;
      return false;
    }
    if (prev == 142 && last == 47) { // (142,47)
      // DropLast(47)
      dropLast();
      replace(47);
      return true;
    }
    if (prev == 142 && last == 72) { // (142,72)
      // ReplaceEdge(151,111,None)
      dropLast();
      replace(151);
      append(111);
      pendingFinish = -1;
      return false;
    }
    if (prev == 142 && last == 78) { // (142,78)
      // ReplaceEdge(142,102,Some(116))
      replace(102);
      pendingFinish = 116;
      return false;
    }
    if (prev == 142 && last == 90) { // (142,90)
      // ReplaceEdge(151,91,None)
      dropLast();
      replace(151);
      append(91);
      pendingFinish = -1;
      return false;
    }
    if (prev == 142 && last == 91) { // (142,91)
      // ReplaceEdge(151,117,None)
      dropLast();
      replace(151);
      append(117);
      pendingFinish = -1;
      return false;
    }
    if (prev == 142 && last == 102) { // (142,102)
      // ReplaceEdge(151,121,Some(151))
      dropLast();
      replace(151);
      append(121);
      pendingFinish = 151;
      return false;
    }
    if (prev == 142 && last == 115) { // (142,115)
      // ReplaceEdge(151,131,None)
      dropLast();
      replace(151);
      append(131);
      pendingFinish = -1;
      return false;
    }
    if (prev == 142 && last == 116) { // (142,116)
      // ReplaceEdge(151,132,Some(151))
      dropLast();
      replace(151);
      append(132);
      pendingFinish = 151;
      return false;
    }
    if (prev == 143 && last == 11) { // (143,11)
      // DropLast(143)
      dropLast();
      return true;
    }
    if (prev == 144 && last == 11) { // (144,11)
      // ReplaceEdge(144,59,Some(145))
      replace(59);
      pendingFinish = 145;
      return false;
    }
    if (prev == 144 && last == 34) { // (144,34)
      // ReplaceEdge(144,70,Some(145))
      replace(70);
      pendingFinish = 145;
      return false;
    }
    if (prev == 144 && last == 47) { // (144,47)
      // DropLast(47)
      dropLast();
      replace(47);
      return true;
    }
    if (prev == 144 && last == 59) { // (144,59)
      // ReplaceEdge(151,72,None)
      dropLast();
      replace(151);
      append(72);
      pendingFinish = -1;
      return false;
    }
    if (prev == 144 && last == 70) { // (144,70)
      // ReplaceEdge(151,102,None)
      dropLast();
      replace(151);
      append(102);
      pendingFinish = -1;
      return false;
    }
    if (prev == 144 && last == 90) { // (144,90)
      // ReplaceEdge(151,91,None)
      dropLast();
      replace(151);
      append(91);
      pendingFinish = -1;
      return false;
    }
    if (prev == 144 && last == 91) { // (144,91)
      // ReplaceEdge(151,117,None)
      dropLast();
      replace(151);
      append(117);
      pendingFinish = -1;
      return false;
    }
    if (prev == 145 && last == 11) { // (145,11)
      // ReplaceEdge(145,59,Some(145))
      replace(59);
      pendingFinish = 145;
      return false;
    }
    if (prev == 145 && last == 34) { // (145,34)
      // ReplaceEdge(145,70,Some(145))
      replace(70);
      pendingFinish = 145;
      return false;
    }
    if (prev == 145 && last == 47) { // (145,47)
      // DropLast(47)
      dropLast();
      replace(47);
      return true;
    }
    if (prev == 145 && last == 59) { // (145,59)
      // ReplaceEdge(47,72,None)
      dropLast();
      replace(47);
      append(72);
      pendingFinish = -1;
      return false;
    }
    if (prev == 145 && last == 70) { // (145,70)
      // ReplaceEdge(47,102,None)
      dropLast();
      replace(47);
      append(102);
      pendingFinish = -1;
      return false;
    }
    if (prev == 145 && last == 90) { // (145,90)
      // ReplaceEdge(47,91,None)
      dropLast();
      replace(47);
      append(91);
      pendingFinish = -1;
      return false;
    }
    if (prev == 145 && last == 91) { // (145,91)
      // ReplaceEdge(47,117,None)
      dropLast();
      replace(47);
      append(117);
      pendingFinish = -1;
      return false;
    }
    if (prev == 151 && last == 2) { // (151,2)
      // ReplaceEdge(151,78,None)
      replace(78);
      pendingFinish = -1;
      return false;
    }
    if (prev == 151 && last == 11) { // (151,11)
      // ReplaceEdge(151,59,None)
      replace(59);
      pendingFinish = -1;
      return false;
    }
    if (prev == 151 && last == 21) { // (151,21)
      // ReplaceEdge(151,23,None)
      replace(23);
      pendingFinish = -1;
      return false;
    }
    if (prev == 151 && last == 23) { // (151,23)
      // ReplaceEdge(151,72,None)
      replace(72);
      pendingFinish = -1;
      return false;
    }
    if (prev == 151 && last == 34) { // (151,34)
      // ReplaceEdge(151,70,None)
      replace(70);
      pendingFinish = -1;
      return false;
    }
    if (prev == 151 && last == 47) { // (151,47)
      // DropLast(47)
      dropLast();
      replace(47);
      return true;
    }
    if (prev == 151 && last == 59) { // (151,59)
      // ReplaceEdge(151,72,None)
      replace(72);
      pendingFinish = -1;
      return false;
    }
    if (prev == 151 && last == 70) { // (151,70)
      // ReplaceEdge(151,102,None)
      replace(102);
      pendingFinish = -1;
      return false;
    }
    if (prev == 151 && last == 72) { // (151,72)
      // ReplaceEdge(151,111,None)
      replace(111);
      pendingFinish = -1;
      return false;
    }
    if (prev == 151 && last == 78) { // (151,78)
      // ReplaceEdge(151,102,None)
      replace(102);
      pendingFinish = -1;
      return false;
    }
    if (prev == 151 && last == 90) { // (151,90)
      // ReplaceEdge(151,91,None)
      replace(91);
      pendingFinish = -1;
      return false;
    }
    if (prev == 151 && last == 91) { // (151,91)
      // ReplaceEdge(151,117,None)
      replace(117);
      pendingFinish = -1;
      return false;
    }
    if (prev == 151 && last == 102) { // (151,102)
      // ReplaceEdge(151,121,Some(151))
      replace(121);
      pendingFinish = 151;
      return false;
    }
    if (prev == 151 && last == 111) { // (151,111)
      // ReplaceEdge(151,123,Some(151))
      replace(123);
      pendingFinish = 151;
      return false;
    }
    if (prev == 151 && last == 115) { // (151,115)
      // ReplaceEdge(151,131,None)
      replace(131);
      pendingFinish = -1;
      return false;
    }
    if (prev == 151 && last == 116) { // (151,116)
      // ReplaceEdge(151,132,Some(151))
      replace(132);
      pendingFinish = 151;
      return false;
    }
    if (prev == 151 && last == 117) { // (151,117)
      // ReplaceEdge(151,133,Some(151))
      replace(133);
      pendingFinish = 151;
      return false;
    }
    if (prev == 151 && last == 118) { // (151,118)
      // ReplaceEdge(151,116,None)
      replace(116);
      pendingFinish = -1;
      return false;
    }
    if (prev == 151 && last == 121) { // (151,121)
      // ReplaceEdge(151,135,Some(151))
      replace(135);
      pendingFinish = 151;
      return false;
    }
    if (prev == 151 && last == 123) { // (151,123)
      // ReplaceEdge(151,136,Some(151))
      replace(136);
      pendingFinish = 151;
      return false;
    }
    if (prev == 151 && last == 127) { // (151,127)
      // ReplaceEdge(151,150,None)
      replace(150);
      pendingFinish = -1;
      return false;
    }
    if (prev == 151 && last == 128) { // (151,128)
      // ReplaceEdge(151,138,None)
      replace(138);
      pendingFinish = -1;
      return false;
    }
    if (prev == 151 && last == 131) { // (151,131)
      // ReplaceEdge(151,123,Some(151))
      replace(123);
      pendingFinish = 151;
      return false;
    }
    if (prev == 151 && last == 132) { // (151,132)
      // ReplaceEdge(151,148,None)
      replace(148);
      pendingFinish = -1;
      return false;
    }
    if (prev == 151 && last == 133) { // (151,133)
      // ReplaceEdge(151,147,Some(151))
      replace(147);
      pendingFinish = 151;
      return false;
    }
    if (prev == 151 && last == 135) { // (151,135)
      // ReplaceEdge(151,136,Some(151))
      replace(136);
      pendingFinish = 151;
      return false;
    }
    if (prev == 151 && last == 136) { // (151,136)
      // ReplaceEdge(151,147,Some(151))
      replace(147);
      pendingFinish = 151;
      return false;
    }
    if (prev == 151 && last == 138) { // (151,138)
      // ReplaceEdge(151,151,Some(151))
      replace(151);
      pendingFinish = 151;
      return false;
    }
    if (prev == 151 && last == 139) { // (151,139)
      // ReplaceEdge(151,128,None)
      replace(128);
      pendingFinish = -1;
      return false;
    }
    if (prev == 151 && last == 140) { // (151,140)
      // ReplaceEdge(151,156,Some(151))
      replace(156);
      pendingFinish = 151;
      return false;
    }
    if (prev == 151 && last == 141) { // (151,141)
      // ReplaceEdge(151,156,None)
      replace(156);
      pendingFinish = -1;
      return false;
    }
    if (prev == 151 && last == 142) { // (151,142)
      // ReplaceEdge(151,132,Some(151))
      replace(132);
      pendingFinish = 151;
      return false;
    }
    if (prev == 151 && last == 144) { // (151,144)
      // ReplaceEdge(151,157,Some(151))
      replace(157);
      pendingFinish = 151;
      return false;
    }
    if (prev == 151 && last == 145) { // (151,145)
      // ReplaceEdge(151,157,None)
      replace(157);
      pendingFinish = -1;
      return false;
    }
    if (prev == 151 && last == 147) { // (151,147)
      // ReplaceEdge(151,148,Some(151))
      replace(148);
      pendingFinish = 151;
      return false;
    }
    if (prev == 151 && last == 148) { // (151,148)
      // ReplaceEdge(151,138,Some(151))
      replace(138);
      pendingFinish = 151;
      return false;
    }
    if (prev == 151 && last == 150) { // (151,150)
      // ReplaceEdge(151,147,Some(151))
      replace(147);
      pendingFinish = 151;
      return false;
    }
    if (prev == 151 && last == 151) { // (151,151)
      // DropLast(151)
      dropLast();
      return true;
    }
    if (prev == 151 && last == 154) { // (151,154)
      // ReplaceEdge(151,151,None)
      replace(151);
      pendingFinish = -1;
      return false;
    }
    if (prev == 151 && last == 156) { // (151,156)
      // ReplaceEdge(151,138,Some(151))
      replace(138);
      pendingFinish = 151;
      return false;
    }
    if (prev == 151 && last == 157) { // (151,157)
      // ReplaceEdge(151,147,Some(151))
      replace(147);
      pendingFinish = 151;
      return false;
    }
    if (prev == 154 && last == 11) { // (154,11)
      // ReplaceEdge(154,59,Some(154))
      replace(59);
      pendingFinish = 154;
      return false;
    }
    if (prev == 154 && last == 34) { // (154,34)
      // ReplaceEdge(154,70,Some(154))
      replace(70);
      pendingFinish = 154;
      return false;
    }
    if (prev == 154 && last == 47) { // (154,47)
      // DropLast(47)
      dropLast();
      replace(47);
      return true;
    }
    if (prev == 154 && last == 59) { // (154,59)
      // ReplaceEdge(47,72,None)
      dropLast();
      replace(47);
      append(72);
      pendingFinish = -1;
      return false;
    }
    if (prev == 154 && last == 70) { // (154,70)
      // ReplaceEdge(47,102,None)
      dropLast();
      replace(47);
      append(102);
      pendingFinish = -1;
      return false;
    }
    if (prev == 154 && last == 90) { // (154,90)
      // ReplaceEdge(47,91,None)
      dropLast();
      replace(47);
      append(91);
      pendingFinish = -1;
      return false;
    }
    if (prev == 154 && last == 91) { // (154,91)
      // ReplaceEdge(47,117,None)
      dropLast();
      replace(47);
      append(117);
      pendingFinish = -1;
      return false;
    }
    throw new AssertionError("Unknown edge to finish: " + stackIds());
  }

  private boolean finish() {
    if (stack.prev == null) {
      return false;
    }
    while (finishStep()) {
      if (verbose) {
        printStack();
      }
      if (stack.prev == null) {
        stack = null;
        return false;
      }
    }
    return true;
  }

  private void dropLast() {
    stack = stack.prev;
  }

  public String stackIds() {
    if (stack == null) {
      return ".";
    }
    return stackIds(stack);
  }

  private String stackIds(Stack stack) {
    if (stack.prev == null) return "" + stack.nodeId;
    else return stackIds(stack.prev) + " " + stack.nodeId;
  }

  public String stackDescription() {
    if (stack == null) {
      return ".";
    }
    return stackDescription(stack);
  }

  private String stackDescription(Stack stack) {
    if (stack.prev == null) return nodeDescriptionOf(stack.nodeId);
    else return stackDescription(stack.prev) + " " + nodeDescriptionOf(stack.nodeId);
  }

  private static void log(String s) {
    System.out.println(s);
  }

  private void printStack() {
    if (stack == null) {
      log("  .");
    } else {
      log("  " + stackIds() + " " + stackDescription());
    }
  }

  public boolean proceed(char c) {
    if (stack == null) {
      if (verbose) {
        log("  - already finished");
      }
      return false;
    }
    if (!canAccept(c)) {
      if (verbose) {
        log("  - cannot accept " + c + ", try pendingFinish");
      }
      if (pendingFinish == -1) {
        if (verbose) {
          log("  - pendingFinish unavailable, proceed failed");
        }
        return false;
      }
      dropLast();
      if (stack.nodeId != pendingFinish) {
        replace(pendingFinish);
      }
      if (verbose) {
        printStack();
      }
      if (!finish()) {
        return false;
      }
      return proceed(c);
    }
    switch (stack.nodeId) {
      case 0:
        if (('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ')) {
          // Append(0,1,None)
          append(1);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == '"')) {
          // Append(0,2,None)
          append(2);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == '-')) {
          // Append(0,6,None)
          append(6);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == '0')) {
          // Append(0,8,Some(0))
          append(8);
          pendingFinish = 0;
          if (verbose) printStack();
          return true;
        }
        if (('1' <= c && c <= '9')) {
          // Append(0,4,Some(0))
          append(4);
          pendingFinish = 0;
          if (verbose) printStack();
          return true;
        }
        if ((c == '[')) {
          // Append(0,3,None)
          append(3);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == 'f')) {
          // Append(0,10,None)
          append(10);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == 'n')) {
          // Append(0,7,None)
          append(7);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == 't')) {
          // Append(0,5,None)
          append(5);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == '{')) {
          // Append(0,9,None)
          append(9);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        return false;
      case 1:
        if (('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ')) {
          // Append(11,11,Some(11))
          replace(11);
          append(11);
          pendingFinish = 11;
          if (verbose) printStack();
          return true;
        }
        if ((c == '"')) {
          // Append(12,2,None)
          replace(12);
          append(2);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == '-')) {
          // Append(12,6,None)
          replace(12);
          append(6);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == '0')) {
          // Append(12,14,Some(12))
          replace(12);
          append(14);
          pendingFinish = 12;
          if (verbose) printStack();
          return true;
        }
        if (('1' <= c && c <= '9')) {
          // Append(12,13,Some(12))
          replace(12);
          append(13);
          pendingFinish = 12;
          if (verbose) printStack();
          return true;
        }
        if ((c == '[')) {
          // Append(12,3,None)
          replace(12);
          append(3);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == 'f')) {
          // Append(12,10,None)
          replace(12);
          append(10);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == 'n')) {
          // Append(12,7,None)
          replace(12);
          append(7);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == 't')) {
          // Append(12,5,None)
          replace(12);
          append(5);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == '{')) {
          // Append(12,9,None)
          replace(12);
          append(9);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        return false;
      case 2:
        if ((c == ' ')
            || ('0' <= c && c <= '9')
            || ('A' <= c && c <= 'Z')
            || ('a' <= c && c <= 'z')) {
          // Append(21,24,Some(21))
          replace(21);
          append(24);
          pendingFinish = 21;
          if (verbose) printStack();
          return true;
        }
        if ((c == '"')) {
          // Finish(23)
          replace(23);
          if (verbose) printStack();
          finish();
          if (verbose) printStack();
          return true;
        }
        if ((c == '\\')) {
          // Append(21,22,None)
          replace(21);
          append(22);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        return false;
      case 3:
        if (('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ')) {
          // Append(27,11,Some(27))
          replace(27);
          append(11);
          pendingFinish = 27;
          if (verbose) printStack();
          return true;
        }
        if ((c == '"')) {
          // Append(28,2,None)
          replace(28);
          append(2);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == '-')) {
          // Append(28,6,None)
          replace(28);
          append(6);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == '0')) {
          // Append(28,31,Some(28))
          replace(28);
          append(31);
          pendingFinish = 28;
          if (verbose) printStack();
          return true;
        }
        if (('1' <= c && c <= '9')) {
          // Append(28,29,Some(28))
          replace(28);
          append(29);
          pendingFinish = 28;
          if (verbose) printStack();
          return true;
        }
        if ((c == '[')) {
          // Append(28,3,None)
          replace(28);
          append(3);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == ']')) {
          // Finish(30)
          replace(30);
          if (verbose) printStack();
          finish();
          if (verbose) printStack();
          return true;
        }
        if ((c == 'f')) {
          // Append(28,10,None)
          replace(28);
          append(10);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == 'n')) {
          // Append(28,7,None)
          replace(28);
          append(7);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == 't')) {
          // Append(28,5,None)
          replace(28);
          append(5);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == '{')) {
          // Append(28,9,None)
          replace(28);
          append(9);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        return false;
      case 4:
        if (('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ')) {
          // Append(15,11,Some(15))
          replace(15);
          append(11);
          pendingFinish = 15;
          if (verbose) printStack();
          return true;
        }
        if ((c == '.')) {
          // Append(16,17,None)
          replace(16);
          append(17);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == '0')) {
          // Append(25,26,Some(25))
          replace(25);
          append(26);
          pendingFinish = 25;
          if (verbose) printStack();
          return true;
        }
        if (('1' <= c && c <= '9')) {
          // Append(25,26,Some(25))
          replace(25);
          append(26);
          pendingFinish = 25;
          if (verbose) printStack();
          return true;
        }
        if ((c == 'E')) {
          // Append(18,20,None)
          replace(18);
          append(20);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == 'e')) {
          // Append(18,19,None)
          replace(18);
          append(19);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        return false;
      case 5:
        if ((c == 'r')) {
          // Finish(5)
          finish();
          if (verbose) printStack();
          return true;
        }
        return false;
      case 6:
        if ((c == '0')) {
          // Finish(32)
          replace(32);
          if (verbose) printStack();
          finish();
          if (verbose) printStack();
          return true;
        }
        if (('1' <= c && c <= '9')) {
          // Finish(6)
          finish();
          if (verbose) printStack();
          return true;
        }
        return false;
      case 7:
        if ((c == 'u')) {
          // Finish(7)
          finish();
          if (verbose) printStack();
          return true;
        }
        return false;
      case 8:
        if (('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ')) {
          // Append(15,11,Some(15))
          replace(15);
          append(11);
          pendingFinish = 15;
          if (verbose) printStack();
          return true;
        }
        if ((c == '.')) {
          // Append(16,17,None)
          replace(16);
          append(17);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == 'E')) {
          // Append(18,20,None)
          replace(18);
          append(20);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == 'e')) {
          // Append(18,19,None)
          replace(18);
          append(19);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        return false;
      case 9:
        if (('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ')) {
          // Append(33,34,Some(35))
          replace(33);
          append(34);
          pendingFinish = 35;
          if (verbose) printStack();
          return true;
        }
        if ((c == '"')) {
          // Append(36,2,None)
          replace(36);
          append(2);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == '}')) {
          // Finish(37)
          replace(37);
          if (verbose) printStack();
          finish();
          if (verbose) printStack();
          return true;
        }
        return false;
      case 10:
        if ((c == 'a')) {
          // Finish(10)
          finish();
          if (verbose) printStack();
          return true;
        }
        return false;
      case 11:
        if (('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ')) {
          // Append(11,11,Some(11))
          append(11);
          pendingFinish = 11;
          if (verbose) printStack();
          return true;
        }
        return false;
      case 12:
        if ((c == '"')) {
          // Append(12,2,None)
          append(2);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == '-')) {
          // Append(12,6,None)
          append(6);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == '0')) {
          // Append(12,14,Some(12))
          append(14);
          pendingFinish = 12;
          if (verbose) printStack();
          return true;
        }
        if (('1' <= c && c <= '9')) {
          // Append(12,13,Some(12))
          append(13);
          pendingFinish = 12;
          if (verbose) printStack();
          return true;
        }
        if ((c == '[')) {
          // Append(12,3,None)
          append(3);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == 'f')) {
          // Append(12,10,None)
          append(10);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == 'n')) {
          // Append(12,7,None)
          append(7);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == 't')) {
          // Append(12,5,None)
          append(5);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == '{')) {
          // Append(12,9,None)
          append(9);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        return false;
      case 13:
        if ((c == '.')) {
          // Append(16,17,None)
          replace(16);
          append(17);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == '0')) {
          // Append(25,26,Some(25))
          replace(25);
          append(26);
          pendingFinish = 25;
          if (verbose) printStack();
          return true;
        }
        if (('1' <= c && c <= '9')) {
          // Append(25,26,Some(25))
          replace(25);
          append(26);
          pendingFinish = 25;
          if (verbose) printStack();
          return true;
        }
        if ((c == 'E')) {
          // Append(18,20,None)
          replace(18);
          append(20);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == 'e')) {
          // Append(18,19,None)
          replace(18);
          append(19);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        return false;
      case 14:
        if ((c == '.')) {
          // Append(16,17,None)
          replace(16);
          append(17);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == 'E')) {
          // Append(18,20,None)
          replace(18);
          append(20);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == 'e')) {
          // Append(18,19,None)
          replace(18);
          append(19);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        return false;
      case 15:
        if (('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ')) {
          // Append(15,11,Some(15))
          append(11);
          pendingFinish = 15;
          if (verbose) printStack();
          return true;
        }
        return false;
      case 16:
        if ((c == '.')) {
          // Append(16,17,None)
          append(17);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == 'E')) {
          // Append(47,20,None)
          replace(47);
          append(20);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == 'e')) {
          // Append(47,19,None)
          replace(47);
          append(19);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        return false;
      case 17:
        if (('0' <= c && c <= '9')) {
          // Append(17,50,Some(17))
          append(50);
          pendingFinish = 17;
          if (verbose) printStack();
          return true;
        }
        return false;
      case 18:
        if ((c == 'E')) {
          // Append(18,20,None)
          append(20);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == 'e')) {
          // Append(18,19,None)
          append(19);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        return false;
      case 19:
        if ((c == '+')) {
          // Finish(48)
          replace(48);
          if (verbose) printStack();
          finish();
          if (verbose) printStack();
          return true;
        }
        if ((c == '-')) {
          // Finish(48)
          replace(48);
          if (verbose) printStack();
          finish();
          if (verbose) printStack();
          return true;
        }
        if (('0' <= c && c <= '9')) {
          // Append(49,50,Some(49))
          replace(49);
          append(50);
          pendingFinish = 49;
          if (verbose) printStack();
          return true;
        }
        return false;
      case 20:
        if ((c == '+')) {
          // Finish(51)
          replace(51);
          if (verbose) printStack();
          finish();
          if (verbose) printStack();
          return true;
        }
        if ((c == '-')) {
          // Finish(51)
          replace(51);
          if (verbose) printStack();
          finish();
          if (verbose) printStack();
          return true;
        }
        if (('0' <= c && c <= '9')) {
          // Append(52,50,Some(52))
          replace(52);
          append(50);
          pendingFinish = 52;
          if (verbose) printStack();
          return true;
        }
        return false;
      case 21:
        if ((c == ' ')
            || ('0' <= c && c <= '9')
            || ('A' <= c && c <= 'Z')
            || ('a' <= c && c <= 'z')) {
          // Append(21,24,Some(21))
          append(24);
          pendingFinish = 21;
          if (verbose) printStack();
          return true;
        }
        if ((c == '"')) {
          // Finish(47)
          replace(47);
          if (verbose) printStack();
          finish();
          if (verbose) printStack();
          return true;
        }
        if ((c == '\\')) {
          // Append(21,22,None)
          append(22);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        return false;
      case 22:
        if ((c == '"')
            || (c == '/')
            || (c == '\\')
            || (c == 'b')
            || (c == 'n')
            || (c == 'r')
            || (c == 't')) {
          // Finish(22)
          finish();
          if (verbose) printStack();
          return true;
        }
        if ((c == 'u')) {
          // Append(22,65,None)
          append(65);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        return false;
      case 23:
        if ((c == '"')) {
          // Finish(23)
          finish();
          if (verbose) printStack();
          return true;
        }
        return false;
      case 24:
        if ((c == ' ')
            || ('0' <= c && c <= '9')
            || ('A' <= c && c <= 'Z')
            || ('a' <= c && c <= 'z')) {
          // Append(24,24,Some(24))
          append(24);
          pendingFinish = 24;
          if (verbose) printStack();
          return true;
        }
        if ((c == '\\')) {
          // Append(24,22,None)
          append(22);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        return false;
      case 25:
        if ((c == '0')) {
          // Append(25,26,Some(25))
          append(26);
          pendingFinish = 25;
          if (verbose) printStack();
          return true;
        }
        if (('1' <= c && c <= '9')) {
          // Append(25,26,Some(25))
          append(26);
          pendingFinish = 25;
          if (verbose) printStack();
          return true;
        }
        return false;
      case 26:
        if ((c == '0')) {
          // Append(26,26,Some(26))
          append(26);
          pendingFinish = 26;
          if (verbose) printStack();
          return true;
        }
        if (('1' <= c && c <= '9')) {
          // Append(26,26,Some(26))
          append(26);
          pendingFinish = 26;
          if (verbose) printStack();
          return true;
        }
        return false;
      case 27:
        if (('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ')) {
          // Append(27,11,Some(27))
          append(11);
          pendingFinish = 27;
          if (verbose) printStack();
          return true;
        }
        if ((c == '"')) {
          // Append(47,2,None)
          replace(47);
          append(2);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == '-')) {
          // Append(47,6,None)
          replace(47);
          append(6);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == '0')) {
          // Append(47,57,None)
          replace(47);
          append(57);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if (('1' <= c && c <= '9')) {
          // Append(47,55,None)
          replace(47);
          append(55);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == '[')) {
          // Append(47,3,None)
          replace(47);
          append(3);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == ']')) {
          // Append(47,56,None)
          replace(47);
          append(56);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == 'f')) {
          // Append(47,10,None)
          replace(47);
          append(10);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == 'n')) {
          // Append(47,7,None)
          replace(47);
          append(7);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == 't')) {
          // Append(47,5,None)
          replace(47);
          append(5);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == '{')) {
          // Append(47,9,None)
          replace(47);
          append(9);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        return false;
      case 28:
        if ((c == '"')) {
          // Append(28,2,None)
          append(2);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == '-')) {
          // Append(28,6,None)
          append(6);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == '0')) {
          // Append(28,31,Some(28))
          append(31);
          pendingFinish = 28;
          if (verbose) printStack();
          return true;
        }
        if (('1' <= c && c <= '9')) {
          // Append(28,29,Some(28))
          append(29);
          pendingFinish = 28;
          if (verbose) printStack();
          return true;
        }
        if ((c == '[')) {
          // Append(28,3,None)
          append(3);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == 'f')) {
          // Append(28,10,None)
          append(10);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == 'n')) {
          // Append(28,7,None)
          append(7);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == 't')) {
          // Append(28,5,None)
          append(5);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == '{')) {
          // Append(28,9,None)
          append(9);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        return false;
      case 29:
        if (('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ')) {
          // Append(60,11,Some(60))
          replace(60);
          append(11);
          pendingFinish = 60;
          if (verbose) printStack();
          return true;
        }
        if ((c == ',')) {
          // Finish(61)
          replace(61);
          if (verbose) printStack();
          finish();
          if (verbose) printStack();
          return true;
        }
        if ((c == '.')) {
          // Append(16,17,None)
          replace(16);
          append(17);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == '0')) {
          // Append(25,26,Some(25))
          replace(25);
          append(26);
          pendingFinish = 25;
          if (verbose) printStack();
          return true;
        }
        if (('1' <= c && c <= '9')) {
          // Append(25,26,Some(25))
          replace(25);
          append(26);
          pendingFinish = 25;
          if (verbose) printStack();
          return true;
        }
        if ((c == 'E')) {
          // Append(18,20,None)
          replace(18);
          append(20);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == 'e')) {
          // Append(18,19,None)
          replace(18);
          append(19);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        return false;
      case 30:
        if ((c == ']')) {
          // Finish(30)
          finish();
          if (verbose) printStack();
          return true;
        }
        return false;
      case 31:
        if (('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ')) {
          // Append(60,11,Some(60))
          replace(60);
          append(11);
          pendingFinish = 60;
          if (verbose) printStack();
          return true;
        }
        if ((c == ',')) {
          // Finish(61)
          replace(61);
          if (verbose) printStack();
          finish();
          if (verbose) printStack();
          return true;
        }
        if ((c == '.')) {
          // Append(16,17,None)
          replace(16);
          append(17);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == 'E')) {
          // Append(18,20,None)
          replace(18);
          append(20);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == 'e')) {
          // Append(18,19,None)
          replace(18);
          append(19);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        return false;
      case 32:
        if ((c == '0')) {
          // Finish(32)
          finish();
          if (verbose) printStack();
          return true;
        }
        if (('1' <= c && c <= '9')) {
          // Finish(32)
          finish();
          if (verbose) printStack();
          return true;
        }
        return false;
      case 33:
        if (('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ')) {
          // Append(33,34,Some(35))
          append(34);
          pendingFinish = 35;
          if (verbose) printStack();
          return true;
        }
        if ((c == '"')) {
          // Append(36,2,None)
          replace(36);
          append(2);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == '}')) {
          // Finish(47)
          replace(47);
          if (verbose) printStack();
          finish();
          if (verbose) printStack();
          return true;
        }
        return false;
      case 34:
        if (('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ')) {
          // Append(11,11,Some(11))
          replace(11);
          append(11);
          pendingFinish = 11;
          if (verbose) printStack();
          return true;
        }
        if ((c == '"')) {
          // Append(59,2,None)
          replace(59);
          append(2);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        return false;
      case 35:
        if (('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ')) {
          // Append(35,34,Some(35))
          append(34);
          pendingFinish = 35;
          if (verbose) printStack();
          return true;
        }
        if ((c == '"')) {
          // Append(47,2,None)
          replace(47);
          append(2);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == '}')) {
          // Finish(47)
          replace(47);
          if (verbose) printStack();
          finish();
          if (verbose) printStack();
          return true;
        }
        return false;
      case 36:
        if (('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ')) {
          // Append(36,34,None)
          append(34);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == '"')) {
          // Append(36,2,None)
          append(2);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        return false;
      case 37:
        if ((c == '}')) {
          // Finish(37)
          finish();
          if (verbose) printStack();
          return true;
        }
        return false;
      case 38:
        if (('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ')) {
          // Append(62,34,Some(63))
          replace(62);
          append(34);
          pendingFinish = 63;
          if (verbose) printStack();
          return true;
        }
        if ((c == '"')) {
          // Append(36,2,None)
          replace(36);
          append(2);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == '}')) {
          // Finish(64)
          replace(64);
          if (verbose) printStack();
          finish();
          if (verbose) printStack();
          return true;
        }
        return false;
      case 39:
        if (('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ')) {
          // Append(15,11,Some(15))
          replace(15);
          append(11);
          pendingFinish = 15;
          if (verbose) printStack();
          return true;
        }
        if ((c == 'E')) {
          // Append(18,20,None)
          replace(18);
          append(20);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == 'e')) {
          // Append(18,19,None)
          replace(18);
          append(19);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        return false;
      case 40:
        if ((c == 'l')) {
          // Finish(40)
          finish();
          if (verbose) printStack();
          return true;
        }
        return false;
      case 41:
        if (('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ')) {
          // Append(15,11,Some(15))
          replace(15);
          append(11);
          pendingFinish = 15;
          if (verbose) printStack();
          return true;
        }
        if ((c == '.')) {
          // Append(16,17,None)
          replace(16);
          append(17);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == '0')) {
          // Append(58,26,Some(58))
          replace(58);
          append(26);
          pendingFinish = 58;
          if (verbose) printStack();
          return true;
        }
        if (('1' <= c && c <= '9')) {
          // Append(58,26,Some(58))
          replace(58);
          append(26);
          pendingFinish = 58;
          if (verbose) printStack();
          return true;
        }
        if ((c == 'E')) {
          // Append(18,20,None)
          replace(18);
          append(20);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == 'e')) {
          // Append(18,19,None)
          replace(18);
          append(19);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        return false;
      case 42:
        if ((c == 'u')) {
          // Finish(42)
          finish();
          if (verbose) printStack();
          return true;
        }
        return false;
      case 43:
        if ((c == 'l')) {
          // Finish(43)
          finish();
          if (verbose) printStack();
          return true;
        }
        return false;
      case 44:
        if (('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ')) {
          // Append(53,11,Some(53))
          replace(53);
          append(11);
          pendingFinish = 53;
          if (verbose) printStack();
          return true;
        }
        if ((c == '"')) {
          // Append(28,2,None)
          replace(28);
          append(2);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == '-')) {
          // Append(28,6,None)
          replace(28);
          append(6);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == '0')) {
          // Append(28,31,Some(28))
          replace(28);
          append(31);
          pendingFinish = 28;
          if (verbose) printStack();
          return true;
        }
        if (('1' <= c && c <= '9')) {
          // Append(28,29,Some(28))
          replace(28);
          append(29);
          pendingFinish = 28;
          if (verbose) printStack();
          return true;
        }
        if ((c == '[')) {
          // Append(28,3,None)
          replace(28);
          append(3);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == ']')) {
          // Finish(54)
          replace(54);
          if (verbose) printStack();
          finish();
          if (verbose) printStack();
          return true;
        }
        if ((c == 'f')) {
          // Append(28,10,None)
          replace(28);
          append(10);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == 'n')) {
          // Append(28,7,None)
          replace(28);
          append(7);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == 't')) {
          // Append(28,5,None)
          replace(28);
          append(5);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == '{')) {
          // Append(28,9,None)
          replace(28);
          append(9);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        return false;
      case 45:
        if (('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ')) {
          // Append(15,11,Some(15))
          replace(15);
          append(11);
          pendingFinish = 15;
          if (verbose) printStack();
          return true;
        }
        if ((c == '"')) {
          // Append(12,2,None)
          replace(12);
          append(2);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == '-')) {
          // Append(12,6,None)
          replace(12);
          append(6);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == '0')) {
          // Append(12,14,Some(12))
          replace(12);
          append(14);
          pendingFinish = 12;
          if (verbose) printStack();
          return true;
        }
        if (('1' <= c && c <= '9')) {
          // Append(12,13,Some(12))
          replace(12);
          append(13);
          pendingFinish = 12;
          if (verbose) printStack();
          return true;
        }
        if ((c == '[')) {
          // Append(12,3,None)
          replace(12);
          append(3);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == 'f')) {
          // Append(12,10,None)
          replace(12);
          append(10);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == 'n')) {
          // Append(12,7,None)
          replace(12);
          append(7);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == 't')) {
          // Append(12,5,None)
          replace(12);
          append(5);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == '{')) {
          // Append(12,9,None)
          replace(12);
          append(9);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        return false;
      case 46:
        if (('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ')) {
          // Append(15,11,Some(15))
          replace(15);
          append(11);
          pendingFinish = 15;
          if (verbose) printStack();
          return true;
        }
        if ((c == '"')) {
          // Finish(23)
          replace(23);
          if (verbose) printStack();
          finish();
          if (verbose) printStack();
          return true;
        }
        return false;
      case 48:
        if ((c == '+')) {
          // Finish(48)
          finish();
          if (verbose) printStack();
          return true;
        }
        if ((c == '-')) {
          // Finish(48)
          finish();
          if (verbose) printStack();
          return true;
        }
        if (('0' <= c && c <= '9')) {
          // Append(47,50,None)
          replace(47);
          append(50);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        return false;
      case 49:
        if (('0' <= c && c <= '9')) {
          // Append(49,50,Some(49))
          append(50);
          pendingFinish = 49;
          if (verbose) printStack();
          return true;
        }
        return false;
      case 50:
        if (('0' <= c && c <= '9')) {
          // Finish(50)
          finish();
          if (verbose) printStack();
          return true;
        }
        return false;
      case 51:
        if ((c == '+')) {
          // Finish(51)
          finish();
          if (verbose) printStack();
          return true;
        }
        if ((c == '-')) {
          // Finish(51)
          finish();
          if (verbose) printStack();
          return true;
        }
        if (('0' <= c && c <= '9')) {
          // Append(47,50,None)
          replace(47);
          append(50);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        return false;
      case 52:
        if (('0' <= c && c <= '9')) {
          // Append(52,50,Some(52))
          append(50);
          pendingFinish = 52;
          if (verbose) printStack();
          return true;
        }
        return false;
      case 53:
        if (('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ')) {
          // Append(53,11,Some(53))
          append(11);
          pendingFinish = 53;
          if (verbose) printStack();
          return true;
        }
        if ((c == ']')) {
          // Finish(47)
          replace(47);
          if (verbose) printStack();
          finish();
          if (verbose) printStack();
          return true;
        }
        return false;
      case 54:
        if ((c == ']')) {
          // Finish(54)
          finish();
          if (verbose) printStack();
          return true;
        }
        return false;
      case 55:
        if (('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ')) {
          // Append(97,11,Some(97))
          replace(97);
          append(11);
          pendingFinish = 97;
          if (verbose) printStack();
          return true;
        }
        if ((c == ',')) {
          // Finish(61)
          replace(61);
          if (verbose) printStack();
          finish();
          if (verbose) printStack();
          return true;
        }
        if ((c == '.')) {
          // Append(16,17,None)
          replace(16);
          append(17);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == '0')) {
          // Append(25,26,Some(25))
          replace(25);
          append(26);
          pendingFinish = 25;
          if (verbose) printStack();
          return true;
        }
        if (('1' <= c && c <= '9')) {
          // Append(25,26,Some(25))
          replace(25);
          append(26);
          pendingFinish = 25;
          if (verbose) printStack();
          return true;
        }
        if ((c == 'E')) {
          // Append(18,20,None)
          replace(18);
          append(20);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == ']')) {
          // Finish(94)
          replace(94);
          if (verbose) printStack();
          finish();
          if (verbose) printStack();
          return true;
        }
        if ((c == 'e')) {
          // Append(18,19,None)
          replace(18);
          append(19);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        return false;
      case 56:
        if (('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ')) {
          // Append(97,11,Some(97))
          replace(97);
          append(11);
          pendingFinish = 97;
          if (verbose) printStack();
          return true;
        }
        if ((c == ',')) {
          // Finish(61)
          replace(61);
          if (verbose) printStack();
          finish();
          if (verbose) printStack();
          return true;
        }
        if ((c == ']')) {
          // Finish(94)
          replace(94);
          if (verbose) printStack();
          finish();
          if (verbose) printStack();
          return true;
        }
        return false;
      case 57:
        if (('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ')) {
          // Append(97,11,Some(97))
          replace(97);
          append(11);
          pendingFinish = 97;
          if (verbose) printStack();
          return true;
        }
        if ((c == ',')) {
          // Finish(61)
          replace(61);
          if (verbose) printStack();
          finish();
          if (verbose) printStack();
          return true;
        }
        if ((c == '.')) {
          // Append(16,17,None)
          replace(16);
          append(17);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == 'E')) {
          // Append(18,20,None)
          replace(18);
          append(20);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == ']')) {
          // Finish(94)
          replace(94);
          if (verbose) printStack();
          finish();
          if (verbose) printStack();
          return true;
        }
        if ((c == 'e')) {
          // Append(18,19,None)
          replace(18);
          append(19);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        return false;
      case 58:
        if ((c == '0')) {
          // Append(58,26,Some(58))
          append(26);
          pendingFinish = 58;
          if (verbose) printStack();
          return true;
        }
        if (('1' <= c && c <= '9')) {
          // Append(58,26,Some(58))
          append(26);
          pendingFinish = 58;
          if (verbose) printStack();
          return true;
        }
        return false;
      case 59:
        if ((c == '"')) {
          // Append(59,2,None)
          append(2);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        return false;
      case 60:
        if (('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ')) {
          // Append(60,11,Some(60))
          append(11);
          pendingFinish = 60;
          if (verbose) printStack();
          return true;
        }
        if ((c == ',')) {
          // Append(47,100,None)
          replace(47);
          append(100);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        return false;
      case 61:
        if ((c == ',')) {
          // Finish(61)
          finish();
          if (verbose) printStack();
          return true;
        }
        return false;
      case 62:
        if (('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ')) {
          // Append(62,34,Some(63))
          append(34);
          pendingFinish = 63;
          if (verbose) printStack();
          return true;
        }
        if ((c == '"')) {
          // Append(36,2,None)
          replace(36);
          append(2);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == '}')) {
          // Finish(47)
          replace(47);
          if (verbose) printStack();
          finish();
          if (verbose) printStack();
          return true;
        }
        return false;
      case 63:
        if (('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ')) {
          // Append(63,11,Some(63))
          append(11);
          pendingFinish = 63;
          if (verbose) printStack();
          return true;
        }
        if ((c == '}')) {
          // Finish(47)
          replace(47);
          if (verbose) printStack();
          finish();
          if (verbose) printStack();
          return true;
        }
        return false;
      case 64:
        if ((c == '}')) {
          // Finish(64)
          finish();
          if (verbose) printStack();
          return true;
        }
        return false;
      case 65:
        if ((c == '0')) {
          // Finish(65)
          finish();
          if (verbose) printStack();
          return true;
        }
        if (('1' <= c && c <= '9')) {
          // Finish(65)
          finish();
          if (verbose) printStack();
          return true;
        }
        if (('A' <= c && c <= 'F') || ('a' <= c && c <= 'f')) {
          // Finish(65)
          finish();
          if (verbose) printStack();
          return true;
        }
        return false;
      case 66:
        if ((c == 's')) {
          // Finish(66)
          finish();
          if (verbose) printStack();
          return true;
        }
        return false;
      case 67:
        if (('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ')) {
          // Append(60,11,Some(60))
          replace(60);
          append(11);
          pendingFinish = 60;
          if (verbose) printStack();
          return true;
        }
        if ((c == ',')) {
          // Finish(61)
          replace(61);
          if (verbose) printStack();
          finish();
          if (verbose) printStack();
          return true;
        }
        if ((c == '.')) {
          // Append(16,17,None)
          replace(16);
          append(17);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == '0')) {
          // Append(58,26,Some(58))
          replace(58);
          append(26);
          pendingFinish = 58;
          if (verbose) printStack();
          return true;
        }
        if (('1' <= c && c <= '9')) {
          // Append(58,26,Some(58))
          replace(58);
          append(26);
          pendingFinish = 58;
          if (verbose) printStack();
          return true;
        }
        if ((c == 'E')) {
          // Append(18,20,None)
          replace(18);
          append(20);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == 'e')) {
          // Append(18,19,None)
          replace(18);
          append(19);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        return false;
      case 68:
        if ((c == '"')) {
          // Append(28,2,None)
          replace(28);
          append(2);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == '-')) {
          // Append(28,6,None)
          replace(28);
          append(6);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == '0')) {
          // Append(28,31,Some(28))
          replace(28);
          append(31);
          pendingFinish = 28;
          if (verbose) printStack();
          return true;
        }
        if (('1' <= c && c <= '9')) {
          // Append(28,29,Some(28))
          replace(28);
          append(29);
          pendingFinish = 28;
          if (verbose) printStack();
          return true;
        }
        if ((c == '[')) {
          // Append(28,3,None)
          replace(28);
          append(3);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == ']')) {
          // Finish(30)
          replace(30);
          if (verbose) printStack();
          finish();
          if (verbose) printStack();
          return true;
        }
        if ((c == 'f')) {
          // Append(28,10,None)
          replace(28);
          append(10);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == 'n')) {
          // Append(28,7,None)
          replace(28);
          append(7);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == 't')) {
          // Append(28,5,None)
          replace(28);
          append(5);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == '{')) {
          // Append(28,9,None)
          replace(28);
          append(9);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        return false;
      case 69:
        if (('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ')) {
          // Append(89,11,Some(89))
          replace(89);
          append(11);
          pendingFinish = 89;
          if (verbose) printStack();
          return true;
        }
        if ((c == '}')) {
          // Finish(92)
          replace(92);
          if (verbose) printStack();
          finish();
          if (verbose) printStack();
          return true;
        }
        return false;
      case 70:
        if (('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ')) {
          // Append(90,11,Some(90))
          replace(90);
          append(11);
          pendingFinish = 90;
          if (verbose) printStack();
          return true;
        }
        if ((c == '"')) {
          // Append(59,2,None)
          replace(59);
          append(2);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == ':')) {
          // Finish(91)
          replace(91);
          if (verbose) printStack();
          finish();
          if (verbose) printStack();
          return true;
        }
        return false;
      case 71:
        if (('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ')) {
          // Append(93,11,Some(93))
          replace(93);
          append(11);
          pendingFinish = 93;
          if (verbose) printStack();
          return true;
        }
        if ((c == ']')) {
          // Finish(94)
          replace(94);
          if (verbose) printStack();
          finish();
          if (verbose) printStack();
          return true;
        }
        return false;
      case 72:
        if (('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ')) {
          // Append(90,11,Some(90))
          replace(90);
          append(11);
          pendingFinish = 90;
          if (verbose) printStack();
          return true;
        }
        if ((c == ':')) {
          // Finish(91)
          replace(91);
          if (verbose) printStack();
          finish();
          if (verbose) printStack();
          return true;
        }
        return false;
      case 73:
        if ((c == '.')) {
          // Append(16,17,None)
          replace(16);
          append(17);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == '0')) {
          // Append(58,26,Some(58))
          replace(58);
          append(26);
          pendingFinish = 58;
          if (verbose) printStack();
          return true;
        }
        if (('1' <= c && c <= '9')) {
          // Append(58,26,Some(58))
          replace(58);
          append(26);
          pendingFinish = 58;
          if (verbose) printStack();
          return true;
        }
        if ((c == 'E')) {
          // Append(18,20,None)
          replace(18);
          append(20);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == 'e')) {
          // Append(18,19,None)
          replace(18);
          append(19);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        return false;
      case 74:
        if ((c == 'l')) {
          // Finish(74)
          finish();
          if (verbose) printStack();
          return true;
        }
        return false;
      case 75:
        if (('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ')) {
          // Append(60,11,Some(60))
          replace(60);
          append(11);
          pendingFinish = 60;
          if (verbose) printStack();
          return true;
        }
        if ((c == ',')) {
          // Finish(61)
          replace(61);
          if (verbose) printStack();
          finish();
          if (verbose) printStack();
          return true;
        }
        return false;
      case 76:
        if (('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ')) {
          // Append(53,11,Some(53))
          replace(53);
          append(11);
          pendingFinish = 53;
          if (verbose) printStack();
          return true;
        }
        if ((c == ']')) {
          // Finish(94)
          replace(94);
          if (verbose) printStack();
          finish();
          if (verbose) printStack();
          return true;
        }
        return false;
      case 77:
        if (('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ')) {
          // Append(93,11,Some(93))
          replace(93);
          append(11);
          pendingFinish = 93;
          if (verbose) printStack();
          return true;
        }
        if ((c == '"')) {
          // Append(28,2,None)
          replace(28);
          append(2);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == '-')) {
          // Append(28,6,None)
          replace(28);
          append(6);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == '0')) {
          // Append(28,31,Some(28))
          replace(28);
          append(31);
          pendingFinish = 28;
          if (verbose) printStack();
          return true;
        }
        if (('1' <= c && c <= '9')) {
          // Append(28,29,Some(28))
          replace(28);
          append(29);
          pendingFinish = 28;
          if (verbose) printStack();
          return true;
        }
        if ((c == '[')) {
          // Append(28,3,None)
          replace(28);
          append(3);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == ']')) {
          // Finish(54)
          replace(54);
          if (verbose) printStack();
          finish();
          if (verbose) printStack();
          return true;
        }
        if ((c == 'f')) {
          // Append(28,10,None)
          replace(28);
          append(10);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == 'n')) {
          // Append(28,7,None)
          replace(28);
          append(7);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == 't')) {
          // Append(28,5,None)
          replace(28);
          append(5);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == '{')) {
          // Append(28,9,None)
          replace(28);
          append(9);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        return false;
      case 78:
        if (('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ')) {
          // Append(90,11,Some(90))
          replace(90);
          append(11);
          pendingFinish = 90;
          if (verbose) printStack();
          return true;
        }
        if ((c == '"')) {
          // Finish(23)
          replace(23);
          if (verbose) printStack();
          finish();
          if (verbose) printStack();
          return true;
        }
        if ((c == ':')) {
          // Finish(91)
          replace(91);
          if (verbose) printStack();
          finish();
          if (verbose) printStack();
          return true;
        }
        return false;
      case 79:
        if (('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ')) {
          // Append(98,34,Some(99))
          replace(98);
          append(34);
          pendingFinish = 99;
          if (verbose) printStack();
          return true;
        }
        if ((c == '"')) {
          // Append(36,2,None)
          replace(36);
          append(2);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == ',')) {
          // Finish(61)
          replace(61);
          if (verbose) printStack();
          finish();
          if (verbose) printStack();
          return true;
        }
        if ((c == '}')) {
          // Finish(64)
          replace(64);
          if (verbose) printStack();
          finish();
          if (verbose) printStack();
          return true;
        }
        return false;
      case 80:
        if ((c == 'e')) {
          // Finish(80)
          finish();
          if (verbose) printStack();
          return true;
        }
        return false;
      case 81:
        if (('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ')) {
          // Append(95,11,Some(95))
          replace(95);
          append(11);
          pendingFinish = 95;
          if (verbose) printStack();
          return true;
        }
        if ((c == '"')) {
          // Append(96,2,None)
          replace(96);
          append(2);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == ',')) {
          // Finish(61)
          replace(61);
          if (verbose) printStack();
          finish();
          if (verbose) printStack();
          return true;
        }
        if ((c == '-')) {
          // Append(96,6,None)
          replace(96);
          append(6);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == '0')) {
          // Append(96,31,Some(96))
          replace(96);
          append(31);
          pendingFinish = 96;
          if (verbose) printStack();
          return true;
        }
        if (('1' <= c && c <= '9')) {
          // Append(96,29,Some(96))
          replace(96);
          append(29);
          pendingFinish = 96;
          if (verbose) printStack();
          return true;
        }
        if ((c == 'E')) {
          // Append(18,20,None)
          replace(18);
          append(20);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == '[')) {
          // Append(96,3,None)
          replace(96);
          append(3);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == 'e')) {
          // Append(18,19,None)
          replace(18);
          append(19);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == 'f')) {
          // Append(96,10,None)
          replace(96);
          append(10);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == 'n')) {
          // Append(96,7,None)
          replace(96);
          append(7);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == 't')) {
          // Append(96,5,None)
          replace(96);
          append(5);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == '{')) {
          // Append(96,9,None)
          replace(96);
          append(9);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        return false;
      case 82:
        if (('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ')) {
          // Append(63,11,Some(63))
          replace(63);
          append(11);
          pendingFinish = 63;
          if (verbose) printStack();
          return true;
        }
        if ((c == '}')) {
          // Finish(92)
          replace(92);
          if (verbose) printStack();
          finish();
          if (verbose) printStack();
          return true;
        }
        return false;
      case 83:
        if (('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ')) {
          // Append(88,34,Some(89))
          replace(88);
          append(34);
          pendingFinish = 89;
          if (verbose) printStack();
          return true;
        }
        if ((c == '"')) {
          // Append(36,2,None)
          replace(36);
          append(2);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == '}')) {
          // Finish(64)
          replace(64);
          if (verbose) printStack();
          finish();
          if (verbose) printStack();
          return true;
        }
        return false;
      case 84:
        if (('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ')) {
          // Append(95,11,Some(95))
          replace(95);
          append(11);
          pendingFinish = 95;
          if (verbose) printStack();
          return true;
        }
        if ((c == '"')) {
          // Append(96,2,None)
          replace(96);
          append(2);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == ',')) {
          // Finish(61)
          replace(61);
          if (verbose) printStack();
          finish();
          if (verbose) printStack();
          return true;
        }
        if ((c == '-')) {
          // Append(96,6,None)
          replace(96);
          append(6);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == '.')) {
          // Append(16,17,None)
          replace(16);
          append(17);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == '0')) {
          // Append(96,31,Some(96))
          replace(96);
          append(31);
          pendingFinish = 96;
          if (verbose) printStack();
          return true;
        }
        if (('1' <= c && c <= '9')) {
          // Append(96,29,Some(96))
          replace(96);
          append(29);
          pendingFinish = 96;
          if (verbose) printStack();
          return true;
        }
        if ((c == 'E')) {
          // Append(18,20,None)
          replace(18);
          append(20);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == '[')) {
          // Append(96,3,None)
          replace(96);
          append(3);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == 'e')) {
          // Append(18,19,None)
          replace(18);
          append(19);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == 'f')) {
          // Append(96,10,None)
          replace(96);
          append(10);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == 'n')) {
          // Append(96,7,None)
          replace(96);
          append(7);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == 't')) {
          // Append(96,5,None)
          replace(96);
          append(5);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == '{')) {
          // Append(96,9,None)
          replace(96);
          append(9);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        return false;
      case 85:
        if (('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ')) {
          // Append(60,11,Some(60))
          replace(60);
          append(11);
          pendingFinish = 60;
          if (verbose) printStack();
          return true;
        }
        if ((c == '"')) {
          // Finish(23)
          replace(23);
          if (verbose) printStack();
          finish();
          if (verbose) printStack();
          return true;
        }
        if ((c == ',')) {
          // Finish(61)
          replace(61);
          if (verbose) printStack();
          finish();
          if (verbose) printStack();
          return true;
        }
        return false;
      case 86:
        if (('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ')) {
          // Append(36,34,None)
          replace(36);
          append(34);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == '"')) {
          // Append(36,2,None)
          replace(36);
          append(2);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == '}')) {
          // Finish(37)
          replace(37);
          if (verbose) printStack();
          finish();
          if (verbose) printStack();
          return true;
        }
        return false;
      case 87:
        if (('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ')) {
          // Append(60,11,Some(60))
          replace(60);
          append(11);
          pendingFinish = 60;
          if (verbose) printStack();
          return true;
        }
        if ((c == '"')) {
          // Append(28,2,None)
          replace(28);
          append(2);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == ',')) {
          // Finish(61)
          replace(61);
          if (verbose) printStack();
          finish();
          if (verbose) printStack();
          return true;
        }
        if ((c == '-')) {
          // Append(28,6,None)
          replace(28);
          append(6);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == '0')) {
          // Append(28,31,Some(28))
          replace(28);
          append(31);
          pendingFinish = 28;
          if (verbose) printStack();
          return true;
        }
        if (('1' <= c && c <= '9')) {
          // Append(28,29,Some(28))
          replace(28);
          append(29);
          pendingFinish = 28;
          if (verbose) printStack();
          return true;
        }
        if ((c == '[')) {
          // Append(28,3,None)
          replace(28);
          append(3);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == ']')) {
          // Finish(30)
          replace(30);
          if (verbose) printStack();
          finish();
          if (verbose) printStack();
          return true;
        }
        if ((c == 'f')) {
          // Append(28,10,None)
          replace(28);
          append(10);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == 'n')) {
          // Append(28,7,None)
          replace(28);
          append(7);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == 't')) {
          // Append(28,5,None)
          replace(28);
          append(5);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == '{')) {
          // Append(28,9,None)
          replace(28);
          append(9);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        return false;
      case 88:
        if (('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ')) {
          // Append(88,34,Some(89))
          append(34);
          pendingFinish = 89;
          if (verbose) printStack();
          return true;
        }
        if ((c == '"')) {
          // Append(36,2,None)
          replace(36);
          append(2);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == '}')) {
          // Finish(47)
          replace(47);
          if (verbose) printStack();
          finish();
          if (verbose) printStack();
          return true;
        }
        return false;
      case 89:
        if (('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ')) {
          // Append(89,11,Some(89))
          append(11);
          pendingFinish = 89;
          if (verbose) printStack();
          return true;
        }
        if ((c == '}')) {
          // Finish(47)
          replace(47);
          if (verbose) printStack();
          finish();
          if (verbose) printStack();
          return true;
        }
        return false;
      case 90:
        if (('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ')) {
          // Append(90,11,Some(90))
          append(11);
          pendingFinish = 90;
          if (verbose) printStack();
          return true;
        }
        if ((c == ':')) {
          // Append(47,117,None)
          replace(47);
          append(117);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        return false;
      case 91:
        if ((c == ':')) {
          // Finish(91)
          finish();
          if (verbose) printStack();
          return true;
        }
        return false;
      case 92:
        if ((c == '}')) {
          // Finish(92)
          finish();
          if (verbose) printStack();
          return true;
        }
        return false;
      case 93:
        if (('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ')) {
          // Append(93,11,Some(93))
          append(11);
          pendingFinish = 93;
          if (verbose) printStack();
          return true;
        }
        if ((c == ']')) {
          // Finish(47)
          replace(47);
          if (verbose) printStack();
          finish();
          if (verbose) printStack();
          return true;
        }
        return false;
      case 94:
        if ((c == ']')) {
          // Finish(94)
          finish();
          if (verbose) printStack();
          return true;
        }
        return false;
      case 95:
        if (('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ')) {
          // Append(95,11,Some(95))
          append(11);
          pendingFinish = 95;
          if (verbose) printStack();
          return true;
        }
        if ((c == '"')) {
          // Append(47,2,None)
          replace(47);
          append(2);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == ',')) {
          // Append(47,100,None)
          replace(47);
          append(100);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == '-')) {
          // Append(47,6,None)
          replace(47);
          append(6);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == '0')) {
          // Append(47,31,None)
          replace(47);
          append(31);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if (('1' <= c && c <= '9')) {
          // Append(47,29,None)
          replace(47);
          append(29);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == '[')) {
          // Append(47,3,None)
          replace(47);
          append(3);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == 'f')) {
          // Append(47,10,None)
          replace(47);
          append(10);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == 'n')) {
          // Append(47,7,None)
          replace(47);
          append(7);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == 't')) {
          // Append(47,5,None)
          replace(47);
          append(5);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == '{')) {
          // Append(47,9,None)
          replace(47);
          append(9);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        return false;
      case 96:
        if ((c == '"')) {
          // Append(96,2,None)
          append(2);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == '-')) {
          // Append(96,6,None)
          append(6);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == '0')) {
          // Append(96,31,Some(96))
          append(31);
          pendingFinish = 96;
          if (verbose) printStack();
          return true;
        }
        if (('1' <= c && c <= '9')) {
          // Append(96,29,Some(96))
          append(29);
          pendingFinish = 96;
          if (verbose) printStack();
          return true;
        }
        if ((c == '[')) {
          // Append(96,3,None)
          append(3);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == 'f')) {
          // Append(96,10,None)
          append(10);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == 'n')) {
          // Append(96,7,None)
          append(7);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == 't')) {
          // Append(96,5,None)
          append(5);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == '{')) {
          // Append(96,9,None)
          append(9);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        return false;
      case 97:
        if (('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ')) {
          // Append(97,11,Some(97))
          append(11);
          pendingFinish = 97;
          if (verbose) printStack();
          return true;
        }
        if ((c == ',')) {
          // Append(47,100,None)
          replace(47);
          append(100);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == ']')) {
          // Finish(47)
          replace(47);
          if (verbose) printStack();
          finish();
          if (verbose) printStack();
          return true;
        }
        return false;
      case 98:
        if (('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ')) {
          // Append(98,34,Some(99))
          append(34);
          pendingFinish = 99;
          if (verbose) printStack();
          return true;
        }
        if ((c == '"')) {
          // Append(36,2,None)
          replace(36);
          append(2);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == ',')) {
          // Append(47,100,None)
          replace(47);
          append(100);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == '}')) {
          // Finish(47)
          replace(47);
          if (verbose) printStack();
          finish();
          if (verbose) printStack();
          return true;
        }
        return false;
      case 99:
        if (('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ')) {
          // Append(99,11,Some(99))
          append(11);
          pendingFinish = 99;
          if (verbose) printStack();
          return true;
        }
        if ((c == ',')) {
          // Append(47,100,None)
          replace(47);
          append(100);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == '}')) {
          // Finish(47)
          replace(47);
          if (verbose) printStack();
          finish();
          if (verbose) printStack();
          return true;
        }
        return false;
      case 100:
        if (('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ')) {
          // Append(114,11,Some(114))
          replace(114);
          append(11);
          pendingFinish = 114;
          if (verbose) printStack();
          return true;
        }
        if ((c == '"')) {
          // Append(96,2,None)
          replace(96);
          append(2);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == '-')) {
          // Append(96,6,None)
          replace(96);
          append(6);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == '0')) {
          // Append(96,31,Some(96))
          replace(96);
          append(31);
          pendingFinish = 96;
          if (verbose) printStack();
          return true;
        }
        if (('1' <= c && c <= '9')) {
          // Append(96,29,Some(96))
          replace(96);
          append(29);
          pendingFinish = 96;
          if (verbose) printStack();
          return true;
        }
        if ((c == '[')) {
          // Append(96,3,None)
          replace(96);
          append(3);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == 'f')) {
          // Append(96,10,None)
          replace(96);
          append(10);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == 'n')) {
          // Append(96,7,None)
          replace(96);
          append(7);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == 't')) {
          // Append(96,5,None)
          replace(96);
          append(5);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == '{')) {
          // Append(96,9,None)
          replace(96);
          append(9);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        return false;
      case 101:
        if (('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ')) {
          // Append(99,11,Some(99))
          replace(99);
          append(11);
          pendingFinish = 99;
          if (verbose) printStack();
          return true;
        }
        if ((c == ',')) {
          // Finish(61)
          replace(61);
          if (verbose) printStack();
          finish();
          if (verbose) printStack();
          return true;
        }
        if ((c == '}')) {
          // Finish(92)
          replace(92);
          if (verbose) printStack();
          finish();
          if (verbose) printStack();
          return true;
        }
        return false;
      case 102:
        if (('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ')) {
          // Append(115,11,Some(115))
          replace(115);
          append(11);
          pendingFinish = 115;
          if (verbose) printStack();
          return true;
        }
        if ((c == '"')) {
          // Append(116,2,None)
          replace(116);
          append(2);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == '-')) {
          // Append(116,6,None)
          replace(116);
          append(6);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == '0')) {
          // Append(116,14,Some(116))
          replace(116);
          append(14);
          pendingFinish = 116;
          if (verbose) printStack();
          return true;
        }
        if (('1' <= c && c <= '9')) {
          // Append(116,13,Some(116))
          replace(116);
          append(13);
          pendingFinish = 116;
          if (verbose) printStack();
          return true;
        }
        if ((c == ':')) {
          // Finish(91)
          replace(91);
          if (verbose) printStack();
          finish();
          if (verbose) printStack();
          return true;
        }
        if ((c == '[')) {
          // Append(116,3,None)
          replace(116);
          append(3);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == 'f')) {
          // Append(116,10,None)
          replace(116);
          append(10);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == 'n')) {
          // Append(116,7,None)
          replace(116);
          append(7);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == 't')) {
          // Append(116,5,None)
          replace(116);
          append(5);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == '{')) {
          // Append(116,9,None)
          replace(116);
          append(9);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        return false;
      case 103:
        if (('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ')) {
          // Append(95,11,Some(95))
          replace(95);
          append(11);
          pendingFinish = 95;
          if (verbose) printStack();
          return true;
        }
        if ((c == '"')) {
          // Append(96,2,None)
          replace(96);
          append(2);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == ',')) {
          // Finish(61)
          replace(61);
          if (verbose) printStack();
          finish();
          if (verbose) printStack();
          return true;
        }
        if ((c == '-')) {
          // Append(96,6,None)
          replace(96);
          append(6);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == '0')) {
          // Append(96,31,Some(96))
          replace(96);
          append(31);
          pendingFinish = 96;
          if (verbose) printStack();
          return true;
        }
        if (('1' <= c && c <= '9')) {
          // Append(96,29,Some(96))
          replace(96);
          append(29);
          pendingFinish = 96;
          if (verbose) printStack();
          return true;
        }
        if ((c == '[')) {
          // Append(96,3,None)
          replace(96);
          append(3);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == 'f')) {
          // Append(96,10,None)
          replace(96);
          append(10);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == 'n')) {
          // Append(96,7,None)
          replace(96);
          append(7);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == 't')) {
          // Append(96,5,None)
          replace(96);
          append(5);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == '{')) {
          // Append(96,9,None)
          replace(96);
          append(9);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        return false;
      case 104:
        if ((c == 'e')) {
          // Finish(104)
          finish();
          if (verbose) printStack();
          return true;
        }
        return false;
      case 105:
        if (('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ')) {
          // Append(114,11,Some(114))
          replace(114);
          append(11);
          pendingFinish = 114;
          if (verbose) printStack();
          return true;
        }
        if ((c == '"')) {
          // Append(96,2,None)
          replace(96);
          append(2);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == ',')) {
          // Finish(61)
          replace(61);
          if (verbose) printStack();
          finish();
          if (verbose) printStack();
          return true;
        }
        if ((c == '-')) {
          // Append(96,6,None)
          replace(96);
          append(6);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == '0')) {
          // Append(96,31,Some(96))
          replace(96);
          append(31);
          pendingFinish = 96;
          if (verbose) printStack();
          return true;
        }
        if (('1' <= c && c <= '9')) {
          // Append(96,29,Some(96))
          replace(96);
          append(29);
          pendingFinish = 96;
          if (verbose) printStack();
          return true;
        }
        if ((c == '[')) {
          // Append(96,3,None)
          replace(96);
          append(3);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == 'f')) {
          // Append(96,10,None)
          replace(96);
          append(10);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == 'n')) {
          // Append(96,7,None)
          replace(96);
          append(7);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == 't')) {
          // Append(96,5,None)
          replace(96);
          append(5);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == '{')) {
          // Append(96,9,None)
          replace(96);
          append(9);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        return false;
      case 106:
        if (('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ')) {
          // Append(15,11,Some(15))
          replace(15);
          append(11);
          pendingFinish = 15;
          if (verbose) printStack();
          return true;
        }
        if ((c == ']')) {
          // Finish(94)
          replace(94);
          if (verbose) printStack();
          finish();
          if (verbose) printStack();
          return true;
        }
        return false;
      case 107:
        if (('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ')) {
          // Append(60,11,Some(60))
          replace(60);
          append(11);
          pendingFinish = 60;
          if (verbose) printStack();
          return true;
        }
        if ((c == ',')) {
          // Finish(61)
          replace(61);
          if (verbose) printStack();
          finish();
          if (verbose) printStack();
          return true;
        }
        if ((c == '}')) {
          // Finish(92)
          replace(92);
          if (verbose) printStack();
          finish();
          if (verbose) printStack();
          return true;
        }
        return false;
      case 108:
        if (('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ')) {
          // Append(15,11,Some(15))
          replace(15);
          append(11);
          pendingFinish = 15;
          if (verbose) printStack();
          return true;
        }
        if ((c == '}')) {
          // Finish(92)
          replace(92);
          if (verbose) printStack();
          finish();
          if (verbose) printStack();
          return true;
        }
        return false;
      case 109:
        if ((c == '0')) {
          // Finish(109)
          finish();
          if (verbose) printStack();
          return true;
        }
        if (('1' <= c && c <= '9')) {
          // Finish(109)
          finish();
          if (verbose) printStack();
          return true;
        }
        if (('A' <= c && c <= 'F') || ('a' <= c && c <= 'f')) {
          // Finish(109)
          finish();
          if (verbose) printStack();
          return true;
        }
        return false;
      case 110:
        if (('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ')) {
          // Append(113,11,Some(113))
          replace(113);
          append(11);
          pendingFinish = 113;
          if (verbose) printStack();
          return true;
        }
        if ((c == '"')) {
          // Append(96,2,None)
          replace(96);
          append(2);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == ',')) {
          // Finish(61)
          replace(61);
          if (verbose) printStack();
          finish();
          if (verbose) printStack();
          return true;
        }
        if ((c == '-')) {
          // Append(96,6,None)
          replace(96);
          append(6);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == '0')) {
          // Append(96,31,Some(96))
          replace(96);
          append(31);
          pendingFinish = 96;
          if (verbose) printStack();
          return true;
        }
        if (('1' <= c && c <= '9')) {
          // Append(96,29,Some(96))
          replace(96);
          append(29);
          pendingFinish = 96;
          if (verbose) printStack();
          return true;
        }
        if ((c == '[')) {
          // Append(96,3,None)
          replace(96);
          append(3);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == 'f')) {
          // Append(96,10,None)
          replace(96);
          append(10);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == 'n')) {
          // Append(96,7,None)
          replace(96);
          append(7);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == 't')) {
          // Append(96,5,None)
          replace(96);
          append(5);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == '{')) {
          // Append(96,9,None)
          replace(96);
          append(9);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == '}')) {
          // Finish(92)
          replace(92);
          if (verbose) printStack();
          finish();
          if (verbose) printStack();
          return true;
        }
        return false;
      case 111:
        if (('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ')) {
          // Append(118,11,Some(118))
          replace(118);
          append(11);
          pendingFinish = 118;
          if (verbose) printStack();
          return true;
        }
        if ((c == '"')) {
          // Append(116,2,None)
          replace(116);
          append(2);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == '-')) {
          // Append(116,6,None)
          replace(116);
          append(6);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == '0')) {
          // Append(116,14,Some(116))
          replace(116);
          append(14);
          pendingFinish = 116;
          if (verbose) printStack();
          return true;
        }
        if (('1' <= c && c <= '9')) {
          // Append(116,13,Some(116))
          replace(116);
          append(13);
          pendingFinish = 116;
          if (verbose) printStack();
          return true;
        }
        if ((c == ':')) {
          // Finish(91)
          replace(91);
          if (verbose) printStack();
          finish();
          if (verbose) printStack();
          return true;
        }
        if ((c == '[')) {
          // Append(116,3,None)
          replace(116);
          append(3);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == 'f')) {
          // Append(116,10,None)
          replace(116);
          append(10);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == 'n')) {
          // Append(116,7,None)
          replace(116);
          append(7);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == 't')) {
          // Append(116,5,None)
          replace(116);
          append(5);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == '{')) {
          // Append(116,9,None)
          replace(116);
          append(9);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        return false;
      case 112:
        if (('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ')) {
          // Append(60,11,Some(60))
          replace(60);
          append(11);
          pendingFinish = 60;
          if (verbose) printStack();
          return true;
        }
        if ((c == ',')) {
          // Finish(61)
          replace(61);
          if (verbose) printStack();
          finish();
          if (verbose) printStack();
          return true;
        }
        if ((c == 'E')) {
          // Append(18,20,None)
          replace(18);
          append(20);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == 'e')) {
          // Append(18,19,None)
          replace(18);
          append(19);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        return false;
      case 113:
        if (('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ')) {
          // Append(113,11,Some(113))
          append(11);
          pendingFinish = 113;
          if (verbose) printStack();
          return true;
        }
        if ((c == '"')) {
          // Append(47,2,None)
          replace(47);
          append(2);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == ',')) {
          // Append(47,100,None)
          replace(47);
          append(100);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == '-')) {
          // Append(47,6,None)
          replace(47);
          append(6);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == '0')) {
          // Append(47,31,None)
          replace(47);
          append(31);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if (('1' <= c && c <= '9')) {
          // Append(47,29,None)
          replace(47);
          append(29);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == '[')) {
          // Append(47,3,None)
          replace(47);
          append(3);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == 'f')) {
          // Append(47,10,None)
          replace(47);
          append(10);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == 'n')) {
          // Append(47,7,None)
          replace(47);
          append(7);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == 't')) {
          // Append(47,5,None)
          replace(47);
          append(5);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == '{')) {
          // Append(47,9,None)
          replace(47);
          append(9);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == '}')) {
          // Append(47,75,None)
          replace(47);
          append(75);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        return false;
      case 114:
        if (('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ')) {
          // Append(114,11,Some(114))
          append(11);
          pendingFinish = 114;
          if (verbose) printStack();
          return true;
        }
        if ((c == '"')) {
          // Append(47,2,None)
          replace(47);
          append(2);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == '-')) {
          // Append(47,6,None)
          replace(47);
          append(6);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == '0')) {
          // Append(47,31,None)
          replace(47);
          append(31);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if (('1' <= c && c <= '9')) {
          // Append(47,29,None)
          replace(47);
          append(29);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == '[')) {
          // Append(47,3,None)
          replace(47);
          append(3);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == 'f')) {
          // Append(47,10,None)
          replace(47);
          append(10);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == 'n')) {
          // Append(47,7,None)
          replace(47);
          append(7);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == 't')) {
          // Append(47,5,None)
          replace(47);
          append(5);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == '{')) {
          // Append(47,9,None)
          replace(47);
          append(9);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        return false;
      case 115:
        if (('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ')) {
          // Append(115,11,Some(115))
          append(11);
          pendingFinish = 115;
          if (verbose) printStack();
          return true;
        }
        if ((c == '"')) {
          // Append(47,2,None)
          replace(47);
          append(2);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == '-')) {
          // Append(47,6,None)
          replace(47);
          append(6);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == '0')) {
          // Append(47,14,None)
          replace(47);
          append(14);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if (('1' <= c && c <= '9')) {
          // Append(47,13,None)
          replace(47);
          append(13);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == ':')) {
          // Append(47,117,None)
          replace(47);
          append(117);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == '[')) {
          // Append(47,3,None)
          replace(47);
          append(3);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == 'f')) {
          // Append(47,10,None)
          replace(47);
          append(10);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == 'n')) {
          // Append(47,7,None)
          replace(47);
          append(7);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == 't')) {
          // Append(47,5,None)
          replace(47);
          append(5);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == '{')) {
          // Append(47,9,None)
          replace(47);
          append(9);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        return false;
      case 116:
        if ((c == '"')) {
          // Append(116,2,None)
          append(2);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == '-')) {
          // Append(116,6,None)
          append(6);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == '0')) {
          // Append(116,14,Some(116))
          append(14);
          pendingFinish = 116;
          if (verbose) printStack();
          return true;
        }
        if (('1' <= c && c <= '9')) {
          // Append(116,13,Some(116))
          append(13);
          pendingFinish = 116;
          if (verbose) printStack();
          return true;
        }
        if ((c == '[')) {
          // Append(116,3,None)
          append(3);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == 'f')) {
          // Append(116,10,None)
          append(10);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == 'n')) {
          // Append(116,7,None)
          append(7);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == 't')) {
          // Append(116,5,None)
          append(5);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == '{')) {
          // Append(116,9,None)
          append(9);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        return false;
      case 117:
        if (('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ')) {
          // Append(118,11,Some(118))
          replace(118);
          append(11);
          pendingFinish = 118;
          if (verbose) printStack();
          return true;
        }
        if ((c == '"')) {
          // Append(116,2,None)
          replace(116);
          append(2);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == '-')) {
          // Append(116,6,None)
          replace(116);
          append(6);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == '0')) {
          // Append(116,14,Some(116))
          replace(116);
          append(14);
          pendingFinish = 116;
          if (verbose) printStack();
          return true;
        }
        if (('1' <= c && c <= '9')) {
          // Append(116,13,Some(116))
          replace(116);
          append(13);
          pendingFinish = 116;
          if (verbose) printStack();
          return true;
        }
        if ((c == '[')) {
          // Append(116,3,None)
          replace(116);
          append(3);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == 'f')) {
          // Append(116,10,None)
          replace(116);
          append(10);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == 'n')) {
          // Append(116,7,None)
          replace(116);
          append(7);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == 't')) {
          // Append(116,5,None)
          replace(116);
          append(5);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == '{')) {
          // Append(116,9,None)
          replace(116);
          append(9);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        return false;
      case 118:
        if (('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ')) {
          // Append(118,11,Some(118))
          append(11);
          pendingFinish = 118;
          if (verbose) printStack();
          return true;
        }
        if ((c == '"')) {
          // Append(47,2,None)
          replace(47);
          append(2);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == '-')) {
          // Append(47,6,None)
          replace(47);
          append(6);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == '0')) {
          // Append(47,14,None)
          replace(47);
          append(14);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if (('1' <= c && c <= '9')) {
          // Append(47,13,None)
          replace(47);
          append(13);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == '[')) {
          // Append(47,3,None)
          replace(47);
          append(3);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == 'f')) {
          // Append(47,10,None)
          replace(47);
          append(10);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == 'n')) {
          // Append(47,7,None)
          replace(47);
          append(7);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == 't')) {
          // Append(47,5,None)
          replace(47);
          append(5);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == '{')) {
          // Append(47,9,None)
          replace(47);
          append(9);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        return false;
      case 119:
        if ((c == '"')) {
          // Append(96,2,None)
          replace(96);
          append(2);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == ',')) {
          // Finish(61)
          replace(61);
          if (verbose) printStack();
          finish();
          if (verbose) printStack();
          return true;
        }
        if ((c == '-')) {
          // Append(96,6,None)
          replace(96);
          append(6);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == '0')) {
          // Append(96,31,Some(96))
          replace(96);
          append(31);
          pendingFinish = 96;
          if (verbose) printStack();
          return true;
        }
        if (('1' <= c && c <= '9')) {
          // Append(96,29,Some(96))
          replace(96);
          append(29);
          pendingFinish = 96;
          if (verbose) printStack();
          return true;
        }
        if ((c == '[')) {
          // Append(96,3,None)
          replace(96);
          append(3);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == 'f')) {
          // Append(96,10,None)
          replace(96);
          append(10);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == 'n')) {
          // Append(96,7,None)
          replace(96);
          append(7);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == 't')) {
          // Append(96,5,None)
          replace(96);
          append(5);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == '{')) {
          // Append(96,9,None)
          replace(96);
          append(9);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        return false;
      case 120:
        if (('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ')) {
          // Append(97,11,Some(97))
          replace(97);
          append(11);
          pendingFinish = 97;
          if (verbose) printStack();
          return true;
        }
        if ((c == '"')) {
          // Append(28,2,None)
          replace(28);
          append(2);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == ',')) {
          // Finish(61)
          replace(61);
          if (verbose) printStack();
          finish();
          if (verbose) printStack();
          return true;
        }
        if ((c == '-')) {
          // Append(28,6,None)
          replace(28);
          append(6);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == '0')) {
          // Append(28,31,Some(28))
          replace(28);
          append(31);
          pendingFinish = 28;
          if (verbose) printStack();
          return true;
        }
        if (('1' <= c && c <= '9')) {
          // Append(28,29,Some(28))
          replace(28);
          append(29);
          pendingFinish = 28;
          if (verbose) printStack();
          return true;
        }
        if ((c == '[')) {
          // Append(28,3,None)
          replace(28);
          append(3);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == ']')) {
          // Finish(54)
          replace(54);
          if (verbose) printStack();
          finish();
          if (verbose) printStack();
          return true;
        }
        if ((c == 'f')) {
          // Append(28,10,None)
          replace(28);
          append(10);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == 'n')) {
          // Append(28,7,None)
          replace(28);
          append(7);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == 't')) {
          // Append(28,5,None)
          replace(28);
          append(5);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == '{')) {
          // Append(28,9,None)
          replace(28);
          append(9);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        return false;
      case 121:
        if (('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ')) {
          // Append(127,11,Some(127))
          replace(127);
          append(11);
          pendingFinish = 127;
          if (verbose) printStack();
          return true;
        }
        if ((c == '"')) {
          // Append(116,2,None)
          replace(116);
          append(2);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == ',')) {
          // Finish(128)
          replace(128);
          if (verbose) printStack();
          finish();
          if (verbose) printStack();
          return true;
        }
        if ((c == '-')) {
          // Append(116,6,None)
          replace(116);
          append(6);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == '0')) {
          // Append(116,14,Some(116))
          replace(116);
          append(14);
          pendingFinish = 116;
          if (verbose) printStack();
          return true;
        }
        if (('1' <= c && c <= '9')) {
          // Append(116,13,Some(116))
          replace(116);
          append(13);
          pendingFinish = 116;
          if (verbose) printStack();
          return true;
        }
        if ((c == ':')) {
          // Finish(91)
          replace(91);
          if (verbose) printStack();
          finish();
          if (verbose) printStack();
          return true;
        }
        if ((c == '[')) {
          // Append(116,3,None)
          replace(116);
          append(3);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == 'f')) {
          // Append(116,10,None)
          replace(116);
          append(10);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == 'n')) {
          // Append(116,7,None)
          replace(116);
          append(7);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == 't')) {
          // Append(116,5,None)
          replace(116);
          append(5);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == '{')) {
          // Append(116,9,None)
          replace(116);
          append(9);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        return false;
      case 122:
        if ((c == ',')) {
          // Finish(61)
          replace(61);
          if (verbose) printStack();
          finish();
          if (verbose) printStack();
          return true;
        }
        if ((c == '}')) {
          // Finish(92)
          replace(92);
          if (verbose) printStack();
          finish();
          if (verbose) printStack();
          return true;
        }
        return false;
      case 123:
        if (('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ')) {
          // Append(127,11,Some(127))
          replace(127);
          append(11);
          pendingFinish = 127;
          if (verbose) printStack();
          return true;
        }
        if ((c == '"')) {
          // Append(116,2,None)
          replace(116);
          append(2);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == ',')) {
          // Finish(128)
          replace(128);
          if (verbose) printStack();
          finish();
          if (verbose) printStack();
          return true;
        }
        if ((c == '-')) {
          // Append(116,6,None)
          replace(116);
          append(6);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == '0')) {
          // Append(116,14,Some(116))
          replace(116);
          append(14);
          pendingFinish = 116;
          if (verbose) printStack();
          return true;
        }
        if (('1' <= c && c <= '9')) {
          // Append(116,13,Some(116))
          replace(116);
          append(13);
          pendingFinish = 116;
          if (verbose) printStack();
          return true;
        }
        if ((c == '[')) {
          // Append(116,3,None)
          replace(116);
          append(3);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == 'f')) {
          // Append(116,10,None)
          replace(116);
          append(10);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == 'n')) {
          // Append(116,7,None)
          replace(116);
          append(7);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == 't')) {
          // Append(116,5,None)
          replace(116);
          append(5);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == '{')) {
          // Append(116,9,None)
          replace(116);
          append(9);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        return false;
      case 124:
        if ((c == '0')) {
          // Finish(124)
          finish();
          if (verbose) printStack();
          return true;
        }
        if (('1' <= c && c <= '9')) {
          // Finish(124)
          finish();
          if (verbose) printStack();
          return true;
        }
        if (('A' <= c && c <= 'F') || ('a' <= c && c <= 'f')) {
          // Finish(124)
          finish();
          if (verbose) printStack();
          return true;
        }
        return false;
      case 125:
        if (('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ')) {
          // Append(95,11,Some(95))
          replace(95);
          append(11);
          pendingFinish = 95;
          if (verbose) printStack();
          return true;
        }
        if ((c == '"')) {
          // Append(96,2,None)
          replace(96);
          append(2);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == ',')) {
          // Finish(61)
          replace(61);
          if (verbose) printStack();
          finish();
          if (verbose) printStack();
          return true;
        }
        if ((c == '-')) {
          // Append(96,6,None)
          replace(96);
          append(6);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == '0')) {
          // Append(96,31,Some(96))
          replace(96);
          append(31);
          pendingFinish = 96;
          if (verbose) printStack();
          return true;
        }
        if (('1' <= c && c <= '9')) {
          // Append(96,29,Some(96))
          replace(96);
          append(29);
          pendingFinish = 96;
          if (verbose) printStack();
          return true;
        }
        if ((c == '[')) {
          // Append(96,3,None)
          replace(96);
          append(3);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == 'f')) {
          // Append(96,10,None)
          replace(96);
          append(10);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == 'n')) {
          // Append(96,7,None)
          replace(96);
          append(7);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == 't')) {
          // Append(96,5,None)
          replace(96);
          append(5);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == '{')) {
          // Append(96,9,None)
          replace(96);
          append(9);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == '}')) {
          // Finish(92)
          replace(92);
          if (verbose) printStack();
          finish();
          if (verbose) printStack();
          return true;
        }
        return false;
      case 126:
        if (('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ')) {
          // Append(89,11,Some(89))
          replace(89);
          append(11);
          pendingFinish = 89;
          if (verbose) printStack();
          return true;
        }
        if ((c == ',')) {
          // Finish(61)
          replace(61);
          if (verbose) printStack();
          finish();
          if (verbose) printStack();
          return true;
        }
        if ((c == '}')) {
          // Finish(92)
          replace(92);
          if (verbose) printStack();
          finish();
          if (verbose) printStack();
          return true;
        }
        return false;
      case 127:
        if (('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ')) {
          // Append(127,11,Some(127))
          append(11);
          pendingFinish = 127;
          if (verbose) printStack();
          return true;
        }
        if ((c == '"')) {
          // Append(47,2,None)
          replace(47);
          append(2);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == ',')) {
          // Append(47,138,None)
          replace(47);
          append(138);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == '-')) {
          // Append(47,6,None)
          replace(47);
          append(6);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == '0')) {
          // Append(47,14,None)
          replace(47);
          append(14);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if (('1' <= c && c <= '9')) {
          // Append(47,13,None)
          replace(47);
          append(13);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == '[')) {
          // Append(47,3,None)
          replace(47);
          append(3);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == 'f')) {
          // Append(47,10,None)
          replace(47);
          append(10);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == 'n')) {
          // Append(47,7,None)
          replace(47);
          append(7);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == 't')) {
          // Append(47,5,None)
          replace(47);
          append(5);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == '{')) {
          // Append(47,9,None)
          replace(47);
          append(9);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        return false;
      case 128:
        if ((c == ',')) {
          // Finish(128)
          finish();
          if (verbose) printStack();
          return true;
        }
        return false;
      case 129:
        if ((c == '"')) {
          // Append(96,2,None)
          replace(96);
          append(2);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == ',')) {
          // Finish(61)
          replace(61);
          if (verbose) printStack();
          finish();
          if (verbose) printStack();
          return true;
        }
        if ((c == '-')) {
          // Append(96,6,None)
          replace(96);
          append(6);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == '0')) {
          // Append(96,31,Some(96))
          replace(96);
          append(31);
          pendingFinish = 96;
          if (verbose) printStack();
          return true;
        }
        if (('1' <= c && c <= '9')) {
          // Append(96,29,Some(96))
          replace(96);
          append(29);
          pendingFinish = 96;
          if (verbose) printStack();
          return true;
        }
        if ((c == '[')) {
          // Append(96,3,None)
          replace(96);
          append(3);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == 'f')) {
          // Append(96,10,None)
          replace(96);
          append(10);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == 'n')) {
          // Append(96,7,None)
          replace(96);
          append(7);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == 't')) {
          // Append(96,5,None)
          replace(96);
          append(5);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == '{')) {
          // Append(96,9,None)
          replace(96);
          append(9);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == '}')) {
          // Finish(92)
          replace(92);
          if (verbose) printStack();
          finish();
          if (verbose) printStack();
          return true;
        }
        return false;
      case 130:
        if (('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ')) {
          // Append(143,11,Some(143))
          replace(143);
          append(11);
          pendingFinish = 143;
          if (verbose) printStack();
          return true;
        }
        if ((c == '"')) {
          // Append(96,2,None)
          replace(96);
          append(2);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == ',')) {
          // Finish(61)
          replace(61);
          if (verbose) printStack();
          finish();
          if (verbose) printStack();
          return true;
        }
        if ((c == '-')) {
          // Append(96,6,None)
          replace(96);
          append(6);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == '0')) {
          // Append(96,31,Some(96))
          replace(96);
          append(31);
          pendingFinish = 96;
          if (verbose) printStack();
          return true;
        }
        if (('1' <= c && c <= '9')) {
          // Append(96,29,Some(96))
          replace(96);
          append(29);
          pendingFinish = 96;
          if (verbose) printStack();
          return true;
        }
        if ((c == '[')) {
          // Append(96,3,None)
          replace(96);
          append(3);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == ']')) {
          // Finish(94)
          replace(94);
          if (verbose) printStack();
          finish();
          if (verbose) printStack();
          return true;
        }
        if ((c == 'f')) {
          // Append(96,10,None)
          replace(96);
          append(10);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == 'n')) {
          // Append(96,7,None)
          replace(96);
          append(7);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == 't')) {
          // Append(96,5,None)
          replace(96);
          append(5);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == '{')) {
          // Append(96,9,None)
          replace(96);
          append(9);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        return false;
      case 131:
        if ((c == '"')) {
          // Append(116,2,None)
          replace(116);
          append(2);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == '-')) {
          // Append(116,6,None)
          replace(116);
          append(6);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == '0')) {
          // Append(116,14,Some(116))
          replace(116);
          append(14);
          pendingFinish = 116;
          if (verbose) printStack();
          return true;
        }
        if (('1' <= c && c <= '9')) {
          // Append(116,13,Some(116))
          replace(116);
          append(13);
          pendingFinish = 116;
          if (verbose) printStack();
          return true;
        }
        if ((c == ':')) {
          // Finish(91)
          replace(91);
          if (verbose) printStack();
          finish();
          if (verbose) printStack();
          return true;
        }
        if ((c == '[')) {
          // Append(116,3,None)
          replace(116);
          append(3);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == 'f')) {
          // Append(116,10,None)
          replace(116);
          append(10);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == 'n')) {
          // Append(116,7,None)
          replace(116);
          append(7);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == 't')) {
          // Append(116,5,None)
          replace(116);
          append(5);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == '{')) {
          // Append(116,9,None)
          replace(116);
          append(9);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        return false;
      case 132:
        if (('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ')) {
          // Append(139,11,Some(139))
          replace(139);
          append(11);
          pendingFinish = 139;
          if (verbose) printStack();
          return true;
        }
        if ((c == ',')) {
          // Finish(128)
          replace(128);
          if (verbose) printStack();
          finish();
          if (verbose) printStack();
          return true;
        }
        return false;
      case 133:
        if (('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ')) {
          // Append(139,11,Some(139))
          replace(139);
          append(11);
          pendingFinish = 139;
          if (verbose) printStack();
          return true;
        }
        if ((c == '"')) {
          // Append(116,2,None)
          replace(116);
          append(2);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == ',')) {
          // Finish(128)
          replace(128);
          if (verbose) printStack();
          finish();
          if (verbose) printStack();
          return true;
        }
        if ((c == '-')) {
          // Append(116,6,None)
          replace(116);
          append(6);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == '0')) {
          // Append(116,14,Some(116))
          replace(116);
          append(14);
          pendingFinish = 116;
          if (verbose) printStack();
          return true;
        }
        if (('1' <= c && c <= '9')) {
          // Append(116,13,Some(116))
          replace(116);
          append(13);
          pendingFinish = 116;
          if (verbose) printStack();
          return true;
        }
        if ((c == '[')) {
          // Append(116,3,None)
          replace(116);
          append(3);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == 'f')) {
          // Append(116,10,None)
          replace(116);
          append(10);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == 'n')) {
          // Append(116,7,None)
          replace(116);
          append(7);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == 't')) {
          // Append(116,5,None)
          replace(116);
          append(5);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == '{')) {
          // Append(116,9,None)
          replace(116);
          append(9);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        return false;
      case 134:
        if (('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ')) {
          // Append(60,11,Some(60))
          replace(60);
          append(11);
          pendingFinish = 60;
          if (verbose) printStack();
          return true;
        }
        if ((c == ',')) {
          // Finish(61)
          replace(61);
          if (verbose) printStack();
          finish();
          if (verbose) printStack();
          return true;
        }
        if ((c == ']')) {
          // Finish(94)
          replace(94);
          if (verbose) printStack();
          finish();
          if (verbose) printStack();
          return true;
        }
        return false;
      case 135:
        if (('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ')) {
          // Append(144,34,Some(145))
          replace(144);
          append(34);
          pendingFinish = 145;
          if (verbose) printStack();
          return true;
        }
        if ((c == '"')) {
          // Append(142,2,None)
          replace(142);
          append(2);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == ',')) {
          // Finish(128)
          replace(128);
          if (verbose) printStack();
          finish();
          if (verbose) printStack();
          return true;
        }
        if ((c == '-')) {
          // Append(116,6,None)
          replace(116);
          append(6);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == '0')) {
          // Append(116,14,Some(116))
          replace(116);
          append(14);
          pendingFinish = 116;
          if (verbose) printStack();
          return true;
        }
        if (('1' <= c && c <= '9')) {
          // Append(116,13,Some(116))
          replace(116);
          append(13);
          pendingFinish = 116;
          if (verbose) printStack();
          return true;
        }
        if ((c == '[')) {
          // Append(116,3,None)
          replace(116);
          append(3);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == 'f')) {
          // Append(116,10,None)
          replace(116);
          append(10);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == 'n')) {
          // Append(116,7,None)
          replace(116);
          append(7);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == 't')) {
          // Append(116,5,None)
          replace(116);
          append(5);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == '{')) {
          // Append(116,9,None)
          replace(116);
          append(9);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        return false;
      case 136:
        if (('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ')) {
          // Append(140,34,Some(141))
          replace(140);
          append(34);
          pendingFinish = 141;
          if (verbose) printStack();
          return true;
        }
        if ((c == '"')) {
          // Append(142,2,None)
          replace(142);
          append(2);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == ',')) {
          // Finish(128)
          replace(128);
          if (verbose) printStack();
          finish();
          if (verbose) printStack();
          return true;
        }
        if ((c == '-')) {
          // Append(116,6,None)
          replace(116);
          append(6);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == '0')) {
          // Append(116,14,Some(116))
          replace(116);
          append(14);
          pendingFinish = 116;
          if (verbose) printStack();
          return true;
        }
        if (('1' <= c && c <= '9')) {
          // Append(116,13,Some(116))
          replace(116);
          append(13);
          pendingFinish = 116;
          if (verbose) printStack();
          return true;
        }
        if ((c == '[')) {
          // Append(116,3,None)
          replace(116);
          append(3);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == 'f')) {
          // Append(116,10,None)
          replace(116);
          append(10);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == 'n')) {
          // Append(116,7,None)
          replace(116);
          append(7);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == 't')) {
          // Append(116,5,None)
          replace(116);
          append(5);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == '{')) {
          // Append(116,9,None)
          replace(116);
          append(9);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        return false;
      case 137:
        if ((c == '0')) {
          // Finish(137)
          finish();
          if (verbose) printStack();
          return true;
        }
        if (('1' <= c && c <= '9')) {
          // Finish(137)
          finish();
          if (verbose) printStack();
          return true;
        }
        if (('A' <= c && c <= 'F') || ('a' <= c && c <= 'f')) {
          // Finish(137)
          finish();
          if (verbose) printStack();
          return true;
        }
        return false;
      case 138:
        if (('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ')) {
          // Append(138,34,Some(154))
          append(34);
          pendingFinish = 154;
          if (verbose) printStack();
          return true;
        }
        if ((c == '"')) {
          // Append(151,2,None)
          replace(151);
          append(2);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        return false;
      case 139:
        if (('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ')) {
          // Append(139,11,Some(139))
          append(11);
          pendingFinish = 139;
          if (verbose) printStack();
          return true;
        }
        if ((c == ',')) {
          // Append(47,138,None)
          replace(47);
          append(138);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        return false;
      case 140:
        if (('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ')) {
          // Append(140,34,Some(141))
          append(34);
          pendingFinish = 141;
          if (verbose) printStack();
          return true;
        }
        if ((c == '"')) {
          // Append(151,2,None)
          replace(151);
          append(2);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == ',')) {
          // Append(47,138,None)
          replace(47);
          append(138);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        return false;
      case 141:
        if (('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ')) {
          // Append(141,34,Some(141))
          append(34);
          pendingFinish = 141;
          if (verbose) printStack();
          return true;
        }
        if ((c == '"')) {
          // Append(47,2,None)
          replace(47);
          append(2);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == ',')) {
          // Append(47,138,None)
          replace(47);
          append(138);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        return false;
      case 142:
        if (('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ')) {
          // Append(151,34,None)
          replace(151);
          append(34);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == '"')) {
          // Append(142,2,None)
          append(2);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == '-')) {
          // Append(116,6,None)
          replace(116);
          append(6);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == '0')) {
          // Append(116,14,Some(116))
          replace(116);
          append(14);
          pendingFinish = 116;
          if (verbose) printStack();
          return true;
        }
        if (('1' <= c && c <= '9')) {
          // Append(116,13,Some(116))
          replace(116);
          append(13);
          pendingFinish = 116;
          if (verbose) printStack();
          return true;
        }
        if ((c == '[')) {
          // Append(116,3,None)
          replace(116);
          append(3);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == 'f')) {
          // Append(116,10,None)
          replace(116);
          append(10);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == 'n')) {
          // Append(116,7,None)
          replace(116);
          append(7);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == 't')) {
          // Append(116,5,None)
          replace(116);
          append(5);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == '{')) {
          // Append(116,9,None)
          replace(116);
          append(9);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        return false;
      case 143:
        if (('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ')) {
          // Append(143,11,Some(143))
          append(11);
          pendingFinish = 143;
          if (verbose) printStack();
          return true;
        }
        if ((c == '"')) {
          // Append(47,2,None)
          replace(47);
          append(2);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == ',')) {
          // Append(47,100,None)
          replace(47);
          append(100);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == '-')) {
          // Append(47,6,None)
          replace(47);
          append(6);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == '0')) {
          // Append(47,31,None)
          replace(47);
          append(31);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if (('1' <= c && c <= '9')) {
          // Append(47,29,None)
          replace(47);
          append(29);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == '[')) {
          // Append(47,3,None)
          replace(47);
          append(3);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == ']')) {
          // Append(47,75,None)
          replace(47);
          append(75);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == 'f')) {
          // Append(47,10,None)
          replace(47);
          append(10);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == 'n')) {
          // Append(47,7,None)
          replace(47);
          append(7);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == 't')) {
          // Append(47,5,None)
          replace(47);
          append(5);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == '{')) {
          // Append(47,9,None)
          replace(47);
          append(9);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        return false;
      case 144:
        if (('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ')) {
          // Append(144,34,Some(145))
          append(34);
          pendingFinish = 145;
          if (verbose) printStack();
          return true;
        }
        if ((c == '"')) {
          // Append(151,2,None)
          replace(151);
          append(2);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == ',')) {
          // Append(47,138,None)
          replace(47);
          append(138);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == '-')) {
          // Append(47,6,None)
          replace(47);
          append(6);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == '0')) {
          // Append(47,153,None)
          replace(47);
          append(153);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if (('1' <= c && c <= '9')) {
          // Append(47,152,None)
          replace(47);
          append(152);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == '[')) {
          // Append(47,3,None)
          replace(47);
          append(3);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == 'f')) {
          // Append(47,10,None)
          replace(47);
          append(10);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == 'n')) {
          // Append(47,7,None)
          replace(47);
          append(7);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == 't')) {
          // Append(47,5,None)
          replace(47);
          append(5);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == '{')) {
          // Append(47,9,None)
          replace(47);
          append(9);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        return false;
      case 145:
        if (('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ')) {
          // Append(145,34,Some(145))
          append(34);
          pendingFinish = 145;
          if (verbose) printStack();
          return true;
        }
        if ((c == '"')) {
          // Append(47,2,None)
          replace(47);
          append(2);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == ',')) {
          // Append(47,138,None)
          replace(47);
          append(138);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == '-')) {
          // Append(47,6,None)
          replace(47);
          append(6);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == '0')) {
          // Append(47,153,None)
          replace(47);
          append(153);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if (('1' <= c && c <= '9')) {
          // Append(47,152,None)
          replace(47);
          append(152);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == '[')) {
          // Append(47,3,None)
          replace(47);
          append(3);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == 'f')) {
          // Append(47,10,None)
          replace(47);
          append(10);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == 'n')) {
          // Append(47,7,None)
          replace(47);
          append(7);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == 't')) {
          // Append(47,5,None)
          replace(47);
          append(5);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == '{')) {
          // Append(47,9,None)
          replace(47);
          append(9);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        return false;
      case 146:
        if (('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ')) {
          // Append(95,11,Some(95))
          replace(95);
          append(11);
          pendingFinish = 95;
          if (verbose) printStack();
          return true;
        }
        if ((c == '"')) {
          // Append(96,2,None)
          replace(96);
          append(2);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == ',')) {
          // Finish(61)
          replace(61);
          if (verbose) printStack();
          finish();
          if (verbose) printStack();
          return true;
        }
        if ((c == '-')) {
          // Append(96,6,None)
          replace(96);
          append(6);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == '0')) {
          // Append(96,31,Some(96))
          replace(96);
          append(31);
          pendingFinish = 96;
          if (verbose) printStack();
          return true;
        }
        if (('1' <= c && c <= '9')) {
          // Append(96,29,Some(96))
          replace(96);
          append(29);
          pendingFinish = 96;
          if (verbose) printStack();
          return true;
        }
        if ((c == '[')) {
          // Append(96,3,None)
          replace(96);
          append(3);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == ']')) {
          // Finish(94)
          replace(94);
          if (verbose) printStack();
          finish();
          if (verbose) printStack();
          return true;
        }
        if ((c == 'f')) {
          // Append(96,10,None)
          replace(96);
          append(10);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == 'n')) {
          // Append(96,7,None)
          replace(96);
          append(7);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == 't')) {
          // Append(96,5,None)
          replace(96);
          append(5);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == '{')) {
          // Append(96,9,None)
          replace(96);
          append(9);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        return false;
      case 147:
        if (('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ')) {
          // Append(140,34,Some(141))
          replace(140);
          append(34);
          pendingFinish = 141;
          if (verbose) printStack();
          return true;
        }
        if ((c == '"')) {
          // Append(151,2,None)
          replace(151);
          append(2);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == ',')) {
          // Finish(128)
          replace(128);
          if (verbose) printStack();
          finish();
          if (verbose) printStack();
          return true;
        }
        return false;
      case 148:
        if (('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ')) {
          // Append(138,34,Some(154))
          replace(138);
          append(34);
          pendingFinish = 154;
          if (verbose) printStack();
          return true;
        }
        if ((c == '"')) {
          // Append(151,2,None)
          replace(151);
          append(2);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == ',')) {
          // Finish(128)
          replace(128);
          if (verbose) printStack();
          finish();
          if (verbose) printStack();
          return true;
        }
        return false;
      case 149:
        if ((c == ',')) {
          // Finish(61)
          replace(61);
          if (verbose) printStack();
          finish();
          if (verbose) printStack();
          return true;
        }
        if ((c == ']')) {
          // Finish(94)
          replace(94);
          if (verbose) printStack();
          finish();
          if (verbose) printStack();
          return true;
        }
        return false;
      case 150:
        if ((c == '"')) {
          // Append(116,2,None)
          replace(116);
          append(2);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == ',')) {
          // Finish(128)
          replace(128);
          if (verbose) printStack();
          finish();
          if (verbose) printStack();
          return true;
        }
        if ((c == '-')) {
          // Append(116,6,None)
          replace(116);
          append(6);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == '0')) {
          // Append(116,14,Some(116))
          replace(116);
          append(14);
          pendingFinish = 116;
          if (verbose) printStack();
          return true;
        }
        if (('1' <= c && c <= '9')) {
          // Append(116,13,Some(116))
          replace(116);
          append(13);
          pendingFinish = 116;
          if (verbose) printStack();
          return true;
        }
        if ((c == '[')) {
          // Append(116,3,None)
          replace(116);
          append(3);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == 'f')) {
          // Append(116,10,None)
          replace(116);
          append(10);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == 'n')) {
          // Append(116,7,None)
          replace(116);
          append(7);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == 't')) {
          // Append(116,5,None)
          replace(116);
          append(5);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == '{')) {
          // Append(116,9,None)
          replace(116);
          append(9);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        return false;
      case 151:
        if (('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ')) {
          // Append(151,34,None)
          append(34);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == '"')) {
          // Append(151,2,None)
          append(2);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        return false;
      case 152:
        if (('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ')) {
          // Append(139,11,Some(139))
          replace(139);
          append(11);
          pendingFinish = 139;
          if (verbose) printStack();
          return true;
        }
        if ((c == ',')) {
          // Finish(128)
          replace(128);
          if (verbose) printStack();
          finish();
          if (verbose) printStack();
          return true;
        }
        if ((c == '.')) {
          // Append(16,17,None)
          replace(16);
          append(17);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == '0')) {
          // Append(25,26,Some(25))
          replace(25);
          append(26);
          pendingFinish = 25;
          if (verbose) printStack();
          return true;
        }
        if (('1' <= c && c <= '9')) {
          // Append(25,26,Some(25))
          replace(25);
          append(26);
          pendingFinish = 25;
          if (verbose) printStack();
          return true;
        }
        if ((c == 'E')) {
          // Append(18,20,None)
          replace(18);
          append(20);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == 'e')) {
          // Append(18,19,None)
          replace(18);
          append(19);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        return false;
      case 153:
        if (('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ')) {
          // Append(139,11,Some(139))
          replace(139);
          append(11);
          pendingFinish = 139;
          if (verbose) printStack();
          return true;
        }
        if ((c == ',')) {
          // Finish(128)
          replace(128);
          if (verbose) printStack();
          finish();
          if (verbose) printStack();
          return true;
        }
        if ((c == '.')) {
          // Append(16,17,None)
          replace(16);
          append(17);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == 'E')) {
          // Append(18,20,None)
          replace(18);
          append(20);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == 'e')) {
          // Append(18,19,None)
          replace(18);
          append(19);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        return false;
      case 154:
        if (('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ')) {
          // Append(154,34,Some(154))
          append(34);
          pendingFinish = 154;
          if (verbose) printStack();
          return true;
        }
        if ((c == '"')) {
          // Append(47,2,None)
          replace(47);
          append(2);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        return false;
      case 155:
        if ((c == '"')) {
          // Append(96,2,None)
          replace(96);
          append(2);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == ',')) {
          // Finish(61)
          replace(61);
          if (verbose) printStack();
          finish();
          if (verbose) printStack();
          return true;
        }
        if ((c == '-')) {
          // Append(96,6,None)
          replace(96);
          append(6);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == '0')) {
          // Append(96,31,Some(96))
          replace(96);
          append(31);
          pendingFinish = 96;
          if (verbose) printStack();
          return true;
        }
        if (('1' <= c && c <= '9')) {
          // Append(96,29,Some(96))
          replace(96);
          append(29);
          pendingFinish = 96;
          if (verbose) printStack();
          return true;
        }
        if ((c == '[')) {
          // Append(96,3,None)
          replace(96);
          append(3);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == ']')) {
          // Finish(94)
          replace(94);
          if (verbose) printStack();
          finish();
          if (verbose) printStack();
          return true;
        }
        if ((c == 'f')) {
          // Append(96,10,None)
          replace(96);
          append(10);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == 'n')) {
          // Append(96,7,None)
          replace(96);
          append(7);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == 't')) {
          // Append(96,5,None)
          replace(96);
          append(5);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == '{')) {
          // Append(96,9,None)
          replace(96);
          append(9);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        return false;
      case 156:
        if (('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ')) {
          // Append(151,34,None)
          replace(151);
          append(34);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == '"')) {
          // Append(151,2,None)
          replace(151);
          append(2);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == ',')) {
          // Finish(128)
          replace(128);
          if (verbose) printStack();
          finish();
          if (verbose) printStack();
          return true;
        }
        return false;
      case 157:
        if (('\t' <= c && c <= '\n') || (c == '\r') || (c == ' ')) {
          // Append(151,34,None)
          replace(151);
          append(34);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == '"')) {
          // Append(142,2,None)
          replace(142);
          append(2);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == ',')) {
          // Finish(128)
          replace(128);
          if (verbose) printStack();
          finish();
          if (verbose) printStack();
          return true;
        }
        if ((c == '-')) {
          // Append(116,6,None)
          replace(116);
          append(6);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == '0')) {
          // Append(116,14,Some(116))
          replace(116);
          append(14);
          pendingFinish = 116;
          if (verbose) printStack();
          return true;
        }
        if (('1' <= c && c <= '9')) {
          // Append(116,13,Some(116))
          replace(116);
          append(13);
          pendingFinish = 116;
          if (verbose) printStack();
          return true;
        }
        if ((c == '[')) {
          // Append(116,3,None)
          replace(116);
          append(3);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == 'f')) {
          // Append(116,10,None)
          replace(116);
          append(10);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == 'n')) {
          // Append(116,7,None)
          replace(116);
          append(7);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == 't')) {
          // Append(116,5,None)
          replace(116);
          append(5);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        if ((c == '{')) {
          // Append(116,9,None)
          replace(116);
          append(9);
          pendingFinish = -1;
          if (verbose) printStack();
          return true;
        }
        return false;
    }
    throw new AssertionError("Unknown nodeId: " + stack.nodeId);
  }

  public boolean proceedEof() {
    if (stack == null) {
      if (verbose) {
        log("  - already finished");
        return true;
      }
    }
    if (pendingFinish == -1) {
      if (stack.prev == null && stack.nodeId == 0) {
        return true;
      }
      if (verbose) {
        log("  - pendingFinish unavailable, proceedEof failed");
      }
      return false;
    }
    dropLast();
    if (stack.nodeId != pendingFinish) {
      replace(pendingFinish);
    }
    if (verbose) printStack();
    while (stack.prev != null) {
      boolean finishNeeded = finishStep();
      if (verbose) printStack();
      if (!finishNeeded) {
        if (pendingFinish == -1) {
          return false;
        }
        dropLast();
        replace(pendingFinish);
        if (verbose) {
          printStack();
        }
      }
    }
    return true;
  }

  public static boolean parse(String s) {
    JsonParser parser = new JsonParser(false);
    for (int i = 0; i < s.length(); i++) {
      if (!parser.proceed(s.charAt(i))) {
        return false;
      }
    }
    return parser.proceedEof();
  }

  public static boolean parseVerbose(String s) {
    JsonParser parser = new JsonParser(true);
    for (int i = 0; i < s.length(); i++) {
      log("Proceed char at " + i + ": " + s.charAt(i));
      if (!parser.proceed(s.charAt(i))) {
        return false;
      }
    }
    log("Proceed EOF");
    return parser.proceedEof();
  }

  public static void main(String[] args) {
    boolean succeed = parseVerbose("{\"abcd\": [\"hello\", 123, {\"xyz\": 1}]}");
    log("Parsing " + (succeed ? "succeeded" : "failed"));
  }
}
