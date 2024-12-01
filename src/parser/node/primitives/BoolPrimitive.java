package parser.node.primitives;

import tokeniser.Token;

public class BoolPrimitive extends NodePrimitive {
    public static final String TYPE_STRING = "bool";

    private boolean value;

    public BoolPrimitive(Token token) {
        super(token);
        this.value = Boolean.parseBoolean(token.value);
    }

    @Override
    public String getTypeString() {
        return TYPE_STRING;
    }

    public void setValue(boolean newValue) {
        value = newValue;
    }

    public boolean getValue() {
        return value;
    }
}
