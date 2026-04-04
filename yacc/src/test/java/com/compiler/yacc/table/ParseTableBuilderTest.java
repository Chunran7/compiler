package com.compiler.yacc.table;

import com.compiler.yacc.grammar.Grammar;
import com.compiler.yacc.grammar.SubsetGrammarFactory;
import com.compiler.yacc.lr1.CanonicalCollection;
import com.compiler.yacc.lr1.CanonicalCollectionBuilder;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ParseTableBuilderTest {

    @Test
    void shouldBuildShiftActionFromInitialStateOnINT() {
        Grammar grammar = new SubsetGrammarFactory().build();
        CanonicalCollection collection = new CanonicalCollectionBuilder().build(grammar);
        ParseTable table = new ParseTableBuilder().build(grammar, collection);

        Action action = table.getAction(0, grammar.getTerminal("INT"));

        assertNotNull(action);
        assertEquals(ActionType.SHIFT, action.getType());

        Integer target = collection.getTarget(0, grammar.getTerminal("INT"));
        assertEquals(target, action.getTargetState());
    }

    @Test
    void shouldBuildGotoEntryFromInitialStateOnProgram() {
        Grammar grammar = new SubsetGrammarFactory().build();
        CanonicalCollection collection = new CanonicalCollectionBuilder().build(grammar);
        ParseTable table = new ParseTableBuilder().build(grammar, collection);

        Integer target = table.getGoto(0, grammar.getNonTerminal("Program"));
        assertNotNull(target);

        Integer expected = collection.getTarget(0, grammar.getNonTerminal("Program"));
        assertEquals(expected, target);
    }

    @Test
    void shouldBuildAcceptActionForAugmentedCompletedState() {
        Grammar grammar = new SubsetGrammarFactory().build();
        CanonicalCollection collection = new CanonicalCollectionBuilder().build(grammar);
        ParseTable table = new ParseTableBuilder().build(grammar, collection);

        Integer acceptState = collection.getTarget(0, grammar.getNonTerminal("Program"));
        assertNotNull(acceptState);

        Action action = table.getAction(acceptState, grammar.getEof());

        assertNotNull(action);
        assertEquals(ActionType.ACCEPT, action.getType());
    }

    @Test
    void shouldContainAtLeastOneReduceAction() {
        Grammar grammar = new SubsetGrammarFactory().build();
        CanonicalCollection collection = new CanonicalCollectionBuilder().build(grammar);
        ParseTable table = new ParseTableBuilder().build(grammar, collection);

        boolean hasReduce = table.getAllActions().values().stream()
                .flatMap(row -> row.values().stream())
                .anyMatch(action -> action.getType() == ActionType.REDUCE);

        assertTrue(hasReduce);
    }
}
