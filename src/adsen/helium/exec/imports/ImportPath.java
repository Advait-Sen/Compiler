package adsen.helium.exec.imports;

import adsen.helium.tokeniser.Token;
import java.util.List;

import static java.io.File.separator;

/**
 * This class represents a single import string, im preparation to find its location in memory
 */
public class ImportPath {
    /**
     * Used to generate errors
     */
    public final Token token;

    private final List<String> path;
    /**
     * The name of the file with the Helium code
     */
    public final String file;

    public ImportPath(List<Token> importTokens) {
        token = importTokens.getFirst();
        path = importTokens.stream().skip(1).limit(importTokens.size() - 2).map(t -> t.value).toList();
        file = importTokens.getLast().value;
    }

    public boolean isNative() {
        return path.getFirst().equals(ImportHandler.HELIUM_NAMESPACE);
    }

    public String path() {
        return path.stream().reduce((s1, s2) -> s1 + separator + s2).orElse("");
    }

    public String toString() {
        return path() + separator + file;
    }
}
