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

    private AstNode(String symbolName,
                    String lexeme,
                    List<AstNode> children,
                    boolean semanticActionNode,
                    String actionCode) {
        this.symbolName = symbolName;
        this.lexeme = lexeme;
        this.children = new ArrayList<>(children);
        this.semanticActionNode = semanticActionNode;
        this.actionCode = actionCode;
    }

    public static AstNode leaf(String symbolName, String lexeme) {
        return new AstNode(symbolName, lexeme, List.of(), false, null);
    }

    public static AstNode nonTerminal(String symbolName, List<AstNode> children) {
        return new AstNode(symbolName, null, children, false, null);
    }

    public static AstNode semanticAction(String symbolName, String actionCode) {
        return new AstNode(symbolName, null, List.of(), true, actionCode);
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