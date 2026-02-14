package adsen.helium.parser.statement.atomic;

import adsen.helium.parser.statement.AtomicStatement;
import adsen.helium.tokeniser.Token;

public class BreakStatement extends AtomicStatement {
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
