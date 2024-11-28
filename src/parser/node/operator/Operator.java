package parser.node.operator;

import parser.node.NodeExpr;

import java.util.HashMap;
import java.util.Map;

public interface Operator extends NodeExpr {
    OperatorType type();

    @Override
    default boolean isRoot() {
        return false;
    }

    Map<String, OperatorType> operatorType = new HashMap<>();
}
