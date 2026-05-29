package com.example.compiler.yacc.emitter;

import com.example.compiler.yacc.grammar.Grammar;
import com.example.compiler.yacc.grammar.NonTerminal;
import com.example.compiler.yacc.grammar.Production;
import com.example.compiler.yacc.grammar.Terminal;
import com.example.compiler.yacc.table.Action;
import com.example.compiler.yacc.table.ActionType;
import com.example.compiler.yacc.table.ParseTable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 生成 C 版表驱动语法分析程序 yyparse.c。
 *
 * <p>生成程序读取 tokens.txt，使用编码在 C 源码中的 ACTION/GOTO 表执行
 * shift/reduce/accept，并在规约时构造 action-tree.txt。它对应课程流程图中的
 * yacc -> yyparse.c -> yyparse -> action-tree.txt。</p>
 */
public final class CParserProgramEmitter {

    public String emit(Grammar grammar, ParseTable parseTable) {
        Objects.requireNonNull(grammar, "grammar");
        Objects.requireNonNull(parseTable, "parseTable");

        StringBuilder out = new StringBuilder();
        emitHeader(out);
        emitProductionTables(out, grammar);
        emitActionTable(out, parseTable);
        emitGotoTable(out, parseTable);
        emitRuntime(out);
        return out.toString();
    }

    public Path emitToFile(Path outputFile, Grammar grammar, ParseTable parseTable) throws IOException {
        Objects.requireNonNull(outputFile, "outputFile");
        if (outputFile.getParent() != null) {
            Files.createDirectories(outputFile.getParent());
        }
        Files.writeString(outputFile, emit(grammar, parseTable));
        return outputFile;
    }

    private void emitHeader(StringBuilder out) {
        out.append("""
                #include <stdio.h>
                #include <stdlib.h>
                #include <string.h>

                typedef enum { ACT_SHIFT = 1, ACT_REDUCE = 2, ACT_ACCEPT = 3 } ActionKind;

                typedef struct {
                    int state;
                    const char* terminal;
                    ActionKind kind;
                    int value;
                } ActionEntry;

                typedef struct {
                    int state;
                    const char* non_terminal;
                    int value;
                } GotoEntry;

                typedef struct Node {
                    char* symbol;
                    char* lexeme;
                    int is_action;
                    char* action_code;
                    int production_id;
                    int child_count;
                    struct Node** children;
                } Node;

                typedef struct {
                    char* type;
                    char* lexeme;
                } Token;

                """);
    }

    private void emitProductionTables(StringBuilder out, Grammar grammar) {
        List<Production> productions = grammar.getProductions();

        // 产生式表是 yyparse.c 执行 REDUCE 的依据：
        // LHS 用于查 GOTO，RHS_LEN 决定弹栈数量，ACTION_CODE 用于输出动作节点。
        out.append("static const int PRODUCTION_COUNT = ").append(productions.size()).append(";\n");

        out.append("static const char* PRODUCTION_LHS[] = {\n");
        for (Production production : productions) {
            out.append("    \"").append(cString(production.getLeft().getName())).append("\",\n");
        }
        out.append("};\n\n");

        out.append("static const int PRODUCTION_RHS_LEN[] = {\n");
        for (Production production : productions) {
            out.append("    ").append(production.getRight().size()).append(",\n");
        }
        out.append("};\n\n");

        out.append("static const int PRODUCTION_IS_ACTION[] = {\n");
        for (Production production : productions) {
            out.append("    ").append(isSemanticActionProduction(production) ? 1 : 0).append(",\n");
        }
        out.append("};\n\n");

        out.append("static const char* PRODUCTION_ACTION_CODE[] = {\n");
        for (Production production : productions) {
            if (production.getActionCode() == null) {
                out.append("    NULL,\n");
            } else {
                out.append("    \"").append(cString(production.getActionCode())).append("\",\n");
            }
        }
        out.append("};\n\n");
    }

    private void emitActionTable(StringBuilder out, ParseTable parseTable) {
        // ACTION 表在 C 程序中采用稀疏数组编码。运行时通过线性查找
        // (state, terminal) 对应的 shift/reduce/accept 动作；课程规模下足够清晰。
        List<Map.Entry<Integer, Map<Terminal, Action>>> rows = parseTable.actionRows()
                .entrySet()
                .stream()
                .sorted(Map.Entry.comparingByKey())
                .toList();

        int count = rows.stream().mapToInt(entry -> entry.getValue().size()).sum();
        out.append("static const int ACTION_COUNT = ").append(count).append(";\n");
        out.append("static const ActionEntry ACTIONS[] = {\n");
        for (Map.Entry<Integer, Map<Terminal, Action>> row : rows) {
            row.getValue().entrySet()
                    .stream()
                    .sorted(Comparator.comparing(entry -> entry.getKey().getName()))
                    .forEach(entry -> {
                        Action action = entry.getValue();
                        String kind = switch (action.type()) {
                            case SHIFT -> "ACT_SHIFT";
                            case REDUCE -> "ACT_REDUCE";
                            case ACCEPT -> "ACT_ACCEPT";
                        };
                        int value = switch (action.type()) {
                            case SHIFT -> action.targetState();
                            case REDUCE -> action.productionId();
                            case ACCEPT -> -1;
                        };
                        out.append("    {")
                                .append(row.getKey())
                                .append(", \"")
                                .append(cString(entry.getKey().getName()))
                                .append("\", ")
                                .append(kind)
                                .append(", ")
                                .append(value)
                                .append("},\n");
                    });
        }
        out.append("};\n\n");
    }

    private void emitGotoTable(StringBuilder out, ParseTable parseTable) {
        // GOTO 表同样采用稀疏数组编码，用于 REDUCE 后根据
        // “规约前的新栈顶状态 + 产生式左部”跳转到下一个状态。
        List<Map.Entry<Integer, Map<NonTerminal, Integer>>> rows = parseTable.gotoRows()
                .entrySet()
                .stream()
                .sorted(Map.Entry.comparingByKey())
                .toList();

        int count = rows.stream().mapToInt(entry -> entry.getValue().size()).sum();
        out.append("static const int GOTO_COUNT = ").append(count).append(";\n");
        out.append("static const GotoEntry GOTOS[] = {\n");
        for (Map.Entry<Integer, Map<NonTerminal, Integer>> row : rows) {
            row.getValue().entrySet()
                    .stream()
                    .sorted(Comparator.comparing(entry -> entry.getKey().getName()))
                    .forEach(entry -> out.append("    {")
                            .append(row.getKey())
                            .append(", \"")
                            .append(cString(entry.getKey().getName()))
                            .append("\", ")
                            .append(entry.getValue())
                            .append("},\n"));
        }
        out.append("};\n\n");
    }

    private void emitRuntime(StringBuilder out) {
        out.append("""
                static char* dup_text(const char* text) {
                    if (text == NULL) return NULL;
                    size_t len = strlen(text);
                    char* copy = (char*)malloc(len + 1);
                    if (copy == NULL) {
                        fprintf(stderr, "out of memory\\n");
                        exit(2);
                    }
                    memcpy(copy, text, len + 1);
                    return copy;
                }

                static char* trim_newline(char* text) {
                    size_t len = strlen(text);
                    while (len > 0 && (text[len - 1] == '\\n' || text[len - 1] == '\\r')) {
                        text[--len] = '\\0';
                    }
                    return text;
                }

                static Node* make_node(const char* symbol, const char* lexeme, int is_action,
                                       const char* action_code, int production_id,
                                       int child_count, Node** children) {
                    /* Node 是 action-tree.txt 的内存形态。普通叶子节点保存 token，
                       非终结符节点保存 production_id 和 children，动作节点额外保存 action_code。 */
                    Node* node = (Node*)calloc(1, sizeof(Node));
                    if (node == NULL) {
                        fprintf(stderr, "out of memory\\n");
                        exit(2);
                    }
                    node->symbol = dup_text(symbol);
                    node->lexeme = dup_text(lexeme);
                    node->is_action = is_action;
                    node->action_code = dup_text(action_code);
                    node->production_id = production_id;
                    node->child_count = child_count;
                    node->children = children;
                    return node;
                }

                static const ActionEntry* find_action(int state, const char* terminal) {
                    for (int i = 0; i < ACTION_COUNT; i++) {
                        if (ACTIONS[i].state == state && strcmp(ACTIONS[i].terminal, terminal) == 0) {
                            return &ACTIONS[i];
                        }
                    }
                    return NULL;
                }

                static int find_goto(int state, const char* non_terminal) {
                    for (int i = 0; i < GOTO_COUNT; i++) {
                        if (GOTOS[i].state == state && strcmp(GOTOS[i].non_terminal, non_terminal) == 0) {
                            return GOTOS[i].value;
                        }
                    }
                    return -1;
                }

                static Token* read_tokens(const char* path, int* out_count) {
                    /* tokens.txt 每行格式为 TOKEN<TAB>lexeme。
                       yyparse.c 只关心 token 类型做语法分析，同时保留 lexeme 写入叶子节点。 */
                    FILE* file = fopen(path, "r");
                    if (file == NULL) {
                        perror(path);
                        exit(2);
                    }

                    int capacity = 256;
                    int count = 0;
                    Token* tokens = (Token*)calloc((size_t)capacity, sizeof(Token));
                    if (tokens == NULL) {
                        fprintf(stderr, "out of memory\\n");
                        exit(2);
                    }

                    char line[4096];
                    while (fgets(line, sizeof(line), file) != NULL) {
                        trim_newline(line);
                        if (line[0] == '\\0') continue;

                        char* tab = strchr(line, '\\t');
                        char* type = line;
                        char* lexeme = "";
                        if (tab != NULL) {
                            *tab = '\\0';
                            lexeme = tab + 1;
                        }

                        if (count == capacity) {
                            capacity *= 2;
                            tokens = (Token*)realloc(tokens, (size_t)capacity * sizeof(Token));
                            if (tokens == NULL) {
                                fprintf(stderr, "out of memory\\n");
                                exit(2);
                            }
                        }
                        tokens[count].type = dup_text(type);
                        tokens[count].lexeme = dup_text(lexeme);
                        count++;
                    }
                    fclose(file);
                    *out_count = count;
                    return tokens;
                }

                static void write_node(FILE* out, const Node* node) {
                    if (node == NULL) return;
                    /* action-tree.txt 使用先序遍历：先写当前节点，再写所有子树。
                       child_count 让 AstTreeCodec 能够无歧义地递归恢复树结构。 */
                    fprintf(out, "NODE\\t%s\\t%s\\t%d\\t%s\\t%d\\t%d\\n",
                            node->symbol == NULL ? "" : node->symbol,
                            node->lexeme == NULL ? "" : node->lexeme,
                            node->is_action,
                            node->action_code == NULL ? "" : node->action_code,
                            node->production_id,
                            node->child_count);
                    for (int i = 0; i < node->child_count; i++) {
                        write_node(out, node->children[i]);
                    }
                }

                static int parse(Token* tokens, int token_count, const char* output_path) {
                    int state_capacity = 4096;
                    int node_capacity = 4096;
                    int* states = (int*)calloc((size_t)state_capacity, sizeof(int));
                    char** symbols = (char**)calloc((size_t)state_capacity, sizeof(char*));
                    Node** nodes = (Node**)calloc((size_t)node_capacity, sizeof(Node*));
                    if (states == NULL || symbols == NULL || nodes == NULL) {
                        fprintf(stderr, "out of memory\\n");
                        return 2;
                    }

                    int state_top = 0;
                    int symbol_top = 0;
                    int node_top = 0;
                    int index = 0;
                    states[state_top++] = 0;

                    while (1) {
                        if (index >= token_count) {
                            fprintf(stderr, "Input token stream ended before EOF.\\n");
                            return 1;
                        }

                        int current_state = states[state_top - 1];
                        const char* terminal = tokens[index].type;
                        const ActionEntry* action = find_action(current_state, terminal);
                        if (action == NULL) {
                            fprintf(stderr, "No ACTION for state=%d, token=%s (%s)\\n",
                                    current_state, terminal, tokens[index].lexeme);
                            return 1;
                        }

                        if (action->kind == ACT_SHIFT) {
                            /* SHIFT：消费当前 token，压入终结符叶子节点，并进入目标状态。 */
                            if (state_top >= state_capacity || symbol_top >= state_capacity || node_top >= node_capacity) {
                                fprintf(stderr, "parser stack overflow\\n");
                                return 2;
                            }
                            symbols[symbol_top++] = dup_text(terminal);
                            nodes[node_top++] = make_node(terminal, tokens[index].lexeme, 0, NULL, -1, 0, NULL);
                            states[state_top++] = action->value;
                            index++;
                            continue;
                        }

                        if (action->kind == ACT_REDUCE) {
                            /* REDUCE：按产生式右部长度弹栈，组装父节点，再根据产生式左部查 GOTO。 */
                            int production_id = action->value;
                            if (production_id < 0 || production_id >= PRODUCTION_COUNT) {
                                fprintf(stderr, "Invalid production id: %d\\n", production_id);
                                return 1;
                            }

                            int rhs_len = PRODUCTION_RHS_LEN[production_id];
                            if (state_top <= rhs_len || symbol_top < rhs_len || node_top < rhs_len) {
                                fprintf(stderr, "Stack underflow during reduce, production=%d\\n", production_id);
                                return 1;
                            }

                            Node** children = NULL;
                            if (rhs_len > 0) {
                                children = (Node**)calloc((size_t)rhs_len, sizeof(Node*));
                                if (children == NULL) {
                                    fprintf(stderr, "out of memory\\n");
                                    return 2;
                                }
                            }

                            for (int i = rhs_len - 1; i >= 0; i--) {
                                /* 从右向左填 children，抵消栈弹出顺序，保证 action-tree 中孩子仍是源码顺序。 */
                                state_top--;
                                free(symbols[--symbol_top]);
                                children[i] = nodes[--node_top];
                            }

                            const char* lhs = PRODUCTION_LHS[production_id];
                            Node* parent = make_node(
                                    lhs,
                                    NULL,
                                    PRODUCTION_IS_ACTION[production_id],
                                    PRODUCTION_ACTION_CODE[production_id],
                                    production_id,
                                    rhs_len,
                                    children);

                            int goto_state = find_goto(states[state_top - 1], lhs);
                            if (goto_state < 0) {
                                fprintf(stderr, "No GOTO after reduce: state=%d, nonTerminal=%s\\n",
                                        states[state_top - 1], lhs);
                                return 1;
                            }

                            symbols[symbol_top++] = dup_text(lhs);
                            nodes[node_top++] = parent;
                            states[state_top++] = goto_state;
                            continue;
                        }

                        if (action->kind == ACT_ACCEPT) {
                            /* ACCEPT：语法分析完成，AST 栈顶就是完整 translation_unit。 */
                            FILE* out = fopen(output_path, "w");
                            if (out == NULL) {
                                perror(output_path);
                                return 2;
                            }
                            Node* root = node_top == 0 ? NULL : nodes[node_top - 1];
                            write_node(out, root);
                            fclose(out);
                            return 0;
                        }

                        fprintf(stderr, "Unknown action kind.\\n");
                        return 1;
                    }
                }

                int main(int argc, char** argv) {
                    if (argc != 3) {
                        fprintf(stderr, "usage: %s <tokens.txt> <action-tree.txt>\\n", argv[0]);
                        return 2;
                    }

                    int token_count = 0;
                    Token* tokens = read_tokens(argv[1], &token_count);
                    return parse(tokens, token_count, argv[2]);
                }
                """);
    }

    private static boolean isSemanticActionProduction(Production production) {
        return production.isEpsilon()
                && production.getLeft().getName().startsWith("__ACT_")
                && production.hasActionCode();
    }

    private static String cString(String value) {
        if (value == null) {
            return "";
        }

        StringBuilder out = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            switch (ch) {
                case '\\' -> out.append("\\\\");
                case '"' -> out.append("\\\"");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                default -> {
                    if (ch < 32 || ch > 126) {
                        out.append(String.format("\\x%02x", (int) ch));
                    } else {
                        out.append(ch);
                    }
                }
            }
        }
        return out.toString();
    }
}
