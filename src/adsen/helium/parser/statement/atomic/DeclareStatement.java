package adsen.helium.parser.statement.atomic;

import adsen.helium.parser.expr.NodeExpr;
import adsen.helium.parser.expr.NodeIdentifier;
import adsen.helium.parser.statement.HeliumStatement.AtomicStatement;
import adsen.helium.tokeniser.Token;

public class DeclareStatement extends AtomicStatement {
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
