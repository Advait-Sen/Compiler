package adsen.helium.exec.interpreter;

import adsen.helium.parser.expr.primitives.NodePrimitive;
import java.util.Optional;
import java.util.Stack;

import static adsen.helium.exec.interpreter.ExitCause.*;

/**
 * This is the object that will be used to track scopes when interpreting
 */
public class InterpreterScopeStack extends Stack<InterpreterScope> {



    public InterpreterScopeStack() {
        // Add a new function scope for main function
    }

    public InterpreterScope currentScope() {
        return peek();
    }

    public boolean functionReturn(NodePrimitive value) {
        return currentScope().endScope(RETURN, value);
    }

    public boolean functionExit(NodePrimitive value) {
        return currentScope().endScope(EXIT, value);
    }

    public boolean loopBreak() {
        return currentScope().endScope(LOOP_BREAK);
    }

    public boolean loopContinue() {
        return currentScope().endScope(LOOP_CONTINUE);
    }

    /**
     * Returns the value of a particular variable from the scope. If the variable doesn't exist in the scope,
     * then we will eventually check global variables. But for now, that's just an error, so we return null.
     */
    NodePrimitive getVariable(String variableName) {
        return currentScope().getVariable(variableName);
    }
}
