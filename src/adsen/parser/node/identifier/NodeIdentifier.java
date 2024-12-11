package adsen.parser.node.identifier;

import adsen.error.ExpressionError;
import adsen.parser.node.NodeExpr;
import adsen.tokeniser.Token;
import adsen.tokeniser.TokenType;

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
