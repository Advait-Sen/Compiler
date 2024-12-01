package parser.node.primitives;

import tokeniser.Token;

public class FloatPrimitive extends NodePrimitive {
    public static final String TYPE_STRING = "float";

    private double value;

    public FloatPrimitive(Token token) {
        super(token);
        this.value = Double.parseDouble(token.value);
    }

    @Override
    public String getTypeString() {
        return TYPE_STRING;
    }

    public void setValue(double newValue) {
        value = newValue;
    }

    public double getValue() {
        return value;
    }
}

