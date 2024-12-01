package parser.node.primitives;

import parser.node.NodeExpr;
import tokeniser.Token;

public abstract sealed class NodePrimitive implements NodeExpr permits IntPrimitive, CharPrimitive, FloatPrimitive, BoolPrimitive {
    final Token token;

    public NodePrimitive(Token token){
        this.token = token;
    }

    @Override
    public boolean isRoot(){
        return false;
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
