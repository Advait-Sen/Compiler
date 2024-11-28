package tokeniser;

import parser.node.operator.OperatorType;

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
        put("int", PRIMITIVE_TYPE);
        put("float", PRIMITIVE_TYPE);
        put("char", PRIMITIVE_TYPE);
        put("bool", PRIMITIVE_TYPE);


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
