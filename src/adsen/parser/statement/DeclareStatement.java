package adsen.parser.statement;

import adsen.parser.expr.NodeExpr;
import adsen.parser.expr.NodeIdentifier;
import adsen.tokeniser.Token;

public class DeclareStatement implements Statement {
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

    @Override
    public Token primaryToken() {
        return declarer;
    }
}
