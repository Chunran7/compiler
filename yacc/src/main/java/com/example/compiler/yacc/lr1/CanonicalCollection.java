package com.example.compiler.yacc.lr1;

import com.example.compiler.yacc.grammar.Symbol;

import java.util.List;
import java.util.Map;
import java.util.Set;

public record CanonicalCollection(
        List<Set<LR1Item>> states,
        Map<Integer, Map<Symbol, Integer>> transitions
) {
}
