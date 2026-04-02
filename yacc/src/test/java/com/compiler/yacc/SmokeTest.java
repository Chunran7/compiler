//这个代码文件一定要放到test目录下
// main 目录：存放项目实际代码，不会自动包含 JUnit 依赖
//
//test 目录：存放测试代码，Maven 会自动添加 JUnit 到 classpath
package com.compiler.yacc;//声明这个类所在的包（文件夹目录）
import org.junit.jupiter.api.Test;//导入JUnit 5 的@Test注解，JUnit是java中最常用的单元测试框架
//@Test是在告诉JUnit SmokeTest类里的哪些方法是测试方法

import static org.junit.jupiter.api.Assertions.assertEquals;//静态导入assertEquals方法，用来断言两个值是否相等
//由于是静态导入，在使用的时候不需要类名前缀
//该处用的是只导入一个方法，而不是该类中的所有静态方法
public class SmokeTest {
    @Test
    void testBasic() {
        assertEquals(2, 1 + 1);
    }
}
//公共类，其他类可以访问，类名需要首字母大写
//JUnit 启动
//    ↓
//找到所有带 @Test 的方法
//    ↓
//执行 testBasic() 方法
//    ↓
//计算 1 + 1 = 2
//    ↓
//调用 assertEquals(2, 2)
//    ↓
//2 == 2 → 相等 → 测试通过 ✅
//    ↓
//显示绿色