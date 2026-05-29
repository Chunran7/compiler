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

/**
 * ACTION/GOTO 分析表构造器。
 *
 * <p>输入是 Grammar 和 LR(1)/LALR 项目集族；输出是 {@link ParseTable}。
 * 对每个状态：
 * 若点后是终结符，写入 shift；
 * 若项目完成，按 lookahead 写入 reduce；
 * 若完成的是增广开始产生式并展望 EOF，写入 accept；
 * 非终结符转移写入 GOTO。</p>
 */
public final class ParseTableBuilder {
    private final Grammar grammar;
    private final CanonicalCollection collection;

    public ParseTableBuilder(Grammar grammar, CanonicalCollection collection) {
        this.grammar = grammar;
        this.collection = collection;
    }

    /**
     * 生成完整 ACTION/GOTO 表。
     *
     * @return 可被 ParserDriver 或 ParserProgramEmitter 使用的分析表
     * @throws IllegalStateException 遇到无法用优先级/结合性解决的冲突时抛出
     */
    public ParseTable build() {
        ParseTable table = new ParseTable();

        for (int stateId = 0; stateId < collection.states().size(); stateId++) {
            Set<LR1Item> state = collection.states().get(stateId);
            Map<Symbol, Integer> transitions = collection.transitions().getOrDefault(stateId, Map.of());

            for (LR1Item item : state) {
                Symbol next = item.getSymbolAfterDot();

                if (next instanceof Terminal terminal) {
                    // 项目形如 A -> α · a β，点后是终结符 a。
                    // 自动机转移目标就是移进 a 后的新状态，因此 ACTION 写 shift。
                    Integer target = transitions.get(terminal);
                    if (target != null) {
                        putAction(table, stateId, terminal, Action.shift(target), null);
                    }
                    continue;
                }

                if (item.isComplete()) {
                    // 项目点在最右侧，说明某个产生式右部已经识别完成。
                    // 增广开始产生式完成且展望 EOF 时接受；其它完成项目按 lookahead 规约。
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
                    // 非终结符转移不属于 ACTION，而是在规约后查 GOTO。
                    // ParserDriver 规约成 nonTerminal 后用当前栈顶状态查这里的目标状态。
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
            // 没有冲突，或重复写入完全相同的动作，直接接受。
            table.putResolvedAction(stateId, terminal, candidate);
            return;
        }

        // 同一个 ACTION 单元出现不同动作时才进入冲突处理。
        // 返回 null 表示非结合或无法决策，调用方会抛出错误阻止生成错误表。
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

    /**
     * 处理 ACTION 表冲突。
     *
     * <p>当前实现支持 yacc 常见的 shift/reduce 优先级解决策略；reduce/reduce
     * 冲突会直接报错，因为这通常说明文法本身有二义性或需要改写。</p>
     */
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
            Production p1 = grammar.getProduction(existing.productionId());
            Production p2 = grammar.getProduction(candidate.productionId());
            throw new IllegalStateException(
                    "Reduce/Reduce conflict: " + existing + " vs " + candidate
                            + "\n  Production " + existing.productionId() + ": " + p1
                            + "\n  Production " + candidate.productionId() + ": " + p2
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

        // 没有优先级/结合性信息时，默认 shift（标准 yacc 行为，解决 dangling-else 等）
        if (terminalPrecedence == null || productionPrecedence == null) {
            return shiftAction;
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
