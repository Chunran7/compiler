package com.example.compiler.semantic;

public record Symbol(String name, SymbolType type, int scopeLevel, int parameterCount) {
    @Override
    public String toString() {
        if (type == SymbolType.FUNCTION) {
            return "Symbol{name='%s', type=%s, scope=%d, params=%d}".formatted(name, type, scopeLevel, parameterCount);
        }
        return "Symbol{name='%s', type=%s, scope=%d}".formatted(name, type, scopeLevel);
    }
}