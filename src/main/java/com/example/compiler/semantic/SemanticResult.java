package com.example.compiler.semantic;

import com.example.compiler.ir.IrInstruction;
import com.example.compiler.yacc.ast.CoreAstNode;

import java.util.List;
import java.util.Objects;

public record SemanticResult(
        CoreAstNode astRoot,
        SymbolTable symbolTable,
        List<IrInstruction> preliminaryIr
) {
    public SemanticResult {
        Objects.requireNonNull(astRoot, "astRoot");
        Objects.requireNonNull(symbolTable, "symbolTable");
        Objects.requireNonNull(preliminaryIr, "preliminaryIr");
        preliminaryIr = List.copyOf(preliminaryIr);
    }
}