package com.example.compiler.yacc.runtime;

import com.example.compiler.yacc.ast.AstNode;

import java.util.ArrayList;
import java.util.List;

public final class ParseResult {
    private final boolean accepted;
    private final List<Integer> reductions;
    private final AstNode astRoot;
    private final String errorMessage;

    private ParseResult(boolean accepted, List<Integer> reductions, AstNode astRoot, String errorMessage) {
        this.accepted = accepted;
        this.reductions = new ArrayList<>(reductions);
        this.astRoot = astRoot;
        this.errorMessage = errorMessage;
    }

    public static ParseResult success(List<Integer> reductions, AstNode astRoot) {
        return new ParseResult(true, reductions, astRoot, null);
    }

    public static ParseResult failure(List<Integer> reductions, String errorMessage) {
        return new ParseResult(false, reductions, null, errorMessage);
    }

    public boolean isAccepted() {
        return accepted;
    }

    public List<Integer> getReductions() {
        return List.copyOf(reductions);
    }

    public AstNode getAstRoot() {
        return astRoot;
    }

    public String getErrorMessage() {
        return errorMessage;
    }
}
