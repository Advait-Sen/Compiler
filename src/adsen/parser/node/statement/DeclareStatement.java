package adsen.parser.node.statement;

import adsen.parser.node.expr.NodeExpr;
import adsen.parser.node.expr.NodeIdentifier;
import adsen.tokeniser.Token;

public class DeclareStatement implements NodeStatement {
    NodeIdentifier identifier;
    Token declarer;
    NodeExpr expression;

    public DeclareStatement(NodeIdentifier identifier, Token declarer, NodeExpr expr) {
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
        return String.join(" ", "let", identifier.asString(), declarer.value, expression.asString());
    }

    @Override
    public String typeString() {
        return "declaration";
    }
}
