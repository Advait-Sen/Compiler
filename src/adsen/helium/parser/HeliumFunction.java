package adsen.helium.parser;

import adsen.helium.error.ParsingError;
import adsen.helium.parser.statement.HeliumStatement;
import adsen.helium.tokeniser.Token;

import java.util.ArrayList;
import java.util.Collections;
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
    public final int argumentCount;
    final List<HeliumStatement> body;

    public HeliumFunction(Token returnType, Token nameToken, List<Token> signature, List<HeliumStatement> statements) {
        this.returnType = returnType;
        this.token = nameToken;
        this.name = nameToken.value;
        this.body = Collections.unmodifiableList(statements);

        if (signature.size() % 2 != 0)
            throw new ParsingError("Invalid function signature", nameToken);

        this.signature = Collections.unmodifiableList(signature);
        this.argumentCount = signature.size() / 2;
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

        if (argumentCount > 0)
            builder.append(signature.get(0).value).append(" ").append(signature.get(1).value);

        for (int i = 1; i < argumentCount; i++) {
            builder.append(", ").append(signature.get(i * 2).value).append(" ").append(signature.get(i * 2 + 1).value);
        }

        builder.append(')');
        return builder.toString();
    }
}
