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
        printer.printNfa(globalStart);
    }
}
