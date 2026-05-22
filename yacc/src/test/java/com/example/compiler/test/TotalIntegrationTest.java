package com.example.compiler.test;

import com.example.compiler.Compiler;
import com.example.compiler.ir.IrGenerationResult;
import com.example.compiler.ir.LlvmLikeTextEmitter;
import com.example.compiler.ir.YaccIrBridge;
import com.example.compiler.semantic.SemanticException;
import com.example.compiler.semantic.SemanticResult;
import com.example.compiler.semantic.action.ActionArgument;
import com.example.compiler.semantic.action.ActionPattern;
import com.example.compiler.semantic.action.ActionPatternParser;
import com.example.compiler.semantic.action.ActionRegistry;
import com.example.compiler.yacc.ast.AstKind;
import com.example.compiler.yacc.ast.AstNode;
import com.example.compiler.yacc.ast.CoreAstNode;
import com.example.compiler.yacc.emitter.AstMarkdownEmitter;
import com.example.compiler.yacc.generator.SeuYaccGenerator;
import com.example.compiler.yacc.grammar.Grammar;
import com.example.compiler.yacc.grammar.Production;
import com.example.compiler.yacc.runtime.ParseResult;
import com.example.compiler.yacc.runtime.ParserDriver;
import com.example.compiler.yacc.token.Token;
import com.example.compiler.yacc.token.TokenType;

import com.example.compiler.lex.GeneratedLexer;

import java.io.FileReader;
import java.io.Reader;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class TotalIntegrationTest {
    public static void main(String[] args) throws Exception {
        testC99GrammarParsing();
        testOperatorPrecedenceGrammar();
        testLalrStateMerging();
        testConflictReporting();
        testActionPatternParsingAndRegistry();
        testC99LexerSpecTokenCoverage();
        testC99LexerToParserIntegration();
        testC99ParserOnlySamplesAndProductionCoverage();
        testMiniCSubsetSemanticIrPipeline();
        testGeneratedCSemanticProgramPipeline();
        testGeneratedExecutablePipeline();
        testAstMarkdownEmission();
        testDuplicateDeclaration();
        testUndeclaredUse();
        testUndefinedFunctionCall();
        testArgumentCountMismatch();
        System.out.println("=== ALL TESTS PASSED ===");
    }

    // ── C99 grammar parsing ──

    private static void testC99GrammarParsing() throws Exception {
        try (Reader reader = new FileReader(grammarPath().toFile())) {
            SeuYaccGenerator generator = new SeuYaccGenerator(reader, true);
            Grammar grammar = generator.getGrammar();
            assertNotNull(grammar.getStartSymbol(), "start symbol should exist");
            assertEquals("translation_unit", grammar.getStartSymbol().getName(), "start symbol mismatch");
            assertTrue(generator.getCollection().states().size() > 0, "state collection should not be empty");
            assertTrue(!generator.getParseTable().actionRows().isEmpty(), "parse table should have actions");

            // Verify key C99 productions exist
            boolean hasFuncDef = false;
            boolean hasDecl = false;
            boolean hasExpr = false;
            for (Production prod : grammar.getProductions()) {
                String lhs = prod.getLeft().getName();
                if ("function_definition".equals(lhs)) hasFuncDef = true;
                if ("declaration".equals(lhs)) hasDecl = true;
                if ("expression".equals(lhs)) hasExpr = true;
            }
            assertTrue(hasFuncDef, "should have function_definition production");
            assertTrue(hasDecl, "should have declaration production");
            assertTrue(hasExpr, "should have expression production");
        }
        System.out.println("[PASS] C99 grammar parsing");
    }

    // ── Operator precedence (unchanged) ──

    private static void testOperatorPrecedenceGrammar() throws Exception {
        SeuYaccGenerator generator;
        try (Reader reader = new FileReader(testGrammarPath("expr_precedence.y").toFile())) {
            generator = new SeuYaccGenerator(reader, false);
        }
        ParserDriver driver = new ParserDriver(generator.getGrammar(), generator.getParseTable());
        ParseResult result = driver.parse(TestSupport.precedenceExpressionTokens());
        assertTrue(result.isAccepted(), "precedence grammar should parse: " + result.getErrorMessage());
        assertNotNull(result.getAstRoot(), "precedence parse tree should not be null");
        System.out.println("[PASS] Yacc precedence / associativity conflict resolution");
    }

    // ── LALR merging (unchanged) ──

    private static void testLalrStateMerging() throws Exception {
        int lr1States;
        int lalrStates;
        try (Reader reader = new FileReader(testGrammarPath("lalr_core_merge.y").toFile())) {
            SeuYaccGenerator lr1Generator = new SeuYaccGenerator(reader, false);
            lr1States = lr1Generator.getCollection().states().size();
        }
        try (Reader reader = new FileReader(testGrammarPath("lalr_core_merge.y").toFile())) {
            SeuYaccGenerator lalrGenerator = new SeuYaccGenerator(reader, true);
            lalrStates = lalrGenerator.getCollection().states().size();
            ParserDriver driver = new ParserDriver(lalrGenerator.getGrammar(), lalrGenerator.getParseTable());
            ParseResult result = driver.parse(lalrSampleTokens());
            assertTrue(result.isAccepted(), "LALR grammar should still parse correctly");
        }
        assertTrue(lalrStates <= lr1States, "LALR state count should not exceed LR(1) state count");
        if (lalrStates < lr1States) {
            System.out.println("[PASS] LALR state merging verified (" + lr1States + " -> " + lalrStates + ")");
        } else {
            System.out.println("[PASS] LALR construction verified (no observable merge: " + lr1States + " -> " + lalrStates + ")");
        }
    }

    // ── Conflict reporting (unchanged) ──

    private static void testConflictReporting() throws Exception {
        // Use a grammar with reduce/reduce conflict (cannot be auto-resolved)
        String rrGrammar = """
                %token ID EOF
                %start S
                %%
                S : A
                  | B
                  ;
                A : ID
                  ;
                B : ID
                  ;
                %%""";
        Path tmpGrammar = Path.of("target", "rr_conflict_test.y");
        Files.createDirectories(tmpGrammar.getParent());
        Files.writeString(tmpGrammar, rrGrammar);

        boolean thrown = false;
        try (Reader reader = new FileReader(tmpGrammar.toFile())) {
            new SeuYaccGenerator(reader, false);
        } catch (IllegalStateException ex) {
            String message = ex.getMessage() == null ? "" : ex.getMessage().toLowerCase();
            thrown = message.contains("reduce/reduce") || message.contains("conflict");
        }
        assertTrue(thrown, "reduce/reduce conflict grammar should trigger conflict reporting");
        System.out.println("[PASS] Conflict reporting verified");
    }

    // ── Action patterns (unchanged) ──

    private static void testActionPatternParsingAndRegistry() {
        ActionPatternParser parser = new ActionPatternParser();

        ActionPattern directAssign = parser.parse("$$ = $1;");
        assertTrue(directAssign.isDirectReferenceAssign(), "should be direct reference assign");
        assertEquals(1, directAssign.getDirectReferenceIndex(), "direct ref index mismatch");

        ActionPattern functionAssign = parser.parse("$$ = makeBinary(\"+\", $1, $3);");
        assertTrue(functionAssign.isFunctionCallAssign(), "should be function call assign");
        assertEquals("makeBinary", functionAssign.getInvocation().getFunctionName(), "function name mismatch");
        assertEquals(3, functionAssign.getInvocation().getArguments().size(), "arg count mismatch");

        ActionRegistry registry = ActionRegistry.defaultRegistry();
        assertTrue(registry.contains("makeProgram"), "should contain makeProgram");
        assertTrue(registry.contains("makeBinary"), "should contain makeBinary");
        assertTrue(!registry.contains("nonExistingAction"), "should not contain unknown");
        System.out.println("[PASS] Action pattern parsing and registry");
    }

    // ── C99 lexer/parser coverage ──

    private static void testC99LexerSpecTokenCoverage() {
        Map<TokenType, String> samples = c99LexerTokenSamples();
        Set<TokenType> covered = EnumSet.noneOf(TokenType.class);

        for (Map.Entry<TokenType, String> entry : samples.entrySet()) {
            Token token = firstToken(entry.getValue());
            assertEquals(entry.getKey(), token.type().canonical(),
                    "lexer sample should produce " + entry.getKey() + " from " + entry.getValue());
            covered.add(token.type().canonical());
        }

        Set<TokenType> expected = EnumSet.allOf(TokenType.class);
        expected.remove(TokenType.EOF);
        expected.remove(TokenType.TYPE_NAME); // c99.l documents check_type() as always returning IDENTIFIER here.
        expected.removeIf(TokenType::isAlias);

        Set<TokenType> missing = new LinkedHashSet<>(expected);
        missing.removeAll(covered);
        assertTrue(missing.isEmpty(), "c99.l token sample coverage missing: " + missing);

        List<Token> tokens = lexSource("int main() { return 0; }");
        assertEquals(TokenType.EOF, tokens.get(tokens.size() - 1).type().canonical(),
                "last token should be EOF");
        assertEquals(TokenType.INT, firstToken("/* block comment */ int").type().canonical(),
                "block comments should be skipped");
        assertEquals(TokenType.INT, firstToken("// line comment\nint").type().canonical(),
                "line comments should be skipped");
        assertEquals(TokenType.LBRACE, firstToken("<%").type().canonical(), "digraph <% should map to {");
        assertEquals(TokenType.RBRACE, firstToken("%>").type().canonical(), "digraph %> should map to }");
        assertEquals(TokenType.LBRACKET, firstToken("<:").type().canonical(), "digraph <: should map to [");
        assertEquals(TokenType.RBRACKET, firstToken(":>").type().canonical(), "digraph :> should map to ]");

        System.out.println("[PASS] C99 lexer token coverage (" + covered.size()
                + "/" + expected.size() + ", TYPE_NAME requires typedef-name tracking)");
    }

    private static void testC99LexerToParserIntegration() throws Exception {
        // Simple C99 program: int add(int x, int y) { return x + y; } int main() { return add(1, 2); }
        String source = """
                int add(int x, int y) { return x + y; }
                int main() { return add(1, 2); }
                """;

        List<Token> tokens = lexSource(source);

        // Parse with C99 grammar
        SeuYaccGenerator generator;
        try (Reader reader = new FileReader(grammarPath().toFile())) {
            generator = new SeuYaccGenerator(reader, true);
        }

        ParserDriver driver = new ParserDriver(generator.getGrammar(), generator.getParseTable());
        ParseResult parseResult = driver.parse(tokens);

        assertTrue(parseResult.isAccepted(),
                "Lexer→Parser integration should parse: " + parseResult.getErrorMessage());
        assertNotNull(parseResult.getAstRoot(), "parse tree should not be null");
        assertEquals("translation_unit", parseResult.getAstRoot().getSymbolName(),
                "root should be translation_unit");

        // Run semantic + IR pipeline
        YaccIrBridge bridge = new YaccIrBridge();
        IrGenerationResult ir = bridge.generate(parseResult);
        String llvmText = new LlvmLikeTextEmitter().emit(ir);

        assertTrue(llvmText.contains("define i32 @add"), "IR should contain add function");
        assertTrue(llvmText.contains("define i32 @main"), "IR should contain main function");
        assertTrue(llvmText.contains("call i32 @add"), "IR should contain function call");

        System.out.println("[PASS] C99 lexer/parser → MiniC semantic/IR integration");
    }

    private static void testC99ParserOnlySamplesAndProductionCoverage() throws Exception {
        SeuYaccGenerator generator;
        try (Reader reader = new FileReader(grammarPath().toFile())) {
            generator = new SeuYaccGenerator(reader, true);
        }

        ParserDriver driver = new ParserDriver(generator.getGrammar(), generator.getParseTable());
        Set<Integer> coveredProductions = new HashSet<>();

        for (String source : c99ParserOnlySamples()) {
            ParseResult parseResult = driver.parse(lexSource(source));
            assertTrue(parseResult.isAccepted(),
                    "C99 parser-only sample should parse:\n" + source + "\n" + parseResult.getErrorMessage());
            coveredProductions.addAll(parseResult.getReductions());
        }

        int totalProductions = generator.getGrammar().getProductions().size();
        assertTrue(!coveredProductions.isEmpty(), "parser-only samples should reduce at least one production");

        System.out.println("[PASS] C99 lexer + yacc parser-only samples (production coverage "
                + coveredProductions.size() + "/" + totalProductions
                + "; semantic/IR intentionally not run for full C99 samples)");
    }

    /**
     * Tokenize source code string using GeneratedLexer.
     */
    private static List<Token> lexSource(String source) {
        List<Token> tokens = new ArrayList<>();
        GeneratedLexer lexer = new GeneratedLexer(new StringReader(source));
        Token token;
        while (true) {
            token = lexer.nextToken();
            tokens.add(token);
            if (token.type() == TokenType.EOF) break;
        }
        return tokens;
    }

    // ── MiniC semantic/IR subset pipeline ──

    private static void testMiniCSubsetSemanticIrPipeline() throws Exception {
        SeuYaccGenerator generator;
        try (Reader reader = new FileReader(grammarPath().toFile())) {
            generator = new SeuYaccGenerator(reader, true);
        }

        ParserDriver driver = new ParserDriver(generator.getGrammar(), generator.getParseTable());
        ParseResult parseResult = driver.parse(TestSupport.validProgramTokens());

        assertTrue(parseResult.isAccepted(), "parse should succeed: " + parseResult.getErrorMessage());
        assertNotNull(parseResult.getAstRoot(), "AST root should not be null");
        assertEquals("translation_unit", parseResult.getAstRoot().getSymbolName(), "root should be translation_unit");
        assertTrue(!parseResult.getReductions().isEmpty(), "reductions should not be empty");

        YaccIrBridge bridge = new YaccIrBridge();
        SemanticResult semanticResult = bridge.analyze(parseResult);
        assertNotNull(semanticResult.astRoot(), "semantic AST should not be null");
        assertTrue(semanticResult.symbolTable().getAllSymbols().size() >= 2,
                "symbol table should contain functions");
        assertTrue(!semanticResult.preliminaryIr().isEmpty(),
                "runtime semantic translation should emit three-address IR");

        IrGenerationResult ir = bridge.generate(parseResult);
        String llvmText = new LlvmLikeTextEmitter().emit(ir);

        assertTrue(ir.getInstructions().size() > 0, "IR should not be empty");
        assertTrue(llvmText.contains("define i32 @add"), "IR should contain add function");
        assertTrue(llvmText.contains("define i32 @main"), "IR should contain main function");
        assertTrue(llvmText.contains("call i32 @add"), "IR should contain function call");
        assertTrue(llvmText.contains("br i1") || llvmText.contains("br label"),
                "IR should contain LLVM branch control-flow");

        System.out.println("[PASS] C99 parser + MiniC semantic/IR subset pipeline");
    }

    private static void testGeneratedCSemanticProgramPipeline() throws Exception {
        String source = """
                int add(int x, int y) { return x + y; }
                int main() { return add(3, 4); }
                """;

        var result = new Compiler().compileViaGeneratedC(source);
        assertTrue(result.isSuccess(), "generated C semantic route should parse successfully");
        String irText = result.irText();

        assertTrue(irText.contains("; generated by yysemantic.c"),
                "IR should be produced by generated C semantic program");
        assertTrue(irText.contains("define i32 @add(i32 %x, i32 %y)"),
                "generated C semantic program should emit function signature");
        assertTrue(irText.contains("call i32 @add"),
                "generated C semantic program should emit function call");
        assertTrue(Files.exists(Path.of("generated", "semantic", "yysemantic.c")),
                "generated C semantic source should be materialized");

        System.out.println("[PASS] Generated C semantic program → LLVM IR text pipeline");
    }

    private static void testGeneratedExecutablePipeline() throws Exception {
        String source = """
                int add(int x, int y) { return x + y; }
                int main() { return add(3, 4); }
                """;

        Path llFile = Path.of("generated", "final", "program.ll");
        Path executable = Path.of("generated", "final", "program.exe");
        var result = new Compiler().compileViaGeneratedC(
                new StringReader(source),
                Path.of("generated", "semantic"),
                llFile,
                executable
        );

        assertTrue(result.isSuccess(), "generated executable route should parse successfully");
        assertTrue(Files.exists(llFile), "LLVM IR file should be written");
        assertTrue(Files.exists(executable), "executable file should be written");

        Process process = new ProcessBuilder(executable.toAbsolutePath().toString()).start();
        int exit = process.waitFor();
        assertEquals(7, exit, "generated executable should return add(3, 4)");

        System.out.println("[PASS] LLVM IR text → executable pipeline");
    }

    // ── Markdown emission ──

    private static void testAstMarkdownEmission() throws Exception {
        SeuYaccGenerator generator;
        try (Reader reader = new FileReader(grammarPath().toFile())) {
            generator = new SeuYaccGenerator(reader, true);
        }

        ParserDriver driver = new ParserDriver(generator.getGrammar(), generator.getParseTable());
        ParseResult parseResult = driver.parse(TestSupport.validProgramTokens());

        assertTrue(parseResult.isAccepted(), "parse should succeed before markdown emission");

        AstMarkdownEmitter emitter = new AstMarkdownEmitter();

        Path parseTreeOutput = Path.of("generated", "parse-tree.md");
        emitter.writeParseTree(parseTreeOutput, parseResult.getAstRoot(), "C99 Parse Tree");

        YaccIrBridge bridge = new YaccIrBridge();
        SemanticResult semanticResult = bridge.analyze(parseResult);

        Path coreAstOutput = Path.of("generated", "core-ast.md");
        emitter.writeCoreAst(coreAstOutput, semanticResult.astRoot(), "Core Semantic AST");

        assertTrue(Files.exists(parseTreeOutput), "parse tree markdown should exist");
        assertTrue(Files.exists(coreAstOutput), "core AST markdown should exist");

        System.out.println("[PASS] AST markdown emission");
    }

    // ── Semantic error tests ──

    private static void testDuplicateDeclaration() throws Exception {
        SeuYaccGenerator generator;
        try (Reader reader = new FileReader(grammarPath().toFile())) {
            generator = new SeuYaccGenerator(reader, true);
        }

        ParserDriver driver = new ParserDriver(generator.getGrammar(), generator.getParseTable());
        ParseResult parseResult = driver.parse(TestSupport.duplicateDeclarationTokens());
        assertTrue(parseResult.isAccepted(), "duplicate declaration program should still parse");

        boolean thrown = false;
        try {
            new YaccIrBridge().analyze(parseResult);
        } catch (SemanticException ex) {
            thrown = ex.getMessage().contains("Duplicate declaration");
        }
        assertTrue(thrown, "duplicate declaration should raise SemanticException");
        System.out.println("[PASS] Duplicate declaration semantic check");
    }

    private static void testUndeclaredUse() throws Exception {
        SeuYaccGenerator generator;
        try (Reader reader = new FileReader(grammarPath().toFile())) {
            generator = new SeuYaccGenerator(reader, true);
        }

        ParserDriver driver = new ParserDriver(generator.getGrammar(), generator.getParseTable());
        ParseResult parseResult = driver.parse(TestSupport.undeclaredUseTokens());
        assertTrue(parseResult.isAccepted(), "undeclared use program should still parse");

        boolean thrown = false;
        try {
            new YaccIrBridge().analyze(parseResult);
        } catch (SemanticException ex) {
            thrown = ex.getMessage().contains("undeclared identifier");
        }
        assertTrue(thrown, "undeclared use should raise SemanticException");
        System.out.println("[PASS] Undeclared identifier semantic check");
    }

    private static void testUndefinedFunctionCall() throws Exception {
        SeuYaccGenerator generator;
        try (Reader reader = new FileReader(grammarPath().toFile())) {
            generator = new SeuYaccGenerator(reader, true);
        }

        ParserDriver driver = new ParserDriver(generator.getGrammar(), generator.getParseTable());
        ParseResult parseResult = driver.parse(TestSupport.undefinedFunctionCallTokens());
        assertTrue(parseResult.isAccepted(), "undefined function call program should still parse");

        boolean thrown = false;
        try {
            new YaccIrBridge().analyze(parseResult);
        } catch (SemanticException ex) {
            thrown = ex.getMessage().contains("undefined function");
        }
        assertTrue(thrown, "undefined function call should raise SemanticException");
        System.out.println("[PASS] Undefined function semantic check");
    }

    private static void testArgumentCountMismatch() throws Exception {
        SeuYaccGenerator generator;
        try (Reader reader = new FileReader(grammarPath().toFile())) {
            generator = new SeuYaccGenerator(reader, true);
        }

        ParserDriver driver = new ParserDriver(generator.getGrammar(), generator.getParseTable());
        ParseResult parseResult = driver.parse(TestSupport.argumentCountMismatchTokens());
        assertTrue(parseResult.isAccepted(), "argument mismatch program should still parse");

        boolean thrown = false;
        try {
            new YaccIrBridge().analyze(parseResult);
        } catch (SemanticException ex) {
            thrown = ex.getMessage().contains("Argument count mismatch");
        }
        assertTrue(thrown, "argument mismatch should raise SemanticException");
        System.out.println("[PASS] Function argument count semantic check");
    }

    // ── Helpers ──

    private static List<Token> lalrSampleTokens() {
        List<Token> tokens = new ArrayList<>();
        tokens.add(new Token(TokenType.STAR, "*"));
        tokens.add(new Token(TokenType.ID, "id"));
        tokens.add(new Token(TokenType.ASSIGN, "="));
        tokens.add(new Token(TokenType.ID, "id"));
        tokens.add(new Token(TokenType.EOF, "<EOF>"));
        return tokens;
    }

    private static Map<TokenType, String> c99LexerTokenSamples() {
        Map<TokenType, String> samples = new LinkedHashMap<>();

        samples.put(TokenType.AUTO, "auto");
        samples.put(TokenType.BOOL, "_Bool");
        samples.put(TokenType.BREAK, "break");
        samples.put(TokenType.CASE, "case");
        samples.put(TokenType.CHAR, "char");
        samples.put(TokenType.COMPLEX, "_Complex");
        samples.put(TokenType.CONST, "const");
        samples.put(TokenType.CONTINUE, "continue");
        samples.put(TokenType.DEFAULT, "default");
        samples.put(TokenType.DO, "do");
        samples.put(TokenType.DOUBLE, "double");
        samples.put(TokenType.ELSE, "else");
        samples.put(TokenType.ENUM, "enum");
        samples.put(TokenType.EXTERN, "extern");
        samples.put(TokenType.FLOAT, "float");
        samples.put(TokenType.FOR, "for");
        samples.put(TokenType.GOTO, "goto");
        samples.put(TokenType.IF, "if");
        samples.put(TokenType.IMAGINARY, "_Imaginary");
        samples.put(TokenType.INLINE, "inline");
        samples.put(TokenType.INT, "int");
        samples.put(TokenType.LONG, "long");
        samples.put(TokenType.REGISTER, "register");
        samples.put(TokenType.RESTRICT, "restrict");
        samples.put(TokenType.RETURN, "return");
        samples.put(TokenType.SHORT, "short");
        samples.put(TokenType.SIGNED, "signed");
        samples.put(TokenType.SIZEOF, "sizeof");
        samples.put(TokenType.STATIC, "static");
        samples.put(TokenType.STRUCT, "struct");
        samples.put(TokenType.SWITCH, "switch");
        samples.put(TokenType.TYPEDEF, "typedef");
        samples.put(TokenType.UNION, "union");
        samples.put(TokenType.UNSIGNED, "unsigned");
        samples.put(TokenType.VOID, "void");
        samples.put(TokenType.VOLATILE, "volatile");
        samples.put(TokenType.WHILE, "while");

        samples.put(TokenType.IDENTIFIER, "identifier_1");
        samples.put(TokenType.CONSTANT, "0x1p5");
        samples.put(TokenType.STRING_LITERAL, "\"hello\"");
        samples.put(TokenType.ELLIPSIS, "...");

        samples.put(TokenType.RIGHT_ASSIGN, ">>=");
        samples.put(TokenType.LEFT_ASSIGN, "<<=");
        samples.put(TokenType.ADD_ASSIGN, "+=");
        samples.put(TokenType.SUB_ASSIGN, "-=");
        samples.put(TokenType.MUL_ASSIGN, "*=");
        samples.put(TokenType.DIV_ASSIGN, "/=");
        samples.put(TokenType.MOD_ASSIGN, "%=");
        samples.put(TokenType.AND_ASSIGN, "&=");
        samples.put(TokenType.XOR_ASSIGN, "^=");
        samples.put(TokenType.OR_ASSIGN, "|=");
        samples.put(TokenType.RIGHT_OP, ">>");
        samples.put(TokenType.LEFT_OP, "<<");
        samples.put(TokenType.INC_OP, "++");
        samples.put(TokenType.DEC_OP, "--");
        samples.put(TokenType.PTR_OP, "->");
        samples.put(TokenType.AND_OP, "&&");
        samples.put(TokenType.OR_OP, "||");
        samples.put(TokenType.LE_OP, "<=");
        samples.put(TokenType.GE_OP, ">=");
        samples.put(TokenType.EQ_OP, "==");
        samples.put(TokenType.NE_OP, "!=");

        samples.put(TokenType.SEMI, ";");
        samples.put(TokenType.LBRACE, "{");
        samples.put(TokenType.RBRACE, "}");
        samples.put(TokenType.COMMA, ",");
        samples.put(TokenType.COLON, ":");
        samples.put(TokenType.ASSIGN, "=");
        samples.put(TokenType.LPAREN, "(");
        samples.put(TokenType.RPAREN, ")");
        samples.put(TokenType.LBRACKET, "[");
        samples.put(TokenType.RBRACKET, "]");
        samples.put(TokenType.DOT, ".");
        samples.put(TokenType.AMPERSAND, "&");
        samples.put(TokenType.BANG, "!");
        samples.put(TokenType.TILDE, "~");
        samples.put(TokenType.MINUS, "-");
        samples.put(TokenType.PLUS, "+");
        samples.put(TokenType.STAR, "*");
        samples.put(TokenType.SLASH, "/");
        samples.put(TokenType.PERCENT, "%");
        samples.put(TokenType.LT, "<");
        samples.put(TokenType.GT, ">");
        samples.put(TokenType.CARET, "^");
        samples.put(TokenType.PIPE, "|");
        samples.put(TokenType.QUESTION, "?");

        return samples;
    }

    private static List<String> c99ParserOnlySamples() {
        return List.of(
                """
                int main() {
                    return 0;
                }
                """,
                """
                static const int values[3] = {1, 2, 3};
                int sum(int a, int b) {
                    int i = 0;
                    int total = 0;
                    for (i = 0; i < 3; i = i + 1) {
                        total += values[i];
                    }
                    return total + a + b;
                }
                """,
                """
                struct Point { int x; int y; };
                union Value { int i; float f; };
                enum Color { RED, GREEN = 2, BLUE };
                int pick(enum Color c) {
                    switch (c) {
                    case RED:
                        return 1;
                    default:
                        return 0;
                    }
                }
                """,
                """
                int control(int x) {
                again:
                    do {
                        x--;
                        if (x == 2) continue;
                        if (x == 1) break;
                    } while (x > 0);
                    goto done;
                done:
                    return x;
                }
                """,
                """
                inline int call(int (*fn)(int), int value) {
                    return fn(value);
                }
                int arrays(int a[static const 3], int n) {
                    return sizeof(a[0]) + n;
                }
                """
        );
    }

    private static Token firstToken(String source) {
        return lexSource(source).get(0);
    }

    private static Path grammarPath() {
        return Path.of("resources", "c99.y");
    }

    private static Path testGrammarPath(String fileName) {
        return Path.of("src", "test", "resources", "grammars", fileName);
    }

    private static void assertTrue(boolean value, String message) {
        if (!value) {
            throw new AssertionError(message);
        }
    }

    private static void assertNotNull(Object value, String message) {
        if (value == null) {
            throw new AssertionError(message);
        }
    }

    private static void assertEquals(Object expected, Object actual, String message) {
        if (expected == null ? actual != null : !expected.equals(actual)) {
            throw new AssertionError(message + " | expected=" + expected + ", actual=" + actual);
        }
    }
}
