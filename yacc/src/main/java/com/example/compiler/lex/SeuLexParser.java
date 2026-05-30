package com.example.compiler.lex;

import java.util.*;
import java.util.regex.*;

import com.example.compiler.yacc.token.TokenType;

/**
 * SeuLex 词法分析生成器 - 基础解析框架
 */
public class SeuLexParser {

    // C99 词法文件路径配置
    private static final String LEX_FILE_PATH = "yacc/resources/c99.l";

    // 内部类：存储 RE-Action 对
    public static class LexRule {
        public int id;
        public String regex; // 处理后的正规表达式
        public String action; // 对应的 C/Java 代码动作

        public LexRule(int id, String regex, String action) {
            this.id = id;
            this.regex = regex;
            this.action = action;
        }

        @Override
        public String toString() {
            return String.format("Rule #%d: [%s] -> {%s}", id, regex, action);
        }
    }

    private String definitionPart = "";
    private String rulePart = "";
    private String userSubroutinePart = "";

    private final Map<String, String> regularDefs = new LinkedHashMap<>();
    private final List<LexRule> rules = new ArrayList<>();

    // ========== Getter 方法 ==========
    public Map<String, String> getRegularDefs() {
        return regularDefs;
    }

    public List<LexRule> getRules() {
        return rules;
    }

    /**
     * 获取定义段文本（用于测试）
     */
    public String getDefinitionPart() {
        return definitionPart;
    }

    /**
     * 获取规则段文本（用于测试）
     */
    public String getRulePart() {
        return rulePart;
    }

    /**
     * 获取用户代码段文本（用于测试）
     */
    public String getUserSubroutinePart() {
        return userSubroutinePart;
    }

    /**
     * 第一步：将 Lex 文件内容拆分为三个核心部分
     */
    public void splitLexFile(String fullContent) {
        // 使用正则匹配行首的 %%
        String[] sections = fullContent.split("(?m)^%%\\s*");
        // (?m)多行模式，^ 表示行首，\\s* 表示任意空白字符

        // trim()表示去掉首尾空白
        if (sections.length >= 1)
            definitionPart = sections[0].trim();
        if (sections.length >= 2)
            rulePart = sections[1].trim();
        if (sections.length >= 3)
            userSubroutinePart = sections[2].trim();
    }

    /**
     * 第二步：解析定义段，处理正规定义
     */
    public void parseDefinitions() {
        String[] lines = definitionPart.split("\n");
        // 匹配宏定义格式：名称（字母/下划线开头）+ 空白分隔符 + 翻译内容
        Pattern defPattern = Pattern.compile("^([a-zA-Z_][a-zA-Z0-9_]*)\\s+(.+)$");

        // 第一阶段：收集所有原始定义（未展开）
        Map<String, String> rawDefs = new LinkedHashMap<>();
        for (String line : lines) {
            line = line.trim(); // 去掉行首尾空白
            if (line.isEmpty() || line.startsWith("%")) // 跳过空行和 %{ %} 开头的行
                continue;

            // 检查格式是否匹配宏定义的规范
            Matcher m = defPattern.matcher(line);
            if (m.find()) {
                // 分别获取宏名称和翻译内容
                String name = m.group(1);
                String translation = m.group(2);

                // 去掉翻译部分的注释
                translation = removeComments(translation).trim();

                // 先存入原始定义，暂不展开
                rawDefs.put(name, translation);
            }
        }

        // 第二阶段：基于完整定义集合统一展开所有宏，防止因宏定义先后问题导致有些宏未被正确展开
        for (Map.Entry<String, String> entry : rawDefs.entrySet()) {
            String expanded = expandMacros(entry.getValue(), rawDefs);
            regularDefs.put(entry.getKey(), expanded);
        }
    }

    /**
     * 递归替换宏引用，例如将 {D} 替换为 [0-9]
     * 
     * @param input   待展开的字符串
     * @param allDefs 完整的宏定义集合（用于查找所有宏）
     * @return 展开后的字符串
     */
    private String expandMacros(String input, Map<String, String> allDefs) {
        String result = input;
        boolean changed;
        int iterations = 0;

        // 不断替换宏引用直到没有可以替换的为止
        do {
            changed = false;
            for (Map.Entry<String, String> entry : allDefs.entrySet()) {
                // entry.getKey() 是宏名称，entry.getValue() 是宏定义的内容
                String macro = "{" + entry.getKey() + "}";
                // macro 一定是以 { 开头，} 结尾的字符串，区分于正常字符

                if (result.contains(macro)) {
                    // 使用括号包裹以维持优先级
                    result = result.replace(macro, "(" + entry.getValue() + ")");
                    changed = true;
                }
            }
            iterations++;
            if (iterations > 50) {
                throw new RuntimeException("宏展开失败：可能存在循环引用或嵌套过深");
            }
        } while (changed); // 处理嵌套定义
        return result;
    }

    /**
     * 重载方法：兼容旧代码调用（使用全局 regularDefs）
     */
    private String expandMacros(String input) {
        return expandMacros(input, regularDefs);
    }

    /**
     * 第三步：解析规则段，提取正则表达式和动作
     */
    public void parseRules() {
        String[] lines = rulePart.split("\n");
        int ruleId = 0;

        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty() || line.startsWith("%"))
                continue;

            // 寻找第一个空白字符（空格或制表符）作为 RE 和 Action 的分界
            // 需要跳过引号内的内容，避免由于规则中包含引号而导致的错误分割
            int splitIdx = -1;
            boolean inQuotes = false;
            boolean inBracket = false;

            for (int i = 0; i < line.length(); i++) {
                char c = line.charAt(i);

                // 检查引号，如果引号位于开头或者前面没有转义符，则切换状态
                if (c == '"' && (i == 0 || line.charAt(i - 1) != '\\')) {
                    inQuotes = !inQuotes;
                } else if (!inQuotes && c == '[' && (i == 0 || line.charAt(i - 1) != '\\')) {
                    inBracket = true;
                } else if (!inQuotes && c == ']' && (i == 0 || line.charAt(i - 1) != '\\')) {
                    inBracket = false;
                } else if (!inQuotes && !inBracket && (c == ' ' || c == '\t')) {
                    splitIdx = i;
                    break;
                }
            }

            if (splitIdx != -1) {
                String re = line.substring(0, splitIdx).trim();
                String action = line.substring(splitIdx).trim();

                // 去掉动作部分的注释
                action = removeComments(action).trim();

                // 对规则中的宏进行最终展开
                String fullRegex = expandMacros(re);
                // 将 C 风格 action 翻译为 Java 代码
                String javaAction = translateAction(action);
                rules.add(new LexRule(++ruleId, fullRegex, javaAction));
            } else {
                // 没有动作的规则（只有正则表达式）
                String fullRegex = expandMacros(line);
                rules.add(new LexRule(++ruleId, fullRegex, ""));
            }
        }
    }

    /**
     * 将 c99.l 中的 C 风格 action 翻译为 Java 代码。
     *
     * <p>
     * 处理两种 return 模式：
     * <ul>
     * <li>{@code return(TOKEN_NAME)} →
     * {@code return new Token(TokenType.TOKEN_NAME, new String(yytext, 0, yyleng))}</li>
     * <li>{@code return('c')} →
     * {@code return new Token(TokenType.forChar('c'), new String(yytext, 0, yyleng))}</li>
     * </ul>
     * 其他内容保持原样。
     * </p>
     */
    public static String translateAction(String action) {
        // 如果已经是 Java 代码（包含 new Token( 或 TokenType.），跳过翻译
        if (action.contains("new Token(") || action.contains("TokenType.")) {
            return action;
        }

        // 1. 翻译 return(大写_TOKEN) → return new Token(TokenType.TOKEN, ...)
        // 只匹配大写字母和下划线开头的 token 名，避免误匹配 check_type() 这种小写函数
        action = action.replaceAll(
                "return\\(([A-Z_][A-Z_0-9]*)\\)",
                "return new Token(TokenType.$1, new String(yytext, 0, yyleng))");

        // 2. 翻译 return('c') → return new Token(TokenType.forChar('c'), ...)
        Pattern charRetPattern = Pattern.compile("return\\('(.)'\\)");
        Matcher m = charRetPattern.matcher(action);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            String ch = m.group(1);
            // 对特殊字符转义
            String escaped = ch.equals("'") ? "\\'" : ch;
            m.appendReplacement(sb,
                    "return new Token(TokenType.forChar('" + escaped + "'), new String(yytext, 0, yyleng))");
        }
        m.appendTail(sb);

        return sb.toString();
    }

    // 打印结果用于调试
    public void debugPrint() {
        System.out.println("=== Regular Definitions ===");
        regularDefs.forEach((k, v) -> System.out.println(k + " = " + v));
        System.out.println("\n=== Rules ===");
        rules.forEach(System.out::println);
    }

    /**
     * 测试主函数 - 演示解析流程
     */
    public static void main(String[] args) {
        SeuLexParser parser = new SeuLexParser();

        try {
            // 读取 c99.l 文件内容（使用绝对路径）
            String lexFilePath = LEX_FILE_PATH;
            System.out.println("正在读取文件: " + lexFilePath);

            String content = new String(java.nio.file.Files.readAllBytes(java.nio.file.Paths.get(lexFilePath)));

            System.out.println("=== 原始文件内容前 200 字符 ===");
            System.out.println(content.substring(0, Math.min(200, content.length())));
            System.out.println("\n");

            parser.splitLexFile(content);

            System.out.println("=== 分割后的三个部分 ===");
            System.out.println("定义段长度：" + parser.definitionPart.length());
            System.out.println("规则段长度：" + parser.rulePart.length());
            System.out
                    .println("用户代码段长度：" + (parser.userSubroutinePart != null ? parser.userSubroutinePart.length() : 0));
            System.out.println();

            parser.parseDefinitions();
            parser.parseRules();
            parser.debugPrint();

            System.out.println("\n=== 测试宏展开 ===");
            System.out.println("{L} 展开后 = " + parser.regularDefs.get("L"));
            System.out.println("{D} 展开后 = " + parser.regularDefs.get("D"));
            System.out.println("{H} 展开后 = " + parser.regularDefs.get("H"));

            System.out.println("\n=== 前 5 条规则 ===");
            for (int i = 0; i < Math.min(5, parser.rules.size()); i++) {
                System.out.println(parser.rules.get(i));
            }
        } catch (Exception e) {
            System.err.println("读取文件失败：" + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 去掉字符串中的 C 风格注释（包括 /* 和 //）
     */
    private String removeComments(String input) {
        // 先去掉 /* ... */ 形式的注释（非贪婪匹配）
        String result = input.replaceAll("/\\*.*?\\*/", "");
        // 再去掉 // 形式的行内注释
        result = result.replaceAll("//.*$", "");
        return result.trim();
    }
}