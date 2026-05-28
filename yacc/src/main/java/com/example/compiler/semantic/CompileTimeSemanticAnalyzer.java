package com.example.compiler.semantic;

import com.example.compiler.yacc.ast.AstKind;
import com.example.compiler.yacc.ast.CoreAstNode;

public final class CompileTimeSemanticAnalyzer {
    public SemanticResult analyze(CoreAstNode coreRoot) {
        SymbolTable symbolTable = new SymbolTable();
        analyzeProgram(coreRoot, symbolTable);
        return new SemanticResult(coreRoot, symbolTable, java.util.List.of());
    }

    private void analyzeProgram(CoreAstNode node, SymbolTable symbolTable) {
        expectKind(node, AstKind.PROGRAM);

        if (node.getChildren().isEmpty()) {
            throw new SemanticException("Program must contain at least one function");
        }

        boolean hasMain = false;
        for (CoreAstNode function : node.getChildren()) {
            if (function.getKind() != AstKind.FUNCTION_DEF && function.getKind() != AstKind.MAIN_FUNCTION) {
                throw new IllegalStateException("PROGRAM child must be FUNCTION_DEF or MAIN_FUNCTION, but got " + function.getKind());
            }

            String name = function.getText();
            if (name == null || name.isBlank()) {
                throw new SemanticException("Function name cannot be empty");
            }

            if ("main".equals(name)) {
                hasMain = true;
            }

            symbolTable.declareFunction(name, parameterCount(function));
        }

        if (!hasMain) {
            throw new SemanticException("Program must define int main()");
        }

        for (CoreAstNode function : node.getChildren()) {
            analyzeFunction(function, symbolTable);
        }
    }

    private int parameterCount(CoreAstNode function) {
        return function.getChildren().size() - 1;
    }

    private void analyzeFunction(CoreAstNode node, SymbolTable symbolTable) {
        if (node.getKind() != AstKind.FUNCTION_DEF && node.getKind() != AstKind.MAIN_FUNCTION) {
            throw new IllegalStateException("Expected function node but got " + node.getKind());
        }

        if (node.getChildren().isEmpty()) {
            throw new SemanticException("Function must contain a block body");
        }

        symbolTable.enterScope();
        try {
            for (int i = 0; i < node.getChildren().size() - 1; i++) {
                CoreAstNode param = node.getChildren().get(i);
                expectKind(param, AstKind.PARAMETER);
                symbolTable.declare(param.getText(), SymbolType.INT);
            }

            CoreAstNode block = node.getChildren().get(node.getChildren().size() - 1);
            analyzeBlock(block, symbolTable);
        } finally {
            symbolTable.exitScope();
        }
    }

    private void analyzeBlock(CoreAstNode node, SymbolTable symbolTable) {
        expectKind(node, AstKind.BLOCK);

        symbolTable.enterScope();
        try {
            for (CoreAstNode child : node.getChildren()) {
                analyzeStatement(child, symbolTable);
            }
        } finally {
            symbolTable.exitScope();
        }
    }

    private void analyzeStatement(CoreAstNode node, SymbolTable symbolTable) {
        switch (node.getKind()) {
            case DECLARATION -> analyzeDeclaration(node, symbolTable);
            case ASSIGNMENT -> analyzeAssignment(node, symbolTable);
            case EXPRESSION_STMT -> analyzeExpressionStatement(node, symbolTable);
            case RETURN_STMT -> analyzeReturn(node, symbolTable);
            case IF_STMT -> analyzeIf(node, symbolTable);
            case WHILE_STMT -> analyzeWhile(node, symbolTable);
            case BLOCK -> analyzeBlock(node, symbolTable);
            default -> throw new IllegalStateException("Unsupported statement kind in semantic phase: " + node.getKind());
        }
    }

    private void analyzeDeclaration(CoreAstNode node, SymbolTable symbolTable) {
        expectKind(node, AstKind.DECLARATION);

        if (node.getChildren().isEmpty()) {
            throw new SemanticException("Declaration must contain an identifier");
        }

        CoreAstNode identifier = node.getChildren().get(0);
        expectKind(identifier, AstKind.IDENTIFIER);
        symbolTable.declare(identifier.getText(), SymbolType.INT);

        if (node.getChildren().size() >= 2) {
            analyzeExpression(node.getChildren().get(1), symbolTable);
        }
    }

    private void analyzeAssignment(CoreAstNode node, SymbolTable symbolTable) {
        expectKind(node, AstKind.ASSIGNMENT);

        if (node.getChildren().size() != 2) {
            throw new SemanticException("Assignment must contain lhs and rhs");
        }

        CoreAstNode identifier = node.getChildren().get(0);
        expectKind(identifier, AstKind.IDENTIFIER);
        ensureDeclared(identifier.getText(), symbolTable);
        analyzeExpression(node.getChildren().get(1), symbolTable);
    }

    private void analyzeExpressionStatement(CoreAstNode node, SymbolTable symbolTable) {
        expectKind(node, AstKind.EXPRESSION_STMT);
        if (node.getChildren().size() != 1) {
            throw new SemanticException("Expression statement must contain one expression");
        }
        analyzeExpression(node.getChildren().get(0), symbolTable);
    }

    private void analyzeReturn(CoreAstNode node, SymbolTable symbolTable) {
        expectKind(node, AstKind.RETURN_STMT);
        if (node.getChildren().size() != 1) {
            throw new SemanticException("Return statement must contain one expression");
        }
        analyzeExpression(node.getChildren().get(0), symbolTable);
    }

    private void analyzeIf(CoreAstNode node, SymbolTable symbolTable) {
        expectKind(node, AstKind.IF_STMT);
        if (node.getChildren().size() != 2 && node.getChildren().size() != 3) {
            throw new SemanticException("If statement must contain 2 or 3 children");
        }
        analyzeExpression(node.getChildren().get(0), symbolTable);
        analyzeStatement(node.getChildren().get(1), symbolTable);
        if (node.getChildren().size() == 3) {
            analyzeStatement(node.getChildren().get(2), symbolTable);
        }
    }

    private void analyzeWhile(CoreAstNode node, SymbolTable symbolTable) {
        expectKind(node, AstKind.WHILE_STMT);
        if (node.getChildren().size() != 2) {
            throw new SemanticException("While statement must contain condition and body");
        }
        analyzeExpression(node.getChildren().get(0), symbolTable);
        analyzeStatement(node.getChildren().get(1), symbolTable);
    }

    private void analyzeExpression(CoreAstNode node, SymbolTable symbolTable) {
        switch (node.getKind()) {
            case IDENTIFIER -> ensureDeclared(node.getText(), symbolTable);
            case INT_LITERAL -> {
            }
            case BINARY_EXPR -> {
                if (node.getChildren().size() != 2) {
                    throw new SemanticException("Binary expression must contain 2 operands");
                }
                analyzeExpression(node.getChildren().get(0), symbolTable);
                analyzeExpression(node.getChildren().get(1), symbolTable);
            }
            case FUNCTION_CALL -> analyzeFunctionCall(node, symbolTable);
            default -> throw new IllegalStateException("Unsupported expression kind in semantic phase: " + node.getKind());
        }
    }

    private void analyzeFunctionCall(CoreAstNode node, SymbolTable symbolTable) {
        expectKind(node, AstKind.FUNCTION_CALL);

        String functionName = node.getText();
        Symbol function = symbolTable.resolveFunction(functionName);
        if (function == null) {
            throw new SemanticException("Call to undefined function: " + functionName);
        }

        if (function.parameterCount() != node.getChildren().size()) {
            throw new SemanticException(
                    "Argument count mismatch for function " + functionName
                            + ": expected " + function.parameterCount()
                            + ", actual " + node.getChildren().size()
            );
        }

        for (CoreAstNode arg : node.getChildren()) {
            analyzeExpression(arg, symbolTable);
        }
    }

    private void ensureDeclared(String name, SymbolTable symbolTable) {
        if (symbolTable.resolve(name) == null) {
            throw new SemanticException("Use of undeclared identifier: " + name);
        }
    }

    private void expectKind(CoreAstNode node, AstKind expected) {
        if (node.getKind() != expected) {
            throw new IllegalStateException("Expected " + expected + " but got " + node.getKind());
        }
    }
}
