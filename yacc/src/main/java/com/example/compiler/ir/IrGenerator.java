package com.example.compiler.ir;

import com.example.compiler.semantic.SemanticResult;

import java.util.List;
import java.util.Objects;

public final class IrGenerator {
    public IrGenerationResult generate(SemanticResult semanticResult) {
        Objects.requireNonNull(semanticResult, "semanticResult");

        List<IrInstruction> instructions = semanticResult.preliminaryIr();
        List<BasicBlock> basicBlocks = new BasicBlockBuilder().build(instructions);

        return new IrGenerationResult(
                semanticResult.symbolTable().getAllSymbols(),
                instructions,
                basicBlocks
        );
    }
}