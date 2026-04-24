package com.example.compiler.yacc.emitter;

import com.example.compiler.yacc.ast.AstNode;
import com.example.compiler.yacc.ast.CoreAstNode;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

public final class AstMarkdownEmitter {

    public String emitParseTree(AstNode root, String title) {
        Objects.requireNonNull(root, "root");

        StringBuilder sb = new StringBuilder();
        sb.append("# ").append(escapeMarkdown(title)).append(System.lineSeparator());
        sb.append(System.lineSeparator());

        sb.append("## 1. Text Tree").append(System.lineSeparator());
        sb.append(System.lineSeparator());
        sb.append("```text").append(System.lineSeparator());
        sb.append(root.prettyPrint());
        sb.append("```").append(System.lineSeparator());
        sb.append(System.lineSeparator());

        sb.append("## 2. Mermaid Tree").append(System.lineSeparator());
        sb.append(System.lineSeparator());
        sb.append("```mermaid").append(System.lineSeparator());
        sb.append("graph TD").append(System.lineSeparator());

        int[] counter = {0};
        emitAstNodeMermaid(root, sb, counter);

        sb.append("```").append(System.lineSeparator());
        return sb.toString();
    }

    public String emitCoreAst(CoreAstNode root, String title) {
        Objects.requireNonNull(root, "root");

        StringBuilder sb = new StringBuilder();
        sb.append("# ").append(escapeMarkdown(title)).append(System.lineSeparator());
        sb.append(System.lineSeparator());

        sb.append("## 1. Text Tree").append(System.lineSeparator());
        sb.append(System.lineSeparator());
        sb.append("```text").append(System.lineSeparator());
        sb.append(root.prettyPrint());
        sb.append("```").append(System.lineSeparator());
        sb.append(System.lineSeparator());

        sb.append("## 2. Mermaid Tree").append(System.lineSeparator());
        sb.append(System.lineSeparator());
        sb.append("```mermaid").append(System.lineSeparator());
        sb.append("graph TD").append(System.lineSeparator());

        int[] counter = {0};
        emitCoreNodeMermaid(root, sb, counter);

        sb.append("```").append(System.lineSeparator());
        return sb.toString();
    }

    public Path writeParseTree(Path outputFile, AstNode root, String title) throws IOException {
        Objects.requireNonNull(outputFile, "outputFile");
        if (outputFile.getParent() != null) {
            Files.createDirectories(outputFile.getParent());
        }
        Files.writeString(outputFile, emitParseTree(root, title));
        return outputFile;
    }

    public Path writeCoreAst(Path outputFile, CoreAstNode root, String title) throws IOException {
        Objects.requireNonNull(outputFile, "outputFile");
        if (outputFile.getParent() != null) {
            Files.createDirectories(outputFile.getParent());
        }
        Files.writeString(outputFile, emitCoreAst(root, title));
        return outputFile;
    }

    private int emitAstNodeMermaid(AstNode node, StringBuilder sb, int[] counter) {
        int currentId = counter[0]++;
        String currentName = "n" + currentId;

        sb.append("    ")
                .append(currentName)
                .append("[\"")
                .append(escapeMermaidLabel(astNodeLabel(node)))
                .append("\"]")
                .append(System.lineSeparator());

        for (AstNode child : node.getChildren()) {
            int childId = emitAstNodeMermaid(child, sb, counter);
            sb.append("    ")
                    .append(currentName)
                    .append(" --> ")
                    .append("n")
                    .append(childId)
                    .append(System.lineSeparator());
        }

        return currentId;
    }

    private int emitCoreNodeMermaid(CoreAstNode node, StringBuilder sb, int[] counter) {
        int currentId = counter[0]++;
        String currentName = "n" + currentId;

        sb.append("    ")
                .append(currentName)
                .append("[\"")
                .append(escapeMermaidLabel(coreNodeLabel(node)))
                .append("\"]")
                .append(System.lineSeparator());

        for (CoreAstNode child : node.getChildren()) {
            int childId = emitCoreNodeMermaid(child, sb, counter);
            sb.append("    ")
                    .append(currentName)
                    .append(" --> ")
                    .append("n")
                    .append(childId)
                    .append(System.lineSeparator());
        }

        return currentId;
    }

    private String astNodeLabel(AstNode node) {
        StringBuilder label = new StringBuilder();

        label.append(node.getSymbolName());

        if (node.getLexeme() != null) {
            label.append("\\nlexeme: ").append(node.getLexeme());
        }

        if (node.getProductionId() >= 0) {
            label.append("\\nproduction: ").append(node.getProductionId());
        }

        if (node.isSemanticActionNode()) {
            label.append("\\nsemantic action");
        }

        return label.toString();
    }

    private String coreNodeLabel(CoreAstNode node) {
        StringBuilder label = new StringBuilder();

        label.append(node.getKind());

        if (node.getText() != null) {
            label.append("\\ntext: ").append(node.getText());
        }

        return label.toString();
    }

    private String escapeMermaidLabel(String text) {
        if (text == null) {
            return "";
        }
        return text
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\r", "")
                .replace("\n", "\\n");
    }

    private String escapeMarkdown(String text) {
        if (text == null || text.isBlank()) {
            return "AST";
        }
        return text.replace("#", "\\#");
    }
}
