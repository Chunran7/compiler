package com.example.compiler.yacc.ast;

public enum AstKind {
    PROGRAM,
    MAIN_FUNCTION,
    FUNCTION_DEF,
    PARAMETER,
    BLOCK,
    DECLARATION,
    ASSIGNMENT,
    EXPRESSION_STMT,
    RETURN_STMT,
    IF_STMT,
    WHILE_STMT,
    BINARY_EXPR,
    FUNCTION_CALL,
    IDENTIFIER,
    INT_LITERAL
}