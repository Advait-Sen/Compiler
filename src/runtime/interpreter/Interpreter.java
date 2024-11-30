package runtime.interpreter;

import parser.node.NodeExpr;
import parser.node.NodeProgram;
import parser.node.primitives.NodePrimitive;
import parser.node.statement.ExitStatement;
import parser.node.statement.NodeStatement;

import java.util.Map;

/**
 * A class which will interpret Helium programming language, instead of compiling
 * This is simply because interpreting it is much easier than compiling,
 * and writing an runtime.interpreter is good practice for writing a compiler (I think)
 */
public abstract class Interpreter {

    /**
     * The program from which we are generating assembly
     */
    public final NodeProgram program;

    /**
     * Map used to keep track of variables
     */
    private Map<String, NodePrimitive> variables;

    public Interpreter(NodeProgram program) {
        this.program = program;
    }

    public int run() {

        for (int i = 0; i < program.statements.size(); i++) {
            NodeStatement statement = program.statements.get(i);

            if(statement instanceof ExitStatement exit){

            }
        }

        return 0;
    }

    private NodePrimitive evaulateExpr(NodeExpr expr){
        return null;
    }

}
