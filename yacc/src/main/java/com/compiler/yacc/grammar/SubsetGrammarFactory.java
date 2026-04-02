package com.compiler.yacc.grammar;

public class SubsetGrammarFactory {

    public Grammar build() {
        Grammar g = new Grammar();

        Terminal INT = g.terminal("INT");
        Terminal MAIN = g.terminal("MAIN");
        Terminal IF = g.terminal("IF");
        Terminal ELSE = g.terminal("ELSE");
        Terminal WHILE = g.terminal("WHILE");
        Terminal RETURN = g.terminal("RETURN");
        Terminal ID = g.terminal("ID");
        Terminal NUM = g.terminal("NUM");

        Terminal PLUS = g.terminal("PLUS");
        Terminal MINUS = g.terminal("MINUS");
        Terminal STAR = g.terminal("STAR");
        Terminal SLASH = g.terminal("SLASH");

        Terminal LT = g.terminal("LT");
        Terminal GT = g.terminal("GT");
        Terminal LE = g.terminal("LE");
        Terminal GE = g.terminal("GE");
        Terminal EQ = g.terminal("EQ");
        Terminal NE = g.terminal("NE");

        Terminal ASSIGN = g.terminal("ASSIGN");
        Terminal SEMI = g.terminal("SEMI");
        Terminal LPAREN = g.terminal("LPAREN");
        Terminal RPAREN = g.terminal("RPAREN");
        Terminal LBRACE = g.terminal("LBRACE");
        Terminal RBRACE = g.terminal("RBRACE");
        Terminal EOF = g.terminal("EOF");

        NonTerminal S_ = g.nonTerminal("S'");
        NonTerminal Program = g.nonTerminal("Program");
        NonTerminal MainFunc = g.nonTerminal("MainFunc");
        NonTerminal Block = g.nonTerminal("Block");
        NonTerminal ItemList = g.nonTerminal("ItemList");
        NonTerminal Item = g.nonTerminal("Item");
        NonTerminal Decl = g.nonTerminal("Decl");
        NonTerminal DeclInitOpt = g.nonTerminal("DeclInitOpt");
        NonTerminal Stmt = g.nonTerminal("Stmt");
        NonTerminal MatchedStmt = g.nonTerminal("MatchedStmt");
        NonTerminal UnmatchedStmt = g.nonTerminal("UnmatchedStmt");
        NonTerminal AssignStmt = g.nonTerminal("AssignStmt");
        NonTerminal ReturnStmt = g.nonTerminal("ReturnStmt");
        NonTerminal Cond = g.nonTerminal("Cond");
        NonTerminal RelOp = g.nonTerminal("RelOp");
        NonTerminal Expr = g.nonTerminal("Expr");
        NonTerminal Term = g.nonTerminal("Term");
        NonTerminal Factor = g.nonTerminal("Factor");

        g.setStartSymbol(Program);
        g.setAugmentedStartSymbol(S_);
        g.setEof(EOF);

        g.addProduction(S_, Program);

        g.addProduction(Program, MainFunc);
        g.addProduction(MainFunc, INT, MAIN, LPAREN, RPAREN, Block);

        g.addProduction(Block, LBRACE, ItemList, RBRACE);
        g.addProduction(ItemList, ItemList, Item);
        g.addEpsilonProduction(ItemList);

        g.addProduction(Item, Decl);
        g.addProduction(Item, Stmt);

        g.addProduction(Decl, INT, ID, DeclInitOpt, SEMI);
        g.addProduction(DeclInitOpt, ASSIGN, Expr);
        g.addEpsilonProduction(DeclInitOpt);

        g.addProduction(Stmt, MatchedStmt);
        g.addProduction(Stmt, UnmatchedStmt);

        g.addProduction(MatchedStmt, AssignStmt);
        g.addProduction(MatchedStmt, ReturnStmt);
        g.addProduction(MatchedStmt, Block);
        g.addProduction(MatchedStmt, WHILE, LPAREN, Cond, RPAREN, MatchedStmt);
        g.addProduction(MatchedStmt, IF, LPAREN, Cond, RPAREN, MatchedStmt, ELSE, MatchedStmt);

        g.addProduction(UnmatchedStmt, IF, LPAREN, Cond, RPAREN, Stmt);
        g.addProduction(UnmatchedStmt, IF, LPAREN, Cond, RPAREN, MatchedStmt, ELSE, UnmatchedStmt);
        g.addProduction(UnmatchedStmt, WHILE, LPAREN, Cond, RPAREN, UnmatchedStmt);

        g.addProduction(AssignStmt, ID, ASSIGN, Expr, SEMI);
        g.addProduction(ReturnStmt, RETURN, Expr, SEMI);

        g.addProduction(Cond, Expr, RelOp, Expr);

        g.addProduction(RelOp, LT);
        g.addProduction(RelOp, GT);
        g.addProduction(RelOp, LE);
        g.addProduction(RelOp, GE);
        g.addProduction(RelOp, EQ);
        g.addProduction(RelOp, NE);

        g.addProduction(Expr, Expr, PLUS, Term);
        g.addProduction(Expr, Expr, MINUS, Term);
        g.addProduction(Expr, Term);

        g.addProduction(Term, Term, STAR, Factor);
        g.addProduction(Term, Term, SLASH, Factor);
        g.addProduction(Term, Factor);

        g.addProduction(Factor, LPAREN, Expr, RPAREN);
        g.addProduction(Factor, ID);
        g.addProduction(Factor, NUM);

        return g;
    }
}
