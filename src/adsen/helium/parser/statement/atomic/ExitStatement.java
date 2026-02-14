package adsen.helium.parser.statement.atomic;

import adsen.helium.parser.expr.NodeExpr;
import adsen.helium.parser.statement.AtomicStatement;
import adsen.helium.tokeniser.Token;

public class ExitStatement extends AtomicStatement {
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

    @Override
    public Token primaryToken() {
        return token;
    }
}
