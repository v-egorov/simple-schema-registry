package ru.vegorov.schemaregistry.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

public class CompatibilityCheckRequest {

    @NotNull
    private Map<String, Object> schema;

    @NotBlank
    private String subject;

    // Constructors
    public CompatibilityCheckRequest() {}

    public CompatibilityCheckRequest(Map<String, Object> schema, String subject) {
        this.schema = schema;
        this.subject = subject;
    }

    // Getters and Setters
    public Map<String, Object> getSchema() {
        return schema;
    }

    public void setSchema(Map<String, Object> schema) {
        this.schema = schema;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }
}