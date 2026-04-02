//测试MiniCLexer
package com.compiler.yacc.token;

import com.compiler.yacc.support.MiniCLexer;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class MiniCLexerTest {

    void shouldTokenizeMiniCProgram() {
        String code = "int main() { int a = 3; if (a >= 1) a = a - 1; return a; }";

        MiniCLexer lexer = new MiniCLexer();
        List<TokenType> types = lexer.tokenize(code)
                .stream()
                .map(Token::getType)
                .collect(Collectors.toList());

        List<TokenType> expected = List.of(
                TokenType.INT, TokenType.MAIN, TokenType.LPAREN, TokenType.RPAREN,
                TokenType.LBRACE,
                TokenType.INT, TokenType.ID, TokenType.ASSIGN, TokenType.NUM, TokenType.SEMI,
                TokenType.IF, TokenType.LPAREN, TokenType.ID, TokenType.GE, TokenType.NUM, TokenType.RPAREN,
                TokenType.ID, TokenType.ASSIGN, TokenType.ID, TokenType.MINUS, TokenType.NUM, TokenType.SEMI,
                TokenType.RETURN, TokenType.ID, TokenType.SEMI,
                TokenType.RBRACE,
                TokenType.EOF
        );

        assertEquals(expected, types);
    }
}