package com.example.compiler.test;

import com.example.compiler.lex.GeneratedLexer;
import com.example.compiler.yacc.token.Token;
import com.example.compiler.yacc.token.TokenType;

import java.io.*;
import java.util.*;

/**
 * 词法分析器高级测试 — 验证浮点数、字符常量、字符串字面量的词法识别。
 *
 * <p>对应 c99.l 中先前被跳过的规则（已修复）：
 * <ul>
 *   <li>十进制浮点数（含小数点/指数）—— 规则行 67-69</li>
 *   <li>十六进制浮点数（含小数点/指数）—— 规则行 70-72</li>
 *   <li>字符常量 {@code 'a'} —— 规则行 65</li>
 *   <li>字符串字面量 {@code "hello"} —— 规则行 75</li>
 * </ul>
 *
 * <p>运行方式:
 * <pre>
 *   cd yacc
 *   javac -encoding UTF-8 -d target/test-classes -cp target/classes \
 *       src/test/java/com/example/compiler/test/LexerAdvancedTest.java
 *   java -cp "target/classes;target/test-classes" com.example.compiler.test.LexerAdvancedTest
 * </pre>
 */
public final class LexerAdvancedTest {
    public static void main(String[] args) {
        testFloatLiterals();
        testCharConstants();
        testStringLiterals();
        testIntegerLiteralsStillWork();
        testLineComments();
        System.out.println("=== ALL ADVANCED LEXER TESTS PASSED ===");
    }

    // ── Token 提取辅助 ──

    static List<Token> lex(String source) {
        List<Token> tokens = new ArrayList<>();
        GeneratedLexer lexer = new GeneratedLexer(new StringReader(source));
        Token token;
        while (true) {
            token = lexer.nextToken();
            if (token.type() == TokenType.EOF) break;
            tokens.add(token);
        }
        return tokens;
    }

    static Token first(String source) { return lex(source).get(0); }
    static int count(String source) { return lex(source).size(); }

    // ── 1. 浮点数字面量 ──

    static void testFloatLiterals() {
        System.out.println("─── testFloatLiterals ───");

        // 十进制浮点数：含小数点
        assertEquals(TokenType.CONSTANT, first("3.14").type(),     "3.14 是浮点常量");
        assertEquals(TokenType.CONSTANT, first(".5").type(),       ".5 是浮点常量");
        assertEquals(TokenType.CONSTANT, first("5.").type(),       "5. 是浮点常量");

        // 十进制浮点数：含指数
        assertEquals(TokenType.CONSTANT, first("1e5").type(),      "1e5 是浮点常量");
        assertEquals(TokenType.CONSTANT, first("2.0e-3").type(),   "2.0e-3 是浮点常量");
        assertEquals(TokenType.CONSTANT, first("1.5E+10").type(),  "1.5E+10 是浮点常量");

        // 十六进制浮点数
        assertEquals(TokenType.CONSTANT, first("0x1p5").type(),    "0x1p5 是十六进制浮点常量");
        assertEquals(TokenType.CONSTANT, first("0xFF.8p+3").type(), "0xFF.8p+3 是十六进制浮点常量");

        // 浮点数后缀 f/F/l/L
        assertEquals(TokenType.CONSTANT, first("3.14f").type(),    "3.14f 是浮点常量");
        assertEquals(TokenType.CONSTANT, first("1e5L").type(),     "1e5L 是浮点常量");

        System.out.println("  PASS");
    }

    // ── 2. 字符常量 ──

    static void testCharConstants() {
        System.out.println("─── testCharConstants ───");

        // 基本字符常量
        assertEquals(TokenType.CONSTANT, first("'a'").type(),   "'a' 是字符常量");
        assertEquals(TokenType.CONSTANT, first("'X'").type(),   "'X' 是字符常量");
        assertEquals(TokenType.CONSTANT, first("'0'").type(),   "'0' 是字符常量");
        assertEquals(TokenType.CONSTANT, first("'_'").type(),   "'_' 是字符常量");

        // 带 L 前缀的宽字符常量
        assertEquals(TokenType.CONSTANT, first("L'a'").type(),  "L'a' 是宽字符常量");

        System.out.println("  PASS");
    }

    // ── 3. 字符串字面量 ──

    static void testStringLiterals() {
        System.out.println("─── testStringLiterals ───");

        assertEquals(TokenType.STRING_LITERAL, first("\"hello\"").type(),       "\"hello\" 是字符串");
        assertEquals(TokenType.STRING_LITERAL, first("\"world\"").type(),       "\"world\" 是字符串");
        assertEquals(TokenType.STRING_LITERAL, first("\"x\"").type(),           "\"x\" 是字符串");
        assertEquals(TokenType.STRING_LITERAL, first("\"a=1\"").type(),         "\"a=1\" 是字符串");

        // 宽字符串
        assertEquals(TokenType.STRING_LITERAL, first("L\"wide\"").type(),       "L\"wide\" 是宽字符串");

        System.out.println("  PASS");
    }

    // ── 4. 回归：整数字面量不受影响 ──

    static void testIntegerLiteralsStillWork() {
        System.out.println("─── testIntegerLiteralsStillWork ───");

        // 十进制整数
        assertEquals(TokenType.CONSTANT, first("42").type(),     "42 仍是 CONSTANT");
        assertEquals(TokenType.CONSTANT, first("0").type(),      "0 仍是 CONSTANT");

        // 十六进制整数
        assertEquals(TokenType.CONSTANT, first("0xFF").type(),   "0xFF 仍是 CONSTANT");

        // 八进制整数
        assertEquals(TokenType.CONSTANT, first("077").type(),    "077 仍是 CONSTANT");

        // 整数后缀
        assertEquals(TokenType.CONSTANT, first("42U").type(),    "42U 仍是 CONSTANT");
        assertEquals(TokenType.CONSTANT, first("42L").type(),    "42L 仍是 CONSTANT");
        assertEquals(TokenType.CONSTANT, first("42ULL").type(),  "42ULL 仍是 CONSTANT");

        System.out.println("  PASS");
    }

    // ── 5. 单行注释（也依赖 [^...] 否定字符集） ──

    static void testLineComments() {
        System.out.println("─── testLineComments ───");

        // // 注释应被消费，不产生任何 token
        assertEquals(1, count("// comment\nint"),
                "// comment 被跳过，只剩 int");
        assertEquals(TokenType.INT, first("// comment\nint").type(),
                "跳过 // comment 后第一个 token 是 int");

        // // 注释到行尾，return 应在下一个 token
        List<Token> tokens = lex("int // comment\nreturn");
        assertEquals(2, tokens.size(), "int 和 return 两个 token");
        assertEquals(TokenType.INT, tokens.get(0).type(), "第一个是 int");
        assertEquals(TokenType.RETURN, tokens.get(1).type(), "第二个是 return");

        System.out.println("  PASS");
    }

    // ── 断言 ──

    static void assertEquals(Object expected, Object actual, String label) {
        if (expected == null ? actual != null : !expected.equals(actual)) {
            throw new AssertionError(label + " | 期望=" + expected + ", 实际=" + actual);
        }
    }
}
