package ru.inversion.fru.model.exceptions;

import ru.inversion.fru.api.exceptions.FruException;

/** */
public class FruModelException extends FruException {

    /** */
    public FruModelException(String message) {
        super(message);
    }

    /** */
    public FruModelException(String message, Throwable cause) {
        super(message, cause);
    }
}
