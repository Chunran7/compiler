package com.example.compiler.semantic;

import com.example.compiler.semantic.action.ActionArgument;
import com.example.compiler.semantic.action.ActionInvocation;
import com.example.compiler.semantic.action.ActionPattern;
import com.example.compiler.semantic.action.ActionPatternParser;
import com.example.compiler.semantic.action.ActionRegistry;
import com.example.compiler.yacc.ast.AstNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class TranslationSchemeExecutor {
    private final ActionPatternParser patternParser;
    private final ActionRegistry registry;

    public TranslationSchemeExecutor() {
        this(new ActionPatternParser(), ActionRegistry.defaultRegistry());
    }

    public TranslationSchemeExecutor(ActionPatternParser patternParser, ActionRegistry registry) {
        this.patternParser = Objects.requireNonNull(patternParser, "patternParser");
        this.registry = Objects.requireNonNull(registry, "registry");
    }

    public void execute(AstNode root) {
        Objects.requireNonNull(root, "root");
        clearSemanticValues(root);
        walk(root);
    }

    private void clearSemanticValues(AstNode node) {
        node.clearSemanticValue();
        for (AstNode child : node.getChildren()) {
            clearSemanticValues(child);
        }
    }

    private void walk(AstNode node) {
        // 关键修复：先处理语义动作节点，再处理普通叶子
        if (node.isSemanticActionNode()) {
            executeActionNode(node);
            return;
        }

        if (node.isLeaf()) {
            if (node.getLexeme() != null) {
                node.setSemanticValue(node.getLexeme());
            } else {
                node.setSemanticValue(node.getSymbolName());
            }
            return;
        }

        for (AstNode child : node.getChildren()) {
            walk(child);
        }

        if (node.getSemanticValue() == null) {
            List<AstNode> nonActionChildren = nonActionChildren(node);
            if (nonActionChildren.size() == 1) {
                node.setSemanticValue(nonActionChildren.get(0).getSemanticValue());
            }
        }
    }

    private void executeActionNode(AstNode actionNode) {
        if (!actionNode.isSemanticActionNode()) {
            throw new IllegalStateException("executeActionNode requires a semantic action node");
        }
        if (actionNode.getActionCode() == null || actionNode.getActionCode().isBlank()) {
            throw new IllegalStateException("Semantic action node has empty action code");
        }
        if (!actionNode.hasParent()) {
            throw new IllegalStateException("Semantic action node must have a parent");
        }

        ActionPattern pattern = patternParser.parse(actionNode.getActionCode());
        Object result;

        if (pattern.isDirectReferenceAssign()) {
            result = resolvePositionalReference(actionNode, pattern.getDirectReferenceIndex());
        } else if (pattern.isFunctionCallAssign()) {
            result = executeInvocation(actionNode, pattern.getInvocation());
        } else {
            throw new IllegalStateException("Unsupported action pattern: " + pattern);
        }

        actionNode.setSemanticValue(result);
        actionNode.getParent().setSemanticValue(result);
    }

    private Object executeInvocation(AstNode actionNode, ActionInvocation invocation) {
        List<Object> resolvedArguments = new ArrayList<>();
        for (ActionArgument argument : invocation.getArguments()) {
            resolvedArguments.add(resolveArgument(actionNode, argument));
        }
        return registry.invoke(invocation.getFunctionName(), resolvedArguments);
    }

    private Object resolveArgument(AstNode actionNode, ActionArgument argument) {
        return switch (argument.getKind()) {
            case POSITIONAL_REF -> resolvePositionalReference(actionNode, argument.getRefIndex());
            case STRING_LITERAL -> argument.getText();
            case RAW_LITERAL -> argument.getText();
        };
    }

    private Object resolvePositionalReference(AstNode actionNode, int refIndex) {
        if (!actionNode.hasParent()) {
            throw new IllegalStateException("Cannot resolve $" + refIndex + " without parent node");
        }

        List<AstNode> semanticSiblings = nonActionChildren(actionNode.getParent());
        if (refIndex <= 0 || refIndex > semanticSiblings.size()) {
            throw new IllegalStateException(
                    "Reference $" + refIndex + " out of range for parent " + actionNode.getParent().getSymbolName()
                            + " with " + semanticSiblings.size() + " semantic children"
            );
        }

        AstNode referenced = semanticSiblings.get(refIndex - 1);
        return referenced.getSemanticValue();
    }

    private List<AstNode> nonActionChildren(AstNode node) {
        List<AstNode> result = new ArrayList<>();
        for (AstNode child : node.getChildren()) {
            if (!child.isSemanticActionNode()) {
                result.add(child);
            }
        }
        return result;
    }
}