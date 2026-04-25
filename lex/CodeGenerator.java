import java.util.*;

public class CodeGenerator {
    public String generateC(List<DfaState> states, List<SeuLexParser.LexRule> rules, String definitions, String userCode) {
        StringBuilder sb = new StringBuilder();
        
        // 1. Definition block %{ ... %}
        if (definitions != null && definitions.contains("%{") && definitions.contains("%}")) {
            int start = definitions.indexOf("%{") + 2;
            int end = definitions.indexOf("%}");
            sb.append(definitions.substring(start, end));
        }
        
        sb.append("\n#include <stdio.h>\n");
        sb.append("void comment(void);\n");
        sb.append("int check_type(void);\n");
        sb.append("#include <stdlib.h>\n");
        sb.append("#include <string.h>\n");
        sb.append("extern FILE *yyin;\n");
        sb.append("#define ECHO printf(\"%s\", yytext)\n");
        sb.append("char yytext[4096];\n");
        sb.append("int yyleng = 0;\n\n");
        
        // 2. Transition table
        sb.append("int transition_table[").append(states.size()).append("][256] = {\n");
        for (int i = 0; i < states.size(); i++) {
            DfaState state = states.get(i);
            sb.append("  {");
            for (int c = 0; c < 256; c++) {
                char ch = (char) c;
                if (state.transitions.containsKey(ch)) {
                    sb.append(state.transitions.get(ch).id);
                } else {
                    sb.append("-1");
                }
                if (c < 255) sb.append(", ");
            }
            sb.append("}");
            if (i < states.size() - 1) sb.append(",\n");
            else sb.append("\n");
        }
        sb.append("};\n\n");
        
        // 3. Accept rule table
        sb.append("int accept_rule[").append(states.size()).append("] = {\n");
        for (int i = 0; i < states.size(); i++) {
            DfaState state = states.get(i);
            if (state.isAccept) {
                sb.append(state.acceptedRuleId);
            } else {
                sb.append("-1");
            }
            if (i < states.size() - 1) sb.append(", ");
        }
        sb.append("\n};\n\n");
        
        // 4. yylex function
        sb.append("FILE *yyin = NULL;\n");
        sb.append("int input() {\n");
        sb.append("    if (!yyin) yyin = stdin;\n");
        sb.append("    int c = fgetc(yyin);\n");
        sb.append("    return c;\n");
        sb.append("}\n\n");
        
        sb.append("int yylex() {\n");
        sb.append("    int state = 0;\n");
        sb.append("    int last_accept_state = -1;\n");
        sb.append("    int last_accept_len = 0;\n");
        sb.append("    yyleng = 0;\n");
        sb.append("    int c;\n");
        
        sb.append("    while ((c = input()) != EOF && c != 0) {\n");
        sb.append("        if (c < 0 || c >= 256) break;\n");
        sb.append("        yytext[yyleng++] = c;\n");
        sb.append("        yytext[yyleng] = '\\0';\n");
        sb.append("        int next_state = transition_table[state][c];\n");
        sb.append("        if (next_state == -1) {\n");
        sb.append("            break;\n");
        sb.append("        }\n");
        sb.append("        state = next_state;\n");
        sb.append("        if (accept_rule[state] != -1) {\n");
        sb.append("            last_accept_state = state;\n");
        sb.append("            last_accept_len = yyleng;\n");
        sb.append("        }\n");
        sb.append("    }\n");
        
        sb.append("    if (last_accept_state != -1) {\n");
        sb.append("        for (int i = yyleng - 1; i >= last_accept_len; i--) {\n");
        sb.append("            ungetc(yytext[i], yyin);\n");
        sb.append("        }\n");
        sb.append("        yyleng = last_accept_len;\n");
        sb.append("        yytext[yyleng] = '\\0';\n");
        
        sb.append("        switch (accept_rule[last_accept_state]) {\n");
        for (SeuLexParser.LexRule rule : rules) {
            sb.append("            case ").append(rule.id).append(":\n");
            sb.append("                ").append(rule.action).append("\n");
            sb.append("                break;\n");
        }
        sb.append("        }\n");
        sb.append("    } else if (c == EOF || c == 0) {\n");
        sb.append("        return 0;\n");
        sb.append("    }\n");
        sb.append("    return -1;\n");
        sb.append("}\n\n");
        
        // 5. User Code
        if (userCode != null) {
            sb.append(userCode).append("\n");
        }
        
        return sb.toString();
    }
}
