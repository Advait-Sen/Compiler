package adsen.parser.node.statement;

import adsen.parser.node.expr.NodeExpr;
import adsen.tokeniser.Token;

public class ExitStatement implements NodeStatement {
    public final Token token;

    public ExitStatement(Token exit, NodeExpr expr) {
        this.token = exit;
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
