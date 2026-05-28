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
     * 处理双引号字符串字面量。
     * 
     * 处理的正则模式：将 "..." 包裹的字符串转换为普通正则表达式。
     * 例如："hello" -> hello, "a*b" -> a\*b（元字符被转义）
     * 
     * 原理：
     * 1. 遍历输入字符串，检测未转义的双引号
     * 2. 找到配对的结束引号，提取引号内的内容
     * 3. 对引号内的正则元字符（* + ? | ( ) [ ]）添加反斜杠转义
     * 4. 移除引号本身，使引号内内容作为字面量参与后续处理
     * 5. 非引号部分保持不变
     * 
     * 这样做确保用户可以用引号明确指定某些字符是字面量而非运算符，
     * 避免与正则表达式的特殊含义冲突。
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
     * 处理字符集 [...] 和取反字符集 [^...]。
     * 
     * 处理的正则模式：
     * 1. 普通字符集 [abc] -> (a|b|c)，展开为交替选择形式
     * 2. 范围字符集 [a-z] -> (a|b|c|...|z)，展开所有范围内的字符
     * 3. 混合字符集 [a-z0-9] -> (a|b|...|z|0|1|...|9)
     * 4. 取反字符集 [^abc] -> (除a,b,c外的所有可打印ASCII字符的交替)
     * 5. 取反范围 [^a-z] -> (除a到z外所有可打印ASCII字符的交替)
     * 
     * 原理：
     * 1. 扫描字符串，定位未转义的左方括号 [
     * 2. 找到对应的右方括号 ]，提取中间的内容
     * 3. 对于普通字符集：调用 expandCharRange 展开范围和单个字符
     * - 遇到连字符 - 时，识别为范围表达式（如 a-z）
     * - 将范围展开为该范围内所有字符的交替（用 | 分隔）
     * - 单个字符直接添加到结果中
     * - 整个结果用括号包裹，形成 (a|b|c) 的形式
     * 4. 对于取反字符集（以 ^ 开头）：调用 expandNegatedCharRange
     * - 先收集所有要排除的字符（同样处理范围和单个字符）
     * - 在 ASCII 可打印范围 0x01-0x7E 内构建补集
     * - 将所有不在排除集合中的字符用 | 连接
     * - 对正则元字符添加转义前缀，避免后续流程误读
     * 5. 替换原字符集为展开后的交替表达式
     * 
     * 注意：此步骤必须在 processPlusAndQuestion 之后执行，
     * 否则字符集内的 + 或 ? 会被误当作量词运算符。
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

    /**
     * 展开普通字符集内容为交替表达式。
     * 
     * 处理的模式：
     * - 单个字符：a -> a
     * - 字符范围：a-z -> a|b|c|...|z
     * - 转义序列：\\n -> \n（换行符），\\t -> \t（制表符）等
     * - 混合：a-z0-9_ -> a|b|...|z|0|1|...|9|_
     * 
     * 原理：
     * 1. 逐个字符遍历字符集内容
     * 2. 如果遇到反斜杠转义，解析转义序列得到实际字符
     * 3. 如果当前字符后紧跟连字符 - 且有下一个字符，识别为范围表达式
     * - 提取范围的起始字符和结束字符
     * - 使用循环生成从起始到结束的所有字符
     * - 用 | 连接这些字符
     * 4. 如果不是范围，直接将字符添加到结果
     * 5. 多个字符/范围之间用 | 分隔
     * 
     * 返回值是不带括号的纯交替表达式，外层括号由调用者添加。
     */
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
     * 展开取反字符集 [^...] 为补集的交替表达式。
     * 
     * 处理的模式：
     * - [^abc] -> 所有不在 {a,b,c} 中的可打印ASCII字符的交替
     * - [^a-z] -> 所有不在 a到z 范围内的可打印ASCII字符的交替
     * - [^0-9] -> 所有非数字的可打印ASCII字符的交替
     * 
     * 原理：
     * 1. 首先解析并收集所有要排除的字符（与 expandCharRange 相同的逻辑）
     * - 处理转义序列
     * - 处理范围表达式（如 a-z），将范围内所有字符加入排除集合
     * - 处理单个字符，直接加入排除集合
     * 2. 在 ASCII 可打印范围 0x01-0x7E（即十进制1-126）内构建补集
     * - 遍历该范围内的每个字符
     * - 检查是否在排除集合中
     * - 如果不在排除集合中，则将其加入结果
     * 3. 对正则元字符（* + ? | ( ) [ ]）添加反斜杠转义
     * - 防止后续处理阶段将这些字面量误读为运算符
     * 4. 用括号包裹所有字符的交替表达式
     * 
     * 注意：只处理可打印ASCII范围，不包括控制字符（0x00）和DEL（0x7F及以上）。
     * 这样生成的表达式可能很长，但保证了语义的正确性。
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
     * 处理量词语法糖：+（一次或多次）和 ?（零次或一次）。
     * 
     * 处理的正则模式：
     * 1. a+ -> aa* （等价转换：至少一次 = 一次 + 零次或多次）
     * 2. (abc)+ -> (abc)(abc)* （分组后重复）
     * 3. [a-z]+ -> [a-z][a-z]* （字符集后重复）
     * 4. a? -> (a|ε) （等价转换：零次或一次 = 要么匹配a，要么匹配空串）
     * 5. (abc)? -> ((abc)|ε)
     * 6. [a-z]? -> ([a-z]|ε)
     * 
     * 原理：
     * 1. 遍历字符串，跟踪是否在字符集 [...] 内部（通过 inCharClass 标志）
     * - 字符集内的 + 或 ? 是字面量，不进行处理
     * - 只有字符集外的 + 或 ? 才是量词运算符
     * 2. 当检测到未转义的 + 或 ? 且不在字符集内时：
     * - 向前查找最近的操作数（operand）
     * - 操作数可能是：单个字符、括号分组 (...)、或字符集 [...]
     * - 通过 findMatchingBracket 找到匹配的括号位置
     * - 提取操作数并从结果缓冲区删除
     * 3. 根据量词类型进行转换：
     * - 对于 +：拼接 target + target + * （即 aa* 的形式）
     * - 对于 ?：拼接 ( + target + | + ε + ) （即 (a|ε) 的形式）
     * 4. 其他字符直接追加到结果
     * 
     * 重要：此步骤必须在 processCharSet 之前执行！
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
     * 显式插入连接运算符 ·（concatenation）。
     * 
     * 处理的正则模式：在需要隐式连接的位置插入显式的连接符。
     * 例如：
     * - ab -> a·b （两个操作数相邻）
     * - a(b|c) -> a·(b|c) （操作数后跟分组）
     * - (a)(b) -> (a)·(b) （两个分组相邻）
     * - a* b -> a*·b （闭包后跟操作数）
     * - \\x y -> \\x·y （转义序列后跟操作数）
     * 
     * 原理：
     * 1. 遍历字符串，逐字符检查当前位置和下一位置的关系
     * 2. 判断是否应该插入连接符的条件（shouldConcat）：
     * 左侧满足以下任一条件：
     * - 是被转义的字符（如 \\*、\\|、\\x 等）
     * - 是普通操作数（字母、数字等非运算符字符）
     * - 是右括号 ) （表示一个分组结束）
     * - 是闭包运算符 * （表示前面的表达式可以重复）
     * 
     * 右侧满足以下任一条件：
     * - 是反斜杠 \\ （表示转义序列的开始，整体视为操作数）
     * - 是普通操作数
     * - 是左括号 ( （表示新分组的开始）
     * 3. 如果左右两侧都满足条件，则在它们之间插入连接符 ·
     * 4. 特殊处理：遇到反斜杠时设置 prevWasBackslash 标志，
     * 跳过下一个字符的运算符解读（因为 \\x 是一个整体操作数）
     * 
     * 这一步的目的是将隐式的连接关系显式化，便于后续的调度场算法
     * 正确识别运算符优先级和操作数边界。
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
     * 将中缀正则表达式转换为后缀表达式（逆波兰表示法）。
     * 
     * 处理的正则模式：包含运算符 |（选择）、·（连接）、*（闭包）的中缀表达式。
     * 例如：
     * - a|b·c -> abc·| （先连接b和c，再与a选择）
     * - (a|b)* -> ab|* （先选择a或b，再闭包）
     * - a·b*·c -> ab*c· （b闭包后与a连接，再与c连接）
     * 
     * 原理：使用经典的调度场算法（Shunting-yard algorithm）
     * 1. 从左到右扫描中缀表达式的每个字符
     * 2. 对于不同类型的字符采取不同策略：
     * 
     * a) 转义序列 \\x：
     * - 将 \\ 和 x 一起作为整体操作数输出到结果
     * - 跳过下一个字符的处理
     * 
     * b) 操作数（字母、数字、ε 等）：
     * - 直接追加到输出队列
     * 
     * c) 左括号 (：
     * - 压入运算符栈
     * - 用于标记分组的开始
     * 
     * d) 右括号 )：
     * - 不断弹出栈顶运算符并输出，直到遇到左括号
     * - 弹出左括号但不输出（括号在后缀中不需要）
     * 
     * e) 运算符（|、·、*）：
     * - 比较当前运算符与栈顶运算符的优先级
     * - 如果栈顶运算符优先级 >= 当前运算符优先级：
     * 弹出栈顶并输出，重复此过程
     * - 将当前运算符压入栈
     * 
     * 3. 扫描结束后，将栈中剩余的所有运算符依次弹出并输出
     * 
     * 优先级定义（从高到低）：
     * - *（闭包）：优先级 3，最高，因为是单目运算符
     * - ·（连接）：优先级 2，中等
     * - |（选择）：优先级 1，最低
     * 
     * 后缀表达式的优势：
     * - 无需括号即可明确表示运算顺序
     * - 便于从左到右线性扫描构建 NFA
     * - 操作数在前，运算符在后，符合栈式计算模型
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

    /**
     * 完整的正则表达式转换流程。
     * 
     * 处理的完整流程（按顺序执行）：
     * 1. processQuotes：处理双引号字符串字面量，转义引号内的元字符
     * 2. processPlusAndQuestion：展开量词语法糖 + 和 ?
     * 3. processCharSet：展开字符集 [...] 和取反字符集 [^...]
     * 4. insertConcatOperator：显式插入连接运算符 ·
     * 5. toPostfix：中缀表达式转后缀表达式
     * 6. resolveEscapes：解析转义序列，将 \\x 转换为实际字符
     * 
     * 输入示例及转换过程：
     * 原始输入：[0-9]+
     * 步骤1（processQuotes）：[0-9]+ （无引号，不变）
     * 步骤2（processPlusAndQuestion）：[0-9][0-9]* （+ 展开为 aa*）
     * 步骤3（processCharSet）：(0|1|2|3|4|5|6|7|8|9)(0|1|2|3|4|5|6|7|8|9)*
     * 步骤4（insertConcatOperator）：(0|1|...|9)·(0|1|...|9)* （插入连接符）
     * 步骤5（toPostfix）：012...9|·012...9|*· （转为后缀）
     * 步骤6（resolveEscapes）：012...9|·012...9|*· （无转义，不变）
     * 
     * 最终输出：适合 NFA 构建器处理的后缀表达式
     */
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
     * 解析后缀表达式中的转义序列。
     * 
     * 处理的模式：将 \\x 形式的转义序列转换为实际字符 x。
     * 例如：
     * - \\n -> \n（换行符）
     * - \\t -> \t（制表符）
     * - \\( -> (（左括号字面量）
     * - \\* -> \\*（保留转义，因为 * 是运算符）
     * - \\| -> \\|（保留转义，因为 | 是运算符）
     * 
     * 原理：
     * 1. 遍历后缀表达式，检测反斜杠 \\
     * 2. 如果后面还有字符，检查下一个字符的类型：
     * 
     * a) 如果下一个字符是 * 或 |：
     * - 保留转义形式 \\* 或 \\|
     * - 原因：这两个字符在后缀表达式中是运算符（Kleene 闭包和选择）
     * - 如果还原为字面量，NFA 构建器会误将它们当作运算符
     * - 由 NfaBuilder 专门处理这种特殊情况
     * 
     * b) 其他字符（如 n, t, (, ), [, ] 等）：
     * - 调用 resolveEscape 解析转义序列
     * - 将 \\n 转换为实际的换行符 \n
     * - 将 \\( 转换为左括号字面量 (
     * - 将 \\x 转换为普通字符 x
     * 
     * 3. 非转义字符直接追加到结果
     * 
     * 特殊处理的原因：
     * 在后缀表达式中，* 和 | 是真正的运算符，需要被 NFA 构建器识别。
     * 但如果用户想匹配字面量的 * 或 |，我们会写成 \\* 或 \\|。
     * 如果在这里将它们还原为 * 和 |，NFA 构建器就无法区分这是运算符还是字面量。
     * 因此保留转义形式，让 NfaBuilder 在看到 \\* 时知道这是字面量星号。
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