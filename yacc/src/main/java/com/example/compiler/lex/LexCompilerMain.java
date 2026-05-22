package com.example.compiler.lex;

import java.util.*;
import java.nio.file.*;

public class LexCompilerMain {
    // C99 词法文件路径配置
    private static final String LEX_FILE_PATH = "resources/c99.l";
    // 生成的词法分析器输出路径
    private static final String OUTPUT_LEXER_PATH = "src/main/java/com/example/compiler/lex/GeneratedLexer.java";
    
    public static void main(String[] args) throws Exception {
        String lexFile = LEX_FILE_PATH;
        if (args.length > 0) {
            lexFile = args[0];
        }
        
        System.out.println(">>> 1. 解析 Lex 文件: " + lexFile + " ...");
        SeuLexParser parser = new SeuLexParser();
        String content = new String(Files.readAllBytes(Paths.get(lexFile)));
        parser.splitLexFile(content);
        parser.parseDefinitions();
        parser.parseRules();
        
        List<SeuLexParser.LexRule> rules = parser.getRules();
        System.out.println("   获取到 " + rules.size() + " 条规则");
        
        System.out.println(">>> 2. 转换规则为 NFA...");
        NfaManager manager = new NfaManager();
        NfaState globalStart = manager.buildCombinedNfa(rules);
        
        System.out.println(">>> 3. 将 NFA 确定化为 DFA...");
        NfaToDfaConverter dfaConverter = new NfaToDfaConverter();
        List<DfaState> dfaStates = dfaConverter.convert(globalStart);
        
        System.out.println("   子集构造法得到初始 DFA 状态总数: " + dfaStates.size());
        
        System.out.println(">>> 3.5. 最小化 DFA...");
        dfaStates = dfaConverter.minimize(dfaStates);
        System.out.println("   最小化后 DFA 状态总数: " + dfaStates.size());
        
        System.out.println(">>> 4. 生成目标代码...");
        CodeGenerator generator = new CodeGenerator();
        String javaCode = generator.generateJava(dfaStates, rules, parser.getDefinitionPart(), parser.getUserSubroutinePart());
        
        Files.write(Paths.get(OUTPUT_LEXER_PATH), javaCode.getBytes());
        System.out.println("   完成！已生成 GeneratedLexer.java");
    }
}