package ru.vegorov.schemaregistry.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.vegorov.schemaregistry.entity.SchemaEntity;
import ru.vegorov.schemaregistry.entity.SchemaType;

import java.util.List;
import java.util.Optional;

@Repository
public interface SchemaRepository extends JpaRepository<SchemaEntity, Long> {

    /**
     * Find all schemas for a given subject ordered by version descending
     */
    List<SchemaEntity> findBySubjectOrderByVersionDesc(String subject);

    /**
     * Find a specific schema version for a subject
     */
    Optional<SchemaEntity> findBySubjectAndVersion(String subject, String version);

    /**
     * Find the latest version of a schema for a subject
     */
    Optional<SchemaEntity> findFirstBySubjectOrderByVersionDesc(String subject);

    /**
     * Check if a subject exists
     */
    boolean existsBySubject(String subject);

    /**
     * Check if a specific version exists for a subject
     */
    boolean existsBySubjectAndVersion(String subject, String version);

    /**
     * Get all unique subjects
     */
    @Query("SELECT DISTINCT s.subject FROM SchemaEntity s ORDER BY s.subject")
    List<String> findAllSubjects();

    /**
     * Find the latest schema for a subject by creation time
     */
    Optional<SchemaEntity> findFirstBySubjectOrderByCreatedAtDesc(String subject);

    // ===== NEW METHODS FOR SCHEMA TYPES AND CONSUMER RELATIONSHIPS =====

    /**
     * Find all canonical schemas for a subject ordered by version descending
     */
    List<SchemaEntity> findBySubjectAndSchemaTypeOrderByVersionDesc(String subject, SchemaType schemaType);

    /**
     * Find all consumer output schemas for a specific consumer and subject
     */
    List<SchemaEntity> findBySubjectAndSchemaTypeAndConsumerIdOrderByVersionDesc(String subject, SchemaType schemaType, String consumerId);

    /**
     * Find a specific canonical schema version for a subject
     */
    Optional<SchemaEntity> findBySubjectAndSchemaTypeAndVersion(String subject, SchemaType schemaType, String version);

    /**
     * Find a specific consumer output schema version for a consumer and subject
     */
    Optional<SchemaEntity> findBySubjectAndSchemaTypeAndConsumerIdAndVersion(String subject, SchemaType schemaType, String consumerId, String version);

    /**
     * Find the latest canonical schema for a subject
     */
    Optional<SchemaEntity> findFirstBySubjectAndSchemaTypeOrderByVersionDesc(String subject, SchemaType schemaType);

    /**
     * Find the latest consumer output schema for a consumer and subject
     */
    Optional<SchemaEntity> findFirstBySubjectAndSchemaTypeAndConsumerIdOrderByVersionDesc(String subject, SchemaType schemaType, String consumerId);

    /**
     * Check if a canonical subject exists
     */
    boolean existsBySubjectAndSchemaType(String subject, SchemaType schemaType);

    /**
     * Check if a consumer output schema exists for a consumer and subject
     */
    boolean existsBySubjectAndSchemaTypeAndConsumerId(String subject, SchemaType schemaType, String consumerId);

    /**
     * Check if a specific canonical schema version exists
     */
    boolean existsBySubjectAndSchemaTypeAndVersion(String subject, SchemaType schemaType, String version);

    /**
     * Check if a specific consumer output schema version exists
     */
    boolean existsBySubjectAndSchemaTypeAndConsumerIdAndVersion(String subject, SchemaType schemaType, String consumerId, String version);

    /**
     * Get all unique subjects that have canonical schemas
     */
    @Query("SELECT DISTINCT s.subject FROM SchemaEntity s WHERE s.schemaType = :schemaType ORDER BY s.subject")
    List<String> findAllSubjectsBySchemaType(@Param("schemaType") SchemaType schemaType);

    /**
     * Get all consumer IDs that have output schemas for a subject
     */
    @Query("SELECT DISTINCT s.consumerId FROM SchemaEntity s WHERE s.subject = :subject AND s.schemaType = :schemaType AND s.consumerId IS NOT NULL ORDER BY s.consumerId")
    List<String> findConsumerIdsBySubjectAndSchemaType(@Param("subject") String subject, @Param("schemaType") SchemaType schemaType);

    /**
     * Get all subjects that have consumer output schemas for a consumer
     */
    @Query("SELECT DISTINCT s.subject FROM SchemaEntity s WHERE s.consumerId = :consumerId AND s.schemaType = :schemaType ORDER BY s.subject")
    List<String> findSubjectsByConsumerIdAndSchemaType(@Param("consumerId") String consumerId, @Param("schemaType") SchemaType schemaType);
}