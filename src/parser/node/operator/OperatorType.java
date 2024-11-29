package parser.node.operator;

import tokeniser.Keywords;
import tokeniser.TokenType;

import static parser.node.operator.Operator.operatorType;

public enum OperatorType {
    SUM("+", 10),
    DIFFERENCE("-", 10),
    ASSIGN("="),

    NEGATE("!", 15, false, 1),
    ;

    // Constructor for assignment, since we treat that differently in tokeniser itself
    OperatorType(String value){
        this.value = value;
        this.precedence = 0;
        this.isLeftAssoc = false;
        this.args = -1;
    }

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
                case -1-> TokenType.ASSIGNMENT_OPERATOR;
                case 1-> TokenType.UNARY_OPERATOR;
                case 2-> TokenType.BINARY_OPERATOR;
                default -> throw new RuntimeException("How did we get here? Had illegal number of arguments for operator");
            });
        }

    }

    public static void noop(){}

}
