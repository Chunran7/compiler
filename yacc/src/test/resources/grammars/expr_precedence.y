%token NUM PLUS STAR LPAREN RPAREN EOF
%left PLUS
%left STAR
%start Expr
%%
Expr
    : Expr PLUS Expr
    | Expr STAR Expr
    | LPAREN Expr RPAREN
    | NUM
    ;
%%