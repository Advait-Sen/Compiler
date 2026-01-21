package adsen.parser.statement;

import adsen.tokeniser.Token;

//So apparently this was the Command pattern all along
public interface Statement {
    String asString();

    /**
     * String indicating type of statement
     */
    String typeString();

    /**
     * A token that can be used in error messages
     */
    Token primaryToken();
}
