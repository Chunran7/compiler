package com.example.compiler.yacc.grammar;

import java.util.List;

/**
 * 文法产生式。
 *
 * <p>一个 Production 表示 {@code A -> α}。它的 id 会被 ParseTable 的
 * REDUCE 动作引用，也会写入 AstNode，便于调试 action-tree/parse-tree。
 * actionCode 保存 yacc 规则中的语义动作文本；precedence 保存该产生式
 * 参与冲突处理时使用的优先级和结合性。</p>
 */
public final class Production {
    private final int id;
    private final NonTerminal left;
    private final List<Symbol> right;
    private final String actionCode;
    private final String precedenceTokenName;
    private final Precedence precedence;

    public Production(int id,
                      NonTerminal left,
                      List<Symbol> right,
                      String actionCode,
                      String precedenceTokenName,
                      Precedence precedence) {
        this.id = id;
        this.left = left;
        this.right = List.copyOf(right);
        this.actionCode = actionCode == null || actionCode.isBlank() ? null : actionCode.trim();
        this.precedenceTokenName = precedenceTokenName;
        this.precedence = precedence;
    }

    public int getId() {
        return id;
    }

    public NonTerminal getLeft() {
        return left;
    }

    public List<Symbol> getRight() {
        return right;
    }

    public String getActionCode() {
        return actionCode;
    }

    public boolean hasActionCode() {
        return actionCode != null && !actionCode.isBlank();
    }

    public boolean isEpsilon() {
        return right.isEmpty();
    }

    public String getPrecedenceTokenName() {
        return precedenceTokenName;
    }

    public Precedence getPrecedence() {
        return precedence;
    }

    public boolean hasPrecedence() {
        return precedence != null;
    }

    @Override
    public String toString() {
        String rhs = right.isEmpty() ? "ε" : right.toString();
        StringBuilder sb = new StringBuilder();
        sb.append(id).append(": ").append(left).append(" -> ").append(rhs);
        if (hasActionCode()) {
            sb.append(' ').append(actionCode);
        }
        if (precedenceTokenName != null) {
            sb.append(" [prec=").append(precedenceTokenName).append(']');
        }
        return sb.toString();
    }
}
