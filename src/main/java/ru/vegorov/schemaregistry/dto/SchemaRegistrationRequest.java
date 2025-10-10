package ru.vegorov.schemaregistry.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

public class SchemaRegistrationRequest {

    @NotBlank
    private String subject;

    @NotNull
    private Map<String, Object> schema;

    private String compatibility = "BACKWARD";

    private String description;

    // Constructors
    public SchemaRegistrationRequest() {}

    public SchemaRegistrationRequest(String subject, Map<String, Object> schema,
                                   String compatibility, String description) {
        this.subject = subject;
        this.schema = schema;
        this.compatibility = compatibility;
        this.description = description;
    }

    // Getters and Setters
    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public Map<String, Object> getSchema() {
        return schema;
    }

    public void setSchema(Map<String, Object> schema) {
        this.schema = schema;
    }

    public String getCompatibility() {
        return compatibility;
    }

    public void setCompatibility(String compatibility) {
        this.compatibility = compatibility;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}