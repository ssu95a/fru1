package ru.inversion.fru.data.exceptions;

import ru.inversion.fru.api.exceptions.FruException;

public class FruDataException extends FruException {

    public FruDataException(String message) {
        super(message);
    }

    public FruDataException(String message, Throwable cause) {
        super(message, cause);
    }
}
