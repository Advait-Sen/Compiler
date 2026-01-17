package adsen.parser.node.statement;

import adsen.tokeniser.Token;

public class BreakStatement implements Statement {
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
}
