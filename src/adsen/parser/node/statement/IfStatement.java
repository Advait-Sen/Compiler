package adsen.parser.node.statement;

import adsen.parser.node.NodeExpr;
import adsen.tokeniser.Token;

public class IfStatement implements NodeStatement {
    public final Token token;
    NodeExpr condition;
    NodeStatement thenStatement;

    /**
     * Parser flag based on whether or not the if statement has got its condition
     */
    private boolean complete;

    public IfStatement(Token token, NodeExpr condition) {
        this.token = token;
        this.condition = condition;
        this.complete = false;
    }

    public IfStatement(Token token, NodeExpr condition, NodeStatement statement) {
        this.token = token;
        this.condition = condition;
        this.thenStatement = statement;
        this.complete = true;
    }

    public void setStatement(NodeStatement statement) {
        if (!complete) {
            this.thenStatement = statement;
            this.complete = true;
        }
    }

    public NodeExpr getCondition(){
        return this.condition;
    }

    public NodeStatement thenStatement(){
        return this.thenStatement;
    }

    public boolean isComplete(){
        return this.complete;
    }

    @Override
    public String asString() {
        return "if (" + condition.asString() + "): " + thenStatement.asString();
    }

    @Override
    public String typeString() {
        return "if";
    }
}
