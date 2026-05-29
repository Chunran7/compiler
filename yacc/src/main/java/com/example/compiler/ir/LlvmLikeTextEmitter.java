package com.example.compiler.ir;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;

/**
 * 三地址 IR 到 LLVM 风格文本的发射器。
 *
 * <p>输入是 IrGenerationResult，输出是可读的 LLVM-like IR 文本。它负责把变量
 * 映射到 alloca slot，把赋值转换为 store/load，把算术运算转换为 add/sub/mul/sdiv，
 * 把比较转换为 icmp，把控制流转换为 br/label，把 return 转换为 ret。</p>
 *
 * <p>注意：项目中另有 CSemanticProgramEmitter 路径，会先生成 yysemantic.c，
 * 再由该 C 程序运行时打印 LLVM IR；本类则是 Java 内部直接文本发射路径。</p>
 */
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
                    // FUNCTION_BEGIN 开启一个新的 LLVM 函数块。
                    // 如果上一个函数没有显式 FUNCTION_END，也先补全，避免输出结构损坏。
                    if (function != null) {
                        function.finish(sb);
                    }
                    function = new FunctionEmitter(instruction.getResult(), instruction.getValues());
                    function.begin(sb);
                }
                case FUNCTION_END -> {
                    // FUNCTION_END 负责补齐默认 return 并写出右花括号。
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
            // 参数在 LLVM 文本中以 SSA 形式进入函数。为了复用变量读写逻辑，
            // 进入 entry 后立即给每个参数分配栈槽并 store 一次。
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
            // 若源程序函数体没有显式 return，补一个 ret i32 0，
            // 保证输出的 LLVM-like 函数结构完整。
            if (!currentBlockTerminated) {
                emitLine(sb, "ret i32 0");
            }
            sb.append("}").append(System.lineSeparator()).append(System.lineSeparator());
        }

        private void emit(StringBuilder sb, IrInstruction instruction) {
            switch (instruction.getOp()) {
                case ASSIGN -> {
                    // 三地址赋值 target = value：
                    // target 一律映射成栈槽，右值可能是立即数、临时值或变量 load。
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
                    // 比较指令在 LLVM 中产生 i1；记录到 i1Temps，
                    // 后续若把它当作 i32 使用，需要 zext 扩展。
                    String left = valueAsI32(sb, instruction.getArg1());
                    String right = valueAsI32(sb, instruction.getArg2());
                    emitLine(sb, "%" + instruction.getResult() + " = icmp "
                            + comparePredicate(instruction.getOp()) + " i32 " + left + ", " + right);
                    i1Temps.add(instruction.getResult());
                    currentBlockTerminated = false;
                }
                case LABEL -> {
                    // LLVM 基本块不能无故落入一个新 label。若上一块没有终结指令，
                    // 自动补 br label，保证控制流显式。
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
                // 局部变量第一次出现时才分配栈槽，避免提前遍历函数体收集变量。
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
                    // 比较结果是 i1，但算术、store、return 使用 i32；
                    // 这里按需要插入 zext。
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
