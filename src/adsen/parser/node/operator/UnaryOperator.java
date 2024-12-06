package adsen.parser.node.operator;

import adsen.parser.node.NodeExpr;
import adsen.tokeniser.Token;

public class UnaryOperator implements Operator {
    OperatorType operation;

    NodeExpr operand;

    public UnaryOperator(OperatorType operation, NodeExpr operand) {
        this.operation = operation;
        this.operand = operand;
    }

    public UnaryOperator(Token operator, NodeExpr operand) {
        this(Operator.operatorType.get(operator.value), operand);
    }

    public NodeExpr operand() {
        return operand;
    }

    @Override
    public OperatorType type() {
        return operation;
    }

    @Override
    public int operators() {
        return 1;
    }

    @Override
    public String asString() {
        return '(' + String.join(" ", operation.value, operand.asString()) + ')';
    }
}
