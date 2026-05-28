package com.example.compiler.lex;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public final class CLexerToolchainEmitter {
    public String emitFromLexFile(Path lexFile) throws IOException {
        SeuLexParser parser = new SeuLexParser();
        String content = Files.readString(lexFile);
        parser.splitLexFile(content);
        parser.parseDefinitions();
        parser.parseRules();

        NfaManager manager = new NfaManager();
        NfaState globalStart = manager.buildCombinedNfa(parser.getRules());
        NfaToDfaConverter converter = new NfaToDfaConverter();
        List<DfaState> states = converter.minimize(converter.convert(globalStart));
        return new CLexerProgramEmitter().emit(states, parser.getRules());
    }
}
