package com.compiler.yacc.ast;

public enum AstKind {
    PROGRAM,
    MAIN_FUNCTION,
    BLOCK,

    DECLARATION,
    ASSIGNMENT,
    RETURN_STMT,
    IF_STMT,
    WHILE_STMT,

    BINARY_EXPR,

    IDENTIFIER,
    INT_LITERAL
}
