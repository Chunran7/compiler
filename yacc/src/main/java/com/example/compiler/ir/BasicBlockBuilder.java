package com.example.compiler.ir;

import java.util.ArrayList;
import java.util.List;

public final class BasicBlockBuilder {
    public List<BasicBlock> build(List<IrInstruction> instructions) {
        List<BasicBlock> blocks = new ArrayList<>();
        List<IrInstruction> current = new ArrayList<>();
        String currentLabel = "entry";
        int anonymousCounter = 0;

        for (IrInstruction instruction : instructions) {
            if (instruction.getOp() == IrOp.FUNCTION_BEGIN) {
                if (!current.isEmpty()) {
                    blocks.add(new BasicBlock(currentLabel, current));
                }
                current = new ArrayList<>();
                currentLabel = instruction.getResult();
                current.add(instruction);
                continue;
            }

            if (instruction.getOp() == IrOp.FUNCTION_END) {
                current.add(instruction);
                blocks.add(new BasicBlock(currentLabel, current));
                current = new ArrayList<>();
                currentLabel = "entry";
                continue;
            }

            if (instruction.isLabel()) {
                if (!current.isEmpty()) {
                    blocks.add(new BasicBlock(currentLabel, current));
                    current = new ArrayList<>();
                }
                currentLabel = instruction.getResult();
                current.add(instruction);
                continue;
            }

            current.add(instruction);
            if (instruction.isBlockTerminator()) {
                blocks.add(new BasicBlock(currentLabel, current));
                current = new ArrayList<>();
                currentLabel = "bb" + (++anonymousCounter);
            }
        }

        if (!current.isEmpty()) {
            blocks.add(new BasicBlock(currentLabel, current));
        }

        return blocks;
    }
}