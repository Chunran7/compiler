package com.example.compiler.semantic.action;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public final class ActionInvocation {
    private final String functionName;
    private final List<ActionArgument> arguments;

    public ActionInvocation(String functionName, List<ActionArgument> arguments) {
        this.functionName = Objects.requireNonNull(functionName, "functionName");
        this.arguments = new ArrayList<>(Objects.requireNonNull(arguments, "arguments"));
    }

    public String getFunctionName() {
        return functionName;
    }

    public List<ActionArgument> getArguments() {
        return Collections.unmodifiableList(arguments);
    }

    @Override
    public String toString() {
        return functionName + "(" + arguments + ")";
    }
}