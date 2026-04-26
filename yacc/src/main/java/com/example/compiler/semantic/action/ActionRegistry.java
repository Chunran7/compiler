package com.example.compiler.semantic.action;

import com.example.compiler.yacc.ast.AstKind;
import com.example.compiler.yacc.ast.CoreAstNode;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class ActionRegistry {
    public record ActionResult(String functionName, List<Object> arguments) {
    }

    private enum SpecialValue {
        NO_INITIALIZER
    }

    private final Map<String, ActionFunction> functions = new LinkedHashMap<>();

    public void register(String name, ActionFunction function) {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(function, "function");
        functions.put(name, function);
    }

    public boolean contains(String name) {
        return functions.containsKey(name);
    }

    public ActionFunction getRequired(String name) {
        ActionFunction function = functions.get(name);
        if (function == null) {
            throw new IllegalStateException("Unregistered action function: " + name);
        }
        return function;
    }

    public Object invoke(String name, List<Object> arguments) {
        return getRequired(name).apply(arguments);
    }

    public static ActionRegistry defaultRegistry() {
        ActionRegistry registry = new ActionRegistry();

        // Program / function structure
        registry.register("makeProgram", args ->
                CoreAstNode.node(AstKind.PROGRAM, asNodeList(args.get(0), "makeProgram.arg0"))
        );

        registry.register("appendFunction", args -> {
            List<CoreAstNode> list = copyNodeList(args.get(0), "appendFunction.arg0");
            list.add(asNode(args.get(1), "appendFunction.arg1"));
            return list;
        });

        registry.register("makeFunctionList", args -> {
            List<CoreAstNode> list = new ArrayList<>();
            list.add(asNode(args.get(0), "makeFunctionList.arg0"));
            return list;
        });

        registry.register("makeFunction", args -> {
            String functionName = asText(args.get(0), "makeFunction.arg0");
            List<CoreAstNode> params = copyNodeList(args.get(1), "makeFunction.arg1");
            CoreAstNode block = asNode(args.get(2), "makeFunction.arg2");

            List<CoreAstNode> children = new ArrayList<>(params);
            children.add(block);

            if ("main".equals(functionName)) {
                return CoreAstNode.node(AstKind.MAIN_FUNCTION, functionName, children);
            }
            return CoreAstNode.node(AstKind.FUNCTION_DEF, functionName, children);
        });

        // Parameters
        registry.register("makeEmptyParamList", args -> new ArrayList<CoreAstNode>());

        registry.register("appendParam", args -> {
            List<CoreAstNode> list = copyNodeList(args.get(0), "appendParam.arg0");
            list.add(asNode(args.get(1), "appendParam.arg1"));
            return list;
        });

        registry.register("makeParamList", args -> {
            List<CoreAstNode> list = new ArrayList<>();
            list.add(asNode(args.get(0), "makeParamList.arg0"));
            return list;
        });

        registry.register("makeParam", args ->
                CoreAstNode.leaf(AstKind.PARAMETER, asText(args.get(0), "makeParam.arg0"))
        );

        // Block / items
        registry.register("makeBlock", args ->
                CoreAstNode.node(AstKind.BLOCK, asNodeList(args.get(0), "makeBlock.arg0"))
        );

        registry.register("makeEmptyItemList", args -> new ArrayList<CoreAstNode>());

        registry.register("appendItem", args -> {
            List<CoreAstNode> list = copyNodeList(args.get(0), "appendItem.arg0");
            list.add(asNode(args.get(1), "appendItem.arg1"));
            return list;
        });

        // Declarations
        registry.register("makeDeclaration", args -> {
            String name = asText(args.get(0), "makeDeclaration.arg0");
            Object init = args.get(1);

            List<CoreAstNode> children = new ArrayList<>();
            children.add(CoreAstNode.leaf(AstKind.IDENTIFIER, name));

            if (init != SpecialValue.NO_INITIALIZER) {
                children.add(asNode(init, "makeDeclaration.arg1"));
            }

            return CoreAstNode.node(AstKind.DECLARATION, children);
        });

        registry.register("makeNoInitializer", args -> SpecialValue.NO_INITIALIZER);

        registry.register("makeInitializer", args -> asNode(args.get(0), "makeInitializer.arg0"));

        // Statements
        registry.register("makeWhile", args ->
                CoreAstNode.node(
                        AstKind.WHILE_STMT,
                        List.of(
                                asNode(args.get(0), "makeWhile.arg0"),
                                asNode(args.get(1), "makeWhile.arg1")
                        )
                )
        );

        registry.register("makeIfElse", args ->
                CoreAstNode.node(
                        AstKind.IF_STMT,
                        List.of(
                                asNode(args.get(0), "makeIfElse.arg0"),
                                asNode(args.get(1), "makeIfElse.arg1"),
                                asNode(args.get(2), "makeIfElse.arg2")
                        )
                )
        );

        registry.register("makeIf", args ->
                CoreAstNode.node(
                        AstKind.IF_STMT,
                        List.of(
                                asNode(args.get(0), "makeIf.arg0"),
                                asNode(args.get(1), "makeIf.arg1")
                        )
                )
        );

        registry.register("makeAssignment", args ->
                CoreAstNode.node(
                        AstKind.ASSIGNMENT,
                        List.of(
                                CoreAstNode.leaf(AstKind.IDENTIFIER, asText(args.get(0), "makeAssignment.arg0")),
                                asNode(args.get(1), "makeAssignment.arg1")
                        )
                )
        );

        registry.register("makeExprStmt", args ->
                CoreAstNode.node(
                        AstKind.EXPRESSION_STMT,
                        List.of(asNode(args.get(0), "makeExprStmt.arg0"))
                )
        );

        registry.register("makeReturn", args ->
                CoreAstNode.node(
                        AstKind.RETURN_STMT,
                        List.of(asNode(args.get(0), "makeReturn.arg0"))
                )
        );

        // Conditions / expressions
        registry.register("makeCondition", args ->
                CoreAstNode.node(
                        AstKind.BINARY_EXPR,
                        asText(args.get(1), "makeCondition.arg1"),
                        List.of(
                                asNode(args.get(0), "makeCondition.arg0"),
                                asNode(args.get(2), "makeCondition.arg2")
                        )
                )
        );

        registry.register("makeRelOp", args -> asText(args.get(0), "makeRelOp.arg0"));

        registry.register("makeBinary", args ->
                CoreAstNode.node(
                        AstKind.BINARY_EXPR,
                        asText(args.get(0), "makeBinary.arg0"),
                        List.of(
                                asNode(args.get(1), "makeBinary.arg1"),
                                asNode(args.get(2), "makeBinary.arg2")
                        )
                )
        );

        registry.register("makeIdentifier", args ->
                CoreAstNode.leaf(AstKind.IDENTIFIER, asText(args.get(0), "makeIdentifier.arg0"))
        );

        registry.register("makeIntLiteral", args ->
                CoreAstNode.leaf(AstKind.INT_LITERAL, asText(args.get(0), "makeIntLiteral.arg0"))
        );

        // Calls / arguments
        registry.register("makeCall", args ->
                CoreAstNode.node(
                        AstKind.FUNCTION_CALL,
                        asText(args.get(0), "makeCall.arg0"),
                        asNodeList(args.get(1), "makeCall.arg1")
                )
        );

        registry.register("makeEmptyArgList", args -> new ArrayList<CoreAstNode>());

        registry.register("appendArg", args -> {
            List<CoreAstNode> list = copyNodeList(args.get(0), "appendArg.arg0");
            list.add(asNode(args.get(1), "appendArg.arg1"));
            return list;
        });

        registry.register("makeArgList", args -> {
            List<CoreAstNode> list = new ArrayList<>();
            list.add(asNode(args.get(0), "makeArgList.arg0"));
            return list;
        });

        return registry;
    }

    private static CoreAstNode asNode(Object value, String label) {
        if (value instanceof CoreAstNode node) {
            return node;
        }
        throw new IllegalStateException(label + " should be CoreAstNode, but got: " + value);
    }

    @SuppressWarnings("unchecked")
    private static List<CoreAstNode> asNodeList(Object value, String label) {
        if (value instanceof List<?> list) {
            for (Object item : list) {
                if (!(item instanceof CoreAstNode)) {
                    throw new IllegalStateException(label + " should be List<CoreAstNode>, but contains: " + item);
                }
            }
            return (List<CoreAstNode>) list;
        }
        throw new IllegalStateException(label + " should be List<CoreAstNode>, but got: " + value);
    }

    private static List<CoreAstNode> copyNodeList(Object value, String label) {
        return new ArrayList<>(asNodeList(value, label));
    }

    private static String asText(Object value, String label) {
        if (value instanceof String text) {
            return text;
        }
        throw new IllegalStateException(label + " should be String, but got: " + value);
    }
}