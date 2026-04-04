package com.compiler.yacc.runtime;

import com.compiler.yacc.grammar.Grammar;
import com.compiler.yacc.grammar.NonTerminal;
import com.compiler.yacc.grammar.Production;
import com.compiler.yacc.grammar.Symbol;
import com.compiler.yacc.grammar.Terminal;
import com.compiler.yacc.table.Action;
import com.compiler.yacc.table.ActionType;
import com.compiler.yacc.table.ParseTable;
import com.compiler.yacc.token.Token;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

public class ParserDriver {

    private final Grammar grammar;
    private final ParseTable parseTable;

    public ParserDriver(Grammar grammar, ParseTable parseTable) {
        this.grammar = grammar;
        this.parseTable = parseTable;
    }

    public ParseResult parse(List<Token> tokens) {
        Deque<Integer> stateStack = new ArrayDeque<>();
        Deque<Symbol> symbolStack = new ArrayDeque<>();
        List<Integer> reductions = new ArrayList<>();

        stateStack.push(0);
        int index = 0;

        while (true) {
            if (index >= tokens.size()) {
                return ParseResult.failure(reductions, "输入 token 串提前结束，缺少 EOF。");
            }

            int currentState = stateStack.peek();
            Token currentToken = tokens.get(index);
            Terminal currentTerminal = mapTokenToTerminal(currentToken);

            if (currentTerminal == null) {
                return ParseResult.failure(
                        reductions,
                        "无法把 token 映射到文法终结符: " + currentToken
                );
            }

            Action action = parseTable.getAction(currentState, currentTerminal);

            if (action == null) {
                return ParseResult.failure(
                        reductions,
                        "没有可用 ACTION: state=" + currentState +
                                ", token=" + currentToken
                );
            }

            if (action.getType() == ActionType.SHIFT) {
                symbolStack.push(currentTerminal);
                stateStack.push(action.getTargetState());
                index++;
                continue;
            }

            if (action.getType() == ActionType.REDUCE) {
                Production production = grammar.getProduction(action.getProductionId());
                int popCount = production.getRight().size();

                for (int i = 0; i < popCount; i++) {
                    if (stateStack.isEmpty() || symbolStack.isEmpty()) {
                        return ParseResult.failure(
                                reductions,
                                "规约时栈为空，产生式=" + production
                        );
                    }
                    stateStack.pop();
                    symbolStack.pop();
                }

                NonTerminal left = production.getLeft();
                Integer gotoState = parseTable.getGoto(stateStack.peek(), left);

                if (gotoState == null) {
                    return ParseResult.failure(
                            reductions,
                            "规约后没有 GOTO: state=" + stateStack.peek() +
                                    ", nonTerminal=" + left.getName()
                    );
                }

                symbolStack.push(left);
                stateStack.push(gotoState);
                reductions.add(production.getId());
                continue;
            }

            if (action.getType() == ActionType.ACCEPT) {
                return ParseResult.success(reductions);
            }

            return ParseResult.failure(reductions, "未知 ACTION 类型: " + action);
        }
    }

    private Terminal mapTokenToTerminal(Token token) {
        // 这里依赖一个前提：
        // TokenType 的名字和 Grammar 里终结符名字一致
        // 例如 TokenType.INT <-> terminal("INT")
        return grammar.getTerminal(token.getType().name());
    }
}
