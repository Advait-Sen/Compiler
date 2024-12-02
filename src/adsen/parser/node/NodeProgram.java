package adsen.parser.node;

import adsen.parser.node.statement.NodeStatement;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class NodeProgram {

    public List<NodeStatement> statements = new ArrayList<>();

    public String asString() {
        Stream<String> stream = statements.stream().map(NodeStatement::asString);

        return stream.reduce("", (s1, s2) -> s1 + '\n' + s2);
    }
}
