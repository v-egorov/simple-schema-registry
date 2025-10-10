package ru.vegorov.schemaregistry.dto;

public class CompatibilityCheckResponse {

    private boolean compatible;
    private String message;

    // Constructors
    public CompatibilityCheckResponse() {}

    public CompatibilityCheckResponse(boolean compatible, String message) {
        this.compatible = compatible;
        this.message = message;
    }

    // Getters and Setters
    public boolean isCompatible() {
        return compatible;
    }

    public void setCompatible(boolean compatible) {
        this.compatible = compatible;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}