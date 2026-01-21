package adsen.parser.statement;

import adsen.error.ExpressionError;
import adsen.parser.expr.NodeExpr;
import adsen.tokeniser.Token;

public class ForStatement implements Statement {
    public final Token token;

    Statement assigner;
    NodeExpr loopCondition;
    Statement incrementer;
    Statement executionStatement;
    /**
     * Ever since new parsing method, this is needed to keep track of the status of the for loop, since the assigner,
     * incrementer and statement are added separately
     */
    CreationStatus status;

    public ForStatement(Token token, Statement assignment, NodeExpr condition) {
        this.token = token;
        this.assigner = assignment;
        this.loopCondition = condition;
        this.status = CreationStatus.ASSIGNER;
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
        return switch (status) {
            case COMPLETED ->
                    "for (" + assigner.asString() + "; " + loopCondition.asString() + "; " + incrementer.asString() + "):\n" + executionStatement.asString();

            case INCREMENTER ->
                    "for (" + assigner.asString() + "; " + loopCondition.asString() + "; " + incrementer.asString() + "):\n null";

            case ASSIGNER -> "for (" + assigner.asString() + "; " + loopCondition.asString() + "; null):\n null";
        };
    }

    @Override
    public String typeString() {
        return "for";
    }

    @Override
    public Token primaryToken() {
        return token;
    }

    public void addIncrementer(Statement increment) {
        if (status == CreationStatus.ASSIGNER) {
            incrementer = increment;
            status = CreationStatus.INCREMENTER;
        } else {
            throw new ExpressionError("Incorrect for loop formation, found incrementer in the wrong place", token);
        }
    }

    public void addStatement(Statement statement) {
        if (status == CreationStatus.INCREMENTER) {
            executionStatement = statement;
            status = CreationStatus.COMPLETED;
        } else {
            throw new ExpressionError("Incorrect for loop formation, found statement in the wrong place", token);
        }
    }

}

enum CreationStatus {
    ASSIGNER,
    INCREMENTER,
    COMPLETED
}