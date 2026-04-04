package com.compiler.yacc.lr1;

import com.compiler.yacc.grammar.Grammar;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class LR1State {
    private final int id;
    private final Set<LR1Item> items;

    public LR1State(int id, Set<LR1Item> items) {
        this.id = id;
        this.items = new LinkedHashSet<>(items);
    }

    public int getId() {
        return id;
    }

    public Set<LR1Item> getItems() {
        return items;
    }

    public String format(Grammar grammar) {
        return items.stream()
                .map(item -> item.format(grammar))
                .collect(Collectors.joining("\n"));
    }
}