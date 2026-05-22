%token ID STAR ASSIGN EOF
%start S
%%
S
    : L ASSIGN R
    | R
    ;

L
    : STAR R
    | ID
    ;

R
    : L
    ;
%%