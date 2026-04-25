package com.example.compiler.semantic.action;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class ActionPatternParser {
    public ActionPattern parse(String actionCode) {
        Objects.requireNonNull(actionCode, "actionCode");

        String normalized = normalize(actionCode);
        if (!normalized.startsWith("$$")) {
            throw new IllegalStateException("Only actions assigning to $$ are supported now: " + actionCode);
        }

        int eqIndex = normalized.indexOf('=');
        if (eqIndex < 0) {
            throw new IllegalStateException("Action must contain '=': " + actionCode);
        }

        String lhs = normalized.substring(0, eqIndex).trim();
        String rhs = normalized.substring(eqIndex + 1).trim();

        if (!"$$".equals(lhs)) {
            throw new IllegalStateException("Only '$$ = ...' actions are supported now: " + actionCode);
        }

        if (rhs.matches("\\$\\d+")) {
            int refIndex = Integer.parseInt(rhs.substring(1));
            return ActionPattern.directReferenceAssign(actionCode, refIndex);
        }

        int lp = rhs.indexOf('(');
        int rp = rhs.lastIndexOf(')');
        if (lp < 0 || rp < lp) {
            throw new IllegalStateException("Unsupported action RHS: " + actionCode);
        }

        String functionName = rhs.substring(0, lp).trim();
        String argsText = rhs.substring(lp + 1, rp).trim();
        List<ActionArgument> arguments = parseArguments(argsText);

        return ActionPattern.functionCallAssign(
                actionCode,
                new ActionInvocation(functionName, arguments)
        );
    }

    private String normalize(String code) {
        String result = code.trim();

        // 去掉最外层的 { ... }
        if (result.startsWith("{") && result.endsWith("}")) {
            result = result.substring(1, result.length() - 1).trim();
        }

        // 去掉末尾分号
        if (result.endsWith(";")) {
            result = result.substring(0, result.length() - 1).trim();
        }

        result = result.replace("\r", " ").replace("\n", " ").trim();
        result = result.replaceAll("\\s+", " ");

        // 某些情况下去掉花括号后还会残留多余空格，再做一次保护
        if (result.startsWith("{") && result.endsWith("}")) {
            result = result.substring(1, result.length() - 1).trim();
        }

        return result;
    }

    private List<ActionArgument> parseArguments(String argsText) {
        List<ActionArgument> result = new ArrayList<>();
        if (argsText.isBlank()) {
            return result;
        }

        List<String> pieces = splitArguments(argsText);
        for (String piece : pieces) {
            String token = piece.trim();
            if (token.matches("\\$\\d+")) {
                result.add(ActionArgument.positionalRef(Integer.parseInt(token.substring(1))));
            } else if (token.startsWith("\"") && token.endsWith("\"") && token.length() >= 2) {
                result.add(ActionArgument.stringLiteral(unescape(token.substring(1, token.length() - 1))));
            } else {
                result.add(ActionArgument.rawLiteral(token));
            }
        }
        return result;
    }

    private List<String> splitArguments(String text) {
        List<String> parts = new ArrayList<>();
        StringBuilder current = new StringBuilder();

        boolean inDoubleQuote = false;
        boolean escaped = false;

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);

            if (inDoubleQuote) {
                current.append(c);
                if (escaped) {
                    escaped = false;
                } else if (c == '\\') {
                    escaped = true;
                } else if (c == '"') {
                    inDoubleQuote = false;
                }
                continue;
            }

            if (c == '"') {
                current.append(c);
                inDoubleQuote = true;
                continue;
            }

            if (c == ',') {
                parts.add(current.toString().trim());
                current.setLength(0);
                continue;
            }

            current.append(c);
        }

        String tail = current.toString().trim();
        if (!tail.isEmpty()) {
            parts.add(tail);
        }

        return parts;
    }

    private String unescape(String text) {
        return text.replace("\\\"", "\"").replace("\\\\", "\\");
    }
}