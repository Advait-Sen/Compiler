package adsen.parser.node.expr.primitives;

import adsen.tokeniser.Token;
import adsen.tokeniser.TokenType;

public final class BoolPrimitive extends NodePrimitive {
    public static final String TYPE_STRING = "bool";

    private boolean value;

    public BoolPrimitive(boolean value) {
        super(new Token(String.valueOf(value), TokenType.BOOL_LITERAL));
        this.value = value;
    }

    public BoolPrimitive(Token token) {
        super(token);
        this.value = Boolean.parseBoolean(token.value);
    }

    public static BoolPrimitive of(boolean value) {
        return new BoolPrimitive(value);
    }

    @Override
    public String getTypeString() {
        return TYPE_STRING;
    }

    @Override
    public NodePrimitive negate() {
        return BoolPrimitive.of(!value);
    }

    public BoolPrimitive setValue(boolean newValue) {
        value = newValue;
        return this;
    }

    public boolean getValue() {
        return value;
    }

    public String asString(){
        token.value = String.valueOf(value);
        return super.asString();
    }
}
