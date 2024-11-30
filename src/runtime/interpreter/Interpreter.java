package runtime.interpreter;

import error.ExpressionError;
import parser.node.NodeExpr;
import parser.node.NodeProgram;
import parser.node.identifier.NodeIdentifier;
import parser.node.primitives.IntPrimitive;
import parser.node.primitives.NodePrimitive;
import parser.node.statement.ExitStatement;
import parser.node.statement.NodeStatement;

import java.util.Map;

/**
 * A class which will interpret Helium programming language, instead of compiling
 * This is simply because interpreting it is much easier than compiling,
 * and writing an runtime.interpreter is good practice for writing a compiler (I think)
 */
public class Interpreter {

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

    public long run() throws ExpressionError {

        for (int i = 0; i < program.statements.size(); i++) {
            NodeStatement statement = program.statements.get(i);

            if (statement instanceof ExitStatement exit) {
                NodePrimitive exitValue = evaluateExpr(exit.expr());
                if (!(exitValue instanceof IntPrimitive intVal))
                    throw new ExpressionError("Must have ", exit.token);

                return intVal.getValue();
            }
        }

        return 0;
    }

    private NodePrimitive evaluateExpr(NodeExpr expr) {
        if (expr instanceof NodePrimitive) {
            return (NodePrimitive) expr;
        }

        if (expr instanceof NodeIdentifier ident) {
            if (!variables.containsKey(ident.asString()))
                throw new ExpressionError("Unknown variable '" + ident.asString() + "'", ident.token);

            return variables.get(ident.asString());
        }

        return new IntPrimitive(0);
    }

}
