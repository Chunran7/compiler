package com.example.compiler.yacc.lr1;

import com.example.compiler.yacc.first.FirstSetCalculator;
import com.example.compiler.yacc.grammar.Grammar;
import com.example.compiler.yacc.grammar.NonTerminal;
import com.example.compiler.yacc.grammar.Production;
import com.example.compiler.yacc.grammar.Symbol;
import com.example.compiler.yacc.grammar.Terminal;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class ClosureBuilder {
    private final Grammar grammar;
    private final FirstSetCalculator firstSetCalculator;

    public ClosureBuilder(Grammar grammar, FirstSetCalculator firstSetCalculator) {
        this.grammar = grammar;
        this.firstSetCalculator = firstSetCalculator;
    }

    public Set<LR1Item> closure(Set<LR1Item> seed) {
        LinkedHashSet<LR1Item> result = new LinkedHashSet<>(seed);
        boolean changed;
        do {
            changed = false;
            Set<LR1Item> snapshot = Set.copyOf(result);
            for (LR1Item item : snapshot) {
                Symbol next = item.getSymbolAfterDot();
                if (!(next instanceof NonTerminal nonTerminal)) {
                    continue;
                }

                List<Symbol> rhs = item.getProduction().getRight();
                Set<Terminal> lookaheads = firstSetCalculator.firstOfSuffix(rhs, item.getDotPosition() + 1, item.getLookahead());
                for (Production production : grammar.getProductionsFor(nonTerminal)) {
                    for (Terminal lookahead : lookaheads) {
                        LR1Item newItem = new LR1Item(production, 0, lookahead);
                        if (result.add(newItem)) {
                            changed = true;
                        }
                    }
                }
            }
        } while (changed);
        return result;
    }

    public Set<LR1Item> goTo(Set<LR1Item> state, Symbol symbol) {
        LinkedHashSet<LR1Item> moved = new LinkedHashSet<>();
        for (LR1Item item : state) {
            Symbol next = item.getSymbolAfterDot();
            if (symbol.equals(next)) {
                moved.add(item.advance());
            }
        }
        if (moved.isEmpty()) {
            return Set.of();
        }
        return closure(moved);
    }
}
