package com.example.compiler.ir;

import com.example.compiler.semantic.Symbol;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class IrGenerationResult {
    private final List<Symbol> symbols;
    private final List<IrInstruction> instructions;
    private final List<BasicBlock> basicBlocks;

    public IrGenerationResult(List<Symbol> symbols, List<IrInstruction> instructions, List<BasicBlock> basicBlocks) {
        this.symbols = new ArrayList<>(symbols);
        this.instructions = new ArrayList<>(instructions);
        this.basicBlocks = new ArrayList<>(basicBlocks);
    }

    public List<Symbol> getSymbols() {
        return Collections.unmodifiableList(symbols);
    }

    public List<IrInstruction> getInstructions() {
        return Collections.unmodifiableList(instructions);
    }

    public List<BasicBlock> getBasicBlocks() {
        return Collections.unmodifiableList(basicBlocks);
    }
}
