package adsen.helium.parser.statement;

import adsen.helium.tokeniser.Token;

//So apparently this was the Command pattern all along
public sealed interface HeliumStatement permits HeliumStatement.AggregateStatement, HeliumStatement.AtomicStatement {
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

    abstract non-sealed class AtomicStatement implements HeliumStatement {
        @Override
        public int length(){
            return 1;
        }
    }

    /**
     * Represents statements which have their own function, involving the calling of more statements contained within
     */
    abstract non-sealed class AggregateStatement implements HeliumStatement {}
}
