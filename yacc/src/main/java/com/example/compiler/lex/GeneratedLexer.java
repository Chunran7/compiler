package com.example.compiler.lex;

import java.io.*;
import com.example.compiler.yacc.token.*;

public class GeneratedLexer {
    // #include <stdio.h>
    // #include "y.tab.h"
    // void count(void);   /* 计算词法元素所占用的列，在规则部分被使用，函数体在第二个%%之后 */ (forward decl)
    private PushbackReader yyin;
    public char[] yytext = new char[4096];
    public int yyleng = 0;

    public GeneratedLexer(Reader in) {
        this.yyin = new PushbackReader(in, 4096);
    }

    public GeneratedLexer(InputStream in) {
        this.yyin = new PushbackReader(new InputStreamReader(in), 4096);
    }

    private static final int[][] transition_table;
    private static final int[] accept_rule;
    static {
        try (DataInputStream dis = new DataInputStream(GeneratedLexer.class.getResourceAsStream("/lexer_tables.dat") != null ? GeneratedLexer.class.getResourceAsStream("/lexer_tables.dat") : new FileInputStream("src/main/resources/lexer_tables.dat"))) {
            int r = dis.readInt();
            int c = dis.readInt();
            transition_table = new int[r][c];
            for (int i = 0; i < r; i++) {
                for (int j = 0; j < c; j++) {
                    transition_table[i][j] = dis.readInt();
                }
            }
            accept_rule = new int[r];
            for(int i = 0; i < r; i++) accept_rule[i] = dis.readInt();
        } catch (Exception e) {
            throw new RuntimeException("Failed to load lexer tables", e);
        }
    }

    private int input() {
        try {
            return yyin.read();
        } catch (IOException e) {
            return -1;
        }
    }

    private void ungetc(int c) {
        try { yyin.unread(c); } catch (IOException e) { throw new RuntimeException(e); }
    }

    public Token nextToken() {
        int first = input();
        if (first == '/') {
            int second = input();
            if (second == '*') {
                comment();
                return nextToken();
            }
            if (second == '/') {
                skipLineComment();
                return nextToken();
            }
            if (second != -1) {
                ungetc(second);
            }
            ungetc(first);
        } else if (first != -1) {
            ungetc(first);
        }

        int state = 0;
        int last_accept_state = -1;
        int last_accept_len = 0;
        yyleng = 0;
        int c;
        while ((c = input()) != -1 && c != 0) {
            if (c < 0 || c >= 256) break;
            yytext[yyleng++] = (char)c;
            int next_state = transition_table[state][c];
            if (next_state == -1) {
                break;
            }
            state = next_state;
            if (accept_rule[state] != -1) {
                last_accept_state = state;
                last_accept_len = yyleng;
            }
        }
        if (last_accept_state != -1) {
            for (int i = yyleng - 1; i >= last_accept_len; i--) {
                ungetc(yytext[i]);
            }
            yyleng = last_accept_len;

            switch (accept_rule[last_accept_state]) {
                case 1:
                    { comment(); }
                    break;
                case 2:
                    {  }
                    break;
                case 3:
                    { count(); return new Token(TokenType.AUTO, new String(yytext, 0, yyleng)); }
                case 4:
                    { count(); return new Token(TokenType.BOOL, new String(yytext, 0, yyleng)); }
                case 5:
                    { count(); return new Token(TokenType.BREAK, new String(yytext, 0, yyleng)); }
                case 6:
                    { count(); return new Token(TokenType.CASE, new String(yytext, 0, yyleng)); }
                case 7:
                    { count(); return new Token(TokenType.CHAR, new String(yytext, 0, yyleng)); }
                case 8:
                    { count(); return new Token(TokenType.COMPLEX, new String(yytext, 0, yyleng)); }
                case 9:
                    { count(); return new Token(TokenType.CONST, new String(yytext, 0, yyleng)); }
                case 10:
                    { count(); return new Token(TokenType.CONTINUE, new String(yytext, 0, yyleng)); }
                case 11:
                    { count(); return new Token(TokenType.DEFAULT, new String(yytext, 0, yyleng)); }
                case 12:
                    { count(); return new Token(TokenType.DO, new String(yytext, 0, yyleng)); }
                case 13:
                    { count(); return new Token(TokenType.DOUBLE, new String(yytext, 0, yyleng)); }
                case 14:
                    { count(); return new Token(TokenType.ELSE, new String(yytext, 0, yyleng)); }
                case 15:
                    { count(); return new Token(TokenType.ENUM, new String(yytext, 0, yyleng)); }
                case 16:
                    { count(); return new Token(TokenType.EXTERN, new String(yytext, 0, yyleng)); }
                case 17:
                    { count(); return new Token(TokenType.FLOAT, new String(yytext, 0, yyleng)); }
                case 18:
                    { count(); return new Token(TokenType.FOR, new String(yytext, 0, yyleng)); }
                case 19:
                    { count(); return new Token(TokenType.GOTO, new String(yytext, 0, yyleng)); }
                case 20:
                    { count(); return new Token(TokenType.IF, new String(yytext, 0, yyleng)); }
                case 21:
                    { count(); return new Token(TokenType.IMAGINARY, new String(yytext, 0, yyleng)); }
                case 22:
                    { count(); return new Token(TokenType.INLINE, new String(yytext, 0, yyleng)); }
                case 23:
                    { count(); return new Token(TokenType.INT, new String(yytext, 0, yyleng)); }
                case 24:
                    { count(); return new Token(TokenType.LONG, new String(yytext, 0, yyleng)); }
                case 25:
                    { count(); return new Token(TokenType.REGISTER, new String(yytext, 0, yyleng)); }
                case 26:
                    { count(); return new Token(TokenType.RESTRICT, new String(yytext, 0, yyleng)); }
                case 27:
                    { count(); return new Token(TokenType.RETURN, new String(yytext, 0, yyleng)); }
                case 28:
                    { count(); return new Token(TokenType.SHORT, new String(yytext, 0, yyleng)); }
                case 29:
                    { count(); return new Token(TokenType.SIGNED, new String(yytext, 0, yyleng)); }
                case 30:
                    { count(); return new Token(TokenType.SIZEOF, new String(yytext, 0, yyleng)); }
                case 31:
                    { count(); return new Token(TokenType.STATIC, new String(yytext, 0, yyleng)); }
                case 32:
                    { count(); return new Token(TokenType.STRUCT, new String(yytext, 0, yyleng)); }
                case 33:
                    { count(); return new Token(TokenType.SWITCH, new String(yytext, 0, yyleng)); }
                case 34:
                    { count(); return new Token(TokenType.TYPEDEF, new String(yytext, 0, yyleng)); }
                case 35:
                    { count(); return new Token(TokenType.UNION, new String(yytext, 0, yyleng)); }
                case 36:
                    { count(); return new Token(TokenType.UNSIGNED, new String(yytext, 0, yyleng)); }
                case 37:
                    { count(); return new Token(TokenType.VOID, new String(yytext, 0, yyleng)); }
                case 38:
                    { count(); return new Token(TokenType.VOLATILE, new String(yytext, 0, yyleng)); }
                case 39:
                    { count(); return new Token(TokenType.WHILE, new String(yytext, 0, yyleng)); }
                case 40:
                    { count(); return(check_type()); }
                case 41:
                    { count(); return new Token(TokenType.CONSTANT, new String(yytext, 0, yyleng)); }
                case 42:
                    { count(); return new Token(TokenType.CONSTANT, new String(yytext, 0, yyleng)); }
                case 43:
                    { count(); return new Token(TokenType.CONSTANT, new String(yytext, 0, yyleng)); }
                case 44:
                    { count(); return new Token(TokenType.CONSTANT, new String(yytext, 0, yyleng)); }
                case 45:
                    { count(); return new Token(TokenType.CONSTANT, new String(yytext, 0, yyleng)); }
                case 46:
                    { count(); return new Token(TokenType.CONSTANT, new String(yytext, 0, yyleng)); }
                case 47:
                    { count(); return new Token(TokenType.CONSTANT, new String(yytext, 0, yyleng)); }
                case 48:
                    { count(); return new Token(TokenType.CONSTANT, new String(yytext, 0, yyleng)); }
                case 49:
                    { count(); return new Token(TokenType.CONSTANT, new String(yytext, 0, yyleng)); }
                case 50:
                    { count(); return new Token(TokenType.CONSTANT, new String(yytext, 0, yyleng)); }
                case 51:
                    { count(); return new Token(TokenType.STRING_LITERAL, new String(yytext, 0, yyleng)); }
                case 52:
                    { count(); return new Token(TokenType.ELLIPSIS, new String(yytext, 0, yyleng)); }
                case 53:
                    { count(); return new Token(TokenType.RIGHT_ASSIGN, new String(yytext, 0, yyleng)); }
                case 54:
                    { count(); return new Token(TokenType.LEFT_ASSIGN, new String(yytext, 0, yyleng)); }
                case 55:
                    { count(); return new Token(TokenType.ADD_ASSIGN, new String(yytext, 0, yyleng)); }
                case 56:
                    { count(); return new Token(TokenType.SUB_ASSIGN, new String(yytext, 0, yyleng)); }
                case 57:
                    { count(); return new Token(TokenType.MUL_ASSIGN, new String(yytext, 0, yyleng)); }
                case 58:
                    { count(); return new Token(TokenType.DIV_ASSIGN, new String(yytext, 0, yyleng)); }
                case 59:
                    { count(); return new Token(TokenType.MOD_ASSIGN, new String(yytext, 0, yyleng)); }
                case 60:
                    { count(); return new Token(TokenType.AND_ASSIGN, new String(yytext, 0, yyleng)); }
                case 61:
                    { count(); return new Token(TokenType.XOR_ASSIGN, new String(yytext, 0, yyleng)); }
                case 62:
                    { count(); return new Token(TokenType.OR_ASSIGN, new String(yytext, 0, yyleng)); }
                case 63:
                    { count(); return new Token(TokenType.RIGHT_OP, new String(yytext, 0, yyleng)); }
                case 64:
                    { count(); return new Token(TokenType.LEFT_OP, new String(yytext, 0, yyleng)); }
                case 65:
                    { count(); return new Token(TokenType.INC_OP, new String(yytext, 0, yyleng)); }
                case 66:
                    { count(); return new Token(TokenType.DEC_OP, new String(yytext, 0, yyleng)); }
                case 67:
                    { count(); return new Token(TokenType.PTR_OP, new String(yytext, 0, yyleng)); }
                case 68:
                    { count(); return new Token(TokenType.AND_OP, new String(yytext, 0, yyleng)); }
                case 69:
                    { count(); return new Token(TokenType.OR_OP, new String(yytext, 0, yyleng)); }
                case 70:
                    { count(); return new Token(TokenType.LE_OP, new String(yytext, 0, yyleng)); }
                case 71:
                    { count(); return new Token(TokenType.GE_OP, new String(yytext, 0, yyleng)); }
                case 72:
                    { count(); return new Token(TokenType.EQ_OP, new String(yytext, 0, yyleng)); }
                case 73:
                    { count(); return new Token(TokenType.NE_OP, new String(yytext, 0, yyleng)); }
                case 74:
                    { count(); return new Token(TokenType.forChar(';'), new String(yytext, 0, yyleng)); }
                case 75:
                    { count(); return new Token(TokenType.forChar('{'), new String(yytext, 0, yyleng)); }
                case 76:
                    { count(); return new Token(TokenType.forChar('}'), new String(yytext, 0, yyleng)); }
                case 77:
                    { count(); return new Token(TokenType.forChar(','), new String(yytext, 0, yyleng)); }
                case 78:
                    { count(); return new Token(TokenType.forChar(':'), new String(yytext, 0, yyleng)); }
                case 79:
                    { count(); return new Token(TokenType.forChar('='), new String(yytext, 0, yyleng)); }
                case 80:
                    { count(); return new Token(TokenType.forChar('('), new String(yytext, 0, yyleng)); }
                case 81:
                    { count(); return new Token(TokenType.forChar(')'), new String(yytext, 0, yyleng)); }
                case 82:
                    { count(); return new Token(TokenType.forChar('['), new String(yytext, 0, yyleng)); }
                case 83:
                    { count(); return new Token(TokenType.forChar(']'), new String(yytext, 0, yyleng)); }
                case 84:
                    { count(); return new Token(TokenType.forChar('.'), new String(yytext, 0, yyleng)); }
                case 85:
                    { count(); return new Token(TokenType.forChar('&'), new String(yytext, 0, yyleng)); }
                case 86:
                    { count(); return new Token(TokenType.forChar('!'), new String(yytext, 0, yyleng)); }
                case 87:
                    { count(); return new Token(TokenType.forChar('~'), new String(yytext, 0, yyleng)); }
                case 88:
                    { count(); return new Token(TokenType.forChar('-'), new String(yytext, 0, yyleng)); }
                case 89:
                    { count(); return new Token(TokenType.forChar('+'), new String(yytext, 0, yyleng)); }
                case 90:
                    { count(); return new Token(TokenType.forChar('*'), new String(yytext, 0, yyleng)); }
                case 91:
                    { count(); return new Token(TokenType.forChar('/'), new String(yytext, 0, yyleng)); }
                case 92:
                    { count(); return new Token(TokenType.forChar('%'), new String(yytext, 0, yyleng)); }
                case 93:
                    { count(); return new Token(TokenType.forChar('<'), new String(yytext, 0, yyleng)); }
                case 94:
                    { count(); return new Token(TokenType.forChar('>'), new String(yytext, 0, yyleng)); }
                case 95:
                    { count(); return new Token(TokenType.forChar('^'), new String(yytext, 0, yyleng)); }
                case 96:
                    { count(); return new Token(TokenType.forChar('|'), new String(yytext, 0, yyleng)); }
                case 97:
                    { count(); return new Token(TokenType.forChar('?'), new String(yytext, 0, yyleng)); }
                case 98:
                    { count(); }
                    break;
                case 99:
                    {  }
                    break;
            }
            return nextToken();
        } else if (c == -1 || c == 0) {
            if (yyleng == 0) return new Token(TokenType.EOF, "EOF");
            else throw new RuntimeException("Lexer error: unexpected end of file");
        }
        throw new RuntimeException("Lexer error: unexpected character '" + (char)c + "'");
    }

    private void error(String msg) {
    throw new RuntimeException("error: %s\n" + msg);
}

    private void comment() {
	char c, prev = 0;
  
	while ((c = input()) != -1 && c != 0)      
	{
		if (c == '/' && prev == '*')     
			return;
		prev = c;
	}
	error("unterminated comment");
}

    public int column = 0;
    private void count() {
	for (int i = 0; i < yyleng; i++)         
		if (yytext[i] == '\n')
			column = 0;
		else if (yytext[i] == '\t')
			column += 8 - (column % 8);
		else
			column++;

	System.out.print(new String(yytext, 0, yyleng));
}

    private void skipLineComment() {
        int c;
        while ((c = input()) != -1 && c != 0) {
            if (c == '\n') return;
        }
    }

    private Token check_type() {




	return new Token(TokenType.IDENTIFIER, new String(yytext, 0, yyleng));
}

}
