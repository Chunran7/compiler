package com.compiler.yacc.first;

import com.compiler.yacc.grammar.Grammar;
import com.compiler.yacc.grammar.NonTerminal;
import com.compiler.yacc.grammar.Production;
import com.compiler.yacc.grammar.Symbol;
import com.compiler.yacc.grammar.Terminal;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class FirstSetCalculator {

    /**
     * 计算所有 nullable 的非终结符。
     * nullable(A) = true 表示 A =>* ε
     */
    public Set<NonTerminal> computeNullable(Grammar grammar) {
        Set<NonTerminal> nullable = new LinkedHashSet<>();
        boolean changed;

        do {
            changed = false;

            for (Production production : grammar.getProductions()) {
                NonTerminal left = production.getLeft();
                List<Symbol> right = production.getRight();

                // A -> ε
                if (right.isEmpty()) {
                    if (nullable.add(left)) {
                        changed = true;
                    }
                    continue;
                }

                // A -> X1 X2 ... Xn 且所有 Xi 都 nullable
                boolean allNullable = true;
                for (Symbol symbol : right) {
                    if (symbol.isTerminal()) {
                        allNullable = false;
                        break;
                    }

                    NonTerminal nt = (NonTerminal) symbol;
                    if (!nullable.contains(nt)) {
                        allNullable = false;
                        break;
                    }
                }

                if (allNullable) {
                    if (nullable.add(left)) {
                        changed = true;
                    }
                }
            }
        } while (changed);

        return nullable;
    }

    /**
     * 计算每个非终结符的 FIRST 集。
     * 注意：这里 FIRST 集里不显式放 ε。
     * ε 是否可推导，单独由 nullable 集合表示。
     */
    public Map<NonTerminal, Set<Terminal>> computeFirstSets(Grammar grammar) {
        Map<NonTerminal, Set<Terminal>> firstSets = new LinkedHashMap<>();
        for (NonTerminal nt : grammar.getNonTerminals()) {
            firstSets.put(nt, new LinkedHashSet<>());
        }

        Set<NonTerminal> nullable = computeNullable(grammar);
        boolean changed;

        do {
            changed = false;

            for (Production production : grammar.getProductions()) {
                NonTerminal left = production.getLeft();
                List<Symbol> right = production.getRight();

                if (right.isEmpty()) {
                    // A -> ε，不往 FIRST(A) 里放任何终结符
                    continue;
                }

                for (Symbol symbol : right) {
                    if (symbol.isTerminal()) {
                        Terminal t = (Terminal) symbol;
                        if (firstSets.get(left).add(t)) {
                            changed = true;
                        }
                        break;
                    } else {
                        NonTerminal nt = (NonTerminal) symbol;
                        if (firstSets.get(left).addAll(firstSets.get(nt))) {
                            changed = true;
                        }

                        if (!nullable.contains(nt)) {
                            break;
                        }
                    }
                }
            }
        } while (changed);

        return firstSets;
    }

    /**
     * 计算一个符号串的 FIRST。
     * 例如 FIRST(DeclInitOpt SEMI) = { ASSIGN, SEMI }
     *
     * 这里也不显式返回 ε。
     * 如果整个串都 nullable，那么返回空集合。
     * 在后面 LR(1) 中，通常会算 FIRST(βa)，其中 a 是 lookahead 终结符，
     * 所以最终通常不会丢失预测符。
     */
    public Set<Terminal> firstOfSequence(
            Grammar grammar,
            Map<NonTerminal, Set<Terminal>> firstSets,
            List<Symbol> symbols
    ) {
        Set<Terminal> result = new LinkedHashSet<>();
        Set<NonTerminal> nullable = computeNullable(grammar);

        if (symbols == null || symbols.isEmpty()) {
            return result;
        }

        for (Symbol symbol : symbols) {
            if (symbol.isTerminal()) {
                result.add((Terminal) symbol);
                break;
            } else {
                NonTerminal nt = (NonTerminal) symbol;
                result.addAll(firstSets.get(nt));

                if (!nullable.contains(nt)) {
                    break;
                }
            }
        }

        return result;
    }
}