package adsen.parser.node.statement;

import adsen.tokeniser.Token;

public class ElseStatement  implements NodeStatement {
    public final Token token;
    NodeStatement thenStatement;

    public ElseStatement(Token token, NodeStatement statement) {
        this.token = token;
        this.thenStatement = statement;
    }

    public NodeStatement thenStatement() {
        return this.thenStatement;
    }

    @Override
    public String asString() {
        return "else: " + thenStatement.asString();
    }

    @Override
    public String typeString() {
        return "else";
    }
}
