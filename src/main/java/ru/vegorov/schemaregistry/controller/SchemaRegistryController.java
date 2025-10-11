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
import ru.vegorov.schemaregistry.service.SchemaRegistryService;

import java.util.List;

@RestController
@RequestMapping("/api/schemas")
@Tag(name = "Schema Registry", description = "Schema management and compatibility checking")
public class SchemaRegistryController {

    private final SchemaRegistryService schemaService;

    public SchemaRegistryController(SchemaRegistryService schemaService) {
        this.schemaService = schemaService;
    }

    @PostMapping
    @Operation(summary = "Register a new schema", description = "Register a new schema or create a new version")
    public ResponseEntity<SchemaResponse> registerSchema(
            @Valid @RequestBody SchemaRegistrationRequest request) {
        SchemaResponse response = schemaService.registerSchema(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{subject}")
    @Operation(summary = "Get all versions of a schema", description = "Retrieve all versions of a schema by subject")
    public ResponseEntity<List<SchemaResponse>> getSchemasBySubject(
            @Parameter(description = "Schema subject") @PathVariable String subject) {
        List<SchemaResponse> responses = schemaService.getSchemasBySubject(subject);
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/{subject}/{version}")
    @Operation(summary = "Get specific schema version", description = "Retrieve a specific version of a schema")
    public ResponseEntity<SchemaResponse> getSchema(
            @Parameter(description = "Schema subject") @PathVariable String subject,
            @Parameter(description = "Schema version") @PathVariable String version) {
        SchemaResponse response = schemaService.getSchema(subject, version);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{subject}/latest")
    @Operation(summary = "Get latest schema version", description = "Retrieve the latest version of a schema")
    public ResponseEntity<SchemaResponse> getLatestSchema(
            @Parameter(description = "Schema subject") @PathVariable String subject) {
        SchemaResponse response = schemaService.getLatestSchema(subject);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/compat")
    @Operation(summary = "Check schema compatibility", description = "Check if a new schema is compatible with existing versions")
    public ResponseEntity<CompatibilityCheckResponse> checkCompatibility(
            @Valid @RequestBody CompatibilityCheckRequest request) {
        CompatibilityCheckResponse response = schemaService.checkCompatibility(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/subjects")
    @Operation(summary = "List all schema subjects", description = "Retrieve all unique schema subjects")
    public ResponseEntity<List<String>> getAllSubjects() {
        List<String> subjects = schemaService.getAllSubjects();
        return ResponseEntity.ok(subjects);
    }
}