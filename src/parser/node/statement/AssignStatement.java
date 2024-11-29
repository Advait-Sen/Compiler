package parser.node.statement;

import parser.node.NodeExpr;
import parser.node.identifier.NodeIdentifier;
import tokeniser.Token;

public class AssignStatement implements NodeStatement {
    NodeIdentifier identifier;
    Token assigner;
    NodeExpr expression;

    public AssignStatement(NodeIdentifier identifier, Token assigner, NodeExpr expr) {
        this.identifier = identifier;
        this.assigner = assigner;
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
        return identifier.asString() + " " + assigner.value + " " + expression.asString();
    }

    @Override
    public String typeString() {
        return "assignment";
    }
}
