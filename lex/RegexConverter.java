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
     * 1. 处理字符集 [] -> (a|b|c)
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
                result.append("(").append(expandCharRange(content)).append(")");
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
            if (i + 2 < content.length() && content.charAt(i + 1) == '-') {
                char start = content.charAt(i);
                char end = content.charAt(i + 2);
                for (char c = start; c <= end; c++) {
                    if (sb.length() > 0)
                        sb.append(ALT);
                    sb.append(c);
                }
                i += 2;
            } else {
                if (sb.length() > 0)
                    sb.append(ALT);
                sb.append(content.charAt(i));
            }
        }
        return sb.toString();
    }

    /**
     * 2. 处理 + 和 ? (修正版)
     * a+ -> aa*
     * a? -> (a|ε)
     */
    public String processPlusAndQuestion(String regex) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < regex.length(); i++) {
            char c = regex.charAt(i);
            if ((c == '+' || c == '?') && !isEscaped(regex, i)) {
                int lastIdx = result.length() - 1;
                String target;
                if (result.charAt(lastIdx) == ')') {
                    int start = findMatchingBracket(result.toString(), lastIdx);
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
        for (int i = 0; i < regex.length(); i++) {
            char c1 = regex.charAt(i);
            result.append(c1);
            if (i + 1 < regex.length()) {
                char c2 = regex.charAt(i + 1);
                if (shouldConcat(c1, c2)) {
                    result.append(CONCAT);
                }
            }
        }
        return result.toString();
    }

    private boolean shouldConcat(char c1, char c2) {
        // 左侧是操作数、右括号、或闭包
        boolean leftReady = isOperand(c1) || c1 == ')' || c1 == KLEENE;
        // 右侧是操作数或左括号
        boolean rightReady = isOperand(c2) || c2 == '(';
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

    private int findMatchingBracket(String str, int end) {
        int count = 1;
        for (int i = end - 1; i >= 0; i--) {
            if (str.charAt(i) == ')')
                count++;
            else if (str.charAt(i) == '(')
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
        String s1 = processCharSet(regex);
        String s2 = processPlusAndQuestion(s1);
        String s3 = insertConcatOperator(s2);
        return toPostfix(s3);
    }

    public static void main(String[] args) {
        RegexConverter conv = new RegexConverter();
        String[] tests = { "a+b", "a?", "[0-9]+", "a(b|c)*d" };
        for (String t : tests) {
            System.out.println("原始: " + t + " -> 后缀: " + conv.convert(t));
        }
    }
}