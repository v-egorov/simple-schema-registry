package ru.vegorov.schemaregistry.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import ru.vegorov.schemaregistry.dto.ConsumerRegistrationRequest;
import ru.vegorov.schemaregistry.dto.ConsumerResponse;
import ru.vegorov.schemaregistry.service.ConsumerService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/consumers")
@Tag(name = "Consumer Management", description = "Consumer registration and management")
public class ConsumerController {

    private static final Logger logger = LoggerFactory.getLogger(ConsumerController.class);

    private final ConsumerService consumerService;

    @Value("${app.logging.requests.enabled:true}")
    private boolean requestLoggingEnabled;

    public ConsumerController(ConsumerService consumerService) {
        this.consumerService = consumerService;
    }

    @PostMapping
    @Operation(summary = "Register a new consumer", description = "Register a new consumer application")
    public ResponseEntity<ConsumerResponse> registerConsumer(
            @Valid @RequestBody ConsumerRegistrationRequest request) {

        String correlationId = UUID.randomUUID().toString();
        MDC.put("correlationId", correlationId);
        MDC.put("operation", "registerConsumer");
        MDC.put("consumerId", request.getConsumerId());

        // Store correlationId in request attributes for exception handler access
        ServletRequestAttributes requestAttributes = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
        requestAttributes.setAttribute("correlationId", correlationId, 0);

        try {
            if (requestLoggingEnabled) {
                logger.info("Processing consumer registration request");
            }

            ConsumerResponse response = consumerService.registerConsumer(request);

            if (requestLoggingEnabled) {
                logger.info("Consumer registration completed successfully: status={}", HttpStatus.CREATED.value());
            }

            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } finally {
            MDC.clear();
        }
    }

    @GetMapping
    @Operation(summary = "List all consumers", description = "Retrieve all registered consumers")
    public ResponseEntity<List<ConsumerResponse>> getAllConsumers() {

        String correlationId = UUID.randomUUID().toString();
        MDC.put("correlationId", correlationId);
        MDC.put("operation", "getAllConsumers");

        // Store correlationId in request attributes for exception handler access
        ServletRequestAttributes requestAttributes = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
        requestAttributes.setAttribute("correlationId", correlationId, 0);

        try {
            if (requestLoggingEnabled) {
                logger.info("Processing get all consumers request");
            }

            List<ConsumerResponse> responses = consumerService.getAllConsumers();

            if (requestLoggingEnabled) {
                logger.info("Get all consumers completed successfully: count={}", responses.size());
            }

            return ResponseEntity.ok(responses);
        } finally {
            MDC.clear();
        }
    }

    @GetMapping("/{consumerId}")
    @Operation(summary = "Get consumer details", description = "Retrieve details of a specific consumer")
    public ResponseEntity<ConsumerResponse> getConsumer(
            @PathVariable String consumerId) {

        String correlationId = UUID.randomUUID().toString();
        MDC.put("correlationId", correlationId);
        MDC.put("operation", "getConsumer");
        MDC.put("consumerId", consumerId);

        // Store correlationId in request attributes for exception handler access
        ServletRequestAttributes requestAttributes = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
        requestAttributes.setAttribute("correlationId", correlationId, 0);

        try {
            if (requestLoggingEnabled) {
                logger.info("Processing get consumer request");
            }

            ConsumerResponse response = consumerService.getConsumer(consumerId);

            if (requestLoggingEnabled) {
                logger.info("Get consumer completed successfully");
            }

            return ResponseEntity.ok(response);
        } finally {
            MDC.clear();
        }
    }
}