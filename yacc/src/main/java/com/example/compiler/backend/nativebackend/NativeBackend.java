package com.example.compiler.backend.nativebackend;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

public final class NativeBackend {
    private final ProcessRunner processRunner;

    public NativeBackend() {
        this(new ProcessRunner());
    }

    public NativeBackend(ProcessRunner processRunner) {
        this.processRunner = processRunner;
    }

    public NativeBackendResult run(NativeBackendConfig config) throws IOException, InterruptedException {
        Files.createDirectories(config.outputDir());
        deleteOldOutputs(config.outputDir());

        List<NativeBackendStep> steps = new ArrayList<>();
        Path validateObject = config.outputDir().resolve("validate.o");
        Path assemblyFile = config.outputDir().resolve("output.s");
        Path objectFile = config.outputDir().resolve("output.o");
        Path executableFile = config.outputDir().resolve("native-executable");
        Path traceFile = config.outputDir().resolve("native-backend-trace.json");
        Path reportFile = config.outputDir().resolve("native-backend-report.md");

        NativeBackendStep detect = runStep(
                "native-detect-clang",
                List.of(config.clangCommand(), "--version"),
                config.outputDir(),
                List.of(),
                List.of(),
                config
        );
        steps.add(detect);
        if (!detect.success()) {
            NativeBackendResult skipped = result(config, true, "clang not found", assemblyFile, objectFile,
                    executableFile, traceFile, reportFile, null, null, null, null, null, steps);
            writeTrace(traceFile, steps);
            writeReport(reportFile, skipped, detect.stdout());
            appendPipelineEvidence(config, skipped);
            return skipped;
        }

        Integer validateExit = null;
        if (config.validateIr()) {
            NativeBackendStep validate = runStep(
                    "native-validate-llvm-ir",
                    clangBase(config, "-c", config.llvmIrFile().toAbsolutePath().toString(), "-o", validateObject.toAbsolutePath().toString()),
                    config.outputDir(),
                    List.of(config.llvmIrFile().toString()),
                    List.of(validateObject.toString()),
                    config
            );
            steps.add(validate);
            validateExit = validate.exitCode();
            if (!validate.success()) {
                NativeBackendResult failed = result(config, false, null, assemblyFile, objectFile,
                        executableFile, traceFile, reportFile, validateExit, null, null, null, null, steps);
                writeTrace(traceFile, steps);
                writeReport(reportFile, failed, detect.stdout());
                appendPipelineEvidence(config, failed);
                return failed;
            }
        }

        Integer assemblyExit = null;
        if (config.emitAssembly()) {
            NativeBackendStep assembly = runStep(
                    "native-emit-assembly",
                    clangBase(config, "-S", config.llvmIrFile().toAbsolutePath().toString(), "-o", assemblyFile.toAbsolutePath().toString()),
                    config.outputDir(),
                    List.of(config.llvmIrFile().toString()),
                    List.of(assemblyFile.toString()),
                    config
            );
            steps.add(assembly);
            assemblyExit = assembly.exitCode();
            if (!assembly.success()) {
                NativeBackendResult failed = result(config, false, null, assemblyFile, objectFile,
                        executableFile, traceFile, reportFile, validateExit, assemblyExit, null, null, null, steps);
                writeTrace(traceFile, steps);
                writeReport(reportFile, failed, detect.stdout());
                appendPipelineEvidence(config, failed);
                return failed;
            }
        }

        Integer objectExit = null;
        if (config.emitObject()) {
            NativeBackendStep object = runStep(
                    "native-emit-object",
                    clangBase(config, "-c", config.llvmIrFile().toAbsolutePath().toString(), "-o", objectFile.toAbsolutePath().toString()),
                    config.outputDir(),
                    List.of(config.llvmIrFile().toString()),
                    List.of(objectFile.toString()),
                    config
            );
            steps.add(object);
            objectExit = object.exitCode();
            if (!object.success()) {
                NativeBackendResult failed = result(config, false, null, assemblyFile, objectFile,
                        executableFile, traceFile, reportFile, validateExit, assemblyExit, objectExit, null, null, steps);
                writeTrace(traceFile, steps);
                writeReport(reportFile, failed, detect.stdout());
                appendPipelineEvidence(config, failed);
                return failed;
            }
        }

        Integer linkExit = null;
        if (config.linkExecutable()) {
            NativeBackendStep link = runStep(
                    "native-link-executable",
                    List.of(config.clangCommand(), objectFile.toAbsolutePath().toString(), "-o", executableFile.toAbsolutePath().toString()),
                    config.outputDir(),
                    List.of(objectFile.toString()),
                    List.of(executableFile.toString()),
                    config
            );
            steps.add(link);
            linkExit = link.exitCode();
            if (!link.success()) {
                NativeBackendResult failed = result(config, false, null, assemblyFile, objectFile,
                        executableFile, traceFile, reportFile, validateExit, assemblyExit, objectExit, linkExit, null, steps);
                writeTrace(traceFile, steps);
                writeReport(reportFile, failed, detect.stdout());
                appendPipelineEvidence(config, failed);
                return failed;
            }
        }

        Integer runExit = null;
        String stdout = "";
        String stderr = "";
        if (config.runExecutable()) {
            NativeBackendStep run = runStep(
                    "native-run-executable",
                    List.of(executableFile.toAbsolutePath().toString()),
                    config.outputDir(),
                    List.of(executableFile.toString()),
                    List.of(),
                    config
            );
            steps.add(run);
            runExit = run.exitCode();
            stdout = run.stdout();
            stderr = run.stderr();
        }

        NativeBackendResult result = new NativeBackendResult(
                allStepsSuccessful(steps),
                false,
                null,
                config.llvmIrFile(),
                assemblyFile,
                objectFile,
                executableFile,
                traceFile,
                reportFile,
                validateExit,
                assemblyExit,
                objectExit,
                linkExit,
                runExit,
                stdout,
                stderr,
                steps
        );
        writeTrace(traceFile, steps);
        writeReport(reportFile, result, detect.stdout());
        appendPipelineEvidence(config, result);
        return result;
    }

    private NativeBackendStep runStep(String stage,
                                      List<String> command,
                                      Path workingDirectory,
                                      List<String> inputs,
                                      List<String> outputs,
                                      NativeBackendConfig config) throws IOException, InterruptedException {
        ProcessRunner.ProcessResult result = processRunner.run(command, workingDirectory, config.timeout());
        boolean success = "native-run-executable".equals(stage) ? !result.timedOut() : result.exitCode() == 0;
        return new NativeBackendStep(stage, displayCommand(command), inputs, outputs, result.exitCode(),
                success, result.timedOut() ? "timeout" : null, result.durationMs(),
                result.stdout(), result.stderr());
    }

    private static List<String> clangBase(NativeBackendConfig config, String... args) {
        List<String> command = new ArrayList<>();
        command.add(config.clangCommand());
        command.add("-" + config.optimizationLevel());
        command.addAll(config.extraClangArgs());
        command.addAll(List.of(args));
        return command;
    }

    private static boolean allStepsSuccessful(List<NativeBackendStep> steps) {
        for (NativeBackendStep step : steps) {
            if (!step.success()) {
                return false;
            }
        }
        return true;
    }

    private static NativeBackendResult result(NativeBackendConfig config,
                                              boolean skipped,
                                              String skippedReason,
                                              Path assemblyFile,
                                              Path objectFile,
                                              Path executableFile,
                                              Path traceFile,
                                              Path reportFile,
                                              Integer validateExit,
                                              Integer assemblyExit,
                                              Integer objectExit,
                                              Integer linkExit,
                                              Integer runExit,
                                              List<NativeBackendStep> steps) {
        return new NativeBackendResult(
                !skipped && allStepsSuccessful(steps),
                skipped,
                skippedReason,
                config.llvmIrFile(),
                assemblyFile,
                objectFile,
                executableFile,
                traceFile,
                reportFile,
                validateExit,
                assemblyExit,
                objectExit,
                linkExit,
                runExit,
                "",
                "",
                steps
        );
    }

    private static void deleteOldOutputs(Path outputDir) throws IOException {
        for (String name : List.of("validate.o", "output.s", "output.o", "native-executable",
                "native-backend-trace.json", "native-backend-report.md")) {
            Files.deleteIfExists(outputDir.resolve(name));
        }
    }

    private static void writeTrace(Path traceFile, List<NativeBackendStep> steps) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("[\n");
        for (int i = 0; i < steps.size(); i++) {
            NativeBackendStep step = steps.get(i);
            sb.append("  {\n");
            sb.append("    \"stage\": \"").append(json(step.stage())).append("\",\n");
            sb.append("    \"inputs\": ").append(jsonArray(step.inputs())).append(",\n");
            sb.append("    \"outputs\": ").append(jsonArray(step.outputs())).append(",\n");
            sb.append("    \"command\": \"").append(json(step.command())).append("\",\n");
            sb.append("    \"exitCode\": ").append(step.exitCode() == null ? "null" : step.exitCode()).append(",\n");
            sb.append("    \"success\": ").append(step.success()).append(",\n");
            sb.append("    \"skippedReason\": ")
                    .append(step.skippedReason() == null ? "null" : "\"" + json(step.skippedReason()) + "\"").append(",\n");
            sb.append("    \"durationMs\": ").append(step.durationMs()).append(",\n");
            sb.append("    \"stdout\": \"").append(json(step.stdout())).append("\",\n");
            sb.append("    \"stderr\": \"").append(json(step.stderr())).append("\"\n");
            sb.append("  }");
            if (i + 1 < steps.size()) {
                sb.append(",");
            }
            sb.append("\n");
        }
        sb.append("]\n");
        Files.writeString(traceFile, sb.toString());
    }

    private static void writeReport(Path reportFile, NativeBackendResult result, String clangVersion) throws IOException {
        String report = """
                # Native Backend Report

                - clang version:
                ```text
                %s
                ```
                - input LLVM IR path: `%s`
                - validate result: `%s`
                - assembly output path: `%s`
                - object output path: `%s`
                - executable output path: `%s`
                - run exit code: `%s`
                - stdout:
                ```text
                %s
                ```
                - stderr:
                ```text
                %s
                ```
                - skipped reason: `%s`
                - limitations: Native backend depends on local clang. Current IR is generated for the project C subset; future external functions will need runtime libraries or extra link arguments.
                """.formatted(
                clangVersion == null ? "" : clangVersion.strip(),
                result.inputLl(),
                result.validateExitCode(),
                result.assemblyFile(),
                result.objectFile(),
                result.executableFile(),
                result.runExitCode(),
                result.stdout() == null ? "" : result.stdout(),
                result.stderr() == null ? "" : result.stderr(),
                result.skippedReason() == null ? "" : result.skippedReason()
        );
        Files.writeString(reportFile, report);
    }

    private static void appendPipelineEvidence(NativeBackendConfig config, NativeBackendResult result) throws IOException {
        Path pipelineRoot = config.outputDir().getParent();
        if (pipelineRoot == null) {
            return;
        }
        Path commandsLog = pipelineRoot.resolve("commands.log");
        if (Files.exists(commandsLog)) {
            for (NativeBackendStep step : result.steps()) {
                Files.writeString(commandsLog, step.command() + System.lineSeparator(),
                        StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                if (!step.stdout().isBlank()) {
                    Files.writeString(commandsLog, "  stdout: " + step.stdout().replace(System.lineSeparator(), "\\n")
                                    + System.lineSeparator(),
                            StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                }
                if (!step.stderr().isBlank()) {
                    Files.writeString(commandsLog, "  stderr: " + step.stderr().replace(System.lineSeparator(), "\\n")
                                    + System.lineSeparator(),
                            StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                }
            }
        }

        Path traceFile = pipelineRoot.resolve("pipeline-trace.json");
        if (Files.exists(traceFile)) {
            appendTrace(traceFile, result.steps());
        }

        Path evidenceFile = pipelineRoot.resolve("FLOWCHART_EVIDENCE.md");
        if (Files.exists(evidenceFile)) {
            String evidence = Files.readString(evidenceFile);
            if (!evidence.contains("项目扩展后端 Native Backend")) {
                Files.writeString(evidenceFile, """

                        ## 项目扩展后端 Native Backend

                        | 扩展后端节点 | 实际模块/命令 | 生成物 |
                        |---|---|---|
                        | LLVM IR | 04-ir/output.ll | - |
                        | Clang IR 校验 | clang -c output.ll -o validate.o | 06-native/validate.o |
                        | 生成汇编 | clang -S output.ll -o output.s | 06-native/output.s |
                        | 生成目标文件 | clang -c output.ll -o output.o | 06-native/output.o |
                        | 链接本机可执行文件 | clang output.o -o native-executable | 06-native/native-executable |
                        | 运行本机程序 | ./native-executable | exitCode / stdout / stderr |
                        """, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            }
        }
    }

    private static void appendTrace(Path traceFile, List<NativeBackendStep> steps) throws IOException {
        String existing = Files.readString(traceFile).stripTrailing();
        if (existing.endsWith("]")) {
            existing = existing.substring(0, existing.length() - 1).stripTrailing();
        }
        StringBuilder sb = new StringBuilder(existing);
        if (!existing.endsWith("[")) {
            sb.append(",");
        }
        sb.append("\n");
        for (int i = 0; i < steps.size(); i++) {
            NativeBackendStep step = steps.get(i);
            sb.append("  {\n");
            sb.append("    \"stage\": \"").append(json(step.stage())).append("\",\n");
            sb.append("    \"inputs\": ").append(jsonArray(step.inputs())).append(",\n");
            sb.append("    \"outputs\": ").append(jsonArray(step.outputs())).append(",\n");
            sb.append("    \"command\": \"").append(json(step.command())).append("\",\n");
            sb.append("    \"exitCode\": ").append(step.exitCode() == null ? "null" : step.exitCode()).append(",\n");
            sb.append("    \"success\": ").append(step.success()).append(",\n");
            sb.append("    \"skippedReason\": ")
                    .append(step.skippedReason() == null ? "null" : "\"" + json(step.skippedReason()) + "\"").append(",\n");
            sb.append("    \"durationMs\": ").append(step.durationMs()).append("\n");
            sb.append("  }");
            if (i + 1 < steps.size()) {
                sb.append(",");
            }
            sb.append("\n");
        }
        sb.append("]\n");
        Files.writeString(traceFile, sb.toString());
    }

    private static String displayCommand(List<String> command) {
        List<String> display = new ArrayList<>(command);
        for (int i = 0; i < display.size(); i++) {
            String value = display.get(i);
            if (value.endsWith("/output.ll")) {
                display.set(i, "../04-ir/output.ll");
            } else if (value.endsWith("/output.o")) {
                display.set(i, "output.o");
            } else if (value.endsWith("/validate.o")) {
                display.set(i, "validate.o");
            } else if (value.endsWith("/output.s")) {
                display.set(i, "output.s");
            } else if (value.endsWith("/native-executable")) {
                display.set(i, "./native-executable");
            }
        }
        return String.join(" ", display);
    }

    private static String jsonArray(List<String> values) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append("\"").append(json(values.get(i))).append("\"");
        }
        return sb.append("]").toString();
    }

    private static String json(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }
}
