package tokeniser;

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
}
