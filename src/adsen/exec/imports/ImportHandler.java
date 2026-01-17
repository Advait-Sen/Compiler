package adsen.exec.imports;

import adsen.tokeniser.Token;
import java.util.List;

/**
 * This will handle taking in imports
 */
public interface ImportHandler {

    /**
     * The name of the namespace where native libraries are stored.
     * IDK where this will go in the future
     */
    String HELIUM_NAMESPACE = "helium";

    /**
     * Takes in the tokens at the top of the file and creates {@link ImportPath} objects for further processing
     */
    void acceptImports(List<List<Token>> imports);

    /**
     * Handles loading native imports, which will vary depending on the execution method
     */
    void loadNativeImportData();

    /**
     * Loads all external imports from file
     */
    void loadImportData();
}
