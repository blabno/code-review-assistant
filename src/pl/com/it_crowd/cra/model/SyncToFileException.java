package pl.com.it_crowd.cra.model;

public class SyncToFileException extends RuntimeException {
// --------------------------- CONSTRUCTORS ---------------------------

    public SyncToFileException(String message)
    {
        super(message);
    }

    public SyncToFileException(String message, Throwable cause)
    {
        super(message, cause);
    }
}
