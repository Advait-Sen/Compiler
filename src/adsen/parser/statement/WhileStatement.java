package adsen.parser.statement;

import adsen.parser.node.expr.NodeExpr;
import adsen.tokeniser.Token;

public class WhileStatement implements Statement {
    public final Token token;
    NodeExpr loopCondition;
    Statement executionStatement;

    public WhileStatement(Token token, NodeExpr condition, Statement statement) {
        this.token = token;
        this.loopCondition = condition;
        this.executionStatement = statement;
    }

    public NodeExpr condition() {
        return loopCondition;
    }

    public Statement statement() {
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
