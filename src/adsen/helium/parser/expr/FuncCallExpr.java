package adsen.helium.parser.expr;

import adsen.helium.tokeniser.Token;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * This will allow to call functions inside of expressions, and have their return value be used in calculations
 */
public class FuncCallExpr implements NodeExpr {
    public final Token token;
    public final String name;
    private int argCount;
    public final List<NodeExpr> arguments = new ArrayList<>();

    public FuncCallExpr(Token token) {
        this.token = token;
        this.name = token.value;
    }

    public void addArgument(NodeExpr arg) {
        arguments.addFirst(arg);
        argCount = arguments.size();
    }

    public int getArgCount() {
        return argCount;
    }

    @Override
    public String asString() {
        return "( " + name + "(" + arguments.stream().map(NodeExpr::asString).collect(Collectors.joining(", ")) + ") )";
    }
}
