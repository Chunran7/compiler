package com.example.compiler.test;

import com.example.compiler.yacc.token.Token;
import com.example.compiler.yacc.token.TokenType;

import java.util.ArrayList;
import java.util.List;

public final class TestSupport {
    private TestSupport() {
    }

    public static List<Token> precedenceExpressionTokens() {
        List<Token> tokens = new ArrayList<>();
        tokens.add(new Token(TokenType.NUM, "1"));
        tokens.add(new Token(TokenType.PLUS, "+"));
        tokens.add(new Token(TokenType.NUM, "2"));
        tokens.add(new Token(TokenType.STAR, "*"));
        tokens.add(new Token(TokenType.NUM, "3"));
        tokens.add(new Token(TokenType.EOF, "<EOF>"));
        return tokens;
    }

    public static List<Token> validProgramTokens() {
        List<Token> tokens = new ArrayList<>();

        // int add(int x, int y) { return x + y; }
        tokens.add(new Token(TokenType.INT, "int"));
        tokens.add(new Token(TokenType.ID, "add"));
        tokens.add(new Token(TokenType.LPAREN, "("));
        tokens.add(new Token(TokenType.INT, "int"));
        tokens.add(new Token(TokenType.ID, "x"));
        tokens.add(new Token(TokenType.COMMA, ","));
        tokens.add(new Token(TokenType.INT, "int"));
        tokens.add(new Token(TokenType.ID, "y"));
        tokens.add(new Token(TokenType.RPAREN, ")"));
        tokens.add(new Token(TokenType.LBRACE, "{"));
        tokens.add(new Token(TokenType.RETURN, "return"));
        tokens.add(new Token(TokenType.ID, "x"));
        tokens.add(new Token(TokenType.PLUS, "+"));
        tokens.add(new Token(TokenType.ID, "y"));
        tokens.add(new Token(TokenType.SEMI, ";"));
        tokens.add(new Token(TokenType.RBRACE, "}"));

        // int main() { ... }
        tokens.add(new Token(TokenType.INT, "int"));
        tokens.add(new Token(TokenType.MAIN, "main"));
        tokens.add(new Token(TokenType.LPAREN, "("));
        tokens.add(new Token(TokenType.RPAREN, ")"));
        tokens.add(new Token(TokenType.LBRACE, "{"));

        tokens.add(new Token(TokenType.INT, "int"));
        tokens.add(new Token(TokenType.ID, "a"));
        tokens.add(new Token(TokenType.SEMI, ";"));

        tokens.add(new Token(TokenType.INT, "int"));
        tokens.add(new Token(TokenType.ID, "b"));
        tokens.add(new Token(TokenType.ASSIGN, "="));
        tokens.add(new Token(TokenType.NUM, "5"));
        tokens.add(new Token(TokenType.SEMI, ";"));

        // a = add(b, 3);
        tokens.add(new Token(TokenType.ID, "a"));
        tokens.add(new Token(TokenType.ASSIGN, "="));
        tokens.add(new Token(TokenType.ID, "add"));
        tokens.add(new Token(TokenType.LPAREN, "("));
        tokens.add(new Token(TokenType.ID, "b"));
        tokens.add(new Token(TokenType.COMMA, ","));
        tokens.add(new Token(TokenType.NUM, "3"));
        tokens.add(new Token(TokenType.RPAREN, ")"));
        tokens.add(new Token(TokenType.SEMI, ";"));

        // if (a < b) a = add(a, 1); else a = add(a, b);
        tokens.add(new Token(TokenType.IF, "if"));
        tokens.add(new Token(TokenType.LPAREN, "("));
        tokens.add(new Token(TokenType.ID, "a"));
        tokens.add(new Token(TokenType.LT, "<"));
        tokens.add(new Token(TokenType.ID, "b"));
        tokens.add(new Token(TokenType.RPAREN, ")"));
        tokens.add(new Token(TokenType.ID, "a"));
        tokens.add(new Token(TokenType.ASSIGN, "="));
        tokens.add(new Token(TokenType.ID, "add"));
        tokens.add(new Token(TokenType.LPAREN, "("));
        tokens.add(new Token(TokenType.ID, "a"));
        tokens.add(new Token(TokenType.COMMA, ","));
        tokens.add(new Token(TokenType.NUM, "1"));
        tokens.add(new Token(TokenType.RPAREN, ")"));
        tokens.add(new Token(TokenType.SEMI, ";"));
        tokens.add(new Token(TokenType.ELSE, "else"));
        tokens.add(new Token(TokenType.ID, "a"));
        tokens.add(new Token(TokenType.ASSIGN, "="));
        tokens.add(new Token(TokenType.ID, "add"));
        tokens.add(new Token(TokenType.LPAREN, "("));
        tokens.add(new Token(TokenType.ID, "a"));
        tokens.add(new Token(TokenType.COMMA, ","));
        tokens.add(new Token(TokenType.ID, "b"));
        tokens.add(new Token(TokenType.RPAREN, ")"));
        tokens.add(new Token(TokenType.SEMI, ";"));

        // add(a, b);
        tokens.add(new Token(TokenType.ID, "add"));
        tokens.add(new Token(TokenType.LPAREN, "("));
        tokens.add(new Token(TokenType.ID, "a"));
        tokens.add(new Token(TokenType.COMMA, ","));
        tokens.add(new Token(TokenType.ID, "b"));
        tokens.add(new Token(TokenType.RPAREN, ")"));
        tokens.add(new Token(TokenType.SEMI, ";"));

        // while (a != b) { a = add(a, 1); }
        tokens.add(new Token(TokenType.WHILE, "while"));
        tokens.add(new Token(TokenType.LPAREN, "("));
        tokens.add(new Token(TokenType.ID, "a"));
        tokens.add(new Token(TokenType.NE, "!="));
        tokens.add(new Token(TokenType.ID, "b"));
        tokens.add(new Token(TokenType.RPAREN, ")"));
        tokens.add(new Token(TokenType.LBRACE, "{"));
        tokens.add(new Token(TokenType.ID, "a"));
        tokens.add(new Token(TokenType.ASSIGN, "="));
        tokens.add(new Token(TokenType.ID, "add"));
        tokens.add(new Token(TokenType.LPAREN, "("));
        tokens.add(new Token(TokenType.ID, "a"));
        tokens.add(new Token(TokenType.COMMA, ","));
        tokens.add(new Token(TokenType.NUM, "1"));
        tokens.add(new Token(TokenType.RPAREN, ")"));
        tokens.add(new Token(TokenType.SEMI, ";"));
        tokens.add(new Token(TokenType.RBRACE, "}"));

        tokens.add(new Token(TokenType.RETURN, "return"));
        tokens.add(new Token(TokenType.ID, "a"));
        tokens.add(new Token(TokenType.SEMI, ";"));
        tokens.add(new Token(TokenType.RBRACE, "}"));
        tokens.add(new Token(TokenType.EOF, "<EOF>"));
        return tokens;
    }

    public static List<Token> duplicateDeclarationTokens() {
        List<Token> tokens = new ArrayList<>();
        tokens.add(new Token(TokenType.INT, "int"));
        tokens.add(new Token(TokenType.MAIN, "main"));
        tokens.add(new Token(TokenType.LPAREN, "("));
        tokens.add(new Token(TokenType.RPAREN, ")"));
        tokens.add(new Token(TokenType.LBRACE, "{"));
        tokens.add(new Token(TokenType.INT, "int"));
        tokens.add(new Token(TokenType.ID, "a"));
        tokens.add(new Token(TokenType.SEMI, ";"));
        tokens.add(new Token(TokenType.INT, "int"));
        tokens.add(new Token(TokenType.ID, "a"));
        tokens.add(new Token(TokenType.SEMI, ";"));
        tokens.add(new Token(TokenType.RETURN, "return"));
        tokens.add(new Token(TokenType.NUM, "0"));
        tokens.add(new Token(TokenType.SEMI, ";"));
        tokens.add(new Token(TokenType.RBRACE, "}"));
        tokens.add(new Token(TokenType.EOF, "<EOF>"));
        return tokens;
    }

    public static List<Token> undeclaredUseTokens() {
        List<Token> tokens = new ArrayList<>();
        tokens.add(new Token(TokenType.INT, "int"));
        tokens.add(new Token(TokenType.MAIN, "main"));
        tokens.add(new Token(TokenType.LPAREN, "("));
        tokens.add(new Token(TokenType.RPAREN, ")"));
        tokens.add(new Token(TokenType.LBRACE, "{"));
        tokens.add(new Token(TokenType.ID, "a"));
        tokens.add(new Token(TokenType.ASSIGN, "="));
        tokens.add(new Token(TokenType.NUM, "1"));
        tokens.add(new Token(TokenType.SEMI, ";"));
        tokens.add(new Token(TokenType.RETURN, "return"));
        tokens.add(new Token(TokenType.NUM, "0"));
        tokens.add(new Token(TokenType.SEMI, ";"));
        tokens.add(new Token(TokenType.RBRACE, "}"));
        tokens.add(new Token(TokenType.EOF, "<EOF>"));
        return tokens;
    }

    public static List<Token> undefinedFunctionCallTokens() {
        List<Token> tokens = new ArrayList<>();
        tokens.add(new Token(TokenType.INT, "int"));
        tokens.add(new Token(TokenType.MAIN, "main"));
        tokens.add(new Token(TokenType.LPAREN, "("));
        tokens.add(new Token(TokenType.RPAREN, ")"));
        tokens.add(new Token(TokenType.LBRACE, "{"));
        tokens.add(new Token(TokenType.INT, "int"));
        tokens.add(new Token(TokenType.ID, "a"));
        tokens.add(new Token(TokenType.SEMI, ";"));
        tokens.add(new Token(TokenType.ID, "a"));
        tokens.add(new Token(TokenType.ASSIGN, "="));
        tokens.add(new Token(TokenType.ID, "foo"));
        tokens.add(new Token(TokenType.LPAREN, "("));
        tokens.add(new Token(TokenType.NUM, "1"));
        tokens.add(new Token(TokenType.RPAREN, ")"));
        tokens.add(new Token(TokenType.SEMI, ";"));
        tokens.add(new Token(TokenType.RETURN, "return"));
        tokens.add(new Token(TokenType.ID, "a"));
        tokens.add(new Token(TokenType.SEMI, ";"));
        tokens.add(new Token(TokenType.RBRACE, "}"));
        tokens.add(new Token(TokenType.EOF, "<EOF>"));
        return tokens;
    }

    public static List<Token> argumentCountMismatchTokens() {
        List<Token> tokens = new ArrayList<>();
        // int add(int x, int y) { return x + y; }
        tokens.add(new Token(TokenType.INT, "int"));
        tokens.add(new Token(TokenType.ID, "add"));
        tokens.add(new Token(TokenType.LPAREN, "("));
        tokens.add(new Token(TokenType.INT, "int"));
        tokens.add(new Token(TokenType.ID, "x"));
        tokens.add(new Token(TokenType.COMMA, ","));
        tokens.add(new Token(TokenType.INT, "int"));
        tokens.add(new Token(TokenType.ID, "y"));
        tokens.add(new Token(TokenType.RPAREN, ")"));
        tokens.add(new Token(TokenType.LBRACE, "{"));
        tokens.add(new Token(TokenType.RETURN, "return"));
        tokens.add(new Token(TokenType.ID, "x"));
        tokens.add(new Token(TokenType.PLUS, "+"));
        tokens.add(new Token(TokenType.ID, "y"));
        tokens.add(new Token(TokenType.SEMI, ";"));
        tokens.add(new Token(TokenType.RBRACE, "}"));

        // int main() { return add(1); }
        tokens.add(new Token(TokenType.INT, "int"));
        tokens.add(new Token(TokenType.MAIN, "main"));
        tokens.add(new Token(TokenType.LPAREN, "("));
        tokens.add(new Token(TokenType.RPAREN, ")"));
        tokens.add(new Token(TokenType.LBRACE, "{"));
        tokens.add(new Token(TokenType.RETURN, "return"));
        tokens.add(new Token(TokenType.ID, "add"));
        tokens.add(new Token(TokenType.LPAREN, "("));
        tokens.add(new Token(TokenType.NUM, "1"));
        tokens.add(new Token(TokenType.RPAREN, ")"));
        tokens.add(new Token(TokenType.SEMI, ";"));
        tokens.add(new Token(TokenType.RBRACE, "}"));
        tokens.add(new Token(TokenType.EOF, "<EOF>"));
        return tokens;
    }
}