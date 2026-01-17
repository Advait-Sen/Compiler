package adsen.exec;

import adsen.tokeniser.Token;
import java.util.List;

/**
 * This will handle taking in imports
 */
public interface ImportHandler {

    void acceptImports(List<List<Token>> imports);

    void importImports();
}
