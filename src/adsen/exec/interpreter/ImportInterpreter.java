package adsen.exec.interpreter;

import adsen.exec.imports.ImportPath;
import adsen.exec.imports.ImportHandler;
import adsen.tokeniser.Token;
import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ImportInterpreter implements ImportHandler {


    private final Set<ImportPath> nativeImports = new HashSet<>();
    private final Set<ImportPath> externalImports = new HashSet<>();

    @Override
    public void acceptImports(List<List<Token>> importTokens) {
        importTokens.stream().map(ImportPath::new).forEach(ip->{
            if(ip.isNative()) nativeImports.add(ip);
            else externalImports.add(ip);
        });
    }

    @Override
    public void loadNativeImportData() {
        //This is a temporary fix which imports from hard-coded files
        //In the future, libraries will be standardised across interpreter and translated versions
        //That will involve creating built-in libraries which will be accessed through this function
        //Till then, the hard-coded native version will suffice


    }

    @Override
    public void loadImportData() {
        for (ImportPath importPath : externalImports) {
            try {
                String pathString = importPath.path();
                File directory = new File(pathString);

                File[] files;

                //I have never used Java files before in my life. Is that obvious?
                if(!directory.isDirectory() || (files = directory.listFiles())==null || files.length==0){
                    throw new IOException("Found nothing at '"+pathString+"'");
                }

                boolean foundFile = false;

                for (int i = 0; i < files.length && !foundFile; i++) {
                    File file = files[i];

                }


            } catch (IOException ioException) {
                System.out.println("Could not import "+importPath.file +" due to:" + ioException.getMessage());
            }
        }
    }
}
