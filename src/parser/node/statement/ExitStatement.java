package parser.node.statement;

import parser.node.NodeExpr;
import tokeniser.Token;

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
