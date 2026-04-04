package com.compiler.yacc.first;

import com.compiler.yacc.grammar.Grammar;
import com.compiler.yacc.grammar.NonTerminal;
import com.compiler.yacc.grammar.SubsetGrammarFactory;
import com.compiler.yacc.grammar.Symbol;
import com.compiler.yacc.grammar.Terminal;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class FirstSetCalculatorTest {

    @Test
    void shouldComputeNullableSymbols() {
        Grammar grammar = new SubsetGrammarFactory().build();
        FirstSetCalculator calculator = new FirstSetCalculator();

        Set<NonTerminal> nullable = calculator.computeNullable(grammar);

        assertTrue(nullable.contains(grammar.getNonTerminal("DeclInitOpt")));
        assertTrue(nullable.contains(grammar.getNonTerminal("ItemList")));

        assertFalse(nullable.contains(grammar.getNonTerminal("Expr")));
        assertFalse(nullable.contains(grammar.getNonTerminal("Term")));
        assertFalse(nullable.contains(grammar.getNonTerminal("Factor")));
        assertFalse(nullable.contains(grammar.getNonTerminal("Stmt")));
    }

    @Test
    void shouldComputeFirstSetsForCoreNonTerminals() {
        Grammar grammar = new SubsetGrammarFactory().build();
        FirstSetCalculator calculator = new FirstSetCalculator();

        Map<NonTerminal, Set<Terminal>> firstSets = calculator.computeFirstSets(grammar);

        assertEquals(
                Set.of("LPAREN", "ID", "NUM"),
                toNameSet(firstSets.get(grammar.getNonTerminal("Factor")))
        );

        assertEquals(
                Set.of("LPAREN", "ID", "NUM"),
                toNameSet(firstSets.get(grammar.getNonTerminal("Term")))
        );

        assertEquals(
                Set.of("LPAREN", "ID", "NUM"),
                toNameSet(firstSets.get(grammar.getNonTerminal("Expr")))
        );

        assertEquals(
                Set.of("INT"),
                toNameSet(firstSets.get(grammar.getNonTerminal("Decl")))
        );

        assertEquals(
                Set.of("ID", "RETURN", "LBRACE", "WHILE", "IF"),
                toNameSet(firstSets.get(grammar.getNonTerminal("Stmt")))
        );

        assertEquals(
                Set.of("INT", "ID", "RETURN", "LBRACE", "WHILE", "IF"),
                toNameSet(firstSets.get(grammar.getNonTerminal("Item")))
        );
    }

    @Test
    void shouldComputeFirstOfSequence() {
        Grammar grammar = new SubsetGrammarFactory().build();
        FirstSetCalculator calculator = new FirstSetCalculator();

        Map<NonTerminal, Set<Terminal>> firstSets = calculator.computeFirstSets(grammar);

        // FIRST(DeclInitOpt SEMI) = { ASSIGN, SEMI }
        List<Symbol> seq1 = List.of(
                grammar.getNonTerminal("DeclInitOpt"),
                grammar.getTerminal("SEMI")
        );

        assertEquals(
                Set.of("ASSIGN", "SEMI"),
                toNameSet(calculator.firstOfSequence(grammar, firstSets, seq1))
        );

        // FIRST(ItemList RBRACE)
        // ItemList -> ItemList Item | ε
        // Item -> Decl | Stmt
        // Decl starts with INT
        // Stmt starts with ID / RETURN / LBRACE / WHILE / IF
        // 因为 ItemList nullable，所以还要加上 RBRACE
        List<Symbol> seq2 = List.of(
                grammar.getNonTerminal("ItemList"),
                grammar.getTerminal("RBRACE")
        );

        assertEquals(
                Set.of("INT", "ID", "RETURN", "LBRACE", "WHILE", "IF", "RBRACE"),
                toNameSet(calculator.firstOfSequence(grammar, firstSets, seq2))
        );
    }

    private Set<String> toNameSet(Set<Terminal> terminals) {
        return terminals.stream()
                .map(Terminal::getName)
                .collect(Collectors.toSet());
    }
}
