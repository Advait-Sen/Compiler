package adsen.helium.exec.interpreter;

import adsen.helium.error.ParsingError;
import adsen.helium.exec.imports.ImportData;
import adsen.helium.exec.imports.ImportPath;
import adsen.helium.exec.imports.ImportHandler;
import adsen.helium.tokeniser.Token;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import static adsen.helium.parser.Parser.ROOT_DIRECTORY;

public class ImportInterpreter implements ImportHandler {

    //TODO eventually handle duplicate imports
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

            ImportData importData;

            try (Stream<Path> pathStream = Files.list(ROOT_DIRECTORY.resolve(pathString))) {

                //Inspired by carpet mod's script finding system from /script load
                Optional<Path> path = pathStream.filter(p -> {
                    String fullName = p.getFileName().toString();
                    String fileName = fullName.substring(0, fullName.lastIndexOf('.'));

                    return fileName.equals(importPath.file);
                }).findFirst();

                if (path.isPresent()) {
                    importData = new ImportData(importPath, path.get());
                } else throw new IOException("Could not find file " + importPath.file);

            } catch (IOException ioException) {
                throw new ParsingError("Could not import " + importPath.file + " from " + pathString + " due to: " + ioException.getMessage(), importPath.token);
            }

            externalImportData.add(importData);
        }
    }
}
