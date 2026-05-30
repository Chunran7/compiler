package com.example.compiler;

import com.example.compiler.ir.IrGenerationResult;
import com.example.compiler.semantic.SemanticResult;
import com.example.compiler.yacc.runtime.ParseResult;
import com.example.compiler.yacc.token.Token;

import java.nio.file.Path;
import java.util.List;

/**
 * 编译器一次调用的完整产出。
 */
public final class CompileResult {
    List<Token> tokens;
    ParseResult parseResult;
    SemanticResult semanticResult;
    IrGenerationResult ir;
    String irText;
    Path generatedSemanticC;
    Path llvmIrFile;
    Path executableFile;

    public List<Token> tokens()            { return tokens; }
    public ParseResult parseResult()       { return parseResult; }
    public SemanticResult semanticResult() { return semanticResult; }
    public IrGenerationResult ir()         { return ir; }
    public String irText()                 { return irText; }
    public Path generatedSemanticC()       { return generatedSemanticC; }
    public Path llvmIrFile()               { return llvmIrFile; }
    public Path executableFile()           { return executableFile; }

    public boolean isSuccess() {
        return parseResult != null && parseResult.isAccepted();
    }
}
