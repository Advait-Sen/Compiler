package adsen.parser.statement;

import java.util.List;

/**
 * A scope will be declared by curly brackets surrounding some statements
 */
public class ScopeStatement implements Statement {
    public final List<Statement> statements;
    private boolean isLoop = false;
    public final String name; //If applicable

    public ScopeStatement(List<Statement> statements) {
        this(statements, "");
    }

    public ScopeStatement(List<Statement> statements, String name) {
        this.statements = statements;
        this.name = name;
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
        for (Statement statement : statements) {
            string.append("    ").append(statement.typeString()).append(" : ").append(statement.asString()).append('\n');
        }

        return string.toString();
    }

    @Override
    public String typeString() {
        return "scope (" + (isNamed() ? name + ", " : "") + statements.size() + ")";
    }
}