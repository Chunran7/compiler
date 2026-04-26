package com.example.compiler.yacc.table;

import com.example.compiler.yacc.grammar.Associativity;
import com.example.compiler.yacc.grammar.Grammar;
import com.example.compiler.yacc.grammar.NonTerminal;
import com.example.compiler.yacc.grammar.Precedence;
import com.example.compiler.yacc.grammar.Production;
import com.example.compiler.yacc.grammar.Symbol;
import com.example.compiler.yacc.grammar.Terminal;
import com.example.compiler.yacc.lr1.CanonicalCollection;
import com.example.compiler.yacc.lr1.LR1Item;

import java.util.Map;
import java.util.Set;

public final class ParseTableBuilder {
    private final Grammar grammar;
    private final CanonicalCollection collection;

    public ParseTableBuilder(Grammar grammar, CanonicalCollection collection) {
        this.grammar = grammar;
        this.collection = collection;
    }

    public ParseTable build() {
        ParseTable table = new ParseTable();

        for (int stateId = 0; stateId < collection.states().size(); stateId++) {
            Set<LR1Item> state = collection.states().get(stateId);
            Map<Symbol, Integer> transitions = collection.transitions().getOrDefault(stateId, Map.of());

            for (LR1Item item : state) {
                Symbol next = item.getSymbolAfterDot();

                if (next instanceof Terminal terminal) {
                    Integer target = transitions.get(terminal);
                    if (target != null) {
                        putAction(table, stateId, terminal, Action.shift(target), null);
                    }
                    continue;
                }

                if (item.isComplete()) {
                    if (item.getProduction().getLeft().equals(grammar.getAugmentedStartSymbol())
                            && item.getLookahead().equals(grammar.getEof())) {
                        putAction(table, stateId, grammar.getEof(), Action.accept(), null);
                    } else {
                        putAction(
                                table,
                                stateId,
                                item.getLookahead(),
                                Action.reduce(item.getProduction().getId()),
                                item.getProduction()
                        );
                    }
                }
            }

            for (Map.Entry<Symbol, Integer> entry : transitions.entrySet()) {
                if (entry.getKey() instanceof NonTerminal nonTerminal) {
                    table.setGoto(stateId, nonTerminal, entry.getValue());
                }
            }
        }

        return table;
    }

    private void putAction(ParseTable table,
                           int stateId,
                           Terminal terminal,
                           Action candidate,
                           Production reduceProduction) {
        Action existing = table.getAction(stateId, terminal);

        if (existing == null || existing.equals(candidate)) {
            table.putResolvedAction(stateId, terminal, candidate);
            return;
        }

        Action resolved = resolveConflict(existing, candidate, terminal, reduceProduction);
        if (resolved == null) {
            throw new IllegalStateException(
                    "Unresolved ACTION conflict at state "
                            + stateId
                            + ", terminal "
                            + terminal.getName()
                            + ": "
                            + existing
                            + " vs "
                            + candidate
            );
        }

        table.putResolvedAction(stateId, terminal, resolved);
    }

    private Action resolveConflict(Action existing,
                                   Action candidate,
                                   Terminal lookahead,
                                   Production reduceProduction) {
        if (existing.type() == ActionType.SHIFT && candidate.type() == ActionType.REDUCE) {
            return resolveShiftReduce(existing, candidate, lookahead, reduceProduction);
        }

        if (existing.type() == ActionType.REDUCE && candidate.type() == ActionType.SHIFT) {
            Production existingReduce = grammar.getProduction(existing.productionId());
            return resolveShiftReduce(candidate, existing, lookahead, existingReduce);
        }

        if (existing.type() == ActionType.REDUCE && candidate.type() == ActionType.REDUCE) {
            throw new IllegalStateException(
                    "Reduce/Reduce conflict: " + existing + " vs " + candidate
            );
        }

        if (existing.type() == ActionType.ACCEPT || candidate.type() == ActionType.ACCEPT) {
            throw new IllegalStateException(
                    "Unexpected ACCEPT conflict: " + existing + " vs " + candidate
            );
        }

        return null;
    }

    private Action resolveShiftReduce(Action shiftAction,
                                      Action reduceAction,
                                      Terminal lookahead,
                                      Production reduceProduction) {
        Precedence terminalPrecedence = grammar.getTerminalPrecedence(lookahead.getName());
        Precedence productionPrecedence = reduceProduction == null ? null : reduceProduction.getPrecedence();

        // 关键修改：
        // 没有优先级/结合性信息时，不再默认 shift，而是明确报告 conflict
        if (terminalPrecedence == null || productionPrecedence == null) {
            return null;
        }

        if (terminalPrecedence.level() > productionPrecedence.level()) {
            return shiftAction;
        }

        if (terminalPrecedence.level() < productionPrecedence.level()) {
            return reduceAction;
        }

        Associativity associativity = terminalPrecedence.associativity();
        return switch (associativity) {
            case LEFT -> reduceAction;
            case RIGHT -> shiftAction;
            case NONASSOC -> null;
        };
    }
}