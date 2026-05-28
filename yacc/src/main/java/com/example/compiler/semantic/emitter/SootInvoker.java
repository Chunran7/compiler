package com.example.compiler.semantic.emitter;

import com.example.compiler.backend.nativebackend.ProcessRunner;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

public final class SootInvoker {
    public Path invokeIfAvailable(Path jimpleFile, Path bytecodeOutputDir) throws IOException, InterruptedException {
        String sootJar = System.getenv("SOOT_JAR");
        if (sootJar == null || sootJar.isBlank()) {
            return null;
        }

        Files.createDirectories(bytecodeOutputDir);
        ProcessRunner.ProcessResult result = new ProcessRunner().run(List.of(
                "java",
                "-cp",
                sootJar,
                "soot.Main",
                "-src-prec",
                "J",
                "-process-dir",
                jimpleFile.getParent().toString(),
                "-output-dir",
                bytecodeOutputDir.toString()
        ), Path.of(".").toAbsolutePath().normalize(), Duration.ofSeconds(60));
        if (result.exitCode() != 0) {
            throw new RuntimeException("Command failed (" + result.exitCode() + "): soot"
                    + System.lineSeparator() + result.stdout() + result.stderr());
        }
        return bytecodeOutputDir;
    }
}
