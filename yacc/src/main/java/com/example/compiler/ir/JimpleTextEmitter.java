package com.example.compiler.ir;

import com.example.compiler.semantic.SemanticResult;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class JimpleTextEmitter {
    public String emit(SemanticResult semanticResult) {
        StringBuilder sb = new StringBuilder();
        sb.append("public class GeneratedProgram\n{\n");

        List<IrInstruction> instructions = semanticResult.preliminaryIr();
        List<IrInstruction> current = new ArrayList<>();
        for (IrInstruction instruction : instructions) {
            if (instruction.getOp() == IrOp.FUNCTION_BEGIN) {
                current = new ArrayList<>();
                current.add(instruction);
                continue;
            }
            if (!current.isEmpty()) {
                current.add(instruction);
            }
            if (instruction.getOp() == IrOp.FUNCTION_END && !current.isEmpty()) {
                emitFunction(sb, current);
                current = new ArrayList<>();
            }
        }

        sb.append("}\n");
        return sb.toString();
    }

    private void emitFunction(StringBuilder sb, List<IrInstruction> functionInstructions) {
        IrInstruction begin = functionInstructions.get(0);
        String name = begin.getResult();
        List<String> params = begin.getValues();

        sb.append("    public static int ").append(name).append("(");
        for (int i = 0; i < params.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append("int ").append(params.get(i));
        }
        sb.append(")\n    {\n");

        Set<String> locals = new LinkedHashSet<>();
        for (IrInstruction instruction : functionInstructions) {
            collectLocal(locals, instruction.getResult());
            collectLocal(locals, instruction.getArg1());
            collectLocal(locals, instruction.getArg2());
            for (String value : instruction.getValues()) {
                collectLocal(locals, value);
            }
        }
        locals.removeAll(params);
        locals.remove(name);

        if (!locals.isEmpty()) {
            sb.append("        int ");
            int i = 0;
            for (String local : locals) {
                if (i++ > 0) sb.append(", ");
                sb.append(local);
            }
            sb.append(";\n");
        }

        for (int i = 1; i < functionInstructions.size() - 1; i++) {
            IrInstruction instruction = functionInstructions.get(i);
            emitInstruction(sb, instruction);
        }

        sb.append("    }\n\n");
    }

    private void emitInstruction(StringBuilder sb, IrInstruction instruction) {
        switch (instruction.getOp()) {
            case ASSIGN -> sb.append("        ").append(instruction.getResult()).append(" = ")
                    .append(valueText(instruction.getArg1())).append(";\n");
            case CALL -> sb.append("        ").append(instruction.getResult()).append(" = staticinvoke <GeneratedProgram: int ")
                    .append(instruction.getArg1()).append("(").append(paramTypes(instruction.getValues())).append(")>(")
                    .append(String.join(", ", instruction.getValues())).append(");\n");
            case ADD, SUB, MUL, DIV, LT, LE, GT, GE, EQ, NE -> sb.append("        ")
                    .append(instruction.getResult()).append(" = ")
                    .append(valueText(instruction.getArg1())).append(" ")
                    .append(opText(instruction.getOp())).append(" ")
                    .append(valueText(instruction.getArg2())).append(";\n");
            case LABEL -> sb.append("     ").append(instruction.getResult()).append(":\n");
            case GOTO -> sb.append("        goto ").append(instruction.getResult()).append(";\n");
            case IF_FALSE_GOTO -> sb.append("        if ")
                    .append(valueText(instruction.getArg1())).append(" == 0 goto ")
                    .append(instruction.getResult()).append(";\n");
            case RETURN -> sb.append("        return ").append(valueText(instruction.getArg1())).append(";\n");
            default -> {
            }
        }
    }

    private void collectLocal(Set<String> locals, String value) {
        if (value == null || value.isBlank()) return;
        if (value.matches("-?\\d+")) return;
        if (value.startsWith("L")) return;
        locals.add(value);
    }

    private String valueText(String text) {
        return text == null ? "0" : text;
    }

    private String paramTypes(List<String> values) {
        if (values.isEmpty()) {
            return "";
        }
        return String.join(", ", values.stream().map(v -> "int").toList());
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
