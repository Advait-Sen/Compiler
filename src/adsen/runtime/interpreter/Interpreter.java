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
import adsen.parser.node.statement.NodeStatement;
import adsen.parser.node.statement.StaticDeclareStatement;
import adsen.runtime.Scope;
import adsen.tokeniser.Token;

import java.util.Map;
import java.util.Stack;
import java.util.function.BiFunction;
import java.util.function.DoubleBinaryOperator;
import java.util.function.LongBinaryOperator;

/**
 * A class which will interpret Helium programming language, instead of compiling
 * This is simply because interpreting it is much easier than compiling,
 * and writing an runtime.interpreter is good practice for writing a compiler (I think)
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

    public Scope currentScope;

    public Interpreter(NodeProgram program) {
        this.program = program;
    }

    public NodePrimitive run() throws ExpressionError {
        scopeStack = new Stack<>();

        currentScope = Scope.empty("main"); //this is gonna change when I implement main function

        scopeStack.push(currentScope);

        for (int i = 0; i < program.statements.size(); i++) {
            NodeStatement statement = program.statements.get(i);

            if (statement instanceof ExitStatement exit) {
                return evaluateExpr(exit.expr());
            } else if (statement instanceof DeclareStatement declare) {

                NodeIdentifier identifier = declare.identifier();

                if (currentScope.hasVariable(identifier.asString())) {
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

                currentScope.setVariable(identifier.asString(), value);

            } else if (statement instanceof AssignStatement assign) {
                String variableName = assign.identifier().asString();

                if (!currentScope.hasVariable(variableName)) {
                    throw new ExpressionError("Unknown variable '" + variableName + "'", assign.identifier().token);
                }

                NodePrimitive value = evaluateExpr(assign.expr());

                String requiredType = currentScope.getVariable(variableName).getTypeString();
                String providedType = value.getTypeString();

                if (!requiredType.equals(providedType)) {
                    throw new ExpressionError("Cannot assign '" + providedType + "' to '" + requiredType + "' type", assign.identifier().token);
                }

                currentScope.setVariable(variableName, value);
            }
        }

        return IntPrimitive.of(0);
    }

    private NodePrimitive evaluateExpr(NodeExpr expr) {
        if (expr instanceof IntPrimitive intP) { //So that hex literals get printed in base 10. Might change once I add proper expression evaluation
            return IntPrimitive.of(intP.getValue());
        }

        if (expr instanceof NodePrimitive) {
            return (NodePrimitive) expr;
        }

        if (expr instanceof NodeIdentifier ident) {
            if (!currentScope.hasVariable(ident.asString()))
                throw new ExpressionError("Unknown variable '" + ident.asString() + "'", ident.token);

            return currentScope.getVariable(ident.asString());
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

            BiFunction<DoubleBinaryOperator, LongBinaryOperator, NodePrimitive> mathematicalBinOp = (dbop, lbop) -> {
                //Placeholder made up token until I figure out better error messages
                Token errorTok = new Token(binOp.asString(), binOp.type().type);

                //todo implicit casting later on

                if (left instanceof FloatPrimitive leftF && right instanceof FloatPrimitive rightF) {

                    return FloatPrimitive.of(dbop.applyAsDouble(leftF.getValue(), rightF.getValue()));

                } else if (left instanceof IntPrimitive leftI && right instanceof IntPrimitive rightI) {

                    return IntPrimitive.of(lbop.applyAsLong(leftI.getValue(), rightI.getValue()));

                } else if (left instanceof CharPrimitive leftC && right instanceof CharPrimitive rightC) {

                    return CharPrimitive.of((char) lbop.applyAsLong(leftC.getValue(), rightC.getValue()));
                }

                throw new ExpressionError("Undefined '%s' operator for '%s' and '%s'".formatted(binOp.type().value, left.getTypeString(), right.getTypeString()), errorTok);
            };

            switch (binOp.type()) {
                case SUM -> {
                    return mathematicalBinOp.apply(Double::sum, Long::sum);
                }
                case DIFFERENCE -> {
                    return mathematicalBinOp.apply((d1, d2) -> d1 - d2, (l1, l2) -> l1 - l2);
                }
            }
        }

        return IntPrimitive.of(0);
    }


    /**
     * Currently only used for verbose messages, but might in future be more useful.
     */
    public Map<String, NodePrimitive> variables() {
        return currentScope.getVariables();
    }
}
