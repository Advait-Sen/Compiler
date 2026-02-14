package adsen.helium.parser.statement.atomic;

import adsen.helium.parser.expr.NodeExpr;
import adsen.helium.parser.expr.NodeIdentifier;
import adsen.helium.tokeniser.Token;

/**
 * Here the expression must evaluate to the type indicated by {@link StaticDeclareStatement#valueType}
 */
public class StaticDeclareStatement extends DeclareStatement {
    public final Token valueType;

    public StaticDeclareStatement(Token type, NodeIdentifier identifier, Token declarer, NodeExpr expr) {
        super(identifier, declarer, expr);
        this.valueType = type;
    }

    @Override
    public String asString() {
        return String.join(" ", valueType.value, identifier.asString(), declarer.value, expression.asString());
    }

    @Override
    public String typeString() {
        return "static_declaration"; //Maybe the _ isn't needed here, but keep it in case I want it later on
    }
}
