package adsen.parser.node.statement;

import adsen.parser.node.expr.NodeExpr;
import adsen.tokeniser.Token;

public class IfStatement extends NodeStatement {
    public final Token token;
    NodeExpr condition;
    NodeStatement thenStatement;

    public final Token elseToken;
    NodeStatement elseStatement;


    public IfStatement(Token token, NodeExpr condition, NodeStatement statement) {
        this.token = token;
        this.condition = condition;
        this.thenStatement = statement;
        this.elseToken = null;
        this.elseStatement = null;
    }

    public IfStatement(Token token, NodeExpr condition, NodeStatement statement, Token elseToken, NodeStatement elseStatement) {
        this.token = token;
        this.condition = condition;
        this.thenStatement = statement;
        this.elseToken = elseToken;
        this.elseStatement = elseStatement;
    }

    public NodeExpr getCondition() {
        return this.condition;
    }

    public NodeStatement thenStatement() {
        return this.thenStatement;
    }

    public NodeStatement elseStatement() {
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
}
