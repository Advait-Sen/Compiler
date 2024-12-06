package adsen.parser.node.operator;

import adsen.parser.node.NodeExpr;

import java.util.HashMap;
import java.util.Map;

public interface Operator extends NodeExpr {
    OperatorType type();

    int operators();

    Map<String, OperatorType> operatorType = new HashMap<>();
}
