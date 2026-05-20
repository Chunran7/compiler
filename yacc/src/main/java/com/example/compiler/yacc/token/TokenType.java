package com.example.compiler.yacc.token;

/**
 * C99 Token 类型全集，与 c99.l / c99.y 对齐。
 *
 * <p>多字符 token（关键词、运算符、字面量）以 C99 标准命名为准，
 * 同时保留 MiniC 兼容别名，通过 {@link #canonical()} 映射到规范名。</p>
 *
 * <p>单字符 token 使用语义化命名，通过 {@link #forChar(char)} 映射。</p>
 */
public enum TokenType {

    // ── 关键词 (Keywords) ── 37 个, 对齐 c99.l:22-58 ──
    AUTO,
    BOOL,
    BREAK,
    CASE,
    CHAR,
    COMPLEX,
    CONST,
    CONTINUE,
    DEFAULT,
    DO,
    DOUBLE,
    ELSE,
    ENUM,
    EXTERN,
    FLOAT,
    FOR,
    GOTO,
    IF,
    IMAGINARY,
    INLINE,
    INT,
    LONG,
    REGISTER,
    RESTRICT,
    RETURN,
    SHORT,
    SIGNED,
    SIZEOF,
    STATIC,
    STRUCT,
    SWITCH,
    TYPEDEF,
    UNION,
    UNSIGNED,
    VOID,
    VOLATILE,
    WHILE,

    // ── 字面量 / 标识符 (Literals & Identifiers) ── 5 个, 对齐 c99.y:1-5 ──
    IDENTIFIER,
    CONSTANT,
    STRING_LITERAL,
    ELLIPSIS,
    TYPE_NAME,

    // ── MiniC 兼容别名 (Compatibility aliases) ──
    /** MiniC alias for {@link #IDENTIFIER} */
    ID,
    /** MiniC alias for {@link #CONSTANT} */
    NUM,
    /** MiniC pseudo-keyword: {@code main} is an identifier in C99 */
    MAIN,
    /** MiniC alias for {@link #LE_OP} */
    LE,
    /** MiniC alias for {@link #GE_OP} */
    GE,
    /** MiniC alias for {@link #EQ_OP} */
    EQ,
    /** MiniC alias for {@link #NE_OP} */
    NE,

    // ── 运算符 (Operators) ── 21 个, 对齐 c99.y:2-5 ──
    RIGHT_ASSIGN,
    LEFT_ASSIGN,
    ADD_ASSIGN,
    SUB_ASSIGN,
    MUL_ASSIGN,
    DIV_ASSIGN,
    MOD_ASSIGN,
    AND_ASSIGN,
    XOR_ASSIGN,
    OR_ASSIGN,
    RIGHT_OP,
    LEFT_OP,
    INC_OP,
    DEC_OP,
    PTR_OP,
    AND_OP,
    OR_OP,
    LE_OP,
    GE_OP,
    EQ_OP,
    NE_OP,

    // ── 单字符 token ── 25 个, 对应 c99.l 中 return('c') 的字符 ──
    SEMI,       // ;
    LBRACE,     // {  (also <%)
    RBRACE,     // }  (also %>)
    COMMA,      // ,
    COLON,      // :
    ASSIGN,     // =
    LPAREN,     // (
    RPAREN,     // )
    LBRACKET,   // [  (also <:)
    RBRACKET,   // ]  (also :>)
    DOT,        // .
    AMPERSAND,  // &
    BANG,       // !
    TILDE,      // ~
    MINUS,      // -
    PLUS,       // +
    STAR,       // *
    SLASH,      // /
    PERCENT,    // %
    LT,         // <
    GT,         // >
    CARET,      // ^
    PIPE,       // |
    QUESTION,   // ?

    // ── 特殊 ──
    EOF;

    /**
     * 返回此 token 的规范 C99 形式。MiniC 别名会被映射到对应的 C99 标准名。
     */
    public TokenType canonical() {
        return switch (this) {
            case ID -> IDENTIFIER;
            case NUM -> CONSTANT;
            case LE -> LE_OP;
            case GE -> GE_OP;
            case EQ -> EQ_OP;
            case NE -> NE_OP;
            case MAIN -> IDENTIFIER; // main 在 C99 中就是普通标识符
            default -> this;
        };
    }

    /**
     * 此 token 是否为兼容别名（非 C99 标准名）。
     */
    public boolean isAlias() {
        return this != canonical();
    }

    /**
     * 将 c99.l 中单字符 return('c') 的字符映射到对应 TokenType。
     * 仅覆盖 24 个有语义命名的单字符 token，EOF 不在此列。
     */
    public static TokenType forChar(char c) {
        return switch (c) {
            case ';' -> SEMI;
            case '{' -> LBRACE;
            case '}' -> RBRACE;
            case ',' -> COMMA;
            case ':' -> COLON;
            case '=' -> ASSIGN;
            case '(' -> LPAREN;
            case ')' -> RPAREN;
            case '[' -> LBRACKET;
            case ']' -> RBRACKET;
            case '.' -> DOT;
            case '&' -> AMPERSAND;
            case '!' -> BANG;
            case '~' -> TILDE;
            case '-' -> MINUS;
            case '+' -> PLUS;
            case '*' -> STAR;
            case '/' -> SLASH;
            case '%' -> PERCENT;
            case '<' -> LT;
            case '>' -> GT;
            case '^' -> CARET;
            case '|' -> PIPE;
            case '?' -> QUESTION;
            default ->
                throw new IllegalArgumentException("No TokenType for character: '" + c + "'");
        };
    }
}
