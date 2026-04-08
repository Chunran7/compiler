package com.compiler.yacc.ast;

import java.util.ArrayList;
import java.util.List;

public class AstNormalizer {

    public CoreAstNode normalize(AstNode parseTreeRoot) {
        if (parseTreeRoot == null) {
            throw new IllegalArgumentException("parseTreeRoot 不能为空");
        }
        return normalizeProgram(parseTreeRoot);
    }

    private CoreAstNode normalizeProgram(AstNode node) {
        requireSymbol(node, "Program");
        AstNode mainFunc = child(node, 0, "MainFunc");
        return CoreAstNode.node(
                AstKind.PROGRAM,
                List.of(normalizeMainFunc(mainFunc))
        );
    }

    private CoreAstNode normalizeMainFunc(AstNode node) {
        requireSymbol(node, "MainFunc");
        AstNode block = child(node, 4, "Block");
        return CoreAstNode.node(
                AstKind.MAIN_FUNCTION,
                List.of(normalizeBlock(block))
        );
    }

    private CoreAstNode normalizeBlock(AstNode node) {
        requireSymbol(node, "Block");

        AstNode itemList = child(node, 1, "ItemList");
        List<CoreAstNode> statements = new ArrayList<>();
        collectItemList(itemList, statements);

        return CoreAstNode.node(AstKind.BLOCK, statements);
    }

    /**
     * ItemList -> ItemList Item | ε
     * 把左递归链压平成一个线性列表
     */
    private void collectItemList(AstNode node, List<CoreAstNode> out) {
        requireSymbol(node, "ItemList");

        if (node.getChildren().isEmpty()) {
            return; // ε
        }

        if (node.getChildren().size() == 2) {
            collectItemList(child(node, 0, "ItemList"), out);
            out.add(normalizeItem(child(node, 1, "Item")));
            return;
        }

        throw new IllegalStateException("非法 ItemList 结构: " + node.prettyPrint());
    }

    private CoreAstNode normalizeItem(AstNode node) {
        requireSymbol(node, "Item");
        AstNode actual = node.getChildren().get(0);

        return switch (actual.getSymbolName()) {
            case "Decl" -> normalizeDecl(actual);
            case "Stmt" -> normalizeStmt(actual);
            default -> throw new IllegalStateException("未知 Item 子节点: " + actual.getSymbolName());
        };
    }

    private CoreAstNode normalizeDecl(AstNode node) {
        requireSymbol(node, "Decl");

        AstNode idNode = child(node, 1, "ID");
        AstNode declInitOpt = child(node, 2, "DeclInitOpt");

        List<CoreAstNode> children = new ArrayList<>();
        children.add(identifierNode(idNode));

        if (!declInitOpt.getChildren().isEmpty()) {
            // DeclInitOpt -> ASSIGN Expr
            AstNode expr = child(declInitOpt, 1, "Expr");
            children.add(normalizeExpr(expr));
        }

        return CoreAstNode.node(AstKind.DECLARATION, children);
    }

    private CoreAstNode normalizeStmt(AstNode node) {
        requireSymbol(node, "Stmt");
        AstNode actual = node.getChildren().get(0);

        return switch (actual.getSymbolName()) {
            case "MatchedStmt" -> normalizeMatchedStmt(actual);
            case "UnmatchedStmt" -> normalizeUnmatchedStmt(actual);
            default -> throw new IllegalStateException("未知 Stmt 子节点: " + actual.getSymbolName());
        };
    }

    private CoreAstNode normalizeMatchedStmt(AstNode node) {
        requireSymbol(node, "MatchedStmt");

        if (node.getChildren().size() == 1) {
            AstNode only = node.getChildren().get(0);
            return switch (only.getSymbolName()) {
                case "AssignStmt" -> normalizeAssignStmt(only);
                case "ReturnStmt" -> normalizeReturnStmt(only);
                case "Block" -> normalizeBlock(only);
                default -> throw new IllegalStateException("未知 MatchedStmt 单子节点: " + only.getSymbolName());
            };
        }

        // WHILE LPAREN Cond RPAREN MatchedStmt
        if ("WHILE".equals(node.getChildren().get(0).getSymbolName())) {
            AstNode cond = child(node, 2, "Cond");
            AstNode body = child(node, 4, "MatchedStmt");
            return CoreAstNode.node(
                    AstKind.WHILE_STMT,
                    List.of(
                            normalizeCond(cond),
                            normalizeMatchedStmt(body)
                    )
            );
        }

        // IF LPAREN Cond RPAREN MatchedStmt ELSE MatchedStmt
        if ("IF".equals(node.getChildren().get(0).getSymbolName())) {
            AstNode cond = child(node, 2, "Cond");
            AstNode thenBranch = child(node, 4, "MatchedStmt");
            AstNode elseBranch = child(node, 6, "MatchedStmt");

            return CoreAstNode.node(
                    AstKind.IF_STMT,
                    List.of(
                            normalizeCond(cond),
                            normalizeMatchedStmt(thenBranch),
                            normalizeMatchedStmt(elseBranch)
                    )
            );
        }

        throw new IllegalStateException("非法 MatchedStmt 结构: " + node.prettyPrint());
    }

    private CoreAstNode normalizeUnmatchedStmt(AstNode node) {
        requireSymbol(node, "UnmatchedStmt");

        // IF LPAREN Cond RPAREN Stmt
        if (node.getChildren().size() == 5 && "IF".equals(node.getChildren().get(0).getSymbolName())) {
            AstNode cond = child(node, 2, "Cond");
            AstNode thenBranch = child(node, 4, "Stmt");

            return CoreAstNode.node(
                    AstKind.IF_STMT,
                    List.of(
                            normalizeCond(cond),
                            normalizeStmt(thenBranch)
                    )
            );
        }

        // IF LPAREN Cond RPAREN MatchedStmt ELSE UnmatchedStmt
        if (node.getChildren().size() == 7 && "IF".equals(node.getChildren().get(0).getSymbolName())) {
            AstNode cond = child(node, 2, "Cond");
            AstNode thenBranch = child(node, 4, "MatchedStmt");
            AstNode elseBranch = child(node, 6, "UnmatchedStmt");

            return CoreAstNode.node(
                    AstKind.IF_STMT,
                    List.of(
                            normalizeCond(cond),
                            normalizeMatchedStmt(thenBranch),
                            normalizeUnmatchedStmt(elseBranch)
                    )
            );
        }

        // WHILE LPAREN Cond RPAREN UnmatchedStmt
        if ("WHILE".equals(node.getChildren().get(0).getSymbolName())) {
            AstNode cond = child(node, 2, "Cond");
            AstNode body = child(node, 4, "UnmatchedStmt");

            return CoreAstNode.node(
                    AstKind.WHILE_STMT,
                    List.of(
                            normalizeCond(cond),
                            normalizeUnmatchedStmt(body)
                    )
            );
        }

        throw new IllegalStateException("非法 UnmatchedStmt 结构: " + node.prettyPrint());
    }

    private CoreAstNode normalizeAssignStmt(AstNode node) {
        requireSymbol(node, "AssignStmt");

        AstNode idNode = child(node, 0, "ID");
        AstNode expr = child(node, 2, "Expr");

        return CoreAstNode.node(
                AstKind.ASSIGNMENT,
                List.of(
                        identifierNode(idNode),
                        normalizeExpr(expr)
                )
        );
    }

    private CoreAstNode normalizeReturnStmt(AstNode node) {
        requireSymbol(node, "ReturnStmt");
        AstNode expr = child(node, 1, "Expr");

        return CoreAstNode.node(
                AstKind.RETURN_STMT,
                List.of(normalizeExpr(expr))
        );
    }

    private CoreAstNode normalizeCond(AstNode node) {
        requireSymbol(node, "Cond");

        AstNode leftExpr = child(node, 0, "Expr");
        AstNode relOp = child(node, 1, "RelOp");
        AstNode rightExpr = child(node, 2, "Expr");

        return CoreAstNode.node(
                AstKind.BINARY_EXPR,
                relOperatorText(relOp),
                List.of(
                        normalizeExpr(leftExpr),
                        normalizeExpr(rightExpr)
                )
        );
    }

    private String relOperatorText(AstNode relOpNode) {
        requireSymbol(relOpNode, "RelOp");
        AstNode terminal = relOpNode.getChildren().get(0);
        return terminal.getLexeme();
    }

    private CoreAstNode normalizeExpr(AstNode node) {
        requireSymbol(node, "Expr");

        if (node.getChildren().size() == 1) {
            return normalizeTerm(child(node, 0, "Term"));
        }

        AstNode leftExpr = child(node, 0, "Expr");
        AstNode op = node.getChildren().get(1);
        AstNode rightTerm = child(node, 2, "Term");

        return CoreAstNode.node(
                AstKind.BINARY_EXPR,
                op.getLexeme(),
                List.of(
                        normalizeExpr(leftExpr),
                        normalizeTerm(rightTerm)
                )
        );
    }

    private CoreAstNode normalizeTerm(AstNode node) {
        requireSymbol(node, "Term");

        if (node.getChildren().size() == 1) {
            return normalizeFactor(child(node, 0, "Factor"));
        }

        AstNode leftTerm = child(node, 0, "Term");
        AstNode op = node.getChildren().get(1);
        AstNode rightFactor = child(node, 2, "Factor");

        return CoreAstNode.node(
                AstKind.BINARY_EXPR,
                op.getLexeme(),
                List.of(
                        normalizeTerm(leftTerm),
                        normalizeFactor(rightFactor)
                )
        );
    }

    private CoreAstNode normalizeFactor(AstNode node) {
        requireSymbol(node, "Factor");

        if (node.getChildren().size() == 1) {
            AstNode only = node.getChildren().get(0);

            return switch (only.getSymbolName()) {
                case "ID" -> identifierNode(only);
                case "NUM" -> intLiteralNode(only);
                default -> throw new IllegalStateException("未知 Factor 单子节点: " + only.getSymbolName());
            };
        }

        // Factor -> LPAREN Expr RPAREN
        AstNode expr = child(node, 1, "Expr");
        return normalizeExpr(expr);
    }

    private CoreAstNode identifierNode(AstNode idNode) {
        requireSymbol(idNode, "ID");
        return CoreAstNode.leaf(AstKind.IDENTIFIER, idNode.getLexeme());
    }

    private CoreAstNode intLiteralNode(AstNode numNode) {
        requireSymbol(numNode, "NUM");
        return CoreAstNode.leaf(AstKind.INT_LITERAL, numNode.getLexeme());
    }

    private AstNode child(AstNode node, int index, String expectedSymbol) {
        AstNode child = node.getChildren().get(index);
        requireSymbol(child, expectedSymbol);
        return child;
    }

    private void requireSymbol(AstNode node, String expectedSymbol) {
        if (!expectedSymbol.equals(node.getSymbolName())) {
            throw new IllegalStateException(
                    "期望符号 " + expectedSymbol + "，实际是 " + node.getSymbolName()
            );
        }
    }
}
