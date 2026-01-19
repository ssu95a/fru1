package ru.inversion.fru.data.exceptions;

import ru.inversion.fru.api.exceptions.FruException;

import java.nio.file.Path;

public class FruDataException extends FruException {

    final private Path datFile;

    public FruDataException( Path datFile, String message ) {
        super(message);
        this.datFile = datFile;
    }

    public FruDataException( Path datFile, String message, Throwable cause) {
        super(message, cause);
        this.datFile = datFile;
    }
}
