package com.example.compiler.ir;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;

public final class LlvmLikeTextEmitter {
    public String emit(IrGenerationResult result) {
        StringBuilder sb = new StringBuilder();

        sb.append("; symbols").append(System.lineSeparator());
        for (var symbol : result.getSymbols()) {
            sb.append("; ").append(symbol).append(System.lineSeparator());
        }
        sb.append(System.lineSeparator());

        FunctionEmitter function = null;
        for (IrInstruction instruction : result.getInstructions()) {
            switch (instruction.getOp()) {
                case FUNCTION_BEGIN -> {
                    if (function != null) {
                        function.finish(sb);
                    }
                    function = new FunctionEmitter(instruction.getResult(), instruction.getValues());
                    function.begin(sb);
                }
                case FUNCTION_END -> {
                    if (function != null) {
                        function.finish(sb);
                        function = null;
                    }
                }
                default -> {
                    if (function == null) {
                        throw new IllegalStateException("LLVM instruction emitted outside function: " + instruction);
                    }
                    function.emit(sb, instruction);
                }
            }
        }

        if (function != null) {
            function.finish(sb);
        }

        return sb.toString();
    }

    private static final class FunctionEmitter {
        private final String name;
        private final java.util.List<String> params;
        private final Map<String, String> slots = new LinkedHashMap<>();
        private final Set<String> i1Temps = new HashSet<>();
        private int tempCounter;
        private boolean currentBlockTerminated;

        private FunctionEmitter(String name, java.util.List<String> params) {
            this.name = name;
            this.params = params;
        }

        private void begin(StringBuilder sb) {
            sb.append("define i32 @").append(name).append("(");
            for (int i = 0; i < params.size(); i++) {
                if (i > 0) {
                    sb.append(", ");
                }
                sb.append("i32 %").append(params.get(i));
            }
            sb.append(") {").append(System.lineSeparator());
            sb.append("entry:").append(System.lineSeparator());

            for (String param : params) {
                ensureSlot(sb, param);
                emitLine(sb, "store i32 %" + param + ", ptr " + slots.get(param));
            }
        }

        private void finish(StringBuilder sb) {
            if (!currentBlockTerminated) {
                emitLine(sb, "ret i32 0");
            }
            sb.append("}").append(System.lineSeparator()).append(System.lineSeparator());
        }

        private void emit(StringBuilder sb, IrInstruction instruction) {
            switch (instruction.getOp()) {
                case ASSIGN -> {
                    ensureSlot(sb, instruction.getResult());
                    String value = valueAsI32(sb, instruction.getArg1());
                    emitLine(sb, "store i32 " + value + ", ptr " + slots.get(instruction.getResult()));
                    currentBlockTerminated = false;
                }
                case CALL -> {
                    String call = llvmCall(sb, instruction);
                    if (instruction.getResult() != null) {
                        emitLine(sb, "%" + instruction.getResult() + " = " + call);
                    } else {
                        emitLine(sb, call);
                    }
                    currentBlockTerminated = false;
                }
                case ADD, SUB, MUL, DIV -> {
                    String left = valueAsI32(sb, instruction.getArg1());
                    String right = valueAsI32(sb, instruction.getArg2());
                    emitLine(sb, "%" + instruction.getResult() + " = "
                            + arithmeticOp(instruction.getOp()) + " i32 " + left + ", " + right);
                    currentBlockTerminated = false;
                }
                case LT, LE, GT, GE, EQ, NE -> {
                    String left = valueAsI32(sb, instruction.getArg1());
                    String right = valueAsI32(sb, instruction.getArg2());
                    emitLine(sb, "%" + instruction.getResult() + " = icmp "
                            + comparePredicate(instruction.getOp()) + " i32 " + left + ", " + right);
                    i1Temps.add(instruction.getResult());
                    currentBlockTerminated = false;
                }
                case LABEL -> {
                    if (!currentBlockTerminated) {
                        emitLine(sb, "br label %" + instruction.getResult());
                    }
                    sb.append(instruction.getResult()).append(":").append(System.lineSeparator());
                    currentBlockTerminated = false;
                }
                case GOTO -> {
                    emitLine(sb, "br label %" + instruction.getResult());
                    currentBlockTerminated = true;
                }
                case IF_FALSE_GOTO -> {
                    String condition = valueAsI1(sb, instruction.getArg1());
                    String thenLabel = nextInternalLabel();
                    emitLine(sb, "br i1 " + condition + ", label %" + thenLabel + ", label %" + instruction.getResult());
                    sb.append(thenLabel).append(":").append(System.lineSeparator());
                    currentBlockTerminated = false;
                }
                case RETURN -> {
                    emitLine(sb, "ret i32 " + valueAsI32(sb, instruction.getArg1()));
                    currentBlockTerminated = true;
                }
                case FUNCTION_BEGIN, FUNCTION_END -> throw new IllegalStateException("Unexpected boundary: " + instruction);
            }
        }

        private void ensureSlot(StringBuilder sb, String variable) {
            if (isImmediate(variable) || isTemp(variable)) {
                return;
            }
            if (!slots.containsKey(variable)) {
                String slot = "%" + variable + ".addr";
                slots.put(variable, slot);
                emitLine(sb, slot + " = alloca i32, align 4");
            }
        }

        private String valueAsI32(StringBuilder sb, String value) {
            if (isImmediate(value)) {
                return value;
            }
            if (isTemp(value)) {
                if (i1Temps.contains(value)) {
                    String extended = nextInternalTemp();
                    emitLine(sb, extended + " = zext i1 %" + value + " to i32");
                    return extended;
                }
                return "%" + value;
            }
            ensureSlot(sb, value);
            String loaded = nextInternalTemp();
            emitLine(sb, loaded + " = load i32, ptr " + slots.get(value) + ", align 4");
            return loaded;
        }

        private String valueAsI1(StringBuilder sb, String value) {
            if (isTemp(value)) {
                if (i1Temps.contains(value)) {
                    return "%" + value;
                }
                String condition = nextInternalTemp();
                emitLine(sb, condition + " = icmp ne i32 %" + value + ", 0");
                return condition;
            }
            String i32Value = valueAsI32(sb, value);
            String condition = nextInternalTemp();
            emitLine(sb, condition + " = icmp ne i32 " + i32Value + ", 0");
            return condition;
        }

        private String llvmCall(StringBuilder sb, IrInstruction instruction) {
            StringBuilder out = new StringBuilder("call i32 @").append(instruction.getArg1()).append("(");
            for (int i = 0; i < instruction.getValues().size(); i++) {
                if (i > 0) {
                    out.append(", ");
                }
                out.append("i32 ").append(valueAsI32(sb, instruction.getValues().get(i)));
            }
            out.append(")");
            return out.toString();
        }

        private String nextInternalTemp() {
            return "%v" + (++tempCounter);
        }

        private String nextInternalLabel() {
            return "bb" + (++tempCounter);
        }

        private static void emitLine(StringBuilder sb, String line) {
            sb.append("  ").append(line).append(System.lineSeparator());
        }

        private static boolean isImmediate(String value) {
            return value != null && value.matches("-?\\d+");
        }

        private static boolean isTemp(String value) {
            return value != null && value.matches("t\\d+");
        }

        private static String arithmeticOp(IrOp op) {
            return switch (op) {
                case ADD -> "add";
                case SUB -> "sub";
                case MUL -> "mul";
                case DIV -> "sdiv";
                default -> throw new IllegalStateException("Unexpected arithmetic op: " + op);
            };
        }

        private static String comparePredicate(IrOp op) {
            return switch (op) {
                case LT -> "slt";
                case LE -> "sle";
                case GT -> "sgt";
                case GE -> "sge";
                case EQ -> "eq";
                case NE -> "ne";
                default -> throw new IllegalStateException("Unexpected compare op: " + op);
            };
        }
    }
}
