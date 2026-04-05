package com.compiler.yacc.ast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AstNode {
    private final String symbolName;
    private final String lexeme;
    private final List<AstNode> children;

    public AstNode(String symbolName, String lexeme, List<AstNode> children) {
        this.symbolName = symbolName;
        this.lexeme = lexeme;
        this.children = new ArrayList<>(children);
    }

    /**
     * 终结符叶子节点，例如：
     * ID("a"), NUM("123"), PLUS("+")
     */
    public static AstNode leaf(String symbolName, String lexeme) {
        return new AstNode(symbolName, lexeme, List.of());
    }

    /**
     * 非终结符内部节点，例如：
     * Expr, Stmt, Program
     */
    public static AstNode nonTerminal(String symbolName, List<AstNode> children) {
        return new AstNode(symbolName, null, children);
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
        return symbolName;
    }
}
