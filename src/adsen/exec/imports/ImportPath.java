package adsen.exec.imports;

import adsen.tokeniser.Token;
import java.util.List;

/**
 * This class represents a single import string, im preparation to find its location in memory
 */
public class ImportPath {
    private final List<String> path;
    /**
     * The name of the file with the Helium code
     */
    public final String file;

    public ImportPath(List<Token> importTokens) {
        path = importTokens.stream().limit(importTokens.size() - 1).map(t -> t.value).toList();
        file = importTokens.getLast().value;
    }

    public boolean isNative() {
        return path.getFirst().equals(ImportHandler.HELIUM_NAMESPACE);
    }

    public String path() {
        return "/" + path.stream().reduce((s1, s2) -> s1 + "/" + s2).orElse("");
    }
}
