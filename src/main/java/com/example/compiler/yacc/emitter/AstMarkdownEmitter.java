package com.example.compiler.yacc.emitter;

import com.example.compiler.yacc.ast.AstNode;
import com.example.compiler.yacc.ast.CoreAstNode;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public final class AstMarkdownEmitter {

    public String emitParseTree(AstNode root, String title) {
        Objects.requireNonNull(root, "root");

        List<AstNode> nodes = new ArrayList<>();
        IdentityHashMap<AstNode, Integer> ids = new IdentityHashMap<>();
        indexAst(root, nodes, ids);

        StringBuilder sb = new StringBuilder();

        sb.append("# ").append(escapeMarkdownTitle(title)).append(System.lineSeparator());
        sb.append(System.lineSeparator());

        sb.append("## 1. 语法树节点数据结构").append(System.lineSeparator());
        sb.append(System.lineSeparator());
        sb.append("本节以表格形式展示语法树节点的数据结构，包括节点编号、符号名、词素值、产生式编号、孩子节点以及是否为语义动作节点。")
                .append(System.lineSeparator());
        sb.append(System.lineSeparator());

        appendAstNodeTable(sb, nodes, ids);

        sb.append(System.lineSeparator());
        sb.append("## 2. 嵌入语义动作的文本语法树").append(System.lineSeparator());
        sb.append(System.lineSeparator());
        sb.append("```text").append(System.lineSeparator());
        sb.append(root.prettyPrint());
        sb.append("```").append(System.lineSeparator());
        sb.append(System.lineSeparator());

        sb.append("## 3. Mermaid 可视化语法树").append(System.lineSeparator());
        sb.append(System.lineSeparator());
        appendAstMermaid(sb, nodes, ids);

        sb.append(System.lineSeparator());
        sb.append("## 4. 语义动作节点列表").append(System.lineSeparator());
        sb.append(System.lineSeparator());
        appendSemanticActionList(sb, nodes, ids);

        return sb.toString();
    }

    public String emitCoreAst(CoreAstNode root, String title) {
        Objects.requireNonNull(root, "root");

        List<CoreAstNode> nodes = new ArrayList<>();
        IdentityHashMap<CoreAstNode, Integer> ids = new IdentityHashMap<>();
        indexCoreAst(root, nodes, ids);

        StringBuilder sb = new StringBuilder();

        sb.append("# ").append(escapeMarkdownTitle(title)).append(System.lineSeparator());
        sb.append(System.lineSeparator());

        sb.append("## 1. 核心语义 AST 数据结构").append(System.lineSeparator());
        sb.append(System.lineSeparator());
        appendCoreAstTable(sb, nodes, ids);

        sb.append(System.lineSeparator());
        sb.append("## 2. 文本形式核心语义 AST").append(System.lineSeparator());
        sb.append(System.lineSeparator());
        sb.append("```text").append(System.lineSeparator());
        sb.append(root.prettyPrint());
        sb.append("```").append(System.lineSeparator());
        sb.append(System.lineSeparator());

        sb.append("## 3. Mermaid 可视化核心语义 AST").append(System.lineSeparator());
        sb.append(System.lineSeparator());
        appendCoreAstMermaid(sb, nodes, ids);

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

    private void indexAst(AstNode node, List<AstNode> nodes, IdentityHashMap<AstNode, Integer> ids) {
        if (ids.containsKey(node)) {
            return;
        }

        int id = nodes.size();
        ids.put(node, id);
        nodes.add(node);

        for (AstNode child : node.getChildren()) {
            indexAst(child, nodes, ids);
        }
    }

    private void indexCoreAst(CoreAstNode node, List<CoreAstNode> nodes, IdentityHashMap<CoreAstNode, Integer> ids) {
        if (ids.containsKey(node)) {
            return;
        }

        int id = nodes.size();
        ids.put(node, id);
        nodes.add(node);

        for (CoreAstNode child : node.getChildren()) {
            indexCoreAst(child, nodes, ids);
        }
    }

    private void appendAstNodeTable(StringBuilder sb, List<AstNode> nodes, IdentityHashMap<AstNode, Integer> ids) {
        sb.append("| 节点ID | 节点类型 | 符号名 | 词素值 | 产生式编号 | 孩子节点 | 语义动作代码预览 |")
                .append(System.lineSeparator());
        sb.append("|---|---|---|---|---:|---|---|").append(System.lineSeparator());

        for (AstNode node : nodes) {
            String nodeId = "n" + ids.get(node);
            String nodeType = astNodeType(node);
            String children = node.getChildren().isEmpty()
                    ? "-"
                    : node.getChildren()
                    .stream()
                    .map(child -> "n" + ids.get(child))
                    .collect(Collectors.joining(", "));

            String actionPreview = node.isSemanticActionNode()
                    ? shorten(node.getActionCode(), 80)
                    : "-";

            sb.append("| ")
                    .append(nodeId)
                    .append(" | ")
                    .append(markdownCell(nodeType))
                    .append(" | ")
                    .append(markdownCell(node.getSymbolName()))
                    .append(" | ")
                    .append(markdownCell(node.getLexeme()))
                    .append(" | ")
                    .append(node.getProductionId())
                    .append(" | ")
                    .append(markdownCell(children))
                    .append(" | ")
                    .append(markdownCell(actionPreview))
                    .append(" |")
                    .append(System.lineSeparator());
        }
    }

    private void appendCoreAstTable(StringBuilder sb, List<CoreAstNode> nodes, IdentityHashMap<CoreAstNode, Integer> ids) {
        sb.append("| 节点ID | 节点类型 | 文本值 | 孩子节点 |").append(System.lineSeparator());
        sb.append("|---|---|---|---|").append(System.lineSeparator());

        for (CoreAstNode node : nodes) {
            String nodeId = "n" + ids.get(node);
            String children = node.getChildren().isEmpty()
                    ? "-"
                    : node.getChildren()
                    .stream()
                    .map(child -> "n" + ids.get(child))
                    .collect(Collectors.joining(", "));

            sb.append("| ")
                    .append(nodeId)
                    .append(" | ")
                    .append(markdownCell(node.getKind().name()))
                    .append(" | ")
                    .append(markdownCell(node.getText()))
                    .append(" | ")
                    .append(markdownCell(children))
                    .append(" |")
                    .append(System.lineSeparator());
        }
    }

    private void appendAstMermaid(StringBuilder sb, List<AstNode> nodes, IdentityHashMap<AstNode, Integer> ids) {
        sb.append("```mermaid").append(System.lineSeparator());
        sb.append("flowchart TD").append(System.lineSeparator());

        for (AstNode node : nodes) {
            int id = ids.get(node);
            String className = astMermaidClass(node);

            sb.append("    n")
                    .append(id)
                    .append("[\"")
                    .append(escapeMermaid(astMermaidLabel(node, id)))
                    .append("\"]");

            if (!className.isBlank()) {
                sb.append(":::").append(className);
            }

            sb.append(System.lineSeparator());
        }

        for (AstNode node : nodes) {
            int parentId = ids.get(node);
            for (AstNode child : node.getChildren()) {
                int childId = ids.get(child);
                sb.append("    n")
                        .append(parentId)
                        .append(" --> n")
                        .append(childId)
                        .append(System.lineSeparator());
            }
        }

        sb.append("    classDef semanticAction fill:#fff3cd,stroke:#f39c12,stroke-width:2px")
                .append(System.lineSeparator());
        sb.append("    classDef terminal fill:#e8f4fd,stroke:#2c7fb8")
                .append(System.lineSeparator());
        sb.append("    classDef nonTerminal fill:#eef7ee,stroke:#2e7d32")
                .append(System.lineSeparator());

        sb.append("```").append(System.lineSeparator());
    }

    private void appendCoreAstMermaid(StringBuilder sb, List<CoreAstNode> nodes, IdentityHashMap<CoreAstNode, Integer> ids) {
        sb.append("```mermaid").append(System.lineSeparator());
        sb.append("flowchart TD").append(System.lineSeparator());

        for (CoreAstNode node : nodes) {
            int id = ids.get(node);

            sb.append("    n")
                    .append(id)
                    .append("[\"")
                    .append(escapeMermaid(coreAstMermaidLabel(node, id)))
                    .append("\"]")
                    .append(System.lineSeparator());
        }

        for (CoreAstNode node : nodes) {
            int parentId = ids.get(node);
            for (CoreAstNode child : node.getChildren()) {
                int childId = ids.get(child);
                sb.append("    n")
                        .append(parentId)
                        .append(" --> n")
                        .append(childId)
                        .append(System.lineSeparator());
            }
        }

        sb.append("```").append(System.lineSeparator());
    }

    private void appendSemanticActionList(StringBuilder sb, List<AstNode> nodes, IdentityHashMap<AstNode, Integer> ids) {
        boolean hasAction = false;

        for (AstNode node : nodes) {
            if (!node.isSemanticActionNode()) {
                continue;
            }

            hasAction = true;
            sb.append("### ")
                    .append("n")
                    .append(ids.get(node))
                    .append(" `")
                    .append(node.getSymbolName())
                    .append("`")
                    .append(System.lineSeparator());
            sb.append(System.lineSeparator());

            sb.append("```text").append(System.lineSeparator());
            sb.append(node.getActionCode() == null ? "" : node.getActionCode());
            sb.append(System.lineSeparator());
            sb.append("```").append(System.lineSeparator());
            sb.append(System.lineSeparator());
        }

        if (!hasAction) {
            sb.append("当前语法树中没有语义动作节点。").append(System.lineSeparator());
        }
    }

    private String astNodeType(AstNode node) {
        if (node.isSemanticActionNode()) {
            return "SEMANTIC_ACTION";
        }
        if (node.isLeaf()) {
            return "TERMINAL_LEAF";
        }
        return "NON_TERMINAL";
    }

    private String astMermaidClass(AstNode node) {
        if (node.isSemanticActionNode()) {
            return "semanticAction";
        }
        if (node.isLeaf()) {
            return "terminal";
        }
        return "nonTerminal";
    }

    private String astMermaidLabel(AstNode node, int id) {
        StringBuilder label = new StringBuilder();

        label.append("n").append(id).append(": ").append(node.getSymbolName());

        if (node.getLexeme() != null) {
            label.append("\nlexeme = ").append(node.getLexeme());
        }

        if (node.getProductionId() >= 0) {
            label.append("\nproduction = ").append(node.getProductionId());
        }

        if (node.isSemanticActionNode()) {
            label.append("\nsemantic action");
            if (node.getActionCode() != null && !node.getActionCode().isBlank()) {
                label.append("\n").append(shorten(node.getActionCode(), 60));
            }
        }

        return label.toString();
    }

    private String coreAstMermaidLabel(CoreAstNode node, int id) {
        StringBuilder label = new StringBuilder();

        label.append("n").append(id).append(": ").append(node.getKind());

        if (node.getText() != null) {
            label.append("\ntext = ").append(node.getText());
        }

        return label.toString();
    }

    private String markdownCell(String text) {
        if (text == null || text.isBlank()) {
            return "-";
        }

        return text
                .replace("\\", "\\\\")
                .replace("|", "\\|")
                .replace("\r", " ")
                .replace("\n", "<br/>")
                .replace("`", "\\`");
    }

    private String escapeMarkdownTitle(String text) {
        if (text == null || text.isBlank()) {
            return "AST Markdown Visualization";
        }
        return text.replace("#", "\\#");
    }

    private String escapeMermaid(String text) {
        if (text == null) {
            return "";
        }

        return text
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("\r", "")
                .replace("\n", "<br/>");
    }

    private String shorten(String text, int maxLength) {
        if (text == null) {
            return "-";
        }

        String normalized = text
                .replace("\r", " ")
                .replace("\n", " ")
                .replaceAll("\\s+", " ")
                .trim();

        if (normalized.length() <= maxLength) {
            return normalized;
        }

        return normalized.substring(0, maxLength) + "...";
    }
}