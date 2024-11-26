package tokeniser;

import java.util.HashMap;
import java.util.Map;

import static tokeniser.TokenType.BOOL_LITERAL;
import static tokeniser.TokenType.ELSE;
import static tokeniser.TokenType.EXIT;
import static tokeniser.TokenType.IF;

public class Keywords {
    public static Map<String, TokenType> tokeniserKeywords = new HashMap<>(){{
       put("exit", EXIT);
       put("true", BOOL_LITERAL);
       put("false", BOOL_LITERAL);
       put("if", IF);
       put("else", ELSE);
    }};
}
