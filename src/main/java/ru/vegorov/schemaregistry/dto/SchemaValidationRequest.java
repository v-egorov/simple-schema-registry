package ru.vegorov.schemaregistry.dto;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Request DTO for JSON schema validation
 */
public class SchemaValidationRequest {

    @NotBlank(message = "Subject is required")
    private String subject;

    @NotNull(message = "JSON data to validate is required")
    private JsonNode jsonData;

    private String version; // Optional - if not provided, uses latest version

    public SchemaValidationRequest() {}

    public SchemaValidationRequest(String subject, JsonNode jsonData) {
        this.subject = subject;
        this.jsonData = jsonData;
    }

    public SchemaValidationRequest(String subject, JsonNode jsonData, String version) {
        this.subject = subject;
        this.jsonData = jsonData;
        this.version = version;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public JsonNode getJsonData() {
        return jsonData;
    }

    public void setJsonData(JsonNode jsonData) {
        this.jsonData = jsonData;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }
}