# SeuYacc + IR 独立项目

这是一个可直接落地到本地空项目中的 Java 版本示例，包含：

- `c99.l` 词法规则与生成词法分析器
- `c99.y` C99 文法解析
- `.y` 文法文件解析
- FIRST 集计算
- LR(1) 自动机构造
- LALR(1) 状态合并
- ACTION / GOTO 分析表构造
- 基于分析表的语法分析
- C99 语法树 -> MiniC 子集编译时语义分析 -> Core AST / 符号表
- 符号表与作用域检查
- MiniC 子集运行时语义翻译 -> 生成 C 语义程序 -> 运行 C 程序生成 LLVM IR 文本子集
- 基本块划分
- 总测试入口

## 目录

- `src/main/java`：主代码
- `src/test/java`：测试代码
- `resources/c99.l`：正式 C99 词法规则
- `resources/c99.y`：正式 C99 文法
- `src/test/resources/grammars`：yacc 生成器专项测试文法夹具

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

按课程 PPT 路线运行（生成 `yysemantic.c`，编译并执行该 C 程序生成中间代码）：

```bash
bash run-tests.sh
java -cp out com.example.compiler.Compiler --via-c -e 'int add(int x, int y) { return x + y; } int main() { return add(3, 4); }'
```

生成的 C 语义程序位于：

```text
generated/semantic/yysemantic.c
```

继续按 LLVM 路线生成最终可执行文件：

```bash
java -cp out com.example.compiler.Compiler \
  --via-c \
  --emit-ll generated/final/program.ll \
  --emit-exe generated/final/program.exe \
  -e 'int add(int x, int y) { return x + y; } int main() { return add(3, 4); }'
```

该流程为：

```text
源码 -> lex/yacc -> Core AST/符号表 -> yysemantic.c -> 运行 C 语义程序生成 .ll -> clang 编译 .ll 得到可执行文件
```

## 测试内容

总测试入口：

- `com.example.compiler.test.TotalIntegrationTest`

测试会覆盖：

1. `c99.l` 可直接产生的 C99 token 样例覆盖
2. `c99.y` 文法解析、LR(1) / LALR(1) 表构造
3. `GeneratedLexer + c99.y` 的 C99 parser-only 样例解析与产生式覆盖统计
4. yacc 生成器专项能力测试（优先级/结合性、LALR 状态合并、冲突报告）
5. MiniC 子集编译时语义分析、符号表、生成 C 语义程序并运行得到 LLVM IR 文本
6. LLVM IR 文本经 `clang` 编译生成可执行文件
7. MiniC 子集重复声明 / 未声明变量 / 函数调用错误

## 说明

这个项目是“能直接部署、直接跑”的独立骨架。

词法和语法阶段以 `resources/c99.l` 与 `resources/c99.y` 为正式规格；语义分析和 IR 生成阶段目前只支持 C99 语法树中的 MiniC 子集。测试中会明确区分 parser-only 的完整 C99 语法样例，以及会继续进入 semantic/IR 的 MiniC 子集样例。

语义阶段分为两条路径：

1. 课程 PPT 对齐路径：`CompileTimeSemanticAnalyzer` 构造 Core AST 与符号表并完成重复声明、未声明变量、函数调用参数数量等编译时检查；`CSemanticProgramEmitter` 根据 Core AST 生成 `yysemantic.c`；随后 `gcc` 编译并运行该 C 程序，由 C 程序执行赋值、算术、函数调用、分支等运行时语义动作并输出 LLVM IR 文本子集（如 `define`、`alloca`、`store`、`load`、`add`、`icmp`、`br`、`ret`）。
2. Java 直接路径：`ThreeAddressIrGenerator` 与 `LlvmLikeTextEmitter` 保留为调试和对照使用，可直接在 Java 进程内生成三地址式 IR 和 LLVM IR 文本子集。

当前输出不是 LLVM bitcode，也不是 Java 字节码。

当前 `TYPE_NAME` 依赖 typedef-name 跟踪，`c99.l` 中的 `check_type()` 仍按原始注释返回 `IDENTIFIER`，因此 lexer token 覆盖统计会把 `TYPE_NAME` 作为已知边界单独说明。
