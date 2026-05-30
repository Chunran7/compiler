package com.example.compiler.yacc.generator;

import com.example.compiler.yacc.first.FirstSetCalculator;
import com.example.compiler.yacc.grammar.Grammar;
import com.example.compiler.yacc.lalr.LALRConverter;
import com.example.compiler.yacc.lr1.CanonicalCollection;
import com.example.compiler.yacc.lr1.CanonicalCollectionBuilder;
import com.example.compiler.yacc.parser.YaccParser;
import com.example.compiler.yacc.table.ParseTable;
import com.example.compiler.yacc.table.ParseTableBuilder;

import java.io.IOException;
import java.io.Reader;

/**
 * Yacc 主生成器门面。
 *
 * <p>输入是 {@code c99.y} 的 Reader；输出是三类核心产物：
 * {@link Grammar}、LR(1) 或 LALR 项目集 {@link CanonicalCollection}、
 * 以及最终语法分析表 {@link ParseTable}。它把 YaccParser、FIRST 集计算、
 * LR(1) 项目集构造、LALR 合并和 ParseTable 构造串成一条生成链。</p>
 *
 * <p>在报告中对应“语法分析程序生成器（YACC）”模块；在运行时通常被
 * {@code Compiler} 懒加载，然后交给 {@code ParserDriver} 执行移进/规约。</p>
 */
public final class SeuYaccGenerator {
    private final Grammar grammar;
    private final ParseTable parseTable;
    private final CanonicalCollection collection;

    /**
     * 构造完整的 yacc 分析数据。
     *
     * @param reader 语法规则文件输入流，通常来自 resources/c99.y
     * @param useLalr 为 true 时把规范 LR(1) 项目集合并为 LALR；为 false 时保留 LR(1)
     * @throws IOException 读取规则文件失败时抛出
     */
    public SeuYaccGenerator(Reader reader, boolean useLalr) throws IOException {
        this.grammar = YaccParser.parse(reader);

        FirstSetCalculator firstSetCalculator = new FirstSetCalculator(grammar);
        firstSetCalculator.compute();

        CanonicalCollection lr1 = new CanonicalCollectionBuilder(grammar, firstSetCalculator).build();
        this.collection = useLalr ? new LALRConverter().convert(lr1) : lr1;
        this.parseTable = new ParseTableBuilder(grammar, collection).build();
    }

    public Grammar getGrammar() {
        return grammar;
    }

    public ParseTable getParseTable() {
        return parseTable;
    }

    public CanonicalCollection getCollection() {
        return collection;
    }
}
