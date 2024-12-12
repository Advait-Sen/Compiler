package adsen.parser.node.expr.operator;

import adsen.parser.node.expr.NodeExpr;

import java.util.HashMap;
import java.util.Map;

public interface Operator extends NodeExpr {
    OperatorType type();

    Map<String, OperatorType> operatorType = new HashMap<>();
}
