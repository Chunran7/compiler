package com.example.compiler.yacc.runtime;

import com.example.compiler.yacc.ast.AstNode;
import com.example.compiler.yacc.grammar.Grammar;
import com.example.compiler.yacc.grammar.NonTerminal;
import com.example.compiler.yacc.grammar.Production;
import com.example.compiler.yacc.grammar.Symbol;
import com.example.compiler.yacc.grammar.Terminal;
import com.example.compiler.yacc.table.Action;
import com.example.compiler.yacc.table.ActionType;
import com.example.compiler.yacc.table.ParseTable;
import com.example.compiler.yacc.token.Token;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;

/**
 * 表驱动语法分析运行器。
 *
 * <p>输入是 Lex 阶段得到的 token 列表（测试中等价于 tokens.txt 的内容），
 * 以及 SeuYaccGenerator 生成的 Grammar/ParseTable。输出是 {@link ParseResult}：
 * 包括是否接受、规约序列和带语义动作节点的 parse tree 根节点。</p>
 *
 * <p>在课程流程中，它对应“可执行语法分析程序 yyparse”的核心运行逻辑：
 * 通过 ACTION/GOTO 表执行 shift/reduce/accept，并在规约时构造语法树。</p>
 */
public final class ParserDriver {
    private final Grammar grammar;
    private final ParseTable parseTable;

    public ParserDriver(Grammar grammar, ParseTable parseTable) {
        this.grammar = grammar;
        this.parseTable = parseTable;
    }

    /**
     * 执行 LR 表驱动分析。
     *
     * <p>stateStack 保存自动机状态，symbolStack 保存已识别语法符号，
     * astStack 与符号栈同步保存树节点。SHIFT 时压入终结符叶子；
     * REDUCE 时按产生式右部长度弹栈并构造父节点；ACCEPT 时返回根节点。</p>
     */
    public ParseResult parse(List<Token> tokens) {
        Deque<Integer> stateStack = new ArrayDeque<>();
        Deque<Symbol> symbolStack = new ArrayDeque<>();
        Deque<AstNode> astStack = new ArrayDeque<>();
        List<Integer> reductions = new ArrayList<>();

        stateStack.push(0);
        int index = 0;

        while (true) {
            if (index >= tokens.size()) {
                return ParseResult.failure(reductions, "Input token stream ended before EOF.");
            }

            int currentState = stateStack.peek();
            Token currentToken = tokens.get(index);
            Terminal currentTerminal = mapTokenToTerminal(currentToken);

            if (currentTerminal == null) {
                return ParseResult.failure(reductions, "Cannot map token to terminal: " + currentToken);
            }

            Action action = parseTable.getAction(currentState, currentTerminal);
            if (action == null) {
                return ParseResult.failure(
                        reductions,
                        "No ACTION for state=" + currentState + ", token=" + currentToken
                );
            }

            if (action.type() == ActionType.SHIFT) {
                symbolStack.push(currentTerminal);
                astStack.push(AstNode.leaf(currentTerminal.getName(), currentToken.lexeme()));
                stateStack.push(action.targetState());
                index++;
                continue;
            }

            if (action.type() == ActionType.REDUCE) {
                Production production = grammar.getProduction(action.productionId());
                int popCount = production.getRight().size();

                LinkedList<AstNode> children = new LinkedList<>();

                for (int i = 0; i < popCount; i++) {
                    if (stateStack.isEmpty() || symbolStack.isEmpty() || astStack.isEmpty()) {
                        return ParseResult.failure(reductions, "Stack underflow during reduce: " + production);
                    }

                    stateStack.pop();
                    symbolStack.pop();
                    children.addFirst(astStack.pop());
                }

                NonTerminal left = production.getLeft();
                AstNode parent;

                if (isSemanticActionProduction(production)) {
                    parent = AstNode.semanticAction(
                            left.getName(),
                            production.getActionCode(),
                            production.getId()
                    );
                } else {
                    parent = AstNode.nonTerminal(
                            left.getName(),
                            children,
                            production.getId()
                    );
                }

                Integer gotoState = parseTable.getGoto(stateStack.peek(), left);
                if (gotoState == null) {
                    return ParseResult.failure(
                            reductions,
                            "No GOTO after reduce: state=" + stateStack.peek() + ", nonTerminal=" + left.getName()
                    );
                }

                symbolStack.push(left);
                astStack.push(parent);
                stateStack.push(gotoState);
                reductions.add(production.getId());
                continue;
            }

            if (action.type() == ActionType.ACCEPT) {
                AstNode root = astStack.isEmpty() ? null : astStack.peek();
                return ParseResult.success(reductions, root);
            }

            return ParseResult.failure(reductions, "Unknown action type: " + action);
        }
    }

    private boolean isSemanticActionProduction(Production production) {
        return production.isEpsilon()
                && production.getLeft().getName().startsWith("__ACT_")
                && production.hasActionCode();
    }

    private Terminal mapTokenToTerminal(Token token) {
        // Try canonical (C99) name first, fall back to raw name for MiniC grammars
        Terminal t = grammar.getTerminal(token.type().canonical().name());
        if (t != null) return t;
        return grammar.getTerminal(token.type().name());
    }
}
