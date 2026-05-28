package com.example.compiler;

import com.example.compiler.backend.nativebackend.NativeBackend;
import com.example.compiler.backend.nativebackend.NativeBackendConfig;
import com.example.compiler.backend.nativebackend.NativeBackendResult;

import java.nio.file.Files;
import java.nio.file.Path;

public final class CompilerPipeline {
    private CompilerPipeline() {
    }

    public static void main(String[] args) throws Exception {
        Path lexFile = Path.of("resources", "c99.l");
        Path yaccFile = Path.of("resources", "c99.y");
        Path sourceFile = null;
        Path outputDir = Path.of("generated", "pipeline");
        Path executableFile = null;
        Path bytecodeDir = null;
        boolean nativeBackend = false;
        boolean runNative = false;
        Path nativeOut = null;
        String clangCommand = "clang";
        String nativeOpt = "O0";

        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--lex" -> lexFile = Path.of(requireValue(args, ++i, "--lex"));
                case "--yacc" -> yaccFile = Path.of(requireValue(args, ++i, "--yacc"));
                case "--source" -> sourceFile = Path.of(requireValue(args, ++i, "--source"));
                case "--out" -> outputDir = Path.of(requireValue(args, ++i, "--out"));
                case "--emit-exe" -> executableFile = Path.of(requireValue(args, ++i, "--emit-exe"));
                case "--emit-class" -> bytecodeDir = Path.of(requireValue(args, ++i, "--emit-class"));
                case "--native-backend" -> nativeBackend = true;
                case "--run-native" -> {
                    nativeBackend = true;
                    runNative = true;
                }
                case "--native-out" -> nativeOut = Path.of(requireValue(args, ++i, "--native-out"));
                case "--clang" -> clangCommand = requireValue(args, ++i, "--clang");
                case "--native-opt" -> nativeOpt = requireValue(args, ++i, "--native-opt");
                default -> throw new IllegalArgumentException("Unknown option: " + args[i]);
            }
        }

        if (sourceFile == null) {
            throw new IllegalArgumentException("missing required option --source <test.c>");
        }

        Files.createDirectories(outputDir);
        Compiler compiler = new Compiler();
        CompileResult result = compiler.compileStrictFlowchart(
                lexFile,
                yaccFile,
                sourceFile,
                outputDir,
                null,
                nativeBackend ? null : executableFile,
                null,
                bytecodeDir
        );

        if (!result.isSuccess()) {
            throw new IllegalStateException("pipeline failed");
        }

        NativeBackendResult nativeResult = null;
        if (nativeBackend) {
            Path actualNativeOut = nativeOut == null ? outputDir.resolve("06-native") : nativeOut;
            NativeBackendConfig nativeConfig = NativeBackendConfig.defaults(result.llvmIrFile(), actualNativeOut)
                    .withClangCommand(clangCommand)
                    .withOptimizationLevel(nativeOpt)
                    .withRunExecutable(runNative);
            nativeResult = new NativeBackend().run(nativeConfig);
            result.nativeBackendResult = nativeResult;
        }

        System.out.println("Pipeline completed:");
        System.out.println("  yylex.c: " + result.generatedLexerC());
        System.out.println("  yylex: " + result.generatedLexerC().getParent().resolve("yylex"));
        System.out.println("  yyparse.c: " + result.generatedParserC());
        System.out.println("  yyparse: " + result.generatedParserC().getParent().resolve("yyparse"));
        System.out.println("  tokens.txt: " + result.tokenFile());
        System.out.println("  action-tree.txt: " + result.parseTreeFile());
        System.out.println("  core-ast.txt: " + result.coreAstFile());
        System.out.println("  symbol-table.txt: " + result.symbolTableFile());
        System.out.println("  yysemantic.c: " + result.generatedSemanticC());
        System.out.println("  yysemantic: " + result.generatedSemanticC().getParent().resolve("yysemantic"));
        System.out.println("  output.ll: " + result.llvmIrFile());
        System.out.println("  output.jimple: " + result.jimpleFile());
        System.out.println("  commands.log: " + result.commandsLogFile());
        System.out.println("  pipeline-trace.json: " + result.pipelineTraceFile());
        System.out.println("  FLOWCHART_EVIDENCE.md: " + result.evidenceFile());
        if (!nativeBackend && result.executableFile() == null) {
            System.out.println("  native executable: skipped (clang not requested)");
        } else if (result.executableFile() != null) {
            System.out.println("  native executable: " + result.executableFile());
        }
        if (bytecodeDir != null && result.bytecodeOutput() == null) {
            System.out.println("  bytecode: skipped (SOOT_JAR not available)");
        } else if (result.bytecodeOutput() != null) {
            System.out.println("  bytecode: " + result.bytecodeOutput());
        }
        if (nativeResult != null) {
            System.out.println("  native backend trace: " + nativeResult.traceFile());
            System.out.println("  native backend report: " + nativeResult.reportFile());
            if (nativeResult.skipped()) {
                System.out.println("  native backend: skipped (" + nativeResult.skippedReason() + ")");
            } else {
                System.out.println("  output.s: " + nativeResult.assemblyFile());
                System.out.println("  output.o: " + nativeResult.objectFile());
                System.out.println("  native-executable: " + nativeResult.executableFile());
                if (nativeResult.runExitCode() != null) {
                    System.out.println("  native run exitCode: " + nativeResult.runExitCode());
                }
            }
        }
    }

    private static String requireValue(String[] args, int index, String option) {
        if (index >= args.length) {
            throw new IllegalArgumentException("missing value for " + option);
        }
        return args[index];
    }
}
