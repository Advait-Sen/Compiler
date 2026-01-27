package adsen.helium.parser.expr;

import adsen.helium.error.ExpressionError;
import adsen.helium.tokeniser.Token;
import adsen.helium.tokeniser.TokenType;

public class NodeIdentifier implements NodeExpr {
    public final Token token;
    String name;

    public NodeIdentifier(Token token) {
        if (token.type != TokenType.IDENTIFIER && token.type!=TokenType.VARIABLE) {
            throw new ExpressionError("How did we get here? Tried to create an identifier node with non-identifier token", token);
        }
        this.token = token;
        this.name = token.value;
    }

    @Override
    public String asString() {
        return name;
    }
}
