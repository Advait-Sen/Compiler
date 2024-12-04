package adsen.parser.node.operator;

import adsen.tokeniser.Keywords;
import adsen.tokeniser.TokenType;

import static adsen.parser.node.operator.Operator.operatorType;
import static adsen.tokeniser.TokenType.*;

public enum OperatorType {
    // Maths operators
    EXPONENT("**", 12, BINARY_OPERATOR),
    PRODUCT("*", 11, BINARY_OPERATOR),
    QUOTIENT("/", 11, BINARY_OPERATOR),
    REMAINDER("%", 11, BINARY_OPERATOR),
    SUM("+", 10, BINARY_OPERATOR),
    DIFFERENCE("-", 10, BINARY_OPERATOR),

    // Assignment operators
    ASSIGN("=", DECLARATION_OPERATION),

    // Boolean operators
    NEGATE("!", 15, UNARY_OPERATOR, false),
    ;

    // Constructor for assignment, since we treat that differently in tokeniser itself
    OperatorType(String value, TokenType type) {
        this.value = value;
        this.precedence = 0;
        this.leftToRight = false;
        this.type = type;
    }

    OperatorType(String value, int precedence, TokenType type) {
        this.value = value;
        this.precedence = precedence;
        this.leftToRight = true;
        this.type = type;
    }

    OperatorType(String value, int precedence, TokenType type, boolean leftToRight) {
        this.value = value;
        this.precedence = precedence;
        this.leftToRight = leftToRight;
        this.type = type;
    }

    public final String value;
    public final int precedence;
    public final boolean leftToRight;
    public final TokenType type;


    static {
        for (OperatorType op : OperatorType.values()) {
            operatorType.put(op.value, op);
            Keywords.operatorTokens.put(op.value, op.type);
        }
    }

    public static void noop() {}

}
