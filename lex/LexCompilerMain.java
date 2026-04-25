import java.util.*;
import java.nio.file.*;

public class LexCompilerMain {
    public static void main(String[] args) throws Exception {
        String lexFile = "c99.l";
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
        String cCode = generator.generateC(dfaStates, rules, parser.getDefinitionPart(), parser.getUserSubroutinePart());
        
        Files.write(Paths.get("lex.yy.c"), cCode.getBytes());
        System.out.println("   完成！已生成 lex.yy.c");
    }
}
