package adsen.helium.exec.interpreter;

import adsen.helium.parser.HeliumFunction;
import adsen.helium.parser.expr.primitives.NodePrimitive;
import adsen.helium.parser.statement.HeliumStatement;
import adsen.helium.parser.statement.aggregate.ScopeStatement;
import java.util.List;
import java.util.Stack;

public class InterpreterFunctionScopeStack {
    Stack<InterpreterFunctionScope> functionStack = new Stack<>();

    public InterpreterFunctionScopeStack() {
        //Add a new scope for main function here

    }

    public InterpreterFunctionScope currentScope() {
        return functionStack.peek();
    }

    public boolean scopeHasStatements() {
        return currentScope().hasMoreStatements();
    }

    public HeliumStatement nextStatement() {
        return currentScope().nextStatement();
    }

    public void addNewFunctionScope(HeliumFunction function, HeliumStatement functionCall, List<NodePrimitive> arguments) {
        functionStack.push(InterpreterFunctionScope.fromStatement(function, functionCall, arguments, currentScope()));
    }

    public void addNewNestedScope(String name, ScopeStatement scope){
        currentScope().scopeStack.addNestedScope(name, scope);
    }
}
