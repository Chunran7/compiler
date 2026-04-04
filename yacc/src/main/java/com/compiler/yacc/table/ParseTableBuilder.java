package com.compiler.yacc.table;

import com.compiler.yacc.grammar.Grammar;
import com.compiler.yacc.grammar.NonTerminal;
import com.compiler.yacc.grammar.Production;
import com.compiler.yacc.grammar.Symbol;
import com.compiler.yacc.grammar.Terminal;
import com.compiler.yacc.lr1.CanonicalCollection;
import com.compiler.yacc.lr1.LR1Item;
import com.compiler.yacc.lr1.LR1State;

import java.util.Map;

public class ParseTableBuilder {

    public ParseTable build(Grammar grammar, CanonicalCollection collection) {
        ParseTable table = new ParseTable();

        for (LR1State state : collection.getStates()) {
            int stateId = state.getId();

            // 1. 先处理 shift / goto，依据状态转移边
            Map<Symbol, Integer> transitions = collection.getTransitionsFrom(stateId);
            for (Map.Entry<Symbol, Integer> entry : transitions.entrySet()) {
                Symbol symbol = entry.getKey();
                Integer targetState = entry.getValue();

                if (symbol.isTerminal()) {
                    putActionWithConflictCheck(
                            table,
                            stateId,
                            (Terminal) symbol,
                            Action.shift(targetState)
                    );
                } else {
                    table.putGoto(stateId, (NonTerminal) symbol, targetState);
                }
            }

            // 2. 再处理 reduce / accept，依据“点到末尾”的项目
            for (LR1Item item : state.getItems()) {
                Production production = item.getProduction(grammar);

                if (!item.isComplete(grammar)) {
                    continue;
                }

                // accept: [S' -> Program ·, EOF]
                if (production.getLeft().equals(grammar.getAugmentedStartSymbol())
                        && item.getLookahead().equals(grammar.getEof())) {
                    putActionWithConflictCheck(
                            table,
                            stateId,
                            grammar.getEof(),
                            Action.accept()
                    );
                    continue;
                }

                // reduce: [A -> α ·, a]
                putActionWithConflictCheck(
                        table,
                        stateId,
                        item.getLookahead(),
                        Action.reduce(production.getId())
                );
            }
        }

        return table;
    }

    private void putActionWithConflictCheck(
            ParseTable table,
            int stateId,
            Terminal terminal,
            Action newAction
    ) {
        Action oldAction = table.getAction(stateId, terminal);

        if (oldAction != null && !oldAction.equals(newAction)) {
            throw new IllegalStateException(
                    "ACTION conflict at state " + stateId +
                            ", terminal " + terminal.getName() +
                            ": old=" + oldAction +
                            ", new=" + newAction
            );
        }

        table.putAction(stateId, terminal, newAction);
    }
}
