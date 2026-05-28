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

/**
 * LR(1) closure/goto 算法实现。
 *
 * <p>输入是文法和 FIRST 集；输出是 LR(1) 状态扩展结果。它由
 * {@link CanonicalCollectionBuilder} 调用，是构造规范 LR(1) 项目集族的
 * 核心算法类。</p>
 */
public final class ClosureBuilder {
    private final Grammar grammar;
    private final FirstSetCalculator firstSetCalculator;

    public ClosureBuilder(Grammar grammar, FirstSetCalculator firstSetCalculator) {
        this.grammar = grammar;
        this.firstSetCalculator = firstSetCalculator;
    }

    /**
     * 对一组项目求闭包。
     *
     * <p>如果项目形如 {@code A -> α · B β, a}，则对 B 的每条产生式
     * {@code B -> γ} 加入 {@code B -> · γ, b}，其中 b 来自 FIRST(βa)。
     * 反复执行直到不再产生新项目。</p>
     */
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

    /**
     * LR goto 操作：把 state 中点前进经过指定 symbol 的项目收集起来，
     * 再对结果求 closure，得到自动机的一条转移目标状态。
     */
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
