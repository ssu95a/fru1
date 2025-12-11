package ru.inversion.fru.print.altprint;

import ru.inversion.utils.IExceptionInfo;

public class ALTException extends RuntimeException implements IExceptionInfo {

    public ALTException(String message) {
        super(message);
    }

    public ALTException(String message, Throwable cause)
    {
        super(message, cause);
    }

    /** */
    @Override
    public String getCategory() {
        return "Alt";
    }
}
