package com.example.compiler.yacc.ast;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * action-tree.txt 编解码器。
 *
 * <p>C 版 yyparse 按先序输出节点，每行格式为：
 * {@code NODE<TAB>symbol<TAB>lexeme<TAB>isAction<TAB>actionCode<TAB>productionId<TAB>childCount}。
 * 本类负责把该落盘格式恢复为 Java 语义阶段使用的 {@link AstNode}。</p>
 */
public final class AstTreeCodec {
    private AstTreeCodec() {
    }

    public static AstNode read(Path inputFile) throws IOException {
        Objects.requireNonNull(inputFile, "inputFile");
        List<String> lines = Files.readAllLines(inputFile)
                .stream()
                .filter(line -> !line.isBlank())
                .toList();
        if (lines.isEmpty()) {
            throw new IllegalArgumentException("Empty action tree file: " + inputFile);
        }

        Cursor cursor = new Cursor(lines);
        AstNode root = readNode(cursor);
        if (cursor.hasNext()) {
            throw new IllegalArgumentException("Trailing nodes in action tree file: " + inputFile);
        }
        return root;
    }

    public static void write(Path outputFile, AstNode root) throws IOException {
        Objects.requireNonNull(outputFile, "outputFile");
        Objects.requireNonNull(root, "root");
        if (outputFile.getParent() != null) {
            Files.createDirectories(outputFile.getParent());
        }
        StringBuilder out = new StringBuilder();
        writeNode(out, root);
        Files.writeString(outputFile, out);
    }

    private static AstNode readNode(Cursor cursor) {
        String line = cursor.next();
        String[] fields = line.split("\t", -1);
        if (fields.length != 7 || !"NODE".equals(fields[0])) {
            throw new IllegalArgumentException("Invalid action tree line: " + line);
        }

        String symbol = fields[1];
        String lexeme = emptyToNull(fields[2]);
        boolean isAction = "1".equals(fields[3]);
        String actionCode = emptyToNull(fields[4]);
        int productionId = parseInt(fields[5], "production id", line);
        int childCount = parseInt(fields[6], "child count", line);

        if (childCount < 0) {
            throw new IllegalArgumentException("Negative child count in action tree line: " + line);
        }

        if (isAction) {
            if (childCount != 0) {
                throw new IllegalArgumentException("Semantic action node cannot have children: " + line);
            }
            return AstNode.semanticAction(symbol, actionCode, productionId);
        }

        List<AstNode> children = new ArrayList<>();
        for (int i = 0; i < childCount; i++) {
            children.add(readNode(cursor));
        }

        if (childCount == 0 && productionId < 0) {
            return AstNode.leaf(symbol, lexeme);
        }
        return AstNode.nonTerminal(symbol, children, productionId);
    }

    private static void writeNode(StringBuilder out, AstNode node) {
        out.append("NODE")
                .append('\t').append(nullToEmpty(node.getSymbolName()))
                .append('\t').append(nullToEmpty(node.getLexeme()))
                .append('\t').append(node.isSemanticActionNode() ? "1" : "0")
                .append('\t').append(nullToEmpty(node.getActionCode()))
                .append('\t').append(node.getProductionId())
                .append('\t').append(node.getChildren().size())
                .append(System.lineSeparator());

        for (AstNode child : node.getChildren()) {
            writeNode(out, child);
        }
    }

    private static int parseInt(String value, String label, String line) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Invalid " + label + " in action tree line: " + line, ex);
        }
    }

    private static String emptyToNull(String value) {
        return value == null || value.isEmpty() ? null : value;
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private static final class Cursor {
        private final List<String> lines;
        private int index;

        private Cursor(List<String> lines) {
            this.lines = lines;
        }

        private boolean hasNext() {
            return index < lines.size();
        }

        private String next() {
            if (!hasNext()) {
                throw new IllegalArgumentException("Unexpected end of action tree file");
            }
            return lines.get(index++);
        }
    }
}
