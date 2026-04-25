package com.example.compiler.ir;

public final class LlvmLikeTextEmitter {
    public String emit(IrGenerationResult result) {
        StringBuilder sb = new StringBuilder();

        sb.append("; symbols").append(System.lineSeparator());
        for (var symbol : result.getSymbols()) {
            sb.append("; ").append(symbol).append(System.lineSeparator());
        }
        sb.append(System.lineSeparator());

        boolean inFunction = false;

        for (IrInstruction instruction : result.getInstructions()) {
            switch (instruction.getOp()) {
                case FUNCTION_BEGIN -> {
                    if (inFunction) {
                        sb.append("}").append(System.lineSeparator()).append(System.lineSeparator());
                    }
                    sb.append("define i32 @")
                            .append(instruction.getResult())
                            .append("() {")
                            .append(System.lineSeparator());
                    inFunction = true;
                }
                case FUNCTION_END -> {
                    if (inFunction) {
                        sb.append("}").append(System.lineSeparator()).append(System.lineSeparator());
                        inFunction = false;
                    }
                }
                default -> sb.append("  ").append(instruction).append(System.lineSeparator());
            }
        }

        if (inFunction) {
            sb.append("}").append(System.lineSeparator());
        }

        return sb.toString();
    }
}