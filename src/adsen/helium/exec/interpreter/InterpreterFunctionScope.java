package adsen.helium.exec.interpreter;

import adsen.helium.error.InterpreterError;
import adsen.helium.parser.HeliumFunction;
import adsen.helium.parser.expr.primitives.NodePrimitive;
import adsen.helium.parser.statement.HeliumStatement;
import adsen.helium.tokeniser.Token;
import java.util.List;
import java.util.function.Consumer;

import static adsen.helium.parser.HeliumProgram.MAIN_FUNCTION;

public class InterpreterFunctionScope {
    final InterpreterScopeStack scopeStack;
    private final HeliumFunction function;
    private final HeliumStatement exprStatement;
    private final Consumer<NodePrimitive> returnHandler;

    InterpreterFunctionScope(HeliumFunction function, HeliumStatement exprStatement, List<NodePrimitive> arguments, Consumer<NodePrimitive> returnHandler) {
        this.function = function;
        this.exprStatement = exprStatement;
        this.returnHandler = returnHandler;
        this.scopeStack = new InterpreterScopeStack(this);

        List<String> typeSignature = function.getTypeSignature();
        boolean argumentSettingSucceeded = true;
        int i = 0;
        for (; i < typeSignature.size() && argumentSettingSucceeded; i++) {
            argumentSettingSucceeded = createVariable(typeSignature.get(i), arguments.get(i));
        }
        if (!argumentSettingSucceeded)
            throw new InterpreterError("Unable to set variable " + typeSignature.get(i), exprStatement);
    }

    public HeliumFunction getFunction() {
        return function;
    }

    public Token returnType() {
        return function.returnType;
    }

    public HeliumStatement getFunctionCall() {
        return exprStatement;
    }

    public String name() {
        return exprStatement == null ? MAIN_FUNCTION : exprStatement.asString();
    }


    boolean hasVariable(String variableName) {
        return scopeStack.hasVariable(variableName);
    }

    //Otherwise they would eventually get garbage collected by Java when the scope ends.
    //But since freeing will also be a thing in the code generator, it makes sense to have it as a functionality here

    NodePrimitive getVariable(String variableName) {
        return scopeStack.getVariable(variableName);
    }


    boolean setVariable(String variableName, NodePrimitive newValue) {
        return scopeStack.setVariable(variableName, newValue);
    }

    NodePrimitive removeVariable(String variableName) {
        return scopeStack.removeVariable(variableName);
    }

    /**
     * Attempts to create a new variable with this name in this scope.
     * If the variable already exists, returns false, to be handled by the caller
     */
    boolean createVariable(String variableName, NodePrimitive initialValue) {
        return scopeStack.createVariable(variableName, initialValue);
    }

    boolean hasMoreStatements() {
        return scopeStack.scopeHasStatements();
    }

    HeliumStatement nextStatement() {
        return scopeStack.nextStatement();
    }

    public void handleReturn(NodePrimitive value) {
        scopeStack.functionReturn(value);
        returnHandler.accept(value);
    }
}
