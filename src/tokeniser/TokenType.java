package tokeniser;

public enum TokenType {
    INT_LITERAL, FLOAT_LITERAL, HEX_LITERAL, CHAR_LITERAL, STR_LITERAL, BOOL_LITERAL,
    OPEN_PAREN, CLOSE_PAREN, SQ_OPEN_PAREN, SQ_CLOSE_PAREN, C_OPEN_PAREN, C_CLOSE_PAREN,
    SEMICOLON, COMMA, POINT, OPERATOR,
    IDENTIFIER,
    //keywords
    EXIT, IF, ELSE,
}
