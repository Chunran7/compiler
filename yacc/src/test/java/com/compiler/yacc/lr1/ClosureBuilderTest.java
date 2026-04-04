package com.compiler.yacc.lr1;

import com.compiler.yacc.grammar.Grammar;
import com.compiler.yacc.grammar.Production;
import com.compiler.yacc.grammar.SubsetGrammarFactory;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ClosureBuilderTest {

    @Test
    void shouldBuildClosureForAugmentedStartItem() {
        Grammar grammar = new SubsetGrammarFactory().build();
        ClosureBuilder closureBuilder = new ClosureBuilder();

        Production startProduction = grammar.getProduction(0); // S' -> Program

        LR1Item startItem = new LR1Item(
                startProduction.getId(),
                0,
                grammar.getTerminal("EOF")
        );

        Set<LR1Item> kernel = new LinkedHashSet<>();
        kernel.add(startItem);

        Set<LR1Item> closure = closureBuilder.closure(grammar, kernel);

        Set<String> actual = closure.stream()
                .map(item -> item.format(grammar))
                .collect(Collectors.toSet());

        assertEquals(3, closure.size());

        assertTrue(actual.contains("[S' -> · Program , EOF]") ||
                actual.contains("[S' -> · Program, EOF]"));

        assertTrue(actual.contains("[Program -> · MainFunc , EOF]") ||
                actual.contains("[Program -> · MainFunc, EOF]"));

        assertTrue(actual.contains("[MainFunc -> · INT MAIN LPAREN RPAREN Block , EOF]") ||
                actual.contains("[MainFunc -> · INT MAIN LPAREN RPAREN Block, EOF]"));
    }

    @Test
    void shouldPropagateLookaheadUsingFirstOfBetaA() {
        Grammar grammar = new SubsetGrammarFactory().build();
        ClosureBuilder closureBuilder = new ClosureBuilder();

        // 找到：Decl -> INT ID DeclInitOpt SEMI
        Production declProduction = grammar.getProductions().stream()
                .filter(p -> p.getLeft().getName().equals("Decl"))
                .filter(p -> p.getRight().size() == 4)
                .findFirst()
                .orElseThrow();

        // 构造项目：
        // [Decl -> INT ID · DeclInitOpt SEMI, EOF]
        LR1Item item = new LR1Item(
                declProduction.getId(),
                2,
                grammar.getTerminal("EOF")
        );

        Set<LR1Item> kernel = new LinkedHashSet<>();
        kernel.add(item);

        Set<LR1Item> closure = closureBuilder.closure(grammar, kernel);

        Set<String> actual = closure.stream()
                .map(i -> i.format(grammar))
                .collect(Collectors.toSet());

        // 因为 beta = SEMI，lookahead = EOF
        // FIRST(beta a) = FIRST(SEMI EOF) = { SEMI }
        assertTrue(actual.contains("[DeclInitOpt -> · ASSIGN Expr , SEMI]") ||
                actual.contains("[DeclInitOpt -> · ASSIGN Expr, SEMI]"));

        assertTrue(actual.contains("[DeclInitOpt -> · , SEMI]") ||
                actual.contains("[DeclInitOpt -> ·, SEMI]"));
    }
}
