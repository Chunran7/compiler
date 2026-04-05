package com.compiler.yacc.runtime;

import com.compiler.yacc.ast.AstNode;
import com.compiler.yacc.grammar.Grammar;
import com.compiler.yacc.grammar.SubsetGrammarFactory;
import com.compiler.yacc.lr1.CanonicalCollection;
import com.compiler.yacc.lr1.CanonicalCollectionBuilder;
import com.compiler.yacc.support.MiniCLexer;
import com.compiler.yacc.table.ParseTable;
import com.compiler.yacc.table.ParseTableBuilder;
import com.compiler.yacc.token.Token;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class ParserDriverAstTest {

    private ParserDriver createParser() {
        Grammar grammar = new SubsetGrammarFactory().build();
        CanonicalCollection collection = new CanonicalCollectionBuilder().build(grammar);
        ParseTable table = new ParseTableBuilder().build(grammar, collection);
        return new ParserDriver(grammar, table);
    }

    private List<Token> tokenize(String code) {
        return new MiniCLexer().tokenize(code);
    }

    @Test
    void shouldBuildAstForMinimalMainFunction() {
        ParserDriver parser = createParser();

        String code = "int main() { return 1; }";
        ParseResult result = parser.parse(tokenize(code));

        System.out.println(result.getAstRoot().prettyPrint());
        assertTrue(result.isAccepted(), result.getErrorMessage());
        assertNotNull(result.getAstRoot());

        AstNode root = result.getAstRoot();
        assertEquals("Program", root.getSymbolName());

        assertTrue(containsSymbol(root, "MainFunc"));
        assertTrue(containsSymbol(root, "ReturnStmt"));
        assertTrue(containsLeaf(root, "NUM", "1"));
    }

    @Test
    void shouldBuildAstForAssignmentExpression() {
        ParserDriver parser = createParser();

        String code = """
                int main() {
                    int a;
                    a = 1 + 2 * 3;
                    return a;
                }
                """;

        ParseResult result = parser.parse(tokenize(code));

        assertTrue(result.isAccepted(), result.getErrorMessage());
        assertNotNull(result.getAstRoot());

        AstNode root = result.getAstRoot();

        assertTrue(containsSymbol(root, "Decl"));
        assertTrue(containsSymbol(root, "AssignStmt"));
        assertTrue(containsSymbol(root, "Expr"));
        assertTrue(containsLeaf(root, "NUM", "1"));
        assertTrue(containsLeaf(root, "NUM", "2"));
        assertTrue(containsLeaf(root, "NUM", "3"));
        assertTrue(containsLeaf(root, "STAR", "*"));
        assertTrue(containsLeaf(root, "PLUS", "+"));
    }

    @Test
    void shouldBuildAstForIfElseAndWhile() {
        ParserDriver parser = createParser();

        String code = """
                int main() {
                    int a = 3;
                    while (a > 0) {
                        if (a > 1) a = a - 1;
                        else a = 0;
                    }
                    return a;
                }
                """;

        ParseResult result = parser.parse(tokenize(code));

        assertTrue(result.isAccepted(), result.getErrorMessage());
        assertNotNull(result.getAstRoot());

        AstNode root = result.getAstRoot();

        assertTrue(containsLeaf(root, "WHILE", "while"));
        assertTrue(containsLeaf(root, "IF", "if"));
        assertTrue(containsLeaf(root, "ELSE", "else"));
        assertTrue(containsSymbol(root, "Stmt"));
        assertTrue(containsSymbol(root, "Cond"));
    }

    private boolean containsSymbol(AstNode node, String symbolName) {
        if (node.getSymbolName().equals(symbolName)) {
            return true;
        }
        for (AstNode child : node.getChildren()) {
            if (containsSymbol(child, symbolName)) {
                return true;
            }
        }
        return false;
    }

    private boolean containsLeaf(AstNode node, String symbolName, String lexeme) {
        if (node.getSymbolName().equals(symbolName)) {
            if (lexeme == null) {
                return true;
            }
            if (lexeme.equals(node.getLexeme())) {
                return true;
            }
        }
        for (AstNode child : node.getChildren()) {
            if (containsLeaf(child, symbolName, lexeme)) {
                return true;
            }
        }
        return false;
    }
}
