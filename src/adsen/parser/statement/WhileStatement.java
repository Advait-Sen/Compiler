package adsen.parser.statement;

import adsen.parser.expr.NodeExpr;
import adsen.tokeniser.Token;

public class WhileStatement implements HeliumStatement {
    public final Token token;
    NodeExpr loopCondition;
    HeliumStatement executionStatement;

    public WhileStatement(Token token, NodeExpr condition, HeliumStatement statement) {
        this.token = token;
        this.loopCondition = condition;
        this.executionStatement = statement;
    }

    public NodeExpr condition() {
        return loopCondition;
    }

    public HeliumStatement statement() {
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

    @Override
    public Token primaryToken() {
        return token;
    }
}
