package com.example.compiler.test;

import com.example.compiler.ir.IrGenerationResult;
import com.example.compiler.ir.LlvmLikeTextEmitter;
import com.example.compiler.ir.YaccIrBridge;
import com.example.compiler.lex.GeneratedLexer;
import com.example.compiler.yacc.generator.SeuYaccGenerator;
import com.example.compiler.yacc.runtime.ParseResult;
import com.example.compiler.yacc.runtime.ParserDriver;
import com.example.compiler.yacc.token.Token;
import com.example.compiler.yacc.token.TokenType;

import java.io.*;
import java.util.*;

/**
 * 端到端流水线测试：源代码 → 词法 → 语法 → 语义 → IR
 *
 * <p>运行方式:
 * <pre>
 *   cd yacc
 *   javac -encoding UTF-8 -d target/test-classes -cp target/classes \
 *       src/test/java/com/example/compiler/test/PipelineTest.java
 *   java -cp "target/classes;target/test-classes" com.example.compiler.test.PipelineTest
 * </pre>
 */
public final class PipelineTest {
    public static void main(String[] args) {
        testSimpleProgram();
        testArithmeticExpression();
        testNestedFunctionCall();
        System.out.println("=== ALL PIPELINE TESTS PASSED ===");
    }

    // ── 通用的 "源码→IR" 流水线 ──

    static String runPipeline(String sourceCode) {
        // 1. 词法分析
        List<Token> tokens = new ArrayList<>();
        GeneratedLexer lexer = new GeneratedLexer(new StringReader(sourceCode));
        Token token;
        while (true) {
            token = lexer.nextToken();
            if (token.type() == TokenType.EOF) break;
            tokens.add(token);
        }
        tokens.add(new Token(TokenType.EOF, "EOF"));

        // 2. 语法分析
        SeuYaccGenerator generator;
        try (Reader reader = new FileReader("resources/c99.y")) {
            generator = new SeuYaccGenerator(reader, true);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load grammar", e);
        }
        ParserDriver driver = new ParserDriver(generator.getGrammar(), generator.getParseTable());
        ParseResult parseResult = driver.parse(tokens);

        if (!parseResult.isAccepted()) {
            throw new RuntimeException("Parse failed: " + parseResult.getErrorMessage());
        }

        // 3. 语义分析 + IR 生成
        YaccIrBridge bridge = new YaccIrBridge();
        IrGenerationResult ir = bridge.generate(parseResult);

        // 4. 输出 IR 文本
        return new LlvmLikeTextEmitter().emit(ir);
    }

    // ── 例1: 简单函数 ──

    static void testSimpleProgram() {
        System.out.println("─── testSimpleProgram ───");
        String src = """
                int add(int x, int y) { return x + y; }
                int main() { return add(3, 4); }
                """;
        System.out.println("输入:\n" + src);

        String ir = runPipeline(src);

        System.out.println(ir);

        assertContains(ir, "define i32 @add", "add 函数定义");
        assertContains(ir, "define i32 @main", "main 函数定义");
        assertContains(ir, "return", "return 语句");

        System.out.println("  PASS\n");
    }

    // ── 例2: 多次运算 ──

    static void testArithmeticExpression() {
        System.out.println("─── testArithmeticExpression ───");
        String src = """
                int calc(int a, int b) { return a * b + a; }
                int main() { return calc(2, 5); }
                """;
        System.out.println("输入:\n" + src);

        String ir = runPipeline(src);

        System.out.println(ir);

        assertContains(ir, "define i32 @calc", "calc 函数定义");
        assertContains(ir, "define i32 @main", "main 函数定义");
        assertContains(ir, "return", "return 语句");

        System.out.println("  PASS\n");
    }

    // ── 例3: 嵌套函数调用 ──

    static void testNestedFunctionCall() {
        System.out.println("─── testNestedFunctionCall ───");
        System.out.println("输入:");
        String src = """
                int add(int x, int y) { return x + y; }
                int main() { return add(1, add(2, 3)); }
                """;
        System.out.println(src);

        String ir = runPipeline(src);

        System.out.println(ir);

        assertContains(ir, "define i32 @add", "add 函数");
        assertContains(ir, "define i32 @main", "main 函数");
        assertContains(ir, "call add", "add 调用（至少一处）");

        System.out.println("  PASS\n");
    }

    // ── 断言 ──

    static void assertContains(String haystack, String needle, String label) {
        if (!haystack.contains(needle)) {
            throw new AssertionError(label + " | IR 中找不到: \"" + needle + "\"");
        }
    }
}
