package adsen.helium.parser.statement.atomic;

import adsen.helium.parser.expr.NodeExpr;
import adsen.helium.parser.statement.AtomicStatement;
import adsen.helium.tokeniser.Token;

import java.util.List;

//TODO see what turning this into a record class means
public class FunctionCallStatement extends AtomicStatement {

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

    @Override
    public Token primaryToken() {
        return name;
    }
}
