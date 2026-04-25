package com.example.compiler.yacc.token;

public record Token(TokenType type, String lexeme) {
    @Override
    public String toString() {
        return type + "(\"" + lexeme + "\")";
    }
}
