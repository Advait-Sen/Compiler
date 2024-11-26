package parser.node.primitives;

import tokeniser.Token;

public class FloatPrimitive extends NodePrimitive {
    private double value;

    public FloatPrimitive(Token token) {
        super(token);
        this.value = Double.parseDouble(token.value);
    }

    public void setValue(double newValue) {
        value = newValue;
    }

    public double getValue() {
        return value;
    }
}

