package com.example.compiler.semantic;

import com.example.compiler.ir.IrInstruction;
import com.example.compiler.ir.IrOp;
import com.example.compiler.yacc.ast.AstKind;
import com.example.compiler.yacc.ast.AstNode;
import com.example.compiler.yacc.ast.CoreAstNode;

import java.util.ArrayList;
import java.util.List;

public final class SemanticActionEngine {
    private final TranslationSchemeExecutor executor = new TranslationSchemeExecutor();

    private List<IrInstruction> instructions;
    private int tempCounter;
    private int labelCounter;

    public SemanticResult analyze(AstNode parseTreeRoot) {
        executor.execute(parseTreeRoot);

        Object semanticRoot = parseTreeRoot.getSemanticValue();
        if (!(semanticRoot instanceof CoreAstNode coreRoot)) {
            throw new IllegalStateException(
                    "TranslationSchemeExecutor did not produce a CoreAstNode root, actual value: " + semanticRoot
            );
        }

        SymbolTable symbolTable = new SymbolTable();
        this.instructions = new ArrayList<>();
        this.tempCounter = 0;
        this.labelCounter = 0;

        generateProgram(coreRoot, symbolTable);

        return new SemanticResult(coreRoot, symbolTable, instructions);
    }

    private void generateProgram(CoreAstNode node, SymbolTable symbolTable) {
        expectKind(node, AstKind.PROGRAM);

        List<CoreAstNode> functions = node.getChildren();
        if (functions.isEmpty()) {
            throw new SemanticException("Program must contain at least one function");
        }

        boolean hasMain = false;

        for (CoreAstNode function : functions) {
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

        for (CoreAstNode function : functions) {
            generateFunction(function, symbolTable);
        }
    }

    private int parameterCount(CoreAstNode function) {
        return function.getChildren().size() - 1;
    }

    private void generateFunction(CoreAstNode node, SymbolTable symbolTable) {
        if (node.getKind() != AstKind.FUNCTION_DEF && node.getKind() != AstKind.MAIN_FUNCTION) {
            throw new IllegalStateException("Expected function node but got " + node.getKind());
        }

        if (node.getChildren().isEmpty()) {
            throw new SemanticException("Function must contain a block body");
        }

        String functionName = node.getText();
        instructions.add(IrInstruction.functionBegin(functionName));

        symbolTable.enterScope();
        try {
            for (int i = 0; i < node.getChildren().size() - 1; i++) {
                CoreAstNode param = node.getChildren().get(i);
                expectKind(param, AstKind.PARAMETER);
                symbolTable.declare(param.getText(), SymbolType.INT);
            }

            CoreAstNode block = node.getChildren().get(node.getChildren().size() - 1);
            generateBlock(block, symbolTable);
        } finally {
            symbolTable.exitScope();
        }

        instructions.add(IrInstruction.functionEnd(functionName));
    }

    private void generateBlock(CoreAstNode node, SymbolTable symbolTable) {
        expectKind(node, AstKind.BLOCK);

        symbolTable.enterScope();
        try {
            for (CoreAstNode child : node.getChildren()) {
                generateStatement(child, symbolTable);
            }
        } finally {
            symbolTable.exitScope();
        }
    }

    private void generateStatement(CoreAstNode node, SymbolTable symbolTable) {
        switch (node.getKind()) {
            case DECLARATION -> generateDeclaration(node, symbolTable);
            case ASSIGNMENT -> generateAssignment(node, symbolTable);
            case EXPRESSION_STMT -> generateExpressionStatement(node, symbolTable);
            case RETURN_STMT -> generateReturn(node, symbolTable);
            case IF_STMT -> generateIf(node, symbolTable);
            case WHILE_STMT -> generateWhile(node, symbolTable);
            case BLOCK -> generateBlock(node, symbolTable);
            default -> throw new IllegalStateException("Unsupported statement kind in semantic phase: " + node.getKind());
        }
    }

    private void generateDeclaration(CoreAstNode node, SymbolTable symbolTable) {
        expectKind(node, AstKind.DECLARATION);

        if (node.getChildren().isEmpty()) {
            throw new SemanticException("Declaration must contain an identifier");
        }

        CoreAstNode identifier = node.getChildren().get(0);
        expectKind(identifier, AstKind.IDENTIFIER);

        symbolTable.declare(identifier.getText(), SymbolType.INT);

        if (node.getChildren().size() >= 2) {
            String value = generateExpression(node.getChildren().get(1), symbolTable);
            instructions.add(IrInstruction.assign(identifier.getText(), value));
        }
    }

    private void generateAssignment(CoreAstNode node, SymbolTable symbolTable) {
        expectKind(node, AstKind.ASSIGNMENT);

        if (node.getChildren().size() != 2) {
            throw new SemanticException("Assignment must contain lhs and rhs");
        }

        CoreAstNode identifier = node.getChildren().get(0);
        expectKind(identifier, AstKind.IDENTIFIER);
        ensureDeclared(identifier.getText(), symbolTable);

        String value = generateExpression(node.getChildren().get(1), symbolTable);
        instructions.add(IrInstruction.assign(identifier.getText(), value));
    }

    private void generateExpressionStatement(CoreAstNode node, SymbolTable symbolTable) {
        expectKind(node, AstKind.EXPRESSION_STMT);

        if (node.getChildren().size() != 1) {
            throw new SemanticException("Expression statement must contain one expression");
        }

        CoreAstNode expr = node.getChildren().get(0);
        if (expr.getKind() == AstKind.FUNCTION_CALL) {
            emitFunctionCall(expr, symbolTable, false);
            return;
        }

        generateExpression(expr, symbolTable);
    }

    private void generateReturn(CoreAstNode node, SymbolTable symbolTable) {
        expectKind(node, AstKind.RETURN_STMT);

        if (node.getChildren().size() != 1) {
            throw new SemanticException("Return statement must contain one expression");
        }

        String value = generateExpression(node.getChildren().get(0), symbolTable);
        instructions.add(IrInstruction.ret(value));
    }

    private void generateIf(CoreAstNode node, SymbolTable symbolTable) {
        expectKind(node, AstKind.IF_STMT);

        if (node.getChildren().size() != 2 && node.getChildren().size() != 3) {
            throw new SemanticException("If statement must contain 2 or 3 children");
        }

        String condition = generateExpression(node.getChildren().get(0), symbolTable);

        if (node.getChildren().size() == 2) {
            String endLabel = nextLabel();
            instructions.add(IrInstruction.ifFalseGoTo(condition, endLabel));
            generateStatement(node.getChildren().get(1), symbolTable);
            instructions.add(IrInstruction.label(endLabel));
            return;
        }

        String elseLabel = nextLabel();
        String endLabel = nextLabel();

        instructions.add(IrInstruction.ifFalseGoTo(condition, elseLabel));
        generateStatement(node.getChildren().get(1), symbolTable);
        instructions.add(IrInstruction.goTo(endLabel));
        instructions.add(IrInstruction.label(elseLabel));
        generateStatement(node.getChildren().get(2), symbolTable);
        instructions.add(IrInstruction.label(endLabel));
    }

    private void generateWhile(CoreAstNode node, SymbolTable symbolTable) {
        expectKind(node, AstKind.WHILE_STMT);

        if (node.getChildren().size() != 2) {
            throw new SemanticException("While statement must contain condition and body");
        }

        String startLabel = nextLabel();
        String endLabel = nextLabel();

        instructions.add(IrInstruction.label(startLabel));
        String condition = generateExpression(node.getChildren().get(0), symbolTable);
        instructions.add(IrInstruction.ifFalseGoTo(condition, endLabel));
        generateStatement(node.getChildren().get(1), symbolTable);
        instructions.add(IrInstruction.goTo(startLabel));
        instructions.add(IrInstruction.label(endLabel));
    }

    private String generateExpression(CoreAstNode node, SymbolTable symbolTable) {
        return switch (node.getKind()) {
            case IDENTIFIER -> {
                ensureDeclared(node.getText(), symbolTable);
                yield node.getText();
            }
            case INT_LITERAL -> node.getText();
            case BINARY_EXPR -> generateBinary(node, symbolTable);
            case FUNCTION_CALL -> emitFunctionCall(node, symbolTable, true);
            default -> throw new IllegalStateException("Unsupported expression kind in semantic phase: " + node.getKind());
        };
    }

    private String generateBinary(CoreAstNode node, SymbolTable symbolTable) {
        if (node.getChildren().size() != 2) {
            throw new SemanticException("Binary expression must contain 2 operands");
        }

        String left = generateExpression(node.getChildren().get(0), symbolTable);
        String right = generateExpression(node.getChildren().get(1), symbolTable);
        String temp = nextTemp();

        instructions.add(IrInstruction.binary(mapBinaryOp(node.getText()), temp, left, right));
        return temp;
    }

    private String emitFunctionCall(CoreAstNode node, SymbolTable symbolTable, boolean withResult) {
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

        List<String> args = new ArrayList<>();
        for (CoreAstNode arg : node.getChildren()) {
            args.add(generateExpression(arg, symbolTable));
        }

        String argsText = String.join(", ", args);
        String target = withResult ? nextTemp() : null;
        instructions.add(IrInstruction.call(target, functionName, argsText));
        return target;
    }

    private void ensureDeclared(String name, SymbolTable symbolTable) {
        if (symbolTable.resolve(name) == null) {
            throw new SemanticException("Use of undeclared identifier: " + name);
        }
    }

    private IrOp mapBinaryOp(String op) {
        return switch (op) {
            case "+" -> IrOp.ADD;
            case "-" -> IrOp.SUB;
            case "*" -> IrOp.MUL;
            case "/" -> IrOp.DIV;
            case "<" -> IrOp.LT;
            case "<=" -> IrOp.LE;
            case ">" -> IrOp.GT;
            case ">=" -> IrOp.GE;
            case "==" -> IrOp.EQ;
            case "!=" -> IrOp.NE;
            default -> throw new IllegalStateException("Unsupported operator: " + op);
        };
    }

    private String nextTemp() {
        return "t" + (++tempCounter);
    }

    private String nextLabel() {
        return "L" + (++labelCounter);
    }

    private void expectKind(CoreAstNode node, AstKind expected) {
        if (node.getKind() != expected) {
            throw new IllegalStateException("Expected " + expected + " but got " + node.getKind());
        }
    }
}