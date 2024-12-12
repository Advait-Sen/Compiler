package adsen.parser.node;

import java.util.HashMap;
import java.util.Map;

import static adsen.runtime.Scope.MAIN_FUNCTION;

public class NodeProgram {

    /**
     * Map of function names to functions
     */
    public Map<String, NodeFunction> functions = new HashMap<>();

    public NodeFunction getFunction(String name) {
        if (!functions.containsKey(name))
            throw new RuntimeException("No such function '" + name + "'");
        return functions.get(name);
    }

    public NodeFunction mainFunction() {
        return getFunction(MAIN_FUNCTION);
    }

    /**
     * Prints out all the function headers in the program
     * <p>
     * In future will contain boilerplate, imports, defines, etc.
     */
    public String asString() {
        return functions.values().stream().map(NodeFunction::asString).reduce("", (s1, s2) -> s1 + "\n\n" + s2);
    }
}
