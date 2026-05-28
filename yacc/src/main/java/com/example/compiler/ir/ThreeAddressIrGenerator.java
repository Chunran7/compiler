package com.example.compiler.ir;

import com.example.compiler.yacc.ast.AstKind;
import com.example.compiler.yacc.ast.CoreAstNode;

import java.util.ArrayList;
import java.util.List;

/**
 * Core AST 到三地址 IR 的生成器。
 *
 * <p>输入是经过语义检查的 Core AST；输出是 IrInstruction 列表。这里的 IR
 * 表达的是运行时动态语义：变量赋值、二元运算、函数调用、return、if/while
 * 控制流等。随后 LlvmLikeTextEmitter 或 CSemanticProgramEmitter 会把这些
 * 语义转换为 LLVM 风格文本。</p>
 */
public final class ThreeAddressIrGenerator {
    private final List<IrInstruction> instructions = new ArrayList<>();
    private int tempCounter;
    private int labelCounter;

    /**
     * 从 PROGRAM 根节点生成完整三地址 IR。
     *
     * @param coreRoot Core AST 根节点
     * @return 不可变 IR 指令列表
     */
    public List<IrInstruction> generate(CoreAstNode coreRoot) {
        instructions.clear();
        tempCounter = 0;
        labelCounter = 0;

        generateProgram(coreRoot);
        return List.copyOf(instructions);
    }

    private void generateProgram(CoreAstNode node) {
        expectKind(node, AstKind.PROGRAM);
        for (CoreAstNode function : node.getChildren()) {
            generateFunction(function);
        }
    }

    private void generateFunction(CoreAstNode node) {
        if (node.getKind() != AstKind.FUNCTION_DEF && node.getKind() != AstKind.MAIN_FUNCTION) {
            throw new IllegalStateException("Expected function node but got " + node.getKind());
        }

        List<String> params = new ArrayList<>();
        for (int i = 0; i < node.getChildren().size() - 1; i++) {
            CoreAstNode param = node.getChildren().get(i);
            expectKind(param, AstKind.PARAMETER);
            params.add(param.getText());
        }

        instructions.add(IrInstruction.functionBegin(node.getText(), params));
        generateBlock(node.getChildren().get(node.getChildren().size() - 1));
        instructions.add(IrInstruction.functionEnd(node.getText()));
    }

    private void generateBlock(CoreAstNode node) {
        expectKind(node, AstKind.BLOCK);
        for (CoreAstNode child : node.getChildren()) {
            generateStatement(child);
        }
    }

    private void generateStatement(CoreAstNode node) {
        switch (node.getKind()) {
            case DECLARATION -> generateDeclaration(node);
            case ASSIGNMENT -> generateAssignment(node);
            case EXPRESSION_STMT -> generateExpressionStatement(node);
            case RETURN_STMT -> generateReturn(node);
            case IF_STMT -> generateIf(node);
            case WHILE_STMT -> generateWhile(node);
            case BLOCK -> generateBlock(node);
            default -> throw new IllegalStateException("Unsupported statement kind in IR generation: " + node.getKind());
        }
    }

    private void generateDeclaration(CoreAstNode node) {
        expectKind(node, AstKind.DECLARATION);
        if (node.getChildren().size() >= 2) {
            CoreAstNode identifier = node.getChildren().get(0);
            String value = generateExpression(node.getChildren().get(1));
            instructions.add(IrInstruction.assign(identifier.getText(), value));
        }
    }

    private void generateAssignment(CoreAstNode node) {
        expectKind(node, AstKind.ASSIGNMENT);
        String value = generateExpression(node.getChildren().get(1));
        instructions.add(IrInstruction.assign(node.getChildren().get(0).getText(), value));
    }

    private void generateExpressionStatement(CoreAstNode node) {
        expectKind(node, AstKind.EXPRESSION_STMT);
        CoreAstNode expr = node.getChildren().get(0);
        if (expr.getKind() == AstKind.FUNCTION_CALL) {
            emitFunctionCall(expr, false);
            return;
        }
        generateExpression(expr);
    }

    private void generateReturn(CoreAstNode node) {
        expectKind(node, AstKind.RETURN_STMT);
        instructions.add(IrInstruction.ret(generateExpression(node.getChildren().get(0))));
    }

    private void generateIf(CoreAstNode node) {
        expectKind(node, AstKind.IF_STMT);

        String condition = generateExpression(node.getChildren().get(0));

        if (node.getChildren().size() == 2) {
            String endLabel = nextLabel();
            instructions.add(IrInstruction.ifFalseGoTo(condition, endLabel));
            generateStatement(node.getChildren().get(1));
            instructions.add(IrInstruction.label(endLabel));
            return;
        }

        String elseLabel = nextLabel();
        String endLabel = nextLabel();

        instructions.add(IrInstruction.ifFalseGoTo(condition, elseLabel));
        generateStatement(node.getChildren().get(1));
        instructions.add(IrInstruction.goTo(endLabel));
        instructions.add(IrInstruction.label(elseLabel));
        generateStatement(node.getChildren().get(2));
        instructions.add(IrInstruction.label(endLabel));
    }

    private void generateWhile(CoreAstNode node) {
        expectKind(node, AstKind.WHILE_STMT);

        String startLabel = nextLabel();
        String endLabel = nextLabel();

        instructions.add(IrInstruction.label(startLabel));
        String condition = generateExpression(node.getChildren().get(0));
        instructions.add(IrInstruction.ifFalseGoTo(condition, endLabel));
        generateStatement(node.getChildren().get(1));
        instructions.add(IrInstruction.goTo(startLabel));
        instructions.add(IrInstruction.label(endLabel));
    }

    private String generateExpression(CoreAstNode node) {
        return switch (node.getKind()) {
            case IDENTIFIER, INT_LITERAL -> node.getText();
            case BINARY_EXPR -> generateBinary(node);
            case FUNCTION_CALL -> emitFunctionCall(node, true);
            default -> throw new IllegalStateException("Unsupported expression kind in IR generation: " + node.getKind());
        };
    }

    private String generateBinary(CoreAstNode node) {
        String left = generateExpression(node.getChildren().get(0));
        String right = generateExpression(node.getChildren().get(1));
        String temp = nextTemp();

        instructions.add(IrInstruction.binary(mapBinaryOp(node.getText()), temp, left, right));
        return temp;
    }

    private String emitFunctionCall(CoreAstNode node, boolean withResult) {
        List<String> args = new ArrayList<>();
        for (CoreAstNode arg : node.getChildren()) {
            args.add(generateExpression(arg));
        }

        String target = withResult ? nextTemp() : null;
        instructions.add(IrInstruction.call(target, node.getText(), args));
        return target;
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
