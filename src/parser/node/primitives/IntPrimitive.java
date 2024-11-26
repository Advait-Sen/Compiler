package parser.node.primitives;

import tokeniser.Token;
import tokeniser.TokenType;

public class IntPrimitive extends NodePrimitive {
    private long value;

    public IntPrimitive(Token token) {
        super(token);
        if(token.type== TokenType.INT_LITERAL)
            this.value = Long.parseLong(token.value);
        else //Else it's hexadecimal
            this.value = Long.parseLong(token.value.substring(2), 16);
    }

    public void setValue(long newValue) {
        value = newValue;
    }

    public long getValue() {
        return value;
    }
}
