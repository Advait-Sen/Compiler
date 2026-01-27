package adsen.helium.parser.statement;

import adsen.helium.tokeniser.Token;

public class BreakStatement implements HeliumStatement {
    public final Token token;
    public BreakStatement(Token token){
        this.token = token;
    }

    @Override
    public String asString() {
        return token.value;
    }

    @Override
    public String typeString() {
        return "continue";
    }

    @Override
    public Token primaryToken() {
        return token;
    }
}
