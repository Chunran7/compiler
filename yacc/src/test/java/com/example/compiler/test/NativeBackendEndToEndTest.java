package com.example.compiler.test;

import com.example.compiler.CompileResult;
import com.example.compiler.Compiler;
import com.example.compiler.backend.nativebackend.NativeBackend;
import com.example.compiler.backend.nativebackend.NativeBackendConfig;
import com.example.compiler.backend.nativebackend.NativeBackendResult;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

public final class NativeBackendEndToEndTest {
    @Test
    void clangCompilesAndRunsGeneratedLlvmIr() throws Exception {
        assumeTrue(commandAvailable("gcc"), "skipped because gcc not found");
        assumeTrue(commandAvailable("clang"), "skipped because clang not found");

        Path outputRoot = Path.of("generated", "native-backend-test");
        CompileResult compileResult = new Compiler().compileStrictFlowchart(
                Path.of("resources", "c99.l"),
                Path.of("resources", "c99.y"),
                Path.of("src", "test", "resources", "pipeline", "minimal.c"),
                outputRoot,
                null,
                null,
                null,
                null
        );
        assertTrue(compileResult.isSuccess());
        assertNonEmpty(outputRoot.resolve("04-ir").resolve("output.ll"));

        NativeBackendResult nativeResult = new NativeBackend().run(
                NativeBackendConfig.defaults(compileResult.llvmIrFile(), outputRoot.resolve("06-native"))
                        .withRunExecutable(true)
        );

        assertTrue(nativeResult.success(), "native backend should succeed");
        assertTrue(!nativeResult.skipped(), "native backend should not skip when clang exists");
        assertNonEmpty(nativeResult.assemblyFile());
        assertNonEmpty(nativeResult.objectFile());
        assertNonEmpty(nativeResult.executableFile());
        assertNonEmpty(nativeResult.traceFile());
        assertNonEmpty(nativeResult.reportFile());
        assertEquals(0, nativeResult.validateExitCode());
        assertEquals(0, nativeResult.assemblyExitCode());
        assertEquals(0, nativeResult.objectExitCode());
        assertEquals(0, nativeResult.linkExitCode());
        assertEquals(3, nativeResult.runExitCode(), "minimal.c should return 1 + 2");

        String pipelineTrace = Files.readString(outputRoot.resolve("pipeline-trace.json"));
        assertTrue(pipelineTrace.contains("\"stage\": \"native-detect-clang\""));
        assertTrue(pipelineTrace.contains("\"stage\": \"native-run-executable\""));
        assertTrue(!pipelineTrace.contains("\"success\": false"));

        String commands = Files.readString(outputRoot.resolve("commands.log"));
        assertTrue(commands.contains("clang --version"));
        assertTrue(commands.contains("clang -O0 -c ../04-ir/output.ll -o validate.o"));
        assertTrue(commands.contains("clang -O0 -S ../04-ir/output.ll -o output.s"));
        assertTrue(commands.contains("clang -O0 -c ../04-ir/output.ll -o output.o"));
        assertTrue(commands.contains("clang output.o -o ./native-executable"));
        assertTrue(commands.contains("./native-executable"));

        String report = Files.readString(nativeResult.reportFile());
        assertTrue(report.contains("# Native Backend Report"));
        assertTrue(report.contains("run exit code: `3`"));
    }

    private static void assertNonEmpty(Path file) throws Exception {
        assertNotNull(file, "path should not be null");
        assertTrue(Files.exists(file), file + " should exist");
        assertTrue(Files.size(file) > 0, file + " should not be empty");
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
