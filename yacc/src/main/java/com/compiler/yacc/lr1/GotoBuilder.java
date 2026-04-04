package com.compiler.yacc.lr1;

import com.compiler.yacc.grammar.Grammar;
import com.compiler.yacc.grammar.Symbol;

import java.util.LinkedHashSet;
import java.util.Set;

public class GotoBuilder {

    private final ClosureBuilder closureBuilder = new ClosureBuilder();

    /**
     * 计算 goto(I, X)
     * I 是一个项集，X 是一个文法符号
     */
    public Set<LR1Item> goTo(Grammar grammar, Set<LR1Item> items, Symbol symbol) {
        Set<LR1Item> movedItems = new LinkedHashSet<>();

        for (LR1Item item : items) {
            Symbol next = item.symbolAfterDot(grammar);
            if (symbol.equals(next)) {
                movedItems.add(item.advance());
            }
        }

        if (movedItems.isEmpty()) {
            return Set.of();
        }

        return closureBuilder.closure(grammar, movedItems);
    }
}