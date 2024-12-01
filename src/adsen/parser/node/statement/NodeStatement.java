package adsen.parser.node.statement;

import adsen.parser.node.ASTNode;

public interface NodeStatement extends ASTNode {

    @Override
    default boolean isRoot() {
        return false;
    }

    /**
     * String indicating type of statement
     */
    String typeString();
}
