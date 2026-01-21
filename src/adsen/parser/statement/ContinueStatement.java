package adsen.parser.statement;

import adsen.tokeniser.Token;

public class ContinueStatement implements Statement {
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
