package runtime.interpreter;

import error.ExpressionError;
import parser.node.NodeExpr;
import parser.node.NodeProgram;
import parser.node.identifier.NodeIdentifier;
import parser.node.primitives.IntPrimitive;
import parser.node.primitives.NodePrimitive;
import parser.node.statement.AssignStatement;
import parser.node.statement.ExitStatement;
import parser.node.statement.NodeStatement;
import parser.node.statement.StaticAssignStatement;

import java.util.HashMap;
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
    Map<String, NodePrimitive> variables;

    public Interpreter(NodeProgram program) {
        this.program = program;
    }

    public NodePrimitive run() throws ExpressionError {
        variables = new HashMap<>();

        for (int i = 0; i < program.statements.size(); i++) {
            NodeStatement statement = program.statements.get(i);

            if (statement instanceof ExitStatement exit) {
                return evaluateExpr(exit.expr());
            } else if (statement instanceof StaticAssignStatement staticAssign) {
                //todo this
                //todo rename assign and static assign to declare and static declare
            } else if (statement instanceof AssignStatement assign) {
                if (variables.containsKey(assign.identifier().asString())) {
                    //Copy of Java error message
                    throw new ExpressionError("Variable '" + assign.identifier().asString() + "' is already defined in the scope", assign.identifier().token);
                }
                variables.put(assign.identifier().asString(), evaluateExpr(assign.expr()));
            }
        }

        return IntPrimitive.of(0);
    }

    private NodePrimitive evaluateExpr(NodeExpr expr) {
        if (expr instanceof IntPrimitive intP) { //So that hex literals get printed in base 10. Might change once I add proper expression evaluation
            return IntPrimitive.of(intP.getValue());
        }

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


    public Map<String, NodePrimitive> variables() {
        return variables;
    }
}
