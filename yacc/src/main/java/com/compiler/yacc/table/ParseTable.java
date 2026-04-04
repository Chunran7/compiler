package com.compiler.yacc.table;

import com.compiler.yacc.grammar.NonTerminal;
import com.compiler.yacc.grammar.Terminal;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class ParseTable {

    private final Map<Integer, Map<Terminal, Action>> actionTable = new LinkedHashMap<>();
    private final Map<Integer, Map<NonTerminal, Integer>> gotoTable = new LinkedHashMap<>();

    public void putAction(int stateId, Terminal terminal, Action action) {
        actionTable
                .computeIfAbsent(stateId, k -> new LinkedHashMap<>())
                .put(terminal, action);
    }

    public Action getAction(int stateId, Terminal terminal) {
        return actionTable.getOrDefault(stateId, Collections.emptyMap()).get(terminal);
    }

    public void putGoto(int stateId, NonTerminal nonTerminal, int targetState) {
        gotoTable
                .computeIfAbsent(stateId, k -> new LinkedHashMap<>())
                .put(nonTerminal, targetState);
    }

    public Integer getGoto(int stateId, NonTerminal nonTerminal) {
        return gotoTable.getOrDefault(stateId, Collections.emptyMap()).get(nonTerminal);
    }

    public Map<Terminal, Action> getActionsFrom(int stateId) {
        return actionTable.getOrDefault(stateId, Collections.emptyMap());
    }

    public Map<NonTerminal, Integer> getGotosFrom(int stateId) {
        return gotoTable.getOrDefault(stateId, Collections.emptyMap());
    }

    public Map<Integer, Map<Terminal, Action>> getAllActions() {
        return actionTable;
    }

    public Map<Integer, Map<NonTerminal, Integer>> getAllGotos() {
        return gotoTable;
    }
}