package adsen.parser.node.primitives;

import adsen.parser.node.NodeExpr;
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

    @Override
    public final String asString(){
        return token.value;
    }
}
