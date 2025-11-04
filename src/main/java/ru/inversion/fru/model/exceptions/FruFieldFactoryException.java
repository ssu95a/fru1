package ru.inversion.fru.model.exceptions;

public class FruFieldFactoryException extends FruFieldException {
    public FruFieldFactoryException(String fieldName, String issue) {
        super(fieldName, "Factory error: " + issue);
    }
}
