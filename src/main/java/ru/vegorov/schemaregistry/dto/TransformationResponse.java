package ru.vegorov.schemaregistry.dto;

import java.util.Map;

public class TransformationResponse {

    private Map<String, Object> transformedJson;

    // Constructors
    public TransformationResponse() {}

    public TransformationResponse(Map<String, Object> transformedJson) {
        this.transformedJson = transformedJson;
    }

    // Getters and Setters
    public Map<String, Object> getTransformedJson() {
        return transformedJson;
    }

    public void setTransformedJson(Map<String, Object> transformedJson) {
        this.transformedJson = transformedJson;
    }
}