package ru.inversion.fru.api.exceptions;

public class FruCommandLineException extends FruException {

    /** */
    public FruCommandLineException(String message) {
        super(message);
    }

    /** */
    public FruCommandLineException(String message, Throwable cause) {
        super( message, cause );
    }
}
