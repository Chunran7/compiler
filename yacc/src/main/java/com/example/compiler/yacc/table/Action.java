package com.example.compiler.yacc.table;

public record Action(ActionType type, int targetState, int productionId) {
    public static Action shift(int state) {
        return new Action(ActionType.SHIFT, state, -1);
    }

    public static Action reduce(int productionId) {
        return new Action(ActionType.REDUCE, -1, productionId);
    }

    public static Action accept() {
        return new Action(ActionType.ACCEPT, -1, -1);
    }

    @Override
    public String toString() {
        return switch (type) {
            case SHIFT -> "s" + targetState;
            case REDUCE -> "r" + productionId;
            case ACCEPT -> "acc";
        };
    }
}
