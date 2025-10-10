package ru.vegorov.schemaregistry.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.vegorov.schemaregistry.entity.ConsumerEntity;

import java.util.Optional;

@Repository
public interface ConsumerRepository extends JpaRepository<ConsumerEntity, Long> {

    /**
     * Find consumer by consumerId
     */
    Optional<ConsumerEntity> findByConsumerId(String consumerId);

    /**
     * Check if consumer exists by consumerId
     */
    boolean existsByConsumerId(String consumerId);
}