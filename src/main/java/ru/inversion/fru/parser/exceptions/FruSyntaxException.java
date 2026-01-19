package ru.inversion.fru.parser.exceptions;

import java.nio.file.Path;

public class FruSyntaxException extends FruParseException {

    public FruSyntaxException(Path fruFile, String message) {
        super(fruFile, message);
    }

    public FruSyntaxException( Path fruFile, String message, Throwable cause) {
        super(fruFile, message, cause);
    }
}
