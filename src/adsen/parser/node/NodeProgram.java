package adsen.parser.node;

import adsen.parser.node.statement.NodeStatement;

import java.util.ArrayList;
import java.util.List;

public class NodeProgram {

    /**
     * List of statements in the program (old system)
     */
    @Deprecated
    public List<NodeStatement> statements = new ArrayList<>();
    /**
     * List of functions in the program (new system)
     */
    public List<NodeFunction> functions = new ArrayList<>();

    public String asStringOld() {
        return statements.stream().map(NodeStatement::asString).reduce("", (s1, s2) -> s1 + '\n' + s2);
    }

    /**
     * New way of getting string form of program, will supersede old {@link NodeProgram#asStringOld()}
     * once refactoring is complete
     */
    public String asString() {
        return functions.stream().map(NodeFunction::asString).reduce("", (s1, s2) -> s1 + "\n\n" + s2);
    }
}
