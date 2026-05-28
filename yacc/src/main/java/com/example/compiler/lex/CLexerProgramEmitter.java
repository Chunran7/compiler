package com.example.compiler.lex;

import com.example.compiler.yacc.token.TokenType;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class CLexerProgramEmitter {
    private static final Pattern TOKEN_RETURN =
            Pattern.compile("return\\s+new Token\\(TokenType\\.([A-Z_][A-Z_0-9]*)");
    private static final Pattern CHAR_RETURN =
            Pattern.compile("return\\s+new Token\\(TokenType\\.forChar\\('(.)'\\)");

    public String emit(List<DfaState> states, List<SeuLexParser.LexRule> rules) {
        StringBuilder sb = new StringBuilder();
        sb.append("#include <stdio.h>\n");
        sb.append("#include <stdlib.h>\n");
        sb.append("#include <string.h>\n\n");
        sb.append("static FILE* yyin;\n");
        sb.append("static FILE* yyout;\n");
        sb.append("static char yytext[4096];\n");
        sb.append("static int yyleng = 0;\n");
        sb.append("static int column = 0;\n\n");

        emitTables(sb, states);
        emitRuntime(sb);
        emitNextToken(sb, rules);
        emitMain(sb);
        return sb.toString();
    }

    private void emitTables(StringBuilder sb, List<DfaState> states) {
        sb.append("static const int transition_table[").append(states.size()).append("][256] = {\n");
        for (int i = 0; i < states.size(); i++) {
            DfaState state = states.get(i);
            sb.append("    {");
            for (int c = 0; c < 256; c++) {
                if (c > 0) {
                    sb.append(", ");
                }
                DfaState next = state.transitions.get((char) c);
                sb.append(next == null ? -1 : next.id);
            }
            sb.append("}");
            if (i + 1 < states.size()) {
                sb.append(",");
            }
            sb.append("\n");
        }
        sb.append("};\n\n");

        sb.append("static const int accept_rule[").append(states.size()).append("] = {");
        for (int i = 0; i < states.size(); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            DfaState state = states.get(i);
            sb.append(state.isAccept ? state.acceptedRuleId : -1);
        }
        sb.append("};\n\n");
    }

    private void emitRuntime(StringBuilder sb) {
        sb.append("""
static char* dup_text(const char* text) {
    size_t len = strlen(text);
    char* copy = (char*)malloc(len + 1);
    if (!copy) {
        fprintf(stderr, "out of memory\\n");
        exit(2);
    }
    memcpy(copy, text, len + 1);
    return copy;
}

static int input_char(void) {
    return fgetc(yyin);
}

static void ungetc_char(int c) {
    if (c != EOF) {
        ungetc(c, yyin);
    }
}

static void count_text(void) {
    for (int i = 0; i < yyleng; i++) {
        if (yytext[i] == '\\n') {
            column = 0;
        } else if (yytext[i] == '\\t') {
            column += 8 - (column % 8);
        } else {
            column++;
        }
    }
}

static void lexer_error(const char* msg) {
    fprintf(stderr, "lexer error: %s\\n", msg);
    exit(1);
}

static void comment(void) {
    int c;
    int prev = 0;
    while ((c = input_char()) != EOF) {
        if (c == '/' && prev == '*') {
            return;
        }
        prev = c;
    }
    lexer_error("unterminated comment");
}

static void skip_line_comment(void) {
    int c;
    while ((c = input_char()) != EOF) {
        if (c == '\\n') {
            return;
        }
    }
}

static const char* check_type(void) {
    return "IDENTIFIER";
}

static void write_escaped(FILE* out, const char* text) {
    for (const unsigned char* p = (const unsigned char*)text; *p; ++p) {
        switch (*p) {
            case '\\\\': fputs("\\\\\\\\", out); break;
            case '\\n': fputs("\\\\n", out); break;
            case '\\r': fputs("\\\\r", out); break;
            case '\\t': fputs("\\\\t", out); break;
            default: fputc(*p, out); break;
        }
    }
}

static void write_token(const char* type_name) {
    fputs(type_name, yyout);
    fputc('\\t', yyout);
    yytext[yyleng] = '\\0';
    write_escaped(yyout, yytext);
    fputc('\\n', yyout);
}

""");
    }

    private void emitNextToken(StringBuilder sb, List<SeuLexParser.LexRule> rules) {
        sb.append("static int next_token(void) {\n");
        sb.append("    int first = input_char();\n");
        sb.append("    if (first == '/') {\n");
        sb.append("        int second = input_char();\n");
        sb.append("        if (second == '*') { comment(); return next_token(); }\n");
        sb.append("        if (second == '/') { skip_line_comment(); return next_token(); }\n");
        sb.append("        if (second != EOF) ungetc_char(second);\n");
        sb.append("        ungetc_char(first);\n");
        sb.append("    } else if (first != EOF) {\n");
        sb.append("        ungetc_char(first);\n");
        sb.append("    }\n");
        sb.append("    int state = 0;\n");
        sb.append("    int last_accept_state = -1;\n");
        sb.append("    int last_accept_len = 0;\n");
        sb.append("    yyleng = 0;\n");
        sb.append("    int c;\n");
        sb.append("    while ((c = input_char()) != EOF && c != 0) {\n");
        sb.append("        if (c < 0 || c >= 256) break;\n");
        sb.append("        yytext[yyleng++] = (char)c;\n");
        sb.append("        int next_state = transition_table[state][c];\n");
        sb.append("        if (next_state == -1) break;\n");
        sb.append("        state = next_state;\n");
        sb.append("        if (accept_rule[state] != -1) {\n");
        sb.append("            last_accept_state = state;\n");
        sb.append("            last_accept_len = yyleng;\n");
        sb.append("        }\n");
        sb.append("    }\n");
        sb.append("    if (last_accept_state != -1) {\n");
        sb.append("        for (int i = yyleng - 1; i >= last_accept_len; i--) ungetc_char((unsigned char)yytext[i]);\n");
        sb.append("        yyleng = last_accept_len;\n");
        sb.append("        yytext[yyleng] = '\\0';\n");
        sb.append("        switch (accept_rule[last_accept_state]) {\n");
        for (SeuLexParser.LexRule rule : rules) {
            sb.append("            case ").append(rule.id).append(":\n");
            emitRuleCase(sb, rule);
        }
        sb.append("            default: return next_token();\n");
        sb.append("        }\n");
        sb.append("    }\n");
        sb.append("    if (c == EOF || c == 0) {\n");
        sb.append("        if (yyleng == 0) {\n");
        sb.append("            fputs(\"EOF\\tEOF\\n\", yyout);\n");
        sb.append("            return 0;\n");
        sb.append("        }\n");
        sb.append("        lexer_error(\"unexpected end of file\");\n");
        sb.append("    }\n");
        sb.append("    lexer_error(\"unexpected character\");\n");
        sb.append("    return -1;\n");
        sb.append("}\n\n");
    }

    private void emitRuleCase(StringBuilder sb, SeuLexParser.LexRule rule) {
        String action = rule.action == null ? "" : rule.action;
        String tokenName = tokenNameForAction(action);
        if (action.contains("count()")) {
            sb.append("                count_text();\n");
        }
        if (action.contains("comment()")) {
            sb.append("                comment();\n");
            sb.append("                return next_token();\n");
            return;
        }
        if (tokenName == null) {
            sb.append("                return next_token();\n");
            return;
        }
        if ("CHECK_TYPE".equals(tokenName)) {
            sb.append("                write_token(check_type());\n");
        } else {
            sb.append("                write_token(\"").append(tokenName).append("\");\n");
        }
        sb.append("                return 1;\n");
    }

    private String tokenNameForAction(String action) {
        Matcher tokenMatcher = TOKEN_RETURN.matcher(action);
        if (tokenMatcher.find()) {
            return tokenMatcher.group(1);
        }
        Matcher charMatcher = CHAR_RETURN.matcher(action);
        if (charMatcher.find()) {
            return charToTokenName(charMatcher.group(1).charAt(0));
        }
        if (action.contains("check_type()")) {
            return "CHECK_TYPE";
        }
        return null;
    }

    private String charToTokenName(char ch) {
        return TokenType.forChar(ch).name();
    }

    private void emitMain(StringBuilder sb) {
        sb.append("""
int main(int argc, char** argv) {
    if (argc != 3) {
        fprintf(stderr, "usage: %s <source.c> <tokens.txt>\\n", argv[0]);
        return 1;
    }
    yyin = fopen(argv[1], "rb");
    if (!yyin) {
        perror("open source");
        return 1;
    }
    yyout = fopen(argv[2], "wb");
    if (!yyout) {
        perror("open token output");
        fclose(yyin);
        return 1;
    }
    while (next_token() > 0) {
    }
    fclose(yyout);
    fclose(yyin);
    return 0;
}
""");
    }
}
