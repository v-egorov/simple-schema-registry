package ru.vegorov.schemaregistry.dto;

import jakarta.validation.constraints.NotNull;

import java.util.Map;

public class CompatibilityCheckRequest {

    @NotNull
    private Map<String, Object> newSchema;

    // Constructors
    public CompatibilityCheckRequest() {}

    public CompatibilityCheckRequest(Map<String, Object> newSchema) {
        this.newSchema = newSchema;
    }

    // Getters and Setters
    public Map<String, Object> getNewSchema() {
        return newSchema;
    }

    public void setNewSchema(Map<String, Object> newSchema) {
        this.newSchema = newSchema;
    }
}