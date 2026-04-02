import java.util.*;

/**
 * NfaManager: 负责管理多个 NFA 的生成与合并
 */
public class NfaManager {
    private int globalStateCounter = 0;
    private RegexConverter converter = new RegexConverter();

    // 内部使用的 NfaBuilder，共用一个状态计数器，保证全局 ID 唯一
    private class InternalNfaBuilder {
        public NfaFragment build(String postfix) {
            Stack<NfaFragment> stack = new Stack<>();
            for (char c : postfix.toCharArray()) {
                switch (c) {
                    case '*':
                        stack.push(doKleene(stack.pop()));
                        break;
                    case '|':
                        NfaFragment rA = stack.pop();
                        NfaFragment lA = stack.pop();
                        stack.push(doAlt(lA, rA));
                        break;
                    case '·':
                        NfaFragment rC = stack.pop();
                        NfaFragment lC = stack.pop();
                        stack.push(doConcat(lC, rC));
                        break;
                    default:
                        stack.push(doOperand(c));
                        break;
                }
            }
            return stack.pop();
        }

        private NfaState createNode() {
            return new NfaState(globalStateCounter++);
        }

        private NfaFragment doOperand(char c) {
            NfaState s1 = createNode();
            NfaState s2 = createNode();
            s1.transition = c;
            s1.nextStates.add(s2);
            return new NfaFragment(s1, s2);
        }

        private NfaFragment doConcat(NfaFragment f1, NfaFragment f2) {
            f1.accept.nextStates.add(f2.start);
            return new NfaFragment(f1.start, f2.accept);
        }

        private NfaFragment doAlt(NfaFragment f1, NfaFragment f2) {
            NfaState s = createNode();
            NfaState e = createNode();
            s.nextStates.add(f1.start);
            s.nextStates.add(f2.start);
            f1.accept.nextStates.add(e);
            f2.accept.nextStates.add(e);
            return new NfaFragment(s, e);
        }

        private NfaFragment doKleene(NfaFragment f) {
            NfaState s = createNode();
            NfaState e = createNode();
            s.nextStates.add(f.start);
            s.nextStates.add(e);
            f.accept.nextStates.add(f.start);
            f.accept.nextStates.add(e);
            return new NfaFragment(s, e);
        }
    }

    /**
     * 核心方法：合并所有词法规则
     * 
     * @param rules 从 SeuLexParser 拿到的规则列表
     * @return 整个巨大的 NFA 的起始节点
     */
    public NfaState buildCombinedNfa(List<SeuLexParser.LexRule> rules) {
        InternalNfaBuilder builder = new InternalNfaBuilder();

        // 1. 创建总起点
        NfaState globalStart = new NfaState(globalStateCounter++);

        System.out.println("--- 正在构建合并 NFA ---");

        for (SeuLexParser.LexRule rule : rules) {
            try {
                // 2. 正则 -> 后缀
                String postfix = converter.convert(rule.regex);

                // 3. 构建单个规则的 NFA
                NfaFragment fragment = builder.build(postfix);

                // 4. 标记接受态及其对应的规则 ID (Lex 优先级原则)
                fragment.accept.isAccept = true;
                fragment.accept.ruleId = rule.id; // rule.id 通常是它在 .l 文件中的行号

                // 5. 用 ε 边将总起点连向该规则的起点
                globalStart.nextStates.add(fragment.start);

                System.out.println("已合并规则: " + rule.regex);
            } catch (Exception e) {
                System.err.println("跳过无效规则 [" + rule.regex + "]: " + e.getMessage());
            }
        }

        System.out.println("合并完成。总状态数: " + globalStateCounter);
        return globalStart;
    }
}
