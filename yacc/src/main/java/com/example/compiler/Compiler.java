package com.example.compiler;

import com.example.compiler.ir.IrGenerationResult;
import com.example.compiler.ir.LlvmLikeTextEmitter;
import com.example.compiler.ir.YaccIrBridge;
import com.example.compiler.lex.GeneratedLexer;
import com.example.compiler.semantic.CompileTimeSemanticAnalyzer;
import com.example.compiler.semantic.SemanticResult;
import com.example.compiler.semantic.emitter.CSemanticProgramEmitter;
import com.example.compiler.yacc.generator.SeuYaccGenerator;
import com.example.compiler.yacc.runtime.ParseResult;
import com.example.compiler.yacc.runtime.ParserDriver;
import com.example.compiler.yacc.token.Token;
import com.example.compiler.yacc.token.TokenType;

import java.io.*;
import java.nio.file.*;
import java.util.*;

/**
 * 编译器统一入口：源码 → 词法 → 语法 → 语义 → IR。
 *
 * <p>当前仓库的主入口是 Compiler。它使用 GeneratedLexer 直接得到 token 列表，
 * 再懒加载 resources/c99.y，经 SeuYaccGenerator 构造 LALR 分析表，
 * 交给 ParserDriver 生成 parse tree，最后通过语义/IR 模块输出 LLVM-like IR。
 * {@link #compileViaGeneratedC(Reader, Path, Path, Path)} 额外走 yysemantic.c 路线：
 * Core AST -> CSemanticProgramEmitter -> gcc -> yysemantic -> LLVM IR 文本。</p>
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

            if (executableFile != null) {
                if (executableFile.getParent() != null) {
                    Files.createDirectories(executableFile.getParent());
                }
                runProcess(List.of("clang", llFile.toString(), "-o", executableFile.toString()));
                result.executableFile = executableFile;
            }
        }
        return result;
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

    // ── main：命令行入口 ──

    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            System.out.println("用法: java Compiler <source.c>");
            System.out.println("      java Compiler -e \"<code>\"");
            System.out.println("      java Compiler --via-c <source.c>");
            System.out.println("      java Compiler --via-c -e \"<code>\"");
            System.out.println("      java Compiler --via-c --emit-ll generated/final/program.ll --emit-exe generated/final/program.exe <source.c>");
            System.exit(1);
        }

        Compiler compiler = new Compiler();
        CompileResult result;
        boolean viaGeneratedC = false;
        Path emitLl = null;
        Path emitExe = null;
        List<String> positional = new ArrayList<>();

        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--via-c" -> viaGeneratedC = true;
                case "--emit-ll" -> {
                    if (i + 1 >= args.length) {
                        System.err.println("缺少 --emit-ll 输出路径");
                        System.exit(1);
                        return;
                    }
                    emitLl = Path.of(args[++i]);
                    viaGeneratedC = true;
                }
                case "--emit-exe" -> {
                    if (i + 1 >= args.length) {
                        System.err.println("缺少 --emit-exe 输出路径");
                        System.exit(1);
                        return;
                    }
                    emitExe = Path.of(args[++i]);
                    viaGeneratedC = true;
                }
                default -> positional.add(args[i]);
            }
        }

        if (positional.isEmpty()) {
            System.err.println("缺少源码输入");
            System.exit(1);
            return;
        }

        if (positional.get(0).equals("-e")) {
            if (positional.size() <= 1) {
                System.err.println("缺少 -e 后的源码字符串");
                System.exit(1);
                return;
            }
            result = viaGeneratedC
                    ? compiler.compileViaGeneratedC(
                            new StringReader(positional.get(1)),
                            Path.of("generated", "semantic"),
                            emitLl,
                            emitExe
                    )
                    : compiler.compile(positional.get(1));
        } else {
            result = viaGeneratedC
                    ? compiler.compileFileViaGeneratedC(Path.of(positional.get(0)), emitLl, emitExe)
                    : compiler.compileFile(Path.of(positional.get(0)));
        }

        if (!result.isSuccess()) {
            System.err.println("编译失败: " + result.parseResult.getErrorMessage());
            System.exit(1);
        }

        System.out.println(result.irText);
        if (result.llvmIrFile() != null) {
            System.out.println("; LLVM IR written to " + result.llvmIrFile().toAbsolutePath().normalize());
        }
        if (result.executableFile() != null) {
            System.out.println("; executable written to " + result.executableFile().toAbsolutePath().normalize());
        }
    }
}
