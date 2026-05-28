# 1. 项目真实 Goal

本项目不是完整 C99 编译器。

项目真实目标是复现老师图片中的编译流程骨架，并在语义和 IR 阶段支持一个 MiniC/C 子集：

- 使用 `resources/c99.l` 生成 C 词法分析程序 `yylex.c`。
- 使用 `resources/c99.y` 生成 C 语法分析程序 `yyparse.c`。
- `gcc` 编译并运行 `yylex`，输出 `tokens.txt`。
- `gcc` 编译并运行 `yyparse`，输出带语义动作节点的 `action-tree.txt`。
- 使用 `AstTreeCodec` 反序列化 `action-tree.txt`。
- 使用 `SemanticActionEngine` 和 `TranslationSchemeExecutor` 执行动作节点，构建 Core AST。
- 使用 `CompileTimeSemanticAnalyzer` 完成基础语义检查并生成 `SymbolTable`。
- 使用 `CSemanticProgramEmitter` 生成 `yysemantic.c`。
- `gcc` 编译并运行 `yysemantic`，从标准输出得到 LLVM IR `output.ll`。
- 使用 `JimpleTextEmitter` 从 Core AST/IR 生成 Jimple 文本 `output.jimple`。
- 可选使用 `NativeBackend` 调用 `clang` 验证 LLVM IR、生成汇编、目标文件和本机可执行文件。
- 可选使用 `SootInvoker` 调用 Soot 处理 Jimple。

# 2. 当前真实实现状态

| 项目 | 当前状态 |
|---|---|
| `yylex.c` | 已实现。由 `CLexerToolchainEmitter` 从 `c99.l` 生成。 |
| `yylex` | 已实现。主流程用 `gcc` 编译 `yylex.c`。 |
| `tokens.txt` | 已实现。主流程运行 `yylex test.c tokens.txt`。 |
| `yyparse.c` | 已实现。由 `CParserProgramEmitter` 从 `c99.y`、LALR parse table 和语义动作映射生成。 |
| `yyparse` | 已实现。主流程用 `gcc` 编译 `yyparse.c`。 |
| `action-tree.txt` | 已实现并真实落盘。由 `yyparse tokens.txt action-tree.txt` 生成。 |
| `AstTreeCodec` | 已实现。读取 `action-tree.txt` 的 `NODE` 序列化格式。 |
| `SemanticActionEngine` | 已实现。当前主流程使用 `analyzeActionTree`。 |
| `TranslationSchemeExecutor` | 已实现。执行 `{ $$ = ... }` 和 `{ $$ = $n; }` 形式动作。 |
| Core AST | 已实现。`CoreAstNode`/`AstKind` 表示 MiniC 语义树。 |
| `CompileTimeSemanticAnalyzer` | 已实现。检查 main、声明、作用域、函数声明和基础表达式。 |
| `SymbolTable` | 已实现。输出 `symbol-table.txt`。 |
| `yysemantic.c` | 已实现。由 `CSemanticProgramEmitter` 从 Core AST 生成。 |
| `yysemantic` | 已实现。主流程用 `gcc` 编译并运行。 |
| `output.ll` | 已实现。由 `yysemantic` 的 stdout 写入。 |
| `output.jimple` | 已实现为文本 emitter。它不是完整 Soot 工程，只是 Jimple-like 文本输出。 |
| `NativeBackend` | 已实现。可选调用 `clang` 生成 `validate.o`、`output.s`、`output.o`、`native-executable`。 |
| `SootInvoker` | 部分实现。只有 `SOOT_JAR` 环境变量存在且显式请求 bytecode 输出时才运行；默认测试记录 `soot-skipped.txt`。 |

# 3. 当前真实支持的 C 子集

以下能力以当前严格主流程 `compileStrictFlowchart` 和 `C99SubsetSemanticActions` 为准：

- `int` 函数定义，当前主动作映射只覆盖无参数函数的可靠路径。
- `int` 局部变量声明。
- 局部变量初始化，例如 `int a = 1;`。
- `return expression;`。
- 复合语句块 `{ ... }`。
- `if (...) statement` 和 `if (...) statement else statement` 的 Core AST/IR/emitter 路径。
- `while (...) statement` 的 Core AST/IR/emitter 路径。
- 整数字面量。
- 标识符读取。
- 二元表达式：`+`、`-`、`*`、`/`。
- 关系/相等表达式：`<`、`<=`、`>`、`>=`、`==`、`!=`。
- Jimple 文本输出。
- LLVM IR 输出。
- clang native backend，可选且依赖本机 `clang`。

需要注意：`AstKind`、`ActionRegistry`、`CompileTimeSemanticAnalyzer` 和 IR generator 中存在 `FUNCTION_CALL`、`ASSIGNMENT` 等节点支撑，但当前严格主流程的 `C99SubsetSemanticActions` 尚未完整把 `c99.y` 中函数调用、带参数函数、赋值表达式全部映射到这些 Core AST 节点。因此它们不能写成当前默认主流程已验证支持。

# 4. 当前不支持内容

当前项目不支持完整 C99。以下内容不属于当前真实主流程能力：

- pointer。
- array。
- struct/union/enum 的语义建模。
- typedef。
- float/double/char 等完整类型系统。
- string literal 的真实字符串语义。
- switch。
- for。
- do while。
- break。
- continue。
- goto。
- 函数参数在严格 action-tree 主流程中的完整生成与调用联动。
- 函数调用在严格 action-tree 主流程中的完整生成与调用联动。
- 多声明列表的完整语义，例如 `int a, b;`。
- 赋值语句在严格 action-tree 主流程中的完整语义动作映射。
- 逻辑与/或、位运算、移位、取模在 LLVM/Jimple emitter 中的完整输出。
- 完整 Jimple/Soot class 生成。当前 Soot 是可选接入，默认没有 `SOOT_JAR` 时跳过。
