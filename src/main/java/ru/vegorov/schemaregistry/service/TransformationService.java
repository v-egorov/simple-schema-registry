package ru.vegorov.schemaregistry.service;

import com.fasterxml.jackson.databind.ObjectMapper;
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
    private final RouterTransformationEngine routerEngine;
    private final PipelineTransformationEngine pipelineEngine;
    private final ConsumerService consumerService;
    private final ObjectMapper objectMapper;

    public TransformationService(TransformationTemplateRepository templateRepository,
                                JsltTransformationEngine jsltEngine,
                                RouterTransformationEngine routerEngine,
                                PipelineTransformationEngine pipelineEngine,
                                ConsumerService consumerService,
                                ObjectMapper objectMapper) {
        this.templateRepository = templateRepository;
        this.jsltEngine = jsltEngine;
        this.routerEngine = routerEngine;
        this.pipelineEngine = pipelineEngine;
        this.consumerService = consumerService;
        this.objectMapper = objectMapper;
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

        // Prepare the expression/configuration based on engine type
        String expression;
        String configuration;

        try {
            if ("jslt".equalsIgnoreCase(request.getEngine())) {
                // For JSLT engine, use the expression field
                expression = request.getExpression();
                configuration = null;
            } else if ("router".equalsIgnoreCase(request.getEngine())) {
                // For router engine, serialize the router configuration
                if (request.getRouterConfig() == null) {
                    throw new IllegalArgumentException("Router configuration is required for router engine");
                }
                expression = objectMapper.writeValueAsString(request.getRouterConfig());
                configuration = expression; // Store config in both fields for now
            } else if ("pipeline".equalsIgnoreCase(request.getEngine())) {
                // For pipeline engine, serialize the pipeline configuration
                if (request.getPipelineConfig() == null) {
                    throw new IllegalArgumentException("Pipeline configuration is required for pipeline engine");
                }
                expression = objectMapper.writeValueAsString(request.getPipelineConfig());
                configuration = expression; // Store config in both fields for now
            } else {
                throw new IllegalArgumentException("Unsupported engine: " + request.getEngine());
            }
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new IllegalArgumentException("Failed to serialize configuration: " + e.getMessage(), e);
        }

        // Validate the transformation expression/configuration
        TransformationEngine engine = getEngine(request.getEngine());
        if (!engine.validateExpression(expression)) {
            throw new IllegalArgumentException("Invalid transformation configuration");
        }

        // Check if template already exists
        Optional<TransformationTemplateEntity> existingTemplate =
            templateRepository.findByConsumerId(consumerId);

        TransformationTemplateEntity entity;
        if (existingTemplate.isPresent()) {
            // Update existing template
            entity = existingTemplate.get();
            entity.setEngine(request.getEngine());
            entity.setTemplateExpression(expression);
            entity.setConfiguration(configuration);
            entity.setDescription(request.getDescription());
        } else {
            // Create new template
            entity = new TransformationTemplateEntity(
                consumerId,
                request.getEngine(),
                expression,
                request.getDescription()
            );
            entity.setConfiguration(configuration);
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
        return List.of("jslt", "router", "pipeline");
    }

    /**
     * Get transformation engine by name
     */
    private TransformationEngine getEngine(String engineName) {
        if ("jslt".equalsIgnoreCase(engineName)) {
            return jsltEngine;
        } else if ("router".equalsIgnoreCase(engineName)) {
            return routerEngine;
        } else if ("pipeline".equalsIgnoreCase(engineName)) {
            return pipelineEngine;
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
            entity.getConfiguration(),
            entity.getDescription(),
            entity.getCreatedAt(),
            entity.getUpdatedAt()
        );
    }
}