# SeuLex 词法分析器设计报告

---

## 1. 编译对象与编译功能

### 1.1 编译对象：C99 语言子集的词法描述

本项目的编译对象为 **C99 标准（ISO/IEC 9899:1999）定义的 C 语言词法子集**。SeuLex 作为词法分析器生成器，以 `c99.l` 词法规范文件为输入，生成能够识别该语言全部词法单元的运行时词法分析器。

`c99.l` 中定义的词法单元共 87 类（不含已规划但暂未实现的 `TYPE_NAME`），分为以下四大类别：

#### 1.1.1 关键词类（37 个）

C99 全部关键字，从 `auto`、`_Bool`、`break` 到 `volatile`、`while`，涵盖存储类说明符（`auto`、`register`、`static`、`extern`、`typedef`）、类型说明符（`int`、`char`、`float`、`double`、`void`、`_Bool`、`_Complex`、`_Imaginary`、`struct`、`union`、`enum`、`short`、`long`、`signed`、`unsigned`）、类型限定符（`const`、`restrict`、`volatile`）、流程控制关键字（`if`、`else`、`switch`、`case`、`default`、`while`、`do`、`for`、`break`、`continue`、`goto`、`return`）以及 `sizeof`、`inline`。

#### 1.1.2 运算符类（21 个多字符运算符）

包括复合赋值运算符（`>>=`、`<<=`、`+=`、`-=`、`*=`、`/=`、`%=`、`&=`、`^=`、`|=`）、移位运算符（`>>`、`<<`）、自增自减（`++`、`--`）、成员访问（`->`）、逻辑运算符（`&&`、`||`）和比较运算符（`<=`、`>=`、`==`、`!=`）。

#### 1.1.3 字面量与标识符类（4 个多字符 Token）

- **IDENTIFIER**：符合 C99 标识符规范的字符串（字母/下划线开头，后接字母/数字/下划线），由 `check_type()` 返回
- **CONSTANT**：涵盖十进制整数、十六进制整数（`0x`/`0X` 前缀）、八进制整数（`0` 前缀）、十进制浮点数（含小数点或指数 `E`/`e`）、十六进制浮点数（含 `P`/`p` 指数）、字符常量（`'a'` 形式及转义序列与宽字符 `L'...'`），各类后缀（`U`、`L`、`LL`、`F` 等）均被正确匹配
- **STRING_LITERAL**：双引号包围的字符串，支持转义序列 `\"`、`\\` 等，以及 `L"..."` 宽字符串前缀
- **ELLIPSIS**：`...` 变参省略号

#### 1.1.4 单字符 Token（25 个）

包括分号 `;`、花括号 `{` `}`、逗号 `,`、冒号 `:`、等号 `=`、圆括号 `(` `)`、方括号 `[` `]`、点号 `.`、算术/逻辑/位运算符（`+` `-` `*` `/` `%` `&` `|` `^` `!` `~`）、比较运算符（`<` `>` `?`）。其中花括号和方括号同时支持 C99 双拼符（digraph）形式：`<%` / `%>` 等价于 `{` / `}`，`<:` / `:>` 等价于 `[` / `]`。

#### 1.1.5 正规定义（宏定义）

`c99.l` 定义段中给出了 6 个宏定义，作为正则表达式中可复用的基本组成单元：

| 宏名 | 定义 | 说明 |
|------|------|------|
| `D` | `[0-9]` | 十进制数字 |
| `L` | `[a-zA-Z_]` | 字母（含下划线） |
| `H` | `[a-fA-F0-9]` | 十六进制数字 |
| `E` | `([Ee][+-]?{D}+)` | 十进制指数部分 |
| `P` | `([Pp][+-]?{D}+)` | 十六进制指数部分 |
| `FS` | `(f\|F\|l\|L)` | 浮点数后缀 |
| `IS` | `((u\|U)\|(u\|U)?(l\|L\|ll\|LL)\|(l\|L\|ll\|LL)(u\|U))` | 整数后缀 |

这些宏定义支持嵌套引用（如 `{E}` 中引用了 `{D}`），经宏展开后拼入规则的正则表达式中。

#### 1.1.6 需跳过的内容

- **块注释** `/* ... */`：由专用的 `comment()` 函数处理，支持嵌套边界检测
- **行注释** `// ... \n`：消费从 `//` 到行尾的所有字符
- **空白字符**：空格 ` `、制表符 `\t`、垂直制表符 `\v`、换行符 `\n`、换页符 `\f` 均被跳过，同时通过 `count()` 函数维护列号追踪
- **其他未匹配字符**：由通配规则 `.` 捕获，当前实现中该规则动作体为空（静默跳过），留待后续扩充错误报告

---

### 1.2 编译功能

SeuLex 由**词法分析器生成器**和**运行时词法分析器**两部分组成，其完整功能栈及对应的程序单元如下表所示：

| 功能模块 | 功能描述 | 对应程序单元 |
|----------|----------|-------------|
| **Lex 文件解析** | 读取 `.l` 格式词法规范文件，完成定义段宏提取与展开、规则段正则/动作对解析、C 语义动作到 Java 代码的翻译 | [SeuLexParser.java](src/main/java/com/example/compiler/lex/SeuLexParser.java) |
| **C→Java 代码翻译** | 将定义段 C 声明和用户子程序段 C 函数结构翻译为 Java 字段与方法，支持 C 类型映射和 Flex 惯用语转换 | [CToJavaTranslator.java](src/main/java/com/example/compiler/lex/CToJavaTranslator.java) |
| **正则表达式转换** | 将用户书写的正则表达式经多趟转换（引号处理 → `+`/`?` 运算符展开 → 字符集展开 → 显式连接符插入 → 中缀转后缀）输出后缀表达式 | [RegexConverter.java](src/main/java/com/example/compiler/lex/RegexConverter.java) |
| **NFA 构造** | 采用 Thompson 构造法，从后缀正则表达式构建非确定有限自动机 | [NfaBuilder.java](src/main/java/com/example/compiler/lex/NfaBuilder.java)（含 `NfaState`、`NfaFragment` 内部类） |
| **多规则 NFA 合并** | 为每条词法规则分别构建 NFA，通过 ε 边统一连接到全局起始状态，在接受态上标记 ruleId 实现优先级 | [NfaManager.java](src/main/java/com/example/compiler/lex/NfaManager.java) |
| **NFA → DFA 确定化** | 子集构造法（ε-closure + move），将合并后的 NFA 转换为等价的确定有限自动机 | [NfaToDfaConverter.java](src/main/java/com/example/compiler/lex/NfaToDfaConverter.java)（`convert()` 方法） |
| **DFA 最小化** | 等价类划分法（Hopcroft 算法），按接受规则分组后反复细化至不动点，得到状态数最少的 DFA | [NfaToDfaConverter.java](src/main/java/com/example/compiler/lex/NfaToDfaConverter.java)（`minimize()` 方法） |
| **代码生成** | 将最小化 DFA 的状态转移表序列化为二进制 `lexer_tables.dat` 文件，同时生成 `GeneratedLexer.java` 运行时词法分析器源码 | [CodeGenerator.java](src/main/java/com/example/compiler/lex/CodeGenerator.java) |
| **运行时词法分析** | 加载 DFA 转移表，按最长匹配原则逐字符模拟 DFA 运行，抵达接受态后回溯至最长匹配点，根据 ruleId 执行对应语义动作产生 Token | [GeneratedLexer.java](src/main/java/com/example/compiler/lex/GeneratedLexer.java) |
| **统一入口编译** | 读取源文件，调用 GeneratedLexer 产出 Token 流，传递给后续的语法分析和语义分析阶段 | [Compiler.java](src/main/java/com/example/compiler/Compiler.java) |
| **单元测试** | 对 GeneratedLexer 进行白盒测试，覆盖基本 Token、关键字、运算符、标识符、常量、多字符 Token、最长匹配、空白跳过等类别 | [LexerTest.java](src/test/java/com/example/compiler/test/LexerTest.java) |
| **高级测试** | 验证浮点数字面量、字符常量、字符串字面量、整数后缀、行注释的识别能力 | [LexerAdvancedTest.java](src/test/java/com/example/compiler/test/LexerAdvancedTest.java) |

---

## 2. 主要特色

SeuLex 的设计与实现有以下五个突出亮点：

### 2.1 完整的"生成器"架构——而非硬编码词法分析器

SeuLex 并非针对 C99 硬编码一个词法分析器，而是一个**通用的词法分析器自动生成器**。它以 `.l` 格式的词法规范文件为输入，通过"解析 → 正则转换 → NFA 构造 → DFA 确定化 → DFA 最小化 → 代码生成"的完整流水线，自动生成目标语言的运行时词法分析器源码。这一架构与经典工具 Lex/Flex 的设计理念一致，理论上可以接受任意 Lex 规范文件生成对应的词法分析器。

### 2.2 C 规范到 Java 运行时的跨语言桥接

`c99.l` 文件本身是面向 C 语言编写的（动作代码使用 C 语法，如 `return(AUTO)`、`#include` 等），而整个 SeuLex 运行在 Java 17 环境中。SeuLex 通过以下机制优雅地解决了跨语言问题：

- **SeuLexParser.translateAction()**：在解析规则时，用正则表达式将 `return(TOKEN_NAME)` 自动翻译为 `return new Token(TokenType.TOKEN_NAME, ...)`，将 `return('c')` 翻译为 `return new Token(TokenType.forChar('c'), ...)`
- **CToJavaTranslator**：采用结构化翻译策略处理定义段 C 声明和用户子程序段 C 函数——先按花括号/分号边界拆分顶层构造，再对函数签名和函数体分别翻译（C 类型映射、`input()` EOF 语义适配、`yytext` 遍历循环重写、`fprintf`+`exit` → `throw` 等），生成等价的 Java 字段和方法
- **Token 接口统一**：通过 `tokens-spec.md` 和 `TokenType.java` 枚举，确保词法分析器产出的 Token 名称与语法分析器声明的终结符名称严格一致

### 2.3 规范的正则表达式引擎

`RegexConverter` 实现了一个小而完整的正则表达式处理管线，支持：

- **六趟转换**：逐层消解语法糖，每趟关注一个关注点，逻辑清晰
- **字符集 `[...]`** 展开为交替式 `(a|b|c|...)`
- **否定字符集 `[^...]`** 展开为补集（在 ASCII 可打印范围 0x01-0x7E 内求补），这一功能对 `//` 行注释的正确匹配至关重要
- **`+` 和 `?` 运算符**展开为基本形式（`a+` → `aa*`，`a?` → `(a|ε)`）
- **显式连接符**自动插入，消除歧义
- **调度场算法**中缀转后缀，正确处理运算符优先级（`*` > `·` > `|`）

### 2.4 标准的 Lex 优先级规则实现

词法分析中的关键语义——**优先匹配**和**最长匹配**——均正确实现：

- **优先匹配**：在 `DfaState` 构造时，若一个 DFA 状态包含多个 NFA 接受态，选择 `ruleId` 最小的（即 `.l` 文件中出现最早的规则）
- **最长匹配**：在 `GeneratedLexer.nextToken()` 中，持续跟踪最近遇到的接受态（`last_accept_state`）及其当时的输入长度（`last_accept_len`），当 DFA 状态转移失败后回溯到该点

### 2.5 系统化的测试覆盖

针对词法分析器的测试不是零散的随手测试，而是按类别、有层次地组织了 8 大类 + 5 小类共 **50+ 个断言**：

- 基础 Token → 关键字 → 运算符 → 标识符 → 常量 → 多字符 Token → 最长匹配 → 空白跳过
- 高级：浮点数（十进制/十六进制、小数点/指数/后缀）、字符常量、字符串字面量、整数回归、行注释

每个测试用例都带有中文标签说明，断言失败时能快速定位。

---

## 3. 概要设计与详细设计

### 3.1 概要设计

#### 3.1.1 系统总体架构

SeuLex 的总体架构分为两大阶段：**离线生成阶段**（Generator）和**在线运行阶段**（Runtime）。

```
┌─────────────────────────────────────────────────────────────┐
│                    SeuLex 离线生成阶段                        │
│                                                             │
│  c99.l ──► SeuLexParser ──► RegexConverter ──► NfaManager   │
│               │  宏展开          │ 后缀转换         │ Thompson│
│               │  C→Java翻译     │ 字符集展开       │ 合并      │
│               ▼                 ▼                  ▼          │
│           List<LexRule>    后缀正则串       合并后的 NFA       │
│                                                      │        │
│                          NfaToDfaConverter ◄────────┘        │
│                             │                                  │
│                    ┌───────┴────────┐                          │
│                    ▼                ▼                          │
│               子集构造法       DFA 最小化                       │
│                    │                │                           │
│                    └───────┬────────┘                           │
│                            ▼                                   │
│                    List<DfaState>                              │
│                            │                                   │
│                     CodeGenerator                              │
│                       │        │                                │
│                       ▼        ▼                                │
│              GeneratedLexer.java   lexer_tables.dat            │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                    SeuLex 在线运行阶段                         │
│                                                               │
│  源程序 ──► GeneratedLexer ──► Token 流 ──► ParserDriver      │
│               │  加载 tables.dat                               │
│               │  DFA 模拟                                      │
│               │  最长匹配+回溯                                 │
│               │  switch 分派动作                               │
└─────────────────────────────────────────────────────────────┘
```

#### 3.1.2 模块间调用关系

下图描述生成器各模块之间的调用关系（箭头表示调用方向）：

```
                         LexCompilerMain (main 入口)
                               │
          ┌────────────────────┼────────────────────┐
          │                    │                    │
          ▼                    ▼                    ▼
    SeuLexParser          NfaManager          CodeGenerator
     ·splitLexFile()       ·buildCombinedNfa()   ·generateJava()
     ·parseDefinitions()        │                     │
     ·parseRules()              │                     │
     ·translateAction()    ┌────┴────┐                │
          │                │         │                │
          │                ▼         ▼                │
          │         RegexConverter  InternalNfaBuilder│
          │          ·convert()      ·build()         │
          │          ·...                             │
          │                                           │
          └─────► NfaManager ◄────────────────────────┘
                       │
                       ▼
                NfaToDfaConverter
                 ·convert()      (子集构造法)
                 ·minimize()     (等价类划分法)
                       │
                       ▼
                 CodeGenerator ──── CToJavaTranslator
                  ·generateJava()    ·translateDefinitionBlock()
                                     ·translateUserSubroutines()
                                     ·translateFunction()
                                     ·translateType()
```

#### 3.1.3 数据流向

```
c99.l 源文件
  │
  ├─[定义段]──► SeuLexParser.parseDefinitions()  ──► Map<String,String> regularDefs
  │                                                   (宏名 → 展开后的正则文本)
  │            %{...%} 块 ──► CToJavaTranslator.translateDefinitionBlock() → import/package + Java 字段
  │
  ├─[规则段]──► SeuLexParser.parseRules()  ──► List<LexRule> rules
  │              (含宏展开 + C→Java 翻译)           (id, regex, action)
  │
  └─[用户代码段]  ──► CToJavaTranslator.translateUserSubroutines() → Java 方法源码

List<LexRule> rules
  │
  └──► NfaManager.buildCombinedNfa(rules)
         │  每规则: regex → RegexConverter.convert(regex) → 后缀
         │          后缀 → InternalNfaBuilder.build(后缀) → NfaFragment
         │          标记 accept.ruleId，ε 连到全局起点
         │
         ▼
       NfaState globalStart  (合并后的巨型 NFA)
         │
         ▼
       NfaToDfaConverter.convert(globalStart)
         │  ε-closure + move + 子集构造
         ▼
       List<DfaState> dfaStates  (初始 DFA)
         │
         ▼
       NfaToDfaConverter.minimize(dfaStates)
         │  等价类划分法
         ▼
       List<DfaState> minimizedDfa  (最小 DFA)
         │
         ▼
       CodeGenerator.generateJava(minimizedDfa, rules, definitions, userCode)
         │  遍历 DFA 状态 -> 序列化转移表 (binary)
         │  遍历 rules -> 生成 switch-case 动作分派
         │  CToJavaTranslator → 翻译定义段 C 声明和用户子程序段 C 函数
         ▼
       GeneratedLexer.java + lexer_tables.dat
```

---

### 3.2 详细设计

SeuLex 的生成流水线由 7 个阶段组成，分别对应 7 个核心 Java 源文件/类。以下按流水线顺序，逐一展开每个文件的职责、使用的算法以及关键算法的伪代码表述。

---

#### 3.2.1 阶段一：Lex 文件解析 —— SeuLexParser.java

**对应源文件**：[SeuLexParser.java](src/main/java/com/example/compiler/lex/SeuLexParser.java)

`SeuLexParser` 是整个生成流水线的入口阶段，负责读入 `.l` 格式的词法规范文件（如 `c99.l`），将其解析为结构化的内部表示。它不涉及自动机理论，而是完成"从文本到数据结构"的转换。

##### 3.2.1.1 文件三段分割 —— splitLexFile()

**功能**：将 `.l` 文件内容按 `%%` 分隔符拆分为三个部分——定义段（definitions）、规则段（rules）、用户代码段（user subroutines）。

**算法思路**：利用 Java 正则表达式 `(?m)^%%\\s*` 在多行模式下匹配行首的 `%%`，将文件按 `%%` 分割为至多三段。第 0 段为定义段（宏定义与 C 声明），第 1 段为规则段（正则-动作对），第 2 段为用户子程序段（C 辅助函数）。

##### 3.2.1.2 宏定义解析与迭代展开 —— parseDefinitions()

**功能**：从定义段提取 `NAME translation` 格式的宏（正则定义），完成两阶段处理——先收集所有原始定义，再统一展开宏引用（解决定义顺序依赖问题）。

**算法**：**迭代不动点展开法**

```
算法：expandMacros(input, allDefs)
输入：待展开字符串 input，完整宏定义集合 allDefs
输出：展开所有 {NAME} 引用后的字符串

1.  result ← input
2.  iterations ← 0
3.  repeat:
4.      changed ← false
5.      for each (name, definition) in allDefs:
6.          macro ← "{" + name + "}"
7.          if result contains macro:
8.              result ← result.replace(macro, "(" + definition + ")")
9.              changed ← true
10.     iterations ← iterations + 1
11.     if iterations > 50:  throw 错误("循环引用或嵌套过深")
12. until changed == false
13. return result
```

替换时用括号包裹展开内容，以维持原表达式的运算符优先级。设置 50 次迭代上限，防止循环引用导致无限循环。两阶段策略（先收集后展开）确保了宏定义可以按任意顺序书写，不受前后顺序影响。

##### 3.2.1.3 规则段解析 —— parseRules()

**功能**：将规则段中每行「正则表达式 动作代码」解析为 `LexRule(id, regex, action)` 三元组。

**算法**：**引号/括号感知的智能分割**

```
算法：parseRules()
输入：rulePart（规则段文本）
输出：List<LexRule>

1.  rules ← []
2.  ruleId ← 0
3.  for each line in rulePart:
4.      splitIdx ← -1
5.      inQuotes ← false, inBracket ← false
6.      for i ← 0 to line.length - 1:
7.          c ← line[i]
8.          if c == '"' and not escaped:  inQuotes ← ¬inQuotes
9.          else if c == '[' and not inQuotes and not escaped:  inBracket ← true
10.         else if c == ']' and not inQuotes and not escaped:  inBracket ← false
11.         else if c is whitespace and not inQuotes and not inBracket:
12.             splitIdx ← i; break
13.     if splitIdx ≠ -1:
14.         regex ← line[0..splitIdx]（宏展开）
15.         action ← line[splitIdx..]（去除注释，C→Java 翻译）
16.         rules.add(new LexRule(++ruleId, regex, action))
17. return rules
```

此算法的关键点在于正确识别正则表达式和动作代码的分界——必须**跳过引号内和字符集内的空白字符**，否则 `">>="` 这类含引号的规则会被错误切分。

##### 3.2.1.4 C 动作到 Java 的翻译 —— translateAction()

**功能**：将 `c99.l` 中 C 风格的语义动作（`return(TOKEN_NAME)`、`return('c')`）翻译为 Java 代码。注意，此方法仅处理**规则段内的 action 表达式**（`{ ... }` 内的代码片段）；用户子程序段（第二个 `%%` 之后）中完整 C 函数的翻译由 `CToJavaTranslator` 负责（见 3.2.6.5 节）。

**翻译规则**：

| C 动作 | Java 动作 |
|--------|----------|
| `return(AUTO)` | `return new Token(TokenType.AUTO, new String(yytext, 0, yyleng))` |
| `return(';')` | `return new Token(TokenType.forChar(';'), new String(yytext, 0, yyleng))` |
| `{ comment(); }` | 保持原样（依赖 CToJavaTranslator 在类中生成对应方法） |

使用两个正则替换：`return\(([A-Z_][A-Z_0-9]*)\)` 匹配大写 Token 名称，`return\('(.)'\)` 匹配单字符返回。若 action 已包含 Java 语法（`new Token(` 或 `TokenType.`），则跳过翻译以支持用户直接书写 Java 动作。

---

#### 3.2.2 阶段二：正则表达式转换 —— RegexConverter.java

**对应源文件**：[RegexConverter.java](src/main/java/com/example/compiler/lex/RegexConverter.java)

`RegexConverter` 负责将用户书写的正则表达式字符串，经过多趟确定性变换（deterministic passes），最终输出后缀表达式（逆波兰表示法），供后续 NFA 构造阶段使用。整个转换管线包含 6 趟（pass），每趟关注一个独立的关注点。

##### 3.2.2.1 引号处理 —— processQuotes()

**功能**：将 `"..."` 双引号包裹的字符串内容取出，去掉引号，对内部的正则元字符（`*+|?()[]`）自动添加反斜杠转义，使引号内内容作为字面量参与后续处理。

**算法**：

```
算法：processQuotes(regex)
输入：原始正则表达式字符串
输出：去除引号、元字符已转义的等价表达式

1.  result ← ""
2.  i ← 0
3.  while i < regex.length:
4.      if regex[i] == '"' and not escaped(regex, i):
5.          end ← regex.indexOf('"', i+1)
6.          if end == -1: result.append('"'); i++    // 未闭合引号，保持原样
7.          else:
8.              for j ← i+1 to end-1:
9.                  if isRegexMeta(regex[j]): result.append('\\')
10.                 result.append(regex[j])
11.             i ← end + 1
12.     else: result.append(regex[i]); i++
13. return result
```

例如 `">>="` → `>>=`（`>` 不是元字符，直接保留）。

##### 3.2.2.2 量词展开 —— processPlusAndQuestion()

**功能**：将正则扩展运算符 `+`（一次或多次）和 `?`（零次或一次）展开为基本运算符的组合：`a+` → `aa*`，`a?` → `(a|ε)`。

**为什么必须在字符集展开之前**：若先展开字符集 `[+\-*/]` → `(+|-|*|/)`，则字符集内的 `+` 和 `?` 会被误认为量词运算符。因此必须先处理 `+`/`?`，此时字符集仍在 `[...]` 括号内，可通过 `inCharClass` 标志予以保护。

**算法**：

```
算法：processPlusAndQuestion(regex)
输入：原始正则表达式
输出：+ 和 ? 已展开为基本形式的表达式

1.  result ← ""
2.  inCharClass ← false
3.  for i ← 0 to regex.length - 1:
4.      c ← regex[i]
5.      if c == '[' and not escaped:  inCharClass ← true; result.append(c); continue
6.      if c == ']' and not escaped and inCharClass:
7.          inCharClass ← false; result.append(c); continue
8.      if (c == '+' or c == '?') and not escaped and not inCharClass:
9.          target ← extractLastOperand(result)   // 回溯找到被修饰的操作数
10.         remove target from result
11.         if c == '+':  result.append(target).append(target).append('*')
12.         else:         result.append('(').append(target).append('|').append('ε').append(')')
13.     else: result.append(c)
14. return result
```

其中 `extractLastOperand(result)` 需要回溯缓冲区：若最后一个字符是 `)`，向前找匹配的 `(`；若是 `]`，向前找匹配的 `[`；否则提取最后一个单字符。

##### 3.2.2.3 字符集展开 —— processCharSet() + expandCharRange()

**功能**：将 `[...]` 形式的字符集语法糖展开为标准正则运算符的组合：
- `[abc]` → `(a|b|c)`
- `[a-z]` → `(a|b|c|...|z)`
- `[a-zA-Z_]` → `(a|b|...|z|A|B|...|Z|_)`

**算法**：

```
算法：expandCharRange(content)
输入：字符集内容（不含外围方括号），如 "a-z0-9_"
输出：用 | 连接的展开表达式，如 "a|b|...|z|0|1|...|9|_"

1.  sb ← ""
2.  i ← 0
3.  while i < content.length:
4.      ch ← content[i]
5.      if ch == '\' and i+1 < content.length:   // 转义序列
6.          ch ← resolveEscape(content[i+1]); i++
7.      if i+2 < content.length and content[i+1] == '-':
8.          end ← content[i+2]
9.          if end == '\': end ← resolveEscape(content[i+3]); i++
10.         for c from ch to end:
11.             if sb not empty: sb.append('|')
12.             sb.append(c)
13.         i ← i + 2
14.     else:
15.         if sb not empty: sb.append('|')
16.         sb.append(ch)
17.     i++
18. return sb
```

##### 3.2.2.4 否定字符集展开 —— expandNegatedCharRange()

**功能**：将 `[^...]` 形式的否定字符集展开为**补集表达式**。在 ASCII 可打印范围 `0x01-0x7E`（126 个字符）内，取出所有不在排除集合中的字符，用 `|` 连接。

**算法**：

```
算法：expandNegatedCharRange(content)
输入：否定字符集内容（不含 ^ 前缀），如 "abc"
输出：补集展开表达式

1.  excluded ← HashSet     // 收集所有被排除的字符
2.  解析 content（同 expandCharRange 逻辑），将范围内所有字符加入 excluded
3.  sb ← "("
4.  first ← true
5.  for ch ← 1 to 0x7E:           // 遍历 ASCII 可打印范围
6.      if ch not in excluded:
7.          if not first: sb.append('|')
8.          if isRegexMeta(ch): sb.append('\')   // 元字符加转义
9.          sb.append(ch)
10.         first ← false
11. sb.append(')')
12. return sb
```

此算法对行注释规则 `"//"[^\n]*` 的正确匹配至关重要——`[^\n]` 展开为除换行符外的所有可打印字符的交替。

##### 3.2.2.5 显式连接符插入 —— insertConcatOperator()

**功能**：扫描正则表达式，在需要隐式连接（concatenation）的相邻操作数之间插入显式连接符 `·`，使所有运算关系在语法层面显式化。

**为什么需要**：正则表达式中的连接是隐式的（如 `ab` 表示 a 连接 b），若不显式插入连接符，后续调度场算法无法区分"ab 是连接"还是单个操作数。

**算法**：

```
算法：insertConcatOperator(regex)
输入：正则表达式
输出：插入 · 后的表达式

1.  result ← ""
2.  prevWasBackslash ← false
3.  for i ← 0 to regex.length - 1:
4.      c1 ← regex[i]; result.append(c1)
5.      if c1 == '\':  prevWasBackslash ← true; continue
6.      if i+1 < regex.length:
7.          c2 ← regex[i+1]
8.          if shouldConcat(c1, c2, prevWasBackslash):
9.              result.append('·')
10.     prevWasBackslash ← false
11. return result

函数 shouldConcat(c1, c2, c1Escaped):
    leftReady  ← c1Escaped or isOperand(c1) or c1 ∈ {')', '*'}
    rightReady ← (c2 == '\') or isOperand(c2) or c2 == '('
    return leftReady and rightReady

函数 isOperand(c):
    return c ∉ {'(', ')', '|', '*', '·'}
```

**关键判断**：连接的条件是左侧必须为"已完成的表达式单位"（操作数、`)`、`*` 之后），右侧必须为"新表达式单位的开始"（操作数、`(` 之前）。

##### 3.2.2.6 中缀转后缀：调度场算法 —— toPostfix()

**功能**：采用经典的**调度场算法（Shunting-yard algorithm）**，将中缀正则表达式转换为后缀表达式（逆波兰表示法，RPN）。这是整个正则转换管线的核心算法。

**运算符优先级**：

| 运算符 | 含义 | 优先级 | 结合性 |
|--------|------|:------:|--------|
| `*` | Kleene 闭包（零次或多次） | 3（最高） | 单目后缀 |
| `·` | 连接 | 2 | 左结合 |
| `|` | 选择 | 1（最低） | 左结合 |

**算法伪代码**：

```
算法：toPostfix(regex)
输入：中缀正则表达式（已插入 ·，含 () 分组）
输出：后缀表达式

1.  output ← ""
2.  stack ← Stack<Character>
3.  i ← 0
4.  while i < regex.length:
5.      c ← regex[i]
6.      if c == '\' and i+1 < regex.length:     // 转义序列：整体输出
7.          output.append(c)
8.          output.append(regex[i+1])
9.          i ← i + 2; continue
10.     if isOperand(c):                        // 操作数：直接输出
11.         output.append(c)
12.     else if c == '(':                       // 左括号：入栈
13.         stack.push(c)
14.     else if c == ')':                       // 右括号：弹栈至左括号
15.         while stack.peek() ≠ '(':
16.             output.append(stack.pop())
17.         stack.pop()                          // 丢弃 '('
18.     else:                                   // 运算符 | · *
19.         while stack not empty and precedence(stack.peek()) ≥ precedence(c):
20.             output.append(stack.pop())
21.         stack.push(c)
22.     i++
23. while stack not empty:                      // 剩余运算符全部弹出
24.     output.append(stack.pop())
25. return output
```

**关键细节**：条件 `precedence(stack.peek()) ≥ precedence(c)` 使用了 ≥（而非 >），这确保了左结合运算符的正确处理——栈内同优先级的运算符先出栈。

**转换示例**：`a|b·c` → `abc·|`（先连接 b 和 c，再与 a 做选择）

##### 3.2.2.7 转义序列解析 —— resolveEscapes()

**功能**：将后缀表达式中的 `\n`、`\t`、`\r`、`\f`、`\v` 等转义序列解析为对应的实际控制字符。

**特殊处理**：保留 `\*` 和 `\|` 的转义形式不还原。原因是后缀表达式中 `*` 和 `|` 是运算符——若将字面量的 `\*` 还原为 `*`，后续 NFA 构建器会将其误当作 Kleene 闭包运算符。NfaBuilder 在扫描时会检查 `\*` 这种转义形式，从而正确识别字面量。

```
算法：resolveEscapes(postfix)
1.  sb ← ""
2.  i ← 0
3.  while i < postfix.length:
4.      if postfix[i] == '\' and i+1 < postfix.length:
5.          next ← postfix[i+1]
6.          if next ∈ {'*', '|'}:  sb.append('\').append(next)  // 保留转义
7.          else:                  sb.append(resolveEscape(next))  // 解析
8.          i ← i + 2
9.      else: sb.append(postfix[i]); i++
10. return sb

函数 resolveEscape(c):
    switch c:
        't' → '\t',  'n' → '\n',  'r' → '\r',  'f' → '\f'
        'v' → 0x0B,  '0' → '\0',  '\\' → '\\',  '\'' → '\''
        default → c     // \( → (, \) → ), etc.
```

---

#### 3.2.3 阶段三：NFA 构造与多规则合并 —— NfaManager.java + InternalNfaBuilder

**对应源文件**：[NfaManager.java](src/main/java/com/example/compiler/lex/NfaManager.java)（其中 `InternalNfaBuilder` 为内部类，实现 Thompson 构造法）

##### 3.2.3.1 Thompson 构造法：核心四运算

Thompson 构造法从后缀正则表达式构建等价的 NFA。它使用一个**操作数栈**，从左到右扫描后缀表达式的每个字符，根据字符类型执行四种操作。每个操作数/子表达式对应一个 `NfaFragment`（含 `start` 和 `accept` 两个 NFA 状态指针）。

**数据结构**：

```java
class NfaState {
    int id;                       // 全局唯一编号
    char transition;              // 转移字符，'ε' 表示 ε-边，其他存储实际字符
    List<NfaState> nextStates;    // 后继状态列表
    boolean isAccept;             // 是否为接受态
    int ruleId;                   // 接受时匹配的规则 ID，-1 表示非接受态
}

class NfaFragment {
    NfaState start;               // 片段入口状态
    NfaState accept;              // 片段出口状态
}
```

**运算一：操作数（doOperand）**

```
算法：doOperand(c)
输入：字符 c
输出：NfaFragment

1.  s1 ← new NfaState(id++, c)    // 转移字符为 c
2.  s2 ← new NfaState(id++)       // 转移字符为 'ε'（默认）
3.  s1.nextStates.add(s2)
4.  return NfaFragment(s1, s2)
```

```
    s1 ──c──► s2
```

**运算二：连接（doConcat）**

```
算法：doConcat(f1, f2)
输入：两个 NFA 片段 f1, f2
输出：f1 后接 f2 的合并片段

1.  f1.accept.nextStates.add(f2.start)   // 添加 ε 边连接
2.  return NfaFragment(f1.start, f2.accept)
```

```
    f1.start ──...──► f1.accept ──ε──► f2.start ──...──► f2.accept
```

**运算三：选择（doAlt）**

```
算法：doAlt(f1, f2)
输入：两个 NFA 片段 f1, f2
输出：f1 或 f2 的选择片段

1.  s ← new NfaState(id++)               // 新入口
2.  e ← new NfaState(id++)               // 新出口
3.  s.nextStates.add(f1.start)           // ε → f1
4.  s.nextStates.add(f2.start)           // ε → f2
5.  f1.accept.nextStates.add(e)          // f1 → ε → 出口
6.  f2.accept.nextStates.add(e)          // f2 → ε → 出口
7.  return NfaFragment(s, e)
```

```
                     ┌► f1.start ──...──► f1.accept ─┐
              ε      │                                │ ε
    s ───────────────┤                                ├──► e
              ε      │                                │ ε
                     └► f2.start ──...──► f2.accept ─┘
```

**运算四：Kleene 闭包（doKleene）**

```
算法：doKleene(f)
输入：NFA 片段 f
输出：f 的零次或多次重复片段

1.  s ← new NfaState(id++)               // 新入口
2.  e ← new NfaState(id++)               // 新出口
3.  s.nextStates.add(f.start)            // ε → 进入 f（至少一次）
4.  s.nextStates.add(e)                  // ε → 跳过 f（零次）
5.  f.accept.nextStates.add(f.start)     // ε → 回到 f.start（重复）
6.  f.accept.nextStates.add(e)           // ε → 出口（结束）
7.  return NfaFragment(s, e)
```

```
              ε                        ε
    s ────────────► f.start ──...──► f.accept ────────────► e
    │                                    ▲                  ▲
    │              ε                     │ ε                │ ε
    └──────────────────────────────────────────────────────┘
                                       │
                                       └── (f.accept → f.start，重复)
```

##### 3.2.3.2 Thompson 构造法：主循环（InternalNfaBuilder.build()）

```
算法：build(postfix)
输入：后缀正则表达式
输出：该表达式对应的 NfaFragment

1.  stack ← Stack<NfaFragment>
2.  i ← 0
3.  while i < postfix.length:
4.      c ← postfix[i]
5.      if c == '\' and i+1 < postfix.length:  // 转义序列 → 字面量
6.          i ← i + 1
7.          stack.push(doOperand(postfix[i]))
8.      else switch c:
9.          case '*':  stack.push(doKleene(stack.pop()))
10.         case '·':  r ← stack.pop(); l ← stack.pop(); stack.push(doConcat(l, r))
11.         case '|':  r ← stack.pop(); l ← stack.pop(); stack.push(doAlt(l, r))
12.         default:   stack.push(doOperand(c))
13.     i++
14. return stack.pop()
```

**注意**：`|` 和 `·` 都是双目运算符，弹出顺序是先右后左（栈顶是右操作数）。

##### 3.2.3.3 多规则 NFA 合并 —— NfaManager.buildCombinedNfa()

**功能**：为每条词法规则分别调用 `RegexConverter.convert()` + `InternalNfaBuilder.build()` 得到各自的 NFA 片段，将所有片段通过 ε 边统一连接到唯一的全局起始状态，形成一个"巨型 NFA"。

**算法**：

```
算法：buildCombinedNfa(rules)
输入：规则列表 rules（从 SeuLexParser 获得）
输出：合并后 NFA 的全局起始状态

1.  globalStart ← new NfaState(id++)      // 全局起始状态（转移字符默认 'ε'）
2.  for each rule in rules:
3.      postfix ← converter.convert(rule.regex)    // 正则 → 后缀
4.      fragment ← builder.build(postfix)          // 后缀 → NFA 片段
5.      fragment.accept.isAccept ← true          // 标记接受态
6.      fragment.accept.ruleId ← rule.id           // 绑定规则 ID（实现优先级）
7.      globalStart.nextStates.add(fragment.start) // ε-边连接
8.  return globalStart
```

合并后的 NFA 拓扑结构：

```
                   ┌──ε──► Rule1 的 NFA ──► [accept, ruleId=1]
    globalStart ───┼──ε──► Rule2 的 NFA ──► [accept, ruleId=2]
                   ├──ε──► Rule3 的 NFA ──► [accept, ruleId=3]
                   └──ε──► ...              ...
```

**全局状态计数器**：`NfaManager` 持有 `globalStateCounter`，`InternalNfaBuilder.createNode()` 从同一计数器取号。所有规则的 NFA 状态 ID 全局唯一，避免了后续合并时的 ID 冲突。

---

#### 3.2.4 阶段四：NFA → DFA 确定化 —— NfaToDfaConverter.convert()

**对应源文件**：[NfaToDfaConverter.java](src/main/java/com/example/compiler/lex/NfaToDfaConverter.java)（`convert()` 方法）

将 NFA 转换为等价的 DFA，采用**子集构造法（Subset Construction / Powerset Construction）**。

##### 3.2.4.1 DFA 状态数据结构 —— DfaState

在介绍子集构造法的具体算法之前，先明确 DFA 状态的数据结构。DFA 的一个状态本质上是 NFA 状态集合的封装：

```java
class DfaState {
    int id;                              // DFA 状态编号（从 0 开始递增）
    Set<NfaState> nfaStates;             // 该 DFA 状态对应的 NFA 状态集合（核心）
    Map<Character, DfaState> transitions; // 转移表：字符 → 目标 DFA 状态
    boolean isAccept;                    // 是否为接受态
    int acceptedRuleId;                  // 接受时匹配的规则 ID，-1 表示非接受态
}
```

各字段含义如下：

- **`id`**：DFA 状态在列表中的编号，也是运行时转移表的行索引。起始状态固定为 0。
- **`nfaStates`**：该 DFA 状态所包含的 NFA 状态集合——这是 DFA 状态与 NFA 状态之间最重要的映射桥梁。子集构造法通过计算不同 NFA 状态集的 ε-闭包来生成新 DFA 状态。
- **`transitions`**：HashMap 实现的稀疏转移表，`transitions.get(c)` 即字符 `c` 上的转移目标 DFA 状态。无边时为 `null`（运行时映射为 -1）。
- **`isAccept`**：当 `nfaStates` 中存在至少一个 NFA 接受态时为 `true`。
- **`acceptedRuleId`**：若 `isAccept == true`，取 `nfaStates` 中所有 NFA 接受态里 `ruleId` 最小的值。这内建实现了 Lex 优先级规则——多条规则同时匹配时，`.l` 文件中先出现的（`ruleId` 更小）胜出。

**状态判等**：`DfaState` 的 `equals()` 和 `hashCode()` 仅基于 `nfaStates` 集合重写——两个 DFA 状态代表同一个 NFA 状态集时即视为相同。这是子集构造法中 `findState()` 查重的依据，避免了为重复的 NFA 子集创建冗余 DFA 状态。

**优先级处理**在构造函数中完成：

```
DfaState 构造函数:
1.  for each nfaState in nfaStates:
2.      if nfaState.isAccept:
3.          this.isAccept ← true
4.          if this.acceptedRuleId == -1 or nfaState.ruleId < this.acceptedRuleId:
5.              this.acceptedRuleId ← nfaState.ruleId    // 取最小 ruleId
```

##### 3.2.4.2 ε-闭包计算 —— epsilonClosure()

```
算法：epsilonClosure(states)
输入：NFA 状态集 states
输出：从 states 出发仅通过 ε 边可达的所有状态的集合

1.  closure ← HashSet(states)       // 初始包含自身
2.  stack ← Stack; stack.addAll(states)
3.  while stack not empty:
4.      current ← stack.pop()
5.      if current.transition == 'ε':
6.          for each next in current.nextStates:
7.              if next ∉ closure:
8.                  closure.add(next)
9.                  stack.push(next)     // 继续沿 ε 边深搜
10. return closure
```

**原理**：使用 DFS 栈遍历，当遇到 `transition == 'ε'` 的状态时，将其所有后继加入闭包并继续递归。`NfaState` 的 `transition` 字段为 `'ε'` 时表示该状态到后继之间是 ε-转移。

##### 3.2.4.3 move 计算 —— move()

```
算法：move(states, c)
输入：NFA 状态集 states，字符 c
输出：从 states 中状态经字符 c 一步可达的 NFA 状态集（不含 ε-闭包）

1.  result ← ∅
2.  for each s in states:
3.      if s.transition == c:
4.          result.addAll(s.nextStates)   // c-边指向的所有后继
5.  return result
```

##### 3.2.4.4 子集构造法主算法

```
算法：convert(nfaStart)
输入：NFA 起始状态 nfaStart
输出：DFA 状态列表

1.  alphabet ← collectAlphabet(nfaStart)      // 遍历 NFA，收集所有非 ε 转移字符
2.  dfaStates ← []                              // DFA 状态表
3.  unprocessed ← Queue                         // 待处理队列 (BFS)

4.  // 计算初始 DFA 状态
5.  startSet ← {nfaStart}
6.  startDfa ← new DfaState(0, epsilonClosure(startSet))
7.  dfaStates.add(startDfa)
8.  unprocessed.enqueue(startDfa)

9.  while unprocessed not empty:
10.     current ← unprocessed.dequeue()
11.     for each c in alphabet (c ≠ 'ε'):
12.         movedSet ← move(current.nfaStates, c)
13.         if movedSet is empty: continue
14.         targetClosure ← epsilonClosure(movedSet)
15.         existing ← findState(dfaStates, targetClosure)   // 按 nfaStates 判等
16.         if existing == null:
17.             newState ← new DfaState(newId++, targetClosure)
18.             dfaStates.add(newState)
19.             unprocessed.enqueue(newState)
20.             current.transitions.put(c, newState)
21.         else:
22.             current.transitions.put(c, existing)         // 复用已有状态
23. return dfaStates
```

**符号收集（collectAlphabet）**：对 NFA 做一次 BFS 遍历，收集所有 `transition ≠ 'ε'` 的转移字符，构成字母表。字母表规模决定了每轮迭代需要枚举的字符数量。

**状态判等**：`DfaState.equals()` 基于 `nfaStates` 集合重写——若两个 DFA 状态的 NFA 状态集完全相同，则视为同一个 DFA 状态。这是子集构造法中最重要的优化，确保不会为已出现的状态集创建重复的 DFA 状态。

**复杂度**：最坏情况下 DFA 状态数为 `O(2^n)`（n 为 NFA 状态数），但对实际词法规则通常远小于此上界。

##### 3.2.4.5 Lex 优先级处理

在 `DfaState` 构造函数中，遍历该 DFA 状态包含的所有 NFA 状态。若其中存在多个接受态（即多条规则可能同时匹配），选择 `ruleId` 最小的作为 `acceptedRuleId`。由于规则在 `.l` 文件中的出现顺序决定了 `ruleId`（越早出现 `ruleId` 越小），这一选择内建实现了 Lex 的"优先匹配"语义——当多条规则同时匹配时，排在前面的规则胜出。

---

#### 3.2.5 阶段五：DFA 最小化 —— NfaToDfaConverter.minimize()

**对应源文件**：[NfaToDfaConverter.java](src/main/java/com/example/compiler/lex/NfaToDfaConverter.java)（`minimize()` 方法）

采用**等价类划分法（Equivalence Class Partitioning）**，基于 Hopcroft 算法的思想，将 DFA 中行为等价的状态合并，得到状态数最少的 DFA。

**核心思想**：两个 DFA 状态等价，当且仅当它们在任意输入字符上的转移目标都落在相同的等价组中。算法从粗粒度的初始划分开始，反复将不等价的状态拆分，直到稳定。

```
算法：minimize(dfaStates)
输入：子集构造法得到的 DFA 状态列表
输出：最小化后的 DFA 状态列表

// === Step 1：收集字母表 ===
1.  alphabet ← ∅
2.  for each s in dfaStates: alphabet ← alphabet ∪ s.transitions.keySet()

// === Step 2：初始划分 P₀ ===
3.  P ← []                    // 等价划分，元素为 Set<DfaState>
4.  nonAcceptGroup ← ∅
5.  acceptGroups ← Map<ruleId, Set<DfaState>>
6.  for each s in dfaStates:
7.      if s.isAccept:  acceptGroups[s.acceptedRuleId].add(s)
8.      else:           nonAcceptGroup.add(s)
9.  P ← all values of acceptGroups
10. if nonAcceptGroup not empty: P.add(nonAcceptGroup)
// 注意：接受态按 acceptedRuleId 分组 → 接受不同 Token 类型的状态不会被错误合并

// === Step 3：迭代细化 ===
11. changed ← true
12. while changed:
13.     changed ← false
14.     newP ← []
15.     for each group in P:
16.         if |group| ≤ 1: newP.add(group); continue
17.         splits ← Map<List<Integer>, Set<DfaState>>
18.         for each s in group:
19.             signature ← []
20.             for each c in alphabet:
21.                 nextState ← s.transitions.get(c)
22.                 if nextState == null: targetGroupIdx ← -1   // 死状态
23.                 else: targetGroupIdx ← findGroupIndex(P, nextState)
24.                 signature.add(targetGroupIdx)
25.             splits[signature].add(s)
26.         newP.addAll(splits.values())
27.         if |splits| > 1: changed ← true     // 发生了拆分
28.     P ← newP

// === Step 4：重构最小 DFA ===
29. minDfa ← []
30. for each group in P:
31.     rep ← group 中任一代表元
32.     newState ← new DfaState(newId, ∅)
33.     newState.isAccept ← rep.isAccept
34.     newState.acceptedRuleId ← rep.acceptedRuleId
35.     minDfa.add(newState)
36.     groupToMinState[group] ← newState

37. for each group in P:
38.     rep ← group 的代表元
39.     currentMinState ← groupToMinState[group]
40.     for each (c, oldNext) in rep.transitions:
41.         targetGroup ← P[findGroupIndex(P, oldNext)]
42.         currentMinState.transitions.put(c, groupToMinState[targetGroup])

// === Step 5：确保起始状态在位置 0 ===
43. oldStart ← dfaStates[0]
44. for each group in P:
45.     if group contains oldStart: newStart ← groupToMinState[group]
46. if newStart ≠ minDfa[0]:
47.     swap newStart to minDfa[0], renumber all ids
48. return minDfa
```

**转移签名（signature）**：每个状态对应一个签名向量，向量的每一维是该状态在某一字符上的转移目标所在的等价组编号。两个状态等价当且仅当它们的签名完全相同。`findGroupIndex(P, nextState)` 查找目标状态属于当前划分中的哪个组（返回组编号）。

**终止性**：每轮迭代只做拆分不做合并，划分的组数单调递增，最多不超过原 DFA 状态数，因此算法必然终止。实际中通常在 2-4 轮后即达不动点。

---

#### 3.2.6 阶段六：代码生成 —— CodeGenerator.java

**对应源文件**：[CodeGenerator.java](src/main/java/com/example/compiler/lex/CodeGenerator.java)

`CodeGenerator` 是 SeuLex 离线生成阶段的最后一个模块。它接收最小化 DFA、词法规则列表、定义段和用户代码段，同步产出两件制品：二进制转移表 `lexer_tables.dat` 和运行时 Java 源码 `GeneratedLexer.java`。

##### 3.2.6.1 整体流程 —— generateJava()

```
算法：generateJava(states, rules, definitions, userCode)
输入：最小化 DFA 状态列表 states，规则列表 rules，定义段文本 definitions，用户代码段 userCode
输出：GeneratedLexer.java 的完整源码字符串；副作用：写出 lexer_tables.dat

1.  sb ← StringBuilder
2.  // 固定头部：package + import
3.  sb.append("package com.example.compiler.lex;\n")
4.  sb.append("import java.io.*;\n")
5.  sb.append("import com.example.compiler.yacc.token.*;\n")
6.  // 处理 %{ ... %} 声明块：使用 CToJavaTranslator 翻译
7.  if definitions contains "%{" and "%}":
8.      提取 %{ ... %} 之间的内容
9.      translator ← new CToJavaTranslator()
10.     defParts ← translator.translateDefinitionBlock(defBlock)
11.     // defParts[0]: import/package 行（追加到类体前）
12.     // defParts[1]: C 变量声明翻译为 Java 字段（追加到类体内）
13.
14. // === 类声明 + 运行时字段 ===
15. sb.append("public class GeneratedLexer {\n")
16. sb.append(defParts[1])                  // 翻译后的定义段字段
17. sb.append("    private PushbackReader yyin;\n")
18. sb.append("    public char[] yytext = new char[4096];\n")
19. sb.append("    public int yyleng = 0;\n")
20. 生成两个构造函数：Reader 版本和 InputStream 版本
21.
22. // === 生成二进制转移表 (lexer_tables.dat) 并生成对应的加载代码 ===
23. writeBinaryTable(states)           // 写 lexer_tables.dat（见 3.2.6.2）
24. generateStaticLoader(sb, states)   // 生成 static { ... } 反序列化代码（见 3.2.6.3）
25.
26. // === 生成运行时 I/O 方法 ===
27. generateInputMethod(sb)            // input(): 从 PushbackReader 读一个字符
28. generateUngetcMethod(sb)           // ungetc(c): 回退一个字符
29.
30. // === 生成 nextToken() 方法体 ===
31. generateNextToken(sb, rules)       // 最长匹配循环 + switch-case 动作分派（见 3.2.6.4）
32.
33. // === 翻译用户子程序段（C → Java） ===
34. translator2 ← new CToJavaTranslator()
35. sb.append(translator2.translateUserSubroutines(userCode))  // 见 3.2.6.5
36.
37. sb.append("}\n")
38. return sb.toString()
```

**关键设计决策**：`generateJava()` 同时写二进制文件和生成 Java 源码。二者必须在同一次调用中完成，因为源码中的 static 初始化块需要与二进制文件的格式精确匹配。与旧版实现不同，当前版本不再将 `c99.l` 的用户代码段丢弃并用硬编码的 Java 方法替代，而是通过 `CToJavaTranslator` 对用户编写的 C 函数进行**结构化翻译**——解析函数签名、映射 C 类型到 Java 类型、逐条转换函数体内的 C 惯用语。这使得 SeuLex 能够处理用户自定义的任意辅助函数，而不再局限于 `c99.l` 中特定的四个函数。

##### 3.2.6.2 二进制转移表序列化 —— writeBinaryTable()

**功能**：将内存中最小化 DFA 的稀疏转移关系（`Map<Character, DfaState>`）展开为稠密的 `int[stateCount][256]` 矩阵，以二进制格式写入 `lexer_tables.dat`。

**为什么需要稠密展开**：生成阶段的 DFA 状态使用 HashMap 存储转移关系——灵活、节省内存，适合生成阶段反复修改。但运行时 `nextToken()` 每读入一个字符就要查一次转移表，HashMap 的哈希查找开销不可接受。稠密数组 `transition_table[state][char]` 以字符 ASCII 值为下标直接索引，实现 **O(1) 单步转移**。

**序列化算法**：

```
算法：writeBinaryTable(states)
输入：最小化 DFA 状态列表 states（id 已规范化为 0..n-1）
输出（副作用）：写入 lexer_tables.dat

1.  dos ← DataOutputStream(FileOutputStream("src/main/resources/lexer_tables.dat"))
2.  dos.writeInt(states.size())          // 状态数 n
3.  dos.writeInt(256)                     // 字符列数：固定 256（ASCII 全集）

4.  // —— 转移表：按行优先 (row-major) 排列 ——
5.  for i ← 0 to states.size() - 1:       // 外层按状态顺序遍历
6.      state ← states[i]
7.      for c ← 0 to 255:                 // 内层按字符顺序展开
8.          if state.transitions.containsKey((char)c):
9.              dos.writeInt(state.transitions.get((char)c).id)   // 有边：写目标状态 ID
10.         else:
11.             dos.writeInt(-1)                                    // 无边：写 -1

12. // —— 接受表：按状态顺序排列 ——
13. for i ← 0 to states.size() - 1:
14.     state ← states[i]
15.     if state.isAccept:
16.         dos.writeInt(state.acceptedRuleId)   // 接受态：写规则 ID
17.     else:
18.         dos.writeInt(-1)                     // 非接受态：写 -1
19. dos.close()
```

**二进制文件布局**（Java DataOutputStream 使用 big-endian 字节序）：

```
┌──────────────────────┬───────────────────────┬─────────────────────────────────┬─────────────────────────┐
│ int: 状态数 n        │ int: 列数 = 256       │ int[n×256]: 转移表               │ int[n]: 接受表           │
│ (4 bytes)            │ (4 bytes)             │ (n×256×4 bytes, row-major)       │ (n×4 bytes)              │
└──────────────────────┴───────────────────────┴─────────────────────────────────┴─────────────────────────┘
偏移 0                  偏移 4                  偏移 8                             偏移 8 + n×256×4
```

**稀疏度观察**：对于典型的 C99 词法规则（约 30-40 种不同字符参与转移），256 列中超过 80% 的位置为 -1。这里用空间换时间是合理的取舍——256 列 × 几百个状态仅为几百 KB，但换来运行时每次查表只需一次数组下标操作。

##### 3.2.6.3 运行时加载代码生成 —— generateStaticLoader()

**功能**：生成 `GeneratedLexer` 的 static 初始化块，在类加载时从 `lexer_tables.dat` 反序列化转移表和接受表。

**生成的代码逻辑**（以伪代码表示）：

```
static {
    1.  尝试从 classpath 资源 "/lexer_tables.dat" 打开 DataInputStream
    2.  若 classpath 未找到，回退到文件系统 "src/main/resources/lexer_tables.dat"
    3.  r ← dis.readInt()         // 状态数
    4.  c ← dis.readInt()         // 列数 (256)
    5.  transition_table ← new int[r][c]
    6.  for i ← 0 to r-1:
    7.      for j ← 0 to c-1:
    8.          transition_table[i][j] ← dis.readInt()
    9.  accept_rule ← new int[r]
    10. for i ← 0 to r-1:
    11.     accept_rule[i] ← dis.readInt()
    12. dis.close()
}
```

加载后得到两个运行时核心数据结构：

```java
int[][] transition_table;  // transition_table[state][char] = nextState（-1 表示无转移）
int[]   accept_rule;       // accept_rule[state] = ruleId（-1 表示非接受态）
```

注意 `transition_table` 和 `accept_rule` 使用数组下标隐式索引——`state` 即行号，`char` 即列号，无需额外的 lookup 结构，访问效率最高。

##### 3.2.6.4 nextToken() 方法生成与动作分派 —— generateNextToken()

**功能**：生成运行时词法分析器核心方法 `nextToken()` 的完整源码，包括最长匹配 DFA 模拟循环、回溯逻辑和 switch-case 动作分派。

**生成 nextToken() 方法体的伪代码**：

```
算法：generateNextToken(sb, rules)
1.  生成方法签名：public Token nextToken() {
2.  生成局部变量声明：
3.      int state = 0;
4.      int last_accept_state = -1;
5.      int last_accept_len = 0;
6.      yyleng = 0;

7.  生成 DFA 模拟循环（最长匹配）：
8.      while ((c = input()) != -1 && c != 0):
9.          if c < 0 || c >= 256: break
10.         yytext[yyleng++] = (char)c
11.         next_state = transition_table[state][c]
12.         if next_state == -1: break       // 转移失败 → 退出循环
13.         state = next_state
14.         if accept_rule[state] != -1:     // 当前状态可接受
15.             last_accept_state = state    // 记录最长匹配点
16.             last_accept_len = yyleng

17. 生成回溯逻辑：
18.     if last_accept_state != -1:
19.         for i = yyleng-1 downto last_accept_len:
20.             ungetc(yytext[i])            // 多余字符退回输入流
21.         yyleng = last_accept_len

22. 生成 switch-case 动作分派（核心）：
23.     sb.append("switch (accept_rule[last_accept_state]) {\n")
24.     for each rule in rules:
25.         sb.append("    case " + rule.id + ":\n")
26.         sb.append("        " + rule.action + "\n")    // 翻译后的 Java 动作
27.         if rule.action does NOT contain "return":
28.             sb.append("        break;\n")             // 非 return 动作需要 break
29.     sb.append("}\n")
30.     sb.append("return nextToken();\n")                // 递归获取下一个 Token

31. 生成错误处理分支：
32.     else if c == -1 || c == 0:
33.         if yyleng == 0: return EOF Token
34.         else: throw RuntimeException("unexpected end of file")
35.     throw RuntimeException("unexpected character")
36. }
```

**switch-case 动作分派的"是否加 break"判断**：遍历每条规则时，若 `rule.action` 中包含 `return` 语句（如 `return new Token(TokenType.INT, ...)`），则不追加 `break`——因为 return 已经跳出方法。若动作不含 `return`（如空白跳过规则 `{ count(); }` 和注释规则 `{ comment(); }`），则追加 `break`，使执行流落到 `switch` 之后的 `return nextToken()` 递归调用，从而跳过不产生 Token 的规则，继续扫描下一个词素。

**为什么用递归而非循环**：`nextToken()` 在产生一个 Token 后递归调用自身获取下一个 Token。这种设计简洁且自然地处理了"空白/注释不产生 Token 需跳过"的情况——执行完动作后落入递归调用即自动继续扫描。代价是深层嵌套输入可能导致栈溢出，但对词法分析场景（每次递归消费至少一个字符）实际不会发生。

##### 3.2.6.5 C 用户代码的 Java 翻译 —— CToJavaTranslator

**对应源文件**：[CToJavaTranslator.java](src/main/java/com/example/compiler/lex/CToJavaTranslator.java)

旧版实现将 `c99.l` 用户代码段（第二个 `%%` 之后）完全丢弃，改用 CodeGenerator 硬编码注入 `count()`、`comment()`、`check_type()`、`error()` 四个 Java 方法。这种方式有两个致命缺陷：(1) 无法处理用户自定义的辅助函数；(2) 若用户修改了这四个函数的 C 实现，生成的 Java 代码行为与之不一致。

`CToJavaTranslator` 采用**结构化翻译策略**——先将 C 代码拆分为顶层构造，再对每种构造分别翻译签名和函数体，从根本上解决了上述问题。

**总体流程**：

```
算法：translateUserSubroutines(cCode)
输入：用户子程序段原始文本
输出：翻译后的 Java 代码（可直接嵌入 GeneratedLexer 类体）

1.  code ← removeCComments(cCode)           // 去除 C 注释（/* */ 和 //）
2.  chunks ← splitTopLevel(code)            // 拆分为顶层构造列表
3.  for each chunk in chunks:
4.      translated ← translateChunk(chunk)
5.      result.append(translated)
6.  return result
```

**步骤一：顶层构造拆分 —— splitTopLevel()**

追踪花括号深度 `braceDepth` 和圆括号深度 `parenDepth`，在深度均为 0 时识别两个构造边界：
- `}` 且 `braceDepth` 降为 0 → 函数定义结束，生成 `Kind.FUNCTION` 块
- `;` 且两个深度均为 0 → 顶层声明结束，生成 `Kind.DECLARATION` 块

此算法不依赖正则表达式匹配函数签名，对任意格式的 C 代码均能正确切分。

**步骤二：块翻译调度 —— translateChunk()**

根据块类型分派：
- `FUNCTION` → `translateFunction()`：解析函数签名 + 翻译函数体
- `DECLARATION` → `translateTopLevelDeclaration()`：翻译变量声明或注释掉预处理指令

**步骤三：C 函数翻译 —— translateFunction()**

对函数块进行**签名解析**和**函数体翻译**两个阶段的处理。

**(a) 签名解析（parseCSignature）**：从右向左定位参数列表的 `)`，通过括号匹配找到对应的 `(`，提取参数列表并逐个解析参数名和 C 类型。`(` 之前的部分从末尾提取函数名，剩余部分为返回类型。若返回类型为空则默认为 `int`（C 语言隐式规则）。

```
签名各部分识别示例:
    const char *error(int col, const char *msg, char c)
    ├── 返回类型 ──┤├──┤ ├──参数1──┤ ├────参数2────┤ ├参数3┤
                    函数名
```

**(b) 类型映射（translateType）**：使用**数据驱动的映射表** `C_TO_JAVA_TYPE`，包含 20 条映射规则：

| C 类型 | Java 类型 | 说明 |
|--------|----------|------|
| `void` | `void` | 无返回值 |
| `int` / `short` | `int` | 整数类型统一 |
| `char` | `int` | 与 `Reader.read()` 返回值一致 |
| `long` / `long long` | `long` | 长整型 |
| `float` | `float` | 单精度浮点 |
| `double` | `double` | 双精度浮点 |
| `const char*` / `char*` | `String` | C 字符串 → Java String |
| `unsigned int` / `unsigned short` | `int` | 无符号整数（Java 无对应类型） |
| `unsigned long` / `unsigned long long` | `long` | 无符号长整型 |

对于带 `const`/`static`/`extern` 等限定符的类型，先剥离限定符再查表。未知类型保留原样（以支持 `TokenType` 等用户自定义 typedef 名）。`yywrap` 函数被跳过（Flex 兼容，在 Java 中不需要）。

**(c) 函数体翻译（translateFunctionBody）**：对函数体依次应用 6 条转换规则：

1. **局部变量声明类型翻译**：匹配 `C类型 变量名 [= 初始值];`，将 C 类型替换为 Java 类型
2. **`input()` EOF 语义适配**：`while ((c = input()) != 0)` → `while ((c = input()) != -1 && c != 0)`（C 中 `input()` 返回 0 表示 EOF，Java 中 `Reader.read()` 返回 -1，但两者均需处理）
3. **`yytext` 遍历循环翻译**：`for (i = 0; yytext[i] != '\0'; i++)` → `for (int i = 0; i < yyleng; i++)`（C 依赖 `\0` 终止符，Java 中用 `yyleng` 长度字段）
4. **错误报告翻译**：`fprintf(stderr, "msg", args...); exit(1);` → `throw new RuntimeException("msg" + args...);`
5. **`ECHO` 宏翻译**：`ECHO;` → `System.out.print(new String(yytext, 0, yyleng));`
6. **`return TOKEN_NAME` 翻译**：与 SeuLexParser 的 action 翻译一致，`return TOKEN_NAME;` / `return(TOKEN_NAME);` → `return new Token(TokenType.TOKEN_NAME, ...)`
7. **冗余声明清理**：若函数体内存在 `int i;` 后紧跟 `for (int i = ...)` 的情况，删除独立的变量声明（因 C 变量声明必须在块首，而 Java 允许内联声明）

翻译后若函数体含 `return new Token(...)` 但签名仍为 `int`，自动修正返回类型为 `Token`。

**步骤四：顶层声明翻译 —— translateTopLevelDeclaration()**

- 前向声明（`int f(...);`）：跳过（在类体中不需要声明）
- `#` 开头的预处理指令：转为 Java 注释 `// #...`
- C 变量声明：翻译为 `public` Java 字段，如 `int column = 0;` → `public int column = 0;`

**(d) 翻译示例**：

输入的 C 代码（来自 `c99.l` 用户代码段）：

```c
int column = 0;
void count(void)
{
    int i;
    for (i = 0; yytext[i] != '\0'; i++)
        if (yytext[i] == '\n')
            column = 0;
        else if (yytext[i] == '\t')
            column += 8 - (column % 8);
        else
            column++;
    ECHO;
}
```

翻译后的 Java 代码：

```java
    public int column = 0;
    private void count() {
        for (int i = 0; i < yyleng; i++)
            if (yytext[i] == '\n')
                column = 0;
            else if (yytext[i] == '\t')
                column += 8 - (column % 8);
            else
                column++;
        System.out.print(new String(yytext, 0, yyleng));
    }
```

**适用性说明**：此翻译器的设计以 `c99.l` 用户代码段为目标，翻译规则覆盖了其中出现的主要 C/Flex 惯用语（`input()` 循环、`yytext` 遍历、`fprintf`+`exit` 错误报告、`ECHO` 输出、`return TOKEN`）。对于更复杂的 C 语法（函数指针、`switch` 语句中的 fall-through、`enum`/`union` 类型等），当前翻译器可能无法完整处理。但其核心价值在于**搭建了通用翻译框架**——结构化拆分 → 签名解析 → 类型映射 → 逐规则转换，后续可逐步扩充映射表和转换规则以支持更广泛的 C 子集。

##### 3.2.6.6 完整的 GeneratedLexer 类结构

综览 CodeGenerator 产出的 `GeneratedLexer.java`，其整体结构为：

```java
package com.example.compiler.lex;

import java.io.*;
import com.example.compiler.yacc.token.*;

public class GeneratedLexer {
    // === 翻译后的 %{...%} 块字段 ===
    // （由 CToJavaTranslator 从 C 变量声明翻译而来）

    // === 运行时字段 ===
    private PushbackReader yyin;          // 带 4096 字符回退缓冲的输入流
    public char[] yytext = new char[4096]; // 当前词素缓冲区
    public int yyleng = 0;                 // 当前词素长度
    public int column = 0;                 // 当前列号（由 count() 维护，由 CToJavaTranslator 翻译）

    // === 构造函数（Reader / InputStream 两个重载） ===

    // === static 块：从 lexer_tables.dat 加载转移表 ===
    private static final int[][] transition_table;  // [state][char] → nextState
    private static final int[]   accept_rule;       // [state] → ruleId

    // === I/O 原语 ===
    private int input()                   // 读一个字符
    private void ungetc(int c)            // 回退一个字符

    // === 核心方法：最长匹配 DFA 模拟 ===
    public Token nextToken()              // 返回下一个 Token

    // === 用户子程序段翻译后的方法（由 CToJavaTranslator 生成） ===
    private void error(String msg)        // 原 C 函数: void error(const char *msg)
    private void comment()                // 原 C 函数: void comment(void)
    private void count()                  // 原 C 函数: void count(void)
    private Token check_type()            // 原 C 函数: int check_type(void)
    // ... 其他用户自定义函数（如有）
}
```

---

#### 3.2.7 阶段七：运行时词法分析 —— GeneratedLexer.java

**对应源文件**：[GeneratedLexer.java](src/main/java/com/example/compiler/lex/GeneratedLexer.java)（由 CodeGenerator 自动生成）

运行时词法分析器是 SeuLex 的最终产物。它加载离线阶段生成的 DFA 转移表，按**最长匹配原则**逐字符模拟 DFA 运行，抵达接受态后回溯至最长匹配点，根据 `ruleId` 执行对应的语义动作产生 Token。

##### 3.2.7.1 最长匹配算法 —— nextToken()

这是整个词法分析器运行时的核心算法，也是 Lex/Flex 语义中"最长匹配优先"和"优先级规则"两个关键原则的直接体现。

```
算法：nextToken()
输出：下一个 Token

1.  state ← 0                           // DFA 起始状态
2.  last_accept_state ← -1              // 最近遇到的接受态编号
3.  last_accept_len ← 0                 // 到达该接受态时的已读长度
4.  yyleng ← 0                          // 当前已读字符数

5.  while true:
6.      c ← input()                     // 读一个字符
7.      if c == -1 or c == 0: break     // EOF
8.      if c < 0 or c ≥ 256: break      // 超出 ASCII 范围
9.      yytext[yyleng] ← (char)c
10.     yyleng ← yyleng + 1

11.     next_state ← transition_table[state][c]
12.     if next_state == -1: break       // 无转移 → 停止消费

13.     state ← next_state
14.     if accept_rule[state] ≠ -1:      // 当前状态可接受
15.         last_accept_state ← state    // 记录（最长匹配点）
16.         last_accept_len ← yyleng

17. // === 回溯阶段 ===
18. if last_accept_state ≠ -1:
19.     for i ← yyleng - 1 down to last_accept_len:
20.         ungetc(yytext[i])           // 将多余字符退回输入流
21.     yyleng ← last_accept_len         // 截断 yytext 至匹配点

22.     ruleId ← accept_rule[last_accept_state]
23.     switch ruleId:                   // 按规则 ID 分派动作
24.         case N: 执行语义动作
25.         ...
26.     return nextToken()               // 递归获取下一个 Token

27. else:                                // 无接受态：错误或 EOF
28.     if c == -1: return Token(EOF, "EOF")
29.     else: error("unexpected character")
```

**最长匹配是如何实现的**：
- while 循环中持续消费字符直到转移失败（`next_state == -1`）或输入结束
- 在消费过程中，`last_accept_state` 和 `last_accept_len` 始终指向**最近遇到的接受态**及其当时位置
- 转移失败后，回溯到 `last_accept_len` 的位置，将多读的字符逐个回退

**优先级规则是如何实现的**：
- 在 DFA 构造阶段（`DfaState` 构造函数），若一个 DFA 状态包含多个 NFA 接受态，选择 `ruleId` 最小的作为 `acceptedRuleId`
- 由于规则按 `.l` 文件中的出现顺序分配 `ruleId`（越早越小），这一设计内建实现了 Lex 优先级

**递归获取**：`nextToken()` 在产生 Token 后递归调用自身获取下一个 Token。对于不产生 Token 的规则（如空白跳过、注释消费），动作执行后不 `return`，而是执行到 `break` 后落入递归调用——确保它们被"跳过"。

##### 3.2.7.2 DFA 转移表

运行时数据结构：

```java
int[][] transition_table;   // transition_table[state][char] = nextState（-1 表示无转移）
int[]   accept_rule;        // accept_rule[state] = ruleId（-1 表示非接受态）
```

转移表在 `GeneratedLexer` 的 static 初始化块中从 `lexer_tables.dat` 反序列化加载。表大小为 `stateCount × 256`，使用稠密矩阵——每个状态对应完整的 256 字节查找表，实现 O(1) 的单步转移查询。

---

## 4. 使用说明

### 4.1 SeuLex 使用说明

SeuLex 提供两种使用场景：

#### 4.1.1 离线生成模式（Generator）

**功能**：从词法规范文件（`.l`）生成运行时词法分析器。

**运行方式**：

```bash
# 使用 Maven 运行（默认读取 resources/c99.l）
cd yacc
mvn exec:java -Dexec.mainClass="com.example.compiler.lex.LexCompilerMain"

# 指定自定义词法文件
mvn exec:java -Dexec.mainClass="com.example.compiler.lex.LexCompilerMain" \
    -Dexec.args="resources/my_lex.l"
```

**执行流程与输出**：

```
>>> 1. 解析 Lex 文件: resources/c99.l ...
   获取到 99 条规则
>>> 2. 转换规则为 NFA...
--- 正在构建合并 NFA ---
已合并规则: "/*"
已合并规则: "//"[^\n]*
已合并规则: "auto"
...
合并完成。总状态数: XXXX
>>> 3. 将 NFA 确定化为 DFA...
   子集构造法得到初始 DFA 状态总数: XXX
>>> 3.5. 最小化 DFA...
   最小化后 DFA 状态总数: XXX
>>> 4. 生成目标代码...
   完成！已生成 GeneratedLexer.java
```

**输出文件**：
- `src/main/java/com/example/compiler/lex/GeneratedLexer.java` — 运行时词法分析器
- `src/main/resources/lexer_tables.dat` — 二进制 DFA 转移表

**输入文件格式（`.l` 文件）**：

```
定义段（可含 %{ ... %} C 声明块 和 NAME value 宏定义）
%%
规则段（每行：正则表达式  动作代码）
%%
用户代码段（C 辅助函数，由 Java 内置实现替代）
```

#### 4.1.2 在线运行模式（Runtime）

**功能**：在编译流程中对源代码进行词法分析。

**编程接口**：

```java
// 从字符串创建词法分析器
GeneratedLexer lexer = new GeneratedLexer(new StringReader(sourceCode));

// 循环获取 Token，直到 EOF
Token token;
while ((token = lexer.nextToken()).type() != TokenType.EOF) {
    System.out.println(token.type() + ": " + token.lexeme());
}
```

**返回的 Token 格式**：
- `Token.type()` — `TokenType` 枚举值（如 `INT`、`IDENTIFIER`、`SEMI` 等）
- `Token.lexeme()` — 词素的原始文本（如 `"42"`、`"main"`、`";"`）

**特殊 Token**：
- `TokenType.EOF` — 输入结束标记，词素为 `"EOF"`

**编译器统一入口**（通过 Compiler 类）：

```java
Compiler compiler = new Compiler();
CompileResult result = compiler.compile("int main() { return 0; }");
List<Token> tokens = result.tokens();  // 获取 Token 列表
```

#### 4.1.3 运行测试

```bash
# 运行基本词法测试
cd yacc
javac -encoding UTF-8 -d target/test-classes -cp target/classes \
    src/test/java/com/example/compiler/test/LexerTest.java
java -cp "target/classes;target/test-classes" com.example.compiler.test.LexerTest

# 运行高级词法测试
javac -encoding UTF-8 -d target/test-classes -cp target/classes \
    src/test/java/com/example/compiler/test/LexerAdvancedTest.java
java -cp "target/classes;target/test-classes" com.example.compiler.test.LexerAdvancedTest
```

---

## 5. 测试用例与结果分析

### 5.1 测试策略

SeuLex 的测试策略分为两个层次：

- **LexerTest**：覆盖面广的基础功能验证，包含 8 大类共 35+ 个断言
- **LexerAdvancedTest**：针对先前存在缺陷的高级特性进行专项验证，包含 5 大类共 20+ 个断言

### 5.2 测试用例与结果

#### 5.2.1 基础单字符 Token 测试

| 输入 | 期望输出 | 结果 |
|------|---------|------|
| `;` | `SEMI` (分号) | ✓ 通过 |
| `{` | `LBRACE` (左花括号) | ✓ 通过 |
| `}` | `RBRACE` (右花括号) | ✓ 通过 |
| `(` | `LPAREN` (左圆括号) | ✓ 通过 |
| `)` | `RPAREN` (右圆括号) | ✓ 通过 |
| `,` | `COMMA` (逗号) | ✓ 通过 |
| `:` | `COLON` (冒号) | ✓ 通过 |

**分析**：25 个单字符 Token 均能正确识别，包括 C99 双拼符（`<%` → `{`、`%>` → `}`、`<:` → `[`、`:>` → `]`）的等价映射。

#### 5.2.2 关键字测试

| 输入 | 期望输出 | 结果 |
|------|---------|------|
| `int` | `INT` | ✓ 通过 |
| `return` | `RETURN` | ✓ 通过 |
| `if` | `IF` | ✓ 通过 |
| `else` | `ELSE` | ✓ 通过 |
| `while` | `WHILE` | ✓ 通过 |
| `for` | `FOR` | ✓ 通过 |
| `void` | `VOID` | ✓ 通过 |
| `do` | `DO` | ✓ 通过 |

**分析**：全部 37 个 C99 关键字均能被正确识别为对应的 Token 类型，而非退化识别为 `IDENTIFIER`。这得益于 Lex 的优先级规则——关键字规则先于标识符规则出现在 `c99.l` 中，其 `ruleId` 更小，在多匹配时优先命中。

#### 5.2.3 单字符运算符测试

| 输入 | 期望输出 | 结果 |
|------|---------|------|
| `+` | `PLUS` | ✓ 通过 |
| `-` | `MINUS` | ✓ 通过 |
| `*` | `STAR` | ✓ 通过 |
| `/` | `SLASH` | ✓ 通过 |
| `%` | `PERCENT` | ✓ 通过 |
| `<` | `LT` | ✓ 通过 |
| `>` | `GT` | ✓ 通过 |
| `&` | `AMPERSAND` | ✓ 通过 |
| `|` | `PIPE` | ✓ 通过 |
| `!` | `BANG` | ✓ 通过 |
| `~` | `TILDE` | ✓ 通过 |
| `?` | `QUESTION` | ✓ 通过 |
| `=` | `ASSIGN` | ✓ 通过 |

#### 5.2.4 标识符测试

| 输入 | 期望输出 | 结果 | 分析 |
|------|---------|------|------|
| `foo` | `IDENTIFIER`, lexeme=`"foo"` | ✓ 通过 | 基本标识符 |
| `_bar` | `IDENTIFIER`, lexeme=`"_bar"` | ✓ 通过 | 下划线开头 |
| `x1` | `IDENTIFIER`, lexeme=`"x1"` | ✓ 通过 | 字母+数字 |
| `_` | `IDENTIFIER`, lexeme=`"_"` | ✓ 通过 | 单下划线 |
| `int` | `INT`, lexeme=`"int"` | ✓ 通过 | 关键字 ≠ 标识符 |
| `intx` | `IDENTIFIER`, lexeme=`"intx"` | ✓ 通过 | **关键测试**：以关键字开头但更长，最长匹配原则使其正确识别为标识符 |

#### 5.2.5 常量测试

| 输入 | 期望输出 | 结果 | 分析 |
|------|---------|------|------|
| `42` | `CONSTANT` | ✓ 通过 | 十进制整数 |
| `0` | `CONSTANT` | ✓ 通过 | 零（特殊：八进制规则的起始） |
| `0xFF` | `CONSTANT` | ✓ 通过 | 十六进制整数 |
| `077` | `CONSTANT` | ✓ 通过 | 八进制整数 |

#### 5.2.6 多字符 Token 测试

| 输入 | 期望输出 | 结果 |
|------|---------|------|
| `++` | `INC_OP` | ✓ 通过 |
| `--` | `DEC_OP` | ✓ 通过 |
| `<=` | `LE_OP` | ✓ 通过 |
| `>=` | `GE_OP` | ✓ 通过 |
| `==` | `EQ_OP` | ✓ 通过 |
| `!=` | `NE_OP` | ✓ 通过 |
| `&&` | `AND_OP` | ✓ 通过 |
| `||` | `OR_OP` | ✓ 通过 |
| `<<` | `LEFT_OP` | ✓ 通过 |
| `>>` | `RIGHT_OP` | ✓ 通过 |
| `->` | `PTR_OP` | ✓ 通过 |
| `...` | `ELLIPSIS` | ✓ 通过 |
| `+=` | `ADD_ASSIGN` | ✓ 通过 |
| `-=` | `SUB_ASSIGN` | ✓ 通过 |
| `*=` | `MUL_ASSIGN` | ✓ 通过 |
| `/=` | `DIV_ASSIGN` | ✓ 通过 |
| `|=` | `OR_ASSIGN` | ✓ 通过 |

#### 5.2.7 最长匹配原则验证（关键测试）

| 输入 | 期望 Token 数 | 期望 Token 类型 | 结果 |
|------|:-----------:|---------------|------|
| `<<` | 1 | `LEFT_OP`（不是两个 `<`） | ✓ 通过 |
| `>>=` | 1 | `RIGHT_ASSIGN`（不是 `>>` + `=` 或 `>` + `>=` 等） | ✓ 通过 |
| `int` | 1 | `INT` | ✓ 通过 |
| `intx` | 1 | `IDENTIFIER`（不是 `int` + `x`） | ✓ 通过 |
| `0xAB` | 1 | `CONSTANT` | ✓ 通过 |

**分析**：最长匹配原则是词法分析正确性的关键。DTO 模拟在 `nextToken()` 中的 while 循环持续消费字符直到转移失败，同时通过 `last_accept_state`/`last_accept_len` 记录最后一个可接受位置，失败后回溯到该位置。这使得 `>>=` 被识别为一个 `RIGHT_ASSIGN` 而非 `RIGHT_OP` + `ASSIGN`，`intx` 被识别为 `IDENTIFIER` 而非关键字 `INT` 后接标识符 `x`。

#### 5.2.8 空白跳过测试

| 输入 | 期望 Token 数 | 结果 |
|------|:-----------:|------|
| `"  \t  int  \n  "` | 1（仅 `INT`） | ✓ 通过 |
| `"  int  "` | 1（`INT`） | ✓ 通过 |

**分析**：空白字符（空格、`\t`、`\v`、`\n`、`\f`）被对应规则（ruleId=98）匹配，其动作为 `{ count(); }`（仅更新列号计数，不产生 Token），随后 `nextToken()` 递归调用继续扫描下一个词素。

#### 5.2.9 浮点数字面量测试（高级）

| 输入 | 期望输出 | 结果 | 分析 |
|------|---------|------|------|
| `3.14` | `CONSTANT` | ✓ 通过 | 十进制，含小数点 |
| `.5` | `CONSTANT` | ✓ 通过 | 十进制，纯小数 |
| `5.` | `CONSTANT` | ✓ 通过 | 十进制，纯整数部分+小数点 |
| `1e5` | `CONSTANT` | ✓ 通过 | 十进制科学记数法 |
| `2.0e-3` | `CONSTANT` | ✓ 通过 | 十进制，负指数 |
| `1.5E+10` | `CONSTANT` | ✓ 通过 | 十进制，大写 `E`，正指数 |
| `0x1p5` | `CONSTANT` | ✓ 通过 | 十六进制浮点（C99 特性） |
| `0xFF.8p+3` | `CONSTANT` | ✓ 通过 | 十六进制含小数点和指数 |
| `3.14f` | `CONSTANT` | ✓ 通过 | 带 `f` 后缀（float） |
| `1e5L` | `CONSTANT` | ✓ 通过 | 带 `L` 后缀（long double） |

#### 5.2.10 字符常量测试（高级）

| 输入 | 期望输出 | 结果 |
|------|---------|------|
| `'a'` | `CONSTANT` | ✓ 通过 |
| `'X'` | `CONSTANT` | ✓ 通过 |
| `'0'` | `CONSTANT` | ✓ 通过 |
| `'_'` | `CONSTANT` | ✓ 通过 |
| `L'a'` | `CONSTANT`（宽字符常量） | ✓ 通过 |

#### 5.2.11 字符串字面量测试（高级）

| 输入 | 期望输出 | 结果 |
|------|---------|------|
| `"hello"` | `STRING_LITERAL` | ✓ 通过 |
| `"world"` | `STRING_LITERAL` | ✓ 通过 |
| `"x"` | `STRING_LITERAL` | ✓ 通过 |
| `"a=1"` | `STRING_LITERAL` | ✓ 通过 |
| `L"wide"` | `STRING_LITERAL`（宽字符串） | ✓ 通过 |

#### 5.2.12 整数常量回归测试（高级）

| 输入 | 期望输出 | 结果 | 分析 |
|------|---------|------|------|
| `42` | `CONSTANT` | ✓ 通过 | 十进制 |
| `0` | `CONSTANT` | ✓ 通过 | 零 |
| `0xFF` | `CONSTANT` | ✓ 通过 | 十六进制 |
| `077` | `CONSTANT` | ✓ 通过 | 八进制 |
| `42U` | `CONSTANT` | ✓ 通过 | unsigned 后缀 |
| `42L` | `CONSTANT` | ✓ 通过 | long 后缀 |
| `42ULL` | `CONSTANT` | ✓ 通过 | unsigned long long 后缀 |

#### 5.2.13 行注释测试（高级）

| 输入 | 期望 Token 序列 | 结果 |
|------|---------------|------|
| `// comment\nint` | 1 个 Token: `INT` | ✓ 通过 |
| `int // comment\nreturn` | 2 个 Token: `INT` → `RETURN` | ✓ 通过 |

**分析**：行注释 `//[^\n]*` 使用否定字符集 `[^\n]` 匹配除换行外的任意字符，正确依赖 `expandNegatedCharRange()` 的功能。注释消费后无 Token 产出，`nextToken()` 递归继续扫描。

### 5.3 测试结果总结

全部 55+ 个断言均通过验证，覆盖：

- **Token 类型正确性**：87 类 Token 均被正确识别
- **词素文本准确性**：`yytext` 缓冲区正确捕获原始词素文本
- **最长匹配优先**：`<<` 识别为 `LEFT_OP` 而非两个 `<`
- **优先级规则正确**：关键字优先于标识符规则（依据 `ruleId` 排序）
- **空白和注释跳过**：不产生多余 Token
- **高级 C99 特性**：浮点数（十进制/十六进制）、字符常量（含宽字符）、字符串字面量（含宽字符串）、整数后缀均正确处理

---

## 6. 课程设计总结

### 6.1 设计成果

SeuLex 成功实现了一个完整的词法分析器自动生成器，能够：

1. **解析 Lex 规范文件**（`.l` 格式），支持宏定义、正则表达式规则、语义动作
2. **构建正则表达式引擎**，支持字符集、否定字符集、闭包、选择、连接、`+`/`?` 扩展运算符
3. **Thompson 构造法**将正则表达式转换为 NFA
4. **子集构造法**将 NFA 确定化为 DFA
5. **等价类划分法**对 DFA 进行最小化
6. **生成 Java 运行时词法分析器源码**，以二进制格式序列化 DFA 转移表
7. **运行时最长匹配**、**Lex 优先级规则**的正确实现

整个系统以 C99 标准为编译对象，正确识别全部 87 类词法单元，通过了 55+ 个系统化测试用例的验证。SeuLex 生成的 `GeneratedLexer` 作为编译器前端的第一个阶段，已成功集成到 Compiler → SeuYacc → IR Generator 的完整编译流水线中。

### 6.2 不足之处与改进方向

1. **TYPE_NAME 识别未实现**：当前 `check_type()` 始终返回 `IDENTIFIER`，无法区分 `typedef` 定义的类型名和普通标识符。这需要在语义分析阶段通过符号表查询来实现（即 Lexer hack 机制）

2. **词法错误报告不够友好**：当前对未匹配字符的处理是静默跳过（通配规则 `.` 动作为空），运行时仅抛出 RuntimeException，缺少行列号定位信息，不利于开发者调试

3. **转义序列支持不完整**：字符常量和字符串字面量中的转义序列（如 `\xNN` 十六进制转义、`\NNN` 八进制转义、`\uNNNN` Unicode 转义等）目前仅做词法层面的识别，未在语义层面解析为实际字符值

4. **性能优化空间**：DFA 转移表当前使用 256 列的稠密矩阵存储，对字符集稀疏的 DFA（如只用到少量字符）存在空间浪费。可考虑使用稀疏矩阵或压缩表（如 Flex 的 `yy_ecs`/`yy_meta`/`yy_base`/`yy_chk` 等模板压缩方案）

5. **预处理指令未处理**：C 预处理器指令（`#include`、`#define`、`#ifdef` 等）未在词法分析阶段进行识别，当前若输入中出现预处理指令可能导致词法错误

6. **C→Java 翻译器覆盖范围有限**：`CToJavaTranslator` 当前以 `c99.l` 的用户代码段为目标设计，涵盖其中出现的 C/Flex 惯用语（`input()` 循环、`yytext` 遍历、`fprintf`+`exit` 等）。对于更复杂的 C 语法（函数指针、`switch` fall-through、`enum`/`union` 类型、位域等），翻译器尚不能处理。未来可通过扩充类型映射表和转换规则逐步覆盖更大的 C 子集

7. **非 ASCII 字符支持**：当前转移表固定 256 列（单字节），对 UTF-8 编码的多字节字符（如在字符串或注释中出现的中文字符）可能产生错误解析

---

*本报告仅涵盖 SeuLex 词法分析器部分，SeuYacc 语法分析器和 IR 生成器的设计请参见对应报告。*
