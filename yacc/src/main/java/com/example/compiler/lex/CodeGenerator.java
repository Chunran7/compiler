package com.example.compiler.lex;

import java.io.*;
import java.util.*;

public class CodeGenerator {
    public String generateJava(List<DfaState> states, List<SeuLexParser.LexRule> rules, String definitions, String userCode) {
        StringBuilder sb = new StringBuilder();
        
        sb.append("package com.example.compiler.lex;\n\n");
        sb.append("import java.io.*;\n");
        sb.append("import com.example.compiler.yacc.token.*;\n");
        
        // 1. Definition block — 跳过 C 的 #include, 由 Generator 自行导入所需类型
        if (definitions != null && definitions.contains("%{") && definitions.contains("%}")) {
            int start = definitions.indexOf("%{") + 2;
            int end = definitions.indexOf("%}");
            String defBlock = definitions.substring(start, end);
            // 仅保留 Java import 行，过滤 C 的 #include / 声明
            for (String line : defBlock.split("\n")) {
                String trimmed = line.trim();
                if (trimmed.startsWith("import ") || trimmed.startsWith("package ")) {
                    sb.append(trimmed).append("\n");
                }
            }
        }
        
        sb.append("\npublic class GeneratedLexer {\n");
        sb.append("    private PushbackReader yyin;\n");
        sb.append("    public char[] yytext = new char[4096];\n");
        sb.append("    public int yyleng = 0;\n\n");
        
        sb.append("    public GeneratedLexer(Reader in) {\n");
        sb.append("        this.yyin = new PushbackReader(in, 4096);\n");
        sb.append("    }\n\n");

        sb.append("    public GeneratedLexer(InputStream in) {\n");
        sb.append("        this.yyin = new PushbackReader(new InputStreamReader(in), 4096);\n");
        sb.append("    }\n\n");
        
        // 2. Transition table and other tables
        try (DataOutputStream dos = new DataOutputStream(new FileOutputStream("src/main/resources/lexer_tables.dat"))) {
            dos.writeInt(states.size());
            dos.writeInt(256);
            for (int i = 0; i < states.size(); i++) {
                DfaState state = states.get(i);
                for (int c = 0; c < 256; c++) {
                    char ch = (char) c;
                    if (state.transitions.containsKey(ch)) {
                        dos.writeInt(state.transitions.get(ch).id);
                    } else {
                        dos.writeInt(-1);
                    }
                }
            }
            for (int i = 0; i < states.size(); i++) {
                DfaState state = states.get(i);
                dos.writeInt(state.isAccept ? state.acceptedRuleId : -1);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to write lexer_tables.dat", e);
        }
        
        sb.append("    private static final int[][] transition_table;\n");
        sb.append("    private static final int[] accept_rule;\n");
        sb.append("    static {\n");
        sb.append("        try (DataInputStream dis = new DataInputStream(GeneratedLexer.class.getResourceAsStream(\"/lexer_tables.dat\") != null ? GeneratedLexer.class.getResourceAsStream(\"/lexer_tables.dat\") : new FileInputStream(\"src/main/resources/lexer_tables.dat\"))) {\n");
        sb.append("            int r = dis.readInt();\n");
        sb.append("            int c = dis.readInt();\n");
        sb.append("            transition_table = new int[r][c];\n");
        sb.append("            for (int i = 0; i < r; i++) {\n");
        sb.append("                for (int j = 0; j < c; j++) {\n");
        sb.append("                    transition_table[i][j] = dis.readInt();\n");
        sb.append("                }\n");
        sb.append("            }\n");
        sb.append("            accept_rule = new int[r];\n");
        sb.append("            for(int i = 0; i < r; i++) accept_rule[i] = dis.readInt();\n");
        sb.append("        } catch (Exception e) {\n");
        sb.append("            throw new RuntimeException(\"Failed to load lexer tables\", e);\n");
        sb.append("        }\n");
        sb.append("    }\n\n");
        
        // 4. Input functions
        sb.append("    private int input() {\n");
        sb.append("        try {\n");
        sb.append("            return yyin.read();\n");
        sb.append("        } catch (IOException e) {\n");
        sb.append("            return -1;\n");
        sb.append("        }\n");
        sb.append("    }\n\n");

        sb.append("    private void ungetc(int c) {\n");
        sb.append("        try { yyin.unread(c); } catch (IOException e) { throw new RuntimeException(e); }\n");
        sb.append("    }\n\n");
        
        // 5. nextToken
        sb.append("    public Token nextToken() {\n");
        sb.append("        int first = input();\n");
        sb.append("        if (first == '/') {\n");
        sb.append("            int second = input();\n");
        sb.append("            if (second == '*') {\n");
        sb.append("                comment();\n");
        sb.append("                return nextToken();\n");
        sb.append("            }\n");
        sb.append("            if (second == '/') {\n");
        sb.append("                skipLineComment();\n");
        sb.append("                return nextToken();\n");
        sb.append("            }\n");
        sb.append("            if (second != -1) {\n");
        sb.append("                ungetc(second);\n");
        sb.append("            }\n");
        sb.append("            ungetc(first);\n");
        sb.append("        } else if (first != -1) {\n");
        sb.append("            ungetc(first);\n");
        sb.append("        }\n\n");
        sb.append("        int state = 0;\n");
        sb.append("        int last_accept_state = -1;\n");
        sb.append("        int last_accept_len = 0;\n");
        sb.append("        yyleng = 0;\n");
        sb.append("        int c;\n");
        
        sb.append("        while ((c = input()) != -1 && c != 0) {\n");
        sb.append("            if (c < 0 || c >= 256) break;\n");
        sb.append("            yytext[yyleng++] = (char)c;\n");
        sb.append("            int next_state = transition_table[state][c];\n");
        sb.append("            if (next_state == -1) {\n");
        sb.append("                break;\n");
        sb.append("            }\n");
        sb.append("            state = next_state;\n");
        sb.append("            if (accept_rule[state] != -1) {\n");
        sb.append("                last_accept_state = state;\n");
        sb.append("                last_accept_len = yyleng;\n");
        sb.append("            }\n");
        sb.append("        }\n");
        
        sb.append("        if (last_accept_state != -1) {\n");
        sb.append("            for (int i = yyleng - 1; i >= last_accept_len; i--) {\n");
        sb.append("                ungetc(yytext[i]);\n");
        sb.append("            }\n");
        sb.append("            yyleng = last_accept_len;\n\n");
        
        sb.append("            switch (accept_rule[last_accept_state]) {\n");
        for (SeuLexParser.LexRule rule : rules) {
            sb.append("                case ").append(rule.id).append(":\n");
            sb.append("                    ").append(rule.action).append("\n");
            if (rule.action == null || (!rule.action.contains("return"))) {
                sb.append("                    break;\n");
            }
        }
        sb.append("            }\n");
        sb.append("            return nextToken();\n");
        sb.append("        } else if (c == -1 || c == 0) {\n");
        sb.append("            if (yyleng == 0) return new Token(TokenType.EOF, \"EOF\");\n");
        sb.append("            else throw new RuntimeException(\"Lexer error: unexpected end of file\");\n");
        sb.append("        }\n");
        sb.append("        throw new RuntimeException(\"Lexer error: unexpected character '\" + (char)c + \"'\");\n");
        sb.append("    }\n\n");
        
        // 6. Built-in Java 辅助方法（替代 c99.l 尾部 C 代码的用户子程序段）
        sb.append("    public int column = 0;\n\n");

        sb.append("    private void count() {\n");
        sb.append("        for (int i = 0; i < yyleng; i++) {\n");
        sb.append("            if (yytext[i] == '\\n') {\n");
        sb.append("                column = 0;\n");
        sb.append("            } else if (yytext[i] == '\\t') {\n");
        sb.append("                column += 8 - (column % 8);\n");
        sb.append("            } else {\n");
        sb.append("                column++;\n");
        sb.append("            }\n");
        sb.append("        }\n");
        sb.append("    }\n\n");

        sb.append("    private void comment() {\n");
        sb.append("        int c, prev = 0;\n");
        sb.append("        while ((c = input()) != -1 && c != 0) {\n");
        sb.append("            if (c == '/' && prev == '*') return;\n");
        sb.append("            prev = c;\n");
        sb.append("        }\n");
        sb.append("        error(\"unterminated comment\");\n");
        sb.append("    }\n\n");

        sb.append("    private void skipLineComment() {\n");
        sb.append("        int c;\n");
        sb.append("        while ((c = input()) != -1 && c != 0) {\n");
        sb.append("            if (c == '\\n') return;\n");
        sb.append("        }\n");
        sb.append("    }\n\n");

        sb.append("    private Token check_type() {\n");
        sb.append("        return new Token(TokenType.IDENTIFIER, new String(yytext, 0, yyleng));\n");
        sb.append("    }\n\n");

        sb.append("    private void error(String msg) {\n");
        sb.append("        throw new RuntimeException(\"Lexer error: \" + msg);\n");
        sb.append("    }\n\n");

        // 7. 用户自定义代码：已由上方 Java 方法替代，不再追加 C 代码
        // （c99.l 尾部的 count/comment/check_type/error 等 C 函数由内置 Java 实现替代）

        sb.append("}\n");
        
        return sb.toString();
    }
}
