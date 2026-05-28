package com.example.compiler.test;

import com.example.compiler.CompileResult;
import com.example.compiler.Compiler;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

public final class FlowchartPipelineEndToEndTest {
    @Test
    void strictFlowchartPipelineProducesEvidence() throws Exception {
        assumeTrue(commandAvailable("gcc"), "skipped because gcc not found");

        Path outputRoot = Path.of("generated", "strict-flowchart-test");
        CompileResult result = new Compiler().compileStrictFlowchart(
                Path.of("resources", "c99.l"),
                Path.of("resources", "c99.y"),
                Path.of("src", "test", "resources", "pipeline", "minimal.c"),
                outputRoot,
                null,
                null,
                null,
                null
        );

        assertTrue(result.isSuccess(), "strict flowchart pipeline should succeed");

        assertNonEmpty(outputRoot.resolve("00-input").resolve("c99.l"));
        assertNonEmpty(outputRoot.resolve("00-input").resolve("c99.y"));
        assertNonEmpty(outputRoot.resolve("00-input").resolve("test.c"));
        assertEquals(Files.readString(Path.of("resources", "c99.l")),
                Files.readString(outputRoot.resolve("00-input").resolve("c99.l")),
                "flow must copy and use resources/c99.l");
        assertEquals(Files.readString(Path.of("resources", "c99.y")),
                Files.readString(outputRoot.resolve("00-input").resolve("c99.y")),
                "flow must copy and use resources/c99.y");

        assertNonEmpty(result.generatedLexerC());
        assertExecutable(outputRoot.resolve("01-lex").resolve("yylex"));
        assertNonEmpty(result.tokenFile());
        assertNonEmpty(result.generatedParserC());
        assertExecutable(outputRoot.resolve("02-yacc").resolve("yyparse"));
        assertNonEmpty(result.parseTreeFile());
        assertNonEmpty(result.coreAstFile());
        assertNonEmpty(result.symbolTableFile());
        assertNonEmpty(result.generatedSemanticC());
        assertExecutable(outputRoot.resolve("03-semantic").resolve("yysemantic"));
        assertNonEmpty(result.llvmIrFile());
        assertNonEmpty(result.jimpleFile());

        String llvm = Files.readString(result.llvmIrFile());
        assertTrue(llvm.contains("define") || llvm.contains("main"),
                "LLVM IR should contain define or main");
        assertTrue(!Files.readString(result.jimpleFile()).isBlank(),
                "Jimple output should not be blank");
        assertTrue(Files.readString(result.parseTreeFile()).contains("__ACT_"),
                "action tree should contain semantic action nodes");
        assertTrue(Files.readString(result.symbolTableFile()).contains("main"),
                "symbol-table.txt should contain main");

        assertNonEmpty(result.commandsLogFile());
        String commands = Files.readString(result.commandsLogFile());
        assertTrue(commands.contains("gcc yylex.c -o yylex"), "commands.log should prove lexer gcc step");
        assertTrue(commands.contains("./yylex"), "commands.log should prove yylex execution");
        assertTrue(commands.contains("gcc yyparse.c -o yyparse"), "commands.log should prove parser gcc step");
        assertTrue(commands.contains("./yyparse"), "commands.log should prove yyparse execution");
        assertTrue(commands.contains("gcc yysemantic.c -o yysemantic"), "commands.log should prove semantic gcc step");
        assertTrue(commands.contains("./yysemantic"), "commands.log should prove yysemantic execution");

        assertNonEmpty(result.pipelineTraceFile());
        String trace = Files.readString(result.pipelineTraceFile());
        assertTrue(trace.contains("\"stage\": \"lex-generate-yylex-c\""));
        assertTrue(trace.contains("\"stage\": \"yacc-generate-yyparse-c\""));
        assertTrue(trace.contains("\"stage\": \"semantic-action-tree-to-core-ast\""));
        assertTrue(trace.contains("\"stage\": \"semantic-check-symbol-table\""));
        assertTrue(trace.contains("\"stage\": \"semantic-compile-yysemantic\""));
        assertTrue(trace.contains("\"stage\": \"ir-write-jimple\""));
        assertTrue(trace.contains("\"stage\": \"soot-detect\""));
        assertTrue(trace.contains("\"stage\": \"soot-skipped\"") || trace.contains("\"stage\": \"soot-run\""));
        assertTrue(!trace.contains("\"success\": false"), "every pipeline stage should succeed");

        Path sootSkipped = outputRoot.resolve("05-soot").resolve("soot-skipped.txt");
        Path sootOutput = outputRoot.resolve("05-soot").resolve("soot-output");
        assertTrue(Files.exists(sootSkipped) || Files.exists(sootOutput),
                "Soot backend should either produce soot output or soot-skipped.txt");
        if (Files.exists(sootSkipped)) {
            assertNonEmpty(sootSkipped);
        }

        assertNonEmpty(result.evidenceFile());
        String evidence = Files.readString(result.evidenceFile());
        assertTrue(evidence.contains("词法规则 c99.l"));
        assertTrue(evidence.contains("语法规则 c99.y"));
        assertTrue(evidence.contains("yysemantic"));
    }

    private static void assertNonEmpty(Path file) throws Exception {
        assertNotNull(file, "path should not be null");
        assertTrue(Files.exists(file), file + " should exist");
        assertTrue(Files.size(file) > 0, file + " should not be empty");
    }

    private static void assertExecutable(Path base) {
        assertTrue(Files.exists(base) || Files.exists(Path.of(base.toString() + ".exe")),
                base + " or .exe should exist");
    }

    private static boolean commandAvailable(String command) {
        try {
            Process process = new ProcessBuilder(command, "--version")
                    .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                    .redirectError(ProcessBuilder.Redirect.DISCARD)
                    .start();
            return process.waitFor() == 0;
        } catch (Exception ignored) {
            return false;
        }
    }
}
