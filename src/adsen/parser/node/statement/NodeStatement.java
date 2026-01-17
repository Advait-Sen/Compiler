package adsen.parser.node.statement;

//So apparently this was the Command pattern all along
//TODO rename this to Statement and move the package out of node, since these aren't nodes
public interface NodeStatement {
    String asString();

    /**
     * String indicating type of statement
     */
    String typeString();

}
