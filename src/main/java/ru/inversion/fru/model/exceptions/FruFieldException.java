package ru.inversion.fru.model.exceptions;

/** */
public class FruFieldException extends FruModelException {

    private final String fieldName;

    /** */
    public FruFieldException( String fieldName, String message ) {
        super("Field '" + fieldName + "' error: " + message);
        this.fieldName = fieldName;
    }

    /** */
    public String getFieldName( ) {
        return fieldName;
    }
}