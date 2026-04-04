package com.compiler.yacc.table;

import java.util.Objects;

public class Action {
    private final ActionType type;
    private final Integer targetState;
    private final Integer productionId;

    private Action(ActionType type, Integer targetState, Integer productionId) {
        this.type = type;
        this.targetState = targetState;
        this.productionId = productionId;
    }

    public static Action shift(int targetState) {
        return new Action(ActionType.SHIFT, targetState, null);
    }

    public static Action reduce(int productionId) {
        return new Action(ActionType.REDUCE, null, productionId);
    }

    public static Action accept() {
        return new Action(ActionType.ACCEPT, null, null);
    }

    public ActionType getType() {
        return type;
    }

    public Integer getTargetState() {
        return targetState;
    }

    public Integer getProductionId() {
        return productionId;
    }

    @Override
    public String toString() {
        return switch (type) {
            case SHIFT -> "s" + targetState;
            case REDUCE -> "r" + productionId;
            case ACCEPT -> "acc";
        };
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Action action)) return false;
        return type == action.type &&
                Objects.equals(targetState, action.targetState) &&
                Objects.equals(productionId, action.productionId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, targetState, productionId);
    }
}
