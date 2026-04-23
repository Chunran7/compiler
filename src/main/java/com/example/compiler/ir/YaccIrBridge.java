package com.example.compiler.ir;

import com.example.compiler.semantic.SemanticActionEngine;
import com.example.compiler.semantic.SemanticResult;
import com.example.compiler.yacc.ast.AstNode;
import com.example.compiler.yacc.runtime.ParseResult;

import java.util.Objects;

public final class YaccIrBridge {
    private final SemanticActionEngine semanticEngine = new SemanticActionEngine();
    private final IrGenerator generator = new IrGenerator();

    public SemanticResult analyze(ParseResult parseResult) {
        Objects.requireNonNull(parseResult, "parseResult");
        if (!parseResult.isAccepted()) {
            throw new IllegalArgumentException("Parse failed: " + parseResult.getErrorMessage());
        }
        return analyze(parseResult.getAstRoot());
    }

    public SemanticResult analyze(AstNode parseTreeRoot) {
        Objects.requireNonNull(parseTreeRoot, "parseTreeRoot");
        return semanticEngine.analyze(parseTreeRoot);
    }

    public IrGenerationResult generate(ParseResult parseResult) {
        return generate(analyze(parseResult));
    }

    public IrGenerationResult generate(AstNode parseTreeRoot) {
        return generate(analyze(parseTreeRoot));
    }

    public IrGenerationResult generate(SemanticResult semanticResult) {
        Objects.requireNonNull(semanticResult, "semanticResult");
        return generator.generate(semanticResult);
    }
}