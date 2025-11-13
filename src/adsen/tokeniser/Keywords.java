package adsen.tokeniser;

import adsen.parser.node.expr.operator.OperatorType;
import adsen.parser.node.expr.primitives.BoolPrimitive;
import adsen.parser.node.expr.primitives.CharPrimitive;
import adsen.parser.node.expr.primitives.FloatPrimitive;
import adsen.parser.node.expr.primitives.IntPrimitive;

import java.util.HashMap;
import java.util.Map;

import static adsen.tokeniser.TokenType.BOOL_LITERAL;
import static adsen.tokeniser.TokenType.ELSE;
import static adsen.tokeniser.TokenType.EXIT;
import static adsen.tokeniser.TokenType.FOR;
import static adsen.tokeniser.TokenType.IF;
import static adsen.tokeniser.TokenType.LET;
import static adsen.tokeniser.TokenType.PRIMITIVE_TYPE;
import static adsen.tokeniser.TokenType.RETURN;
import static adsen.tokeniser.TokenType.VOID;
import static adsen.tokeniser.TokenType.WHILE;

public class Keywords {
    public static Map<String, TokenType> tokeniserKeywords = new HashMap<>() {{
        put("true", BOOL_LITERAL);
        put("false", BOOL_LITERAL);

        //Types
        put(IntPrimitive.TYPE_STRING, PRIMITIVE_TYPE);
        put(FloatPrimitive.TYPE_STRING, PRIMITIVE_TYPE);
        put(BoolPrimitive.TYPE_STRING, PRIMITIVE_TYPE);
        put(CharPrimitive.TYPE_STRING, PRIMITIVE_TYPE);
        put("void", VOID);


        //Keywords
        put("let", LET);
        put("exit", EXIT);
        put("if", IF);
        put("else", ELSE);
        put("while", WHILE);
        put("for", FOR);
        put("return", RETURN);
    }};

    public static Map<String, TokenType> operatorTokens = new HashMap<>();

    static {
        OperatorType.noop();
    }
}
