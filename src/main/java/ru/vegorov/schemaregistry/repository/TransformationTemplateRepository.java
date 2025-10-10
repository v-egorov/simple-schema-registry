package ru.vegorov.schemaregistry.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.vegorov.schemaregistry.entity.TransformationTemplateEntity;

import java.util.Optional;

@Repository
public interface TransformationTemplateRepository extends JpaRepository<TransformationTemplateEntity, Long> {

    /**
     * Find transformation template by consumerId
     */
    Optional<TransformationTemplateEntity> findByConsumerId(String consumerId);

    /**
     * Check if transformation template exists for consumerId
     */
    boolean existsByConsumerId(String consumerId);
}