package adsen.parser.statement;

import adsen.error.ExpressionError;
import adsen.parser.node.expr.NodeIdentifier;
import adsen.parser.node.expr.operator.Operator;
import adsen.parser.node.expr.operator.OperatorType;
import adsen.tokeniser.Token;

/**
 * ++i, i++, --i, i--
 */
public class IncrementStatement extends AssignStatement {

    /**
     * Whether or not the incrementation is before or after the value itself
     */
    public final boolean isPre;
    public final OperatorType incrementor;

    public IncrementStatement(NodeIdentifier identifier, Token declarer, boolean pre) {
        super(identifier, declarer, null);
        this.isPre = pre;
        incrementor = Operator.operatorType.get(declarer.value);

        if (incrementor != OperatorType.INCREMENT && incrementor != OperatorType.DECREMENT) {
            throw new ExpressionError("Must increment with ++ or decrement with --", declarer);
        }
    }


    @Override
    public String asString() {
        return isPre ? declarer.value + identifier.asString() : identifier.asString() + declarer.value;
    }

    @Override
    public String typeString() {
        return "incrementation";
    }
}
