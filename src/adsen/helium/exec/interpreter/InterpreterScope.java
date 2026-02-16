package adsen.helium.exec.interpreter;

import adsen.helium.parser.HeliumFunction;
import adsen.helium.parser.expr.primitives.NodePrimitive;
import adsen.helium.parser.statement.HeliumStatement;
import adsen.helium.parser.statement.aggregate.ScopeStatement;
import adsen.helium.parser.statement.atomic.FunctionCallStatement;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static adsen.helium.parser.HeliumProgram.MAIN_FUNCTION;


public abstract class InterpreterScope {

    NodePrimitive returnValue = null;

    /**
     * Represents if this scope is ended. It's set either when {@link InterpreterScope#endScope(ExitCause, NodePrimitive)}
     * is called in this scope, or in a child scope that propagated the call up to this one.
     */
    boolean finished = false;

    InterpreterScope parent;

    // CODE TO HANDLE STATEMENTS
    List<HeliumStatement> scopeStatements = getStatements();
    int statementPosition = -1;

    boolean hasMoreStatements() {
        return statementPosition + 1 < scopeStatements.size();
    }

    HeliumStatement nextStatement() {
        statementPosition++;
        return scopeStatements.get(statementPosition);
    }


    // CODE TO BE OVERWRITTEN BY NEW SCOPE TYPES

    abstract List<HeliumStatement> getStatements();

    /**
     * Attempts to exit a scope. A value of true means the scope was exited successfully. A value of false means the
     * {@link ExitCause} was unable to propagate to the right parent scope, causing an error.
     * The default implementation is to pass onto the parent scope, leaving it to handle things
     */
    boolean endScope(ExitCause cause, NodePrimitive value) {
        finished = true;

        return parent.endScope(cause, value);
    }

    public abstract String name();

    /**
     * Statement responsible for creating this scope, todo maybe use this for errors
     */
    public abstract HeliumStatement responsibleStatement();

    //Might not be necessary idk
    public abstract ScopeType scopeType();


    // FUNCTIONS TO GENERATE NEW SCOPES; TO BE EXTRACTED EVENTUALLY

    static InterpreterScope function(FunctionCallStatement stmt, HeliumFunction function, InterpreterScope parent) {
        FunctionScope fScope = new FunctionScope(stmt, function);
        fScope.parent = parent;
        return fScope;
    }

    static InterpreterScope nested(String name, ScopeStatement stmt, InterpreterScope parent) {
        NestedScope scope = new NestedScope(name, stmt);
        scope.parent = parent;
        return scope;
    }

    static InterpreterScope loop(String name, ScopeStatement stmt, InterpreterScope parent) {
        LoopScope scope = new LoopScope(name, stmt);
        scope.parent = parent;
        return scope;
    }


    // VARIABLE HANDLING CODE

    Map<String, NodePrimitive> variables = new HashMap<>();

    /**
     * Returns whether a variable of this name is accessible here, either from this scope or parent scopes
     */
    boolean hasVariable(String variableName) {
        return variables.containsKey(variableName) || (parent != null && parent.hasVariable(variableName));
    }

    /**
     * Returns the value of a particular variable, either from this scope or from parent scopes.
     * If the variable doesn't exit, returns null, to be handled by the caller
     */
    NodePrimitive getVariable(String variableName) {
        if (variables.containsKey(variableName)) {
            return variables.get(variableName);

        } else if (parent != null) {
            return parent.getVariable(variableName);

        }

        return null;
    }

    /**
     * Attempts to create a new variable with this name in this scope.
     * If the variable already exists, returns false, to be handled by the caller
     */
    boolean createVariable(String variableName, NodePrimitive initialValue) {
        if (hasVariable(variableName)) return false;

        variables.put(variableName, initialValue);

        return true;
    }

    /**
     * Attempts to set the value of a variable in this scope or a parent scope.
     * Leaves type checking to the caller.
     * Returns false if the variable did not exist, to be handled by the caller
     */
    boolean setVariable(String variableName, NodePrimitive newValue) {
        if (variables.containsKey(variableName)) {
            variables.put(variableName, newValue);
            return true;
        } else if (parent != null) {
            return parent.setVariable(variableName, newValue);
        }

        return false;
    }
}

enum ScopeType {
    NESTED_SCOPE,
    FUNCTION,
    LOOP,
}

enum ExitCause {
    //ERROR, //For when we eventually throw errors within code
    RETURN,
    EXIT,
    LOOP_BREAK,
    LOOP_CONTINUE
}

/**
 * New scope created from a function call statement. It overrides a lot of variable setting code, since it shouldn't
 * have access to variables from the scope in which the function was called, except global variables, which will be
 * handled by the {@link InterpreterScopeStack} class, and not here.
 * <p>
 * TODO make main function scope, which won't have a parent or a FunctionCallStatement, just a HeliumFunction of the main function
 */
class FunctionScope extends InterpreterScope {

    final HeliumFunction function;
    final FunctionCallStatement functionCall;

    FunctionScope(FunctionCallStatement stmt, HeliumFunction function) {
        this.function = function;
        this.functionCall = stmt;
    }

    @Override
    public HeliumStatement responsibleStatement() {
        return functionCall;
    }

    @Override
    public ScopeType scopeType() {
        return ScopeType.FUNCTION;
    }

    @Override
    List<HeliumStatement> getStatements() {
        return function.getBody();
    }

    @Override
    public boolean endScope(ExitCause cause, NodePrimitive value) {
        finished = true;

        // Handles exits and returns. Doesn't handle loop stuff
        return switch (cause) {
            case EXIT -> {
                if (parent == null) { // we are in main scope
                    yield true;
                }
                //This propagates the value up to the parent scope, ending all scopes along the way
                yield parent.endScope(cause, value);
            }

            //Type checking is handled prior to this, so they get essentially the same code
            case RETURN -> {
                returnValue = value;
                yield true;
            }

            default -> false;
        };
    }

    @Override
    public String name() {
        return functionCall == null ? MAIN_FUNCTION : functionCall.asString();
    }


    @Override
    boolean hasVariable(String variableName) {
        return variables.containsKey(variableName);
    }

    //TODO eventually implement freeing variables, which (in the interpreter) will remove them from this map
    //Otherwise they would eventually get garbage collected by Java when the scope ends.
    //But since freeing will also be a thing in the code generator, it makes sense to have it as a functionality here
    @Override
    NodePrimitive getVariable(String variableName) {
        if (variables.containsKey(variableName)) {
            return variables.get(variableName);
        }
        return null;
    }


    @Override
    boolean setVariable(String variableName, NodePrimitive newValue) {
        if (variables.containsKey(variableName)) {
            variables.put(variableName, newValue);
            return true;
        }

        return false;
    }
}

class NestedScope extends InterpreterScope {

    final ScopeStatement statement;
    final String name;

    NestedScope(String name, ScopeStatement stmt) {
        this.name = name;
        this.statement = stmt;
    }

    @Override
    public HeliumStatement responsibleStatement() {
        return statement;
    }

    @Override
    public ScopeType scopeType() {
        return ScopeType.NESTED_SCOPE;
    }

    @Override
    List<HeliumStatement> getStatements() {
        return statement.statements;
    }

    @Override
    public String name() {
        return name;
    }
}

/**
 * This is functionally identical to a {@link NestedScope}, except it can handle loop exits
 */
class LoopScope extends NestedScope {

    LoopScope(String name, ScopeStatement stmt) {
        super(name, stmt);
    }

    @Override
    public ScopeType scopeType() {
        return ScopeType.LOOP;
    }

    @Override
    public boolean endScope(ExitCause cause, NodePrimitive value) {
        finished = true;

        // Handles loop stuff, sends the rest off to its parent
        return switch (cause) {
            case LOOP_BREAK -> true;

            //Continue just resets the position back to -1, since we're re-doing all the statements in the loop
            //TODO see if this jank-ass solution works
            case LOOP_CONTINUE -> {
                finished = false; // We'd still be in the same scope
                statementPosition = -1;
                yield true;
            }

            default -> parent.endScope(cause, value);
        };
    }
}
