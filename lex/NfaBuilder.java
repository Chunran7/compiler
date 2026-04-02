import java.util.*;

/**
 * NFA 状态节点
 */
class NfaState {
    int id;
    char transition; // 转换字符，'ε' 表示空跳转
    List<NfaState> nextStates = new ArrayList<>(); // 存储跳转目标
    boolean isAccept = false; // 是否为接受态（在此阶段主要用于标识片段终点）
    // 核心：记录这是哪条规则的接受态。-1 表示非接受态，>=0 表示规则索引
    int ruleId = -1;

    public NfaState(int id) {
        this.id = id;
        this.transition = 'ε'; // 默认是 ε 边
    }

    public NfaState(int id, char transition) {
        this.id = id;
        this.transition = transition;
    }
}

/**
 * NFA 片段（包含一个入口和一个出口）
 */
class NfaFragment {
    NfaState start;
    NfaState accept;

    public NfaFragment(NfaState start, NfaState accept) {
        this.start = start;
        this.accept = accept;
    }
}

/**
 * NFA 构造器 - 使用 Thompson 构造法
 */
public class NfaBuilder {
    private int stateCounter = 0;
    private static final char EPSILON = 'ε';
    private static final char CONCAT = '·';
    private static final char ALT = '|';
    private static final char KLEENE = '*';

    private NfaState createNode() {
        return new NfaState(stateCounter++);
    }

    /**
     * 根据后缀表达式构建 NFA
     */
    public NfaFragment buildNfa(String postfix) {
        Stack<NfaFragment> stack = new Stack<>();

        for (int i = 0; i < postfix.length(); i++) {
            char c = postfix.charAt(i);

            switch (c) {
                case KLEENE: // 闭包运算 *
                    stack.push(doKleene(stack.pop()));
                    break;
                case ALT: // 选择运算 |
                    NfaFragment rightAlt = stack.pop();
                    NfaFragment leftAlt = stack.pop();
                    stack.push(doAlt(leftAlt, rightAlt));
                    break;
                case CONCAT: // 连接运算 ·
                    NfaFragment rightConcat = stack.pop();
                    NfaFragment leftConcat = stack.pop();
                    stack.push(doConcat(leftConcat, rightConcat));
                    break;
                default: // 普通字符（操作数）
                    stack.push(doOperand(c));
                    break;
            }
        }

        NfaFragment finalNfa = stack.pop();
        finalNfa.accept.isAccept = true;
        return finalNfa;
    }

    // 处理普通字符：s0 --c--> s1
    private NfaFragment doOperand(char c) {
        NfaState start = createNode();
        NfaState accept = createNode();
        start.transition = c;
        start.nextStates.add(accept);
        return new NfaFragment(start, accept);
    }

    // 处理连接 a·b：让 a 的终点指向 b 的起点
    private NfaFragment doConcat(NfaFragment f1, NfaFragment f2) {
        // Thompson 构造法中，连接只需将第一个片段的接受态指向第二个片段的起始态
        f1.accept.nextStates.add(f2.start);
        return new NfaFragment(f1.start, f2.accept);
    }

    // 处理选择 a|b
    private NfaFragment doAlt(NfaFragment f1, NfaFragment f2) {
        NfaState start = createNode();
        NfaState accept = createNode();

        // 新起点通过 ε 边连向两个子片段的起点
        start.nextStates.add(f1.start);
        start.nextStates.add(f2.start);

        // 两个子片段的终点通过 ε 边连向新终点
        f1.accept.nextStates.add(accept);
        f2.accept.nextStates.add(accept);

        return new NfaFragment(start, accept);
    }

    // 处理闭包 a*
    private NfaFragment doKleene(NfaFragment f1) {
        NfaState start = createNode();
        NfaState accept = createNode();

        // 1. 匹配 0 次：start -> accept
        start.nextStates.add(accept);
        // 2. 进入匹配：start -> f1.start
        start.nextStates.add(f1.start);

        // 3. 匹配结束：f1.accept -> accept
        f1.accept.nextStates.add(accept);
        // 4. 重复匹配：f1.accept -> f1.start
        f1.accept.nextStates.add(f1.start);

        return new NfaFragment(start, accept);
    }

    /**
     * 辅助调试：打印 NFA 的所有状态转移
     */
    public void printNfa(NfaState start) {
        Set<NfaState> visited = new HashSet<>();
        Queue<NfaState> queue = new LinkedList<>();
        queue.add(start);
        visited.add(start);

        System.out.println("=== NFA 状态转移表 ===");
        while (!queue.isEmpty()) {
            NfaState current = queue.poll();
            for (NfaState next : current.nextStates) {
                System.out.printf("State %d --(%c)--> State %d %s\n",
                        current.id, current.transition, next.id, next.isAccept ? "[ACCEPT]" : "");
                if (!visited.contains(next)) {
                    visited.add(next);
                    queue.add(next);
                }
            }
        }
    }

    public static void main(String[] args) {
        // 1. 模拟 RegexConverter 的输出结果
        // 示例正则：(a|b)*
        // 后缀表达式：ab|*
        String postfix = "ab|*";

        NfaBuilder builder = new NfaBuilder();
        NfaFragment nfa = builder.buildNfa(postfix);

        // 2. 打印生成的 NFA 结构
        builder.printNfa(nfa.start);
    }
}