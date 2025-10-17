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

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class ConsumerService {

    private static final Logger logger = LoggerFactory.getLogger(ConsumerService.class);

    private final ConsumerRepository consumerRepository;

    @Value("${app.logging.business-operations.enabled:true}")
    private boolean businessLoggingEnabled;

    @Value("${app.logging.performance.enabled:true}")
    private boolean performanceLoggingEnabled;

    @Value("${app.logging.performance.slow-threshold-ms:1000}")
    private long slowThresholdMs;

    public ConsumerService(ConsumerRepository consumerRepository) {
        this.consumerRepository = consumerRepository;
    }

    /**
     * Register a new consumer
     */
    public ConsumerResponse registerConsumer(ConsumerRegistrationRequest request) {
        Instant start = performanceLoggingEnabled ? Instant.now() : null;

        if (businessLoggingEnabled) {
            logger.info("Registering new consumer: consumerId={}", request.getConsumerId());
        }

        try {
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

            if (performanceLoggingEnabled) {
                long duration = Duration.between(start, Instant.now()).toMillis();
                if (duration > slowThresholdMs) {
                    logger.warn("Slow consumer registration detected: consumerId={}, duration={}ms",
                        request.getConsumerId(), duration);
                } else {
                    logger.debug("Consumer registration performance: consumerId={}, duration={}ms",
                        request.getConsumerId(), duration);
                }
            }

            return mapToResponse(savedEntity);
        } catch (Exception e) {
            if (performanceLoggingEnabled) {
                long duration = Duration.between(start, Instant.now()).toMillis();
                logger.error("Consumer registration failed: consumerId={}, duration={}ms, error={}",
                    request.getConsumerId(), duration, e.getMessage(), e);
            } else if (businessLoggingEnabled) {
                logger.error("Consumer registration failed: consumerId={}, error={}",
                    request.getConsumerId(), e.getMessage(), e);
            }
            throw e;
        }
    }

    /**
     * Get all consumers
     */
    @Transactional(readOnly = true)
    public List<ConsumerResponse> getAllConsumers() {
        Instant start = performanceLoggingEnabled ? Instant.now() : null;

        try {
            List<ConsumerEntity> entities = consumerRepository.findAll();
            List<ConsumerResponse> responses = entities.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());

            if (performanceLoggingEnabled) {
                long duration = Duration.between(start, Instant.now()).toMillis();
                if (duration > slowThresholdMs) {
                    logger.warn("Slow get all consumers detected: count={}, duration={}ms",
                        responses.size(), duration);
                } else {
                    logger.debug("Get all consumers performance: count={}, duration={}ms",
                        responses.size(), duration);
                }
            }

            return responses;
        } catch (Exception e) {
            if (performanceLoggingEnabled) {
                long duration = Duration.between(start, Instant.now()).toMillis();
                logger.error("Get all consumers failed: duration={}ms, error={}",
                    duration, e.getMessage(), e);
            } else if (businessLoggingEnabled) {
                logger.error("Get all consumers failed: error={}", e.getMessage(), e);
            }
            throw e;
        }
    }

    /**
     * Get consumer by consumerId
     */
    @Transactional(readOnly = true)
    public ConsumerResponse getConsumer(String consumerId) {
        Instant start = performanceLoggingEnabled ? Instant.now() : null;

        if (businessLoggingEnabled) {
            logger.debug("Retrieving consumer: consumerId={}", consumerId);
        }

        try {
            ConsumerEntity entity = consumerRepository.findByConsumerId(consumerId)
                .orElseThrow(() -> new ResourceNotFoundException(
                    String.format("Consumer not found: %s", consumerId)));

            if (businessLoggingEnabled) {
                logger.debug("Consumer retrieved successfully: consumerId={}", consumerId);
            }

            if (performanceLoggingEnabled) {
                long duration = Duration.between(start, Instant.now()).toMillis();
                if (duration > slowThresholdMs) {
                    logger.warn("Slow consumer retrieval detected: consumerId={}, duration={}ms",
                        consumerId, duration);
                } else {
                    logger.debug("Consumer retrieval performance: consumerId={}, duration={}ms",
                        consumerId, duration);
                }
            }

            return mapToResponse(entity);
        } catch (Exception e) {
            if (performanceLoggingEnabled) {
                long duration = Duration.between(start, Instant.now()).toMillis();
                logger.error("Consumer retrieval failed: consumerId={}, duration={}ms, error={}",
                    consumerId, duration, e.getMessage(), e);
            } else if (businessLoggingEnabled) {
                logger.error("Consumer retrieval failed: consumerId={}, error={}",
                    consumerId, e.getMessage(), e);
            }
            throw e;
        }
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