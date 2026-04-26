# Parse Tree With Semantic Actions

## 1. 语法树节点数据结构

本节以表格形式展示语法树节点的数据结构，包括节点编号、符号名、词素值、产生式编号、孩子节点以及是否为语义动作节点。

| 节点ID | 节点类型 | 符号名 | 词素值 | 产生式编号 | 孩子节点 | 语义动作代码预览 |
|---|---|---|---|---:|---|---|
| n0 | NON_TERMINAL | Program | - | 2 | n1, n410 | - |
| n1 | NON_TERMINAL | FuncList | - | 4 | n2, n60, n409 | - |
| n2 | NON_TERMINAL | FuncList | - | 6 | n3, n59 | - |
| n3 | NON_TERMINAL | FuncDef | - | 8 | n4, n5, n8, n9, n24, n25, n58 | - |
| n4 | TERMINAL_LEAF | INT | int | -1 | - | - |
| n5 | NON_TERMINAL | FuncName | - | 10 | n6, n7 | - |
| n6 | TERMINAL_LEAF | ID | add | -1 | - | - |
| n7 | SEMANTIC_ACTION | __ACT_5 | - | 9 | - | { $$ = $1; } |
| n8 | TERMINAL_LEAF | LPAREN | ( | -1 | - | - |
| n9 | NON_TERMINAL | ParamListOpt | - | 16 | n10, n23 | - |
| n10 | NON_TERMINAL | ParamList | - | 18 | n11, n17, n18, n22 | - |
| n11 | NON_TERMINAL | ParamList | - | 20 | n12, n16 | - |
| n12 | NON_TERMINAL | Param | - | 22 | n13, n14, n15 | - |
| n13 | TERMINAL_LEAF | INT | int | -1 | - | - |
| n14 | TERMINAL_LEAF | ID | x | -1 | - | - |
| n15 | SEMANTIC_ACTION | __ACT_11 | - | 21 | - | { $$ = makeParam($2); } |
| n16 | SEMANTIC_ACTION | __ACT_10 | - | 19 | - | { $$ = makeParamList($1); } |
| n17 | TERMINAL_LEAF | COMMA | , | -1 | - | - |
| n18 | NON_TERMINAL | Param | - | 22 | n19, n20, n21 | - |
| n19 | TERMINAL_LEAF | INT | int | -1 | - | - |
| n20 | TERMINAL_LEAF | ID | y | -1 | - | - |
| n21 | SEMANTIC_ACTION | __ACT_11 | - | 21 | - | { $$ = makeParam($2); } |
| n22 | SEMANTIC_ACTION | __ACT_9 | - | 17 | - | { $$ = appendParam($1, $3); } |
| n23 | SEMANTIC_ACTION | __ACT_8 | - | 15 | - | { $$ = $1; } |
| n24 | TERMINAL_LEAF | RPAREN | ) | -1 | - | - |
| n25 | NON_TERMINAL | Block | - | 24 | n26, n27, n56, n57 | - |
| n26 | TERMINAL_LEAF | LBRACE | { | -1 | - | - |
| n27 | NON_TERMINAL | ItemList | - | 28 | n28, n30, n55 | - |
| n28 | NON_TERMINAL | ItemList | - | 26 | n29 | - |
| n29 | SEMANTIC_ACTION | __ACT_13 | - | 25 | - | { $$ = makeEmptyItemList(); } |
| n30 | NON_TERMINAL | Item | - | 32 | n31, n54 | - |
| n31 | NON_TERMINAL | Stmt | - | 40 | n32, n53 | - |
| n32 | NON_TERMINAL | MatchedStmt | - | 48 | n33, n52 | - |
| n33 | NON_TERMINAL | ReturnStmt | - | 66 | n34, n35, n50, n51 | - |
| n34 | TERMINAL_LEAF | RETURN | return | -1 | - | - |
| n35 | NON_TERMINAL | Expr | - | 82 | n36, n43, n44, n49 | - |
| n36 | NON_TERMINAL | Expr | - | 86 | n37, n42 | - |
| n37 | NON_TERMINAL | Term | - | 92 | n38, n41 | - |
| n38 | NON_TERMINAL | Factor | - | 96 | n39, n40 | - |
| n39 | TERMINAL_LEAF | ID | x | -1 | - | - |
| n40 | SEMANTIC_ACTION | __ACT_48 | - | 95 | - | { $$ = makeIdentifier($1); } |
| n41 | SEMANTIC_ACTION | __ACT_46 | - | 91 | - | { $$ = $1; } |
| n42 | SEMANTIC_ACTION | __ACT_43 | - | 85 | - | { $$ = $1; } |
| n43 | TERMINAL_LEAF | PLUS | + | -1 | - | - |
| n44 | NON_TERMINAL | Term | - | 92 | n45, n48 | - |
| n45 | NON_TERMINAL | Factor | - | 96 | n46, n47 | - |
| n46 | TERMINAL_LEAF | ID | y | -1 | - | - |
| n47 | SEMANTIC_ACTION | __ACT_48 | - | 95 | - | { $$ = makeIdentifier($1); } |
| n48 | SEMANTIC_ACTION | __ACT_46 | - | 91 | - | { $$ = $1; } |
| n49 | SEMANTIC_ACTION | __ACT_41 | - | 81 | - | { $$ = makeBinary("+", $1, $3); } |
| n50 | TERMINAL_LEAF | SEMI | ; | -1 | - | - |
| n51 | SEMANTIC_ACTION | __ACT_33 | - | 65 | - | { $$ = makeReturn($2); } |
| n52 | SEMANTIC_ACTION | __ACT_24 | - | 47 | - | { $$ = $1; } |
| n53 | SEMANTIC_ACTION | __ACT_20 | - | 39 | - | { $$ = $1; } |
| n54 | SEMANTIC_ACTION | __ACT_16 | - | 31 | - | { $$ = $1; } |
| n55 | SEMANTIC_ACTION | __ACT_14 | - | 27 | - | { $$ = appendItem($1, $2); } |
| n56 | TERMINAL_LEAF | RBRACE | } | -1 | - | - |
| n57 | SEMANTIC_ACTION | __ACT_12 | - | 23 | - | { $$ = makeBlock($2); } |
| n58 | SEMANTIC_ACTION | __ACT_4 | - | 7 | - | { $$ = makeFunction($2, $4, $6); } |
| n59 | SEMANTIC_ACTION | __ACT_3 | - | 5 | - | { $$ = makeFunctionList($1); } |
| n60 | NON_TERMINAL | FuncDef | - | 8 | n61, n62, n65, n66, n68, n69, n408 | - |
| n61 | TERMINAL_LEAF | INT | int | -1 | - | - |
| n62 | NON_TERMINAL | FuncName | - | 12 | n63, n64 | - |
| n63 | TERMINAL_LEAF | MAIN | main | -1 | - | - |
| n64 | SEMANTIC_ACTION | __ACT_6 | - | 11 | - | { $$ = $1; } |
| n65 | TERMINAL_LEAF | LPAREN | ( | -1 | - | - |
| n66 | NON_TERMINAL | ParamListOpt | - | 14 | n67 | - |
| n67 | SEMANTIC_ACTION | __ACT_7 | - | 13 | - | { $$ = makeEmptyParamList(); } |
| n68 | TERMINAL_LEAF | RPAREN | ) | -1 | - | - |
| n69 | NON_TERMINAL | Block | - | 24 | n70, n71, n406, n407 | - |
| n70 | TERMINAL_LEAF | LBRACE | { | -1 | - | - |
| n71 | NON_TERMINAL | ItemList | - | 28 | n72, n388, n405 | - |
| n72 | NON_TERMINAL | ItemList | - | 28 | n73, n304, n387 | - |
| n73 | NON_TERMINAL | ItemList | - | 28 | n74, n266, n303 | - |
| n74 | NON_TERMINAL | ItemList | - | 28 | n75, n154, n265 | - |
| n75 | NON_TERMINAL | ItemList | - | 28 | n76, n108, n153 | - |
| n76 | NON_TERMINAL | ItemList | - | 28 | n77, n90, n107 | - |
| n77 | NON_TERMINAL | ItemList | - | 28 | n78, n80, n89 | - |
| n78 | NON_TERMINAL | ItemList | - | 26 | n79 | - |
| n79 | SEMANTIC_ACTION | __ACT_13 | - | 25 | - | { $$ = makeEmptyItemList(); } |
| n80 | NON_TERMINAL | Item | - | 30 | n81, n88 | - |
| n81 | NON_TERMINAL | Decl | - | 34 | n82, n83, n84, n86, n87 | - |
| n82 | TERMINAL_LEAF | INT | int | -1 | - | - |
| n83 | TERMINAL_LEAF | ID | a | -1 | - | - |
| n84 | NON_TERMINAL | DeclInitOpt | - | 36 | n85 | - |
| n85 | SEMANTIC_ACTION | __ACT_18 | - | 35 | - | { $$ = makeNoInitializer(); } |
| n86 | TERMINAL_LEAF | SEMI | ; | -1 | - | - |
| n87 | SEMANTIC_ACTION | __ACT_17 | - | 33 | - | { $$ = makeDeclaration($2, $3); } |
| n88 | SEMANTIC_ACTION | __ACT_15 | - | 29 | - | { $$ = $1; } |
| n89 | SEMANTIC_ACTION | __ACT_14 | - | 27 | - | { $$ = appendItem($1, $2); } |
| n90 | NON_TERMINAL | Item | - | 30 | n91, n106 | - |
| n91 | NON_TERMINAL | Decl | - | 34 | n92, n93, n94, n104, n105 | - |
| n92 | TERMINAL_LEAF | INT | int | -1 | - | - |
| n93 | TERMINAL_LEAF | ID | b | -1 | - | - |
| n94 | NON_TERMINAL | DeclInitOpt | - | 38 | n95, n96, n103 | - |
| n95 | TERMINAL_LEAF | ASSIGN | = | -1 | - | - |
| n96 | NON_TERMINAL | Expr | - | 86 | n97, n102 | - |
| n97 | NON_TERMINAL | Term | - | 92 | n98, n101 | - |
| n98 | NON_TERMINAL | Factor | - | 98 | n99, n100 | - |
| n99 | TERMINAL_LEAF | NUM | 5 | -1 | - | - |
| n100 | SEMANTIC_ACTION | __ACT_49 | - | 97 | - | { $$ = makeIntLiteral($1); } |
| n101 | SEMANTIC_ACTION | __ACT_46 | - | 91 | - | { $$ = $1; } |
| n102 | SEMANTIC_ACTION | __ACT_43 | - | 85 | - | { $$ = $1; } |
| n103 | SEMANTIC_ACTION | __ACT_19 | - | 37 | - | { $$ = makeInitializer($2); } |
| n104 | TERMINAL_LEAF | SEMI | ; | -1 | - | - |
| n105 | SEMANTIC_ACTION | __ACT_17 | - | 33 | - | { $$ = makeDeclaration($2, $3); } |
| n106 | SEMANTIC_ACTION | __ACT_15 | - | 29 | - | { $$ = $1; } |
| n107 | SEMANTIC_ACTION | __ACT_14 | - | 27 | - | { $$ = appendItem($1, $2); } |
| n108 | NON_TERMINAL | Item | - | 32 | n109, n152 | - |
| n109 | NON_TERMINAL | Stmt | - | 40 | n110, n151 | - |
| n110 | NON_TERMINAL | MatchedStmt | - | 44 | n111, n150 | - |
| n111 | NON_TERMINAL | AssignStmt | - | 62 | n112, n113, n114, n148, n149 | - |
| n112 | TERMINAL_LEAF | ID | a | -1 | - | - |
| n113 | TERMINAL_LEAF | ASSIGN | = | -1 | - | - |
| n114 | NON_TERMINAL | Expr | - | 86 | n115, n147 | - |
| n115 | NON_TERMINAL | Term | - | 92 | n116, n146 | - |
| n116 | NON_TERMINAL | Factor | - | 100 | n117, n145 | - |
| n117 | NON_TERMINAL | CallExpr | - | 102 | n118, n121, n122, n143, n144 | - |
| n118 | NON_TERMINAL | FuncName | - | 10 | n119, n120 | - |
| n119 | TERMINAL_LEAF | ID | add | -1 | - | - |
| n120 | SEMANTIC_ACTION | __ACT_5 | - | 9 | - | { $$ = $1; } |
| n121 | TERMINAL_LEAF | LPAREN | ( | -1 | - | - |
| n122 | NON_TERMINAL | ArgListOpt | - | 106 | n123, n142 | - |
| n123 | NON_TERMINAL | ArgList | - | 108 | n124, n133, n134, n141 | - |
| n124 | NON_TERMINAL | ArgList | - | 110 | n125, n132 | - |
| n125 | NON_TERMINAL | Expr | - | 86 | n126, n131 | - |
| n126 | NON_TERMINAL | Term | - | 92 | n127, n130 | - |
| n127 | NON_TERMINAL | Factor | - | 96 | n128, n129 | - |
| n128 | TERMINAL_LEAF | ID | b | -1 | - | - |
| n129 | SEMANTIC_ACTION | __ACT_48 | - | 95 | - | { $$ = makeIdentifier($1); } |
| n130 | SEMANTIC_ACTION | __ACT_46 | - | 91 | - | { $$ = $1; } |
| n131 | SEMANTIC_ACTION | __ACT_43 | - | 85 | - | { $$ = $1; } |
| n132 | SEMANTIC_ACTION | __ACT_55 | - | 109 | - | { $$ = makeArgList($1); } |
| n133 | TERMINAL_LEAF | COMMA | , | -1 | - | - |
| n134 | NON_TERMINAL | Expr | - | 86 | n135, n140 | - |
| n135 | NON_TERMINAL | Term | - | 92 | n136, n139 | - |
| n136 | NON_TERMINAL | Factor | - | 98 | n137, n138 | - |
| n137 | TERMINAL_LEAF | NUM | 3 | -1 | - | - |
| n138 | SEMANTIC_ACTION | __ACT_49 | - | 97 | - | { $$ = makeIntLiteral($1); } |
| n139 | SEMANTIC_ACTION | __ACT_46 | - | 91 | - | { $$ = $1; } |
| n140 | SEMANTIC_ACTION | __ACT_43 | - | 85 | - | { $$ = $1; } |
| n141 | SEMANTIC_ACTION | __ACT_54 | - | 107 | - | { $$ = appendArg($1, $3); } |
| n142 | SEMANTIC_ACTION | __ACT_53 | - | 105 | - | { $$ = $1; } |
| n143 | TERMINAL_LEAF | RPAREN | ) | -1 | - | - |
| n144 | SEMANTIC_ACTION | __ACT_51 | - | 101 | - | { $$ = makeCall($1, $3); } |
| n145 | SEMANTIC_ACTION | __ACT_50 | - | 99 | - | { $$ = $1; } |
| n146 | SEMANTIC_ACTION | __ACT_46 | - | 91 | - | { $$ = $1; } |
| n147 | SEMANTIC_ACTION | __ACT_43 | - | 85 | - | { $$ = $1; } |
| n148 | TERMINAL_LEAF | SEMI | ; | -1 | - | - |
| n149 | SEMANTIC_ACTION | __ACT_31 | - | 61 | - | { $$ = makeAssignment($1, $3); } |
| n150 | SEMANTIC_ACTION | __ACT_22 | - | 43 | - | { $$ = $1; } |
| n151 | SEMANTIC_ACTION | __ACT_20 | - | 39 | - | { $$ = $1; } |
| n152 | SEMANTIC_ACTION | __ACT_16 | - | 31 | - | { $$ = $1; } |
| n153 | SEMANTIC_ACTION | __ACT_14 | - | 27 | - | { $$ = appendItem($1, $2); } |
| n154 | NON_TERMINAL | Item | - | 32 | n155, n264 | - |
| n155 | NON_TERMINAL | Stmt | - | 40 | n156, n263 | - |
| n156 | NON_TERMINAL | MatchedStmt | - | 54 | n157, n158, n159, n178, n179, n220, n221, n262 | - |
| n157 | TERMINAL_LEAF | IF | if | -1 | - | - |
| n158 | TERMINAL_LEAF | LPAREN | ( | -1 | - | - |
| n159 | NON_TERMINAL | Cond | - | 68 | n160, n167, n170, n177 | - |
| n160 | NON_TERMINAL | Expr | - | 86 | n161, n166 | - |
| n161 | NON_TERMINAL | Term | - | 92 | n162, n165 | - |
| n162 | NON_TERMINAL | Factor | - | 96 | n163, n164 | - |
| n163 | TERMINAL_LEAF | ID | a | -1 | - | - |
| n164 | SEMANTIC_ACTION | __ACT_48 | - | 95 | - | { $$ = makeIdentifier($1); } |
| n165 | SEMANTIC_ACTION | __ACT_46 | - | 91 | - | { $$ = $1; } |
| n166 | SEMANTIC_ACTION | __ACT_43 | - | 85 | - | { $$ = $1; } |
| n167 | NON_TERMINAL | RelOp | - | 70 | n168, n169 | - |
| n168 | TERMINAL_LEAF | LT | < | -1 | - | - |
| n169 | SEMANTIC_ACTION | __ACT_35 | - | 69 | - | { $$ = makeRelOp($1); } |
| n170 | NON_TERMINAL | Expr | - | 86 | n171, n176 | - |
| n171 | NON_TERMINAL | Term | - | 92 | n172, n175 | - |
| n172 | NON_TERMINAL | Factor | - | 96 | n173, n174 | - |
| n173 | TERMINAL_LEAF | ID | b | -1 | - | - |
| n174 | SEMANTIC_ACTION | __ACT_48 | - | 95 | - | { $$ = makeIdentifier($1); } |
| n175 | SEMANTIC_ACTION | __ACT_46 | - | 91 | - | { $$ = $1; } |
| n176 | SEMANTIC_ACTION | __ACT_43 | - | 85 | - | { $$ = $1; } |
| n177 | SEMANTIC_ACTION | __ACT_34 | - | 67 | - | { $$ = makeCondition($1, $2, $3); } |
| n178 | TERMINAL_LEAF | RPAREN | ) | -1 | - | - |
| n179 | NON_TERMINAL | MatchedStmt | - | 44 | n180, n219 | - |
| n180 | NON_TERMINAL | AssignStmt | - | 62 | n181, n182, n183, n217, n218 | - |
| n181 | TERMINAL_LEAF | ID | a | -1 | - | - |
| n182 | TERMINAL_LEAF | ASSIGN | = | -1 | - | - |
| n183 | NON_TERMINAL | Expr | - | 86 | n184, n216 | - |
| n184 | NON_TERMINAL | Term | - | 92 | n185, n215 | - |
| n185 | NON_TERMINAL | Factor | - | 100 | n186, n214 | - |
| n186 | NON_TERMINAL | CallExpr | - | 102 | n187, n190, n191, n212, n213 | - |
| n187 | NON_TERMINAL | FuncName | - | 10 | n188, n189 | - |
| n188 | TERMINAL_LEAF | ID | add | -1 | - | - |
| n189 | SEMANTIC_ACTION | __ACT_5 | - | 9 | - | { $$ = $1; } |
| n190 | TERMINAL_LEAF | LPAREN | ( | -1 | - | - |
| n191 | NON_TERMINAL | ArgListOpt | - | 106 | n192, n211 | - |
| n192 | NON_TERMINAL | ArgList | - | 108 | n193, n202, n203, n210 | - |
| n193 | NON_TERMINAL | ArgList | - | 110 | n194, n201 | - |
| n194 | NON_TERMINAL | Expr | - | 86 | n195, n200 | - |
| n195 | NON_TERMINAL | Term | - | 92 | n196, n199 | - |
| n196 | NON_TERMINAL | Factor | - | 96 | n197, n198 | - |
| n197 | TERMINAL_LEAF | ID | a | -1 | - | - |
| n198 | SEMANTIC_ACTION | __ACT_48 | - | 95 | - | { $$ = makeIdentifier($1); } |
| n199 | SEMANTIC_ACTION | __ACT_46 | - | 91 | - | { $$ = $1; } |
| n200 | SEMANTIC_ACTION | __ACT_43 | - | 85 | - | { $$ = $1; } |
| n201 | SEMANTIC_ACTION | __ACT_55 | - | 109 | - | { $$ = makeArgList($1); } |
| n202 | TERMINAL_LEAF | COMMA | , | -1 | - | - |
| n203 | NON_TERMINAL | Expr | - | 86 | n204, n209 | - |
| n204 | NON_TERMINAL | Term | - | 92 | n205, n208 | - |
| n205 | NON_TERMINAL | Factor | - | 98 | n206, n207 | - |
| n206 | TERMINAL_LEAF | NUM | 1 | -1 | - | - |
| n207 | SEMANTIC_ACTION | __ACT_49 | - | 97 | - | { $$ = makeIntLiteral($1); } |
| n208 | SEMANTIC_ACTION | __ACT_46 | - | 91 | - | { $$ = $1; } |
| n209 | SEMANTIC_ACTION | __ACT_43 | - | 85 | - | { $$ = $1; } |
| n210 | SEMANTIC_ACTION | __ACT_54 | - | 107 | - | { $$ = appendArg($1, $3); } |
| n211 | SEMANTIC_ACTION | __ACT_53 | - | 105 | - | { $$ = $1; } |
| n212 | TERMINAL_LEAF | RPAREN | ) | -1 | - | - |
| n213 | SEMANTIC_ACTION | __ACT_51 | - | 101 | - | { $$ = makeCall($1, $3); } |
| n214 | SEMANTIC_ACTION | __ACT_50 | - | 99 | - | { $$ = $1; } |
| n215 | SEMANTIC_ACTION | __ACT_46 | - | 91 | - | { $$ = $1; } |
| n216 | SEMANTIC_ACTION | __ACT_43 | - | 85 | - | { $$ = $1; } |
| n217 | TERMINAL_LEAF | SEMI | ; | -1 | - | - |
| n218 | SEMANTIC_ACTION | __ACT_31 | - | 61 | - | { $$ = makeAssignment($1, $3); } |
| n219 | SEMANTIC_ACTION | __ACT_22 | - | 43 | - | { $$ = $1; } |
| n220 | TERMINAL_LEAF | ELSE | else | -1 | - | - |
| n221 | NON_TERMINAL | MatchedStmt | - | 44 | n222, n261 | - |
| n222 | NON_TERMINAL | AssignStmt | - | 62 | n223, n224, n225, n259, n260 | - |
| n223 | TERMINAL_LEAF | ID | a | -1 | - | - |
| n224 | TERMINAL_LEAF | ASSIGN | = | -1 | - | - |
| n225 | NON_TERMINAL | Expr | - | 86 | n226, n258 | - |
| n226 | NON_TERMINAL | Term | - | 92 | n227, n257 | - |
| n227 | NON_TERMINAL | Factor | - | 100 | n228, n256 | - |
| n228 | NON_TERMINAL | CallExpr | - | 102 | n229, n232, n233, n254, n255 | - |
| n229 | NON_TERMINAL | FuncName | - | 10 | n230, n231 | - |
| n230 | TERMINAL_LEAF | ID | add | -1 | - | - |
| n231 | SEMANTIC_ACTION | __ACT_5 | - | 9 | - | { $$ = $1; } |
| n232 | TERMINAL_LEAF | LPAREN | ( | -1 | - | - |
| n233 | NON_TERMINAL | ArgListOpt | - | 106 | n234, n253 | - |
| n234 | NON_TERMINAL | ArgList | - | 108 | n235, n244, n245, n252 | - |
| n235 | NON_TERMINAL | ArgList | - | 110 | n236, n243 | - |
| n236 | NON_TERMINAL | Expr | - | 86 | n237, n242 | - |
| n237 | NON_TERMINAL | Term | - | 92 | n238, n241 | - |
| n238 | NON_TERMINAL | Factor | - | 96 | n239, n240 | - |
| n239 | TERMINAL_LEAF | ID | a | -1 | - | - |
| n240 | SEMANTIC_ACTION | __ACT_48 | - | 95 | - | { $$ = makeIdentifier($1); } |
| n241 | SEMANTIC_ACTION | __ACT_46 | - | 91 | - | { $$ = $1; } |
| n242 | SEMANTIC_ACTION | __ACT_43 | - | 85 | - | { $$ = $1; } |
| n243 | SEMANTIC_ACTION | __ACT_55 | - | 109 | - | { $$ = makeArgList($1); } |
| n244 | TERMINAL_LEAF | COMMA | , | -1 | - | - |
| n245 | NON_TERMINAL | Expr | - | 86 | n246, n251 | - |
| n246 | NON_TERMINAL | Term | - | 92 | n247, n250 | - |
| n247 | NON_TERMINAL | Factor | - | 96 | n248, n249 | - |
| n248 | TERMINAL_LEAF | ID | b | -1 | - | - |
| n249 | SEMANTIC_ACTION | __ACT_48 | - | 95 | - | { $$ = makeIdentifier($1); } |
| n250 | SEMANTIC_ACTION | __ACT_46 | - | 91 | - | { $$ = $1; } |
| n251 | SEMANTIC_ACTION | __ACT_43 | - | 85 | - | { $$ = $1; } |
| n252 | SEMANTIC_ACTION | __ACT_54 | - | 107 | - | { $$ = appendArg($1, $3); } |
| n253 | SEMANTIC_ACTION | __ACT_53 | - | 105 | - | { $$ = $1; } |
| n254 | TERMINAL_LEAF | RPAREN | ) | -1 | - | - |
| n255 | SEMANTIC_ACTION | __ACT_51 | - | 101 | - | { $$ = makeCall($1, $3); } |
| n256 | SEMANTIC_ACTION | __ACT_50 | - | 99 | - | { $$ = $1; } |
| n257 | SEMANTIC_ACTION | __ACT_46 | - | 91 | - | { $$ = $1; } |
| n258 | SEMANTIC_ACTION | __ACT_43 | - | 85 | - | { $$ = $1; } |
| n259 | TERMINAL_LEAF | SEMI | ; | -1 | - | - |
| n260 | SEMANTIC_ACTION | __ACT_31 | - | 61 | - | { $$ = makeAssignment($1, $3); } |
| n261 | SEMANTIC_ACTION | __ACT_22 | - | 43 | - | { $$ = $1; } |
| n262 | SEMANTIC_ACTION | __ACT_27 | - | 53 | - | { $$ = makeIfElse($3, $5, $7); } |
| n263 | SEMANTIC_ACTION | __ACT_20 | - | 39 | - | { $$ = $1; } |
| n264 | SEMANTIC_ACTION | __ACT_16 | - | 31 | - | { $$ = $1; } |
| n265 | SEMANTIC_ACTION | __ACT_14 | - | 27 | - | { $$ = appendItem($1, $2); } |
| n266 | NON_TERMINAL | Item | - | 32 | n267, n302 | - |
| n267 | NON_TERMINAL | Stmt | - | 40 | n268, n301 | - |
| n268 | NON_TERMINAL | MatchedStmt | - | 46 | n269, n300 | - |
| n269 | NON_TERMINAL | ExprStmt | - | 64 | n270, n298, n299 | - |
| n270 | NON_TERMINAL | CallExpr | - | 102 | n271, n274, n275, n296, n297 | - |
| n271 | NON_TERMINAL | FuncName | - | 10 | n272, n273 | - |
| n272 | TERMINAL_LEAF | ID | add | -1 | - | - |
| n273 | SEMANTIC_ACTION | __ACT_5 | - | 9 | - | { $$ = $1; } |
| n274 | TERMINAL_LEAF | LPAREN | ( | -1 | - | - |
| n275 | NON_TERMINAL | ArgListOpt | - | 106 | n276, n295 | - |
| n276 | NON_TERMINAL | ArgList | - | 108 | n277, n286, n287, n294 | - |
| n277 | NON_TERMINAL | ArgList | - | 110 | n278, n285 | - |
| n278 | NON_TERMINAL | Expr | - | 86 | n279, n284 | - |
| n279 | NON_TERMINAL | Term | - | 92 | n280, n283 | - |
| n280 | NON_TERMINAL | Factor | - | 96 | n281, n282 | - |
| n281 | TERMINAL_LEAF | ID | a | -1 | - | - |
| n282 | SEMANTIC_ACTION | __ACT_48 | - | 95 | - | { $$ = makeIdentifier($1); } |
| n283 | SEMANTIC_ACTION | __ACT_46 | - | 91 | - | { $$ = $1; } |
| n284 | SEMANTIC_ACTION | __ACT_43 | - | 85 | - | { $$ = $1; } |
| n285 | SEMANTIC_ACTION | __ACT_55 | - | 109 | - | { $$ = makeArgList($1); } |
| n286 | TERMINAL_LEAF | COMMA | , | -1 | - | - |
| n287 | NON_TERMINAL | Expr | - | 86 | n288, n293 | - |
| n288 | NON_TERMINAL | Term | - | 92 | n289, n292 | - |
| n289 | NON_TERMINAL | Factor | - | 96 | n290, n291 | - |
| n290 | TERMINAL_LEAF | ID | b | -1 | - | - |
| n291 | SEMANTIC_ACTION | __ACT_48 | - | 95 | - | { $$ = makeIdentifier($1); } |
| n292 | SEMANTIC_ACTION | __ACT_46 | - | 91 | - | { $$ = $1; } |
| n293 | SEMANTIC_ACTION | __ACT_43 | - | 85 | - | { $$ = $1; } |
| n294 | SEMANTIC_ACTION | __ACT_54 | - | 107 | - | { $$ = appendArg($1, $3); } |
| n295 | SEMANTIC_ACTION | __ACT_53 | - | 105 | - | { $$ = $1; } |
| n296 | TERMINAL_LEAF | RPAREN | ) | -1 | - | - |
| n297 | SEMANTIC_ACTION | __ACT_51 | - | 101 | - | { $$ = makeCall($1, $3); } |
| n298 | TERMINAL_LEAF | SEMI | ; | -1 | - | - |
| n299 | SEMANTIC_ACTION | __ACT_32 | - | 63 | - | { $$ = makeExprStmt($1); } |
| n300 | SEMANTIC_ACTION | __ACT_23 | - | 45 | - | { $$ = $1; } |
| n301 | SEMANTIC_ACTION | __ACT_20 | - | 39 | - | { $$ = $1; } |
| n302 | SEMANTIC_ACTION | __ACT_16 | - | 31 | - | { $$ = $1; } |
| n303 | SEMANTIC_ACTION | __ACT_14 | - | 27 | - | { $$ = appendItem($1, $2); } |
| n304 | NON_TERMINAL | Item | - | 32 | n305, n386 | - |
| n305 | NON_TERMINAL | Stmt | - | 40 | n306, n385 | - |
| n306 | NON_TERMINAL | MatchedStmt | - | 52 | n307, n308, n309, n328, n329, n384 | - |
| n307 | TERMINAL_LEAF | WHILE | while | -1 | - | - |
| n308 | TERMINAL_LEAF | LPAREN | ( | -1 | - | - |
| n309 | NON_TERMINAL | Cond | - | 68 | n310, n317, n320, n327 | - |
| n310 | NON_TERMINAL | Expr | - | 86 | n311, n316 | - |
| n311 | NON_TERMINAL | Term | - | 92 | n312, n315 | - |
| n312 | NON_TERMINAL | Factor | - | 96 | n313, n314 | - |
| n313 | TERMINAL_LEAF | ID | a | -1 | - | - |
| n314 | SEMANTIC_ACTION | __ACT_48 | - | 95 | - | { $$ = makeIdentifier($1); } |
| n315 | SEMANTIC_ACTION | __ACT_46 | - | 91 | - | { $$ = $1; } |
| n316 | SEMANTIC_ACTION | __ACT_43 | - | 85 | - | { $$ = $1; } |
| n317 | NON_TERMINAL | RelOp | - | 80 | n318, n319 | - |
| n318 | TERMINAL_LEAF | NE | != | -1 | - | - |
| n319 | SEMANTIC_ACTION | __ACT_40 | - | 79 | - | { $$ = makeRelOp($1); } |
| n320 | NON_TERMINAL | Expr | - | 86 | n321, n326 | - |
| n321 | NON_TERMINAL | Term | - | 92 | n322, n325 | - |
| n322 | NON_TERMINAL | Factor | - | 96 | n323, n324 | - |
| n323 | TERMINAL_LEAF | ID | b | -1 | - | - |
| n324 | SEMANTIC_ACTION | __ACT_48 | - | 95 | - | { $$ = makeIdentifier($1); } |
| n325 | SEMANTIC_ACTION | __ACT_46 | - | 91 | - | { $$ = $1; } |
| n326 | SEMANTIC_ACTION | __ACT_43 | - | 85 | - | { $$ = $1; } |
| n327 | SEMANTIC_ACTION | __ACT_34 | - | 67 | - | { $$ = makeCondition($1, $2, $3); } |
| n328 | TERMINAL_LEAF | RPAREN | ) | -1 | - | - |
| n329 | NON_TERMINAL | MatchedStmt | - | 50 | n330, n383 | - |
| n330 | NON_TERMINAL | Block | - | 24 | n331, n332, n381, n382 | - |
| n331 | TERMINAL_LEAF | LBRACE | { | -1 | - | - |
| n332 | NON_TERMINAL | ItemList | - | 28 | n333, n335, n380 | - |
| n333 | NON_TERMINAL | ItemList | - | 26 | n334 | - |
| n334 | SEMANTIC_ACTION | __ACT_13 | - | 25 | - | { $$ = makeEmptyItemList(); } |
| n335 | NON_TERMINAL | Item | - | 32 | n336, n379 | - |
| n336 | NON_TERMINAL | Stmt | - | 40 | n337, n378 | - |
| n337 | NON_TERMINAL | MatchedStmt | - | 44 | n338, n377 | - |
| n338 | NON_TERMINAL | AssignStmt | - | 62 | n339, n340, n341, n375, n376 | - |
| n339 | TERMINAL_LEAF | ID | a | -1 | - | - |
| n340 | TERMINAL_LEAF | ASSIGN | = | -1 | - | - |
| n341 | NON_TERMINAL | Expr | - | 86 | n342, n374 | - |
| n342 | NON_TERMINAL | Term | - | 92 | n343, n373 | - |
| n343 | NON_TERMINAL | Factor | - | 100 | n344, n372 | - |
| n344 | NON_TERMINAL | CallExpr | - | 102 | n345, n348, n349, n370, n371 | - |
| n345 | NON_TERMINAL | FuncName | - | 10 | n346, n347 | - |
| n346 | TERMINAL_LEAF | ID | add | -1 | - | - |
| n347 | SEMANTIC_ACTION | __ACT_5 | - | 9 | - | { $$ = $1; } |
| n348 | TERMINAL_LEAF | LPAREN | ( | -1 | - | - |
| n349 | NON_TERMINAL | ArgListOpt | - | 106 | n350, n369 | - |
| n350 | NON_TERMINAL | ArgList | - | 108 | n351, n360, n361, n368 | - |
| n351 | NON_TERMINAL | ArgList | - | 110 | n352, n359 | - |
| n352 | NON_TERMINAL | Expr | - | 86 | n353, n358 | - |
| n353 | NON_TERMINAL | Term | - | 92 | n354, n357 | - |
| n354 | NON_TERMINAL | Factor | - | 96 | n355, n356 | - |
| n355 | TERMINAL_LEAF | ID | a | -1 | - | - |
| n356 | SEMANTIC_ACTION | __ACT_48 | - | 95 | - | { $$ = makeIdentifier($1); } |
| n357 | SEMANTIC_ACTION | __ACT_46 | - | 91 | - | { $$ = $1; } |
| n358 | SEMANTIC_ACTION | __ACT_43 | - | 85 | - | { $$ = $1; } |
| n359 | SEMANTIC_ACTION | __ACT_55 | - | 109 | - | { $$ = makeArgList($1); } |
| n360 | TERMINAL_LEAF | COMMA | , | -1 | - | - |
| n361 | NON_TERMINAL | Expr | - | 86 | n362, n367 | - |
| n362 | NON_TERMINAL | Term | - | 92 | n363, n366 | - |
| n363 | NON_TERMINAL | Factor | - | 98 | n364, n365 | - |
| n364 | TERMINAL_LEAF | NUM | 1 | -1 | - | - |
| n365 | SEMANTIC_ACTION | __ACT_49 | - | 97 | - | { $$ = makeIntLiteral($1); } |
| n366 | SEMANTIC_ACTION | __ACT_46 | - | 91 | - | { $$ = $1; } |
| n367 | SEMANTIC_ACTION | __ACT_43 | - | 85 | - | { $$ = $1; } |
| n368 | SEMANTIC_ACTION | __ACT_54 | - | 107 | - | { $$ = appendArg($1, $3); } |
| n369 | SEMANTIC_ACTION | __ACT_53 | - | 105 | - | { $$ = $1; } |
| n370 | TERMINAL_LEAF | RPAREN | ) | -1 | - | - |
| n371 | SEMANTIC_ACTION | __ACT_51 | - | 101 | - | { $$ = makeCall($1, $3); } |
| n372 | SEMANTIC_ACTION | __ACT_50 | - | 99 | - | { $$ = $1; } |
| n373 | SEMANTIC_ACTION | __ACT_46 | - | 91 | - | { $$ = $1; } |
| n374 | SEMANTIC_ACTION | __ACT_43 | - | 85 | - | { $$ = $1; } |
| n375 | TERMINAL_LEAF | SEMI | ; | -1 | - | - |
| n376 | SEMANTIC_ACTION | __ACT_31 | - | 61 | - | { $$ = makeAssignment($1, $3); } |
| n377 | SEMANTIC_ACTION | __ACT_22 | - | 43 | - | { $$ = $1; } |
| n378 | SEMANTIC_ACTION | __ACT_20 | - | 39 | - | { $$ = $1; } |
| n379 | SEMANTIC_ACTION | __ACT_16 | - | 31 | - | { $$ = $1; } |
| n380 | SEMANTIC_ACTION | __ACT_14 | - | 27 | - | { $$ = appendItem($1, $2); } |
| n381 | TERMINAL_LEAF | RBRACE | } | -1 | - | - |
| n382 | SEMANTIC_ACTION | __ACT_12 | - | 23 | - | { $$ = makeBlock($2); } |
| n383 | SEMANTIC_ACTION | __ACT_25 | - | 49 | - | { $$ = $1; } |
| n384 | SEMANTIC_ACTION | __ACT_26 | - | 51 | - | { $$ = makeWhile($3, $5); } |
| n385 | SEMANTIC_ACTION | __ACT_20 | - | 39 | - | { $$ = $1; } |
| n386 | SEMANTIC_ACTION | __ACT_16 | - | 31 | - | { $$ = $1; } |
| n387 | SEMANTIC_ACTION | __ACT_14 | - | 27 | - | { $$ = appendItem($1, $2); } |
| n388 | NON_TERMINAL | Item | - | 32 | n389, n404 | - |
| n389 | NON_TERMINAL | Stmt | - | 40 | n390, n403 | - |
| n390 | NON_TERMINAL | MatchedStmt | - | 48 | n391, n402 | - |
| n391 | NON_TERMINAL | ReturnStmt | - | 66 | n392, n393, n400, n401 | - |
| n392 | TERMINAL_LEAF | RETURN | return | -1 | - | - |
| n393 | NON_TERMINAL | Expr | - | 86 | n394, n399 | - |
| n394 | NON_TERMINAL | Term | - | 92 | n395, n398 | - |
| n395 | NON_TERMINAL | Factor | - | 96 | n396, n397 | - |
| n396 | TERMINAL_LEAF | ID | a | -1 | - | - |
| n397 | SEMANTIC_ACTION | __ACT_48 | - | 95 | - | { $$ = makeIdentifier($1); } |
| n398 | SEMANTIC_ACTION | __ACT_46 | - | 91 | - | { $$ = $1; } |
| n399 | SEMANTIC_ACTION | __ACT_43 | - | 85 | - | { $$ = $1; } |
| n400 | TERMINAL_LEAF | SEMI | ; | -1 | - | - |
| n401 | SEMANTIC_ACTION | __ACT_33 | - | 65 | - | { $$ = makeReturn($2); } |
| n402 | SEMANTIC_ACTION | __ACT_24 | - | 47 | - | { $$ = $1; } |
| n403 | SEMANTIC_ACTION | __ACT_20 | - | 39 | - | { $$ = $1; } |
| n404 | SEMANTIC_ACTION | __ACT_16 | - | 31 | - | { $$ = $1; } |
| n405 | SEMANTIC_ACTION | __ACT_14 | - | 27 | - | { $$ = appendItem($1, $2); } |
| n406 | TERMINAL_LEAF | RBRACE | } | -1 | - | - |
| n407 | SEMANTIC_ACTION | __ACT_12 | - | 23 | - | { $$ = makeBlock($2); } |
| n408 | SEMANTIC_ACTION | __ACT_4 | - | 7 | - | { $$ = makeFunction($2, $4, $6); } |
| n409 | SEMANTIC_ACTION | __ACT_2 | - | 3 | - | { $$ = appendFunction($1, $2); } |
| n410 | SEMANTIC_ACTION | __ACT_1 | - | 1 | - | { $$ = makeProgram($1); } |

## 2. 嵌入语义动作的文本语法树

```text
└── Program [p=2]
    ├── FuncList [p=4]
    │   ├── FuncList [p=6]
    │   │   ├── FuncDef [p=8]
    │   │   │   ├── INT("int")
    │   │   │   ├── FuncName [p=10]
    │   │   │   │   ├── ID("add")
    │   │   │   │   └── __ACT_5 [p=9] {action}
    │   │   │   ├── LPAREN("(")
    │   │   │   ├── ParamListOpt [p=16]
    │   │   │   │   ├── ParamList [p=18]
    │   │   │   │   │   ├── ParamList [p=20]
    │   │   │   │   │   │   ├── Param [p=22]
    │   │   │   │   │   │   │   ├── INT("int")
    │   │   │   │   │   │   │   ├── ID("x")
    │   │   │   │   │   │   │   └── __ACT_11 [p=21] {action}
    │   │   │   │   │   │   └── __ACT_10 [p=19] {action}
    │   │   │   │   │   ├── COMMA(",")
    │   │   │   │   │   ├── Param [p=22]
    │   │   │   │   │   │   ├── INT("int")
    │   │   │   │   │   │   ├── ID("y")
    │   │   │   │   │   │   └── __ACT_11 [p=21] {action}
    │   │   │   │   │   └── __ACT_9 [p=17] {action}
    │   │   │   │   └── __ACT_8 [p=15] {action}
    │   │   │   ├── RPAREN(")")
    │   │   │   ├── Block [p=24]
    │   │   │   │   ├── LBRACE("{")
    │   │   │   │   ├── ItemList [p=28]
    │   │   │   │   │   ├── ItemList [p=26]
    │   │   │   │   │   │   └── __ACT_13 [p=25] {action}
    │   │   │   │   │   ├── Item [p=32]
    │   │   │   │   │   │   ├── Stmt [p=40]
    │   │   │   │   │   │   │   ├── MatchedStmt [p=48]
    │   │   │   │   │   │   │   │   ├── ReturnStmt [p=66]
    │   │   │   │   │   │   │   │   │   ├── RETURN("return")
    │   │   │   │   │   │   │   │   │   ├── Expr [p=82]
    │   │   │   │   │   │   │   │   │   │   ├── Expr [p=86]
    │   │   │   │   │   │   │   │   │   │   │   ├── Term [p=92]
    │   │   │   │   │   │   │   │   │   │   │   │   ├── Factor [p=96]
    │   │   │   │   │   │   │   │   │   │   │   │   │   ├── ID("x")
    │   │   │   │   │   │   │   │   │   │   │   │   │   └── __ACT_48 [p=95] {action}
    │   │   │   │   │   │   │   │   │   │   │   │   └── __ACT_46 [p=91] {action}
    │   │   │   │   │   │   │   │   │   │   │   └── __ACT_43 [p=85] {action}
    │   │   │   │   │   │   │   │   │   │   ├── PLUS("+")
    │   │   │   │   │   │   │   │   │   │   ├── Term [p=92]
    │   │   │   │   │   │   │   │   │   │   │   ├── Factor [p=96]
    │   │   │   │   │   │   │   │   │   │   │   │   ├── ID("y")
    │   │   │   │   │   │   │   │   │   │   │   │   └── __ACT_48 [p=95] {action}
    │   │   │   │   │   │   │   │   │   │   │   └── __ACT_46 [p=91] {action}
    │   │   │   │   │   │   │   │   │   │   └── __ACT_41 [p=81] {action}
    │   │   │   │   │   │   │   │   │   ├── SEMI(";")
    │   │   │   │   │   │   │   │   │   └── __ACT_33 [p=65] {action}
    │   │   │   │   │   │   │   │   └── __ACT_24 [p=47] {action}
    │   │   │   │   │   │   │   └── __ACT_20 [p=39] {action}
    │   │   │   │   │   │   └── __ACT_16 [p=31] {action}
    │   │   │   │   │   └── __ACT_14 [p=27] {action}
    │   │   │   │   ├── RBRACE("}")
    │   │   │   │   └── __ACT_12 [p=23] {action}
    │   │   │   └── __ACT_4 [p=7] {action}
    │   │   └── __ACT_3 [p=5] {action}
    │   ├── FuncDef [p=8]
    │   │   ├── INT("int")
    │   │   ├── FuncName [p=12]
    │   │   │   ├── MAIN("main")
    │   │   │   └── __ACT_6 [p=11] {action}
    │   │   ├── LPAREN("(")
    │   │   ├── ParamListOpt [p=14]
    │   │   │   └── __ACT_7 [p=13] {action}
    │   │   ├── RPAREN(")")
    │   │   ├── Block [p=24]
    │   │   │   ├── LBRACE("{")
    │   │   │   ├── ItemList [p=28]
    │   │   │   │   ├── ItemList [p=28]
    │   │   │   │   │   ├── ItemList [p=28]
    │   │   │   │   │   │   ├── ItemList [p=28]
    │   │   │   │   │   │   │   ├── ItemList [p=28]
    │   │   │   │   │   │   │   │   ├── ItemList [p=28]
    │   │   │   │   │   │   │   │   │   ├── ItemList [p=28]
    │   │   │   │   │   │   │   │   │   │   ├── ItemList [p=26]
    │   │   │   │   │   │   │   │   │   │   │   └── __ACT_13 [p=25] {action}
    │   │   │   │   │   │   │   │   │   │   ├── Item [p=30]
    │   │   │   │   │   │   │   │   │   │   │   ├── Decl [p=34]
    │   │   │   │   │   │   │   │   │   │   │   │   ├── INT("int")
    │   │   │   │   │   │   │   │   │   │   │   │   ├── ID("a")
    │   │   │   │   │   │   │   │   │   │   │   │   ├── DeclInitOpt [p=36]
    │   │   │   │   │   │   │   │   │   │   │   │   │   └── __ACT_18 [p=35] {action}
    │   │   │   │   │   │   │   │   │   │   │   │   ├── SEMI(";")
    │   │   │   │   │   │   │   │   │   │   │   │   └── __ACT_17 [p=33] {action}
    │   │   │   │   │   │   │   │   │   │   │   └── __ACT_15 [p=29] {action}
    │   │   │   │   │   │   │   │   │   │   └── __ACT_14 [p=27] {action}
    │   │   │   │   │   │   │   │   │   ├── Item [p=30]
    │   │   │   │   │   │   │   │   │   │   ├── Decl [p=34]
    │   │   │   │   │   │   │   │   │   │   │   ├── INT("int")
    │   │   │   │   │   │   │   │   │   │   │   ├── ID("b")
    │   │   │   │   │   │   │   │   │   │   │   ├── DeclInitOpt [p=38]
    │   │   │   │   │   │   │   │   │   │   │   │   ├── ASSIGN("=")
    │   │   │   │   │   │   │   │   │   │   │   │   ├── Expr [p=86]
    │   │   │   │   │   │   │   │   │   │   │   │   │   ├── Term [p=92]
    │   │   │   │   │   │   │   │   │   │   │   │   │   │   ├── Factor [p=98]
    │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   ├── NUM("5")
    │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   └── __ACT_49 [p=97] {action}
    │   │   │   │   │   │   │   │   │   │   │   │   │   │   └── __ACT_46 [p=91] {action}
    │   │   │   │   │   │   │   │   │   │   │   │   │   └── __ACT_43 [p=85] {action}
    │   │   │   │   │   │   │   │   │   │   │   │   └── __ACT_19 [p=37] {action}
    │   │   │   │   │   │   │   │   │   │   │   ├── SEMI(";")
    │   │   │   │   │   │   │   │   │   │   │   └── __ACT_17 [p=33] {action}
    │   │   │   │   │   │   │   │   │   │   └── __ACT_15 [p=29] {action}
    │   │   │   │   │   │   │   │   │   └── __ACT_14 [p=27] {action}
    │   │   │   │   │   │   │   │   ├── Item [p=32]
    │   │   │   │   │   │   │   │   │   ├── Stmt [p=40]
    │   │   │   │   │   │   │   │   │   │   ├── MatchedStmt [p=44]
    │   │   │   │   │   │   │   │   │   │   │   ├── AssignStmt [p=62]
    │   │   │   │   │   │   │   │   │   │   │   │   ├── ID("a")
    │   │   │   │   │   │   │   │   │   │   │   │   ├── ASSIGN("=")
    │   │   │   │   │   │   │   │   │   │   │   │   ├── Expr [p=86]
    │   │   │   │   │   │   │   │   │   │   │   │   │   ├── Term [p=92]
    │   │   │   │   │   │   │   │   │   │   │   │   │   │   ├── Factor [p=100]
    │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   ├── CallExpr [p=102]
    │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   ├── FuncName [p=10]
    │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   ├── ID("add")
    │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   └── __ACT_5 [p=9] {action}
    │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   ├── LPAREN("(")
    │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   ├── ArgListOpt [p=106]
    │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   ├── ArgList [p=108]
    │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   ├── ArgList [p=110]
    │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   ├── Expr [p=86]
    │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   ├── Term [p=92]
    │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   ├── Factor [p=96]
    │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   ├── ID("b")
    │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   └── __ACT_48 [p=95] {action}
    │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   └── __ACT_46 [p=91] {action}
    │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   └── __ACT_43 [p=85] {action}
    │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   └── __ACT_55 [p=109] {action}
    │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   ├── COMMA(",")
    │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   ├── Expr [p=86]
    │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   ├── Term [p=92]
    │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   ├── Factor [p=98]
    │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   ├── NUM("3")
    │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   └── __ACT_49 [p=97] {action}
    │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   └── __ACT_46 [p=91] {action}
    │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   └── __ACT_43 [p=85] {action}
    │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   └── __ACT_54 [p=107] {action}
    │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   └── __ACT_53 [p=105] {action}
    │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   ├── RPAREN(")")
    │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   └── __ACT_51 [p=101] {action}
    │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   └── __ACT_50 [p=99] {action}
    │   │   │   │   │   │   │   │   │   │   │   │   │   │   └── __ACT_46 [p=91] {action}
    │   │   │   │   │   │   │   │   │   │   │   │   │   └── __ACT_43 [p=85] {action}
    │   │   │   │   │   │   │   │   │   │   │   │   ├── SEMI(";")
    │   │   │   │   │   │   │   │   │   │   │   │   └── __ACT_31 [p=61] {action}
    │   │   │   │   │   │   │   │   │   │   │   └── __ACT_22 [p=43] {action}
    │   │   │   │   │   │   │   │   │   │   └── __ACT_20 [p=39] {action}
    │   │   │   │   │   │   │   │   │   └── __ACT_16 [p=31] {action}
    │   │   │   │   │   │   │   │   └── __ACT_14 [p=27] {action}
    │   │   │   │   │   │   │   ├── Item [p=32]
    │   │   │   │   │   │   │   │   ├── Stmt [p=40]
    │   │   │   │   │   │   │   │   │   ├── MatchedStmt [p=54]
    │   │   │   │   │   │   │   │   │   │   ├── IF("if")
    │   │   │   │   │   │   │   │   │   │   ├── LPAREN("(")
    │   │   │   │   │   │   │   │   │   │   ├── Cond [p=68]
    │   │   │   │   │   │   │   │   │   │   │   ├── Expr [p=86]
    │   │   │   │   │   │   │   │   │   │   │   │   ├── Term [p=92]
    │   │   │   │   │   │   │   │   │   │   │   │   │   ├── Factor [p=96]
    │   │   │   │   │   │   │   │   │   │   │   │   │   │   ├── ID("a")
    │   │   │   │   │   │   │   │   │   │   │   │   │   │   └── __ACT_48 [p=95] {action}
    │   │   │   │   │   │   │   │   │   │   │   │   │   └── __ACT_46 [p=91] {action}
    │   │   │   │   │   │   │   │   │   │   │   │   └── __ACT_43 [p=85] {action}
    │   │   │   │   │   │   │   │   │   │   │   ├── RelOp [p=70]
    │   │   │   │   │   │   │   │   │   │   │   │   ├── LT("<")
    │   │   │   │   │   │   │   │   │   │   │   │   └── __ACT_35 [p=69] {action}
    │   │   │   │   │   │   │   │   │   │   │   ├── Expr [p=86]
    │   │   │   │   │   │   │   │   │   │   │   │   ├── Term [p=92]
    │   │   │   │   │   │   │   │   │   │   │   │   │   ├── Factor [p=96]
    │   │   │   │   │   │   │   │   │   │   │   │   │   │   ├── ID("b")
    │   │   │   │   │   │   │   │   │   │   │   │   │   │   └── __ACT_48 [p=95] {action}
    │   │   │   │   │   │   │   │   │   │   │   │   │   └── __ACT_46 [p=91] {action}
    │   │   │   │   │   │   │   │   │   │   │   │   └── __ACT_43 [p=85] {action}
    │   │   │   │   │   │   │   │   │   │   │   └── __ACT_34 [p=67] {action}
    │   │   │   │   │   │   │   │   │   │   ├── RPAREN(")")
    │   │   │   │   │   │   │   │   │   │   ├── MatchedStmt [p=44]
    │   │   │   │   │   │   │   │   │   │   │   ├── AssignStmt [p=62]
    │   │   │   │   │   │   │   │   │   │   │   │   ├── ID("a")
    │   │   │   │   │   │   │   │   │   │   │   │   ├── ASSIGN("=")
    │   │   │   │   │   │   │   │   │   │   │   │   ├── Expr [p=86]
    │   │   │   │   │   │   │   │   │   │   │   │   │   ├── Term [p=92]
    │   │   │   │   │   │   │   │   │   │   │   │   │   │   ├── Factor [p=100]
    │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   ├── CallExpr [p=102]
    │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   ├── FuncName [p=10]
    │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   ├── ID("add")
    │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   └── __ACT_5 [p=9] {action}
    │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   ├── LPAREN("(")
    │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   ├── ArgListOpt [p=106]
    │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   ├── ArgList [p=108]
    │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   ├── ArgList [p=110]
    │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   ├── Expr [p=86]
    │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   ├── Term [p=92]
    │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   ├── Factor [p=96]
    │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   ├── ID("a")
    │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   └── __ACT_48 [p=95] {action}
    │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   └── __ACT_46 [p=91] {action}
    │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   └── __ACT_43 [p=85] {action}
    │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   └── __ACT_55 [p=109] {action}
    │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   ├── COMMA(",")
    │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   ├── Expr [p=86]
    │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   ├── Term [p=92]
    │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   ├── Factor [p=98]
    │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   ├── NUM("1")
    │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   └── __ACT_49 [p=97] {action}
    │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   └── __ACT_46 [p=91] {action}
    │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   └── __ACT_43 [p=85] {action}
    │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   └── __ACT_54 [p=107] {action}
    │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   └── __ACT_53 [p=105] {action}
    │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   ├── RPAREN(")")
    │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   └── __ACT_51 [p=101] {action}
    │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   └── __ACT_50 [p=99] {action}
    │   │   │   │   │   │   │   │   │   │   │   │   │   │   └── __ACT_46 [p=91] {action}
    │   │   │   │   │   │   │   │   │   │   │   │   │   └── __ACT_43 [p=85] {action}
    │   │   │   │   │   │   │   │   │   │   │   │   ├── SEMI(";")
    │   │   │   │   │   │   │   │   │   │   │   │   └── __ACT_31 [p=61] {action}
    │   │   │   │   │   │   │   │   │   │   │   └── __ACT_22 [p=43] {action}
    │   │   │   │   │   │   │   │   │   │   ├── ELSE("else")
    │   │   │   │   │   │   │   │   │   │   ├── MatchedStmt [p=44]
    │   │   │   │   │   │   │   │   │   │   │   ├── AssignStmt [p=62]
    │   │   │   │   │   │   │   │   │   │   │   │   ├── ID("a")
    │   │   │   │   │   │   │   │   │   │   │   │   ├── ASSIGN("=")
    │   │   │   │   │   │   │   │   │   │   │   │   ├── Expr [p=86]
    │   │   │   │   │   │   │   │   │   │   │   │   │   ├── Term [p=92]
    │   │   │   │   │   │   │   │   │   │   │   │   │   │   ├── Factor [p=100]
    │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   ├── CallExpr [p=102]
    │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   ├── FuncName [p=10]
    │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   ├── ID("add")
    │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   └── __ACT_5 [p=9] {action}
    │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   ├── LPAREN("(")
    │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   ├── ArgListOpt [p=106]
    │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   ├── ArgList [p=108]
    │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   ├── ArgList [p=110]
    │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   ├── Expr [p=86]
    │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   ├── Term [p=92]
    │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   ├── Factor [p=96]
    │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   ├── ID("a")
    │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   └── __ACT_48 [p=95] {action}
    │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   └── __ACT_46 [p=91] {action}
    │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   └── __ACT_43 [p=85] {action}
    │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   └── __ACT_55 [p=109] {action}
    │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   ├── COMMA(",")
    │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   ├── Expr [p=86]
    │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   ├── Term [p=92]
    │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   ├── Factor [p=96]
    │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   ├── ID("b")
    │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   └── __ACT_48 [p=95] {action}
    │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   └── __ACT_46 [p=91] {action}
    │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   └── __ACT_43 [p=85] {action}
    │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   └── __ACT_54 [p=107] {action}
    │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   └── __ACT_53 [p=105] {action}
    │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   ├── RPAREN(")")
    │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   └── __ACT_51 [p=101] {action}
    │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   └── __ACT_50 [p=99] {action}
    │   │   │   │   │   │   │   │   │   │   │   │   │   │   └── __ACT_46 [p=91] {action}
    │   │   │   │   │   │   │   │   │   │   │   │   │   └── __ACT_43 [p=85] {action}
    │   │   │   │   │   │   │   │   │   │   │   │   ├── SEMI(";")
    │   │   │   │   │   │   │   │   │   │   │   │   └── __ACT_31 [p=61] {action}
    │   │   │   │   │   │   │   │   │   │   │   └── __ACT_22 [p=43] {action}
    │   │   │   │   │   │   │   │   │   │   └── __ACT_27 [p=53] {action}
    │   │   │   │   │   │   │   │   │   └── __ACT_20 [p=39] {action}
    │   │   │   │   │   │   │   │   └── __ACT_16 [p=31] {action}
    │   │   │   │   │   │   │   └── __ACT_14 [p=27] {action}
    │   │   │   │   │   │   ├── Item [p=32]
    │   │   │   │   │   │   │   ├── Stmt [p=40]
    │   │   │   │   │   │   │   │   ├── MatchedStmt [p=46]
    │   │   │   │   │   │   │   │   │   ├── ExprStmt [p=64]
    │   │   │   │   │   │   │   │   │   │   ├── CallExpr [p=102]
    │   │   │   │   │   │   │   │   │   │   │   ├── FuncName [p=10]
    │   │   │   │   │   │   │   │   │   │   │   │   ├── ID("add")
    │   │   │   │   │   │   │   │   │   │   │   │   └── __ACT_5 [p=9] {action}
    │   │   │   │   │   │   │   │   │   │   │   ├── LPAREN("(")
    │   │   │   │   │   │   │   │   │   │   │   ├── ArgListOpt [p=106]
    │   │   │   │   │   │   │   │   │   │   │   │   ├── ArgList [p=108]
    │   │   │   │   │   │   │   │   │   │   │   │   │   ├── ArgList [p=110]
    │   │   │   │   │   │   │   │   │   │   │   │   │   │   ├── Expr [p=86]
    │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   ├── Term [p=92]
    │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   ├── Factor [p=96]
    │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   ├── ID("a")
    │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   └── __ACT_48 [p=95] {action}
    │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   └── __ACT_46 [p=91] {action}
    │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   └── __ACT_43 [p=85] {action}
    │   │   │   │   │   │   │   │   │   │   │   │   │   │   └── __ACT_55 [p=109] {action}
    │   │   │   │   │   │   │   │   │   │   │   │   │   ├── COMMA(",")
    │   │   │   │   │   │   │   │   │   │   │   │   │   ├── Expr [p=86]
    │   │   │   │   │   │   │   │   │   │   │   │   │   │   ├── Term [p=92]
    │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   ├── Factor [p=96]
    │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   ├── ID("b")
    │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   └── __ACT_48 [p=95] {action}
    │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   └── __ACT_46 [p=91] {action}
    │   │   │   │   │   │   │   │   │   │   │   │   │   │   └── __ACT_43 [p=85] {action}
    │   │   │   │   │   │   │   │   │   │   │   │   │   └── __ACT_54 [p=107] {action}
    │   │   │   │   │   │   │   │   │   │   │   │   └── __ACT_53 [p=105] {action}
    │   │   │   │   │   │   │   │   │   │   │   ├── RPAREN(")")
    │   │   │   │   │   │   │   │   │   │   │   └── __ACT_51 [p=101] {action}
    │   │   │   │   │   │   │   │   │   │   ├── SEMI(";")
    │   │   │   │   │   │   │   │   │   │   └── __ACT_32 [p=63] {action}
    │   │   │   │   │   │   │   │   │   └── __ACT_23 [p=45] {action}
    │   │   │   │   │   │   │   │   └── __ACT_20 [p=39] {action}
    │   │   │   │   │   │   │   └── __ACT_16 [p=31] {action}
    │   │   │   │   │   │   └── __ACT_14 [p=27] {action}
    │   │   │   │   │   ├── Item [p=32]
    │   │   │   │   │   │   ├── Stmt [p=40]
    │   │   │   │   │   │   │   ├── MatchedStmt [p=52]
    │   │   │   │   │   │   │   │   ├── WHILE("while")
    │   │   │   │   │   │   │   │   ├── LPAREN("(")
    │   │   │   │   │   │   │   │   ├── Cond [p=68]
    │   │   │   │   │   │   │   │   │   ├── Expr [p=86]
    │   │   │   │   │   │   │   │   │   │   ├── Term [p=92]
    │   │   │   │   │   │   │   │   │   │   │   ├── Factor [p=96]
    │   │   │   │   │   │   │   │   │   │   │   │   ├── ID("a")
    │   │   │   │   │   │   │   │   │   │   │   │   └── __ACT_48 [p=95] {action}
    │   │   │   │   │   │   │   │   │   │   │   └── __ACT_46 [p=91] {action}
    │   │   │   │   │   │   │   │   │   │   └── __ACT_43 [p=85] {action}
    │   │   │   │   │   │   │   │   │   ├── RelOp [p=80]
    │   │   │   │   │   │   │   │   │   │   ├── NE("!=")
    │   │   │   │   │   │   │   │   │   │   └── __ACT_40 [p=79] {action}
    │   │   │   │   │   │   │   │   │   ├── Expr [p=86]
    │   │   │   │   │   │   │   │   │   │   ├── Term [p=92]
    │   │   │   │   │   │   │   │   │   │   │   ├── Factor [p=96]
    │   │   │   │   │   │   │   │   │   │   │   │   ├── ID("b")
    │   │   │   │   │   │   │   │   │   │   │   │   └── __ACT_48 [p=95] {action}
    │   │   │   │   │   │   │   │   │   │   │   └── __ACT_46 [p=91] {action}
    │   │   │   │   │   │   │   │   │   │   └── __ACT_43 [p=85] {action}
    │   │   │   │   │   │   │   │   │   └── __ACT_34 [p=67] {action}
    │   │   │   │   │   │   │   │   ├── RPAREN(")")
    │   │   │   │   │   │   │   │   ├── MatchedStmt [p=50]
    │   │   │   │   │   │   │   │   │   ├── Block [p=24]
    │   │   │   │   │   │   │   │   │   │   ├── LBRACE("{")
    │   │   │   │   │   │   │   │   │   │   ├── ItemList [p=28]
    │   │   │   │   │   │   │   │   │   │   │   ├── ItemList [p=26]
    │   │   │   │   │   │   │   │   │   │   │   │   └── __ACT_13 [p=25] {action}
    │   │   │   │   │   │   │   │   │   │   │   ├── Item [p=32]
    │   │   │   │   │   │   │   │   │   │   │   │   ├── Stmt [p=40]
    │   │   │   │   │   │   │   │   │   │   │   │   │   ├── MatchedStmt [p=44]
    │   │   │   │   │   │   │   │   │   │   │   │   │   │   ├── AssignStmt [p=62]
    │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   ├── ID("a")
    │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   ├── ASSIGN("=")
    │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   ├── Expr [p=86]
    │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   ├── Term [p=92]
    │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   ├── Factor [p=100]
    │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   ├── CallExpr [p=102]
    │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   ├── FuncName [p=10]
    │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   ├── ID("add")
    │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   └── __ACT_5 [p=9] {action}
    │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   ├── LPAREN("(")
    │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   ├── ArgListOpt [p=106]
    │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   ├── ArgList [p=108]
    │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   ├── ArgList [p=110]
    │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   ├── Expr [p=86]
    │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   ├── Term [p=92]
    │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   ├── Factor [p=96]
    │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   ├── ID("a")
    │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   └── __ACT_48 [p=95] {action}
    │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   └── __ACT_46 [p=91] {action}
    │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   └── __ACT_43 [p=85] {action}
    │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   └── __ACT_55 [p=109] {action}
    │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   ├── COMMA(",")
    │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   ├── Expr [p=86]
    │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   ├── Term [p=92]
    │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   ├── Factor [p=98]
    │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   ├── NUM("1")
    │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   └── __ACT_49 [p=97] {action}
    │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   └── __ACT_46 [p=91] {action}
    │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   └── __ACT_43 [p=85] {action}
    │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   └── __ACT_54 [p=107] {action}
    │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   └── __ACT_53 [p=105] {action}
    │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   ├── RPAREN(")")
    │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   └── __ACT_51 [p=101] {action}
    │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   └── __ACT_50 [p=99] {action}
    │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   └── __ACT_46 [p=91] {action}
    │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   └── __ACT_43 [p=85] {action}
    │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   ├── SEMI(";")
    │   │   │   │   │   │   │   │   │   │   │   │   │   │   │   └── __ACT_31 [p=61] {action}
    │   │   │   │   │   │   │   │   │   │   │   │   │   │   └── __ACT_22 [p=43] {action}
    │   │   │   │   │   │   │   │   │   │   │   │   │   └── __ACT_20 [p=39] {action}
    │   │   │   │   │   │   │   │   │   │   │   │   └── __ACT_16 [p=31] {action}
    │   │   │   │   │   │   │   │   │   │   │   └── __ACT_14 [p=27] {action}
    │   │   │   │   │   │   │   │   │   │   ├── RBRACE("}")
    │   │   │   │   │   │   │   │   │   │   └── __ACT_12 [p=23] {action}
    │   │   │   │   │   │   │   │   │   └── __ACT_25 [p=49] {action}
    │   │   │   │   │   │   │   │   └── __ACT_26 [p=51] {action}
    │   │   │   │   │   │   │   └── __ACT_20 [p=39] {action}
    │   │   │   │   │   │   └── __ACT_16 [p=31] {action}
    │   │   │   │   │   └── __ACT_14 [p=27] {action}
    │   │   │   │   ├── Item [p=32]
    │   │   │   │   │   ├── Stmt [p=40]
    │   │   │   │   │   │   ├── MatchedStmt [p=48]
    │   │   │   │   │   │   │   ├── ReturnStmt [p=66]
    │   │   │   │   │   │   │   │   ├── RETURN("return")
    │   │   │   │   │   │   │   │   ├── Expr [p=86]
    │   │   │   │   │   │   │   │   │   ├── Term [p=92]
    │   │   │   │   │   │   │   │   │   │   ├── Factor [p=96]
    │   │   │   │   │   │   │   │   │   │   │   ├── ID("a")
    │   │   │   │   │   │   │   │   │   │   │   └── __ACT_48 [p=95] {action}
    │   │   │   │   │   │   │   │   │   │   └── __ACT_46 [p=91] {action}
    │   │   │   │   │   │   │   │   │   └── __ACT_43 [p=85] {action}
    │   │   │   │   │   │   │   │   ├── SEMI(";")
    │   │   │   │   │   │   │   │   └── __ACT_33 [p=65] {action}
    │   │   │   │   │   │   │   └── __ACT_24 [p=47] {action}
    │   │   │   │   │   │   └── __ACT_20 [p=39] {action}
    │   │   │   │   │   └── __ACT_16 [p=31] {action}
    │   │   │   │   └── __ACT_14 [p=27] {action}
    │   │   │   ├── RBRACE("}")
    │   │   │   └── __ACT_12 [p=23] {action}
    │   │   └── __ACT_4 [p=7] {action}
    │   └── __ACT_2 [p=3] {action}
    └── __ACT_1 [p=1] {action}
```

## 3. Mermaid 可视化语法树

```mermaid
flowchart TD
    n0["n0: Program<br/>production = 2"]:::nonTerminal
    n1["n1: FuncList<br/>production = 4"]:::nonTerminal
    n2["n2: FuncList<br/>production = 6"]:::nonTerminal
    n3["n3: FuncDef<br/>production = 8"]:::nonTerminal
    n4["n4: INT<br/>lexeme = int"]:::terminal
    n5["n5: FuncName<br/>production = 10"]:::nonTerminal
    n6["n6: ID<br/>lexeme = add"]:::terminal
    n7["n7: __ACT_5<br/>production = 9<br/>semantic action<br/>{ $$ = $1; }"]:::semanticAction
    n8["n8: LPAREN<br/>lexeme = ("]:::terminal
    n9["n9: ParamListOpt<br/>production = 16"]:::nonTerminal
    n10["n10: ParamList<br/>production = 18"]:::nonTerminal
    n11["n11: ParamList<br/>production = 20"]:::nonTerminal
    n12["n12: Param<br/>production = 22"]:::nonTerminal
    n13["n13: INT<br/>lexeme = int"]:::terminal
    n14["n14: ID<br/>lexeme = x"]:::terminal
    n15["n15: __ACT_11<br/>production = 21<br/>semantic action<br/>{ $$ = makeParam($2); }"]:::semanticAction
    n16["n16: __ACT_10<br/>production = 19<br/>semantic action<br/>{ $$ = makeParamList($1); }"]:::semanticAction
    n17["n17: COMMA<br/>lexeme = ,"]:::terminal
    n18["n18: Param<br/>production = 22"]:::nonTerminal
    n19["n19: INT<br/>lexeme = int"]:::terminal
    n20["n20: ID<br/>lexeme = y"]:::terminal
    n21["n21: __ACT_11<br/>production = 21<br/>semantic action<br/>{ $$ = makeParam($2); }"]:::semanticAction
    n22["n22: __ACT_9<br/>production = 17<br/>semantic action<br/>{ $$ = appendParam($1, $3); }"]:::semanticAction
    n23["n23: __ACT_8<br/>production = 15<br/>semantic action<br/>{ $$ = $1; }"]:::semanticAction
    n24["n24: RPAREN<br/>lexeme = )"]:::terminal
    n25["n25: Block<br/>production = 24"]:::nonTerminal
    n26["n26: LBRACE<br/>lexeme = {"]:::terminal
    n27["n27: ItemList<br/>production = 28"]:::nonTerminal
    n28["n28: ItemList<br/>production = 26"]:::nonTerminal
    n29["n29: __ACT_13<br/>production = 25<br/>semantic action<br/>{ $$ = makeEmptyItemList(); }"]:::semanticAction
    n30["n30: Item<br/>production = 32"]:::nonTerminal
    n31["n31: Stmt<br/>production = 40"]:::nonTerminal
    n32["n32: MatchedStmt<br/>production = 48"]:::nonTerminal
    n33["n33: ReturnStmt<br/>production = 66"]:::nonTerminal
    n34["n34: RETURN<br/>lexeme = return"]:::terminal
    n35["n35: Expr<br/>production = 82"]:::nonTerminal
    n36["n36: Expr<br/>production = 86"]:::nonTerminal
    n37["n37: Term<br/>production = 92"]:::nonTerminal
    n38["n38: Factor<br/>production = 96"]:::nonTerminal
    n39["n39: ID<br/>lexeme = x"]:::terminal
    n40["n40: __ACT_48<br/>production = 95<br/>semantic action<br/>{ $$ = makeIdentifier($1); }"]:::semanticAction
    n41["n41: __ACT_46<br/>production = 91<br/>semantic action<br/>{ $$ = $1; }"]:::semanticAction
    n42["n42: __ACT_43<br/>production = 85<br/>semantic action<br/>{ $$ = $1; }"]:::semanticAction
    n43["n43: PLUS<br/>lexeme = +"]:::terminal
    n44["n44: Term<br/>production = 92"]:::nonTerminal
    n45["n45: Factor<br/>production = 96"]:::nonTerminal
    n46["n46: ID<br/>lexeme = y"]:::terminal
    n47["n47: __ACT_48<br/>production = 95<br/>semantic action<br/>{ $$ = makeIdentifier($1); }"]:::semanticAction
    n48["n48: __ACT_46<br/>production = 91<br/>semantic action<br/>{ $$ = $1; }"]:::semanticAction
    n49["n49: __ACT_41<br/>production = 81<br/>semantic action<br/>{ $$ = makeBinary(&quot;+&quot;, $1, $3); }"]:::semanticAction
    n50["n50: SEMI<br/>lexeme = ;"]:::terminal
    n51["n51: __ACT_33<br/>production = 65<br/>semantic action<br/>{ $$ = makeReturn($2); }"]:::semanticAction
    n52["n52: __ACT_24<br/>production = 47<br/>semantic action<br/>{ $$ = $1; }"]:::semanticAction
    n53["n53: __ACT_20<br/>production = 39<br/>semantic action<br/>{ $$ = $1; }"]:::semanticAction
    n54["n54: __ACT_16<br/>production = 31<br/>semantic action<br/>{ $$ = $1; }"]:::semanticAction
    n55["n55: __ACT_14<br/>production = 27<br/>semantic action<br/>{ $$ = appendItem($1, $2); }"]:::semanticAction
    n56["n56: RBRACE<br/>lexeme = }"]:::terminal
    n57["n57: __ACT_12<br/>production = 23<br/>semantic action<br/>{ $$ = makeBlock($2); }"]:::semanticAction
    n58["n58: __ACT_4<br/>production = 7<br/>semantic action<br/>{ $$ = makeFunction($2, $4, $6); }"]:::semanticAction
    n59["n59: __ACT_3<br/>production = 5<br/>semantic action<br/>{ $$ = makeFunctionList($1); }"]:::semanticAction
    n60["n60: FuncDef<br/>production = 8"]:::nonTerminal
    n61["n61: INT<br/>lexeme = int"]:::terminal
    n62["n62: FuncName<br/>production = 12"]:::nonTerminal
    n63["n63: MAIN<br/>lexeme = main"]:::terminal
    n64["n64: __ACT_6<br/>production = 11<br/>semantic action<br/>{ $$ = $1; }"]:::semanticAction
    n65["n65: LPAREN<br/>lexeme = ("]:::terminal
    n66["n66: ParamListOpt<br/>production = 14"]:::nonTerminal
    n67["n67: __ACT_7<br/>production = 13<br/>semantic action<br/>{ $$ = makeEmptyParamList(); }"]:::semanticAction
    n68["n68: RPAREN<br/>lexeme = )"]:::terminal
    n69["n69: Block<br/>production = 24"]:::nonTerminal
    n70["n70: LBRACE<br/>lexeme = {"]:::terminal
    n71["n71: ItemList<br/>production = 28"]:::nonTerminal
    n72["n72: ItemList<br/>production = 28"]:::nonTerminal
    n73["n73: ItemList<br/>production = 28"]:::nonTerminal
    n74["n74: ItemList<br/>production = 28"]:::nonTerminal
    n75["n75: ItemList<br/>production = 28"]:::nonTerminal
    n76["n76: ItemList<br/>production = 28"]:::nonTerminal
    n77["n77: ItemList<br/>production = 28"]:::nonTerminal
    n78["n78: ItemList<br/>production = 26"]:::nonTerminal
    n79["n79: __ACT_13<br/>production = 25<br/>semantic action<br/>{ $$ = makeEmptyItemList(); }"]:::semanticAction
    n80["n80: Item<br/>production = 30"]:::nonTerminal
    n81["n81: Decl<br/>production = 34"]:::nonTerminal
    n82["n82: INT<br/>lexeme = int"]:::terminal
    n83["n83: ID<br/>lexeme = a"]:::terminal
    n84["n84: DeclInitOpt<br/>production = 36"]:::nonTerminal
    n85["n85: __ACT_18<br/>production = 35<br/>semantic action<br/>{ $$ = makeNoInitializer(); }"]:::semanticAction
    n86["n86: SEMI<br/>lexeme = ;"]:::terminal
    n87["n87: __ACT_17<br/>production = 33<br/>semantic action<br/>{ $$ = makeDeclaration($2, $3); }"]:::semanticAction
    n88["n88: __ACT_15<br/>production = 29<br/>semantic action<br/>{ $$ = $1; }"]:::semanticAction
    n89["n89: __ACT_14<br/>production = 27<br/>semantic action<br/>{ $$ = appendItem($1, $2); }"]:::semanticAction
    n90["n90: Item<br/>production = 30"]:::nonTerminal
    n91["n91: Decl<br/>production = 34"]:::nonTerminal
    n92["n92: INT<br/>lexeme = int"]:::terminal
    n93["n93: ID<br/>lexeme = b"]:::terminal
    n94["n94: DeclInitOpt<br/>production = 38"]:::nonTerminal
    n95["n95: ASSIGN<br/>lexeme = ="]:::terminal
    n96["n96: Expr<br/>production = 86"]:::nonTerminal
    n97["n97: Term<br/>production = 92"]:::nonTerminal
    n98["n98: Factor<br/>production = 98"]:::nonTerminal
    n99["n99: NUM<br/>lexeme = 5"]:::terminal
    n100["n100: __ACT_49<br/>production = 97<br/>semantic action<br/>{ $$ = makeIntLiteral($1); }"]:::semanticAction
    n101["n101: __ACT_46<br/>production = 91<br/>semantic action<br/>{ $$ = $1; }"]:::semanticAction
    n102["n102: __ACT_43<br/>production = 85<br/>semantic action<br/>{ $$ = $1; }"]:::semanticAction
    n103["n103: __ACT_19<br/>production = 37<br/>semantic action<br/>{ $$ = makeInitializer($2); }"]:::semanticAction
    n104["n104: SEMI<br/>lexeme = ;"]:::terminal
    n105["n105: __ACT_17<br/>production = 33<br/>semantic action<br/>{ $$ = makeDeclaration($2, $3); }"]:::semanticAction
    n106["n106: __ACT_15<br/>production = 29<br/>semantic action<br/>{ $$ = $1; }"]:::semanticAction
    n107["n107: __ACT_14<br/>production = 27<br/>semantic action<br/>{ $$ = appendItem($1, $2); }"]:::semanticAction
    n108["n108: Item<br/>production = 32"]:::nonTerminal
    n109["n109: Stmt<br/>production = 40"]:::nonTerminal
    n110["n110: MatchedStmt<br/>production = 44"]:::nonTerminal
    n111["n111: AssignStmt<br/>production = 62"]:::nonTerminal
    n112["n112: ID<br/>lexeme = a"]:::terminal
    n113["n113: ASSIGN<br/>lexeme = ="]:::terminal
    n114["n114: Expr<br/>production = 86"]:::nonTerminal
    n115["n115: Term<br/>production = 92"]:::nonTerminal
    n116["n116: Factor<br/>production = 100"]:::nonTerminal
    n117["n117: CallExpr<br/>production = 102"]:::nonTerminal
    n118["n118: FuncName<br/>production = 10"]:::nonTerminal
    n119["n119: ID<br/>lexeme = add"]:::terminal
    n120["n120: __ACT_5<br/>production = 9<br/>semantic action<br/>{ $$ = $1; }"]:::semanticAction
    n121["n121: LPAREN<br/>lexeme = ("]:::terminal
    n122["n122: ArgListOpt<br/>production = 106"]:::nonTerminal
    n123["n123: ArgList<br/>production = 108"]:::nonTerminal
    n124["n124: ArgList<br/>production = 110"]:::nonTerminal
    n125["n125: Expr<br/>production = 86"]:::nonTerminal
    n126["n126: Term<br/>production = 92"]:::nonTerminal
    n127["n127: Factor<br/>production = 96"]:::nonTerminal
    n128["n128: ID<br/>lexeme = b"]:::terminal
    n129["n129: __ACT_48<br/>production = 95<br/>semantic action<br/>{ $$ = makeIdentifier($1); }"]:::semanticAction
    n130["n130: __ACT_46<br/>production = 91<br/>semantic action<br/>{ $$ = $1; }"]:::semanticAction
    n131["n131: __ACT_43<br/>production = 85<br/>semantic action<br/>{ $$ = $1; }"]:::semanticAction
    n132["n132: __ACT_55<br/>production = 109<br/>semantic action<br/>{ $$ = makeArgList($1); }"]:::semanticAction
    n133["n133: COMMA<br/>lexeme = ,"]:::terminal
    n134["n134: Expr<br/>production = 86"]:::nonTerminal
    n135["n135: Term<br/>production = 92"]:::nonTerminal
    n136["n136: Factor<br/>production = 98"]:::nonTerminal
    n137["n137: NUM<br/>lexeme = 3"]:::terminal
    n138["n138: __ACT_49<br/>production = 97<br/>semantic action<br/>{ $$ = makeIntLiteral($1); }"]:::semanticAction
    n139["n139: __ACT_46<br/>production = 91<br/>semantic action<br/>{ $$ = $1; }"]:::semanticAction
    n140["n140: __ACT_43<br/>production = 85<br/>semantic action<br/>{ $$ = $1; }"]:::semanticAction
    n141["n141: __ACT_54<br/>production = 107<br/>semantic action<br/>{ $$ = appendArg($1, $3); }"]:::semanticAction
    n142["n142: __ACT_53<br/>production = 105<br/>semantic action<br/>{ $$ = $1; }"]:::semanticAction
    n143["n143: RPAREN<br/>lexeme = )"]:::terminal
    n144["n144: __ACT_51<br/>production = 101<br/>semantic action<br/>{ $$ = makeCall($1, $3); }"]:::semanticAction
    n145["n145: __ACT_50<br/>production = 99<br/>semantic action<br/>{ $$ = $1; }"]:::semanticAction
    n146["n146: __ACT_46<br/>production = 91<br/>semantic action<br/>{ $$ = $1; }"]:::semanticAction
    n147["n147: __ACT_43<br/>production = 85<br/>semantic action<br/>{ $$ = $1; }"]:::semanticAction
    n148["n148: SEMI<br/>lexeme = ;"]:::terminal
    n149["n149: __ACT_31<br/>production = 61<br/>semantic action<br/>{ $$ = makeAssignment($1, $3); }"]:::semanticAction
    n150["n150: __ACT_22<br/>production = 43<br/>semantic action<br/>{ $$ = $1; }"]:::semanticAction
    n151["n151: __ACT_20<br/>production = 39<br/>semantic action<br/>{ $$ = $1; }"]:::semanticAction
    n152["n152: __ACT_16<br/>production = 31<br/>semantic action<br/>{ $$ = $1; }"]:::semanticAction
    n153["n153: __ACT_14<br/>production = 27<br/>semantic action<br/>{ $$ = appendItem($1, $2); }"]:::semanticAction
    n154["n154: Item<br/>production = 32"]:::nonTerminal
    n155["n155: Stmt<br/>production = 40"]:::nonTerminal
    n156["n156: MatchedStmt<br/>production = 54"]:::nonTerminal
    n157["n157: IF<br/>lexeme = if"]:::terminal
    n158["n158: LPAREN<br/>lexeme = ("]:::terminal
    n159["n159: Cond<br/>production = 68"]:::nonTerminal
    n160["n160: Expr<br/>production = 86"]:::nonTerminal
    n161["n161: Term<br/>production = 92"]:::nonTerminal
    n162["n162: Factor<br/>production = 96"]:::nonTerminal
    n163["n163: ID<br/>lexeme = a"]:::terminal
    n164["n164: __ACT_48<br/>production = 95<br/>semantic action<br/>{ $$ = makeIdentifier($1); }"]:::semanticAction
    n165["n165: __ACT_46<br/>production = 91<br/>semantic action<br/>{ $$ = $1; }"]:::semanticAction
    n166["n166: __ACT_43<br/>production = 85<br/>semantic action<br/>{ $$ = $1; }"]:::semanticAction
    n167["n167: RelOp<br/>production = 70"]:::nonTerminal
    n168["n168: LT<br/>lexeme = &lt;"]:::terminal
    n169["n169: __ACT_35<br/>production = 69<br/>semantic action<br/>{ $$ = makeRelOp($1); }"]:::semanticAction
    n170["n170: Expr<br/>production = 86"]:::nonTerminal
    n171["n171: Term<br/>production = 92"]:::nonTerminal
    n172["n172: Factor<br/>production = 96"]:::nonTerminal
    n173["n173: ID<br/>lexeme = b"]:::terminal
    n174["n174: __ACT_48<br/>production = 95<br/>semantic action<br/>{ $$ = makeIdentifier($1); }"]:::semanticAction
    n175["n175: __ACT_46<br/>production = 91<br/>semantic action<br/>{ $$ = $1; }"]:::semanticAction
    n176["n176: __ACT_43<br/>production = 85<br/>semantic action<br/>{ $$ = $1; }"]:::semanticAction
    n177["n177: __ACT_34<br/>production = 67<br/>semantic action<br/>{ $$ = makeCondition($1, $2, $3); }"]:::semanticAction
    n178["n178: RPAREN<br/>lexeme = )"]:::terminal
    n179["n179: MatchedStmt<br/>production = 44"]:::nonTerminal
    n180["n180: AssignStmt<br/>production = 62"]:::nonTerminal
    n181["n181: ID<br/>lexeme = a"]:::terminal
    n182["n182: ASSIGN<br/>lexeme = ="]:::terminal
    n183["n183: Expr<br/>production = 86"]:::nonTerminal
    n184["n184: Term<br/>production = 92"]:::nonTerminal
    n185["n185: Factor<br/>production = 100"]:::nonTerminal
    n186["n186: CallExpr<br/>production = 102"]:::nonTerminal
    n187["n187: FuncName<br/>production = 10"]:::nonTerminal
    n188["n188: ID<br/>lexeme = add"]:::terminal
    n189["n189: __ACT_5<br/>production = 9<br/>semantic action<br/>{ $$ = $1; }"]:::semanticAction
    n190["n190: LPAREN<br/>lexeme = ("]:::terminal
    n191["n191: ArgListOpt<br/>production = 106"]:::nonTerminal
    n192["n192: ArgList<br/>production = 108"]:::nonTerminal
    n193["n193: ArgList<br/>production = 110"]:::nonTerminal
    n194["n194: Expr<br/>production = 86"]:::nonTerminal
    n195["n195: Term<br/>production = 92"]:::nonTerminal
    n196["n196: Factor<br/>production = 96"]:::nonTerminal
    n197["n197: ID<br/>lexeme = a"]:::terminal
    n198["n198: __ACT_48<br/>production = 95<br/>semantic action<br/>{ $$ = makeIdentifier($1); }"]:::semanticAction
    n199["n199: __ACT_46<br/>production = 91<br/>semantic action<br/>{ $$ = $1; }"]:::semanticAction
    n200["n200: __ACT_43<br/>production = 85<br/>semantic action<br/>{ $$ = $1; }"]:::semanticAction
    n201["n201: __ACT_55<br/>production = 109<br/>semantic action<br/>{ $$ = makeArgList($1); }"]:::semanticAction
    n202["n202: COMMA<br/>lexeme = ,"]:::terminal
    n203["n203: Expr<br/>production = 86"]:::nonTerminal
    n204["n204: Term<br/>production = 92"]:::nonTerminal
    n205["n205: Factor<br/>production = 98"]:::nonTerminal
    n206["n206: NUM<br/>lexeme = 1"]:::terminal
    n207["n207: __ACT_49<br/>production = 97<br/>semantic action<br/>{ $$ = makeIntLiteral($1); }"]:::semanticAction
    n208["n208: __ACT_46<br/>production = 91<br/>semantic action<br/>{ $$ = $1; }"]:::semanticAction
    n209["n209: __ACT_43<br/>production = 85<br/>semantic action<br/>{ $$ = $1; }"]:::semanticAction
    n210["n210: __ACT_54<br/>production = 107<br/>semantic action<br/>{ $$ = appendArg($1, $3); }"]:::semanticAction
    n211["n211: __ACT_53<br/>production = 105<br/>semantic action<br/>{ $$ = $1; }"]:::semanticAction
    n212["n212: RPAREN<br/>lexeme = )"]:::terminal
    n213["n213: __ACT_51<br/>production = 101<br/>semantic action<br/>{ $$ = makeCall($1, $3); }"]:::semanticAction
    n214["n214: __ACT_50<br/>production = 99<br/>semantic action<br/>{ $$ = $1; }"]:::semanticAction
    n215["n215: __ACT_46<br/>production = 91<br/>semantic action<br/>{ $$ = $1; }"]:::semanticAction
    n216["n216: __ACT_43<br/>production = 85<br/>semantic action<br/>{ $$ = $1; }"]:::semanticAction
    n217["n217: SEMI<br/>lexeme = ;"]:::terminal
    n218["n218: __ACT_31<br/>production = 61<br/>semantic action<br/>{ $$ = makeAssignment($1, $3); }"]:::semanticAction
    n219["n219: __ACT_22<br/>production = 43<br/>semantic action<br/>{ $$ = $1; }"]:::semanticAction
    n220["n220: ELSE<br/>lexeme = else"]:::terminal
    n221["n221: MatchedStmt<br/>production = 44"]:::nonTerminal
    n222["n222: AssignStmt<br/>production = 62"]:::nonTerminal
    n223["n223: ID<br/>lexeme = a"]:::terminal
    n224["n224: ASSIGN<br/>lexeme = ="]:::terminal
    n225["n225: Expr<br/>production = 86"]:::nonTerminal
    n226["n226: Term<br/>production = 92"]:::nonTerminal
    n227["n227: Factor<br/>production = 100"]:::nonTerminal
    n228["n228: CallExpr<br/>production = 102"]:::nonTerminal
    n229["n229: FuncName<br/>production = 10"]:::nonTerminal
    n230["n230: ID<br/>lexeme = add"]:::terminal
    n231["n231: __ACT_5<br/>production = 9<br/>semantic action<br/>{ $$ = $1; }"]:::semanticAction
    n232["n232: LPAREN<br/>lexeme = ("]:::terminal
    n233["n233: ArgListOpt<br/>production = 106"]:::nonTerminal
    n234["n234: ArgList<br/>production = 108"]:::nonTerminal
    n235["n235: ArgList<br/>production = 110"]:::nonTerminal
    n236["n236: Expr<br/>production = 86"]:::nonTerminal
    n237["n237: Term<br/>production = 92"]:::nonTerminal
    n238["n238: Factor<br/>production = 96"]:::nonTerminal
    n239["n239: ID<br/>lexeme = a"]:::terminal
    n240["n240: __ACT_48<br/>production = 95<br/>semantic action<br/>{ $$ = makeIdentifier($1); }"]:::semanticAction
    n241["n241: __ACT_46<br/>production = 91<br/>semantic action<br/>{ $$ = $1; }"]:::semanticAction
    n242["n242: __ACT_43<br/>production = 85<br/>semantic action<br/>{ $$ = $1; }"]:::semanticAction
    n243["n243: __ACT_55<br/>production = 109<br/>semantic action<br/>{ $$ = makeArgList($1); }"]:::semanticAction
    n244["n244: COMMA<br/>lexeme = ,"]:::terminal
    n245["n245: Expr<br/>production = 86"]:::nonTerminal
    n246["n246: Term<br/>production = 92"]:::nonTerminal
    n247["n247: Factor<br/>production = 96"]:::nonTerminal
    n248["n248: ID<br/>lexeme = b"]:::terminal
    n249["n249: __ACT_48<br/>production = 95<br/>semantic action<br/>{ $$ = makeIdentifier($1); }"]:::semanticAction
    n250["n250: __ACT_46<br/>production = 91<br/>semantic action<br/>{ $$ = $1; }"]:::semanticAction
    n251["n251: __ACT_43<br/>production = 85<br/>semantic action<br/>{ $$ = $1; }"]:::semanticAction
    n252["n252: __ACT_54<br/>production = 107<br/>semantic action<br/>{ $$ = appendArg($1, $3); }"]:::semanticAction
    n253["n253: __ACT_53<br/>production = 105<br/>semantic action<br/>{ $$ = $1; }"]:::semanticAction
    n254["n254: RPAREN<br/>lexeme = )"]:::terminal
    n255["n255: __ACT_51<br/>production = 101<br/>semantic action<br/>{ $$ = makeCall($1, $3); }"]:::semanticAction
    n256["n256: __ACT_50<br/>production = 99<br/>semantic action<br/>{ $$ = $1; }"]:::semanticAction
    n257["n257: __ACT_46<br/>production = 91<br/>semantic action<br/>{ $$ = $1; }"]:::semanticAction
    n258["n258: __ACT_43<br/>production = 85<br/>semantic action<br/>{ $$ = $1; }"]:::semanticAction
    n259["n259: SEMI<br/>lexeme = ;"]:::terminal
    n260["n260: __ACT_31<br/>production = 61<br/>semantic action<br/>{ $$ = makeAssignment($1, $3); }"]:::semanticAction
    n261["n261: __ACT_22<br/>production = 43<br/>semantic action<br/>{ $$ = $1; }"]:::semanticAction
    n262["n262: __ACT_27<br/>production = 53<br/>semantic action<br/>{ $$ = makeIfElse($3, $5, $7); }"]:::semanticAction
    n263["n263: __ACT_20<br/>production = 39<br/>semantic action<br/>{ $$ = $1; }"]:::semanticAction
    n264["n264: __ACT_16<br/>production = 31<br/>semantic action<br/>{ $$ = $1; }"]:::semanticAction
    n265["n265: __ACT_14<br/>production = 27<br/>semantic action<br/>{ $$ = appendItem($1, $2); }"]:::semanticAction
    n266["n266: Item<br/>production = 32"]:::nonTerminal
    n267["n267: Stmt<br/>production = 40"]:::nonTerminal
    n268["n268: MatchedStmt<br/>production = 46"]:::nonTerminal
    n269["n269: ExprStmt<br/>production = 64"]:::nonTerminal
    n270["n270: CallExpr<br/>production = 102"]:::nonTerminal
    n271["n271: FuncName<br/>production = 10"]:::nonTerminal
    n272["n272: ID<br/>lexeme = add"]:::terminal
    n273["n273: __ACT_5<br/>production = 9<br/>semantic action<br/>{ $$ = $1; }"]:::semanticAction
    n274["n274: LPAREN<br/>lexeme = ("]:::terminal
    n275["n275: ArgListOpt<br/>production = 106"]:::nonTerminal
    n276["n276: ArgList<br/>production = 108"]:::nonTerminal
    n277["n277: ArgList<br/>production = 110"]:::nonTerminal
    n278["n278: Expr<br/>production = 86"]:::nonTerminal
    n279["n279: Term<br/>production = 92"]:::nonTerminal
    n280["n280: Factor<br/>production = 96"]:::nonTerminal
    n281["n281: ID<br/>lexeme = a"]:::terminal
    n282["n282: __ACT_48<br/>production = 95<br/>semantic action<br/>{ $$ = makeIdentifier($1); }"]:::semanticAction
    n283["n283: __ACT_46<br/>production = 91<br/>semantic action<br/>{ $$ = $1; }"]:::semanticAction
    n284["n284: __ACT_43<br/>production = 85<br/>semantic action<br/>{ $$ = $1; }"]:::semanticAction
    n285["n285: __ACT_55<br/>production = 109<br/>semantic action<br/>{ $$ = makeArgList($1); }"]:::semanticAction
    n286["n286: COMMA<br/>lexeme = ,"]:::terminal
    n287["n287: Expr<br/>production = 86"]:::nonTerminal
    n288["n288: Term<br/>production = 92"]:::nonTerminal
    n289["n289: Factor<br/>production = 96"]:::nonTerminal
    n290["n290: ID<br/>lexeme = b"]:::terminal
    n291["n291: __ACT_48<br/>production = 95<br/>semantic action<br/>{ $$ = makeIdentifier($1); }"]:::semanticAction
    n292["n292: __ACT_46<br/>production = 91<br/>semantic action<br/>{ $$ = $1; }"]:::semanticAction
    n293["n293: __ACT_43<br/>production = 85<br/>semantic action<br/>{ $$ = $1; }"]:::semanticAction
    n294["n294: __ACT_54<br/>production = 107<br/>semantic action<br/>{ $$ = appendArg($1, $3); }"]:::semanticAction
    n295["n295: __ACT_53<br/>production = 105<br/>semantic action<br/>{ $$ = $1; }"]:::semanticAction
    n296["n296: RPAREN<br/>lexeme = )"]:::terminal
    n297["n297: __ACT_51<br/>production = 101<br/>semantic action<br/>{ $$ = makeCall($1, $3); }"]:::semanticAction
    n298["n298: SEMI<br/>lexeme = ;"]:::terminal
    n299["n299: __ACT_32<br/>production = 63<br/>semantic action<br/>{ $$ = makeExprStmt($1); }"]:::semanticAction
    n300["n300: __ACT_23<br/>production = 45<br/>semantic action<br/>{ $$ = $1; }"]:::semanticAction
    n301["n301: __ACT_20<br/>production = 39<br/>semantic action<br/>{ $$ = $1; }"]:::semanticAction
    n302["n302: __ACT_16<br/>production = 31<br/>semantic action<br/>{ $$ = $1; }"]:::semanticAction
    n303["n303: __ACT_14<br/>production = 27<br/>semantic action<br/>{ $$ = appendItem($1, $2); }"]:::semanticAction
    n304["n304: Item<br/>production = 32"]:::nonTerminal
    n305["n305: Stmt<br/>production = 40"]:::nonTerminal
    n306["n306: MatchedStmt<br/>production = 52"]:::nonTerminal
    n307["n307: WHILE<br/>lexeme = while"]:::terminal
    n308["n308: LPAREN<br/>lexeme = ("]:::terminal
    n309["n309: Cond<br/>production = 68"]:::nonTerminal
    n310["n310: Expr<br/>production = 86"]:::nonTerminal
    n311["n311: Term<br/>production = 92"]:::nonTerminal
    n312["n312: Factor<br/>production = 96"]:::nonTerminal
    n313["n313: ID<br/>lexeme = a"]:::terminal
    n314["n314: __ACT_48<br/>production = 95<br/>semantic action<br/>{ $$ = makeIdentifier($1); }"]:::semanticAction
    n315["n315: __ACT_46<br/>production = 91<br/>semantic action<br/>{ $$ = $1; }"]:::semanticAction
    n316["n316: __ACT_43<br/>production = 85<br/>semantic action<br/>{ $$ = $1; }"]:::semanticAction
    n317["n317: RelOp<br/>production = 80"]:::nonTerminal
    n318["n318: NE<br/>lexeme = !="]:::terminal
    n319["n319: __ACT_40<br/>production = 79<br/>semantic action<br/>{ $$ = makeRelOp($1); }"]:::semanticAction
    n320["n320: Expr<br/>production = 86"]:::nonTerminal
    n321["n321: Term<br/>production = 92"]:::nonTerminal
    n322["n322: Factor<br/>production = 96"]:::nonTerminal
    n323["n323: ID<br/>lexeme = b"]:::terminal
    n324["n324: __ACT_48<br/>production = 95<br/>semantic action<br/>{ $$ = makeIdentifier($1); }"]:::semanticAction
    n325["n325: __ACT_46<br/>production = 91<br/>semantic action<br/>{ $$ = $1; }"]:::semanticAction
    n326["n326: __ACT_43<br/>production = 85<br/>semantic action<br/>{ $$ = $1; }"]:::semanticAction
    n327["n327: __ACT_34<br/>production = 67<br/>semantic action<br/>{ $$ = makeCondition($1, $2, $3); }"]:::semanticAction
    n328["n328: RPAREN<br/>lexeme = )"]:::terminal
    n329["n329: MatchedStmt<br/>production = 50"]:::nonTerminal
    n330["n330: Block<br/>production = 24"]:::nonTerminal
    n331["n331: LBRACE<br/>lexeme = {"]:::terminal
    n332["n332: ItemList<br/>production = 28"]:::nonTerminal
    n333["n333: ItemList<br/>production = 26"]:::nonTerminal
    n334["n334: __ACT_13<br/>production = 25<br/>semantic action<br/>{ $$ = makeEmptyItemList(); }"]:::semanticAction
    n335["n335: Item<br/>production = 32"]:::nonTerminal
    n336["n336: Stmt<br/>production = 40"]:::nonTerminal
    n337["n337: MatchedStmt<br/>production = 44"]:::nonTerminal
    n338["n338: AssignStmt<br/>production = 62"]:::nonTerminal
    n339["n339: ID<br/>lexeme = a"]:::terminal
    n340["n340: ASSIGN<br/>lexeme = ="]:::terminal
    n341["n341: Expr<br/>production = 86"]:::nonTerminal
    n342["n342: Term<br/>production = 92"]:::nonTerminal
    n343["n343: Factor<br/>production = 100"]:::nonTerminal
    n344["n344: CallExpr<br/>production = 102"]:::nonTerminal
    n345["n345: FuncName<br/>production = 10"]:::nonTerminal
    n346["n346: ID<br/>lexeme = add"]:::terminal
    n347["n347: __ACT_5<br/>production = 9<br/>semantic action<br/>{ $$ = $1; }"]:::semanticAction
    n348["n348: LPAREN<br/>lexeme = ("]:::terminal
    n349["n349: ArgListOpt<br/>production = 106"]:::nonTerminal
    n350["n350: ArgList<br/>production = 108"]:::nonTerminal
    n351["n351: ArgList<br/>production = 110"]:::nonTerminal
    n352["n352: Expr<br/>production = 86"]:::nonTerminal
    n353["n353: Term<br/>production = 92"]:::nonTerminal
    n354["n354: Factor<br/>production = 96"]:::nonTerminal
    n355["n355: ID<br/>lexeme = a"]:::terminal
    n356["n356: __ACT_48<br/>production = 95<br/>semantic action<br/>{ $$ = makeIdentifier($1); }"]:::semanticAction
    n357["n357: __ACT_46<br/>production = 91<br/>semantic action<br/>{ $$ = $1; }"]:::semanticAction
    n358["n358: __ACT_43<br/>production = 85<br/>semantic action<br/>{ $$ = $1; }"]:::semanticAction
    n359["n359: __ACT_55<br/>production = 109<br/>semantic action<br/>{ $$ = makeArgList($1); }"]:::semanticAction
    n360["n360: COMMA<br/>lexeme = ,"]:::terminal
    n361["n361: Expr<br/>production = 86"]:::nonTerminal
    n362["n362: Term<br/>production = 92"]:::nonTerminal
    n363["n363: Factor<br/>production = 98"]:::nonTerminal
    n364["n364: NUM<br/>lexeme = 1"]:::terminal
    n365["n365: __ACT_49<br/>production = 97<br/>semantic action<br/>{ $$ = makeIntLiteral($1); }"]:::semanticAction
    n366["n366: __ACT_46<br/>production = 91<br/>semantic action<br/>{ $$ = $1; }"]:::semanticAction
    n367["n367: __ACT_43<br/>production = 85<br/>semantic action<br/>{ $$ = $1; }"]:::semanticAction
    n368["n368: __ACT_54<br/>production = 107<br/>semantic action<br/>{ $$ = appendArg($1, $3); }"]:::semanticAction
    n369["n369: __ACT_53<br/>production = 105<br/>semantic action<br/>{ $$ = $1; }"]:::semanticAction
    n370["n370: RPAREN<br/>lexeme = )"]:::terminal
    n371["n371: __ACT_51<br/>production = 101<br/>semantic action<br/>{ $$ = makeCall($1, $3); }"]:::semanticAction
    n372["n372: __ACT_50<br/>production = 99<br/>semantic action<br/>{ $$ = $1; }"]:::semanticAction
    n373["n373: __ACT_46<br/>production = 91<br/>semantic action<br/>{ $$ = $1; }"]:::semanticAction
    n374["n374: __ACT_43<br/>production = 85<br/>semantic action<br/>{ $$ = $1; }"]:::semanticAction
    n375["n375: SEMI<br/>lexeme = ;"]:::terminal
    n376["n376: __ACT_31<br/>production = 61<br/>semantic action<br/>{ $$ = makeAssignment($1, $3); }"]:::semanticAction
    n377["n377: __ACT_22<br/>production = 43<br/>semantic action<br/>{ $$ = $1; }"]:::semanticAction
    n378["n378: __ACT_20<br/>production = 39<br/>semantic action<br/>{ $$ = $1; }"]:::semanticAction
    n379["n379: __ACT_16<br/>production = 31<br/>semantic action<br/>{ $$ = $1; }"]:::semanticAction
    n380["n380: __ACT_14<br/>production = 27<br/>semantic action<br/>{ $$ = appendItem($1, $2); }"]:::semanticAction
    n381["n381: RBRACE<br/>lexeme = }"]:::terminal
    n382["n382: __ACT_12<br/>production = 23<br/>semantic action<br/>{ $$ = makeBlock($2); }"]:::semanticAction
    n383["n383: __ACT_25<br/>production = 49<br/>semantic action<br/>{ $$ = $1; }"]:::semanticAction
    n384["n384: __ACT_26<br/>production = 51<br/>semantic action<br/>{ $$ = makeWhile($3, $5); }"]:::semanticAction
    n385["n385: __ACT_20<br/>production = 39<br/>semantic action<br/>{ $$ = $1; }"]:::semanticAction
    n386["n386: __ACT_16<br/>production = 31<br/>semantic action<br/>{ $$ = $1; }"]:::semanticAction
    n387["n387: __ACT_14<br/>production = 27<br/>semantic action<br/>{ $$ = appendItem($1, $2); }"]:::semanticAction
    n388["n388: Item<br/>production = 32"]:::nonTerminal
    n389["n389: Stmt<br/>production = 40"]:::nonTerminal
    n390["n390: MatchedStmt<br/>production = 48"]:::nonTerminal
    n391["n391: ReturnStmt<br/>production = 66"]:::nonTerminal
    n392["n392: RETURN<br/>lexeme = return"]:::terminal
    n393["n393: Expr<br/>production = 86"]:::nonTerminal
    n394["n394: Term<br/>production = 92"]:::nonTerminal
    n395["n395: Factor<br/>production = 96"]:::nonTerminal
    n396["n396: ID<br/>lexeme = a"]:::terminal
    n397["n397: __ACT_48<br/>production = 95<br/>semantic action<br/>{ $$ = makeIdentifier($1); }"]:::semanticAction
    n398["n398: __ACT_46<br/>production = 91<br/>semantic action<br/>{ $$ = $1; }"]:::semanticAction
    n399["n399: __ACT_43<br/>production = 85<br/>semantic action<br/>{ $$ = $1; }"]:::semanticAction
    n400["n400: SEMI<br/>lexeme = ;"]:::terminal
    n401["n401: __ACT_33<br/>production = 65<br/>semantic action<br/>{ $$ = makeReturn($2); }"]:::semanticAction
    n402["n402: __ACT_24<br/>production = 47<br/>semantic action<br/>{ $$ = $1; }"]:::semanticAction
    n403["n403: __ACT_20<br/>production = 39<br/>semantic action<br/>{ $$ = $1; }"]:::semanticAction
    n404["n404: __ACT_16<br/>production = 31<br/>semantic action<br/>{ $$ = $1; }"]:::semanticAction
    n405["n405: __ACT_14<br/>production = 27<br/>semantic action<br/>{ $$ = appendItem($1, $2); }"]:::semanticAction
    n406["n406: RBRACE<br/>lexeme = }"]:::terminal
    n407["n407: __ACT_12<br/>production = 23<br/>semantic action<br/>{ $$ = makeBlock($2); }"]:::semanticAction
    n408["n408: __ACT_4<br/>production = 7<br/>semantic action<br/>{ $$ = makeFunction($2, $4, $6); }"]:::semanticAction
    n409["n409: __ACT_2<br/>production = 3<br/>semantic action<br/>{ $$ = appendFunction($1, $2); }"]:::semanticAction
    n410["n410: __ACT_1<br/>production = 1<br/>semantic action<br/>{ $$ = makeProgram($1); }"]:::semanticAction
    n0 --> n1
    n0 --> n410
    n1 --> n2
    n1 --> n60
    n1 --> n409
    n2 --> n3
    n2 --> n59
    n3 --> n4
    n3 --> n5
    n3 --> n8
    n3 --> n9
    n3 --> n24
    n3 --> n25
    n3 --> n58
    n5 --> n6
    n5 --> n7
    n9 --> n10
    n9 --> n23
    n10 --> n11
    n10 --> n17
    n10 --> n18
    n10 --> n22
    n11 --> n12
    n11 --> n16
    n12 --> n13
    n12 --> n14
    n12 --> n15
    n18 --> n19
    n18 --> n20
    n18 --> n21
    n25 --> n26
    n25 --> n27
    n25 --> n56
    n25 --> n57
    n27 --> n28
    n27 --> n30
    n27 --> n55
    n28 --> n29
    n30 --> n31
    n30 --> n54
    n31 --> n32
    n31 --> n53
    n32 --> n33
    n32 --> n52
    n33 --> n34
    n33 --> n35
    n33 --> n50
    n33 --> n51
    n35 --> n36
    n35 --> n43
    n35 --> n44
    n35 --> n49
    n36 --> n37
    n36 --> n42
    n37 --> n38
    n37 --> n41
    n38 --> n39
    n38 --> n40
    n44 --> n45
    n44 --> n48
    n45 --> n46
    n45 --> n47
    n60 --> n61
    n60 --> n62
    n60 --> n65
    n60 --> n66
    n60 --> n68
    n60 --> n69
    n60 --> n408
    n62 --> n63
    n62 --> n64
    n66 --> n67
    n69 --> n70
    n69 --> n71
    n69 --> n406
    n69 --> n407
    n71 --> n72
    n71 --> n388
    n71 --> n405
    n72 --> n73
    n72 --> n304
    n72 --> n387
    n73 --> n74
    n73 --> n266
    n73 --> n303
    n74 --> n75
    n74 --> n154
    n74 --> n265
    n75 --> n76
    n75 --> n108
    n75 --> n153
    n76 --> n77
    n76 --> n90
    n76 --> n107
    n77 --> n78
    n77 --> n80
    n77 --> n89
    n78 --> n79
    n80 --> n81
    n80 --> n88
    n81 --> n82
    n81 --> n83
    n81 --> n84
    n81 --> n86
    n81 --> n87
    n84 --> n85
    n90 --> n91
    n90 --> n106
    n91 --> n92
    n91 --> n93
    n91 --> n94
    n91 --> n104
    n91 --> n105
    n94 --> n95
    n94 --> n96
    n94 --> n103
    n96 --> n97
    n96 --> n102
    n97 --> n98
    n97 --> n101
    n98 --> n99
    n98 --> n100
    n108 --> n109
    n108 --> n152
    n109 --> n110
    n109 --> n151
    n110 --> n111
    n110 --> n150
    n111 --> n112
    n111 --> n113
    n111 --> n114
    n111 --> n148
    n111 --> n149
    n114 --> n115
    n114 --> n147
    n115 --> n116
    n115 --> n146
    n116 --> n117
    n116 --> n145
    n117 --> n118
    n117 --> n121
    n117 --> n122
    n117 --> n143
    n117 --> n144
    n118 --> n119
    n118 --> n120
    n122 --> n123
    n122 --> n142
    n123 --> n124
    n123 --> n133
    n123 --> n134
    n123 --> n141
    n124 --> n125
    n124 --> n132
    n125 --> n126
    n125 --> n131
    n126 --> n127
    n126 --> n130
    n127 --> n128
    n127 --> n129
    n134 --> n135
    n134 --> n140
    n135 --> n136
    n135 --> n139
    n136 --> n137
    n136 --> n138
    n154 --> n155
    n154 --> n264
    n155 --> n156
    n155 --> n263
    n156 --> n157
    n156 --> n158
    n156 --> n159
    n156 --> n178
    n156 --> n179
    n156 --> n220
    n156 --> n221
    n156 --> n262
    n159 --> n160
    n159 --> n167
    n159 --> n170
    n159 --> n177
    n160 --> n161
    n160 --> n166
    n161 --> n162
    n161 --> n165
    n162 --> n163
    n162 --> n164
    n167 --> n168
    n167 --> n169
    n170 --> n171
    n170 --> n176
    n171 --> n172
    n171 --> n175
    n172 --> n173
    n172 --> n174
    n179 --> n180
    n179 --> n219
    n180 --> n181
    n180 --> n182
    n180 --> n183
    n180 --> n217
    n180 --> n218
    n183 --> n184
    n183 --> n216
    n184 --> n185
    n184 --> n215
    n185 --> n186
    n185 --> n214
    n186 --> n187
    n186 --> n190
    n186 --> n191
    n186 --> n212
    n186 --> n213
    n187 --> n188
    n187 --> n189
    n191 --> n192
    n191 --> n211
    n192 --> n193
    n192 --> n202
    n192 --> n203
    n192 --> n210
    n193 --> n194
    n193 --> n201
    n194 --> n195
    n194 --> n200
    n195 --> n196
    n195 --> n199
    n196 --> n197
    n196 --> n198
    n203 --> n204
    n203 --> n209
    n204 --> n205
    n204 --> n208
    n205 --> n206
    n205 --> n207
    n221 --> n222
    n221 --> n261
    n222 --> n223
    n222 --> n224
    n222 --> n225
    n222 --> n259
    n222 --> n260
    n225 --> n226
    n225 --> n258
    n226 --> n227
    n226 --> n257
    n227 --> n228
    n227 --> n256
    n228 --> n229
    n228 --> n232
    n228 --> n233
    n228 --> n254
    n228 --> n255
    n229 --> n230
    n229 --> n231
    n233 --> n234
    n233 --> n253
    n234 --> n235
    n234 --> n244
    n234 --> n245
    n234 --> n252
    n235 --> n236
    n235 --> n243
    n236 --> n237
    n236 --> n242
    n237 --> n238
    n237 --> n241
    n238 --> n239
    n238 --> n240
    n245 --> n246
    n245 --> n251
    n246 --> n247
    n246 --> n250
    n247 --> n248
    n247 --> n249
    n266 --> n267
    n266 --> n302
    n267 --> n268
    n267 --> n301
    n268 --> n269
    n268 --> n300
    n269 --> n270
    n269 --> n298
    n269 --> n299
    n270 --> n271
    n270 --> n274
    n270 --> n275
    n270 --> n296
    n270 --> n297
    n271 --> n272
    n271 --> n273
    n275 --> n276
    n275 --> n295
    n276 --> n277
    n276 --> n286
    n276 --> n287
    n276 --> n294
    n277 --> n278
    n277 --> n285
    n278 --> n279
    n278 --> n284
    n279 --> n280
    n279 --> n283
    n280 --> n281
    n280 --> n282
    n287 --> n288
    n287 --> n293
    n288 --> n289
    n288 --> n292
    n289 --> n290
    n289 --> n291
    n304 --> n305
    n304 --> n386
    n305 --> n306
    n305 --> n385
    n306 --> n307
    n306 --> n308
    n306 --> n309
    n306 --> n328
    n306 --> n329
    n306 --> n384
    n309 --> n310
    n309 --> n317
    n309 --> n320
    n309 --> n327
    n310 --> n311
    n310 --> n316
    n311 --> n312
    n311 --> n315
    n312 --> n313
    n312 --> n314
    n317 --> n318
    n317 --> n319
    n320 --> n321
    n320 --> n326
    n321 --> n322
    n321 --> n325
    n322 --> n323
    n322 --> n324
    n329 --> n330
    n329 --> n383
    n330 --> n331
    n330 --> n332
    n330 --> n381
    n330 --> n382
    n332 --> n333
    n332 --> n335
    n332 --> n380
    n333 --> n334
    n335 --> n336
    n335 --> n379
    n336 --> n337
    n336 --> n378
    n337 --> n338
    n337 --> n377
    n338 --> n339
    n338 --> n340
    n338 --> n341
    n338 --> n375
    n338 --> n376
    n341 --> n342
    n341 --> n374
    n342 --> n343
    n342 --> n373
    n343 --> n344
    n343 --> n372
    n344 --> n345
    n344 --> n348
    n344 --> n349
    n344 --> n370
    n344 --> n371
    n345 --> n346
    n345 --> n347
    n349 --> n350
    n349 --> n369
    n350 --> n351
    n350 --> n360
    n350 --> n361
    n350 --> n368
    n351 --> n352
    n351 --> n359
    n352 --> n353
    n352 --> n358
    n353 --> n354
    n353 --> n357
    n354 --> n355
    n354 --> n356
    n361 --> n362
    n361 --> n367
    n362 --> n363
    n362 --> n366
    n363 --> n364
    n363 --> n365
    n388 --> n389
    n388 --> n404
    n389 --> n390
    n389 --> n403
    n390 --> n391
    n390 --> n402
    n391 --> n392
    n391 --> n393
    n391 --> n400
    n391 --> n401
    n393 --> n394
    n393 --> n399
    n394 --> n395
    n394 --> n398
    n395 --> n396
    n395 --> n397
    classDef semanticAction fill:#fff3cd,stroke:#f39c12,stroke-width:2px
    classDef terminal fill:#e8f4fd,stroke:#2c7fb8
    classDef nonTerminal fill:#eef7ee,stroke:#2e7d32
```

## 4. 语义动作节点列表

### n7 `__ACT_5`

```text
{ $$ = $1; }
```

### n15 `__ACT_11`

```text
{ $$ = makeParam($2); }
```

### n16 `__ACT_10`

```text
{ $$ = makeParamList($1); }
```

### n21 `__ACT_11`

```text
{ $$ = makeParam($2); }
```

### n22 `__ACT_9`

```text
{ $$ = appendParam($1, $3); }
```

### n23 `__ACT_8`

```text
{ $$ = $1; }
```

### n29 `__ACT_13`

```text
{ $$ = makeEmptyItemList(); }
```

### n40 `__ACT_48`

```text
{ $$ = makeIdentifier($1); }
```

### n41 `__ACT_46`

```text
{ $$ = $1; }
```

### n42 `__ACT_43`

```text
{ $$ = $1; }
```

### n47 `__ACT_48`

```text
{ $$ = makeIdentifier($1); }
```

### n48 `__ACT_46`

```text
{ $$ = $1; }
```

### n49 `__ACT_41`

```text
{ $$ = makeBinary("+", $1, $3); }
```

### n51 `__ACT_33`

```text
{ $$ = makeReturn($2); }
```

### n52 `__ACT_24`

```text
{ $$ = $1; }
```

### n53 `__ACT_20`

```text
{ $$ = $1; }
```

### n54 `__ACT_16`

```text
{ $$ = $1; }
```

### n55 `__ACT_14`

```text
{ $$ = appendItem($1, $2); }
```

### n57 `__ACT_12`

```text
{ $$ = makeBlock($2); }
```

### n58 `__ACT_4`

```text
{ $$ = makeFunction($2, $4, $6); }
```

### n59 `__ACT_3`

```text
{ $$ = makeFunctionList($1); }
```

### n64 `__ACT_6`

```text
{ $$ = $1; }
```

### n67 `__ACT_7`

```text
{ $$ = makeEmptyParamList(); }
```

### n79 `__ACT_13`

```text
{ $$ = makeEmptyItemList(); }
```

### n85 `__ACT_18`

```text
{ $$ = makeNoInitializer(); }
```

### n87 `__ACT_17`

```text
{ $$ = makeDeclaration($2, $3); }
```

### n88 `__ACT_15`

```text
{ $$ = $1; }
```

### n89 `__ACT_14`

```text
{ $$ = appendItem($1, $2); }
```

### n100 `__ACT_49`

```text
{ $$ = makeIntLiteral($1); }
```

### n101 `__ACT_46`

```text
{ $$ = $1; }
```

### n102 `__ACT_43`

```text
{ $$ = $1; }
```

### n103 `__ACT_19`

```text
{ $$ = makeInitializer($2); }
```

### n105 `__ACT_17`

```text
{ $$ = makeDeclaration($2, $3); }
```

### n106 `__ACT_15`

```text
{ $$ = $1; }
```

### n107 `__ACT_14`

```text
{ $$ = appendItem($1, $2); }
```

### n120 `__ACT_5`

```text
{ $$ = $1; }
```

### n129 `__ACT_48`

```text
{ $$ = makeIdentifier($1); }
```

### n130 `__ACT_46`

```text
{ $$ = $1; }
```

### n131 `__ACT_43`

```text
{ $$ = $1; }
```

### n132 `__ACT_55`

```text
{ $$ = makeArgList($1); }
```

### n138 `__ACT_49`

```text
{ $$ = makeIntLiteral($1); }
```

### n139 `__ACT_46`

```text
{ $$ = $1; }
```

### n140 `__ACT_43`

```text
{ $$ = $1; }
```

### n141 `__ACT_54`

```text
{ $$ = appendArg($1, $3); }
```

### n142 `__ACT_53`

```text
{ $$ = $1; }
```

### n144 `__ACT_51`

```text
{ $$ = makeCall($1, $3); }
```

### n145 `__ACT_50`

```text
{ $$ = $1; }
```

### n146 `__ACT_46`

```text
{ $$ = $1; }
```

### n147 `__ACT_43`

```text
{ $$ = $1; }
```

### n149 `__ACT_31`

```text
{ $$ = makeAssignment($1, $3); }
```

### n150 `__ACT_22`

```text
{ $$ = $1; }
```

### n151 `__ACT_20`

```text
{ $$ = $1; }
```

### n152 `__ACT_16`

```text
{ $$ = $1; }
```

### n153 `__ACT_14`

```text
{ $$ = appendItem($1, $2); }
```

### n164 `__ACT_48`

```text
{ $$ = makeIdentifier($1); }
```

### n165 `__ACT_46`

```text
{ $$ = $1; }
```

### n166 `__ACT_43`

```text
{ $$ = $1; }
```

### n169 `__ACT_35`

```text
{ $$ = makeRelOp($1); }
```

### n174 `__ACT_48`

```text
{ $$ = makeIdentifier($1); }
```

### n175 `__ACT_46`

```text
{ $$ = $1; }
```

### n176 `__ACT_43`

```text
{ $$ = $1; }
```

### n177 `__ACT_34`

```text
{ $$ = makeCondition($1, $2, $3); }
```

### n189 `__ACT_5`

```text
{ $$ = $1; }
```

### n198 `__ACT_48`

```text
{ $$ = makeIdentifier($1); }
```

### n199 `__ACT_46`

```text
{ $$ = $1; }
```

### n200 `__ACT_43`

```text
{ $$ = $1; }
```

### n201 `__ACT_55`

```text
{ $$ = makeArgList($1); }
```

### n207 `__ACT_49`

```text
{ $$ = makeIntLiteral($1); }
```

### n208 `__ACT_46`

```text
{ $$ = $1; }
```

### n209 `__ACT_43`

```text
{ $$ = $1; }
```

### n210 `__ACT_54`

```text
{ $$ = appendArg($1, $3); }
```

### n211 `__ACT_53`

```text
{ $$ = $1; }
```

### n213 `__ACT_51`

```text
{ $$ = makeCall($1, $3); }
```

### n214 `__ACT_50`

```text
{ $$ = $1; }
```

### n215 `__ACT_46`

```text
{ $$ = $1; }
```

### n216 `__ACT_43`

```text
{ $$ = $1; }
```

### n218 `__ACT_31`

```text
{ $$ = makeAssignment($1, $3); }
```

### n219 `__ACT_22`

```text
{ $$ = $1; }
```

### n231 `__ACT_5`

```text
{ $$ = $1; }
```

### n240 `__ACT_48`

```text
{ $$ = makeIdentifier($1); }
```

### n241 `__ACT_46`

```text
{ $$ = $1; }
```

### n242 `__ACT_43`

```text
{ $$ = $1; }
```

### n243 `__ACT_55`

```text
{ $$ = makeArgList($1); }
```

### n249 `__ACT_48`

```text
{ $$ = makeIdentifier($1); }
```

### n250 `__ACT_46`

```text
{ $$ = $1; }
```

### n251 `__ACT_43`

```text
{ $$ = $1; }
```

### n252 `__ACT_54`

```text
{ $$ = appendArg($1, $3); }
```

### n253 `__ACT_53`

```text
{ $$ = $1; }
```

### n255 `__ACT_51`

```text
{ $$ = makeCall($1, $3); }
```

### n256 `__ACT_50`

```text
{ $$ = $1; }
```

### n257 `__ACT_46`

```text
{ $$ = $1; }
```

### n258 `__ACT_43`

```text
{ $$ = $1; }
```

### n260 `__ACT_31`

```text
{ $$ = makeAssignment($1, $3); }
```

### n261 `__ACT_22`

```text
{ $$ = $1; }
```

### n262 `__ACT_27`

```text
{ $$ = makeIfElse($3, $5, $7); }
```

### n263 `__ACT_20`

```text
{ $$ = $1; }
```

### n264 `__ACT_16`

```text
{ $$ = $1; }
```

### n265 `__ACT_14`

```text
{ $$ = appendItem($1, $2); }
```

### n273 `__ACT_5`

```text
{ $$ = $1; }
```

### n282 `__ACT_48`

```text
{ $$ = makeIdentifier($1); }
```

### n283 `__ACT_46`

```text
{ $$ = $1; }
```

### n284 `__ACT_43`

```text
{ $$ = $1; }
```

### n285 `__ACT_55`

```text
{ $$ = makeArgList($1); }
```

### n291 `__ACT_48`

```text
{ $$ = makeIdentifier($1); }
```

### n292 `__ACT_46`

```text
{ $$ = $1; }
```

### n293 `__ACT_43`

```text
{ $$ = $1; }
```

### n294 `__ACT_54`

```text
{ $$ = appendArg($1, $3); }
```

### n295 `__ACT_53`

```text
{ $$ = $1; }
```

### n297 `__ACT_51`

```text
{ $$ = makeCall($1, $3); }
```

### n299 `__ACT_32`

```text
{ $$ = makeExprStmt($1); }
```

### n300 `__ACT_23`

```text
{ $$ = $1; }
```

### n301 `__ACT_20`

```text
{ $$ = $1; }
```

### n302 `__ACT_16`

```text
{ $$ = $1; }
```

### n303 `__ACT_14`

```text
{ $$ = appendItem($1, $2); }
```

### n314 `__ACT_48`

```text
{ $$ = makeIdentifier($1); }
```

### n315 `__ACT_46`

```text
{ $$ = $1; }
```

### n316 `__ACT_43`

```text
{ $$ = $1; }
```

### n319 `__ACT_40`

```text
{ $$ = makeRelOp($1); }
```

### n324 `__ACT_48`

```text
{ $$ = makeIdentifier($1); }
```

### n325 `__ACT_46`

```text
{ $$ = $1; }
```

### n326 `__ACT_43`

```text
{ $$ = $1; }
```

### n327 `__ACT_34`

```text
{ $$ = makeCondition($1, $2, $3); }
```

### n334 `__ACT_13`

```text
{ $$ = makeEmptyItemList(); }
```

### n347 `__ACT_5`

```text
{ $$ = $1; }
```

### n356 `__ACT_48`

```text
{ $$ = makeIdentifier($1); }
```

### n357 `__ACT_46`

```text
{ $$ = $1; }
```

### n358 `__ACT_43`

```text
{ $$ = $1; }
```

### n359 `__ACT_55`

```text
{ $$ = makeArgList($1); }
```

### n365 `__ACT_49`

```text
{ $$ = makeIntLiteral($1); }
```

### n366 `__ACT_46`

```text
{ $$ = $1; }
```

### n367 `__ACT_43`

```text
{ $$ = $1; }
```

### n368 `__ACT_54`

```text
{ $$ = appendArg($1, $3); }
```

### n369 `__ACT_53`

```text
{ $$ = $1; }
```

### n371 `__ACT_51`

```text
{ $$ = makeCall($1, $3); }
```

### n372 `__ACT_50`

```text
{ $$ = $1; }
```

### n373 `__ACT_46`

```text
{ $$ = $1; }
```

### n374 `__ACT_43`

```text
{ $$ = $1; }
```

### n376 `__ACT_31`

```text
{ $$ = makeAssignment($1, $3); }
```

### n377 `__ACT_22`

```text
{ $$ = $1; }
```

### n378 `__ACT_20`

```text
{ $$ = $1; }
```

### n379 `__ACT_16`

```text
{ $$ = $1; }
```

### n380 `__ACT_14`

```text
{ $$ = appendItem($1, $2); }
```

### n382 `__ACT_12`

```text
{ $$ = makeBlock($2); }
```

### n383 `__ACT_25`

```text
{ $$ = $1; }
```

### n384 `__ACT_26`

```text
{ $$ = makeWhile($3, $5); }
```

### n385 `__ACT_20`

```text
{ $$ = $1; }
```

### n386 `__ACT_16`

```text
{ $$ = $1; }
```

### n387 `__ACT_14`

```text
{ $$ = appendItem($1, $2); }
```

### n397 `__ACT_48`

```text
{ $$ = makeIdentifier($1); }
```

### n398 `__ACT_46`

```text
{ $$ = $1; }
```

### n399 `__ACT_43`

```text
{ $$ = $1; }
```

### n401 `__ACT_33`

```text
{ $$ = makeReturn($2); }
```

### n402 `__ACT_24`

```text
{ $$ = $1; }
```

### n403 `__ACT_20`

```text
{ $$ = $1; }
```

### n404 `__ACT_16`

```text
{ $$ = $1; }
```

### n405 `__ACT_14`

```text
{ $$ = appendItem($1, $2); }
```

### n407 `__ACT_12`

```text
{ $$ = makeBlock($2); }
```

### n408 `__ACT_4`

```text
{ $$ = makeFunction($2, $4, $6); }
```

### n409 `__ACT_2`

```text
{ $$ = appendFunction($1, $2); }
```

### n410 `__ACT_1`

```text
{ $$ = makeProgram($1); }
```

