package ru.vegorov.schemaregistry.service;

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

    private final ConsumerRepository consumerRepository;

    public ConsumerService(ConsumerRepository consumerRepository) {
        this.consumerRepository = consumerRepository;
    }

    /**
     * Register a new consumer
     */
    public ConsumerResponse registerConsumer(ConsumerRegistrationRequest request) {
        if (consumerRepository.existsByConsumerId(request.getConsumerId())) {
            throw new ConflictException(
                String.format("Consumer with ID '%s' already exists", request.getConsumerId()));
        }

        ConsumerEntity entity = new ConsumerEntity(
            request.getConsumerId(),
            request.getName(),
            request.getDescription()
        );

        ConsumerEntity savedEntity = consumerRepository.save(entity);
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
        ConsumerEntity entity = consumerRepository.findByConsumerId(consumerId)
            .orElseThrow(() -> new ResourceNotFoundException(
                String.format("Consumer not found: %s", consumerId)));
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