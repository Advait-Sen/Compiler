package adsen.parser.node;

/**
 * TODO remove this sooner rather than later, there's no benefit to have everything inherit from this
 */
public interface ASTNode {
    String asString();

    boolean isRoot();
}
