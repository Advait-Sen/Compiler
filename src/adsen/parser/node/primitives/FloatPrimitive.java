package adsen.parser.node.primitives;

import adsen.tokeniser.Token;
import adsen.tokeniser.TokenType;

public final class FloatPrimitive extends NodePrimitive {
    public static final String TYPE_STRING = "float";

    private double value;

    public FloatPrimitive(double value) {
        super(new Token(String.valueOf(value), TokenType.FLOAT_LITERAL));
        this.value = value;
    }

    public FloatPrimitive(Token token) {
        super(token);
        this.value = Double.parseDouble(token.value);
    }

    @Override
    public String getTypeString() {
        return TYPE_STRING;
    }

    public static FloatPrimitive of(double value) {
        return new FloatPrimitive(value);
    }

    public void setValue(double newValue) {
        value = newValue;
    }

    public double getValue() {
        return value;
    }
}

