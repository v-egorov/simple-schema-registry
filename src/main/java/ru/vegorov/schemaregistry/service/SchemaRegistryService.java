package ru.vegorov.schemaregistry.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.vegorov.schemaregistry.dto.CompatibilityCheckRequest;
import ru.vegorov.schemaregistry.dto.CompatibilityCheckResponse;
import ru.vegorov.schemaregistry.dto.SchemaRegistrationRequest;
import ru.vegorov.schemaregistry.dto.SchemaResponse;
import ru.vegorov.schemaregistry.dto.SchemaValidationRequest;
import ru.vegorov.schemaregistry.dto.SchemaValidationResponse;
import ru.vegorov.schemaregistry.entity.SchemaEntity;
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

    private final SchemaRepository schemaRepository;
    private final ObjectMapper objectMapper;

    public SchemaRegistryService(SchemaRepository schemaRepository, ObjectMapper objectMapper) {
        this.schemaRepository = schemaRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * Register a new schema or create a new version
     */
    public SchemaResponse registerSchema(SchemaRegistrationRequest request) {
        String subject = request.getSubject();

        // Get the next version number
        String nextVersion = getNextVersion(subject);

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
    public SchemaResponse getSchema(String subject, String version) {
        SchemaEntity entity = schemaRepository.findBySubjectAndVersion(subject, version)
            .orElseThrow(() -> new ResourceNotFoundException(
                String.format("Schema not found: subject=%s, version=%s", subject, version)));
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
    public CompatibilityCheckResponse checkCompatibility(CompatibilityCheckRequest request) {
        String subject = request.getSubject();
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
     * Validate JSON data against a schema
     */
    @Transactional(readOnly = true)
    public SchemaValidationResponse validateJson(SchemaValidationRequest request) {
        String subject = request.getSubject();
        JsonNode jsonData = request.getJsonData();

        // Get the schema to validate against
        SchemaEntity schemaEntity;
        if (request.getVersion() != null) {
            // Validate against specific version
            schemaEntity = schemaRepository.findBySubjectAndVersion(subject, request.getVersion())
                .orElseThrow(() -> new ResourceNotFoundException(
                    String.format("Schema not found: subject=%s, version=%s", subject, request.getVersion())));
        } else {
            // Validate against latest version
            schemaEntity = schemaRepository.findFirstBySubjectOrderByVersionDesc(subject)
                .orElseThrow(() -> new ResourceNotFoundException(
                    String.format("No schemas found for subject: %s", subject)));
        }

        // Perform validation
        try {
            String schemaJsonString = objectMapper.writeValueAsString(schemaEntity.getSchemaJson());
            JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7);
            JsonSchema schema = factory.getSchema(schemaJsonString);

            java.util.Set<ValidationMessage> validationMessages = schema.validate(jsonData);

            if (validationMessages.isEmpty()) {
                return new SchemaValidationResponse(true, subject, schemaEntity.getVersion());
            } else {
                List<String> errors = validationMessages.stream()
                    .map(ValidationMessage::getMessage)
                    .collect(Collectors.toList());
                return new SchemaValidationResponse(false, subject, schemaEntity.getVersion(), errors);
            }
        } catch (Exception e) {
            throw new SchemaValidationException("Failed to validate JSON against schema: " + e.getMessage());
        }
    }

    /**
     * Get the next version number for a subject
     * TODO - we may need to be able to specify version explicitly
     */
    private String getNextVersion(String subject) {
        List<SchemaEntity> schemas = schemaRepository.findBySubjectOrderByVersionDesc(subject);
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
