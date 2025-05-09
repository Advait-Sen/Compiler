package adsen.runtime;

import adsen.parser.node.NodeFunction;
import adsen.parser.node.expr.primitives.NodePrimitive;
import adsen.parser.node.statement.NodeStatement;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Scope {
    /**
     * Name of the main function in the program
     */
    public static final String MAIN_FUNCTION = "main";

    public final String name;
    final Map<String, NodePrimitive> variables;
    private final List<NodeStatement> statements;
    private int pos;

    Scope(String name, List<NodeStatement> statements) {
        this.name = name;
        this.variables = new HashMap<>();
        this.statements = statements;
        pos = 0;
    }

    Scope(String name, Scope existing, List<NodeStatement> statements) {
        this.name = name;
        this.variables = new HashMap<>(existing.variables);
        this.statements = statements; //Statements are part of new scope code
        pos = 0;
    }

    public static Scope empty(String name, List<NodeStatement> statements) {
        return new Scope(name, statements);
    }

    public static Scope filled(String name, Scope existing, List<NodeStatement> statements) {
        return new Scope(name, existing, statements);
    }

    /**
     * Returns a blank {@link Scope} given a function with no arguments.
     * Mostly only used for main function in the beginning
     */
    public static Scope fromFunction(NodeFunction func) {
        return fromFunction(func, Collections.emptyMap());
    }

    /**
     * Returns a blank {@link Scope} from a function.
     */
    public static Scope fromFunction(NodeFunction func, Map<String, NodePrimitive> arguments) {
        if (arguments.size() != func.args)
            throw new RuntimeException("Incorrect number of arguments, expected " + func.args + ", found " + arguments.size());

        Scope newScope = Scope.empty(func.name, func.getBody());
        newScope.variables.putAll(arguments);

        return newScope;
    }

    public NodeStatement getStatement(int i) {
        pos = i;
        return statements.get(i);
    }

    public List<NodeStatement> getStatements() {
        return statements;
    }

    public int getPos() {
        return pos;
    }


    public void setVariable(String varName, NodePrimitive value) {
        variables.put(varName, value);
    }

    public NodePrimitive getVariable(String varName) {
        return variables.get(varName);
    }

    public NodePrimitive removeVariable(String varName) {
        return variables.remove(varName);
    }

    public Map<String, NodePrimitive> getVariables() {
        return variables;
    }

    public boolean hasVariable(String varName) {
        return variables.containsKey(varName);
    }

}
