package com.example.compiler.semantic;

import com.example.compiler.ir.IrInstruction;
import com.example.compiler.ir.ThreeAddressIrGenerator;
import com.example.compiler.yacc.ast.AstNode;

import java.util.List;

/**
 * 语义阶段总控入口。
 *
 * <p>输入是语法分析阶段得到的 AstNode parse tree/action-tree；输出是
 * {@link SemanticResult}，其中包含 Core AST、SymbolTable 和初步三地址 IR。
 * 它先调用 {@link CompileTimeSemanticAnalyzer} 完成 Core AST 构建与语义检查，
 * 再调用 {@link ThreeAddressIrGenerator} 把动态语义（赋值、运算、调用、分支）
 * 转换为三地址中间表示。</p>
 */
public final class SemanticActionEngine {
    private final CompileTimeSemanticAnalyzer compileTimeAnalyzer = new CompileTimeSemanticAnalyzer();
    private final ThreeAddressIrGenerator irGenerator = new ThreeAddressIrGenerator();

    /**
     * 执行“编译时语义动作 + 运行时中间代码动作”。
     *
     * @param parseTreeRoot ParserDriver 接受后返回的语法树根
     * @return 包含 Core AST、符号表和三地址 IR 的语义结果
     */
    public SemanticResult analyze(AstNode parseTreeRoot) {
        SemanticResult checked = compileTimeAnalyzer.analyze(parseTreeRoot);
        List<IrInstruction> instructions = irGenerator.generate(checked.astRoot());
        return new SemanticResult(checked.astRoot(), checked.symbolTable(), instructions);
    }
}
