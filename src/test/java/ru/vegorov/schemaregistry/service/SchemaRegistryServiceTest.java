package ru.vegorov.schemaregistry.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.vegorov.schemaregistry.dto.SchemaRegistrationRequest;
import ru.vegorov.schemaregistry.dto.SchemaResponse;
import ru.vegorov.schemaregistry.dto.SchemaValidationRequest;
import ru.vegorov.schemaregistry.dto.SchemaValidationResponse;
import ru.vegorov.schemaregistry.entity.SchemaEntity;
import ru.vegorov.schemaregistry.entity.SchemaType;
import ru.vegorov.schemaregistry.repository.SchemaRepository;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SchemaRegistryServiceTest {

    @Mock
    private SchemaRepository schemaRepository;

    private ObjectMapper objectMapper = new ObjectMapper();

    private SchemaRegistryService schemaService;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        schemaService = new SchemaRegistryService(schemaRepository, objectMapper);
    }

    @Test
    void registerCanonicalSchema_shouldReturnSchemaResponse() {
        // Given
        Map<String, Object> schemaJson = new HashMap<>();
        schemaJson.put("type", "object");

        SchemaRegistrationRequest request = new SchemaRegistrationRequest(
            "test-subject",
            schemaJson,
            "BACKWARD",
            "Test schema"
        );

        SchemaEntity savedEntity = new SchemaEntity();
        savedEntity.setId(1L);
        savedEntity.setSubject("test-subject");
        savedEntity.setSchemaType(SchemaType.CANONICAL);
        savedEntity.setVersion("1.0.0");
        savedEntity.setSchemaJson(schemaJson);

        when(schemaRepository.findBySubjectAndSchemaTypeOrderByVersionDesc("test-subject", SchemaType.CANONICAL))
            .thenReturn(java.util.Collections.emptyList());
        when(schemaRepository.save(any(SchemaEntity.class))).thenReturn(savedEntity);

        // When
        SchemaResponse response = schemaService.registerCanonicalSchema(request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getSubject()).isEqualTo("test-subject");
        assertThat(response.getVersion()).isEqualTo("1.0.0");
    }

    @Test
    void registerConsumerOutputSchema_shouldReturnSchemaResponse() {
        // Given
        Map<String, Object> schemaJson = new HashMap<>();
        schemaJson.put("type", "object");

        SchemaRegistrationRequest request = new SchemaRegistrationRequest(
            "test-subject",
            schemaJson,
            "BACKWARD",
            "Test consumer output schema"
        );

        SchemaEntity savedEntity = new SchemaEntity();
        savedEntity.setId(1L);
        savedEntity.setSubject("test-subject");
        savedEntity.setSchemaType(SchemaType.CONSUMER_OUTPUT);
        savedEntity.setConsumerId("consumer-1");
        savedEntity.setVersion("1.0.0");
        savedEntity.setSchemaJson(schemaJson);

        when(schemaRepository.findBySubjectAndSchemaTypeAndConsumerIdOrderByVersionDesc("test-subject", SchemaType.CONSUMER_OUTPUT, "consumer-1"))
            .thenReturn(java.util.Collections.emptyList());
        when(schemaRepository.save(any(SchemaEntity.class))).thenReturn(savedEntity);

        // When
        SchemaResponse response = schemaService.registerConsumerOutputSchema("consumer-1", request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getSubject()).isEqualTo("test-subject");
        assertThat(response.getVersion()).isEqualTo("1.0.0");
    }

    @Test
    void validateJson_shouldReturnValidResponse() throws Exception {
        // Given
        ObjectMapper objectMapper = new ObjectMapper();

        // Schema that requires id and name
        Map<String, Object> schemaJson = new HashMap<>();
        schemaJson.put("$schema", "http://json-schema.org/draft-07/schema#");
        schemaJson.put("type", "object");
        Map<String, Object> properties = new HashMap<>();
        Map<String, Object> idProp = new HashMap<>();
        idProp.put("type", "integer");
        properties.put("id", idProp);
        Map<String, Object> nameProp = new HashMap<>();
        nameProp.put("type", "string");
        properties.put("name", nameProp);
        schemaJson.put("properties", properties);
        schemaJson.put("required", java.util.Arrays.asList("id", "name"));

        SchemaEntity schemaEntity = new SchemaEntity();
        schemaEntity.setSubject("test-subject");
        schemaEntity.setSchemaType(SchemaType.CANONICAL);
        schemaEntity.setVersion("1.0.0");
        schemaEntity.setSchemaJson(schemaJson);

        // Valid JSON data
        JsonNode validJson = objectMapper.readTree("{\"id\": 123, \"name\": \"John Doe\"}");

        SchemaValidationRequest request = new SchemaValidationRequest("test-subject", validJson);

        when(schemaRepository.findFirstBySubjectAndSchemaTypeOrderByVersionDesc("test-subject", SchemaType.CANONICAL))
            .thenReturn(Optional.of(schemaEntity));

        // When
        SchemaValidationResponse response = schemaService.validateJson(request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.isValid()).isTrue();
        assertThat(response.getSubject()).isEqualTo("test-subject");
        assertThat(response.getSchemaVersion()).isEqualTo("1.0.0");
        assertThat(response.getErrors()).isNullOrEmpty();
    }

    @Test
    void validateJsonAgainstConsumerOutputSchema_shouldReturnValidResponse() throws Exception {
        // Given
        ObjectMapper objectMapper = new ObjectMapper();

        // Schema that requires id and name
        Map<String, Object> schemaJson = new HashMap<>();
        schemaJson.put("$schema", "http://json-schema.org/draft-07/schema#");
        schemaJson.put("type", "object");
        Map<String, Object> properties = new HashMap<>();
        Map<String, Object> idProp = new HashMap<>();
        idProp.put("type", "integer");
        properties.put("id", idProp);
        Map<String, Object> nameProp = new HashMap<>();
        nameProp.put("type", "string");
        properties.put("name", nameProp);
        schemaJson.put("properties", properties);
        schemaJson.put("required", java.util.Arrays.asList("id", "name"));

        SchemaEntity schemaEntity = new SchemaEntity();
        schemaEntity.setSubject("test-subject");
        schemaEntity.setSchemaType(SchemaType.CONSUMER_OUTPUT);
        schemaEntity.setConsumerId("consumer-1");
        schemaEntity.setVersion("1.0.0");
        schemaEntity.setSchemaJson(schemaJson);

        // Valid JSON data
        JsonNode validJson = objectMapper.readTree("{\"id\": 123, \"name\": \"John Doe\"}");

        SchemaValidationRequest request = new SchemaValidationRequest("test-subject", validJson);

        when(schemaRepository.findFirstBySubjectAndSchemaTypeAndConsumerIdOrderByVersionDesc("test-subject", SchemaType.CONSUMER_OUTPUT, "consumer-1"))
            .thenReturn(Optional.of(schemaEntity));

        // When
        SchemaValidationResponse response = schemaService.validateJsonAgainstConsumerOutputSchema(request, "consumer-1");

        // Then
        assertThat(response).isNotNull();
        assertThat(response.isValid()).isTrue();
        assertThat(response.getSubject()).isEqualTo("test-subject");
        assertThat(response.getSchemaVersion()).isEqualTo("1.0.0");
        assertThat(response.getErrors()).isNullOrEmpty();
    }
}