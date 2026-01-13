package ru.inversion.fru.generator.exceptions;

import ru.inversion.fru.api.exceptions.FruException;

public class FruScriptException extends FruException {

    private final String script;

    /** */
    public FruScriptException( String message, String script ) {
        super(message);
        this.script = script;
    }

    public FruScriptException(String message, Throwable cause, String script ) {
        super(message, cause);
        this.script = script;
    }

    @Override
    public String getDetailedMessage() {
        return script;
    }
}
