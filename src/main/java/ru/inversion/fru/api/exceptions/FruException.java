package ru.inversion.fru.api.exceptions;

import ru.inversion.utils.IExceptionInfo;

/** */
public class FruException extends RuntimeException implements IExceptionInfo {

    public FruException( String message ) {
        super(message);
    }

    public FruException( String message, Throwable cause ) {
        super(message, cause);
    }

    public FruException( Throwable cause ) {
        super(cause);
    }

    @Override
    public String getCategory() {
        return "FRU";
    }
}
