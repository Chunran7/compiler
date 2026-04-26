package com.example.compiler.yacc.grammar;

import java.util.Objects;

public abstract class Symbol {
    private final String name;

    protected Symbol(String name) {
        this.name = Objects.requireNonNull(name, "name");
    }

    public String getName() {
        return name;
    }

    public boolean isTerminal() {
        return this instanceof Terminal;
    }

    public boolean isNonTerminal() {
        return this instanceof NonTerminal;
    }

    @Override
    public final boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Symbol other)) {
            return false;
        }
        return getClass() == other.getClass() && name.equals(other.name);
    }

    @Override
    public final int hashCode() {
        return Objects.hash(getClass(), name);
    }

    @Override
    public String toString() {
        return name;
    }
}
