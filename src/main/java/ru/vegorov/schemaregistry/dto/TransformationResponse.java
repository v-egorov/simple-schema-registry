package ru.vegorov.schemaregistry.dto;

import java.util.Map;

public class TransformationResponse {

    private Map<String, Object> transformedJson;
    private String subject;

    // Constructors
    public TransformationResponse() {}

    public TransformationResponse(Map<String, Object> transformedJson, String subject) {
        this.transformedJson = transformedJson;
        this.subject = subject;
    }

    // Getters and Setters
    public Map<String, Object> getTransformedJson() {
        return transformedJson;
    }

    public void setTransformedJson(Map<String, Object> transformedJson) {
        this.transformedJson = transformedJson;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }
}