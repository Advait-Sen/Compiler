package adsen.helium.parser.statement;

/**
 * Represents all statements which have just one function, and do not contain more statements within
 */
public abstract non-sealed class AtomicStatement implements HeliumStatement {
    @Override
    public int length(){
        return 1;
    }
}
