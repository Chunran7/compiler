package com.example.compiler.lex;

import java.io.*;
import java.util.*;

public class CodeGenerator {
    public String generateJava(List<DfaState> states, List<SeuLexParser.LexRule> rules, String definitions, String userCode) {
        StringBuilder sb = new StringBuilder();
        String defClassBody = "";

        sb.append("package com.example.compiler.lex;\n\n");
        sb.append("import java.io.*;\n");
        sb.append("import com.example.compiler.yacc.token.*;\n");

        // 1. Definition block — 使用 C→Java 翻译器处理 %{...%} 块
        if (definitions != null && definitions.contains("%{") && definitions.contains("%}")) {
            int start = definitions.indexOf("%{") + 2;
            int end = definitions.indexOf("%}");
            String defBlock = definitions.substring(start, end);
            CToJavaTranslator translator = new CToJavaTranslator();
            String[] defParts = translator.translateDefinitionBlock(defBlock);
            // defParts[0] = import/package 行（类体前）
            if (!defParts[0].isEmpty()) {
                sb.append(defParts[0]);
            }
            // defParts[1] = 类体内代码
            defClassBody = defParts[1];
        }

        sb.append("\npublic class GeneratedLexer {\n");

        // 发出 %{...%} 块中翻译后的类体内代码
        if (!defClassBody.isEmpty()) {
            sb.append(defClassBody);
        }

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
        
        // 6. 用户子程序段 — 使用 C→Java 翻译器处理 .l 文件的尾部 C 代码
        if (userCode != null && !userCode.isBlank()) {
            CToJavaTranslator translator = new CToJavaTranslator();
            sb.append(translator.translateUserSubroutines(userCode));
        }

        sb.append("}\n");
        
        return sb.toString();
    }
}
