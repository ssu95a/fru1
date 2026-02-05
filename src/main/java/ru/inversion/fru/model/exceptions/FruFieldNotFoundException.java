package ru.inversion.fru.model.exceptions;


public class FruFieldNotFoundException extends FruFieldException {

    public FruFieldNotFoundException(String fieldName) {
        super( fieldName, "Field not found: " + fieldName );
    }
}