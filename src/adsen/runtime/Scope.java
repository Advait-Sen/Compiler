package adsen.runtime;

import adsen.parser.node.NodeFunction;
import adsen.parser.node.expr.primitives.NodePrimitive;
import adsen.parser.node.statement.NodeStatement;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Scope {
    /**
     * Name of the main function in the program
     */
    public static final String MAIN_FUNCTION = "main";

    public final String name;
    final Map<String, NodePrimitive> variables;
    private final List<NodeStatement> statements;
    private int pos;
    private LoopState loopState;
    private final LoopState initialLoopState;
    private final String returnType;

    private Scope(String name, List<NodeStatement> statements, String returnType) {
        this.name = name;
        this.variables = new HashMap<>();
        this.statements = statements;
        this.loopState = LoopState.NOT_LOOP;
        this.initialLoopState = loopState;
        this.returnType = returnType;
        pos = 0;
    }

    private Scope(String name, Scope existing, List<NodeStatement> statements) {
        this(name, existing, statements, existing.isLoop());
    }

    private Scope(String name, Scope existing, List<NodeStatement> statements, boolean isLoop) {
        this.name = name;
        this.variables = new HashMap<>(existing.variables);
        this.statements = statements; //Statements are part of new scope code
        this.loopState = isLoop ? LoopState.LOOP : LoopState.NOT_LOOP;
        this.initialLoopState = loopState;
        this.returnType = existing.returnType;
        pos = 0;
    }

    public static Scope empty(String name, List<NodeStatement> statements, String returnType) {
        return new Scope(name, statements,returnType);
    }

    public static Scope fromPrevious(String name, Scope existing, List<NodeStatement> statements) {
        return new Scope(name, existing, statements);
    }

    public static Scope fromPreviousWithLoop(String name, Scope existing, List<NodeStatement> statements) {
        return new Scope(name, existing, statements, true);
    }

    /**
     * Returns a blank {@link Scope} given a function with no arguments.
     * Mostly only used for main function in the beginning
     */
    public static Scope fromFunction(NodeFunction func) {
        return fromFunction(func, Collections.emptyMap());
    }

    /**
     * Returns a blank {@link Scope} from a function.
     */
    public static Scope fromFunction(NodeFunction func, Map<String, NodePrimitive> arguments) {
        if (arguments.size() != func.args)
            throw new RuntimeException("Incorrect number of arguments, expected " + func.args + ", found " + arguments.size());

        Scope newScope = Scope.empty(func.name, func.getBody(), func.returnType.value);
        newScope.variables.putAll(arguments);

        return newScope;
    }

    public String getReturnType() {
        return returnType;
    }

    public NodeStatement getStatement(int i) {
        pos = i;
        return statements.get(i);
    }

    public List<NodeStatement> getStatements() {
        return statements;
    }

    public int getPos() {
        return pos;
    }


    public void setVariable(String varName, NodePrimitive value) {
        variables.put(varName, value);
    }

    public NodePrimitive getVariable(String varName) {
        return variables.get(varName);
    }

    public NodePrimitive removeVariable(String varName) {
        return variables.remove(varName);
    }

    public Map<String, NodePrimitive> getVariables() {
        return variables;
    }

    @Deprecated
    public boolean hasVariable(String varName) {
        return variables.containsKey(varName);
    }

    public boolean hasVariableName(String varName) {
        return variables.containsKey(varName);
    }

    //Todo test this cos it 90% does not work
    public boolean hasVariableType(String varName, Class<? extends NodePrimitive> type) {
        return variables.containsKey(varName) && variables.get(varName).getClass().equals(type);
    }


    //Loop methods

    /**
     * Is it not not a loop? Then it is a loop
     */
    public boolean isLoop() {
        return this.loopState != LoopState.NOT_LOOP;
    }

    /**
     * Attempts to run {@code continue} within a loop. Leaves throwing errors in case of invalid state to the caller
     *
     * @return {@code true} if {@link Scope#loopState} was equal to {@link LoopState#LOOP}, now set to {@link LoopState#LOOP_CONTINUE}
     */
    public boolean continueLoop() {
        if (this.loopState == LoopState.LOOP) {
            this.loopState = LoopState.LOOP_CONTINUE;
            return true;
        }
        return false;
    }

    /**
     * Attempts to run {@code break} within a loop. Leaves throwing errors in case of invalid state to the caller
     *
     * @return {@code true} if {@link Scope#loopState} was equal to {@link LoopState#LOOP}, now set to {@link LoopState#LOOP_BREAK}
     */
    public boolean breakLoop() {
        if (this.loopState == LoopState.LOOP) {
            this.loopState = LoopState.LOOP_BREAK;
            return true;
        }
        return false;
    }

    /**
     * Returns true if the loop has seen a {@code continue} statement
     *
     * @return {@code true} if the loop is in the {@link LoopState#LOOP_CONTINUE} state
     */
    public boolean isLoopContinued() {
        return this.loopState == LoopState.LOOP_CONTINUE;
    }

    /**
     * Returns true if the loop has seen a {@code break} statement
     *
     * @return {@code true} if the loop is in the {@link LoopState#LOOP_BREAK} state
     */
    public boolean isLoopBroken() {
        return this.loopState == LoopState.LOOP_BREAK;
    }

    public boolean returnFromContinue() {
        if (this.loopState == LoopState.LOOP_CONTINUE) {
            this.loopState = initialLoopState;
            return true;
        }
        return false;
    }

    public boolean returnFromBreak() {
        if (this.loopState == LoopState.LOOP_BREAK) {
            this.loopState = initialLoopState;
            return true;
        }
        return false;
    }

    /**
     * When continue or break is called in a child scope, but not handled, the parent scope must inherit it
     */
    public void inheritLoopState(Scope child) {
        this.loopState = child.loopState;
    }
}

/**
 * Enum to track state with regard to loops
 */
enum LoopState {
    NOT_LOOP, LOOP, LOOP_CONTINUE, LOOP_BREAK
}
