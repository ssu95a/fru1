package ru.inversion.fru.parser.exceptions;

public class FruSyntaxException extends FruParseException {

    public FruSyntaxException(String fruFile, String message) {
        super(fruFile, message);
    }

    public FruSyntaxException(String fruFile, String message, Throwable cause) {
        super(fruFile, message, cause);
    }
}
