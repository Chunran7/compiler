package com.compiler.yacc.grammar;

import java.util.*;

public class Grammar {
    private final Map<String, Terminal> terminals = new LinkedHashMap<>();
    private final Map<String, NonTerminal> nonTerminals = new LinkedHashMap<>();
    private final List<Production> productions = new ArrayList<>();
    private final Map<NonTerminal, List<Production>> productionsByLeft = new LinkedHashMap<>();

    private NonTerminal startSymbol;
    private NonTerminal augmentedStartSymbol;
    private Terminal eof;

    public Terminal terminal(String name) {
        return terminals.computeIfAbsent(name, Terminal::new);
    }

    public NonTerminal nonTerminal(String name) {
        return nonTerminals.computeIfAbsent(name, NonTerminal::new);
    }

    public Production addProduction(NonTerminal left, Symbol... right) {
        Production p = new Production(productions.size(), left, List.of(right));
        productions.add(p);
        productionsByLeft.computeIfAbsent(left, k -> new ArrayList<>()).add(p);
        return p;
    }

    public Production addEpsilonProduction(NonTerminal left) {
        Production p = new Production(productions.size(), left, List.of());
        productions.add(p);
        productionsByLeft.computeIfAbsent(left, k -> new ArrayList<>()).add(p);
        return p;
    }

    public List<Production> getProductions() {
        return productions;
    }

    public List<Production> getProductionsFor(NonTerminal left) {
        return productionsByLeft.getOrDefault(left, List.of());
    }

    public Collection<Terminal> getTerminals() {
        return terminals.values();
    }

    public Collection<NonTerminal> getNonTerminals() {
        return nonTerminals.values();
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

    public Terminal getTerminal(String name) {
        return terminals.get(name);
    }

    public NonTerminal getNonTerminal(String name) {
        return nonTerminals.get(name);
    }
}
