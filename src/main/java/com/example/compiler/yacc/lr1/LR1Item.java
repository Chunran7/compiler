package com.example.compiler.yacc.lr1;

import com.example.compiler.yacc.grammar.Production;
import com.example.compiler.yacc.grammar.Symbol;
import com.example.compiler.yacc.grammar.Terminal;

import java.util.Objects;

public final class LR1Item {
    private final Production production;
    private final int dotPosition;
    private final Terminal lookahead;

    public LR1Item(Production production, int dotPosition, Terminal lookahead) {
        this.production = Objects.requireNonNull(production, "production");
        this.dotPosition = dotPosition;
        this.lookahead = Objects.requireNonNull(lookahead, "lookahead");
    }

    public Production getProduction() {
        return production;
    }

    public int getDotPosition() {
        return dotPosition;
    }

    public Terminal getLookahead() {
        return lookahead;
    }

    public boolean isComplete() {
        return dotPosition >= production.getRight().size();
    }

    public Symbol getSymbolAfterDot() {
        return isComplete() ? null : production.getRight().get(dotPosition);
    }

    public LR1Item advance() {
        if (isComplete()) {
            throw new IllegalStateException("Cannot advance completed item");
        }
        return new LR1Item(production, dotPosition + 1, lookahead);
    }

    public String coreKey() {
        return production.getId() + "#" + dotPosition;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof LR1Item other)) {
            return false;
        }
        return production.getId() == other.production.getId()
                && dotPosition == other.dotPosition
                && lookahead.equals(other.lookahead);
    }

    @Override
    public int hashCode() {
        return Objects.hash(production.getId(), dotPosition, lookahead);
    }

    @Override
    public String toString() {
        return "[" + production.getLeft() + " -> " + production.getRight() + ", dot=" + dotPosition + ", " + lookahead + "]";
    }
}
