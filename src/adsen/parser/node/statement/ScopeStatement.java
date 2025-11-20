package adsen.parser.node.statement;

import adsen.error.ExpressionError;
import java.util.List;

/**
 * A scope will be declared by curly brackets surrounding some statements
 */
public class ScopeStatement implements NodeStatement {
    public final List<NodeStatement> statements;
    private LoopCondition loopState;
    public final String name; //If applicable

    public ScopeStatement(List<NodeStatement> statements) {
        this(statements, "");
    }

    public ScopeStatement(List<NodeStatement> statements, String name) {
        this.statements = statements;
        this.name = name;
        this.loopState = LoopCondition.NOT_LOOP;
    }

    public void setLoop() {
        this.loopState = LoopCondition.LOOP;
    }

    public boolean isLoop() {
        return this.loopState != LoopCondition.NOT_LOOP;
    }

    /**
     * Will do nothing if this is not a loop. Leaves throwing errors to caller
     */
    public void loopContinue() {
        if (isLoop()) {
            loopState = LoopCondition.LOOP_CONTINUE;
        }
    }

    /**
     * Will do nothing if this is not a loop. Leaves throwing errors to caller
     * <p>
     * If continue is called in a loop, it will return the state to {@link LoopCondition#LOOP} and return true.
     * If continue wasn't called, it will stay in the current state and return false
     */
    public boolean returnFromContinue() {
        if (isLoop() && loopState == LoopCondition.LOOP_CONTINUE) {
            loopState = LoopCondition.LOOP;
            return true;
        }
        return false;
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

/**
 * Just a little enum to track what the state is with regard to loops
 */
enum LoopCondition {
    NOT_LOOP,
    LOOP,
    LOOP_CONTINUE,
    LOOP_BREAK
}
