package com.example.compiler.semantic;

import com.example.compiler.yacc.ast.AstKind;
import com.example.compiler.yacc.ast.AstNode;
import com.example.compiler.yacc.ast.C99AstNormalizer;
import com.example.compiler.yacc.ast.CoreAstNode;

/**
 * 编译时语义分析器。
 *
 * <p>输入是 c99.y 规约得到的 Parse Tree，先通过 C99AstNormalizer 抽取
 * Core AST，再建立 {@link SymbolTable} 并进行静态语义检查。当前检查包括：
 * 程序必须包含 main、同作用域重复声明、未声明变量、未定义函数调用、
 * 函数实参数量不匹配，以及语句/表达式节点结构合法性。</p>
 */
public final class CompileTimeSemanticAnalyzer {
    private final C99AstNormalizer normalizer = new C99AstNormalizer();

    /**
     * 完成 Core AST 构建和符号表检查。
     *
     * @param parseTreeRoot ParserDriver 产生的 parse tree 根节点
     * @return 语义结果，包含 Core AST 与 SymbolTable；此阶段不直接输出 LLVM
     */
    public SemanticResult analyze(AstNode parseTreeRoot) {
        CoreAstNode coreRoot = normalizer.normalize(parseTreeRoot);
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

            // 第一遍先登记所有函数签名，使得 main 中可以调用定义在后面的函数。
            // 这里只登记函数名和参数数量，不进入函数体检查变量。
            symbolTable.declareFunction(name, parameterCount(function));
        }

        if (!hasMain) {
            throw new SemanticException("Program must define int main()");
        }

        for (CoreAstNode function : node.getChildren()) {
            // 第二遍再逐个分析函数体。这样函数调用检查可以查询完整函数表。
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
            // 参数属于函数作用域，和局部变量使用同一个 SymbolTable 栈。
            // 若参数重名，declare 会按“同一作用域重复声明”报错。
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

        // 每个复合语句块都创建独立作用域，使内层变量可以遮蔽外层变量，
        // 但同一块内重复声明仍然会被 SymbolTable.declare 拦截。
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
        // 声明先写入当前作用域，再检查初始化表达式。
        // 这样可允许类似 int a = a; 被未声明检查捕获为读取当前变量，
        // 具体是否允许自引用可在这里进一步收紧。
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
        // 赋值左侧必须已经声明；右侧表达式会递归检查其中的变量和函数调用。
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
