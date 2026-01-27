package adsen.helium.parser.expr.operator;

import adsen.helium.parser.expr.NodeExpr;

import java.util.HashMap;
import java.util.Map;

public interface Operator extends NodeExpr {
    OperatorType type();

    Map<String, OperatorType> operatorType = new HashMap<>();
}
