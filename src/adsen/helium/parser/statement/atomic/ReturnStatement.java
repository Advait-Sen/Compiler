package adsen.helium.parser.statement.atomic;

import adsen.helium.parser.expr.NodeExpr;
import adsen.helium.parser.statement.AtomicStatement;
import adsen.helium.tokeniser.Token;

/**
 * Structurally identical to a {@link ExitStatement}, but it should return a value when used within a function
 */
public class ReturnStatement extends AtomicStatement {
    public final Token token;
    public final boolean empty;
    NodeExpr expression;

    public ReturnStatement(Token exit, NodeExpr expr) {
        this.token = exit;
        this.empty = expr == null;
        this.expression = expr;
    }

    public NodeExpr expr() {
        return expression;
    }

    @Override
    public String asString() {
        return "return" + (empty ? "" : " " + expression.asString());
    }

    @Override
    public String typeString() {
        return "return";
    }

    @Override
    public Token primaryToken() {
        return token;
    }
}
