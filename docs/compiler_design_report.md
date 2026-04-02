# 编译原理专题实践详细设计报告

## 1. 课题名称

**基于 Lex/Yacc 思想的小型编译器前端设计与实现**

---

## 2. 项目概述

本项目模仿经典 `Lex/Yacc` 的设计思想，构建一个小型编译器前端系统。系统由三部分组成：

- `SeuLex`：词法分析程序生成器
- `SeuYacc`：语法分析程序生成器
- `IR Generator`：语义分析与中间代码生成模块

系统目标不是手写一个固定语言的词法分析器和语法分析器，而是实现一个规则驱动的分析器生成框架：

1. 从 `.l` 风格文件读取词法规则；
2. 从 `.y` 风格文件读取语法规则；
3. 自动构造词法分析器和语法分析器；
4. 在一个 C 语言子集上生成抽象语法树和中间代码；
5. 打通从源程序到中间表示的完整编译前端流程。

---

## 3. 系统总体设计

### 3.1 总体流程

```text
源程序
  ↓
SeuLex 生成的词法分析器
  ↓
Token 流
  ↓
SeuYacc 生成的语法分析器
  ↓
抽象语法树 AST
  ↓
语义分析 / 符号表构造
  ↓
三地址码 / 四元式
```

### 3.2 模块划分

系统分为四层：

1. **规则输入层**
   - 词法规则文件 `.l`
   - 语法规则文件 `.y`

2. **生成器层**
   - `SeuLex`：负责从正则规则生成词法分析器
   - `SeuYacc`：负责从文法规则生成语法分析器

3. **运行时层**
   - 词法分析运行时
   - LR 分析总控运行时

4. **语义翻译层**
   - AST 构造
   - 符号表管理
   - 中间代码生成

### 3.3 GitHub 仓库建议结构

```text
project/
├── docs/
│   ├── compiler_design_report.md
│   ├── weekly-progress.md
│   └── images/
├── common/
│   ├── token.h
│   ├── ast.h
│   ├── symbol.h
│   └── quad.h
├── seulex/
│   ├── lex_parser.cpp
│   ├── regex_parser.cpp
│   ├── nfa_builder.cpp
│   ├── dfa_builder.cpp
│   ├── dfa_minimizer.cpp
│   └── lexer_runtime.cpp
├── seuyacc/
│   ├── yacc_parser.cpp
│   ├── grammar.cpp
│   ├── first_set.cpp
│   ├── lr1_builder.cpp
│   ├── lalr_builder.cpp
│   ├── parse_table.cpp
│   └── parser_runtime.cpp
├── semantic/
│   ├── semantic_analyzer.cpp
│   ├── symbol_table.cpp
│   └── ir_generator.cpp
├── tests/
│   ├── lex_cases/
│   ├── yacc_cases/
│   └── integration/
└── main.cpp
```

---

## 4. 目标语言子集设计

为保证课程设计能够按期完成，同时覆盖编译前端的核心知识点，本项目不直接处理完整 C99，而是选择一个 **C 语言子集** 作为目标语言。

### 4.1 支持内容

- 基本类型：`int`
- 标识符：`ID`
- 整型常量：`NUM`
- 算术运算：`+ - * /`
- 关系运算：`< > <= >= == !=`
- 赋值语句：`a = b + 1;`
- 声明语句：`int a;`、`int b = 3;`
- 复合语句：`{ ... }`
- 选择语句：`if (...) ... else ...`
- 循环语句：`while (...) ...`
- 返回语句：`return expr;`
- 主函数：`int main() { ... }`

### 4.2 设计理由

1. 能够覆盖词法分析、语法分析和语义分析的主要流程；
2. 包含表达式、控制流和作用域等关键结构；
3. 便于构造 AST 和中间代码；
4. 难度适中，适合三人并行开发。

---

## 5. 三人分工与协作方式

### 5.1 成员一：SeuLex 模块负责人

负责内容：

- 设计并解析 `.l` 风格规则文件
- 扩展正规式展开与预处理
- 正规式转 NFA
- NFA 转 DFA
- DFA 最小化
- 词法分析运行时实现
- Token 流输出与测试

输出给下游模块：

- `TokenType`
- `Token`
- 错误位置（行号、列号）

### 5.2 成员二：SeuYacc 模块负责人

负责内容：

- 设计并解析 `.y` 风格规则文件
- 构造产生式集合
- 计算 First 集
- 构造 LR(1) 项目集规范族
- 构造 ACTION / GOTO 表
- 实现 LALR(1) 状态合并（可选优化）
- 实现 LR 分析总控
- 构造 AST

输出给下游模块：

- `ASTNode* root`
- 语法错误信息
- 调试用项目集与分析表

### 5.3 成员三：语义分析与中间代码负责人

负责内容：

- AST 节点需求确认
- 符号表设计与实现
- 声明、赋值、表达式、控制流语义处理
- 作用域管理
- 三地址码 / 四元式生成
- 联调测试

输出结果：

- 符号表
- 四元式序列
- 语义错误信息

### 5.4 三人共同任务

- 确定目标语言子集
- 统一 Token 名称
- 统一 AST 结构
- 统一接口与错误格式
- 共同完成联调、测试与文档撰写

---

## 6. 公共数据结构设计

为便于模块对接，系统将公共数据结构统一定义在 `common/` 目录下。

### 6.1 Token 结构

```cpp
struct Token {
    TokenType type;
    std::string lexeme;
    int line;
    int column;
};
```

### 6.2 产生式结构

```cpp
struct Production {
    int id;
    std::string left;
    std::vector<std::string> right;
};
```

### 6.3 AST 节点结构

```cpp
struct ASTNode {
    NodeType type;
    std::string text;
    std::vector<ASTNode*> children;
};
```

### 6.4 符号表项结构

```cpp
struct Symbol {
    std::string name;
    std::string type;
    int scopeLevel;
    bool initialized;
};
```

### 6.5 四元式结构

```cpp
struct Quad {
    std::string op;
    std::string arg1;
    std::string arg2;
    std::string result;
};
```

---

## 7. SeuLex 详细设计

## 7.1 功能目标

`SeuLex` 的目标是读取 `.l` 规则文件，自动构造词法分析器，将源程序字符流转换为 Token 流。

其处理流程为：

1. 解析 `.l` 文件；
2. 展开宏定义；
3. 解析正规式；
4. 构造 NFA；
5. 合并总 NFA；
6. 确定化为 DFA；
7. 最小化 DFA；
8. 由 DFA 驱动扫描器。

### 7.1.1 Lex 文件结构

```lex
定义区
%%
规则区
%%
用户代码区
```

示例：

```lex
DIGIT   [0-9]
LETTER  [a-zA-Z_]
ID      {LETTER}({LETTER}|{DIGIT})*

%%
"int"           { return INT; }
"if"            { return IF; }
"while"         { return WHILE; }
{ID}             { return ID; }
{DIGIT}+         { return NUM; }
"+"             { return PLUS; }
[ \t\n]+        { /* skip */ }
%%
```

### 7.1.2 NFA 状态结构

```cpp
struct NFAState {
    int id;
    std::map<char, std::vector<NFAState*>> trans;
    std::vector<NFAState*> epsilon;
    bool isAccept;
    int tokenType;
};
```

### 7.1.3 DFA 状态结构

```cpp
struct DFAState {
    int id;
    std::set<int> nfaStates;
    std::map<char, int> trans;
    bool isAccept;
    int tokenType;
};
```

### 7.1.4 SeuLex 核心接口

```cpp
bool loadLexFile(const std::string& filename);
bool buildLexer();
Token getNextToken();
```

### 7.1.5 SeuLex 伪代码

#### 伪代码 1：解析 `.l` 文件

```text
Algorithm ParseLexFile(file)
Input:  .l 规则文件
Output: definitions, rules, userCode
1. state ← DEFINITIONS
2. for each line in file:
3.     if line == "%%" and state == DEFINITIONS:
4.         state ← RULES
5.         continue
6.     if line == "%%" and state == RULES:
7.         state ← USERCODE
8.         continue
9.     if state == DEFINITIONS:
10.        parse macro definition
11.    else if state == RULES:
12.        parse regex-action pair
13.    else:
14.        append to userCode
15. return definitions, rules, userCode
```

#### 伪代码 2：宏展开

```text
Algorithm ExpandRegex(regex, definitions)
Input:  原始正规式 regex, 宏定义表 definitions
Output: 展开后的正规式
1. result ← regex
2. for each macro in definitions:
3.     replace all occurrences of {macro.name} in result with (macro.regex)
4. return result
```

#### 伪代码 3：正规式转后缀表达式

```text
Algorithm RegexToPostfix(regex)
Input:  中缀正规式
Output: 后缀表达式
1. insert explicit concatenation operator '.' into regex
2. create empty operator stack opStack
3. create empty output list postfix
4. for each symbol ch in regex:
5.     if ch is operand:
6.         append ch to postfix
7.     else if ch is '(': 
8.         push ch to opStack
9.     else if ch is ')':
10.        pop operators until '(' appears
11.    else:
12.        while precedence(top(opStack)) >= precedence(ch):
13.            pop to postfix
14.        push ch to opStack
15. pop remaining operators to postfix
16. return postfix
```

#### 伪代码 4：Thompson 构造 NFA

```text
Algorithm BuildNFA(postfix)
Input:  后缀正规式
Output: NFA fragment
1. create empty stack S
2. for each token x in postfix:
3.     if x is operand:
4.         create basic NFA fragment f
5.         push f into S
6.     else if x is concatenation:
7.         b ← pop S, a ← pop S
8.         connect a.accept to b.start with epsilon
9.         push merged fragment into S
10.    else if x is union '|':
11.        b ← pop S, a ← pop S
12.        create new start and accept
13.        connect new start to a.start and b.start
14.        connect a.accept and b.accept to new accept
15.        push merged fragment into S
16.    else if x is closure '*':
17.        a ← pop S
18.        create new start and accept
19.        add loop and bypass edges
20.        push merged fragment into S
21. return top(S)
```

#### 伪代码 5：NFA 合并

```text
Algorithm MergeNFAs(nfaList)
Input:  多个规则对应的 NFA 列表
Output: 总 NFA
1. create new globalStart
2. for each NFA_i in nfaList:
3.     add epsilon edge from globalStart to NFA_i.start
4. return NFA(globalStart)
```

#### 伪代码 6：子集构造 DFA

```text
Algorithm SubsetConstruction(NFA)
Input:  总 NFA
Output: DFA state set
1. startSet ← epsilonClosure({NFA.start})
2. create DFA state D0 from startSet
3. queue ← [D0]
4. while queue not empty:
5.     T ← pop queue
6.     for each input symbol a:
7.         U ← epsilonClosure(move(T, a))
8.         if U is empty:
9.             continue
10.        if U not in DFA states:
11.            create new DFA state for U
12.            push it into queue
13.        add transition T --a--> U
14. return DFA states
```

#### 伪代码 7：DFA 最小化

```text
Algorithm MinimizeDFA(DFA)
Input:  DFA
Output: Minimized DFA
1. partition P into accepting and non-accepting states
2. repeat
3.     changed ← false
4.     for each group G in P:
5.         split G according to transition behavior
6.         if G is split:
7.             replace G in P
8.             changed ← true
9. until changed == false
10. build minimized DFA from partition P
11. return minimized DFA
```

#### 伪代码 8：词法扫描

```text
Algorithm NextToken(input)
Input:  源程序字符流
Output: one token
1. state ← DFA.start
2. lastAcceptState ← null
3. lastAcceptPos ← current input position
4. while current character can transition from state:
5.     state ← transition(state, current character)
6.     advance input pointer
7.     if state is accepting:
8.         lastAcceptState ← state
9.         lastAcceptPos ← current input position
10. if lastAcceptState is null:
11.    report lexical error
12.    skip one character and retry
13. rollback input pointer to lastAcceptPos
14. return token corresponding to lastAcceptState
```

### 7.1.6 词法错误处理

当无法匹配任何规则时：

- 报告非法字符；
- 标记行号、列号；
- 跳过当前字符继续扫描。

---

## 8. SeuYacc 详细设计

## 8.1 功能目标

`SeuYacc` 的目标是读取 `.y` 文法文件，自动构造 LR(1) 或 LALR(1) 分析器，对 Token 流进行语法分析，并输出 AST。

其处理流程为：

1. 解析 `.y` 文件；
2. 提取终结符、非终结符和产生式；
3. 计算 First 集；
4. 构造 LR(1) 项目集规范族；
5. 构造 ACTION / GOTO 表；
6. 执行移进-归约分析；
7. 构造 AST。

### 8.1.1 Yacc 文件结构

```yacc
说明定义区
%%
文法规则区
%%
用户代码区
```

示例：

```yacc
%token INT IF ELSE WHILE RETURN ID NUM
%left PLUS MINUS
%left MUL DIV

%%
program     : function_def ;
function_def: INT ID LPAREN RPAREN block ;
block       : LBRACE stmt_list RBRACE ;
stmt_list   : stmt_list stmt | stmt ;
stmt        : decl_stmt
            | assign_stmt
            | if_stmt
            | while_stmt
            | return_stmt
            | block ;
%%
```

### 8.1.2 LR(1) 项结构

```cpp
struct LR1Item {
    int productionId;
    int dotPos;
    std::string lookahead;
};
```

### 8.1.3 分析表项结构

```cpp
struct ActionEntry {
    enum Type { SHIFT, REDUCE, ACCEPT, ERROR } type;
    int value;
};
```

### 8.1.4 SeuYacc 核心接口

```cpp
bool loadYaccFile(const std::string& filename);
bool buildParser();
ASTNode* parse();
```

### 8.1.5 SeuYacc 伪代码

#### 伪代码 9：解析 `.y` 文件

```text
Algorithm ParseYaccFile(file)
Input:  .y 文件
Output: declarations, productions, userCode
1. state ← DECLARATIONS
2. for each line in file:
3.     if line == "%%" and state == DECLARATIONS:
4.         state ← PRODUCTIONS
5.         continue
6.     if line == "%%" and state == PRODUCTIONS:
7.         state ← USERCODE
8.         continue
9.     if state == DECLARATIONS:
10.        parse token and precedence declarations
11.    else if state == PRODUCTIONS:
12.        parse grammar productions
13.    else:
14.        append to userCode
15. return declarations, productions, userCode
```

#### 伪代码 10：计算 First 集

```text
Algorithm ComputeFirst(grammar)
Input:  文法 grammar
Output: FIRST sets
1. initialize FIRST(X) for all grammar symbols X
2. repeat
3.     changed ← false
4.     for each production A → X1 X2 ... Xn:
5.         add FIRST(X1) - {epsilon} into FIRST(A)
6.         if X1 can derive epsilon:
7.             continue to X2, X3 ...
8.         if all Xi can derive epsilon:
9.             add epsilon into FIRST(A)
10.        if FIRST(A) changed:
11.            changed ← true
12. until changed == false
13. return FIRST
```

#### 伪代码 11：Closure 计算

```text
Algorithm Closure(I)
Input:  LR(1) item set I
Output: closed item set
1. J ← I
2. repeat
3.     changed ← false
4.     for each item [A → α · B β, a] in J:
5.         for each production B → γ:
6.             for each b in FIRST(βa):
7.                 if [B → · γ, b] not in J:
8.                     add [B → · γ, b] to J
9.                     changed ← true
10. until changed == false
11. return J
```

#### 伪代码 12：GOTO 计算

```text
Algorithm GOTO(I, X)
Input:  item set I, grammar symbol X
Output: item set
1. J ← empty set
2. for each item [A → α · X β, a] in I:
3.     add [A → α X · β, a] to J
4. return Closure(J)
```

#### 伪代码 13：构造 LR(1) 项目集规范族

```text
Algorithm BuildCanonicalCollection(grammar)
Input:  增广文法 grammar
Output: collection C of LR(1) item sets
1. I0 ← Closure({[S' → · S, $]})
2. C ← {I0}
3. queue ← [I0]
4. while queue not empty:
5.     I ← pop queue
6.     for each grammar symbol X:
7.         J ← GOTO(I, X)
8.         if J is not empty and J not in C:
9.             add J to C
10.            push J into queue
11.        record transition I --X--> J
12. return C
```

#### 伪代码 14：构造 ACTION/GOTO 表

```text
Algorithm BuildParseTable(C)
Input:  LR(1) item sets C
Output: ACTION table, GOTO table
1. for each state Ii in C:
2.     for each item [A → α · a β, b] in Ii where a is terminal:
3.         ACTION[i, a] ← shift j, where GOTO(Ii, a) = Ij
4.     for each item [A → α ·, a] in Ii and A ≠ S':
5.         ACTION[i, a] ← reduce A → α
6.     if [S' → S ·, $] in Ii:
7.         ACTION[i, $] ← accept
8.     for each nonterminal A:
9.         if GOTO(Ii, A) = Ij:
10.            GOTO[i, A] ← j
11. return ACTION, GOTO
```

#### 伪代码 15：LALR 状态合并

```text
Algorithm MergeLR1ToLALR(C)
Input:  LR(1) item sets C
Output: merged LALR item sets
1. group states with identical LR(0) cores
2. for each group G:
3.     merge lookaheads of items with same core
4. rebuild transitions among merged states
5. return merged collection
```

#### 伪代码 16：LR 分析总控

```text
Algorithm LRParse(tokens)
Input:  token sequence
Output: AST root or syntax error
1. stateStack ← [0]
2. symbolStack ← []
3. token ← nextToken()
4. loop:
5.     s ← top(stateStack)
6.     action ← ACTION[s, token.type]
7.     if action is shift t:
8.         push token to symbolStack
9.         push t to stateStack
10.        token ← nextToken()
11.    else if action is reduce A → β:
12.        pop |β| symbols and states
13.        node ← buildAST(A, β)
14.        s ← top(stateStack)
15.        push node to symbolStack
16.        push GOTO[s, A] to stateStack
17.    else if action is accept:
18.        return top(symbolStack)
19.    else:
20.        report syntax error
21.        return failure
```

#### 伪代码 17：AST 节点构造

```text
Algorithm BuildAST(A, beta)
Input:  reduction A → beta
Output: AST node
1. create new AST node node with type A
2. attach child nodes corresponding to beta in grammar order
3. if A represents expression/statement:
4.     normalize node shape according to AST design
5. return node
```

### 8.1.6 语法错误处理

当分析表中 `ACTION[state, token]` 为空时：

- 报告当前 token；
- 输出位置；
- 输出期望终结符集合；
- 停止分析并返回错误。

---

## 9. 语义分析与中间代码生成详细设计

## 9.1 功能目标

该模块接收 AST，进行语义分析并生成中间代码。中间表示采用 **三地址码 / 四元式** 形式。

主要任务：

1. 构造并维护符号表；
2. 检查重复定义和未声明使用；
3. 管理语句块作用域；
4. 翻译表达式、赋值、条件和循环；
5. 生成四元式序列。

### 9.1.1 AST 节点类型

- `Program`
- `FunctionDef`
- `Block`
- `DeclStmt`
- `AssignStmt`
- `IfStmt`
- `WhileStmt`
- `ReturnStmt`
- `BinaryExpr`
- `UnaryExpr`
- `Identifier`
- `IntLiteral`

### 9.1.2 符号表组织方式

采用“**作用域栈 + 哈希表**”方式：

- 进入新块时压栈；
- 离开块时弹栈；
- 查找变量时从内层向外层查找。

### 9.1.3 IR 模块核心接口

```cpp
class IRGenerator {
public:
    void generate(ASTNode* root);
    std::vector<Quad> getQuads();
};
```

### 9.1.4 中间代码示例

```text
(+, a, b, t1)
(*, t1, 3, t2)
(assign, t2, -, c)
(jfalse, t3, -, L1)
(label, -, -, L1)
```

### 9.1.5 IR 伪代码

#### 伪代码 18：符号表查找

```text
Algorithm LookupSymbol(name)
Input:  symbol name
Output: symbol or null
1. for scope from innermost to outermost:
2.     if name exists in scope.table:
3.         return scope.table[name]
4. return null
```

#### 伪代码 19：声明语句处理

```text
Algorithm HandleDecl(node)
Input:  declaration AST node
Output: none
1. name ← declared identifier
2. if LookupCurrentScope(name) != null:
3.     report redefinition error
4.     return
5. insert symbol(name, type, currentScope)
6. if node has initializer expr:
7.     place ← TranslateExpr(expr)
8.     emit (assign, place, -, name)
```

#### 伪代码 20：表达式翻译

```text
Algorithm TranslateExpr(node)
Input:  expression AST node
Output: place
1. if node is IntLiteral:
2.     return literal value
3. if node is Identifier:
4.     check symbol exists
5.     return identifier name
6. if node is BinaryExpr(op, left, right):
7.     p1 ← TranslateExpr(left)
8.     p2 ← TranslateExpr(right)
9.     t ← NewTemp()
10.    emit (op, p1, p2, t)
11.    return t
```

#### 伪代码 21：赋值语句翻译

```text
Algorithm HandleAssign(node)
Input:  assignment AST node
Output: none
1. name ← left identifier
2. if LookupSymbol(name) == null:
3.     report undefined identifier
4.     return
5. place ← TranslateExpr(rightExpr)
6. emit (assign, place, -, name)
```

#### 伪代码 22：if 语句翻译

```text
Algorithm HandleIf(node)
Input:  if statement AST node
Output: none
1. condPlace ← TranslateExpr(node.condition)
2. Lfalse ← NewLabel()
3. Lend ← NewLabel()
4. emit (jfalse, condPlace, -, Lfalse)
5. TranslateStmt(node.thenBranch)
6. if node has elseBranch:
7.     emit (jmp, -, -, Lend)
8.     emit (label, -, -, Lfalse)
9.     TranslateStmt(node.elseBranch)
10.    emit (label, -, -, Lend)
11. else:
12.    emit (label, -, -, Lfalse)
```

#### 伪代码 23：while 语句翻译

```text
Algorithm HandleWhile(node)
Input:  while statement AST node
Output: none
1. Lbegin ← NewLabel()
2. Lend ← NewLabel()
3. emit (label, -, -, Lbegin)
4. condPlace ← TranslateExpr(node.condition)
5. emit (jfalse, condPlace, -, Lend)
6. TranslateStmt(node.body)
7. emit (jmp, -, -, Lbegin)
8. emit (label, -, -, Lend)
```

#### 伪代码 24：返回语句翻译

```text
Algorithm HandleReturn(node)
Input:  return statement AST node
Output: none
1. place ← TranslateExpr(node.expr)
2. emit (return, place, -, -)
```

#### 伪代码 25：语义分析总控

```text
Algorithm GenerateIR(root)
Input:  AST root
Output: quad list
1. initialize symbol table stack
2. recursively traverse AST root
3. when entering block:
4.     push new scope
5. when leaving block:
6.     pop scope
7. dispatch node by type:
8.     DeclStmt   → HandleDecl
9.     AssignStmt → HandleAssign
10.    IfStmt     → HandleIf
11.    WhileStmt  → HandleWhile
12.    ReturnStmt → HandleReturn
13. return quad list
```

### 9.1.6 语义错误处理

- 重复定义：同一作用域重复声明；
- 未声明使用：引用不存在变量；
- 类型错误：表达式类型不兼容；
- 返回值错误：函数返回类型不匹配。

---

## 10. 模块集成设计

### 10.1 接口约定

#### Lex → Yacc

- `TokenType` 名称必须完全一致；
- `Token` 中必须携带 `type`、`lexeme`、`line`、`column`。

#### Yacc → IR

- AST 节点类型必须统一；
- 表达式和语句节点的孩子顺序必须统一；
- 标识符和字面量节点必须保留原始文本。

### 10.2 集成流程

```text
main()
 ├── loadLexFile("grammar/test.l")
 ├── buildLexer()
 ├── loadYaccFile("grammar/test.y")
 ├── buildParser()
 ├── ast = parse()
 ├── generate(ast)
 └── print quads
```

### 10.3 联调检查项

- Lex 输出 token 是否与 Yacc 声明一致；
- Yacc 规约出的 AST 是否符合 IR 模块预期；
- 错误位置是否能在三个模块中保持一致；
- 测试样例是否能完整跑通。

---

## 11. 测试设计

## 11.1 测试目标

1. 验证词法规则解析正确性；
2. 验证 DFA 构造正确性；
3. 验证 LR 分析表构造正确性；
4. 验证 AST 构造正确性；
5. 验证四元式翻译正确性；
6. 验证整条流水线联调成功。

### 11.1.1 测试样例

源程序：

```c
int main() {
    int a;
    int b;
    a = 3;
    b = a + 2;
    if (b < 10) {
        b = b + 1;
    }
    return b;
}
```

### 11.1.2 预期 Token 序列

```text
INT ID LPAREN RPAREN LBRACE
INT ID SEMI
INT ID SEMI
ID ASSIGN NUM SEMI
ID ASSIGN ID PLUS NUM SEMI
IF LPAREN ID LT NUM RPAREN LBRACE
ID ASSIGN ID PLUS NUM SEMI
RBRACE
RETURN ID SEMI
RBRACE
```

### 11.1.3 预期中间代码

```text
(assign, 3, -, a)
(+, a, 2, t1)
(assign, t1, -, b)
(<, b, 10, t2)
(jfalse, t2, -, L1)
(+, b, 1, t3)
(assign, t3, -, b)
(label, -, -, L1)
(return, b, -, -)
```

---

## 12. 关键难点分析

### 12.1 Lex 部分难点

1. 扩展正规式展开可能导致括号与连接关系出错；
2. ε-closure 实现需要保证状态集合判重正确；
3. 最长匹配与规则优先级需要同时兼顾。

### 12.2 Yacc 部分难点

1. First 集与 Closure 计算细节较多；
2. LR(1) 状态数较多，调试成本高；
3. 文法稍有不慎就会引发冲突。

### 12.3 IR 部分难点

1. AST 结构若前期设计不统一，后期翻译困难；
2. 控制流翻译需要仔细维护标签和跳转；
3. 符号表作用域管理容易在块嵌套中出错。

### 12.4 联调难点

1. Token 名称和优先级规则必须一致；
2. AST 节点格式必须提前统一；
3. 三个模块调试输出应采用统一格式。

---

## 13. 进度安排

### 第 1 阶段：需求分析与总体设计

- 明确课程设计要求；
- 确定目标语言子集；
- 确定三人分工；
- 设计统一接口与数据结构；
- 完成详细设计报告。

### 第 2 阶段：核心模块实现

- 完成 Lex 文件解析和自动机构造；
- 完成 Yacc 文件解析和 LR 分析表构造；
- 完成 AST 节点框架和符号表框架。

### 第 3 阶段：中间代码与联调

- 实现表达式、赋值、控制流翻译；
- 联调词法、语法和语义模块；
- 补充测试样例与错误处理；
- 完成最终文档整理。

---

## 14. 预期成果

本项目预期产出如下：

1. `SeuLex`：一个 Lex 风格的词法分析程序生成器；
2. `SeuYacc`：一个 Yacc 风格的语法分析程序生成器；
3. 一个 AST 构造模块；
4. 一个符号表管理模块；
5. 一个三地址码 / 四元式中间代码生成模块；
6. 一套完整的测试样例；
7. 一份可放在 GitHub `docs/` 下的 Markdown 设计文档。

---

## 15. 总结

本项目基于 Lex/Yacc 的经典思想，构建了一个小型编译器前端系统。系统围绕词法分析、语法分析和中间代码生成三部分展开，通过三人协作分别完成 `SeuLex`、`SeuYacc` 和 `IR Generator`，并最终整合为从源程序到中间代码的完整流水线。

该设计既覆盖了正规式、自动机、上下文无关文法、LR 分析、语义翻译等编译原理核心内容，也体现了模块化设计、接口协同、错误处理和联调测试等软件工程实践。项目后续还可进一步扩展为支持更多语言成分、输出 LLVM IR 或增加简单优化模块。
