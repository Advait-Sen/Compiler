package adsen.parser.node.statement;

import adsen.parser.node.expr.NodeExpr;
import adsen.tokeniser.Token;

public class WhileStatement extends NodeStatement {
    public final Token token;
    NodeExpr loopCondition;
    NodeStatement executionStatement;

    public WhileStatement(Token token, NodeExpr condition, NodeStatement statement) {
        this.token = token;
        this.loopCondition = condition;
        this.executionStatement = statement;
    }

    public NodeExpr condition() {
        return loopCondition;
    }

    public NodeStatement statement() {
        return executionStatement;
    }

    @Override
    public String asString() {
        return "while (" + loopCondition.asString() + "): " + executionStatement.asString();
    }

    @Override
    public String typeString() {
        return "while";
    }
}
