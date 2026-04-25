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

    /**
     * DFA 最小化 (等价类划分法)
     */
    public List<DfaState> minimize(List<DfaState> dfaStates) {
        if (dfaStates.isEmpty()) return dfaStates;

        // 1. 收集所有出现过的输入字符作为字母表
        Set<Character> alphabet = new HashSet<>();
        for (DfaState s : dfaStates) {
            alphabet.addAll(s.transitions.keySet());
        }

        // 2. 建立初始划分 P0
        // 非接受态归为一组，接受态按照 acceptedRuleId 分组 (解决规则冲突)
        Map<Integer, Set<DfaState>> acceptGroups = new HashMap<>();
        Set<DfaState> nonAcceptGroup = new HashSet<>();
        
        for (DfaState s : dfaStates) {
            if (s.isAccept) {
                acceptGroups.computeIfAbsent(s.acceptedRuleId, k -> new HashSet<>()).add(s);
            } else {
                nonAcceptGroup.add(s);
            }
        }
        
        List<Set<DfaState>> P = new ArrayList<>(acceptGroups.values());
        if (!nonAcceptGroup.isEmpty()) {
            P.add(nonAcceptGroup);
        }

        // 3. 不断分割直至稳定
        boolean changed = true;
        while (changed) {
            changed = false;
            List<Set<DfaState>> newP = new ArrayList<>();
            
            for (Set<DfaState> group : P) {
                if (group.size() <= 1) {
                    newP.add(group);
                    continue;
                }
                
                // 将同一个 group 内的状态按照它们在各字符下的转移目标所在的分组进行签名分类
                Map<List<Integer>, Set<DfaState>> splits = new HashMap<>();
                for (DfaState s : group) {
                    List<Integer> signature = new ArrayList<>();
                    for (char c : alphabet) {
                        DfaState nextState = s.transitions.get(c);
                        int targetGroupIdx = -1; // -1 表示转移到死状态(无边)
                        if (nextState != null) {
                            targetGroupIdx = findGroupIndex(P, nextState);
                        }
                        signature.add(targetGroupIdx);
                    }
                    splits.computeIfAbsent(signature, k -> new HashSet<>()).add(s);
                }
                
                newP.addAll(splits.values());
                if (splits.size() > 1) {
                    changed = true; // 发生了实际分割
                }
            }
            P = newP;
        }

        // 4. 根据最终的等价划分重构最小 DFA
        List<DfaState> minDfa = new ArrayList<>();
        Map<Set<DfaState>, DfaState> groupToMinState = new HashMap<>();
        
        // 为每个等价类创建一个新的 DFA 状态
        int minIdCounter = 0;
        for (Set<DfaState> group : P) {
            DfaState rep = group.iterator().next(); // 取一个代表元
            // 组内所有状态的接受性质是一致的
            DfaState newState = new DfaState(minIdCounter++, new HashSet<>()); 
            newState.isAccept = rep.isAccept;
            newState.acceptedRuleId = rep.acceptedRuleId;
            minDfa.add(newState);
            groupToMinState.put(group, newState);
        }

        // 建立新状态之间的边
        // 找到原来的起点的旧状态，它所在的组对应的新状态就是新起点
        DfaState oldStart = dfaStates.get(0); 
        DfaState newStart = null;
        
        for (Set<DfaState> group : P) {
            DfaState rep = group.iterator().next();
            DfaState currentMinState = groupToMinState.get(group);
            
            if (group.contains(oldStart)) {
                newStart = currentMinState;
            }
            
            for (Map.Entry<Character, DfaState> entry : rep.transitions.entrySet()) {
                char c = entry.getKey();
                DfaState oldNext = entry.getValue();
                Set<DfaState> targetGroup = P.get(findGroupIndex(P, oldNext));
                DfaState newNext = groupToMinState.get(targetGroup);
                currentMinState.transitions.put(c, newNext);
            }
        }
        
        // 把 start 调整到 index 0 的位置
        if (newStart != null && minDfa.get(0) != newStart) {
            minDfa.remove(newStart);
            minDfa.add(0, newStart);
            // 重新按顺序给一下 id，保证输出干净
            for(int i=0; i<minDfa.size(); i++){
                minDfa.get(i).id = i;
            }
        }

        return minDfa;
    }

    private int findGroupIndex(List<Set<DfaState>> partitions, DfaState target) {
        for (int i = 0; i < partitions.size(); i++) {
            if (partitions.get(i).contains(target)) {
                return i;
            }
        }
        return -1;
    }
}
