package ru.vegorov.schemaregistry.service;

/**
 * Exception thrown when JSLT function execution fails.
 */
public class JsltFunctionException extends Exception {

    public JsltFunctionException(String message) {
        super(message);
    }

    public JsltFunctionException(String message, Throwable cause) {
        super(message, cause);
    }
}