package adsen.parser.node.statement;

import adsen.parser.node.expr.NodeExpr;
import adsen.tokeniser.Token;

public class ForStatement implements NodeStatement {
    public final Token token;

    NodeStatement assigner;
    NodeExpr loopCondition;
    NodeStatement incrementer;
    NodeStatement executionStatement;

    public ForStatement(Token token, NodeStatement assignment, NodeStatement increment, NodeExpr condition, NodeStatement statement) {
        this.token = token;
        this.assigner = assignment;
        this.incrementer = increment;
        this.loopCondition = condition;
        this.executionStatement = statement;
    }

    public NodeExpr condition() {
        return loopCondition;
    }

    public NodeStatement getAssigner() {
        return assigner;
    }

    public NodeStatement getIncrementer() {
        return incrementer;
    }

    public NodeStatement statement() {
        return executionStatement;
    }

    @Override
    public String asString() {
        return "for (" + assigner.asString() + "; " + loopCondition.asString() + "; " + incrementer.asString() + "):\n" + executionStatement.asString();
    }

    @Override
    public String typeString() {
        return "for";
    }
}
