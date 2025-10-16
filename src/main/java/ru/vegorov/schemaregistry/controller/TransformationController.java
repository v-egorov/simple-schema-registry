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
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import ru.vegorov.schemaregistry.dto.TransformationRequest;
import ru.vegorov.schemaregistry.dto.TransformationResponse;
import ru.vegorov.schemaregistry.dto.TransformationTemplateRequest;
import ru.vegorov.schemaregistry.dto.TransformationTemplateResponse;
import ru.vegorov.schemaregistry.service.TransformationException;
import ru.vegorov.schemaregistry.service.TransformationService;
import ru.vegorov.schemaregistry.service.TransformationVersionService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/consumers")
@Validated
@Tag(name = "JSON Transformation", description = "JSON data transformation for different consumers")
public class TransformationController {

    private static final Logger logger = LoggerFactory.getLogger(TransformationController.class);

    static {
        logger.info("TransformationController loaded");
    }

    private final TransformationService transformationService;
    private final TransformationVersionService versionService;

    @Value("${app.logging.requests.enabled:true}")
    private boolean requestLoggingEnabled;

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

        String correlationId = UUID.randomUUID().toString();
        MDC.put("correlationId", correlationId);
        MDC.put("operation", "transform");
        MDC.put("consumerId", consumerId);
        MDC.put("subject", subject);

        // Store correlationId in request attributes for exception handler access
        ServletRequestAttributes requestAttributes = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
        requestAttributes.setAttribute("correlationId", correlationId, 0);

        logger.info("Processing transformation request for consumer={}, subject={}", consumerId, subject);

        try {

            // Set subject in request
            request.setSubject(subject);
            TransformationResponse response = transformationService.transform(consumerId, request);

            if (requestLoggingEnabled) {
                logger.info("Transformation request completed successfully");
            }

            return ResponseEntity.ok(response);
        } finally {
            MDC.clear();
        }
    }

    @PostMapping("/{consumerId}/subjects/{subject}/transform/versions/{version}")
    @Operation(summary = "Transform JSON data with specific version", description = "Transform canonical JSON data using a specific template version")
    public ResponseEntity<TransformationResponse> transformWithVersion(
            @Parameter(description = "Consumer ID") @PathVariable String consumerId,
            @Parameter(description = "Subject") @PathVariable String subject,
            @Parameter(description = "Template version") @PathVariable String version,
            @Valid @RequestBody TransformationRequest request) throws TransformationException {

        String correlationId = UUID.randomUUID().toString();
        MDC.put("correlationId", correlationId);
        MDC.put("operation", "transformWithVersion");
        MDC.put("consumerId", consumerId);
        MDC.put("subject", subject);
        MDC.put("version", version);

        // Store correlationId in request attributes for exception handler access
        ServletRequestAttributes requestAttributes = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
        requestAttributes.setAttribute("correlationId", correlationId, 0);

        logger.info("Processing transformation request with version for consumer={}, subject={}, version={}", consumerId, subject, version);

        try {

            // Set subject and version in request
            request.setSubject(subject);
            request.setTransformationVersion(version);
            TransformationResponse response = transformationService.transform(consumerId, request);

            if (requestLoggingEnabled) {
                logger.info("Transformation request with version completed successfully");
            }

            return ResponseEntity.ok(response);
        } finally {
            MDC.clear();
        }
    }

    // ===== TEMPLATE MANAGEMENT ENDPOINTS =====

    @PostMapping("/{consumerId}/subjects/{subject}/templates")
    @Operation(summary = "Create transformation template version", description = "Create a new version of a transformation template")
    public ResponseEntity<TransformationTemplateResponse> createTemplateVersion(
            @Parameter(description = "Consumer ID") @PathVariable String consumerId,
            @Parameter(description = "Subject") @PathVariable String subject,
            @Valid @RequestBody TransformationTemplateRequest request) {

        String correlationId = UUID.randomUUID().toString();
        MDC.put("correlationId", correlationId);
        MDC.put("operation", "createTemplateVersion");
        MDC.put("consumerId", consumerId);
        MDC.put("subject", subject);

        // Store correlationId in request attributes for exception handler access
        ServletRequestAttributes requestAttributes = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
        requestAttributes.setAttribute("correlationId", correlationId, 0);

        try {
            if (requestLoggingEnabled) {
                logger.info("Processing template creation request");
            }

            TransformationTemplateResponse response = transformationService.createTemplateVersion(consumerId, request);

            if (requestLoggingEnabled) {
                logger.info("Template creation completed successfully: status={}", HttpStatus.CREATED.value());
            }

            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } finally {
            MDC.clear();
        }
    }

    @GetMapping("/{consumerId}/subjects/{subject}/templates")
    @Operation(summary = "Get all template versions", description = "Retrieve all versions of transformation templates for a consumer and subject")
    public ResponseEntity<List<TransformationTemplateResponse>> getTemplateVersions(
            @Parameter(description = "Consumer ID") @PathVariable String consumerId,
            @Parameter(description = "Subject") @PathVariable String subject) {

        String correlationId = UUID.randomUUID().toString();
        MDC.put("correlationId", correlationId);
        MDC.put("operation", "getTemplateVersions");
        MDC.put("consumerId", consumerId);
        MDC.put("subject", subject);

        // Store correlationId in request attributes for exception handler access
        ServletRequestAttributes requestAttributes = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
        requestAttributes.setAttribute("correlationId", correlationId, 0);

        try {
            if (requestLoggingEnabled) {
                logger.info("Processing get template versions request");
            }

            List<TransformationTemplateResponse> responses = transformationService.getTemplateVersions(consumerId, subject);

            if (requestLoggingEnabled) {
                logger.info("Get template versions completed successfully: count={}", responses.size());
            }

            return ResponseEntity.ok(responses);
        } finally {
            MDC.clear();
        }
    }

    @GetMapping("/{consumerId}/subjects/{subject}/templates/active")
    @Operation(summary = "Get active template", description = "Retrieve the currently active transformation template for a consumer and subject")
    public ResponseEntity<TransformationTemplateResponse> getActiveTemplate(
            @Parameter(description = "Consumer ID") @PathVariable String consumerId,
            @Parameter(description = "Subject") @PathVariable String subject) {

        String correlationId = UUID.randomUUID().toString();
        MDC.put("correlationId", correlationId);
        MDC.put("operation", "getActiveTemplate");
        MDC.put("consumerId", consumerId);
        MDC.put("subject", subject);

        // Store correlationId in request attributes for exception handler access
        ServletRequestAttributes requestAttributes = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
        requestAttributes.setAttribute("correlationId", correlationId, 0);

        try {
            if (requestLoggingEnabled) {
                logger.info("Processing get active template request");
            }

            TransformationTemplateResponse response = transformationService.getActiveTemplate(consumerId, subject);

            if (requestLoggingEnabled) {
                logger.info("Get active template completed successfully");
            }

            return ResponseEntity.ok(response);
        } finally {
            MDC.clear();
        }
    }

    @GetMapping("/{consumerId}/subjects/{subject}/templates/versions/{version}")
    @Operation(summary = "Get specific template version", description = "Retrieve a specific version of a transformation template")
    public ResponseEntity<TransformationTemplateResponse> getTemplateVersion(
            @Parameter(description = "Consumer ID") @PathVariable String consumerId,
            @Parameter(description = "Subject") @PathVariable String subject,
            @Parameter(description = "Template version") @PathVariable String version) {

        String correlationId = UUID.randomUUID().toString();
        MDC.put("correlationId", correlationId);
        MDC.put("operation", "getTemplateVersion");
        MDC.put("consumerId", consumerId);
        MDC.put("subject", subject);
        MDC.put("version", version);

        // Store correlationId in request attributes for exception handler access
        ServletRequestAttributes requestAttributes = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
        requestAttributes.setAttribute("correlationId", correlationId, 0);

        try {
            if (requestLoggingEnabled) {
                logger.info("Processing get template version request");
            }

            TransformationTemplateResponse response = transformationService.getTemplateVersion(consumerId, subject, version);

            if (requestLoggingEnabled) {
                logger.info("Get template version completed successfully");
            }

            return ResponseEntity.ok(response);
        } finally {
            MDC.clear();
        }
    }

    // ===== VERSION MANAGEMENT ENDPOINTS =====

    @PutMapping("/{consumerId}/subjects/{subject}/templates/versions/{version}/activate")
    @Operation(summary = "Activate template version", description = "Activate a specific version of a transformation template")
    public ResponseEntity<TransformationTemplateResponse> activateTemplateVersion(
            @Parameter(description = "Consumer ID") @PathVariable String consumerId,
            @Parameter(description = "Subject") @PathVariable String subject,
            @Parameter(description = "Template version") @PathVariable String version) {

        String correlationId = UUID.randomUUID().toString();
        MDC.put("correlationId", correlationId);
        MDC.put("operation", "activateTemplateVersion");
        MDC.put("consumerId", consumerId);
        MDC.put("subject", subject);
        MDC.put("version", version);

        // Store correlationId in request attributes for exception handler access
        ServletRequestAttributes requestAttributes = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
        requestAttributes.setAttribute("correlationId", correlationId, 0);

        try {
            if (requestLoggingEnabled) {
                logger.info("Processing activate template version request");
            }

            TransformationTemplateResponse response = versionService.activateVersion(consumerId, subject, version);

            if (requestLoggingEnabled) {
                logger.info("Activate template version completed successfully");
            }

            return ResponseEntity.ok(response);
        } finally {
            MDC.clear();
        }
    }

    @PutMapping("/{consumerId}/subjects/{subject}/templates/versions/{version}/deactivate")
    @Operation(summary = "Deactivate template version", description = "Deactivate a specific version of a transformation template")
    public ResponseEntity<TransformationTemplateResponse> deactivateTemplateVersion(
            @Parameter(description = "Consumer ID") @PathVariable String consumerId,
            @Parameter(description = "Subject") @PathVariable String subject,
            @Parameter(description = "Template version") @PathVariable String version) {

        String correlationId = UUID.randomUUID().toString();
        MDC.put("correlationId", correlationId);
        MDC.put("operation", "deactivateTemplateVersion");
        MDC.put("consumerId", consumerId);
        MDC.put("subject", subject);
        MDC.put("version", version);

        // Store correlationId in request attributes for exception handler access
        ServletRequestAttributes requestAttributes = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
        requestAttributes.setAttribute("correlationId", correlationId, 0);

        try {
            if (requestLoggingEnabled) {
                logger.info("Processing deactivate template version request");
            }

            TransformationTemplateResponse response = versionService.deactivateVersion(consumerId, subject, version);

            if (requestLoggingEnabled) {
                logger.info("Deactivate template version completed successfully");
            }

            return ResponseEntity.ok(response);
        } finally {
            MDC.clear();
        }
    }

    @DeleteMapping("/{consumerId}/subjects/{subject}/templates/versions/{version}")
    @Operation(summary = "Delete template version", description = "Delete a specific version of a transformation template (only if not active)")
    public ResponseEntity<Void> deleteTemplateVersion(
            @Parameter(description = "Consumer ID") @PathVariable String consumerId,
            @Parameter(description = "Subject") @PathVariable String subject,
            @Parameter(description = "Template version") @PathVariable String version) {

        String correlationId = UUID.randomUUID().toString();
        MDC.put("correlationId", correlationId);
        MDC.put("operation", "deleteTemplateVersion");
        MDC.put("consumerId", consumerId);
        MDC.put("subject", subject);
        MDC.put("version", version);

        // Store correlationId in request attributes for exception handler access
        ServletRequestAttributes requestAttributes = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
        requestAttributes.setAttribute("correlationId", correlationId, 0);

        try {
            if (requestLoggingEnabled) {
                logger.info("Processing delete template version request");
            }

            versionService.deleteVersion(consumerId, subject, version);

            if (requestLoggingEnabled) {
                logger.info("Delete template version completed successfully");
            }

            return ResponseEntity.noContent().build();
        } finally {
            MDC.clear();
        }
    }

    // ===== UTILITY ENDPOINTS =====

    @GetMapping("/engines")
    @Operation(summary = "List available transformation engines", description = "Get list of supported transformation engines")
    public ResponseEntity<List<String>> getAvailableEngines() {
        List<String> engines = transformationService.getAvailableEngines();
        return ResponseEntity.ok(engines);
    }




}