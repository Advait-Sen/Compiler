package adsen.parser.node.statement;

import adsen.parser.node.ASTNode;

public interface NodeStatement extends ASTNode {
    /**
     * String indicating type of statement
     */
    String typeString();
}
