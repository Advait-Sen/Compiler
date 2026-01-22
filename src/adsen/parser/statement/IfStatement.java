package adsen.parser.statement;

import adsen.parser.expr.NodeExpr;
import adsen.tokeniser.Token;

public class IfStatement implements HeliumStatement {
    public final Token token;
    NodeExpr condition;
    HeliumStatement thenStatement;

    public final Token elseToken;
    HeliumStatement elseStatement;


    public IfStatement(Token token, NodeExpr condition, HeliumStatement statement) {
        this.token = token;
        this.condition = condition;
        this.thenStatement = statement;
        this.elseToken = null;
        this.elseStatement = null;
    }

    public IfStatement(Token token, NodeExpr condition, HeliumStatement statement, Token elseToken, HeliumStatement elseStatement) {
        this.token = token;
        this.condition = condition;
        this.thenStatement = statement;
        this.elseToken = elseToken;
        this.elseStatement = elseStatement;
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
        return elseToken != null;
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
}
