package ru.vegorov.schemaregistry.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;

@Entity
@Table(name = "schemas")
public class SchemaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false)
    private String subject;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "schema_type", nullable = false)
    private SchemaType schemaType;

    @Column(name = "consumer_id")
    private String consumerId;

    @NotNull
    @Column(nullable = false)
    private String version;

    @NotNull
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "schema_json", columnDefinition = "TEXT", nullable = false)
    private Map<String, Object> schemaJson;

    @NotBlank
    @Column(nullable = false)
    private String compatibility = "BACKWARD";

    @Column(length = 1000)
    private String description;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // Constructors
    public SchemaEntity() {}

    public SchemaEntity(String subject, SchemaType schemaType, String version,
                        Map<String, Object> schemaJson, String compatibility, String description) {
        this.subject = subject;
        this.schemaType = schemaType;
        this.version = version;
        this.schemaJson = schemaJson;
        this.compatibility = compatibility != null ? compatibility : "BACKWARD";
        this.description = description;
    }

    public SchemaEntity(String subject, SchemaType schemaType, String consumerId, String version,
                        Map<String, Object> schemaJson, String compatibility, String description) {
        this.subject = subject;
        this.schemaType = schemaType;
        this.consumerId = consumerId;
        this.version = version;
        this.schemaJson = schemaJson;
        this.compatibility = compatibility != null ? compatibility : "BACKWARD";
        this.description = description;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public SchemaType getSchemaType() {
        return schemaType;
    }

    public void setSchemaType(SchemaType schemaType) {
        this.schemaType = schemaType;
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

    public Map<String, Object> getSchemaJson() {
        return schemaJson;
    }

    public void setSchemaJson(Map<String, Object> schemaJson) {
        this.schemaJson = schemaJson;
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