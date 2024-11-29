package parser.node.identifier;

import error.ExpressionError;
import parser.node.NodeExpr;
import tokeniser.Token;
import tokeniser.TokenType;

public class NodeIdentifier implements NodeExpr {
    public final Token token;
    String name;

    public NodeIdentifier(Token token) {
        if (token.type != TokenType.IDENTIFIER) {
            throw new ExpressionError("How did we get here? Tried to create an identifier node with non-identifier token", token);
        }
        this.token = token;
        this.name = token.value;
    }

    @Override
    public String asString() {
        return name;
    }

    @Override
    public boolean isRoot() {
        return false;
    }
}
