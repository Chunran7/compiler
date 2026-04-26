package com.example.compiler.semantic.action;

import java.util.List;

@FunctionalInterface
public interface ActionFunction {
    Object apply(List<Object> arguments);
}