package parser.node.statement;

import parser.node.NodeExpr;
import parser.node.identifier.NodeIdentifier;
import tokeniser.Token;

/**
 * Here the expression must evaluate to the type indicated by {@link StaticAssignStatement#type}
 */
public class StaticAssignStatement extends AssignStatement {
    public final Token type;

    public StaticAssignStatement(Token type, NodeIdentifier identifier, Token assigner, NodeExpr expr) {
        super(identifier, assigner, expr);
        this.type = type;
    }

    @Override
    public String asString() {
        return String.join(" ", type.value, identifier.asString(), assigner.value, expression.asString());
    }

    @Override
    public String typeString() {
        return "static_assignment"; //Maybe the _ isn't needed here, but keep it in case I want it later on
    }
}
