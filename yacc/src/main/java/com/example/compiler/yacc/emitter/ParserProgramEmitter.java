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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class ParserProgramEmitter {
    public String emit(String className, Grammar grammar, ParseTable parseTable) {
        Objects.requireNonNull(className, "className");
        Objects.requireNonNull(grammar, "grammar");
        Objects.requireNonNull(parseTable, "parseTable");

        StringBuilder sb = new StringBuilder();

        sb.append("import com.example.compiler.yacc.token.Token;\n");
        sb.append("import com.example.compiler.yacc.token.TokenType;\n");
        sb.append("\n");
        sb.append("import java.nio.file.Files;\n");
        sb.append("import java.nio.file.Path;\n");
        sb.append("import java.util.ArrayDeque;\n");
        sb.append("import java.util.ArrayList;\n");
        sb.append("import java.util.Deque;\n");
        sb.append("import java.util.LinkedHashMap;\n");
        sb.append("import java.util.LinkedList;\n");
        sb.append("import java.util.List;\n");
        sb.append("import java.util.Map;\n");
        sb.append("\n");

        sb.append("public final class ").append(className).append(" {\n\n");

        emitProductionArrays(sb, grammar);
        emitTableFields(sb);
        emitRecordAndNode(sb);

        sb.append("    static {\n");
        emitActionTableInitializer(sb, parseTable);
        emitGotoTableInitializer(sb, parseTable);
        sb.append("    }\n\n");

        emitUtilityMethods(sb);
        emitParseMethod(sb);
        emitReadTokensMethod(sb);
        emitMainMethod(sb, className);

        sb.append("}\n");

        return sb.toString();
    }

    public Path emitToFile(Path outputFile, String className, Grammar grammar, ParseTable parseTable) throws IOException {
        Objects.requireNonNull(outputFile, "outputFile");
        String source = emit(className, grammar, parseTable);
        if (outputFile.getParent() != null) {
            Files.createDirectories(outputFile.getParent());
        }
        Files.writeString(outputFile, source);
        return outputFile;
    }

    private void emitProductionArrays(StringBuilder sb, Grammar grammar) {
        List<Production> productions = grammar.getProductions();

        sb.append("    private static final String[] PRODUCTION_LHS = new String[] {\n");
        for (int i = 0; i < productions.size(); i++) {
            Production p = productions.get(i);
            sb.append("        \"").append(escape(p.getLeft().getName())).append("\"");
            if (i < productions.size() - 1) {
                sb.append(",");
            }
            sb.append("\n");
        }
        sb.append("    };\n\n");

        sb.append("    private static final int[] PRODUCTION_RHS_LEN = new int[] {\n");
        for (int i = 0; i < productions.size(); i++) {
            Production p = productions.get(i);
            sb.append("        ").append(p.getRight().size());
            if (i < productions.size() - 1) {
                sb.append(",");
            }
            sb.append("\n");
        }
        sb.append("    };\n\n");
    }

    private void emitTableFields(StringBuilder sb) {
        sb.append("    private static final Map<Integer, Map<String, EncodedAction>> ACTION = new LinkedHashMap<>();\n");
        sb.append("    private static final Map<Integer, Map<String, Integer>> GOTO = new LinkedHashMap<>();\n\n");
    }

    private void emitRecordAndNode(StringBuilder sb) {
        sb.append("    private record EncodedAction(String kind, int value) {\n");
        sb.append("    }\n\n");

        sb.append("    public record ParseOutcome(boolean accepted, String errorMessage, List<Integer> reductions, Node root) {\n");
        sb.append("    }\n\n");

        sb.append("    public static final class Node {\n");
        sb.append("        private final String symbol;\n");
        sb.append("        private final String lexeme;\n");
        sb.append("        private final List<Node> children;\n\n");

        sb.append("        private Node(String symbol, String lexeme, List<Node> children) {\n");
        sb.append("            this.symbol = symbol;\n");
        sb.append("            this.lexeme = lexeme;\n");
        sb.append("            this.children = new ArrayList<>(children);\n");
        sb.append("        }\n\n");

        sb.append("        public static Node leaf(String symbol, String lexeme) {\n");
        sb.append("            return new Node(symbol, lexeme, List.of());\n");
        sb.append("        }\n\n");

        sb.append("        public static Node nonTerminal(String symbol, List<Node> children) {\n");
        sb.append("            return new Node(symbol, null, children);\n");
        sb.append("        }\n\n");

        sb.append("        public String prettyPrint() {\n");
        sb.append("            StringBuilder sb = new StringBuilder();\n");
        sb.append("            prettyPrint(sb, \"\", true);\n");
        sb.append("            return sb.toString();\n");
        sb.append("        }\n\n");

        sb.append("        private void prettyPrint(StringBuilder sb, String prefix, boolean isLast) {\n");
        sb.append("            sb.append(prefix).append(isLast ? \"└── \" : \"├── \").append(symbol);\n");
        sb.append("            if (lexeme != null) {\n");
        sb.append("                sb.append(\"(\\\"\").append(lexeme).append(\"\\\")\");\n");
        sb.append("            }\n");
        sb.append("            sb.append(\"\\n\");\n\n");
        sb.append("            for (int i = 0; i < children.size(); i++) {\n");
        sb.append("                children.get(i).prettyPrint(sb, prefix + (isLast ? \"    \" : \"│   \"), i == children.size() - 1);\n");
        sb.append("            }\n");
        sb.append("        }\n");
        sb.append("    }\n\n");
    }

    private void emitUtilityMethods(StringBuilder sb) {
        sb.append("    private static void putAction(int state, String terminal, String kind, int value) {\n");
        sb.append("        ACTION.computeIfAbsent(state, key -> new LinkedHashMap<>())\n");
        sb.append("                .put(terminal, new EncodedAction(kind, value));\n");
        sb.append("    }\n\n");

        sb.append("    private static void putGoto(int state, String nonTerminal, int target) {\n");
        sb.append("        GOTO.computeIfAbsent(state, key -> new LinkedHashMap<>())\n");
        sb.append("                .put(nonTerminal, target);\n");
        sb.append("    }\n\n");
    }

    private void emitParseMethod(StringBuilder sb) {
        sb.append("    public static ParseOutcome parse(List<Token> tokens) {\n");
        sb.append("        Deque<Integer> stateStack = new ArrayDeque<>();\n");
        sb.append("        Deque<String> symbolStack = new ArrayDeque<>();\n");
        sb.append("        Deque<Node> nodeStack = new ArrayDeque<>();\n");
        sb.append("        List<Integer> reductions = new ArrayList<>();\n\n");

        sb.append("        stateStack.push(0);\n");
        sb.append("        int index = 0;\n\n");

        sb.append("        while (true) {\n");
        sb.append("            if (index >= tokens.size()) {\n");
        sb.append("                return new ParseOutcome(false, \"Input token stream ended before EOF.\", reductions, null);\n");
        sb.append("            }\n\n");

        sb.append("            int currentState = stateStack.peek();\n");
        sb.append("            Token currentToken = tokens.get(index);\n");
        sb.append("            String terminalName = currentToken.type().name();\n\n");

        sb.append("            EncodedAction action = ACTION.getOrDefault(currentState, Map.of()).get(terminalName);\n");
        sb.append("            if (action == null) {\n");
        sb.append("                return new ParseOutcome(\n");
        sb.append("                        false,\n");
        sb.append("                        \"No ACTION for state=\" + currentState + \", token=\" + currentToken,\n");
        sb.append("                        reductions,\n");
        sb.append("                        null\n");
        sb.append("                );\n");
        sb.append("            }\n\n");

        sb.append("            switch (action.kind()) {\n");
        sb.append("                case \"SHIFT\" -> {\n");
        sb.append("                    symbolStack.push(terminalName);\n");
        sb.append("                    nodeStack.push(Node.leaf(terminalName, currentToken.lexeme()));\n");
        sb.append("                    stateStack.push(action.value());\n");
        sb.append("                    index++;\n");
        sb.append("                }\n");
        sb.append("                case \"REDUCE\" -> {\n");
        sb.append("                    int productionId = action.value();\n");
        sb.append("                    String lhs = PRODUCTION_LHS[productionId];\n");
        sb.append("                    int rhsLen = PRODUCTION_RHS_LEN[productionId];\n\n");

        sb.append("                    LinkedList<Node> children = new LinkedList<>();\n");
        sb.append("                    for (int i = 0; i < rhsLen; i++) {\n");
        sb.append("                        if (stateStack.isEmpty() || symbolStack.isEmpty() || nodeStack.isEmpty()) {\n");
        sb.append("                            return new ParseOutcome(false, \"Stack underflow during reduce, production=\" + productionId, reductions, null);\n");
        sb.append("                        }\n");
        sb.append("                        stateStack.pop();\n");
        sb.append("                        symbolStack.pop();\n");
        sb.append("                        children.addFirst(nodeStack.pop());\n");
        sb.append("                    }\n\n");

        sb.append("                    Integer gotoState = GOTO.getOrDefault(stateStack.peek(), Map.of()).get(lhs);\n");
        sb.append("                    if (gotoState == null) {\n");
        sb.append("                        return new ParseOutcome(\n");
        sb.append("                                false,\n");
        sb.append("                                \"No GOTO after reduce: state=\" + stateStack.peek() + \", nonTerminal=\" + lhs,\n");
        sb.append("                                reductions,\n");
        sb.append("                                null\n");
        sb.append("                        );\n");
        sb.append("                    }\n\n");

        sb.append("                    symbolStack.push(lhs);\n");
        sb.append("                    nodeStack.push(Node.nonTerminal(lhs, children));\n");
        sb.append("                    stateStack.push(gotoState);\n");
        sb.append("                    reductions.add(productionId);\n");
        sb.append("                }\n");
        sb.append("                case \"ACCEPT\" -> {\n");
        sb.append("                    Node root = nodeStack.isEmpty() ? null : nodeStack.peek();\n");
        sb.append("                    return new ParseOutcome(true, null, reductions, root);\n");
        sb.append("                }\n");
        sb.append("                default -> {\n");
        sb.append("                    return new ParseOutcome(false, \"Unknown action kind: \" + action.kind(), reductions, null);\n");
        sb.append("                }\n");
        sb.append("            }\n");
        sb.append("        }\n");
        sb.append("    }\n\n");
    }

    private void emitReadTokensMethod(StringBuilder sb) {
        sb.append("    public static List<Token> readTokens(Path path) throws Exception {\n");
        sb.append("        List<String> lines = Files.readAllLines(path);\n");
        sb.append("        List<Token> tokens = new ArrayList<>();\n\n");

        sb.append("        for (String raw : lines) {\n");
        sb.append("            String line = raw.trim();\n");
        sb.append("            if (line.isEmpty() || line.startsWith(\"#\")) {\n");
        sb.append("                continue;\n");
        sb.append("            }\n\n");

        sb.append("            String[] parts = line.split(\"\\\\s+\", 2);\n");
        sb.append("            TokenType type = TokenType.valueOf(parts[0]);\n");
        sb.append("            String lexeme = parts.length == 2 ? parts[1] : parts[0];\n");
        sb.append("            tokens.add(new Token(type, lexeme));\n");
        sb.append("        }\n\n");

        sb.append("        if (tokens.isEmpty() || tokens.get(tokens.size() - 1).type() != TokenType.EOF) {\n");
        sb.append("            tokens.add(new Token(TokenType.EOF, \"<EOF>\"));\n");
        sb.append("        }\n\n");

        sb.append("        return tokens;\n");
        sb.append("    }\n\n");
    }

    private void emitMainMethod(StringBuilder sb, String className) {
        sb.append("    public static void main(String[] args) throws Exception {\n");
        sb.append("        if (args.length != 1) {\n");
        sb.append("            System.err.println(\"Usage: java ").append(escape(className)).append(" <token-file>\");\n");
        sb.append("            System.exit(1);\n");
        sb.append("        }\n\n");

        sb.append("        List<Token> tokens = readTokens(Path.of(args[0]));\n");
        sb.append("        ParseOutcome outcome = parse(tokens);\n\n");

        sb.append("        if (!outcome.accepted()) {\n");
        sb.append("            System.err.println(\"PARSE FAILED\");\n");
        sb.append("            System.err.println(outcome.errorMessage());\n");
        sb.append("            System.exit(2);\n");
        sb.append("        }\n\n");

        sb.append("        System.out.println(\"PARSE ACCEPTED\");\n");
        sb.append("        System.out.println(\"Reductions: \" + outcome.reductions());\n");
        sb.append("        if (outcome.root() != null) {\n");
        sb.append("            System.out.println(outcome.root().prettyPrint());\n");
        sb.append("        }\n");
        sb.append("    }\n\n");
    }

    private void emitActionTableInitializer(StringBuilder sb, ParseTable parseTable) {
        List<Integer> states = new ArrayList<>(parseTable.actionRows().keySet());
        states.sort(Comparator.naturalOrder());

        for (Integer state : states) {
            Map<Terminal, Action> row = parseTable.actionRows().get(state);
            List<Map.Entry<Terminal, Action>> entries = new ArrayList<>(row.entrySet());
            entries.sort(Comparator.comparing(e -> e.getKey().getName()));

            for (Map.Entry<Terminal, Action> entry : entries) {
                Terminal terminal = entry.getKey();
                Action action = entry.getValue();

                String kind;
                int value;
                if (action.type() == ActionType.SHIFT) {
                    kind = "SHIFT";
                    value = action.targetState();
                } else if (action.type() == ActionType.REDUCE) {
                    kind = "REDUCE";
                    value = action.productionId();
                } else {
                    kind = "ACCEPT";
                    value = -1;
                }

                sb.append("        putAction(")
                        .append(state)
                        .append(", \"")
                        .append(escape(terminal.getName()))
                        .append("\", \"")
                        .append(kind)
                        .append("\", ")
                        .append(value)
                        .append(");\n");
            }
        }
    }

    private void emitGotoTableInitializer(StringBuilder sb, ParseTable parseTable) {
        List<Integer> states = new ArrayList<>(parseTable.gotoRows().keySet());
        states.sort(Comparator.naturalOrder());

        for (Integer state : states) {
            Map<NonTerminal, Integer> row = parseTable.gotoRows().get(state);
            List<Map.Entry<NonTerminal, Integer>> entries = new ArrayList<>(row.entrySet());
            entries.sort(Comparator.comparing(e -> e.getKey().getName()));

            for (Map.Entry<NonTerminal, Integer> entry : entries) {
                sb.append("        putGoto(")
                        .append(state)
                        .append(", \"")
                        .append(escape(entry.getKey().getName()))
                        .append("\", ")
                        .append(entry.getValue())
                        .append(");\n");
            }
        }
    }

    private String escape(String text) {
        return text.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}