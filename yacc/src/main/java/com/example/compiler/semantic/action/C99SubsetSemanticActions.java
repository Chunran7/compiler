package com.example.compiler.semantic.action;

import com.example.compiler.yacc.grammar.Grammar;
import com.example.compiler.yacc.grammar.Production;
import com.example.compiler.yacc.grammar.Symbol;

import java.util.LinkedHashMap;
import java.util.Map;

public final class C99SubsetSemanticActions {
    private C99SubsetSemanticActions() {
    }

    public static Map<Integer, String> resolve(Grammar grammar) {
        Map<Integer, String> actions = new LinkedHashMap<>();
        for (Production production : grammar.getProductions()) {
            String action = actionFor(signature(production));
            if (action != null) {
                actions.put(production.getId(), action);
            }
        }
        return actions;
    }

    private static String signature(Production production) {
        StringBuilder sb = new StringBuilder();
        sb.append(production.getLeft().getName()).append(" ->");
        if (production.getRight().isEmpty()) {
            sb.append(" ε");
            return sb.toString();
        }
        for (Symbol symbol : production.getRight()) {
            sb.append(" ").append(symbol.getName());
        }
        return sb.toString();
    }

    private static String actionFor(String signature) {
        return switch (signature) {
            case "translation_unit -> external_declaration" -> "{ $$ = makeProgramFromExternal($1); }";
            case "translation_unit -> translation_unit external_declaration" -> "{ $$ = appendProgramExternal($1, $2); }";
            case "external_declaration -> function_definition" -> "{ $$ = $1; }";

            case "function_definition -> declaration_specifiers declarator compound_statement" -> "{ $$ = makeFunctionNoParams($2, $3); }";
            case "declarator -> direct_declarator" -> "{ $$ = $1; }";
            case "direct_declarator -> IDENTIFIER" -> "{ $$ = $1; }";
            case "direct_declarator -> direct_declarator LPAREN RPAREN" -> "{ $$ = $1; }";

            case "compound_statement -> LBRACE RBRACE" -> "{ $$ = makeEmptyBlock(); }";
            case "compound_statement -> LBRACE block_item_list RBRACE" -> "{ $$ = makeBlock($2); }";
            case "block_item_list -> block_item" -> "{ $$ = makeItemList($1); }";
            case "block_item_list -> block_item_list block_item" -> "{ $$ = appendItem($1, $2); }";
            case "block_item -> declaration" -> "{ $$ = $1; }";
            case "block_item -> statement" -> "{ $$ = $1; }";

            case "declaration -> declaration_specifiers init_declarator_list SEMI" -> "{ $$ = $2; }";
            case "init_declarator_list -> init_declarator" -> "{ $$ = $1; }";
            case "init_declarator -> declarator" -> "{ $$ = makeBareDeclaration($1); }";
            case "init_declarator -> declarator ASSIGN initializer" -> "{ $$ = makeInitializedDeclaration($1, $3); }";
            case "initializer -> assignment_expression" -> "{ $$ = $1; }";

            case "statement -> compound_statement" -> "{ $$ = $1; }";
            case "statement -> expression_statement" -> "{ $$ = $1; }";
            case "statement -> selection_statement" -> "{ $$ = $1; }";
            case "statement -> iteration_statement" -> "{ $$ = $1; }";
            case "statement -> jump_statement" -> "{ $$ = $1; }";
            case "expression_statement -> expression SEMI" -> "{ $$ = wrapExpressionStatement($1); }";
            case "jump_statement -> RETURN expression SEMI" -> "{ $$ = makeReturn($2); }";
            case "selection_statement -> IF LPAREN expression RPAREN statement" -> "{ $$ = makeIf($3, $5); }";
            case "selection_statement -> IF LPAREN expression RPAREN statement ELSE statement" -> "{ $$ = makeIfElse($3, $5, $7); }";
            case "iteration_statement -> WHILE LPAREN expression RPAREN statement" -> "{ $$ = makeWhile($3, $5); }";

            case "expression -> assignment_expression" -> "{ $$ = $1; }";
            case "assignment_expression -> conditional_expression" -> "{ $$ = $1; }";
            case "conditional_expression -> logical_or_expression" -> "{ $$ = $1; }";
            case "logical_or_expression -> logical_and_expression" -> "{ $$ = $1; }";
            case "logical_and_expression -> inclusive_or_expression" -> "{ $$ = $1; }";
            case "inclusive_or_expression -> exclusive_or_expression" -> "{ $$ = $1; }";
            case "exclusive_or_expression -> and_expression" -> "{ $$ = $1; }";
            case "and_expression -> equality_expression" -> "{ $$ = $1; }";
            case "equality_expression -> relational_expression" -> "{ $$ = $1; }";
            case "equality_expression -> equality_expression EQ_OP relational_expression" -> "{ $$ = makeBinary(\"==\", $1, $3); }";
            case "equality_expression -> equality_expression NE_OP relational_expression" -> "{ $$ = makeBinary(\"!=\", $1, $3); }";
            case "relational_expression -> shift_expression" -> "{ $$ = $1; }";
            case "relational_expression -> relational_expression LT shift_expression" -> "{ $$ = makeBinary(\"<\", $1, $3); }";
            case "relational_expression -> relational_expression GT shift_expression" -> "{ $$ = makeBinary(\">\", $1, $3); }";
            case "relational_expression -> relational_expression LE_OP shift_expression" -> "{ $$ = makeBinary(\"<=\", $1, $3); }";
            case "relational_expression -> relational_expression GE_OP shift_expression" -> "{ $$ = makeBinary(\">=\", $1, $3); }";
            case "shift_expression -> additive_expression" -> "{ $$ = $1; }";
            case "additive_expression -> multiplicative_expression" -> "{ $$ = $1; }";
            case "additive_expression -> additive_expression PLUS multiplicative_expression" -> "{ $$ = makeBinary(\"+\", $1, $3); }";
            case "additive_expression -> additive_expression MINUS multiplicative_expression" -> "{ $$ = makeBinary(\"-\", $1, $3); }";
            case "multiplicative_expression -> cast_expression" -> "{ $$ = $1; }";
            case "multiplicative_expression -> multiplicative_expression STAR cast_expression" -> "{ $$ = makeBinary(\"*\", $1, $3); }";
            case "multiplicative_expression -> multiplicative_expression SLASH cast_expression" -> "{ $$ = makeBinary(\"/\", $1, $3); }";
            case "cast_expression -> unary_expression" -> "{ $$ = $1; }";
            case "unary_expression -> postfix_expression" -> "{ $$ = $1; }";
            case "postfix_expression -> primary_expression" -> "{ $$ = $1; }";
            case "primary_expression -> IDENTIFIER" -> "{ $$ = makeIdentifier($1); }";
            case "primary_expression -> CONSTANT" -> "{ $$ = makeIntLiteral($1); }";
            case "primary_expression -> LPAREN expression RPAREN" -> "{ $$ = $2; }";
            default -> null;
        };
    }
}
