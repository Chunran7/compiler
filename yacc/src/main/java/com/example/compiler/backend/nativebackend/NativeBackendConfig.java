package com.example.compiler.backend.nativebackend;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

public record NativeBackendConfig(
        Path llvmIrFile,
        Path outputDir,
        String clangCommand,
        boolean validateIr,
        boolean emitAssembly,
        boolean emitObject,
        boolean linkExecutable,
        boolean runExecutable,
        String optimizationLevel,
        List<String> extraClangArgs,
        Duration timeout
) {
    public NativeBackendConfig {
        if (clangCommand == null || clangCommand.isBlank()) {
            clangCommand = "clang";
        }
        if (optimizationLevel == null || optimizationLevel.isBlank()) {
            optimizationLevel = "O0";
        }
        if (!List.of("O0", "O1", "O2", "O3").contains(optimizationLevel)) {
            throw new IllegalArgumentException("Unsupported native optimization level: " + optimizationLevel);
        }
        if (extraClangArgs == null) {
            extraClangArgs = List.of();
        } else {
            extraClangArgs = List.copyOf(extraClangArgs);
        }
        if (timeout == null) {
            timeout = Duration.ofSeconds(30);
        }
    }

    public static NativeBackendConfig defaults(Path llvmIrFile, Path outputDir) {
        return new NativeBackendConfig(
                llvmIrFile,
                outputDir,
                "clang",
                true,
                true,
                true,
                true,
                false,
                "O0",
                List.of(),
                Duration.ofSeconds(30)
        );
    }

    public NativeBackendConfig withRunExecutable(boolean run) {
        return new NativeBackendConfig(llvmIrFile, outputDir, clangCommand, validateIr, emitAssembly,
                emitObject, linkExecutable, run, optimizationLevel, extraClangArgs, timeout);
    }

    public NativeBackendConfig withClangCommand(String command) {
        return new NativeBackendConfig(llvmIrFile, outputDir, command, validateIr, emitAssembly,
                emitObject, linkExecutable, runExecutable, optimizationLevel, extraClangArgs, timeout);
    }

    public NativeBackendConfig withOptimizationLevel(String level) {
        return new NativeBackendConfig(llvmIrFile, outputDir, clangCommand, validateIr, emitAssembly,
                emitObject, linkExecutable, runExecutable, level, extraClangArgs, timeout);
    }
}
