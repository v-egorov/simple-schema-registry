package ru.vegorov.schemaregistry.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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

import com.fasterxml.jackson.databind.ObjectMapper;
import ru.vegorov.schemaregistry.util.LoggingUtils;

import java.util.UUID;
import ru.vegorov.schemaregistry.dto.CompatibilityCheckRequest;
import ru.vegorov.schemaregistry.dto.CompatibilityCheckResponse;
import ru.vegorov.schemaregistry.dto.SchemaRegistrationRequest;
import ru.vegorov.schemaregistry.dto.SchemaResponse;
import ru.vegorov.schemaregistry.dto.SchemaValidationRequest;
import ru.vegorov.schemaregistry.dto.SchemaValidationResponse;
import ru.vegorov.schemaregistry.service.SchemaRegistryService;

import java.util.List;

@RestController
@RequestMapping("/api")
@Tag(name = "Schema Registry", description = "Schema management and compatibility checking")
public class SchemaRegistryController {

    private static final Logger logger = LoggerFactory.getLogger(SchemaRegistryController.class);

    private final SchemaRegistryService schemaService;
    private final ObjectMapper objectMapper;

    @Value("${app.logging.requests.enabled:true}")
    private boolean requestLoggingEnabled;

    @Value("${app.logging.requests.include-payload:false}")
    private boolean includePayloadGlobal;

    @Value("${app.logging.requests.include-payload.schema:false}")
    private boolean includePayloadSchema;

    @Value("${app.logging.requests.payload-max-length:0}")
    private int payloadMaxLength;

    /**
     * Get effective payload logging setting for schema operations.
     * Per-controller setting takes precedence over global.
     */
    private boolean shouldIncludePayload() {
        return includePayloadSchema || (includePayloadGlobal && !includePayloadSchema);
    }

    public SchemaRegistryController(SchemaRegistryService schemaService, ObjectMapper objectMapper) {
        this.schemaService = schemaService;
        this.objectMapper = objectMapper;
    }

    // ===== CANONICAL SCHEMA ENDPOINTS =====

    @PostMapping("/schemas/{subject}")
    @Operation(summary = "Register a new canonical schema", description = "Register a new canonical schema or create a new version")
    public ResponseEntity<SchemaResponse> registerCanonicalSchema(
            @Parameter(description = "Schema subject") @PathVariable String subject,
            @Valid @RequestBody SchemaRegistrationRequest request) {

        String correlationId = UUID.randomUUID().toString();
        MDC.put("correlationId", correlationId);
        MDC.put("operation", "registerCanonicalSchema");
        MDC.put("subject", subject);

        // Store correlationId in request attributes for exception handler access
        ServletRequestAttributes requestAttributes = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
        requestAttributes.setAttribute("correlationId", correlationId, 0);

        try {
            if (requestLoggingEnabled) {
                logger.info("Processing canonical schema registration request");
                if (shouldIncludePayload()) {
                    try {
                        String payload = objectMapper.writeValueAsString(request);
                        int originalLength = payload.length();
                        payload = LoggingUtils.truncatePayload(payload, payloadMaxLength);
                        logger.debug("Request payload ({} chars): {}", originalLength, payload);
                    } catch (Exception e) {
                        logger.warn("Failed to serialize request payload for logging", e);
                    }
                }
            }

            // Ensure the subject in path matches the request
            if (!subject.equals(request.getSubject())) {
                if (requestLoggingEnabled) {
                    logger.warn("Subject mismatch in request: pathSubject={}, requestSubject={}", subject, request.getSubject());
                }
                return ResponseEntity.badRequest().build();
            }

            SchemaResponse response = schemaService.registerCanonicalSchema(request);

            if (requestLoggingEnabled) {
                logger.info("Canonical schema registration completed successfully: status={}", HttpStatus.CREATED.value());
            }

            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } finally {
            MDC.clear();
        }
    }

    @GetMapping("/schemas/{subject}/versions")
    @Operation(summary = "Get all versions of a canonical schema", description = "Retrieve all versions of a canonical schema by subject")
    public ResponseEntity<List<SchemaResponse>> getCanonicalSchemasBySubject(
            @Parameter(description = "Schema subject") @PathVariable String subject) {

        String correlationId = UUID.randomUUID().toString();
        MDC.put("correlationId", correlationId);
        MDC.put("operation", "getCanonicalSchemasBySubject");
        MDC.put("subject", subject);

        // Store correlationId in request attributes for exception handler access
        ServletRequestAttributes requestAttributes = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
        requestAttributes.setAttribute("correlationId", correlationId, 0);

        try {
            if (requestLoggingEnabled) {
                logger.info("Processing get canonical schemas by subject request");
            }

            List<SchemaResponse> responses = schemaService.getCanonicalSchemasBySubject(subject);

            if (requestLoggingEnabled) {
                logger.info("Get canonical schemas by subject completed successfully: count={}", responses.size());
            }

            return ResponseEntity.ok(responses);
        } finally {
            MDC.clear();
        }
    }

    @GetMapping("/schemas/{subject}/versions/{version}")
    @Operation(summary = "Get specific canonical schema version", description = "Retrieve a specific version of a canonical schema")
    public ResponseEntity<SchemaResponse> getCanonicalSchema(
            @Parameter(description = "Schema subject") @PathVariable String subject,
            @Parameter(description = "Schema version") @PathVariable String version) {

        String correlationId = UUID.randomUUID().toString();
        MDC.put("correlationId", correlationId);
        MDC.put("operation", "getCanonicalSchema");
        MDC.put("subject", subject);
        MDC.put("version", version);

        // Store correlationId in request attributes for exception handler access
        ServletRequestAttributes requestAttributes = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
        requestAttributes.setAttribute("correlationId", correlationId, 0);

        try {
            if (requestLoggingEnabled) {
                logger.info("Processing get canonical schema request");
            }

            SchemaResponse response = schemaService.getCanonicalSchema(subject, version);

            if (requestLoggingEnabled) {
                logger.info("Get canonical schema completed successfully");
            }

            return ResponseEntity.ok(response);
        } finally {
            MDC.clear();
        }
    }

    @GetMapping("/schemas/{subject}")
    @Operation(summary = "Get latest canonical schema version", description = "Retrieve the latest version of a canonical schema")
    public ResponseEntity<SchemaResponse> getLatestCanonicalSchema(
            @Parameter(description = "Schema subject") @PathVariable String subject) {

        String correlationId = UUID.randomUUID().toString();
        MDC.put("correlationId", correlationId);
        MDC.put("operation", "getLatestCanonicalSchema");
        MDC.put("subject", subject);

        // Store correlationId in request attributes for exception handler access
        ServletRequestAttributes requestAttributes = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
        requestAttributes.setAttribute("correlationId", correlationId, 0);

        try {
            if (requestLoggingEnabled) {
                logger.info("Processing get latest canonical schema request");
            }

            SchemaResponse response = schemaService.getLatestCanonicalSchema(subject);

            if (requestLoggingEnabled) {
                logger.info("Get latest canonical schema completed successfully");
            }

            return ResponseEntity.ok(response);
        } finally {
            MDC.clear();
        }
    }

    @PostMapping("/schemas/{subject}/compat")
    @Operation(summary = "Check canonical schema compatibility", description = "Check if a new canonical schema is compatible with existing versions")
    public ResponseEntity<CompatibilityCheckResponse> checkCanonicalSchemaCompatibility(
            @Parameter(description = "Schema subject") @PathVariable String subject,
            @Valid @RequestBody CompatibilityCheckRequest request) {

        String correlationId = UUID.randomUUID().toString();
        MDC.put("correlationId", correlationId);
        MDC.put("operation", "checkCanonicalSchemaCompatibility");
        MDC.put("subject", subject);

        // Store correlationId in request attributes for exception handler access
        ServletRequestAttributes requestAttributes = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
        requestAttributes.setAttribute("correlationId", correlationId, 0);

        try {
            if (requestLoggingEnabled) {
                logger.info("Processing canonical schema compatibility check request");
            }

            // Ensure the subject in path matches the request
            if (!subject.equals(request.getSubject())) {
                if (requestLoggingEnabled) {
                    logger.warn("Subject mismatch in compatibility check request: pathSubject={}, requestSubject={}", subject, request.getSubject());
                }
                return ResponseEntity.badRequest().build();
            }

            CompatibilityCheckResponse response = schemaService.checkCanonicalSchemaCompatibility(request);

            if (requestLoggingEnabled) {
                logger.info("Canonical schema compatibility check completed: compatible={}", response.isCompatible());
            }

            return ResponseEntity.ok(response);
        } finally {
            MDC.clear();
        }
    }

    @PostMapping("/schemas/{subject}/validate")
    @Operation(summary = "Validate JSON against canonical schema", description = "Validate JSON data against the latest version of a canonical schema or a specific version")
    public ResponseEntity<SchemaValidationResponse> validateJsonAgainstCanonicalSchema(
            @Parameter(description = "Schema subject") @PathVariable String subject,
            @Valid @RequestBody SchemaValidationRequest request) {

        String correlationId = UUID.randomUUID().toString();
        MDC.put("correlationId", correlationId);
        MDC.put("operation", "validateJsonAgainstCanonicalSchema");
        MDC.put("subject", subject);

        // Store correlationId in request attributes for exception handler access
        ServletRequestAttributes requestAttributes = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
        requestAttributes.setAttribute("correlationId", correlationId, 0);

        try {
            if (requestLoggingEnabled) {
                logger.info("Processing canonical schema validation request");
                if (shouldIncludePayload()) {
                    try {
                        String payload = objectMapper.writeValueAsString(request);
                        int originalLength = payload.length();
                        payload = LoggingUtils.truncatePayload(payload, payloadMaxLength);
                        logger.debug("Request payload ({} chars): {}", originalLength, payload);
                    } catch (Exception e) {
                        logger.warn("Failed to serialize request payload for logging", e);
                    }
                }
            }

            // Ensure the subject in path matches the request
            if (!subject.equals(request.getSubject())) {
                if (requestLoggingEnabled) {
                    logger.warn("Subject mismatch in validation request: pathSubject={}, requestSubject={}", subject, request.getSubject());
                }
                return ResponseEntity.badRequest().build();
            }

            SchemaValidationResponse response = schemaService.validateJson(request);

            if (requestLoggingEnabled) {
                logger.info("Canonical schema validation request completed: valid={}", response.isValid());
                if (shouldIncludePayload()) {
                    try {
                        String responsePayload = objectMapper.writeValueAsString(response);
                        int originalLength = responsePayload.length();
                        responsePayload = LoggingUtils.truncatePayload(responsePayload, payloadMaxLength);
                        logger.debug("Response payload ({} chars): {}", originalLength, responsePayload);
                    } catch (Exception e) {
                        logger.warn("Failed to serialize response payload for logging", e);
                    }
                }
            }

            return ResponseEntity.ok(response);
        } finally {
            MDC.clear();
        }
    }

    // ===== CONSUMER OUTPUT SCHEMA ENDPOINTS =====

    @PostMapping("/consumers/{consumerId}/schemas/{subject}")
    @Operation(summary = "Register a consumer output schema", description = "Register a new consumer output schema or create a new version")
    public ResponseEntity<SchemaResponse> registerConsumerOutputSchema(
            @Parameter(description = "Consumer ID") @PathVariable String consumerId,
            @Parameter(description = "Schema subject") @PathVariable String subject,
            @Valid @RequestBody SchemaRegistrationRequest request) {

        String correlationId = UUID.randomUUID().toString();
        MDC.put("correlationId", correlationId);
        MDC.put("operation", "registerConsumerOutputSchema");
        MDC.put("consumerId", consumerId);
        MDC.put("subject", subject);

        // Store correlationId in request attributes for exception handler access
        ServletRequestAttributes requestAttributes = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
        requestAttributes.setAttribute("correlationId", correlationId, 0);

        try {
            if (requestLoggingEnabled) {
                logger.info("Processing consumer output schema registration request");
            }

            // Ensure the subject in path matches the request
            if (!subject.equals(request.getSubject())) {
                if (requestLoggingEnabled) {
                    logger.warn("Subject mismatch in consumer request: pathSubject={}, requestSubject={}, consumerId={}",
                        subject, request.getSubject(), consumerId);
                }
                return ResponseEntity.badRequest().build();
            }

            SchemaResponse response = schemaService.registerConsumerOutputSchema(consumerId, request);

            if (requestLoggingEnabled) {
                logger.info("Consumer output schema registration completed successfully: status={}", HttpStatus.CREATED.value());
            }

            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } finally {
            MDC.clear();
        }
    }

    @GetMapping("/consumers/{consumerId}/schemas/{subject}/versions")
    @Operation(summary = "Get all versions of a consumer output schema", description = "Retrieve all versions of a consumer output schema")
    public ResponseEntity<List<SchemaResponse>> getConsumerOutputSchemasBySubjectAndConsumer(
            @Parameter(description = "Consumer ID") @PathVariable String consumerId,
            @Parameter(description = "Schema subject") @PathVariable String subject) {

        String correlationId = UUID.randomUUID().toString();
        MDC.put("correlationId", correlationId);
        MDC.put("operation", "getConsumerOutputSchemasBySubjectAndConsumer");
        MDC.put("consumerId", consumerId);
        MDC.put("subject", subject);

        // Store correlationId in request attributes for exception handler access
        ServletRequestAttributes requestAttributes = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
        requestAttributes.setAttribute("correlationId", correlationId, 0);

        try {
            if (requestLoggingEnabled) {
                logger.info("Processing get consumer output schemas by subject and consumer request");
            }

            List<SchemaResponse> responses = schemaService.getConsumerOutputSchemasBySubjectAndConsumer(subject, consumerId);

            if (requestLoggingEnabled) {
                logger.info("Get consumer output schemas by subject and consumer completed successfully: count={}", responses.size());
            }

            return ResponseEntity.ok(responses);
        } finally {
            MDC.clear();
        }
    }

    @GetMapping("/consumers/{consumerId}/schemas/{subject}/versions/{version}")
    @Operation(summary = "Get specific consumer output schema version", description = "Retrieve a specific version of a consumer output schema")
    public ResponseEntity<SchemaResponse> getConsumerOutputSchema(
            @Parameter(description = "Consumer ID") @PathVariable String consumerId,
            @Parameter(description = "Schema subject") @PathVariable String subject,
            @Parameter(description = "Schema version") @PathVariable String version) {

        String correlationId = UUID.randomUUID().toString();
        MDC.put("correlationId", correlationId);
        MDC.put("operation", "getConsumerOutputSchema");
        MDC.put("consumerId", consumerId);
        MDC.put("subject", subject);
        MDC.put("version", version);

        // Store correlationId in request attributes for exception handler access
        ServletRequestAttributes requestAttributes = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
        requestAttributes.setAttribute("correlationId", correlationId, 0);

        try {
            if (requestLoggingEnabled) {
                logger.info("Processing get consumer output schema request");
            }

            SchemaResponse response = schemaService.getConsumerOutputSchema(subject, consumerId, version);

            if (requestLoggingEnabled) {
                logger.info("Get consumer output schema completed successfully");
            }

            return ResponseEntity.ok(response);
        } finally {
            MDC.clear();
        }
    }

    @GetMapping("/consumers/{consumerId}/schemas/{subject}")
    @Operation(summary = "Get latest consumer output schema version", description = "Retrieve the latest version of a consumer output schema")
    public ResponseEntity<SchemaResponse> getLatestConsumerOutputSchema(
            @Parameter(description = "Consumer ID") @PathVariable String consumerId,
            @Parameter(description = "Schema subject") @PathVariable String subject) {

        String correlationId = UUID.randomUUID().toString();
        MDC.put("correlationId", correlationId);
        MDC.put("operation", "getLatestConsumerOutputSchema");
        MDC.put("consumerId", consumerId);
        MDC.put("subject", subject);

        // Store correlationId in request attributes for exception handler access
        ServletRequestAttributes requestAttributes = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
        requestAttributes.setAttribute("correlationId", correlationId, 0);

        try {
            if (requestLoggingEnabled) {
                logger.info("Processing get latest consumer output schema request");
            }

            SchemaResponse response = schemaService.getLatestConsumerOutputSchema(subject, consumerId);

            if (requestLoggingEnabled) {
                logger.info("Get latest consumer output schema completed successfully");
            }

            return ResponseEntity.ok(response);
        } finally {
            MDC.clear();
        }
    }

    @PostMapping("/consumers/{consumerId}/schemas/{subject}/compat")
    @Operation(summary = "Check consumer output schema compatibility", description = "Check if a new consumer output schema is compatible with existing versions")
    public ResponseEntity<CompatibilityCheckResponse> checkConsumerOutputSchemaCompatibility(
            @Parameter(description = "Consumer ID") @PathVariable String consumerId,
            @Parameter(description = "Schema subject") @PathVariable String subject,
            @Valid @RequestBody CompatibilityCheckRequest request) {

        String correlationId = UUID.randomUUID().toString();
        MDC.put("correlationId", correlationId);
        MDC.put("operation", "checkConsumerOutputSchemaCompatibility");
        MDC.put("consumerId", consumerId);
        MDC.put("subject", subject);

        // Store correlationId in request attributes for exception handler access
        ServletRequestAttributes requestAttributes = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
        requestAttributes.setAttribute("correlationId", correlationId, 0);

        try {
            if (requestLoggingEnabled) {
                logger.info("Processing consumer output schema compatibility check request");
            }

            // Ensure the subject in path matches the request
            if (!subject.equals(request.getSubject())) {
                if (requestLoggingEnabled) {
                    logger.warn("Subject mismatch in consumer compatibility check request: pathSubject={}, requestSubject={}, consumerId={}",
                        subject, request.getSubject(), consumerId);
                }
                return ResponseEntity.badRequest().build();
            }

            CompatibilityCheckResponse response = schemaService.checkConsumerOutputSchemaCompatibility(consumerId, request);

            if (requestLoggingEnabled) {
                logger.info("Consumer output schema compatibility check completed: compatible={}", response.isCompatible());
            }

            return ResponseEntity.ok(response);
        } finally {
            MDC.clear();
        }
    }

    @PostMapping("/consumers/{consumerId}/schemas/{subject}/validate")
    @Operation(summary = "Validate JSON against consumer output schema", description = "Validate JSON data against the latest version of a consumer output schema or a specific version")
    public ResponseEntity<SchemaValidationResponse> validateJsonAgainstConsumerOutputSchema(
            @Parameter(description = "Consumer ID") @PathVariable String consumerId,
            @Parameter(description = "Schema subject") @PathVariable String subject,
            @Valid @RequestBody SchemaValidationRequest request) {

        String correlationId = UUID.randomUUID().toString();
        MDC.put("correlationId", correlationId);
        MDC.put("operation", "validateJsonAgainstConsumerOutputSchema");
        MDC.put("consumerId", consumerId);
        MDC.put("subject", subject);

        // Store correlationId in request attributes for exception handler access
        ServletRequestAttributes requestAttributes = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
        requestAttributes.setAttribute("correlationId", correlationId, 0);

        try {
            if (requestLoggingEnabled) {
                logger.info("Processing consumer output schema validation request: consumerId={}", consumerId);
                if (shouldIncludePayload()) {
                    try {
                        String payload = objectMapper.writeValueAsString(request);
                        int originalLength = payload.length();
                        payload = LoggingUtils.truncatePayload(payload, payloadMaxLength);
                        logger.debug("Request payload ({} chars): {}", originalLength, payload);
                    } catch (Exception e) {
                        logger.warn("Failed to serialize request payload for logging", e);
                    }
                }
            }

            // Ensure the subject in path matches the request
            if (!subject.equals(request.getSubject())) {
                if (requestLoggingEnabled) {
                    logger.warn("Subject mismatch in consumer validation request: pathSubject={}, requestSubject={}, consumerId={}",
                        subject, request.getSubject(), consumerId);
                }
                return ResponseEntity.badRequest().build();
            }

            SchemaValidationResponse response = schemaService.validateJsonAgainstConsumerOutputSchema(request, consumerId);

            if (requestLoggingEnabled) {
                logger.info("Consumer output schema validation request completed: consumerId={}, valid={}", consumerId, response.isValid());
                if (shouldIncludePayload()) {
                    try {
                        String responsePayload = objectMapper.writeValueAsString(response);
                        int originalLength = responsePayload.length();
                        responsePayload = LoggingUtils.truncatePayload(responsePayload, payloadMaxLength);
                        logger.debug("Response payload ({} chars): {}", originalLength, responsePayload);
                    } catch (Exception e) {
                        logger.warn("Failed to serialize response payload for logging", e);
                    }
                }
            }

            return ResponseEntity.ok(response);
        } finally {
            MDC.clear();
        }
    }

    // ===== GENERAL ENDPOINTS =====

    @GetMapping("/schemas/subjects")
    @Operation(summary = "List all canonical schema subjects", description = "Retrieve all unique canonical schema subjects")
    public ResponseEntity<List<String>> getAllCanonicalSubjects() {

        String correlationId = UUID.randomUUID().toString();
        MDC.put("correlationId", correlationId);
        MDC.put("operation", "getAllCanonicalSubjects");

        // Store correlationId in request attributes for exception handler access
        ServletRequestAttributes requestAttributes = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
        requestAttributes.setAttribute("correlationId", correlationId, 0);

        try {
            if (requestLoggingEnabled) {
                logger.info("Processing get all canonical subjects request");
            }

            List<String> subjects = schemaService.getAllCanonicalSubjects();

            if (requestLoggingEnabled) {
                logger.info("Get all canonical subjects completed successfully: count={}", subjects.size());
            }

            return ResponseEntity.ok(subjects);
        } finally {
            MDC.clear();
        }
    }

    @GetMapping("/consumers/{consumerId}/schemas/subjects")
    @Operation(summary = "List all subjects for a consumer", description = "Retrieve all subjects that have consumer output schemas for the specified consumer")
    public ResponseEntity<List<String>> getSubjectsForConsumer(
            @Parameter(description = "Consumer ID") @PathVariable String consumerId) {

        String correlationId = UUID.randomUUID().toString();
        MDC.put("correlationId", correlationId);
        MDC.put("operation", "getSubjectsForConsumer");
        MDC.put("consumerId", consumerId);

        // Store correlationId in request attributes for exception handler access
        ServletRequestAttributes requestAttributes = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
        requestAttributes.setAttribute("correlationId", correlationId, 0);

        try {
            if (requestLoggingEnabled) {
                logger.info("Processing get subjects for consumer request");
            }

            List<String> subjects = schemaService.getSubjectsForConsumer(consumerId);

            if (requestLoggingEnabled) {
                logger.info("Get subjects for consumer completed successfully: count={}", subjects.size());
            }

            return ResponseEntity.ok(subjects);
        } finally {
            MDC.clear();
        }
    }


}