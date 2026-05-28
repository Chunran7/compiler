package com.example.compiler;

import com.example.compiler.ir.IrGenerationResult;
import com.example.compiler.ir.JimpleTextEmitter;
import com.example.compiler.ir.LlvmLikeTextEmitter;
import com.example.compiler.ir.YaccIrBridge;
import com.example.compiler.lex.CLexerToolchainEmitter;
import com.example.compiler.lex.GeneratedLexer;
import com.example.compiler.semantic.CompileTimeSemanticAnalyzer;
import com.example.compiler.semantic.SemanticActionEngine;
import com.example.compiler.semantic.SemanticResult;
import com.example.compiler.semantic.action.C99SubsetSemanticActions;
import com.example.compiler.semantic.emitter.CSemanticProgramEmitter;
import com.example.compiler.semantic.emitter.SootInvoker;
import com.example.compiler.yacc.ast.AstNode;
import com.example.compiler.yacc.ast.AstTreeCodec;
import com.example.compiler.yacc.emitter.CParserProgramEmitter;
import com.example.compiler.yacc.generator.SeuYaccGenerator;
import com.example.compiler.yacc.runtime.ParseResult;
import com.example.compiler.yacc.runtime.ParserDriver;
import com.example.compiler.yacc.token.Token;
import com.example.compiler.yacc.token.TokenType;

import java.io.*;
import java.nio.file.*;
import java.util.*;

/**
 * 编译器统一入口：源码 → 词法 → 语法 → 语义 → IR
 *
 * <pre>
 * Compiler compiler = new Compiler();
 * CompilerResult result = compiler.compile("int main() { return 0; }");
 * System.out.println(result.getIrText());
 * </pre>
 */
public final class Compiler {

    private final String grammarPath;

    public Compiler() {
        this("resources/c99.y");
    }

    public Compiler(String grammarPath) {
        this.grammarPath = grammarPath;
    }

    // ── 入口 1: 源码字符串 ──

    public CompileResult compile(String source) {
        return compile(new StringReader(source));
    }

    // ── 入口 2: Reader ──

    public CompileResult compile(Reader source) {
        CompileResult result = new CompileResult();

        // 1. 词法分析
        List<Token> tokens = lex(source);
        result.tokens = tokens;

        // 2. 语法分析
        ParseResult parseResult = parse(tokens);
        result.parseResult = parseResult;
        if (!parseResult.isAccepted()) {
            return result; // 语法错误，后续阶段跳过
        }

        // 3. 语义分析 + IR 生成
        YaccIrBridge bridge = new YaccIrBridge();
        SemanticResult semanticResult = bridge.analyze(parseResult);
        result.semanticResult = semanticResult;

        // 4. IR 优化后生成
        IrGenerationResult ir = bridge.generate(parseResult);
        result.ir = ir;
        result.irText = new LlvmLikeTextEmitter().emit(ir);

        return result;
    }

    public CompileResult compileViaGeneratedC(String source) throws IOException, InterruptedException {
        return compileViaGeneratedC(new StringReader(source), Path.of("generated", "semantic"));
    }

    public CompileResult compileViaGeneratedC(Reader source, Path outputDir) throws IOException, InterruptedException {
        return compileViaGeneratedC(source, outputDir, null, null);
    }

    public CompileResult compileViaGeneratedC(Reader source,
                                              Path semanticOutputDir,
                                              Path llvmIrFile,
                                              Path executableFile) throws IOException, InterruptedException {
        CompileResult result = new CompileResult();

        List<Token> tokens = lex(source);
        result.tokens = tokens;

        ParseResult parseResult = parse(tokens);
        result.parseResult = parseResult;
        if (!parseResult.isAccepted()) {
            return result;
        }

        CompileTimeSemanticAnalyzer analyzer = new CompileTimeSemanticAnalyzer();
        SemanticResult semanticResult = analyzer.analyze(parseResult.getAstRoot());
        result.semanticResult = semanticResult;

        Files.createDirectories(semanticOutputDir);
        Path cFile = semanticOutputDir.resolve("yysemantic.c");
        Path executable = semanticOutputDir.resolve("yysemantic");
        Files.writeString(cFile, new CSemanticProgramEmitter().emit(semanticResult.astRoot()));
        result.generatedSemanticC = cFile;

        runProcess(List.of("gcc", "-std=c99", "-Wall", "-Wextra", "-o", executable.toString(), cFile.toString()));
        result.irText = runProcess(List.of(executable.toAbsolutePath().toString()));

        if (llvmIrFile != null || executableFile != null) {
            Path llFile = llvmIrFile == null ? Path.of("generated", "final", "program.ll") : llvmIrFile;
            if (llFile.getParent() != null) {
                Files.createDirectories(llFile.getParent());
            }
            Files.writeString(llFile, result.irText);
            result.llvmIrFile = llFile;

        }
        return result;
    }

    public CompileResult compileStrictFlowchart(String source) throws IOException, InterruptedException {
        return compileStrictFlowchart(Path.of("resources", "c99.l"),
                Path.of("resources", "c99.y"),
                new StringReader(source),
                Path.of("generated", "strict"),
                null, null, null, null);
    }

    public CompileResult compileStrictFlowchart(Reader source,
                                                Path outputRoot,
                                                Path llvmIrFile,
                                                Path executableFile,
                                                Path jimpleFile,
                                                Path bytecodeOutput) throws IOException, InterruptedException {
        return compileStrictFlowchart(
                Path.of("resources", "c99.l"),
                Path.of("resources", "c99.y"),
                source,
                outputRoot,
                llvmIrFile,
                executableFile,
                jimpleFile,
                bytecodeOutput
        );
    }

    public CompileResult compileStrictFlowchart(Path lexFile,
                                                Path yaccFile,
                                                Reader source,
                                                Path outputRoot,
                                                Path llvmIrFile,
                                                Path executableFile,
                                                Path jimpleFile,
                                                Path bytecodeOutput) throws IOException, InterruptedException {
        Files.createDirectories(outputRoot);
        Path sourceFile = outputRoot.resolve("input.c");
        Files.writeString(sourceFile, readAll(source));
        return compileStrictFlowchart(lexFile, yaccFile, sourceFile, outputRoot, llvmIrFile, executableFile, jimpleFile, bytecodeOutput);
    }

    public CompileResult compileStrictFlowchart(Path lexFile,
                                                Path yaccFile,
                                                Path sourceFile,
                                                Path outputRoot,
                                                Path llvmIrFile,
                                                Path executableFile,
                                                Path jimpleFile,
                                                Path bytecodeOutput) throws IOException, InterruptedException {
        CompileResult result = new CompileResult();
        Files.createDirectories(outputRoot);
        deleteLegacyRootArtifacts(outputRoot);
        Path inputDir = outputRoot.resolve("00-input");
        Path lexDir = outputRoot.resolve("01-lex");
        Path yaccDir = outputRoot.resolve("02-yacc");
        Path semanticDir = outputRoot.resolve("03-semantic");
        Path irDir = outputRoot.resolve("04-ir");
        Path sootDir = outputRoot.resolve("05-soot");
        Files.createDirectories(inputDir);
        Files.createDirectories(lexDir);
        Files.createDirectories(yaccDir);
        Files.createDirectories(semanticDir);
        Files.createDirectories(irDir);
        Files.createDirectories(sootDir);

        Path commandsLog = outputRoot.resolve("commands.log");
        Files.deleteIfExists(commandsLog);
        result.commandsLogFile = commandsLog;
        List<TraceEntry> trace = new ArrayList<>();

        Path inputLex = inputDir.resolve("c99.l");
        Path inputYacc = inputDir.resolve("c99.y");
        Path inputSource = inputDir.resolve("test.c");
        Files.copy(lexFile, inputLex, StandardCopyOption.REPLACE_EXISTING);
        Files.copy(yaccFile, inputYacc, StandardCopyOption.REPLACE_EXISTING);
        Files.copy(sourceFile, inputSource, StandardCopyOption.REPLACE_EXISTING);
        addTrace(trace, "copy-inputs",
                List.of(lexFile.toString(), yaccFile.toString(), sourceFile.toString()),
                List.of(inputLex.toString(), inputYacc.toString(), inputSource.toString()),
                null, null, true, null, 0);

        Path lexerC = lexDir.resolve("yylex.c");
        Path lexerExe = lexDir.resolve("yylex");
        long start = System.nanoTime();
        emitLexerProgram(inputLex, lexerC);
        result.generatedLexerC = lexerC;
        addTrace(trace, "lex-generate-yylex-c",
                List.of(inputLex.toString()),
                List.of(lexerC.toString()),
                "SeuLexParser / NFA / DFA / Minimize / CLexerProgramEmitter",
                null, true, null, elapsedMs(start));
        runProcessLogged(
                List.of("gcc", "-std=c99", "-Wall", "-Wextra", "-o", "yylex", "yylex.c"),
                lexDir,
                commandsLog,
                "gcc yylex.c -o yylex",
                trace,
                "lex-compile-yylex",
                List.of(lexerC.toString()),
                List.of(lexerExe.toString()),
                false
        );

        Path tokenFile = lexDir.resolve("tokens.txt");
        runProcessLogged(
                List.of(lexerExe.toAbsolutePath().toString(), inputSource.toAbsolutePath().toString(), tokenFile.toAbsolutePath().toString()),
                lexDir,
                commandsLog,
                "./yylex ../00-input/test.c tokens.txt",
                trace,
                "lex-run-yylex",
                List.of(inputSource.toString(), lexerExe.toString()),
                List.of(tokenFile.toString()),
                false
        );
        result.tokenFile = tokenFile;

        Path parserC = yaccDir.resolve("yyparse.c");
        Path parserExe = yaccDir.resolve("yyparse");
        start = System.nanoTime();
        emitParserProgram(inputYacc, parserC);
        result.generatedParserC = parserC;
        addTrace(trace, "yacc-generate-yyparse-c",
                List.of(inputYacc.toString()),
                List.of(parserC.toString()),
                "YaccParser / LR(1) / LALR / ParseTable / CParserProgramEmitter",
                null, true, null, elapsedMs(start));
        runProcessLogged(
                List.of("gcc", "-std=c99", "-Wall", "-Wextra", "-o", "yyparse", "yyparse.c"),
                yaccDir,
                commandsLog,
                "gcc yyparse.c -o yyparse",
                trace,
                "yacc-compile-yyparse",
                List.of(parserC.toString()),
                List.of(parserExe.toString()),
                false
        );

        Path treeFile = yaccDir.resolve("action-tree.txt");
        runProcessLogged(
                List.of(parserExe.toAbsolutePath().toString(), tokenFile.toAbsolutePath().toString(), treeFile.toAbsolutePath().toString()),
                yaccDir,
                commandsLog,
                "./yyparse ../01-lex/tokens.txt action-tree.txt",
                trace,
                "yacc-run-yyparse",
                List.of(parserExe.toString(), tokenFile.toString()),
                List.of(treeFile.toString()),
                false
        );
        result.parseTreeFile = treeFile;

        start = System.nanoTime();
        AstNode actionTree = new AstTreeCodec().read(treeFile);
        result.parseResult = ParseResult.success(List.of(), actionTree);
        SemanticActionEngine semanticEngine = new SemanticActionEngine();
        SemanticResult semanticResult = semanticEngine.analyzeActionTree(actionTree);
        result.semanticResult = semanticResult;
        result.ir = new YaccIrBridge().generate(semanticResult);
        Path coreAstFile = semanticDir.resolve("core-ast.txt");
        Files.writeString(coreAstFile, semanticResult.astRoot().prettyPrint());
        result.coreAstFile = coreAstFile;
        Path symbolTableFile = semanticDir.resolve("symbol-table.txt");
        Files.writeString(symbolTableFile, semanticResult.symbolTable().prettyPrint());
        result.symbolTableFile = symbolTableFile;
        addTrace(trace, "semantic-action-tree-to-core-ast",
                List.of(treeFile.toString()),
                List.of(coreAstFile.toString()),
                "AstTreeCodec / SemanticActionEngine / TranslationSchemeExecutor",
                null, true, null, elapsedMs(start));
        addTrace(trace, "semantic-check-symbol-table",
                List.of(coreAstFile.toString()),
                List.of(symbolTableFile.toString()),
                "CompileTimeSemanticAnalyzer / SymbolTable",
                null, true, null, 0);

        Path semanticC = semanticDir.resolve("yysemantic.c");
        Path semanticExe = semanticDir.resolve("yysemantic");
        start = System.nanoTime();
        Files.writeString(semanticC, new CSemanticProgramEmitter().emit(semanticResult.astRoot()));
        result.generatedSemanticC = semanticC;
        addTrace(trace, "semantic-generate-yysemantic-c",
                List.of(coreAstFile.toString()),
                List.of(semanticC.toString()),
                "CSemanticProgramEmitter",
                null, true, null, elapsedMs(start));
        runProcessLogged(
                List.of("gcc", "-std=c99", "-Wall", "-Wextra", "-o", "yysemantic", "yysemantic.c"),
                semanticDir,
                commandsLog,
                "gcc yysemantic.c -o yysemantic",
                trace,
                "semantic-compile-yysemantic",
                List.of(semanticC.toString()),
                List.of(semanticExe.toString()),
                false
        );
        String semanticOutput = runProcessLogged(
                List.of(semanticExe.toAbsolutePath().toString()),
                semanticDir,
                commandsLog,
                "./yysemantic",
                trace,
                "semantic-run-yysemantic",
                List.of(semanticExe.toString()),
                List.of(irDir.resolve("output.ll").toString()),
                false
        );
        result.irText = semanticOutput;

        Path llFile = llvmIrFile == null ? irDir.resolve("output.ll") : llvmIrFile;
        if (llFile.getParent() != null) {
            Files.createDirectories(llFile.getParent());
        }
        Files.writeString(llFile, result.irText);
        result.llvmIrFile = llFile;
        addTrace(trace, "ir-write-llvm",
                List.of(semanticExe.toString()),
                List.of(llFile.toString()),
                "yysemantic stdout",
                null, true, null, 0);

        Path actualJimpleFile = jimpleFile == null ? irDir.resolve("output.jimple") : jimpleFile;
        if (actualJimpleFile.getParent() != null) {
            Files.createDirectories(actualJimpleFile.getParent());
        }
        start = System.nanoTime();
        Files.writeString(actualJimpleFile, new JimpleTextEmitter().emit(semanticResult));
        result.jimpleFile = actualJimpleFile;
        addTrace(trace, "ir-write-jimple",
                List.of(coreAstFile.toString()),
                List.of(actualJimpleFile.toString()),
                "JimpleTextEmitter",
                null, true, null, elapsedMs(start));

        String sootJar = System.getenv("SOOT_JAR");
        boolean sootAvailable = sootJar != null && !sootJar.isBlank();
        addTrace(trace, "soot-detect", List.of(actualJimpleFile.toString()), List.of(),
                "check SOOT_JAR", null, true, sootAvailable ? null : "SOOT_JAR not available", 0);
        if (bytecodeOutput != null) {
            Path sootOutput = new SootInvoker().invokeIfAvailable(actualJimpleFile, bytecodeOutput);
            result.bytecodeOutput = sootOutput;
            if (sootOutput == null) {
                Path skipped = sootDir.resolve("soot-skipped.txt");
                Files.writeString(skipped, "SOOT_JAR not available" + System.lineSeparator());
                addTrace(trace, "soot-skipped", List.of(actualJimpleFile.toString()), List.of(skipped.toString()),
                        "SootInvoker", null, true, "SOOT_JAR not available", 0);
            } else {
                addTrace(trace, "soot-run", List.of(actualJimpleFile.toString()), List.of(sootOutput.toString()),
                        "SootInvoker", null, true, null, 0);
            }
        } else {
            Path skipped = sootDir.resolve("soot-skipped.txt");
            Files.writeString(skipped, "Soot not requested" + System.lineSeparator());
            addTrace(trace, "soot-skipped", List.of(actualJimpleFile.toString()), List.of(skipped.toString()),
                    "SootInvoker", null, true, "Soot not requested", 0);
        }

        Path traceFile = outputRoot.resolve("pipeline-trace.json");
        Files.writeString(traceFile, traceToJson(trace));
        result.pipelineTraceFile = traceFile;
        Path evidenceFile = outputRoot.resolve("FLOWCHART_EVIDENCE.md");
        Files.writeString(evidenceFile, evidenceMarkdown(outputRoot));
        result.evidenceFile = evidenceFile;
        return result;
    }

    private static void deleteLegacyRootArtifacts(Path outputRoot) throws IOException {
        for (String fileName : List.of(
                "yylex.c", "yyparse.c", "yysemantic.c",
                "yylex", "yyparse", "yysemantic",
                "yylex.exe", "yyparse.exe", "yysemantic.exe",
                "tokens.txt", "action-tree.txt", "core-ast.txt", "core-ast.json",
                "symbol-table.txt", "output.ll", "output.jimple")) {
            Files.deleteIfExists(outputRoot.resolve(fileName));
        }
        Path sootDir = outputRoot.resolve("05-soot");
        for (String fileName : List.of("soot-skipped.txt")) {
            Files.deleteIfExists(sootDir.resolve(fileName));
        }
        Path nativeDir = outputRoot.resolve("06-native");
        for (String fileName : List.of("validate.o", "output.s", "output.o", "native-executable",
                "native-backend-trace.json", "native-backend-report.md")) {
            Files.deleteIfExists(nativeDir.resolve(fileName));
        }
    }

    // ── 入口 3: 文件 ──

    public CompileResult compileFile(Path file) throws IOException {
        return compile(Files.readString(file));
    }

    public CompileResult compileFileViaGeneratedC(Path file) throws IOException, InterruptedException {
        return compileViaGeneratedC(Files.newBufferedReader(file), Path.of("generated", "semantic"));
    }

    public CompileResult compileFileViaGeneratedC(Path file,
                                                  Path llvmIrFile,
                                                  Path executableFile) throws IOException, InterruptedException {
        return compileViaGeneratedC(
                Files.newBufferedReader(file),
                Path.of("generated", "semantic"),
                llvmIrFile,
                executableFile
        );
    }

    public CompileResult compileFileStrictFlowchart(Path file,
                                                    Path llvmIrFile,
                                                    Path executableFile,
                                                    Path jimpleFile,
                                                    Path bytecodeOutput) throws IOException, InterruptedException {
        return compileStrictFlowchart(
                Path.of("resources", "c99.l"),
                Path.of("resources", "c99.y"),
                file,
                Path.of("generated", "strict"),
                llvmIrFile,
                executableFile,
                jimpleFile,
                bytecodeOutput
        );
    }

    // ── 内部：词法分析 ──

    private List<Token> lex(Reader source) {
        List<Token> tokens = new ArrayList<>();
        GeneratedLexer lexer = new GeneratedLexer(source);
        Token token;
        while (true) {
            token = lexer.nextToken();
            if (token.type() == TokenType.EOF) break;
            tokens.add(token);
        }
        tokens.add(new Token(TokenType.EOF, "EOF"));
        return tokens;
    }

    // ── 内部：语法分析（懒加载语法） ──

    private transient SeuYaccGenerator _generator;
    private transient ParserDriver _driver;

    private ParseResult parse(List<Token> tokens) {
        if (_driver == null) {
            try (Reader reader = new FileReader(grammarPath)) {
                _generator = new SeuYaccGenerator(reader, true);
                _driver = new ParserDriver(_generator.getGrammar(), _generator.getParseTable());
            } catch (Exception e) {
                throw new RuntimeException("Failed to load grammar: " + grammarPath, e);
            }
        }
        return _driver.parse(tokens);
    }

    private String runProcess(List<String> command) throws IOException, InterruptedException {
        ProcessBuilder builder = new ProcessBuilder(command);
        builder.directory(Path.of(".").toAbsolutePath().normalize().toFile());
        builder.redirectErrorStream(true);
        Process process = builder.start();
        String output;
        try (InputStream in = process.getInputStream()) {
            output = new String(in.readAllBytes());
        }
        int exit = process.waitFor();
        if (exit != 0) {
            throw new RuntimeException("Command failed (" + exit + "): " + String.join(" ", command)
                    + System.lineSeparator() + output);
        }
        return output;
    }

    private void emitLexerProgram(Path lexFile, Path outputFile) throws IOException {
        if (outputFile.getParent() != null) {
            Files.createDirectories(outputFile.getParent());
        }
        Files.writeString(outputFile, new CLexerToolchainEmitter().emitFromLexFile(lexFile));
    }

    private void emitParserProgram(Path yaccFile, Path outputFile) throws IOException {
        SeuYaccGenerator generator;
        try (Reader reader = Files.newBufferedReader(yaccFile)) {
            generator = new SeuYaccGenerator(reader, true);
        }
        if (outputFile.getParent() != null) {
            Files.createDirectories(outputFile.getParent());
        }
        Files.writeString(
                outputFile,
                new CParserProgramEmitter().emit(
                        generator.getGrammar(),
                        generator.getParseTable(),
                        C99SubsetSemanticActions.resolve(generator.getGrammar())
                )
        );
    }

    private String runProcessLogged(List<String> command,
                                    Path workingDirectory,
                                    Path commandsLog,
                                    String displayCommand,
                                    List<TraceEntry> trace,
                                    String stage,
                                    List<String> inputs,
                                    List<String> outputs,
                                    boolean optional) throws IOException, InterruptedException {
        long start = System.nanoTime();
        Files.createDirectories(workingDirectory);
        Files.writeString(commandsLog, displayCommand + System.lineSeparator(),
                StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        ProcessBuilder builder = new ProcessBuilder(command);
        builder.directory(workingDirectory.toFile());
        builder.redirectErrorStream(true);
        Process process = builder.start();
        String output;
        try (InputStream in = process.getInputStream()) {
            output = new String(in.readAllBytes());
        }
        int exit = process.waitFor();
        Files.writeString(commandsLog, "  actual: " + String.join(" ", command) + System.lineSeparator(),
                StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        if (!output.isBlank()) {
            Files.writeString(commandsLog, "  output: " + output.replace(System.lineSeparator(), "\\n")
                            + System.lineSeparator(),
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        }
        addTrace(trace, stage, inputs, outputs, displayCommand, exit, exit == 0,
                exit == 0 ? null : "command failed", elapsedMs(start));
        if (exit != 0 && !optional) {
            throw new RuntimeException("Command failed (" + exit + "): " + displayCommand
                    + System.lineSeparator() + output);
        }
        return output;
    }

    private static long elapsedMs(long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000L;
    }

    private static void addTrace(List<TraceEntry> trace,
                                 String stage,
                                 List<String> inputs,
                                 List<String> outputs,
                                 String command,
                                 Integer exitCode,
                                 boolean success,
                                 String skippedReason,
                                 long durationMs) {
        trace.add(new TraceEntry(stage, inputs, outputs, command, exitCode, success, skippedReason, durationMs));
    }

    private static String traceToJson(List<TraceEntry> trace) {
        StringBuilder sb = new StringBuilder();
        sb.append("[\n");
        for (int i = 0; i < trace.size(); i++) {
            TraceEntry entry = trace.get(i);
            sb.append("  {\n");
            sb.append("    \"stage\": \"").append(json(entry.stage)).append("\",\n");
            sb.append("    \"inputs\": ").append(jsonArray(entry.inputs)).append(",\n");
            sb.append("    \"outputs\": ").append(jsonArray(entry.outputs)).append(",\n");
            sb.append("    \"command\": ").append(entry.command == null ? "null" : "\"" + json(entry.command) + "\"").append(",\n");
            sb.append("    \"exitCode\": ").append(entry.exitCode == null ? "null" : entry.exitCode).append(",\n");
            sb.append("    \"success\": ").append(entry.success).append(",\n");
            sb.append("    \"skippedReason\": ")
                    .append(entry.skippedReason == null ? "null" : "\"" + json(entry.skippedReason) + "\"").append(",\n");
            sb.append("    \"durationMs\": ").append(entry.durationMs).append("\n");
            sb.append("  }");
            if (i + 1 < trace.size()) {
                sb.append(",");
            }
            sb.append("\n");
        }
        sb.append("]\n");
        return sb.toString();
    }

    private static String jsonArray(List<String> values) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append("\"").append(json(values.get(i))).append("\"");
        }
        return sb.append("]").toString();
    }

    private static String json(String value) {
        return value.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }

    private static String evidenceMarkdown(Path outputRoot) {
        return """
                # Flowchart Evidence

                ## 基础流程证据

                | 图片节点 | 实际模块/命令 | 生成物 |
                |---|---|---|
                | 词法规则 c99.l | 00-input/c99.l | - |
                | 词法分析程序生成器 LEX | SeuLexParser / NFA / DFA / Minimize / CLexerProgramEmitter | 01-lex/yylex.c |
                | C 编译器 | gcc yylex.c -o yylex | 01-lex/yylex |
                | 可执行词法分析程序 | ./yylex test.c tokens.txt | 01-lex/tokens.txt |
                | 语法规则 c99.y | 00-input/c99.y | - |
                | 语法分析程序生成器 YACC | YaccParser / LR(1) / LALR / ParseTable / CParserProgramEmitter | 02-yacc/yyparse.c |
                | C 编译器 | gcc yyparse.c -o yyparse | 02-yacc/yyparse |
                | 可执行语法分析程序 | ./yyparse tokens.txt action-tree.txt | 02-yacc/action-tree.txt |
                | 带语义动作节点的语法树 | AstTreeCodec 读取 action-tree.txt | 03-semantic/core-ast.txt |
                | 语义引擎 | SemanticActionEngine / TranslationSchemeExecutor | 03-semantic/core-ast.txt |
                | 语义检查 | CompileTimeSemanticAnalyzer / SymbolTable | 03-semantic/symbol-table.txt |
                | 中间代码生成程序 | CSemanticProgramEmitter -> gcc yysemantic.c -> yysemantic | 04-ir/output.ll |
                | Jimple codes | JimpleTextEmitter | 04-ir/output.jimple |

                ## 老师图片后端接入

                | 后端节点 | 实际模块/命令 | 生成物 |
                |---|---|---|
                | Jimple codes | 04-ir/output.jimple | - |
                | Soot 后端 | SootInvoker，依赖 SOOT_JAR | 05-soot/soot-output 或 05-soot/soot-skipped.txt |
                | Java bytecode/class | SOOT_JAR 存在时生成 | class 输出目录 |

                ## 项目扩展后端 Native Backend

                | 扩展后端节点 | 实际模块/命令 | 生成物 |
                |---|---|---|
                | LLVM IR | 04-ir/output.ll | - |
                | Clang IR 校验 | clang -c output.ll -o validate.o | 06-native/validate.o |
                | 生成汇编 | clang -S output.ll -o output.s | 06-native/output.s |
                | 生成目标文件 | clang -c output.ll -o output.o | 06-native/output.o |
                | 链接本机可执行文件 | clang output.o -o native-executable | 06-native/native-executable |
                | 运行本机程序 | ./native-executable | exitCode / stdout / stderr |
                """.replace("generated/strict-flowchart-run", outputRoot.toString());
    }

    private record TraceEntry(String stage,
                              List<String> inputs,
                              List<String> outputs,
                              String command,
                              Integer exitCode,
                              boolean success,
                              String skippedReason,
                              long durationMs) {
        private TraceEntry {
            inputs = List.copyOf(inputs);
            outputs = List.copyOf(outputs);
        }
    }

    private static String readAll(Reader reader) throws IOException {
        StringBuilder sb = new StringBuilder();
        char[] buffer = new char[4096];
        int n;
        while ((n = reader.read(buffer)) != -1) {
            sb.append(buffer, 0, n);
        }
        return sb.toString();
    }

}
