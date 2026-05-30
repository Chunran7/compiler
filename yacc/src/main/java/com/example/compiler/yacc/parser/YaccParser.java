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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Yacc 规则文件解析器。
 *
 * <p>本类位于语法分析程序生成器的最前端：输入是 {@code resources/c99.y}
 * 这样的 yacc/bison 风格语法规则文件，输出是项目内部的 {@link Grammar}。
 * 它由 {@code SeuYaccGenerator} 调用，下游会继续计算 FIRST 集、构造
 * LR(1)/LALR 项目集并生成 ACTION/GOTO 分析表。</p>
 *
 * <p>报告中可把它对应为“语法规则 c99.y -> YACC 前端解析”模块。
 * 注意：这里解析的是语法规则文件，不是待编译的 C 源程序；C 源程序先由
 * Lex 变成 token 序列，再交给语法分析驱动。</p>
 */
public final class YaccParser {
    private YaccParser() {
    }

    /**
     * 将 yacc 文本解析为 Grammar。
     *
     * <p>关键步骤包括：
     * 1. 读取声明区的 {@code %token/%start/%left/%right/%nonassoc}；
     * 2. 将规则区按顶层分号和竖线拆成产生式；
     * 3. 把 yacc 字符字面量（如 {@code '('}）转成项目中统一的 token 名；
     * 4. 为开始符号添加增广产生式；
     * 5. 把规则中的语义动作块提取成 {@code __ACT_n -> ε} 形式的合成产生式。</p>
     *
     * @param reader c99.y 或测试语法文件的字符流
     * @return 可供 FIRST/LR/LALR/ParseTable 阶段使用的 Grammar
     * @throws IOException 读取语法规则失败时抛出
     */
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

        // c99.y 中大量使用 ';'、'(' 这样的字符终结符。项目内部表驱动统一使用
        // SEMI、LPAREN 等名字，因此先把这些隐式终结符加入 token 集。
        addImplicitCharTokens(tokenNames);
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
                    // yacc 中的 { ... } 动作并不是普通语法符号。这里把它改写为
                    // 一个空产生式动作节点，使 ParserDriver 规约时能把动作保留到
                    // parse tree/action tree 中，后续 TranslationSchemeExecutor 再执行。
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

    /**
     * 按顶层分隔符拆分 yacc 规则。
     *
     * <p>不能直接 {@code split(";")} 或 {@code split("|")}，因为语义动作块、
     * 字符串、字符字面量和注释内部也可能出现这些字符。该方法用简单状态机
     * 跟踪花括号深度和引号/注释状态，只在顶层切分。</p>
     */
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

    /**
     * 解析一个右部候选式。
     *
     * <p>返回值同时包含普通 RHS 符号、语义动作代码和 {@code %prec} 指定。
     * 语义动作本身先只作为文本保存，真正执行发生在语义阶段。</p>
     */
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
            if (c == '\'') {
                symbols.append(c);
                inSingleQuote = true;
                continue;
            }
            if (inSingleQuote) {
                symbols.append(c);
                if (c == '\'') {
                    inSingleQuote = false;
                }
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
        return convertCharLiterals(text.trim().replaceAll("\\s+", " "));
    }

    /**
     * Convert yacc character literals {@code 'c'} to named tokens, e.g. {@code '('} → {@code LPAREN}.
     * Also handles digraphs like {@code <%} → {@code LBRACE}.
     */
    private static String convertCharLiterals(String text) {
        // Replace digraphs
        text = text.replace("<%", "LBRACE");
        text = text.replace("%>", "RBRACE");
        text = text.replace("<:", "LBRACKET");
        text = text.replace(":>", "RBRACKET");

        // Replace character literals 'c' with named token
        Pattern charLit = Pattern.compile("'(.)'");
        Matcher m = charLit.matcher(text);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            String ch = m.group(1);
            String tokenName = charToTokenName(ch);
            m.appendReplacement(sb, Matcher.quoteReplacement(tokenName));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    private static String charToTokenName(String ch) {
        if (ch.length() != 1) return ch;
        return switch (ch.charAt(0)) {
            case ';' -> "SEMI";
            case '{' -> "LBRACE";
            case '}' -> "RBRACE";
            case ',' -> "COMMA";
            case ':' -> "COLON";
            case '=' -> "ASSIGN";
            case '(' -> "LPAREN";
            case ')' -> "RPAREN";
            case '[' -> "LBRACKET";
            case ']' -> "RBRACKET";
            case '.' -> "DOT";
            case '&' -> "AMPERSAND";
            case '!' -> "BANG";
            case '~' -> "TILDE";
            case '-' -> "MINUS";
            case '+' -> "PLUS";
            case '*' -> "STAR";
            case '/' -> "SLASH";
            case '%' -> "PERCENT";
            case '<' -> "LT";
            case '>' -> "GT";
            case '^' -> "CARET";
            case '|' -> "PIPE";
            case '?' -> "QUESTION";
            default -> ch;
        };
    }

    private record ParsedAlternative(String symbolsText, String actionCode, String explicitPrecedenceToken) {
    }

    /**
     * Adds named single-char tokens (SEMI, LPAREN, etc.) that appear as {@code 'c'} in
     * yacc grammar rules to the tokenNames set so they are treated as terminals.
     */
    private static void addImplicitCharTokens(Set<String> tokenNames) {
        for (String name : CHAR_TOKEN_NAMES) {
            tokenNames.add(name);
        }
    }

    private static final Set<String> CHAR_TOKEN_NAMES = Set.of(
        "SEMI", "LBRACE", "RBRACE", "COMMA", "COLON", "ASSIGN",
        "LPAREN", "RPAREN", "LBRACKET", "RBRACKET", "DOT",
        "AMPERSAND", "BANG", "TILDE", "MINUS", "PLUS", "STAR",
        "SLASH", "PERCENT", "LT", "GT", "CARET", "PIPE", "QUESTION"
    );
}
