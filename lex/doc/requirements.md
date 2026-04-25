根据《编译原理专题实践 2026》PPT的内容，关于 **Lex（SeuLex）部分** 的所有需求与技术要点总结如下。这份文档旨在为 AI 提供高度一致的开发参考。

---

# SeuLex 词法分析程序生成器需求文档

## 1. 课程目的与基本流程
* [cite_start]**主要目的**：通过编写词法分析程序生成器（SeuLex），加深对编译程序构造过程的理解，增强系统软件编写能力 [cite: 1983, 1986, 1988]。
* **基本流程**：
    1.  [cite_start]输入：词法规则文件（如 `c99.l`） [cite: 1992]。
    2.  [cite_start]处理：SeuLex 读取规则并生成可执行的词法分析程序（如 `yylex.c` 或 Java 版源程序） [cite: 1993, 2005, 2007]。
    3.  [cite_start]运行：生成的词法分析程序读入测试用例程序，输出 Token 流供语法分析器使用 [cite: 1994, 2005]。

## 2. SeuLex 核心工作流程
[cite_start]Lex 部分的开发应遵循以下标准步骤 [cite: 2016-2023]：
1.  [cite_start]**输入文件解析**：解析 Lex 源文件，将 Lex 扩展正规表达式（Extended RE）转换为常规正规表达式 [cite: 2017]。
2.  [cite_start]**正规式解析**：将常规正规表达式转换为语法树或后缀表达式（逆波兰表示法） [cite: 2018]。
3.  [cite_start]**构造 NFA**：利用 Thompson 构造法等，将每个正规式转换为对应的 NFA 片段 [cite: 2019]。
4.  [cite_start]**合并 NFA**：创建一个新的开始节点，通过 $\epsilon$-边连接所有正规式的 NFA 起始节点，合并为一个巨大的 NFA [cite: 2020, 2531]。
5.  [cite_start]**NFA 确定化**：使用子集构造法将 NFA 转换为 DFA [cite: 2021]。
6.  [cite_start]**DFA 最小化**：根据 Action 是否相同进行初始划分，合并等价状态，得到最小 DFA [cite: 2022, 2480]。
7.  [cite_start]**代码生成**：根据最小 DFA 生成目标语言（C 或 Java）的词法分析程序 `yylex` [cite: 2023]。

## 3. Lex 输入文件格式规范
[cite_start]Lex 源文件（`.l`）由三部分组成，通过 `%%` 分隔 [cite: 2254-2255]：
`{definitions} %% {rules} %% {user subroutines}`

### 3.1 定义段 (Definitions Section)
* [cite_start]**C 代码块**：包含在 `%{` 和 `%}` 之间的内容直接复制到生成的文件中 [cite: 2257]。
* [cite_start]**正规定义 (Regular Definitions)**：从第一列开始定义，格式为 `name translation`（如 `digit [0-9]`） [cite: 2261-2262]。
* [cite_start]**作用域**：这些定义对后续所有规则可见 [cite: 2265]。

### 3.2 规则段 (Rules Section)
* [cite_start]**格式**：`regular_expression action` [cite: 2373]。
* [cite_start]**Action**：匹配到对应正则后执行的 C/Java 代码逻辑。如果 Action 为空语句 `;`，则忽略该匹配项 [cite: 2375-2376]。

### 3.3 用户辅助程序段 (User Subroutines Section)
* [cite_start]包含用户自定义的函数（如 `yywrap`、`comment` 等），原样复制到生成的文件末尾 [cite: 2255, 2516]。

## 4. 正规表达式（RE）语法支持
[cite_start]SeuLex 需支持以下 Lex 扩展操作符 [cite: 2270-2271, 2334]：
* [cite_start]**基本操作**：选择 `|`、打包 `()`、闭包 `*`、正闭包 `+`、可选 `?` [cite: 2313-2314, 2319, 2323]。
* **字符类**：
    * [cite_start]`[abc]`：匹配 a 或 b 或 c [cite: 2284]。
    * [cite_start]`[a-z]`：匹配范围内的字符 [cite: 2288]。
    * [cite_start]`[^abc]`：匹配补集（除了 a, b, c 之外的所有字符） [cite: 2298]。
* [cite_start]**通配符**：`.` 匹配除换行符外的任意符号 [cite: 2301]。
* **转义与引用**：
    * [cite_start]`\n`, `\t`, `\\`：C 风格转义 [cite: 2277-2280]。
    * [cite_start]`" "`：双引号内的所有内容视为目标语言符号（字面量） [cite: 2302-2303]。
* [cite_start]**引用宏**：使用 `{name}` 引用定义段中的正规定义 [cite: 2273-2274]。
* [cite_start]**特殊边界**：`$` 表示匹配行末 [cite: 2393]。

## 5. 正规表达式预处理逻辑
[cite_start]将扩展 RE 转为普通 RE 的处理顺序 [cite: 2351-2365]：
1.  **宏展开**：将 `{}` 内的 ID 替换为被定义的内容。
2.  **处理通配符**：将 `.` 展开为全集。
3.  **处理字符类**：将 `[xyz]` 转换为 `(x|y|z)`，处理范围 `-` 和补集 `^`。
4.  **处理引用**：处理双引号中的转义。
5.  **转换语法糖**：将 `?`、`+`、`{min, max}` 转换为基本算符形式。
6.  **显式插入连接符**：为中缀转后缀做准备。

## 6. 词法分析器的执行逻辑
[cite_start]生成的 `yylex` 程序需遵循以下规则处理二义性 [cite: 2417-2419]：
1.  [cite_start]**最长匹配原则**：优先匹配能识别最长字符串的规则（例如 `integers` 识别为标识符而非 `integer` 关键字） [cite: 2423, 2562]。
2.  [cite_start]**顺序优先原则**：如果多个规则匹配长度相同，排在前面的规则优先 [cite: 2443]。

### 关键变量与函数
* [cite_start]**`yytext`**：存储当前匹配到的字符串 [cite: 2381]。
* [cite_start]**`yyleng`**：当前匹配字符串的长度 [cite: 2386]。
* [cite_start]**`yyless(n)`**：仅保留匹配字符串的前 $n$ 个字符，其余回退 [cite: 2397]。
* [cite_start]**`yywrap()`**：处理输入文件结束逻辑，返回 1 表示正常结束 [cite: 2413]。

## 7. 数据结构设计建议
* [cite_start]**Rule 结构**：存储 `expr` (RE) 和 `action` (代码) [cite: 2518-2521]。
* [cite_start]**NFA 状态**：存储状态编号、出边（通常使用 `unordered_multimap`）及是否为接受态 [cite: 2524-2525]。
* [cite_start]**DFA 状态**：存储对应的 NFA 状态集合 [cite: 2538]。
* [cite_start]**状态转换表**：存储 DFA 状态变迁关系 [cite: 2536]。

## 8. 参考进度安排 (Lex 相关)
* [cite_start]**第 1 周**：确定 Lex 输入文件，设计解析算法和数据结构 [cite: 2085-2086]。
* [cite_start]**第 4 周**：完成 Lex 详细设计及算法伪代码 [cite: 2111]。
* [cite_start]**第 5 周**：实现 Lex 输入文件解析 (Lex RE $\to$ 常规 RE) [cite: 2118]。
* [cite_start]**第 6 周**：正规式解析（转后缀），构造 NFA 并实现可视化 [cite: 2127-2128]。
* [cite_start]**第 7 周**：实现 NFA 确定化及 DFA 可视化 [cite: 2139]。
* [cite_start]**第 8 周**：实现 DFA 最小化并根据 DFA 进行词法分析 [cite: 2146-2147]。
* [cite_start]**第 9 周**：联调测试与报告撰写 [cite: 2155-2156]。