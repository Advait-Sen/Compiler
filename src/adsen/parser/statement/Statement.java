package adsen.parser.statement;

//So apparently this was the Command pattern all along
//TODO rename this to Statement and move the package out of node, since these aren't nodes
public interface Statement {
    String asString();

    /**
     * String indicating type of statement
     */
    String typeString();

}
