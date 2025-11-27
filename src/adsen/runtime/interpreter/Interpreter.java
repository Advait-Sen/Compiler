package adsen.runtime.interpreter;

import adsen.error.ExpressionError;
import adsen.parser.node.expr.FuncCallExpr;
import adsen.parser.node.expr.NodeExpr;
import adsen.parser.node.NodeFunction;
import adsen.parser.node.NodeProgram;
import adsen.parser.node.expr.NodeIdentifier;
import adsen.parser.node.expr.operator.BinaryOperator;
import adsen.parser.node.expr.operator.OperatorType;
import adsen.parser.node.expr.operator.UnaryOperator;
import adsen.parser.node.expr.primitives.*;
import adsen.parser.node.statement.*;
import adsen.runtime.Context;
import adsen.runtime.Scope;
import adsen.tokeniser.Token;

import adsen.tokeniser.TokenType;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Stack;
import java.util.function.BiFunction;
import java.util.function.DoubleBinaryOperator;
import java.util.function.Function;
import java.util.function.IntPredicate;
import java.util.function.LongBinaryOperator;
import java.util.function.Supplier;

import static adsen.runtime.Context.*;
import static adsen.runtime.Scope.MAIN_FUNCTION;

/**
 * A class which will interpret Helium programming language, instead of compiling.
 * <p>
 * This is simply because interpreting is much easier than compiling,
 * and writing an interpreter is good practice for writing a compiler (I think)
 */
public class Interpreter {

    /**
     * The statements to run
     */
    List<NodeStatement> statements;

    /**
     * The program to run
     */
    NodeProgram program;

    /**
     * Stack used to keep track of scopes
     */
    public Stack<Scope> scopeStack;

    /**
     * This is an older way of running the Interpreter, and is only used for certain testing
     */
    @SuppressWarnings("DeprecatedIsStillUsed")
    @Deprecated()
    public Interpreter(List<NodeStatement> program) {
        this.statements = program;
    }

    public Interpreter(NodeProgram program) {
        this.program = program;
    }


    /**
     * For when the Interpreter has been initialised with a {@link List}<{@link NodeStatement}>
     * instead of with {@link NodeProgram}.
     * @deprecated This is not to be used, use {@link Interpreter#run()} instead
     */
    @SuppressWarnings("DeprecatedIsStillUsed")
    @Deprecated
    public NodePrimitive runStatements() throws ExpressionError {
        scopeStack = new Stack<>();

        scopeStack.push(Scope.empty("main", statements));

        Optional<NodePrimitive> retVal = Optional.empty();

        for (int i = 0; i < scopeStack.getFirst().getStatements().size() && retVal.isEmpty(); i++) {
            retVal = executeStatement(i);
        }

        if (scopeStack.size() > 1) throw new RuntimeException("Did not pop scopes correctly");

        return retVal.orElseGet(() -> IntPrimitive.of(0));
    }

    /**
     * For when the Interpreter has been initialised with a {@link NodeProgram}
     */
    public NodePrimitive run() {
        if (!program.functions.containsKey(MAIN_FUNCTION))
            throw new RuntimeException("Program does not contain main function");

        NodeFunction mainFunction = program.mainFunction();

        scopeStack = new Stack<>();
        scopeStack.push(Scope.fromFunction(mainFunction));

        Optional<NodePrimitive> retVal = Optional.empty();

        for (int i = 0; i < scopeStack.getFirst().getStatements().size() && retVal.isEmpty(); i++) {
            retVal = executeStatement(i);
        }

        if (scopeStack.size() > 1) throw new RuntimeException("Did not pop scopes correctly");

        return retVal.orElseGet(() -> IntPrimitive.of(0));
    }

    /**
     * Executes a particular statement in the scope
     */
    Optional<NodePrimitive> executeStatement(int i) {
        return executeStatement(scope().getStatement(i));
    }

    /**
     * Executes a generic statement
     */
    Optional<NodePrimitive> executeStatement(NodeStatement statement) {
        int pos = scope().getPos(); //Completely unused, not even sure if it's accurate, but eh it does no harm to keep it jic

        Optional<NodePrimitive> ret = Optional.empty();

        switch (statement) {
            case ExitStatement exit -> ret = Optional.of(evaluateExpr(exit.expr()));

            case DeclareStatement declare -> {
                NodeIdentifier identifier = declare.identifier();

                if (scope().hasVariable(identifier.asString())) {
                    //Copy of Java error message
                    throw new ExpressionError("Variable '" + identifier.asString() + "' is already defined in the scope", identifier.token);
                }

                NodePrimitive value = evaluateExpr(declare.expr());

                if (declare instanceof StaticDeclareStatement staticDeclare) {
                    String requiredType = staticDeclare.valueType.value;
                    String providedType = value.getTypeString();

                    if (!requiredType.equals(providedType)) {
                        throw new ExpressionError("Cannot assign '" + providedType + "' to '" + requiredType + "' type", identifier.token);
                    }
                }

                scope().setVariable(identifier.asString(), value);
            }
            case AssignStatement assign -> {
                String variableName = assign.identifier().asString();

                if (!scope().hasVariable(variableName)) {
                    throw new ExpressionError("Unknown variable '" + variableName + "'", assign.identifier().token);
                }

                if (assign instanceof IncrementStatement inc) {
                    String providedType = scope().getVariable(variableName).getTypeString();
                    if (!providedType.equals(IntPrimitive.TYPE_STRING)) {
                        throw new ExpressionError("Cannot increment '" + providedType + "', only '" + IntPrimitive.TYPE_STRING + "' type", assign.identifier().token);
                    }
                    IntPrimitive value = ((IntPrimitive) scope().getVariable(variableName));
                    scope().setVariable(variableName, value.setValue(value.getValue() + (inc.incrementor == OperatorType.INCREMENT ? 1 : -1)));
                    break;
                }

                NodePrimitive value = evaluateExpr(assign.expr());

                String requiredType = scope().getVariable(variableName).getTypeString();
                String providedType = value.getTypeString();

                if (!requiredType.equals(providedType)) {
                    throw new ExpressionError("Cannot assign '" + providedType + "' to '" + requiredType + "' type", assign.identifier().token);
                }

                scope().setVariable(variableName, value);
            }
            case ScopeStatement scope -> { //todo test exit from within scopes
                Scope newScope = Scope.filled(scope.name, scope(), scope.statements);

                if (scope.isLoop()) { //If this scope statement came from a loop, then make this scope a loop
                    newScope.setLoop();
                }

                scopeStack.push(newScope);

                for (int j = 0; j < scope.statements.size() && ret.isEmpty(); j++) {
                    NodeStatement scopeNodeStmt = scope().getStatement(j);
                    if (scopeNodeStmt instanceof ContinueStatement continueStmt) {
                        handleContinueStatement(continueStmt);
                    } else { //Not gonna execute break and continue statements, since they do nothing and skip the rest of the statements in the scope
                        ret = executeStatement(j);
                    }

                    if (scope().isLoopContinued()) {
                        //Not executing the rest of the statements in this scope
                        //Calling it here, since continue might have been called (and not handled) in a child scope too
                        break;
                    }
                }
                /* For printing out scopes as debug feature
                for (Scope stackScope : scopeStack) {
                    System.out.println("Printing scope " + stackScope.name);
                    stackScope.getVariables().forEach((s, np) -> {
                        System.out.println("    " + s + " (" + np.getTypeString() + "): " + np.asStringOld());
                    });
                    System.out.println("End of scope\n");
                }
                */
                newScope = scopeStack.pop();


                //If we continued to the end of this scope, we'll inform the previous scope
                //That way, even the previous scope can continue if necessary, or reach the actual loop, in which case the loop will continue
                //Idem for break
                scope().inheritLoopState(newScope);

                // If we modified a variable that was obtained from earlier scope, then remember the update
                // This is possibly not the most efficient or correct way of doing things
                // But this might be the most that can be done with an interpreted language
                newScope.getVariables().forEach((s, np) -> {
                    if (scope().hasVariable(s)) {
                        scope().setVariable(s, np);
                    }
                });

            }
            case IfStatement ifStmt -> {
                BoolPrimitive run = evaluateExprBool(ifStmt.getCondition());

                if (run.getValue()) {
                    ret = executeStatement(ifStmt.thenStatement());
                } else if (ifStmt.hasElse()) {
                    ret = executeStatement(ifStmt.elseStatement());
                }
            }
            case WhileStatement whileStmt -> {
                while (evaluateExprBool(whileStmt.condition()).getValue() && ret.isEmpty()) {
                    //todo check for continue and break here with whileStmt.statement()
                    ret = executeStatement(whileStmt.statement());

                    //Idk what to do with the return value of this method
                    scope().returnFromContinue();
                }
            }
            case ForStatement forStmt -> {
                executeStatement(forStmt.getAssigner()); //If the assignment is an exit statement, ignore it

                String forLoopVariable = "";
                //If we managed to execute it, that means it went successfully
                if (forStmt.getAssigner() instanceof DeclareStatement declare) {
                    forLoopVariable = declare.identifier().asString();
                }

                while (evaluateExprBool(forStmt.condition()).getValue() && ret.isEmpty()) {
                    //todo check for continue and break here with forStmt.statement()
                    ret = executeStatement(forStmt.statement());

                    //Idk what to do with the return value of this method
                    scope().returnFromContinue();

                    executeStatement(forStmt.getIncrementer());
                }

                // Technically don't need this, since removing "" won't do anything
                // But this makes it more legible, and also future-safe
                if (!forLoopVariable.isEmpty()) {
                    scope().removeVariable(forLoopVariable);
                }
            }

            case FunctionCallStatement fCallStmt -> {
                //System.out.println("Calling function "+fCallStmt.name.value);
                // Check that the function called is an actual function in the scope
                // This will be more complicated later on with imports etc.
                NodeFunction func = program.getFunction(fCallStmt.name.value);

                // Check that the signature is correct
                if (fCallStmt.args.size() != func.args)
                    throw new ExpressionError("Incorrect number of arguments for function", fCallStmt.name);

                Scope newScope;

                if (func.args == 0) {
                    newScope = Scope.fromFunction(func);
                } else {
                    Map<String, NodePrimitive> arguments = new HashMap<>();

                    //We have correct number of arguments, now we check if they are of the right type
                    for (int i = 0; i < func.args; i++) {
                        String desiredType = func.getSignature().get(i * 2).value;
                        NodePrimitive argValue = evaluateExpr(fCallStmt.args.get(i));
                        String obtainedType = argValue.getTypeString();

                        if (!obtainedType.equals(desiredType)) {
                            throw new ExpressionError("Expected '" + desiredType + "', obtained '" + obtainedType + "'", fCallStmt.name);
                        } else {
                            //Name of the argument, value of the argument
                            arguments.put(func.getSignature().get(i * 2 + 1).value, argValue);
                        }
                    }

                    newScope = Scope.fromFunction(func, arguments);
                }
                scopeStack.push(newScope);

                for (int j = 0; j < newScope.getStatements().size() && ret.isEmpty(); j++) {
                    ret = executeStatement(j);
                }

                scopeStack.pop();
            }

            case ReturnStatement retStmt -> {
                Scope scope = scope();
                //System.out.println("Returning from " + scope.name);

                NodePrimitive retValue = evaluateExpr(retStmt.expr());

                //The currently executing function
                NodeFunction function = program.getFunction(scope.name);

                if (!retValue.getTypeString().equals(function.returnType.value))
                    throw new ExpressionError("Expected '" + function.returnType.value + "' return type from function '" + function.name + "', got '" + retValue.getTypeString() + "' instead", retValue.getToken());


                ret = Optional.of(retValue);
            }

            case ContinueStatement continueStmt -> handleContinueStatement(continueStmt);

            default -> //Might throw an error here at some point later on
                    System.out.println("Reached an unhandled statement type: " + statement.typeString());
        }

        return ret;
    }

    private Scope scope() {
        return scopeStack.peek();
    }

    private void handleContinueStatement(ContinueStatement continueStmt) {
        if (scope().isLoop()) {

            if (!scope().continueLoop())
                throw new ExpressionError("Unexpected 'continue', should not have reached this point", continueStmt.token);

        } else throw new ExpressionError("Unexpected 'continue' outside of loop", continueStmt.token);
    }

    private NodePrimitive evaluateExpr(NodeExpr expr) {
        return evaluateExpr(expr, NONE);
    }

    private BoolPrimitive evaluateExprBool(NodeExpr expr) {
        return (BoolPrimitive) evaluateExpr(expr, BOOL);
    }

    private IntPrimitive evaluateExprInt(NodeExpr expr) {
        return (IntPrimitive) evaluateExpr(expr, INTEGER);
    }

    private CharPrimitive evaluateExprChar(NodeExpr expr) {
        return (CharPrimitive) evaluateExpr(expr, CHAR);
    }

    private FloatPrimitive evaluateExprFloat(NodeExpr expr) {
        return (FloatPrimitive) evaluateExpr(expr, FLOAT);
    }

    private NodePrimitive evaluateExpr(NodeExpr expr, Context context) {
        /* So that hex literals get printed in base 10. Don't rly need it, but maybe it will be useful.
        if (expr instanceof IntPrimitive intP) {
            return IntPrimitive.of(intP.getValue());
        }
        */

        //todo add a new NodePrimitive type of pointer, which will point to complex objects
        //Todo add function call expr or something, which will allow for use of functions in expressions

        NodePrimitive retVal = switch (expr) {
            case NodePrimitive nodePrimitive -> nodePrimitive;

            case NodeIdentifier ident -> {
                if (!scope().hasVariable(ident.asString()))
                    throw new ExpressionError("Unknown variable '" + ident.asString() + "'", ident.token);

                yield scope().getVariable(ident.asString());
            }

            case UnaryOperator unOp -> {
                //Placeholder made up token until I figure out better error messages
                Token errorTok = new Token(unOp.asString(), unOp.type().type);

                yield switch (unOp.type()) {
                    //Todo figure out what this does to non-boolean values
                    case NOT -> evaluateExprBool(unOp.operand()).negate();

                    case INCREMENT -> {
                        IntPrimitive intP = evaluateExprInt(unOp.operand());

                        intP.setValue(intP.getValue() + 1); //This might source of problems down the line, might want to make new BoolPrimitive instead

                        yield intP;
                    }

                    case DECREMENT -> {
                        IntPrimitive intP = evaluateExprInt(unOp.operand());

                        intP.setValue(intP.getValue() - 1); //This might source of problems down the line, might want to make new BoolPrimitive instead

                        yield intP;
                    }
                    //Does literally nothing except check that it's a number
                    case POSITIVE -> {
                        NodePrimitive operand = evaluateExpr(unOp.operand());

                        //Much simpler to go by exclusion
                        if (operand instanceof BoolPrimitive) {
                            throw new ExpressionError("Expected numeric value, not 'bool'", errorTok);
                        }

                        yield operand;
                    }
                    case NEGATIVE -> {
                        NodePrimitive operand = evaluateExpr(unOp.operand());

                        yield switch (operand) {
                            case IntPrimitive intP -> IntPrimitive.of(-intP.getValue());
                            case FloatPrimitive floatP -> FloatPrimitive.of(-floatP.getValue());
                            case CharPrimitive charP -> CharPrimitive.of((char) -charP.getValue());
                            //Use ! to negate bool, not unary -
                            case BoolPrimitive _ ->
                                    throw new ExpressionError("Expected numeric value, not 'bool'", errorTok);
                        };
                    }
                    default ->
                            throw new ExpressionError("Don't know how we got here, unknown unary operator", errorTok);
                };

            }
            case BinaryOperator binOp -> {

                //Placeholder made up token until I figure out better error messages
                Token errorTok = new Token(binOp.asString(), binOp.type().type);

                //Don't keep the error as a variable since it causes expressions to be evaluated that we might not want evaluated
                Supplier<ExpressionError> errorCreator = () -> new ExpressionError("Undefined '%s' operator for '%s' and '%s'".formatted(binOp.type().value, evaluateExpr(binOp.left()).getTypeString(), evaluateExpr(binOp.right()).getTypeString()), errorTok);


                BiFunction<DoubleBinaryOperator, LongBinaryOperator, NodePrimitive> mathematicalBinOp = (dbop, lbop) -> {
                    NodePrimitive left = evaluateExpr(binOp.left());
                    NodePrimitive right = evaluateExpr(binOp.right());

                    //todo implicit casting later on
                    return switch (left) {
                        case FloatPrimitive leftF when right instanceof FloatPrimitive rightF ->
                                FloatPrimitive.of(dbop.applyAsDouble(leftF.getValue(), rightF.getValue()));
                        case IntPrimitive leftI when right instanceof IntPrimitive rightI ->
                                IntPrimitive.of(lbop.applyAsLong(leftI.getValue(), rightI.getValue()));
                        case CharPrimitive leftC when right instanceof CharPrimitive rightC ->
                                CharPrimitive.of((char) lbop.applyAsLong(leftC.getValue(), rightC.getValue()));
                        case null, default -> throw errorCreator.get();
                    };
                };

                Function<IntPredicate, BoolPrimitive> comparisonOp = (intop) -> {
                    NodePrimitive left = evaluateExpr(binOp.left());
                    NodePrimitive right = evaluateExpr(binOp.right());

                    //todo implicit casting later on
                    int comparison = switch (left) {
                        case FloatPrimitive leftF when right instanceof FloatPrimitive rightF ->
                                Double.compare(leftF.getValue(), rightF.getValue());
                        case IntPrimitive leftI when right instanceof IntPrimitive rightI ->
                                Long.compare(leftI.getValue(), rightI.getValue());
                        case CharPrimitive leftC when right instanceof CharPrimitive rightC ->
                                Character.compare(leftC.getValue(), rightC.getValue());
                        case BoolPrimitive leftC when right instanceof BoolPrimitive rightC ->
                                Boolean.compare(leftC.getValue(), rightC.getValue());
                        case null, default -> throw errorCreator.get();
                    };

                    return BoolPrimitive.of(intop.test(comparison));
                };

                yield switch (binOp.type()) {
                    case SUM -> mathematicalBinOp.apply(Double::sum, Long::sum);
                    case DIFFERENCE -> mathematicalBinOp.apply((d1, d2) -> d1 - d2, (l1, l2) -> l1 - l2);
                    case PRODUCT -> mathematicalBinOp.apply((d1, d2) -> d1 * d2, (l1, l2) -> l1 * l2);
                    case QUOTIENT -> mathematicalBinOp.apply((d1, d2) -> d1 / d2, (l1, l2) -> l1 / l2);
                    case REMAINDER -> mathematicalBinOp.apply((d1, d2) -> d1 % d2, (l1, l2) -> l1 % l2);
                    case EXPONENT -> mathematicalBinOp.apply(Math::pow, (l1, l2) -> (long) Math.pow(l1, l2));

                    case EQUAL -> comparisonOp.apply(i -> i == 0);
                    case DIFFERENT -> comparisonOp.apply(i -> i != 0);
                    case LESS -> comparisonOp.apply(i -> i < 0);
                    case GREATER -> comparisonOp.apply(i -> i > 0);
                    case LESS_EQ -> comparisonOp.apply(i -> i <= 0);
                    case GREATER_EQ -> comparisonOp.apply(i -> i >= 0);

                    case AND -> {
                        if ((evaluateExpr(binOp.left()) instanceof BoolPrimitive leftB)) {
                            if (leftB.getValue()) {
                                if (evaluateExpr(binOp.right()) instanceof BoolPrimitive rightB) {
                                    yield BoolPrimitive.of(rightB.getValue());
                                }
                            } else { //If first value is false, don't evaluate second expression
                                yield BoolPrimitive.of(false);
                            }
                        }
                        throw errorCreator.get();
                    }

                    case OR -> {
                        if ((evaluateExpr(binOp.left()) instanceof BoolPrimitive leftB)) {
                            if (!leftB.getValue()) {
                                if (evaluateExpr(binOp.right()) instanceof BoolPrimitive rightB) {
                                    yield BoolPrimitive.of(rightB.getValue());
                                }
                            } else { //If first value is true, don't evaluate second expression
                                yield BoolPrimitive.of(true);
                            }
                        }
                        throw errorCreator.get();
                    }
                    //case OR -> boolBiOp.apply(Boolean::logicalOr);
                    default ->
                            throw new ExpressionError("Don't know how we got here, unknown binary operator", errorTok);
                };
            }

            case FuncCallExpr fCall -> {
                //System.out.println("Calling function " + fCall.name + " within an expression");
                // Check that the function called is an actual function in the scope
                // This will be more complicated later on with imports etc.
                NodeFunction func = program.getFunction(fCall.name);

                // Check that the signature is correct
                if (fCall.argCount != func.args)
                    throw new ExpressionError("Incorrect number of arguments for function", fCall.token);

                if (func.returnType.type == TokenType.VOID)
                    throw new ExpressionError("Invalid return type, cannot have void return type here", fCall.token);

                Scope newScope;

                if (func.args == 0) {
                    newScope = Scope.fromFunction(func);
                } else {
                    Map<String, NodePrimitive> arguments = new HashMap<>();

                    //We have correct number of arguments, now we check if they are of the right type
                    for (int i = 0; i < func.args; i++) {
                        String desiredType = func.getSignature().get(i * 2).value;
                        NodePrimitive argValue = evaluateExpr(fCall.arguments.get(i));
                        String obtainedType = argValue.getTypeString();

                        if (!obtainedType.equals(desiredType)) {
                            throw new ExpressionError("Expected '" + desiredType + "', obtained '" + obtainedType + "'", fCall.token);
                        } else {
                            //Name of the argument, value of the argument
                            arguments.put(func.getSignature().get(i * 2 + 1).value, argValue);
                        }
                    }

                    newScope = Scope.fromFunction(func, arguments);
                }
                scopeStack.push(newScope);

                Optional<NodePrimitive> ret = Optional.empty();

                for (int j = 0; j < newScope.getStatements().size() && ret.isEmpty(); j++) {
                    ret = executeStatement(j);
                }

                scopeStack.pop();

                if (ret.isEmpty())
                    throw new ExpressionError("Did not return a value from function '" + fCall.name + "'", fCall.token);

                yield ret.get();
            }

            //This branch should only happen when we hit a new NodeExpr that hasn't been handled yet
            default -> IntPrimitive.of(0);
        };

        return (switch (context) {
            case NONE -> retVal;

            case BOOL -> {
                if (retVal instanceof BoolPrimitive) yield retVal;

                throw new ExpressionError("Expected bool value, not '" + retVal.getTypeString() + "'", retVal.getToken());
            }
            case INTEGER -> {
                if (retVal instanceof IntPrimitive) yield retVal;

                throw new ExpressionError("Expected int value, not '" + retVal.getTypeString() + "'", retVal.getToken());
            }
            case FLOAT -> {
                if (retVal instanceof FloatPrimitive) yield retVal;

                throw new ExpressionError("Expected float value, not '" + retVal.getTypeString() + "'", retVal.getToken());
            }
            case CHAR -> {
                if (retVal instanceof CharPrimitive) yield retVal;

                throw new ExpressionError("Expected char value, not '" + retVal.getTypeString() + "'", retVal.getToken());
            }
            //noinspection UnnecessaryDefault since it might be necessary in the future
            default ->
                    throw new ExpressionError("Don't know how we got here, found unknown evaluation context", retVal.getToken());
        }).copy(); //todo check later if this copy breaks things
    }


    /**
     * Currently only used for verbose messages, but might in future be more useful.
     */
    public Map<String, NodePrimitive> variables() {
        return scope().getVariables();
    }
}
