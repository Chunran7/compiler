package com.example.compiler.yacc.parser;

import com.example.compiler.yacc.grammar.Associativity;
import com.example.compiler.yacc.grammar.Grammar;
import com.example.compiler.yacc.grammar.NonTerminal;
import com.example.compiler.yacc.grammar.Symbol;
import com.example.compiler.yacc.grammar.Terminal;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class YaccParser {
    private YaccParser() {
    }

    public static Grammar parse(Reader reader) throws IOException {
        BufferedReader br = new BufferedReader(reader);
        Grammar grammar = new Grammar();

        Set<String> tokenNames = new LinkedHashSet<>();
        String startSymbolName = null;
        StringBuilder rulesText = new StringBuilder();
        int precedenceLevel = 0;
        int syntheticActionCounter = 0;

        boolean inRules = false;
        String rawLine;
        while ((rawLine = br.readLine()) != null) {
            String line = rawLine.trim();

            if (!inRules) {
                if (line.isEmpty() || line.startsWith("//") || line.startsWith("#")) {
                    continue;
                }

                if (line.equals("%%")) {
                    inRules = true;
                    continue;
                }

                if (line.startsWith("%token")) {
                    String rest = line.substring("%token".length()).trim();
                    if (!rest.isEmpty()) {
                        for (String tok : rest.split("\\s+")) {
                            if (!tok.isEmpty()) {
                                tokenNames.add(tok);
                            }
                        }
                    }
                } else if (line.startsWith("%start")) {
                    String rest = line.substring("%start".length()).trim();
                    if (!rest.isEmpty()) {
                        startSymbolName = rest.split("\\s+")[0];
                    }
                } else if (line.startsWith("%left") || line.startsWith("%right") || line.startsWith("%nonassoc")) {
                    Associativity associativity;
                    String rest;

                    if (line.startsWith("%left")) {
                        associativity = Associativity.LEFT;
                        rest = line.substring("%left".length()).trim();
                    } else if (line.startsWith("%right")) {
                        associativity = Associativity.RIGHT;
                        rest = line.substring("%right".length()).trim();
                    } else {
                        associativity = Associativity.NONASSOC;
                        rest = line.substring("%nonassoc".length()).trim();
                    }

                    precedenceLevel++;
                    if (!rest.isEmpty()) {
                        for (String tok : rest.split("\\s+")) {
                            if (!tok.isEmpty()) {
                                tokenNames.add(tok);
                                grammar.definePrecedence(tok, precedenceLevel, associativity);
                            }
                        }
                    }
                }

                continue;
            }

            if (line.equals("%%")) {
                break;
            }

            rulesText.append(rawLine).append('\n');
        }

        for (String tok : tokenNames) {
            grammar.terminal(tok);
        }
        Terminal eof = grammar.terminal("EOF");
        grammar.setEof(eof);

        List<String> ruleBlocks = splitTopLevel(rulesText.toString(), ';');
        String actualStartName = resolveStartSymbolName(startSymbolName, ruleBlocks);

        if (actualStartName != null) {
            NonTerminal startNt = grammar.nonTerminal(actualStartName);
            grammar.setStartSymbol(startNt);

            String baseAugName = startNt.getName() + "'";
            int suffix = 0;
            String candidate = baseAugName;
            while (grammar.getNonTerminal(candidate) != null) {
                suffix++;
                candidate = baseAugName + suffix;
            }

            NonTerminal augmented = grammar.nonTerminal(candidate);
            grammar.setAugmentedStartSymbol(augmented);
            grammar.addProduction(augmented, startNt);
        }

        for (String ruleBlock : ruleBlocks) {
            if (ruleBlock == null || ruleBlock.isBlank()) {
                continue;
            }

            int idxColon = indexOfTopLevel(ruleBlock, ':');
            if (idxColon < 0) {
                continue;
            }

            String lhsName = ruleBlock.substring(0, idxColon).trim();
            if (lhsName.isEmpty()) {
                continue;
            }

            NonTerminal lhs = grammar.nonTerminal(lhsName);
            String rhsPart = ruleBlock.substring(idxColon + 1).trim();

            List<String> alternatives = splitTopLevel(rhsPart, '|');
            if (alternatives.isEmpty()) {
                grammar.addEpsilonProduction(lhs);
                continue;
            }

            for (String alternative : alternatives) {
                ParsedAlternative parsed = parseAlternative(alternative);

                List<Symbol> rhsSymbols = new ArrayList<>();

                if (!parsed.symbolsText().isEmpty() && !"ε".equals(parsed.symbolsText())) {
                    String[] symNames = parsed.symbolsText().split("\\s+");
                    for (String sym : symNames) {
                        if (sym.isEmpty()) {
                            continue;
                        }
                        Symbol symbol = tokenNames.contains(sym)
                                ? grammar.terminal(sym)
                                : grammar.nonTerminal(sym);
                        rhsSymbols.add(symbol);
                    }
                }

                if (parsed.actionCode() != null && !parsed.actionCode().isBlank()) {
                    String actionNtName = "__ACT_" + (++syntheticActionCounter);
                    NonTerminal actionNt = grammar.nonTerminal(actionNtName);
                    grammar.addEpsilonProduction(actionNt, parsed.actionCode());
                    rhsSymbols.add(actionNt);
                }

                if (rhsSymbols.isEmpty()) {
                    grammar.addEpsilonProduction(lhs, parsed.actionCode(), parsed.explicitPrecedenceToken());
                } else {
                    grammar.addProduction(lhs, parsed.actionCode(), parsed.explicitPrecedenceToken(), rhsSymbols.toArray(new Symbol[0]));
                }
            }
        }

        if (grammar.getStartSymbol() == null && !grammar.getNonTerminals().isEmpty()) {
            grammar.setStartSymbol(grammar.getNonTerminals().iterator().next());
        }

        if (grammar.getAugmentedStartSymbol() == null && grammar.getStartSymbol() != null) {
            NonTerminal start = grammar.getStartSymbol();
            String augName = start.getName() + "'";
            int suffix = 0;
            String candidate = augName;
            while (grammar.getNonTerminal(candidate) != null) {
                suffix++;
                candidate = augName + suffix;
            }
            NonTerminal augmented = grammar.nonTerminal(candidate);
            grammar.setAugmentedStartSymbol(augmented);
            grammar.addProduction(augmented, start);
        }

        return grammar;
    }

    private static String resolveStartSymbolName(String declaredStart, List<String> ruleBlocks) {
        if (declaredStart != null && !declaredStart.isEmpty()) {
            return declaredStart;
        }
        for (String ruleBlock : ruleBlocks) {
            if (ruleBlock == null || ruleBlock.isBlank()) {
                continue;
            }
            int idxColon = indexOfTopLevel(ruleBlock, ':');
            if (idxColon > 0) {
                String candidateName = ruleBlock.substring(0, idxColon).trim();
                if (!candidateName.isEmpty()) {
                    return candidateName;
                }
            }
        }
        return null;
    }

    private static List<String> splitTopLevel(String text, char delimiter) {
        List<String> parts = new ArrayList<>();
        if (text == null || text.isEmpty()) {
            return parts;
        }

        StringBuilder current = new StringBuilder();
        int braceDepth = 0;
        boolean inLineComment = false;
        boolean inBlockComment = false;
        boolean inSingleQuote = false;
        boolean inDoubleQuote = false;
        boolean escaped = false;

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            char next = i + 1 < text.length() ? text.charAt(i + 1) : '\0';

            if (inLineComment) {
                current.append(c);
                if (c == '\n') {
                    inLineComment = false;
                }
                continue;
            }
            if (inBlockComment) {
                current.append(c);
                if (c == '*' && next == '/') {
                    current.append(next);
                    i++;
                    inBlockComment = false;
                }
                continue;
            }
            if (inSingleQuote) {
                current.append(c);
                if (escaped) {
                    escaped = false;
                } else if (c == '\\') {
                    escaped = true;
                } else if (c == '\'') {
                    inSingleQuote = false;
                }
                continue;
            }
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
            if (c == '/' && next == '/') {
                current.append(c).append(next);
                i++;
                inLineComment = true;
                continue;
            }
            if (c == '/' && next == '*') {
                current.append(c).append(next);
                i++;
                inBlockComment = true;
                continue;
            }
            if (c == '\'') {
                current.append(c);
                inSingleQuote = true;
                continue;
            }
            if (c == '"') {
                current.append(c);
                inDoubleQuote = true;
                continue;
            }
            if (c == '{') {
                braceDepth++;
            } else if (c == '}' && braceDepth > 0) {
                braceDepth--;
            }

            if (c == delimiter && braceDepth == 0) {
                String part = current.toString().trim();
                if (!part.isEmpty()) {
                    parts.add(part);
                }
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

    private static int indexOfTopLevel(String text, char target) {
        if (text == null || text.isEmpty()) {
            return -1;
        }

        int braceDepth = 0;
        boolean inLineComment = false;
        boolean inBlockComment = false;
        boolean inSingleQuote = false;
        boolean inDoubleQuote = false;
        boolean escaped = false;

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            char next = i + 1 < text.length() ? text.charAt(i + 1) : '\0';

            if (inLineComment) {
                if (c == '\n') {
                    inLineComment = false;
                }
                continue;
            }
            if (inBlockComment) {
                if (c == '*' && next == '/') {
                    i++;
                    inBlockComment = false;
                }
                continue;
            }
            if (inSingleQuote) {
                if (escaped) {
                    escaped = false;
                } else if (c == '\\') {
                    escaped = true;
                } else if (c == '\'') {
                    inSingleQuote = false;
                }
                continue;
            }
            if (inDoubleQuote) {
                if (escaped) {
                    escaped = false;
                } else if (c == '\\') {
                    escaped = true;
                } else if (c == '"') {
                    inDoubleQuote = false;
                }
                continue;
            }
            if (c == '/' && next == '/') {
                i++;
                inLineComment = true;
                continue;
            }
            if (c == '/' && next == '*') {
                i++;
                inBlockComment = true;
                continue;
            }
            if (c == '\'') {
                inSingleQuote = true;
                continue;
            }
            if (c == '"') {
                inDoubleQuote = true;
                continue;
            }
            if (c == '{') {
                braceDepth++;
                continue;
            }
            if (c == '}') {
                if (braceDepth > 0) {
                    braceDepth--;
                }
                continue;
            }
            if (c == target && braceDepth == 0) {
                return i;
            }
        }

        return -1;
    }

    private static ParsedAlternative parseAlternative(String text) {
        StringBuilder symbols = new StringBuilder();
        StringBuilder currentAction = new StringBuilder();
        List<String> actions = new ArrayList<>();

        int actionDepth = 0;
        boolean inLineComment = false;
        boolean inBlockComment = false;
        boolean inSingleQuote = false;
        boolean inDoubleQuote = false;
        boolean escaped = false;

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            char next = i + 1 < text.length() ? text.charAt(i + 1) : '\0';

            if (actionDepth > 0) {
                currentAction.append(c);

                if (inLineComment) {
                    if (c == '\n') {
                        inLineComment = false;
                    }
                    continue;
                }
                if (inBlockComment) {
                    if (c == '*' && next == '/') {
                        currentAction.append(next);
                        i++;
                        inBlockComment = false;
                    }
                    continue;
                }
                if (inSingleQuote) {
                    if (escaped) {
                        escaped = false;
                    } else if (c == '\\') {
                        escaped = true;
                    } else if (c == '\'') {
                        inSingleQuote = false;
                    }
                    continue;
                }
                if (inDoubleQuote) {
                    if (escaped) {
                        escaped = false;
                    } else if (c == '\\') {
                        escaped = true;
                    } else if (c == '"') {
                        inDoubleQuote = false;
                    }
                    continue;
                }
                if (c == '/' && next == '/') {
                    currentAction.append(next);
                    i++;
                    inLineComment = true;
                    continue;
                }
                if (c == '/' && next == '*') {
                    currentAction.append(next);
                    i++;
                    inBlockComment = true;
                    continue;
                }
                if (c == '\'') {
                    inSingleQuote = true;
                    continue;
                }
                if (c == '"') {
                    inDoubleQuote = true;
                    continue;
                }
                if (c == '{') {
                    actionDepth++;
                    continue;
                }
                if (c == '}') {
                    actionDepth--;
                    if (actionDepth == 0) {
                        String action = currentAction.toString().trim();
                        if (!action.isEmpty()) {
                            actions.add(action);
                        }
                        currentAction.setLength(0);
                    }
                }
                continue;
            }

            if (inLineComment) {
                if (c == '\n') {
                    inLineComment = false;
                    symbols.append(' ');
                }
                continue;
            }
            if (inBlockComment) {
                if (c == '*' && next == '/') {
                    i++;
                    inBlockComment = false;
                    symbols.append(' ');
                }
                continue;
            }
            if (c == '/' && next == '/') {
                i++;
                inLineComment = true;
                continue;
            }
            if (c == '/' && next == '*') {
                i++;
                inBlockComment = true;
                continue;
            }
            if (c == '{') {
                actionDepth = 1;
                currentAction.append(c);
                continue;
            }

            symbols.append(c);
        }

        String actionCode = actions.isEmpty() ? null : String.join("\n", actions).trim();
        String normalizedSymbols = normalizeSymbolsText(symbols.toString());

        String explicitPrecedenceToken = null;
        if (!normalizedSymbols.isEmpty()) {
            String[] parts = normalizedSymbols.split("\\s+");
            List<String> cleaned = new ArrayList<>();
            for (int i = 0; i < parts.length; i++) {
                if ("%prec".equals(parts[i])) {
                    if (i + 1 >= parts.length) {
                        throw new IllegalStateException("%prec must be followed by a terminal name");
                    }
                    explicitPrecedenceToken = parts[++i];
                    continue;
                }
                cleaned.add(parts[i]);
            }
            normalizedSymbols = String.join(" ", cleaned);
        }

        return new ParsedAlternative(normalizedSymbols, actionCode, explicitPrecedenceToken);
    }

    private static String normalizeSymbolsText(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }
        return text.trim().replaceAll("\\s+", " ");
    }

    private record ParsedAlternative(String symbolsText, String actionCode, String explicitPrecedenceToken) {
    }
}