package com.example.compiler.semantic;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class SymbolTable {
    private final Deque<Map<String, Symbol>> scopes = new ArrayDeque<>();
    private final Map<String, Symbol> functions = new LinkedHashMap<>();
    private final List<Symbol> allSymbols = new ArrayList<>();

    public SymbolTable() {
        enterScope();
    }

    public void enterScope() {
        scopes.push(new LinkedHashMap<>());
    }

    public void exitScope() {
        if (scopes.isEmpty()) {
            throw new IllegalStateException("No scope to exit");
        }
        scopes.pop();
    }

    public int currentScopeLevel() {
        return scopes.size() - 1;
    }

    public Symbol declare(String name, SymbolType type) {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(type, "type");
        if (type != SymbolType.INT) {
            throw new IllegalArgumentException("declare() is for variable declarations only");
        }
        Map<String, Symbol> current = scopes.peek();
        if (current == null) {
            throw new IllegalStateException("No active scope");
        }
        if (current.containsKey(name)) {
            throw new SemanticException("Duplicate declaration in same scope: " + name);
        }
        Symbol symbol = new Symbol(name, type, currentScopeLevel(), -1);
        current.put(name, symbol);
        allSymbols.add(symbol);
        return symbol;
    }

    public Symbol declareFunction(String name, int parameterCount) {
        Objects.requireNonNull(name, "name");
        if (functions.containsKey(name)) {
            throw new SemanticException("Duplicate function definition: " + name);
        }
        Symbol symbol = new Symbol(name, SymbolType.FUNCTION, 0, parameterCount);
        functions.put(name, symbol);
        allSymbols.add(symbol);
        return symbol;
    }

    public Symbol resolve(String name) {
        for (Map<String, Symbol> scope : scopes) {
            Symbol symbol = scope.get(name);
            if (symbol != null) {
                return symbol;
            }
        }
        return null;
    }

    public Symbol resolveFunction(String name) {
        return functions.get(name);
    }

    public List<Symbol> getAllSymbols() {
        return Collections.unmodifiableList(allSymbols);
    }

    public String prettyPrint() {
        StringBuilder sb = new StringBuilder();
        for (Symbol symbol : allSymbols) {
            sb.append(symbol).append(System.lineSeparator());
        }
        return sb.toString();
    }
}