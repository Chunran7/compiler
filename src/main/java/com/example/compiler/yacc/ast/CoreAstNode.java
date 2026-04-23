package com.example.compiler.yacc.ast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class CoreAstNode {
    private final AstKind kind;
    private final String text;
    private final List<CoreAstNode> children;

    public CoreAstNode(AstKind kind, String text, List<CoreAstNode> children) {
        this.kind = kind;
        this.text = text;
        this.children = new ArrayList<>(children);
    }

    public static CoreAstNode leaf(AstKind kind, String text) {
        return new CoreAstNode(kind, text, List.of());
    }

    public static CoreAstNode node(AstKind kind, List<CoreAstNode> children) {
        return new CoreAstNode(kind, null, children);
    }

    public static CoreAstNode node(AstKind kind, String text, List<CoreAstNode> children) {
        return new CoreAstNode(kind, text, children);
    }

    public AstKind getKind() {
        return kind;
    }

    public String getText() {
        return text;
    }

    public List<CoreAstNode> getChildren() {
        return Collections.unmodifiableList(children);
    }

    public String prettyPrint() {
        StringBuilder sb = new StringBuilder();
        prettyPrint(sb, "", true);
        return sb.toString();
    }

    private void prettyPrint(StringBuilder sb, String prefix, boolean isLast) {
        sb.append(prefix).append(isLast ? "└── " : "├── ").append(kind);
        if (text != null) {
            sb.append("(\"").append(text).append("\")");
        }
        sb.append(System.lineSeparator());
        for (int i = 0; i < children.size(); i++) {
            children.get(i).prettyPrint(sb, prefix + (isLast ? "    " : "│   "), i == children.size() - 1);
        }
    }
}
