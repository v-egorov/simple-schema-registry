package ru.vegorov.schemaregistry.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.vegorov.schemaregistry.entity.SchemaEntity;

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
    Optional<SchemaEntity> findBySubjectAndVersion(String subject, Integer version);

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
    boolean existsBySubjectAndVersion(String subject, Integer version);

    /**
     * Get all unique subjects
     */
    @Query("SELECT DISTINCT s.subject FROM SchemaEntity s ORDER BY s.subject")
    List<String> findAllSubjects();

    /**
     * Find the maximum version for a subject
     */
    @Query("SELECT MAX(s.version) FROM SchemaEntity s WHERE s.subject = :subject")
    Optional<Integer> findMaxVersionBySubject(@Param("subject") String subject);
}