package com.example.compiler.backend.nativebackend;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

public final class ProcessRunner {
    public ProcessResult run(List<String> command, Path workingDirectory, Duration timeout)
            throws IOException, InterruptedException {
        long start = System.nanoTime();
        ProcessBuilder builder = new ProcessBuilder(command);
        if (workingDirectory != null) {
            builder.directory(workingDirectory.toFile());
        }
        Process process = builder.start();
        boolean completed = process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS);
        if (!completed) {
            process.destroyForcibly();
            String stderr = new String(process.getErrorStream().readAllBytes(), StandardCharsets.UTF_8)
                    + "Process timed out after " + timeout;
            return new ProcessResult(-1,
                    new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8),
                    stderr,
                    elapsedMs(start),
                    true);
        }
        return new ProcessResult(
                process.exitValue(),
                new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8),
                new String(process.getErrorStream().readAllBytes(), StandardCharsets.UTF_8),
                elapsedMs(start),
                false
        );
    }

    private static long elapsedMs(long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000L;
    }

    public record ProcessResult(int exitCode, String stdout, String stderr, long durationMs, boolean timedOut) {
    }
}
