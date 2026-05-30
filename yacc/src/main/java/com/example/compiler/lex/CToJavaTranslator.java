package com.example.compiler.lex;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 将 .l 文件中用户编写的 C 代码翻译为可嵌入 Java 类的 Java 代码。
 *
 * 采用结构化翻译策略：先将 C 代码拆分为顶层构造（函数定义、声明），
 * 再对每个构造分别翻译签名和函数体，避免正则匹配的格式依赖。
 */
public class CToJavaTranslator {

    // ── 数据驱动的类型系统 ──

    /** C 类型关键字（用于识别前向声明和变量声明） */
    private static final Set<String> C_TYPE_KEYWORDS = Set.of(
            "void", "int", "char", "short", "long", "float", "double",
            "unsigned", "signed", "static", "extern", "const",
            "struct", "enum", "union");

    /** C 限定符 / 存储类（不单独构成类型，只修饰后面的类型） */
    private static final Set<String> C_QUALIFIERS = Set.of(
            "unsigned", "signed", "static", "extern", "const");

    /** C 类型 → Java 类型映射 */
    private static final Map<String, String> C_TO_JAVA_TYPE = Map.ofEntries(
            Map.entry("void", "void"),
            Map.entry("int", "int"),
            Map.entry("char", "int"), // Reader.read() returns int
            Map.entry("short", "int"),
            Map.entry("long", "long"),
            Map.entry("float", "float"),
            Map.entry("double", "double"),
            Map.entry("const char*", "String"),
            Map.entry("char*", "String"),
            Map.entry("unsigned", "int"),
            Map.entry("unsigned int", "int"),
            Map.entry("unsigned short", "int"),
            Map.entry("unsigned long", "long"),
            Map.entry("signed", "int"),
            Map.entry("signed int", "int"),
            Map.entry("signed char", "int"),
            Map.entry("signed short", "int"),
            Map.entry("signed long", "long"),
            Map.entry("long long", "long"),
            Map.entry("unsigned long long", "long"));

    // ── 公共入口 ──

    /**
     * 翻译 %{...%} 定义块。
     * 
     * @return [0] = 类体前（import/package），[1] = 类体内
     */
    public String[] translateDefinitionBlock(String cCode) {
        if (cCode == null || cCode.isBlank())
            return new String[] { "", "" };

        StringBuilder beforeClass = new StringBuilder();
        StringBuilder insideClass = new StringBuilder();

        for (String rawLine : cCode.split("\n")) {
            String line = rawLine.trim();
            if (line.isEmpty())
                continue;

            if (line.startsWith("import ") || line.startsWith("package ")) {
                beforeClass.append(line).append("\n");
            } else if (line.startsWith("#")) {
                insideClass.append("    // ").append(line).append("\n");
            } else if (isForwardDeclaration(line)) {
                insideClass.append("    // ").append(line).append(" (forward decl)\n");
            } else {
                // 变量声明、宏、注释等：保留在类体内
                ParsedDecl decl = parseDeclaration(line);
                if (decl != null) {
                    insideClass.append("    public ").append(decl.type).append(" ").append(decl.name);
                    if (decl.init != null)
                        insideClass.append(" = ").append(decl.init);
                    insideClass.append(";\n");
                } else {
                    insideClass.append("    ").append(line).append("\n");
                }
            }
        }

        return new String[] { beforeClass.toString(), insideClass.toString() };
    }

    /**
     * 翻译用户子程序段（第二个 %% 之后）。
     * 分为三步：拆分顶层构造 → 分类 → 翻译。
     */
    public String translateUserSubroutines(String cCode) {
        if (cCode == null || cCode.isBlank())
            return "";

        String code = removeCComments(cCode);
        List<TopLevelChunk> chunks = splitTopLevel(code);

        StringBuilder result = new StringBuilder();
        for (TopLevelChunk chunk : chunks) {
            String translated = translateChunk(chunk);
            if (!translated.isEmpty()) {
                result.append(translated).append("\n");
            }
        }
        return result.toString();
    }

    // ── 顶层拆分 ──

    private static class TopLevelChunk {
        enum Kind {
            FUNCTION, DECLARATION
        }

        Kind kind;
        String text;
    }

    /**
     * 将去除注释后的 C 代码拆分为顶层构造。
     * 追踪括号深度，在深度为 0 时识别函数边界（}）和声明边界（;）。
     */
    private List<TopLevelChunk> splitTopLevel(String code) {
        List<TopLevelChunk> chunks = new ArrayList<>();
        int chunkStart = 0;
        int braceDepth = 0;
        int parenDepth = 0;

        for (int i = 0; i < code.length(); i++) {
            char c = code.charAt(i);

            if (c == '(' && braceDepth == 0)
                parenDepth++;
            else if (c == ')' && braceDepth == 0)
                parenDepth--;
            else if (c == '{')
                braceDepth++;
            else if (c == '}') {
                braceDepth--;
                if (braceDepth == 0) {
                    // 函数定义结束
                    TopLevelChunk chunk = new TopLevelChunk();
                    chunk.text = code.substring(chunkStart, i + 1);
                    chunk.kind = TopLevelChunk.Kind.FUNCTION;
                    chunks.add(chunk);
                    chunkStart = i + 1;
                }
            } else if (c == ';' && braceDepth == 0 && parenDepth == 0) {
                // 顶层声明结束
                String text = code.substring(chunkStart, i + 1).trim();
                if (!text.isEmpty()) {
                    TopLevelChunk chunk = new TopLevelChunk();
                    chunk.text = text;
                    chunk.kind = TopLevelChunk.Kind.DECLARATION;
                    chunks.add(chunk);
                }
                chunkStart = i + 1;
            }
        }

        // 尾部剩余（通常为空或空白）
        String remaining = code.substring(chunkStart).trim();
        if (!remaining.isEmpty()) {
            TopLevelChunk chunk = new TopLevelChunk();
            chunk.text = remaining;
            chunk.kind = TopLevelChunk.Kind.DECLARATION;
            chunks.add(chunk);
        }

        return chunks;
    }

    // ── Chunk 翻译调度 ──

    private String translateChunk(TopLevelChunk chunk) {
        if (chunk.text.isBlank())
            return "";

        return switch (chunk.kind) {
            case FUNCTION -> translateFunction(chunk.text);
            case DECLARATION -> translateTopLevelDeclaration(chunk.text);
        };
    }

    // ── 结构化函数表示 ──

    private static class ParsedFunction {
        String returnType; // C 返回类型原文，如 "int", "const char*", "void"
        String name; // 函数名
        List<Param> params; // 参数列表
        String body; // 函数体（含最外层 { }）

        static class Param {
            String type; // C 类型原文
            String name; // 参数名
        }
    }

    /** 解析 C 函数定义：签名 + 函数体 */
    private ParsedFunction parseFunction(String funcText) {
        int braceIdx = funcText.indexOf('{');
        if (braceIdx < 0)
            return null;

        String sigPart = funcText.substring(0, braceIdx).trim();
        String body = funcText.substring(braceIdx);

        ParsedFunction func = new ParsedFunction();
        func.body = body;

        // 解析签名：返回类型 函数名 ( 参数列表 )
        parseCSignature(sigPart, func);
        return func;
    }

    /**
     * 解析 C 函数签名。
     * 从右向左：先找参数列表的 )，匹配到 (，前面是函数名，再前面是返回类型。
     */
    private void parseCSignature(String sig, ParsedFunction func) {
        sig = sig.trim();

        // 查找最右边的 )
        int rparen = sig.lastIndexOf(')');
        if (rparen < 0) {
            // 无参数：func_name
            func.name = sig;
            func.returnType = "int"; // C 默认返回 int
            func.params = List.of();
            return;
        }

        // 匹配对应的 (
        int lparen = findMatchingParen(sig, rparen);
        if (lparen < 0) {
            func.name = sig;
            func.returnType = "int";
            func.params = List.of();
            return;
        }

        // 解析参数列表
        String paramsStr = sig.substring(lparen + 1, rparen).trim();
        func.params = parseParamList(paramsStr);

        // 函数名在 ( 之前
        String beforeParen = sig.substring(0, lparen).trim();
        // 从末尾提取函数名（标识符）
        int nameEnd = beforeParen.length();
        while (nameEnd > 0 && Character.isWhitespace(beforeParen.charAt(nameEnd - 1)))
            nameEnd--;
        int nameStart = nameEnd;
        while (nameStart > 0 && (Character.isLetterOrDigit(beforeParen.charAt(nameStart - 1))
                || beforeParen.charAt(nameStart - 1) == '_'))
            nameStart--;

        func.name = beforeParen.substring(nameStart, nameEnd);

        // 返回类型在函数名之前
        func.returnType = beforeParen.substring(0, nameStart).trim();
        if (func.returnType.isEmpty())
            func.returnType = "int"; // C 默认
    }

    /** 找到与 pos 处的 ) 匹配的 ( */
    private int findMatchingParen(String s, int rparen) {
        int depth = 1;
        for (int i = rparen - 1; i >= 0; i--) {
            char c = s.charAt(i);
            if (c == ')')
                depth++;
            else if (c == '(')
                depth--;
            if (depth == 0)
                return i;
        }
        return -1;
    }

    /** 解析参数列表 "int x, const char *msg" → List<Param> */
    private List<ParsedFunction.Param> parseParamList(String paramsStr) {
        List<ParsedFunction.Param> params = new ArrayList<>();
        if (paramsStr.isEmpty() || paramsStr.equals("void"))
            return params;

        // 按逗号分割（尊重嵌套括号）
        List<String> parts = splitRespectingParens(paramsStr, ',');
        for (String part : parts) {
            part = part.trim();
            if (part.isEmpty())
                continue;

            ParsedFunction.Param p = new ParsedFunction.Param();
            // 从末尾提取参数名
            int nameEnd = part.length();
            while (nameEnd > 0 && Character.isWhitespace(part.charAt(nameEnd - 1)))
                nameEnd--;
            int nameStart = nameEnd;
            while (nameStart > 0
                    && (Character.isLetterOrDigit(part.charAt(nameStart - 1)) || part.charAt(nameStart - 1) == '_'))
                nameStart--;

            if (nameStart < nameEnd) {
                p.name = part.substring(nameStart, nameEnd);
                p.type = part.substring(0, nameStart).trim();
            } else {
                // 无参数名（如 "int"），只有类型
                p.name = "";
                p.type = part.trim();
            }
            params.add(p);
        }
        return params;
    }

    /** 按分隔符分割，但尊重括号内的内容不分 */
    private List<String> splitRespectingParens(String s, char delim) {
        List<String> result = new ArrayList<>();
        int depth = 0;
        int start = 0;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '(')
                depth++;
            else if (c == ')')
                depth--;
            else if (c == delim && depth == 0) {
                result.add(s.substring(start, i));
                start = i + 1;
            }
        }
        result.add(s.substring(start));
        return result;
    }

    // ── 类型翻译 ──

    /** 将 C 类型字符串翻译为 Java 类型 */
    private String translateType(String cType) {
        // 规范化：去掉多余的空白
        String normalized = cType.trim().replaceAll("\\s+", " ");

        // 特殊处理：const char * → String
        if (normalized.equals("const char *") || normalized.equals("const char*")) {
            return "String";
        }
        // char * → String（但 "char * const" 等暂不处理）
        if (normalized.equals("char *") || normalized.equals("char*")) {
            return "String";
        }

        // 查表
        String javaType = C_TO_JAVA_TYPE.get(normalized);
        if (javaType != null)
            return javaType;

        // 去掉 const / static / extern 修饰后再查
        String stripped = normalized;
        for (String qual : C_QUALIFIERS) {
            stripped = stripped.replaceFirst("^" + qual + "\\s+", "");
        }
        javaType = C_TO_JAVA_TYPE.get(stripped);
        if (javaType != null)
            return javaType;

        // 未知类型：保留原样（可能是用户自定义的 typedef 名如 TokenType）
        return normalized;
    }

    // ── 函数翻译 ──

    private String translateFunction(String funcText) {
        ParsedFunction func = parseFunction(funcText);
        if (func == null)
            return funcText; // 解析失败，原样返回

        // 跳过 yywrap
        if ("yywrap".equals(func.name))
            return "";

        // 翻译签名
        String javaReturnType = translateType(func.returnType);
        StringBuilder sig = new StringBuilder();
        sig.append("private ").append(javaReturnType).append(" ").append(func.name).append("(");
        for (int i = 0; i < func.params.size(); i++) {
            if (i > 0)
                sig.append(", ");
            ParsedFunction.Param p = func.params.get(i);
            sig.append(translateType(p.type));
            if (!p.name.isEmpty())
                sig.append(" ").append(p.name);
        }
        sig.append(")");

        // 翻译函数体
        String javaBody = translateFunctionBody(func);

        // 函数体翻译后可能改变返回类型（如 return TOKEN_NAME → return new Token(...)）
        if (javaBody.contains("return new Token(") && "int".equals(javaReturnType)) {
            javaReturnType = "Token";
            // 重新生成签名
            sig = new StringBuilder();
            sig.append("private ").append(javaReturnType).append(" ").append(func.name).append("(");
            for (int i = 0; i < func.params.size(); i++) {
                if (i > 0)
                    sig.append(", ");
                ParsedFunction.Param p = func.params.get(i);
                sig.append(translateType(p.type));
                if (!p.name.isEmpty())
                    sig.append(" ").append(p.name);
            }
            sig.append(")");
        }

        return "    " + sig + " " + javaBody;
    }

    /** 翻译函数体：应用 Lex C→Java 转换规则 */
    private String translateFunctionBody(ParsedFunction func) {
        String body = func.body;

        // 1. 翻译函数体内的局部变量声明类型
        body = translateLocalDeclarations(body, func);

        // 2. 翻译 while ((c = input()) != 0) → while ((c = input()) != -1 && c != 0)
        body = translateInputEofPattern(body);

        // 3. 翻译 for (i=0; yytext[i]!='\0'; i++) → for (int i=0; i<yyleng; i++)
        body = translateYytextLoop(body);

        // 4. 翻译 fprintf(stderr, ...); exit(1); → throw new RuntimeException(...);
        body = translateFprintfExit(body);

        // 5. 翻译 ECHO;
        body = body.replace("ECHO;", "System.out.print(new String(yytext, 0, yyleng));");

        // 6. 翻译 return TOKEN_NAME; / return(TOKEN_NAME);
        body = translateReturnToken(body);

        // 7. 最终清理：移除因 for 循环内联声明而变得多余的独立变量声明
        body = removeRedundantDeclarations(body);

        return body;
    }

    /**
     * 移除多余的局部变量声明。
     * 例如：int i; 后面紧跟 for (int i = ...) 时，删除 int i; 行。
     */
    private String removeRedundantDeclarations(String body) {
        // 匹配: type var; [可选空行] for (type var =
        // 适用于 int, char 等常见类型
        return body.replaceAll(
                "(?m)^\\s*(?:int|char|short|long|float|double)\\s+(\\w+)\\s*;\\s*\\n(?:\\s*\\n)?(\\s*for\\s*\\(\\s*(?:int|char|short|long|float|double)\\s+\\1\\s*=)",
                "$2");
    }

    /**
     * 翻译函数体内的局部变量声明。
     * 读取函数体，在遇到 ; 且括号深度为0时检查是否为声明。
     */
    private String translateLocalDeclarations(String body, ParsedFunction func) {
        // 策略：匹配 C 类型关键字开头的语句（简化但涵盖绝大多数情况）
        // 形如： type name [= init];
        StringBuilder result = new StringBuilder();
        String[] lines = body.split("\n");
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) {
                result.append(line).append("\n");
                continue;
            }

            // 尝试解析为声明
            ParsedDecl decl = parseDeclaration(trimmed);
            if (decl != null) {
                String javaType = translateType(decl.type);
                StringBuilder declLine = new StringBuilder();
                // 保留原始缩进
                String indent = line.substring(0, line.indexOf(trimmed.charAt(0)));
                declLine.append(indent).append(javaType).append(" ").append(decl.name);
                if (decl.init != null)
                    declLine.append(" = ").append(decl.init);
                declLine.append(";");
                // 检查下一行是否是 for 循环声明同名变量（需要删除多余声明）
                result.append(declLine);
            } else {
                result.append(line);
            }
            result.append("\n");
        }

        return result.toString();
    }

    /** while ((c = input()) != 0) → while ((c = input()) != -1 && c != 0) */
    private String translateInputEofPattern(String body) {
        // 匹配更灵活的模式：while ( (var = input()) != 0 )
        return body.replaceAll(
                "while\\s*\\(\\s*\\(\\s*(\\w+)\\s*=\\s*input\\s*\\(\\s*\\)\\s*\\)\\s*!=\\s*0\\s*\\)",
                "while (($1 = input()) != -1 && $1 != 0)");
    }

    /** for (i = 0; yytext[i] != '\0'; i++) → for (int i = 0; i < yyleng; i++) */
    private String translateYytextLoop(String body) {
        return body.replaceAll(
                "for\\s*\\(\\s*(\\w+)\\s*=\\s*0\\s*;\\s*yytext\\[\\1\\]\\s*!=\\s*'\\\\0'\\s*;\\s*\\1\\+\\+\\s*\\)",
                "for (int $1 = 0; $1 < yyleng; $1++)");
    }

    /** fprintf(stderr, ...); exit(1); → throw new RuntimeException(...); */
    private String translateFprintfExit(String body) {
        Pattern p = Pattern.compile(
                "fprintf\\s*\\(\\s*stderr\\s*,\\s*\"([^\"]*)\"((?:\\s*,\\s*[^;]+?)*)\\s*\\)\\s*;\\s*exit\\s*\\(\\s*1\\s*\\)\\s*;");
        Matcher m = p.matcher(body);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            String format = m.group(1);
            String argsStr = m.group(2) != null ? m.group(2).trim() : "";

            String msg = "\"" + format + "\"";
            if (!argsStr.isBlank()) {
                msg += " + " + argsStr.replaceFirst("^\\s*,\\s*", "");
            }

            m.appendReplacement(sb, Matcher.quoteReplacement(
                    "throw new RuntimeException(" + msg + ");"));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    /** return TOKEN_NAME; / return(TOKEN_NAME); → new Token(...) */
    private String translateReturnToken(String body) {
        // return TOKEN_NAME;
        body = body.replaceAll(
                "return\\s+([A-Z_][A-Z_0-9]*)\\s*;",
                "return new Token(TokenType.$1, new String(yytext, 0, yyleng));");
        // return(TOKEN_NAME);
        body = body.replaceAll(
                "return\\s*\\(\\s*([A-Z_][A-Z_0-9]*)\\s*\\)\\s*;",
                "return new Token(TokenType.$1, new String(yytext, 0, yyleng));");
        return body;
    }

    // ── 声明解析 ──

    private static class ParsedDecl {
        String type; // C 类型如 "int", "const char*", "unsigned long"
        String name; // 变量名
        String init; // 初始化表达式（可为 null）
    }

    /** 尝试将一行文本解析为 C 变量声明，失败返回 null */
    private ParsedDecl parseDeclaration(String line) {
        String cleaned = line.trim();
        // 去掉尾部分号
        if (!cleaned.endsWith(";"))
            return null;
        cleaned = cleaned.substring(0, cleaned.length() - 1).trim();
        if (cleaned.isEmpty())
            return null;

        // 不能是函数调用或控制流语句
        if (cleaned.contains("("))
            return null;

        // 必须以 C 类型关键字开头
        String firstWord = cleaned.split("\\s+")[0];
        if (!C_TYPE_KEYWORDS.contains(firstWord))
            return null;

        // 解析：类型 名称 [= 初始值]
        // 找出 = 的位置（如果有），以此为界
        int eqIdx = cleaned.indexOf('=');
        String beforeEq, init;
        if (eqIdx >= 0) {
            beforeEq = cleaned.substring(0, eqIdx).trim();
            init = cleaned.substring(eqIdx + 1).trim();
            if (init.isEmpty()) init = null;
        } else {
            beforeEq = cleaned;
            init = null;
        }

        // beforeEq 的构成：类型词 + 变量名
        // 变量名是最后一个合法标识符 token
        String[] words = beforeEq.split("\\s+");
        if (words.length < 2) return null;

        // 从末尾找第一个合法的 C 标识符作为变量名
        int nameIdx = words.length - 1;
        while (nameIdx >= 0 && !words[nameIdx].matches("[a-zA-Z_][a-zA-Z0-9_]*"))
            nameIdx--;
        if (nameIdx <= 0) return null;  // 没有变量名，或变量名就是第一个词（类型）

        String name = words[nameIdx];

        // 类型是 name 之前的所有词
        StringBuilder typeBuilder = new StringBuilder();
        for (int i = 0; i < nameIdx; i++) {
            if (i > 0) typeBuilder.append(" ");
            typeBuilder.append(words[i]);
        }
        String type = typeBuilder.toString();

        // 验证类型以 C 类型关键字开头
        if (!C_TYPE_KEYWORDS.contains(type.split("\\s+")[0]))
            return null;

        ParsedDecl decl = new ParsedDecl();
        decl.type = type;
        decl.name = name;
        decl.init = init;
        return decl;
    }

    // ── 顶层声明翻译 ──

    private String translateTopLevelDeclaration(String text) {
        String trimmed = text.trim();
        if (trimmed.isEmpty())
            return "";

        // 前向声明：跳过
        if (isForwardDeclaration(trimmed))
            return "";

        // # 开头的指令：注释掉
        if (trimmed.startsWith("#")) {
            return "    // " + trimmed;
        }

        // 变量声明
        ParsedDecl decl = parseDeclaration(trimmed);
        if (decl != null) {
            String javaType = translateType(decl.type);
            StringBuilder sb = new StringBuilder();
            sb.append("    public ").append(javaType).append(" ").append(decl.name);
            if (decl.init != null)
                sb.append(" = ").append(decl.init);
            sb.append(";");
            return sb.toString();
        }

        // 无法识别：原样保留
        return "    " + trimmed;
    }

    // ── 前向声明判断 ──

    private boolean isForwardDeclaration(String line) {
        String cleaned = removeCComments(line).trim();
        if (!cleaned.endsWith(";"))
            return false;
        if (cleaned.contains("{"))
            return false;

        // 去掉分号后检查：是否以类型关键字开头 + 包含参数列表括号
        String withoutSemi = cleaned.substring(0, cleaned.length() - 1).trim();
        if (!withoutSemi.contains("(") || !withoutSemi.contains(")"))
            return false;

        String firstWord = withoutSemi.split("\\s+")[0];
        return C_TYPE_KEYWORDS.contains(firstWord);
    }

    // ── 注释处理 ──

    private String removeCComments(String code) {
        code = code.replaceAll("(?s)/\\*.*?\\*/", "");
        code = code.replaceAll("//[^\n]*", "");
        return code;
    }
}
