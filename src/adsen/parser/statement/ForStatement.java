package adsen.parser.statement;

import adsen.parser.node.expr.NodeExpr;
import adsen.tokeniser.Token;

public class ForStatement implements Statement {
    public final Token token;

    Statement assigner;
    NodeExpr loopCondition;
    Statement incrementer;
    Statement executionStatement;

    public ForStatement(Token token, Statement assignment, Statement increment, NodeExpr condition, Statement statement) {
        this.token = token;
        this.assigner = assignment;
        this.incrementer = increment;
        this.loopCondition = condition;
        this.executionStatement = statement;
    }

    public NodeExpr condition() {
        return loopCondition;
    }

    public Statement getAssigner() {
        return assigner;
    }

    public Statement getIncrementer() {
        return incrementer;
    }

    public Statement statement() {
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
