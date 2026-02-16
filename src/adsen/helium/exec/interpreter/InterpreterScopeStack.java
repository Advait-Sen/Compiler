package adsen.helium.exec.interpreter;

import adsen.helium.parser.expr.primitives.NodePrimitive;
import adsen.helium.parser.statement.HeliumStatement;
import java.util.Stack;

import static adsen.helium.exec.interpreter.ExitCause.*;

/**
 * This is the object that will be used to track scopes when interpreting
 */
public class InterpreterScopeStack {

    Stack<InterpreterScope> scopeStack = new Stack<>();

    public InterpreterScopeStack() {
        // Add a new function scope for main function
    }

    public InterpreterScope currentScope() {
        return scopeStack.peek();
    }

    // STATEMENT CODE

    public boolean scopeStatementsExhausted() {
        return currentScope().hasMoreStatements();
    }

    public HeliumStatement nextStatement() {
        return currentScope().nextStatement();
    }


    // SCOPE EXIT CODE

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

//    private boolean endCurrentScope() {
//        return false;
//    }

    // VARIABLE HANDLING CODE

    /**
     * Returns the value of a particular variable within the scope, or null if it doesn't exit / is inaccessible
     */
    public NodePrimitive getVariable(String variableName) {
        return currentScope().getVariable(variableName);
    }

    /**
     * Returns whether or not a variable with that name is accessible in the scope
     */
    public boolean hasVariable(String variableName) {
        return currentScope().hasVariable(variableName);
    }

    /**
     * Attempts to create a new variable with this name in this scope. If the variable already exists (as determined by
     * {@link InterpreterScopeStack#hasVariable(String)}), returns false, to be handled by the caller
     */
    boolean createVariable(String variableName, NodePrimitive initialValue) {
        return currentScope().createVariable(variableName, initialValue);
    }

    /**
     * Attempts to set the value of a variable accessible in this scope.
     * Leaves type checking to the caller.
     * Returns false if the variable did not exist, to be handled by the caller
     */
    boolean setVariable(String variableName, NodePrimitive newValue) {
        return currentScope().setVariable(variableName, newValue);
    }
}
