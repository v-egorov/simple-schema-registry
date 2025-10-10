package ru.vegorov.schemaregistry.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

@Entity
@Table(name = "transformation_templates", indexes = {
    @Index(name = "idx_transformation_templates_consumer_id", columnList = "consumer_id")
})
public class TransformationTemplateEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(name = "consumer_id", nullable = false)
    private String consumerId;

    @NotBlank
    @Column(nullable = false)
    private String engine = "jslt";

    @NotNull
    @Column(name = "template_expression", nullable = false, columnDefinition = "TEXT")
    private String templateExpression;

    @Column(length = 1000)
    private String description;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // Constructors
    public TransformationTemplateEntity() {}

    public TransformationTemplateEntity(String consumerId, String engine,
                                       String templateExpression, String description) {
        this.consumerId = consumerId;
        this.engine = engine != null ? engine : "jslt";
        this.templateExpression = templateExpression;
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