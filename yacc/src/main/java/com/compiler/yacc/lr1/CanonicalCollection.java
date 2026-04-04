package com.compiler.yacc.lr1;

import com.compiler.yacc.grammar.Symbol;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class CanonicalCollection {
    private final List<LR1State> states;
    private final Map<Integer, Map<Symbol, Integer>> transitions;

    public CanonicalCollection(List<LR1State> states,
                               Map<Integer, Map<Symbol, Integer>> transitions) {
        this.states = List.copyOf(states);

        Map<Integer, Map<Symbol, Integer>> copied = new LinkedHashMap<>();
        for (Map.Entry<Integer, Map<Symbol, Integer>> entry : transitions.entrySet()) {
            copied.put(entry.getKey(), new LinkedHashMap<>(entry.getValue()));
        }
        this.transitions = copied;
    }

    public List<LR1State> getStates() {
        return states;
    }

    public Map<Symbol, Integer> getTransitionsFrom(int stateId) {
        return transitions.getOrDefault(stateId, Collections.emptyMap());
    }

    public Integer getTarget(int fromStateId, Symbol symbol) {
        return transitions.getOrDefault(fromStateId, Collections.emptyMap()).get(symbol);
    }
}
