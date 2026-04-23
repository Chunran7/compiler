# SeuYacc + IR 独立项目

这是一个可直接落地到本地空项目中的 Java 版本示例，包含：

- `.y` 文法文件解析
- FIRST 集计算
- LR(1) 自动机构造
- LALR(1) 状态合并
- ACTION / GOTO 分析表构造
- 基于分析表的语法分析
- 语法树 -> 语义阶段 -> Core AST
- 符号表与作用域检查
- 三地址式 IR 生成
- 基本块划分
- 总测试入口

## 目录

- `src/main/java`：主代码
- `src/test/java`：测试代码
- `resources/miniC_semantic_template.y`：示例 MiniC 文法

## 运行方式

### Windows
直接双击或命令行执行：

```bat
run-tests.bat
```

### Linux / macOS
```bash
chmod +x run-tests.sh
./run-tests.sh
```

## 测试内容

总测试入口：

- `com.example.compiler.test.TotalIntegrationTest`

测试会覆盖：

1. `.y` 文法解析与语义动作字符串保留
2. LR/LALR 分析表构造
3. MiniC token 串语法分析
4. 语义分析与符号表
5. IR 生成
6. 重复声明 / 未声明变量语义错误

## 说明

这个项目是“能直接部署、直接跑”的独立骨架，Yacc 与 IR 两部分已经打通。
Lex 部分没有接入，这里用手工构造 token 序列来驱动 Yacc 测试。
