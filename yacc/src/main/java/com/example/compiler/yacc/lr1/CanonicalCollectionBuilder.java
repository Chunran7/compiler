package com.example.compiler.yacc.lr1;

import com.example.compiler.yacc.first.FirstSetCalculator;
import com.example.compiler.yacc.grammar.Grammar;
import com.example.compiler.yacc.grammar.Symbol;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

public final class CanonicalCollectionBuilder {
    private final Grammar grammar;
    private final ClosureBuilder closureBuilder;

    public CanonicalCollectionBuilder(Grammar grammar, FirstSetCalculator firstSetCalculator) {
        this.grammar = grammar;
        this.closureBuilder = new ClosureBuilder(grammar, firstSetCalculator);
    }

    public CanonicalCollection build() {
        LR1Item startItem = new LR1Item(grammar.getProduction(0), 0, grammar.getEof());
        Set<LR1Item> startState = closureBuilder.closure(Set.of(startItem));

        List<Set<LR1Item>> states = new ArrayList<>();
        Map<Integer, Map<Symbol, Integer>> transitions = new LinkedHashMap<>();
        Map<Set<LR1Item>, Integer> stateIds = new LinkedHashMap<>();
        Queue<Set<LR1Item>> queue = new ArrayDeque<>();

        states.add(startState);
        stateIds.put(startState, 0);
        queue.add(startState);

        while (!queue.isEmpty()) {
            Set<LR1Item> state = queue.remove();
            int stateId = stateIds.get(state);

            LinkedHashSet<Symbol> nextSymbols = new LinkedHashSet<>();
            for (LR1Item item : state) {
                Symbol symbol = item.getSymbolAfterDot();
                if (symbol != null) {
                    nextSymbols.add(symbol);
                }
            }

            for (Symbol symbol : nextSymbols) {
                Set<LR1Item> nextState = closureBuilder.goTo(state, symbol);
                if (nextState.isEmpty()) {
                    continue;
                }

                Integer nextId = stateIds.get(nextState);
                if (nextId == null) {
                    nextId = states.size();
                    states.add(nextState);
                    stateIds.put(nextState, nextId);
                    queue.add(nextState);
                }

                transitions.computeIfAbsent(stateId, key -> new LinkedHashMap<>()).put(symbol, nextId);
            }
        }

        return new CanonicalCollection(List.copyOf(states), transitions);
    }
}
