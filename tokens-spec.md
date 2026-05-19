# C99 Token 统一规范

基于 `yacc/resources/c99.l` 和 `yacc/resources/c99.y` 的 token 全集定义。本文件是 lex 和 yacc 之间 token 接口的唯一权威规范。

---

## 1. 关键词类 (Keywords) — 37 个

| 序号 | Token | c99.l 产出 | c99.y 声明 | 说明 |
|------|-------|:---------:|:---------:|------|
| 1 | AUTO | ✓ | ✓ | `auto` |
| 2 | BOOL | ✓ | ✓ | `_Bool` |
| 3 | BREAK | ✓ | ✓ | `break` |
| 4 | CASE | ✓ | ✓ | `case` |
| 5 | CHAR | ✓ | ✓ | `char` |
| 6 | COMPLEX | ✓ | ✓ | `_Complex` |
| 7 | CONST | ✓ | ✓ | `const` |
| 8 | CONTINUE | ✓ | ✓ | `continue` |
| 9 | DEFAULT | ✓ | ✓ | `default` |
| 10 | DO | ✓ | ✓ | `do` |
| 11 | DOUBLE | ✓ | ✓ | `double` |
| 12 | ELSE | ✓ | ✓ | `else` |
| 13 | ENUM | ✓ | ✓ | `enum` |
| 14 | EXTERN | ✓ | ✓ | `extern` |
| 15 | FLOAT | ✓ | ✓ | `float` |
| 16 | FOR | ✓ | ✓ | `for` |
| 17 | GOTO | ✓ | ✓ | `goto` |
| 18 | IF | ✓ | ✓ | `if` |
| 19 | IMAGINARY | ✓ | ✓ | `_Imaginary` |
| 20 | INLINE | ✓ | ✓ | `inline` |
| 21 | INT | ✓ | ✓ | `int` |
| 22 | LONG | ✓ | ✓ | `long` |
| 23 | REGISTER | ✓ | ✓ | `register` |
| 24 | RESTRICT | ✓ | ✓ | `restrict` |
| 25 | RETURN | ✓ | ✓ | `return` |
| 26 | SHORT | ✓ | ✓ | `short` |
| 27 | SIGNED | ✓ | ✓ | `signed` |
| 28 | SIZEOF | ✓ | ✓ | `sizeof` |
| 29 | STATIC | ✓ | ✓ | `static` |
| 30 | STRUCT | ✓ | ✓ | `struct` |
| 31 | SWITCH | ✓ | ✓ | `switch` |
| 32 | TYPEDEF | ✓ | ✓ | `typedef` |
| 33 | UNION | ✓ | ✓ | `union` |
| 34 | UNSIGNED | ✓ | ✓ | `unsigned` |
| 35 | VOID | ✓ | ✓ | `void` |
| 36 | VOLATILE | ✓ | ✓ | `volatile` |
| 37 | WHILE | ✓ | ✓ | `while` |

---

## 2. 运算符类 (Operators) — 21 个

| 序号 | Token | 词素 | c99.l 产出 | c99.y 声明 |
|------|-------|------|:---------:|:---------:|
| 38 | RIGHT_ASSIGN | `>>=` | ✓ | ✓ |
| 39 | LEFT_ASSIGN | `<<=` | ✓ | ✓ |
| 40 | ADD_ASSIGN | `+=` | ✓ | ✓ |
| 41 | SUB_ASSIGN | `-=` | ✓ | ✓ |
| 42 | MUL_ASSIGN | `*=` | ✓ | ✓ |
| 43 | DIV_ASSIGN | `/=` | ✓ | ✓ |
| 44 | MOD_ASSIGN | `%=` | ✓ | ✓ |
| 45 | AND_ASSIGN | `&=` | ✓ | ✓ |
| 46 | XOR_ASSIGN | `^=` | ✓ | ✓ |
| 47 | OR_ASSIGN | `\|=` | ✓ | ✓ |
| 48 | RIGHT_OP | `>>` | ✓ | ✓ |
| 49 | LEFT_OP | `<<` | ✓ | ✓ |
| 50 | INC_OP | `++` | ✓ | ✓ |
| 51 | DEC_OP | `--` | ✓ | ✓ |
| 52 | PTR_OP | `->` | ✓ | ✓ |
| 53 | AND_OP | `&&` | ✓ | ✓ |
| 54 | OR_OP | `\|\|` | ✓ | ✓ |
| 55 | LE_OP | `<=` | ✓ | ✓ |
| 56 | GE_OP | `>=` | ✓ | ✓ |
| 57 | EQ_OP | `==` | ✓ | ✓ |
| 58 | NE_OP | `!=` | ✓ | ✓ |

---

## 3. 字面量/标识符类 (Literals & Identifiers) — 4 个

| 序号 | Token | c99.l 产出 | c99.y 声明 | 说明 |
|------|-------|:---------:|:---------:|------|
| 59 | IDENTIFIER | ✓ | ✓ | 用户标识符，由 `check_type()` 返回 |
| 60 | CONSTANT | ✓ | ✓ | 整型/浮点/字符常量 |
| 61 | STRING_LITERAL | ✓ | ✓ | 字符串字面量 |
| 62 | ELLIPSIS | ✓ | ✓ | `...` 变参省略号 |

---

## 4. 单字符 Token (Single-character) — 25 个

这些 token 在 c99.l 中直接返回字符的 ASCII 值（如 `return ';'`），在 c99.y 中也以字符字面量形式使用（如 `';'`）。不需要 `%token` 声明。

| 字符 | ASCII | 说明 |
|------|-------|------|
| `;` | 59 | 分号 |
| `{` | 123 | 左花括号（含 `<%` 双拼符） |
| `}` | 125 | 右花括号（含 `%>` 双拼符） |
| `,` | 44 | 逗号 |
| `:` | 58 | 冒号 |
| `=` | 61 | 等号 |
| `(` | 40 | 左圆括号 |
| `)` | 41 | 右圆括号 |
| `[` | 91 | 左方括号（含 `<:` 双拼符） |
| `]` | 93 | 右方括号（含 `:>` 双拼符） |
| `.` | 46 | 点号 |
| `&` | 38 | 取地址/位与 |
| `!` | 33 | 逻辑非 |
| `~` | 126 | 按位取反 |
| `-` | 45 | 减号/负号 |
| `+` | 43 | 加号/正号 |
| `*` | 42 | 乘号/解引用 |
| `/` | 47 | 除号 |
| `%` | 37 | 取模 |
| `<` | 60 | 小于 |
| `>` | 62 | 大于 |
| `^` | 94 | 按位异或 |
| `\|` | 124 | 按位或 |
| `?` | 63 | 三目条件 |
| `~` | 126 | 按位取反 |

---

## 5. 已知间隙: TYPE_NAME

| Token | c99.l 产出 | c99.y 声明 | 说明 |
|-------|:---------:|:---------:|------|
| TYPE_NAME | **✗** | ✓ | 语义分析阶段的 typedef 类型名识别 |

c99.l 中 `check_type()` 函数目前始终返回 `IDENTIFIER`（见 c99.l:186），尚未实现 typedef 类型名与普通标识符的区分。这是 TODO 项，需要在语义分析阶段通过符号表实现。

---

## 6. Token 编号方案

C 侧通过 `y.tab.h` 分配 token 常量值：

- **单字符 token**：直接使用 ASCII 值（0-255）
- **多字符 token**：从 256 开始递增分配，避免与 ASCII 冲突

Java 侧通过 `TokenType.java` 枚举定义，枚举值 `.name()` 须与 C 侧 token 名称一致。

---

## 7. 接口契约

```
C 侧:   c99.l (flex) ──返回 token ID──> yyparse() (yacc)
          │                                 │
          └──── 通过 y.tab.h 共享 token 编号 ──┘

Java 侧: GeneratedLexer ──产出 Token(TokenType,lexeme)──> ParserDriver
               │                                              │
               └── TokenType.name() 须匹配 Grammar 中 Terminal 名 ──┘
```

核心约束：
- Lexer 产出的每个 token，其名称必须存在于 Grammar 的 `%token` 声明或单字符字面量中
- Grammar 中每个 `%token` 声明，Lexer 必须能产出对应 token（TYPE_NAME 除外，为已知 TODO）

---

## 8. 汇总

| 类别 | 数量 | 编号范围 |
|------|------|---------|
| 关键词 | 37 | 256-292 |
| 运算符 | 21 | 304-324 |
| 字面量/标识符 | 4 | 300-303 |
| 单字符 | 25 | ASCII (33-126) |
| **总计（多字符）** | **62** | 256-324 |
| **总计（含单字符）** | **87** | — |
| 已知间隙 | 1 (TYPE_NAME) | 待分配 |
