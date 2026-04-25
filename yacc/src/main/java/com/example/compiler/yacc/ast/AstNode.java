package com.example.compiler.yacc.ast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class AstNode {
    private final String symbolName;
    private final String lexeme;
    private final List<AstNode> children;

    private final boolean semanticActionNode;
    private final String actionCode;

    // 这一步新增的元信息
    private final int productionId;

    // 父子关系元信息
    private AstNode parent;
    private int childIndexInParent;

    // 预留语义槽，下一步真正执行 translation scheme 时会用到
    private Object semanticValue;

    private AstNode(String symbolName,
                    String lexeme,
                    List<AstNode> children,
                    boolean semanticActionNode,
                    String actionCode,
                    int productionId) {
        this.symbolName = symbolName;
        this.lexeme = lexeme;
        this.children = new ArrayList<>(children);
        this.semanticActionNode = semanticActionNode;
        this.actionCode = actionCode;
        this.productionId = productionId;

        this.parent = null;
        this.childIndexInParent = -1;
        this.semanticValue = null;

        // 自动回填父节点与孩子下标
        for (int i = 0; i < this.children.size(); i++) {
            AstNode child = this.children.get(i);
            child.parent = this;
            child.childIndexInParent = i;
        }
    }

    public static AstNode leaf(String symbolName, String lexeme) {
        return new AstNode(symbolName, lexeme, List.of(), false, null, -1);
    }

    public static AstNode nonTerminal(String symbolName, List<AstNode> children, int productionId) {
        return new AstNode(symbolName, null, children, false, null, productionId);
    }

    public static AstNode semanticAction(String symbolName, String actionCode, int productionId) {
        return new AstNode(symbolName, null, List.of(), true, actionCode, productionId);
    }

    public String getSymbolName() {
        return symbolName;
    }

    public String getLexeme() {
        return lexeme;
    }

    public List<AstNode> getChildren() {
        return Collections.unmodifiableList(children);
    }

    public boolean isLeaf() {
        return children.isEmpty();
    }

    public boolean isSemanticActionNode() {
        return semanticActionNode;
    }

    public String getActionCode() {
        return actionCode;
    }

    public int getProductionId() {
        return productionId;
    }

    public AstNode getParent() {
        return parent;
    }

    public boolean hasParent() {
        return parent != null;
    }

    public int getChildIndexInParent() {
        return childIndexInParent;
    }

    public AstNode getLeftSibling() {
        if (parent == null || childIndexInParent <= 0) {
            return null;
        }
        return parent.children.get(childIndexInParent - 1);
    }

    public AstNode getRightSibling() {
        if (parent == null || childIndexInParent < 0 || childIndexInParent + 1 >= parent.children.size()) {
            return null;
        }
        return parent.children.get(childIndexInParent + 1);
    }

    public Object getSemanticValue() {
        return semanticValue;
    }

    public void setSemanticValue(Object semanticValue) {
        this.semanticValue = semanticValue;
    }

    public void clearSemanticValue() {
        this.semanticValue = null;
    }

    public String prettyPrint() {
        StringBuilder sb = new StringBuilder();
        prettyPrint(sb, "", true);
        return sb.toString();
    }

    private void prettyPrint(StringBuilder sb, String prefix, boolean isLast) {
        sb.append(prefix);
        sb.append(isLast ? "└── " : "├── ");
        sb.append(symbolName);

        if (lexeme != null) {
            sb.append("(\"").append(lexeme).append("\")");
        }
        if (productionId >= 0) {
            sb.append(" [p=").append(productionId).append("]");
        }
        if (semanticActionNode && actionCode != null) {
            sb.append(" {action}");
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
        if (lexeme != null) {
            return symbolName + "(\"" + lexeme + "\")";
        }
        if (semanticActionNode) {
            return symbolName + "{action}";
        }
        return symbolName;
    }
}