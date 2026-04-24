package com.example.compiler.semantic.action;

public final class ActionArgument {
    public enum Kind {
        POSITIONAL_REF,
        STRING_LITERAL,
        RAW_LITERAL
    }

    private final Kind kind;
    private final int refIndex;
    private final String text;

    private ActionArgument(Kind kind, int refIndex, String text) {
        this.kind = kind;
        this.refIndex = refIndex;
        this.text = text;
    }

    public static ActionArgument positionalRef(int refIndex) {
        if (refIndex <= 0) {
            throw new IllegalArgumentException("refIndex must be positive");
        }
        return new ActionArgument(Kind.POSITIONAL_REF, refIndex, null);
    }

    public static ActionArgument stringLiteral(String text) {
        return new ActionArgument(Kind.STRING_LITERAL, -1, text);
    }

    public static ActionArgument rawLiteral(String text) {
        return new ActionArgument(Kind.RAW_LITERAL, -1, text);
    }

    public Kind getKind() {
        return kind;
    }

    public int getRefIndex() {
        return refIndex;
    }

    public String getText() {
        return text;
    }

    public boolean isPositionalRef() {
        return kind == Kind.POSITIONAL_REF;
    }

    public boolean isStringLiteral() {
        return kind == Kind.STRING_LITERAL;
    }

    public boolean isRawLiteral() {
        return kind == Kind.RAW_LITERAL;
    }

    @Override
    public String toString() {
        return switch (kind) {
            case POSITIONAL_REF -> "$" + refIndex;
            case STRING_LITERAL -> "\"" + text + "\"";
            case RAW_LITERAL -> text;
        };
    }
}