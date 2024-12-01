package adsen.parser.node.primitives;

import adsen.tokeniser.Token;

public final class BoolPrimitive extends NodePrimitive {
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
