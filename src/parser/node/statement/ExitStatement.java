package parser.node.statement;

import parser.node.NodeExpr;
import parser.node.primitives.BoolPrimitive;
import parser.node.primitives.CharPrimitive;
import parser.node.primitives.FloatPrimitive;
import parser.node.primitives.IntPrimitive;

public class ExitStatement implements NodeStatement {
    public ExitStatement(NodeExpr expr) {
        this.expression = expr;
    }

    NodeExpr expression;

    @Override
    public String asString() {
        return "exit " + expression.asString();
    }

    @Override
    public String typeString() {
        return "exit";
    }
}
