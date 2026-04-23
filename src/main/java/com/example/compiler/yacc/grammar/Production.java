package com.example.compiler.yacc.grammar;

import java.util.List;

public final class Production {
    private final int id;
    private final NonTerminal left;
    private final List<Symbol> right;
    private final String actionCode;
    private final String precedenceTokenName;
    private final Precedence precedence;

    public Production(int id,
                      NonTerminal left,
                      List<Symbol> right,
                      String actionCode,
                      String precedenceTokenName,
                      Precedence precedence) {
        this.id = id;
        this.left = left;
        this.right = List.copyOf(right);
        this.actionCode = actionCode == null || actionCode.isBlank() ? null : actionCode.trim();
        this.precedenceTokenName = precedenceTokenName;
        this.precedence = precedence;
    }

    public int getId() {
        return id;
    }

    public NonTerminal getLeft() {
        return left;
    }

    public List<Symbol> getRight() {
        return right;
    }

    public String getActionCode() {
        return actionCode;
    }

    public boolean hasActionCode() {
        return actionCode != null && !actionCode.isBlank();
    }

    public boolean isEpsilon() {
        return right.isEmpty();
    }

    public String getPrecedenceTokenName() {
        return precedenceTokenName;
    }

    public Precedence getPrecedence() {
        return precedence;
    }

    public boolean hasPrecedence() {
        return precedence != null;
    }

    @Override
    public String toString() {
        String rhs = right.isEmpty() ? "ε" : right.toString();
        StringBuilder sb = new StringBuilder();
        sb.append(id).append(": ").append(left).append(" -> ").append(rhs);
        if (hasActionCode()) {
            sb.append(' ').append(actionCode);
        }
        if (precedenceTokenName != null) {
            sb.append(" [prec=").append(precedenceTokenName).append(']');
        }
        return sb.toString();
    }
}