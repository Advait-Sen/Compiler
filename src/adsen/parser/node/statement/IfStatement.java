package adsen.parser.node.statement;

import adsen.parser.node.NodeExpr;
import adsen.tokeniser.Token;

public class IfStatement implements NodeStatement {
    public final Token token;
    NodeExpr condition;
    NodeStatement thenStatement;

    public IfStatement(Token token, NodeExpr condition, NodeStatement statement) {
        this.token = token;
        this.condition = condition;
        this.thenStatement = statement;
    }

    public NodeExpr getCondition() {
        return this.condition;
    }

    public NodeStatement thenStatement() {
        return this.thenStatement;
    }

    @Override
    public String asString() {
        return "if (" + condition.asString() + "): " + thenStatement.asString();
    }

    @Override
    public String typeString() {
        return "if";
    }
}
