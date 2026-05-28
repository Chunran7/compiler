package com.example.compiler.backend.nativebackend;

import java.util.List;

public record NativeBackendStep(
        String stage,
        String command,
        List<String> inputs,
        List<String> outputs,
        Integer exitCode,
        boolean success,
        String skippedReason,
        long durationMs,
        String stdout,
        String stderr
) {
    public NativeBackendStep {
        inputs = inputs == null ? List.of() : List.copyOf(inputs);
        outputs = outputs == null ? List.of() : List.copyOf(outputs);
    }
}
