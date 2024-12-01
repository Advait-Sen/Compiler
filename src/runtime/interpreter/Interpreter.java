package runtime.interpreter;

import error.ExpressionError;
import parser.node.NodeExpr;
import parser.node.NodeProgram;
import parser.node.identifier.NodeIdentifier;
import parser.node.primitives.IntPrimitive;
import parser.node.primitives.NodePrimitive;
import parser.node.statement.DeclareStatement;
import parser.node.statement.ExitStatement;
import parser.node.statement.NodeStatement;
import parser.node.statement.StaticDeclareStatement;
import runtime.Scope;

import java.util.Map;
import java.util.Stack;

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
     * Stack used to keep track of scopes
     */
    public Stack<Scope> scopeStack;

    public Scope currentScope;

    public Interpreter(NodeProgram program) {
        this.program = program;
    }

    public NodePrimitive run() throws ExpressionError {
        scopeStack = new Stack<>();

        currentScope = Scope.empty("main"); //this is gonna change when I implement main function

        scopeStack.push(currentScope);

        for (int i = 0; i < program.statements.size(); i++) {
            NodeStatement statement = program.statements.get(i);

            if (statement instanceof ExitStatement exit) {
                return evaluateExpr(exit.expr());
            } else if (statement instanceof DeclareStatement declare) {

                if (currentScope.hasVariable(declare.identifier().asString())) {
                    //Copy of Java error message
                    throw new ExpressionError("Variable '" + declare.identifier().asString() + "' is already defined in the scope", declare.identifier().token);
                }

                NodePrimitive value = evaluateExpr(declare.expr());

                if (declare instanceof StaticDeclareStatement staticDeclare) {
                    String requiredType = staticDeclare.valueType.value;
                    String providedType = value.getTypeString();

                    if (!requiredType.equals(providedType)) {
                        throw new ExpressionError("Required '" + requiredType + "', provided '" + providedType + "'", staticDeclare.identifier().token);
                    }
                }

                currentScope.setVariable(declare.identifier().asString(), value);
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
            if (!currentScope.hasVariable(ident.asString()))
                throw new ExpressionError("Unknown variable '" + ident.asString() + "'", ident.token);

            return currentScope.getVariable(ident.asString());
        }

        return IntPrimitive.of(0);
    }


    /**
     * Currently only used for verbose messages, but might in future be more useful.
     */
    public Map<String, NodePrimitive> variables() {
        return currentScope.getVariables();
    }
}
