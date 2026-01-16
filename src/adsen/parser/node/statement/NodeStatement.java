package adsen.parser.node.statement;

public abstract class NodeStatement {
    public abstract String asString();
    /**
     * String indicating type of statement
     */
    public abstract String typeString();

    //TODO implement execute() function to implement Factory Method design pattern or smth
}
