//测试专用MiniCLexer
package com.compiler.yacc.support;

import com.compiler.yacc.token.Token;
import com.compiler.yacc.token.TokenType;

import java.util.ArrayList;
import java.util.List;

public class MiniCLexer {

    public List<Token> tokenize(String input) {
        List<Token> tokens = new ArrayList<>();
        int i = 0;

        while (i < input.length()) {
            char c = input.charAt(i);

            if (Character.isWhitespace(c)) {
                i++;
                continue;
            }

            if (i + 1 < input.length()) {
                String two = input.substring(i, i + 2);
                switch (two) {
                    case "<=" -> {
                        tokens.add(new Token(TokenType.LE, two));
                        i += 2;
                        continue;
                    }
                    case ">=" -> {
                        tokens.add(new Token(TokenType.GE, two));
                        i += 2;
                        continue;
                    }
                    case "==" -> {
                        tokens.add(new Token(TokenType.EQ, two));
                        i += 2;
                        continue;
                    }
                    case "!=" -> {
                        tokens.add(new Token(TokenType.NE, two));
                        i += 2;
                        continue;
                    }
                }
            }

            if (Character.isLetter(c) || c == '_') {
                int start = i;
                while (i < input.length() &&
                        (Character.isLetterOrDigit(input.charAt(i)) || input.charAt(i) == '_')) {
                    i++;
                }
                String word = input.substring(start, i);
                tokens.add(new Token(keywordType(word), word));
                continue;
            }

            if (Character.isDigit(c)) {
                int start = i;
                while (i < input.length() && Character.isDigit(input.charAt(i))) {
                    i++;
                }
                String num = input.substring(start, i);
                tokens.add(new Token(TokenType.NUM, num));
                continue;
            }

            switch (c) {
                case '+' -> tokens.add(new Token(TokenType.PLUS, "+"));
                case '-' -> tokens.add(new Token(TokenType.MINUS, "-"));
                case '*' -> tokens.add(new Token(TokenType.STAR, "*"));
                case '/' -> tokens.add(new Token(TokenType.SLASH, "/"));
                case '<' -> tokens.add(new Token(TokenType.LT, "<"));
                case '>' -> tokens.add(new Token(TokenType.GT, ">"));
                case '=' -> tokens.add(new Token(TokenType.ASSIGN, "="));
                case ';' -> tokens.add(new Token(TokenType.SEMI, ";"));
                case '(' -> tokens.add(new Token(TokenType.LPAREN, "("));
                case ')' -> tokens.add(new Token(TokenType.RPAREN, ")"));
                case '{' -> tokens.add(new Token(TokenType.LBRACE, "{"));
                case '}' -> tokens.add(new Token(TokenType.RBRACE, "}"));
                default -> throw new IllegalArgumentException("Unknown character: " + c);
            }
            i++;
        }

        tokens.add(new Token(TokenType.EOF, "$"));
        return tokens;
    }

    private TokenType keywordType(String word) {
        return switch (word) {
            case "int" -> TokenType.INT;
            case "main" -> TokenType.MAIN;
            case "if" -> TokenType.IF;
            case "else" -> TokenType.ELSE;
            case "while" -> TokenType.WHILE;
            case "return" -> TokenType.RETURN;
            default -> TokenType.ID;
        };
    }
}