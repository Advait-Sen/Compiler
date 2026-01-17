package adsen.parser.node.statement;

//So apparently this was the Command pattern all along
public interface NodeStatement {
    String asString();

    /**
     * String indicating type of statement
     */
    String typeString();

}
