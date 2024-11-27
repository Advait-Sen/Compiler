package error;

import tokeniser.Token;

public class ExpressionError extends RuntimeException {
    Token token;
    int linepos;
    int colpos;

    public ExpressionError(String message, Token token) {
        super(message);
        this.token = token;
        this.linepos = token.linepos;
        this.colpos = token.colpos;
    }

    //todo better error messages, for eg if token is a string then wrapping with " + value + ", etc.
    public String getMessage() {
        //Adding 1 to linepos and colpos since they start from 0
        return "Error: %s\n  At %d:%d \n--> %s".formatted(super.getMessage(), linepos + 1, colpos + 1, token.value);
    }
}
