%token NUM PLUS STAR EOF
%start Expr
%%
Expr
    : Expr PLUS Expr
    | Expr STAR Expr
    | NUM
    ;
%%