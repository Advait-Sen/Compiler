package adsen.helium.parser.statement;

import adsen.helium.error.ExpressionError;
import adsen.helium.parser.expr.NodeExpr;
import adsen.helium.tokeniser.Token;

public class ForStatement implements HeliumStatement {
    public final Token token;

    HeliumStatement assigner;
    NodeExpr loopCondition;
    HeliumStatement incrementer;
    HeliumStatement executionStatement;
    /**
     * Ever since new parsing method, this is needed to keep track of the status of the for loop, since the assigner,
     * incrementer and statement are added separately
     */
    ForCreationStatus status = ForCreationStatus.ASSIGNER;

    public ForStatement(Token token, HeliumStatement assignment, NodeExpr condition) {
        this.token = token;
        this.assigner = assignment;
        this.loopCondition = condition;
    }

    public NodeExpr condition() {
        return loopCondition;
    }

    public HeliumStatement getAssigner() {
        return assigner;
    }

    public HeliumStatement getIncrementer() {
        return incrementer;
    }

    public HeliumStatement statement() {
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

    public void addIncrementer(HeliumStatement increment) {
        if (status == ForCreationStatus.ASSIGNER) {
            incrementer = increment;
            status = ForCreationStatus.INCREMENTER;
        } else {
            throw new ExpressionError("Incorrect for loop formation, found incrementer in the wrong place", token);
        }
    }

    public void addStatement(HeliumStatement statement) {
        if (status == ForCreationStatus.INCREMENTER) {
            executionStatement = statement;
            status = ForCreationStatus.COMPLETED;
        } else {
            throw new ExpressionError("Incorrect for loop formation, found statement in the wrong place", token);
        }
    }

}

enum ForCreationStatus {
    ASSIGNER,
    INCREMENTER,
    COMPLETED
}