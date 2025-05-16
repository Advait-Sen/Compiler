package adsen.parser.node.expr.operator;

import adsen.tokeniser.Keywords;
import adsen.tokeniser.TokenType;

import static adsen.parser.node.expr.operator.Operator.operatorType;
import static adsen.tokeniser.TokenType.*;

public enum OperatorType {
    //Some of the precedences are taken from here: https://pythongeeks.org/python-operator-precedence/

    // Maths operators
    INCREMENT("++", 15, UNARY_OPERATOR),
    DECREMENT("--", 15, UNARY_OPERATOR),
    EXPONENT("**", 12, BINARY_OPERATOR, false),
    PRODUCT("*", 11, BINARY_OPERATOR),
    QUOTIENT("/", 11, BINARY_OPERATOR),
    REMAINDER("%", 11, BINARY_OPERATOR),
    SUM("+", 10, BINARY_OPERATOR),
    DIFFERENCE("-", 10, BINARY_OPERATOR),
    //This way of handling unary minus and plus is kinda stolen from scarpet (https://github.com/gnembon/scarpet)
    NEGATIVE("u-", 14, UNARY_OPERATOR),
    POSITIVE("u+", 14, UNARY_OPERATOR),


    // Assignment operators
    ASSIGN("=", DECLARATION_OPERATION),

    // Boolean operators
    NOT("!", 15, UNARY_OPERATOR),
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
        this(value, 0, type);
    }

    OperatorType(String value, int precedence, TokenType type) {
        this(value, precedence, type, precedence != 0 && type == BINARY_OPERATOR);
    }

    OperatorType(String value, int precedence, TokenType type, boolean leftToRight) {
        this(value, precedence, type, leftToRight, type == UNARY_OPERATOR ? 1 : 2);
    }

    OperatorType(String value, int precedence, TokenType type, boolean leftToRight, int args) {
        this.value = value;
        this.precedence = precedence;
        this.leftToRight = leftToRight;
        this.type = type;
        this.args = args;
    }

    public final String value;
    public final int precedence;
    public final boolean leftToRight;
    public final TokenType type;
    public final int args;


    static {
        for (OperatorType op : OperatorType.values()) {
            operatorType.put(op.value, op);
            Keywords.operatorTokens.put(op.value, op.type);
        }
    }

    public static void noop() {
    }

}
