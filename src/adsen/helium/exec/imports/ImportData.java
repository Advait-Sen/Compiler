package adsen.helium.exec.imports;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Stores data about an imported file, such as its code, what type of file extension it has, etc.
 */
public class ImportData {
    /**
     * The path to the file from which this data was derived
     */
    private final ImportPath importPath;
    private final FileType type;

    /**
     * Code contained within the file, if relevant
     */
    private final String code;

    public ImportData(ImportPath importPath, Path filePath) throws IOException {
        this.importPath = importPath;


        String fullFileName = filePath.getFileName().toString();

        int dotIndex = fullFileName.indexOf('.');

        String fileName = fullFileName.substring(0, dotIndex);
        String ending = fullFileName.substring(dotIndex+1);

        if (!fileName.equals(importPath.file)) throw new IOException("Mismatched name!?!?");


        type = FileType.getTypeFromExtension(ending);

        if (type == null) throw new IOException("Invalid file extension '" + ending + "'");

        switch (type) {
            case STANDARD, LIBRARY -> {
                code = Files.readString(filePath);
            }
            default -> {
                code = null;
            }
        }
    }

    public String getCode() {
        return code != null ? code : "";
    }

    public String toString() {
        //TODO change this when other filetypes are implemented
        return "[" + type.name() + "] " + importPath.toString() + File.separator + importPath.file + ": " + code;
    }
}

