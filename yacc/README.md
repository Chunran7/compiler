# SeuYacc Compiler Pipeline

本项目实现一个基于 LEX/YACC 思路的 C 子集编译流程。词法和语法阶段以完整 `resources/c99.l`、`resources/c99.y` 为输入生成 C 版本分析程序；语义和 IR 阶段只支持项目定义的 C 子集。

## 项目目标

- 使用完整 `c99.l` 生成 `yylex.c`，经 `gcc` 编译为 `yylex`。
- 使用完整 `c99.y` 生成 `yyparse.c`，经 `gcc` 编译为 `yyparse`。
- 通过语义动作树构造 Core AST，完成 SymbolTable 和语义检查。
- 生成 `yysemantic.c`，经 `gcc` 编译为 `yysemantic`，运行后输出 LLVM IR。
- 从 Core AST 生成 Jimple 文本。
- Jimple 可选接入 Soot，生成 Java bytecode/class。
- LLVM IR 额外接入 Clang Native Backend，生成汇编、目标文件和本机可执行文件。

## 流程图文字版

基础流程：

```text
c99.l -> LEX -> yylex.c -> gcc -> yylex -> tokens.txt
c99.y -> YACC -> yyparse.c -> gcc -> yyparse -> action-tree.txt
action-tree.txt -> Core AST / SymbolTable -> yysemantic.c -> gcc -> yysemantic
yysemantic -> output.ll
Core AST -> output.jimple
```

老师图片后端：

```text
output.jimple -> Soot -> Java bytecode/class
```

项目扩展后端：

```text
output.ll -> clang IR 校验 -> output.s -> output.o -> native-executable
```

## 运行方式

推荐直接运行：

```bash
bash run-tests.sh
```

或运行默认 Maven 测试：

```bash
mvn test
```

手动运行完整命令行 pipeline：

```bash
java -cp target/classes com.example.compiler.CompilerPipeline \
  --lex resources/c99.l \
  --yacc resources/c99.y \
  --source src/test/resources/pipeline/minimal.c \
  --out generated/strict-flowchart-run \
  --native-backend \
  --run-native
```

回归测试使用：

```bash
mvn test -Pall-tests
```

## 测试结果说明

默认 `mvn test` 运行：

- `FlowchartPipelineEndToEndTest`：验证基础主流程和 Soot 可选后端 skipped/输出。
- `NativeBackendEndToEndTest`：验证 LLVM IR 经 clang 生成 `validate.o`、`output.s`、`output.o`、`native-executable`，并运行得到 `exitCode == 3`。

`mvn test -Pall-tests` 会额外运行 regression 测试。

## 输出目录

`generated/strict-flowchart-run/` 的结构：

```text
00-input/       c99.l、c99.y、test.c
01-lex/         yylex.c、yylex、tokens.txt
02-yacc/        yyparse.c、yyparse、action-tree.txt
03-semantic/    core-ast.txt、symbol-table.txt、yysemantic.c、yysemantic
04-ir/          output.ll、output.jimple
05-soot/        soot-output/ 或 soot-skipped.txt
06-native/      validate.o、output.s、output.o、native-executable、native-backend-trace.json、native-backend-report.md
commands.log    真实执行过的外部命令
pipeline-trace.json
FLOWCHART_EVIDENCE.md
```

## 当前限制

- Yacc 阶段使用完整 `resources/c99.y` 生成语法分析程序。
- 语义分析和 IR 生成只支持项目定义的 C 子集。
- Soot 后端依赖环境变量 `SOOT_JAR`。
- Native Backend 依赖本机 `clang`。
- 目前不是完整 C99 编译器。
