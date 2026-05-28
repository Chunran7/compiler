package com.example.compiler.ir;

import com.example.compiler.semantic.SemanticActionEngine;
import com.example.compiler.semantic.SemanticResult;
import com.example.compiler.yacc.ast.AstNode;
import com.example.compiler.yacc.runtime.ParseResult;

import java.util.Objects;

/**
 * Yacc 语法树到 IR 生成结果之间的桥接层。
 *
 * <p>输入可以是 ParseResult 或 AstNode；内部先调用 SemanticActionEngine 生成
 * Core AST、SymbolTable 和初步 IR，再交给 IrGenerator 包装为
 * IrGenerationResult。Compiler.compile() 使用该类把语法分析结果接入
 * LLVM-like 文本发射器。</p>
 */
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
