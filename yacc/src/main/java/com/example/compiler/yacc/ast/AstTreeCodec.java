package com.example.compiler.yacc.ast;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public final class AstTreeCodec {
    public AstNode read(Path path) throws IOException {
        List<String> lines = Files.readAllLines(path);
        Iterator<String> iterator = lines.iterator();
        if (!iterator.hasNext()) {
            throw new IllegalStateException("Serialized AST is empty: " + path);
        }
        return readNode(iterator);
    }

    private AstNode readNode(Iterator<String> iterator) {
        if (!iterator.hasNext()) {
            throw new IllegalStateException("Unexpected end of serialized AST");
        }
        String line = iterator.next();
        String[] parts = split(line, 7);
        if (parts.length != 7 || !"NODE".equals(parts[0])) {
            throw new IllegalStateException("Invalid serialized AST line: " + line);
        }

        String symbol = unescape(parts[1]);
        String lexeme = emptyToNull(unescape(parts[2]));
        boolean semanticAction = "1".equals(parts[3]);
        String actionCode = emptyToNull(unescape(parts[4]));
        int productionId = Integer.parseInt(parts[5]);
        int childCount = Integer.parseInt(parts[6]);

        if (semanticAction) {
            return AstNode.semanticAction(symbol, actionCode, productionId);
        }
        if (childCount == 0 && lexeme != null) {
            return AstNode.leaf(symbol, lexeme);
        }

        List<AstNode> children = new ArrayList<>();
        for (int i = 0; i < childCount; i++) {
            children.add(readNode(iterator));
        }
        return AstNode.nonTerminal(symbol, children, productionId);
    }

    private static String[] split(String line, int expectedParts) {
        List<String> parts = new ArrayList<>(expectedParts);
        StringBuilder current = new StringBuilder();
        boolean escaped = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (escaped) {
                current.append('\\').append(c);
                escaped = false;
                continue;
            }
            if (c == '\\') {
                escaped = true;
                continue;
            }
            if (c == '\t') {
                parts.add(current.toString());
                current.setLength(0);
                continue;
            }
            current.append(c);
        }
        parts.add(current.toString());
        return parts.toArray(String[]::new);
    }

    private static String unescape(String text) {
        StringBuilder out = new StringBuilder();
        boolean escaped = false;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (!escaped) {
                if (c == '\\') {
                    escaped = true;
                } else {
                    out.append(c);
                }
                continue;
            }
            switch (c) {
                case 'n' -> out.append('\n');
                case 'r' -> out.append('\r');
                case 't' -> out.append('\t');
                case '\\' -> out.append('\\');
                default -> out.append(c);
            }
            escaped = false;
        }
        if (escaped) {
            out.append('\\');
        }
        return out.toString();
    }

    private static String emptyToNull(String text) {
        return text == null || text.isEmpty() ? null : text;
    }
}
