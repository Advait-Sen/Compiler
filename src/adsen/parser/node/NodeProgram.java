package adsen.parser.node;

import adsen.parser.node.statement.NodeStatement;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class NodeProgram implements ASTNode{

    public List<NodeStatement> statements = new ArrayList<>();

    @Override
    public String asString() {
        Stream<String> stream = statements.stream().map(ASTNode::asString);

        return stream.reduce("", (s1, s2)->s1+'\n'+s2);
    }

    @Override
    public boolean isRoot() {
        return true;
    }
}
