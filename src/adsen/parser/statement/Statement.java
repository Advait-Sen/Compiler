package adsen.parser.statement;

//So apparently this was the Command pattern all along
public interface Statement {
    String asString();

    /**
     * String indicating type of statement
     */
    String typeString();

}
