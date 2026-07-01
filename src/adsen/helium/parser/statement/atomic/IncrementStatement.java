package adsen.helium.parser.statement.atomic;

import adsen.helium.error.ParsingError;
import adsen.helium.parser.expr.NodeIdentifier;
import adsen.helium.parser.expr.operator.Operator;
import adsen.helium.parser.expr.operator.OperatorType;
import adsen.helium.tokeniser.Token;

/**
 * ++i, i++, --i, i--
 */
public class IncrementStatement extends AssignStatement {

    /**
     * Whether or not the incrementation is before or after the value itself
     */
    public final boolean isPre;
    public final OperatorType incrementor;

    public IncrementStatement(NodeIdentifier identifier, Token declarer, boolean before) {
        super(identifier, declarer, null);
        this.isPre = before;
        incrementor = Operator.operatorType.get(declarer.value);

        if (incrementor != OperatorType.INCREMENT && incrementor != OperatorType.DECREMENT) {
            throw new ParsingError("Must increment with ++ or decrement with --", declarer);
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
