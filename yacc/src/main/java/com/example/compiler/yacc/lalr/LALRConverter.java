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

/**
 * LR(1) 到 LALR 的状态合并器。
 *
 * <p>LALR 的核心思想是：若两个 LR(1) 状态拥有相同 LR(0) core
 * （即产生式编号和点位置相同），就把它们合并为一个状态，并把各项目的
 * lookahead 集合取并集。这样可以显著减少 c99.y 这类大文法的状态数量，
 * 同时保持 yacc 常用的分析能力。</p>
 */
public final class LALRConverter {
    /**
     * 合并 LR(1) 项目集族。
     *
     * @param lr1 规范 LR(1) 项目集族
     * @return 状态数更少、转移已重新映射的 LALR 项目集族
     */
    public CanonicalCollection convert(CanonicalCollection lr1) {
        Map<String, Integer> coreGroupIds = new LinkedHashMap<>();
        List<Set<LR1Item>> mergedStates = new ArrayList<>();
        Map<Integer, Integer> oldToNew = new LinkedHashMap<>();

        for (int oldStateId = 0; oldStateId < lr1.states().size(); oldStateId++) {
            Set<LR1Item> state = lr1.states().get(oldStateId);
            // coreKey 只包含“产生式编号 + 点位置”，故意忽略 lookahead。
            // core 相同的 LR(1) 状态在 LALR 中合并，lookahead 集合通过 addAll 取并集。
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
                // 状态合并后，原来的边 oldFrom --X--> oldTo 必须重映射为
                // newFrom --X--> newTo，否则后续 ParseTable 会引用不存在的旧状态号。
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
