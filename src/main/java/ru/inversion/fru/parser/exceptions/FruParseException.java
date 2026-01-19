package ru.inversion.fru.parser.exceptions;

import ru.inversion.fru.api.exceptions.FruException;

import java.nio.file.Path;

/** */
public class FruParseException extends FruException {

    private Path fruFile;

    public FruParseException( Path fruFile, String message) {
        super(message);
        this.fruFile = fruFile;
    }

    public FruParseException( Path fruFile, String message, Throwable cause) {
        super(message, cause);
        this.fruFile = fruFile;
    }
}
