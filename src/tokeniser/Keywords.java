package tokeniser;

import parser.node.operator.OperatorType;
import parser.node.primitives.BoolPrimitive;
import parser.node.primitives.CharPrimitive;
import parser.node.primitives.FloatPrimitive;
import parser.node.primitives.IntPrimitive;

import java.util.HashMap;
import java.util.Map;

import static tokeniser.TokenType.BOOL_LITERAL;
import static tokeniser.TokenType.ELSE;
import static tokeniser.TokenType.EXIT;
import static tokeniser.TokenType.IF;
import static tokeniser.TokenType.LET;
import static tokeniser.TokenType.PRIMITIVE_TYPE;

public class Keywords {
    public static Map<String, TokenType> tokeniserKeywords = new HashMap<>() {{
        put("true", BOOL_LITERAL);
        put("false", BOOL_LITERAL);

        //Types
        put(IntPrimitive.TYPE_STRING, PRIMITIVE_TYPE);
        put(FloatPrimitive.TYPE_STRING, PRIMITIVE_TYPE);
        put(BoolPrimitive.TYPE_STRING, PRIMITIVE_TYPE);
        put(CharPrimitive.TYPE_STRING, PRIMITIVE_TYPE);


        //Keywords
        put("let", LET);
        put("exit", EXIT);
        put("if", IF);
        put("else", ELSE);
    }};

    public static Map<String, TokenType> operatorTokens = new HashMap<>();

    static {
        OperatorType.noop();
    }
}
