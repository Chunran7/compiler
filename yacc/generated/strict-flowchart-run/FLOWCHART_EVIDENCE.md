# Flowchart Evidence

## 基础流程证据

| 图片节点 | 实际模块/命令 | 生成物 |
|---|---|---|
| 词法规则 c99.l | 00-input/c99.l | - |
| 词法分析程序生成器 LEX | SeuLexParser / NFA / DFA / Minimize / CLexerProgramEmitter | 01-lex/yylex.c |
| C 编译器 | gcc yylex.c -o yylex | 01-lex/yylex |
| 可执行词法分析程序 | ./yylex test.c tokens.txt | 01-lex/tokens.txt |
| 语法规则 c99.y | 00-input/c99.y | - |
| 语法分析程序生成器 YACC | YaccParser / LR(1) / LALR / ParseTable / CParserProgramEmitter | 02-yacc/yyparse.c |
| C 编译器 | gcc yyparse.c -o yyparse | 02-yacc/yyparse |
| 可执行语法分析程序 | ./yyparse tokens.txt action-tree.txt | 02-yacc/action-tree.txt |
| 带语义动作节点的语法树 | AstTreeCodec 读取 action-tree.txt | 03-semantic/core-ast.txt |
| 语义引擎 | SemanticActionEngine / TranslationSchemeExecutor | 03-semantic/core-ast.txt |
| 语义检查 | CompileTimeSemanticAnalyzer / SymbolTable | 03-semantic/symbol-table.txt |
| 中间代码生成程序 | CSemanticProgramEmitter -> gcc yysemantic.c -> yysemantic | 04-ir/output.ll |
| Jimple codes | JimpleTextEmitter | 04-ir/output.jimple |

## 老师图片后端接入

| 后端节点 | 实际模块/命令 | 生成物 |
|---|---|---|
| Jimple codes | 04-ir/output.jimple | - |
| Soot 后端 | SootInvoker，依赖 SOOT_JAR | 05-soot/soot-output 或 05-soot/soot-skipped.txt |
| Java bytecode/class | SOOT_JAR 存在时生成 | class 输出目录 |

## 项目扩展后端 Native Backend

| 扩展后端节点 | 实际模块/命令 | 生成物 |
|---|---|---|
| LLVM IR | 04-ir/output.ll | - |
| Clang IR 校验 | clang -c output.ll -o validate.o | 06-native/validate.o |
| 生成汇编 | clang -S output.ll -o output.s | 06-native/output.s |
| 生成目标文件 | clang -c output.ll -o output.o | 06-native/output.o |
| 链接本机可执行文件 | clang output.o -o native-executable | 06-native/native-executable |
| 运行本机程序 | ./native-executable | exitCode / stdout / stderr |
