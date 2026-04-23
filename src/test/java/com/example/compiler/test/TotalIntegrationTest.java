package com.example.compiler.test;

import com.example.compiler.ir.IrGenerationResult;
import com.example.compiler.ir.LlvmLikeTextEmitter;
import com.example.compiler.ir.YaccIrBridge;
import com.example.compiler.semantic.SemanticException;
import com.example.compiler.semantic.SemanticResult;
import com.example.compiler.yacc.ast.AstNode;
import com.example.compiler.yacc.generator.SeuYaccGenerator;
import com.example.compiler.yacc.grammar.Grammar;
import com.example.compiler.yacc.grammar.Production;
import com.example.compiler.yacc.runtime.ParseResult;
import com.example.compiler.yacc.runtime.ParserDriver;
import com.example.compiler.yacc.token.Token;
import com.example.compiler.yacc.token.TokenType;

import java.io.FileReader;
import java.io.Reader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class TotalIntegrationTest {
    public static void main(String[] args) throws Exception {
        testGrammarParsingAndActionPreservation();
        testOperatorPrecedenceGrammar();
        testLalrStateMerging();
        testConflictReporting();
        testSemanticActionNodesAppearInParseTree();
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
        assertTrue(semanticResult.symbolTable().getAllSymbols().size() >= 4, "symbol table should contain functions and variables");
        assertTrue(!semanticResult.preliminaryIr().isEmpty(), "semantic engine should emit preliminary TAC");

        IrGenerationResult ir = bridge.generate(parseResult);

        System.out.println("=== DEBUG IR INSTRUCTIONS ===");
        for (var instruction : ir.getInstructions()) {
            System.out.println(instruction);
        }

        String llvmText = new LlvmLikeTextEmitter().emit(ir);
        System.out.println("=== DEBUG LLVM TEXT ===");
        System.out.println(llvmText);

        assertTrue(ir.getInstructions().size() > 0, "IR should not be empty");
        assertTrue(llvmText.contains("define i32 @add()"), "IR text should contain add function");
        assertTrue(llvmText.contains("define i32 @main()"), "IR text should contain main function");
        assertTrue(llvmText.contains("call add("), "IR text should contain function call");
        assertTrue(llvmText.contains("ifFalse") || llvmText.contains("goto"), "IR text should contain control-flow");

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