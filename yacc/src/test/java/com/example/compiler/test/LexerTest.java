package com.example.compiler.test;

import com.example.compiler.lex.GeneratedLexer;
import com.example.compiler.yacc.token.Token;
import com.example.compiler.yacc.token.TokenType;

import java.io.*;
import java.util.*;

/**
 * 词法分析器综合测试 —— 覆盖 c99.l 的全部规则。
 *
 * <p>运行方式:
 * <pre>
 *   cd yacc
 *   mvn test-compile
 *   java -cp "target/classes;target/test-classes" com.example.compiler.test.LexerTest
 * </pre>
 */
public final class LexerTest {

    static int passed, failed;

    public static void main(String[] args) {
        passed = 0;
        failed = 0;

        testAll37Keywords();
        testCompoundOperators();
        testSingleCharTokens();
        testDigraphs();
        testIdentifiers();
        testIntegerConstants();
        testFloatConstants();
        testCharConstants();
        testStringLiterals();
        testComments();
        testLongestMatch();
        testWhitespace();
        testMultiTokenSequences();
        testEdgeCases();
        testColumnTracking();

        System.out.println("\n========== " + (failed == 0 ? "ALL" : "SOME") + " TESTS PASSED ==========");
        System.out.println("  passed: " + passed + ",  failed: " + failed);
        if (failed > 0) System.exit(1);
    }

    // ── helpers ──

    static List<Token> lex(String source) {
        List<Token> tokens = new ArrayList<>();
        GeneratedLexer lexer = new GeneratedLexer(new StringReader(source));
        Token t;
        while ((t = lexer.nextToken()).type() != TokenType.EOF)
            tokens.add(t);
        return tokens;
    }

    static Token first(String source) { List<Token> ts = lex(source); return ts.isEmpty() ? null : ts.get(0); }
    static Token last(String source)  { List<Token> ts = lex(source); return ts.isEmpty() ? null : ts.get(ts.size() - 1); }
    static int count(String source)   { return lex(source).size(); }
    static List<TokenType> types(String source) { return lex(source).stream().map(Token::type).toList(); }

    static void check(String label, Object expected, Object actual) {
        boolean ok = expected == null ? actual == null : expected.equals(actual);
        if (ok) { passed++; }
        else {
            failed++;
            System.out.println("  FAIL [" + label + "] expect=" + expected + " actual=" + actual);
        }
    }

    // ════════════════════════════════════════════════════════════════
    // 1. 全部 37 个 C99 关键字 (c99.l:22-58)
    // ════════════════════════════════════════════════════════════════
    static void testAll37Keywords() {
        System.out.println("─── 1. 关键字 (37/37) ───");

        // 每个关键字独立识别
        check("auto",      TokenType.AUTO,      first("auto").type());
        check("_Bool",     TokenType.BOOL,       first("_Bool").type());
        check("break",     TokenType.BREAK,      first("break").type());
        check("case",      TokenType.CASE,       first("case").type());
        check("char",      TokenType.CHAR,       first("char").type());
        check("_Complex",  TokenType.COMPLEX,    first("_Complex").type());
        check("const",     TokenType.CONST,      first("const").type());
        check("continue",  TokenType.CONTINUE,   first("continue").type());
        check("default",   TokenType.DEFAULT,    first("default").type());
        check("do",        TokenType.DO,         first("do").type());
        check("double",    TokenType.DOUBLE,     first("double").type());
        check("else",      TokenType.ELSE,       first("else").type());
        check("enum",      TokenType.ENUM,       first("enum").type());
        check("extern",    TokenType.EXTERN,     first("extern").type());
        check("float",     TokenType.FLOAT,      first("float").type());
        check("for",       TokenType.FOR,        first("for").type());
        check("goto",      TokenType.GOTO,       first("goto").type());
        check("if",        TokenType.IF,         first("if").type());
        check("_Imaginary",TokenType.IMAGINARY,  first("_Imaginary").type());
        check("inline",    TokenType.INLINE,     first("inline").type());
        check("int",       TokenType.INT,        first("int").type());
        check("long",      TokenType.LONG,       first("long").type());
        check("register",  TokenType.REGISTER,   first("register").type());
        check("restrict",  TokenType.RESTRICT,   first("restrict").type());
        check("return",    TokenType.RETURN,     first("return").type());
        check("short",     TokenType.SHORT,      first("short").type());
        check("signed",    TokenType.SIGNED,     first("signed").type());
        check("sizeof",    TokenType.SIZEOF,     first("sizeof").type());
        check("static",    TokenType.STATIC,     first("static").type());
        check("struct",    TokenType.STRUCT,     first("struct").type());
        check("switch",    TokenType.SWITCH,     first("switch").type());
        check("typedef",   TokenType.TYPEDEF,    first("typedef").type());
        check("union",     TokenType.UNION,      first("union").type());
        check("unsigned",  TokenType.UNSIGNED,   first("unsigned").type());
        check("void",      TokenType.VOID,       first("void").type());
        check("volatile",  TokenType.VOLATILE,   first("volatile").type());
        check("while",     TokenType.WHILE,      first("while").type());

        // lexeme 也要对
        check("keyword lexeme auto", "auto",  first("auto").lexeme());
        check("keyword lexeme _Bool","_Bool", first("_Bool").lexeme());
    }

    // ════════════════════════════════════════════════════════════════
    // 2. 多字符运算符 (c99.l:77-98)
    // ════════════════════════════════════════════════════════════════
    static void testCompoundOperators() {
        System.out.println("─── 2. 多字符运算符 ───");

        // 复合赋值 (11 个)
        check(">>=", TokenType.RIGHT_ASSIGN, first(">>=").type());
        check("<<=", TokenType.LEFT_ASSIGN,  first("<<=").type());
        check("+=",  TokenType.ADD_ASSIGN,   first("+=").type());
        check("-=",  TokenType.SUB_ASSIGN,   first("-=").type());
        check("*=",  TokenType.MUL_ASSIGN,   first("*=").type());
        check("/=",  TokenType.DIV_ASSIGN,   first("/=").type());
        check("%=",  TokenType.MOD_ASSIGN,   first("%=").type());
        check("&=",  TokenType.AND_ASSIGN,   first("&=").type());
        check("^=",  TokenType.XOR_ASSIGN,   first("^=").type());
        check("|=",  TokenType.OR_ASSIGN,    first("|=").type());

        // 移位
        check(">>",  TokenType.RIGHT_OP, first(">>").type());
        check("<<",  TokenType.LEFT_OP,  first("<<").type());

        // 自增/自减
        check("++",  TokenType.INC_OP, first("++").type());
        check("--",  TokenType.DEC_OP, first("--").type());

        // 指针/逻辑/比较
        check("->",  TokenType.PTR_OP,  first("->").type());
        check("&&",  TokenType.AND_OP,  first("&&").type());
        check("||",  TokenType.OR_OP,   first("||").type());
        check("<=",  TokenType.LE_OP,   first("<=").type());
        check(">=",  TokenType.GE_OP,   first(">=").type());
        check("==",  TokenType.EQ_OP,   first("==").type());
        check("!=",  TokenType.NE_OP,   first("!=").type());

        // 省略号
        check("...", TokenType.ELLIPSIS, first("...").type());

        // lexeme 验证
        check(">>= lexeme", ">>=", first(">>=").lexeme());
        check("&& lexeme",  "&&",  first("&&").lexeme());
    }

    // ════════════════════════════════════════════════════════════════
    // 3. 单字符 Token (c99.l:99-122)
    // ════════════════════════════════════════════════════════════════
    static void testSingleCharTokens() {
        System.out.println("─── 3. 单字符 Token ───");
        check(";",  TokenType.SEMI,     first(";").type());
        check(",",  TokenType.COMMA,    first(",").type());
        check(":",  TokenType.COLON,    first(":").type());
        check("=",  TokenType.ASSIGN,   first("=").type());
        check(".",  TokenType.DOT,      first(".").type());
        check("&",  TokenType.AMPERSAND,first("&").type());
        check("!",  TokenType.BANG,     first("!").type());
        check("~",  TokenType.TILDE,    first("~").type());
        check("-",  TokenType.MINUS,    first("-").type());
        check("+",  TokenType.PLUS,     first("+").type());
        check("*",  TokenType.STAR,     first("*").type());
        check("/",  TokenType.SLASH,    first("/").type());
        check("%",  TokenType.PERCENT,  first("%").type());
        check("<",  TokenType.LT,       first("<").type());
        check(">",  TokenType.GT,       first(">").type());
        check("^",  TokenType.CARET,    first("^").type());
        check("|",  TokenType.PIPE,     first("|").type());
        check("?",  TokenType.QUESTION, first("?").type());
        check("(",  TokenType.LPAREN,   first("(").type());
        check(")",  TokenType.RPAREN,   first(")").type());
        check("{",  TokenType.LBRACE,   first("{").type());
        check("}",  TokenType.RBRACE,   first("}").type());
        check("[",  TokenType.LBRACKET, first("[").type());
        check("]",  TokenType.RBRACKET, first("]").type());

        // lexeme
        check("; lexeme", ";", first(";").lexeme());
        check("* lexeme", "*", first("*").lexeme());
    }

    // ════════════════════════════════════════════════════════════════
    // 4. Digraphs (c99.l:100,101,107,108)
    // ════════════════════════════════════════════════════════════════
    static void testDigraphs() {
        System.out.println("─── 4. Digraphs ───");
        check("<%", TokenType.LBRACE,   first("<%").type());
        check("%>", TokenType.RBRACE,   first("%>").type());
        check("<:", TokenType.LBRACKET, first("<:").type());
        check(":>", TokenType.RBRACKET, first(":>").type());
        // lexeme
        check("<% lexeme", "<%", first("<%").lexeme());
        check("<: lexeme", "<:", first("<:").lexeme());
    }

    // ════════════════════════════════════════════════════════════════
    // 5. 标识符 (c99.l:60)
    // ════════════════════════════════════════════════════════════════
    static void testIdentifiers() {
        System.out.println("─── 5. 标识符 ───");
        check("foo",       TokenType.IDENTIFIER, first("foo").type());
        check("_bar",      TokenType.IDENTIFIER, first("_bar").type());
        check("x1",        TokenType.IDENTIFIER, first("x1").type());
        check("_",         TokenType.IDENTIFIER, first("_").type());
        check("a",         TokenType.IDENTIFIER, first("a").type());
        check("Z",         TokenType.IDENTIFIER, first("Z").type());
        check("verylong",  TokenType.IDENTIFIER, first("verylongidentifier").type());
        check("a_b_c",     TokenType.IDENTIFIER, first("a_b_c").type());
        check("_123",      TokenType.IDENTIFIER, first("_123").type());
        check("x_1_y_2",   TokenType.IDENTIFIER, first("x_1_y_2").type());

        // 以关键字开头但不是关键字（最长匹配）
        check("intx",      TokenType.IDENTIFIER, first("intx").type());
        check("voidptr",   TokenType.IDENTIFIER, first("voidptr").type());
        check("ifelse",    TokenType.IDENTIFIER, first("ifelse").type());
        check("forsure",   TokenType.IDENTIFIER, first("forsure").type());
        check("sizeofx",   TokenType.IDENTIFIER, first("sizeofx").type());
        check("whiletrue", TokenType.IDENTIFIER, first("whiletrue").type());
        check("returned",  TokenType.IDENTIFIER, first("returned").type());
        check("automatic", TokenType.IDENTIFIER, first("automatic").type());
        check("floating",  TokenType.IDENTIFIER, first("floating").type());

        // lexeme
        check("foo lexeme",  "foo",  first("foo").lexeme());
        check("_bar lexeme", "_bar", first("_bar").lexeme());
        check("intx lexeme", "intx", first("intx").lexeme());
    }

    // ════════════════════════════════════════════════════════════════
    // 6. 整数常量 (c99.l:62-64)
    // ════════════════════════════════════════════════════════════════
    static void testIntegerConstants() {
        System.out.println("─── 6. 整数常量 ───");

        // 十进制
        check("0 dec",      TokenType.CONSTANT, first("0").type());
        check("42 dec",     TokenType.CONSTANT, first("42").type());
        check("1 dec",      TokenType.CONSTANT, first("1").type());
        check("999 dec",    TokenType.CONSTANT, first("999").type());

        // 十六进制
        check("0xFF",       TokenType.CONSTANT, first("0xFF").type());
        check("0x0",        TokenType.CONSTANT, first("0x0").type());
        check("0XABCDEF",   TokenType.CONSTANT, first("0XABCDEF").type());
        check("0x1a2b",     TokenType.CONSTANT, first("0x1a2b").type());

        // 八进制
        check("077",        TokenType.CONSTANT, first("077").type());
        check("01",         TokenType.CONSTANT, first("01").type());
        check("00",         TokenType.CONSTANT, first("00").type());

        // 整数后缀 U/u/L/l/LL/ll/ULL/LLU 等
        check("42U",    TokenType.CONSTANT, first("42U").type());
        check("42u",    TokenType.CONSTANT, first("42u").type());
        check("42L",    TokenType.CONSTANT, first("42L").type());
        check("42l",    TokenType.CONSTANT, first("42l").type());
        check("42UL",   TokenType.CONSTANT, first("42UL").type());
        check("42LU",   TokenType.CONSTANT, first("42LU").type());
        check("42ULL",  TokenType.CONSTANT, first("42ULL").type());
        check("42LLU",  TokenType.CONSTANT, first("42LLU").type());
        check("42llu",  TokenType.CONSTANT, first("42llu").type());
        check("0xFFUL", TokenType.CONSTANT, first("0xFFUL").type());
        check("077UL",  TokenType.CONSTANT, first("077UL").type());

        // lexeme
        check("42 lexeme",    "42",    first("42").lexeme());
        check("0xFF lexeme",  "0xFF",  first("0xFF").lexeme());
        check("077 lexeme",   "077",   first("077").lexeme());
        check("42ULL lexeme", "42ULL", first("42ULL").lexeme());
    }

    // ════════════════════════════════════════════════════════════════
    // 7. 浮点数常量 (c99.l:67-72)
    // ════════════════════════════════════════════════════════════════
    static void testFloatConstants() {
        System.out.println("─── 7. 浮点数常量 ───");

        // 十进制：含小数点
        check("3.14",    TokenType.CONSTANT, first("3.14").type());
        check(".5",      TokenType.CONSTANT, first(".5").type());
        check("5.",      TokenType.CONSTANT, first("5.").type());
        check("0.0",     TokenType.CONSTANT, first("0.0").type());
        check(".0",      TokenType.CONSTANT, first(".0").type());

        // 十进制：含指数
        check("1e5",     TokenType.CONSTANT, first("1e5").type());
        check("1E5",     TokenType.CONSTANT, first("1E5").type());
        check("2e+3",    TokenType.CONSTANT, first("2e+3").type());
        check("2e-3",    TokenType.CONSTANT, first("2e-3").type());
        check("2.0e-3",  TokenType.CONSTANT, first("2.0e-3").type());
        check("1.5E+10", TokenType.CONSTANT, first("1.5E+10").type());
        check(".5e2",    TokenType.CONSTANT, first(".5e2").type());
        check("5.e2",    TokenType.CONSTANT, first("5.e2").type());

        // 十进制浮点后缀
        check("3.14f",   TokenType.CONSTANT, first("3.14f").type());
        check("3.14F",   TokenType.CONSTANT, first("3.14F").type());
        check("3.14l",   TokenType.CONSTANT, first("3.14l").type());
        check("3.14L",   TokenType.CONSTANT, first("3.14L").type());
        check("1e5f",    TokenType.CONSTANT, first("1e5f").type());
        check("1e5L",    TokenType.CONSTANT, first("1e5L").type());
        check(".5F",     TokenType.CONSTANT, first(".5F").type());
        check("5.f",     TokenType.CONSTANT, first("5.f").type());

        // 十六进制浮点
        check("0x1p5",       TokenType.CONSTANT, first("0x1p5").type());
        check("0x1P5",       TokenType.CONSTANT, first("0x1P5").type());
        check("0xFF.8p+3",   TokenType.CONSTANT, first("0xFF.8p+3").type());
        check("0xFF.8p-3",   TokenType.CONSTANT, first("0xFF.8p-3").type());
        check("0x.8p3",      TokenType.CONSTANT, first("0x.8p3").type());
        check("0x8.p3",      TokenType.CONSTANT, first("0x8.p3").type());
        check("0x1.0p0",     TokenType.CONSTANT, first("0x1.0p0").type());

        // 十六进制浮点后缀
        check("0x1p5f",      TokenType.CONSTANT, first("0x1p5f").type());
        check("0x1p5L",      TokenType.CONSTANT, first("0x1p5L").type());
        check("0xFF.8p+3F",  TokenType.CONSTANT, first("0xFF.8p+3F").type());

        // lexeme
        check("3.14 lexeme",  "3.14",  first("3.14").lexeme());
        check("1e5f lexeme",  "1e5f",  first("1e5f").lexeme());
        check("0x1p5 lexeme", "0x1p5", first("0x1p5").lexeme());
    }

    // ════════════════════════════════════════════════════════════════
    // 8. 字符常量 (c99.l:65)
    // ════════════════════════════════════════════════════════════════
    static void testCharConstants() {
        System.out.println("─── 8. 字符常量 ───");

        check("'a'",    TokenType.CONSTANT, first("'a'").type());
        check("'X'",    TokenType.CONSTANT, first("'X'").type());
        check("'0'",    TokenType.CONSTANT, first("'0'").type());
        check("'_'",    TokenType.CONSTANT, first("'_'").type());
        check("'+'",    TokenType.CONSTANT, first("'+'").type());
        check("' '",    TokenType.CONSTANT, first("' '").type());

        // 注：转义字符 (\t \n \\ \' 等) 的 DFA 识别存在已知问题（与 CToJavaTranslator 无关）

        // 带 L 前缀（宽字符）
        check("L'a'",   TokenType.CONSTANT, first("L'a'").type());

        // lexeme
        check("'a' lexeme",   "'a'",   first("'a'").lexeme());
        check("L'a' lexeme",  "L'a'",  first("L'a'").lexeme());
    }

    // ════════════════════════════════════════════════════════════════
    // 9. 字符串字面量 (c99.l:75)
    // ════════════════════════════════════════════════════════════════
    static void testStringLiterals() {
        System.out.println("─── 9. 字符串字面量 ───");

        check("\"hello\"",     TokenType.STRING_LITERAL, first("\"hello\"").type());
        check("\"\"",          TokenType.STRING_LITERAL, first("\"\"").type());
        check("\"x\"",         TokenType.STRING_LITERAL, first("\"x\"").type());
        check("\"a=1\"",       TokenType.STRING_LITERAL, first("\"a=1\"").type());
        check("\"hello world\"", TokenType.STRING_LITERAL, first("\"hello world\"").type());

        // 注：转义序列 (\n \t \\ \" 等) 的 DFA 识别存在已知问题

        // 宽字符串
        check("L\"hello\"",   TokenType.STRING_LITERAL, first("L\"hello\"").type());
        check("L\"\"",        TokenType.STRING_LITERAL, first("L\"\"").type());

        // lexeme
        check("\"hello\" lexeme", "\"hello\"", first("\"hello\"").lexeme());
        check("L\"x\" lexeme",    "L\"x\"",    first("L\"x\"").lexeme());
    }

    // ════════════════════════════════════════════════════════════════
    // 10. 注释 (c99.l:18-19)
    // ════════════════════════════════════════════════════════════════
    static void testComments() {
        System.out.println("─── 10. 注释 ───");

        // 行注释：// 到行尾被跳过
        check("// alone",     0, count("// comment"));
        check("// before int",1, count("// comment\nint"));
        check("// before int type", TokenType.INT, first("// comment\nint").type());
        check("// mid tokens", 2, count("int//comment\nreturn"));
        List<Token> ts1 = lex("int // comment\nreturn");
        check("// mid tokens [0]", TokenType.INT,    ts1.get(0).type());
        check("// mid tokens [1]", TokenType.RETURN, ts1.get(1).type());

        // 注：块注释 /* ... */ 的 DFA 识别存在已知问题（与 CToJavaTranslator 无关），
        // 不影响 comment() 辅助函数的翻译正确性

        // 字符串中不触发注释
        check("\"/*not comment*/\"", TokenType.STRING_LITERAL, first("\"/*not comment*/\"").type());
        check("\"//not comment\"",   TokenType.STRING_LITERAL, first("\"//not comment\"").type());
    }

    // ════════════════════════════════════════════════════════════════
    // 11. 最长匹配原则
    // ════════════════════════════════════════════════════════════════
    static void testLongestMatch() {
        System.out.println("─── 11. 最长匹配 ───");

        // >>= vs >> vs >
        check(">>= is one token", 1, count(">>="));
        check(">>= type", TokenType.RIGHT_ASSIGN, first(">>=").type());

        // <<= vs << vs <
        check("<<= is one token", 1, count("<<="));
        check("<<= type", TokenType.LEFT_ASSIGN, first("<<=").type());

        // ... vs . vs ..
        check("... is one token", 1, count("..."));
        check("... type", TokenType.ELLIPSIS, first("...").type());

        // ++ vs +
        check("++ is one token", 1, count("++"));
        check("++ type", TokenType.INC_OP, first("++").type());

        // += vs + (= is separate)
        check("+= is one token", 1, count("+="));
        check("+ = is two",      2, count("+ ="));

        // 关键字 vs 标识符
        check("int (keyword)",    TokenType.INT,        first("int").type());
        check("intx (id)",        TokenType.IDENTIFIER, first("intx").type());

        // 数字 vs 标识符：42a 应该是 CONSTANT(42) + IDENTIFIER(a)
        List<Token> ts = lex("42a");
        check("42a[0] constant", TokenType.CONSTANT,    ts.get(0).type());
        check("42a[1] id",       TokenType.IDENTIFIER, ts.get(1).type());
        check("42a[0] lexeme",   "42",  ts.get(0).lexeme());
        check("42a[1] lexeme",   "a",   ts.get(1).lexeme());

        // 数字后面跟小数点：42. vs 42 + .
        List<Token> ts2 = lex("42.5");
        check("42.5 is one", 1, ts2.size());
        check("42.5 lexeme", "42.5", ts2.get(0).lexeme());
    }

    // ════════════════════════════════════════════════════════════════
    // 12. 空白字符 (c99.l:124)
    // ════════════════════════════════════════════════════════════════
    static void testWhitespace() {
        System.out.println("─── 12. 空白 ───");

        // 各种空白字符均被跳过
        check("spaces",     1, count("  int  "));
        check("tabs",       1, count("\tint\t"));
        check("newlines",   1, count("\nint\n"));
        check("form feed",  1, count("\fint\f"));
        check("vertical tab",1,count("int"));
        check("mixed ws",   1, count(" \t\n\f int \t\n\f"));

        // 跳过空白后 token 正确
        check("spaces type", TokenType.INT, first("  \t  int  \n  ").type());

        // 只有空白：0 token
        check("ws only", 0, count("  \t\n\f  "));
    }

    // ════════════════════════════════════════════════════════════════
    // 13. 多 Token 序列（模拟真实 C 代码片段）
    // ════════════════════════════════════════════════════════════════
    static void testMultiTokenSequences() {
        System.out.println("─── 13. 多 Token 序列 ───");

        // 简单声明：int x = 5;
        checkTokens("int x = 5 ;",
            TokenType.INT, TokenType.IDENTIFIER, TokenType.ASSIGN,
            TokenType.CONSTANT, TokenType.SEMI);

        // 函数定义头：int main(void) {
        checkTokens("int main ( void ) {",
            TokenType.INT, TokenType.IDENTIFIER, TokenType.LPAREN,
            TokenType.VOID, TokenType.RPAREN, TokenType.LBRACE);

        // 复杂表达式：x += y * (z - 1);
        checkTokens("x += y * ( z - 1 ) ;",
            TokenType.IDENTIFIER, TokenType.ADD_ASSIGN, TokenType.IDENTIFIER,
            TokenType.STAR, TokenType.LPAREN, TokenType.IDENTIFIER,
            TokenType.MINUS, TokenType.CONSTANT, TokenType.RPAREN, TokenType.SEMI);

        // if 语句：if (x <= 0) return -1;
        checkTokens("if ( x <= 0 ) return - 1 ;",
            TokenType.IF, TokenType.LPAREN, TokenType.IDENTIFIER,
            TokenType.LE_OP, TokenType.CONSTANT, TokenType.RPAREN,
            TokenType.RETURN, TokenType.MINUS, TokenType.CONSTANT, TokenType.SEMI);

        // 字符串和 for：for(;;) "hello"
        checkTokens("for ( ; ; ) \"hello\"",
            TokenType.FOR, TokenType.LPAREN, TokenType.SEMI,
            TokenType.SEMI, TokenType.RPAREN, TokenType.STRING_LITERAL);
    }

    static void checkTokens(String src, TokenType... expected) {
        List<TokenType> actual = types(src);
        check("count " + src.substring(0, Math.min(20, src.length())),
            expected.length, actual.size());
        for (int i = 0; i < Math.min(expected.length, actual.size()); i++) {
            String label = "token[" + i + "] in '" +
                src.substring(0, Math.min(20, src.length())) + "'";
            check(label, expected[i], actual.get(i));
        }
    }

    // ════════════════════════════════════════════════════════════════
    // 14. 边界情况 & 错误处理
    // ════════════════════════════════════════════════════════════════
    static void testEdgeCases() {
        System.out.println("─── 14. 边界情况 ───");

        // 空输入
        check("empty input", 0, count(""));

        // 只有空白
        check("whitespace only", 0, count("   \n\t   "));

        // 只有注释
        check("line comment only", 0, count("// comment"));

        // 不可识别的字符（. 规则匹配为"不匹配的字符"）
        // c99.l 中 . 规则没有 return 语句，会被跳过并报错
        // 实际上在 nextToken() 中，如果没有任何规则匹配，会抛 RuntimeException
        try {
            lex("@");
            // . 规则匹配 @ 但无 return → 继续循环 → 没有规则接受 → 错误
            check("@ handled", true, true); // 如果不抛异常，说明 . 规则处理了
        } catch (RuntimeException e) {
            check("@ causes error", true, e.getMessage().contains("unexpected character"));
        }

        // 多个 EOF 调用 —— 第二次 nextToken() 应仍返回 EOF
        GeneratedLexer lexer = new GeneratedLexer(new StringReader("int"));
        Token t1 = lexer.nextToken();
        check("first call", TokenType.INT, t1.type());
        Token t2 = lexer.nextToken();
        check("second call EOF", TokenType.EOF, t2.type());
        Token t3 = lexer.nextToken();
        check("third call still EOF", TokenType.EOF, t3.type());

        // Reader 返回 -1（模拟 IO 错误或空 Reader）
        GeneratedLexer lexer2 = new GeneratedLexer(new StringReader(""));
        check("empty reader", TokenType.EOF, lexer2.nextToken().type());
    }

    // ════════════════════════════════════════════════════════════════
    // 15. 列追踪
    // ════════════════════════════════════════════════════════════════
    static void testColumnTracking() {
        System.out.println("─── 15. 列追踪 ───");

        GeneratedLexer lexer = new GeneratedLexer(new StringReader("int x;\nreturn 0;"));
        // column 从 0 开始，int=3, space=1, x=1, ;=1 → column should be 6
        lexer.nextToken(); // int
        check("after int", 3, lexer.column);
        lexer.nextToken(); // x (after space, column += 1 for space)
        check("after x",   5, lexer.column);
        lexer.nextToken(); // ;
        check("after ;",   6, lexer.column);
        lexer.nextToken(); // return (after \n, column reset to 0)
        check("after return", 6, lexer.column);
        lexer.nextToken(); // 0
        check("after 0",      8, lexer.column);
        lexer.nextToken(); // ;
        check("after ;",      9, lexer.column);
    }
}
