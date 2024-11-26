package parser.node.statement;

import parser.node.NodeExpr;
import parser.node.primitives.IntPrimitive;

public class ExitStatement implements NodeStatement {
    public ExitStatement(NodeExpr expr){
        this.expression = expr;
    }

    NodeExpr expression;

    @Override
    public String asString() {
        if(expression instanceof IntPrimitive intp){
            return "exit " + intp.getValue();
        }

        return expression.toString();
    }
}
