package com.example.compiler.semantic.action;

import java.util.Objects;

public final class ActionPattern {
    public enum Kind {
        DIRECT_REFERENCE_ASSIGN,
        FUNCTION_CALL_ASSIGN
    }

    private final String rawCode;
    private final Kind kind;
    private final int directReferenceIndex;
    private final ActionInvocation invocation;

    private ActionPattern(String rawCode,
                          Kind kind,
                          int directReferenceIndex,
                          ActionInvocation invocation) {
        this.rawCode = Objects.requireNonNull(rawCode, "rawCode");
        this.kind = Objects.requireNonNull(kind, "kind");
        this.directReferenceIndex = directReferenceIndex;
        this.invocation = invocation;
    }

    public static ActionPattern directReferenceAssign(String rawCode, int refIndex) {
        if (refIndex <= 0) {
            throw new IllegalArgumentException("refIndex must be positive");
        }
        return new ActionPattern(rawCode, Kind.DIRECT_REFERENCE_ASSIGN, refIndex, null);
    }

    public static ActionPattern functionCallAssign(String rawCode, ActionInvocation invocation) {
        return new ActionPattern(rawCode, Kind.FUNCTION_CALL_ASSIGN, -1, Objects.requireNonNull(invocation, "invocation"));
    }

    public String getRawCode() {
        return rawCode;
    }

    public Kind getKind() {
        return kind;
    }

    public boolean isDirectReferenceAssign() {
        return kind == Kind.DIRECT_REFERENCE_ASSIGN;
    }

    public boolean isFunctionCallAssign() {
        return kind == Kind.FUNCTION_CALL_ASSIGN;
    }

    public int getDirectReferenceIndex() {
        return directReferenceIndex;
    }

    public ActionInvocation getInvocation() {
        return invocation;
    }

    @Override
    public String toString() {
        return switch (kind) {
            case DIRECT_REFERENCE_ASSIGN -> "$$ = $" + directReferenceIndex;
            case FUNCTION_CALL_ASSIGN -> "$$ = " + invocation;
        };
    }
}
