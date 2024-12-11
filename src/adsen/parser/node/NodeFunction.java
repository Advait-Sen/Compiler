package adsen.parser.node;

import adsen.error.ExpressionError;
import adsen.parser.node.statement.NodeStatement;
import adsen.tokeniser.Token;

import java.util.ArrayList;
import java.util.List;

public class NodeFunction {
    public final Token returnType;
    public final Token token;
    public final String name;
    /**
     * This is a list of tokens of the type:
     * [Type, name, Type, name, ...]
     * <p>
     * So the number of arguments is equal to the length of this list divided by 2
     */
    final List<Token> signature;
    public final int args;
    List<NodeStatement> body = new ArrayList<>();

    public NodeFunction(Token returnType, Token nameToken, List<Token> signature) {
        this.returnType = returnType;
        this.token = nameToken;
        this.name = nameToken.value;

        if (signature.size() % 2 != 0)
            throw new ExpressionError("Invalid function signature", nameToken);

        this.signature = signature;
        this.args = signature.size() >> 1;
    }

    public void andThen(NodeStatement statement) {
        body.add(statement);
    }

    public List<NodeStatement> getBody() {
        return body;
    }

    public String asString() {
        return returnType.value + " " + name + " (";
    }
}
