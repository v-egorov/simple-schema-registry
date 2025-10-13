package ru.vegorov.schemaregistry.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

@Entity
@Table(name = "transformation_templates")
public class TransformationTemplateEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(name = "consumer_id", nullable = false)
    private String consumerId;

    @NotBlank
    @Column(nullable = false)
    private String subject;

    @NotBlank
    @Column(nullable = false)
    private String version;

    @NotBlank
    @Column(nullable = false)
    private String engine = "jslt";

    @Column(name = "template_expression")
    private String templateExpression;

    @Column(name = "configuration")
    private String configuration;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "input_schema_id", nullable = false)
    private SchemaEntity inputSchema;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "output_schema_id", nullable = false)
    private SchemaEntity outputSchema;

    @NotNull
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = false;

    @Column(length = 1000)
    private String description;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // Constructors
    public TransformationTemplateEntity() {}

    public TransformationTemplateEntity(String consumerId, String subject, String version,
                                      String engine, String templateExpression, String configuration,
                                      SchemaEntity inputSchema, SchemaEntity outputSchema,
                                      Boolean isActive, String description) {
        this.consumerId = consumerId;
        this.subject = subject;
        this.version = version;
        this.engine = engine != null ? engine : "jslt";
        this.templateExpression = templateExpression;
        this.configuration = configuration;
        this.inputSchema = inputSchema;
        this.outputSchema = outputSchema;
        this.isActive = isActive != null ? isActive : false;
        this.description = description;
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

    public String getTemplateExpression() {
        return templateExpression;
    }

    public void setTemplateExpression(String templateExpression) {
        this.templateExpression = templateExpression;
    }

    public String getConfiguration() {
        return configuration;
    }

    public void setConfiguration(String configuration) {
        this.configuration = configuration;
    }

    public SchemaEntity getInputSchema() {
        return inputSchema;
    }

    public void setInputSchema(SchemaEntity inputSchema) {
        this.inputSchema = inputSchema;
    }

    public SchemaEntity getOutputSchema() {
        return outputSchema;
    }

    public void setOutputSchema(SchemaEntity outputSchema) {
        this.outputSchema = outputSchema;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
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

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}