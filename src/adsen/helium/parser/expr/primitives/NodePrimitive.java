package adsen.helium.parser.expr.primitives;

import adsen.helium.parser.expr.NodeExpr;
import adsen.helium.tokeniser.Token;

public abstract sealed class NodePrimitive implements NodeExpr permits IntPrimitive, CharPrimitive, FloatPrimitive, BoolPrimitive {
    final Token token;

    public NodePrimitive(Token token){
        this.token = token;
    }

    public Token getToken(){
        return token;
    }

    public abstract String getTypeString();

    public abstract NodePrimitive copy();

    @Override
    public String asString() {
        return token.value;
    }
}
