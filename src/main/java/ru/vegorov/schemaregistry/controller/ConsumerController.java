package ru.vegorov.schemaregistry.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.vegorov.schemaregistry.dto.ConsumerRegistrationRequest;
import ru.vegorov.schemaregistry.dto.ConsumerResponse;
import ru.vegorov.schemaregistry.service.ConsumerService;

import java.util.List;

@RestController
@RequestMapping("/api/consumers")
@Tag(name = "Consumer Management", description = "Consumer registration and management")
public class ConsumerController {

    private final ConsumerService consumerService;

    public ConsumerController(ConsumerService consumerService) {
        this.consumerService = consumerService;
    }

    @PostMapping
    @Operation(summary = "Register a new consumer", description = "Register a new consumer application")
    public ResponseEntity<ConsumerResponse> registerConsumer(
            @Valid @RequestBody ConsumerRegistrationRequest request) {
        ConsumerResponse response = consumerService.registerConsumer(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @Operation(summary = "List all consumers", description = "Retrieve all registered consumers")
    public ResponseEntity<List<ConsumerResponse>> getAllConsumers() {
        List<ConsumerResponse> responses = consumerService.getAllConsumers();
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/{consumerId}")
    @Operation(summary = "Get consumer details", description = "Retrieve details of a specific consumer")
    public ResponseEntity<ConsumerResponse> getConsumer(
            @PathVariable String consumerId) {
        ConsumerResponse response = consumerService.getConsumer(consumerId);
        return ResponseEntity.ok(response);
    }
}