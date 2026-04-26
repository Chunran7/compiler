package com.example.compiler.semantic.emitter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

public final class SemanticProgramEmitter {
    public String emit(String className) {
        Objects.requireNonNull(className, "className");

        StringBuilder sb = new StringBuilder();

        sb.append("import com.example.compiler.ir.IrGenerationResult;\n");
        sb.append("import com.example.compiler.ir.IrGenerator;\n");
        sb.append("import com.example.compiler.ir.LlvmLikeTextEmitter;\n");
        sb.append("import com.example.compiler.semantic.SemanticActionEngine;\n");
        sb.append("import com.example.compiler.semantic.SemanticResult;\n");
        sb.append("import com.example.compiler.yacc.generator.SeuYaccGenerator;\n");
        sb.append("import com.example.compiler.yacc.runtime.ParseResult;\n");
        sb.append("import com.example.compiler.yacc.runtime.ParserDriver;\n");
        sb.append("import com.example.compiler.yacc.token.Token;\n");
        sb.append("import com.example.compiler.yacc.token.TokenType;\n");
        sb.append("\n");
        sb.append("import java.io.FileReader;\n");
        sb.append("import java.io.Reader;\n");
        sb.append("import java.nio.file.Files;\n");
        sb.append("import java.nio.file.Path;\n");
        sb.append("import java.util.ArrayList;\n");
        sb.append("import java.util.List;\n");
        sb.append("\n");

        sb.append("public final class ").append(className).append(" {\n\n");

        emitReadTokensMethod(sb);
        emitMainMethod(sb, className);

        sb.append("}\n");

        return sb.toString();
    }

    public Path emitToFile(Path outputFile, String className) throws IOException {
        Objects.requireNonNull(outputFile, "outputFile");
        String source = emit(className);
        if (outputFile.getParent() != null) {
            Files.createDirectories(outputFile.getParent());
        }
        Files.writeString(outputFile, source);
        return outputFile;
    }

    private void emitReadTokensMethod(StringBuilder sb) {
        sb.append("    public static List<Token> readTokens(Path path) throws Exception {\n");
        sb.append("        List<String> lines = Files.readAllLines(path);\n");
        sb.append("        List<Token> tokens = new ArrayList<>();\n\n");

        sb.append("        for (String raw : lines) {\n");
        sb.append("            String line = raw.trim();\n");
        sb.append("            if (line.isEmpty() || line.startsWith(\"#\")) {\n");
        sb.append("                continue;\n");
        sb.append("            }\n\n");

        sb.append("            String[] parts = line.split(\"\\\\s+\", 2);\n");
        sb.append("            TokenType type = TokenType.valueOf(parts[0]);\n");
        sb.append("            String lexeme = parts.length == 2 ? parts[1] : parts[0];\n");
        sb.append("            tokens.add(new Token(type, lexeme));\n");
        sb.append("        }\n\n");

        sb.append("        if (tokens.isEmpty() || tokens.get(tokens.size() - 1).type() != TokenType.EOF) {\n");
        sb.append("            tokens.add(new Token(TokenType.EOF, \"<EOF>\"));\n");
        sb.append("        }\n\n");

        sb.append("        return tokens;\n");
        sb.append("    }\n\n");
    }

    private void emitMainMethod(StringBuilder sb, String className) {
        sb.append("    public static void main(String[] args) throws Exception {\n");
        sb.append("        if (args.length != 2) {\n");
        sb.append("            System.err.println(\"Usage: java ").append(escape(className)).append(" <grammar-file> <token-file>\");\n");
        sb.append("            System.exit(1);\n");
        sb.append("        }\n\n");

        sb.append("        Path grammarFile = Path.of(args[0]);\n");
        sb.append("        Path tokenFile = Path.of(args[1]);\n\n");

        sb.append("        SeuYaccGenerator generator;\n");
        sb.append("        try (Reader reader = new FileReader(grammarFile.toFile())) {\n");
        sb.append("            generator = new SeuYaccGenerator(reader, true);\n");
        sb.append("        }\n\n");

        sb.append("        ParserDriver driver = new ParserDriver(generator.getGrammar(), generator.getParseTable());\n");
        sb.append("        List<Token> tokens = readTokens(tokenFile);\n");
        sb.append("        ParseResult parseResult = driver.parse(tokens);\n\n");

        sb.append("        if (!parseResult.isAccepted()) {\n");
        sb.append("            System.err.println(\"PARSE FAILED\");\n");
        sb.append("            System.err.println(parseResult.getErrorMessage());\n");
        sb.append("            System.exit(2);\n");
        sb.append("        }\n\n");

        sb.append("        SemanticActionEngine semanticEngine = new SemanticActionEngine();\n");
        sb.append("        SemanticResult semanticResult = semanticEngine.analyze(parseResult.getAstRoot());\n\n");

        sb.append("        System.out.println(\"SEMANTIC OK\");\n");
        sb.append("        System.out.println(\"=== SYMBOLS ===\");\n");
        sb.append("        System.out.print(semanticResult.symbolTable().prettyPrint());\n\n");

        sb.append("        System.out.println(\"=== PRELIMINARY TAC ===\");\n");
        sb.append("        for (var instruction : semanticResult.preliminaryIr()) {\n");
        sb.append("            System.out.println(instruction);\n");
        sb.append("        }\n\n");

        sb.append("        IrGenerator irGenerator = new IrGenerator();\n");
        sb.append("        IrGenerationResult ir = irGenerator.generate(semanticResult);\n\n");

        sb.append("        System.out.println(\"=== LLVM-LIKE IR ===\");\n");
        sb.append("        System.out.println(new LlvmLikeTextEmitter().emit(ir));\n");
        sb.append("    }\n");
    }

    private String escape(String text) {
        return text.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}