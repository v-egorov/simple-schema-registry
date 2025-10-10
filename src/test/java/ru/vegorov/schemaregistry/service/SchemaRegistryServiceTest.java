package ru.vegorov.schemaregistry.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.vegorov.schemaregistry.dto.SchemaRegistrationRequest;
import ru.vegorov.schemaregistry.dto.SchemaResponse;
import ru.vegorov.schemaregistry.entity.SchemaEntity;
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

    @InjectMocks
    private SchemaRegistryService schemaService;

    @Test
    void registerSchema_shouldReturnSchemaResponse() {
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
        savedEntity.setVersion(1);
        savedEntity.setSchemaJson(schemaJson);

        when(schemaRepository.findMaxVersionBySubject("test-subject")).thenReturn(Optional.empty());
        when(schemaRepository.save(any(SchemaEntity.class))).thenReturn(savedEntity);

        // When
        SchemaResponse response = schemaService.registerSchema(request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getSubject()).isEqualTo("test-subject");
        assertThat(response.getVersion()).isEqualTo(1);
    }
}