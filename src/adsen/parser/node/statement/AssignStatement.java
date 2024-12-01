package adsen.parser.node.statement;

import adsen.parser.node.NodeExpr;
import adsen.parser.node.identifier.NodeIdentifier;
import adsen.tokeniser.Token;

public class AssignStatement implements NodeStatement {
    NodeIdentifier identifier;
    Token declarer;
    NodeExpr expression;

    public AssignStatement(NodeIdentifier identifier, Token declarer, NodeExpr expr) {
        this.identifier = identifier;
        this.declarer = declarer;
        this.expression = expr;
    }

    public NodeIdentifier identifier() {
        return identifier;
    }

    public NodeExpr expr() {
        return expression;
    }

    @Override
    public String asString() {
        return String.join(" ", identifier.asString(), declarer.value, expression.asString());
    }

    @Override
    public String typeString() {
        return "assignment";
    }
}
