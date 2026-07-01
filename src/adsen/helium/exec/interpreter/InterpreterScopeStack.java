package adsen.helium.exec.interpreter;

import adsen.helium.parser.HeliumFunction;
import adsen.helium.parser.expr.primitives.NodePrimitive;
import adsen.helium.parser.statement.HeliumStatement;
import adsen.helium.parser.statement.aggregate.ScopeStatement;
import adsen.helium.parser.statement.atomic.FunctionCallStatement;
import java.util.List;
import java.util.Stack;

import static adsen.helium.exec.interpreter.ExitCause.*;

/**
 * This is the object that will be used to track scopes when interpreting
 */
public class InterpreterScopeStack {

    Stack<InterpreterScope> scopeStack = new Stack<>();

    public InterpreterScope currentScope() {
        return scopeStack.peek();
    }

    final InterpreterFunctionScope functionScope;

    public InterpreterScopeStack(InterpreterFunctionScope functionScope) {
        this.functionScope = functionScope;
        scopeStack.push(new FunctionScope(functionScope));
    }

    // STATEMENT CODE

    public boolean scopeHasStatements() {
        return currentScope().hasMoreStatements();
    }

    public HeliumStatement nextStatement() {
        return currentScope().nextStatement();
    }

    // SCOPE CREATION CODE
    public void addLoopScope(String name, ScopeStatement scope) {
        scopeStack.push(InterpreterScope.loop(name, scope, currentScope()));
    }

    public void addNestedScope(String name, ScopeStatement scope) {
        scopeStack.push(InterpreterScope.nested(name, scope, currentScope()));
    }


    // SCOPE EXIT CODE

    public void functionReturn(NodePrimitive value) {
        endCurrentScope(RETURN, value);
    }

    public boolean functionExit(NodePrimitive value) {
        return endCurrentScope(EXIT, value);
    }

    public boolean loopBreak() {
        return endCurrentScope(LOOP_BREAK);
    }

    public boolean loopContinue() {
        return endCurrentScope(LOOP_CONTINUE);
    }

    private boolean endCurrentScope(ExitCause cause) {
        return endCurrentScope(cause, null);
    }

    private boolean endCurrentScope(ExitCause cause, NodePrimitive value) {
        boolean result = currentScope().endScope(cause, value);

        if (!result) {
            return false; //TODO see who handles errors here
        }

        //Removing all scopes which have finished
        while (!scopeStack.isEmpty() && currentScope().finished) {
            scopeStack.pop();
        }

        return true;
    }

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

    NodePrimitive removeVariable(String variableName) {
        return currentScope().removeVariable(variableName);
    }
}
