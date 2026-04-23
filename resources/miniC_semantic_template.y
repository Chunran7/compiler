%{
/*
 * C99 subset v1 for semantic / IR stage:
 * - multiple int functions
 * - int parameters
 * - declarations / assignments / blocks
 * - if / else / while / return
 * - function call expression and function call statement
 */
%}

%token INT MAIN IF ELSE WHILE RETURN
%token ID NUM
%token PLUS MINUS STAR SLASH
%token LT GT LE GE EQ NE
%token ASSIGN
%token SEMI COMMA LPAREN RPAREN LBRACE RBRACE
%token EOF

%start Program

%%

Program
    : FuncList
      { $$ = makeProgram($1); }
    ;

FuncList
    : FuncList FuncDef
      { $$ = appendFunction($1, $2); }
    | FuncDef
      { $$ = makeFunctionList($1); }
    ;

FuncDef
    : INT FuncName LPAREN ParamListOpt RPAREN Block
      { $$ = makeFunction($2, $4, $6); }
    ;

FuncName
    : ID
      { $$ = $1; }
    | MAIN
      { $$ = $1; }
    ;

ParamListOpt
    :
      { $$ = makeEmptyParamList(); }
    | ParamList
      { $$ = $1; }
    ;

ParamList
    : ParamList COMMA Param
      { $$ = appendParam($1, $3); }
    | Param
      { $$ = makeParamList($1); }
    ;

Param
    : INT ID
      { $$ = makeParam($2); }
    ;

Block
    : LBRACE ItemList RBRACE
      { $$ = makeBlock($2); }
    ;

ItemList
    :
      { $$ = makeEmptyItemList(); }
    | ItemList Item
      { $$ = appendItem($1, $2); }
    ;

Item
    : Decl
      { $$ = $1; }
    | Stmt
      { $$ = $1; }
    ;

Decl
    : INT ID DeclInitOpt SEMI
      { $$ = makeDeclaration($2, $3); }
    ;

DeclInitOpt
    :
      { $$ = makeNoInitializer(); }
    | ASSIGN Expr
      { $$ = makeInitializer($2); }
    ;

Stmt
    : MatchedStmt
      { $$ = $1; }
    | UnmatchedStmt
      { $$ = $1; }
    ;

MatchedStmt
    : AssignStmt
      { $$ = $1; }
    | ExprStmt
      { $$ = $1; }
    | ReturnStmt
      { $$ = $1; }
    | Block
      { $$ = $1; }
    | WHILE LPAREN Cond RPAREN MatchedStmt
      { $$ = makeWhile($3, $5); }
    | IF LPAREN Cond RPAREN MatchedStmt ELSE MatchedStmt
      { $$ = makeIfElse($3, $5, $7); }
    ;

UnmatchedStmt
    : IF LPAREN Cond RPAREN Stmt
      { $$ = makeIf($3, $5); }
    | IF LPAREN Cond RPAREN MatchedStmt ELSE UnmatchedStmt
      { $$ = makeIfElse($3, $5, $7); }
    | WHILE LPAREN Cond RPAREN UnmatchedStmt
      { $$ = makeWhile($3, $5); }
    ;

AssignStmt
    : ID ASSIGN Expr SEMI
      { $$ = makeAssignment($1, $3); }
    ;

ExprStmt
    : CallExpr SEMI
      { $$ = makeExprStmt($1); }
    ;

ReturnStmt
    : RETURN Expr SEMI
      { $$ = makeReturn($2); }
    ;

Cond
    : Expr RelOp Expr
      { $$ = makeCondition($1, $2, $3); }
    ;

RelOp
    : LT
      { $$ = makeRelOp($1); }
    | GT
      { $$ = makeRelOp($1); }
    | LE
      { $$ = makeRelOp($1); }
    | GE
      { $$ = makeRelOp($1); }
    | EQ
      { $$ = makeRelOp($1); }
    | NE
      { $$ = makeRelOp($1); }
    ;

Expr
    : Expr PLUS Term
      { $$ = makeBinary("+", $1, $3); }
    | Expr MINUS Term
      { $$ = makeBinary("-", $1, $3); }
    | Term
      { $$ = $1; }
    ;

Term
    : Term STAR Factor
      { $$ = makeBinary("*", $1, $3); }
    | Term SLASH Factor
      { $$ = makeBinary("/", $1, $3); }
    | Factor
      { $$ = $1; }
    ;

Factor
    : LPAREN Expr RPAREN
      { $$ = $2; }
    | ID
      { $$ = makeIdentifier($1); }
    | NUM
      { $$ = makeIntLiteral($1); }
    | CallExpr
      { $$ = $1; }
    ;

CallExpr
    : FuncName LPAREN ArgListOpt RPAREN
      { $$ = makeCall($1, $3); }
    ;

ArgListOpt
    :
      { $$ = makeEmptyArgList(); }
    | ArgList
      { $$ = $1; }
    ;

ArgList
    : ArgList COMMA Expr
      { $$ = appendArg($1, $3); }
    | Expr
      { $$ = makeArgList($1); }
    ;

%%