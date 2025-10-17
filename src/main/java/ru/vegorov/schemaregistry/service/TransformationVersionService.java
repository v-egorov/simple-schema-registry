package ru.vegorov.schemaregistry.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.vegorov.schemaregistry.dto.SchemaReference;
import ru.vegorov.schemaregistry.dto.TransformationTemplateResponse;
import ru.vegorov.schemaregistry.entity.TransformationTemplateEntity;
import ru.vegorov.schemaregistry.exception.ConflictException;
import ru.vegorov.schemaregistry.exception.ResourceNotFoundException;
import ru.vegorov.schemaregistry.repository.TransformationTemplateRepository;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service for managing transformation template versions
 */
@Service
@Transactional
public class TransformationVersionService {

    private static final Logger logger = LoggerFactory.getLogger(TransformationVersionService.class);

    private final TransformationTemplateRepository templateRepository;

    @Value("${app.logging.business-operations.enabled:true}")
    private boolean businessLoggingEnabled;

    @Value("${app.logging.performance.enabled:true}")
    private boolean performanceLoggingEnabled;

    @Value("${app.logging.performance.slow-threshold-ms:1000}")
    private long slowThresholdMs;

    public TransformationVersionService(TransformationTemplateRepository templateRepository) {
        this.templateRepository = templateRepository;
    }

    /**
     * Activate a specific version of a transformation template
     */
    public TransformationTemplateResponse activateVersion(String consumerId, String subject, String version) {
        Instant start = performanceLoggingEnabled ? Instant.now() : null;

        try {
            // Check if the version exists
            TransformationTemplateEntity template = templateRepository.findByConsumerIdAndSubjectAndVersion(consumerId, subject, version)
                .orElseThrow(() -> new ResourceNotFoundException(
                    String.format("Transformation template version not found: consumer=%s, subject=%s, version=%s",
                        consumerId, subject, version)));

            // If already active, return it
            if (template.getIsActive()) {
                if (performanceLoggingEnabled) {
                    long duration = Duration.between(start, Instant.now()).toMillis();
                    logger.debug("Version already active: consumer={}, subject={}, version={}, duration={}ms",
                        consumerId, subject, version, duration);
                }
                return mapToResponse(template);
            }

            // Deactivate the currently active version (should be only one due to unique constraint)
            Optional<TransformationTemplateEntity> currentlyActive = templateRepository
                .findByConsumerIdAndSubjectAndIsActiveTrue(consumerId, subject);
            if (currentlyActive.isPresent() && !currentlyActive.get().getVersion().equals(version)) {
                currentlyActive.get().setIsActive(false);
                templateRepository.saveAndFlush(currentlyActive.get());
            }

            // Activate this version
            template.setIsActive(true);
            TransformationTemplateEntity savedEntity = templateRepository.save(template);

            if (performanceLoggingEnabled) {
                long duration = Duration.between(start, Instant.now()).toMillis();
                if (duration > slowThresholdMs) {
                    logger.warn("Slow version activation detected: consumer={}, subject={}, version={}, duration={}ms",
                        consumerId, subject, version, duration);
                } else {
                    logger.debug("Version activation performance: consumer={}, subject={}, version={}, duration={}ms",
                        consumerId, subject, version, duration);
                }
            }

            return mapToResponse(savedEntity);
        } catch (Exception e) {
            if (performanceLoggingEnabled) {
                long duration = Duration.between(start, Instant.now()).toMillis();
                logger.error("Version activation failed: consumer={}, subject={}, version={}, duration={}ms, error={}",
                    consumerId, subject, version, duration, e.getMessage(), e);
            } else if (businessLoggingEnabled) {
                logger.error("Version activation failed: consumer={}, subject={}, version={}, error={}",
                    consumerId, subject, version, e.getMessage(), e);
            }
            throw e;
        }
    }

    /**
     * Deactivate a specific version of a transformation template
     */
    public TransformationTemplateResponse deactivateVersion(String consumerId, String subject, String version) {
        Instant start = performanceLoggingEnabled ? Instant.now() : null;

        try {
            TransformationTemplateEntity template = templateRepository.findByConsumerIdAndSubjectAndVersion(consumerId, subject, version)
                .orElseThrow(() -> new ResourceNotFoundException(
                    String.format("Transformation template version not found: consumer=%s, subject=%s, version=%s",
                        consumerId, subject, version)));

            if (!template.getIsActive()) {
                throw new ConflictException(
                    String.format("Version is already inactive: consumer=%s, subject=%s, version=%s",
                        consumerId, subject, version));
            }

            // Check if this is the only version
            long versionCount = templateRepository.countByConsumerIdAndSubject(consumerId, subject);
            if (versionCount <= 1) {
                throw new ConflictException(
                    String.format("Cannot deactivate the only version for consumer=%s, subject=%s", consumerId, subject));
            }

            template.setIsActive(false);
            TransformationTemplateEntity savedEntity = templateRepository.save(template);

            if (performanceLoggingEnabled) {
                long duration = Duration.between(start, Instant.now()).toMillis();
                if (duration > slowThresholdMs) {
                    logger.warn("Slow version deactivation detected: consumer={}, subject={}, version={}, duration={}ms",
                        consumerId, subject, version, duration);
                } else {
                    logger.debug("Version deactivation performance: consumer={}, subject={}, version={}, duration={}ms",
                        consumerId, subject, version, duration);
                }
            }

            return mapToResponse(savedEntity);
        } catch (Exception e) {
            if (performanceLoggingEnabled) {
                long duration = Duration.between(start, Instant.now()).toMillis();
                logger.error("Version deactivation failed: consumer={}, subject={}, version={}, duration={}ms, error={}",
                    consumerId, subject, version, duration, e.getMessage(), e);
            } else if (businessLoggingEnabled) {
                logger.error("Version deactivation failed: consumer={}, subject={}, version={}, error={}",
                    consumerId, subject, version, e.getMessage(), e);
            }
            throw e;
        }
    }

    /**
     * Get the currently active version for a consumer and subject
     */
    @Transactional(readOnly = true)
    public TransformationTemplateResponse getActiveVersion(String consumerId, String subject) {
        Instant start = performanceLoggingEnabled ? Instant.now() : null;

        try {
            TransformationTemplateEntity entity = templateRepository.findByConsumerIdAndSubjectAndIsActiveTrue(consumerId, subject)
                .orElseThrow(() -> new ResourceNotFoundException(
                    String.format("No active version found for consumer: %s, subject: %s", consumerId, subject)));

            if (performanceLoggingEnabled) {
                long duration = Duration.between(start, Instant.now()).toMillis();
                if (duration > slowThresholdMs) {
                    logger.warn("Slow get active version detected: consumer={}, subject={}, duration={}ms",
                        consumerId, subject, duration);
                } else {
                    logger.debug("Get active version performance: consumer={}, subject={}, duration={}ms",
                        consumerId, subject, duration);
                }
            }

            return mapToResponse(entity);
        } catch (Exception e) {
            if (performanceLoggingEnabled) {
                long duration = Duration.between(start, Instant.now()).toMillis();
                logger.error("Get active version failed: consumer={}, subject={}, duration={}ms, error={}",
                    consumerId, subject, duration, e.getMessage(), e);
            } else if (businessLoggingEnabled) {
                logger.error("Get active version failed: consumer={}, subject={}, error={}",
                    consumerId, subject, e.getMessage(), e);
            }
            throw e;
        }
    }

    /**
     * Get version history for a consumer and subject
     */
    @Transactional(readOnly = true)
    public List<TransformationTemplateResponse> getVersionHistory(String consumerId, String subject) {
        Instant start = performanceLoggingEnabled ? Instant.now() : null;

        try {
            List<TransformationTemplateEntity> entities = templateRepository.findByConsumerIdAndSubject(consumerId, subject);
            List<TransformationTemplateResponse> responses = entities.stream()
                .sorted((a, b) -> compareSemver(b.getVersion(), a.getVersion())) // Sort descending
                .map(this::mapToResponse)
                .collect(Collectors.toList());

            if (performanceLoggingEnabled) {
                long duration = Duration.between(start, Instant.now()).toMillis();
                if (duration > slowThresholdMs) {
                    logger.warn("Slow get version history detected: consumer={}, subject={}, count={}, duration={}ms",
                        consumerId, subject, responses.size(), duration);
                } else {
                    logger.debug("Get version history performance: consumer={}, subject={}, count={}, duration={}ms",
                        consumerId, subject, responses.size(), duration);
                }
            }

            return responses;
        } catch (Exception e) {
            if (performanceLoggingEnabled) {
                long duration = Duration.between(start, Instant.now()).toMillis();
                logger.error("Get version history failed: consumer={}, subject={}, duration={}ms, error={}",
                    consumerId, subject, duration, e.getMessage(), e);
            } else if (businessLoggingEnabled) {
                logger.error("Get version history failed: consumer={}, subject={}, error={}",
                    consumerId, subject, e.getMessage(), e);
            }
            throw e;
        }
    }

    /**
     * Delete a specific version (only if not active and not the only version)
     */
    public void deleteVersion(String consumerId, String subject, String version) {
        Instant start = performanceLoggingEnabled ? Instant.now() : null;

        try {
            TransformationTemplateEntity template = templateRepository.findByConsumerIdAndSubjectAndVersion(consumerId, subject, version)
                .orElseThrow(() -> new ResourceNotFoundException(
                    String.format("Transformation template version not found: consumer=%s, subject=%s, version=%s",
                        consumerId, subject, version)));

            if (template.getIsActive()) {
                throw new ConflictException(
                    String.format("Cannot delete active version: consumer=%s, subject=%s, version=%s",
                        consumerId, subject, version));
            }

            long versionCount = templateRepository.countByConsumerIdAndSubject(consumerId, subject);
            if (versionCount <= 1) {
                throw new ConflictException(
                    String.format("Cannot delete the only version for consumer=%s, subject=%s", consumerId, subject));
            }

            templateRepository.delete(template);

            if (performanceLoggingEnabled) {
                long duration = Duration.between(start, Instant.now()).toMillis();
                if (duration > slowThresholdMs) {
                    logger.warn("Slow version deletion detected: consumer={}, subject={}, version={}, duration={}ms",
                        consumerId, subject, version, duration);
                } else {
                    logger.debug("Version deletion performance: consumer={}, subject={}, version={}, duration={}ms",
                        consumerId, subject, version, duration);
                }
            }
        } catch (Exception e) {
            if (performanceLoggingEnabled) {
                long duration = Duration.between(start, Instant.now()).toMillis();
                logger.error("Version deletion failed: consumer={}, subject={}, version={}, duration={}ms, error={}",
                    consumerId, subject, version, duration, e.getMessage(), e);
            } else if (businessLoggingEnabled) {
                logger.error("Version deletion failed: consumer={}, subject={}, version={}, error={}",
                    consumerId, subject, version, e.getMessage(), e);
            }
            throw e;
        }
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
            int p1 = i < parts1.length ? Integer.parseInt(parts1[i]) : 0;
            int p2 = i < parts2.length ? Integer.parseInt(parts2[i]) : 0;
            if (p1 != p2) return Integer.compare(p1, p2);
        }
        return 0;
    }
 }