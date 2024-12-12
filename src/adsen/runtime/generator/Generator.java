package adsen.runtime.generator;

import adsen.parser.node.expr.NodeExpr;
import adsen.parser.node.NodeProgram;

import java.util.Set;
import java.util.Stack;

/**
 * Classes which generate assembly from code will inherit from this class.
 * <p>
 * In order to make it easier (and since the language is still pretty simple, a lot of work is standardised
 * This means that classes inheriting from {@link Generator} will simply need to implement the direct assembly-translating methods
 */
public abstract class Generator {
    /**
     * The program from which we are generating assembly
     */
    public final NodeProgram program;
    /**
     * Stack used to keep track of variables and expressions at compiletime
     */
    private final Stack<NodeExpr> exprStack = new Stack<>();
    /**
     * Final code generated
     */
    private StringBuilder code;
    /**
     * Map used to keep track of variables
     */
    private Set<String> variables;

    public Generator(NodeProgram program) {
        this.program = program;
    }

}
