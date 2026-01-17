package adsen.runtime.interpreter;

import adsen.runtime.ImportHandler;
import adsen.tokeniser.Token;
import java.util.ArrayList;
import java.util.List;

public class ImportInterpreter implements ImportHandler {


    //Singleton pattern I think
    private static ImportInterpreter importHandler= null;
    public static ImportInterpreter getInstance() {
        if(importHandler == null){
            importHandler = new ImportInterpreter();
        }
        return importHandler;
    }

    private ImportInterpreter() {}

    @Override
    public void acceptImports(List<List<Token>> imports) {
        List<String> importStrings = new ArrayList<>();

        for (List<Token> importList : imports) {
            StringBuilder sb = new StringBuilder();

            for (Token token : importList) {
                sb.append(token.value).append('/');
            }

            importStrings.add(sb.toString());
        }
    }

    @Override
    public void importImports() {

    }
}
