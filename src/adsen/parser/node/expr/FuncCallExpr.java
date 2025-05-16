package adsen.parser.node.expr;

import adsen.tokeniser.Token;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * This will allow to call functions inside of expressions, and have their return value be used in calculations
 */
public class FuncCallExpr implements NodeExpr {
    public final Token token;
    public final String name;
    /**
     * This is for testing purposes, should be the same as {@link FuncCallExpr#argCount}
     */
    public int expectedArgCount;
    public int argCount;
    public List<NodeExpr> arguments = new ArrayList<>();

    public FuncCallExpr(Token token, int args) {
        this.token = token;
        this.name = token.value;
        this.expectedArgCount = args;
    }

    public void addArgument(NodeExpr arg) {
        arguments.addFirst(arg);
        argCount = arguments.size();
    }

    @Override
    public String asString() {
        return "( " + name + "(" + arguments.stream().map(NodeExpr::asString).collect(Collectors.joining(", ")) + ") )";
    }
}
