package com.compiler.yacc.token;
//枚举类，用来列举Token中的类型
public enum TokenType {
    INT, MAIN, IF, ELSE, WHILE, RETURN,
    ID, NUM,

    PLUS, MINUS, STAR, SLASH,
    LT, GT, LE, GE, EQ, NE,
    ASSIGN,

    SEMI,
    LPAREN, RPAREN,
    LBRACE, RBRACE,

    EOF
}