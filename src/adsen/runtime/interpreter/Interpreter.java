package adsen.runtime.interpreter;

import adsen.error.ExpressionError;
import adsen.parser.node.NodeExpr;
import adsen.parser.node.NodeProgram;
import adsen.parser.node.identifier.NodeIdentifier;
import adsen.parser.node.operator.BinaryOperator;
import adsen.parser.node.operator.UnaryOperator;
import adsen.parser.node.primitives.BoolPrimitive;
import adsen.parser.node.primitives.CharPrimitive;
import adsen.parser.node.primitives.FloatPrimitive;
import adsen.parser.node.primitives.IntPrimitive;
import adsen.parser.node.primitives.NodePrimitive;
import adsen.parser.node.statement.AssignStatement;
import adsen.parser.node.statement.DeclareStatement;
import adsen.parser.node.statement.ExitStatement;
import adsen.parser.node.statement.IfStatement;
import adsen.parser.node.statement.NodeStatement;
import adsen.parser.node.statement.ScopeStatement;
import adsen.parser.node.statement.StaticDeclareStatement;
import adsen.runtime.Scope;
import adsen.tokeniser.Token;

import java.util.Map;
import java.util.Optional;
import java.util.Stack;
import java.util.function.BiFunction;
import java.util.function.DoubleBinaryOperator;
import java.util.function.Function;
import java.util.function.IntPredicate;
import java.util.function.LongBinaryOperator;

/**
 * A class which will interpret Helium programming language, instead of compiling.
 * <p>
 * This is simply because interpreting is much easier than compiling,
 * and writing an interpreter is good practice for writing a compiler (I think)
 */
public class Interpreter {

    /**
     * The program from which we are generating assembly
     */
    public final NodeProgram program;

    /**
     * Stack used to keep track of scopes
     */
    public Stack<Scope> scopeStack;

    public Interpreter(NodeProgram program) {
        this.program = program;
    }

    public NodePrimitive run() throws ExpressionError {
        scopeStack = new Stack<>();

        scopeStack.push(Scope.empty("main", program.statements)); //this is gonna change when I implement main function

        Optional<NodePrimitive> retVal = Optional.empty();

        for (int i = 0; i < scopeStack.getFirst().getStatements().size() && retVal.isEmpty(); i++) {
            retVal = executeStatement(i);
        }

        if (scopeStack.size() > 1)
            throw new RuntimeException("Did not pop scopes correctly");

        return retVal.orElseGet(() -> IntPrimitive.of(0));
    }

    /**
     * Executes the next statement in the scope
     */
    Optional<NodePrimitive> executeStatement(int i) {
        return executeStatement(scopeStack.peek().getStatement(i));
    }

    /**
     * Executes a generic statement without scope context
     */
    Optional<NodePrimitive> executeStatement(NodeStatement statement) {
        int pos = scopeStack.peek().getPos();

        Optional<NodePrimitive> ret = Optional.empty();

        if (statement instanceof ExitStatement exit) {

            ret = Optional.ofNullable(evaluateExpr(exit.expr()));

        } else if (statement instanceof DeclareStatement declare) {

            NodeIdentifier identifier = declare.identifier();

            if (scopeStack.peek().hasVariable(identifier.asString())) {
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

            scopeStack.peek().setVariable(identifier.asString(), value);

        } else if (statement instanceof AssignStatement assign) {
            String variableName = assign.identifier().asString();

            if (!scopeStack.peek().hasVariable(variableName)) {
                throw new ExpressionError("Unknown variable '" + variableName + "'", assign.identifier().token);
            }

            NodePrimitive value = evaluateExpr(assign.expr());

            String requiredType = scopeStack.peek().getVariable(variableName).getTypeString();
            String providedType = value.getTypeString();

            if (!requiredType.equals(providedType)) {
                throw new ExpressionError("Cannot assign '" + providedType + "' to '" + requiredType + "' type", assign.identifier().token);
            }

            scopeStack.peek().setVariable(variableName, value);

        } else if (statement instanceof ScopeStatement scope) {
            Scope newScope = Scope.filled(scope.name, scopeStack.peek(), scope.statements);
            scopeStack.push(newScope);

            for (int j = 0; j < scope.statements.size() && ret.isEmpty(); j++) {
                ret = executeStatement(j);
            }
            /* For printing out scopes as debug feature
            for (Scope stackScope : scopeStack) {
                System.out.println("Printing scope " + stackScope.name);
                stackScope.getVariables().forEach((s, np) -> {
                    System.out.println("    " + s + " (" + np.getTypeString() + "): " + np.asString());
                });
                System.out.println("End of scope\n");
            }
            */
            newScope = scopeStack.pop();

            // If we modified a variable that was obtained from earlier scope, then remember the update
            // This is possibly not the most efficient way of doing things
            // But this might be the most that can be done with an interpreted language
            newScope.getVariables().forEach((s, np) -> {
                if (scopeStack.peek().hasVariable(s)) {
                    scopeStack.peek().setVariable(s, np);
                }
            });

        } else if (statement instanceof IfStatement ifStmt) {
            NodePrimitive shouldRun = evaluateExpr(ifStmt.getCondition());
            if (!(shouldRun instanceof BoolPrimitive run))
                throw new ExpressionError("Must have boolean condition for if statement", ifStmt.token);

            if (run.getValue()) {
                ret = executeStatement(ifStmt.thenStatement());
            } else if (ifStmt.hasElse()) {
                ret = executeStatement(ifStmt.elseStatement());
            }
        }

        return ret;
    }

    private NodePrimitive evaluateExpr(NodeExpr expr) {
        if (expr instanceof IntPrimitive intP) { //So that hex literals get printed in base 10. Might change once I add proper expression evaluation
            return IntPrimitive.of(intP.getValue());
        }

        if (expr instanceof NodePrimitive) {
            return (NodePrimitive) expr;
        }

        if (expr instanceof NodeIdentifier ident) {
            if (!scopeStack.peek().hasVariable(ident.asString()))
                throw new ExpressionError("Unknown variable '" + ident.asString() + "'", ident.token);

            return scopeStack.peek().getVariable(ident.asString());
        }

        if (expr instanceof UnaryOperator unOp) {
            NodePrimitive operand = evaluateExpr(unOp.operand());
            switch (unOp.type()) {
                case NEGATE -> {
                    if (!(operand instanceof BoolPrimitive bool)) //Exact copy of Java error message
                        throw new ExpressionError("Operator '!' cannot be applied to '" + operand.getTypeString() + "'", operand.getToken());
                    bool.setValue(!bool.getValue()); //This might source of problems down the line, would want to make new BoolPrimitive instead
                    return bool;
                }
            }
        }

        if (expr instanceof BinaryOperator binOp) {
            NodePrimitive left = evaluateExpr(binOp.left());
            NodePrimitive right = evaluateExpr(binOp.right());

            //Placeholder made up token until I figure out better error messages
            Token errorTok = new Token(binOp.asString(), binOp.type().type);

            BiFunction<DoubleBinaryOperator, LongBinaryOperator, NodePrimitive> mathematicalBinOp = (dbop, lbop) ->
                    //todo implicit casting later on
                    switch (left) {
                        case FloatPrimitive leftF when right instanceof FloatPrimitive rightF ->
                                FloatPrimitive.of(dbop.applyAsDouble(leftF.getValue(), rightF.getValue()));
                        case IntPrimitive leftI when right instanceof IntPrimitive rightI ->
                                IntPrimitive.of(lbop.applyAsLong(leftI.getValue(), rightI.getValue()));
                        case CharPrimitive leftC when right instanceof CharPrimitive rightC ->
                                CharPrimitive.of((char) lbop.applyAsLong(leftC.getValue(), rightC.getValue()));
                        case null, default ->
                                throw new ExpressionError("Undefined '%s' operator for '%s' and '%s'".formatted(binOp.type().value, left.getTypeString(), right.getTypeString()), errorTok);
                    };

            Function<IntPredicate, BoolPrimitive> comparisonOp = (intop) -> {
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
                    case null, default ->
                            throw new ExpressionError("Undefined '%s' operator for '%s' and '%s'".formatted(binOp.type().value, left.getTypeString(), right.getTypeString()), errorTok);
                };

                return BoolPrimitive.of(intop.test(comparison));
            };

            Function<java.util.function.BinaryOperator<Boolean>, BoolPrimitive> boolBiOp = (bop) -> {
                if (left instanceof BoolPrimitive leftB && right instanceof BoolPrimitive rightB) {
                    return BoolPrimitive.of(bop.apply(leftB.getValue(), rightB.getValue()));
                } else
                    throw new ExpressionError("Undefined '%s' operator for '%s' and '%s'".formatted(binOp.type().value, left.getTypeString(), right.getTypeString()), errorTok);
            };

            return switch (binOp.type()) { //todo simplify with return switch() etc...
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

                case AND -> boolBiOp.apply(Boolean::logicalAnd);
                case OR -> boolBiOp.apply(Boolean::logicalOr);
                default -> throw new ExpressionError("Don't know how we got here", errorTok);
            };
        }

        return IntPrimitive.of(0);
    }


    /**
     * Currently only used for verbose messages, but might in future be more useful.
     */
    public Map<String, NodePrimitive> variables() {
        return scopeStack.peek().getVariables();
    }
}
