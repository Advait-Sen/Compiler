package adsen.parser.node.statement;

import adsen.parser.node.expr.NodeExpr;
import adsen.tokeniser.Token;

import java.util.List;

public class FunctionCallStatement implements NodeStatement {

    public final Token name;
    public final List<NodeExpr> args;

    public FunctionCallStatement(Token token, List<NodeExpr> args) {
        this.name = token;
        this.args = args;
    }

    @Override
    public String asString() {
        String argStr = "(";

        if (!args.isEmpty()) argStr = argStr.concat(args.getFirst().asString());

        for (int i = 1; i < args.size(); i++) {
            argStr = argStr.concat(", ").concat(args.get(i).asString());
        }

        argStr = argStr.concat(")");

        return name.value + argStr;
    }

    @Override
    public String typeString() {
        return "function_call";
    }
}
