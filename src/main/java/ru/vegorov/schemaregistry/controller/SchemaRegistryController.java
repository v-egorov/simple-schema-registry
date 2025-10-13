package ru.vegorov.schemaregistry.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
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

    private final SchemaRegistryService schemaService;

    public SchemaRegistryController(SchemaRegistryService schemaService) {
        this.schemaService = schemaService;
    }

    // ===== CANONICAL SCHEMA ENDPOINTS =====

    @PostMapping("/schemas/{subject}")
    @Operation(summary = "Register a new canonical schema", description = "Register a new canonical schema or create a new version")
    public ResponseEntity<SchemaResponse> registerCanonicalSchema(
            @Parameter(description = "Schema subject") @PathVariable String subject,
            @Valid @RequestBody SchemaRegistrationRequest request) {

        // Ensure the subject in path matches the request
        if (!subject.equals(request.getSubject())) {
            return ResponseEntity.badRequest().build();
        }

        SchemaResponse response = schemaService.registerCanonicalSchema(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/schemas/{subject}/versions")
    @Operation(summary = "Get all versions of a canonical schema", description = "Retrieve all versions of a canonical schema by subject")
    public ResponseEntity<List<SchemaResponse>> getCanonicalSchemasBySubject(
            @Parameter(description = "Schema subject") @PathVariable String subject) {
        List<SchemaResponse> responses = schemaService.getCanonicalSchemasBySubject(subject);
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/schemas/{subject}/versions/{version}")
    @Operation(summary = "Get specific canonical schema version", description = "Retrieve a specific version of a canonical schema")
    public ResponseEntity<SchemaResponse> getCanonicalSchema(
            @Parameter(description = "Schema subject") @PathVariable String subject,
            @Parameter(description = "Schema version") @PathVariable String version) {
        SchemaResponse response = schemaService.getCanonicalSchema(subject, version);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/schemas/{subject}")
    @Operation(summary = "Get latest canonical schema version", description = "Retrieve the latest version of a canonical schema")
    public ResponseEntity<SchemaResponse> getLatestCanonicalSchema(
            @Parameter(description = "Schema subject") @PathVariable String subject) {
        SchemaResponse response = schemaService.getLatestCanonicalSchema(subject);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/schemas/{subject}/compat")
    @Operation(summary = "Check canonical schema compatibility", description = "Check if a new canonical schema is compatible with existing versions")
    public ResponseEntity<CompatibilityCheckResponse> checkCanonicalSchemaCompatibility(
            @Parameter(description = "Schema subject") @PathVariable String subject,
            @Valid @RequestBody CompatibilityCheckRequest request) {

        // Ensure the subject in path matches the request
        if (!subject.equals(request.getSubject())) {
            return ResponseEntity.badRequest().build();
        }

        CompatibilityCheckResponse response = schemaService.checkCanonicalSchemaCompatibility(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/schemas/{subject}/validate")
    @Operation(summary = "Validate JSON against canonical schema", description = "Validate JSON data against the latest version of a canonical schema or a specific version")
    public ResponseEntity<SchemaValidationResponse> validateJsonAgainstCanonicalSchema(
            @Parameter(description = "Schema subject") @PathVariable String subject,
            @Valid @RequestBody SchemaValidationRequest request) {

        // Ensure the subject in path matches the request
        if (!subject.equals(request.getSubject())) {
            return ResponseEntity.badRequest().build();
        }

        SchemaValidationResponse response = schemaService.validateJson(request);
        return ResponseEntity.ok(response);
    }

    // ===== CONSUMER OUTPUT SCHEMA ENDPOINTS =====

    @PostMapping("/consumers/{consumerId}/schemas/{subject}")
    @Operation(summary = "Register a consumer output schema", description = "Register a new consumer output schema or create a new version")
    public ResponseEntity<SchemaResponse> registerConsumerOutputSchema(
            @Parameter(description = "Consumer ID") @PathVariable String consumerId,
            @Parameter(description = "Schema subject") @PathVariable String subject,
            @Valid @RequestBody SchemaRegistrationRequest request) {

        // Ensure the subject in path matches the request
        if (!subject.equals(request.getSubject())) {
            return ResponseEntity.badRequest().build();
        }

        SchemaResponse response = schemaService.registerConsumerOutputSchema(consumerId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/consumers/{consumerId}/schemas/{subject}/versions")
    @Operation(summary = "Get all versions of a consumer output schema", description = "Retrieve all versions of a consumer output schema")
    public ResponseEntity<List<SchemaResponse>> getConsumerOutputSchemasBySubjectAndConsumer(
            @Parameter(description = "Consumer ID") @PathVariable String consumerId,
            @Parameter(description = "Schema subject") @PathVariable String subject) {
        List<SchemaResponse> responses = schemaService.getConsumerOutputSchemasBySubjectAndConsumer(subject, consumerId);
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/consumers/{consumerId}/schemas/{subject}/versions/{version}")
    @Operation(summary = "Get specific consumer output schema version", description = "Retrieve a specific version of a consumer output schema")
    public ResponseEntity<SchemaResponse> getConsumerOutputSchema(
            @Parameter(description = "Consumer ID") @PathVariable String consumerId,
            @Parameter(description = "Schema subject") @PathVariable String subject,
            @Parameter(description = "Schema version") @PathVariable String version) {
        SchemaResponse response = schemaService.getConsumerOutputSchema(subject, consumerId, version);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/consumers/{consumerId}/schemas/{subject}")
    @Operation(summary = "Get latest consumer output schema version", description = "Retrieve the latest version of a consumer output schema")
    public ResponseEntity<SchemaResponse> getLatestConsumerOutputSchema(
            @Parameter(description = "Consumer ID") @PathVariable String consumerId,
            @Parameter(description = "Schema subject") @PathVariable String subject) {
        SchemaResponse response = schemaService.getLatestConsumerOutputSchema(subject, consumerId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/consumers/{consumerId}/schemas/{subject}/compat")
    @Operation(summary = "Check consumer output schema compatibility", description = "Check if a new consumer output schema is compatible with existing versions")
    public ResponseEntity<CompatibilityCheckResponse> checkConsumerOutputSchemaCompatibility(
            @Parameter(description = "Consumer ID") @PathVariable String consumerId,
            @Parameter(description = "Schema subject") @PathVariable String subject,
            @Valid @RequestBody CompatibilityCheckRequest request) {

        // Ensure the subject in path matches the request
        if (!subject.equals(request.getSubject())) {
            return ResponseEntity.badRequest().build();
        }

        CompatibilityCheckResponse response = schemaService.checkConsumerOutputSchemaCompatibility(consumerId, request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/consumers/{consumerId}/schemas/{subject}/validate")
    @Operation(summary = "Validate JSON against consumer output schema", description = "Validate JSON data against the latest version of a consumer output schema or a specific version")
    public ResponseEntity<SchemaValidationResponse> validateJsonAgainstConsumerOutputSchema(
            @Parameter(description = "Consumer ID") @PathVariable String consumerId,
            @Parameter(description = "Schema subject") @PathVariable String subject,
            @Valid @RequestBody SchemaValidationRequest request) {

        // Ensure the subject in path matches the request
        if (!subject.equals(request.getSubject())) {
            return ResponseEntity.badRequest().build();
        }

        SchemaValidationResponse response = schemaService.validateJsonAgainstConsumerOutputSchema(request, consumerId);
        return ResponseEntity.ok(response);
    }

    // ===== GENERAL ENDPOINTS =====

    @GetMapping("/schemas/subjects")
    @Operation(summary = "List all canonical schema subjects", description = "Retrieve all unique canonical schema subjects")
    public ResponseEntity<List<String>> getAllCanonicalSubjects() {
        List<String> subjects = schemaService.getAllCanonicalSubjects();
        return ResponseEntity.ok(subjects);
    }

    @GetMapping("/consumers/{consumerId}/schemas/subjects")
    @Operation(summary = "List all subjects for a consumer", description = "Retrieve all subjects that have consumer output schemas for the specified consumer")
    public ResponseEntity<List<String>> getConsumersForSubject(
            @Parameter(description = "Consumer ID") @PathVariable String consumerId) {
        List<String> subjects = schemaService.getConsumersForSubject(consumerId);
        return ResponseEntity.ok(subjects);
    }

    // ===== LEGACY ENDPOINTS (for backward compatibility) =====

    @PostMapping("/schemas")
    @Operation(summary = "Register a new schema - LEGACY", description = "Register a new schema or create a new version - LEGACY ENDPOINT")
    @Deprecated
    public ResponseEntity<SchemaResponse> registerSchema(
            @Valid @RequestBody SchemaRegistrationRequest request) {
        SchemaResponse response = schemaService.registerSchema(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/schemas/{subject}/all")
    @Operation(summary = "Get all versions of a schema - LEGACY", description = "Retrieve all versions of a schema by subject - LEGACY ENDPOINT")
    @Deprecated
    public ResponseEntity<List<SchemaResponse>> getSchemasBySubject(
            @Parameter(description = "Schema subject") @PathVariable String subject) {
        List<SchemaResponse> responses = schemaService.getSchemasBySubject(subject);
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/schemas/{subject}/version/{version}")
    @Operation(summary = "Get specific schema version - LEGACY", description = "Retrieve a specific version of a schema - LEGACY ENDPOINT")
    @Deprecated
    public ResponseEntity<SchemaResponse> getSchema(
            @Parameter(description = "Schema subject") @PathVariable String subject,
            @Parameter(description = "Schema version") @PathVariable String version) {
        SchemaResponse response = schemaService.getSchema(subject, version);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/schemas/compat")
    @Operation(summary = "Check schema compatibility - LEGACY", description = "Check if a new schema is compatible with existing versions - LEGACY ENDPOINT")
    @Deprecated
    public ResponseEntity<CompatibilityCheckResponse> checkCompatibility(
            @Valid @RequestBody CompatibilityCheckRequest request) {
        CompatibilityCheckResponse response = schemaService.checkCompatibility(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/subjects")
    @Operation(summary = "List all schema subjects - LEGACY", description = "Retrieve all unique schema subjects - LEGACY ENDPOINT")
    @Deprecated
    public ResponseEntity<List<String>> getAllSubjects() {
        List<String> subjects = schemaService.getAllSubjects();
        return ResponseEntity.ok(subjects);
    }
}