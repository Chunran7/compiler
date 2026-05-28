package com.example.compiler.yacc.ast;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * C99 parse tree 到 Core AST 的归一化器。
 *
 * <p>输入是 ParserDriver 根据完整 {@code c99.y} 规约出的 Parse Tree；
 * 输出是项目中间代码生成所需的 MiniC 子集 {@link CoreAstNode}。这正是
 * “语法覆盖完整 c99.y，但语义/IR 只支持课程子集”的边界所在。</p>
 *
 * <p>当前支持函数定义、参数、复合语句、声明、赋值、return、if、while、
 * 二元表达式、函数调用、标识符和整数字面量。数组、结构体、指针等复杂 C99
 * 构造不会作为完整语义进入 Core AST。</p>
 */
public final class C99AstNormalizer {

    /**
     * 从 translation_unit 根节点开始抽取 Core AST。
     *
     * @param parseTreeRoot c99.y 语法树根节点
     * @return PROGRAM 类型的 Core AST 根节点
     */
    public CoreAstNode normalize(AstNode parseTreeRoot) {
        requireSymbol(parseTreeRoot, "translation_unit");
        List<CoreAstNode> functions = new ArrayList<>();
        collectTranslationUnit(parseTreeRoot, functions);
        return CoreAstNode.node(AstKind.PROGRAM, functions);
    }

    // ── translation_unit ──

    private void collectTranslationUnit(AstNode node, List<CoreAstNode> out) {
        requireSymbol(node, "translation_unit");
        List<AstNode> kids = children(node);

        if (kids.size() == 1) {
            // external_declaration
            collectExternalDeclaration(expect(kids.get(0), "external_declaration"), out);
        } else if (kids.size() == 2) {
            // translation_unit external_declaration
            collectTranslationUnit(expect(kids.get(0), "translation_unit"), out);
            collectExternalDeclaration(expect(kids.get(1), "external_declaration"), out);
        } else {
            throw structureError(node);
        }
    }

    private void collectExternalDeclaration(AstNode node, List<CoreAstNode> out) {
        requireSymbol(node, "external_declaration");
        AstNode inner = children(node).get(0);
        switch (inner.getSymbolName()) {
            case "function_definition" -> out.add(normalizeFunctionDefinition(inner));
            case "declaration" -> { /* skip global declarations for now */ }
            default -> { /* skip other external declarations */ }
        }
    }

    // ── function_definition ──

    private CoreAstNode normalizeFunctionDefinition(AstNode node) {
        requireSymbol(node, "function_definition");
        List<AstNode> kids = children(node);

        AstNode declSpecs = expect(kids.get(0), "declaration_specifiers");
        AstNode declarator = expect(kids.get(1), "declarator");

        // body may be at index 2 (no declaration_list) or 3 (with declaration_list)
        AstNode body;
        if (kids.size() == 3) {
            body = expect(kids.get(2), "compound_statement");
        } else if (kids.size() == 4) {
            // declaration_list at index 2, compound_statement at index 3
            body = expect(kids.get(3), "compound_statement");
        } else {
            throw structureError(node);
        }

        // Extract function name and params from declarator
        FunctionInfo info = extractFunctionDeclarator(declarator);
        String name = info.name;
        List<CoreAstNode> params = info.params;
        CoreAstNode block = normalizeCompoundStatement(body);

        List<CoreAstNode> children = new ArrayList<>(params);
        children.add(block);

        if ("main".equals(name)) {
            return CoreAstNode.node(AstKind.MAIN_FUNCTION, name, children);
        }
        return CoreAstNode.node(AstKind.FUNCTION_DEF, name, children);
    }

    // ── compound_statement / block ──

    private CoreAstNode normalizeCompoundStatement(AstNode node) {
        requireSymbol(node, "compound_statement");
        List<AstNode> kids = children(node);
        // LBRACE RBRACE → empty block
        if (kids.size() == 2) {
            return CoreAstNode.node(AstKind.BLOCK, List.of());
        }
        // LBRACE block_item_list RBRACE
        AstNode itemList = findChild(node, "block_item_list");
        if (itemList == null) {
            return CoreAstNode.node(AstKind.BLOCK, List.of());
        }
        List<CoreAstNode> items = new ArrayList<>();
        collectBlockItemList(itemList, items);
        return CoreAstNode.node(AstKind.BLOCK, items);
    }

    private void collectBlockItemList(AstNode node, List<CoreAstNode> out) {
        requireSymbol(node, "block_item_list");
        List<AstNode> kids = children(node);
        if (kids.size() == 1) {
            CoreAstNode item = normalizeBlockItem(expect(kids.get(0), "block_item"));
            if (item != null) out.add(item);
        } else if (kids.size() == 2) {
            collectBlockItemList(expect(kids.get(0), "block_item_list"), out);
            CoreAstNode item = normalizeBlockItem(expect(kids.get(1), "block_item"));
            if (item != null) out.add(item);
        } else {
            throw structureError(node);
        }
    }

    private CoreAstNode normalizeBlockItem(AstNode node) {
        requireSymbol(node, "block_item");
        AstNode inner = children(node).get(0);
        return switch (inner.getSymbolName()) {
            case "declaration" -> normalizeDeclaration(inner);
            case "statement" -> normalizeStatement(inner);
            default -> null;
        };
    }

    // ── declaration ──

    private CoreAstNode normalizeDeclaration(AstNode node) {
        requireSymbol(node, "declaration");
        List<AstNode> kids = children(node);

        // declaration_specifiers at index 0 (skip type check for MiniC - assume int)
        // If kids.size() == 2: declaration_specifiers ';' (no declarator)
        // If kids.size() == 3: declaration_specifiers init_declarator_list ';'

        if (kids.size() < 3) return null; // just a type with no declarator, e.g. "int;"

        AstNode initDeclList = expect(kids.get(1), "init_declarator_list");
        return normalizeInitDeclaratorList(initDeclList);
    }

    private CoreAstNode normalizeInitDeclaratorList(AstNode node) {
        requireSymbol(node, "init_declarator_list");
        List<AstNode> kids = children(node);
        // For MiniC, we only handle single declarator: init_declarator
        // init_declarator_list : init_declarator | init_declarator_list ',' init_declarator
        if (kids.size() == 1) {
            return normalizeInitDeclarator(expect(kids.get(0), "init_declarator"));
        }
        // For simplicity, just handle the last one in a list
        return normalizeInitDeclaratorList(kids.get(0));
    }

    private CoreAstNode normalizeInitDeclarator(AstNode node) {
        requireSymbol(node, "init_declarator");
        List<AstNode> kids = children(node);

        // declarator
        AstNode declarator = expect(kids.get(0), "declarator");
        String name = extractVariableName(declarator);

        List<CoreAstNode> result = new ArrayList<>();
        result.add(CoreAstNode.leaf(AstKind.IDENTIFIER, name));

        // declarator '=' initializer
        if (kids.size() >= 3) {
            // '=' at index 1, initializer at index 2
            AstNode initializer = kids.get(2);
            CoreAstNode initExpr = normalizeInitializer(initializer);
            if (initExpr != null) {
                result.add(initExpr);
            }
        }

        return CoreAstNode.node(AstKind.DECLARATION, result);
    }

    private CoreAstNode normalizeInitializer(AstNode node) {
        requireSymbol(node, "initializer");
        List<AstNode> kids = children(node);
        if (kids.size() == 1) {
            // assignment_expression
            return normalizeAssignmentExpression(expect(kids.get(0), "assignment_expression"));
        }
        // For { initializer_list } - skip for MiniC
        return null;
    }

    // ── statement ──

    private CoreAstNode normalizeStatement(AstNode node) {
        requireSymbol(node, "statement");
        AstNode inner = children(node).get(0);
        return switch (inner.getSymbolName()) {
            case "expression_statement" -> normalizeExpressionStatement(inner);
            case "selection_statement" -> normalizeSelectionStatement(inner);
            case "iteration_statement" -> normalizeIterationStatement(inner);
            case "jump_statement" -> normalizeJumpStatement(inner);
            case "compound_statement" -> normalizeCompoundStatement(inner);
            default -> null;
        };
    }

    private CoreAstNode normalizeExpressionStatement(AstNode node) {
        requireSymbol(node, "expression_statement");
        List<AstNode> kids = children(node);
        if (kids.isEmpty() || kids.get(0).getSymbolName().equals(";")) {
            return null; // empty statement ";"
        }
        // expression ';'
        AstNode expr = expect(kids.get(0), "expression");
        return normalizeExpressionAsStatement(expr);
    }

    private CoreAstNode normalizeExpressionAsStatement(AstNode node) {
        // Check if this is an assignment: expression ',' assignment_expression
        // or just assignment_expression
        requireSymbol(node, "expression");
        List<AstNode> kids = children(node);

        AstNode assignExpr;
        if (kids.size() == 1) {
            assignExpr = kids.get(0);
        } else {
            // expression ',' assignment_expression - take the last one
            assignExpr = kids.get(kids.size() - 1);
        }

        CoreAstNode exprNode = normalizeAssignmentExpression(assignExpr);
        if (exprNode == null) return null;

        // If it's a function call, wrap in EXPRESSION_STMT
        if (exprNode.getKind() == AstKind.FUNCTION_CALL) {
            return CoreAstNode.node(AstKind.EXPRESSION_STMT, List.of(exprNode));
        }

        // If it's a binary/identifier expression without assignment, return as-is for expr-stmt
        return exprNode;
    }

    private CoreAstNode normalizeSelectionStatement(AstNode node) {
        requireSymbol(node, "selection_statement");
        List<AstNode> kids = children(node);

        if (kids.size() >= 5 && "IF".equals(kids.get(0).getSymbolName())) {
            // IF '(' expression ')' statement
            // or IF '(' expression ')' statement ELSE statement
            AstNode condExpr = expect(kids.get(2), "expression");
            AstNode thenStmt = expect(kids.get(4), "statement");

            if (kids.size() >= 7 && "ELSE".equals(kids.get(5).getSymbolName())) {
                AstNode elseStmt = expect(kids.get(6), "statement");
                return CoreAstNode.node(AstKind.IF_STMT,
                    List.of(
                        normalizeExpression(condExpr),
                        normalizeStatement(thenStmt),
                        normalizeStatement(elseStmt)
                    ));
            } else {
                return CoreAstNode.node(AstKind.IF_STMT,
                    List.of(
                        normalizeExpression(condExpr),
                        normalizeStatement(thenStmt)
                    ));
            }
        }
        // SWITCH - not supported in MiniC
        return null;
    }

    private CoreAstNode normalizeIterationStatement(AstNode node) {
        requireSymbol(node, "iteration_statement");
        List<AstNode> kids = children(node);

        if ("WHILE".equals(kids.get(0).getSymbolName())) {
            // WHILE '(' expression ')' statement
            AstNode condExpr = expect(kids.get(2), "expression");
            AstNode bodyStmt = expect(kids.get(4), "statement");
            return CoreAstNode.node(AstKind.WHILE_STMT,
                List.of(normalizeExpression(condExpr), normalizeStatement(bodyStmt)));
        }
        // DO, FOR - not supported in MiniC
        return null;
    }

    private CoreAstNode normalizeJumpStatement(AstNode node) {
        requireSymbol(node, "jump_statement");
        List<AstNode> kids = children(node);

        if ("RETURN".equals(kids.get(0).getSymbolName())) {
            if (kids.size() == 2) {
                // RETURN ';'
                return null; // empty return, skip for MiniC
            }
            // RETURN expression ';'
            AstNode expr = expect(kids.get(1), "expression");
            return CoreAstNode.node(AstKind.RETURN_STMT,
                List.of(normalizeExpression(expr)));
        }
        // GOTO, CONTINUE, BREAK - not supported
        return null;
    }

    // ── expressions ──

    private CoreAstNode normalizeExpression(AstNode node) {
        requireSymbol(node, "expression");
        List<AstNode> kids = children(node);

        if (kids.size() == 1) {
            return normalizeAssignmentExpression(expect(kids.get(0), "assignment_expression"));
        }
        // expression ',' assignment_expression - just take the last for MiniC
        return normalizeAssignmentExpression(expect(kids.get(kids.size() - 1), "assignment_expression"));
    }

    private CoreAstNode normalizeAssignmentExpression(AstNode node) {
        requireSymbol(node, "assignment_expression");
        List<AstNode> kids = children(node);

        if (kids.size() == 1) {
            // conditional_expression
            return normalizeConditionalExpression(expect(kids.get(0), "conditional_expression"));
        }

        // unary_expression assignment_operator assignment_expression
        AstNode lhs = kids.get(0); // unary_expression
        AstNode rhs = expect(kids.get(2), "assignment_expression");

        String lhsName = extractTerminalText(lhs);
        CoreAstNode rhsNode = normalizeAssignmentExpression(rhs);

        return CoreAstNode.node(AstKind.ASSIGNMENT,
            List.of(
                CoreAstNode.leaf(AstKind.IDENTIFIER, lhsName),
                rhsNode
            ));
    }

    private CoreAstNode normalizeConditionalExpression(AstNode node) {
        requireSymbol(node, "conditional_expression");
        List<AstNode> kids = children(node);

        if (kids.size() == 1) {
            return normalizeLogicalOrExpression(expect(kids.get(0), "logical_or_expression"));
        }
        // logical_or_expression '?' expression ':' conditional_expression
        // Ternary - not supported in MiniC, just return the condition
        return normalizeLogicalOrExpression(expect(kids.get(0), "logical_or_expression"));
    }

    private CoreAstNode normalizeLogicalOrExpression(AstNode node) {
        return normalizeBinaryChain(node, "logical_or_expression", "logical_and_expression", "OR_OP");
    }

    private CoreAstNode normalizeLogicalAndExpression(AstNode node) {
        return normalizeBinaryChain(node, "logical_and_expression", "inclusive_or_expression", "AND_OP");
    }

    private CoreAstNode normalizeInclusiveOrExpression(AstNode node) {
        return normalizeBinaryChain(node, "inclusive_or_expression", "exclusive_or_expression", "|");
    }

    private CoreAstNode normalizeExclusiveOrExpression(AstNode node) {
        return normalizeBinaryChain(node, "exclusive_or_expression", "and_expression", "^");
    }

    private CoreAstNode normalizeAndExpression(AstNode node) {
        return normalizeBinaryChain(node, "and_expression", "equality_expression", "&");
    }

    private CoreAstNode normalizeEqualityExpression(AstNode node) {
        return normalizeBinaryChain(node, "equality_expression", "relational_expression", "EQ_OP", "NE_OP");
    }

    private CoreAstNode normalizeRelationalExpression(AstNode node) {
        return normalizeBinaryChain(node, "relational_expression", "shift_expression", "<", ">", "LE_OP", "GE_OP");
    }

    private CoreAstNode normalizeShiftExpression(AstNode node) {
        return normalizeBinaryChain(node, "shift_expression", "additive_expression", "LEFT_OP", "RIGHT_OP");
    }

    private CoreAstNode normalizeAdditiveExpression(AstNode node) {
        return normalizeBinaryChain(node, "additive_expression", "multiplicative_expression", "+", "-");
    }

    private CoreAstNode normalizeMultiplicativeExpression(AstNode node) {
        return normalizeBinaryChain(node, "multiplicative_expression", "cast_expression", "*", "/", "%");
    }

    /**
     * Generic binary expression chain handler. Handles productions like:
     *   X : Y | X op Y
     */
    private CoreAstNode normalizeBinaryChain(AstNode node, String selfSymbol, String nextSymbol, String... opSymbols) {
        requireSymbol(node, selfSymbol);
        List<AstNode> kids = children(node);

        if (kids.size() == 1) {
            // pass through to next level
            return normalizeNextLevel(expect(kids.get(0), nextSymbol));
        }

        // X op Y
        CoreAstNode left = normalizeBinaryChain(expect(kids.get(0), selfSymbol), selfSymbol, nextSymbol, opSymbols);
        String op = extractTerminalText(kids.get(1));
        CoreAstNode right = normalizeNextLevel(expect(kids.get(2), nextSymbol));

        return CoreAstNode.node(AstKind.BINARY_EXPR, op, List.of(left, right));
    }

    private CoreAstNode normalizeNextLevel(AstNode node) {
        return switch (node.getSymbolName()) {
            case "cast_expression" -> normalizeCastExpression(node);
            case "unary_expression" -> normalizeUnaryExpression(node);
            case "postfix_expression" -> normalizePostfixExpression(node);
            case "primary_expression" -> normalizePrimaryExpression(node);
            case "multiplicative_expression" -> normalizeMultiplicativeExpression(node);
            case "additive_expression" -> normalizeAdditiveExpression(node);
            case "shift_expression" -> normalizeShiftExpression(node);
            case "relational_expression" -> normalizeRelationalExpression(node);
            case "equality_expression" -> normalizeEqualityExpression(node);
            case "and_expression" -> normalizeAndExpression(node);
            case "exclusive_or_expression" -> normalizeExclusiveOrExpression(node);
            case "inclusive_or_expression" -> normalizeInclusiveOrExpression(node);
            case "logical_and_expression" -> normalizeLogicalAndExpression(node);
            case "logical_or_expression" -> normalizeLogicalOrExpression(node);
            case "conditional_expression" -> normalizeConditionalExpression(node);
            case "assignment_expression" -> normalizeAssignmentExpression(node);
            default -> throw new IllegalStateException("Unexpected expression node: " + node.getSymbolName());
        };
    }

    private CoreAstNode normalizeCastExpression(AstNode node) {
        requireSymbol(node, "cast_expression");
        List<AstNode> kids = children(node);

        if (kids.size() == 1) {
            return normalizeUnaryExpression(expect(kids.get(0), "unary_expression"));
        }
        // '(' type_name ')' cast_expression - skip cast for MiniC
        return normalizeCastExpression(expect(kids.get(kids.size() - 1), "cast_expression"));
    }

    private CoreAstNode normalizeUnaryExpression(AstNode node) {
        requireSymbol(node, "unary_expression");
        List<AstNode> kids = children(node);

        if (kids.size() == 1) {
            return normalizePostfixExpression(expect(kids.get(0), "postfix_expression"));
        }

        // unary_operator cast_expression, INC_OP/DEC_OP unary_expression, SIZEOF etc.
        // For MiniC, skip unary operators
        String first = kids.get(0).getSymbolName();
        if ("SIZEOF".equals(first)) {
            return null;
        }
        // For INC_OP, DEC_OP, unary_operator - just normalize the inner expression
        AstNode inner = kids.get(kids.size() - 1);
        return normalizeNextLevel(inner);
    }

    private CoreAstNode normalizePostfixExpression(AstNode node) {
        requireSymbol(node, "postfix_expression");
        List<AstNode> kids = children(node);

        if (kids.size() == 1) {
            return normalizePrimaryExpression(expect(kids.get(0), "primary_expression"));
        }

        // Check for function call: postfix_expression '(' ... ')'
        if (kids.size() >= 3) {
            AstNode maybeParen = kids.get(1);
            if (maybeParen.isLeaf() && "(".equals(maybeParen.getSymbolName())
                    || "LPAREN".equals(maybeParen.getSymbolName())) {
                // Function call
                String funcName = extractTerminalText(expect(kids.get(0), "postfix_expression"));
                List<CoreAstNode> args = new ArrayList<>();

                if (kids.size() > 3) {
                    // Has arguments: postfix_expression '(' argument_expression_list ')'
                    if (!")".equals(kids.get(kids.size() - 1).getSymbolName())
                            && !"RPAREN".equals(kids.get(kids.size() - 1).getSymbolName())) {
                        // If the last is not ')', assume the argument list is at index 2
                    }
                    // find the argument_expression_list
                    for (int i = 2; i < kids.size() - 1; i++) {
                        AstNode kid = kids.get(i);
                        if ("argument_expression_list".equals(kid.getSymbolName())) {
                            collectArgExpressionList(kid, args);
                            break;
                        }
                    }
                    // Handle case: postfix_expression '(' ')' where kids = [postfix, (, )]
                    // arguments list might be directly embedded
                }

                return CoreAstNode.node(AstKind.FUNCTION_CALL, funcName, args);
            }

            // Array access: postfix_expression '[' expression ']' - not supported, return inner
            if (maybeParen.isLeaf() && ("[".equals(maybeParen.getSymbolName())
                    || "LBRACKET".equals(maybeParen.getSymbolName()))) {
                return normalizePostfixExpression(expect(kids.get(0), "postfix_expression"));
            }

            // postfix_expression '.' IDENTIFIER or postfix_expression PTR_OP IDENTIFIER
            // Not supported, return inner
            return normalizePostfixExpression(expect(kids.get(0), "postfix_expression"));
        }

        // INC_OP, DEC_OP after postfix - return inner
        return normalizePostfixExpression(expect(kids.get(0), "postfix_expression"));
    }

    private CoreAstNode normalizePrimaryExpression(AstNode node) {
        requireSymbol(node, "primary_expression");
        AstNode inner = children(node).get(0);

        return switch (inner.getSymbolName()) {
            case "IDENTIFIER" -> CoreAstNode.leaf(AstKind.IDENTIFIER, inner.getLexeme());
            case "CONSTANT" -> CoreAstNode.leaf(AstKind.INT_LITERAL, inner.getLexeme());
            case "STRING_LITERAL" -> CoreAstNode.leaf(AstKind.INT_LITERAL, inner.getLexeme());
            default -> {
                // '(' expression ')' - recurse into expression
                if ("(".equals(inner.getSymbolName()) || "LPAREN".equals(inner.getSymbolName())) {
                    yield normalizeExpression(findChild(node, "expression"));
                }
                yield null;
            }
        };
    }

    private void collectArgExpressionList(AstNode node, List<CoreAstNode> out) {
        requireSymbol(node, "argument_expression_list");
        List<AstNode> kids = children(node);

        if (kids.size() == 1) {
            out.add(normalizeAssignmentExpression(expect(kids.get(0), "assignment_expression")));
        } else if (kids.size() == 3) {
            collectArgExpressionList(expect(kids.get(0), "argument_expression_list"), out);
            out.add(normalizeAssignmentExpression(expect(kids.get(2), "assignment_expression")));
        } else {
            throw structureError(node);
        }
    }

    // ── helpers ──

    /**
     * Extract function name and parameter list from a declarator.
     * Handles: declarator -> direct_declarator -> IDENTIFIER (params...)
     */
    private FunctionInfo extractFunctionDeclarator(AstNode declarator) {
        requireSymbol(declarator, "declarator");
        List<AstNode> kids = children(declarator);

        AstNode directDecl;
        if (kids.size() == 1) {
            directDecl = kids.get(0);
        } else {
            // pointer direct_declarator - find direct_declarator
            directDecl = findChild(declarator, "direct_declarator");
            if (directDecl == null) directDecl = kids.get(kids.size() - 1);
        }
        requireSymbol(directDecl, "direct_declarator");

        return extractFunctionDirectDeclarator(directDecl);
    }

    private FunctionInfo extractFunctionDirectDeclarator(AstNode node) {
        requireSymbol(node, "direct_declarator");
        List<AstNode> kids = children(node);

        // Case 1: direct_declarator -> IDENTIFIER (simple function name like "main")
        if (kids.size() == 1 && kids.get(0).isLeaf()) {
            return new FunctionInfo(kids.get(0).getLexeme(), List.of());
        }

        // Case 2: direct_declarator -> IDENTIFIER ( parameter_type_list )
        // or direct_declarator -> direct_declarator ( parameter_type_list )
        if (kids.size() >= 3) {
            String name;
            int paramStart;

            if (kids.get(0).isLeaf()) {
                // IDENTIFIER ( ...
                name = kids.get(0).getLexeme();
                paramStart = 2;
            } else {
                // direct_declarator ( ... - recurse to get name
                name = extractFunctionDirectDeclarator(expect(kids.get(0), "direct_declarator")).name;
                paramStart = 2;
            }

            List<CoreAstNode> params = new ArrayList<>();
            // Look for parameter_type_list
            for (int i = paramStart; i < kids.size(); i++) {
                if ("parameter_type_list".equals(kids.get(i).getSymbolName())) {
                    collectParamTypeList(kids.get(i), params);
                    break;
                }
            }
            return new FunctionInfo(name, params);
        }

        // Case 3: direct_declarator -> '(' declarator ')' - recurse
        if (kids.size() == 3 && "(".equals(kids.get(0).getSymbolName())) {
            return extractFunctionDeclarator(expect(kids.get(1), "declarator"));
        }

        // Other cases (arrays, etc.) - try to extract name
        String name = extractTerminalText(node);
        return new FunctionInfo(name, List.of());
    }

    private String extractVariableName(AstNode declarator) {
        requireSymbol(declarator, "declarator");
        return extractTerminalText(declarator);
    }

    private void collectParamTypeList(AstNode node, List<CoreAstNode> out) {
        requireSymbol(node, "parameter_type_list");
        List<AstNode> kids = children(node);

        // parameter_list or parameter_list ',' ELLIPSIS
        AstNode paramList = kids.get(0);
        if ("parameter_list".equals(paramList.getSymbolName())) {
            collectParamList(paramList, out);
        }
    }

    private void collectParamList(AstNode node, List<CoreAstNode> out) {
        requireSymbol(node, "parameter_list");
        List<AstNode> kids = children(node);

        if (kids.size() == 1) {
            CoreAstNode param = normalizeParameterDeclaration(expect(kids.get(0), "parameter_declaration"));
            if (param != null) out.add(param);
        } else if (kids.size() == 3) {
            collectParamList(expect(kids.get(0), "parameter_list"), out);
            CoreAstNode param = normalizeParameterDeclaration(expect(kids.get(2), "parameter_declaration"));
            if (param != null) out.add(param);
        }
    }

    private CoreAstNode normalizeParameterDeclaration(AstNode node) {
        requireSymbol(node, "parameter_declaration");
        // declaration_specifiers declarator  or  declaration_specifiers abstract_declarator  or  declaration_specifiers
        List<AstNode> kids = children(node);
        if (kids.size() < 2) return null;

        AstNode declarator = kids.get(1);
        if ("declarator".equals(declarator.getSymbolName())) {
            String name = extractTerminalText(declarator);
            return CoreAstNode.leaf(AstKind.PARAMETER, name);
        }
        return null;
    }

    // ── general helpers ──

    /**
     * Returns all non-semantic-action children of a node.
     */
    private List<AstNode> children(AstNode node) {
        return node.getChildren().stream()
                .filter(child -> !child.isSemanticActionNode())
                .collect(Collectors.toList());
    }

    /**
     * Extracts the leftmost terminal's lexeme from a subtree.
     */
    private String extractTerminalText(AstNode node) {
        if (node.isLeaf()) {
            return node.getLexeme() != null ? node.getLexeme() : node.getSymbolName();
        }
        for (AstNode child : children(node)) {
            String text = extractTerminalText(child);
            if (text != null) return text;
        }
        return null;
    }

    /**
     * Finds the first child with the given symbol name, recursively.
     */
    private AstNode findChild(AstNode node, String symbol) {
        for (AstNode child : children(node)) {
            if (symbol.equals(child.getSymbolName())) return child;
            AstNode found = findChild(child, symbol);
            if (found != null) return found;
        }
        return null;
    }

    private AstNode expect(AstNode node, String expected) {
        requireSymbol(node, expected);
        return node;
    }

    private void requireSymbol(AstNode node, String expected) {
        if (!expected.equals(node.getSymbolName())) {
            throw new IllegalStateException(
                "Expected symbol " + expected + " but got " + node.getSymbolName());
        }
    }

    private IllegalStateException structureError(AstNode node) {
        return new IllegalStateException(
            "Unexpected structure in " + node.getSymbolName() + " with " + children(node).size() + " children");
    }

    // ── inner types ──

    private static class FunctionInfo {
        final String name;
        final List<CoreAstNode> params;

        FunctionInfo(String name, List<CoreAstNode> params) {
            this.name = name;
            this.params = params;
        }
    }
}
