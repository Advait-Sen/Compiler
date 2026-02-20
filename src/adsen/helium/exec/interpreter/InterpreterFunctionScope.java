package adsen.helium.exec.interpreter;

import adsen.helium.error.InterpreterError;
import adsen.helium.parser.HeliumFunction;
import adsen.helium.parser.expr.primitives.NodePrimitive;
import adsen.helium.parser.statement.HeliumStatement;
import adsen.helium.tokeniser.Token;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static adsen.helium.parser.HeliumProgram.MAIN_FUNCTION;

public class InterpreterFunctionScope {
    NodePrimitive returnValue = null;
    int statementPosition = -1;

    final InterpreterScopeStack scopeStack = new InterpreterScopeStack();
    final HeliumFunction function;
    final InterpreterFunctionScope parent;
    final List<NodePrimitive> arguments;
    final HeliumStatement exprStatement;

    InterpreterFunctionScope(HeliumFunction function, HeliumStatement exprStatement, InterpreterFunctionScope parent, List<NodePrimitive> arguments) {
        this.function = function;
        this.parent = parent;
        this.arguments = arguments;
        this.exprStatement = exprStatement;

        List<String> typeSignature = function.getTypeSignature();
        boolean argumentSettingSucceeded = true;
        int i = 0;
        for (; i < typeSignature.size() && argumentSettingSucceeded; i++) {
            argumentSettingSucceeded = createVariable(typeSignature.get(i), arguments.get(i));
        }
        if (!argumentSettingSucceeded)
            throw new InterpreterError("Unable to set variable " + typeSignature.get(i), exprStatement);
    }

    public static InterpreterFunctionScope fromStatement(HeliumFunction function, HeliumStatement functionCall, List<NodePrimitive> arguments, InterpreterFunctionScope parent) {
        return new InterpreterFunctionScope(function, functionCall, parent, arguments);
    }


    List<HeliumStatement> getStatements() {
        return function.getBody();
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

    final Map<String, NodePrimitive> variables = new HashMap<>();

    boolean hasVariable(String variableName) {
        return variables.containsKey(variableName);
    }

    //TODO eventually implement freeing variables, which (in the interpreter) will remove them from this map
    //Otherwise they would eventually get garbage collected by Java when the scope ends.
    //But since freeing will also be a thing in the code generator, it makes sense to have it as a functionality here

    NodePrimitive getVariable(String variableName) {
        if (variables.containsKey(variableName)) {
            return variables.get(variableName);
        }
        return null;
    }


    boolean setVariable(String variableName, NodePrimitive newValue) {
        if (variables.containsKey(variableName)) {
            variables.put(variableName, newValue);
            return true;
        }

        return false;
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

    boolean hasMoreStatements() {
        return statementPosition + 1 < getStatements().size();
    }

    HeliumStatement nextStatement() {
        statementPosition++;
        return getStatements().get(statementPosition);
    }
}
