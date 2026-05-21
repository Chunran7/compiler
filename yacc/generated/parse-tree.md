# C99 Parse Tree

## 1. 语法树节点数据结构

本节以表格形式展示语法树节点的数据结构，包括节点编号、符号名、词素值、产生式编号、孩子节点以及是否为语义动作节点。

| 节点ID | 节点类型 | 符号名 | 词素值 | 产生式编号 | 孩子节点 | 语义动作代码预览 |
|---|---|---|---|---:|---|---|
| n0 | NON_TERMINAL | translation_unit | - | 231 | n1, n66 | - |
| n1 | NON_TERMINAL | translation_unit | - | 230 | n2 | - |
| n2 | NON_TERMINAL | external_declaration | - | 232 | n3 | - |
| n3 | NON_TERMINAL | function_definition | - | 235 | n4, n7, n31 | - |
| n4 | NON_TERMINAL | declaration_specifiers | - | 81 | n5 | - |
| n5 | NON_TERMINAL | type_specifier | - | 99 | n6 | - |
| n6 | TERMINAL_LEAF | INT | int | -1 | - | - |
| n7 | NON_TERMINAL | declarator | - | 142 | n8 | - |
| n8 | NON_TERMINAL | direct_declarator | - | 153 | n9, n11, n12, n30 | - |
| n9 | NON_TERMINAL | direct_declarator | - | 143 | n10 | - |
| n10 | TERMINAL_LEAF | IDENTIFIER | add | -1 | - | - |
| n11 | TERMINAL_LEAF | LPAREN | ( | -1 | - | - |
| n12 | NON_TERMINAL | parameter_type_list | - | 162 | n13 | - |
| n13 | NON_TERMINAL | parameter_list | - | 165 | n14, n22, n23 | - |
| n14 | NON_TERMINAL | parameter_list | - | 164 | n15 | - |
| n15 | NON_TERMINAL | parameter_declaration | - | 166 | n16, n19 | - |
| n16 | NON_TERMINAL | declaration_specifiers | - | 81 | n17 | - |
| n17 | NON_TERMINAL | type_specifier | - | 99 | n18 | - |
| n18 | TERMINAL_LEAF | INT | int | -1 | - | - |
| n19 | NON_TERMINAL | declarator | - | 142 | n20 | - |
| n20 | NON_TERMINAL | direct_declarator | - | 143 | n21 | - |
| n21 | TERMINAL_LEAF | IDENTIFIER | x | -1 | - | - |
| n22 | TERMINAL_LEAF | COMMA | , | -1 | - | - |
| n23 | NON_TERMINAL | parameter_declaration | - | 166 | n24, n27 | - |
| n24 | NON_TERMINAL | declaration_specifiers | - | 81 | n25 | - |
| n25 | NON_TERMINAL | type_specifier | - | 99 | n26 | - |
| n26 | TERMINAL_LEAF | INT | int | -1 | - | - |
| n27 | NON_TERMINAL | declarator | - | 142 | n28 | - |
| n28 | NON_TERMINAL | direct_declarator | - | 143 | n29 | - |
| n29 | TERMINAL_LEAF | IDENTIFIER | y | -1 | - | - |
| n30 | TERMINAL_LEAF | RPAREN | ) | -1 | - | - |
| n31 | NON_TERMINAL | compound_statement | - | 209 | n32, n33, n65 | - |
| n32 | TERMINAL_LEAF | LBRACE | { | -1 | - | - |
| n33 | NON_TERMINAL | block_item_list | - | 210 | n34 | - |
| n34 | NON_TERMINAL | block_item | - | 213 | n35 | - |
| n35 | NON_TERMINAL | statement | - | 204 | n36 | - |
| n36 | NON_TERMINAL | jump_statement | - | 229 | n37, n38, n64 | - |
| n37 | TERMINAL_LEAF | RETURN | return | -1 | - | - |
| n38 | NON_TERMINAL | expression | - | 74 | n39 | - |
| n39 | NON_TERMINAL | assignment_expression | - | 61 | n40 | - |
| n40 | NON_TERMINAL | conditional_expression | - | 59 | n41 | - |
| n41 | NON_TERMINAL | logical_or_expression | - | 57 | n42 | - |
| n42 | NON_TERMINAL | logical_and_expression | - | 55 | n43 | - |
| n43 | NON_TERMINAL | inclusive_or_expression | - | 53 | n44 | - |
| n44 | NON_TERMINAL | exclusive_or_expression | - | 51 | n45 | - |
| n45 | NON_TERMINAL | and_expression | - | 49 | n46 | - |
| n46 | NON_TERMINAL | equality_expression | - | 46 | n47 | - |
| n47 | NON_TERMINAL | relational_expression | - | 41 | n48 | - |
| n48 | NON_TERMINAL | shift_expression | - | 38 | n49 | - |
| n49 | NON_TERMINAL | additive_expression | - | 36 | n50, n57, n58 | - |
| n50 | NON_TERMINAL | additive_expression | - | 35 | n51 | - |
| n51 | NON_TERMINAL | multiplicative_expression | - | 31 | n52 | - |
| n52 | NON_TERMINAL | cast_expression | - | 29 | n53 | - |
| n53 | NON_TERMINAL | unary_expression | - | 17 | n54 | - |
| n54 | NON_TERMINAL | postfix_expression | - | 5 | n55 | - |
| n55 | NON_TERMINAL | primary_expression | - | 1 | n56 | - |
| n56 | TERMINAL_LEAF | IDENTIFIER | x | -1 | - | - |
| n57 | TERMINAL_LEAF | PLUS | + | -1 | - | - |
| n58 | NON_TERMINAL | multiplicative_expression | - | 31 | n59 | - |
| n59 | NON_TERMINAL | cast_expression | - | 29 | n60 | - |
| n60 | NON_TERMINAL | unary_expression | - | 17 | n61 | - |
| n61 | NON_TERMINAL | postfix_expression | - | 5 | n62 | - |
| n62 | NON_TERMINAL | primary_expression | - | 1 | n63 | - |
| n63 | TERMINAL_LEAF | IDENTIFIER | y | -1 | - | - |
| n64 | TERMINAL_LEAF | SEMI | ; | -1 | - | - |
| n65 | TERMINAL_LEAF | RBRACE | } | -1 | - | - |
| n66 | NON_TERMINAL | external_declaration | - | 232 | n67 | - |
| n67 | NON_TERMINAL | function_definition | - | 235 | n68, n71, n77 | - |
| n68 | NON_TERMINAL | declaration_specifiers | - | 81 | n69 | - |
| n69 | NON_TERMINAL | type_specifier | - | 99 | n70 | - |
| n70 | TERMINAL_LEAF | INT | int | -1 | - | - |
| n71 | NON_TERMINAL | declarator | - | 142 | n72 | - |
| n72 | NON_TERMINAL | direct_declarator | - | 155 | n73, n75, n76 | - |
| n73 | NON_TERMINAL | direct_declarator | - | 143 | n74 | - |
| n74 | TERMINAL_LEAF | IDENTIFIER | main | -1 | - | - |
| n75 | TERMINAL_LEAF | LPAREN | ( | -1 | - | - |
| n76 | TERMINAL_LEAF | RPAREN | ) | -1 | - | - |
| n77 | NON_TERMINAL | compound_statement | - | 209 | n78, n79, n561 | - |
| n78 | TERMINAL_LEAF | LBRACE | { | -1 | - | - |
| n79 | NON_TERMINAL | block_item_list | - | 211 | n80, n538 | - |
| n80 | NON_TERMINAL | block_item_list | - | 211 | n81, n429 | - |
| n81 | NON_TERMINAL | block_item_list | - | 211 | n82, n367 | - |
| n82 | NON_TERMINAL | block_item_list | - | 211 | n83, n196 | - |
| n83 | NON_TERMINAL | block_item_list | - | 211 | n84, n127 | - |
| n84 | NON_TERMINAL | block_item_list | - | 211 | n85, n97 | - |
| n85 | NON_TERMINAL | block_item_list | - | 210 | n86 | - |
| n86 | NON_TERMINAL | block_item | - | 212 | n87 | - |
| n87 | NON_TERMINAL | declaration | - | 78 | n88, n91, n96 | - |
| n88 | NON_TERMINAL | declaration_specifiers | - | 81 | n89 | - |
| n89 | NON_TERMINAL | type_specifier | - | 99 | n90 | - |
| n90 | TERMINAL_LEAF | INT | int | -1 | - | - |
| n91 | NON_TERMINAL | init_declarator_list | - | 87 | n92 | - |
| n92 | NON_TERMINAL | init_declarator | - | 89 | n93 | - |
| n93 | NON_TERMINAL | declarator | - | 142 | n94 | - |
| n94 | NON_TERMINAL | direct_declarator | - | 143 | n95 | - |
| n95 | TERMINAL_LEAF | IDENTIFIER | a | -1 | - | - |
| n96 | TERMINAL_LEAF | SEMI | ; | -1 | - | - |
| n97 | NON_TERMINAL | block_item | - | 212 | n98 | - |
| n98 | NON_TERMINAL | declaration | - | 78 | n99, n102, n126 | - |
| n99 | NON_TERMINAL | declaration_specifiers | - | 81 | n100 | - |
| n100 | NON_TERMINAL | type_specifier | - | 99 | n101 | - |
| n101 | TERMINAL_LEAF | INT | int | -1 | - | - |
| n102 | NON_TERMINAL | init_declarator_list | - | 87 | n103 | - |
| n103 | NON_TERMINAL | init_declarator | - | 90 | n104, n107, n108 | - |
| n104 | NON_TERMINAL | declarator | - | 142 | n105 | - |
| n105 | NON_TERMINAL | direct_declarator | - | 143 | n106 | - |
| n106 | TERMINAL_LEAF | IDENTIFIER | b | -1 | - | - |
| n107 | TERMINAL_LEAF | ASSIGN | = | -1 | - | - |
| n108 | NON_TERMINAL | initializer | - | 187 | n109 | - |
| n109 | NON_TERMINAL | assignment_expression | - | 61 | n110 | - |
| n110 | NON_TERMINAL | conditional_expression | - | 59 | n111 | - |
| n111 | NON_TERMINAL | logical_or_expression | - | 57 | n112 | - |
| n112 | NON_TERMINAL | logical_and_expression | - | 55 | n113 | - |
| n113 | NON_TERMINAL | inclusive_or_expression | - | 53 | n114 | - |
| n114 | NON_TERMINAL | exclusive_or_expression | - | 51 | n115 | - |
| n115 | NON_TERMINAL | and_expression | - | 49 | n116 | - |
| n116 | NON_TERMINAL | equality_expression | - | 46 | n117 | - |
| n117 | NON_TERMINAL | relational_expression | - | 41 | n118 | - |
| n118 | NON_TERMINAL | shift_expression | - | 38 | n119 | - |
| n119 | NON_TERMINAL | additive_expression | - | 35 | n120 | - |
| n120 | NON_TERMINAL | multiplicative_expression | - | 31 | n121 | - |
| n121 | NON_TERMINAL | cast_expression | - | 29 | n122 | - |
| n122 | NON_TERMINAL | unary_expression | - | 17 | n123 | - |
| n123 | NON_TERMINAL | postfix_expression | - | 5 | n124 | - |
| n124 | NON_TERMINAL | primary_expression | - | 2 | n125 | - |
| n125 | TERMINAL_LEAF | CONSTANT | 5 | -1 | - | - |
| n126 | TERMINAL_LEAF | SEMI | ; | -1 | - | - |
| n127 | NON_TERMINAL | block_item | - | 213 | n128 | - |
| n128 | NON_TERMINAL | statement | - | 201 | n129 | - |
| n129 | NON_TERMINAL | expression_statement | - | 215 | n130, n195 | - |
| n130 | NON_TERMINAL | expression | - | 74 | n131 | - |
| n131 | NON_TERMINAL | assignment_expression | - | 62 | n132, n136, n138 | - |
| n132 | NON_TERMINAL | unary_expression | - | 17 | n133 | - |
| n133 | NON_TERMINAL | postfix_expression | - | 5 | n134 | - |
| n134 | NON_TERMINAL | primary_expression | - | 1 | n135 | - |
| n135 | TERMINAL_LEAF | IDENTIFIER | a | -1 | - | - |
| n136 | NON_TERMINAL | assignment_operator | - | 63 | n137 | - |
| n137 | TERMINAL_LEAF | ASSIGN | = | -1 | - | - |
| n138 | NON_TERMINAL | assignment_expression | - | 61 | n139 | - |
| n139 | NON_TERMINAL | conditional_expression | - | 59 | n140 | - |
| n140 | NON_TERMINAL | logical_or_expression | - | 57 | n141 | - |
| n141 | NON_TERMINAL | logical_and_expression | - | 55 | n142 | - |
| n142 | NON_TERMINAL | inclusive_or_expression | - | 53 | n143 | - |
| n143 | NON_TERMINAL | exclusive_or_expression | - | 51 | n144 | - |
| n144 | NON_TERMINAL | and_expression | - | 49 | n145 | - |
| n145 | NON_TERMINAL | equality_expression | - | 46 | n146 | - |
| n146 | NON_TERMINAL | relational_expression | - | 41 | n147 | - |
| n147 | NON_TERMINAL | shift_expression | - | 38 | n148 | - |
| n148 | NON_TERMINAL | additive_expression | - | 35 | n149 | - |
| n149 | NON_TERMINAL | multiplicative_expression | - | 31 | n150 | - |
| n150 | NON_TERMINAL | cast_expression | - | 29 | n151 | - |
| n151 | NON_TERMINAL | unary_expression | - | 17 | n152 | - |
| n152 | NON_TERMINAL | postfix_expression | - | 8 | n153, n156, n157, n194 | - |
| n153 | NON_TERMINAL | postfix_expression | - | 5 | n154 | - |
| n154 | NON_TERMINAL | primary_expression | - | 1 | n155 | - |
| n155 | TERMINAL_LEAF | IDENTIFIER | add | -1 | - | - |
| n156 | TERMINAL_LEAF | LPAREN | ( | -1 | - | - |
| n157 | NON_TERMINAL | argument_expression_list | - | 16 | n158, n176, n177 | - |
| n158 | NON_TERMINAL | argument_expression_list | - | 15 | n159 | - |
| n159 | NON_TERMINAL | assignment_expression | - | 61 | n160 | - |
| n160 | NON_TERMINAL | conditional_expression | - | 59 | n161 | - |
| n161 | NON_TERMINAL | logical_or_expression | - | 57 | n162 | - |
| n162 | NON_TERMINAL | logical_and_expression | - | 55 | n163 | - |
| n163 | NON_TERMINAL | inclusive_or_expression | - | 53 | n164 | - |
| n164 | NON_TERMINAL | exclusive_or_expression | - | 51 | n165 | - |
| n165 | NON_TERMINAL | and_expression | - | 49 | n166 | - |
| n166 | NON_TERMINAL | equality_expression | - | 46 | n167 | - |
| n167 | NON_TERMINAL | relational_expression | - | 41 | n168 | - |
| n168 | NON_TERMINAL | shift_expression | - | 38 | n169 | - |
| n169 | NON_TERMINAL | additive_expression | - | 35 | n170 | - |
| n170 | NON_TERMINAL | multiplicative_expression | - | 31 | n171 | - |
| n171 | NON_TERMINAL | cast_expression | - | 29 | n172 | - |
| n172 | NON_TERMINAL | unary_expression | - | 17 | n173 | - |
| n173 | NON_TERMINAL | postfix_expression | - | 5 | n174 | - |
| n174 | NON_TERMINAL | primary_expression | - | 1 | n175 | - |
| n175 | TERMINAL_LEAF | IDENTIFIER | b | -1 | - | - |
| n176 | TERMINAL_LEAF | COMMA | , | -1 | - | - |
| n177 | NON_TERMINAL | assignment_expression | - | 61 | n178 | - |
| n178 | NON_TERMINAL | conditional_expression | - | 59 | n179 | - |
| n179 | NON_TERMINAL | logical_or_expression | - | 57 | n180 | - |
| n180 | NON_TERMINAL | logical_and_expression | - | 55 | n181 | - |
| n181 | NON_TERMINAL | inclusive_or_expression | - | 53 | n182 | - |
| n182 | NON_TERMINAL | exclusive_or_expression | - | 51 | n183 | - |
| n183 | NON_TERMINAL | and_expression | - | 49 | n184 | - |
| n184 | NON_TERMINAL | equality_expression | - | 46 | n185 | - |
| n185 | NON_TERMINAL | relational_expression | - | 41 | n186 | - |
| n186 | NON_TERMINAL | shift_expression | - | 38 | n187 | - |
| n187 | NON_TERMINAL | additive_expression | - | 35 | n188 | - |
| n188 | NON_TERMINAL | multiplicative_expression | - | 31 | n189 | - |
| n189 | NON_TERMINAL | cast_expression | - | 29 | n190 | - |
| n190 | NON_TERMINAL | unary_expression | - | 17 | n191 | - |
| n191 | NON_TERMINAL | postfix_expression | - | 5 | n192 | - |
| n192 | NON_TERMINAL | primary_expression | - | 2 | n193 | - |
| n193 | TERMINAL_LEAF | CONSTANT | 3 | -1 | - | - |
| n194 | TERMINAL_LEAF | RPAREN | ) | -1 | - | - |
| n195 | TERMINAL_LEAF | SEMI | ; | -1 | - | - |
| n196 | NON_TERMINAL | block_item | - | 213 | n197 | - |
| n197 | NON_TERMINAL | statement | - | 202 | n198 | - |
| n198 | NON_TERMINAL | selection_statement | - | 217 | n199, n200, n201, n229, n230, n298, n299 | - |
| n199 | TERMINAL_LEAF | IF | if | -1 | - | - |
| n200 | TERMINAL_LEAF | LPAREN | ( | -1 | - | - |
| n201 | NON_TERMINAL | expression | - | 74 | n202 | - |
| n202 | NON_TERMINAL | assignment_expression | - | 61 | n203 | - |
| n203 | NON_TERMINAL | conditional_expression | - | 59 | n204 | - |
| n204 | NON_TERMINAL | logical_or_expression | - | 57 | n205 | - |
| n205 | NON_TERMINAL | logical_and_expression | - | 55 | n206 | - |
| n206 | NON_TERMINAL | inclusive_or_expression | - | 53 | n207 | - |
| n207 | NON_TERMINAL | exclusive_or_expression | - | 51 | n208 | - |
| n208 | NON_TERMINAL | and_expression | - | 49 | n209 | - |
| n209 | NON_TERMINAL | equality_expression | - | 46 | n210 | - |
| n210 | NON_TERMINAL | relational_expression | - | 42 | n211, n220, n221 | - |
| n211 | NON_TERMINAL | relational_expression | - | 41 | n212 | - |
| n212 | NON_TERMINAL | shift_expression | - | 38 | n213 | - |
| n213 | NON_TERMINAL | additive_expression | - | 35 | n214 | - |
| n214 | NON_TERMINAL | multiplicative_expression | - | 31 | n215 | - |
| n215 | NON_TERMINAL | cast_expression | - | 29 | n216 | - |
| n216 | NON_TERMINAL | unary_expression | - | 17 | n217 | - |
| n217 | NON_TERMINAL | postfix_expression | - | 5 | n218 | - |
| n218 | NON_TERMINAL | primary_expression | - | 1 | n219 | - |
| n219 | TERMINAL_LEAF | IDENTIFIER | a | -1 | - | - |
| n220 | TERMINAL_LEAF | LT | < | -1 | - | - |
| n221 | NON_TERMINAL | shift_expression | - | 38 | n222 | - |
| n222 | NON_TERMINAL | additive_expression | - | 35 | n223 | - |
| n223 | NON_TERMINAL | multiplicative_expression | - | 31 | n224 | - |
| n224 | NON_TERMINAL | cast_expression | - | 29 | n225 | - |
| n225 | NON_TERMINAL | unary_expression | - | 17 | n226 | - |
| n226 | NON_TERMINAL | postfix_expression | - | 5 | n227 | - |
| n227 | NON_TERMINAL | primary_expression | - | 1 | n228 | - |
| n228 | TERMINAL_LEAF | IDENTIFIER | b | -1 | - | - |
| n229 | TERMINAL_LEAF | RPAREN | ) | -1 | - | - |
| n230 | NON_TERMINAL | statement | - | 201 | n231 | - |
| n231 | NON_TERMINAL | expression_statement | - | 215 | n232, n297 | - |
| n232 | NON_TERMINAL | expression | - | 74 | n233 | - |
| n233 | NON_TERMINAL | assignment_expression | - | 62 | n234, n238, n240 | - |
| n234 | NON_TERMINAL | unary_expression | - | 17 | n235 | - |
| n235 | NON_TERMINAL | postfix_expression | - | 5 | n236 | - |
| n236 | NON_TERMINAL | primary_expression | - | 1 | n237 | - |
| n237 | TERMINAL_LEAF | IDENTIFIER | a | -1 | - | - |
| n238 | NON_TERMINAL | assignment_operator | - | 63 | n239 | - |
| n239 | TERMINAL_LEAF | ASSIGN | = | -1 | - | - |
| n240 | NON_TERMINAL | assignment_expression | - | 61 | n241 | - |
| n241 | NON_TERMINAL | conditional_expression | - | 59 | n242 | - |
| n242 | NON_TERMINAL | logical_or_expression | - | 57 | n243 | - |
| n243 | NON_TERMINAL | logical_and_expression | - | 55 | n244 | - |
| n244 | NON_TERMINAL | inclusive_or_expression | - | 53 | n245 | - |
| n245 | NON_TERMINAL | exclusive_or_expression | - | 51 | n246 | - |
| n246 | NON_TERMINAL | and_expression | - | 49 | n247 | - |
| n247 | NON_TERMINAL | equality_expression | - | 46 | n248 | - |
| n248 | NON_TERMINAL | relational_expression | - | 41 | n249 | - |
| n249 | NON_TERMINAL | shift_expression | - | 38 | n250 | - |
| n250 | NON_TERMINAL | additive_expression | - | 35 | n251 | - |
| n251 | NON_TERMINAL | multiplicative_expression | - | 31 | n252 | - |
| n252 | NON_TERMINAL | cast_expression | - | 29 | n253 | - |
| n253 | NON_TERMINAL | unary_expression | - | 17 | n254 | - |
| n254 | NON_TERMINAL | postfix_expression | - | 8 | n255, n258, n259, n296 | - |
| n255 | NON_TERMINAL | postfix_expression | - | 5 | n256 | - |
| n256 | NON_TERMINAL | primary_expression | - | 1 | n257 | - |
| n257 | TERMINAL_LEAF | IDENTIFIER | add | -1 | - | - |
| n258 | TERMINAL_LEAF | LPAREN | ( | -1 | - | - |
| n259 | NON_TERMINAL | argument_expression_list | - | 16 | n260, n278, n279 | - |
| n260 | NON_TERMINAL | argument_expression_list | - | 15 | n261 | - |
| n261 | NON_TERMINAL | assignment_expression | - | 61 | n262 | - |
| n262 | NON_TERMINAL | conditional_expression | - | 59 | n263 | - |
| n263 | NON_TERMINAL | logical_or_expression | - | 57 | n264 | - |
| n264 | NON_TERMINAL | logical_and_expression | - | 55 | n265 | - |
| n265 | NON_TERMINAL | inclusive_or_expression | - | 53 | n266 | - |
| n266 | NON_TERMINAL | exclusive_or_expression | - | 51 | n267 | - |
| n267 | NON_TERMINAL | and_expression | - | 49 | n268 | - |
| n268 | NON_TERMINAL | equality_expression | - | 46 | n269 | - |
| n269 | NON_TERMINAL | relational_expression | - | 41 | n270 | - |
| n270 | NON_TERMINAL | shift_expression | - | 38 | n271 | - |
| n271 | NON_TERMINAL | additive_expression | - | 35 | n272 | - |
| n272 | NON_TERMINAL | multiplicative_expression | - | 31 | n273 | - |
| n273 | NON_TERMINAL | cast_expression | - | 29 | n274 | - |
| n274 | NON_TERMINAL | unary_expression | - | 17 | n275 | - |
| n275 | NON_TERMINAL | postfix_expression | - | 5 | n276 | - |
| n276 | NON_TERMINAL | primary_expression | - | 1 | n277 | - |
| n277 | TERMINAL_LEAF | IDENTIFIER | a | -1 | - | - |
| n278 | TERMINAL_LEAF | COMMA | , | -1 | - | - |
| n279 | NON_TERMINAL | assignment_expression | - | 61 | n280 | - |
| n280 | NON_TERMINAL | conditional_expression | - | 59 | n281 | - |
| n281 | NON_TERMINAL | logical_or_expression | - | 57 | n282 | - |
| n282 | NON_TERMINAL | logical_and_expression | - | 55 | n283 | - |
| n283 | NON_TERMINAL | inclusive_or_expression | - | 53 | n284 | - |
| n284 | NON_TERMINAL | exclusive_or_expression | - | 51 | n285 | - |
| n285 | NON_TERMINAL | and_expression | - | 49 | n286 | - |
| n286 | NON_TERMINAL | equality_expression | - | 46 | n287 | - |
| n287 | NON_TERMINAL | relational_expression | - | 41 | n288 | - |
| n288 | NON_TERMINAL | shift_expression | - | 38 | n289 | - |
| n289 | NON_TERMINAL | additive_expression | - | 35 | n290 | - |
| n290 | NON_TERMINAL | multiplicative_expression | - | 31 | n291 | - |
| n291 | NON_TERMINAL | cast_expression | - | 29 | n292 | - |
| n292 | NON_TERMINAL | unary_expression | - | 17 | n293 | - |
| n293 | NON_TERMINAL | postfix_expression | - | 5 | n294 | - |
| n294 | NON_TERMINAL | primary_expression | - | 2 | n295 | - |
| n295 | TERMINAL_LEAF | CONSTANT | 1 | -1 | - | - |
| n296 | TERMINAL_LEAF | RPAREN | ) | -1 | - | - |
| n297 | TERMINAL_LEAF | SEMI | ; | -1 | - | - |
| n298 | TERMINAL_LEAF | ELSE | else | -1 | - | - |
| n299 | NON_TERMINAL | statement | - | 201 | n300 | - |
| n300 | NON_TERMINAL | expression_statement | - | 215 | n301, n366 | - |
| n301 | NON_TERMINAL | expression | - | 74 | n302 | - |
| n302 | NON_TERMINAL | assignment_expression | - | 62 | n303, n307, n309 | - |
| n303 | NON_TERMINAL | unary_expression | - | 17 | n304 | - |
| n304 | NON_TERMINAL | postfix_expression | - | 5 | n305 | - |
| n305 | NON_TERMINAL | primary_expression | - | 1 | n306 | - |
| n306 | TERMINAL_LEAF | IDENTIFIER | a | -1 | - | - |
| n307 | NON_TERMINAL | assignment_operator | - | 63 | n308 | - |
| n308 | TERMINAL_LEAF | ASSIGN | = | -1 | - | - |
| n309 | NON_TERMINAL | assignment_expression | - | 61 | n310 | - |
| n310 | NON_TERMINAL | conditional_expression | - | 59 | n311 | - |
| n311 | NON_TERMINAL | logical_or_expression | - | 57 | n312 | - |
| n312 | NON_TERMINAL | logical_and_expression | - | 55 | n313 | - |
| n313 | NON_TERMINAL | inclusive_or_expression | - | 53 | n314 | - |
| n314 | NON_TERMINAL | exclusive_or_expression | - | 51 | n315 | - |
| n315 | NON_TERMINAL | and_expression | - | 49 | n316 | - |
| n316 | NON_TERMINAL | equality_expression | - | 46 | n317 | - |
| n317 | NON_TERMINAL | relational_expression | - | 41 | n318 | - |
| n318 | NON_TERMINAL | shift_expression | - | 38 | n319 | - |
| n319 | NON_TERMINAL | additive_expression | - | 35 | n320 | - |
| n320 | NON_TERMINAL | multiplicative_expression | - | 31 | n321 | - |
| n321 | NON_TERMINAL | cast_expression | - | 29 | n322 | - |
| n322 | NON_TERMINAL | unary_expression | - | 17 | n323 | - |
| n323 | NON_TERMINAL | postfix_expression | - | 8 | n324, n327, n328, n365 | - |
| n324 | NON_TERMINAL | postfix_expression | - | 5 | n325 | - |
| n325 | NON_TERMINAL | primary_expression | - | 1 | n326 | - |
| n326 | TERMINAL_LEAF | IDENTIFIER | add | -1 | - | - |
| n327 | TERMINAL_LEAF | LPAREN | ( | -1 | - | - |
| n328 | NON_TERMINAL | argument_expression_list | - | 16 | n329, n347, n348 | - |
| n329 | NON_TERMINAL | argument_expression_list | - | 15 | n330 | - |
| n330 | NON_TERMINAL | assignment_expression | - | 61 | n331 | - |
| n331 | NON_TERMINAL | conditional_expression | - | 59 | n332 | - |
| n332 | NON_TERMINAL | logical_or_expression | - | 57 | n333 | - |
| n333 | NON_TERMINAL | logical_and_expression | - | 55 | n334 | - |
| n334 | NON_TERMINAL | inclusive_or_expression | - | 53 | n335 | - |
| n335 | NON_TERMINAL | exclusive_or_expression | - | 51 | n336 | - |
| n336 | NON_TERMINAL | and_expression | - | 49 | n337 | - |
| n337 | NON_TERMINAL | equality_expression | - | 46 | n338 | - |
| n338 | NON_TERMINAL | relational_expression | - | 41 | n339 | - |
| n339 | NON_TERMINAL | shift_expression | - | 38 | n340 | - |
| n340 | NON_TERMINAL | additive_expression | - | 35 | n341 | - |
| n341 | NON_TERMINAL | multiplicative_expression | - | 31 | n342 | - |
| n342 | NON_TERMINAL | cast_expression | - | 29 | n343 | - |
| n343 | NON_TERMINAL | unary_expression | - | 17 | n344 | - |
| n344 | NON_TERMINAL | postfix_expression | - | 5 | n345 | - |
| n345 | NON_TERMINAL | primary_expression | - | 1 | n346 | - |
| n346 | TERMINAL_LEAF | IDENTIFIER | a | -1 | - | - |
| n347 | TERMINAL_LEAF | COMMA | , | -1 | - | - |
| n348 | NON_TERMINAL | assignment_expression | - | 61 | n349 | - |
| n349 | NON_TERMINAL | conditional_expression | - | 59 | n350 | - |
| n350 | NON_TERMINAL | logical_or_expression | - | 57 | n351 | - |
| n351 | NON_TERMINAL | logical_and_expression | - | 55 | n352 | - |
| n352 | NON_TERMINAL | inclusive_or_expression | - | 53 | n353 | - |
| n353 | NON_TERMINAL | exclusive_or_expression | - | 51 | n354 | - |
| n354 | NON_TERMINAL | and_expression | - | 49 | n355 | - |
| n355 | NON_TERMINAL | equality_expression | - | 46 | n356 | - |
| n356 | NON_TERMINAL | relational_expression | - | 41 | n357 | - |
| n357 | NON_TERMINAL | shift_expression | - | 38 | n358 | - |
| n358 | NON_TERMINAL | additive_expression | - | 35 | n359 | - |
| n359 | NON_TERMINAL | multiplicative_expression | - | 31 | n360 | - |
| n360 | NON_TERMINAL | cast_expression | - | 29 | n361 | - |
| n361 | NON_TERMINAL | unary_expression | - | 17 | n362 | - |
| n362 | NON_TERMINAL | postfix_expression | - | 5 | n363 | - |
| n363 | NON_TERMINAL | primary_expression | - | 1 | n364 | - |
| n364 | TERMINAL_LEAF | IDENTIFIER | b | -1 | - | - |
| n365 | TERMINAL_LEAF | RPAREN | ) | -1 | - | - |
| n366 | TERMINAL_LEAF | SEMI | ; | -1 | - | - |
| n367 | NON_TERMINAL | block_item | - | 213 | n368 | - |
| n368 | NON_TERMINAL | statement | - | 201 | n369 | - |
| n369 | NON_TERMINAL | expression_statement | - | 215 | n370, n428 | - |
| n370 | NON_TERMINAL | expression | - | 74 | n371 | - |
| n371 | NON_TERMINAL | assignment_expression | - | 61 | n372 | - |
| n372 | NON_TERMINAL | conditional_expression | - | 59 | n373 | - |
| n373 | NON_TERMINAL | logical_or_expression | - | 57 | n374 | - |
| n374 | NON_TERMINAL | logical_and_expression | - | 55 | n375 | - |
| n375 | NON_TERMINAL | inclusive_or_expression | - | 53 | n376 | - |
| n376 | NON_TERMINAL | exclusive_or_expression | - | 51 | n377 | - |
| n377 | NON_TERMINAL | and_expression | - | 49 | n378 | - |
| n378 | NON_TERMINAL | equality_expression | - | 46 | n379 | - |
| n379 | NON_TERMINAL | relational_expression | - | 41 | n380 | - |
| n380 | NON_TERMINAL | shift_expression | - | 38 | n381 | - |
| n381 | NON_TERMINAL | additive_expression | - | 35 | n382 | - |
| n382 | NON_TERMINAL | multiplicative_expression | - | 31 | n383 | - |
| n383 | NON_TERMINAL | cast_expression | - | 29 | n384 | - |
| n384 | NON_TERMINAL | unary_expression | - | 17 | n385 | - |
| n385 | NON_TERMINAL | postfix_expression | - | 8 | n386, n389, n390, n427 | - |
| n386 | NON_TERMINAL | postfix_expression | - | 5 | n387 | - |
| n387 | NON_TERMINAL | primary_expression | - | 1 | n388 | - |
| n388 | TERMINAL_LEAF | IDENTIFIER | add | -1 | - | - |
| n389 | TERMINAL_LEAF | LPAREN | ( | -1 | - | - |
| n390 | NON_TERMINAL | argument_expression_list | - | 16 | n391, n409, n410 | - |
| n391 | NON_TERMINAL | argument_expression_list | - | 15 | n392 | - |
| n392 | NON_TERMINAL | assignment_expression | - | 61 | n393 | - |
| n393 | NON_TERMINAL | conditional_expression | - | 59 | n394 | - |
| n394 | NON_TERMINAL | logical_or_expression | - | 57 | n395 | - |
| n395 | NON_TERMINAL | logical_and_expression | - | 55 | n396 | - |
| n396 | NON_TERMINAL | inclusive_or_expression | - | 53 | n397 | - |
| n397 | NON_TERMINAL | exclusive_or_expression | - | 51 | n398 | - |
| n398 | NON_TERMINAL | and_expression | - | 49 | n399 | - |
| n399 | NON_TERMINAL | equality_expression | - | 46 | n400 | - |
| n400 | NON_TERMINAL | relational_expression | - | 41 | n401 | - |
| n401 | NON_TERMINAL | shift_expression | - | 38 | n402 | - |
| n402 | NON_TERMINAL | additive_expression | - | 35 | n403 | - |
| n403 | NON_TERMINAL | multiplicative_expression | - | 31 | n404 | - |
| n404 | NON_TERMINAL | cast_expression | - | 29 | n405 | - |
| n405 | NON_TERMINAL | unary_expression | - | 17 | n406 | - |
| n406 | NON_TERMINAL | postfix_expression | - | 5 | n407 | - |
| n407 | NON_TERMINAL | primary_expression | - | 1 | n408 | - |
| n408 | TERMINAL_LEAF | IDENTIFIER | a | -1 | - | - |
| n409 | TERMINAL_LEAF | COMMA | , | -1 | - | - |
| n410 | NON_TERMINAL | assignment_expression | - | 61 | n411 | - |
| n411 | NON_TERMINAL | conditional_expression | - | 59 | n412 | - |
| n412 | NON_TERMINAL | logical_or_expression | - | 57 | n413 | - |
| n413 | NON_TERMINAL | logical_and_expression | - | 55 | n414 | - |
| n414 | NON_TERMINAL | inclusive_or_expression | - | 53 | n415 | - |
| n415 | NON_TERMINAL | exclusive_or_expression | - | 51 | n416 | - |
| n416 | NON_TERMINAL | and_expression | - | 49 | n417 | - |
| n417 | NON_TERMINAL | equality_expression | - | 46 | n418 | - |
| n418 | NON_TERMINAL | relational_expression | - | 41 | n419 | - |
| n419 | NON_TERMINAL | shift_expression | - | 38 | n420 | - |
| n420 | NON_TERMINAL | additive_expression | - | 35 | n421 | - |
| n421 | NON_TERMINAL | multiplicative_expression | - | 31 | n422 | - |
| n422 | NON_TERMINAL | cast_expression | - | 29 | n423 | - |
| n423 | NON_TERMINAL | unary_expression | - | 17 | n424 | - |
| n424 | NON_TERMINAL | postfix_expression | - | 5 | n425 | - |
| n425 | NON_TERMINAL | primary_expression | - | 1 | n426 | - |
| n426 | TERMINAL_LEAF | IDENTIFIER | b | -1 | - | - |
| n427 | TERMINAL_LEAF | RPAREN | ) | -1 | - | - |
| n428 | TERMINAL_LEAF | SEMI | ; | -1 | - | - |
| n429 | NON_TERMINAL | block_item | - | 213 | n430 | - |
| n430 | NON_TERMINAL | statement | - | 203 | n431 | - |
| n431 | NON_TERMINAL | iteration_statement | - | 219 | n432, n433, n434, n463, n464 | - |
| n432 | TERMINAL_LEAF | WHILE | while | -1 | - | - |
| n433 | TERMINAL_LEAF | LPAREN | ( | -1 | - | - |
| n434 | NON_TERMINAL | expression | - | 74 | n435 | - |
| n435 | NON_TERMINAL | assignment_expression | - | 61 | n436 | - |
| n436 | NON_TERMINAL | conditional_expression | - | 59 | n437 | - |
| n437 | NON_TERMINAL | logical_or_expression | - | 57 | n438 | - |
| n438 | NON_TERMINAL | logical_and_expression | - | 55 | n439 | - |
| n439 | NON_TERMINAL | inclusive_or_expression | - | 53 | n440 | - |
| n440 | NON_TERMINAL | exclusive_or_expression | - | 51 | n441 | - |
| n441 | NON_TERMINAL | and_expression | - | 49 | n442 | - |
| n442 | NON_TERMINAL | equality_expression | - | 48 | n443, n453, n454 | - |
| n443 | NON_TERMINAL | equality_expression | - | 46 | n444 | - |
| n444 | NON_TERMINAL | relational_expression | - | 41 | n445 | - |
| n445 | NON_TERMINAL | shift_expression | - | 38 | n446 | - |
| n446 | NON_TERMINAL | additive_expression | - | 35 | n447 | - |
| n447 | NON_TERMINAL | multiplicative_expression | - | 31 | n448 | - |
| n448 | NON_TERMINAL | cast_expression | - | 29 | n449 | - |
| n449 | NON_TERMINAL | unary_expression | - | 17 | n450 | - |
| n450 | NON_TERMINAL | postfix_expression | - | 5 | n451 | - |
| n451 | NON_TERMINAL | primary_expression | - | 1 | n452 | - |
| n452 | TERMINAL_LEAF | IDENTIFIER | a | -1 | - | - |
| n453 | TERMINAL_LEAF | NE_OP | != | -1 | - | - |
| n454 | NON_TERMINAL | relational_expression | - | 41 | n455 | - |
| n455 | NON_TERMINAL | shift_expression | - | 38 | n456 | - |
| n456 | NON_TERMINAL | additive_expression | - | 35 | n457 | - |
| n457 | NON_TERMINAL | multiplicative_expression | - | 31 | n458 | - |
| n458 | NON_TERMINAL | cast_expression | - | 29 | n459 | - |
| n459 | NON_TERMINAL | unary_expression | - | 17 | n460 | - |
| n460 | NON_TERMINAL | postfix_expression | - | 5 | n461 | - |
| n461 | NON_TERMINAL | primary_expression | - | 1 | n462 | - |
| n462 | TERMINAL_LEAF | IDENTIFIER | b | -1 | - | - |
| n463 | TERMINAL_LEAF | RPAREN | ) | -1 | - | - |
| n464 | NON_TERMINAL | statement | - | 200 | n465 | - |
| n465 | NON_TERMINAL | compound_statement | - | 209 | n466, n467, n537 | - |
| n466 | TERMINAL_LEAF | LBRACE | { | -1 | - | - |
| n467 | NON_TERMINAL | block_item_list | - | 210 | n468 | - |
| n468 | NON_TERMINAL | block_item | - | 213 | n469 | - |
| n469 | NON_TERMINAL | statement | - | 201 | n470 | - |
| n470 | NON_TERMINAL | expression_statement | - | 215 | n471, n536 | - |
| n471 | NON_TERMINAL | expression | - | 74 | n472 | - |
| n472 | NON_TERMINAL | assignment_expression | - | 62 | n473, n477, n479 | - |
| n473 | NON_TERMINAL | unary_expression | - | 17 | n474 | - |
| n474 | NON_TERMINAL | postfix_expression | - | 5 | n475 | - |
| n475 | NON_TERMINAL | primary_expression | - | 1 | n476 | - |
| n476 | TERMINAL_LEAF | IDENTIFIER | a | -1 | - | - |
| n477 | NON_TERMINAL | assignment_operator | - | 63 | n478 | - |
| n478 | TERMINAL_LEAF | ASSIGN | = | -1 | - | - |
| n479 | NON_TERMINAL | assignment_expression | - | 61 | n480 | - |
| n480 | NON_TERMINAL | conditional_expression | - | 59 | n481 | - |
| n481 | NON_TERMINAL | logical_or_expression | - | 57 | n482 | - |
| n482 | NON_TERMINAL | logical_and_expression | - | 55 | n483 | - |
| n483 | NON_TERMINAL | inclusive_or_expression | - | 53 | n484 | - |
| n484 | NON_TERMINAL | exclusive_or_expression | - | 51 | n485 | - |
| n485 | NON_TERMINAL | and_expression | - | 49 | n486 | - |
| n486 | NON_TERMINAL | equality_expression | - | 46 | n487 | - |
| n487 | NON_TERMINAL | relational_expression | - | 41 | n488 | - |
| n488 | NON_TERMINAL | shift_expression | - | 38 | n489 | - |
| n489 | NON_TERMINAL | additive_expression | - | 35 | n490 | - |
| n490 | NON_TERMINAL | multiplicative_expression | - | 31 | n491 | - |
| n491 | NON_TERMINAL | cast_expression | - | 29 | n492 | - |
| n492 | NON_TERMINAL | unary_expression | - | 17 | n493 | - |
| n493 | NON_TERMINAL | postfix_expression | - | 8 | n494, n497, n498, n535 | - |
| n494 | NON_TERMINAL | postfix_expression | - | 5 | n495 | - |
| n495 | NON_TERMINAL | primary_expression | - | 1 | n496 | - |
| n496 | TERMINAL_LEAF | IDENTIFIER | add | -1 | - | - |
| n497 | TERMINAL_LEAF | LPAREN | ( | -1 | - | - |
| n498 | NON_TERMINAL | argument_expression_list | - | 16 | n499, n517, n518 | - |
| n499 | NON_TERMINAL | argument_expression_list | - | 15 | n500 | - |
| n500 | NON_TERMINAL | assignment_expression | - | 61 | n501 | - |
| n501 | NON_TERMINAL | conditional_expression | - | 59 | n502 | - |
| n502 | NON_TERMINAL | logical_or_expression | - | 57 | n503 | - |
| n503 | NON_TERMINAL | logical_and_expression | - | 55 | n504 | - |
| n504 | NON_TERMINAL | inclusive_or_expression | - | 53 | n505 | - |
| n505 | NON_TERMINAL | exclusive_or_expression | - | 51 | n506 | - |
| n506 | NON_TERMINAL | and_expression | - | 49 | n507 | - |
| n507 | NON_TERMINAL | equality_expression | - | 46 | n508 | - |
| n508 | NON_TERMINAL | relational_expression | - | 41 | n509 | - |
| n509 | NON_TERMINAL | shift_expression | - | 38 | n510 | - |
| n510 | NON_TERMINAL | additive_expression | - | 35 | n511 | - |
| n511 | NON_TERMINAL | multiplicative_expression | - | 31 | n512 | - |
| n512 | NON_TERMINAL | cast_expression | - | 29 | n513 | - |
| n513 | NON_TERMINAL | unary_expression | - | 17 | n514 | - |
| n514 | NON_TERMINAL | postfix_expression | - | 5 | n515 | - |
| n515 | NON_TERMINAL | primary_expression | - | 1 | n516 | - |
| n516 | TERMINAL_LEAF | IDENTIFIER | a | -1 | - | - |
| n517 | TERMINAL_LEAF | COMMA | , | -1 | - | - |
| n518 | NON_TERMINAL | assignment_expression | - | 61 | n519 | - |
| n519 | NON_TERMINAL | conditional_expression | - | 59 | n520 | - |
| n520 | NON_TERMINAL | logical_or_expression | - | 57 | n521 | - |
| n521 | NON_TERMINAL | logical_and_expression | - | 55 | n522 | - |
| n522 | NON_TERMINAL | inclusive_or_expression | - | 53 | n523 | - |
| n523 | NON_TERMINAL | exclusive_or_expression | - | 51 | n524 | - |
| n524 | NON_TERMINAL | and_expression | - | 49 | n525 | - |
| n525 | NON_TERMINAL | equality_expression | - | 46 | n526 | - |
| n526 | NON_TERMINAL | relational_expression | - | 41 | n527 | - |
| n527 | NON_TERMINAL | shift_expression | - | 38 | n528 | - |
| n528 | NON_TERMINAL | additive_expression | - | 35 | n529 | - |
| n529 | NON_TERMINAL | multiplicative_expression | - | 31 | n530 | - |
| n530 | NON_TERMINAL | cast_expression | - | 29 | n531 | - |
| n531 | NON_TERMINAL | unary_expression | - | 17 | n532 | - |
| n532 | NON_TERMINAL | postfix_expression | - | 5 | n533 | - |
| n533 | NON_TERMINAL | primary_expression | - | 2 | n534 | - |
| n534 | TERMINAL_LEAF | CONSTANT | 1 | -1 | - | - |
| n535 | TERMINAL_LEAF | RPAREN | ) | -1 | - | - |
| n536 | TERMINAL_LEAF | SEMI | ; | -1 | - | - |
| n537 | TERMINAL_LEAF | RBRACE | } | -1 | - | - |
| n538 | NON_TERMINAL | block_item | - | 213 | n539 | - |
| n539 | NON_TERMINAL | statement | - | 204 | n540 | - |
| n540 | NON_TERMINAL | jump_statement | - | 229 | n541, n542, n560 | - |
| n541 | TERMINAL_LEAF | RETURN | return | -1 | - | - |
| n542 | NON_TERMINAL | expression | - | 74 | n543 | - |
| n543 | NON_TERMINAL | assignment_expression | - | 61 | n544 | - |
| n544 | NON_TERMINAL | conditional_expression | - | 59 | n545 | - |
| n545 | NON_TERMINAL | logical_or_expression | - | 57 | n546 | - |
| n546 | NON_TERMINAL | logical_and_expression | - | 55 | n547 | - |
| n547 | NON_TERMINAL | inclusive_or_expression | - | 53 | n548 | - |
| n548 | NON_TERMINAL | exclusive_or_expression | - | 51 | n549 | - |
| n549 | NON_TERMINAL | and_expression | - | 49 | n550 | - |
| n550 | NON_TERMINAL | equality_expression | - | 46 | n551 | - |
| n551 | NON_TERMINAL | relational_expression | - | 41 | n552 | - |
| n552 | NON_TERMINAL | shift_expression | - | 38 | n553 | - |
| n553 | NON_TERMINAL | additive_expression | - | 35 | n554 | - |
| n554 | NON_TERMINAL | multiplicative_expression | - | 31 | n555 | - |
| n555 | NON_TERMINAL | cast_expression | - | 29 | n556 | - |
| n556 | NON_TERMINAL | unary_expression | - | 17 | n557 | - |
| n557 | NON_TERMINAL | postfix_expression | - | 5 | n558 | - |
| n558 | NON_TERMINAL | primary_expression | - | 1 | n559 | - |
| n559 | TERMINAL_LEAF | IDENTIFIER | a | -1 | - | - |
| n560 | TERMINAL_LEAF | SEMI | ; | -1 | - | - |
| n561 | TERMINAL_LEAF | RBRACE | } | -1 | - | - |

## 2. 嵌入语义动作的文本语法树

```text
└── translation_unit [p=231]
    ├── translation_unit [p=230]
    │   └── external_declaration [p=232]
    │       └── function_definition [p=235]
    │           ├── declaration_specifiers [p=81]
    │           │   └── type_specifier [p=99]
    │           │       └── INT("int")
    │           ├── declarator [p=142]
    │           │   └── direct_declarator [p=153]
    │           │       ├── direct_declarator [p=143]
    │           │       │   └── IDENTIFIER("add")
    │           │       ├── LPAREN("(")
    │           │       ├── parameter_type_list [p=162]
    │           │       │   └── parameter_list [p=165]
    │           │       │       ├── parameter_list [p=164]
    │           │       │       │   └── parameter_declaration [p=166]
    │           │       │       │       ├── declaration_specifiers [p=81]
    │           │       │       │       │   └── type_specifier [p=99]
    │           │       │       │       │       └── INT("int")
    │           │       │       │       └── declarator [p=142]
    │           │       │       │           └── direct_declarator [p=143]
    │           │       │       │               └── IDENTIFIER("x")
    │           │       │       ├── COMMA(",")
    │           │       │       └── parameter_declaration [p=166]
    │           │       │           ├── declaration_specifiers [p=81]
    │           │       │           │   └── type_specifier [p=99]
    │           │       │           │       └── INT("int")
    │           │       │           └── declarator [p=142]
    │           │       │               └── direct_declarator [p=143]
    │           │       │                   └── IDENTIFIER("y")
    │           │       └── RPAREN(")")
    │           └── compound_statement [p=209]
    │               ├── LBRACE("{")
    │               ├── block_item_list [p=210]
    │               │   └── block_item [p=213]
    │               │       └── statement [p=204]
    │               │           └── jump_statement [p=229]
    │               │               ├── RETURN("return")
    │               │               ├── expression [p=74]
    │               │               │   └── assignment_expression [p=61]
    │               │               │       └── conditional_expression [p=59]
    │               │               │           └── logical_or_expression [p=57]
    │               │               │               └── logical_and_expression [p=55]
    │               │               │                   └── inclusive_or_expression [p=53]
    │               │               │                       └── exclusive_or_expression [p=51]
    │               │               │                           └── and_expression [p=49]
    │               │               │                               └── equality_expression [p=46]
    │               │               │                                   └── relational_expression [p=41]
    │               │               │                                       └── shift_expression [p=38]
    │               │               │                                           └── additive_expression [p=36]
    │               │               │                                               ├── additive_expression [p=35]
    │               │               │                                               │   └── multiplicative_expression [p=31]
    │               │               │                                               │       └── cast_expression [p=29]
    │               │               │                                               │           └── unary_expression [p=17]
    │               │               │                                               │               └── postfix_expression [p=5]
    │               │               │                                               │                   └── primary_expression [p=1]
    │               │               │                                               │                       └── IDENTIFIER("x")
    │               │               │                                               ├── PLUS("+")
    │               │               │                                               └── multiplicative_expression [p=31]
    │               │               │                                                   └── cast_expression [p=29]
    │               │               │                                                       └── unary_expression [p=17]
    │               │               │                                                           └── postfix_expression [p=5]
    │               │               │                                                               └── primary_expression [p=1]
    │               │               │                                                                   └── IDENTIFIER("y")
    │               │               └── SEMI(";")
    │               └── RBRACE("}")
    └── external_declaration [p=232]
        └── function_definition [p=235]
            ├── declaration_specifiers [p=81]
            │   └── type_specifier [p=99]
            │       └── INT("int")
            ├── declarator [p=142]
            │   └── direct_declarator [p=155]
            │       ├── direct_declarator [p=143]
            │       │   └── IDENTIFIER("main")
            │       ├── LPAREN("(")
            │       └── RPAREN(")")
            └── compound_statement [p=209]
                ├── LBRACE("{")
                ├── block_item_list [p=211]
                │   ├── block_item_list [p=211]
                │   │   ├── block_item_list [p=211]
                │   │   │   ├── block_item_list [p=211]
                │   │   │   │   ├── block_item_list [p=211]
                │   │   │   │   │   ├── block_item_list [p=211]
                │   │   │   │   │   │   ├── block_item_list [p=210]
                │   │   │   │   │   │   │   └── block_item [p=212]
                │   │   │   │   │   │   │       └── declaration [p=78]
                │   │   │   │   │   │   │           ├── declaration_specifiers [p=81]
                │   │   │   │   │   │   │           │   └── type_specifier [p=99]
                │   │   │   │   │   │   │           │       └── INT("int")
                │   │   │   │   │   │   │           ├── init_declarator_list [p=87]
                │   │   │   │   │   │   │           │   └── init_declarator [p=89]
                │   │   │   │   │   │   │           │       └── declarator [p=142]
                │   │   │   │   │   │   │           │           └── direct_declarator [p=143]
                │   │   │   │   │   │   │           │               └── IDENTIFIER("a")
                │   │   │   │   │   │   │           └── SEMI(";")
                │   │   │   │   │   │   └── block_item [p=212]
                │   │   │   │   │   │       └── declaration [p=78]
                │   │   │   │   │   │           ├── declaration_specifiers [p=81]
                │   │   │   │   │   │           │   └── type_specifier [p=99]
                │   │   │   │   │   │           │       └── INT("int")
                │   │   │   │   │   │           ├── init_declarator_list [p=87]
                │   │   │   │   │   │           │   └── init_declarator [p=90]
                │   │   │   │   │   │           │       ├── declarator [p=142]
                │   │   │   │   │   │           │       │   └── direct_declarator [p=143]
                │   │   │   │   │   │           │       │       └── IDENTIFIER("b")
                │   │   │   │   │   │           │       ├── ASSIGN("=")
                │   │   │   │   │   │           │       └── initializer [p=187]
                │   │   │   │   │   │           │           └── assignment_expression [p=61]
                │   │   │   │   │   │           │               └── conditional_expression [p=59]
                │   │   │   │   │   │           │                   └── logical_or_expression [p=57]
                │   │   │   │   │   │           │                       └── logical_and_expression [p=55]
                │   │   │   │   │   │           │                           └── inclusive_or_expression [p=53]
                │   │   │   │   │   │           │                               └── exclusive_or_expression [p=51]
                │   │   │   │   │   │           │                                   └── and_expression [p=49]
                │   │   │   │   │   │           │                                       └── equality_expression [p=46]
                │   │   │   │   │   │           │                                           └── relational_expression [p=41]
                │   │   │   │   │   │           │                                               └── shift_expression [p=38]
                │   │   │   │   │   │           │                                                   └── additive_expression [p=35]
                │   │   │   │   │   │           │                                                       └── multiplicative_expression [p=31]
                │   │   │   │   │   │           │                                                           └── cast_expression [p=29]
                │   │   │   │   │   │           │                                                               └── unary_expression [p=17]
                │   │   │   │   │   │           │                                                                   └── postfix_expression [p=5]
                │   │   │   │   │   │           │                                                                       └── primary_expression [p=2]
                │   │   │   │   │   │           │                                                                           └── CONSTANT("5")
                │   │   │   │   │   │           └── SEMI(";")
                │   │   │   │   │   └── block_item [p=213]
                │   │   │   │   │       └── statement [p=201]
                │   │   │   │   │           └── expression_statement [p=215]
                │   │   │   │   │               ├── expression [p=74]
                │   │   │   │   │               │   └── assignment_expression [p=62]
                │   │   │   │   │               │       ├── unary_expression [p=17]
                │   │   │   │   │               │       │   └── postfix_expression [p=5]
                │   │   │   │   │               │       │       └── primary_expression [p=1]
                │   │   │   │   │               │       │           └── IDENTIFIER("a")
                │   │   │   │   │               │       ├── assignment_operator [p=63]
                │   │   │   │   │               │       │   └── ASSIGN("=")
                │   │   │   │   │               │       └── assignment_expression [p=61]
                │   │   │   │   │               │           └── conditional_expression [p=59]
                │   │   │   │   │               │               └── logical_or_expression [p=57]
                │   │   │   │   │               │                   └── logical_and_expression [p=55]
                │   │   │   │   │               │                       └── inclusive_or_expression [p=53]
                │   │   │   │   │               │                           └── exclusive_or_expression [p=51]
                │   │   │   │   │               │                               └── and_expression [p=49]
                │   │   │   │   │               │                                   └── equality_expression [p=46]
                │   │   │   │   │               │                                       └── relational_expression [p=41]
                │   │   │   │   │               │                                           └── shift_expression [p=38]
                │   │   │   │   │               │                                               └── additive_expression [p=35]
                │   │   │   │   │               │                                                   └── multiplicative_expression [p=31]
                │   │   │   │   │               │                                                       └── cast_expression [p=29]
                │   │   │   │   │               │                                                           └── unary_expression [p=17]
                │   │   │   │   │               │                                                               └── postfix_expression [p=8]
                │   │   │   │   │               │                                                                   ├── postfix_expression [p=5]
                │   │   │   │   │               │                                                                   │   └── primary_expression [p=1]
                │   │   │   │   │               │                                                                   │       └── IDENTIFIER("add")
                │   │   │   │   │               │                                                                   ├── LPAREN("(")
                │   │   │   │   │               │                                                                   ├── argument_expression_list [p=16]
                │   │   │   │   │               │                                                                   │   ├── argument_expression_list [p=15]
                │   │   │   │   │               │                                                                   │   │   └── assignment_expression [p=61]
                │   │   │   │   │               │                                                                   │   │       └── conditional_expression [p=59]
                │   │   │   │   │               │                                                                   │   │           └── logical_or_expression [p=57]
                │   │   │   │   │               │                                                                   │   │               └── logical_and_expression [p=55]
                │   │   │   │   │               │                                                                   │   │                   └── inclusive_or_expression [p=53]
                │   │   │   │   │               │                                                                   │   │                       └── exclusive_or_expression [p=51]
                │   │   │   │   │               │                                                                   │   │                           └── and_expression [p=49]
                │   │   │   │   │               │                                                                   │   │                               └── equality_expression [p=46]
                │   │   │   │   │               │                                                                   │   │                                   └── relational_expression [p=41]
                │   │   │   │   │               │                                                                   │   │                                       └── shift_expression [p=38]
                │   │   │   │   │               │                                                                   │   │                                           └── additive_expression [p=35]
                │   │   │   │   │               │                                                                   │   │                                               └── multiplicative_expression [p=31]
                │   │   │   │   │               │                                                                   │   │                                                   └── cast_expression [p=29]
                │   │   │   │   │               │                                                                   │   │                                                       └── unary_expression [p=17]
                │   │   │   │   │               │                                                                   │   │                                                           └── postfix_expression [p=5]
                │   │   │   │   │               │                                                                   │   │                                                               └── primary_expression [p=1]
                │   │   │   │   │               │                                                                   │   │                                                                   └── IDENTIFIER("b")
                │   │   │   │   │               │                                                                   │   ├── COMMA(",")
                │   │   │   │   │               │                                                                   │   └── assignment_expression [p=61]
                │   │   │   │   │               │                                                                   │       └── conditional_expression [p=59]
                │   │   │   │   │               │                                                                   │           └── logical_or_expression [p=57]
                │   │   │   │   │               │                                                                   │               └── logical_and_expression [p=55]
                │   │   │   │   │               │                                                                   │                   └── inclusive_or_expression [p=53]
                │   │   │   │   │               │                                                                   │                       └── exclusive_or_expression [p=51]
                │   │   │   │   │               │                                                                   │                           └── and_expression [p=49]
                │   │   │   │   │               │                                                                   │                               └── equality_expression [p=46]
                │   │   │   │   │               │                                                                   │                                   └── relational_expression [p=41]
                │   │   │   │   │               │                                                                   │                                       └── shift_expression [p=38]
                │   │   │   │   │               │                                                                   │                                           └── additive_expression [p=35]
                │   │   │   │   │               │                                                                   │                                               └── multiplicative_expression [p=31]
                │   │   │   │   │               │                                                                   │                                                   └── cast_expression [p=29]
                │   │   │   │   │               │                                                                   │                                                       └── unary_expression [p=17]
                │   │   │   │   │               │                                                                   │                                                           └── postfix_expression [p=5]
                │   │   │   │   │               │                                                                   │                                                               └── primary_expression [p=2]
                │   │   │   │   │               │                                                                   │                                                                   └── CONSTANT("3")
                │   │   │   │   │               │                                                                   └── RPAREN(")")
                │   │   │   │   │               └── SEMI(";")
                │   │   │   │   └── block_item [p=213]
                │   │   │   │       └── statement [p=202]
                │   │   │   │           └── selection_statement [p=217]
                │   │   │   │               ├── IF("if")
                │   │   │   │               ├── LPAREN("(")
                │   │   │   │               ├── expression [p=74]
                │   │   │   │               │   └── assignment_expression [p=61]
                │   │   │   │               │       └── conditional_expression [p=59]
                │   │   │   │               │           └── logical_or_expression [p=57]
                │   │   │   │               │               └── logical_and_expression [p=55]
                │   │   │   │               │                   └── inclusive_or_expression [p=53]
                │   │   │   │               │                       └── exclusive_or_expression [p=51]
                │   │   │   │               │                           └── and_expression [p=49]
                │   │   │   │               │                               └── equality_expression [p=46]
                │   │   │   │               │                                   └── relational_expression [p=42]
                │   │   │   │               │                                       ├── relational_expression [p=41]
                │   │   │   │               │                                       │   └── shift_expression [p=38]
                │   │   │   │               │                                       │       └── additive_expression [p=35]
                │   │   │   │               │                                       │           └── multiplicative_expression [p=31]
                │   │   │   │               │                                       │               └── cast_expression [p=29]
                │   │   │   │               │                                       │                   └── unary_expression [p=17]
                │   │   │   │               │                                       │                       └── postfix_expression [p=5]
                │   │   │   │               │                                       │                           └── primary_expression [p=1]
                │   │   │   │               │                                       │                               └── IDENTIFIER("a")
                │   │   │   │               │                                       ├── LT("<")
                │   │   │   │               │                                       └── shift_expression [p=38]
                │   │   │   │               │                                           └── additive_expression [p=35]
                │   │   │   │               │                                               └── multiplicative_expression [p=31]
                │   │   │   │               │                                                   └── cast_expression [p=29]
                │   │   │   │               │                                                       └── unary_expression [p=17]
                │   │   │   │               │                                                           └── postfix_expression [p=5]
                │   │   │   │               │                                                               └── primary_expression [p=1]
                │   │   │   │               │                                                                   └── IDENTIFIER("b")
                │   │   │   │               ├── RPAREN(")")
                │   │   │   │               ├── statement [p=201]
                │   │   │   │               │   └── expression_statement [p=215]
                │   │   │   │               │       ├── expression [p=74]
                │   │   │   │               │       │   └── assignment_expression [p=62]
                │   │   │   │               │       │       ├── unary_expression [p=17]
                │   │   │   │               │       │       │   └── postfix_expression [p=5]
                │   │   │   │               │       │       │       └── primary_expression [p=1]
                │   │   │   │               │       │       │           └── IDENTIFIER("a")
                │   │   │   │               │       │       ├── assignment_operator [p=63]
                │   │   │   │               │       │       │   └── ASSIGN("=")
                │   │   │   │               │       │       └── assignment_expression [p=61]
                │   │   │   │               │       │           └── conditional_expression [p=59]
                │   │   │   │               │       │               └── logical_or_expression [p=57]
                │   │   │   │               │       │                   └── logical_and_expression [p=55]
                │   │   │   │               │       │                       └── inclusive_or_expression [p=53]
                │   │   │   │               │       │                           └── exclusive_or_expression [p=51]
                │   │   │   │               │       │                               └── and_expression [p=49]
                │   │   │   │               │       │                                   └── equality_expression [p=46]
                │   │   │   │               │       │                                       └── relational_expression [p=41]
                │   │   │   │               │       │                                           └── shift_expression [p=38]
                │   │   │   │               │       │                                               └── additive_expression [p=35]
                │   │   │   │               │       │                                                   └── multiplicative_expression [p=31]
                │   │   │   │               │       │                                                       └── cast_expression [p=29]
                │   │   │   │               │       │                                                           └── unary_expression [p=17]
                │   │   │   │               │       │                                                               └── postfix_expression [p=8]
                │   │   │   │               │       │                                                                   ├── postfix_expression [p=5]
                │   │   │   │               │       │                                                                   │   └── primary_expression [p=1]
                │   │   │   │               │       │                                                                   │       └── IDENTIFIER("add")
                │   │   │   │               │       │                                                                   ├── LPAREN("(")
                │   │   │   │               │       │                                                                   ├── argument_expression_list [p=16]
                │   │   │   │               │       │                                                                   │   ├── argument_expression_list [p=15]
                │   │   │   │               │       │                                                                   │   │   └── assignment_expression [p=61]
                │   │   │   │               │       │                                                                   │   │       └── conditional_expression [p=59]
                │   │   │   │               │       │                                                                   │   │           └── logical_or_expression [p=57]
                │   │   │   │               │       │                                                                   │   │               └── logical_and_expression [p=55]
                │   │   │   │               │       │                                                                   │   │                   └── inclusive_or_expression [p=53]
                │   │   │   │               │       │                                                                   │   │                       └── exclusive_or_expression [p=51]
                │   │   │   │               │       │                                                                   │   │                           └── and_expression [p=49]
                │   │   │   │               │       │                                                                   │   │                               └── equality_expression [p=46]
                │   │   │   │               │       │                                                                   │   │                                   └── relational_expression [p=41]
                │   │   │   │               │       │                                                                   │   │                                       └── shift_expression [p=38]
                │   │   │   │               │       │                                                                   │   │                                           └── additive_expression [p=35]
                │   │   │   │               │       │                                                                   │   │                                               └── multiplicative_expression [p=31]
                │   │   │   │               │       │                                                                   │   │                                                   └── cast_expression [p=29]
                │   │   │   │               │       │                                                                   │   │                                                       └── unary_expression [p=17]
                │   │   │   │               │       │                                                                   │   │                                                           └── postfix_expression [p=5]
                │   │   │   │               │       │                                                                   │   │                                                               └── primary_expression [p=1]
                │   │   │   │               │       │                                                                   │   │                                                                   └── IDENTIFIER("a")
                │   │   │   │               │       │                                                                   │   ├── COMMA(",")
                │   │   │   │               │       │                                                                   │   └── assignment_expression [p=61]
                │   │   │   │               │       │                                                                   │       └── conditional_expression [p=59]
                │   │   │   │               │       │                                                                   │           └── logical_or_expression [p=57]
                │   │   │   │               │       │                                                                   │               └── logical_and_expression [p=55]
                │   │   │   │               │       │                                                                   │                   └── inclusive_or_expression [p=53]
                │   │   │   │               │       │                                                                   │                       └── exclusive_or_expression [p=51]
                │   │   │   │               │       │                                                                   │                           └── and_expression [p=49]
                │   │   │   │               │       │                                                                   │                               └── equality_expression [p=46]
                │   │   │   │               │       │                                                                   │                                   └── relational_expression [p=41]
                │   │   │   │               │       │                                                                   │                                       └── shift_expression [p=38]
                │   │   │   │               │       │                                                                   │                                           └── additive_expression [p=35]
                │   │   │   │               │       │                                                                   │                                               └── multiplicative_expression [p=31]
                │   │   │   │               │       │                                                                   │                                                   └── cast_expression [p=29]
                │   │   │   │               │       │                                                                   │                                                       └── unary_expression [p=17]
                │   │   │   │               │       │                                                                   │                                                           └── postfix_expression [p=5]
                │   │   │   │               │       │                                                                   │                                                               └── primary_expression [p=2]
                │   │   │   │               │       │                                                                   │                                                                   └── CONSTANT("1")
                │   │   │   │               │       │                                                                   └── RPAREN(")")
                │   │   │   │               │       └── SEMI(";")
                │   │   │   │               ├── ELSE("else")
                │   │   │   │               └── statement [p=201]
                │   │   │   │                   └── expression_statement [p=215]
                │   │   │   │                       ├── expression [p=74]
                │   │   │   │                       │   └── assignment_expression [p=62]
                │   │   │   │                       │       ├── unary_expression [p=17]
                │   │   │   │                       │       │   └── postfix_expression [p=5]
                │   │   │   │                       │       │       └── primary_expression [p=1]
                │   │   │   │                       │       │           └── IDENTIFIER("a")
                │   │   │   │                       │       ├── assignment_operator [p=63]
                │   │   │   │                       │       │   └── ASSIGN("=")
                │   │   │   │                       │       └── assignment_expression [p=61]
                │   │   │   │                       │           └── conditional_expression [p=59]
                │   │   │   │                       │               └── logical_or_expression [p=57]
                │   │   │   │                       │                   └── logical_and_expression [p=55]
                │   │   │   │                       │                       └── inclusive_or_expression [p=53]
                │   │   │   │                       │                           └── exclusive_or_expression [p=51]
                │   │   │   │                       │                               └── and_expression [p=49]
                │   │   │   │                       │                                   └── equality_expression [p=46]
                │   │   │   │                       │                                       └── relational_expression [p=41]
                │   │   │   │                       │                                           └── shift_expression [p=38]
                │   │   │   │                       │                                               └── additive_expression [p=35]
                │   │   │   │                       │                                                   └── multiplicative_expression [p=31]
                │   │   │   │                       │                                                       └── cast_expression [p=29]
                │   │   │   │                       │                                                           └── unary_expression [p=17]
                │   │   │   │                       │                                                               └── postfix_expression [p=8]
                │   │   │   │                       │                                                                   ├── postfix_expression [p=5]
                │   │   │   │                       │                                                                   │   └── primary_expression [p=1]
                │   │   │   │                       │                                                                   │       └── IDENTIFIER("add")
                │   │   │   │                       │                                                                   ├── LPAREN("(")
                │   │   │   │                       │                                                                   ├── argument_expression_list [p=16]
                │   │   │   │                       │                                                                   │   ├── argument_expression_list [p=15]
                │   │   │   │                       │                                                                   │   │   └── assignment_expression [p=61]
                │   │   │   │                       │                                                                   │   │       └── conditional_expression [p=59]
                │   │   │   │                       │                                                                   │   │           └── logical_or_expression [p=57]
                │   │   │   │                       │                                                                   │   │               └── logical_and_expression [p=55]
                │   │   │   │                       │                                                                   │   │                   └── inclusive_or_expression [p=53]
                │   │   │   │                       │                                                                   │   │                       └── exclusive_or_expression [p=51]
                │   │   │   │                       │                                                                   │   │                           └── and_expression [p=49]
                │   │   │   │                       │                                                                   │   │                               └── equality_expression [p=46]
                │   │   │   │                       │                                                                   │   │                                   └── relational_expression [p=41]
                │   │   │   │                       │                                                                   │   │                                       └── shift_expression [p=38]
                │   │   │   │                       │                                                                   │   │                                           └── additive_expression [p=35]
                │   │   │   │                       │                                                                   │   │                                               └── multiplicative_expression [p=31]
                │   │   │   │                       │                                                                   │   │                                                   └── cast_expression [p=29]
                │   │   │   │                       │                                                                   │   │                                                       └── unary_expression [p=17]
                │   │   │   │                       │                                                                   │   │                                                           └── postfix_expression [p=5]
                │   │   │   │                       │                                                                   │   │                                                               └── primary_expression [p=1]
                │   │   │   │                       │                                                                   │   │                                                                   └── IDENTIFIER("a")
                │   │   │   │                       │                                                                   │   ├── COMMA(",")
                │   │   │   │                       │                                                                   │   └── assignment_expression [p=61]
                │   │   │   │                       │                                                                   │       └── conditional_expression [p=59]
                │   │   │   │                       │                                                                   │           └── logical_or_expression [p=57]
                │   │   │   │                       │                                                                   │               └── logical_and_expression [p=55]
                │   │   │   │                       │                                                                   │                   └── inclusive_or_expression [p=53]
                │   │   │   │                       │                                                                   │                       └── exclusive_or_expression [p=51]
                │   │   │   │                       │                                                                   │                           └── and_expression [p=49]
                │   │   │   │                       │                                                                   │                               └── equality_expression [p=46]
                │   │   │   │                       │                                                                   │                                   └── relational_expression [p=41]
                │   │   │   │                       │                                                                   │                                       └── shift_expression [p=38]
                │   │   │   │                       │                                                                   │                                           └── additive_expression [p=35]
                │   │   │   │                       │                                                                   │                                               └── multiplicative_expression [p=31]
                │   │   │   │                       │                                                                   │                                                   └── cast_expression [p=29]
                │   │   │   │                       │                                                                   │                                                       └── unary_expression [p=17]
                │   │   │   │                       │                                                                   │                                                           └── postfix_expression [p=5]
                │   │   │   │                       │                                                                   │                                                               └── primary_expression [p=1]
                │   │   │   │                       │                                                                   │                                                                   └── IDENTIFIER("b")
                │   │   │   │                       │                                                                   └── RPAREN(")")
                │   │   │   │                       └── SEMI(";")
                │   │   │   └── block_item [p=213]
                │   │   │       └── statement [p=201]
                │   │   │           └── expression_statement [p=215]
                │   │   │               ├── expression [p=74]
                │   │   │               │   └── assignment_expression [p=61]
                │   │   │               │       └── conditional_expression [p=59]
                │   │   │               │           └── logical_or_expression [p=57]
                │   │   │               │               └── logical_and_expression [p=55]
                │   │   │               │                   └── inclusive_or_expression [p=53]
                │   │   │               │                       └── exclusive_or_expression [p=51]
                │   │   │               │                           └── and_expression [p=49]
                │   │   │               │                               └── equality_expression [p=46]
                │   │   │               │                                   └── relational_expression [p=41]
                │   │   │               │                                       └── shift_expression [p=38]
                │   │   │               │                                           └── additive_expression [p=35]
                │   │   │               │                                               └── multiplicative_expression [p=31]
                │   │   │               │                                                   └── cast_expression [p=29]
                │   │   │               │                                                       └── unary_expression [p=17]
                │   │   │               │                                                           └── postfix_expression [p=8]
                │   │   │               │                                                               ├── postfix_expression [p=5]
                │   │   │               │                                                               │   └── primary_expression [p=1]
                │   │   │               │                                                               │       └── IDENTIFIER("add")
                │   │   │               │                                                               ├── LPAREN("(")
                │   │   │               │                                                               ├── argument_expression_list [p=16]
                │   │   │               │                                                               │   ├── argument_expression_list [p=15]
                │   │   │               │                                                               │   │   └── assignment_expression [p=61]
                │   │   │               │                                                               │   │       └── conditional_expression [p=59]
                │   │   │               │                                                               │   │           └── logical_or_expression [p=57]
                │   │   │               │                                                               │   │               └── logical_and_expression [p=55]
                │   │   │               │                                                               │   │                   └── inclusive_or_expression [p=53]
                │   │   │               │                                                               │   │                       └── exclusive_or_expression [p=51]
                │   │   │               │                                                               │   │                           └── and_expression [p=49]
                │   │   │               │                                                               │   │                               └── equality_expression [p=46]
                │   │   │               │                                                               │   │                                   └── relational_expression [p=41]
                │   │   │               │                                                               │   │                                       └── shift_expression [p=38]
                │   │   │               │                                                               │   │                                           └── additive_expression [p=35]
                │   │   │               │                                                               │   │                                               └── multiplicative_expression [p=31]
                │   │   │               │                                                               │   │                                                   └── cast_expression [p=29]
                │   │   │               │                                                               │   │                                                       └── unary_expression [p=17]
                │   │   │               │                                                               │   │                                                           └── postfix_expression [p=5]
                │   │   │               │                                                               │   │                                                               └── primary_expression [p=1]
                │   │   │               │                                                               │   │                                                                   └── IDENTIFIER("a")
                │   │   │               │                                                               │   ├── COMMA(",")
                │   │   │               │                                                               │   └── assignment_expression [p=61]
                │   │   │               │                                                               │       └── conditional_expression [p=59]
                │   │   │               │                                                               │           └── logical_or_expression [p=57]
                │   │   │               │                                                               │               └── logical_and_expression [p=55]
                │   │   │               │                                                               │                   └── inclusive_or_expression [p=53]
                │   │   │               │                                                               │                       └── exclusive_or_expression [p=51]
                │   │   │               │                                                               │                           └── and_expression [p=49]
                │   │   │               │                                                               │                               └── equality_expression [p=46]
                │   │   │               │                                                               │                                   └── relational_expression [p=41]
                │   │   │               │                                                               │                                       └── shift_expression [p=38]
                │   │   │               │                                                               │                                           └── additive_expression [p=35]
                │   │   │               │                                                               │                                               └── multiplicative_expression [p=31]
                │   │   │               │                                                               │                                                   └── cast_expression [p=29]
                │   │   │               │                                                               │                                                       └── unary_expression [p=17]
                │   │   │               │                                                               │                                                           └── postfix_expression [p=5]
                │   │   │               │                                                               │                                                               └── primary_expression [p=1]
                │   │   │               │                                                               │                                                                   └── IDENTIFIER("b")
                │   │   │               │                                                               └── RPAREN(")")
                │   │   │               └── SEMI(";")
                │   │   └── block_item [p=213]
                │   │       └── statement [p=203]
                │   │           └── iteration_statement [p=219]
                │   │               ├── WHILE("while")
                │   │               ├── LPAREN("(")
                │   │               ├── expression [p=74]
                │   │               │   └── assignment_expression [p=61]
                │   │               │       └── conditional_expression [p=59]
                │   │               │           └── logical_or_expression [p=57]
                │   │               │               └── logical_and_expression [p=55]
                │   │               │                   └── inclusive_or_expression [p=53]
                │   │               │                       └── exclusive_or_expression [p=51]
                │   │               │                           └── and_expression [p=49]
                │   │               │                               └── equality_expression [p=48]
                │   │               │                                   ├── equality_expression [p=46]
                │   │               │                                   │   └── relational_expression [p=41]
                │   │               │                                   │       └── shift_expression [p=38]
                │   │               │                                   │           └── additive_expression [p=35]
                │   │               │                                   │               └── multiplicative_expression [p=31]
                │   │               │                                   │                   └── cast_expression [p=29]
                │   │               │                                   │                       └── unary_expression [p=17]
                │   │               │                                   │                           └── postfix_expression [p=5]
                │   │               │                                   │                               └── primary_expression [p=1]
                │   │               │                                   │                                   └── IDENTIFIER("a")
                │   │               │                                   ├── NE_OP("!=")
                │   │               │                                   └── relational_expression [p=41]
                │   │               │                                       └── shift_expression [p=38]
                │   │               │                                           └── additive_expression [p=35]
                │   │               │                                               └── multiplicative_expression [p=31]
                │   │               │                                                   └── cast_expression [p=29]
                │   │               │                                                       └── unary_expression [p=17]
                │   │               │                                                           └── postfix_expression [p=5]
                │   │               │                                                               └── primary_expression [p=1]
                │   │               │                                                                   └── IDENTIFIER("b")
                │   │               ├── RPAREN(")")
                │   │               └── statement [p=200]
                │   │                   └── compound_statement [p=209]
                │   │                       ├── LBRACE("{")
                │   │                       ├── block_item_list [p=210]
                │   │                       │   └── block_item [p=213]
                │   │                       │       └── statement [p=201]
                │   │                       │           └── expression_statement [p=215]
                │   │                       │               ├── expression [p=74]
                │   │                       │               │   └── assignment_expression [p=62]
                │   │                       │               │       ├── unary_expression [p=17]
                │   │                       │               │       │   └── postfix_expression [p=5]
                │   │                       │               │       │       └── primary_expression [p=1]
                │   │                       │               │       │           └── IDENTIFIER("a")
                │   │                       │               │       ├── assignment_operator [p=63]
                │   │                       │               │       │   └── ASSIGN("=")
                │   │                       │               │       └── assignment_expression [p=61]
                │   │                       │               │           └── conditional_expression [p=59]
                │   │                       │               │               └── logical_or_expression [p=57]
                │   │                       │               │                   └── logical_and_expression [p=55]
                │   │                       │               │                       └── inclusive_or_expression [p=53]
                │   │                       │               │                           └── exclusive_or_expression [p=51]
                │   │                       │               │                               └── and_expression [p=49]
                │   │                       │               │                                   └── equality_expression [p=46]
                │   │                       │               │                                       └── relational_expression [p=41]
                │   │                       │               │                                           └── shift_expression [p=38]
                │   │                       │               │                                               └── additive_expression [p=35]
                │   │                       │               │                                                   └── multiplicative_expression [p=31]
                │   │                       │               │                                                       └── cast_expression [p=29]
                │   │                       │               │                                                           └── unary_expression [p=17]
                │   │                       │               │                                                               └── postfix_expression [p=8]
                │   │                       │               │                                                                   ├── postfix_expression [p=5]
                │   │                       │               │                                                                   │   └── primary_expression [p=1]
                │   │                       │               │                                                                   │       └── IDENTIFIER("add")
                │   │                       │               │                                                                   ├── LPAREN("(")
                │   │                       │               │                                                                   ├── argument_expression_list [p=16]
                │   │                       │               │                                                                   │   ├── argument_expression_list [p=15]
                │   │                       │               │                                                                   │   │   └── assignment_expression [p=61]
                │   │                       │               │                                                                   │   │       └── conditional_expression [p=59]
                │   │                       │               │                                                                   │   │           └── logical_or_expression [p=57]
                │   │                       │               │                                                                   │   │               └── logical_and_expression [p=55]
                │   │                       │               │                                                                   │   │                   └── inclusive_or_expression [p=53]
                │   │                       │               │                                                                   │   │                       └── exclusive_or_expression [p=51]
                │   │                       │               │                                                                   │   │                           └── and_expression [p=49]
                │   │                       │               │                                                                   │   │                               └── equality_expression [p=46]
                │   │                       │               │                                                                   │   │                                   └── relational_expression [p=41]
                │   │                       │               │                                                                   │   │                                       └── shift_expression [p=38]
                │   │                       │               │                                                                   │   │                                           └── additive_expression [p=35]
                │   │                       │               │                                                                   │   │                                               └── multiplicative_expression [p=31]
                │   │                       │               │                                                                   │   │                                                   └── cast_expression [p=29]
                │   │                       │               │                                                                   │   │                                                       └── unary_expression [p=17]
                │   │                       │               │                                                                   │   │                                                           └── postfix_expression [p=5]
                │   │                       │               │                                                                   │   │                                                               └── primary_expression [p=1]
                │   │                       │               │                                                                   │   │                                                                   └── IDENTIFIER("a")
                │   │                       │               │                                                                   │   ├── COMMA(",")
                │   │                       │               │                                                                   │   └── assignment_expression [p=61]
                │   │                       │               │                                                                   │       └── conditional_expression [p=59]
                │   │                       │               │                                                                   │           └── logical_or_expression [p=57]
                │   │                       │               │                                                                   │               └── logical_and_expression [p=55]
                │   │                       │               │                                                                   │                   └── inclusive_or_expression [p=53]
                │   │                       │               │                                                                   │                       └── exclusive_or_expression [p=51]
                │   │                       │               │                                                                   │                           └── and_expression [p=49]
                │   │                       │               │                                                                   │                               └── equality_expression [p=46]
                │   │                       │               │                                                                   │                                   └── relational_expression [p=41]
                │   │                       │               │                                                                   │                                       └── shift_expression [p=38]
                │   │                       │               │                                                                   │                                           └── additive_expression [p=35]
                │   │                       │               │                                                                   │                                               └── multiplicative_expression [p=31]
                │   │                       │               │                                                                   │                                                   └── cast_expression [p=29]
                │   │                       │               │                                                                   │                                                       └── unary_expression [p=17]
                │   │                       │               │                                                                   │                                                           └── postfix_expression [p=5]
                │   │                       │               │                                                                   │                                                               └── primary_expression [p=2]
                │   │                       │               │                                                                   │                                                                   └── CONSTANT("1")
                │   │                       │               │                                                                   └── RPAREN(")")
                │   │                       │               └── SEMI(";")
                │   │                       └── RBRACE("}")
                │   └── block_item [p=213]
                │       └── statement [p=204]
                │           └── jump_statement [p=229]
                │               ├── RETURN("return")
                │               ├── expression [p=74]
                │               │   └── assignment_expression [p=61]
                │               │       └── conditional_expression [p=59]
                │               │           └── logical_or_expression [p=57]
                │               │               └── logical_and_expression [p=55]
                │               │                   └── inclusive_or_expression [p=53]
                │               │                       └── exclusive_or_expression [p=51]
                │               │                           └── and_expression [p=49]
                │               │                               └── equality_expression [p=46]
                │               │                                   └── relational_expression [p=41]
                │               │                                       └── shift_expression [p=38]
                │               │                                           └── additive_expression [p=35]
                │               │                                               └── multiplicative_expression [p=31]
                │               │                                                   └── cast_expression [p=29]
                │               │                                                       └── unary_expression [p=17]
                │               │                                                           └── postfix_expression [p=5]
                │               │                                                               └── primary_expression [p=1]
                │               │                                                                   └── IDENTIFIER("a")
                │               └── SEMI(";")
                └── RBRACE("}")
```

## 3. Mermaid 可视化语法树

```mermaid
flowchart TD
    n0["n0: translation_unit<br/>production = 231"]:::nonTerminal
    n1["n1: translation_unit<br/>production = 230"]:::nonTerminal
    n2["n2: external_declaration<br/>production = 232"]:::nonTerminal
    n3["n3: function_definition<br/>production = 235"]:::nonTerminal
    n4["n4: declaration_specifiers<br/>production = 81"]:::nonTerminal
    n5["n5: type_specifier<br/>production = 99"]:::nonTerminal
    n6["n6: INT<br/>lexeme = int"]:::terminal
    n7["n7: declarator<br/>production = 142"]:::nonTerminal
    n8["n8: direct_declarator<br/>production = 153"]:::nonTerminal
    n9["n9: direct_declarator<br/>production = 143"]:::nonTerminal
    n10["n10: IDENTIFIER<br/>lexeme = add"]:::terminal
    n11["n11: LPAREN<br/>lexeme = ("]:::terminal
    n12["n12: parameter_type_list<br/>production = 162"]:::nonTerminal
    n13["n13: parameter_list<br/>production = 165"]:::nonTerminal
    n14["n14: parameter_list<br/>production = 164"]:::nonTerminal
    n15["n15: parameter_declaration<br/>production = 166"]:::nonTerminal
    n16["n16: declaration_specifiers<br/>production = 81"]:::nonTerminal
    n17["n17: type_specifier<br/>production = 99"]:::nonTerminal
    n18["n18: INT<br/>lexeme = int"]:::terminal
    n19["n19: declarator<br/>production = 142"]:::nonTerminal
    n20["n20: direct_declarator<br/>production = 143"]:::nonTerminal
    n21["n21: IDENTIFIER<br/>lexeme = x"]:::terminal
    n22["n22: COMMA<br/>lexeme = ,"]:::terminal
    n23["n23: parameter_declaration<br/>production = 166"]:::nonTerminal
    n24["n24: declaration_specifiers<br/>production = 81"]:::nonTerminal
    n25["n25: type_specifier<br/>production = 99"]:::nonTerminal
    n26["n26: INT<br/>lexeme = int"]:::terminal
    n27["n27: declarator<br/>production = 142"]:::nonTerminal
    n28["n28: direct_declarator<br/>production = 143"]:::nonTerminal
    n29["n29: IDENTIFIER<br/>lexeme = y"]:::terminal
    n30["n30: RPAREN<br/>lexeme = )"]:::terminal
    n31["n31: compound_statement<br/>production = 209"]:::nonTerminal
    n32["n32: LBRACE<br/>lexeme = {"]:::terminal
    n33["n33: block_item_list<br/>production = 210"]:::nonTerminal
    n34["n34: block_item<br/>production = 213"]:::nonTerminal
    n35["n35: statement<br/>production = 204"]:::nonTerminal
    n36["n36: jump_statement<br/>production = 229"]:::nonTerminal
    n37["n37: RETURN<br/>lexeme = return"]:::terminal
    n38["n38: expression<br/>production = 74"]:::nonTerminal
    n39["n39: assignment_expression<br/>production = 61"]:::nonTerminal
    n40["n40: conditional_expression<br/>production = 59"]:::nonTerminal
    n41["n41: logical_or_expression<br/>production = 57"]:::nonTerminal
    n42["n42: logical_and_expression<br/>production = 55"]:::nonTerminal
    n43["n43: inclusive_or_expression<br/>production = 53"]:::nonTerminal
    n44["n44: exclusive_or_expression<br/>production = 51"]:::nonTerminal
    n45["n45: and_expression<br/>production = 49"]:::nonTerminal
    n46["n46: equality_expression<br/>production = 46"]:::nonTerminal
    n47["n47: relational_expression<br/>production = 41"]:::nonTerminal
    n48["n48: shift_expression<br/>production = 38"]:::nonTerminal
    n49["n49: additive_expression<br/>production = 36"]:::nonTerminal
    n50["n50: additive_expression<br/>production = 35"]:::nonTerminal
    n51["n51: multiplicative_expression<br/>production = 31"]:::nonTerminal
    n52["n52: cast_expression<br/>production = 29"]:::nonTerminal
    n53["n53: unary_expression<br/>production = 17"]:::nonTerminal
    n54["n54: postfix_expression<br/>production = 5"]:::nonTerminal
    n55["n55: primary_expression<br/>production = 1"]:::nonTerminal
    n56["n56: IDENTIFIER<br/>lexeme = x"]:::terminal
    n57["n57: PLUS<br/>lexeme = +"]:::terminal
    n58["n58: multiplicative_expression<br/>production = 31"]:::nonTerminal
    n59["n59: cast_expression<br/>production = 29"]:::nonTerminal
    n60["n60: unary_expression<br/>production = 17"]:::nonTerminal
    n61["n61: postfix_expression<br/>production = 5"]:::nonTerminal
    n62["n62: primary_expression<br/>production = 1"]:::nonTerminal
    n63["n63: IDENTIFIER<br/>lexeme = y"]:::terminal
    n64["n64: SEMI<br/>lexeme = ;"]:::terminal
    n65["n65: RBRACE<br/>lexeme = }"]:::terminal
    n66["n66: external_declaration<br/>production = 232"]:::nonTerminal
    n67["n67: function_definition<br/>production = 235"]:::nonTerminal
    n68["n68: declaration_specifiers<br/>production = 81"]:::nonTerminal
    n69["n69: type_specifier<br/>production = 99"]:::nonTerminal
    n70["n70: INT<br/>lexeme = int"]:::terminal
    n71["n71: declarator<br/>production = 142"]:::nonTerminal
    n72["n72: direct_declarator<br/>production = 155"]:::nonTerminal
    n73["n73: direct_declarator<br/>production = 143"]:::nonTerminal
    n74["n74: IDENTIFIER<br/>lexeme = main"]:::terminal
    n75["n75: LPAREN<br/>lexeme = ("]:::terminal
    n76["n76: RPAREN<br/>lexeme = )"]:::terminal
    n77["n77: compound_statement<br/>production = 209"]:::nonTerminal
    n78["n78: LBRACE<br/>lexeme = {"]:::terminal
    n79["n79: block_item_list<br/>production = 211"]:::nonTerminal
    n80["n80: block_item_list<br/>production = 211"]:::nonTerminal
    n81["n81: block_item_list<br/>production = 211"]:::nonTerminal
    n82["n82: block_item_list<br/>production = 211"]:::nonTerminal
    n83["n83: block_item_list<br/>production = 211"]:::nonTerminal
    n84["n84: block_item_list<br/>production = 211"]:::nonTerminal
    n85["n85: block_item_list<br/>production = 210"]:::nonTerminal
    n86["n86: block_item<br/>production = 212"]:::nonTerminal
    n87["n87: declaration<br/>production = 78"]:::nonTerminal
    n88["n88: declaration_specifiers<br/>production = 81"]:::nonTerminal
    n89["n89: type_specifier<br/>production = 99"]:::nonTerminal
    n90["n90: INT<br/>lexeme = int"]:::terminal
    n91["n91: init_declarator_list<br/>production = 87"]:::nonTerminal
    n92["n92: init_declarator<br/>production = 89"]:::nonTerminal
    n93["n93: declarator<br/>production = 142"]:::nonTerminal
    n94["n94: direct_declarator<br/>production = 143"]:::nonTerminal
    n95["n95: IDENTIFIER<br/>lexeme = a"]:::terminal
    n96["n96: SEMI<br/>lexeme = ;"]:::terminal
    n97["n97: block_item<br/>production = 212"]:::nonTerminal
    n98["n98: declaration<br/>production = 78"]:::nonTerminal
    n99["n99: declaration_specifiers<br/>production = 81"]:::nonTerminal
    n100["n100: type_specifier<br/>production = 99"]:::nonTerminal
    n101["n101: INT<br/>lexeme = int"]:::terminal
    n102["n102: init_declarator_list<br/>production = 87"]:::nonTerminal
    n103["n103: init_declarator<br/>production = 90"]:::nonTerminal
    n104["n104: declarator<br/>production = 142"]:::nonTerminal
    n105["n105: direct_declarator<br/>production = 143"]:::nonTerminal
    n106["n106: IDENTIFIER<br/>lexeme = b"]:::terminal
    n107["n107: ASSIGN<br/>lexeme = ="]:::terminal
    n108["n108: initializer<br/>production = 187"]:::nonTerminal
    n109["n109: assignment_expression<br/>production = 61"]:::nonTerminal
    n110["n110: conditional_expression<br/>production = 59"]:::nonTerminal
    n111["n111: logical_or_expression<br/>production = 57"]:::nonTerminal
    n112["n112: logical_and_expression<br/>production = 55"]:::nonTerminal
    n113["n113: inclusive_or_expression<br/>production = 53"]:::nonTerminal
    n114["n114: exclusive_or_expression<br/>production = 51"]:::nonTerminal
    n115["n115: and_expression<br/>production = 49"]:::nonTerminal
    n116["n116: equality_expression<br/>production = 46"]:::nonTerminal
    n117["n117: relational_expression<br/>production = 41"]:::nonTerminal
    n118["n118: shift_expression<br/>production = 38"]:::nonTerminal
    n119["n119: additive_expression<br/>production = 35"]:::nonTerminal
    n120["n120: multiplicative_expression<br/>production = 31"]:::nonTerminal
    n121["n121: cast_expression<br/>production = 29"]:::nonTerminal
    n122["n122: unary_expression<br/>production = 17"]:::nonTerminal
    n123["n123: postfix_expression<br/>production = 5"]:::nonTerminal
    n124["n124: primary_expression<br/>production = 2"]:::nonTerminal
    n125["n125: CONSTANT<br/>lexeme = 5"]:::terminal
    n126["n126: SEMI<br/>lexeme = ;"]:::terminal
    n127["n127: block_item<br/>production = 213"]:::nonTerminal
    n128["n128: statement<br/>production = 201"]:::nonTerminal
    n129["n129: expression_statement<br/>production = 215"]:::nonTerminal
    n130["n130: expression<br/>production = 74"]:::nonTerminal
    n131["n131: assignment_expression<br/>production = 62"]:::nonTerminal
    n132["n132: unary_expression<br/>production = 17"]:::nonTerminal
    n133["n133: postfix_expression<br/>production = 5"]:::nonTerminal
    n134["n134: primary_expression<br/>production = 1"]:::nonTerminal
    n135["n135: IDENTIFIER<br/>lexeme = a"]:::terminal
    n136["n136: assignment_operator<br/>production = 63"]:::nonTerminal
    n137["n137: ASSIGN<br/>lexeme = ="]:::terminal
    n138["n138: assignment_expression<br/>production = 61"]:::nonTerminal
    n139["n139: conditional_expression<br/>production = 59"]:::nonTerminal
    n140["n140: logical_or_expression<br/>production = 57"]:::nonTerminal
    n141["n141: logical_and_expression<br/>production = 55"]:::nonTerminal
    n142["n142: inclusive_or_expression<br/>production = 53"]:::nonTerminal
    n143["n143: exclusive_or_expression<br/>production = 51"]:::nonTerminal
    n144["n144: and_expression<br/>production = 49"]:::nonTerminal
    n145["n145: equality_expression<br/>production = 46"]:::nonTerminal
    n146["n146: relational_expression<br/>production = 41"]:::nonTerminal
    n147["n147: shift_expression<br/>production = 38"]:::nonTerminal
    n148["n148: additive_expression<br/>production = 35"]:::nonTerminal
    n149["n149: multiplicative_expression<br/>production = 31"]:::nonTerminal
    n150["n150: cast_expression<br/>production = 29"]:::nonTerminal
    n151["n151: unary_expression<br/>production = 17"]:::nonTerminal
    n152["n152: postfix_expression<br/>production = 8"]:::nonTerminal
    n153["n153: postfix_expression<br/>production = 5"]:::nonTerminal
    n154["n154: primary_expression<br/>production = 1"]:::nonTerminal
    n155["n155: IDENTIFIER<br/>lexeme = add"]:::terminal
    n156["n156: LPAREN<br/>lexeme = ("]:::terminal
    n157["n157: argument_expression_list<br/>production = 16"]:::nonTerminal
    n158["n158: argument_expression_list<br/>production = 15"]:::nonTerminal
    n159["n159: assignment_expression<br/>production = 61"]:::nonTerminal
    n160["n160: conditional_expression<br/>production = 59"]:::nonTerminal
    n161["n161: logical_or_expression<br/>production = 57"]:::nonTerminal
    n162["n162: logical_and_expression<br/>production = 55"]:::nonTerminal
    n163["n163: inclusive_or_expression<br/>production = 53"]:::nonTerminal
    n164["n164: exclusive_or_expression<br/>production = 51"]:::nonTerminal
    n165["n165: and_expression<br/>production = 49"]:::nonTerminal
    n166["n166: equality_expression<br/>production = 46"]:::nonTerminal
    n167["n167: relational_expression<br/>production = 41"]:::nonTerminal
    n168["n168: shift_expression<br/>production = 38"]:::nonTerminal
    n169["n169: additive_expression<br/>production = 35"]:::nonTerminal
    n170["n170: multiplicative_expression<br/>production = 31"]:::nonTerminal
    n171["n171: cast_expression<br/>production = 29"]:::nonTerminal
    n172["n172: unary_expression<br/>production = 17"]:::nonTerminal
    n173["n173: postfix_expression<br/>production = 5"]:::nonTerminal
    n174["n174: primary_expression<br/>production = 1"]:::nonTerminal
    n175["n175: IDENTIFIER<br/>lexeme = b"]:::terminal
    n176["n176: COMMA<br/>lexeme = ,"]:::terminal
    n177["n177: assignment_expression<br/>production = 61"]:::nonTerminal
    n178["n178: conditional_expression<br/>production = 59"]:::nonTerminal
    n179["n179: logical_or_expression<br/>production = 57"]:::nonTerminal
    n180["n180: logical_and_expression<br/>production = 55"]:::nonTerminal
    n181["n181: inclusive_or_expression<br/>production = 53"]:::nonTerminal
    n182["n182: exclusive_or_expression<br/>production = 51"]:::nonTerminal
    n183["n183: and_expression<br/>production = 49"]:::nonTerminal
    n184["n184: equality_expression<br/>production = 46"]:::nonTerminal
    n185["n185: relational_expression<br/>production = 41"]:::nonTerminal
    n186["n186: shift_expression<br/>production = 38"]:::nonTerminal
    n187["n187: additive_expression<br/>production = 35"]:::nonTerminal
    n188["n188: multiplicative_expression<br/>production = 31"]:::nonTerminal
    n189["n189: cast_expression<br/>production = 29"]:::nonTerminal
    n190["n190: unary_expression<br/>production = 17"]:::nonTerminal
    n191["n191: postfix_expression<br/>production = 5"]:::nonTerminal
    n192["n192: primary_expression<br/>production = 2"]:::nonTerminal
    n193["n193: CONSTANT<br/>lexeme = 3"]:::terminal
    n194["n194: RPAREN<br/>lexeme = )"]:::terminal
    n195["n195: SEMI<br/>lexeme = ;"]:::terminal
    n196["n196: block_item<br/>production = 213"]:::nonTerminal
    n197["n197: statement<br/>production = 202"]:::nonTerminal
    n198["n198: selection_statement<br/>production = 217"]:::nonTerminal
    n199["n199: IF<br/>lexeme = if"]:::terminal
    n200["n200: LPAREN<br/>lexeme = ("]:::terminal
    n201["n201: expression<br/>production = 74"]:::nonTerminal
    n202["n202: assignment_expression<br/>production = 61"]:::nonTerminal
    n203["n203: conditional_expression<br/>production = 59"]:::nonTerminal
    n204["n204: logical_or_expression<br/>production = 57"]:::nonTerminal
    n205["n205: logical_and_expression<br/>production = 55"]:::nonTerminal
    n206["n206: inclusive_or_expression<br/>production = 53"]:::nonTerminal
    n207["n207: exclusive_or_expression<br/>production = 51"]:::nonTerminal
    n208["n208: and_expression<br/>production = 49"]:::nonTerminal
    n209["n209: equality_expression<br/>production = 46"]:::nonTerminal
    n210["n210: relational_expression<br/>production = 42"]:::nonTerminal
    n211["n211: relational_expression<br/>production = 41"]:::nonTerminal
    n212["n212: shift_expression<br/>production = 38"]:::nonTerminal
    n213["n213: additive_expression<br/>production = 35"]:::nonTerminal
    n214["n214: multiplicative_expression<br/>production = 31"]:::nonTerminal
    n215["n215: cast_expression<br/>production = 29"]:::nonTerminal
    n216["n216: unary_expression<br/>production = 17"]:::nonTerminal
    n217["n217: postfix_expression<br/>production = 5"]:::nonTerminal
    n218["n218: primary_expression<br/>production = 1"]:::nonTerminal
    n219["n219: IDENTIFIER<br/>lexeme = a"]:::terminal
    n220["n220: LT<br/>lexeme = &lt;"]:::terminal
    n221["n221: shift_expression<br/>production = 38"]:::nonTerminal
    n222["n222: additive_expression<br/>production = 35"]:::nonTerminal
    n223["n223: multiplicative_expression<br/>production = 31"]:::nonTerminal
    n224["n224: cast_expression<br/>production = 29"]:::nonTerminal
    n225["n225: unary_expression<br/>production = 17"]:::nonTerminal
    n226["n226: postfix_expression<br/>production = 5"]:::nonTerminal
    n227["n227: primary_expression<br/>production = 1"]:::nonTerminal
    n228["n228: IDENTIFIER<br/>lexeme = b"]:::terminal
    n229["n229: RPAREN<br/>lexeme = )"]:::terminal
    n230["n230: statement<br/>production = 201"]:::nonTerminal
    n231["n231: expression_statement<br/>production = 215"]:::nonTerminal
    n232["n232: expression<br/>production = 74"]:::nonTerminal
    n233["n233: assignment_expression<br/>production = 62"]:::nonTerminal
    n234["n234: unary_expression<br/>production = 17"]:::nonTerminal
    n235["n235: postfix_expression<br/>production = 5"]:::nonTerminal
    n236["n236: primary_expression<br/>production = 1"]:::nonTerminal
    n237["n237: IDENTIFIER<br/>lexeme = a"]:::terminal
    n238["n238: assignment_operator<br/>production = 63"]:::nonTerminal
    n239["n239: ASSIGN<br/>lexeme = ="]:::terminal
    n240["n240: assignment_expression<br/>production = 61"]:::nonTerminal
    n241["n241: conditional_expression<br/>production = 59"]:::nonTerminal
    n242["n242: logical_or_expression<br/>production = 57"]:::nonTerminal
    n243["n243: logical_and_expression<br/>production = 55"]:::nonTerminal
    n244["n244: inclusive_or_expression<br/>production = 53"]:::nonTerminal
    n245["n245: exclusive_or_expression<br/>production = 51"]:::nonTerminal
    n246["n246: and_expression<br/>production = 49"]:::nonTerminal
    n247["n247: equality_expression<br/>production = 46"]:::nonTerminal
    n248["n248: relational_expression<br/>production = 41"]:::nonTerminal
    n249["n249: shift_expression<br/>production = 38"]:::nonTerminal
    n250["n250: additive_expression<br/>production = 35"]:::nonTerminal
    n251["n251: multiplicative_expression<br/>production = 31"]:::nonTerminal
    n252["n252: cast_expression<br/>production = 29"]:::nonTerminal
    n253["n253: unary_expression<br/>production = 17"]:::nonTerminal
    n254["n254: postfix_expression<br/>production = 8"]:::nonTerminal
    n255["n255: postfix_expression<br/>production = 5"]:::nonTerminal
    n256["n256: primary_expression<br/>production = 1"]:::nonTerminal
    n257["n257: IDENTIFIER<br/>lexeme = add"]:::terminal
    n258["n258: LPAREN<br/>lexeme = ("]:::terminal
    n259["n259: argument_expression_list<br/>production = 16"]:::nonTerminal
    n260["n260: argument_expression_list<br/>production = 15"]:::nonTerminal
    n261["n261: assignment_expression<br/>production = 61"]:::nonTerminal
    n262["n262: conditional_expression<br/>production = 59"]:::nonTerminal
    n263["n263: logical_or_expression<br/>production = 57"]:::nonTerminal
    n264["n264: logical_and_expression<br/>production = 55"]:::nonTerminal
    n265["n265: inclusive_or_expression<br/>production = 53"]:::nonTerminal
    n266["n266: exclusive_or_expression<br/>production = 51"]:::nonTerminal
    n267["n267: and_expression<br/>production = 49"]:::nonTerminal
    n268["n268: equality_expression<br/>production = 46"]:::nonTerminal
    n269["n269: relational_expression<br/>production = 41"]:::nonTerminal
    n270["n270: shift_expression<br/>production = 38"]:::nonTerminal
    n271["n271: additive_expression<br/>production = 35"]:::nonTerminal
    n272["n272: multiplicative_expression<br/>production = 31"]:::nonTerminal
    n273["n273: cast_expression<br/>production = 29"]:::nonTerminal
    n274["n274: unary_expression<br/>production = 17"]:::nonTerminal
    n275["n275: postfix_expression<br/>production = 5"]:::nonTerminal
    n276["n276: primary_expression<br/>production = 1"]:::nonTerminal
    n277["n277: IDENTIFIER<br/>lexeme = a"]:::terminal
    n278["n278: COMMA<br/>lexeme = ,"]:::terminal
    n279["n279: assignment_expression<br/>production = 61"]:::nonTerminal
    n280["n280: conditional_expression<br/>production = 59"]:::nonTerminal
    n281["n281: logical_or_expression<br/>production = 57"]:::nonTerminal
    n282["n282: logical_and_expression<br/>production = 55"]:::nonTerminal
    n283["n283: inclusive_or_expression<br/>production = 53"]:::nonTerminal
    n284["n284: exclusive_or_expression<br/>production = 51"]:::nonTerminal
    n285["n285: and_expression<br/>production = 49"]:::nonTerminal
    n286["n286: equality_expression<br/>production = 46"]:::nonTerminal
    n287["n287: relational_expression<br/>production = 41"]:::nonTerminal
    n288["n288: shift_expression<br/>production = 38"]:::nonTerminal
    n289["n289: additive_expression<br/>production = 35"]:::nonTerminal
    n290["n290: multiplicative_expression<br/>production = 31"]:::nonTerminal
    n291["n291: cast_expression<br/>production = 29"]:::nonTerminal
    n292["n292: unary_expression<br/>production = 17"]:::nonTerminal
    n293["n293: postfix_expression<br/>production = 5"]:::nonTerminal
    n294["n294: primary_expression<br/>production = 2"]:::nonTerminal
    n295["n295: CONSTANT<br/>lexeme = 1"]:::terminal
    n296["n296: RPAREN<br/>lexeme = )"]:::terminal
    n297["n297: SEMI<br/>lexeme = ;"]:::terminal
    n298["n298: ELSE<br/>lexeme = else"]:::terminal
    n299["n299: statement<br/>production = 201"]:::nonTerminal
    n300["n300: expression_statement<br/>production = 215"]:::nonTerminal
    n301["n301: expression<br/>production = 74"]:::nonTerminal
    n302["n302: assignment_expression<br/>production = 62"]:::nonTerminal
    n303["n303: unary_expression<br/>production = 17"]:::nonTerminal
    n304["n304: postfix_expression<br/>production = 5"]:::nonTerminal
    n305["n305: primary_expression<br/>production = 1"]:::nonTerminal
    n306["n306: IDENTIFIER<br/>lexeme = a"]:::terminal
    n307["n307: assignment_operator<br/>production = 63"]:::nonTerminal
    n308["n308: ASSIGN<br/>lexeme = ="]:::terminal
    n309["n309: assignment_expression<br/>production = 61"]:::nonTerminal
    n310["n310: conditional_expression<br/>production = 59"]:::nonTerminal
    n311["n311: logical_or_expression<br/>production = 57"]:::nonTerminal
    n312["n312: logical_and_expression<br/>production = 55"]:::nonTerminal
    n313["n313: inclusive_or_expression<br/>production = 53"]:::nonTerminal
    n314["n314: exclusive_or_expression<br/>production = 51"]:::nonTerminal
    n315["n315: and_expression<br/>production = 49"]:::nonTerminal
    n316["n316: equality_expression<br/>production = 46"]:::nonTerminal
    n317["n317: relational_expression<br/>production = 41"]:::nonTerminal
    n318["n318: shift_expression<br/>production = 38"]:::nonTerminal
    n319["n319: additive_expression<br/>production = 35"]:::nonTerminal
    n320["n320: multiplicative_expression<br/>production = 31"]:::nonTerminal
    n321["n321: cast_expression<br/>production = 29"]:::nonTerminal
    n322["n322: unary_expression<br/>production = 17"]:::nonTerminal
    n323["n323: postfix_expression<br/>production = 8"]:::nonTerminal
    n324["n324: postfix_expression<br/>production = 5"]:::nonTerminal
    n325["n325: primary_expression<br/>production = 1"]:::nonTerminal
    n326["n326: IDENTIFIER<br/>lexeme = add"]:::terminal
    n327["n327: LPAREN<br/>lexeme = ("]:::terminal
    n328["n328: argument_expression_list<br/>production = 16"]:::nonTerminal
    n329["n329: argument_expression_list<br/>production = 15"]:::nonTerminal
    n330["n330: assignment_expression<br/>production = 61"]:::nonTerminal
    n331["n331: conditional_expression<br/>production = 59"]:::nonTerminal
    n332["n332: logical_or_expression<br/>production = 57"]:::nonTerminal
    n333["n333: logical_and_expression<br/>production = 55"]:::nonTerminal
    n334["n334: inclusive_or_expression<br/>production = 53"]:::nonTerminal
    n335["n335: exclusive_or_expression<br/>production = 51"]:::nonTerminal
    n336["n336: and_expression<br/>production = 49"]:::nonTerminal
    n337["n337: equality_expression<br/>production = 46"]:::nonTerminal
    n338["n338: relational_expression<br/>production = 41"]:::nonTerminal
    n339["n339: shift_expression<br/>production = 38"]:::nonTerminal
    n340["n340: additive_expression<br/>production = 35"]:::nonTerminal
    n341["n341: multiplicative_expression<br/>production = 31"]:::nonTerminal
    n342["n342: cast_expression<br/>production = 29"]:::nonTerminal
    n343["n343: unary_expression<br/>production = 17"]:::nonTerminal
    n344["n344: postfix_expression<br/>production = 5"]:::nonTerminal
    n345["n345: primary_expression<br/>production = 1"]:::nonTerminal
    n346["n346: IDENTIFIER<br/>lexeme = a"]:::terminal
    n347["n347: COMMA<br/>lexeme = ,"]:::terminal
    n348["n348: assignment_expression<br/>production = 61"]:::nonTerminal
    n349["n349: conditional_expression<br/>production = 59"]:::nonTerminal
    n350["n350: logical_or_expression<br/>production = 57"]:::nonTerminal
    n351["n351: logical_and_expression<br/>production = 55"]:::nonTerminal
    n352["n352: inclusive_or_expression<br/>production = 53"]:::nonTerminal
    n353["n353: exclusive_or_expression<br/>production = 51"]:::nonTerminal
    n354["n354: and_expression<br/>production = 49"]:::nonTerminal
    n355["n355: equality_expression<br/>production = 46"]:::nonTerminal
    n356["n356: relational_expression<br/>production = 41"]:::nonTerminal
    n357["n357: shift_expression<br/>production = 38"]:::nonTerminal
    n358["n358: additive_expression<br/>production = 35"]:::nonTerminal
    n359["n359: multiplicative_expression<br/>production = 31"]:::nonTerminal
    n360["n360: cast_expression<br/>production = 29"]:::nonTerminal
    n361["n361: unary_expression<br/>production = 17"]:::nonTerminal
    n362["n362: postfix_expression<br/>production = 5"]:::nonTerminal
    n363["n363: primary_expression<br/>production = 1"]:::nonTerminal
    n364["n364: IDENTIFIER<br/>lexeme = b"]:::terminal
    n365["n365: RPAREN<br/>lexeme = )"]:::terminal
    n366["n366: SEMI<br/>lexeme = ;"]:::terminal
    n367["n367: block_item<br/>production = 213"]:::nonTerminal
    n368["n368: statement<br/>production = 201"]:::nonTerminal
    n369["n369: expression_statement<br/>production = 215"]:::nonTerminal
    n370["n370: expression<br/>production = 74"]:::nonTerminal
    n371["n371: assignment_expression<br/>production = 61"]:::nonTerminal
    n372["n372: conditional_expression<br/>production = 59"]:::nonTerminal
    n373["n373: logical_or_expression<br/>production = 57"]:::nonTerminal
    n374["n374: logical_and_expression<br/>production = 55"]:::nonTerminal
    n375["n375: inclusive_or_expression<br/>production = 53"]:::nonTerminal
    n376["n376: exclusive_or_expression<br/>production = 51"]:::nonTerminal
    n377["n377: and_expression<br/>production = 49"]:::nonTerminal
    n378["n378: equality_expression<br/>production = 46"]:::nonTerminal
    n379["n379: relational_expression<br/>production = 41"]:::nonTerminal
    n380["n380: shift_expression<br/>production = 38"]:::nonTerminal
    n381["n381: additive_expression<br/>production = 35"]:::nonTerminal
    n382["n382: multiplicative_expression<br/>production = 31"]:::nonTerminal
    n383["n383: cast_expression<br/>production = 29"]:::nonTerminal
    n384["n384: unary_expression<br/>production = 17"]:::nonTerminal
    n385["n385: postfix_expression<br/>production = 8"]:::nonTerminal
    n386["n386: postfix_expression<br/>production = 5"]:::nonTerminal
    n387["n387: primary_expression<br/>production = 1"]:::nonTerminal
    n388["n388: IDENTIFIER<br/>lexeme = add"]:::terminal
    n389["n389: LPAREN<br/>lexeme = ("]:::terminal
    n390["n390: argument_expression_list<br/>production = 16"]:::nonTerminal
    n391["n391: argument_expression_list<br/>production = 15"]:::nonTerminal
    n392["n392: assignment_expression<br/>production = 61"]:::nonTerminal
    n393["n393: conditional_expression<br/>production = 59"]:::nonTerminal
    n394["n394: logical_or_expression<br/>production = 57"]:::nonTerminal
    n395["n395: logical_and_expression<br/>production = 55"]:::nonTerminal
    n396["n396: inclusive_or_expression<br/>production = 53"]:::nonTerminal
    n397["n397: exclusive_or_expression<br/>production = 51"]:::nonTerminal
    n398["n398: and_expression<br/>production = 49"]:::nonTerminal
    n399["n399: equality_expression<br/>production = 46"]:::nonTerminal
    n400["n400: relational_expression<br/>production = 41"]:::nonTerminal
    n401["n401: shift_expression<br/>production = 38"]:::nonTerminal
    n402["n402: additive_expression<br/>production = 35"]:::nonTerminal
    n403["n403: multiplicative_expression<br/>production = 31"]:::nonTerminal
    n404["n404: cast_expression<br/>production = 29"]:::nonTerminal
    n405["n405: unary_expression<br/>production = 17"]:::nonTerminal
    n406["n406: postfix_expression<br/>production = 5"]:::nonTerminal
    n407["n407: primary_expression<br/>production = 1"]:::nonTerminal
    n408["n408: IDENTIFIER<br/>lexeme = a"]:::terminal
    n409["n409: COMMA<br/>lexeme = ,"]:::terminal
    n410["n410: assignment_expression<br/>production = 61"]:::nonTerminal
    n411["n411: conditional_expression<br/>production = 59"]:::nonTerminal
    n412["n412: logical_or_expression<br/>production = 57"]:::nonTerminal
    n413["n413: logical_and_expression<br/>production = 55"]:::nonTerminal
    n414["n414: inclusive_or_expression<br/>production = 53"]:::nonTerminal
    n415["n415: exclusive_or_expression<br/>production = 51"]:::nonTerminal
    n416["n416: and_expression<br/>production = 49"]:::nonTerminal
    n417["n417: equality_expression<br/>production = 46"]:::nonTerminal
    n418["n418: relational_expression<br/>production = 41"]:::nonTerminal
    n419["n419: shift_expression<br/>production = 38"]:::nonTerminal
    n420["n420: additive_expression<br/>production = 35"]:::nonTerminal
    n421["n421: multiplicative_expression<br/>production = 31"]:::nonTerminal
    n422["n422: cast_expression<br/>production = 29"]:::nonTerminal
    n423["n423: unary_expression<br/>production = 17"]:::nonTerminal
    n424["n424: postfix_expression<br/>production = 5"]:::nonTerminal
    n425["n425: primary_expression<br/>production = 1"]:::nonTerminal
    n426["n426: IDENTIFIER<br/>lexeme = b"]:::terminal
    n427["n427: RPAREN<br/>lexeme = )"]:::terminal
    n428["n428: SEMI<br/>lexeme = ;"]:::terminal
    n429["n429: block_item<br/>production = 213"]:::nonTerminal
    n430["n430: statement<br/>production = 203"]:::nonTerminal
    n431["n431: iteration_statement<br/>production = 219"]:::nonTerminal
    n432["n432: WHILE<br/>lexeme = while"]:::terminal
    n433["n433: LPAREN<br/>lexeme = ("]:::terminal
    n434["n434: expression<br/>production = 74"]:::nonTerminal
    n435["n435: assignment_expression<br/>production = 61"]:::nonTerminal
    n436["n436: conditional_expression<br/>production = 59"]:::nonTerminal
    n437["n437: logical_or_expression<br/>production = 57"]:::nonTerminal
    n438["n438: logical_and_expression<br/>production = 55"]:::nonTerminal
    n439["n439: inclusive_or_expression<br/>production = 53"]:::nonTerminal
    n440["n440: exclusive_or_expression<br/>production = 51"]:::nonTerminal
    n441["n441: and_expression<br/>production = 49"]:::nonTerminal
    n442["n442: equality_expression<br/>production = 48"]:::nonTerminal
    n443["n443: equality_expression<br/>production = 46"]:::nonTerminal
    n444["n444: relational_expression<br/>production = 41"]:::nonTerminal
    n445["n445: shift_expression<br/>production = 38"]:::nonTerminal
    n446["n446: additive_expression<br/>production = 35"]:::nonTerminal
    n447["n447: multiplicative_expression<br/>production = 31"]:::nonTerminal
    n448["n448: cast_expression<br/>production = 29"]:::nonTerminal
    n449["n449: unary_expression<br/>production = 17"]:::nonTerminal
    n450["n450: postfix_expression<br/>production = 5"]:::nonTerminal
    n451["n451: primary_expression<br/>production = 1"]:::nonTerminal
    n452["n452: IDENTIFIER<br/>lexeme = a"]:::terminal
    n453["n453: NE_OP<br/>lexeme = !="]:::terminal
    n454["n454: relational_expression<br/>production = 41"]:::nonTerminal
    n455["n455: shift_expression<br/>production = 38"]:::nonTerminal
    n456["n456: additive_expression<br/>production = 35"]:::nonTerminal
    n457["n457: multiplicative_expression<br/>production = 31"]:::nonTerminal
    n458["n458: cast_expression<br/>production = 29"]:::nonTerminal
    n459["n459: unary_expression<br/>production = 17"]:::nonTerminal
    n460["n460: postfix_expression<br/>production = 5"]:::nonTerminal
    n461["n461: primary_expression<br/>production = 1"]:::nonTerminal
    n462["n462: IDENTIFIER<br/>lexeme = b"]:::terminal
    n463["n463: RPAREN<br/>lexeme = )"]:::terminal
    n464["n464: statement<br/>production = 200"]:::nonTerminal
    n465["n465: compound_statement<br/>production = 209"]:::nonTerminal
    n466["n466: LBRACE<br/>lexeme = {"]:::terminal
    n467["n467: block_item_list<br/>production = 210"]:::nonTerminal
    n468["n468: block_item<br/>production = 213"]:::nonTerminal
    n469["n469: statement<br/>production = 201"]:::nonTerminal
    n470["n470: expression_statement<br/>production = 215"]:::nonTerminal
    n471["n471: expression<br/>production = 74"]:::nonTerminal
    n472["n472: assignment_expression<br/>production = 62"]:::nonTerminal
    n473["n473: unary_expression<br/>production = 17"]:::nonTerminal
    n474["n474: postfix_expression<br/>production = 5"]:::nonTerminal
    n475["n475: primary_expression<br/>production = 1"]:::nonTerminal
    n476["n476: IDENTIFIER<br/>lexeme = a"]:::terminal
    n477["n477: assignment_operator<br/>production = 63"]:::nonTerminal
    n478["n478: ASSIGN<br/>lexeme = ="]:::terminal
    n479["n479: assignment_expression<br/>production = 61"]:::nonTerminal
    n480["n480: conditional_expression<br/>production = 59"]:::nonTerminal
    n481["n481: logical_or_expression<br/>production = 57"]:::nonTerminal
    n482["n482: logical_and_expression<br/>production = 55"]:::nonTerminal
    n483["n483: inclusive_or_expression<br/>production = 53"]:::nonTerminal
    n484["n484: exclusive_or_expression<br/>production = 51"]:::nonTerminal
    n485["n485: and_expression<br/>production = 49"]:::nonTerminal
    n486["n486: equality_expression<br/>production = 46"]:::nonTerminal
    n487["n487: relational_expression<br/>production = 41"]:::nonTerminal
    n488["n488: shift_expression<br/>production = 38"]:::nonTerminal
    n489["n489: additive_expression<br/>production = 35"]:::nonTerminal
    n490["n490: multiplicative_expression<br/>production = 31"]:::nonTerminal
    n491["n491: cast_expression<br/>production = 29"]:::nonTerminal
    n492["n492: unary_expression<br/>production = 17"]:::nonTerminal
    n493["n493: postfix_expression<br/>production = 8"]:::nonTerminal
    n494["n494: postfix_expression<br/>production = 5"]:::nonTerminal
    n495["n495: primary_expression<br/>production = 1"]:::nonTerminal
    n496["n496: IDENTIFIER<br/>lexeme = add"]:::terminal
    n497["n497: LPAREN<br/>lexeme = ("]:::terminal
    n498["n498: argument_expression_list<br/>production = 16"]:::nonTerminal
    n499["n499: argument_expression_list<br/>production = 15"]:::nonTerminal
    n500["n500: assignment_expression<br/>production = 61"]:::nonTerminal
    n501["n501: conditional_expression<br/>production = 59"]:::nonTerminal
    n502["n502: logical_or_expression<br/>production = 57"]:::nonTerminal
    n503["n503: logical_and_expression<br/>production = 55"]:::nonTerminal
    n504["n504: inclusive_or_expression<br/>production = 53"]:::nonTerminal
    n505["n505: exclusive_or_expression<br/>production = 51"]:::nonTerminal
    n506["n506: and_expression<br/>production = 49"]:::nonTerminal
    n507["n507: equality_expression<br/>production = 46"]:::nonTerminal
    n508["n508: relational_expression<br/>production = 41"]:::nonTerminal
    n509["n509: shift_expression<br/>production = 38"]:::nonTerminal
    n510["n510: additive_expression<br/>production = 35"]:::nonTerminal
    n511["n511: multiplicative_expression<br/>production = 31"]:::nonTerminal
    n512["n512: cast_expression<br/>production = 29"]:::nonTerminal
    n513["n513: unary_expression<br/>production = 17"]:::nonTerminal
    n514["n514: postfix_expression<br/>production = 5"]:::nonTerminal
    n515["n515: primary_expression<br/>production = 1"]:::nonTerminal
    n516["n516: IDENTIFIER<br/>lexeme = a"]:::terminal
    n517["n517: COMMA<br/>lexeme = ,"]:::terminal
    n518["n518: assignment_expression<br/>production = 61"]:::nonTerminal
    n519["n519: conditional_expression<br/>production = 59"]:::nonTerminal
    n520["n520: logical_or_expression<br/>production = 57"]:::nonTerminal
    n521["n521: logical_and_expression<br/>production = 55"]:::nonTerminal
    n522["n522: inclusive_or_expression<br/>production = 53"]:::nonTerminal
    n523["n523: exclusive_or_expression<br/>production = 51"]:::nonTerminal
    n524["n524: and_expression<br/>production = 49"]:::nonTerminal
    n525["n525: equality_expression<br/>production = 46"]:::nonTerminal
    n526["n526: relational_expression<br/>production = 41"]:::nonTerminal
    n527["n527: shift_expression<br/>production = 38"]:::nonTerminal
    n528["n528: additive_expression<br/>production = 35"]:::nonTerminal
    n529["n529: multiplicative_expression<br/>production = 31"]:::nonTerminal
    n530["n530: cast_expression<br/>production = 29"]:::nonTerminal
    n531["n531: unary_expression<br/>production = 17"]:::nonTerminal
    n532["n532: postfix_expression<br/>production = 5"]:::nonTerminal
    n533["n533: primary_expression<br/>production = 2"]:::nonTerminal
    n534["n534: CONSTANT<br/>lexeme = 1"]:::terminal
    n535["n535: RPAREN<br/>lexeme = )"]:::terminal
    n536["n536: SEMI<br/>lexeme = ;"]:::terminal
    n537["n537: RBRACE<br/>lexeme = }"]:::terminal
    n538["n538: block_item<br/>production = 213"]:::nonTerminal
    n539["n539: statement<br/>production = 204"]:::nonTerminal
    n540["n540: jump_statement<br/>production = 229"]:::nonTerminal
    n541["n541: RETURN<br/>lexeme = return"]:::terminal
    n542["n542: expression<br/>production = 74"]:::nonTerminal
    n543["n543: assignment_expression<br/>production = 61"]:::nonTerminal
    n544["n544: conditional_expression<br/>production = 59"]:::nonTerminal
    n545["n545: logical_or_expression<br/>production = 57"]:::nonTerminal
    n546["n546: logical_and_expression<br/>production = 55"]:::nonTerminal
    n547["n547: inclusive_or_expression<br/>production = 53"]:::nonTerminal
    n548["n548: exclusive_or_expression<br/>production = 51"]:::nonTerminal
    n549["n549: and_expression<br/>production = 49"]:::nonTerminal
    n550["n550: equality_expression<br/>production = 46"]:::nonTerminal
    n551["n551: relational_expression<br/>production = 41"]:::nonTerminal
    n552["n552: shift_expression<br/>production = 38"]:::nonTerminal
    n553["n553: additive_expression<br/>production = 35"]:::nonTerminal
    n554["n554: multiplicative_expression<br/>production = 31"]:::nonTerminal
    n555["n555: cast_expression<br/>production = 29"]:::nonTerminal
    n556["n556: unary_expression<br/>production = 17"]:::nonTerminal
    n557["n557: postfix_expression<br/>production = 5"]:::nonTerminal
    n558["n558: primary_expression<br/>production = 1"]:::nonTerminal
    n559["n559: IDENTIFIER<br/>lexeme = a"]:::terminal
    n560["n560: SEMI<br/>lexeme = ;"]:::terminal
    n561["n561: RBRACE<br/>lexeme = }"]:::terminal
    n0 --> n1
    n0 --> n66
    n1 --> n2
    n2 --> n3
    n3 --> n4
    n3 --> n7
    n3 --> n31
    n4 --> n5
    n5 --> n6
    n7 --> n8
    n8 --> n9
    n8 --> n11
    n8 --> n12
    n8 --> n30
    n9 --> n10
    n12 --> n13
    n13 --> n14
    n13 --> n22
    n13 --> n23
    n14 --> n15
    n15 --> n16
    n15 --> n19
    n16 --> n17
    n17 --> n18
    n19 --> n20
    n20 --> n21
    n23 --> n24
    n23 --> n27
    n24 --> n25
    n25 --> n26
    n27 --> n28
    n28 --> n29
    n31 --> n32
    n31 --> n33
    n31 --> n65
    n33 --> n34
    n34 --> n35
    n35 --> n36
    n36 --> n37
    n36 --> n38
    n36 --> n64
    n38 --> n39
    n39 --> n40
    n40 --> n41
    n41 --> n42
    n42 --> n43
    n43 --> n44
    n44 --> n45
    n45 --> n46
    n46 --> n47
    n47 --> n48
    n48 --> n49
    n49 --> n50
    n49 --> n57
    n49 --> n58
    n50 --> n51
    n51 --> n52
    n52 --> n53
    n53 --> n54
    n54 --> n55
    n55 --> n56
    n58 --> n59
    n59 --> n60
    n60 --> n61
    n61 --> n62
    n62 --> n63
    n66 --> n67
    n67 --> n68
    n67 --> n71
    n67 --> n77
    n68 --> n69
    n69 --> n70
    n71 --> n72
    n72 --> n73
    n72 --> n75
    n72 --> n76
    n73 --> n74
    n77 --> n78
    n77 --> n79
    n77 --> n561
    n79 --> n80
    n79 --> n538
    n80 --> n81
    n80 --> n429
    n81 --> n82
    n81 --> n367
    n82 --> n83
    n82 --> n196
    n83 --> n84
    n83 --> n127
    n84 --> n85
    n84 --> n97
    n85 --> n86
    n86 --> n87
    n87 --> n88
    n87 --> n91
    n87 --> n96
    n88 --> n89
    n89 --> n90
    n91 --> n92
    n92 --> n93
    n93 --> n94
    n94 --> n95
    n97 --> n98
    n98 --> n99
    n98 --> n102
    n98 --> n126
    n99 --> n100
    n100 --> n101
    n102 --> n103
    n103 --> n104
    n103 --> n107
    n103 --> n108
    n104 --> n105
    n105 --> n106
    n108 --> n109
    n109 --> n110
    n110 --> n111
    n111 --> n112
    n112 --> n113
    n113 --> n114
    n114 --> n115
    n115 --> n116
    n116 --> n117
    n117 --> n118
    n118 --> n119
    n119 --> n120
    n120 --> n121
    n121 --> n122
    n122 --> n123
    n123 --> n124
    n124 --> n125
    n127 --> n128
    n128 --> n129
    n129 --> n130
    n129 --> n195
    n130 --> n131
    n131 --> n132
    n131 --> n136
    n131 --> n138
    n132 --> n133
    n133 --> n134
    n134 --> n135
    n136 --> n137
    n138 --> n139
    n139 --> n140
    n140 --> n141
    n141 --> n142
    n142 --> n143
    n143 --> n144
    n144 --> n145
    n145 --> n146
    n146 --> n147
    n147 --> n148
    n148 --> n149
    n149 --> n150
    n150 --> n151
    n151 --> n152
    n152 --> n153
    n152 --> n156
    n152 --> n157
    n152 --> n194
    n153 --> n154
    n154 --> n155
    n157 --> n158
    n157 --> n176
    n157 --> n177
    n158 --> n159
    n159 --> n160
    n160 --> n161
    n161 --> n162
    n162 --> n163
    n163 --> n164
    n164 --> n165
    n165 --> n166
    n166 --> n167
    n167 --> n168
    n168 --> n169
    n169 --> n170
    n170 --> n171
    n171 --> n172
    n172 --> n173
    n173 --> n174
    n174 --> n175
    n177 --> n178
    n178 --> n179
    n179 --> n180
    n180 --> n181
    n181 --> n182
    n182 --> n183
    n183 --> n184
    n184 --> n185
    n185 --> n186
    n186 --> n187
    n187 --> n188
    n188 --> n189
    n189 --> n190
    n190 --> n191
    n191 --> n192
    n192 --> n193
    n196 --> n197
    n197 --> n198
    n198 --> n199
    n198 --> n200
    n198 --> n201
    n198 --> n229
    n198 --> n230
    n198 --> n298
    n198 --> n299
    n201 --> n202
    n202 --> n203
    n203 --> n204
    n204 --> n205
    n205 --> n206
    n206 --> n207
    n207 --> n208
    n208 --> n209
    n209 --> n210
    n210 --> n211
    n210 --> n220
    n210 --> n221
    n211 --> n212
    n212 --> n213
    n213 --> n214
    n214 --> n215
    n215 --> n216
    n216 --> n217
    n217 --> n218
    n218 --> n219
    n221 --> n222
    n222 --> n223
    n223 --> n224
    n224 --> n225
    n225 --> n226
    n226 --> n227
    n227 --> n228
    n230 --> n231
    n231 --> n232
    n231 --> n297
    n232 --> n233
    n233 --> n234
    n233 --> n238
    n233 --> n240
    n234 --> n235
    n235 --> n236
    n236 --> n237
    n238 --> n239
    n240 --> n241
    n241 --> n242
    n242 --> n243
    n243 --> n244
    n244 --> n245
    n245 --> n246
    n246 --> n247
    n247 --> n248
    n248 --> n249
    n249 --> n250
    n250 --> n251
    n251 --> n252
    n252 --> n253
    n253 --> n254
    n254 --> n255
    n254 --> n258
    n254 --> n259
    n254 --> n296
    n255 --> n256
    n256 --> n257
    n259 --> n260
    n259 --> n278
    n259 --> n279
    n260 --> n261
    n261 --> n262
    n262 --> n263
    n263 --> n264
    n264 --> n265
    n265 --> n266
    n266 --> n267
    n267 --> n268
    n268 --> n269
    n269 --> n270
    n270 --> n271
    n271 --> n272
    n272 --> n273
    n273 --> n274
    n274 --> n275
    n275 --> n276
    n276 --> n277
    n279 --> n280
    n280 --> n281
    n281 --> n282
    n282 --> n283
    n283 --> n284
    n284 --> n285
    n285 --> n286
    n286 --> n287
    n287 --> n288
    n288 --> n289
    n289 --> n290
    n290 --> n291
    n291 --> n292
    n292 --> n293
    n293 --> n294
    n294 --> n295
    n299 --> n300
    n300 --> n301
    n300 --> n366
    n301 --> n302
    n302 --> n303
    n302 --> n307
    n302 --> n309
    n303 --> n304
    n304 --> n305
    n305 --> n306
    n307 --> n308
    n309 --> n310
    n310 --> n311
    n311 --> n312
    n312 --> n313
    n313 --> n314
    n314 --> n315
    n315 --> n316
    n316 --> n317
    n317 --> n318
    n318 --> n319
    n319 --> n320
    n320 --> n321
    n321 --> n322
    n322 --> n323
    n323 --> n324
    n323 --> n327
    n323 --> n328
    n323 --> n365
    n324 --> n325
    n325 --> n326
    n328 --> n329
    n328 --> n347
    n328 --> n348
    n329 --> n330
    n330 --> n331
    n331 --> n332
    n332 --> n333
    n333 --> n334
    n334 --> n335
    n335 --> n336
    n336 --> n337
    n337 --> n338
    n338 --> n339
    n339 --> n340
    n340 --> n341
    n341 --> n342
    n342 --> n343
    n343 --> n344
    n344 --> n345
    n345 --> n346
    n348 --> n349
    n349 --> n350
    n350 --> n351
    n351 --> n352
    n352 --> n353
    n353 --> n354
    n354 --> n355
    n355 --> n356
    n356 --> n357
    n357 --> n358
    n358 --> n359
    n359 --> n360
    n360 --> n361
    n361 --> n362
    n362 --> n363
    n363 --> n364
    n367 --> n368
    n368 --> n369
    n369 --> n370
    n369 --> n428
    n370 --> n371
    n371 --> n372
    n372 --> n373
    n373 --> n374
    n374 --> n375
    n375 --> n376
    n376 --> n377
    n377 --> n378
    n378 --> n379
    n379 --> n380
    n380 --> n381
    n381 --> n382
    n382 --> n383
    n383 --> n384
    n384 --> n385
    n385 --> n386
    n385 --> n389
    n385 --> n390
    n385 --> n427
    n386 --> n387
    n387 --> n388
    n390 --> n391
    n390 --> n409
    n390 --> n410
    n391 --> n392
    n392 --> n393
    n393 --> n394
    n394 --> n395
    n395 --> n396
    n396 --> n397
    n397 --> n398
    n398 --> n399
    n399 --> n400
    n400 --> n401
    n401 --> n402
    n402 --> n403
    n403 --> n404
    n404 --> n405
    n405 --> n406
    n406 --> n407
    n407 --> n408
    n410 --> n411
    n411 --> n412
    n412 --> n413
    n413 --> n414
    n414 --> n415
    n415 --> n416
    n416 --> n417
    n417 --> n418
    n418 --> n419
    n419 --> n420
    n420 --> n421
    n421 --> n422
    n422 --> n423
    n423 --> n424
    n424 --> n425
    n425 --> n426
    n429 --> n430
    n430 --> n431
    n431 --> n432
    n431 --> n433
    n431 --> n434
    n431 --> n463
    n431 --> n464
    n434 --> n435
    n435 --> n436
    n436 --> n437
    n437 --> n438
    n438 --> n439
    n439 --> n440
    n440 --> n441
    n441 --> n442
    n442 --> n443
    n442 --> n453
    n442 --> n454
    n443 --> n444
    n444 --> n445
    n445 --> n446
    n446 --> n447
    n447 --> n448
    n448 --> n449
    n449 --> n450
    n450 --> n451
    n451 --> n452
    n454 --> n455
    n455 --> n456
    n456 --> n457
    n457 --> n458
    n458 --> n459
    n459 --> n460
    n460 --> n461
    n461 --> n462
    n464 --> n465
    n465 --> n466
    n465 --> n467
    n465 --> n537
    n467 --> n468
    n468 --> n469
    n469 --> n470
    n470 --> n471
    n470 --> n536
    n471 --> n472
    n472 --> n473
    n472 --> n477
    n472 --> n479
    n473 --> n474
    n474 --> n475
    n475 --> n476
    n477 --> n478
    n479 --> n480
    n480 --> n481
    n481 --> n482
    n482 --> n483
    n483 --> n484
    n484 --> n485
    n485 --> n486
    n486 --> n487
    n487 --> n488
    n488 --> n489
    n489 --> n490
    n490 --> n491
    n491 --> n492
    n492 --> n493
    n493 --> n494
    n493 --> n497
    n493 --> n498
    n493 --> n535
    n494 --> n495
    n495 --> n496
    n498 --> n499
    n498 --> n517
    n498 --> n518
    n499 --> n500
    n500 --> n501
    n501 --> n502
    n502 --> n503
    n503 --> n504
    n504 --> n505
    n505 --> n506
    n506 --> n507
    n507 --> n508
    n508 --> n509
    n509 --> n510
    n510 --> n511
    n511 --> n512
    n512 --> n513
    n513 --> n514
    n514 --> n515
    n515 --> n516
    n518 --> n519
    n519 --> n520
    n520 --> n521
    n521 --> n522
    n522 --> n523
    n523 --> n524
    n524 --> n525
    n525 --> n526
    n526 --> n527
    n527 --> n528
    n528 --> n529
    n529 --> n530
    n530 --> n531
    n531 --> n532
    n532 --> n533
    n533 --> n534
    n538 --> n539
    n539 --> n540
    n540 --> n541
    n540 --> n542
    n540 --> n560
    n542 --> n543
    n543 --> n544
    n544 --> n545
    n545 --> n546
    n546 --> n547
    n547 --> n548
    n548 --> n549
    n549 --> n550
    n550 --> n551
    n551 --> n552
    n552 --> n553
    n553 --> n554
    n554 --> n555
    n555 --> n556
    n556 --> n557
    n557 --> n558
    n558 --> n559
    classDef semanticAction fill:#fff3cd,stroke:#f39c12,stroke-width:2px
    classDef terminal fill:#e8f4fd,stroke:#2c7fb8
    classDef nonTerminal fill:#eef7ee,stroke:#2e7d32
```

## 4. 语义动作节点列表

当前语法树中没有语义动作节点。
