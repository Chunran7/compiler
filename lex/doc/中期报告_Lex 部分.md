# Lex 词法分析器生成器 - 详细设计报告

**学生姓名**:李宏文
**学号**: 71123101  
**负责模块**: Lex 词法分析器设计与实现  
**提交日期**: 2026-04-02

---

## 一、系统架构设计

### 1.1 整体架构

```
┌─────────────────────────────────────────────────────────────┐
│                    LexCompilerMain                          │
│                     (主控制流程)                             │
└─────────────────────────────────────────────────────────────┘
                              │
        ┌─────────────────────┼─────────────────────┐
        ▼                     ▼                     ▼
┌──────────────┐    ┌──────────────────┐    ┌──────────────┐
│ SeuLexParser │    │ RegexConverter   │    │ NfaManager   │
│  (Lex 解析器)  │───▶│ (正则转换器)     │───▶│ (NFA 构建器)  │
└──────────────┘    └──────────────────┘    └──────────────┘
                                                   │
                                                   ▼
                                          ┌──────────────────┐
                                          │ NfaToDfaConverter│
                                          │  (NFA→DFA 转换)   │
                                          └──────────────────┘
                                                   │
                                                   ▼
                                          ┌──────────────────┐
                                          │   DfaState       │
                                          │  (DFA 状态类)     │
                                          └──────────────────┘
                                                   │
                                                   ▼
                                    ┌──────────────────────────┐
                                    │  CodeGenerator (待实现)   │
                                    │  (词法分析器代码生成)      │
                                    └──────────────────────────┘
                                                   │
                                                   ▼
                                          ┌──────────────────┐
                                          │ Generated Lexer  │
                                          │ (可执行的词法分析器)│
                                          └──────────────────┘
```

### 1.2 核心类设计

#### 1.2.1 `SeuLexParser` - Lex 文件解析器

**数据结构**:

```java
public static class LexRule {
    public int id;           // 规则 ID（优先级标识）
    public String regex;     // 处理后的正则表达式
    public String action;    // 对应的动作代码
}

private Map<String, String> regularDefs;  // 宏定义表
private List<LexRule> rules;              // 规则列表
```

**核心方法**:

- `splitLexFile(String)` - 分割 Lex 文件为三段
- `parseDefinitions()` - 解析宏定义
- `parseRules()` - 解析词法规则
- `expandMacros(String)` - 递归展开宏引用

#### 1.2.2 `RegexConverter` - 正则表达式转换器

**处理流程**:

```
原始正则 → 字符集展开 → 语法糖消除 → 插入连接符 → 中缀转后缀
   ↓           ↓            ↓            ↓           ↓
 a+b      [0-9]→(0|1|...|9)  a+→aa*     ab→a·b    ab|*
```

**核心方法**:

- `processCharSet(String)` - 处理 `[a-z]` 字符集
- `processPlusAndQuestion(String)` - 消除 `+` 和 `?`
- `insertConcatOperator(String)` - 显式化连接符
- `toPostfix(String)` - 调度场算法转后缀

#### 1.2.3 `NfaManager` - NFA 构建器

**NFA 状态结构**:

```java
class NfaState {
    int id;
    char transition;        // 'ε' 或具体字符
    List<NfaState> nextStates;
    boolean isAccept;
    int ruleId;             // 对应的规则 ID
}
```

**核心方法**:

- `buildCombinedNfa(List<LexRule>)` - 构建合并 NFA

#### 1.2.4 `NfaToDfaConverter` - DFA 确定化器

**DFA 状态结构**:

```java
class DfaState {
    int id;
    Set<NfaState> nfaStates;  // 包含的 NFA 状态集合
    Map<Character, DfaState> transitions;  // 转移表
    boolean isAccept;
    int acceptedRuleId;       // 优先级最高的规则 ID
}
```

**核心方法**:

- `epsilonClosure(Set<NfaState>)` - 计算ε-闭包
- `move(Set<NfaState>, char)` - 计算移动
- `convert(NfaState)` - 子集构造法主算法

#### 1.2.5 `DfaState` - DFA 状态类

**关键设计**:

```java
@Override
public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof DfaState)) return false;
    DfaState dfaState = (DfaState) o;
    return Objects.equals(nfaStates, dfaState.nfaStates);
}

@Override
public int hashCode() {
    return Objects.hash(nfaStates);
}
```

**优先级处理**:

```java
// 若一个 DFA 状态包含多个 NFA 接受态，选择 ruleId 最小的
if (this.acceptedRuleId == -1 || s.ruleId < this.acceptedRuleId) {
    this.acceptedRuleId = s.ruleId;
}
```

#### 1.2.6 `CodeGenerator` - 代码生成器（待实现）

**职责**: 根据 DFA 状态机和语义动作生成可执行的词法分析器代码

**设计思路**:

```java
public class CodeGenerator {

    /**
     * 生成词法分析器源代码
     */
    public String generateLexer(List<DfaState> dfaStates,
                                List<LexRule> rules) {
        StringBuilder code = new StringBuilder();

        // 1. 生成类声明和字段
        code.append(generateClassDeclaration());
        code.append(generateFields(dfaStates));

        // 2. 生成 DFA 转移表
        code.append(generateTransitionTable(dfaStates));

        // 3. 生成语义动作映射表
        code.append(generateActionMap(rules));

        // 4. 生成词法分析方法
        code.append(generateNextTokenMethod());

        // 5. 生成辅助方法
        code.append(generateHelperMethods());

        return code.toString();
    }

    /**
     * 生成 DFA 转移表（压缩存储）
     */
    private String generateTransitionTable(List<DfaState> states) {
        // 方案 1: 二维数组表示
        // TRANSITION_TABLE[stateId][charIndex] = nextStateId

        // 方案 2: 邻接表表示（稀疏图优化）
        // Map<Integer, Map<Character, Integer>>
    }

    /**
     * 生成语义动作包装类
     */
    private String generateActionMap(List<LexRule> rules) {
        // 将每个规则的 action 代码封装为 Runnable 或函数对象
        // ACTIONS.put(ruleId, () -> { action_code });
    }
}
```

**输出示例**:

```java
// 生成的词法分析器代码框架
public class GeneratedLexer {
    // DFA 转移表
    private static final int[][] TRANSITION_TABLE = {
        // 状态 0: [a-z]→1, [0-9]→2, ...
        {1, 2, 3, ...},
        // 状态 1: ...
    };

    // 语义动作映射
    private static final Map<Integer, Runnable> ACTIONS = new HashMap<>();

    static {
        ACTIONS.put(1, () -> { comment(); });
        ACTIONS.put(2, () -> { count(); return(AUTO); });
        // ... 更多规则
    }

    // 词法分析主方法
    public Token nextToken() {
        int currentState = 0;
        int lastAcceptState = -1;
        int inputPos = 0;

        while (true) {
            char c = input.peek(inputPos);
            int nextState = TRANSITION_TABLE[currentState][c];

            if (nextState == -1) break;  // 无转移

            currentState = nextState;
            inputPos++;

            if (DFA_INFO[currentState].isAccept) {
                lastAcceptState = currentState;
                lastAcceptPos = inputPos;
            }
        }

        // 执行最长匹配的动作
        if (lastAcceptState != -1) {
            input.consume(lastAcceptPos);
            int ruleId = DFA_INFO[lastAcceptState].acceptedRuleId;
            ACTIONS.get(ruleId).run();
        }
    }
}
```

#### 1.2.7 运行时支撑类（待实现）

**LexerInput** - 输入缓冲区管理:

```java
public class LexerInput {
    private String input;
    private int pos = 0;
    private int mark = 0;

    public char peek(int offset) { /* 预览字符 */ }
    public void consume(int count) { /* 消耗输入 */ }
    public String getText() { /* 获取匹配的文本 */ }
    public void mark() { /* 标记位置 */ }
    public void reset() { /* 重置到标记位置 */ }
}
```

**Token** - 词法单元表示:

```java
public class Token {
    public final int type;      // Token 类型
    public final String text;   // 原始文本
    public final int line;      // 行号
    public final int column;    // 列号

    public Token(int type, String text, int line, int column) {
        this.type = type;
        this.text = text;
        this.line = line;
        this.column = column;
    }
}
```

### 1.3 数据流设计

```
Lex 文件 → Parser → 规则列表 → NFA → DFA → CodeGenerator → Java 代码
   │          │         │         │      │        │           │
   │          │         │         │      │        │           ▼
   │          │         │         │      │        │      编译器编译
   │          │         │         │      │        │           │
   │          │         │         │      │        │           ▼
   │          │         │         │      │        └──▶ 可执行的词法分析器
   │          │         │         │      │
   │          │         │         │      └──▶ DFA 最小化（可选优化）
   │          │         │         │
   │          │         │         └──▶ Thompson 构造法
   │          │         │
   │          │         └──▶ 正则预处理（消除语法糖）
   │          │
   │          └──▶ 提取宏定义和规则
   │
   └──▶ C99 词法规则（c99.l）
```

### 1.4 模块接口设计

#### 1.4.1 Parser → Converter

```java
// 输入：LexRule.regex (原始正则字符串)
// 输出：String (后缀表达式)
String postfix = converter.convert(rule.regex);
```

#### 1.4.2 Converter → NFA Builder

```java
// 输入：String (后缀表达式)
// 输出：NfaFragment (NFA 片段)
NfaFragment fragment = builder.build(postfix);
```

#### 1.4.3 NFA → DFA Converter

```java
// 输入：NfaState (NFA 起点)
// 输出：List<DfaState> (DFA 状态列表)
List<DfaState> dfaStates = converter.convert(globalStart);
```

#### 1.4.4 DFA → Code Generator

```java
// 输入：List<DfaState>, List<LexRule>
// 输出：String (Java 源代码)
String lexerCode = generator.generate(dfaStates, rules);
```

---

## 二、核心算法详细设计

### 2.1 Lex 文件解析算法

#### 2.1.1 文件分割算法

**输入**: Lex 文件完整内容字符串  
**输出**: 定义段、规则段、用户代码段

```java
Algorithm splitLexFile(fullContent):
    sections ← fullContent.split("(?m)^%%\\s*")

    if sections.length >= 1 then
        definitionPart ← sections[0].trim()
    if sections.length >= 2 then
        rulePart ← sections[1].trim()
    if sections.length >= 3 then
        userSubroutinePart ← sections[2].trim()
```

#### 2.1.2 规则解析算法（核心难点）

**输入**: 规则段文本  
**输出**: LexRule 对象列表

```java
Algorithm parseRules():
    lines ← rulePart.split("\n")
    ruleId ← 0

    for each line in lines do
        line ← line.trim()

        if line is empty or starts with "%" then
            continue

        // 引号保护机制：寻找第一个引号外的空白字符
        splitIdx ← -1
        inQuotes ← false

        for i from 0 to line.length do
            c ← line.charAt(i)

            if c == '"' and (i == 0 or line.charAt(i-1) != '\\') then
                inQuotes ← !inQuotes  // 切换引号状态

            else if not inQuotes and (c == ' ' or c == '\t') then
                splitIdx ← i
                break

        if splitIdx != -1 then
            // 有动作的规则
            re ← line.substring(0, splitIdx).trim()
            action ← line.substring(splitIdx).trim()
            fullRegex ← expandMacros(re)
            rules.add(new LexRule(++ruleId, fullRegex, action))
        else
            // 无动作的规则
            fullRegex ← expandMacros(line)
            rules.add(new LexRule(++ruleId, fullRegex, ""))
```

**关键技术点**:

1. **引号保护**: 跳过引号内的空白字符，避免错误分割
2. **转义处理**: 识别 `\"` 不切换引号状态
3. **空动作兼容**: 支持只有模式串的规则

#### 2.1.3 宏展开算法

```java
Algorithm expandMacros(input):
    result ← input

    repeat
        changed ← false

        for each (name, value) in regularDefs do
            macro ← "{" + name + "}"

            if result contains macro then
                // 用括号包裹以维持优先级
                result ← result.replace(macro, "(" + value + ")")
                changed ← true

    while changed  // 递归替换直到无宏可换

    return result
```

### 2.2 正则表达式转换算法

#### 2.2.1 字符集展开算法

```java
Algorithm processCharSet(regex):
    result ← new StringBuilder()
    i ← 0

    while i < regex.length do
        c ← regex.charAt(i)

        if c == '[' and not isEscaped(regex, i) then
            end ← regex.indexOf(']', i + 1)

            if end == -1 then
                throw Exception("未闭合的字符集")

            content ← regex.substring(i + 1, end)
            expanded ← expandCharRange(content)

            result.append("(" + expanded + ")")
            i ← end + 1
        else
            result.append(c)
            i ← i + 1

    return result.toString()


Algorithm expandCharRange(content):
    sb ← new StringBuilder()

    for i from 0 to content.length do
        if i+2 < content.length and content.charAt(i+1) == '-' then
            start ← content.charAt(i)
            end ← content.charAt(i+2)

            for ch from start to end do
                if sb.length > 0 then
                    sb.append('|')
                sb.append(ch)

            i ← i + 2
        else
            if sb.length > 0 then
                sb.append('|')
            sb.append(content.charAt(i))

    return sb.toString()
```

#### 2.2.2 语法糖消除算法

```java
Algorithm processPlusAndQuestion(regex):
    result ← new StringBuilder()

    for i from 0 to regex.length do
        c ← regex.charAt(i)

        if (c == '+' or c == '?') and not isEscaped(regex, i) then
            lastIdx ← result.length - 1

            // 获取前一个操作数（可能是单字符或括号表达式）
            if result.charAt(lastIdx) == ')' then
                start ← findMatchingBracket(result, lastIdx)
                target ← result.substring(start, lastIdx+1)
                result.delete(start, result.length)
            else
                target ← String.valueOf(result.charAt(lastIdx))
                result.deleteCharAt(lastIdx)

            if c == '+' then
                // a+ → aa*
                result.append(target + target + "*")
            else  // c == '?'
                // a? → (a|ε)
                result.append("(" + target + "|ε)")
        else
            result.append(c)

    return result.toString()
```

#### 2.2.3 连接符显式化算法

```java
Algorithm insertConcatOperator(regex):
    result ← new StringBuilder()

    for i from 0 to regex.length do
        c1 ← regex.charAt(i)
        result.append(c1)

        if i + 1 < regex.length then
            c2 ← regex.charAt(i + 1)

            if shouldConcat(c1, c2) then
                result.append('·')

    return result.toString()


Algorithm shouldConcat(c1, c2):
    leftReady ← isOperand(c1) or c1 == ')' or c1 == '*'
    rightReady ← isOperand(c2) or c2 == '('
    return leftReady and rightReady
```

#### 2.2.4 中缀转后缀算法（调度场算法）

```java
Algorithm toPostfix(regex):
    output ← new StringBuilder()
    stack ← new Stack()

    for i from 0 to regex.length do
        c ← regex.charAt(i)

        if isOperand(c) then
            output.append(c)
        else if c == '(' then
            stack.push(c)
        else if c == ')' then
            while not stack.isEmpty() and stack.peek() != '(' do
                output.append(stack.pop())
            stack.pop()  // 弹出 '('
        else  // 运算符
            while not stack.isEmpty() and
                  precedence(stack.peek()) >= precedence(c) do
                output.append(stack.pop())
            stack.push(c)

    while not stack.isEmpty() do
        output.append(stack.pop())

    return output.toString()


Algorithm precedence(op):
    if op == '*' then return 3
    if op == '·' then return 2
    if op == '|' then return 1
    return 0
```

### 2.3 NFA 构建算法（Thompson 构造法）

#### 2.3.1 整体构建算法

```java
Algorithm buildNfa(postfix):
    stack ← new Stack()

    for each char c in postfix do
        switch c do
            case '*':
                fragment ← stack.pop()
                stack.push(doKleene(fragment))

            case '|':
                right ← stack.pop()
                left ← stack.pop()
                stack.push(doAlt(left, right))

            case '·':
                right ← stack.pop()
                left ← stack.pop()
                stack.push(doConcat(left, right))

            default:  // 操作数
                stack.push(doOperand(c))

    finalNfa ← stack.pop()
    finalNfa.accept.isAccept ← true

    return finalNfa
```

#### 2.3.2 基础操作数构造

```java
Algorithm doOperand(c):
    start ← new NfaState()
    accept ← new NfaState()

    start.transition ← c
    start.nextStates.add(accept)

    return NfaFragment(start, accept)
```

#### 2.3.3 连接运算构造

```java
Algorithm doConcat(f1, f2):
    f1.accept.nextStates.add(f2.start)
    return NfaFragment(f1.start, f2.accept)
```

#### 2.3.4 选择运算构造

```java
Algorithm doAlt(f1, f2):
    start ← new NfaState()
    accept ← new NfaState()

    start.nextStates.add(f1.start)
    start.nextStates.add(f2.start)
    f1.accept.nextStates.add(accept)
    f2.accept.nextStates.add(accept)

    return NfaFragment(start, accept)
```

#### 2.3.5 闭包运算构造

```java
Algorithm doKleene(f):
    start ← new NfaState()
    accept ← new NfaState()

    // 1. 匹配 0 次：start → accept
    start.nextStates.add(accept)

    // 2. 进入匹配：start → f.start
    start.nextStates.add(f.start)

    // 3. 匹配结束：f.accept → accept
    f.accept.nextStates.add(accept)

    // 4. 重复匹配：f.accept → f.start
    f.accept.nextStates.add(f.start)

    return NfaFragment(start, accept)
```

### 2.4 DFA 确定化算法（子集构造法）

#### 2.4.1 ε-闭包计算算法

```java
Algorithm epsilonClosure(states):
    closure ← new Set(states)
    stack ← new Stack()
    stack.addAll(states)

    while not stack.isEmpty() do
        current ← stack.pop()

        if current.transition == 'ε' then
            for each next in current.nextStates do
                if next not in closure then
                    closure.add(next)
                    stack.push(next)

    return closure
```

#### 2.4.2 移动算法

```java
Algorithm move(states, c):
    result ← new Set()

    for each s in states do
        if s.transition == c then
            result.addAll(s.nextStates)

    return result
```

#### 2.4.3 子集构造主算法

```java
Algorithm convert(nfaStart):
    dfaStates ← new List()
    unprocessed ← new Queue()

    // 1. 收集字母表
    alphabet ← collectAlphabet(nfaStart)

    // 2. 创建初始 DFA 状态
    startSet ← {nfaStart}
    startDfa ← new DfaState(epsilonClosure(startSet))

    dfaStates.add(startDfa)
    unprocessed.add(startDfa)

    // 3. 迭代处理
    while not unprocessed.isEmpty() do
        currentDfa ← unprocessed.poll()

        for each c in alphabet do
            if c == 'ε' then
                continue

            movedSet ← move(currentDfa.nfaStates, c)

            if movedSet is empty then
                continue

            targetClosure ← epsilonClosure(movedSet)
            existing ← findState(dfaStates, targetClosure)

            if existing == null then
                newState ← new DfaState(targetClosure)
                dfaStates.add(newState)
                unprocessed.add(newState)
                currentDfa.transitions.put(c, newState)
            else
                currentDfa.transitions.put(c, existing)

    return dfaStates
```

#### 2.4.4 优先级处理算法

```java
Algorithm determineAcceptedRuleId(nfaStates):
    acceptedRuleId ← -1

    for each s in nfaStates do
        if s.isAccept then
            if acceptedRuleId == -1 or s.ruleId < acceptedRuleId then
                acceptedRuleId ← s.ruleId

    return acceptedRuleId
```

**说明**: Lex 规则优先级原则 - 当多个规则都能匹配时，选择文件中定义顺序靠前的（ruleId 最小的）

---

## 三、关键技术难点与解决方案

### 3.1 规则解析的歧义处理

**问题**: Lex 规则中，正则表达式和动作代码的分隔可能因为引号内的空白字符而产生歧义。

**示例**:

```
"hello world"    { printf("greeting\n"); }
```

**解决方案**: 引号保护机制

```java
boolean inQuotes = false;
for (int i = 0; i < line.length(); i++) {
    char c = line.charAt(i);

    if (c == '"' && (i == 0 || line.charAt(i - 1) != '\\')) {
        inQuotes = !inQuotes;  // 切换引号状态
    } else if (!inQuotes && (c == ' ' || c == '\t')) {
        splitIdx = i;  // 只在引号外记录分割点
        break;
    }
}
```

### 3.2 宏的递归展开

**问题**: 宏定义可能相互引用，如 `{ID} = {L}{L,D}*`。

**解决方案**: 不动点迭代算法

```java
do {
    changed = false;
    for (Map.Entry<String, String> entry : regularDefs.entrySet()) {
        String macro = "{" + entry.getKey() + "}";
        if (result.contains(macro)) {
            result = result.replace(macro, "(" + entry.getValue() + ")");
            changed = true;
        }
    }
} while (changed);  // 直到没有宏可替换
```

### 3.3 Lex 优先级规则的实现

**问题**: 当多个规则都能匹配同一输入时，需要遵循最长匹配优先、书写顺序优先。

**解决方案**: DFA 状态构造时处理

```java
public DfaState(int id, Set<NfaState> nfaStates) {
    this.nfaStates = nfaStates;

    for (NfaState s : nfaStates) {
        if (s.isAccept) {
            this.isAccept = true;
            // 选择 ruleId 最小的（文件中最靠前的规则）
            if (this.acceptedRuleId == -1 || s.ruleId < this.acceptedRuleId) {
                this.acceptedRuleId = s.ruleId;
            }
        }
    }
}
```

---

## 四、语义规则实现方案

### 4.1 语义动作的存储结构

```java
public static class LexRule {
    public int id;
    public String regex;      // 正则表达式
    public String action;     // 语义动作代码（C/Java 代码）
}
```

### 4.2 处理策略

**解析阶段**:

- 保留动作代码原样（包括注释）
- 延迟处理，仅提取不解析

**代码生成阶段**:
生成的词法分析器结构：

```java
public class GeneratedLexer {
    private static final Map<Integer, Runnable> ACTIONS = new HashMap<>();

    static {
        ACTIONS.put(1, () -> {
            count(); return(AUTO);
        });
        // ... 更多规则
    }

    public Token nextToken() {
        DfaState currentState = matchLongestPrefix(input);

        if (currentState.isAccept) {
            Runnable action = ACTIONS.get(currentState.acceptedRuleId);
            if (action != null) {
                action.run();
            }
        }

        return token;
    }
}
```

### 4.3 推荐方案：静态代码生成 + 动态执行

**理由**:

1. 性能优 - 编译期生成 Java 代码
2. 调试易 - 生成的代码可读性好
3. 实现简 - 不需要复杂的运行时解释器

---

## 五、实现进度

### 5.1 已完成工作

✅ Lex 文件解析框架搭建
✅ 正则表达式转换算法实现
✅ NFA 构建算法（Thompson 构造法）
✅ DFA 确定化算法（子集构造法）
✅ 核心数据结构设计
✅ 联调测试程序完成

### 5.2 待完成任务

- [ ] Lex 输入文件解析完善（边界情况处理）
- [ ] 单元测试编写
- [ ] 性能优化
- [ ] 与 Yacc 部分集成
- [ ] 代码生成器完善

---

## 六、核心代码文件清单

```
lex/
├── LexCompilerMain.java      # 主控制类（134 行）
├── SeuLexParser.java         # Lex 文件解析器（243 行）
├── RegexConverter.java       # 正则转换器（193 行）
├── NfaBuilder.java           # NFA 构建器（171 行）
├── NfaManager.java           # NFA 管理器（114 行）
├── NfaToDfaConverter.java    # DFA 确定化转换器（133 行）
├── DfaState.java             # DFA 状态类（44 行）
├── ParserRegexTest.java      # 联调测试程序
├── c99.l                     # C99 词法规则示例（187 行）
└── doc/                      # 文档目录
```

**总代码量**: ~1000 行（不含测试文件和注释）

---

**报告完成日期**: 2026-04-02
