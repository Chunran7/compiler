import java.util.*;

/**
 * DFA 状态：本质上是 NFA 状态的一个集合
 */
class DfaState {
    int id;
    Set<NfaState> nfaStates; // 该 DFA 状态包含的 NFA 状态集合
    Map<Character, DfaState> transitions = new HashMap<>(); // 转移表：字符 -> 下一个 DFA 状态
    
    boolean isAccept = false;
    int acceptedRuleId = -1; // 最终匹配到的规则 ID（优先级最高的一个）

    public DfaState(int id, Set<NfaState> nfaStates) {
        this.id = id;
        this.nfaStates = nfaStates;
        
        // 确定该 DFA 状态是否为接受态，并处理 Lex 优先级
        for (NfaState s : nfaStates) {
            if (s.isAccept) {
                this.isAccept = true;
                // 优先级原则：如果一个 DFA 状态包含多个 NFA 接受态，选择 ruleId 最小的
                if (this.acceptedRuleId == -1 || s.ruleId < this.acceptedRuleId) {
                    this.acceptedRuleId = s.ruleId;
                }
            }
        }
    }

    // 重写 equals 和 hashCode 是为了在 Set/Map 中通过 nfaStates 集合来去重
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
}
