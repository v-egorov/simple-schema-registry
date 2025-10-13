package ru.vegorov.schemaregistry.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.vegorov.schemaregistry.dto.TransformationRequest;
import ru.vegorov.schemaregistry.dto.TransformationResponse;
import ru.vegorov.schemaregistry.dto.TransformationTemplateRequest;
import ru.vegorov.schemaregistry.dto.TransformationTemplateResponse;
import ru.vegorov.schemaregistry.entity.SchemaEntity;
import ru.vegorov.schemaregistry.entity.SchemaType;
import ru.vegorov.schemaregistry.entity.TransformationTemplateEntity;
import ru.vegorov.schemaregistry.exception.ResourceNotFoundException;
import ru.vegorov.schemaregistry.repository.SchemaRepository;
import ru.vegorov.schemaregistry.repository.TransformationTemplateRepository;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransformationServiceTest {

    @Mock
    private TransformationTemplateRepository templateRepository;

    @Mock
    private SchemaRepository schemaRepository;

    @Mock
    private ConsumerService consumerService;

    @Mock
    private JsltTransformationEngine jsltEngine;

    @Mock
    private RouterTransformationEngine routerEngine;

    @Mock
    private PipelineTransformationEngine pipelineEngine;

    private ObjectMapper objectMapper = new ObjectMapper();

    private TransformationService transformationService;

    @BeforeEach
    void setUp() {
        transformationService = new TransformationService(
            templateRepository,
            schemaRepository,
            jsltEngine,
            routerEngine,
            pipelineEngine,
            consumerService,
            objectMapper
        );
    }

    @Test
    void createTemplateVersion_shouldCreateNewVersion() {
        // Given
        Map<String, Object> canonicalJson = new HashMap<>();
        canonicalJson.put("id", 123);
        canonicalJson.put("name", "John Doe");

        TransformationTemplateRequest request = new TransformationTemplateRequest(
            "1.0.0",
            "jslt",
            1L, // inputSchemaId
            2L, // outputSchemaId
            ".",
            "Test template"
        );

        SchemaEntity inputSchema = new SchemaEntity();
        inputSchema.setId(1L);
        inputSchema.setSubject("input-subject");
        inputSchema.setSchemaType(SchemaType.canonical);

        SchemaEntity outputSchema = new SchemaEntity();
        outputSchema.setId(2L);
        outputSchema.setSubject("output-subject");
        outputSchema.setSchemaType(SchemaType.consumer_output);
        outputSchema.setConsumerId("consumer-1");

        TransformationTemplateEntity savedEntity = new TransformationTemplateEntity();
        savedEntity.setId(1L);
        savedEntity.setConsumerId("consumer-1");
        savedEntity.setSubject("input-subject");
        savedEntity.setVersion("1.0.0");
        savedEntity.setEngine("jslt");
        savedEntity.setTemplateExpression(".");
        savedEntity.setInputSchema(inputSchema);
        savedEntity.setOutputSchema(outputSchema);
        savedEntity.setIsActive(true);

        when(consumerService.consumerExists("consumer-1")).thenReturn(true);
        when(schemaRepository.findById(1L)).thenReturn(Optional.of(inputSchema));
        when(schemaRepository.findById(2L)).thenReturn(Optional.of(outputSchema));
        when(templateRepository.existsByConsumerIdAndSubjectAndVersion("consumer-1", "input-subject", "1.0.0")).thenReturn(false);
        when(templateRepository.existsByConsumerIdAndSubject("consumer-1", "input-subject")).thenReturn(false);
        when(jsltEngine.validateExpression(".")).thenReturn(true);
        when(templateRepository.save(any(TransformationTemplateEntity.class))).thenReturn(savedEntity);

        // When
        TransformationTemplateResponse response = transformationService.createTemplateVersion("consumer-1", request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getConsumerId()).isEqualTo("consumer-1");
        assertThat(response.getSubject()).isEqualTo("input-subject");
        assertThat(response.getVersion()).isEqualTo("1.0.0");
        assertThat(response.getIsActive()).isTrue();
    }

    @Test
    void createTemplateVersion_shouldThrowWhenConsumerDoesNotExist() {
        // Given
        TransformationTemplateRequest request = new TransformationTemplateRequest(
            "1.0.0", "jslt", 1L, 2L, ".", "Test template"
        );

        when(consumerService.consumerExists("non-existent-consumer")).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> transformationService.createTemplateVersion("non-existent-consumer", request))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("Consumer not found: non-existent-consumer");
    }

    @Test
    void createTemplateVersion_shouldThrowWhenInputSchemaDoesNotExist() {
        // Given
        TransformationTemplateRequest request = new TransformationTemplateRequest(
            "1.0.0", "jslt", 1L, 2L, ".", "Test template"
        );

        when(consumerService.consumerExists("consumer-1")).thenReturn(true);
        when(schemaRepository.findById(1L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> transformationService.createTemplateVersion("consumer-1", request))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("Input schema not found: 1");
    }

    @Test
    void getActiveTemplate_shouldReturnActiveTemplate() {
        // Given
        SchemaEntity inputSchema = new SchemaEntity();
        inputSchema.setId(1L);
        inputSchema.setSubject("test-subject");

        SchemaEntity outputSchema = new SchemaEntity();
        outputSchema.setId(2L);
        outputSchema.setSubject("test-subject");

        TransformationTemplateEntity template = new TransformationTemplateEntity();
        template.setId(1L);
        template.setConsumerId("consumer-1");
        template.setSubject("test-subject");
        template.setVersion("1.0.0");
        template.setIsActive(true);
        template.setInputSchema(inputSchema);
        template.setOutputSchema(outputSchema);

        when(templateRepository.findByConsumerIdAndSubjectAndIsActiveTrue("consumer-1", "test-subject"))
            .thenReturn(Optional.of(template));

        // When
        TransformationTemplateResponse response = transformationService.getActiveTemplate("consumer-1", "test-subject");

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getConsumerId()).isEqualTo("consumer-1");
        assertThat(response.getSubject()).isEqualTo("test-subject");
        assertThat(response.getVersion()).isEqualTo("1.0.0");
        assertThat(response.getIsActive()).isTrue();
    }

    @Test
    void getActiveTemplate_shouldThrowWhenNoActiveTemplate() {
        // Given
        when(templateRepository.findByConsumerIdAndSubjectAndIsActiveTrue("consumer-1", "test-subject"))
            .thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> transformationService.getActiveTemplate("consumer-1", "test-subject"))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("No active transformation template found for consumer: consumer-1, subject: test-subject");
    }

    @Test
    void transform_shouldUseActiveTemplate() throws TransformationException {
        // Given
        Map<String, Object> canonicalJson = new HashMap<>();
        canonicalJson.put("id", 123);
        canonicalJson.put("name", "John Doe");

        Map<String, Object> transformedJson = new HashMap<>();
        transformedJson.put("transformed", true);

        TransformationRequest request = new TransformationRequest(canonicalJson, "test-subject");

        SchemaEntity inputSchema = new SchemaEntity();
        inputSchema.setId(1L);

        SchemaEntity outputSchema = new SchemaEntity();
        outputSchema.setId(2L);

        TransformationTemplateEntity template = new TransformationTemplateEntity();
        template.setId(1L);
        template.setConsumerId("consumer-1");
        template.setSubject("test-subject");
        template.setEngine("jslt");
        template.setTemplateExpression(".");
        template.setInputSchema(inputSchema);
        template.setOutputSchema(outputSchema);

        doNothing().when(consumerService).validateConsumerSubject("consumer-1", "test-subject");
        when(templateRepository.findByConsumerIdAndSubjectAndIsActiveTrue("consumer-1", "test-subject"))
            .thenReturn(Optional.of(template));
        when(jsltEngine.transform(canonicalJson, ".")).thenReturn(transformedJson);

        // When
        TransformationResponse response = transformationService.transform("consumer-1", request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getTransformedJson()).isEqualTo(transformedJson);
    }

    @Test
    void transform_shouldUseSpecificVersionWhenProvided() throws TransformationException {
        // Given
        Map<String, Object> canonicalJson = new HashMap<>();
        canonicalJson.put("id", 123);
        canonicalJson.put("name", "John Doe");

        Map<String, Object> transformedJson = new HashMap<>();
        transformedJson.put("transformed", true);

        TransformationRequest request = new TransformationRequest(canonicalJson, "test-subject", "2.0.0");

        SchemaEntity inputSchema = new SchemaEntity();
        inputSchema.setId(1L);

        SchemaEntity outputSchema = new SchemaEntity();
        outputSchema.setId(2L);

        TransformationTemplateEntity template = new TransformationTemplateEntity();
        template.setId(1L);
        template.setConsumerId("consumer-1");
        template.setSubject("test-subject");
        template.setVersion("2.0.0");
        template.setEngine("jslt");
        template.setTemplateExpression(".");
        template.setInputSchema(inputSchema);
        template.setOutputSchema(outputSchema);

        doNothing().when(consumerService).validateConsumerSubject("consumer-1", "test-subject");
        when(templateRepository.findByConsumerIdAndSubjectAndVersion("consumer-1", "test-subject", "2.0.0"))
            .thenReturn(Optional.of(template));
        when(jsltEngine.transform(canonicalJson, ".")).thenReturn(transformedJson);

        // When
        TransformationResponse response = transformationService.transform("consumer-1", request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getTransformedJson()).isEqualTo(transformedJson);
    }
}