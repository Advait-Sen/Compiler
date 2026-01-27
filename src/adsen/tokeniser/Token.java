package adsen.tokeniser;

import static adsen.tokeniser.TokenType.VARIABLE;

public class Token {
    public String value = "";
    public TokenType type;

    public int linepos;
    public int colpos;
    public int pos;

    public void append(String s) {
        value += s;
    }

    public void append(char c) {
        value += c;
    }

    /**
     * Empty constructor used when tokenising
     */
    public Token() {}

    /**
     * Constructor used for interpreter
     */
    public Token(String value, TokenType type) {
        this.value = value;
        this.type = type;
        this.colpos = -1;
        this.linepos = -1;
    }


    /**
     * Can the token be processed on its own to give a value
     */
    public boolean isValueToken(){
        return switch (type){
            case VARIABLE, IDENTIFIER, BOOL_LITERAL, INT_LITERAL, FLOAT_LITERAL, CHAR_LITERAL, STR_LITERAL -> true;
            default -> false;
        };
    }

    /**
     * Can the token be part of an expression.
     * Preparing for shunting yard.
     * TODO make sure this is correct, cos I have a feeling it isn't
     */
    public boolean isValidExprToken(){
        return switch (type) {
            case LET, EXIT, IF, ELSE, SEMICOLON, C_OPEN_PAREN, C_CLOSE_PAREN, WHILE, FOR, CONTINUE, VOID, BREAK, IMPORT ->
                    false; //Simpler to go by exclusion, it seems
            default -> true;
        };
    }

    /**
     * Can the token be part of an import string. Imports could contain keywords, including import
     */
    public boolean isValidImportToken() {
        return type == VARIABLE || Keywords.tokeniserKeywords.containsKey(value);
    }

    public String toString() {
        return type + ": " + value;
    }
}
