package adsen.error;

import adsen.tokeniser.Token;

import static adsen.tokeniser.TokenType.VOID;

public class ExpressionError extends RuntimeException {
    Token token;
    int linepos;
    int colpos;
    //TODO re-check every single error message to see if I can make use of new Statement.primaryToken()
    public ExpressionError(String message, Token token) {
        super(message);
        if (token != null) { //Token should not be null generally, this is for special cases
            this.token = token;
            this.linepos = token.linepos;
            this.colpos = token.colpos;
        } else {
            this.token = new Token("<empty>", VOID);
            this.linepos = 0;
            this.colpos = 0;
        }
    }

    //todo better error messages, for eg if token is a string then wrapping with " + value + ", etc.
    public String getMessage() {
        //Adding 1 to linepos and colpos since they start from 0
        return "Error: %s\n  At %d:%d \n--> %s".formatted(super.getMessage(), linepos + 1, colpos + 1, token.value);
    }
}
