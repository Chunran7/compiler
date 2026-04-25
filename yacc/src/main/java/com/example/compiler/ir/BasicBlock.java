package com.example.compiler.ir;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class BasicBlock {
    private final String label;
    private final List<IrInstruction> instructions;

    public BasicBlock(String label, List<IrInstruction> instructions) {
        this.label = label;
        this.instructions = new ArrayList<>(instructions);
    }

    public String getLabel() {
        return label;
    }

    public List<IrInstruction> getInstructions() {
        return Collections.unmodifiableList(instructions);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("BasicBlock(").append(label).append(")").append(System.lineSeparator());
        for (IrInstruction instruction : instructions) {
            sb.append("  ").append(instruction).append(System.lineSeparator());
        }
        return sb.toString();
    }
}
