package adsen.tokeniser;

@SuppressWarnings("unused") //Gonna be a while till I use all of these
public enum TokenType {
    //Literals
    INT_LITERAL, FLOAT_LITERAL, HEX_LITERAL, CHAR_LITERAL, STR_LITERAL, BOOL_LITERAL,

    //Punctuation
    OPEN_PAREN, CLOSE_PAREN, SQ_OPEN_PAREN, SQ_CLOSE_PAREN,
    C_OPEN_PAREN, C_CLOSE_PAREN, SEMICOLON, COMMA, POINT,

    //Identifier
    IDENTIFIER,

    //Post-process stuff
    VARIABLE, FUNCTION,

    //Types
    /**
     * For function declarations
     */
    VOID,
    /**
     * This will be 'float', 'int', 'char' and 'boolean'
     */
    PRIMITIVE_TYPE,
    /**
     * I might add 'list', 'map', etc. as built-in complex types, or maybe as native classes
     */
    COMPOUND_TYPE,
    /**
     * For when classes eventually get implemented (a loong way off)
     */
    CLASS_TYPE,

    //Specific operator types
    DECLARATION_OPERATION, BINARY_OPERATOR, UNARY_OPERATOR,

    //Keywords
    LET, EXIT, IF, ELSE, WHILE, FOR, RETURN, CONTINUE, BREAK

}
