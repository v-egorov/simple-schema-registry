package ru.vegorov.schemaregistry.service;

/**
 * Exception thrown when JSON transformation fails
 */
public class TransformationException extends Exception {

    public TransformationException(String message) {
        super(message);
    }

    public TransformationException(String message, Throwable cause) {
        super(message, cause);
    }
}