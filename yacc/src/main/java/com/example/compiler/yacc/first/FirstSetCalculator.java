package com.example.compiler.yacc.first;

import com.example.compiler.yacc.grammar.Grammar;
import com.example.compiler.yacc.grammar.NonTerminal;
import com.example.compiler.yacc.grammar.Production;
import com.example.compiler.yacc.grammar.Symbol;
import com.example.compiler.yacc.grammar.Terminal;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class FirstSetCalculator {
    private final Grammar grammar;
    private final Map<Symbol, Set<Terminal>> firstSets = new LinkedHashMap<>();
    private final Map<NonTerminal, Boolean> nullable = new LinkedHashMap<>();

    public FirstSetCalculator(Grammar grammar) {
        this.grammar = grammar;
    }

    public void compute() {
        firstSets.clear();
        nullable.clear();

        for (Terminal terminal : grammar.getTerminals()) {
            firstSets.put(terminal, new LinkedHashSet<>(Set.of(terminal)));
        }
        for (NonTerminal nonTerminal : grammar.getNonTerminals()) {
            firstSets.put(nonTerminal, new LinkedHashSet<>());
            nullable.put(nonTerminal, false);
        }

        boolean changed;
        do {
            changed = false;
            for (Production production : grammar.getProductions()) {
                NonTerminal left = production.getLeft();
                if (production.isEpsilon()) {
                    if (!nullable.get(left)) {
                        nullable.put(left, true);
                        changed = true;
                    }
                    continue;
                }

                boolean allNullable = true;
                for (Symbol symbol : production.getRight()) {
                    Set<Terminal> target = firstSets.get(left);
                    int before = target.size();
                    target.addAll(firstSets.getOrDefault(symbol, Set.of()));
                    if (target.size() != before) {
                        changed = true;
                    }

                    if (symbol instanceof Terminal) {
                        allNullable = false;
                        break;
                    }
                    if (!nullable.get((NonTerminal) symbol)) {
                        allNullable = false;
                        break;
                    }
                }

                if (allNullable && !nullable.get(left)) {
                    nullable.put(left, true);
                    changed = true;
                }
            }
        } while (changed);
    }

    public Set<Terminal> getFirst(Symbol symbol) {
        return Set.copyOf(firstSets.getOrDefault(symbol, Set.of()));
    }

    public boolean isNullable(NonTerminal nonTerminal) {
        return nullable.getOrDefault(nonTerminal, false);
    }

    public Set<Terminal> firstOfSequence(List<Symbol> symbols, Terminal fallbackLookahead) {
        LinkedHashSet<Terminal> result = new LinkedHashSet<>();
        if (symbols == null || symbols.isEmpty()) {
            result.add(fallbackLookahead);
            return result;
        }

        boolean allNullable = true;
        for (Symbol symbol : symbols) {
            result.addAll(firstSets.getOrDefault(symbol, Set.of()));
            if (symbol instanceof Terminal) {
                allNullable = false;
                break;
            }
            if (!isNullable((NonTerminal) symbol)) {
                allNullable = false;
                break;
            }
        }

        if (allNullable) {
            result.add(fallbackLookahead);
        }
        return result;
    }

    public Set<Terminal> firstOfSuffix(List<Symbol> productionRhs, int fromIndex, Terminal lookahead) {
        List<Symbol> suffix = new ArrayList<>();
        for (int i = fromIndex; i < productionRhs.size(); i++) {
            suffix.add(productionRhs.get(i));
        }
        return firstOfSequence(suffix, lookahead);
    }
}
