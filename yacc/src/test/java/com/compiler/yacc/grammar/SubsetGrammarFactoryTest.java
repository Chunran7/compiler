package com.compiler.yacc.grammar;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class SubsetGrammarFactoryTest {

    @Test
    void shouldBuildSubsetGrammar() {
        Grammar grammar = new SubsetGrammarFactory().build();

        assertNotNull(grammar.getStartSymbol());
        assertEquals("Program", grammar.getStartSymbol().getName());
        assertNotNull(grammar.getAugmentedStartSymbol());
        assertEquals("S'", grammar.getAugmentedStartSymbol().getName());

        assertNotNull(grammar.getTerminal("IF"));
        assertNotNull(grammar.getTerminal("WHILE"));
        assertNotNull(grammar.getTerminal("RETURN"));
        assertNotNull(grammar.getTerminal("ID"));
        assertNotNull(grammar.getTerminal("NUM"));

        assertNotNull(grammar.getNonTerminal("Expr"));
        assertNotNull(grammar.getNonTerminal("Stmt"));
        assertNotNull(grammar.getNonTerminal("Decl"));

        assertTrue(grammar.getProductions().size() > 20);
        assertTrue(grammar.getProductionsFor(grammar.getNonTerminal("DeclInitOpt"))
                .stream()
                .anyMatch(Production::isEpsilon));
    }
}
