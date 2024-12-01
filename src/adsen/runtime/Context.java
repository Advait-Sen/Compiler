package adsen.runtime;

/**
 * An enum to keep track of what we are currently looking to do in a particular piece of code
 */
public enum Context {
    EXIT, ASSIGNMENT,
    INTEGER, FLOAT, BOOL, CHAR
}
