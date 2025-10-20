package ru.vegorov.schemaregistry.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.vegorov.schemaregistry.dto.CompatibilityCheckRequest;
import ru.vegorov.schemaregistry.dto.CompatibilityCheckResponse;
import ru.vegorov.schemaregistry.dto.SchemaRegistrationRequest;
import ru.vegorov.schemaregistry.dto.SchemaResponse;
import ru.vegorov.schemaregistry.dto.SchemaValidationRequest;
import ru.vegorov.schemaregistry.dto.SchemaValidationResponse;
import ru.vegorov.schemaregistry.entity.SchemaEntity;
import ru.vegorov.schemaregistry.entity.SchemaType;
import ru.vegorov.schemaregistry.repository.SchemaRepository;
import ru.vegorov.schemaregistry.service.ConsumerService;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SchemaRegistryServiceTest {

    @Mock
    private SchemaRepository schemaRepository;

    @Mock
    private ConsumerService consumerService;

    private ObjectMapper objectMapper = new ObjectMapper();

    private SchemaRegistryService schemaService;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        schemaService = new SchemaRegistryService(schemaRepository, consumerService, objectMapper);
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
        savedEntity.setSchemaType(SchemaType.canonical);
        savedEntity.setVersion("1.0.0");
        savedEntity.setSchemaJson(schemaJson);

        when(schemaRepository.findBySubjectAndSchemaType("test-subject", SchemaType.canonical))
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
        savedEntity.setSchemaType(SchemaType.consumer_output);
        savedEntity.setConsumerId("consumer-1");
        savedEntity.setVersion("1.0.0");
        savedEntity.setSchemaJson(schemaJson);

        when(schemaRepository.findBySubjectAndSchemaTypeAndConsumerId("test-subject", SchemaType.consumer_output, "consumer-1"))
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
    void registerCanonicalSchema_shouldThrowExceptionForInvalidSchema() {
        // Given
        Map<String, Object> invalidSchemaJson = new HashMap<>();
        invalidSchemaJson.put("invalid", "schema"); // no 'type' or '$schema'

        SchemaRegistrationRequest request = new SchemaRegistrationRequest(
            "test-subject",
            invalidSchemaJson,
            "BACKWARD",
            "Invalid schema"
        );

        // When & Then
        assertThatThrownBy(() -> schemaService.registerCanonicalSchema(request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Schema must contain 'type' or '$schema' keyword");
    }

    @Test
    void registerConsumerOutputSchema_shouldThrowExceptionForInvalidSchema() {
        // Given
        Map<String, Object> invalidSchemaJson = new HashMap<>();
        invalidSchemaJson.put("invalid", "schema"); // no 'type' or '$schema'

        SchemaRegistrationRequest request = new SchemaRegistrationRequest(
            "test-subject",
            invalidSchemaJson,
            "BACKWARD",
            "Invalid consumer output schema"
        );

        when(schemaRepository.findBySubjectAndSchemaTypeAndConsumerId("test-subject", SchemaType.consumer_output, "consumer-1"))
            .thenReturn(java.util.Collections.emptyList());

        // When & Then
        assertThatThrownBy(() -> schemaService.registerConsumerOutputSchema("consumer-1", request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Schema must contain 'type' or '$schema' keyword");
    }

    @Test
    void validateJson_shouldReturnValidResponse() throws Exception {
        // Given
        ObjectMapper objectMapper = new ObjectMapper();

        // Schema that requires id and name
        Map<String, Object> schemaJson = new HashMap<>();
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
        schemaEntity.setSchemaType(SchemaType.canonical);
        schemaEntity.setVersion("1.0.0");
        schemaEntity.setSchemaJson(schemaJson);

        // Valid JSON data
        JsonNode validJson = objectMapper.readTree("{\"id\": 123, \"name\": \"John Doe\"}");

        SchemaValidationRequest request = new SchemaValidationRequest("test-subject", validJson);

        when(schemaRepository.findBySubjectAndSchemaType("test-subject", SchemaType.canonical))
            .thenReturn(java.util.List.of(schemaEntity));

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
        schemaEntity.setSchemaType(SchemaType.consumer_output);
        schemaEntity.setConsumerId("consumer-1");
        schemaEntity.setVersion("1.0.0");
        schemaEntity.setSchemaJson(schemaJson);

        // Valid JSON data
        JsonNode validJson = objectMapper.readTree("{\"id\": 123, \"name\": \"John Doe\"}");

        SchemaValidationRequest request = new SchemaValidationRequest("test-subject", validJson);

        when(schemaRepository.findBySubjectAndSchemaTypeAndConsumerId("test-subject", SchemaType.consumer_output, "consumer-1"))
            .thenReturn(java.util.List.of(schemaEntity));

        // When
        SchemaValidationResponse response = schemaService.validateJsonAgainstConsumerOutputSchema(request, "consumer-1");

        // Then
        assertThat(response).isNotNull();
        assertThat(response.isValid()).isTrue();
        assertThat(response.getSubject()).isEqualTo("test-subject");
        assertThat(response.getSchemaVersion()).isEqualTo("1.0.0");
        assertThat(response.getErrors()).isNullOrEmpty();
    }

    @Test
    void checkCanonicalSchemaCompatibility_shouldReturnCompatibleWhenNoExistingSchema() {
        // Given
        Map<String, Object> newSchema = new HashMap<>();
        newSchema.put("type", "object");

        CompatibilityCheckRequest request = new CompatibilityCheckRequest(newSchema, "test-subject");

        when(schemaRepository.findBySubjectAndSchemaType("test-subject", SchemaType.canonical))
            .thenReturn(java.util.Collections.emptyList());

        // When
        CompatibilityCheckResponse response = schemaService.checkCanonicalSchemaCompatibility(request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.isCompatible()).isTrue();
        assertThat(response.getMessage()).isEqualTo("No existing schema to check against");
    }

    @Test
    void checkCanonicalSchemaCompatibility_shouldCheckCompatibilityAgainstLatestVersion() {
        // Given
        Map<String, Object> existingSchema = new HashMap<>();
        existingSchema.put("type", "object");
        Map<String, Object> existingProperties = new HashMap<>();
        Map<String, Object> idProp = new HashMap<>();
        idProp.put("type", "integer");
        existingProperties.put("id", idProp);
        existingSchema.put("properties", existingProperties);
        existingSchema.put("required", java.util.Arrays.asList("id"));

        SchemaEntity existingEntity = new SchemaEntity();
        existingEntity.setSubject("test-subject");
        existingEntity.setSchemaType(SchemaType.canonical);
        existingEntity.setVersion("1.0.0");
        existingEntity.setCompatibility("BACKWARD");
        existingEntity.setSchemaJson(existingSchema);

        Map<String, Object> newSchema = new HashMap<>();
        newSchema.put("type", "object");
        Map<String, Object> newProperties = new HashMap<>();
        Map<String, Object> idPropNew = new HashMap<>();
        idPropNew.put("type", "integer");
        newProperties.put("id", idPropNew);
        Map<String, Object> nameProp = new HashMap<>();
        nameProp.put("type", "string");
        newProperties.put("name", nameProp);
        newSchema.put("properties", newProperties);
        newSchema.put("required", java.util.Arrays.asList("id"));

        CompatibilityCheckRequest request = new CompatibilityCheckRequest(newSchema, "test-subject");

        when(schemaRepository.findBySubjectAndSchemaType("test-subject", SchemaType.canonical))
            .thenReturn(java.util.List.of(existingEntity));

        // When
        CompatibilityCheckResponse response = schemaService.checkCanonicalSchemaCompatibility(request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.isCompatible()).isTrue(); // Stubbed to return true
        assertThat(response.getMessage()).isEqualTo("Schema is compatible");
    }

    @Test
    void checkConsumerOutputSchemaCompatibility_shouldReturnCompatibleWhenNoExistingSchema() {
        // Given
        Map<String, Object> newSchema = new HashMap<>();
        newSchema.put("type", "object");

        CompatibilityCheckRequest request = new CompatibilityCheckRequest(newSchema, "test-subject");

        when(schemaRepository.findBySubjectAndSchemaTypeAndConsumerId("test-subject", SchemaType.consumer_output, "consumer-1"))
            .thenReturn(java.util.Collections.emptyList());

        // When
        CompatibilityCheckResponse response = schemaService.checkConsumerOutputSchemaCompatibility("consumer-1", request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.isCompatible()).isTrue();
        assertThat(response.getMessage()).isEqualTo("No existing consumer output schema to check against");
    }

    @Test
    void checkConsumerOutputSchemaCompatibility_shouldCheckCompatibilityAgainstLatestVersion() {
        // Given
        Map<String, Object> existingSchema = new HashMap<>();
        existingSchema.put("type", "object");
        Map<String, Object> existingProperties = new HashMap<>();
        Map<String, Object> idProp = new HashMap<>();
        idProp.put("type", "integer");
        existingProperties.put("id", idProp);
        existingSchema.put("properties", existingProperties);
        existingSchema.put("required", java.util.Arrays.asList("id"));

        SchemaEntity existingEntity = new SchemaEntity();
        existingEntity.setSubject("test-subject");
        existingEntity.setSchemaType(SchemaType.consumer_output);
        existingEntity.setConsumerId("consumer-1");
        existingEntity.setVersion("1.0.0");
        existingEntity.setCompatibility("BACKWARD");
        existingEntity.setSchemaJson(existingSchema);

        Map<String, Object> newSchema = new HashMap<>();
        newSchema.put("type", "object");
        Map<String, Object> newProperties = new HashMap<>();
        Map<String, Object> idPropNew = new HashMap<>();
        idPropNew.put("type", "integer");
        newProperties.put("id", idPropNew);
        Map<String, Object> nameProp = new HashMap<>();
        nameProp.put("type", "string");
        newProperties.put("name", nameProp);
        newSchema.put("properties", newProperties);
        newSchema.put("required", java.util.Arrays.asList("id"));

        CompatibilityCheckRequest request = new CompatibilityCheckRequest(newSchema, "test-subject");

        when(schemaRepository.findBySubjectAndSchemaTypeAndConsumerId("test-subject", SchemaType.consumer_output, "consumer-1"))
            .thenReturn(java.util.List.of(existingEntity));

        // When
        CompatibilityCheckResponse response = schemaService.checkConsumerOutputSchemaCompatibility("consumer-1", request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.isCompatible()).isTrue(); // Stubbed to return true
        assertThat(response.getMessage()).isEqualTo("Consumer output schema is compatible");
    }

    @Test
    void registerCanonicalSchema_shouldSupportDraft06Schema() throws Exception {
        // Given
        Map<String, Object> schemaJson = new HashMap<>();
        schemaJson.put("$schema", "http://json-schema.org/draft-06/schema#");
        schemaJson.put("type", "object");
        Map<String, Object> properties = new HashMap<>();
        Map<String, Object> idProp = new HashMap<>();
        idProp.put("type", "integer");
        properties.put("id", idProp);
        schemaJson.put("properties", properties);

        SchemaRegistrationRequest request = new SchemaRegistrationRequest(
            "test-subject", schemaJson, "BACKWARD", "Test schema"
        );

        SchemaEntity savedEntity = new SchemaEntity();
        savedEntity.setId(1L);
        savedEntity.setSubject("test-subject");
        savedEntity.setVersion("1.0.0");
        savedEntity.setSchemaJson(schemaJson);

        when(schemaRepository.save(any(SchemaEntity.class))).thenReturn(savedEntity);

        // When
        SchemaResponse response = schemaService.registerCanonicalSchema(request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getSubject()).isEqualTo("test-subject");
        assertThat(response.getVersion()).isEqualTo("1.0.0");
    }

    @Test
    void registerCanonicalSchema_shouldSupportDraft07Schema() throws Exception {
        // Given
        Map<String, Object> schemaJson = new HashMap<>();
        schemaJson.put("$schema", "http://json-schema.org/draft-07/schema#");
        schemaJson.put("type", "object");
        Map<String, Object> properties = new HashMap<>();
        Map<String, Object> nameProp = new HashMap<>();
        nameProp.put("type", "string");
        properties.put("name", nameProp);
        schemaJson.put("properties", properties);

        SchemaRegistrationRequest request = new SchemaRegistrationRequest(
            "test-subject", schemaJson, "BACKWARD", "Test schema"
        );

        SchemaEntity savedEntity = new SchemaEntity();
        savedEntity.setId(1L);
        savedEntity.setSubject("test-subject");
        savedEntity.setVersion("1.0.0");
        savedEntity.setSchemaJson(schemaJson);

        when(schemaRepository.save(any(SchemaEntity.class))).thenReturn(savedEntity);

        // When
        SchemaResponse response = schemaService.registerCanonicalSchema(request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getSubject()).isEqualTo("test-subject");
        assertThat(response.getVersion()).isEqualTo("1.0.0");
    }

    @Test
    void registerCanonicalSchema_shouldSupportDraft201909Schema() throws Exception {
        // Given
        Map<String, Object> schemaJson = new HashMap<>();
        schemaJson.put("$schema", "https://json-schema.org/draft/2019-09/schema");
        schemaJson.put("type", "object");
        Map<String, Object> properties = new HashMap<>();
        Map<String, Object> emailProp = new HashMap<>();
        emailProp.put("type", "string");
        emailProp.put("format", "email");
        properties.put("email", emailProp);
        schemaJson.put("properties", properties);

        SchemaRegistrationRequest request = new SchemaRegistrationRequest(
            "test-subject", schemaJson, "BACKWARD", "Test schema"
        );

        SchemaEntity savedEntity = new SchemaEntity();
        savedEntity.setId(1L);
        savedEntity.setSubject("test-subject");
        savedEntity.setVersion("1.0.0");
        savedEntity.setSchemaJson(schemaJson);

        when(schemaRepository.save(any(SchemaEntity.class))).thenReturn(savedEntity);

        // When
        SchemaResponse response = schemaService.registerCanonicalSchema(request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getSubject()).isEqualTo("test-subject");
        assertThat(response.getVersion()).isEqualTo("1.0.0");
    }

    @Test
    void registerCanonicalSchema_shouldRejectDraft202012Schema() throws Exception {
        // Given
        Map<String, Object> schemaJson = new HashMap<>();
        schemaJson.put("$schema", "https://json-schema.org/draft/2020-12/schema");
        schemaJson.put("type", "object");
        Map<String, Object> properties = new HashMap<>();
        Map<String, Object> ageProp = new HashMap<>();
        ageProp.put("type", "integer");
        ageProp.put("minimum", 0);
        properties.put("age", ageProp);
        schemaJson.put("properties", properties);

        SchemaRegistrationRequest request = new SchemaRegistrationRequest(
            "test-subject", schemaJson, "BACKWARD", "Test schema"
        );

        // When & Then
        assertThatThrownBy(() -> schemaService.registerCanonicalSchema(request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("JSON Schema draft-2020-12 is not supported");
    }

    @Test
    void registerCanonicalSchema_shouldDefaultToDraft04WhenNoSchemaSpecified() throws Exception {
        // Given
        Map<String, Object> schemaJson = new HashMap<>();
        schemaJson.put("type", "object");
        Map<String, Object> properties = new HashMap<>();
        Map<String, Object> idProp = new HashMap<>();
        idProp.put("type", "integer");
        properties.put("id", idProp);
        schemaJson.put("properties", properties);

        SchemaRegistrationRequest request = new SchemaRegistrationRequest(
            "test-subject", schemaJson, "BACKWARD", "Test schema"
        );

        SchemaEntity savedEntity = new SchemaEntity();
        savedEntity.setId(1L);
        savedEntity.setSubject("test-subject");
        savedEntity.setVersion("1.0.0");
        savedEntity.setSchemaJson(schemaJson);

        when(schemaRepository.save(any(SchemaEntity.class))).thenReturn(savedEntity);

        // When
        SchemaResponse response = schemaService.registerCanonicalSchema(request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getSubject()).isEqualTo("test-subject");
        assertThat(response.getVersion()).isEqualTo("1.0.0");
    }

    @Test
    void registerCanonicalSchema_shouldRejectUnsupportedSchemaVersion() {
        // Given
        Map<String, Object> schemaJson = new HashMap<>();
        schemaJson.put("$schema", "http://json-schema.org/draft-03/schema#");
        schemaJson.put("type", "object");

        SchemaRegistrationRequest request = new SchemaRegistrationRequest(
            "test-subject", schemaJson, "BACKWARD", "Test schema"
        );

        // When & Then
        assertThatThrownBy(() -> schemaService.registerCanonicalSchema(request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Unsupported JSON Schema version: http://json-schema.org/draft-03/schema#");
    }

    @Test
    void validateJson_shouldSupportDifferentSchemaVersions() throws Exception {
        // Given - draft-07 schema
        Map<String, Object> schemaJson = new HashMap<>();
        schemaJson.put("$schema", "http://json-schema.org/draft-07/schema#");
        schemaJson.put("type", "object");
        Map<String, Object> properties = new HashMap<>();
        Map<String, Object> nameProp = new HashMap<>();
        nameProp.put("type", "string");
        properties.put("name", nameProp);
        schemaJson.put("properties", properties);
        schemaJson.put("required", java.util.Arrays.asList("name"));

        SchemaEntity schemaEntity = new SchemaEntity();
        schemaEntity.setSubject("test-subject");
        schemaEntity.setSchemaType(SchemaType.canonical);
        schemaEntity.setVersion("1.0.0");
        schemaEntity.setSchemaJson(schemaJson);

        // Valid JSON data
        JsonNode validJson = objectMapper.readTree("{\"name\": \"John Doe\"}");

        SchemaValidationRequest request = new SchemaValidationRequest("test-subject", validJson);

        when(schemaRepository.findBySubjectAndSchemaType("test-subject", SchemaType.canonical))
            .thenReturn(java.util.List.of(schemaEntity));

        // When
        SchemaValidationResponse response = schemaService.validateJson(request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.isValid()).isTrue();
        assertThat(response.getSubject()).isEqualTo("test-subject");
        assertThat(response.getSchemaVersion()).isEqualTo("1.0.0");
    }
}