package ru.inversion.fru.parser.exceptions;

import ru.inversion.fru.api.exceptions.FruException;

/** */
public class FruParseException extends FruException {

    private String fruFile;

    public FruParseException( String fruFile, String message) {
        super(message);
    }

    public FruParseException( String fruFile, String message, Throwable cause) {
        super(message, cause);
    }
}
