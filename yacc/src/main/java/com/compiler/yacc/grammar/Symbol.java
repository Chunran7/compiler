package com.compiler.yacc.grammar;

import java.util.Objects;

public abstract class Symbol {
    private final String name;

    protected Symbol(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public abstract boolean isTerminal();

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Symbol symbol)) return false;
        return Objects.equals(name, symbol.name) && this.getClass() == symbol.getClass();
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, getClass());
    }

    @Override
    public String toString() {
        return name;
    }
}
