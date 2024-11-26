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

}
