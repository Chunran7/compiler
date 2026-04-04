package com.compiler.yacc.runtime;

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

public class ParserDriverTest {

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
    void shouldAcceptMinimalMainFunction() {
        ParserDriver parser = createParser();

        String code = "int main() { return 1; }";
        ParseResult result = parser.parse(tokenize(code));

        assertTrue(result.isAccepted(), result.getErrorMessage());
        assertFalse(result.getReductions().isEmpty());
    }

    @Test
    void shouldAcceptDeclarationAssignmentAndReturn() {
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
    }

    @Test
    void shouldAcceptIfElseStatement() {
        ParserDriver parser = createParser();

        String code = """
                int main() {
                    int a = 3;
                    if (a > 0) a = a - 1;
                    else a = 0;
                    return a;
                }
                """;

        ParseResult result = parser.parse(tokenize(code));

        assertTrue(result.isAccepted(), result.getErrorMessage());
    }

    @Test
    void shouldAcceptWhileStatement() {
        ParserDriver parser = createParser();

        String code = """
                int main() {
                    int a = 3;
                    while (a > 0) a = a - 1;
                    return a;
                }
                """;

        ParseResult result = parser.parse(tokenize(code));

        assertTrue(result.isAccepted(), result.getErrorMessage());
    }

    @Test
    void shouldAcceptNestedBlock() {
        ParserDriver parser = createParser();

        String code = """
                int main() {
                    int a = 3;
                    {
                        a = a + 1;
                    }
                    return a;
                }
                """;

        ParseResult result = parser.parse(tokenize(code));

        assertTrue(result.isAccepted(), result.getErrorMessage());
    }

    @Test
    void shouldRejectBrokenMainFunction() {
        ParserDriver parser = createParser();

        String code = "int main( { return 1; }";
        ParseResult result = parser.parse(tokenize(code));

        assertFalse(result.isAccepted());
        assertNotNull(result.getErrorMessage());
    }

    @Test
    void shouldRejectInvalidDeclaration() {
        ParserDriver parser = createParser();

        String code = "int main() { int = 3; return 1; }";
        ParseResult result = parser.parse(tokenize(code));

        assertFalse(result.isAccepted());
        assertNotNull(result.getErrorMessage());
    }
}