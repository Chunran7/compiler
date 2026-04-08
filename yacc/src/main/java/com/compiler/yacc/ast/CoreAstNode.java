package com.compiler.yacc.ast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CoreAstNode {
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

    public boolean isLeaf() {
        return children.isEmpty();
    }

    public String prettyPrint() {
        StringBuilder sb = new StringBuilder();
        prettyPrint(sb, "", true);
        return sb.toString();
    }

    private void prettyPrint(StringBuilder sb, String prefix, boolean isLast) {
        sb.append(prefix);
        sb.append(isLast ? "└── " : "├── ");
        sb.append(kind);

        if (text != null) {
            sb.append("(\"").append(text).append("\")");
        }
        sb.append("\n");

        for (int i = 0; i < children.size(); i++) {
            boolean childLast = (i == children.size() - 1);
            children.get(i).prettyPrint(
                    sb,
                    prefix + (isLast ? "    " : "│   "),
                    childLast
            );
        }
    }

    @Override
    public String toString() {
        if (text != null) {
            return kind + "(\"" + text + "\")";
        }
        return kind.toString();
    }
}
