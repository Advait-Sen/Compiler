package parser.node.primitives;

import tokeniser.Token;

public final class CharPrimitive extends NodePrimitive {
    public static final String TYPE_STRING = "char";

    private char value;

    public CharPrimitive(Token token) {
        super(token);
        this.value = token.value.charAt(0);
    }

    @Override
    public String getTypeString() {
        return TYPE_STRING;
    }

    public void setValue(char newValue) {
        value = newValue;
    }

    public char getValue() {
        return value;
    }
}
