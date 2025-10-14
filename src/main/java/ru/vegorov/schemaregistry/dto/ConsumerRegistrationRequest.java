package ru.vegorov.schemaregistry.dto;

import jakarta.validation.constraints.NotBlank;

public class ConsumerRegistrationRequest {

    @NotBlank
    private String consumerId;

    @NotBlank
    private String name;

    private String description;

    // Constructors
    public ConsumerRegistrationRequest() {}

    public ConsumerRegistrationRequest(String consumerId, String name, String description) {
        this.consumerId = consumerId;
        this.name = name;
        this.description = description;
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
}