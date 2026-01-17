package adsen.parser.expr;

import adsen.tokeniser.Token;

/**
 * A type of node used only for debugging purposes
 */
public class NodeDummy implements NodeExpr {
    public final Token token;
    /**
     * Additional info that should be stored along with this token
     */
    public String info;

    public NodeDummy(Token token) {
        this.token = token;
        this.info = "";
    }

    public NodeDummy(Token token, String info) {
        this.token = token;
        this.info = info;
    }

    @Override
    public String asString() {
        return "(dummy: " + token.value + (info.isEmpty() ? "" : " info: " + info)+")";
    }
}
