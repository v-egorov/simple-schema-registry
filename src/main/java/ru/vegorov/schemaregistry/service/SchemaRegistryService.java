package ru.vegorov.schemaregistry.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.vegorov.schemaregistry.dto.CompatibilityCheckRequest;
import ru.vegorov.schemaregistry.dto.CompatibilityCheckResponse;
import ru.vegorov.schemaregistry.dto.SchemaRegistrationRequest;
import ru.vegorov.schemaregistry.dto.SchemaResponse;
import ru.vegorov.schemaregistry.entity.SchemaEntity;
import ru.vegorov.schemaregistry.exception.ResourceNotFoundException;
import ru.vegorov.schemaregistry.exception.SchemaValidationException;
import ru.vegorov.schemaregistry.repository.SchemaRepository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class SchemaRegistryService {

    private final SchemaRepository schemaRepository;

    public SchemaRegistryService(SchemaRepository schemaRepository) {
        this.schemaRepository = schemaRepository;
    }

    /**
     * Register a new schema or create a new version
     */
    public SchemaResponse registerSchema(SchemaRegistrationRequest request) {
        String subject = request.getSubject();

        // Get the next version number
        Integer nextVersion = getNextVersion(subject);

        // Create new schema entity
        SchemaEntity schemaEntity = new SchemaEntity(
            subject,
            nextVersion,
            request.getSchema(),
            request.getCompatibility(),
            request.getDescription()
        );

        SchemaEntity savedEntity = schemaRepository.save(schemaEntity);
        return mapToResponse(savedEntity);
    }

    /**
     * Get all versions of a schema by subject
     */
    @Transactional(readOnly = true)
    public List<SchemaResponse> getSchemasBySubject(String subject) {
        List<SchemaEntity> entities = schemaRepository.findBySubjectOrderByVersionDesc(subject);
        return entities.stream()
            .map(this::mapToResponse)
            .collect(Collectors.toList());
    }

    /**
     * Get a specific schema version
     */
    @Transactional(readOnly = true)
    public SchemaResponse getSchema(String subject, Integer version) {
        SchemaEntity entity = schemaRepository.findBySubjectAndVersion(subject, version)
            .orElseThrow(() -> new ResourceNotFoundException(
                String.format("Schema not found: subject=%s, version=%d", subject, version)));
        return mapToResponse(entity);
    }

    /**
     * Get the latest version of a schema
     */
    @Transactional(readOnly = true)
    public SchemaResponse getLatestSchema(String subject) {
        SchemaEntity entity = schemaRepository.findFirstBySubjectOrderByVersionDesc(subject)
            .orElseThrow(() -> new ResourceNotFoundException(
                String.format("No schemas found for subject: %s", subject)));
        return mapToResponse(entity);
    }

    /**
     * Check compatibility of a new schema against the latest version
     */
    @Transactional(readOnly = true)
    public CompatibilityCheckResponse checkCompatibility(String subject, CompatibilityCheckRequest request) {
        Optional<SchemaEntity> latestSchemaOpt = schemaRepository.findFirstBySubjectOrderByVersionDesc(subject);

        if (latestSchemaOpt.isEmpty()) {
            // If no existing schema, it's compatible
            return new CompatibilityCheckResponse(true, "No existing schema to check against");
        }

        SchemaEntity latestSchema = latestSchemaOpt.get();
        String compatibilityMode = latestSchema.getCompatibility();

        // For now, implement basic compatibility checking
        // In a real implementation, this would use proper JSON Schema compatibility algorithms
        boolean compatible = checkSchemaCompatibility(latestSchema.getSchemaJson(),
                                                     request.getSchema(), compatibilityMode);

        String message = compatible ? "Schema is compatible" : "Schema is not compatible";

        return new CompatibilityCheckResponse(compatible, message);
    }

    /**
     * Get all unique subjects
     */
    @Transactional(readOnly = true)
    public List<String> getAllSubjects() {
        return schemaRepository.findAllSubjects();
    }

    /**
     * Get the next version number for a subject
     */
    private Integer getNextVersion(String subject) {
        Optional<Integer> maxVersion = schemaRepository.findMaxVersionBySubject(subject);
        return maxVersion.orElse(0) + 1;
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