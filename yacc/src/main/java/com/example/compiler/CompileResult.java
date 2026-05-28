package com.example.compiler;

import com.example.compiler.ir.IrGenerationResult;
import com.example.compiler.backend.nativebackend.NativeBackendResult;
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
    Path generatedLexerC;
    Path generatedParserC;
    Path tokenFile;
    Path parseTreeFile;
    Path coreAstFile;
    Path symbolTableFile;
    Path generatedSemanticC;
    Path llvmIrFile;
    Path executableFile;
    Path jimpleFile;
    Path bytecodeOutput;
    Path commandsLogFile;
    Path pipelineTraceFile;
    Path evidenceFile;
    NativeBackendResult nativeBackendResult;

    public List<Token> tokens()            { return tokens; }
    public ParseResult parseResult()       { return parseResult; }
    public SemanticResult semanticResult() { return semanticResult; }
    public IrGenerationResult ir()         { return ir; }
    public String irText()                 { return irText; }
    public Path generatedLexerC()         { return generatedLexerC; }
    public Path generatedParserC()        { return generatedParserC; }
    public Path tokenFile()               { return tokenFile; }
    public Path parseTreeFile()           { return parseTreeFile; }
    public Path coreAstFile()            { return coreAstFile; }
    public Path symbolTableFile()        { return symbolTableFile; }
    public Path generatedSemanticC()       { return generatedSemanticC; }
    public Path llvmIrFile()               { return llvmIrFile; }
    public Path executableFile()           { return executableFile; }
    public Path jimpleFile()              { return jimpleFile; }
    public Path bytecodeOutput()          { return bytecodeOutput; }
    public Path commandsLogFile()         { return commandsLogFile; }
    public Path pipelineTraceFile()       { return pipelineTraceFile; }
    public Path evidenceFile()            { return evidenceFile; }
    public NativeBackendResult nativeBackendResult() { return nativeBackendResult; }

    public boolean isSuccess() {
        return parseResult != null && parseResult.isAccepted();
    }
}
