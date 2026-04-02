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
        // 匹配格式：name translation
        // 排除以 %{ 开头的声明代码
        Pattern defPattern = Pattern.compile("^([A-Z_][A-Z0-9_]*)\\s+(.+)$");
        // 匹配 C 风格注释的正则
        Pattern commentPattern = Pattern.compile("/\\*.*?\\*/|//.*$");

        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty() || line.startsWith("%"))
                continue;

            Matcher m = defPattern.matcher(line);
            if (m.find()) {
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
        do {
            changed = false;
            for (Map.Entry<String, String> entry : regularDefs.entrySet()) {
                String macro = "{" + entry.getKey() + "}";
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
     * 第三步：解析规则段 (初步实现：提取行)
     */
    public void parseRules() {
        String[] lines = rulePart.split("\n");
        int ruleId = 0;
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty())
                continue;

            // 这里使用一个简单的逻辑：寻找第一个空格或制表符作为 RE 和 Action 的分界
            // 复杂的解析建议后续使用双指针或更强的正则
            int splitIdx = line.indexOf(' ');
            if (splitIdx == -1)
                splitIdx = line.indexOf('\t');

            if (splitIdx != -1) {
                String re = line.substring(0, splitIdx).trim();
                String action = line.substring(splitIdx).trim();

                // 对规则中的宏进行最终展开
                String fullRegex = expandMacros(re);
                rules.add(new LexRule(++ruleId, fullRegex, action));
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

            parser.splitLexFile(content);
            parser.parseDefinitions();
            parser.parseRules();
            parser.debugPrint();

            System.out.println("\n=== 测试宏展开 ===");
            System.out.println("{L} 展开后 = " + parser.regularDefs.get("L"));
            System.out.println("{D} 展开后 = " + parser.regularDefs.get("D"));
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