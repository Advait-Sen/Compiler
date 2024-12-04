package adsen.parser.node.statement;

import java.util.List;

/**
 * A scope will be declared by curly brackets surrounding some statements
 */
public class ScopeStatement implements NodeStatement {
    public final List<NodeStatement> statements;
    public final String name; //If applicable

    public ScopeStatement(List<NodeStatement> statements) {
        this(statements, "");
    }

    public ScopeStatement(List<NodeStatement> statements, String name) {
        this.statements = statements;
        this.name = name;
    }


    public boolean isNamed() {
        return !name.isEmpty();
    }

    @Override
    public String asString() {
        StringBuilder string = new StringBuilder("\n");
        for (NodeStatement statement : statements) {
            string.append("    ").append(statement.typeString()).append(" : ").append(statement.asString()).append('\n');
        }

        return string.toString();
    }

    @Override
    public String typeString() {
        return "scope (" + (isNamed() ? name + ", " : "") + statements.size() + ")";
    }
}
