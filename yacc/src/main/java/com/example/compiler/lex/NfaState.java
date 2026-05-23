package com.example.compiler.lex;

import java.util.*;

class NfaState {
    int id;
    char transition;
    List<NfaState> nextStates = new ArrayList<>();
    boolean isAccept = false;
    int ruleId = -1;

    public NfaState(int id) {
        this.id = id;
        this.transition = 'ε';
    }

    public NfaState(int id, char transition) {
        this.id = id;
        this.transition = transition;
    }
}
