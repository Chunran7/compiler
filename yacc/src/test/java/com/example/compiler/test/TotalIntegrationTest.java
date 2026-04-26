package com.example.compiler.test;

import com.example.compiler.ir.IrGenerationResult;
import com.example.compiler.ir.LlvmLikeTextEmitter;
import com.example.compiler.ir.YaccIrBridge;
import com.example.compiler.semantic.SemanticException;
import com.example.compiler.semantic.SemanticResult;
import com.example.compiler.semantic.TranslationSchemeExecutor;
import com.example.compiler.semantic.action.ActionArgument;
import com.example.compiler.semantic.action.ActionPattern;
import com.example.compiler.semantic.action.ActionPatternParser;
import com.example.compiler.semantic.action.ActionRegistry;
import com.example.compiler.semantic.emitter.SemanticProgramEmitter;
import com.example.compiler.yacc.ast.AstKind;
import com.example.compiler.yacc.ast.AstNode;
import com.example.compiler.yacc.ast.CoreAstNode;
import com.example.compiler.yacc.emitter.ParserProgramEmitter;
import com.example.compiler.yacc.generator.SeuYaccGenerator;
import com.example.compiler.yacc.grammar.Grammar;
import com.example.compiler.yacc.grammar.Production;
import com.example.compiler.yacc.runtime.ParseResult;
import com.example.compiler.yacc.runtime.ParserDriver;
import com.example.compiler.yacc.token.Token;
import com.example.compiler.yacc.token.TokenType;
import com.example.compiler.yacc.emitter.AstMarkdownEmitter;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.ByteArrayOutputStream;
import java.io.FileReader;
import java.io.PrintStream;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class TotalIntegrationTest {
    public static void main(String[] args) throws Exception {
        testGrammarParsingAndActionPreservation();
        testOperatorPrecedenceGrammar();
        testLalrStateMerging();
        testConflictReporting();
        testGeneratedParserProgramEmission();
        testGeneratedSemanticProgramEmission();
        testGeneratedProgramsCompile();
        testGeneratedProgramsRunSmokeTest();
        testSemanticActionNodesAppearInParseTree();
        testActionPatternParsingAndRegistry();
        testTranslationSchemeExecutorFirstPass();
        testAstMarkdownEmission();
        testYaccParsingAndIrGeneration();
        testDuplicateDeclaration();
        testUndeclaredUse();
        testUndefinedFunctionCall();
        testArgumentCountMismatch();
        System.out.println("=== ALL TESTS PASSED ===");
    }

    private static void testGrammarParsingAndActionPreservation() throws Exception {
        try (Reader reader = new FileReader(grammarPath().toFile())) {
            SeuYaccGenerator generator = new SeuYaccGenerator(reader, true);
            Grammar grammar = generator.getGrammar();

            assertNotNull(grammar.getStartSymbol(), "start symbol should exist");
            assertEquals("Program", grammar.getStartSymbol().getName(), "start symbol mismatch");
            assertTrue(generator.getCollection().states().size() > 0, "canonical/table collection should not be empty");

            Production programProduction = null;
            for (Production production : grammar.getProductions()) {
                if ("Program".equals(production.getLeft().getName())
                        && production.hasActionCode()
                        && production.getActionCode().contains("makeProgram")) {
                    programProduction = production;
                    break;
                }
            }

            assertNotNull(programProduction, "Program production with preserved action should exist");
            assertEquals("Program", programProduction.getLeft().getName(), "Program production lhs mismatch");
            assertTrue(programProduction.hasActionCode(), "Program production should preserve action code");
            assertTrue(programProduction.getActionCode().contains("makeProgram"), "Program action should contain makeProgram");
        }

        System.out.println("[PASS] Grammar parsing + action preservation");
    }

    private static void testOperatorPrecedenceGrammar() throws Exception {
        SeuYaccGenerator generator;
        try (Reader reader = new FileReader(Path.of("resources", "expr_precedence.y").toFile())) {
            generator = new SeuYaccGenerator(reader, false);
        }

        ParserDriver driver = new ParserDriver(generator.getGrammar(), generator.getParseTable());
        ParseResult result = driver.parse(TestSupport.precedenceExpressionTokens());

        assertTrue(result.isAccepted(), "precedence grammar should parse successfully: " + result.getErrorMessage());
        assertNotNull(result.getAstRoot(), "precedence parse tree should not be null");
        assertEquals("Expr", result.getAstRoot().getSymbolName(), "precedence parse root should be Expr");

        System.out.println("[PASS] Yacc precedence / associativity conflict resolution");
    }

    private static void testLalrStateMerging() throws Exception {
        int lr1States;
        int lalrStates;

        try (Reader reader = new FileReader(Path.of("resources", "lalr_core_merge.y").toFile())) {
            SeuYaccGenerator lr1Generator = new SeuYaccGenerator(reader, false);
            lr1States = lr1Generator.getCollection().states().size();
        }

        try (Reader reader = new FileReader(Path.of("resources", "lalr_core_merge.y").toFile())) {
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
            System.out.println("[PASS] LALR construction verified (no observable merge on this grammar: "
                    + lr1States + " -> " + lalrStates + ")");
        }
    }

    private static void testConflictReporting() throws Exception {
        boolean thrown = false;
        try (Reader reader = new FileReader(Path.of("resources", "conflict_ambiguous_expr.y").toFile())) {
            new SeuYaccGenerator(reader, false);
        } catch (IllegalStateException ex) {
            String message = ex.getMessage() == null ? "" : ex.getMessage().toLowerCase();
            thrown = message.contains("conflict");
        }

        assertTrue(thrown, "ambiguous grammar should trigger explicit conflict reporting");
        System.out.println("[PASS] Conflict reporting verified");
    }

    private static void testGeneratedParserProgramEmission() throws Exception {
        SeuYaccGenerator generator;
        try (Reader reader = new FileReader(grammarPath().toFile())) {
            generator = new SeuYaccGenerator(reader, true);
        }

        ParserProgramEmitter emitter = new ParserProgramEmitter();
        String source = emitter.emit("YYParseProgram", generator.getGrammar(), generator.getParseTable());

        assertTrue(source.contains("public final class YYParseProgram"), "generated parser source should contain class declaration");
        assertTrue(source.contains("public static ParseOutcome parse(List<Token> tokens)"), "generated parser source should contain parse method");
        assertTrue(source.contains("public static void main(String[] args)"), "generated parser source should contain main method");
        assertTrue(source.contains("putAction("), "generated parser source should contain ACTION table initialization");
        assertTrue(source.contains("putGoto("), "generated parser source should contain GOTO table initialization");

        Path output = Path.of("generated", "YYParseProgram.java");
        emitter.emitToFile(output, "YYParseProgram", generator.getGrammar(), generator.getParseTable());

        assertTrue(Files.exists(output), "generated parser source file should exist");
        String written = Files.readString(output);
        assertTrue(written.contains("PARSE ACCEPTED"), "written generated parser should contain runnable main output");

        System.out.println("[PASS] Generated parser program emission");
    }

    private static void testGeneratedSemanticProgramEmission() throws Exception {
        SemanticProgramEmitter emitter = new SemanticProgramEmitter();
        String source = emitter.emit("YYSemanticProgram");

        assertTrue(source.contains("public final class YYSemanticProgram"), "generated semantic source should contain class declaration");
        assertTrue(source.contains("SemanticActionEngine"), "generated semantic source should use SemanticActionEngine");
        assertTrue(source.contains("IrGenerator"), "generated semantic source should use IrGenerator");
        assertTrue(source.contains("public static void main(String[] args)"), "generated semantic source should contain main method");
        assertTrue(source.contains("SEMANTIC OK"), "generated semantic source should contain semantic success output");
        assertTrue(source.contains("=== PRELIMINARY TAC ==="), "generated semantic source should contain TAC output section");
        assertTrue(source.contains("=== LLVM-LIKE IR ==="), "generated semantic source should contain IR output section");

        Path output = Path.of("generated", "YYSemanticProgram.java");
        emitter.emitToFile(output, "YYSemanticProgram");

        assertTrue(Files.exists(output), "generated semantic source file should exist");
        String written = Files.readString(output);
        assertTrue(written.contains("Usage: java YYSemanticProgram <grammar-file> <token-file>"),
                "written generated semantic program should contain runnable usage message");

        System.out.println("[PASS] Generated semantic program emission");
    }

    private static void testGeneratedProgramsCompile() throws Exception {
        Path parserSource = Path.of("generated", "YYParseProgram.java");
        Path semanticSource = Path.of("generated", "YYSemanticProgram.java");

        assertTrue(Files.exists(parserSource), "generated parser source should exist before compilation");
        assertTrue(Files.exists(semanticSource), "generated semantic source should exist before compilation");

        Path outputDir = Path.of("generated-classes");
        Files.createDirectories(outputDir);

        compileJavaSource(parserSource, outputDir);
        compileJavaSource(semanticSource, outputDir);

        assertTrue(Files.exists(outputDir.resolve("YYParseProgram.class")),
                "compiled YYParseProgram.class should exist");
        assertTrue(Files.exists(outputDir.resolve("YYSemanticProgram.class")),
                "compiled YYSemanticProgram.class should exist");

        System.out.println("[PASS] Generated programs compile");
    }

    private static void testGeneratedProgramsRunSmokeTest() throws Exception {
        Path parserJava = Path.of("generated", "YYParseProgram.java");
        Path semanticJava = Path.of("generated", "YYSemanticProgram.java");
        Path classDir = Path.of("generated-classes");
        Path tokenFile = Path.of("generated", "sample.tokens");
        Path grammarFile = grammarPath();

        assertTrue(Files.exists(parserJava), "generated parser source should exist before smoke test");
        assertTrue(Files.exists(semanticJava), "generated semantic source should exist before smoke test");
        assertTrue(Files.exists(classDir.resolve("YYParseProgram.class")),
                "compiled YYParseProgram.class should exist before smoke test");
        assertTrue(Files.exists(classDir.resolve("YYSemanticProgram.class")),
                "compiled YYSemanticProgram.class should exist before smoke test");

        writeTokenFile(tokenFile, TestSupport.validProgramTokens());

        String parserOutput = runGeneratedProgram(
                "YYParseProgram",
                classDir,
                tokenFile.toString()
        );
        assertTrue(parserOutput.contains("PARSE ACCEPTED"),
                "generated parser program should accept the sample token file");

        String semanticOutput = runGeneratedProgram(
                "YYSemanticProgram",
                classDir,
                grammarFile.toString(),
                tokenFile.toString()
        );
        assertTrue(semanticOutput.contains("SEMANTIC OK"),
                "generated semantic program should complete semantic phase");
        assertTrue(semanticOutput.contains("=== PRELIMINARY TAC ==="),
                "generated semantic program should print TAC");
        assertTrue(semanticOutput.contains("=== LLVM-LIKE IR ==="),
                "generated semantic program should print LLVM-like IR");

        System.out.println("[PASS] Generated programs run smoke test");
    }

    private static void testSemanticActionNodesAppearInParseTree() throws Exception {
        SeuYaccGenerator generator;
        try (Reader reader = new FileReader(grammarPath().toFile())) {
            generator = new SeuYaccGenerator(reader, true);
        }

        ParserDriver driver = new ParserDriver(generator.getGrammar(), generator.getParseTable());
        ParseResult parseResult = driver.parse(TestSupport.validProgramTokens());

        assertTrue(parseResult.isAccepted(), "parse should succeed for semantic action node test");

        AstNode root = parseResult.getAstRoot();
        assertNotNull(root, "parse tree root should not be null");
        assertTrue(containsSemanticActionNode(root), "parse tree should contain semantic action nodes");

        System.out.println("[PASS] Semantic action nodes appear in parse tree");
    }

    private static void testActionPatternParsingAndRegistry() {
        ActionPatternParser parser = new ActionPatternParser();

        ActionPattern directAssign = parser.parse("$$ = $1;");
        assertTrue(directAssign.isDirectReferenceAssign(), "direct assign action should be parsed as direct reference");
        assertEquals(1, directAssign.getDirectReferenceIndex(), "direct assign ref index mismatch");

        ActionPattern functionAssign = parser.parse("$$ = makeBinary(\"+\", $1, $3);");
        assertTrue(functionAssign.isFunctionCallAssign(), "function assign action should be parsed as function call");
        assertEquals("makeBinary", functionAssign.getInvocation().getFunctionName(), "function name mismatch");
        assertEquals(3, functionAssign.getInvocation().getArguments().size(), "function arg size mismatch");

        ActionArgument arg0 = functionAssign.getInvocation().getArguments().get(0);
        ActionArgument arg1 = functionAssign.getInvocation().getArguments().get(1);
        ActionArgument arg2 = functionAssign.getInvocation().getArguments().get(2);

        assertTrue(arg0.isStringLiteral(), "arg0 should be a string literal");
        assertEquals("+", arg0.getText(), "arg0 literal mismatch");

        assertTrue(arg1.isPositionalRef(), "arg1 should be positional ref");
        assertEquals(1, arg1.getRefIndex(), "arg1 ref mismatch");

        assertTrue(arg2.isPositionalRef(), "arg2 should be positional ref");
        assertEquals(3, arg2.getRefIndex(), "arg2 ref mismatch");

        ActionRegistry registry = ActionRegistry.defaultRegistry();
        assertTrue(registry.contains("makeProgram"), "registry should contain makeProgram");
        assertTrue(registry.contains("makeBinary"), "registry should contain makeBinary");
        assertTrue(registry.contains("makeCall"), "registry should contain makeCall");
        assertTrue(!registry.contains("nonExistingAction"), "registry should not contain unknown action");

        System.out.println("[PASS] Action pattern parsing and registry");
    }

    private static void testTranslationSchemeExecutorFirstPass() throws Exception {
        SeuYaccGenerator generator;
        try (Reader reader = new FileReader(grammarPath().toFile())) {
            generator = new SeuYaccGenerator(reader, true);
        }

        ParserDriver driver = new ParserDriver(generator.getGrammar(), generator.getParseTable());
        ParseResult parseResult = driver.parse(TestSupport.validProgramTokens());

        assertTrue(parseResult.isAccepted(), "parse should succeed for translation scheme executor test");
        assertNotNull(parseResult.getAstRoot(), "parse tree root should not be null");

        TranslationSchemeExecutor executor = new TranslationSchemeExecutor();
        executor.execute(parseResult.getAstRoot());

        Object rootValue = parseResult.getAstRoot().getSemanticValue();
        assertNotNull(rootValue, "root semantic value should not be null after executing translation scheme");
        assertTrue(rootValue instanceof CoreAstNode, "root semantic value should be CoreAstNode");

        CoreAstNode rootNode = (CoreAstNode) rootValue;
        assertEquals(AstKind.PROGRAM, rootNode.getKind(), "root semantic AST kind mismatch");
        assertTrue(rootNode.getChildren().size() >= 2, "program semantic AST should contain functions");

        boolean hasMain = false;
        for (CoreAstNode child : rootNode.getChildren()) {
            if (child.getKind() == AstKind.MAIN_FUNCTION) {
                hasMain = true;
                break;
            }
        }
        assertTrue(hasMain, "semantic AST should contain main function");

        assertTrue(containsExecutedSemanticActionNode(parseResult.getAstRoot()),
                "at least one semantic action node should have executed and stored semanticValue");

        System.out.println("[PASS] Translation scheme executor first pass");
    }

    private static void testAstMarkdownEmission() throws Exception {
        SeuYaccGenerator generator;
        try (Reader reader = new FileReader(grammarPath().toFile())) {
            generator = new SeuYaccGenerator(reader, true);
        }

        ParserDriver driver = new ParserDriver(generator.getGrammar(), generator.getParseTable());
        ParseResult parseResult = driver.parse(TestSupport.validProgramTokens());

        assertTrue(parseResult.isAccepted(), "parse should succeed before markdown emission");
        assertNotNull(parseResult.getAstRoot(), "parse tree root should not be null");

        AstMarkdownEmitter emitter = new AstMarkdownEmitter();

        Path parseTreeOutput = Path.of("generated", "parse-tree.md");
        emitter.writeParseTree(
                parseTreeOutput,
                parseResult.getAstRoot(),
                "Parse Tree With Semantic Actions"
        );

        YaccIrBridge bridge = new YaccIrBridge();
        SemanticResult semanticResult = bridge.analyze(parseResult);

        Path coreAstOutput = Path.of("generated", "core-ast.md");
        emitter.writeCoreAst(
                coreAstOutput,
                semanticResult.astRoot(),
                "Core Semantic AST"
        );

        assertTrue(Files.exists(parseTreeOutput), "parse tree markdown should exist");
        assertTrue(Files.exists(coreAstOutput), "core AST markdown should exist");

        System.out.println("[PASS] AST markdown emission");
    }
    private static boolean containsExecutedSemanticActionNode(AstNode node) {
        if (node.isSemanticActionNode() && node.getSemanticValue() != null) {
            return true;
        }
        for (AstNode child : node.getChildren()) {
            if (containsExecutedSemanticActionNode(child)) {
                return true;
            }
        }
        return false;
    }

    private static boolean containsSemanticActionNode(AstNode node) {
        if (node.isSemanticActionNode()) {
            return true;
        }
        for (AstNode child : node.getChildren()) {
            if (containsSemanticActionNode(child)) {
                return true;
            }
        }
        return false;
    }

    private static void testYaccParsingAndIrGeneration() throws Exception {
        SeuYaccGenerator generator;
        try (Reader reader = new FileReader(grammarPath().toFile())) {
            generator = new SeuYaccGenerator(reader, true);
        }

        ParserDriver driver = new ParserDriver(generator.getGrammar(), generator.getParseTable());
        ParseResult parseResult = driver.parse(TestSupport.validProgramTokens());

        assertTrue(parseResult.isAccepted(), "parse should succeed: " + parseResult.getErrorMessage());
        assertNotNull(parseResult.getAstRoot(), "AST root should not be null");
        assertEquals("Program", parseResult.getAstRoot().getSymbolName(), "root should be Program");
        assertTrue(!parseResult.getReductions().isEmpty(), "reductions should not be empty");

        YaccIrBridge bridge = new YaccIrBridge();
        SemanticResult semanticResult = bridge.analyze(parseResult);
        assertNotNull(semanticResult.astRoot(), "semantic AST should not be null");
        assertTrue(semanticResult.symbolTable().getAllSymbols().size() >= 4,
                "symbol table should contain functions and variables");
        assertTrue(!semanticResult.preliminaryIr().isEmpty(),
                "semantic engine should emit preliminary TAC");

        IrGenerationResult ir = bridge.generate(parseResult);
        String llvmText = new LlvmLikeTextEmitter().emit(ir);

        assertTrue(ir.getInstructions().size() > 0, "IR should not be empty");
        assertTrue(llvmText.contains("define i32 @add()"), "IR text should contain add function");
        assertTrue(llvmText.contains("define i32 @main()"), "IR text should contain main function");
        assertTrue(llvmText.contains("call add("), "IR text should contain function call");
        assertTrue(llvmText.contains("ifFalse") || llvmText.contains("goto"),
                "IR text should contain control-flow");

        System.out.println("[PASS] Yacc + Semantic + IR pipeline");
    }

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

    private static List<Token> lalrSampleTokens() {
        List<Token> tokens = new ArrayList<>();
        tokens.add(new Token(TokenType.STAR, "*"));
        tokens.add(new Token(TokenType.ID, "id"));
        tokens.add(new Token(TokenType.ASSIGN, "="));
        tokens.add(new Token(TokenType.ID, "id"));
        tokens.add(new Token(TokenType.EOF, "<EOF>"));
        return tokens;
    }

    private static Path grammarPath() {
        return Path.of("resources", "miniC_semantic_template.y");
    }

    private static void compileJavaSource(Path sourceFile, Path outputDir) throws Exception {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        assertNotNull(compiler, "System Java compiler should be available");

        String classpath = System.getProperty("java.class.path");
        int exitCode = compiler.run(
                null,
                null,
                null,
                "-encoding", "UTF-8",
                "-cp", classpath,
                "-d", outputDir.toString(),
                sourceFile.toString()
        );

        assertTrue(exitCode == 0, "Compilation failed for " + sourceFile + ", exitCode=" + exitCode);
    }

    private static void writeTokenFile(Path file, List<Token> tokens) throws Exception {
        if (file.getParent() != null) {
            Files.createDirectories(file.getParent());
        }

        List<String> lines = new ArrayList<>();
        for (Token token : tokens) {
            lines.add(token.type().name() + " " + token.lexeme());
        }
        Files.write(file, lines);
    }

    private static String runGeneratedProgram(String className, Path classDir, String... args) throws Exception {
        try (var loader = new java.net.URLClassLoader(
                new java.net.URL[]{classDir.toUri().toURL()},
                TotalIntegrationTest.class.getClassLoader()
        )) {
            Class<?> clazz = Class.forName(className, true, loader);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PrintStream originalOut = System.out;
            PrintStream originalErr = System.err;

            try (PrintStream capture = new PrintStream(baos, true, "UTF-8")) {
                System.setOut(capture);
                System.setErr(capture);

                clazz.getMethod("main", String[].class).invoke(null, (Object) args);
            } finally {
                System.setOut(originalOut);
                System.setErr(originalErr);
            }

            return baos.toString("UTF-8");
        }
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