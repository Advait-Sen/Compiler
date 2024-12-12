package adsen.parser.node.expr.primitives;

import adsen.parser.node.expr.NodeExpr;
import adsen.tokeniser.Token;

public abstract sealed class NodePrimitive implements NodeExpr permits IntPrimitive, CharPrimitive, FloatPrimitive, BoolPrimitive {
    final Token token;

    public NodePrimitive(Token token){
        this.token = token;
    }

    public Token getToken(){
        return token;
    }

    public abstract String getTypeString();

    public abstract NodePrimitive negate();

    @Override
    public String asString() {
        return token.value;
    }
}
