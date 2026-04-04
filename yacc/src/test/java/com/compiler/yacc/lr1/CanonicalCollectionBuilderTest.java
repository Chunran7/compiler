package com.compiler.yacc.lr1;

import com.compiler.yacc.grammar.Grammar;
import com.compiler.yacc.grammar.SubsetGrammarFactory;
import com.compiler.yacc.grammar.Symbol;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

public class CanonicalCollectionBuilderTest {

    @Test
    void shouldComputeGotoFromInitialStateOnINT() {
        Grammar grammar = new SubsetGrammarFactory().build();
        ClosureBuilder closureBuilder = new ClosureBuilder();
        GotoBuilder gotoBuilder = new GotoBuilder();

        LR1Item startItem = new LR1Item(0, 0, grammar.getEof());
        Set<LR1Item> startKernel = Set.of(startItem);
        Set<LR1Item> initialState = closureBuilder.closure(grammar, startKernel);

        Set<LR1Item> nextState = gotoBuilder.goTo(
                grammar,
                initialState,
                grammar.getTerminal("INT")
        );

        Set<String> actual = nextState.stream()
                .map(item -> item.format(grammar))
                .collect(Collectors.toSet());

        assertTrue(actual.contains("[MainFunc -> INT · MAIN LPAREN RPAREN Block , EOF]") ||
                actual.contains("[MainFunc -> INT · MAIN LPAREN RPAREN Block, EOF]"));
    }

    @Test
    void shouldBuildCanonicalCollectionForSubsetGrammar() {
        Grammar grammar = new SubsetGrammarFactory().build();
        CanonicalCollectionBuilder builder = new CanonicalCollectionBuilder();

        CanonicalCollection collection = builder.build(grammar);

        assertTrue(collection.getStates().size() > 1);

        Map<Symbol, Integer> transitionsFrom0 = collection.getTransitionsFrom(0);

        assertNotNull(transitionsFrom0.get(grammar.getTerminal("INT")));
        assertNotNull(transitionsFrom0.get(grammar.getNonTerminal("Program")));
        assertNotNull(transitionsFrom0.get(grammar.getNonTerminal("MainFunc")));
    }

    @Test
    void shouldReachStateAfterINTFromInitialState() {
        Grammar grammar = new SubsetGrammarFactory().build();
        CanonicalCollectionBuilder builder = new CanonicalCollectionBuilder();

        CanonicalCollection collection = builder.build(grammar);

        Integer targetStateId = collection.getTarget(0, grammar.getTerminal("INT"));
        assertNotNull(targetStateId);

        LR1State targetState = collection.getStates().get(targetStateId);

        Set<String> actual = targetState.getItems().stream()
                .map(item -> item.format(grammar))
                .collect(Collectors.toSet());

        assertTrue(actual.contains("[MainFunc -> INT · MAIN LPAREN RPAREN Block , EOF]") ||
                actual.contains("[MainFunc -> INT · MAIN LPAREN RPAREN Block, EOF]"));
    }
}