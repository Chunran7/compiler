package com.example.compiler.ir;

public final class IrInstruction {
    private final IrOp op;
    private final String result;
    private final String arg1;
    private final String arg2;

    private IrInstruction(IrOp op, String result, String arg1, String arg2) {
        this.op = op;
        this.result = result;
        this.arg1 = arg1;
        this.arg2 = arg2;
    }

    public static IrInstruction functionBegin(String name) {
        return new IrInstruction(IrOp.FUNCTION_BEGIN, name, null, null);
    }

    public static IrInstruction functionEnd(String name) {
        return new IrInstruction(IrOp.FUNCTION_END, name, null, null);
    }

    public static IrInstruction assign(String target, String value) {
        return new IrInstruction(IrOp.ASSIGN, target, value, null);
    }

    public static IrInstruction call(String target, String functionName, String argsText) {
        return new IrInstruction(IrOp.CALL, target, functionName, argsText);
    }

    public static IrInstruction binary(IrOp op, String target, String left, String right) {
        return new IrInstruction(op, target, left, right);
    }

    public static IrInstruction label(String label) {
        return new IrInstruction(IrOp.LABEL, label, null, null);
    }

    public static IrInstruction goTo(String label) {
        return new IrInstruction(IrOp.GOTO, label, null, null);
    }

    public static IrInstruction ifFalseGoTo(String condition, String label) {
        return new IrInstruction(IrOp.IF_FALSE_GOTO, label, condition, null);
    }

    public static IrInstruction ret(String value) {
        return new IrInstruction(IrOp.RETURN, null, value, null);
    }

    public IrOp getOp() {
        return op;
    }

    public String getResult() {
        return result;
    }

    public String getArg1() {
        return arg1;
    }

    public String getArg2() {
        return arg2;
    }

    public boolean isLabel() {
        return op == IrOp.LABEL;
    }

    public boolean isFunctionBoundary() {
        return op == IrOp.FUNCTION_BEGIN || op == IrOp.FUNCTION_END;
    }

    public boolean isBlockTerminator() {
        return op == IrOp.GOTO || op == IrOp.IF_FALSE_GOTO || op == IrOp.RETURN;
    }

    @Override
    public String toString() {
        return switch (op) {
            case FUNCTION_BEGIN -> "func " + result + ":";
            case FUNCTION_END -> "endfunc " + result;
            case ASSIGN -> result + " = " + arg1;
            case CALL -> result == null
                    ? "call " + arg1 + "(" + (arg2 == null ? "" : arg2) + ")"
                    : result + " = call " + arg1 + "(" + (arg2 == null ? "" : arg2) + ")";
            case ADD, SUB, MUL, DIV, LT, LE, GT, GE, EQ, NE -> result + " = " + arg1 + " " + opText(op) + " " + arg2;
            case LABEL -> result + ":";
            case GOTO -> "goto " + result;
            case IF_FALSE_GOTO -> "ifFalse " + arg1 + " goto " + result;
            case RETURN -> "return " + arg1;
        };
    }

    private String opText(IrOp op) {
        return switch (op) {
            case ADD -> "+";
            case SUB -> "-";
            case MUL -> "*";
            case DIV -> "/";
            case LT -> "<";
            case LE -> "<=";
            case GT -> ">";
            case GE -> ">=";
            case EQ -> "==";
            case NE -> "!=";
            default -> throw new IllegalStateException("Unexpected op: " + op);
        };
    }
}