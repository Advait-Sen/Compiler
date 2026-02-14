package adsen.helium.parser.statement;

import adsen.helium.tokeniser.Token;

//So apparently this was the Command pattern all along
public sealed interface HeliumStatement permits AggregateStatement, AtomicStatement{
    String asString();

    /**
     * String indicating type of statement
     */
    String typeString();

    /**
     * A token that can be used in error messages
     */
    Token primaryToken();

    /**
     * How many statements this object represents
     */
    int length();
}
