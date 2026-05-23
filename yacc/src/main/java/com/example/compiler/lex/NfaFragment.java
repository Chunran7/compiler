package com.example.compiler.lex;

class NfaFragment {
    NfaState start;
    NfaState accept;

    public NfaFragment(NfaState start, NfaState accept) {
        this.start = start;
        this.accept = accept;
    }
}
