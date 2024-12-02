package adsen.parser.node.primitives;

import adsen.tokeniser.Token;
import adsen.tokeniser.TokenType;

public final class CharPrimitive extends NodePrimitive {
    public static final String TYPE_STRING = "char";

    private char value;

    public CharPrimitive(char value) {
        super(new Token(String.valueOf(value), TokenType.CHAR_LITERAL));
        this.value = value;
    }

    public CharPrimitive(Token token) {
        super(token);
        this.value = token.value.charAt(0);
    }

    @Override
    public String getTypeString() {
        return TYPE_STRING;
    }

    public static CharPrimitive of(char value) {
        return new CharPrimitive(value);
    }

    public void setValue(char newValue) {
        value = newValue;
    }

    public char getValue() {
        return value;
    }
}
