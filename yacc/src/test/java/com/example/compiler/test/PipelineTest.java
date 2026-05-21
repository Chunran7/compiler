package com.example.compiler.test;

import com.example.compiler.CompileResult;
import com.example.compiler.Compiler;

/**
 * 端到端流水线测试：通过统一入口 Compiler 编译源码 → 检查 IR 输出
 *
 * <p>运行方式:
 * <pre>
 *   cd yacc
 *   javac -encoding UTF-8 -d target/test-classes -cp target/classes \
 *       src/test/java/com/example/compiler/test/PipelineTest.java
 *   java -cp "target/classes;target/test-classes" com.example.compiler.test.PipelineTest
 * </pre>
 */
public final class PipelineTest {

    private static final Compiler compiler = new Compiler();

    public static void main(String[] args) {
        testSimpleProgram();
        testArithmeticExpression();
        testNestedFunctionCall();
        System.out.println("=== ALL PIPELINE TESTS PASSED ===");
    }

    // ── 例1: 简单函数 ──

    static void testSimpleProgram() {
        System.out.println("─── testSimpleProgram ───");
        String src = """
                int add(int x, int y) { return x + y; }
                int main() { return add(3, 4); }
                """;
        System.out.println("输入:\n" + src);

        CompileResult result = compiler.compile(src);
        String ir = result.irText();

        System.out.println(ir);

        assertContains(ir, "define i32 @add", "add 函数定义");
        assertContains(ir, "define i32 @main", "main 函数定义");
        assertContains(ir, "return", "return 语句");

        System.out.println("  PASS\n");
    }

    // ── 例2: 多次运算 ──

    static void testArithmeticExpression() {
        System.out.println("─── testArithmeticExpression ───");
        String src = """
                int calc(int a, int b) { return a * b + a; }
                int main() { return calc(2, 5); }
                """;
        System.out.println("输入:\n" + src);

        CompileResult result = compiler.compile(src);
        String ir = result.irText();

        System.out.println(ir);

        assertContains(ir, "define i32 @calc", "calc 函数定义");
        assertContains(ir, "define i32 @main", "main 函数定义");
        assertContains(ir, "return", "return 语句");

        System.out.println("  PASS\n");
    }

    // ── 例3: 嵌套函数调用 ──

    static void testNestedFunctionCall() {
        System.out.println("─── testNestedFunctionCall ───");
        String src = """
                int add(int x, int y) { return x + y; }
                int main() { return add(1, add(2, 3)); }
                """;
        System.out.println("输入:\n" + src);

        CompileResult result = compiler.compile(src);
        String ir = result.irText();

        System.out.println(ir);

        assertContains(ir, "define i32 @add", "add 函数");
        assertContains(ir, "define i32 @main", "main 函数");
        assertContains(ir, "call add", "add 调用（至少一处）");

        System.out.println("  PASS\n");
    }

    // ── 断言 ──

    static void assertContains(String haystack, String needle, String label) {
        if (!haystack.contains(needle)) {
            throw new AssertionError(label + " | IR 中找不到: \"" + needle + "\"");
        }
    }
}
