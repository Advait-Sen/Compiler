package adsen.helium.parser.statement;

import adsen.helium.tokeniser.Token;

public class ContinueStatement implements HeliumStatement {
    public final Token token;
    public ContinueStatement(Token token){
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
