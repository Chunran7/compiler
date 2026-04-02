import java.util.*;

/**
 * Parser 与正则转换联调测试程序
 * 测试内容：Lex 文件解析 + 宏展开 + 正则转换
 */
public class ParserRegexTest {

    private static final String TEST_FILE_PATH = "lex/c99.l";

    public static void main(String[] args) {
        System.out.println("╔════════════════════════════════════════════════╗");
        System.out.println("║   Lex Parser & Regex Converter 联调测试       ║");
        System.out.println("╚════════════════════════════════════════════════╝\n");

        try {
            // ========== 第一阶段：Lex 文件解析 ==========
            System.out.println("【阶段 1】Lex 文件解析测试");
            System.out.println("─────────────────────────────────────────");

            SeuLexParser parser = new SeuLexParser();
            String content = new String(java.nio.file.Files.readAllBytes(
                    java.nio.file.Paths.get(TEST_FILE_PATH)));

            parser.splitLexFile(content);
            System.out.println("✓ 文件分割完成");
            System.out.println("  - 定义段长度：" + parser.getDefinitionPart().length() + " 字符");
            System.out.println("  - 规则段长度：" + parser.getRulePart().length() + " 字符");
            System.out.println("  - 用户代码段长度：" +
                    (parser.getUserSubroutinePart() != null ? parser.getUserSubroutinePart().length() : 0) + " 字符\n");

            // ========== 第二阶段：宏定义解析 ==========
            System.out.println("【阶段 2】宏定义解析测试");
            System.out.println("─────────────────────────────────────────");

            parser.parseDefinitions();
            Map<String, String> macros = parser.getRegularDefs();

            System.out.println("✓ 成功解析 " + macros.size() + " 个宏定义：\n");

            // 打印所有宏定义
            int macroCount = 0;
            for (Map.Entry<String, String> entry : macros.entrySet()) {
                System.out.printf("  %-8s = %s%n", "{" + entry.getKey() + "}", entry.getValue());
                if (++macroCount >= 10) {
                    System.out.println("  ... (还有 " + (macros.size() - 10) + " 个宏)");
                    break;
                }
            }
            System.out.println();

            // ========== 第三阶段：规则解析 ==========
            System.out.println("【阶段 3】规则解析测试");
            System.out.println("─────────────────────────────────────────");

            parser.parseRules();
            List<SeuLexParser.LexRule> rules = parser.getRules();

            System.out.println("✓ 成功解析 " + rules.size() + " 条规则\n");

            // 打印前 10 条规则
            System.out.println("前 10 条规则预览：");
            for (int i = 0; i < Math.min(10, rules.size()); i++) {
                SeuLexParser.LexRule rule = rules.get(i);
                System.out.printf("  Rule #%2d: %-30s -> %s%n",
                        rule.id,
                        truncate(rule.regex, 28),
                        truncate(rule.action, 40));
            }
            if (rules.size() > 10) {
                System.out.println("  ... (还有 " + (rules.size() - 10) + " 条规则)");
            }
            System.out.println();

            // ========== 第四阶段：正则转换测试 ==========
            System.out.println("【阶段 4】正则转换测试");
            System.out.println("─────────────────────────────────────────");

            RegexConverter converter = new RegexConverter();
            int successCount = 0;
            int failCount = 0;

            // 测试前 20 条规则的正则转换
            int testLimit = rules.size();

            for (int i = 0; i < testLimit; i++) {
                SeuLexParser.LexRule rule = rules.get(i);

                try {
                    long startTime = System.nanoTime();
                    String postfix = converter.convert(rule.regex);
                    long endTime = System.nanoTime();

                    double durationMs = (endTime - startTime) / 1_000_000.0;

                    System.out.printf("\n规则 #%2d (%.2f ms):%n", rule.id, durationMs);
                    System.out.printf("  原始：%s%n", truncate(rule.regex, 50));
                    System.out.printf("  后缀：%s%n", truncate(postfix, 50));

                    successCount++;

                } catch (Exception e) {
                    System.out.printf("\n✗ 规则 #%2d 转换失败:%n", rule.id);
                    System.out.printf("  原始：%s%n", truncate(rule.regex, 50));
                    System.out.printf("  错误：%s%n", e.getMessage());
                    failCount++;
                }
            }

            System.out.println("\n─────────────────────────────────────────");
            System.out.println("转换统计：");
            System.out.println("  ✓ 成功：" + successCount + " 条");
            System.out.println("  ✗ 失败：" + failCount + " 条");
            System.out.println("  ∑ 总计：" + testLimit + " 条");
            System.out.println();

            // ========== 第五阶段：特殊用例测试 ==========
            System.out.println("【阶段 5】特殊用例测试");
            System.out.println("─────────────────────────────────────────");

            runSpecialTests(converter);

            // ========== 最终报告 ==========
            System.out.println("\n╔════════════════════════════════════════════════╗");
            System.out.println("║           联调测试完成报告                    ║");
            System.out.println("╠════════════════════════════════════════════════╣");
            System.out.printf("║  宏定义数量：%3d 个%-21s║%n", macros.size(), "");
            System.out.printf("║  规则总数：  %3d 条%-21s║%n", rules.size(), "");
            System.out.printf("║  转换成功：  %3d 条%-21s║%n", successCount, "");
            System.out.printf("║  转换失败：  %3d 条%-21s║%n", failCount, "");
            System.out.println("║  测试状态： " + (failCount == 0 ? "全部通过 ✓" : "部分失败 ✗") + "                   ║");
            System.out.println("╚════════════════════════════════════════════════╝");

        } catch (Exception e) {
            System.err.println("\n❌ 测试失败：" + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 运行特殊用例测试
     */
    private static void runSpecialTests(RegexConverter converter) {
        String[] testCases = {
                // 基础语法糖
                "a+",
                "a?",
                "[0-9]+",
                "[a-zA-Z_][a-zA-Z0-9_]*",

                // 复杂表达式
                "([Ee][+-]?[0-9]+)",
                "(f|F|l|L)",
                "((u|U)|(u|U)?(l|L|ll|LL)|(l|L|ll|LL)(u|U))",

                // 浮点数模式
                "[0-9]*\\.[0-9]+([Ee][+-]?[0-9]+)?",

                // 字符串模式
                "\"(\\\\.|[^\\\\\"\n])*\""
        };

        System.out.println("特殊用例测试：\n");

        for (String regex : testCases) {
            try {
                String result = converter.convert(regex);
                System.out.printf("  ✓ %-40s%n", truncate(regex, 38));
                System.out.printf("    → %s%n", truncate(result, 50));
            } catch (Exception e) {
                System.out.printf("  ✗ %-40s%n", truncate(regex, 38));
                System.out.printf("    错误：%s%n", e.getMessage());
            }
        }
    }

    /**
     * 截断长字符串用于显示
     */
    private static String truncate(String str, int maxLen) {
        if (str == null)
            return "";
        if (str.length() <= maxLen)
            return str;
        return str.substring(0, maxLen - 3) + "...";
    }
}
