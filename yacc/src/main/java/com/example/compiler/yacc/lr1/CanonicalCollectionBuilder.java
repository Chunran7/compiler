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

/**
 * 规范 LR(1) 项目集族构造器。
 *
 * <p>从增广开始产生式 {@code S' -> · S, EOF} 出发，用 closure 和 goto
 * 生成 LR(1) 自动机的所有状态以及状态间转移。该集合随后可以直接生成
 * LR(1) ParseTable，也可以先交给 LALRConverter 合并同心项目集。</p>
 */
public final class CanonicalCollectionBuilder {
    private final Grammar grammar;
    private final ClosureBuilder closureBuilder;

    public CanonicalCollectionBuilder(Grammar grammar, FirstSetCalculator firstSetCalculator) {
        this.grammar = grammar;
        this.closureBuilder = new ClosureBuilder(grammar, firstSetCalculator);
    }

    /**
     * 广度优先构造 LR(1) 项目集族。
     *
     * @return 包含所有项目集状态和符号转移边的 CanonicalCollection
     */
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

        // 使用 BFS 从 0 号状态开始扩展整个 LR 自动机。
        // 每弹出一个状态，就统计该状态中所有“点后符号”，这些符号就是可能的出边。
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
                // goto(I, X) 表示状态 I 读入符号 X 后到达的新项目集。
                // 如果项目集以前没出现过，就分配新状态号并放入队列继续扩展。
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
