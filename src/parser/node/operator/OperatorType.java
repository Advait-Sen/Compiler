package parser.node.operator;

import tokeniser.Keywords;
import tokeniser.TokenType;

import java.util.HashMap;
import java.util.Map;

import static parser.node.operator.Operator.operatorType;

public enum OperatorType {
    SUM("+", 10),
    DIFFERENCE("-", 10),
    ASSIGN("=", 1, false),

    NEGATE("!", 15, false, 1),
    ;

    OperatorType(String value, int precedence) {
        this.value = value;
        this.precedence = precedence;
        this.isLeftAssoc = true;
        this.args = 2;
    }

    OperatorType(String value, int precedence, boolean leftAssoc) {
        this.value = value;
        this.precedence = precedence;
        this.isLeftAssoc = leftAssoc;
        this.args = 2;
    }

    OperatorType(String value, int precedence, boolean leftAssoc, int args) {
        this.value = value;
        this.precedence = precedence;
        this.isLeftAssoc = leftAssoc;
        this.args = args;
    }

    final String value;
    final int precedence;
    final boolean isLeftAssoc;
    final int args;


    static {
        for (OperatorType op : OperatorType.values()) {
            operatorType.put(op.value, op);
            Keywords.operatorTokens.put(op.value, switch (op.args){
                case 1-> TokenType.UNARY_OPERATOR;
                case 2-> TokenType.BINARY_OPERATOR;
                default -> throw new RuntimeException("How did we get here? Had illegal number of arguments for operator");
            });
        }

    }

}
