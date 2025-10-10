package ru.vegorov.schemaregistry.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import ru.vegorov.schemaregistry.dto.TransformationRequest;
import ru.vegorov.schemaregistry.dto.TransformationResponse;
import ru.vegorov.schemaregistry.dto.TransformationTemplateRequest;
import ru.vegorov.schemaregistry.dto.TransformationTemplateResponse;
import ru.vegorov.schemaregistry.entity.TransformationTemplateEntity;
import ru.vegorov.schemaregistry.exception.ResourceNotFoundException;
import ru.vegorov.schemaregistry.repository.TransformationTemplateRepository;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional
public class TransformationService {

    private final TransformationTemplateRepository templateRepository;
    private final JsltTransformationEngine jsltEngine;
    private final ConsumerService consumerService;

    public TransformationService(TransformationTemplateRepository templateRepository,
                               JsltTransformationEngine jsltEngine,
                               ConsumerService consumerService) {
        this.templateRepository = templateRepository;
        this.jsltEngine = jsltEngine;
        this.consumerService = consumerService;
    }

    /**
     * Transform JSON data for a specific consumer
     */
    public TransformationResponse transform(String consumerId, TransformationRequest request)
        throws TransformationException {

        // Get transformation template for the consumer
        TransformationTemplateEntity template = templateRepository.findByConsumerId(consumerId)
            .orElseThrow(() -> new ResourceNotFoundException(
                String.format("No transformation template found for consumer: %s", consumerId)));

        // Get the appropriate engine (currently only JSLT is supported)
        TransformationEngine engine = getEngine(template.getEngine());

        // Apply transformation
        Map<String, Object> transformedJson = engine.transform(
            request.getCanonicalJson(),
            template.getTemplateExpression()
        );

        return new TransformationResponse(transformedJson);
    }

    /**
     * Create or update transformation template for a consumer
     */
    public TransformationTemplateResponse createOrUpdateTemplate(String consumerId,
                                                               TransformationTemplateRequest request) {

        // Verify consumer exists
        if (!consumerService.consumerExists(consumerId)) {
            throw new ResourceNotFoundException(
                String.format("Consumer not found: %s", consumerId));
        }

        // Validate the transformation expression
        TransformationEngine engine = getEngine(request.getEngine());
        if (!engine.validateExpression(request.getExpression())) {
            throw new IllegalArgumentException("Invalid transformation expression");
        }

        // Check if template already exists
        Optional<TransformationTemplateEntity> existingTemplate =
            templateRepository.findByConsumerId(consumerId);

        TransformationTemplateEntity entity;
        if (existingTemplate.isPresent()) {
            // Update existing template
            entity = existingTemplate.get();
            entity.setEngine(request.getEngine());
            entity.setTemplateExpression(request.getExpression());
            entity.setDescription(request.getDescription());
        } else {
            // Create new template
            entity = new TransformationTemplateEntity(
                consumerId,
                request.getEngine(),
                request.getExpression(),
                request.getDescription()
            );
        }

        TransformationTemplateEntity savedEntity = templateRepository.save(entity);
        return mapToResponse(savedEntity);
    }

    /**
     * Get transformation template for a consumer
     */
    @Transactional(readOnly = true)
    public TransformationTemplateResponse getTemplate(String consumerId) {
        TransformationTemplateEntity entity = templateRepository.findByConsumerId(consumerId)
            .orElseThrow(() -> new ResourceNotFoundException(
                String.format("No transformation template found for consumer: %s", consumerId)));
        return mapToResponse(entity);
    }

    /**
     * Get available transformation engines
     */
    public List<String> getAvailableEngines() {
        // For now, only JSLT is supported
        return List.of("jslt");
    }

    /**
     * Get transformation engine by name
     */
    private TransformationEngine getEngine(String engineName) {
        if ("jslt".equalsIgnoreCase(engineName)) {
            return jsltEngine;
        }
        throw new IllegalArgumentException("Unsupported transformation engine: " + engineName);
    }

    /**
     * Map entity to response DTO
     */
    private TransformationTemplateResponse mapToResponse(TransformationTemplateEntity entity) {
        return new TransformationTemplateResponse(
            entity.getId(),
            entity.getConsumerId(),
            entity.getEngine(),
            entity.getTemplateExpression(),
            entity.getDescription(),
            entity.getCreatedAt(),
            entity.getUpdatedAt()
        );
    }
}