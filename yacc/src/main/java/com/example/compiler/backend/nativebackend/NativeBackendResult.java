package com.example.compiler.backend.nativebackend;

import java.nio.file.Path;
import java.util.List;

public record NativeBackendResult(
        boolean success,
        boolean skipped,
        String skippedReason,
        Path inputLl,
        Path assemblyFile,
        Path objectFile,
        Path executableFile,
        Path traceFile,
        Path reportFile,
        Integer validateExitCode,
        Integer assemblyExitCode,
        Integer objectExitCode,
        Integer linkExitCode,
        Integer runExitCode,
        String stdout,
        String stderr,
        List<NativeBackendStep> steps
) {
    public NativeBackendResult {
        steps = steps == null ? List.of() : List.copyOf(steps);
    }
}
