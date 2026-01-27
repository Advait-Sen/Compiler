package adsen.helium.parser.statement;

import adsen.helium.tokeniser.Token;
import java.util.List;

/**
 * A scope will be declared by curly brackets surrounding some statements
 */
public class ScopeStatement implements HeliumStatement {
    public final List<HeliumStatement> statements;
    private boolean isLoop = false;
    public final String name; //If applicable
    public final Token token;

    public ScopeStatement(List<HeliumStatement> statements, Token closeToken) {
        this(statements, "", closeToken);
    }

    public ScopeStatement(List<HeliumStatement> statements, String name, Token closeToken) {
        this.statements = statements;
        this.name = name;
        this.token = closeToken;
    }

    public void setLoop() {
        this.isLoop = true;
    }

    public boolean isLoop() {
        return isLoop;
    }

    public boolean isNamed() {
        return !name.isEmpty();
    }

    @Override
    public String asString() {
        StringBuilder string = new StringBuilder("\n");
        for (HeliumStatement statement : statements) {
            string.append("    ").append(statement.typeString()).append(" : ").append(statement.asString()).append('\n');
        }

        return string.toString();
    }

    @Override
    public String typeString() {
        return "scope (" + (isNamed() ? name + ", " : "") + statements.size() + ")";
    }

    @Override
    public Token primaryToken() {
        return token;
    }
}