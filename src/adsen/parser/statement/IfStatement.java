package adsen.parser.statement;

import adsen.parser.node.expr.NodeExpr;
import adsen.tokeniser.Token;

public class IfStatement implements Statement {
    public final Token token;
    NodeExpr condition;
    Statement thenStatement;

    public final Token elseToken;
    Statement elseStatement;


    public IfStatement(Token token, NodeExpr condition, Statement statement) {
        this.token = token;
        this.condition = condition;
        this.thenStatement = statement;
        this.elseToken = null;
        this.elseStatement = null;
    }

    public IfStatement(Token token, NodeExpr condition, Statement statement, Token elseToken, Statement elseStatement) {
        this.token = token;
        this.condition = condition;
        this.thenStatement = statement;
        this.elseToken = elseToken;
        this.elseStatement = elseStatement;
    }

    public NodeExpr getCondition() {
        return this.condition;
    }

    public Statement thenStatement() {
        return this.thenStatement;
    }

    public Statement elseStatement() {
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
