package com.example.compiler.yacc.emitter;

import com.example.compiler.yacc.grammar.Grammar;
import com.example.compiler.yacc.grammar.NonTerminal;
import com.example.compiler.yacc.grammar.Production;
import com.example.compiler.yacc.table.Action;
import com.example.compiler.yacc.table.ActionType;
import com.example.compiler.yacc.table.ParseTable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public final class CParserProgramEmitter {
    public String emit(Grammar grammar, ParseTable parseTable) {
        return emit(grammar, parseTable, Map.of());
    }

    public String emit(Grammar grammar, ParseTable parseTable, Map<Integer, String> semanticActions) {
        StringBuilder sb = new StringBuilder();
        sb.append("#include <stdio.h>\n");
        sb.append("#include <stdlib.h>\n");
        sb.append("#include <string.h>\n\n");

        emitTypes(sb);
        emitProductionData(sb, grammar, semanticActions);
        emitParseTables(sb, parseTable);
        emitRuntime(sb);
        emitParseFunction(sb);
        emitMain(sb);
        return sb.toString();
    }

    private void emitTypes(StringBuilder sb) {
        sb.append("""
typedef struct Node {
    char* symbol;
    char* lexeme;
    int semantic_action;
    char* action_code;
    int production_id;
    int child_count;
    struct Node** children;
} Node;

typedef struct {
    char* type;
    char* lexeme;
} Token;

typedef struct {
    int state;
    const char* symbol;
    int kind;
    int value;
} ActionEntry;

typedef struct {
    int state;
    const char* symbol;
    int target;
} GotoEntry;

enum {
    ACTION_SHIFT = 0,
    ACTION_REDUCE = 1,
    ACTION_ACCEPT = 2
};

""");
    }

    private void emitProductionData(StringBuilder sb, Grammar grammar, Map<Integer, String> semanticActions) {
        List<Production> productions = grammar.getProductions();
        sb.append("static const char* PRODUCTION_LHS[] = {\n");
        for (int i = 0; i < productions.size(); i++) {
            sb.append("    \"").append(escape(productions.get(i).getLeft().getName())).append("\"");
            if (i + 1 < productions.size()) {
                sb.append(",");
            }
            sb.append("\n");
        }
        sb.append("};\n\n");

        sb.append("static const int PRODUCTION_RHS_LEN[] = {");
        for (int i = 0; i < productions.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append(productions.get(i).getRight().size());
        }
        sb.append("};\n\n");

        sb.append("static const int PRODUCTION_IS_ACTION[] = {");
        for (int i = 0; i < productions.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append(isSemanticActionProduction(productions.get(i)) ? 1 : 0);
        }
        sb.append("};\n\n");

        sb.append("static const char* PRODUCTION_ACTION_CODE[] = {\n");
        for (int i = 0; i < productions.size(); i++) {
            Production production = productions.get(i);
            String code = production.getActionCode() == null ? semanticActions.getOrDefault(production.getId(), "") : production.getActionCode();
            sb.append("    \"").append(escape(code)).append("\"");
            if (i + 1 < productions.size()) {
                sb.append(",");
            }
            sb.append("\n");
        }
        sb.append("};\n\n");

        sb.append("static const int PRODUCTION_HAS_MAPPED_ACTION[] = {");
        for (int i = 0; i < productions.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append(semanticActions.containsKey(productions.get(i).getId()) ? 1 : 0);
        }
        sb.append("};\n\n");
    }

    private void emitParseTables(StringBuilder sb, ParseTable parseTable) {
        List<String> actionRows = new ArrayList<>();
        for (Map.Entry<Integer, Map<com.example.compiler.yacc.grammar.Terminal, Action>> row : parseTable.actionRows().entrySet()) {
            for (Map.Entry<com.example.compiler.yacc.grammar.Terminal, Action> entry : row.getValue().entrySet()) {
                Action action = entry.getValue();
                int kind = switch (action.type()) {
                    case SHIFT -> 0;
                    case REDUCE -> 1;
                    case ACCEPT -> 2;
                };
                int value = action.type() == ActionType.SHIFT ? action.targetState()
                        : action.type() == ActionType.REDUCE ? action.productionId()
                        : 0;
                actionRows.add("    {" + row.getKey() + ", \"" + escape(entry.getKey().getName()) + "\", " + kind + ", " + value + "}");
            }
        }
        actionRows.sort(Comparator.naturalOrder());
        sb.append("static const ActionEntry ACTION_TABLE[] = {\n");
        for (int i = 0; i < actionRows.size(); i++) {
            sb.append(actionRows.get(i));
            if (i + 1 < actionRows.size()) {
                sb.append(",");
            }
            sb.append("\n");
        }
        sb.append("};\n");
        sb.append("static const int ACTION_TABLE_COUNT = ").append(actionRows.size()).append(";\n\n");

        List<String> gotoRows = new ArrayList<>();
        for (Map.Entry<Integer, Map<NonTerminal, Integer>> row : parseTable.gotoRows().entrySet()) {
            for (Map.Entry<NonTerminal, Integer> entry : row.getValue().entrySet()) {
                gotoRows.add("    {" + row.getKey() + ", \"" + escape(entry.getKey().getName()) + "\", " + entry.getValue() + "}");
            }
        }
        gotoRows.sort(Comparator.naturalOrder());
        sb.append("static const GotoEntry GOTO_TABLE[] = {\n");
        for (int i = 0; i < gotoRows.size(); i++) {
            sb.append(gotoRows.get(i));
            if (i + 1 < gotoRows.size()) {
                sb.append(",");
            }
            sb.append("\n");
        }
        sb.append("};\n");
        sb.append("static const int GOTO_TABLE_COUNT = ").append(gotoRows.size()).append(";\n\n");
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

static char* unescape_text(const char* text) {
    size_t len = strlen(text);
    char* out = (char*)malloc(len + 1);
    if (!out) {
        fprintf(stderr, "out of memory\\n");
        exit(2);
    }
    size_t j = 0;
    for (size_t i = 0; i < len; i++) {
        if (text[i] != '\\\\') {
            out[j++] = text[i];
            continue;
        }
        i++;
        if (i >= len) {
            out[j++] = '\\\\';
            break;
        }
        switch (text[i]) {
            case 'n': out[j++] = '\\n'; break;
            case 'r': out[j++] = '\\r'; break;
            case 't': out[j++] = '\\t'; break;
            case '\\\\': out[j++] = '\\\\'; break;
            default: out[j++] = text[i]; break;
        }
    }
    out[j] = '\\0';
    return out;
}

static void write_escaped(FILE* out, const char* text) {
    if (!text) {
        return;
    }
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

static Node* make_leaf(const char* symbol, const char* lexeme) {
    Node* node = (Node*)calloc(1, sizeof(Node));
    node->symbol = dup_text(symbol);
    node->lexeme = dup_text(lexeme);
    node->production_id = -1;
    return node;
}

static Node* make_nonterminal(const char* symbol, int production_id, int child_count, Node** children) {
    Node* node = (Node*)calloc(1, sizeof(Node));
    node->symbol = dup_text(symbol);
    node->production_id = production_id;
    node->child_count = child_count;
    node->children = children;
    return node;
}

static Node* make_action_node(const char* symbol, const char* action_code, int production_id) {
    Node* node = (Node*)calloc(1, sizeof(Node));
    node->symbol = dup_text(symbol);
    node->semantic_action = 1;
    node->action_code = dup_text(action_code);
    node->production_id = production_id;
    return node;
}

static const ActionEntry* lookup_action(int state, const char* symbol) {
    for (int i = 0; i < ACTION_TABLE_COUNT; i++) {
        if (ACTION_TABLE[i].state == state && strcmp(ACTION_TABLE[i].symbol, symbol) == 0) {
            return &ACTION_TABLE[i];
        }
    }
    return NULL;
}

static int lookup_goto(int state, const char* symbol) {
    for (int i = 0; i < GOTO_TABLE_COUNT; i++) {
        if (GOTO_TABLE[i].state == state && strcmp(GOTO_TABLE[i].symbol, symbol) == 0) {
            return GOTO_TABLE[i].target;
        }
    }
    return -1;
}

static Token* read_tokens(const char* path, int* out_count) {
    FILE* in = fopen(path, "rb");
    if (!in) {
        perror("open token file");
        exit(1);
    }
    int capacity = 128;
    int count = 0;
    Token* tokens = (Token*)calloc(capacity, sizeof(Token));
    char line[8192];
    while (fgets(line, sizeof(line), in)) {
        size_t len = strlen(line);
        while (len > 0 && (line[len - 1] == '\\n' || line[len - 1] == '\\r')) {
            line[--len] = '\\0';
        }
        char* sep = strchr(line, '\\t');
        char* type_text = line;
        char* lexeme_text = sep ? sep + 1 : line;
        if (sep) {
            *sep = '\\0';
        }
        if (count == capacity) {
            capacity *= 2;
            tokens = (Token*)realloc(tokens, sizeof(Token) * capacity);
            if (!tokens) {
                fprintf(stderr, "out of memory\\n");
                exit(2);
            }
        }
        tokens[count].type = dup_text(type_text);
        tokens[count].lexeme = unescape_text(lexeme_text);
        count++;
    }
    fclose(in);
    *out_count = count;
    return tokens;
}

static void serialize_node(FILE* out, Node* node) {
    fputs("NODE\\t", out);
    write_escaped(out, node->symbol ? node->symbol : "");
    fputc('\\t', out);
    write_escaped(out, node->lexeme ? node->lexeme : "");
    fprintf(out, "\\t%d\\t", node->semantic_action ? 1 : 0);
    write_escaped(out, node->action_code ? node->action_code : "");
    fprintf(out, "\\t%d\\t%d\\n", node->production_id, node->child_count);
    for (int i = 0; i < node->child_count; i++) {
        serialize_node(out, node->children[i]);
    }
}

""");
    }

    private void emitParseFunction(StringBuilder sb) {
        sb.append("""
static Node* parse_tokens(Token* tokens, int token_count) {
    int state_capacity = 1024;
    int stack_top = 0;
    int* state_stack = (int*)malloc(sizeof(int) * state_capacity);
    Node** node_stack = (Node**)malloc(sizeof(Node*) * state_capacity);
    state_stack[stack_top] = 0;
    node_stack[stack_top] = NULL;
    int index = 0;

    while (1) {
        if (index >= token_count) {
            fprintf(stderr, "parse error: token stream ended before EOF\\n");
            exit(1);
        }
        int current_state = state_stack[stack_top];
        Token current = tokens[index];
        const ActionEntry* action = lookup_action(current_state, current.type);
        if (!action) {
            fprintf(stderr, "parse error: no ACTION for state=%d token=%s\\n", current_state, current.type);
            exit(1);
        }

        if (action->kind == ACTION_SHIFT) {
            if (stack_top + 2 >= state_capacity) {
                state_capacity *= 2;
                state_stack = (int*)realloc(state_stack, sizeof(int) * state_capacity);
                node_stack = (Node**)realloc(node_stack, sizeof(Node*) * state_capacity);
            }
            stack_top++;
            state_stack[stack_top] = action->value;
            node_stack[stack_top] = make_leaf(current.type, current.lexeme);
            index++;
            continue;
        }

        if (action->kind == ACTION_REDUCE) {
            int production_id = action->value;
            int rhs_len = PRODUCTION_RHS_LEN[production_id];
            Node** children = NULL;
            if (rhs_len > 0) {
                children = (Node**)calloc(rhs_len, sizeof(Node*));
            }
            for (int i = rhs_len - 1; i >= 0; i--) {
                if (stack_top <= 0) {
                    fprintf(stderr, "parse error: stack underflow during reduce\\n");
                    exit(1);
                }
                children[i] = node_stack[stack_top];
                stack_top--;
            }

            Node* parent;
            if (PRODUCTION_IS_ACTION[production_id]) {
                parent = make_action_node(PRODUCTION_LHS[production_id], PRODUCTION_ACTION_CODE[production_id], production_id);
            } else if (PRODUCTION_HAS_MAPPED_ACTION[production_id]) {
                Node** with_action = (Node**)calloc(rhs_len + 1, sizeof(Node*));
                for (int i = 0; i < rhs_len; i++) {
                    with_action[i] = children[i];
                }
                with_action[rhs_len] = make_action_node("__ACT_MAPPED", PRODUCTION_ACTION_CODE[production_id], production_id);
                parent = make_nonterminal(PRODUCTION_LHS[production_id], production_id, rhs_len + 1, with_action);
            } else {
                parent = make_nonterminal(PRODUCTION_LHS[production_id], production_id, rhs_len, children);
            }

            int goto_state = lookup_goto(state_stack[stack_top], PRODUCTION_LHS[production_id]);
            if (goto_state < 0) {
                fprintf(stderr, "parse error: no GOTO after reduce for state=%d nonterminal=%s\\n",
                        state_stack[stack_top], PRODUCTION_LHS[production_id]);
                exit(1);
            }
            stack_top++;
            state_stack[stack_top] = goto_state;
            node_stack[stack_top] = parent;
            continue;
        }

        if (action->kind == ACTION_ACCEPT) {
            return node_stack[stack_top];
        }

        fprintf(stderr, "parse error: unknown action kind=%d\\n", action->kind);
        exit(1);
    }
}

""");
    }

    private void emitMain(StringBuilder sb) {
        sb.append("""
int main(int argc, char** argv) {
    if (argc != 3) {
        fprintf(stderr, "usage: %s <tokens.txt> <parse-tree.txt>\\n", argv[0]);
        return 1;
    }
    int token_count = 0;
    Token* tokens = read_tokens(argv[1], &token_count);
    Node* root = parse_tokens(tokens, token_count);
    FILE* out = fopen(argv[2], "wb");
    if (!out) {
        perror("open parse tree output");
        return 1;
    }
    serialize_node(out, root);
    fclose(out);
    return 0;
}
""");
    }

    private boolean isSemanticActionProduction(Production production) {
        return production.isEpsilon()
                && production.getLeft().getName().startsWith("__ACT_")
                && production.hasActionCode();
    }

    private static String escape(String text) {
        return text.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
