package com.example.compiler.yacc.lalr;

import com.example.compiler.yacc.grammar.Symbol;
import com.example.compiler.yacc.lr1.CanonicalCollection;
import com.example.compiler.yacc.lr1.LR1Item;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class LALRConverter {
    public CanonicalCollection convert(CanonicalCollection lr1) {
        Map<String, Integer> coreGroupIds = new LinkedHashMap<>();
        List<Set<LR1Item>> mergedStates = new ArrayList<>();
        Map<Integer, Integer> oldToNew = new LinkedHashMap<>();

        for (int oldStateId = 0; oldStateId < lr1.states().size(); oldStateId++) {
            Set<LR1Item> state = lr1.states().get(oldStateId);
            String coreKey = coreSetKey(state);
            Integer newStateId = coreGroupIds.get(coreKey);
            if (newStateId == null) {
                newStateId = mergedStates.size();
                coreGroupIds.put(coreKey, newStateId);
                mergedStates.add(new LinkedHashSet<>());
            }
            oldToNew.put(oldStateId, newStateId);
            mergedStates.get(newStateId).addAll(state);
        }

        Map<Integer, Map<Symbol, Integer>> mergedTransitions = new LinkedHashMap<>();
        for (Map.Entry<Integer, Map<Symbol, Integer>> entry : lr1.transitions().entrySet()) {
            Integer newFrom = oldToNew.get(entry.getKey());
            for (Map.Entry<Symbol, Integer> t : entry.getValue().entrySet()) {
                Integer newTo = oldToNew.get(t.getValue());
                mergedTransitions
                        .computeIfAbsent(newFrom, key -> new LinkedHashMap<>())
                        .put(t.getKey(), newTo);
            }
        }

        return new CanonicalCollection(List.copyOf(mergedStates), mergedTransitions);
    }

    private String coreSetKey(Set<LR1Item> state) {
        List<String> keys = new ArrayList<>();
        for (LR1Item item : state) {
            keys.add(item.coreKey());
        }
        keys.sort(String::compareTo);
        return String.join("|", keys);
    }
}
