package com.compiler.yacc.ast;

import com.compiler.yacc.grammar.Grammar;
import com.compiler.yacc.grammar.SubsetGrammarFactory;
import com.compiler.yacc.lr1.CanonicalCollection;
import com.compiler.yacc.lr1.CanonicalCollectionBuilder;
import com.compiler.yacc.runtime.ParseResult;
import com.compiler.yacc.runtime.ParserDriver;
import com.compiler.yacc.support.MiniCLexer;
import com.compiler.yacc.table.ParseTable;
import com.compiler.yacc.table.ParseTableBuilder;
import com.compiler.yacc.token.Token;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class AstNormalizerTest {

    private ParserDriver createParser() {
        Grammar grammar = new SubsetGrammarFactory().build();
        CanonicalCollection collection = new CanonicalCollectionBuilder().build(grammar);
        ParseTable table = new ParseTableBuilder().build(grammar, collection);
        return new ParserDriver(grammar, table);
    }

    private List<Token> tokenize(String code) {
        return new MiniCLexer().tokenize(code);
    }

    private CoreAstNode normalize(String code) {
        ParserDriver parser = createParser();
        ParseResult result = parser.parse(tokenize(code));

        assertTrue(result.isAccepted(), result.getErrorMessage());
        assertNotNull(result.getAstRoot());

        AstNormalizer normalizer = new AstNormalizer();
        return normalizer.normalize(result.getAstRoot());
    }

    @Test
    void shouldNormalizeMinimalMainFunction() {
        CoreAstNode root = normalize("int main() { return 1; }");
        System.out.println(root.prettyPrint());

        assertEquals(AstKind.PROGRAM, root.getKind());
        assertEquals(1, root.getChildren().size());

        CoreAstNode main = root.getChildren().get(0);
        assertEquals(AstKind.MAIN_FUNCTION, main.getKind());

        CoreAstNode block = main.getChildren().get(0);
        assertEquals(AstKind.BLOCK, block.getKind());
        assertEquals(1, block.getChildren().size());

        CoreAstNode ret = block.getChildren().get(0);
        assertEquals(AstKind.RETURN_STMT, ret.getKind());

        CoreAstNode value = ret.getChildren().get(0);
        assertEquals(AstKind.INT_LITERAL, value.getKind());
        assertEquals("1", value.getText());
    }

    @Test
    void shouldNormalizeDeclarationAssignmentAndReturn() {
        CoreAstNode root = normalize("""
                int main() {
                    int a;
                    a = 1 + 2 * 3;
                    return a;
                }
                """);

        assertTrue(containsKind(root, AstKind.DECLARATION));
        assertTrue(containsKind(root, AstKind.ASSIGNMENT));
        assertTrue(containsBinaryOp(root, "+"));
        assertTrue(containsBinaryOp(root, "*"));
        assertTrue(containsIdentifier(root, "a"));
        assertTrue(containsIntLiteral(root, "1"));
        assertTrue(containsIntLiteral(root, "2"));
        assertTrue(containsIntLiteral(root, "3"));
    }

    @Test
    void shouldNormalizeIfElseAndWhile() {
        CoreAstNode root = normalize("""
                int main() {
                    int a = 3;
                    while (a > 0) {
                        if (a > 1) a = a - 1;
                        else a = 0;
                    }
                    return a;
                }
                """);

        assertTrue(containsKind(root, AstKind.WHILE_STMT));
        assertTrue(containsKind(root, AstKind.IF_STMT));
        assertTrue(containsBinaryOp(root, ">"));
        assertTrue(containsBinaryOp(root, "-"));
        assertTrue(containsKind(root, AstKind.ASSIGNMENT));
        assertTrue(containsKind(root, AstKind.RETURN_STMT));
    }

    private boolean containsKind(CoreAstNode node, AstKind kind) {
        if (node.getKind() == kind) {
            return true;
        }
        for (CoreAstNode child : node.getChildren()) {
            if (containsKind(child, kind)) {
                return true;
            }
        }
        return false;
    }

    private boolean containsBinaryOp(CoreAstNode node, String op) {
        if (node.getKind() == AstKind.BINARY_EXPR && op.equals(node.getText())) {
            return true;
        }
        for (CoreAstNode child : node.getChildren()) {
            if (containsBinaryOp(child, op)) {
                return true;
            }
        }
        return false;
    }

    private boolean containsIdentifier(CoreAstNode node, String name) {
        if (node.getKind() == AstKind.IDENTIFIER && name.equals(node.getText())) {
            return true;
        }
        for (CoreAstNode child : node.getChildren()) {
            if (containsIdentifier(child, name)) {
                return true;
            }
        }
        return false;
    }

    private boolean containsIntLiteral(CoreAstNode node, String value) {
        if (node.getKind() == AstKind.INT_LITERAL && value.equals(node.getText())) {
            return true;
        }
        for (CoreAstNode child : node.getChildren()) {
            if (containsIntLiteral(child, value)) {
                return true;
            }
        }
        return false;
    }
}
