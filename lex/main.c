#include <stdio.h>

extern int yylex();
extern char yytext[];
extern FILE *yyin;

int main(int argc, char **argv) {
    if (argc > 1) {
        yyin = fopen(argv[1], "r");
        if (!yyin) {
            printf("Cannot open file: %s\n", argv[1]);
            return 1;
        }
    } else {
        yyin = stdin;
    }

    int token;
    while ((token = yylex()) != 0) {
        if (token == -1) {
            printf("Error/Unknown char at: %s\n", yytext);
            break;
        }
        printf("Matched Token ID: %d, Lexeme: %s\n", token, yytext);
    }
    
    if (yyin != stdin) fclose(yyin);
    return 0;
}
