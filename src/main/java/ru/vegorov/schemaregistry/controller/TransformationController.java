package ru.vegorov.schemaregistry.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.vegorov.schemaregistry.dto.TransformationRequest;
import ru.vegorov.schemaregistry.dto.TransformationResponse;
import ru.vegorov.schemaregistry.dto.TransformationTemplateRequest;
import ru.vegorov.schemaregistry.dto.TransformationTemplateResponse;
import ru.vegorov.schemaregistry.service.TransformationException;
import ru.vegorov.schemaregistry.service.TransformationService;
import ru.vegorov.schemaregistry.service.TransformationVersionService;

import java.util.List;

@RestController
@RequestMapping("/api/consumers")
@Validated
@Tag(name = "JSON Transformation", description = "JSON data transformation for different consumers")
public class TransformationController {

    private final TransformationService transformationService;
    private final TransformationVersionService versionService;

    public TransformationController(TransformationService transformationService,
                                  TransformationVersionService versionService) {
        this.transformationService = transformationService;
        this.versionService = versionService;
    }

    // ===== TRANSFORMATION ENDPOINTS =====

    @PostMapping("/{consumerId}/subjects/{subject}/transform")
    @Operation(summary = "Transform JSON data", description = "Transform canonical JSON data for a specific consumer and subject using the active template version")
    public ResponseEntity<TransformationResponse> transform(
            @Parameter(description = "Consumer ID") @PathVariable String consumerId,
            @Parameter(description = "Subject") @PathVariable String subject,
            @Valid @RequestBody TransformationRequest request) throws TransformationException {
        // Set subject in request
        request.setSubject(subject);
        TransformationResponse response = transformationService.transform(consumerId, request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{consumerId}/subjects/{subject}/transform/versions/{version}")
    @Operation(summary = "Transform JSON data with specific version", description = "Transform canonical JSON data using a specific template version")
    public ResponseEntity<TransformationResponse> transformWithVersion(
            @Parameter(description = "Consumer ID") @PathVariable String consumerId,
            @Parameter(description = "Subject") @PathVariable String subject,
            @Parameter(description = "Template version") @PathVariable String version,
            @Valid @RequestBody TransformationRequest request) throws TransformationException {
        // Set subject and version in request
        request.setSubject(subject);
        request.setTransformationVersion(version);
        TransformationResponse response = transformationService.transform(consumerId, request);
        return ResponseEntity.ok(response);
    }

    // ===== TEMPLATE MANAGEMENT ENDPOINTS =====

    @PostMapping("/{consumerId}/subjects/{subject}/templates")
    @Operation(summary = "Create transformation template version", description = "Create a new version of a transformation template")
    public ResponseEntity<TransformationTemplateResponse> createTemplateVersion(
            @Parameter(description = "Consumer ID") @PathVariable String consumerId,
            @Parameter(description = "Subject") @PathVariable String subject,
            @Valid @RequestBody TransformationTemplateRequest request) {
        TransformationTemplateResponse response = transformationService.createTemplateVersion(consumerId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{consumerId}/subjects/{subject}/templates")
    @Operation(summary = "Get all template versions", description = "Retrieve all versions of transformation templates for a consumer and subject")
    public ResponseEntity<List<TransformationTemplateResponse>> getTemplateVersions(
            @Parameter(description = "Consumer ID") @PathVariable String consumerId,
            @Parameter(description = "Subject") @PathVariable String subject) {
        List<TransformationTemplateResponse> responses = transformationService.getTemplateVersions(consumerId, subject);
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/{consumerId}/subjects/{subject}/templates/active")
    @Operation(summary = "Get active template", description = "Retrieve the currently active transformation template for a consumer and subject")
    public ResponseEntity<TransformationTemplateResponse> getActiveTemplate(
            @Parameter(description = "Consumer ID") @PathVariable String consumerId,
            @Parameter(description = "Subject") @PathVariable String subject) {
        TransformationTemplateResponse response = transformationService.getActiveTemplate(consumerId, subject);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{consumerId}/subjects/{subject}/templates/versions/{version}")
    @Operation(summary = "Get specific template version", description = "Retrieve a specific version of a transformation template")
    public ResponseEntity<TransformationTemplateResponse> getTemplateVersion(
            @Parameter(description = "Consumer ID") @PathVariable String consumerId,
            @Parameter(description = "Subject") @PathVariable String subject,
            @Parameter(description = "Template version") @PathVariable String version) {
        TransformationTemplateResponse response = transformationService.getTemplateVersion(consumerId, subject, version);
        return ResponseEntity.ok(response);
    }

    // ===== VERSION MANAGEMENT ENDPOINTS =====

    @PutMapping("/{consumerId}/subjects/{subject}/templates/versions/{version}/activate")
    @Operation(summary = "Activate template version", description = "Activate a specific version of a transformation template")
    public ResponseEntity<TransformationTemplateResponse> activateTemplateVersion(
            @Parameter(description = "Consumer ID") @PathVariable String consumerId,
            @Parameter(description = "Subject") @PathVariable String subject,
            @Parameter(description = "Template version") @PathVariable String version) {
        TransformationTemplateResponse response = versionService.activateVersion(consumerId, subject, version);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{consumerId}/subjects/{subject}/templates/versions/{version}/deactivate")
    @Operation(summary = "Deactivate template version", description = "Deactivate a specific version of a transformation template")
    public ResponseEntity<TransformationTemplateResponse> deactivateTemplateVersion(
            @Parameter(description = "Consumer ID") @PathVariable String consumerId,
            @Parameter(description = "Subject") @PathVariable String subject,
            @Parameter(description = "Template version") @PathVariable String version) {
        TransformationTemplateResponse response = versionService.deactivateVersion(consumerId, subject, version);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{consumerId}/subjects/{subject}/templates/versions/{version}")
    @Operation(summary = "Delete template version", description = "Delete a specific version of a transformation template (only if not active)")
    public ResponseEntity<Void> deleteTemplateVersion(
            @Parameter(description = "Consumer ID") @PathVariable String consumerId,
            @Parameter(description = "Subject") @PathVariable String subject,
            @Parameter(description = "Template version") @PathVariable String version) {
        versionService.deleteVersion(consumerId, subject, version);
        return ResponseEntity.noContent().build();
    }

    // ===== UTILITY ENDPOINTS =====

    @GetMapping("/engines")
    @Operation(summary = "List available transformation engines", description = "Get list of supported transformation engines")
    public ResponseEntity<List<String>> getAvailableEngines() {
        List<String> engines = transformationService.getAvailableEngines();
        return ResponseEntity.ok(engines);
    }

    // ===== LEGACY ENDPOINTS (for backward compatibility) =====

    @PostMapping("/{consumerId}/transform")
    @Operation(summary = "Transform JSON data - LEGACY", description = "Transform canonical JSON data for a specific consumer - LEGACY ENDPOINT")
    @Deprecated
    public ResponseEntity<TransformationResponse> transformLegacy(
            @Parameter(description = "Consumer ID") @PathVariable String consumerId,
            @Parameter(description = "Subject") @RequestParam("subject") String subject,
            @Valid @RequestBody TransformationRequest request) throws TransformationException {
        // Set subject in request
        request.setSubject(subject);
        TransformationResponse response = transformationService.transform(consumerId, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/templates/{consumerId}")
    @Operation(summary = "Get transformation template - LEGACY", description = "Retrieve transformation template for a consumer - LEGACY ENDPOINT")
    @Deprecated
    public ResponseEntity<TransformationTemplateResponse> getTemplate(
            @Parameter(description = "Consumer ID") @PathVariable String consumerId) {
        TransformationTemplateResponse response = transformationService.getTemplate(consumerId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/templates/{consumerId}")
    @Operation(summary = "Create/update transformation template - LEGACY", description = "Create or update transformation template for a consumer - LEGACY ENDPOINT")
    @Deprecated
    public ResponseEntity<TransformationTemplateResponse> createOrUpdateTemplate(
            @Parameter(description = "Consumer ID") @PathVariable String consumerId,
            @Valid @RequestBody TransformationTemplateRequest request) {
        TransformationTemplateResponse response = transformationService.createOrUpdateTemplate(consumerId, request);
        return ResponseEntity.ok(response);
    }
}