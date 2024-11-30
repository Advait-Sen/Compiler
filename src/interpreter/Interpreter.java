package interpreter;

import parser.node.NodeExpr;
import parser.node.NodeProgram;
import parser.node.primitives.NodePrimitive;

import java.util.Map;
import java.util.Stack;

/**
 * A class which will interpret Helium programming language, instead of compiling
 * This is simply because interpreting it is much easier than compiling,
 * and writing an interpreter is good practice for writing a compiler (I think)
 */
public abstract class Interpreter {

    /**
     * The program from which we are generating assembly
     */
    public final NodeProgram program;
    /**
     * Stack used to keep track of variables and expressions at compiletime
     */
    private final Stack<NodeExpr> exprStack = new Stack<>();

    /**
     * Map used to keep track of variables
     */
    private Map<String, NodePrimitive> variables;

    public Interpreter(NodeProgram program) {
        this.program = program;
    }



}
