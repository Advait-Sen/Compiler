package adsen.parser.statement;

import adsen.parser.node.expr.NodeExpr;
import adsen.tokeniser.Token;

/**
 *  Structurally identical to a {@link ExitStatement}, but it should return a value when used within a function
 */
public class ReturnStatement implements Statement {
    public final Token token;
    NodeExpr expression;

    public ReturnStatement(Token exit, NodeExpr expr) {
        this.token = exit;
        this.expression = expr;
    }

    public NodeExpr expr() {
        return expression;
    }

    @Override
    public String asString() {
        return "return " + expression.asString();
    }

    @Override
    public String typeString() {
        return "return";
    }
}
