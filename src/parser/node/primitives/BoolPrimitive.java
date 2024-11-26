package parser.node.primitives;

import tokeniser.Token;

public class BoolPrimitive extends NodePrimitive {
    private boolean value;

    public BoolPrimitive(Token token) {
        super(token);
        this.value = Boolean.parseBoolean(token.value);
    }

    public void setValue(boolean newValue) {
        value = newValue;
    }

    public boolean getValue() {
        return value;
    }
}
