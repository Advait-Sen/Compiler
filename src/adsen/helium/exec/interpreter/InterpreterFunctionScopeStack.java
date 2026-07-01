package adsen.helium.exec.interpreter;

import adsen.helium.parser.HeliumFunction;
import adsen.helium.parser.expr.primitives.NodePrimitive;
import adsen.helium.parser.statement.HeliumStatement;
import adsen.helium.parser.statement.aggregate.ScopeStatement;
import adsen.helium.parser.statement.atomic.FunctionCallStatement;
import adsen.helium.tokeniser.Token;
import adsen.helium.tokeniser.TokenType;
import java.util.Collections;
import java.util.List;
import java.util.Stack;
import java.util.function.Consumer;

public class InterpreterFunctionScopeStack {
    Stack<InterpreterFunctionScope> functionStack = new Stack<>();

    public InterpreterFunctionScopeStack(HeliumFunction mainFunction, Consumer<NodePrimitive> endMainHandler) {
        //Add a new scope for main function here
        Token mainTok = new Token();
        mainTok.append("main");
        mainTok.type = TokenType.IDENTIFIER;
        FunctionCallStatement mainCall = new FunctionCallStatement(mainTok, Collections.emptyList());
        addNewFunctionScope(mainFunction, mainCall, Collections.emptyList(), endMainHandler);
    }

    public String currentScopeName() {
        return currentScope().scopeStack.currentScope().name();
    }

    public InterpreterFunctionScope currentScope() {
        return functionStack.peek();
    }

    public boolean scopeHasStatements() {
        return !functionStack.isEmpty() && currentScope().hasMoreStatements();
    }

    public HeliumStatement nextStatement() {
        return currentScope().nextStatement();
    }

    public Token returnType() {
        return currentScope().returnType();
    }

    public void addNewFunctionScope(HeliumFunction function, HeliumStatement functionCall, List<NodePrimitive> arguments, Consumer<NodePrimitive> returnHandler) {
        functionStack.push(new InterpreterFunctionScope(function, functionCall, arguments, returnHandler));
    }

    public void addNewNestedScope(String name, ScopeStatement scope) {
        currentScope().scopeStack.addNestedScope(name, scope);
    }

    public void functionReturn(NodePrimitive value) {
        currentScope().handleReturn(value);
        functionStack.pop();
    }
}
