package com.example.compiler.lex;

import java.util.*;

/**
 * SeuLex 正则表达式转换器
 * 功能：语法糖平铺 -> 显式插入连接符 -> 中缀转后缀
 */
public class RegexConverter {

    // 核心算符定义
    private static final char EPSILON = 'ε'; // 空串占位符
    private static final char CONCAT = '·'; // 显式连接符
    private static final char ALT = '|'; // 选择
    private static final char KLEENE = '*'; // 闭包

    /**
     * 0. 处理双引号：去掉引号，引号内字符为字面量，元字符加 \\ 转义
     */
    public String processQuotes(String regex) {
        StringBuilder result = new StringBuilder();
        int i = 0;
        while (i < regex.length()) {
            char c = regex.charAt(i);
            if (c == '"' && !isEscaped(regex, i)) {
                int end = regex.indexOf('"', i + 1);
                if (end == -1) {
                    result.append(c);
                    i++;
                } else {
                    for (int j = i + 1; j < end; j++) {
                        char inner = regex.charAt(j);
                        if (isRegexMeta(inner)) {
                            result.append('\\');
                        }
                        result.append(inner);
                    }
                    i = end + 1;
                }
            } else {
                result.append(c);
                i++;
            }
        }
        return result.toString();
    }

    private boolean isRegexMeta(char c) {
        return c == '*' || c == '+' || c == '?' || c == '|' || c == '(' || c == ')'
                || c == '[' || c == ']';
    }

    /**
     * 1. 处理字符集 [] -> (a|b|c)
     * [^...] 取反字符集 -> 展开为补集（0x01-0x7E 范围内不在集合中的字符）
     */
    public String processCharSet(String regex) {
        StringBuilder result = new StringBuilder();
        int i = 0;
        while (i < regex.length()) {
            char c = regex.charAt(i);
            if (c == '[' && !isEscaped(regex, i)) {
                int end = regex.indexOf(']', i + 1);
                if (end == -1)
                    throw new RuntimeException("未闭合的字符集");
                String content = regex.substring(i + 1, end);

                if (!content.isEmpty() && content.charAt(0) == '^') {
                    // 否定字符集 [^...]
                    String positiveContent = content.substring(1);
                    result.append(expandNegatedCharRange(positiveContent));
                } else {
                    result.append("(").append(expandCharRange(content)).append(")");
                }
                i = end + 1;
            } else {
                result.append(c);
                i++;
            }
        }
        return result.toString();
    }

    private String expandCharRange(String content) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < content.length(); i++) {
            char ch = content.charAt(i);

            // Handle escape sequences inside character class
            if (ch == '\\' && i + 1 < content.length()) {
                ch = resolveEscape(content.charAt(i + 1));
                i++; // Skip the escaped character
            }

            if (i + 2 < content.length() && content.charAt(i + 1) == '-') {
                char end = content.charAt(i + 2);
                // If end is escaped, resolve it
                if (end == '\\' && i + 3 < content.length()) {
                    end = resolveEscape(content.charAt(i + 3));
                    i++; // extra skip
                }
                for (char c = ch; c <= end; c++) {
                    if (sb.length() > 0)
                        sb.append(ALT);
                    sb.append(c);
                }
                i += 2;
            } else {
                if (sb.length() > 0)
                    sb.append(ALT);
                sb.append(ch);
            }
        }
        return sb.toString();
    }

    /**
     * 展开取反字符集 [^...]：收集排除字符集合，在 ASCII 可打印范围 0x01-0x7E 内取补集。
     * 结果用括号包裹的 | 交替式表示，元字符加 \\ 转义。
     */
    private String expandNegatedCharRange(String content) {
        Set<Character> excluded = new HashSet<>();

        // 先收集所有被排除的字符（与 expandCharRange 相同的解析逻辑）
        for (int i = 0; i < content.length(); i++) {
            char ch = content.charAt(i);

            if (ch == '\\' && i + 1 < content.length()) {
                ch = resolveEscape(content.charAt(i + 1));
                i++;
            }

            if (i + 2 < content.length() && content.charAt(i + 1) == '-') {
                char end = content.charAt(i + 2);
                if (end == '\\' && i + 3 < content.length()) {
                    end = resolveEscape(content.charAt(i + 3));
                    i++;
                }
                for (char c = ch; c <= end; c++) {
                    excluded.add(c);
                }
                i += 2;
            } else {
                excluded.add(ch);
            }
        }

        // 在 0x01-0x7E 范围内构建补集
        StringBuilder sb = new StringBuilder();
        sb.append("(");
        boolean first = true;
        for (char ch = 1; ch <= 0x7E; ch++) {
            if (!excluded.contains(ch)) {
                if (!first)
                    sb.append(ALT);
                // 对正则元字符加转义前缀，避免后续流程误读
                if (isRegexMeta(ch)) {
                    sb.append('\\');
                }
                sb.append(ch);
                first = false;
            }
        }
        sb.append(")");
        return sb.toString();
    }

    /**
     * 2. 处理 + 和 ? (修正版)
     * a+ -> aa*
     * a? -> (a|ε)
     *
     * <p>必须在 processCharSet 之前运行，否则字符集展开后的字面量 +/? 会被误当作运算符。
     * 同时跟踪方括号内/外状态，[] 内的 +/? 是字面量，不处理。
     */
    public String processPlusAndQuestion(String regex) {
        StringBuilder result = new StringBuilder();
        boolean inCharClass = false;
        for (int i = 0; i < regex.length(); i++) {
            char c = regex.charAt(i);

            // 跟踪字符集边界
            if (c == '[' && !isEscaped(regex, i)) {
                inCharClass = true;
                result.append(c);
                continue;
            }
            if (c == ']' && !isEscaped(regex, i) && inCharClass) {
                inCharClass = false;
                result.append(c);
                continue;
            }

            if ((c == '+' || c == '?') && !isEscaped(regex, i) && !inCharClass) {
                int lastIdx = result.length() - 1;
                String target;
                if (result.charAt(lastIdx) == ')') {
                    int start = findMatchingBracket(result.toString(), lastIdx, '(', ')');
                    target = result.substring(start);
                    result.delete(start, result.length());
                } else if (result.charAt(lastIdx) == ']') {
                    int start = findMatchingBracket(result.toString(), lastIdx, '[', ']');
                    target = result.substring(start);
                    result.delete(start, result.length());
                } else {
                    target = String.valueOf(result.charAt(lastIdx));
                    result.deleteCharAt(lastIdx);
                }

                if (c == '+') {
                    // a+ -> aa*
                    result.append(target).append(target).append(KLEENE);
                } else {
                    // a? -> (a|ε)
                    result.append("(").append(target).append(ALT).append(EPSILON).append(")");
                }
            } else {
                result.append(c);
            }
        }
        return result.toString();
    }

    /**
     * 3. 显式插入连接符 ·
     */
    public String insertConcatOperator(String regex) {
        StringBuilder result = new StringBuilder();
        boolean prevWasBackslash = false;
        for (int i = 0; i < regex.length(); i++) {
            char c1 = regex.charAt(i);
            result.append(c1);
            // \x 是一个整体操作数，跳过运算符解读
            if (c1 == '\\') {
                prevWasBackslash = true;
                continue;
            }
            if (i + 1 < regex.length()) {
                char c2 = regex.charAt(i + 1);
                if (shouldConcat(c1, c2, prevWasBackslash)) {
                    result.append(CONCAT);
                }
            }
            prevWasBackslash = false;
        }
        return result.toString();
    }

    private boolean shouldConcat(char c1, char c2, boolean c1Escaped) {
        // 左侧：操作数（含被转义的运算符/x）、右括号、或闭包
        boolean leftReady = c1Escaped || isOperand(c1) || c1 == ')' || c1 == KLEENE;
        // 右侧：操作数（\x 整体是操作数）或左括号
        boolean rightReady = (c2 == '\\') || isOperand(c2) || c2 == '(';
        return leftReady && rightReady;
    }

    /**
     * 4. 中缀转后缀 (调度场算法)
     */
    public String toPostfix(String regex) {
        StringBuilder output = new StringBuilder();
        Stack<Character> stack = new Stack<>();
        for (int i = 0; i < regex.length(); i++) {
            char c = regex.charAt(i);
            // 转义序列：作为一个整体操作数输出
            if (c == '\\' && i + 1 < regex.length()) {
                output.append(c);
                output.append(regex.charAt(i + 1));
                i++;
                continue;
            }
            if (isOperand(c)) {
                output.append(c);
            } else if (c == '(') {
                stack.push(c);
            } else if (c == ')') {
                while (!stack.isEmpty() && stack.peek() != '(')
                    output.append(stack.pop());
                stack.pop();
            } else {
                while (!stack.isEmpty() && precedence(stack.peek()) >= precedence(c)) {
                    output.append(stack.pop());
                }
                stack.push(c);
            }
        }
        while (!stack.isEmpty())
            output.append(stack.pop());
        return output.toString();
    }

    private boolean isOperand(char c) {
        return c != '(' && c != ')' && c != ALT && c != KLEENE && c != CONCAT;
    }

    private int precedence(char c) {
        if (c == KLEENE)
            return 3;
        if (c == CONCAT)
            return 2;
        if (c == ALT)
            return 1;
        return 0;
    }

    private int findMatchingBracket(String str, int end, char open, char close) {
        int count = 1;
        for (int i = end - 1; i >= 0; i--) {
            if (str.charAt(i) == close)
                count++;
            else if (str.charAt(i) == open)
                count--;
            if (count == 0)
                return i;
        }
        return -1;
    }

    private boolean isEscaped(String s, int i) {
        return i > 0 && s.charAt(i - 1) == '\\';
    }

    public String convert(String regex) {
        String s0 = processQuotes(regex);
        // processPlusAndQuestion 必须在 processCharSet 之前：
        // 否则字符集 [+\-*/] 展开为 (+|-|*|/) 后，字面量 +/? 会被误当作运算符
        String s1 = processPlusAndQuestion(s0);
        String s2 = processCharSet(s1);
        String s3 = insertConcatOperator(s2);
        String s4 = toPostfix(s3);
        return resolveEscapes(s4);
    }

    /**
     * 把后缀表达式中的转义序列 \x 替换为普通字符 x。
     * 但保留 \* 和 \| 不还原，因为 * 和 | 在后缀中是运算符，
     * 还原了会被 NFA 构建器误读。由 NfaBuilder 处理 \x 字面量。
     */
    private String resolveEscapes(String postfix) {
        StringBuilder sb = new StringBuilder();
        int i = 0;
        while (i < postfix.length()) {
            char c = postfix.charAt(i);
            if (c == '\\' && i + 1 < postfix.length()) {
                char next = postfix.charAt(i + 1);
                // * 和 | 保留转义形式，避免在 NFA 构建时被当作 Kleene/ALT 运算符
                if (next == '*' || next == '|') {
                    sb.append('\\').append(next);
                } else {
                    sb.append(resolveEscape(next));
                }
                i += 2;
            } else {
                sb.append(c);
                i++;
            }
        }
        return sb.toString();
    }

    /** Map escape sequences to actual characters. */
    private char resolveEscape(char c) {
        return switch (c) {
            case 't' -> '\t';
            case 'n' -> '\n';
            case 'r' -> '\r';
            case 'f' -> '\f';
            case 'v' -> (char) 0x0B; // vertical tab
            case '0' -> '\0';
            case '\\' -> '\\';
            case '\'' -> '\'';
            default -> c; // \( → (, \) → ), \* → *, etc.
        };
    }

    public static void main(String[] args) {
        RegexConverter conv = new RegexConverter();
        String[] tests = { "a+b", "a?", "[0-9]+", "a(b|c)*d" };
        for (String t : tests) {
            System.out.println("原始: " + t + " -> 后缀: " + conv.convert(t));
        }
    }
}