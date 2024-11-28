package tokeniser;

public enum TokenType {
    INT_LITERAL, FLOAT_LITERAL, HEX_LITERAL, CHAR_LITERAL, STR_LITERAL, BOOL_LITERAL,
    OPEN_PAREN, CLOSE_PAREN, SQ_OPEN_PAREN, SQ_CLOSE_PAREN, C_OPEN_PAREN, C_CLOSE_PAREN,
    SEMICOLON, COMMA, POINT, OPERATOR,

    //Identifiers
    IDENTIFIER,

    //Types
    /**
     * This will be 'float', 'int', 'char' and 'boolean'
     */
    PRIMITIVE_TYPE,
    /**
     * I might add 'list', 'map', etc. as built-in complex types, or maybe as native classes, idk
     */
    COMPOUND_TYPE,
    /**
     * For when classes eventually get implemented (a loong way off)
     */
    CLASS_TYPE,

    //keywords
    LET, EXIT, IF, ELSE,

}
