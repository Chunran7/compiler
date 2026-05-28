# SeuYacc MiniC Flowchart Compiler

本项目是一个基于 LEX/YACC 思路的课程编译流程项目。它不是完整 C99 编译器；当前真实目标是让老师图片中的主控制流能够被代码、测试和生成物证明，并在语义/IR 阶段支持 MiniC/C 子集。

## 当前真实主流程

```text
resources/c99.l
-> CLexerToolchainEmitter
-> yylex.c
-> gcc
-> yylex
-> tokens.txt

resources/c99.y
-> SeuYaccGenerator / LALR ParseTable
-> CParserProgramEmitter
-> yyparse.c
-> gcc
-> yyparse
-> action-tree.txt

action-tree.txt
-> AstTreeCodec
-> SemanticActionEngine
-> TranslationSchemeExecutor
-> Core AST
-> CompileTimeSemanticAnalyzer
-> SymbolTable

Core AST
-> CSemanticProgramEmitter
-> yysemantic.c
-> gcc
-> yysemantic
-> output.ll

Core AST / preliminary IR
-> JimpleTextEmitter
-> output.jimple
```

`NativeBackend` 可选调用 `clang`，从 `output.ll` 生成 `validate.o`、`output.s`、`output.o` 和 `native-executable`。`SootInvoker` 可选调用 Soot；默认没有 `SOOT_JAR` 或没有请求 bytecode 输出时只生成 `soot-skipped.txt`。

## 运行

默认验证：

```bash
mvn test
```

项目脚本：

```bash
bash run-tests.sh
```

手动运行命令行 pipeline：

```bash
java -cp target/classes com.example.compiler.CompilerPipeline \
  --lex resources/c99.l \
  --yacc resources/c99.y \
  --source src/test/resources/pipeline/minimal.c \
  --out generated/strict-flowchart-run \
  --native-backend \
  --run-native
```

## 默认测试

当前默认 `mvn test` 只验证真实主流程：

- `FlowchartPipelineEndToEndTest`：生成并编译 `yylex.c`、`yyparse.c`、`yysemantic.c`，运行三个可执行文件，检查 `tokens.txt`、`action-tree.txt`、`core-ast.txt`、`symbol-table.txt`、`output.ll`、`output.jimple`、trace 和 evidence。
- `NativeBackendEndToEndTest`：在 `clang` 可用时，验证 `output.ll` 能生成汇编、目标文件、本机可执行文件，并运行得到样例程序返回值。

## generated 输出结构

默认测试会生成：

```text
generated/strict-flowchart-test/
  00-input/       c99.l、c99.y、test.c
  01-lex/         yylex.c、yylex、tokens.txt
  02-yacc/        yyparse.c、yyparse、action-tree.txt
  03-semantic/    core-ast.txt、symbol-table.txt、yysemantic.c、yysemantic
  04-ir/          output.ll、output.jimple
  05-soot/        soot-skipped.txt 或 soot-output/
  commands.log
  pipeline-trace.json
  FLOWCHART_EVIDENCE.md

generated/native-backend-test/
  同上，并额外包含 06-native/
  06-native/      validate.o、output.s、output.o、native-executable、native-backend-trace.json、native-backend-report.md
```

旧的根目录 demo 输出已经移除；`generated/` 下只保留当前测试/主流程会重新生成的产物。

## 当前真实支持的 C 子集

以严格主流程 `compileStrictFlowchart` 为准，当前可靠支持：

- `int` 无参数函数定义。
- `int` 局部变量声明。
- 局部变量初始化。
- `return expression;`。
- `{ ... }` 语句块。
- `if`、`if/else`、`while` 的 Core AST/IR/emitter 路径。
- 整数字面量和标识符读取。
- `+`、`-`、`*`、`/`。
- `<`、`<=`、`>`、`>=`、`==`、`!=`。
- LLVM IR 文本输出。
- Jimple 文本输出。

代码中有 `FUNCTION_CALL`、`ASSIGNMENT` 等 Core AST 支撑，但当前严格主流程的语义动作映射还没有完整接通函数参数、函数调用和赋值表达式，不能作为已完成能力答辩。

## 当前不支持

当前不支持完整 C99，包括 pointer、array、struct/union/enum、typedef、float/double、switch、for、do while、break、continue、goto、完整函数参数/调用、完整赋值语句、多声明列表、完整 Jimple/Soot class 生成。

## 文档

- [项目真实 Goal 与范围](docs/PROJECT_GOAL_AND_SCOPE.md)
- [最终项目控制流](docs/FINAL_PROJECT_CONTROL_FLOW.md)
