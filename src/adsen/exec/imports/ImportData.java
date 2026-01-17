package adsen.exec.imports;

/**
 * Stores data about an imported file, such as its code, what type of file extension it has, etc.
 */
public class ImportData {
    /**
     * The path to the file from which this data was derived
     */
    private ImportPath importPath;
    private FileType type;

    /**
     * Code contained within the file, if relevant
     */
    private String code;

}

