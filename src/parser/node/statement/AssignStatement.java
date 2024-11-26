package parser.node.statement;

import parser.node.NodeExpr;
import tokeniser.Token;

public class AssignStatement implements NodeStatement {
    Token identifier;
    NodeExpr expression;

    @Override
    public String asString() {
        return "";
    }
}
