# Yacc 与中间代码生成模块设计报告材料

## 1. 模块设计目标

本人负责的模块主要包括 Yacc 语法分析程序生成、语法树到核心抽象语法树的转换、符号表与语义检查，以及中间代码生成。Lex 模块作为前置输入，仅通过 token 序列与本模块衔接，不作为本文重点。

本模块的目标是：使用完整 `resources/c99.y` 构造 C99 语法分析能力；在语法分析成功后，将完整语法树归一化为项目定义的 C 子集 Core AST；完成函数、变量、作用域和调用参数等静态语义检查；最后生成 LLVM 风格中间代码，并支持通过 `yysemantic.c` 路线由 C 程序运行输出 LLVM IR。

## 2. 总体流程

系统整体流程可以概括为：

```text
Lex 输出 token 序列
resources/c99.y -> YaccParser -> Grammar
Grammar -> FIRST -> LR(1) -> LALR -> ParseTable
token 序列 + ParseTable -> ParserDriver -> AstNode 语法树
AstNode -> C99AstNormalizer -> Core AST
Core AST -> CompileTimeSemanticAnalyzer -> SymbolTable
Core AST -> ThreeAddressIrGenerator -> IrInstruction
IrInstruction -> LlvmLikeTextEmitter -> LLVM-like IR
Core AST -> CSemanticProgramEmitter -> yysemantic.c -> gcc -> yysemantic -> LLVM IR
```

其中，完整 C99 语法主要用于语法识别；后续语义和 IR 阶段限定于课程设计需要的 C 子集。

## 3. Yacc 模块设计

Yacc 模块以前端解析器 `YaccParser` 为入口。它读取 yacc/bison 风格规则文件，识别 `%token`、`%start`、`%left`、`%right` 和 `%nonassoc` 等声明，并将规则区转换为内部 `Grammar` 对象。

`Grammar` 包含终结符、非终结符、产生式、开始符号、增广开始符号和优先级信息。`Production` 用 id 标识产生式，该 id 后续被 REDUCE 动作和语法树节点引用。对于 yacc 规则中的字符终结符，例如 `';'`、`'('`，系统统一转换为 `SEMI`、`LPAREN` 等内部 token 名。

语义动作块 `{ ... }` 被转换为合成空产生式 `__ACT_n -> ε`，这样语法分析阶段可以保留动作节点的位置，语义阶段再解释动作。

## 4. LR/LALR 分析表生成

分析表生成分为三个步骤。

第一步是 FIRST 集计算。`FirstSetCalculator` 通过不动点迭代计算每个符号的 FIRST 集和非终结符可空性。FIRST 集用于 LR(1) closure 中计算 lookahead。

第二步是 LR(1) 项目集构造。`LR1Item` 表示 `A -> α · β, a`。`ClosureBuilder` 实现 closure 和 goto：closure 根据点后的非终结符扩展项目，goto 表示读入一个语法符号后的状态转移。`CanonicalCollectionBuilder` 从增广开始项目出发，广度优先生成所有 LR(1) 状态。

第三步是 LALR 合并。`LALRConverter` 将拥有相同 LR(0) core 的 LR(1) 状态合并，并合并 lookahead。这样可以减少完整 C99 文法带来的状态数量，更接近传统 yacc 的实现方式。

最终，`ParseTableBuilder` 根据项目集生成 ACTION/GOTO 表。终结符转移产生 shift，完成项目产生 reduce，增广开始产生式完成且展望 EOF 时产生 accept，非终结符转移写入 GOTO。若出现 shift/reduce 冲突，系统根据优先级和结合性解决；reduce/reduce 冲突直接报告错误。

## 5. 语义动作执行

当前系统用 `AstNode` 表示语法树和语义动作节点。普通叶子节点保存 token 类型和词素，非终结符节点保存子节点和规约产生式编号，语义动作节点保存 actionCode。

`TranslationSchemeExecutor` 提供了执行翻译模式的能力，可以解析 `$$ = $1;` 或 `$$ = makeBinary("+", $1, $3);` 这类动作表达式，通过 `ActionRegistry` 调用动作函数并填充节点的 semanticValue。当前主线更多采用 `C99AstNormalizer` 直接从完整语法树抽取 Core AST，但该执行器保留了课程设计中“带动作语法树 -> 语义动作执行”的扩展接口。

## 6. Core AST 设计

完整 C99 parse tree 节点层级非常细，适合证明语法分析正确，但不适合直接生成 IR。因此项目引入 `CoreAstNode`。Core AST 只保留语义阶段需要的信息，包括：

- 程序、函数、main 函数
- 参数、代码块
- 变量声明、赋值、表达式语句、return
- if、while
- 二元表达式、函数调用
- 标识符、整数字面量

`C99AstNormalizer` 从 `translation_unit` 开始遍历语法树，将函数定义、语句、表达式等结构转换为 Core AST。该类明确限定在 C 子集语义，不追求完整 C99 语义覆盖。

## 7. 符号表与语义检查

`SymbolTable` 使用作用域栈管理变量符号，使用全局 map 管理函数符号。进入函数和块时创建新作用域，离开时弹出作用域。变量声明只检查当前作用域是否重复，变量解析从内层作用域向外层查找。

`CompileTimeSemanticAnalyzer` 执行静态语义检查。它先通过 `C99AstNormalizer` 得到 Core AST，然后登记所有函数，检查是否存在 `main`，再逐个分析函数体。支持的检查包括：函数重复定义、变量重复声明、变量未声明使用、调用未定义函数、函数实参数量不匹配，以及 Core AST 结构合法性。

## 8. 中间代码生成

项目提供两条中间代码生成路线。

第一条是 Java 内部路线：`ThreeAddressIrGenerator` 将 Core AST 转为 `IrInstruction` 列表，表达函数边界、赋值、二元运算、调用、标签、跳转和返回。`LlvmLikeTextEmitter` 再将这些指令发射为 LLVM 风格文本。

第二条是课程流程更贴近的生成 C 程序路线：`CSemanticProgramEmitter` 根据 Core AST 生成 `yysemantic.c`。该 C 程序被 gcc 编译后，运行时打印 LLVM IR。这样就形成了“语义引擎生成中间代码生成程序，再运行该程序生成中间代码”的结构。

## 9. LLVM IR 后端

LLVM 风格 IR 中，局部变量通过 `alloca` 建立栈槽，赋值通过 `store` 写入，变量读取通过 `load` 完成，算术表达式通过 `add/sub/mul/sdiv` 表示，比较通过 `icmp` 表示，控制流通过 `br` 和 label 表示，函数返回通过 `ret` 表示。

例如 `return a + b;` 会先读取 a 和 b，再生成 `add i32`，最后 `ret i32` 返回结果。

## 10. Jimple/Soot 后端

当前仓库没有实际 `JimpleTextEmitter` 和 `SootInvoker` 类。因此 Jimple/Soot 目前应作为后续扩展方向描述：理想流程是从 Core AST 或三地址 IR 生成 Jimple 文本，再在配置 `SOOT_JAR` 时调用 Soot 生成 Java class 文件。

## 11. Native Backend 扩展

当前仓库没有独立 `NativeBackend` 类，但 `Compiler.compileViaGeneratedC` 支持可选 clang 调用：当调用者提供 LLVM IR 文件和可执行文件路径时，系统将 `yysemantic` 输出写入 `.ll`，再调用 clang 生成本机可执行文件。

这一路线不是实现 clang，而是复用本机 LLVM/Clang 工具链。

## 12. 测试与验证

仓库中 `mvn test` 主要用于编译验证，因为项目没有引入 JUnit。真正执行全量流程的是 `bash run-tests.sh`，它编译所有源码和测试源码后运行 `TotalIntegrationTest`。

测试覆盖内容包括：C99 grammar 解析、表达式优先级和结合性、LALR 状态合并、冲突报告、语义动作模式解析、c99 lexer token 覆盖、lexer/parser 集成、C99 parser-only 样例、MiniC 语义/IR 流程、`yysemantic.c` 生成和运行、LLVM IR 到本机可执行文件、AST markdown 输出，以及重复声明、未声明变量、未定义函数、参数数量不匹配等语义错误。

## 13. 当前不足与改进方向

当前项目的主要不足是：语法阶段虽然使用完整 `c99.y`，但语义和 IR 仅支持 C 子集；没有真正落盘并反序列化 `action-tree.txt` 的 `AstTreeCodec`；没有 C 版 `yyparse.c` emitter；没有 Jimple/Soot 后端；Native 后端只是轻量 clang 调用而非完整 trace/report 框架。

后续改进方向包括：实现真正 C 版 parser emitter，补充 action-tree 编码与 codec，增加 Jimple/Soot 后端，完善命令行流水线和 trace 文件，并逐步扩展指针、数组、结构体、for/switch 等 C99 语义。
