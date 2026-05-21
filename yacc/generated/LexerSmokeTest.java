import com.example.compiler.lex.GeneratedLexer;
import com.example.compiler.yacc.token.Token;
import com.example.compiler.yacc.token.TokenType;
import java.io.StringReader;

public class LexerSmokeTest {
    public static void main(String[] args) {
        String code = "int add(int x, int y) { return x + y; }";
        GeneratedLexer lexer = new GeneratedLexer(new StringReader(code));
        Token t;
        int count = 0;
        while ((t = lexer.nextToken()) != null && t.type() != TokenType.EOF && count < 30) {
            System.out.println(t.type() + " : '" + t.lexeme() + "'");
            count++;
        }
        System.out.println("Final token: " + (t != null ? t.type() : "null"));
    }
}
