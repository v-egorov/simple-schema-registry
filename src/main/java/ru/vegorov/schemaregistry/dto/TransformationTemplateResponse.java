package ru.vegorov.schemaregistry.dto;

import java.time.LocalDateTime;
import ru.vegorov.schemaregistry.dto.SchemaReference;

public class TransformationTemplateResponse {

    private Long id;
    private String consumerId;
    private String subject;
    private String version;
    private String engine;
    private SchemaReference inputSchema;
    private SchemaReference outputSchema;
    private Boolean isActive;
    private String expression;
    private String configuration;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Constructors
    public TransformationTemplateResponse() {}

    public TransformationTemplateResponse(Long id, String consumerId, String subject, String version,
                                         String engine, SchemaReference inputSchema, SchemaReference outputSchema, Boolean isActive,
                                         String expression, String configuration, String description,
                                         LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.consumerId = consumerId;
        this.subject = subject;
        this.version = version;
        this.engine = engine;
        this.inputSchema = inputSchema;
        this.outputSchema = outputSchema;
        this.isActive = isActive;
        this.expression = expression;
        this.configuration = configuration;
        this.description = description;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getConsumerId() {
        return consumerId;
    }

    public void setConsumerId(String consumerId) {
        this.consumerId = consumerId;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getEngine() {
        return engine;
    }

    public void setEngine(String engine) {
        this.engine = engine;
    }

    public SchemaReference getInputSchema() {
        return inputSchema;
    }

    public void setInputSchema(SchemaReference inputSchema) {
        this.inputSchema = inputSchema;
    }

    public SchemaReference getOutputSchema() {
        return outputSchema;
    }

    public void setOutputSchema(SchemaReference outputSchema) {
        this.outputSchema = outputSchema;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public String getExpression() {
        return expression;
    }

    public void setExpression(String expression) {
        this.expression = expression;
    }

    public String getConfiguration() {
        return configuration;
    }

    public void setConfiguration(String configuration) {
        this.configuration = configuration;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}