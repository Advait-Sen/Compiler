package adsen.tokeniser;

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

    public String toString() {
        return type + ": " + value;
    }
}
