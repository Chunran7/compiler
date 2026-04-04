package com.compiler.yacc.lr1;

import com.compiler.yacc.grammar.Grammar;
import com.compiler.yacc.grammar.Production;
import com.compiler.yacc.grammar.Symbol;
import com.compiler.yacc.grammar.Terminal;

import java.util.List;
import java.util.Objects;

public class LR1Item {
    private final int productionId;
    private final int dotPosition;
    private final Terminal lookahead;

    public LR1Item(int productionId, int dotPosition, Terminal lookahead) {
        this.productionId = productionId;
        this.dotPosition = dotPosition;
        this.lookahead = lookahead;
    }

    public int getProductionId() {
        return productionId;
    }

    public int getDotPosition() {
        return dotPosition;
    }

    public Terminal getLookahead() {
        return lookahead;
    }

    /**
     * 当前项目对应的产生式
     */
    public Production getProduction(Grammar grammar) {
        return grammar.getProduction(productionId);
    }

    /**
     * 点后面的第一个符号
     * 如果点已经在最右边，返回 null
     */
    public Symbol symbolAfterDot(Grammar grammar) {
        Production production = getProduction(grammar);
        List<Symbol> right = production.getRight();

        if (dotPosition >= right.size()) {
            return null;
        }
        return right.get(dotPosition);
    }

    /**
     * 点后面跳过一个符号之后的剩余串 β
     * 对于 [A -> α · B β, a]，这里返回 β
     */
    public List<Symbol> betaAfterNextSymbol(Grammar grammar) {
        Production production = getProduction(grammar);
        List<Symbol> right = production.getRight();

        int start = dotPosition + 1;
        if (start >= right.size()) {
            return List.of();
        }
        return right.subList(start, right.size());
    }

    /**
     * 这个项目是否已经到达产生式末尾
     */
    public boolean isComplete(Grammar grammar) {
        Production production = getProduction(grammar);
        return dotPosition >= production.getRight().size();
    }

    /**
     * 构造“点右移一格”的新项目
     */
    public LR1Item advance() {
        return new LR1Item(productionId, dotPosition + 1, lookahead);
    }

    /**
     * 用于调试打印
     */
    public String format(Grammar grammar) {
        Production production = getProduction(grammar);
        StringBuilder sb = new StringBuilder();

        sb.append("[");
        sb.append(production.getLeft().getName());
        sb.append(" -> ");

        List<Symbol> right = production.getRight();
        for (int i = 0; i <= right.size(); i++) {
            if (i == dotPosition) {
                sb.append("· ");
            }
            if (i < right.size()) {
                sb.append(right.get(i).getName()).append(" ");
            }
        }

        sb.append(", ").append(lookahead.getName()).append("]");
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof LR1Item lr1Item)) return false;
        return productionId == lr1Item.productionId &&
                dotPosition == lr1Item.dotPosition &&
                Objects.equals(lookahead, lr1Item.lookahead);
    }

    @Override
    public int hashCode() {
        return Objects.hash(productionId, dotPosition, lookahead);
    }

    @Override
    public String toString() {
        return "LR1Item{" +
                "productionId=" + productionId +
                ", dotPosition=" + dotPosition +
                ", lookahead=" + lookahead +
                '}';
    }
}
