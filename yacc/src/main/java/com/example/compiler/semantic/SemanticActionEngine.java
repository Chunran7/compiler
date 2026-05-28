package com.example.compiler.semantic;

import com.example.compiler.ir.IrInstruction;
import com.example.compiler.ir.ThreeAddressIrGenerator;
import com.example.compiler.yacc.ast.AstNode;
import com.example.compiler.yacc.ast.CoreAstNode;

import java.util.List;

/**
 * Coordinates the two semantic phases used by the course project.
 *
 * <p>Compile-time actions build the Core AST and symbol table, then runtime
 * semantic actions translate dynamic behavior such as assignment, arithmetic,
 * calls, and branches into three-address IR.</p>
 */
public final class SemanticActionEngine {
    private final CompileTimeSemanticAnalyzer compileTimeAnalyzer = new CompileTimeSemanticAnalyzer();
    private final ThreeAddressIrGenerator irGenerator = new ThreeAddressIrGenerator();
    private final TranslationSchemeExecutor translationSchemeExecutor = new TranslationSchemeExecutor();

    public SemanticResult analyze(AstNode parseTreeRoot) {
        SemanticResult checked = compileTimeAnalyzer.analyze(parseTreeRoot);
        List<IrInstruction> instructions = irGenerator.generate(checked.astRoot());
        return new SemanticResult(checked.astRoot(), checked.symbolTable(), instructions);
    }

    public SemanticResult analyzeActionTree(AstNode parseTreeRoot) {
        translationSchemeExecutor.execute(parseTreeRoot);
        Object value = parseTreeRoot.getSemanticValue();
        if (!(value instanceof CoreAstNode coreRoot)) {
            throw new IllegalStateException("Translation scheme root did not produce CoreAstNode: " + value);
        }
        SemanticResult checked = compileTimeAnalyzer.analyze(coreRoot);
        List<IrInstruction> instructions = irGenerator.generate(checked.astRoot());
        return new SemanticResult(checked.astRoot(), checked.symbolTable(), instructions);
    }
}
