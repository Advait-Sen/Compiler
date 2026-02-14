package adsen.helium.parser.statement.aggregate;

import adsen.helium.error.ExpressionError;
import adsen.helium.parser.expr.NodeExpr;
import adsen.helium.parser.statement.HeliumStatement;
import adsen.helium.tokeniser.Token;

public class IfStatement extends HeliumStatement.AggregateStatement {
    public final Token token;
    NodeExpr condition;
    HeliumStatement thenStatement;

    Token elseToken = null;
    HeliumStatement elseStatement = null;

    IfCreationStatus status = IfCreationStatus.IF;


    public IfStatement(Token token, NodeExpr condition, HeliumStatement statement) {
        this.token = token;
        this.condition = condition;
        this.thenStatement = statement;
    }

    public NodeExpr getCondition() {
        return this.condition;
    }

    public HeliumStatement thenStatement() {
        return this.thenStatement;
    }

    public HeliumStatement elseStatement() {
        return this.elseStatement;
    }

    public boolean hasElse() {
        return status == IfCreationStatus.ELSE;
    }

    @Override
    public String asString() {
        return "if (" + condition.asString() + "): " + thenStatement.asString() + (hasElse() ? "\nelse: " + elseStatement.asString() : "");
    }

    @Override
    public String typeString() {
        return "if";
    }

    @Override
    public Token primaryToken() {
        return token;
    }

    public void addElse(Token elseToken, HeliumStatement elseStatement) {
        if (status == IfCreationStatus.IF) {
            this.elseToken = elseToken;
            this.elseStatement = elseStatement;
            this.status = IfCreationStatus.ELSE;
        } else {
            throw new ExpressionError("Already added an else to this if statement", elseToken);
        }
    }

    @Override
    public int length() {
        if (status == IfCreationStatus.IF) {
            return 1 + thenStatement.length();
        } else {
            return 1 + thenStatement.length() + elseStatement.length();
        }
    }
}

enum IfCreationStatus {
    IF,
    ELSE
}