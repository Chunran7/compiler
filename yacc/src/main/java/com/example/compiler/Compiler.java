package com.example.compiler;

import com.example.compiler.ir.IrGenerationResult;
import com.example.compiler.ir.LlvmLikeTextEmitter;
import com.example.compiler.ir.YaccIrBridge;
import com.example.compiler.lex.GeneratedLexer;
import com.example.compiler.semantic.SemanticResult;
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

    // ── 入口 3: 文件 ──

    public CompileResult compileFile(Path file) throws IOException {
        return compile(Files.readString(file));
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

    // ── main：命令行入口 ──

    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            System.out.println("用法: java Compiler <source.c>");
            System.out.println("      java Compiler -e \"<code>\"");
            System.exit(1);
        }

        Compiler compiler = new Compiler();
        CompileResult result;

        if (args[0].equals("-e")) {
            result = compiler.compile(args[1]);
        } else {
            result = compiler.compileFile(Path.of(args[0]));
        }

        if (!result.isSuccess()) {
            System.err.println("编译失败: " + result.parseResult.getErrorMessage());
            System.exit(1);
        }

        System.out.println(result.irText);
    }
}
