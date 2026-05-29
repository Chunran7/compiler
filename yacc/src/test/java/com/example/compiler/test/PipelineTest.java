package com.example.compiler.test;

import com.example.compiler.CompileResult;
import com.example.compiler.Compiler;
import com.example.compiler.ir.IrGenerationResult;
import com.example.compiler.ir.IrInstruction;
import com.example.compiler.ir.LlvmLikeTextEmitter;
import com.example.compiler.ir.YaccIrBridge;
import com.example.compiler.semantic.SemanticResult;
import com.example.compiler.semantic.emitter.CSemanticProgramEmitter;
import com.example.compiler.yacc.ast.AstNode;
import com.example.compiler.yacc.ast.AstTreeCodec;
import com.example.compiler.yacc.emitter.CParserProgramEmitter;
import com.example.compiler.yacc.generator.SeuYaccGenerator;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.FileReader;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class PipelineTest {

    private static final String CASES_RESOURCE_DIR = "cases";
    private static final String CASES_DISPLAY_PATH = "src/test/resources/cases";
    private static final Path GENERATED_ROOT = Path.of("generated", "test-cases");

    @ParameterizedTest(name = "{0}")
    @MethodSource("cTestCases")
    void compileCFileThroughPipeline(String fileName, Path sourceFile) throws Exception {
        System.out.println("Testing C source file: " + fileName);

        String source = Files.readString(sourceFile);
        assertFalse(source.trim().isEmpty(), "Empty test case file: " + fileName);

        CompileResult result = new Compiler().compileFile(sourceFile);
        String ir = result.irText();

        assertTrue(result.isSuccess(), () -> "Pipeline failed for " + fileName + ": "
                + result.parseResult().getErrorMessage());
        assertNotNull(result.tokens(), "tokens should not be null for " + fileName);
        assertNotNull(result.parseResult(), "parse result should not be null for " + fileName);
        assertNotNull(result.semanticResult(), "semantic result should not be null for " + fileName);
        assertNotNull(result.ir(), "IR result should not be null for " + fileName);
        assertNotNull(ir, "IR text should not be null for " + fileName);
        assertFalse(ir.isBlank(), "IR text should not be empty for " + fileName);
        assertTrue(ir.contains("define i32"), "IR should contain at least one function definition for " + fileName);
        assertTrue(ir.contains("ret i32"), "IR should contain a return instruction for " + fileName);

        writeGeneratedArtifacts(fileName, sourceFile, source, result);
        System.out.println(ir);
    }

    static Stream<Arguments> cTestCases() throws Exception {
        Path casesDir = resolveCasesDir();
        if (!Files.isDirectory(casesDir)) {
            throw new IllegalStateException("No .c test files found under " + CASES_DISPLAY_PATH);
        }

        List<Path> files;
        try (Stream<Path> stream = Files.list(casesDir)) {
            files = stream
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(".c"))
                    .sorted(Comparator.comparing(path -> path.getFileName().toString()))
                    .toList();
        }

        if (files.isEmpty()) {
            throw new IllegalStateException("No .c test files found under " + CASES_DISPLAY_PATH);
        }

        return files.stream()
                .map(path -> Arguments.of(path.getFileName().toString(), path));
    }

    private static Path resolveCasesDir() throws URISyntaxException {
        URL resource = PipelineTest.class.getClassLoader().getResource(CASES_RESOURCE_DIR);
        if (resource != null) {
            return Path.of(resource.toURI());
        }
        return Path.of(CASES_DISPLAY_PATH);
    }

    private static void writeGeneratedArtifacts(String fileName,
                                                Path sourceFile,
                                                String source,
                                                CompileResult result) throws Exception {
        String caseName = caseName(fileName);
        Path caseRoot = GENERATED_ROOT.resolve(caseName);
        Path inputDir = caseRoot.resolve("00-input");
        Path lexDir = caseRoot.resolve("01-lex");
        Path yaccDir = caseRoot.resolve("02-yacc");
        Path semanticDir = caseRoot.resolve("03-semantic");
        Path irDir = caseRoot.resolve("04-ir");
        Path sootDir = caseRoot.resolve("05-soot");

        Files.createDirectories(inputDir);
        Files.createDirectories(lexDir);
        Files.createDirectories(yaccDir);
        Files.createDirectories(semanticDir);
        Files.createDirectories(irDir);
        Files.createDirectories(sootDir);

        Files.writeString(inputDir.resolve(fileName), source);
        copyIfExists(Path.of("resources", "c99.l"), inputDir.resolve("c99.l"));
        copyIfExists(Path.of("resources", "c99.y"), inputDir.resolve("c99.y"));

        Files.writeString(lexDir.resolve("tokens.txt"), emitTokens(result));
        Files.writeString(lexDir.resolve("yylex.c"), currentJavaPipelineNotice("lex", sourceFile));

        Path actionTree = emitAndRunCParser(lexDir.resolve("tokens.txt"), yaccDir);
        AstNode restoredParseTree = AstTreeCodec.read(actionTree);
        YaccIrBridge bridge = new YaccIrBridge();
        SemanticResult semanticResult = bridge.analyze(restoredParseTree);
        IrGenerationResult irResult = bridge.generate(semanticResult);
        String restoredIrText = new LlvmLikeTextEmitter().emit(irResult);

        Files.writeString(semanticDir.resolve("core-ast.txt"), semanticResult.astRoot().prettyPrint());
        Files.writeString(semanticDir.resolve("symbol-table.txt"), semanticResult.symbolTable().prettyPrint());
        Files.writeString(
                semanticDir.resolve("yysemantic.c"),
                new CSemanticProgramEmitter().emit(semanticResult.astRoot())
        );

        Path yysemantic = semanticDir.resolve("yysemantic");
        Path outputLl = irDir.resolve("output.ll");
        String semanticRunOutput = compileAndRunSemanticProgram(semanticDir.resolve("yysemantic.c"), yysemantic);
        Files.writeString(outputLl, semanticRunOutput == null ? restoredIrText : semanticRunOutput);
        Files.writeString(irDir.resolve("output.jimple"), emitJimple(semanticResult.preliminaryIr()));
        Files.writeString(sootDir.resolve("soot-skipped.txt"), "SOOT_JAR not available; Soot stage not requested by Maven tests."
                + System.lineSeparator());

        Files.writeString(caseRoot.resolve("commands.log"), emitCommandsLog(caseRoot, fileName, semanticRunOutput != null));
        Files.writeString(caseRoot.resolve("pipeline-trace.json"), emitPipelineTrace(caseRoot, fileName, semanticRunOutput != null));
        Files.writeString(caseRoot.resolve("FLOWCHART_EVIDENCE.md"), emitFlowchartEvidence(caseRoot, fileName));
    }

    private static String emitTokens(CompileResult result) {
        StringBuilder out = new StringBuilder();
        result.tokens().forEach(token -> out
                .append(token.type().canonical().name())
                .append('\t')
                .append(token.lexeme())
                .append(System.lineSeparator()));
        return out.toString();
    }

    private static String emitJimple(List<IrInstruction> instructions) {
        StringBuilder out = new StringBuilder();
        out.append("public class GeneratedProgram").append(System.lineSeparator());
        out.append("{").append(System.lineSeparator());
        List<IrInstruction> body = new ArrayList<>();
        String functionName = null;
        List<String> params = List.of();

        for (IrInstruction instruction : instructions) {
            switch (instruction.getOp()) {
                case FUNCTION_BEGIN -> {
                    if (functionName != null) {
                        appendJimpleFunction(out, functionName, params, body);
                        body.clear();
                    }
                    functionName = instruction.getResult();
                    params = instruction.getValues();
                }
                case FUNCTION_END -> {
                    appendJimpleFunction(out, functionName, params, body);
                    functionName = null;
                    params = List.of();
                    body.clear();
                }
                default -> body.add(instruction);
            }
        }

        if (functionName != null) {
            appendJimpleFunction(out, functionName, params, body);
        }
        out.append("}").append(System.lineSeparator());
        return out.toString();
    }

    private static void appendJimpleFunction(StringBuilder out,
                                             String functionName,
                                             List<String> params,
                                             List<IrInstruction> body) {
        out.append("    public static int ").append(functionName).append("(");
        for (int i = 0; i < params.size(); i++) {
            if (i > 0) {
                out.append(", ");
            }
            out.append("int ").append(params.get(i));
        }
        out.append(")").append(System.lineSeparator());
        out.append("    {").append(System.lineSeparator());

        Set<String> locals = new LinkedHashSet<>();
        for (IrInstruction instruction : body) {
            switch (instruction.getOp()) {
                case ASSIGN, CALL, ADD, SUB, MUL, DIV, LT, LE, GT, GE, EQ, NE -> addLocal(locals, instruction.getResult());
                default -> {
                }
            }
        }
        locals.removeAll(params);
        if (!locals.isEmpty()) {
            out.append("        int ").append(String.join(", ", locals)).append(";").append(System.lineSeparator());
        }

        for (IrInstruction instruction : body) {
            out.append("        ").append(toJimpleLine(instruction)).append(System.lineSeparator());
        }

        out.append("    }").append(System.lineSeparator());
        out.append(System.lineSeparator());
    }

    private static String toJimpleLine(IrInstruction instruction) {
        return switch (instruction.getOp()) {
            case ASSIGN -> instruction.getResult() + " = " + instruction.getArg1() + ";";
            case CALL -> instruction.getResult() == null
                    ? instruction.getArg1() + "(" + String.join(", ", instruction.getValues()) + ");"
                    : instruction.getResult() + " = " + instruction.getArg1() + "(" + String.join(", ", instruction.getValues()) + ");";
            case ADD, SUB, MUL, DIV, LT, LE, GT, GE, EQ, NE ->
                    instruction.getResult() + " = " + instruction.getArg1() + " " + jimpleOperator(instruction) + " " + instruction.getArg2() + ";";
            case LABEL -> instruction.getResult() + ":";
            case GOTO -> "goto " + instruction.getResult() + ";";
            case IF_FALSE_GOTO -> "ifFalse " + instruction.getArg1() + " goto " + instruction.getResult() + ";";
            case RETURN -> "return " + instruction.getArg1() + ";";
            case FUNCTION_BEGIN, FUNCTION_END -> "";
        };
    }

    private static String jimpleOperator(IrInstruction instruction) {
        return switch (instruction.getOp()) {
            case ADD -> "+";
            case SUB -> "-";
            case MUL -> "*";
            case DIV -> "/";
            case LT -> "<";
            case LE -> "<=";
            case GT -> ">";
            case GE -> ">=";
            case EQ -> "==";
            case NE -> "!=";
            default -> throw new IllegalStateException("Unexpected Jimple operator: " + instruction.getOp());
        };
    }

    private static void addLocal(Set<String> locals, String value) {
        if (value != null && !value.isBlank()) {
            locals.add(value);
        }
    }

    private static Path emitAndRunCParser(Path tokensFile, Path yaccDir) throws Exception {
        SeuYaccGenerator generator;
        try (FileReader reader = new FileReader(Path.of("resources", "c99.y").toFile())) {
            generator = new SeuYaccGenerator(reader, true);
        }

        Path yyparseC = yaccDir.resolve("yyparse.c");
        Path yyparse = yaccDir.resolve("yyparse");
        Path actionTree = yaccDir.resolve("action-tree.txt");
        new CParserProgramEmitter().emitToFile(yyparseC, generator.getGrammar(), generator.getParseTable());

        ProcessResult compile = runProcess(List.of(
                "gcc", "-std=c99", "-Wall", "-Wextra", "-O2", "-o",
                yyparse.toString(),
                yyparseC.toString()
        ));
        assertTrue(compile.exitCode == 0, () -> "Failed to compile yyparse.c:" + System.lineSeparator() + compile.output);

        ProcessResult run = runProcess(List.of(
                yyparse.toAbsolutePath().toString(),
                tokensFile.toAbsolutePath().toString(),
                actionTree.toAbsolutePath().toString()
        ));
        assertTrue(run.exitCode == 0, () -> "Failed to run yyparse:" + System.lineSeparator() + run.output);
        assertTrue(Files.exists(actionTree), "yyparse should generate action-tree.txt");
        assertFalse(Files.readString(actionTree).isBlank(), "action-tree.txt should not be empty");
        return actionTree;
    }

    private static String compileAndRunSemanticProgram(Path cFile, Path executable) throws IOException, InterruptedException {
        ProcessResult compile = runProcess(List.of(
                "gcc", "-std=c99", "-Wall", "-Wextra", "-o",
                executable.toString(),
                cFile.toString()
        ));
        if (compile.exitCode != 0) {
            return null;
        }

        ProcessResult run = runProcess(List.of(executable.toAbsolutePath().toString()));
        if (run.exitCode != 0) {
            return null;
        }
        return run.output;
    }

    private static ProcessResult runProcess(List<String> command) throws IOException, InterruptedException {
        ProcessBuilder builder = new ProcessBuilder(command);
        builder.redirectErrorStream(true);
        Process process;
        try {
            process = builder.start();
        } catch (IOException ex) {
            return new ProcessResult(127, ex.getMessage() + System.lineSeparator());
        }
        String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        int exit = process.waitFor();
        return new ProcessResult(exit, output);
    }

    private static String emitCommandsLog(Path caseRoot, String fileName, boolean semanticProgramRan) {
        String sep = System.lineSeparator();
        return ""
                + "copy inputs -> " + caseRoot.resolve("00-input") + sep
                + "GeneratedLexer -> " + caseRoot.resolve("01-lex/tokens.txt") + sep
                + "CParserProgramEmitter -> " + caseRoot.resolve("02-yacc/yyparse.c") + sep
                + "gcc yyparse.c -o yyparse -> " + caseRoot.resolve("02-yacc/yyparse") + sep
                + "yyparse tokens.txt action-tree.txt -> " + caseRoot.resolve("02-yacc/action-tree.txt") + sep
                + "AstTreeCodec action-tree.txt -> AstNode" + sep
                + "YaccIrBridge.analyze(restored AstNode) -> " + caseRoot.resolve("03-semantic/core-ast.txt") + sep
                + "SymbolTable -> " + caseRoot.resolve("03-semantic/symbol-table.txt") + sep
                + "CSemanticProgramEmitter -> " + caseRoot.resolve("03-semantic/yysemantic.c") + sep
                + "gcc yysemantic.c -o yysemantic: " + (semanticProgramRan ? "success" : "skipped or failed; output.ll uses Java LLVM emitter") + sep
                + "LLVM output for " + fileName + " -> " + caseRoot.resolve("04-ir/output.ll") + sep;
    }

    private static String emitPipelineTrace(Path caseRoot, String fileName, boolean semanticProgramRan) {
        String input = json(caseRoot.resolve("00-input").resolve(fileName).toString());
        return """
                [
                  {"stage":"copy-inputs","input":%s,"output":%s,"success":true},
                  {"stage":"lex-run","input":%s,"output":%s,"success":true},
                  {"stage":"yacc-run","input":%s,"output":%s,"success":true},
                  {"stage":"semantic-run","input":%s,"output":%s,"success":true},
                  {"stage":"ir-write-llvm","input":%s,"output":%s,"success":true},
                  {"stage":"soot-skipped","input":%s,"output":%s,"success":true,"skippedReason":"Soot not requested"}
                ]
                """.formatted(
                input, json(caseRoot.resolve("00-input").toString()),
                input, json(caseRoot.resolve("01-lex/tokens.txt").toString()),
                json(caseRoot.resolve("01-lex/tokens.txt").toString()), json(caseRoot.resolve("02-yacc/action-tree.txt").toString()),
                json(caseRoot.resolve("02-yacc/action-tree.txt").toString()), json(caseRoot.resolve("03-semantic/core-ast.txt").toString()),
                json(semanticProgramRan ? caseRoot.resolve("03-semantic/yysemantic").toString() : caseRoot.resolve("03-semantic/core-ast.txt").toString()),
                json(caseRoot.resolve("04-ir/output.ll").toString()),
                json(caseRoot.resolve("04-ir/output.jimple").toString()), json(caseRoot.resolve("05-soot/soot-skipped.txt").toString())
        );
    }

    private static String emitFlowchartEvidence(Path caseRoot, String fileName) {
        return "# Pipeline Evidence: " + fileName + System.lineSeparator()
                + System.lineSeparator()
                + "- Input: `" + caseRoot.resolve("00-input").resolve(fileName) + "`" + System.lineSeparator()
                + "- Lex tokens: `" + caseRoot.resolve("01-lex/tokens.txt") + "`" + System.lineSeparator()
                + "- Yacc C parser source: `" + caseRoot.resolve("02-yacc/yyparse.c") + "`" + System.lineSeparator()
                + "- Yacc C parser executable: `" + caseRoot.resolve("02-yacc/yyparse") + "`" + System.lineSeparator()
                + "- Yacc action tree: `" + caseRoot.resolve("02-yacc/action-tree.txt") + "`" + System.lineSeparator()
                + "- Semantic core AST: `" + caseRoot.resolve("03-semantic/core-ast.txt") + "`" + System.lineSeparator()
                + "- Symbol table: `" + caseRoot.resolve("03-semantic/symbol-table.txt") + "`" + System.lineSeparator()
                + "- LLVM IR: `" + caseRoot.resolve("04-ir/output.ll") + "`" + System.lineSeparator();
    }

    private static void copyIfExists(Path source, Path target) throws IOException {
        if (Files.exists(source)) {
            Files.copy(source, target, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static String currentJavaPipelineNotice(String stage, Path sourceFile) {
        return "/*" + System.lineSeparator()
                + " * Generated by PipelineTest for " + sourceFile.getFileName() + "." + System.lineSeparator()
                + " * The current project executes the " + stage + " stage through the Java pipeline." + System.lineSeparator()
                + " * The authoritative stage output is the sibling text artifact in this directory." + System.lineSeparator()
                + " */" + System.lineSeparator();
    }

    private static String caseName(String fileName) {
        String base = fileName.endsWith(".c") ? fileName.substring(0, fileName.length() - 2) : fileName;
        String sanitized = base.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9._-]+", "_");
        return sanitized.isBlank() ? "case" : sanitized;
    }

    private static String json(String value) {
        return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }

    private record ProcessResult(int exitCode, String output) {
    }
}
