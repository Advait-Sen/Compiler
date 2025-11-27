package adsen.parser.node.expr.primitives;

import adsen.error.ExpressionError;
import adsen.tokeniser.Token;
import adsen.tokeniser.TokenType;

public final class CharPrimitive extends NodePrimitive {
    public static final String TYPE_STRING = "char";

    private char value;

    public CharPrimitive(char value) {
        super(new Token(String.valueOf(value), TokenType.CHAR_LITERAL));
        this.value = value;
    }

    public CharPrimitive(Token token) {
        super(token);
        this.value = token.value.charAt(0);
    }

    @Override
    public String getTypeString() {
        return TYPE_STRING;
    }

    public static CharPrimitive of(char value) {
        return new CharPrimitive(value);
    }

    public CharPrimitive copy() {
        return CharPrimitive.of(value);
    }

    public CharPrimitive setValue(char newValue) {
        value = newValue;
        return this;
    }

    public char getValue() {
        return value;
    }

    public String asString() {
        token.value = String.valueOf(value);
        return super.asString();
    }
}
