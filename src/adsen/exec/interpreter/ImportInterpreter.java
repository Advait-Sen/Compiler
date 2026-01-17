package adsen.exec.interpreter;

import adsen.error.ExpressionError;
import adsen.exec.imports.ImportData;
import adsen.exec.imports.ImportPath;
import adsen.exec.imports.ImportHandler;
import adsen.tokeniser.Token;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import static adsen.parser.Parser.ROOT_DIRECTORY;

public class ImportInterpreter implements ImportHandler {


    private final Set<ImportPath> nativeImports = new HashSet<>();
    private final Set<ImportPath> externalImports = new HashSet<>();

    private final Set<ImportData> externalImportData = new HashSet<>();

    @Override
    public void acceptImports(List<List<Token>> importTokens) {
        importTokens.stream().map(ImportPath::new).forEach(ip -> {
            if (ip.isNative()) nativeImports.add(ip);
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
            String pathString = importPath.path();
            System.out.println("Attempting to get import data from "+pathString +" \\ "+importPath.file);
            Path fileDirPath = ROOT_DIRECTORY.resolve(pathString);

            ImportData importData;

            try (Stream<Path> pathStream = Files.list(fileDirPath)) {

                Optional<Path> path = pathStream.filter(p -> {
                    String name = p.getFileName().toString();
                    String shortName = name.substring(0, name.lastIndexOf('.'));

                    return shortName.equals(importPath.file);
                }).findFirst();

                if (path.isPresent()) {
                    importData = new ImportData(importPath, path.get());
                } else throw new IOException("Could not find file " + importPath.file);

            } catch (IOException ioException) {
                throw new ExpressionError("Could not import " + importPath.file + " from " + pathString + " due to: " + ioException.getMessage(), importPath.token);
            }

            externalImportData.add(importData);
        }
    }
}
