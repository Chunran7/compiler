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

/**
 * FIRST 集和可空性计算器。
 *
 * <p>它的输入是 {@link Grammar}，输出是每个符号的 FIRST 集以及每个非终结符
 * 是否可推出 ε。LR(1) closure 需要 FIRST(βa) 来决定新项目的 lookahead，
 * 因此 FIRST 集是 LR(1) 项目集构造之前的必要准备。</p>
 */
public final class FirstSetCalculator {
    private final Grammar grammar;
    private final Map<Symbol, Set<Terminal>> firstSets = new LinkedHashMap<>();
    private final Map<NonTerminal, Boolean> nullable = new LinkedHashMap<>();

    public FirstSetCalculator(Grammar grammar) {
        this.grammar = grammar;
    }

    /**
     * 使用不动点迭代计算 FIRST 集。
     *
     * <p>终结符的 FIRST 集初始化为自身；非终结符从空集开始。随后反复扫描
     * 所有产生式，把右部符号的 FIRST 传播到左部，直到本轮没有任何集合或
     * nullable 标记发生变化。</p>
     */
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

    /**
     * 计算 LR(1) closure 中的 FIRST(βa)。
     *
     * @param productionRhs 当前项目所在产生式右部
     * @param fromIndex β 的起始位置，即点后非终结符之后的位置
     * @param lookahead 如果 β 可空，就把原项目展望符 a 放入结果
     */
    public Set<Terminal> firstOfSuffix(List<Symbol> productionRhs, int fromIndex, Terminal lookahead) {
        List<Symbol> suffix = new ArrayList<>();
        for (int i = fromIndex; i < productionRhs.size(); i++) {
            suffix.add(productionRhs.get(i));
        }
        return firstOfSequence(suffix, lookahead);
    }
}
