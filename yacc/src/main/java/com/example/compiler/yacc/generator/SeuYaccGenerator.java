package com.example.compiler.yacc.generator;

import com.example.compiler.yacc.first.FirstSetCalculator;
import com.example.compiler.yacc.grammar.Grammar;
import com.example.compiler.yacc.lalr.LALRConverter;
import com.example.compiler.yacc.lr1.CanonicalCollection;
import com.example.compiler.yacc.lr1.CanonicalCollectionBuilder;
import com.example.compiler.yacc.parser.YaccParser;
import com.example.compiler.yacc.table.ParseTable;
import com.example.compiler.yacc.table.ParseTableBuilder;

import java.io.IOException;
import java.io.Reader;

public final class SeuYaccGenerator {
    private final Grammar grammar;
    private final ParseTable parseTable;
    private final CanonicalCollection collection;

    public SeuYaccGenerator(Reader reader, boolean useLalr) throws IOException {
        this.grammar = YaccParser.parse(reader);

        FirstSetCalculator firstSetCalculator = new FirstSetCalculator(grammar);
        firstSetCalculator.compute();

        CanonicalCollection lr1 = new CanonicalCollectionBuilder(grammar, firstSetCalculator).build();
        this.collection = useLalr ? new LALRConverter().convert(lr1) : lr1;
        this.parseTable = new ParseTableBuilder(grammar, collection).build();
    }

    public Grammar getGrammar() {
        return grammar;
    }

    public ParseTable getParseTable() {
        return parseTable;
    }

    public CanonicalCollection getCollection() {
        return collection;
    }
}
