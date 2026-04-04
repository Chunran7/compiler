package com.compiler.yacc.runtime;

import java.util.ArrayList;
import java.util.List;

public class ParseResult {
    private final boolean accepted;
    private final List<Integer> reductions;
    private final String errorMessage;

    private ParseResult(boolean accepted, List<Integer> reductions, String errorMessage) {
        this.accepted = accepted;
        this.reductions = new ArrayList<>(reductions);
        this.errorMessage = errorMessage;
    }

    public static ParseResult success(List<Integer> reductions) {
        return new ParseResult(true, reductions, null);
    }

    public static ParseResult failure(List<Integer> reductions, String errorMessage) {
        return new ParseResult(false, reductions, errorMessage);
    }

    public boolean isAccepted() {
        return accepted;
    }

    public List<Integer> getReductions() {
        return reductions;
    }

    public String getErrorMessage() {
        return errorMessage;
    }
}