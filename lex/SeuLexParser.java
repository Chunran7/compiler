import java.util.*;
import java.util.regex.*;

/**
 * SeuLex 词法分析生成器 - 基础解析框架
 */
public class SeuLexParser {

    // C99 词法文件路径配置
    private static final String LEX_FILE_PATH = "lex/c99.l";

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

    /**
     * 第一步：将 Lex 文件内容拆分为三个核心部分
     */
    public void splitLexFile(String fullContent) {
        // 使用正则匹配行首的 %%
        String[] sections = fullContent.split("(?m)^%%\\s*");
        // (?m)多行模式，^ 表示行首，\\s* 表示任意空白字符

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
        Pattern defPattern = Pattern.compile("^([A-Z_][A-Z0-9_]*)\\s+(.+)$");
        // 第一段：^([A-Z_][A-Z0-9_]*) - 捕获宏名称，第二段：\\s+ - 匹配分隔符，第三段：(.+)$ - 捕获翻译内容

        // 匹配 C 风格注释的正则
        Pattern commentPattern = Pattern.compile("/\\*.*?\\*/|//.*$");

        for (String line : lines) {
            line = line.trim();// 去掉行首尾空白
            if (line.isEmpty() || line.startsWith("%")) // 跳过空行和%{ %}开头的
                continue;

            // 检查格式是否匹配正则定义的规范
            Matcher m = defPattern.matcher(line);
            if (m.find()) {

                // 分别获取宏名称和翻译内容
                String name = m.group(1);
                String translation = m.group(2);

                // 去掉翻译部分的注释
                translation = removeComments(translation).trim();

                // 存入 Map 前执行宏展开
                String expanded = expandMacros(translation);
                regularDefs.put(name, expanded);
            }
        }
    }

    /**
     * 递归替换宏引用，例如将 {D} 替换为 [0-9]
     */
    private String expandMacros(String input) {
        String result = input;
        boolean changed;

        // 不断替换宏引用直到没有可以替换的为止
        do {
            changed = false;
            for (Map.Entry<String, String> entry : regularDefs.entrySet()) {
                // entry.getKey() 是宏名称，entry.getValue() 是宏定义的内容
                String macro = "{" + entry.getKey() + "}";
                // macro一定是以 { 开头，} 结尾的字符串，区分于正常字符

                if (result.contains(macro)) {
                    // 使用括号包裹以维持优先级
                    result = result.replace(macro, "(" + entry.getValue() + ")");
                    changed = true;
                }
            }
        } while (changed); // 处理嵌套定义
        return result;
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

            for (int i = 0; i < line.length(); i++) {
                char c = line.charAt(i);

                // 检查引号，如果引号位于开头或者前面没有转义符，则切换状态
                if (c == '"' && (i == 0 || line.charAt(i - 1) != '\\')) {
                    inQuotes = !inQuotes;
                } else if (!inQuotes && (c == ' ' || c == '\t')) {
                    splitIdx = i;
                    break;
                }
            }

            if (splitIdx != -1) {
                String re = line.substring(0, splitIdx).trim();
                String action = line.substring(splitIdx).trim();

                // 对规则中的宏进行最终展开
                String fullRegex = expandMacros(re);
                rules.add(new LexRule(++ruleId, fullRegex, action));
            } else {
                // 没有动作的规则（只有正则表达式）
                String fullRegex = expandMacros(line);
                rules.add(new LexRule(++ruleId, fullRegex, ""));
            }
        }
    }

    // 打印结果用于调试
    public void debugPrint() {
        System.out.println("=== Regular Definitions ===");
        regularDefs.forEach((k, v) -> System.out.println(k + " = " + v));
        System.out.println("\n=== Rules ===");
        rules.forEach(System.out::println);
    }

    public static void main(String[] args) {
        SeuLexParser parser = new SeuLexParser();

        try {
            // 读取 c99.l 文件内容
            String content = new String(java.nio.file.Files.readAllBytes(java.nio.file.Paths.get(LEX_FILE_PATH)));

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