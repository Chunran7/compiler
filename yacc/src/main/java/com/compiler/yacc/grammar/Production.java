package com.compiler.yacc.grammar;

import java.util.List;

public class Production {
    private final int id;
    private final NonTerminal left;
    private final List<Symbol> right;

    public Production(int id, NonTerminal left, List<Symbol> right) {
        this.id = id;
        this.left = left;
        this.right = right;
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

    public boolean isEpsilon() {
        return right.isEmpty();
    }

    @Override
    public String toString() {
        if (right.isEmpty()) {
            return id + ": " + left + " -> ε";
        }
        return id + ": " + left + " -> " + right;
    }
}