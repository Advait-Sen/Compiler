package adsen.parser.node.operator;

import adsen.tokeniser.Keywords;
import adsen.tokeniser.TokenType;

import static adsen.parser.node.operator.Operator.operatorType;
import static adsen.tokeniser.TokenType.*;

public enum OperatorType {
    SUM("+", 10, BINARY_OPERATOR),
    DIFFERENCE("-", 10, BINARY_OPERATOR),
    ASSIGN("=", DECLARATION_OPERATION),

    NEGATE("!", 15, UNARY_OPERATOR),
    ;

    // Constructor for assignment, since we treat that differently in tokeniser itself
    OperatorType(String value, TokenType type) {
        this.value = value;
        this.precedence = 0;
        this.isLeftAssoc = false;
        this.type = type;
    }

    OperatorType(String value, int precedence, TokenType type) {
        this.value = value;
        this.precedence = precedence;
        this.isLeftAssoc = true;
        this.type = type;
    }

    public final String value;
    public final int precedence;
    public final boolean isLeftAssoc;
    public final TokenType type;


    static {
        for (OperatorType op : OperatorType.values()) {
            operatorType.put(op.value, op);
            Keywords.operatorTokens.put(op.value, op.type);
        }
    }

    public static void noop() {}

}
