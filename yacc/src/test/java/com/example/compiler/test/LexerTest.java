package com.example.compiler.test;

import com.example.compiler.lex.GeneratedLexer;
import com.example.compiler.yacc.token.Token;
import com.example.compiler.yacc.token.TokenType;

import java.io.*;
import java.util.*;

/**
 * 词法分析器独立测试
 *
 * <p>验证 GeneratedLexer 对各类 C99 词法单元的正确识别。
 *
 * <p>运行方式:
 * <pre>
 *   cd yacc
 *   javac -encoding UTF-8 -d target/test-classes -cp target/classes \
 *       src/test/java/com/example/compiler/test/LexerTest.java
 *   java -cp "target/classes;target/test-classes" com.example.compiler.test.LexerTest
 * </pre>
 */
public final class LexerTest {
    public static void main(String[] args) {
        testBasicTokens();
        testKeywords();
        testOperators();
        testIdentifiers();
        testConstants();
        testMultiCharTokens();
        testLongestMatch();
        testWhitespace();
        System.out.println("=== ALL LEXER TESTS PASSED ===");
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

    // ── 1. 基础单字符 Token ──

    static void testBasicTokens() {
        System.out.println("─── testBasicTokens ───");

        assertEquals(TokenType.SEMI,   first(";").type(), "分号 ;");
        assertEquals(TokenType.LBRACE, first("{").type(), "左大括号 {");
        assertEquals(TokenType.RBRACE, first("}").type(), "右大括号 }");
        assertEquals(TokenType.LPAREN, first("(").type(), "左括号 (");
        assertEquals(TokenType.RPAREN, first(")").type(), "右括号 )");
        assertEquals(TokenType.COMMA,  first(",").type(), "逗号 ,");
        assertEquals(TokenType.COLON,  first(":").type(), "冒号 :");

        System.out.println("  PASS");
    }

    // ── 2. 关键字 ──

    static void testKeywords() {
        System.out.println("─── testKeywords ───");

        assertEquals(TokenType.INT,    first("int").type(),     "int");
        assertEquals(TokenType.RETURN, first("return").type(),  "return");
        assertEquals(TokenType.IF,     first("if").type(),      "if");
        assertEquals(TokenType.ELSE,   first("else").type(),    "else");
        assertEquals(TokenType.WHILE,  first("while").type(),   "while");
        assertEquals(TokenType.FOR,    first("for").type(),     "for");
        assertEquals(TokenType.VOID,   first("void").type(),    "void");
        assertEquals(TokenType.DO,     first("do").type(),      "do");

        System.out.println("  PASS");
    }

    // ── 3. 运算符 ──

    static void testOperators() {
        System.out.println("─── testOperators ───");

        // 单字符运算符 —— 每个都是一个独立 token
        assertEquals(TokenType.PLUS,  first("+").type(), "加号 +");
        assertEquals(TokenType.MINUS, first("-").type(), "减号 -");
        assertEquals(TokenType.STAR,  first("*").type(), "星号 *");
        assertEquals(TokenType.SLASH, first("/").type(), "斜杠 /");
        assertEquals(TokenType.PERCENT, first("%").type(), "百分号 %");
        assertEquals(TokenType.LT,    first("<").type(), "小于 <");
        assertEquals(TokenType.GT,    first(">").type(), "大于 >");
        assertEquals(TokenType.AMPERSAND,   first("&").type(), "与 &");
        assertEquals(TokenType.PIPE,   first("|").type(), "或 |");
        assertEquals(TokenType.CARET,   first("^").type(), "异或 ^");
        assertEquals(TokenType.BANG,  first("!").type(), "非 !");
        assertEquals(TokenType.TILDE, first("~").type(), "取反 ~");
        assertEquals(TokenType.QUESTION, first("?").type(), "问号 ?");
        assertEquals(TokenType.ASSIGN,first("=").type(), "赋值 =");

        System.out.println("  PASS");
    }

    // ── 4. 标识符 ──

    static void testIdentifiers() {
        System.out.println("─── testIdentifiers ───");

        assertEquals(TokenType.IDENTIFIER, first("foo").type(),    "foo");
        assertEquals(TokenType.IDENTIFIER, first("_bar").type(),   "_bar");
        assertEquals(TokenType.IDENTIFIER, first("x1").type(),     "x1");
        assertEquals(TokenType.IDENTIFIER, first("_").type(),      "_ (单下划线)");

        // 关键字不能当标识符
        List<Token> tokens = lex("int");
        assertEquals("int", tokens.get(0).lexeme(), "int lexeme");
        assertEquals(TokenType.INT, tokens.get(0).type(), "int type");

        // 以关键字开头但不是关键字的 —— 最长匹配
        List<Token> tokens2 = lex("intx");
        assertEquals(TokenType.IDENTIFIER, tokens2.get(0).type(), "intx 是标识符");
        assertEquals("intx", tokens2.get(0).lexeme(), "intx lexeme");

        System.out.println("  PASS");
    }

    // ── 5. 常量 ──

    static void testConstants() {
        System.out.println("─── testConstants ───");

        assertEquals(TokenType.CONSTANT, first("42").type(),   "十进制 42");
        assertEquals(TokenType.CONSTANT, first("0").type(),    "零");
        assertEquals(TokenType.CONSTANT, first("0xFF").type(), "十六进制 0xFF");
        assertEquals(TokenType.CONSTANT, first("077").type(),  "八进制 077");

        System.out.println("  PASS");
    }

    // ── 6. 多字符 Token ──

    static void testMultiCharTokens() {
        System.out.println("─── testMultiCharTokens ───");

        assertEquals(TokenType.INC_OP,   first("++").type(), "自增 ++");
        assertEquals(TokenType.DEC_OP,   first("--").type(), "自减 --");
        assertEquals(TokenType.LE_OP,    first("<=").type(), "小于等于 <=");
        assertEquals(TokenType.GE_OP,    first(">=").type(), "大于等于 >=");
        assertEquals(TokenType.EQ_OP,    first("==").type(), "等于 ==");
        assertEquals(TokenType.NE_OP,    first("!=").type(), "不等于 !=");
        assertEquals(TokenType.AND_OP,   first("&&").type(), "逻辑与 &&");
        assertEquals(TokenType.OR_OP,    first("||").type(), "逻辑或 ||");
        assertEquals(TokenType.LEFT_OP,  first("<<").type(), "左移 <<");
        assertEquals(TokenType.RIGHT_OP, first(">>").type(), "右移 >>");
        assertEquals(TokenType.PTR_OP,   first("->").type(), "箭头 ->");
        assertEquals(TokenType.ELLIPSIS, first("...").type(), "省略号 ...");

        // 复合赋值
        assertEquals(TokenType.ADD_ASSIGN, first("+=").type(), "+=");
        assertEquals(TokenType.SUB_ASSIGN, first("-=").type(), "-=");
        assertEquals(TokenType.MUL_ASSIGN, first("*=").type(), "*=");
        assertEquals(TokenType.DIV_ASSIGN, first("/=").type(), "/=");
        assertEquals(TokenType.OR_ASSIGN,  first("|=").type(), "|=");

        System.out.println("  PASS");
    }

    // ── 7. 最长匹配原则 ──

    static void testLongestMatch() {
        System.out.println("─── testLongestMatch ───");

        // "<<" 是一个 token，不是两个 "<"
        List<Token> t1 = lex("<<");
        assertEquals(1, t1.size(), "<< 是一个 token");
        assertEquals(TokenType.LEFT_OP, t1.get(0).type(), "<< type");

        // ">>=" 是一个 token
        assertEquals(TokenType.RIGHT_ASSIGN, first(">>=").type(), ">>= 是一个 token");

        // "int" 是关键字，"intx" 不是
        assertEquals(TokenType.INT,          first("int").type(),  "int");
        assertEquals(TokenType.IDENTIFIER,   first("intx").type(), "intx");

        // "0xFF 是十六进制，"0xG" 应该报错或被最长匹配截断
        assertEquals(TokenType.CONSTANT, first("0xAB").type(), "0xAB 是常量");

        System.out.println("  PASS");
    }

    // ── 8. 空白 ──

    static void testWhitespace() {
        System.out.println("─── testWhitespace ───");

        assertEquals(1, count("  \t  int  \n  "),
                "多余空白都被跳过，只剩 int");
        assertEquals(TokenType.INT, first("  int  ").type(),
                "空白被正确跳过");

        System.out.println("  PASS");
    }

    // ── 断言 ──

    static void assertEquals(Object expected, Object actual, String label) {
        if (expected == null ? actual != null : !expected.equals(actual)) {
            throw new AssertionError(label + " | 期望=" + expected + ", 实际=" + actual);
        }
    }
}
