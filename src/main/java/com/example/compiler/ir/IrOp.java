package com.example.compiler.ir;

public enum IrOp {
    FUNCTION_BEGIN,
    FUNCTION_END,
    ASSIGN,
    CALL,
    ADD,
    SUB,
    MUL,
    DIV,
    LT,
    LE,
    GT,
    GE,
    EQ,
    NE,
    LABEL,
    GOTO,
    IF_FALSE_GOTO,
    RETURN
}