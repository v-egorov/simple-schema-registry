package ru.vegorov.schemaregistry.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.vegorov.schemaregistry.dto.ConsumerRegistrationRequest;
import ru.vegorov.schemaregistry.dto.ConsumerResponse;
import ru.vegorov.schemaregistry.entity.ConsumerEntity;
import ru.vegorov.schemaregistry.exception.ConflictException;
import ru.vegorov.schemaregistry.exception.ResourceNotFoundException;
import ru.vegorov.schemaregistry.repository.ConsumerRepository;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class ConsumerService {

    private static final Logger logger = LoggerFactory.getLogger(ConsumerService.class);

    private final ConsumerRepository consumerRepository;

    @Value("${app.logging.business-operations.enabled:true}")
    private boolean businessLoggingEnabled;

    public ConsumerService(ConsumerRepository consumerRepository) {
        this.consumerRepository = consumerRepository;
    }

    /**
     * Register a new consumer
     */
    public ConsumerResponse registerConsumer(ConsumerRegistrationRequest request) {
        if (businessLoggingEnabled) {
            logger.info("Registering new consumer: consumerId={}", request.getConsumerId());
        }

        if (consumerRepository.existsByConsumerId(request.getConsumerId())) {
            if (businessLoggingEnabled) {
                logger.warn("Consumer registration failed - consumer already exists: consumerId={}",
                    request.getConsumerId());
            }
            throw new ConflictException(
                String.format("Consumer with ID '%s' already exists", request.getConsumerId()));
        }

        ConsumerEntity entity = new ConsumerEntity(
            request.getConsumerId(),
            request.getName(),
            request.getDescription()
        );

        ConsumerEntity savedEntity = consumerRepository.save(entity);

        if (businessLoggingEnabled) {
            logger.info("Consumer registered successfully: consumerId={}, id={}",
                request.getConsumerId(), savedEntity.getId());
        }

        return mapToResponse(savedEntity);
    }

    /**
     * Get all consumers
     */
    @Transactional(readOnly = true)
    public List<ConsumerResponse> getAllConsumers() {
        List<ConsumerEntity> entities = consumerRepository.findAll();
        return entities.stream()
            .map(this::mapToResponse)
            .collect(Collectors.toList());
    }

    /**
     * Get consumer by consumerId
     */
    @Transactional(readOnly = true)
    public ConsumerResponse getConsumer(String consumerId) {
        if (businessLoggingEnabled) {
            logger.debug("Retrieving consumer: consumerId={}", consumerId);
        }

        ConsumerEntity entity = consumerRepository.findByConsumerId(consumerId)
            .orElseThrow(() -> new ResourceNotFoundException(
                String.format("Consumer not found: %s", consumerId)));

        if (businessLoggingEnabled) {
            logger.debug("Consumer retrieved successfully: consumerId={}", consumerId);
        }

        return mapToResponse(entity);
    }

    /**
     * Check if consumer exists
     */
    @Transactional(readOnly = true)
    public boolean consumerExists(String consumerId) {
        return consumerRepository.existsByConsumerId(consumerId);
    }



    /**
     * Map entity to response DTO
     */
    private ConsumerResponse mapToResponse(ConsumerEntity entity) {
        return new ConsumerResponse(
            entity.getId(),
            entity.getConsumerId(),
            entity.getName(),
            entity.getDescription(),
            entity.getCreatedAt(),
            entity.getUpdatedAt()
        );
    }
}