package com.compiler.yacc.grammar;

public class NonTerminal extends Symbol {
    public NonTerminal(String name) {
        super(name);
    }

    @Override
    public boolean isTerminal() {
        return false;
    }
}
