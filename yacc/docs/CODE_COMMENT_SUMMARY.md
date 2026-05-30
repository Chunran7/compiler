# 代码注释变更总结

## 注释覆盖文件

本次只增加中文 Javadoc 和关键算法注释，没有修改业务逻辑。

### Yacc 规则解析与表构造

- `src/main/java/com/example/compiler/yacc/parser/YaccParser.java`
  - 说明 c99.y 如何被解析成 Grammar。
  - 注释 `%token/%start/%left/%right/%nonassoc`、字符终结符转换、语义动作改写为 `__ACT_n -> ε`、顶层规则切分等关键逻辑。

- `src/main/java/com/example/compiler/yacc/generator/SeuYaccGenerator.java`
  - 说明 YaccParser、FIRST、LR(1)、LALR、ParseTable 的串联关系。

- `src/main/java/com/example/compiler/yacc/grammar/Grammar.java`
  - 说明 Grammar 保存终结符、非终结符、产生式、开始符号、EOF 和优先级信息。
  - 补充产生式优先级计算规则说明。

- `src/main/java/com/example/compiler/yacc/grammar/Production.java`
  - 说明 Production id、语义动作文本、优先级信息与 REDUCE/语法树的关系。

- `src/main/java/com/example/compiler/yacc/first/FirstSetCalculator.java`
  - 说明 FIRST 集不动点算法，以及 LR(1) closure 中 FIRST(βa) 的用途。

- `src/main/java/com/example/compiler/yacc/lr1/LR1Item.java`
  - 说明 LR(1) 项目的形式、点位置和 lookahead。

- `src/main/java/com/example/compiler/yacc/lr1/ClosureBuilder.java`
  - 说明 closure 和 goto 算法。

- `src/main/java/com/example/compiler/yacc/lr1/CanonicalCollectionBuilder.java`
  - 说明规范 LR(1) 项目集族的 BFS 构造流程。

- `src/main/java/com/example/compiler/yacc/lalr/LALRConverter.java`
  - 说明按 LR(0) core 合并 LR(1) 状态形成 LALR 的思想。

- `src/main/java/com/example/compiler/yacc/table/ParseTableBuilder.java`
  - 说明 ACTION/GOTO 表生成和 shift/reduce、reduce/reduce 冲突处理。

- `src/main/java/com/example/compiler/yacc/table/ParseTable.java`
  - 说明 ACTION/GOTO 表在运行时 parser 中的作用。

- `src/main/java/com/example/compiler/yacc/runtime/ParserDriver.java`
  - 说明表驱动语法分析的状态栈、符号栈、AST 栈、shift/reduce/accept。

- `src/main/java/com/example/compiler/yacc/emitter/ParserProgramEmitter.java`
  - 说明当前仓库实际生成 Java parser 源码，而不是 C 版 yyparse.c，并指出后续迁移到 C emitter 的方向。

### Core AST、语义检查与 IR

- `src/main/java/com/example/compiler/yacc/ast/AstNode.java`
  - 说明 parse tree/action-tree 节点、语义动作节点和 productionId。

- `src/main/java/com/example/compiler/yacc/ast/CoreAstNode.java`
  - 说明 Core AST 和 Parse Tree 的区别。

- `src/main/java/com/example/compiler/yacc/ast/C99AstNormalizer.java`
  - 说明完整 c99.y 语法树到 MiniC 子集 Core AST 的转换边界。

- `src/main/java/com/example/compiler/semantic/SemanticActionEngine.java`
  - 说明编译时语义分析和运行时三地址 IR 生成的协调关系。

- `src/main/java/com/example/compiler/semantic/TranslationSchemeExecutor.java`
  - 说明带动作语法树中 `$$/$1` 翻译模式的执行方式。

- `src/main/java/com/example/compiler/semantic/CompileTimeSemanticAnalyzer.java`
  - 说明 Core AST 构建、SymbolTable 建立和静态语义检查范围。

- `src/main/java/com/example/compiler/semantic/SymbolTable.java`
  - 说明变量作用域栈、函数表、重复声明和查找策略。

- `src/main/java/com/example/compiler/ir/YaccIrBridge.java`
  - 说明 ParseResult/AstNode 到 SemanticResult/IrGenerationResult 的桥接作用。

- `src/main/java/com/example/compiler/ir/ThreeAddressIrGenerator.java`
  - 说明 Core AST 到三地址 IR 的动态语义翻译。

- `src/main/java/com/example/compiler/ir/LlvmLikeTextEmitter.java`
  - 说明三地址 IR 到 LLVM-like 文本的发射规则。

- `src/main/java/com/example/compiler/semantic/emitter/CSemanticProgramEmitter.java`
  - 说明 Core AST -> yysemantic.c -> gcc -> yysemantic -> LLVM IR 的课程流程对应关系。

- `src/main/java/com/example/compiler/Compiler.java`
  - 说明当前仓库真实主入口、Java 内部路线和 generated C semantic 路线。

## 注释覆盖的关键算法

- c99.y 的声明区和规则区解析。
- 字符终结符到命名 token 的转换。
- 语义动作块到空产生式动作节点的改写。
- FIRST 集和 nullable 计算。
- LR(1) closure / goto。
- 规范 LR(1) 项目集族构造。
- LALR 状态合并。
- ACTION/GOTO 表生成。
- shift/reduce 和 reduce/reduce 冲突处理。
- 表驱动 parser 的移进、规约、接受。
- Parse Tree 到 Core AST 的归一化。
- 符号表作用域维护。
- 重复声明、未声明变量、未定义函数、实参数量检查。
- Core AST 到三地址 IR。
- LLVM-like IR 文本发射。
- yysemantic.c 生成和运行输出 LLVM IR 的设计。

## 是否改动业务逻辑

没有。所有修改都是注释和文档层面的说明，没有改变控制流、数据结构字段、算法判断条件、测试逻辑或生成结果。
