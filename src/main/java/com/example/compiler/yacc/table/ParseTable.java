package com.example.compiler.yacc.table;

import com.example.compiler.yacc.grammar.NonTerminal;
import com.example.compiler.yacc.grammar.Terminal;

import java.util.LinkedHashMap;
import java.util.Map;

public final class ParseTable {
    private final Map<Integer, Map<Terminal, Action>> actions = new LinkedHashMap<>();
    private final Map<Integer, Map<NonTerminal, Integer>> gotos = new LinkedHashMap<>();

    public void setAction(int state, Terminal terminal, Action action) {
        Map<Terminal, Action> row = actions.computeIfAbsent(state, key -> new LinkedHashMap<>());
        Action existing = row.get(terminal);
        if (existing != null && !existing.equals(action)) {
            throw new IllegalStateException("ACTION conflict at state " + state + ", terminal " + terminal + ": " + existing + " vs " + action);
        }
        row.put(terminal, action);
    }

    public void putResolvedAction(int state, Terminal terminal, Action action) {
        actions.computeIfAbsent(state, key -> new LinkedHashMap<>()).put(terminal, action);
    }

    public Action getAction(int state, Terminal terminal) {
        return actions.getOrDefault(state, Map.of()).get(terminal);
    }

    public void setGoto(int state, NonTerminal nonTerminal, int nextState) {
        Map<NonTerminal, Integer> row = gotos.computeIfAbsent(state, key -> new LinkedHashMap<>());
        Integer existing = row.get(nonTerminal);
        if (existing != null && existing != nextState) {
            throw new IllegalStateException("GOTO conflict at state " + state + ", non-terminal " + nonTerminal + ": " + existing + " vs " + nextState);
        }
        row.put(nonTerminal, nextState);
    }

    public Integer getGoto(int state, NonTerminal nonTerminal) {
        return gotos.getOrDefault(state, Map.of()).get(nonTerminal);
    }

    public Map<Integer, Map<Terminal, Action>> actionRows() {
        Map<Integer, Map<Terminal, Action>> snapshot = new LinkedHashMap<>();
        for (Map.Entry<Integer, Map<Terminal, Action>> entry : actions.entrySet()) {
            snapshot.put(entry.getKey(), new LinkedHashMap<>(entry.getValue()));
        }
        return snapshot;
    }

    public Map<Integer, Map<NonTerminal, Integer>> gotoRows() {
        Map<Integer, Map<NonTerminal, Integer>> snapshot = new LinkedHashMap<>();
        for (Map.Entry<Integer, Map<NonTerminal, Integer>> entry : gotos.entrySet()) {
            snapshot.put(entry.getKey(), new LinkedHashMap<>(entry.getValue()));
        }
        return snapshot;
    }
}