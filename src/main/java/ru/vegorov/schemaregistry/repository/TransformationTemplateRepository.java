package ru.vegorov.schemaregistry.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.vegorov.schemaregistry.entity.TransformationTemplateEntity;

import java.util.List;
import java.util.Optional;

@Repository
public interface TransformationTemplateRepository extends JpaRepository<TransformationTemplateEntity, Long> {

    // ===== NEW METHODS FOR VERSIONING SYSTEM =====

    // ===== NEW METHODS FOR VERSIONING SYSTEM =====

    /**
     * Find all transformation templates for a consumer and subject ordered by version descending (deprecated - use application-level sorting)
     */
    @Deprecated
    List<TransformationTemplateEntity> findByConsumerIdAndSubjectOrderByVersionDesc(String consumerId, String subject);

    /**
     * Find all transformation templates for a consumer and subject
     */
    @Query("SELECT t FROM TransformationTemplateEntity t LEFT JOIN FETCH t.inputSchema LEFT JOIN FETCH t.outputSchema WHERE t.consumerId = :consumerId AND t.subject = :subject")
    List<TransformationTemplateEntity> findByConsumerIdAndSubject(String consumerId, String subject);

    /**
     * Find a specific transformation template version for a consumer and subject
     */
    @Query("SELECT t FROM TransformationTemplateEntity t LEFT JOIN FETCH t.inputSchema LEFT JOIN FETCH t.outputSchema WHERE t.consumerId = :consumerId AND t.subject = :subject AND t.version = :version")
    Optional<TransformationTemplateEntity> findByConsumerIdAndSubjectAndVersion(String consumerId, String subject, String version);

    /**
     * Find the active transformation template for a consumer and subject
     */
    @Query("SELECT t FROM TransformationTemplateEntity t LEFT JOIN FETCH t.inputSchema LEFT JOIN FETCH t.outputSchema WHERE t.consumerId = :consumerId AND t.subject = :subject AND t.isActive = true")
    Optional<TransformationTemplateEntity> findByConsumerIdAndSubjectAndIsActiveTrue(String consumerId, String subject);

    /**
     * Find the latest transformation template for a consumer and subject (by version) (deprecated - use application-level sorting)
     */
    @Deprecated
    Optional<TransformationTemplateEntity> findFirstByConsumerIdAndSubjectOrderByVersionDesc(String consumerId, String subject);

    /**
     * Check if any transformation template exists for a consumer and subject
     */
    boolean existsByConsumerIdAndSubject(String consumerId, String subject);

    /**
     * Check if a specific version exists for a consumer and subject
     */
    boolean existsByConsumerIdAndSubjectAndVersion(String consumerId, String subject, String version);

    /**
     * Check if an active template exists for a consumer and subject
     */
    boolean existsByConsumerIdAndSubjectAndIsActiveTrue(String consumerId, String subject);

    /**
     * Get all subjects that have transformation templates for a consumer
     */
    @Query("SELECT DISTINCT t.subject FROM TransformationTemplateEntity t WHERE t.consumerId = :consumerId ORDER BY t.subject")
    List<String> findSubjectsByConsumerId(@Param("consumerId") String consumerId);

    /**
     * Get all consumer IDs that have transformation templates
     */
    @Query("SELECT DISTINCT t.consumerId FROM TransformationTemplateEntity t ORDER BY t.consumerId")
    List<String> findAllConsumerIds();

    /**
     * Get all versions for a consumer and subject ordered by version descending (deprecated - use application-level sorting)
     */
    @Deprecated
    @Query("SELECT t.version FROM TransformationTemplateEntity t WHERE t.consumerId = :consumerId AND t.subject = :subject ORDER BY t.version DESC")
    List<String> findVersionsByConsumerIdAndSubject(@Param("consumerId") String consumerId, @Param("subject") String subject);

    /**
     * Get all versions for a consumer and subject
     */
    @Query("SELECT t.version FROM TransformationTemplateEntity t WHERE t.consumerId = :consumerId AND t.subject = :subject")
    List<String> findVersionsByConsumerIdAndSubjectUnordered(@Param("consumerId") String consumerId, @Param("subject") String subject);

    /**
     * Find all transformation templates for a consumer (across all subjects) (deprecated - use application-level sorting)
     */
    @Deprecated
    List<TransformationTemplateEntity> findByConsumerIdOrderBySubjectAscVersionDesc(String consumerId);

    /**
     * Find all transformation templates for a consumer (across all subjects) ordered by subject ascending
     */
    List<TransformationTemplateEntity> findByConsumerIdOrderBySubjectAsc(String consumerId);

    /**
     * Count transformation templates for a consumer and subject
     */
    long countByConsumerIdAndSubject(String consumerId, String subject);
}