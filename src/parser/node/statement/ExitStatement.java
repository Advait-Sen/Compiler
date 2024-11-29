package parser.node.statement;

import parser.node.NodeExpr;

public class ExitStatement implements NodeStatement {
    public ExitStatement(NodeExpr expr) {
        this.expression = expr;
    }

    NodeExpr expression;

    public NodeExpr expr() {
        return expression;
    }

    @Override
    public String asString() {
        return "exit " + expression.asString();
    }

    @Override
    public String typeString() {
        return "exit";
    }
}
