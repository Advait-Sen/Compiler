package parser.node.statement;

import parser.node.NodeExpr;
import tokeniser.Token;

public class AssignStatement implements NodeStatement {
    Token identifier;
    Token assigner;
    NodeExpr expression;

    public AssignStatement(Token identifier, Token assigner, NodeExpr expr){
        this.identifier = identifier;
        this.assigner = assigner;
        this.expression = expr;
    }

    @Override
    public String asString() {
        return identifier.value + " " + assigner.value + " " + expression.asString();
    }

    @Override
    public String typeString() {
        return "assignment";
    }
}
