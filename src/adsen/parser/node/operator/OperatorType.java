package adsen.parser.node.operator;

import adsen.tokeniser.Keywords;
import adsen.tokeniser.TokenType;

import static adsen.parser.node.operator.Operator.operatorType;
import static adsen.tokeniser.TokenType.*;

public enum OperatorType {
    //Some of the precedences are taken from here: https://pythongeeks.org/python-operator-precedence/

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
    EQUAL("==", 4, BINARY_OPERATOR),
    DIFFERENT("!=", 4, BINARY_OPERATOR),
    GREATER(">", 4, BINARY_OPERATOR),
    LESS("<", 4, BINARY_OPERATOR),
    GREATER_EQ(">=", 4, BINARY_OPERATOR),
    LESS_EQ("<=", 4, BINARY_OPERATOR),
    AND("&&", 2, BINARY_OPERATOR),
    OR("||", 2, BINARY_OPERATOR),
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
