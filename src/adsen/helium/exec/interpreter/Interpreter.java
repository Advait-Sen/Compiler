package adsen.helium.exec.interpreter;

import adsen.helium.error.ParsingError;
import adsen.helium.parser.expr.FuncCallExpr;
import adsen.helium.parser.expr.NodeExpr;
import adsen.helium.parser.HeliumFunction;
import adsen.helium.parser.HeliumProgram;
import adsen.helium.parser.expr.NodeIdentifier;
import adsen.helium.parser.expr.operator.BinaryOperator;
import adsen.helium.parser.expr.operator.OperatorType;
import adsen.helium.parser.expr.operator.UnaryOperator;
import adsen.helium.parser.expr.primitives.BoolPrimitive;
import adsen.helium.parser.expr.primitives.CharPrimitive;
import adsen.helium.parser.expr.primitives.FloatPrimitive;
import adsen.helium.parser.expr.primitives.IntPrimitive;
import adsen.helium.parser.expr.primitives.NodePrimitive;
import adsen.helium.parser.statement.HeliumStatement;
import adsen.helium.parser.statement.atomic.AssignStatement;
import adsen.helium.parser.statement.atomic.BreakStatement;
import adsen.helium.parser.statement.atomic.ContinueStatement;
import adsen.helium.parser.statement.atomic.DeclareStatement;
import adsen.helium.parser.statement.atomic.ExitStatement;
import adsen.helium.parser.statement.atomic.FunctionCallStatement;
import adsen.helium.parser.statement.atomic.IncrementStatement;
import adsen.helium.parser.statement.atomic.ReturnStatement;
import adsen.helium.parser.statement.atomic.StaticDeclareStatement;
import adsen.helium.parser.statement.aggregate.ForStatement;
import adsen.helium.parser.statement.aggregate.IfStatement;
import adsen.helium.parser.statement.aggregate.ScopeStatement;
import adsen.helium.parser.statement.aggregate.WhileStatement;
import adsen.helium.exec.Context;
import adsen.helium.exec.Scope;
import adsen.helium.tokeniser.Token;

import adsen.helium.tokeniser.TokenType;
import java.util.ArrayList;
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

import static adsen.helium.exec.Context.*;
import static adsen.helium.parser.HeliumProgram.MAIN_FUNCTION;

/**
 * A class which will interpret Helium programming language, instead of compiling.
 * <p>
 * This is simply because interpreting is much easier than compiling,
 * and writing an interpreter is good practice for writing a compiler (I think)
 */
public class Interpreter {
    /**
     * The program to run
     */
    HeliumProgram program;

    /**
     * Stack used to keep track of scopes
     */
    public Stack<Scope> scopeStack;

    public static InterpreterScopeStack newScopeStack;

    public Interpreter(HeliumProgram program) {
        this.program = program;
    }

    /**
     * Executes a {@link HeliumProgram}
     */
    public NodePrimitive run() {
        if (program.lacksFunction(MAIN_FUNCTION))
            throw new RuntimeException("Program does not contain main function");

        HeliumFunction mainFunction = program.mainFunction();

        newScopeStack = new InterpreterScopeStack();

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
    Optional<NodePrimitive> executeStatement(HeliumStatement statement) {
        int pos = scope().getPos(); //Completely unused, not even sure if it's accurate, but eh it does no harm to keep it jic

        Optional<NodePrimitive> returnValue = Optional.empty();

        switch (statement) {
            //Todo decide whether this should be a hard exit from all execution, or just remove this
            //Cos rn it works like an unsafe (not type-checked) version of return
            case ExitStatement exit -> returnValue = Optional.of(evaluateExpr(exit.expr()));

            case DeclareStatement declare -> {
                NodeIdentifier identifier = declare.identifier();

                if (scope().hasVariable(identifier.asString())) {
                    //Copy of Java error message
                    throw new ParsingError("Variable '" + identifier.asString() + "' is already defined in the scope", identifier.token);
                }

                NodePrimitive value = evaluateExpr(declare.expr());

                if (declare instanceof StaticDeclareStatement staticDeclare) {
                    String requiredType = staticDeclare.valueType.value;
                    String providedType = value.getTypeString();

                    if (!requiredType.equals(providedType)) {
                        throw new ParsingError("Cannot assign '" + providedType + "' to '" + requiredType + "' type", identifier.token);
                    }
                }

                scope().setVariable(identifier.asString(), value);
            }
            case AssignStatement assign -> {
                String variableName = assign.identifier().asString();

                if (!scope().hasVariable(variableName)) {
                    throw new ParsingError("Unknown variable '" + variableName + "'", assign.identifier().token);
                }

                if (assign instanceof IncrementStatement inc) {
                    String providedType = scope().getVariable(variableName).getTypeString();
                    if (!providedType.equals(IntPrimitive.TYPE_STRING)) {
                        throw new ParsingError("Cannot increment '" + providedType + "', only '" + IntPrimitive.TYPE_STRING + "' type", assign.identifier().token);
                    }
                    IntPrimitive value = ((IntPrimitive) scope().getVariable(variableName));
                    scope().setVariable(variableName, value.setValue(value.getValue() + (inc.incrementor == OperatorType.INCREMENT ? 1 : -1)));
                    break;
                }

                NodePrimitive value = evaluateExpr(assign.expr());

                String requiredType = scope().getVariable(variableName).getTypeString();
                String providedType = value.getTypeString();

                if (!requiredType.equals(providedType)) {
                    throw new ParsingError("Cannot assign '" + providedType + "' to '" + requiredType + "' type", assign.identifier().token);
                }

                scope().setVariable(variableName, value);
            }
            case ScopeStatement scope -> { //todo test exit from within scopes
                Scope newScope;

                // If this scope statement came from a loop, then make this scope a loop
                // If this scope is a new loop, then it can't inherit that state from its parent
                if (scope.isLoop()) {
                    newScope = Scope.fromPreviousWithLoop(scope.name, scope(), scope.statements);
                } else {
                    newScope = Scope.fromPrevious(scope.name, scope(), scope.statements);
                }

                scopeStack.push(newScope);

                for (int j = 0; j < scope.statements.size() && returnValue.isEmpty(); j++) {
                    HeliumStatement scopeNodeStmt = scope().getStatement(j);
                    if (scopeNodeStmt instanceof ContinueStatement continueStmt) {
                        handleContinueStatement(continueStmt);
                    } else if (scopeNodeStmt instanceof BreakStatement breakStmt) {
                        handleBreakStatement(breakStmt);
                    } else { //Not gonna execute break and continue statements, since they do nothing and skip the rest of the statements in the scope
                        returnValue = executeStatement(j);
                    }

                    if (scope().isLoopContinued() || scope().isLoopBroken()) {
                        //Not executing the rest of the statements in this scope
                        //Calling it here, since continue or break might have been called (and not handled) in a child scope too
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
                    returnValue = executeStatement(ifStmt.thenStatement());
                } else if (ifStmt.hasElse()) {
                    returnValue = executeStatement(ifStmt.elseStatement());
                }
            }
            case WhileStatement whileStmt -> {
                while (evaluateExprBool(whileStmt.condition()).getValue() && returnValue.isEmpty()) {
                    returnValue = executeStatement(whileStmt.statement());

                    //Idk what to do with the return value of this method
                    scope().returnFromContinue();

                    if (scope().returnFromBreak()) {
                        break;
                    }
                }
            }
            case ForStatement forStmt -> {

                for (executeStatement(forStmt.getAssigner());
                     evaluateExprBool(forStmt.condition()).getValue() && returnValue.isEmpty();
                     executeStatement(forStmt.getIncrementer())) {

                    returnValue = executeStatement(forStmt.statement());

                    //Idk what to do with the return value of this method
                    scope().returnFromContinue();

                    if (scope().returnFromBreak()) {
                        break;
                    }
                }

                // Removing for loop variable
                // If scope already contained a variable with that name, then the statement won't have executed properly
                // So no need to worry about possibly removing an existing variable
                if (forStmt.getAssigner() instanceof DeclareStatement declare) {
                    scope().removeVariable(declare.identifier().asString());
                }
            }

            case FunctionCallStatement fCallStmt -> {

                Map<String, NodePrimitive> arguments = new HashMap<>();
                List<NodePrimitive> typeSignature = new ArrayList<>();

                for (int i = 0; i < fCallStmt.args.size(); i++) {
                    NodePrimitive argValue = evaluateExpr(fCallStmt.args.get(i));
                    typeSignature.add(argValue);
                }

                HeliumFunction func = program.getFunction(fCallStmt.name, typeSignature);

                for (int i = 0; i < typeSignature.size(); i++) {
                    arguments.put(func.getSignature().get(i * 2 + 1).value, typeSignature.get(i));
                }

                Scope newScope = Scope.fromFunction(func, arguments);

                scopeStack.push(newScope);
                ReturnStatement funcRet = null;
                for (int j = 0; j < newScope.getStatements().size() && returnValue.isEmpty(); j++) {
                    returnValue = executeStatement(j);
                    // This returns in case the return statement was empty, otherwise returnValue would be populated and we'd leave anyways
                    if (scope().getStatement(j) instanceof ReturnStatement) {
                        funcRet = (ReturnStatement) scope().getStatement(j);
                        break;
                    }
                }

                //If there was a return statement (and an error), the following errors will be generated there
                //Otherwise, place the error at the function itself
                Token errorToken = funcRet == null ? func.token : funcRet.token;
                //TODO this is where the distinction between an exit statement and return statement should be. But currently, no such distinction exists, beyond exit statements being plainly dysfunctional
                if (returnValue.isPresent()) {
                    NodePrimitive value = returnValue.get();

                    if (!value.getTypeString().equals(func.returnType.value))
                        throw new ParsingError("Expected '" + func.returnType.value + "' return type in function '" + func.name + "', got '" + value.getTypeString() + "' instead",
                                errorToken);

                    returnValue = Optional.empty(); //Discarding the return value, since it shouldn't matter
                } else {
                    //Did we return nothing from a function that was expecting something?
                    //The return value would have been discarded anyway, but it is still a type error

                    if (!func.returnType.type.equals(TokenType.VOID))
                        throw new ParsingError("Expected '" + func.returnType.value + "' return type in function '" + func.name + "', got '" + TokenType.VOID.name().toLowerCase() + "' instead",
                                errorToken
                        );
                }

                scopeStack.pop();
            }

            case ReturnStatement retStmt -> {
                Scope scope = scope();
                //System.out.println("Returning from " + scope.name);

                //If the return statement is empty, we don't expect a result
                if (retStmt.empty) {
                    if (!scope.getReturnType().equalsIgnoreCase(TokenType.VOID.name())) {
                        // We were expecting something from this function, not void
                        throw new ParsingError("Expected '" + scope.getReturnType() + "' return type from function '" + scope().name + "', got '" + TokenType.VOID.name().toLowerCase() + "' instead", retStmt.token);
                    }
                    returnValue = Optional.empty();
                    break;
                }

                NodePrimitive retValue = evaluateExpr(retStmt.expr());

                if (!retValue.getTypeString().equals(scope.getReturnType()))
                    throw new ParsingError("Expected '" + scope.getReturnType() + "' return type from function '" + scope.name + "', got '" + retValue.getTypeString() + "' instead", retStmt.token);

                returnValue = Optional.of(retValue);
            }

            case ContinueStatement continueStmt -> handleContinueStatement(continueStmt);
            case BreakStatement breakStmt -> handleBreakStatement(breakStmt);

            default -> //Might throw an error here at some point later on
                    System.out.println("Reached an unhandled statement type: " + statement.typeString());
        }

        return returnValue;
    }

    private Scope scope() {
        return scopeStack.peek();
    }

    private void handleContinueStatement(ContinueStatement continueStmt) {
        if (scope().isLoop()) {
            if (!scope().continueLoop())
                throw new ParsingError("Unexpected 'continue', loop might already have been continued or broken", continueStmt.token);

        } else throw new ParsingError("Unexpected 'continue' outside of loop", continueStmt.token);
    }

    private void handleBreakStatement(BreakStatement breakStmt) {
        //This will eventually handle switch statements ig, though if it's too much of a mess I'll just use a different keyword or smth
        if (scope().isLoop()) {
            if (!scope().breakLoop())
                throw new ParsingError("Unexpected 'break', loop might already have been continued or broken", breakStmt.token);

        } else throw new ParsingError("Unexpected 'break' outside of loop", breakStmt.token);
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

        NodePrimitive retVal = switch (expr) {
            case NodePrimitive nodePrimitive -> nodePrimitive;

            case NodeIdentifier ident -> {
                if (!scope().hasVariable(ident.asString()))
                    throw new ParsingError("Unknown variable '" + ident.asString() + "'", ident.token);

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
                            throw new ParsingError("Expected numeric value, not 'bool'", errorTok);
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
                                    throw new ParsingError("Expected numeric value, not 'bool'", errorTok);
                        };
                    }
                    default ->
                            throw new ParsingError("Don't know how we got here, unknown unary operator", errorTok);
                };

            }
            case BinaryOperator binOp -> {

                //Placeholder made up token until I figure out better error messages
                Token errorTok = new Token(binOp.asString(), binOp.type().type);

                //Don't keep the error as a variable since it causes expressions to be evaluated that we might not want evaluated
                //Although this way might cause some stuff to get evaluated twice, which is also bad
                Supplier<ParsingError> errorCreator = () -> new ParsingError("Undefined '%s' operator for '%s' and '%s'".formatted(binOp.type().value, evaluateExpr(binOp.left()).getTypeString(), evaluateExpr(binOp.right()).getTypeString()), errorTok);


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
                            throw new ParsingError("Don't know how we got here, unknown binary operator", errorTok);
                };
            }

            case FuncCallExpr fCall -> {

                Map<String, NodePrimitive> arguments = new HashMap<>();
                List<NodePrimitive> typeSignature = new ArrayList<>();

                for (int i = 0; i < fCall.getArgCount(); i++) {
                    NodePrimitive argValue = evaluateExpr(fCall.arguments.get(i));
                    typeSignature.add(argValue);
                }

                HeliumFunction func = program.getFunction(fCall.token, typeSignature);

                if (func.returnType.type == TokenType.VOID)
                    throw new ParsingError("Tried to use void function in an expression", fCall.token);

                for (int i = 0; i < typeSignature.size(); i++) {
                    arguments.put(func.getSignature().get(i * 2 + 1).value, typeSignature.get(i));
                }

                Scope newScope = Scope.fromFunction(func, arguments);

                scopeStack.push(newScope);

                Optional<NodePrimitive> returnValue = Optional.empty();

                for (int j = 0; j < newScope.getStatements().size() && returnValue.isEmpty(); j++) {
                    returnValue = executeStatement(j);
                }

                scopeStack.pop();

                if (returnValue.isEmpty())
                    throw new ParsingError("Did not return a value from function '" + fCall.name + "'", fCall.token);

                yield returnValue.get();
            }

            //This branch should only happen when we hit a new NodeExpr that hasn't been handled yet
            default -> IntPrimitive.of(0);
        };

        return (switch (context) {
            case NONE -> retVal;

            case BOOL -> {
                if (retVal instanceof BoolPrimitive) yield retVal;

                throw new ParsingError("Expected bool value, not '" + retVal.getTypeString() + "'", retVal.getToken());
            }
            case INTEGER -> {
                if (retVal instanceof IntPrimitive) yield retVal;

                throw new ParsingError("Expected int value, not '" + retVal.getTypeString() + "'", retVal.getToken());
            }
            case FLOAT -> {
                if (retVal instanceof FloatPrimitive) yield retVal;

                throw new ParsingError("Expected float value, not '" + retVal.getTypeString() + "'", retVal.getToken());
            }
            case CHAR -> {
                if (retVal instanceof CharPrimitive) yield retVal;

                throw new ParsingError("Expected char value, not '" + retVal.getTypeString() + "'", retVal.getToken());
            }
            //noinspection UnnecessaryDefault since it might be necessary in the future
            default ->
                    throw new ParsingError("Don't know how we got here, found unknown evaluation context", retVal.getToken());
        }).copy(); //todo check later if this copy breaks things with classes
    }


    /**
     * Currently only used for verbose messages, but might in future be more useful.
     */
    public Map<String, NodePrimitive> variables() {
        return scope().getVariables();
    }
}
