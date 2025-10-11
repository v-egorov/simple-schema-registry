package ru.vegorov.schemaregistry.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.vegorov.schemaregistry.dto.TransformationRequest;
import ru.vegorov.schemaregistry.dto.TransformationResponse;
import ru.vegorov.schemaregistry.dto.TransformationTemplateRequest;
import ru.vegorov.schemaregistry.dto.TransformationTemplateResponse;
import ru.vegorov.schemaregistry.service.TransformationException;
import ru.vegorov.schemaregistry.service.TransformationService;

import java.util.List;

@RestController
@RequestMapping("/api/consumers")
@Validated
@Tag(name = "JSON Transformation", description = "JSON data transformation for different consumers")
public class TransformationController {

    private final TransformationService transformationService;

    public TransformationController(TransformationService transformationService) {
        this.transformationService = transformationService;
    }

    @PostMapping("/{consumerId}/transform")
    @Operation(summary = "Transform JSON data", description = "Transform canonical JSON data for a specific consumer and subject")
    public ResponseEntity<TransformationResponse> transform(
            @Parameter(description = "Consumer ID") @PathVariable String consumerId,
            @Parameter(description = "Subject") @RequestParam("subject") String subject,
            @Valid @RequestBody TransformationRequest request) throws TransformationException {
        // Set subject in request
        request.setSubject(subject);
        TransformationResponse response = transformationService.transform(consumerId, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/templates/{consumerId}")
    @Operation(summary = "Get transformation template", description = "Retrieve transformation template for a consumer")
    public ResponseEntity<TransformationTemplateResponse> getTemplate(
            @Parameter(description = "Consumer ID") @PathVariable String consumerId) {
        TransformationTemplateResponse response = transformationService.getTemplate(consumerId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/templates/{consumerId}")
    @Operation(summary = "Create/update transformation template", description = "Create or update transformation template for a consumer")
    public ResponseEntity<TransformationTemplateResponse> createOrUpdateTemplate(
            @Parameter(description = "Consumer ID") @PathVariable String consumerId,
            @Valid @RequestBody TransformationTemplateRequest request) {
        TransformationTemplateResponse response =
            transformationService.createOrUpdateTemplate(consumerId, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/engines")
    @Operation(summary = "List available transformation engines", description = "Get list of supported transformation engines")
    public ResponseEntity<List<String>> getAvailableEngines() {
        List<String> engines = transformationService.getAvailableEngines();
        return ResponseEntity.ok(engines);
    }
}