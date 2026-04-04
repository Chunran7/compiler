package com.compiler.yacc.lr1;

import com.compiler.yacc.grammar.Grammar;
import com.compiler.yacc.grammar.Symbol;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

public class CanonicalCollectionBuilder {

    private final ClosureBuilder closureBuilder = new ClosureBuilder();
    private final GotoBuilder gotoBuilder = new GotoBuilder();

    public CanonicalCollection build(Grammar grammar) {
        List<LR1State> states = new ArrayList<>();
        Map<Integer, Map<Symbol, Integer>> transitions = new LinkedHashMap<>();

        // 用“项集”本身作为状态判重依据
        Map<Set<LR1Item>, Integer> stateIds = new LinkedHashMap<>();
        Queue<Set<LR1Item>> workQueue = new ArrayDeque<>();

        // 初始项目 [S' -> · Start, EOF]
        LR1Item startItem = new LR1Item(
                0,
                0,
                grammar.getEof()
        );

        Set<LR1Item> startKernel = new LinkedHashSet<>();
        startKernel.add(startItem);

        Set<LR1Item> startClosure = closureBuilder.closure(grammar, startKernel);
        Set<LR1Item> startKey = new LinkedHashSet<>(startClosure);

        stateIds.put(startKey, 0);
        states.add(new LR1State(0, startKey));
        workQueue.add(startKey);

        while (!workQueue.isEmpty()) {
            Set<LR1Item> currentItems = workQueue.poll();
            int currentStateId = stateIds.get(currentItems);

            // 找出当前状态里所有可能作为“点后符号”的 X
            Set<Symbol> nextSymbols = collectNextSymbols(grammar, currentItems);

            for (Symbol symbol : nextSymbols) {
                Set<LR1Item> nextItems = gotoBuilder.goTo(grammar, currentItems, symbol);
                if (nextItems.isEmpty()) {
                    continue;
                }

                Set<LR1Item> nextKey = new LinkedHashSet<>(nextItems);

                Integer nextStateId = stateIds.get(nextKey);
                if (nextStateId == null) {
                    nextStateId = states.size();
                    stateIds.put(nextKey, nextStateId);
                    states.add(new LR1State(nextStateId, nextKey));
                    workQueue.add(nextKey);
                }

                transitions
                        .computeIfAbsent(currentStateId, k -> new LinkedHashMap<>())
                        .put(symbol, nextStateId);
            }
        }

        return new CanonicalCollection(states, transitions);
    }

    private Set<Symbol> collectNextSymbols(Grammar grammar, Set<LR1Item> items) {
        Set<Symbol> result = new LinkedHashSet<>();
        for (LR1Item item : items) {
            Symbol next = item.symbolAfterDot(grammar);
            if (next != null) {
                result.add(next);
            }
        }
        return result;
    }
}