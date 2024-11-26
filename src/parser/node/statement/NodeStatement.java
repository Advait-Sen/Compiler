package parser.node.statement;

import parser.node.ASTNode;

public interface NodeStatement extends ASTNode {

    @Override
    default boolean isRoot() {
        return false;
    }
}
