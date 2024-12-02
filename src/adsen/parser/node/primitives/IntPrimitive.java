package adsen.parser.node.primitives;

import adsen.tokeniser.Token;
import adsen.tokeniser.TokenType;

public final class IntPrimitive extends NodePrimitive {
    public static final String TYPE_STRING = "int";

    private long value;

    public IntPrimitive(long value) {
        super(new Token(String.valueOf(value), TokenType.INT_LITERAL));
        this.value = value;
    }

    public IntPrimitive(Token token) {
        super(token);
        if (token.type == TokenType.INT_LITERAL)
            this.value = Long.parseLong(token.value);
        else {//Else it's hexadecimal
            this.value = Long.parseLong(token.value.substring(2), 16);
        }
    }

    @Override
    public String getTypeString() {
        return TYPE_STRING;
    }

    public static IntPrimitive of(long value) {
        return new IntPrimitive(value);
    }

    public void setValue(long newValue) {
        value = newValue;
    }

    public long getValue() {
        return value;
    }
}
