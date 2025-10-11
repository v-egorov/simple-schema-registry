package ru.vegorov.schemaregistry.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public class ConsumerRegistrationRequest {

    @NotBlank
    private String consumerId;

    @NotBlank
    private String name;

    private String description;

    @NotEmpty
    private List<String> subjects;

    // Constructors
    public ConsumerRegistrationRequest() {}

    public ConsumerRegistrationRequest(String consumerId, String name, String description, List<String> subjects) {
        this.consumerId = consumerId;
        this.name = name;
        this.description = description;
        this.subjects = subjects;
    }

    // Getters and Setters
    public String getConsumerId() {
        return consumerId;
    }

    public void setConsumerId(String consumerId) {
        this.consumerId = consumerId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<String> getSubjects() {
        return subjects;
    }

    public void setSubjects(List<String> subjects) {
        this.subjects = subjects;
    }
}