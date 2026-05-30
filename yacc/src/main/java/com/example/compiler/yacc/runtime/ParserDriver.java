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

            // 表驱动 LR 分析的核心：当前状态 + 当前终结符唯一决定下一步动作。
            // 若 ACTION 为空，说明 token 流无法被当前文法接受。
            Action action = parseTable.getAction(currentState, currentTerminal);
            if (action == null) {
                return ParseResult.failure(
                        reductions,
                        "No ACTION for state=" + currentState + ", token=" + currentToken
                );
            }

            if (action.type() == ActionType.SHIFT) {
                // SHIFT：消费一个输入 token，把终结符和叶子节点压栈，并进入目标状态。
                // astStack 与 symbolStack 保持同步，后续 reduce 时可按右部长度弹出孩子。
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

                // REDUCE：右部有几个符号，就从状态栈/符号栈/AST 栈弹出几个元素。
                // children 用 addFirst 还原原始从左到右顺序，因为栈弹出顺序是反的。
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

                // __ACT_n -> ε 是 YaccParser 为语义动作插入的合成产生式。
                // 普通产生式生成非终结符节点；动作产生式生成 semanticAction 节点，
                // 这样 action-tree 可以保留动作代码和所在位置。
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

                // 规约成左部非终结符后，根据“规约前的新栈顶状态”和左部查 GOTO。
                // 查到的状态就是 LR 自动机规约后应进入的状态。
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
                // ACCEPT：输入符合文法，AST 栈顶即完整 translation_unit 语法树。
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
