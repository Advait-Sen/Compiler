package parser.node.statement;

import parser.node.NodeExpr;
import parser.node.identifier.NodeIdentifier;
import tokeniser.Token;

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
