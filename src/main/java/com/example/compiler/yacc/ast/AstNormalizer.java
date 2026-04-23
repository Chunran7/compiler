package com.example.compiler.yacc.ast;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public final class AstNormalizer {
    public CoreAstNode normalize(AstNode parseTreeRoot) {
        requireSymbol(parseTreeRoot, "Program");
        AstNode funcList = child(parseTreeRoot, 0, "FuncList");
        List<CoreAstNode> functions = new ArrayList<>();
        collectFuncList(funcList, functions);
        return CoreAstNode.node(AstKind.PROGRAM, functions);
    }

    private void collectFuncList(AstNode node, List<CoreAstNode> out) {
        requireSymbol(node, "FuncList");
        List<AstNode> kids = children(node);

        if (kids.size() == 1) {
            out.add(normalizeFuncDef(expect(kids.get(0), "FuncDef")));
            return;
        }
        if (kids.size() == 2) {
            collectFuncList(expect(kids.get(0), "FuncList"), out);
            out.add(normalizeFuncDef(expect(kids.get(1), "FuncDef")));
            return;
        }
        throw new IllegalStateException("Illegal FuncList structure");
    }

    private CoreAstNode normalizeFuncDef(AstNode node) {
        requireSymbol(node, "FuncDef");
        String functionName = normalizeFuncName(child(node, 1, "FuncName"));
        AstNode paramListOpt = child(node, 3, "ParamListOpt");
        AstNode block = child(node, 5, "Block");

        List<CoreAstNode> children = new ArrayList<>();
        collectParamListOpt(paramListOpt, children);
        children.add(normalizeBlock(block));

        if ("main".equals(functionName)) {
            return CoreAstNode.node(AstKind.MAIN_FUNCTION, functionName, children);
        }
        return CoreAstNode.node(AstKind.FUNCTION_DEF, functionName, children);
    }

    private String normalizeFuncName(AstNode node) {
        requireSymbol(node, "FuncName");
        List<AstNode> kids = children(node);
        AstNode actual = kids.get(0);
        return actual.getLexeme();
    }

    private void collectParamListOpt(AstNode node, List<CoreAstNode> out) {
        requireSymbol(node, "ParamListOpt");
        List<AstNode> kids = children(node);
        if (kids.isEmpty()) {
            return;
        }
        collectParamList(expect(kids.get(0), "ParamList"), out);
    }

    private void collectParamList(AstNode node, List<CoreAstNode> out) {
        requireSymbol(node, "ParamList");
        List<AstNode> kids = children(node);

        if (kids.size() == 1) {
            out.add(normalizeParam(expect(kids.get(0), "Param")));
            return;
        }
        if (kids.size() == 3) {
            collectParamList(expect(kids.get(0), "ParamList"), out);
            out.add(normalizeParam(expect(kids.get(2), "Param")));
            return;
        }
        throw new IllegalStateException("Illegal ParamList structure");
    }

    private CoreAstNode normalizeParam(AstNode node) {
        requireSymbol(node, "Param");
        AstNode idNode = child(node, 1, "ID");
        return CoreAstNode.leaf(AstKind.PARAMETER, idNode.getLexeme());
    }

    private CoreAstNode normalizeBlock(AstNode node) {
        requireSymbol(node, "Block");
        AstNode itemList = child(node, 1, "ItemList");
        List<CoreAstNode> statements = new ArrayList<>();
        collectItemList(itemList, statements);
        return CoreAstNode.node(AstKind.BLOCK, statements);
    }

    private void collectItemList(AstNode node, List<CoreAstNode> out) {
        requireSymbol(node, "ItemList");
        List<AstNode> kids = children(node);

        if (kids.isEmpty()) {
            return;
        }
        if (kids.size() == 2) {
            collectItemList(expect(kids.get(0), "ItemList"), out);
            out.add(normalizeItem(expect(kids.get(1), "Item")));
            return;
        }
        throw new IllegalStateException("Illegal ItemList structure");
    }

    private CoreAstNode normalizeItem(AstNode node) {
        requireSymbol(node, "Item");
        AstNode actual = children(node).get(0);

        return switch (actual.getSymbolName()) {
            case "Decl" -> normalizeDecl(actual);
            case "Stmt" -> normalizeStmt(actual);
            default -> throw new IllegalStateException("Unknown item child: " + actual.getSymbolName());
        };
    }

    private CoreAstNode normalizeDecl(AstNode node) {
        requireSymbol(node, "Decl");
        AstNode idNode = child(node, 1, "ID");
        AstNode initOpt = child(node, 2, "DeclInitOpt");

        List<CoreAstNode> result = new ArrayList<>();
        result.add(identifierNode(idNode));

        if (!children(initOpt).isEmpty()) {
            AstNode expr = child(initOpt, 1, "Expr");
            result.add(normalizeExpr(expr));
        }

        return CoreAstNode.node(AstKind.DECLARATION, result);
    }

    private CoreAstNode normalizeStmt(AstNode node) {
        requireSymbol(node, "Stmt");
        AstNode actual = children(node).get(0);

        return switch (actual.getSymbolName()) {
            case "MatchedStmt" -> normalizeMatchedStmt(actual);
            case "UnmatchedStmt" -> normalizeUnmatchedStmt(actual);
            default -> throw new IllegalStateException("Unknown stmt child: " + actual.getSymbolName());
        };
    }

    private CoreAstNode normalizeMatchedStmt(AstNode node) {
        requireSymbol(node, "MatchedStmt");
        List<AstNode> kids = children(node);

        if (kids.size() == 1) {
            AstNode only = kids.get(0);
            return switch (only.getSymbolName()) {
                case "AssignStmt" -> normalizeAssignStmt(only);
                case "ExprStmt" -> normalizeExprStmt(only);
                case "ReturnStmt" -> normalizeReturnStmt(only);
                case "Block" -> normalizeBlock(only);
                default -> throw new IllegalStateException("Unknown matched stmt");
            };
        }

        if ("WHILE".equals(kids.get(0).getSymbolName())) {
            AstNode cond = expect(kids.get(2), "Cond");
            AstNode body = expect(kids.get(4), "MatchedStmt");
            return CoreAstNode.node(AstKind.WHILE_STMT, List.of(normalizeCond(cond), normalizeMatchedStmt(body)));
        }

        if ("IF".equals(kids.get(0).getSymbolName())) {
            AstNode cond = expect(kids.get(2), "Cond");
            AstNode thenBranch = expect(kids.get(4), "MatchedStmt");
            AstNode elseBranch = expect(kids.get(6), "MatchedStmt");
            return CoreAstNode.node(AstKind.IF_STMT, List.of(normalizeCond(cond), normalizeMatchedStmt(thenBranch), normalizeMatchedStmt(elseBranch)));
        }

        throw new IllegalStateException("Illegal MatchedStmt");
    }

    private CoreAstNode normalizeUnmatchedStmt(AstNode node) {
        requireSymbol(node, "UnmatchedStmt");
        List<AstNode> kids = children(node);

        if (kids.size() == 5 && "IF".equals(kids.get(0).getSymbolName())) {
            AstNode cond = expect(kids.get(2), "Cond");
            AstNode thenBranch = expect(kids.get(4), "Stmt");
            return CoreAstNode.node(AstKind.IF_STMT, List.of(normalizeCond(cond), normalizeStmt(thenBranch)));
        }

        if (kids.size() == 7 && "IF".equals(kids.get(0).getSymbolName())) {
            AstNode cond = expect(kids.get(2), "Cond");
            AstNode thenBranch = expect(kids.get(4), "MatchedStmt");
            AstNode elseBranch = expect(kids.get(6), "UnmatchedStmt");
            return CoreAstNode.node(AstKind.IF_STMT, List.of(normalizeCond(cond), normalizeMatchedStmt(thenBranch), normalizeUnmatchedStmt(elseBranch)));
        }

        if ("WHILE".equals(kids.get(0).getSymbolName())) {
            AstNode cond = expect(kids.get(2), "Cond");
            AstNode body = expect(kids.get(4), "UnmatchedStmt");
            return CoreAstNode.node(AstKind.WHILE_STMT, List.of(normalizeCond(cond), normalizeUnmatchedStmt(body)));
        }

        throw new IllegalStateException("Illegal UnmatchedStmt");
    }

    private CoreAstNode normalizeAssignStmt(AstNode node) {
        requireSymbol(node, "AssignStmt");
        AstNode idNode = child(node, 0, "ID");
        AstNode expr = child(node, 2, "Expr");
        return CoreAstNode.node(AstKind.ASSIGNMENT, List.of(identifierNode(idNode), normalizeExpr(expr)));
    }

    private CoreAstNode normalizeExprStmt(AstNode node) {
        requireSymbol(node, "ExprStmt");
        AstNode callExpr = child(node, 0, "CallExpr");
        return CoreAstNode.node(AstKind.EXPRESSION_STMT, List.of(normalizeCallExpr(callExpr)));
    }

    private CoreAstNode normalizeReturnStmt(AstNode node) {
        requireSymbol(node, "ReturnStmt");
        AstNode expr = child(node, 1, "Expr");
        return CoreAstNode.node(AstKind.RETURN_STMT, List.of(normalizeExpr(expr)));
    }

    private CoreAstNode normalizeCond(AstNode node) {
        requireSymbol(node, "Cond");
        AstNode left = child(node, 0, "Expr");
        AstNode relOp = child(node, 1, "RelOp");
        AstNode right = child(node, 2, "Expr");
        return CoreAstNode.node(AstKind.BINARY_EXPR, relOperatorText(relOp), List.of(normalizeExpr(left), normalizeExpr(right)));
    }

    private String relOperatorText(AstNode relOpNode) {
        requireSymbol(relOpNode, "RelOp");
        AstNode terminal = childBySymbol(relOpNode, "LT", "GT", "LE", "GE", "EQ", "NE");
        return terminal.getLexeme();
    }

    private CoreAstNode normalizeExpr(AstNode node) {
        requireSymbol(node, "Expr");
        List<AstNode> kids = children(node);

        if (kids.size() == 1) {
            return normalizeTerm(expect(kids.get(0), "Term"));
        }

        AstNode left = expect(kids.get(0), "Expr");
        AstNode op = kids.get(1);
        AstNode right = expect(kids.get(2), "Term");
        return CoreAstNode.node(AstKind.BINARY_EXPR, op.getLexeme(), List.of(normalizeExpr(left), normalizeTerm(right)));
    }

    private CoreAstNode normalizeTerm(AstNode node) {
        requireSymbol(node, "Term");
        List<AstNode> kids = children(node);

        if (kids.size() == 1) {
            return normalizeFactor(expect(kids.get(0), "Factor"));
        }

        AstNode left = expect(kids.get(0), "Term");
        AstNode op = kids.get(1);
        AstNode right = expect(kids.get(2), "Factor");
        return CoreAstNode.node(AstKind.BINARY_EXPR, op.getLexeme(), List.of(normalizeTerm(left), normalizeFactor(right)));
    }

    private CoreAstNode normalizeFactor(AstNode node) {
        requireSymbol(node, "Factor");
        List<AstNode> kids = children(node);

        if (kids.size() == 1) {
            AstNode only = kids.get(0);
            return switch (only.getSymbolName()) {
                case "ID", "MAIN" -> identifierNode(only);
                case "NUM" -> intLiteralNode(only);
                case "CallExpr" -> normalizeCallExpr(only);
                default -> throw new IllegalStateException("Unknown Factor child: " + only.getSymbolName());
            };
        }

        AstNode expr = expect(kids.get(1), "Expr");
        return normalizeExpr(expr);
    }

    private CoreAstNode normalizeCallExpr(AstNode node) {
        requireSymbol(node, "CallExpr");
        String functionName = normalizeFuncName(child(node, 0, "FuncName"));
        AstNode argListOpt = child(node, 2, "ArgListOpt");

        List<CoreAstNode> args = new ArrayList<>();
        collectArgListOpt(argListOpt, args);
        return CoreAstNode.node(AstKind.FUNCTION_CALL, functionName, args);
    }

    private void collectArgListOpt(AstNode node, List<CoreAstNode> out) {
        requireSymbol(node, "ArgListOpt");
        List<AstNode> kids = children(node);
        if (kids.isEmpty()) {
            return;
        }
        collectArgList(expect(kids.get(0), "ArgList"), out);
    }

    private void collectArgList(AstNode node, List<CoreAstNode> out) {
        requireSymbol(node, "ArgList");
        List<AstNode> kids = children(node);

        if (kids.size() == 1) {
            out.add(normalizeExpr(expect(kids.get(0), "Expr")));
            return;
        }
        if (kids.size() == 3) {
            collectArgList(expect(kids.get(0), "ArgList"), out);
            out.add(normalizeExpr(expect(kids.get(2), "Expr")));
            return;
        }
        throw new IllegalStateException("Illegal ArgList structure");
    }

    private CoreAstNode identifierNode(AstNode idNode) {
        return CoreAstNode.leaf(AstKind.IDENTIFIER, idNode.getLexeme());
    }

    private CoreAstNode intLiteralNode(AstNode numNode) {
        requireSymbol(numNode, "NUM");
        return CoreAstNode.leaf(AstKind.INT_LITERAL, numNode.getLexeme());
    }

    private List<AstNode> children(AstNode node) {
        return node.getChildren().stream()
                .filter(child -> !child.isSemanticActionNode())
                .collect(Collectors.toList());
    }

    private AstNode child(AstNode node, int index, String expected) {
        AstNode child = children(node).get(index);
        requireSymbol(child, expected);
        return child;
    }

    private AstNode childBySymbol(AstNode node, String... expectedSymbols) {
        List<AstNode> kids = children(node);
        for (AstNode child : kids) {
            for (String expected : expectedSymbols) {
                if (expected.equals(child.getSymbolName())) {
                    return child;
                }
            }
        }
        throw new IllegalStateException("Expected one of " + String.join(", ", expectedSymbols));
    }

    private AstNode expect(AstNode node, String expected) {
        requireSymbol(node, expected);
        return node;
    }

    private void requireSymbol(AstNode node, String expected) {
        if (!expected.equals(node.getSymbolName())) {
            throw new IllegalStateException("Expected symbol " + expected + " but got " + node.getSymbolName());
        }
    }
}