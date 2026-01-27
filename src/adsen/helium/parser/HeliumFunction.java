package adsen.helium.parser;

import adsen.helium.error.ExpressionError;
import adsen.helium.parser.statement.HeliumStatement;
import adsen.helium.tokeniser.Token;

import java.util.ArrayList;
import java.util.List;

public class HeliumFunction {
    public final Token returnType;
    public final Token token;
    public final String name;
    /**
     * This is a list of tokens of the type:
     * [Type, name, Type, name, ...]
     * <p>
     * So the number of arguments is equal to the length of this list divided by 2
     */
    final List<Token> signature;
    public final int args;
    List<HeliumStatement> body = new ArrayList<>();

    public HeliumFunction(Token returnType, Token nameToken, List<Token> signature) {
        this.returnType = returnType;
        this.token = nameToken;
        this.name = nameToken.value;

        if (signature.size() % 2 != 0)
            throw new ExpressionError("Invalid function signature", nameToken);

        this.signature = signature;
        this.args = signature.size() / 2;
    }

    public void andThen(HeliumStatement statement) {
        body.add(statement);
    }

    public void andThen(List<HeliumStatement> statements) {
        body.addAll(statements);
    }

    public List<HeliumStatement> getBody() {
        return body;
    }

    /**
     * Intended to be used for storing functions in {@link HeliumProgram}
     */
    public List<String> getTypeSignature() {
        List<String> typeSignature = new ArrayList<>();

        for (int i = 0; i < signature.size(); i += 2) {
            typeSignature.add(signature.get(i).value);
        }
        return typeSignature;
    }

    public List<Token> getSignature() {
        return signature;
    }

    public String asString() {
        StringBuilder builder = new StringBuilder(returnType.value + " " + name + " (");

        if (args > 0)
            builder.append(signature.get(0).value).append(" ").append(signature.get(1).value);

        for (int i = 1; i < args; i++) {
            builder.append(", ").append(signature.get(i * 2).value).append(" ").append(signature.get(i * 2 + 1).value);
        }

        builder.append(')');
        return builder.toString();
    }
}
