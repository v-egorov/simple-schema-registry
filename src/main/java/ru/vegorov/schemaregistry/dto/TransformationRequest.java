package ru.vegorov.schemaregistry.dto;

import jakarta.validation.constraints.NotNull;

import java.util.Map;

public class TransformationRequest {

    @NotNull
    private Map<String, Object> canonicalJson;

    // Constructors
    public TransformationRequest() {}

    public TransformationRequest(Map<String, Object> canonicalJson) {
        this.canonicalJson = canonicalJson;
    }

    // Getters and Setters
    public Map<String, Object> getCanonicalJson() {
        return canonicalJson;
    }

    public void setCanonicalJson(Map<String, Object> canonicalJson) {
        this.canonicalJson = canonicalJson;
    }
}