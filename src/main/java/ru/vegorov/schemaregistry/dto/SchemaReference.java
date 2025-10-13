package ru.vegorov.schemaregistry.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Reference to a schema using meaningful identifiers instead of database IDs
 */
public class SchemaReference {

    @NotBlank(message = "Subject is required")
    private String subject;

    private String consumerId; // Required for consumer_output schemas, null for canonical schemas

    private String version; // Optional - if not provided, uses latest version

    // Constructors
    public SchemaReference() {}

    public SchemaReference(String subject) {
        this.subject = subject;
    }

    public SchemaReference(String subject, String version) {
        this.subject = subject;
        this.version = version;
    }

    public SchemaReference(String subject, String consumerId, String version) {
        this.subject = subject;
        this.consumerId = consumerId;
        this.version = version;
    }

    // Getters and Setters
    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getConsumerId() {
        return consumerId;
    }

    public void setConsumerId(String consumerId) {
        this.consumerId = consumerId;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    @Override
    public String toString() {
        if (consumerId != null) {
            return String.format("consumer:%s:%s:%s", consumerId, subject, version != null ? version : "latest");
        } else {
            return String.format("canonical:%s:%s", subject, version != null ? version : "latest");
        }
    }
}