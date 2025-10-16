package ru.vegorov.schemaregistry.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import ru.vegorov.schemaregistry.dto.CompatibilityCheckRequest;
import ru.vegorov.schemaregistry.dto.CompatibilityCheckResponse;
import ru.vegorov.schemaregistry.dto.SchemaRegistrationRequest;
import ru.vegorov.schemaregistry.dto.SchemaResponse;
import ru.vegorov.schemaregistry.dto.SchemaValidationRequest;
import ru.vegorov.schemaregistry.dto.SchemaValidationResponse;
import ru.vegorov.schemaregistry.entity.SchemaEntity;
import ru.vegorov.schemaregistry.entity.SchemaType;
import ru.vegorov.schemaregistry.exception.ResourceNotFoundException;
import ru.vegorov.schemaregistry.exception.SchemaValidationException;
import ru.vegorov.schemaregistry.repository.SchemaRepository;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional
public class SchemaRegistryService {

    private static final Logger logger = LoggerFactory.getLogger(SchemaRegistryService.class);

    private final SchemaRepository schemaRepository;
    private final ObjectMapper objectMapper;

    @Value("${app.logging.business-operations.enabled:true}")
    private boolean businessLoggingEnabled;

    @Value("${app.logging.performance.enabled:true}")
    private boolean performanceLoggingEnabled;

    @Value("${app.logging.performance.slow-threshold-ms:1000}")
    private long slowThresholdMs;

    public SchemaRegistryService(SchemaRepository schemaRepository, ObjectMapper objectMapper) {
        this.schemaRepository = schemaRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * Register a new canonical schema or create a new version
     */
    public SchemaResponse registerCanonicalSchema(SchemaRegistrationRequest request) {
        Instant start = performanceLoggingEnabled ? Instant.now() : null;
        String subject = request.getSubject();

        if (businessLoggingEnabled) {
            logger.info("Registering canonical schema: subject={}, compatibility={}",
                subject, request.getCompatibility());
        }

        try {
            // Get the next version number for canonical schemas
            String nextVersion = getNextVersionForCanonicalSchema(subject);

            if (businessLoggingEnabled) {
                logger.debug("Calculated next version for canonical schema: subject={}, version={}",
                    subject, nextVersion);
            }

            // Create new canonical schema entity
            SchemaEntity schemaEntity = new SchemaEntity(
                subject,
                SchemaType.canonical,
                nextVersion,
                request.getSchema(),
                request.getCompatibility(),
                request.getDescription()
            );

            SchemaEntity savedEntity = schemaRepository.save(schemaEntity);

            if (businessLoggingEnabled) {
                logger.info("Canonical schema registered successfully: subject={}, version={}, id={}",
                    subject, nextVersion, savedEntity.getId());
            }

            if (performanceLoggingEnabled) {
                long duration = Duration.between(start, Instant.now()).toMillis();
                if (duration > slowThresholdMs) {
                    logger.warn("Slow schema registration detected: subject={}, duration={}ms", subject, duration);
                } else {
                    logger.debug("Schema registration performance: subject={}, duration={}ms", subject, duration);
                }
            }

            return mapToResponse(savedEntity);
        } catch (Exception e) {
            if (performanceLoggingEnabled) {
                long duration = Duration.between(start, Instant.now()).toMillis();
                logger.error("Schema registration failed: subject={}, duration={}ms, error={}",
                    subject, duration, e.getMessage(), e);
            }
            throw e;
        }
    }

    /**
     * Register a consumer output schema
     */
    public SchemaResponse registerConsumerOutputSchema(String consumerId, SchemaRegistrationRequest request) {
        String subject = request.getSubject();

        // Get the next version number for consumer output schemas
        String nextVersion = getNextVersionForConsumerOutputSchema(subject, consumerId);

        // Create new consumer output schema entity
        SchemaEntity schemaEntity = new SchemaEntity(
            subject,
            SchemaType.consumer_output,
            consumerId,
            nextVersion,
            request.getSchema(),
            request.getCompatibility(),
            request.getDescription()
        );

        SchemaEntity savedEntity = schemaRepository.save(schemaEntity);
        return mapToResponse(savedEntity);
    }



    /**
     * Get all versions of canonical schemas by subject
     */
    @Transactional(readOnly = true)
    public List<SchemaResponse> getCanonicalSchemasBySubject(String subject) {
        if (businessLoggingEnabled) {
            logger.debug("Retrieving all canonical schema versions: subject={}", subject);
        }

        List<SchemaEntity> entities = schemaRepository.findBySubjectAndSchemaType(subject, SchemaType.canonical);

        List<SchemaResponse> responses = entities.stream()
            .sorted((a, b) -> compareSemver(b.getVersion(), a.getVersion())) // Sort descending
            .map(this::mapToResponse)
            .collect(Collectors.toList());

        if (businessLoggingEnabled) {
            logger.debug("Retrieved {} canonical schema versions: subject={}", responses.size(), subject);
        }

        return responses;
    }

    /**
     * Get all versions of consumer output schemas by subject and consumer
     */
    @Transactional(readOnly = true)
    public List<SchemaResponse> getConsumerOutputSchemasBySubjectAndConsumer(String subject, String consumerId) {
        List<SchemaEntity> entities = schemaRepository.findBySubjectAndSchemaTypeAndConsumerId(subject, SchemaType.consumer_output, consumerId);
        return entities.stream()
            .sorted((a, b) -> compareSemver(b.getVersion(), a.getVersion())) // Sort descending
            .map(this::mapToResponse)
            .collect(Collectors.toList());
    }



    /**
     * Get a specific canonical schema version
     */
    @Transactional(readOnly = true)
    public SchemaResponse getCanonicalSchema(String subject, String version) {
        SchemaEntity entity = schemaRepository.findBySubjectAndSchemaTypeAndVersion(subject, SchemaType.canonical, version)
            .orElseThrow(() -> new ResourceNotFoundException("Canonical schema not found: " + subject + " version " + version));
        return mapToResponse(entity);
    }

    /**
     * Get a specific consumer output schema version
     */
    @Transactional(readOnly = true)
    public SchemaResponse getConsumerOutputSchema(String subject, String consumerId, String version) {
        SchemaEntity entity = schemaRepository.findBySubjectAndSchemaTypeAndConsumerIdAndVersion(subject, SchemaType.consumer_output, consumerId, version)
            .orElseThrow(() -> new ResourceNotFoundException("Consumer output schema not found: " + subject + " consumer " + consumerId + " version " + version));
        return mapToResponse(entity);
    }



    /**
     * Get the latest version of a canonical schema
     */
    @Transactional(readOnly = true)
    public SchemaResponse getLatestCanonicalSchema(String subject) {
        List<SchemaEntity> entities = schemaRepository.findBySubjectAndSchemaType(subject, SchemaType.canonical);
        if (entities.isEmpty()) {
            throw new ResourceNotFoundException("No canonical schemas found for subject: " + subject);
        }
        SchemaEntity entity = entities.stream()
            .max((a, b) -> compareSemver(a.getVersion(), b.getVersion()))
            .orElseThrow(() -> new ResourceNotFoundException("No canonical schemas found for subject: " + subject));
        return mapToResponse(entity);
    }

    /**
     * Get the latest version of a consumer output schema
     */
    @Transactional(readOnly = true)
    public SchemaResponse getLatestConsumerOutputSchema(String subject, String consumerId) {
        List<SchemaEntity> entities = schemaRepository.findBySubjectAndSchemaTypeAndConsumerId(subject, SchemaType.consumer_output, consumerId);
        if (entities.isEmpty()) {
            throw new ResourceNotFoundException("No consumer output schemas found for subject: " + subject + " and consumer: " + consumerId);
        }
        SchemaEntity entity = entities.stream()
            .max((a, b) -> compareSemver(a.getVersion(), b.getVersion()))
            .orElseThrow(() -> new ResourceNotFoundException("No consumer output schemas found for subject: " + subject + " and consumer: " + consumerId));
        return mapToResponse(entity);
    }



    /**
     * Check compatibility of a new canonical schema against the latest version
     */
    @Transactional(readOnly = true)
    public CompatibilityCheckResponse checkCanonicalSchemaCompatibility(CompatibilityCheckRequest request) {
        String subject = request.getSubject();

        if (businessLoggingEnabled) {
            logger.info("Checking canonical schema compatibility: subject={}", subject);
        }

        List<SchemaEntity> entities = schemaRepository.findBySubjectAndSchemaType(subject, SchemaType.canonical);
        Optional<SchemaEntity> latestSchemaOpt = entities.stream()
            .max((a, b) -> compareSemver(a.getVersion(), b.getVersion()));

        if (latestSchemaOpt.isEmpty()) {
            if (businessLoggingEnabled) {
                logger.info("No existing canonical schema found for compatibility check: subject={}", subject);
            }
            // If no existing schema, it's compatible
            return new CompatibilityCheckResponse(true, "No existing schema to check against");
        }

        SchemaEntity latestSchema = latestSchemaOpt.get();
        String compatibilityMode = latestSchema.getCompatibility();

        if (businessLoggingEnabled) {
            logger.debug("Checking compatibility against existing schema: subject={}, existingVersion={}, mode={}",
                subject, latestSchema.getVersion(), compatibilityMode);
        }

        // For now, implement basic compatibility checking
        // In a real implementation, this would use proper JSON Schema compatibility algorithms
        boolean compatible = checkSchemaCompatibility(latestSchema.getSchemaJson(),
                                                      request.getSchema(), compatibilityMode);

        String message = compatible ? "Schema is compatible" : "Schema is not compatible";

        if (businessLoggingEnabled) {
            logger.info("Schema compatibility check completed: subject={}, compatible={}, mode={}",
                subject, compatible, compatibilityMode);
        }

        return new CompatibilityCheckResponse(compatible, message);
    }

    /**
     * Check compatibility of a new consumer output schema against the latest version
     */
    @Transactional(readOnly = true)
    public CompatibilityCheckResponse checkConsumerOutputSchemaCompatibility(String consumerId, CompatibilityCheckRequest request) {
        String subject = request.getSubject();
        List<SchemaEntity> entities = schemaRepository.findBySubjectAndSchemaTypeAndConsumerId(subject, SchemaType.consumer_output, consumerId);
        Optional<SchemaEntity> latestSchemaOpt = entities.stream()
            .max((a, b) -> compareSemver(a.getVersion(), b.getVersion()));

        if (latestSchemaOpt.isEmpty()) {
            // If no existing schema, it's compatible
            return new CompatibilityCheckResponse(true, "No existing consumer output schema to check against");
        }

        SchemaEntity latestSchema = latestSchemaOpt.get();
        String compatibilityMode = latestSchema.getCompatibility();

        // For now, implement basic compatibility checking
        // In a real implementation, this would use proper JSON Schema compatibility algorithms
        boolean compatible = checkSchemaCompatibility(latestSchema.getSchemaJson(),
                                                     request.getSchema(), compatibilityMode);

        String message = compatible ? "Consumer output schema is compatible" : "Consumer output schema is not compatible";

        return new CompatibilityCheckResponse(compatible, message);
    }



    /**
     * Get all unique subjects that have canonical schemas
     */
    @Transactional(readOnly = true)
    public List<String> getAllCanonicalSubjects() {
        return schemaRepository.findAllSubjectsBySchemaType(SchemaType.canonical);
    }

    /**
     * Get all unique subjects that have consumer output schemas
     */
    @Transactional(readOnly = true)
    public List<String> getAllConsumerOutputSubjects() {
        return schemaRepository.findAllSubjectsBySchemaType(SchemaType.consumer_output);
    }

    /**
     * Get all consumers that have output schemas for a subject
     */
    @Transactional(readOnly = true)
    public List<String> getConsumersForSubject(String subject) {
        return schemaRepository.findConsumerIdsBySubjectAndSchemaType(subject, SchemaType.consumer_output);
    }

    /**
     * Get all subjects that have consumer output schemas for a consumer
     */
    @Transactional(readOnly = true)
    public List<String> getSubjectsForConsumer(String consumerId) {
        return schemaRepository.findSubjectsByConsumerIdAndSchemaType(consumerId, SchemaType.consumer_output);
    }



    /**
     * Validate JSON data against a canonical schema
     */
    @Transactional(readOnly = true)
    public SchemaValidationResponse validateJson(SchemaValidationRequest request) {
        return validateJsonAgainstSchema(request, SchemaType.canonical, null);
    }

    /**
     * Validate JSON data against a consumer output schema
     */
    @Transactional(readOnly = true)
    public SchemaValidationResponse validateJsonAgainstConsumerOutputSchema(SchemaValidationRequest request, String consumerId) {
        return validateJsonAgainstSchema(request, SchemaType.consumer_output, consumerId);
    }

    /**
     * Validate JSON data against a schema of specified type
     */
    @Transactional(readOnly = true)
    private SchemaValidationResponse validateJsonAgainstSchema(SchemaValidationRequest request, SchemaType schemaType, String consumerId) {
        String subject = request.getSubject();
        JsonNode jsonData = request.getJsonData();

        if (businessLoggingEnabled) {
            try {
                String jsonString = objectMapper.writeValueAsString(jsonData);
                logger.info("Validating JSON against {} schema: subject={}, dataSize={} chars",
                    schemaType, subject, jsonString.length());
            } catch (Exception e) {
                logger.warn("Could not serialize JSON for size logging: subject={}, error={}", subject, e.getMessage());
                logger.info("Validating JSON against {} schema: subject={}", schemaType, subject);
            }
        }

        // Get the schema to validate against
        SchemaEntity schemaEntity;
            if (request.getVersion() != null) {
                // Validate against specific version
                if (schemaType == SchemaType.canonical) {
                    schemaEntity = schemaRepository.findBySubjectAndSchemaTypeAndVersion(subject, schemaType, request.getVersion())
                        .orElseThrow(() -> new ResourceNotFoundException(
                            String.format("Canonical schema not found: subject=%s, version=%s", subject, request.getVersion())));
                } else {
                if (consumerId == null) {
                    throw new IllegalArgumentException("Consumer ID is required for consumer output schema validation");
                }
                schemaEntity = schemaRepository.findBySubjectAndSchemaTypeAndConsumerIdAndVersion(subject, schemaType, consumerId, request.getVersion())
                    .orElseThrow(() -> new ResourceNotFoundException(
                        String.format("Consumer output schema not found: subject=%s, consumer=%s, version=%s", subject, consumerId, request.getVersion())));
            }
            } else {
                // Validate against latest version
                if (schemaType == SchemaType.canonical) {
                    List<SchemaEntity> entities = schemaRepository.findBySubjectAndSchemaType(subject, schemaType);
                    schemaEntity = entities.stream()
                        .max((a, b) -> compareSemver(a.getVersion(), b.getVersion()))
                        .orElseThrow(() -> new ResourceNotFoundException(
                            String.format("No canonical schemas found for subject: %s", subject)));
                } else {
                if (consumerId == null) {
                    throw new IllegalArgumentException("Consumer ID is required for consumer output schema validation");
                }
                List<SchemaEntity> entities = schemaRepository.findBySubjectAndSchemaTypeAndConsumerId(subject, schemaType, consumerId);
                schemaEntity = entities.stream()
                    .max((a, b) -> compareSemver(a.getVersion(), b.getVersion()))
                    .orElseThrow(() -> new ResourceNotFoundException(
                        String.format("No consumer output schemas found for subject: %s, consumer: %s", subject, consumerId)));
            }
        }

        // Perform validation
        try {
            String schemaJsonString = objectMapper.writeValueAsString(schemaEntity.getSchemaJson());
            JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V4);
            JsonSchema schema = factory.getSchema(schemaJsonString);

            java.util.Set<ValidationMessage> validationMessages = schema.validate(jsonData);

            if (validationMessages.isEmpty()) {
                if (businessLoggingEnabled) {
                    logger.info("JSON validation successful: subject={}, schemaVersion={}, schemaType={}",
                        subject, schemaEntity.getVersion(), schemaType);
                }
                return new SchemaValidationResponse(true, subject, schemaEntity.getVersion());
            } else {
                List<String> errors = validationMessages.stream()
                    .map(ValidationMessage::getMessage)
                    .collect(Collectors.toList());

                if (businessLoggingEnabled) {
                    logger.warn("JSON validation failed: subject={}, schemaVersion={}, schemaType={}, errorCount={}",
                        subject, schemaEntity.getVersion(), schemaType, errors.size());
                }

                return new SchemaValidationResponse(false, subject, schemaEntity.getVersion(), errors);
            }
        } catch (Exception e) {
            if (businessLoggingEnabled) {
                logger.error("JSON validation error: subject={}, schemaVersion={}, schemaType={}, error={}",
                    subject, schemaEntity.getVersion(), schemaType, e.getMessage(), e);
            }
            throw new SchemaValidationException("Failed to validate JSON against schema: " + e.getMessage());
        }
    }

    /**
     * Get the next version number for a canonical schema subject
     */
    private String getNextVersionForCanonicalSchema(String subject) {
        List<SchemaEntity> schemas = schemaRepository.findBySubjectAndSchemaType(subject, SchemaType.canonical);
        if (schemas.isEmpty()) {
            return "1.0.0";
        }
        // Find the highest version by semver comparison
        String maxVersion = schemas.stream()
            .map(SchemaEntity::getVersion)
            .max(this::compareSemver)
            .orElse("1.0.0");
        // Increment patch version
        return incrementPatchVersion(maxVersion);
    }

    /**
     * Get the next version number for a consumer output schema
     */
    private String getNextVersionForConsumerOutputSchema(String subject, String consumerId) {
        List<SchemaEntity> schemas = schemaRepository.findBySubjectAndSchemaTypeAndConsumerId(subject, SchemaType.consumer_output, consumerId);
        if (schemas.isEmpty()) {
            return "1.0.0";
        }
        // Find the highest version by semver comparison
        String maxVersion = schemas.stream()
            .map(SchemaEntity::getVersion)
            .max(this::compareSemver)
            .orElse("1.0.0");
        // Increment patch version
        return incrementPatchVersion(maxVersion);
    }



    /**
     * Compare two semver strings
     */
    private int compareSemver(String v1, String v2) {
        String[] parts1 = v1.split("\\.");
        String[] parts2 = v2.split("\\.");
        for (int i = 0; i < Math.max(parts1.length, parts2.length); i++) {
            int p1 = i < parts1.length ? Integer.parseInt(parts1[i]) : 0;
            int p2 = i < parts2.length ? Integer.parseInt(parts2[i]) : 0;
            if (p1 != p2) return Integer.compare(p1, p2);
        }
        return 0;
    }

    /**
     * Increment patch version of semver string
     */
    private String incrementPatchVersion(String version) {
        String[] parts = version.split("\\.");
        if (parts.length < 3) {
            return version + ".1";
        }
        int patch = Integer.parseInt(parts[2]) + 1;
        return parts[0] + "." + parts[1] + "." + patch;
    }

    /**
     * Basic schema compatibility checking (simplified implementation)
     * In production, this should use proper JSON Schema compatibility libraries
     */
    private boolean checkSchemaCompatibility(Object existingSchema, Object newSchema, String compatibilityMode) {
        // Simplified compatibility check - in real implementation, use libraries like json-schema-diff
        // For now, assume backward compatibility if the new schema has all required fields of the old one

        if ("BACKWARD".equals(compatibilityMode)) {
            // Check if new schema is backward compatible with existing
            // This is a placeholder - real implementation would be more complex
            return true; // Assume compatible for demo
        } else if ("FORWARD".equals(compatibilityMode)) {
            // Forward compatibility check
            return true; // Assume compatible for demo
        } else if ("FULL".equals(compatibilityMode)) {
            // Full compatibility check
            return true; // Assume compatible for demo
        }

        return false;
    }

    /**
     * Map entity to response DTO
     */
    private SchemaResponse mapToResponse(SchemaEntity entity) {
        return new SchemaResponse(
            entity.getId(),
            entity.getSubject(),
            entity.getVersion(),
            entity.getSchemaJson(),
            entity.getCompatibility(),
            entity.getDescription(),
            entity.getCreatedAt(),
            entity.getUpdatedAt()
        );
    }
}
