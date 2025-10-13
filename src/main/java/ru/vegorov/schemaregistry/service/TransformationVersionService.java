package ru.vegorov.schemaregistry.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.vegorov.schemaregistry.dto.SchemaReference;
import ru.vegorov.schemaregistry.dto.TransformationTemplateResponse;
import ru.vegorov.schemaregistry.entity.TransformationTemplateEntity;
import ru.vegorov.schemaregistry.exception.ConflictException;
import ru.vegorov.schemaregistry.exception.ResourceNotFoundException;
import ru.vegorov.schemaregistry.repository.TransformationTemplateRepository;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for managing transformation template versions
 */
@Service
@Transactional
public class TransformationVersionService {

    private final TransformationTemplateRepository templateRepository;

    public TransformationVersionService(TransformationTemplateRepository templateRepository) {
        this.templateRepository = templateRepository;
    }

    /**
     * Activate a specific version of a transformation template
     */
    public TransformationTemplateResponse activateVersion(String consumerId, String subject, String version) {
        // Check if the version exists
        TransformationTemplateEntity template = templateRepository.findByConsumerIdAndSubjectAndVersion(consumerId, subject, version)
            .orElseThrow(() -> new ResourceNotFoundException(
                String.format("Transformation template version not found: consumer=%s, subject=%s, version=%s",
                    consumerId, subject, version)));

        // If already active, return it
        if (template.getIsActive()) {
            return mapToResponse(template);
        }

        // Deactivate all other versions
        deactivateOtherVersions(consumerId, subject, version);

        // Activate this version
        template.setIsActive(true);
        TransformationTemplateEntity savedEntity = templateRepository.save(template);

        return mapToResponse(savedEntity);
    }

    /**
     * Deactivate a specific version of a transformation template
     */
    public TransformationTemplateResponse deactivateVersion(String consumerId, String subject, String version) {
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

        return mapToResponse(savedEntity);
    }

    /**
     * Get the currently active version for a consumer and subject
     */
    @Transactional(readOnly = true)
    public TransformationTemplateResponse getActiveVersion(String consumerId, String subject) {
        TransformationTemplateEntity entity = templateRepository.findByConsumerIdAndSubjectAndIsActiveTrue(consumerId, subject)
            .orElseThrow(() -> new ResourceNotFoundException(
                String.format("No active version found for consumer: %s, subject: %s", consumerId, subject)));
        return mapToResponse(entity);
    }

    /**
     * Get version history for a consumer and subject
     */
    @Transactional(readOnly = true)
    public List<TransformationTemplateResponse> getVersionHistory(String consumerId, String subject) {
        List<TransformationTemplateEntity> entities = templateRepository.findByConsumerIdAndSubjectOrderByVersionDesc(consumerId, subject);
        return entities.stream()
            .map(this::mapToResponse)
            .collect(Collectors.toList());
    }

    /**
     * Delete a specific version (only if not active and not the only version)
     */
    public void deleteVersion(String consumerId, String subject, String version) {
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
}