package ru.inversion.fru.model.items.exceptions;

import ru.inversion.fru.api.exceptions.FruException;

public class FruItemBadValueException extends FruException {

    /** */
    public FruItemBadValueException( String message ) {
        super( message );
    }

    /** */
    public FruItemBadValueException( String message, Throwable cause ) {
        super( message, cause );
    }
}
