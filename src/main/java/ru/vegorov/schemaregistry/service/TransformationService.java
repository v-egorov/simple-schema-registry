package ru.vegorov.schemaregistry.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import ru.vegorov.schemaregistry.dto.TransformationRequest;
import ru.vegorov.schemaregistry.dto.TransformationResponse;
import ru.vegorov.schemaregistry.dto.TransformationTemplateRequest;
import ru.vegorov.schemaregistry.dto.TransformationTemplateResponse;
import ru.vegorov.schemaregistry.entity.SchemaEntity;
import ru.vegorov.schemaregistry.entity.TransformationTemplateEntity;
import ru.vegorov.schemaregistry.exception.ConflictException;
import ru.vegorov.schemaregistry.exception.ResourceNotFoundException;
import ru.vegorov.schemaregistry.repository.SchemaRepository;
import ru.vegorov.schemaregistry.repository.TransformationTemplateRepository;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional
public class TransformationService {

    private final TransformationTemplateRepository templateRepository;
    private final SchemaRepository schemaRepository;
    private final JsltTransformationEngine jsltEngine;
    private final RouterTransformationEngine routerEngine;
    private final PipelineTransformationEngine pipelineEngine;
    private final ConsumerService consumerService;
    private final ObjectMapper objectMapper;

    public TransformationService(TransformationTemplateRepository templateRepository,
                                SchemaRepository schemaRepository,
                                JsltTransformationEngine jsltEngine,
                                RouterTransformationEngine routerEngine,
                                PipelineTransformationEngine pipelineEngine,
                                ConsumerService consumerService,
                                ObjectMapper objectMapper) {
        this.templateRepository = templateRepository;
        this.schemaRepository = schemaRepository;
        this.jsltEngine = jsltEngine;
        this.routerEngine = routerEngine;
        this.pipelineEngine = pipelineEngine;
        this.consumerService = consumerService;
        this.objectMapper = objectMapper;
    }

    /**
     * Transform JSON data for a specific consumer and subject
     */
    public TransformationResponse transform(String consumerId, TransformationRequest request)
        throws TransformationException {

        String subject = request.getSubject();

        // Validate consumer is registered for the subject
        consumerService.validateConsumerSubject(consumerId, subject);

        // Get transformation template - use specific version if provided, otherwise use active
        TransformationTemplateEntity template;
        if (request.getTransformationVersion() != null) {
            template = templateRepository.findByConsumerIdAndSubjectAndVersion(consumerId, subject, request.getTransformationVersion())
                .orElseThrow(() -> new ResourceNotFoundException(
                    String.format("Transformation template version not found: consumer=%s, subject=%s, version=%s",
                        consumerId, subject, request.getTransformationVersion())));
        } else {
            template = templateRepository.findByConsumerIdAndSubjectAndIsActiveTrue(consumerId, subject)
                .orElseThrow(() -> new ResourceNotFoundException(
                    String.format("No active transformation template found for consumer: %s, subject: %s", consumerId, subject)));
        }

        // Get the appropriate engine (currently only JSLT is supported)
        TransformationEngine engine = getEngine(template.getEngine());

        // Apply transformation
        Map<String, Object> transformedJson = engine.transform(
            request.getCanonicalJson(),
            template.getTemplateExpression()
        );

        return new TransformationResponse(transformedJson, subject);
    }

    /**
     * Create a new transformation template version for a consumer and subject
     */
    public TransformationTemplateResponse createTemplateVersion(String consumerId,
                                                               TransformationTemplateRequest request) {

        // Verify consumer exists
        if (!consumerService.consumerExists(consumerId)) {
            throw new ResourceNotFoundException(
                String.format("Consumer not found: %s", consumerId));
        }

        // Validate input and output schemas exist
        SchemaEntity inputSchema = schemaRepository.findById(request.getInputSchemaId())
            .orElseThrow(() -> new ResourceNotFoundException(
                String.format("Input schema not found: %s", request.getInputSchemaId())));

        SchemaEntity outputSchema = schemaRepository.findById(request.getOutputSchemaId())
            .orElseThrow(() -> new ResourceNotFoundException(
                String.format("Output schema not found: %s", request.getOutputSchemaId())));

        // The subject is derived from the input schema
        String subject = inputSchema.getSubject();

        // Check if this version already exists
        if (templateRepository.existsByConsumerIdAndSubjectAndVersion(consumerId, subject, request.getVersion())) {
            throw new ConflictException(
                String.format("Transformation template version already exists: consumer=%s, subject=%s, version=%s",
                    consumerId, subject, request.getVersion()));
        }

        // Prepare the expression/configuration based on engine type
        String expression;
        String configuration;

        try {
            if ("jslt".equalsIgnoreCase(request.getEngine())) {
                // For JSLT engine, use the expression field
                expression = request.getExpression();
                if (expression == null || expression.trim().isEmpty()) {
                    throw new IllegalArgumentException("Expression is required for JSLT engine");
                }
                configuration = null;
            } else if ("router".equalsIgnoreCase(request.getEngine())) {
                // For router engine, serialize the router configuration
                if (request.getRouterConfig() == null) {
                    throw new IllegalArgumentException("Router configuration is required for router engine");
                }
                configuration = objectMapper.writeValueAsString(request.getRouterConfig());
                expression = configuration; // Use the same config as expression to satisfy validation
            } else if ("pipeline".equalsIgnoreCase(request.getEngine())) {
                // For pipeline engine, serialize the pipeline configuration
                if (request.getPipelineConfig() == null) {
                    throw new IllegalArgumentException("Pipeline configuration is required for pipeline engine");
                }
                configuration = objectMapper.writeValueAsString(request.getPipelineConfig());
                expression = configuration; // Use the same config as expression to satisfy validation
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

        // Determine if this should be the active version
        boolean isActive = !templateRepository.existsByConsumerIdAndSubject(consumerId, subject) ||
                          request.getVersion().equals(getLatestVersionForConsumerAndSubject(consumerId, subject));

        // Create new template version
        TransformationTemplateEntity entity = new TransformationTemplateEntity(
            consumerId,
            subject,
            request.getVersion(),
            request.getEngine(),
            expression,
            configuration,
            inputSchema,
            outputSchema,
            isActive,
            request.getDescription()
        );

        TransformationTemplateEntity savedEntity = templateRepository.save(entity);

        // If this is active, deactivate other versions
        if (isActive) {
            deactivateOtherVersions(consumerId, subject, request.getVersion());
        }

        return mapToResponse(savedEntity);
    }

    /**
     * Get the latest version for a consumer and subject
     */
    private String getLatestVersionForConsumerAndSubject(String consumerId, String subject) {
        return templateRepository.findFirstByConsumerIdAndSubjectOrderByVersionDesc(consumerId, subject)
            .map(TransformationTemplateEntity::getVersion)
            .orElse("0.0.0");
    }

    /**
     * Deactivate all other versions for a consumer and subject except the specified version
     */
    private void deactivateOtherVersions(String consumerId, String subject, String activeVersion) {
        List<TransformationTemplateEntity> otherVersions = templateRepository
            .findByConsumerIdAndSubjectAndIsActiveTrue(consumerId, subject)
            .stream()
            .filter(template -> !template.getVersion().equals(activeVersion))
            .collect(Collectors.toList());

        for (TransformationTemplateEntity template : otherVersions) {
            template.setIsActive(false);
            templateRepository.save(template);
        }
    }

    /**
     * Create or update transformation template for a consumer - LEGACY METHOD
     * @deprecated Use createTemplateVersion instead
     */
    @Deprecated
    public TransformationTemplateResponse createOrUpdateTemplate(String consumerId,
                                                                TransformationTemplateRequest request) {
        // For backward compatibility, assume subject from input schema and use version from request
        return createTemplateVersion(consumerId, request);
    }

    /**
     * Get the active transformation template for a consumer and subject
     */
    @Transactional(readOnly = true)
    public TransformationTemplateResponse getActiveTemplate(String consumerId, String subject) {
        TransformationTemplateEntity entity = templateRepository.findByConsumerIdAndSubjectAndIsActiveTrue(consumerId, subject)
            .orElseThrow(() -> new ResourceNotFoundException(
                String.format("No active transformation template found for consumer: %s, subject: %s", consumerId, subject)));
        return mapToResponse(entity);
    }

    /**
     * Get a specific transformation template version for a consumer and subject
     */
    @Transactional(readOnly = true)
    public TransformationTemplateResponse getTemplateVersion(String consumerId, String subject, String version) {
        TransformationTemplateEntity entity = templateRepository.findByConsumerIdAndSubjectAndVersion(consumerId, subject, version)
            .orElseThrow(() -> new ResourceNotFoundException(
                String.format("Transformation template not found: consumer=%s, subject=%s, version=%s", consumerId, subject, version)));
        return mapToResponse(entity);
    }

    /**
     * Get all transformation template versions for a consumer and subject
     */
    @Transactional(readOnly = true)
    public List<TransformationTemplateResponse> getTemplateVersions(String consumerId, String subject) {
        List<TransformationTemplateEntity> entities = templateRepository.findByConsumerIdAndSubjectOrderByVersionDesc(consumerId, subject);
        return entities.stream()
            .map(this::mapToResponse)
            .collect(Collectors.toList());
    }

    /**
     * Get transformation template for a consumer - LEGACY METHOD
     * @deprecated Use getActiveTemplate with subject parameter
     */
    @Deprecated
    @Transactional(readOnly = true)
    public TransformationTemplateResponse getTemplate(String consumerId) {
        // For backward compatibility, get the first subject for this consumer
        List<String> subjects = templateRepository.findSubjectsByConsumerId(consumerId);
        if (subjects.isEmpty()) {
            throw new ResourceNotFoundException(
                String.format("No transformation templates found for consumer: %s", consumerId));
        }
        return getActiveTemplate(consumerId, subjects.get(0));
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
            entity.getSubject(),
            entity.getVersion(),
            entity.getEngine(),
            entity.getInputSchema() != null ? entity.getInputSchema().getId() : null,
            entity.getOutputSchema() != null ? entity.getOutputSchema().getId() : null,
            entity.getIsActive(),
            entity.getTemplateExpression(),
            entity.getConfiguration(),
            entity.getDescription(),
            entity.getCreatedAt(),
            entity.getUpdatedAt()
        );
    }
}