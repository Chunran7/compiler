import java.util.*;

public class TestNfaMain {
    public static void main(String[] args) {
        List<SeuLexParser.LexRule> rules = new ArrayList<>();
        rules.add(new SeuLexParser.LexRule(1, "if", "RETURN IF"));
        rules.add(new SeuLexParser.LexRule(2, "[a-z]+", "RETURN ID"));
        rules.add(new SeuLexParser.LexRule(3, "[0-9]+", "RETURN NUM"));

        NfaManager manager = new NfaManager();
        NfaState globalStart = manager.buildCombinedNfa(rules);

        // 使用现有的 NfaBuilder 的打印方法查看合并后的 NFA
        NfaBuilder printer = new NfaBuilder();
        System.out.println("===== 合并后的 NFA =====");
        printer.printNfa(globalStart);

        // 启动 NFA -> DFA 转换
        System.out.println("\n>>> 开始 NFA 到 DFA 的确定化...\n");
        NfaToDfaConverter dfaConverter = new NfaToDfaConverter();
        List<DfaState> dfaStates = dfaConverter.convert(globalStart);

        System.out.println("\n>>> 开始最小化 DFA...\n");
        dfaStates = dfaConverter.minimize(dfaStates);

        // 打印 DFA 状态转移表进行验证
        dfaConverter.printDfaTable(dfaStates);
        
        // 输出统计信息
        System.out.println("\n===== 统计信息 =====");
        System.out.println("最小化后 DFA 状态总数：" + dfaStates.size());
        long acceptCount = dfaStates.stream().filter(s -> s.isAccept).count();
        System.out.println("接受态数量：" + acceptCount);
        System.out.println("中间态数量：" + (dfaStates.size() - acceptCount));
    }
}
