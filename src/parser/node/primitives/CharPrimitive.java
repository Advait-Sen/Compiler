package parser.node.primitives;

import tokeniser.Token;

public class CharPrimitive extends NodePrimitive {
    private char value;

    public CharPrimitive(Token token) {
        super(token);
        this.value = token.value.charAt(0);
    }

    public void setValue(char newValue) {
        value = newValue;
    }

    public char getValue() {
        return value;
    }
}
