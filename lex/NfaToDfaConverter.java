import java.util.*;

public class NfaToDfaConverter {
    private int dfaIdCounter = 0;

    /**
     * 核心算法 1: 计算 ε-closure (闭包)
     * 找出从 states 集合出发，仅通过 ε 边能到达的所有 NFA 状态
     */
    public Set<NfaState> epsilonClosure(Set<NfaState> states) {
        Set<NfaState> closure = new HashSet<>(states);
        Stack<NfaState> stack = new Stack<>();
        stack.addAll(states);

        while (!stack.isEmpty()) {
            NfaState current = stack.pop();
            // 如果 transition 是 ε，则把它的 nextStates 都加入闭包
            if (current.transition == 'ε') {
                for (NfaState next : current.nextStates) {
                    if (!closure.contains(next)) {
                        closure.add(next);
                        stack.push(next);
                    }
                }
            }
        }
        return closure;
    }

    /**
     * 核心算法 2: 计算 move(T, c)
     * 找出从 states 集合出发，经过字符 c 跳转到的 NFA 状态
     */
    public Set<NfaState> move(Set<NfaState> states, char c) {
        Set<NfaState> result = new HashSet<>();
        for (NfaState s : states) {
            if (s.transition == c) {
                result.addAll(s.nextStates);
            }
        }
        return result;
    }

    /**
     * 子集构造法：将 NFA 转为 DFA
     */
    public List<DfaState> convert(NfaState nfaStart) {
        List<DfaState> dfaStates = new ArrayList<>();
        Queue<DfaState> unprocessed = new LinkedList<>();

        // 1. 获取字符集（字母表）：遍历 NFA 收集所有非 ε 的字符
        Set<Character> alphabet = collectAlphabet(nfaStart);

        // 2. 创建初始 DFA 状态：ε-closure(NFA 起点)
        Set<NfaState> startSet = new HashSet<>();
        startSet.add(nfaStart);
        DfaState startDfa = new DfaState(dfaIdCounter++, epsilonClosure(startSet));
        
        dfaStates.add(startDfa);
        unprocessed.add(startDfa);

        // 3. 迭代处理每一个尚未展开的 DFA 状态
        while (!unprocessed.isEmpty()) {
            DfaState currentDfa = unprocessed.poll();

            for (char c : alphabet) {
                if (c == 'ε') continue;

                // 计算当前 DFA 状态经过字符 c 后的 NFA 集合
                Set<NfaState> movedSet = move(currentDfa.nfaStates, c);
                if (movedSet.isEmpty()) continue;

                // 计算该集合的闭包
                Set<NfaState> targetClosure = epsilonClosure(movedSet);

                // 检查这个集合是否已经是一个存在的 DFA 状态了
                DfaState existing = findState(dfaStates, targetClosure);
                if (existing == null) {
                    // 如果是新集合，创建新状态并加入队列
                    DfaState newState = new DfaState(dfaIdCounter++, targetClosure);
                    dfaStates.add(newState);
                    unprocessed.add(newState);
                    currentDfa.transitions.put(c, newState);
                } else {
                    // 如果已存在，直接连边
                    currentDfa.transitions.put(c, existing);
                }
            }
        }
        return dfaStates;
    }

    private DfaState findState(List<DfaState> states, Set<NfaState> nfaSet) {
        for (DfaState s : states) {
            if (s.nfaStates.equals(nfaSet)) return s;
        }
        return null;
    }

    private Set<Character> collectAlphabet(NfaState start) {
        Set<Character> alphabet = new HashSet<>();
        Set<NfaState> visited = new HashSet<>();
        Queue<NfaState> q = new LinkedList<>();
        q.add(start);
        visited.add(start);
        while (!q.isEmpty()) {
            NfaState s = q.poll();
            if (s.transition != 'ε') alphabet.add(s.transition);
            for (NfaState n : s.nextStates) {
                if (!visited.contains(n)) {
                    visited.add(n);
                    q.add(n);
                }
            }
        }
        return alphabet;
    }

    /**
     * 打印 DFA 转移表进行验证
     */
    public void printDfaTable(List<DfaState> dfaStates) {
        System.out.println("\n===== DFA 状态转移表 =====");
        for (DfaState s : dfaStates) {
            String type = s.isAccept ? "[接受态 Rule:" + s.acceptedRuleId + "]" : "[中间态]";
            System.out.println("DFA 状态 " + s.id + " " + type + ":");
            s.transitions.forEach((c, next) -> {
                System.out.println("  --(" + c + ")--> 状态 " + next.id);
            });
        }
    }
}
