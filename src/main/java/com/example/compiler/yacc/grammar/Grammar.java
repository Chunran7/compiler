package com.example.compiler.yacc.grammar;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class Grammar {
    private final Map<String, Terminal> terminals = new LinkedHashMap<>();
    private final Map<String, NonTerminal> nonTerminals = new LinkedHashMap<>();
    private final List<Production> productions = new ArrayList<>();
    private final Map<NonTerminal, List<Production>> productionsByLeft = new LinkedHashMap<>();
    private final Map<String, Precedence> precedenceByTerminalName = new LinkedHashMap<>();

    private NonTerminal startSymbol;
    private NonTerminal augmentedStartSymbol;
    private Terminal eof;

    public Terminal terminal(String name) {
        return terminals.computeIfAbsent(name, Terminal::new);
    }

    public NonTerminal nonTerminal(String name) {
        return nonTerminals.computeIfAbsent(name, NonTerminal::new);
    }

    public void definePrecedence(String terminalName, int level, Associativity associativity) {
        terminal(terminalName);
        precedenceByTerminalName.put(terminalName, new Precedence(level, associativity));
    }

    public Precedence getTerminalPrecedence(String terminalName) {
        return precedenceByTerminalName.get(terminalName);
    }

    public Production addProduction(NonTerminal left, Symbol... right) {
        return addProduction(left, null, null, right);
    }

    public Production addProduction(NonTerminal left, String actionCode, Symbol... right) {
        return addProduction(left, actionCode, null, right);
    }

    public Production addProduction(NonTerminal left, String actionCode, String explicitPrecedenceTokenName, Symbol... right) {
        String precedenceTokenName = explicitPrecedenceTokenName != null && !explicitPrecedenceTokenName.isBlank()
                ? explicitPrecedenceTokenName.trim()
                : findRightmostTerminalName(List.of(right));
        Precedence precedence = precedenceTokenName == null ? null : precedenceByTerminalName.get(precedenceTokenName);
        Production production = new Production(
                productions.size(),
                left,
                List.of(right),
                actionCode,
                precedenceTokenName,
                precedence
        );
        productions.add(production);
        productionsByLeft.computeIfAbsent(left, key -> new ArrayList<>()).add(production);
        return production;
    }

    public Production addEpsilonProduction(NonTerminal left) {
        return addEpsilonProduction(left, null, null);
    }

    public Production addEpsilonProduction(NonTerminal left, String actionCode) {
        return addEpsilonProduction(left, actionCode, null);
    }

    public Production addEpsilonProduction(NonTerminal left, String actionCode, String explicitPrecedenceTokenName) {
        String precedenceTokenName = explicitPrecedenceTokenName == null || explicitPrecedenceTokenName.isBlank()
                ? null
                : explicitPrecedenceTokenName.trim();
        Precedence precedence = precedenceTokenName == null ? null : precedenceByTerminalName.get(precedenceTokenName);
        Production production = new Production(
                productions.size(),
                left,
                List.of(),
                actionCode,
                precedenceTokenName,
                precedence
        );
        productions.add(production);
        productionsByLeft.computeIfAbsent(left, key -> new ArrayList<>()).add(production);
        return production;
    }

    private String findRightmostTerminalName(List<Symbol> right) {
        for (int i = right.size() - 1; i >= 0; i--) {
            Symbol symbol = right.get(i);
            if (symbol instanceof Terminal terminal) {
                return terminal.getName();
            }
        }
        return null;
    }

    public Collection<Terminal> getTerminals() {
        return terminals.values();
    }

    public Collection<NonTerminal> getNonTerminals() {
        return nonTerminals.values();
    }

    public List<Production> getProductions() {
        return List.copyOf(productions);
    }

    public List<Production> getProductionsFor(NonTerminal left) {
        return productionsByLeft.getOrDefault(left, List.of());
    }

    public Production getProduction(int id) {
        return productions.get(id);
    }

    public Terminal getTerminal(String name) {
        return terminals.get(name);
    }

    public NonTerminal getNonTerminal(String name) {
        return nonTerminals.get(name);
    }

    public NonTerminal getStartSymbol() {
        return startSymbol;
    }

    public void setStartSymbol(NonTerminal startSymbol) {
        this.startSymbol = startSymbol;
    }

    public NonTerminal getAugmentedStartSymbol() {
        return augmentedStartSymbol;
    }

    public void setAugmentedStartSymbol(NonTerminal augmentedStartSymbol) {
        this.augmentedStartSymbol = augmentedStartSymbol;
    }

    public Terminal getEof() {
        return eof;
    }

    public void setEof(Terminal eof) {
        this.eof = eof;
    }
}