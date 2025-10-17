package ru.vegorov.schemaregistry.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import ru.vegorov.schemaregistry.dto.SchemaReference;
import ru.vegorov.schemaregistry.dto.TransformationRequest;
import ru.vegorov.schemaregistry.dto.TransformationResponse;
import ru.vegorov.schemaregistry.dto.TransformationTemplateRequest;
import ru.vegorov.schemaregistry.dto.TransformationTemplateResponse;
import ru.vegorov.schemaregistry.entity.SchemaEntity;
import ru.vegorov.schemaregistry.entity.SchemaType;
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

    private static final Logger logger = LoggerFactory.getLogger(TransformationService.class);

    private final TransformationTemplateRepository templateRepository;
    private final SchemaRepository schemaRepository;
    private final JsltTransformationEngine jsltEngine;
    private final RouterTransformationEngine routerEngine;
    private final PipelineTransformationEngine pipelineEngine;
    private final ConsumerService consumerService;
    private final ObjectMapper objectMapper;
    private final JsltFunctionRegistry functionRegistry;

    @Value("${app.logging.business-operations.enabled:true}")
    private boolean businessLoggingEnabled;

    @Value("${app.logging.performance.enabled:true}")
    private boolean performanceLoggingEnabled;

    @Value("${app.logging.performance.slow-threshold-ms:1000}")
    private long slowThresholdMs;

    public TransformationService(TransformationTemplateRepository templateRepository,
                                 SchemaRepository schemaRepository,
                                 JsltTransformationEngine jsltEngine,
                                 RouterTransformationEngine routerEngine,
                                 PipelineTransformationEngine pipelineEngine,
                                 ConsumerService consumerService,
                                 ObjectMapper objectMapper,
                                 JsltFunctionRegistry functionRegistry) {
        this.templateRepository = templateRepository;
        this.schemaRepository = schemaRepository;
        this.jsltEngine = jsltEngine;
        this.routerEngine = routerEngine;
        this.pipelineEngine = pipelineEngine;
        this.consumerService = consumerService;
        this.objectMapper = objectMapper;
        this.functionRegistry = functionRegistry;
    }

    /**
     * Transform JSON data for a specific consumer and subject
     */
    public TransformationResponse transform(String consumerId, TransformationRequest request)
        throws TransformationException {

        Instant start = performanceLoggingEnabled ? Instant.now() : null;
        String subject = request.getSubject();

        if (businessLoggingEnabled) {
            logger.info("Starting transformation: consumer={}, subject={}, requestedVersion={}",
                consumerId, subject, request.getTransformationVersion());
        }

        // Validate consumer exists
        consumerService.getConsumer(consumerId);

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

        if (businessLoggingEnabled) {
            logger.debug("Using transformation template: consumer={}, subject={}, templateVersion={}, engine={}",
                consumerId, subject, template.getVersion(), template.getEngine());
        }

        // Get the appropriate engine (currently only JSLT is supported)
        TransformationEngine engine = getEngine(template.getEngine());

        // Apply transformation
        Map<String, Object> transformedJson;
        try {
            if (engine instanceof JsltTransformationEngine && functionRegistry != null) {
                // Use JSLT engine with function registry for custom functions
                if (businessLoggingEnabled) {
                    logger.debug("Applying JSLT transformation with function registry: consumer={}, subject={}",
                        consumerId, subject);
                }
                transformedJson = ((JsltTransformationEngine) engine).transform(
                    request.getCanonicalJson(),
                    template.getTemplateExpression(),
                    functionRegistry
                );
            } else {
                // Fallback to standard transformation
                if (businessLoggingEnabled) {
                    logger.debug("Applying standard transformation: consumer={}, subject={}, engine={}",
                        consumerId, subject, engine.getClass().getSimpleName());
                }
                transformedJson = engine.transform(
                    request.getCanonicalJson(),
                    template.getTemplateExpression()
                );
            }

            if (businessLoggingEnabled) {
                logger.info("Transformation completed successfully: consumer={}, subject={}, outputSize={}",
                    consumerId, subject, transformedJson.size());
            }

            if (performanceLoggingEnabled) {
                long duration = Duration.between(start, Instant.now()).toMillis();
                if (duration > slowThresholdMs) {
                    logger.warn("Slow transformation detected: consumer={}, subject={}, engine={}, duration={}ms",
                        consumerId, subject, template.getEngine(), duration);
                } else {
                    logger.debug("Transformation performance: consumer={}, subject={}, duration={}ms",
                        consumerId, subject, duration);
                }
            }

        } catch (Exception e) {
            if (performanceLoggingEnabled) {
                long duration = Duration.between(start, Instant.now()).toMillis();
                logger.error("Transformation failed: consumer={}, subject={}, engine={}, duration={}ms, error={}",
                    consumerId, subject, template.getEngine(), duration, e.getMessage(), e);
            } else if (businessLoggingEnabled) {
                logger.error("Transformation failed: consumer={}, subject={}, engine={}, error={}",
                    consumerId, subject, template.getEngine(), e.getMessage(), e);
            }
            throw e;
        }

        return new TransformationResponse(transformedJson, subject);
    }

    /**
     * Create a new transformation template version for a consumer and subject
     */
    public TransformationTemplateResponse createTemplateVersion(String consumerId,
                                                                TransformationTemplateRequest request) {
        Instant start = performanceLoggingEnabled ? Instant.now() : null;

        if (businessLoggingEnabled) {
            logger.info("Creating transformation template version: consumer={}, version={}, engine={}",
                consumerId, request.getVersion(), request.getEngine());
        }

        try {
            // Verify consumer exists
            if (!consumerService.consumerExists(consumerId)) {
                throw new ResourceNotFoundException(
                    String.format("Consumer not found: %s", consumerId));
            }

            // Validate input and output schemas exist
            SchemaEntity inputSchema = resolveSchemaReference(request.getInputSchema(), SchemaType.canonical);
            SchemaEntity outputSchema = resolveSchemaReference(request.getOutputSchema(), SchemaType.consumer_output);

            // The subject is derived from the input schema
            String subject = inputSchema.getSubject();

            if (businessLoggingEnabled) {
                logger.debug("Resolved schemas for template creation: consumer={}, subject={}, inputSchemaId={}, outputSchemaId={}",
                    consumerId, subject, inputSchema.getId(), outputSchema.getId());
            }

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
            boolean isValid;
            if (engine instanceof JsltTransformationEngine && functionRegistry != null) {
                // Use JSLT engine with function registry for validation
                if (businessLoggingEnabled) {
                    logger.debug("Validating JSLT expression with function registry: consumer={}, subject={}",
                        consumerId, subject);
                }
                isValid = ((JsltTransformationEngine) engine).validateExpression(expression, functionRegistry);
            } else {
                // Fallback to standard validation
                if (businessLoggingEnabled) {
                    logger.debug("Validating expression with {} engine: consumer={}, subject={}",
                        engine.getClass().getSimpleName(), consumerId, subject);
                }
                isValid = engine.validateExpression(expression);
            }

            if (!isValid) {
                if (businessLoggingEnabled) {
                    logger.error("Invalid transformation configuration: consumer={}, subject={}, engine={}",
                        consumerId, subject, request.getEngine());
                }
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

            if (businessLoggingEnabled) {
                logger.info("Transformation template version created successfully: consumer={}, subject={}, version={}, active={}",
                    consumerId, subject, request.getVersion(), isActive);
            }

            if (performanceLoggingEnabled) {
                long duration = Duration.between(start, Instant.now()).toMillis();
                if (duration > slowThresholdMs) {
                    logger.warn("Slow template version creation detected: consumer={}, subject={}, version={}, engine={}, duration={}ms",
                        consumerId, subject, request.getVersion(), request.getEngine(), duration);
                } else {
                    logger.debug("Template version creation performance: consumer={}, subject={}, version={}, engine={}, duration={}ms",
                        consumerId, subject, request.getVersion(), request.getEngine(), duration);
                }
            }

            if (performanceLoggingEnabled) {
                long duration = Duration.between(start, Instant.now()).toMillis();
                if (duration > slowThresholdMs) {
                    logger.warn("Slow template version creation detected: consumer={}, subject={}, version={}, engine={}, duration={}ms",
                        consumerId, subject, request.getVersion(), request.getEngine(), duration);
                } else {
                    logger.debug("Template version creation performance: consumer={}, subject={}, version={}, engine={}, duration={}ms",
                        consumerId, subject, request.getVersion(), request.getEngine(), duration);
                }
            }

            return mapToResponse(savedEntity);
        } catch (Exception e) {
            if (performanceLoggingEnabled) {
                long duration = Duration.between(start, Instant.now()).toMillis();
                logger.error("Template version creation failed: consumer={}, version={}, engine={}, duration={}ms, error={}",
                    consumerId, request.getVersion(), request.getEngine(), duration, e.getMessage(), e);
            } else if (businessLoggingEnabled) {
                logger.error("Template version creation failed: consumer={}, version={}, engine={}, error={}",
                    consumerId, request.getVersion(), request.getEngine(), e.getMessage(), e);
            }
            throw e;
        }
    }

    /**
     * Get the latest version for a consumer and subject
     */
    private String getLatestVersionForConsumerAndSubject(String consumerId, String subject) {
        List<TransformationTemplateEntity> entities = templateRepository.findByConsumerIdAndSubject(consumerId, subject);
        return entities.stream()
            .map(TransformationTemplateEntity::getVersion)
            .max(this::compareSemver)
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
        List<TransformationTemplateEntity> entities = templateRepository.findByConsumerIdAndSubject(consumerId, subject);
        return entities.stream()
            .sorted((a, b) -> compareSemver(b.getVersion(), a.getVersion())) // Sort descending
            .map(this::mapToResponse)
            .collect(Collectors.toList());
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
     * Resolve a schema reference to a SchemaEntity
     */
    private SchemaEntity resolveSchemaReference(SchemaReference ref, SchemaType expectedType) {
        if (ref == null) {
            throw new IllegalArgumentException("Schema reference cannot be null");
        }

        SchemaEntity schema;
        if (ref.getVersion() != null) {
            // Find specific version
            if (expectedType == SchemaType.canonical) {
                if (ref.getConsumerId() != null) {
                    throw new IllegalArgumentException("Consumer ID should not be specified for canonical schemas");
                }
                schema = schemaRepository.findBySubjectAndSchemaTypeAndVersion(ref.getSubject(), expectedType, ref.getVersion())
                    .orElseThrow(() -> new ResourceNotFoundException(
                        String.format("Canonical schema not found: subject=%s, version=%s", ref.getSubject(), ref.getVersion())));
            } else { // consumer_output
                if (ref.getConsumerId() == null) {
                    throw new IllegalArgumentException("Consumer ID is required for consumer output schemas");
                }
                schema = schemaRepository.findBySubjectAndSchemaTypeAndConsumerIdAndVersion(
                        ref.getSubject(), expectedType, ref.getConsumerId(), ref.getVersion())
                    .orElseThrow(() -> new ResourceNotFoundException(
                        String.format("Consumer output schema not found: subject=%s, consumer=%s, version=%s",
                            ref.getSubject(), ref.getConsumerId(), ref.getVersion())));
            }
        } else {
            // Find latest version
            if (expectedType == SchemaType.canonical) {
                if (ref.getConsumerId() != null) {
                    throw new IllegalArgumentException("Consumer ID should not be specified for canonical schemas");
                }
                List<SchemaEntity> entities = schemaRepository.findBySubjectAndSchemaType(ref.getSubject(), expectedType);
                schema = entities.stream()
                    .max((a, b) -> compareSemver(a.getVersion(), b.getVersion()))
                    .orElseThrow(() -> new ResourceNotFoundException(
                        String.format("No canonical schemas found for subject: %s", ref.getSubject())));
            } else { // consumer_output
                if (ref.getConsumerId() == null) {
                    throw new IllegalArgumentException("Consumer ID is required for consumer output schemas");
                }
                List<SchemaEntity> entities = schemaRepository.findBySubjectAndSchemaTypeAndConsumerId(ref.getSubject(), expectedType, ref.getConsumerId());
                schema = entities.stream()
                    .max((a, b) -> compareSemver(a.getVersion(), b.getVersion()))
                    .orElseThrow(() -> new ResourceNotFoundException(
                        String.format("No consumer output schemas found for subject: %s, consumer: %s",
                            ref.getSubject(), ref.getConsumerId())));
            }
        }

        return schema;
    }

    /**
     * Map entity to response DTO
     */
    private TransformationTemplateResponse mapToResponse(TransformationTemplateEntity entity) {
        // Create schema references from the entity relationships
        SchemaReference inputSchemaRef = null;
        if (entity.getInputSchema() != null) {
            inputSchemaRef = new SchemaReference(
                entity.getInputSchema().getSubject(),
                entity.getInputSchema().getVersion()
            );
        }

        SchemaReference outputSchemaRef = null;
        if (entity.getOutputSchema() != null) {
            outputSchemaRef = new SchemaReference(
                entity.getOutputSchema().getSubject(),
                entity.getOutputSchema().getConsumerId(),
                entity.getOutputSchema().getVersion()
            );
        }

        return new TransformationTemplateResponse(
            entity.getId(),
            entity.getConsumerId(),
            entity.getSubject(),
            entity.getVersion(),
            entity.getEngine(),
            inputSchemaRef,
            outputSchemaRef,
            entity.getIsActive(),
            entity.getTemplateExpression(),
            entity.getConfiguration(),
            entity.getDescription(),
            entity.getCreatedAt(),
            entity.getUpdatedAt()
         );
     }

    /**
     * Compare two semver strings
     */
    private int compareSemver(String v1, String v2) {
        String[] parts1 = v1.split("\\.");
        String[] parts2 = v2.split("\\.");
        for (int i = 0; i < Math.max(parts1.length, parts2.length); i++) {
            String part1 = i < parts1.length ? parts1[i] : "0";
            String part2 = i < parts2.length ? parts2[i] : "0";

            // Extract numeric part (handle suffixes like -beta)
            String numPart1 = part1.split("-")[0];
            String numPart2 = part2.split("-")[0];

            try {
                int p1 = Integer.parseInt(numPart1);
                int p2 = Integer.parseInt(numPart2);
                if (p1 != p2) return Integer.compare(p1, p2);
            } catch (NumberFormatException e) {
                // If not numeric, compare as strings
                int cmp = numPart1.compareTo(numPart2);
                if (cmp != 0) return cmp;
            }

            // If numeric parts are equal, compare suffixes
            String suffix1 = part1.contains("-") ? part1.substring(part1.indexOf("-")) : "";
            String suffix2 = part2.contains("-") ? part2.substring(part2.indexOf("-")) : "";
            if (!suffix1.equals(suffix2)) {
                return suffix1.compareTo(suffix2);
            }
        }
        return 0;
    }
 }