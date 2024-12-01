package adsen.parser.node.operator;

import adsen.parser.node.NodeExpr;
import adsen.tokeniser.Token;

public class BinaryOperator implements Operator {
    OperatorType operation;

    NodeExpr left;
    NodeExpr right;

    public BinaryOperator(NodeExpr left, OperatorType operation, NodeExpr right) {
        this.operation = operation;
        this.left = left;
        this.right = right;
    }

    public BinaryOperator(NodeExpr left, Token operator, NodeExpr right) {
        this(left, Operator.operatorType.get(operator.value), right);
    }

    @Override
    public OperatorType type() {
        return operation;
    }

    @Override
    public int precedence() {
        return operation.precedence;
    }

    @Override
    public String asString() {
        return '(' + String.join(" ", left.asString(), operation.value, right.asString()) + ')';
    }
}
