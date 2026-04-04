package com.compiler.yacc.lr1;

import com.compiler.yacc.first.FirstSetCalculator;
import com.compiler.yacc.grammar.Grammar;
import com.compiler.yacc.grammar.NonTerminal;
import com.compiler.yacc.grammar.Production;
import com.compiler.yacc.grammar.Symbol;
import com.compiler.yacc.grammar.Terminal;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ClosureBuilder {

    private final FirstSetCalculator firstSetCalculator = new FirstSetCalculator();

    /**
     * 计算一个 LR(1) 项集的 closure
     */
    public Set<LR1Item> closure(Grammar grammar, Set<LR1Item> kernelItems) {
        Set<LR1Item> closureSet = new LinkedHashSet<>(kernelItems);
        Map<NonTerminal, Set<Terminal>> firstSets = firstSetCalculator.computeFirstSets(grammar);

        boolean changed;
        do {
            changed = false;

            // 每轮遍历当前已有的所有 item
            List<LR1Item> currentItems = new ArrayList<>(closureSet);

            for (LR1Item item : currentItems) {
                Symbol next = item.symbolAfterDot(grammar);

                // 点后不是非终结符，就不用展开
                if (next == null || next.isTerminal()) {
                    continue;
                }

                NonTerminal B = (NonTerminal) next;

                // 对 [A -> α · B β, a]，取 β
                List<Symbol> beta = item.betaAfterNextSymbol(grammar);

                // 计算 FIRST(βa)
                List<Symbol> betaPlusLookahead = new ArrayList<>(beta);
                betaPlusLookahead.add(item.getLookahead());

                Set<Terminal> lookaheads =
                        firstSetCalculator.firstOfSequence(grammar, firstSets, betaPlusLookahead);

                // 对 B 的每条产生式 B -> γ
                for (Production production : grammar.getProductionsFor(B)) {
                    for (Terminal lookahead : lookaheads) {
                        LR1Item newItem = new LR1Item(production.getId(), 0, lookahead);
                        if (closureSet.add(newItem)) {
                            changed = true;
                        }
                    }
                }
            }
        } while (changed);

        return closureSet;
    }
}