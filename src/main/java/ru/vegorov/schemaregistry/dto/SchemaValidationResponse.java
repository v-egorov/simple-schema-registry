package ru.vegorov.schemaregistry.dto;

import java.util.List;

/**
 * Response DTO for JSON schema validation results
 */
public class SchemaValidationResponse {

    private boolean valid;
    private String subject;
    private String schemaVersion;
    private List<String> errors;

    public SchemaValidationResponse() {}

    public SchemaValidationResponse(boolean valid, String subject, String schemaVersion) {
        this.valid = valid;
        this.subject = subject;
        this.schemaVersion = schemaVersion;
    }

    public SchemaValidationResponse(boolean valid, String subject, String schemaVersion, List<String> errors) {
        this.valid = valid;
        this.subject = subject;
        this.schemaVersion = schemaVersion;
        this.errors = errors;
    }

    public boolean isValid() {
        return valid;
    }

    public void setValid(boolean valid) {
        this.valid = valid;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getSchemaVersion() {
        return schemaVersion;
    }

    public void setSchemaVersion(String schemaVersion) {
        this.schemaVersion = schemaVersion;
    }

    public List<String> getErrors() {
        return errors;
    }

    public void setErrors(List<String> errors) {
        this.errors = errors;
    }
}