package adsen.helium.parser.statement.aggregate;

import adsen.helium.parser.expr.NodeExpr;
import adsen.helium.parser.statement.AggregateStatement;
import adsen.helium.parser.statement.HeliumStatement;
import adsen.helium.tokeniser.Token;

public class WhileStatement extends AggregateStatement {
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

    @Override
    public int length() {
        return 1 + executionStatement.length();
    }
}
