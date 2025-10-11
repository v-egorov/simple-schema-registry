package ru.vegorov.schemaregistry.dto;

import jakarta.validation.constraints.NotNull;

import java.util.Map;

public class TransformationRequest {

    @NotNull
    private Map<String, Object> canonicalJson;

    private String subject;

    // Constructors
    public TransformationRequest() {}

    public TransformationRequest(Map<String, Object> canonicalJson, String subject) {
        this.canonicalJson = canonicalJson;
        this.subject = subject;
    }

    // Getters and Setters
    public Map<String, Object> getCanonicalJson() {
        return canonicalJson;
    }

    public void setCanonicalJson(Map<String, Object> canonicalJson) {
        this.canonicalJson = canonicalJson;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }
}