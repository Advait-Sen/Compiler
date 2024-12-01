package parser.node.statement;

import parser.node.NodeExpr;
import parser.node.identifier.NodeIdentifier;
import tokeniser.Token;

/**
 * Here the expression must evaluate to the type indicated by {@link StaticDeclareStatement#type}
 */
public class StaticDeclareStatement extends DeclareStatement {
    public final Token type;

    public StaticDeclareStatement(Token type, NodeIdentifier identifier, Token declarer, NodeExpr expr) {
        super(identifier, declarer, expr);
        this.type = type;
    }

    @Override
    public String asString() {
        return String.join(" ", type.value, identifier.asString(), declarer.value, expression.asString());
    }

    @Override
    public String typeString() {
        return "static_declaration"; //Maybe the _ isn't needed here, but keep it in case I want it later on
    }
}
