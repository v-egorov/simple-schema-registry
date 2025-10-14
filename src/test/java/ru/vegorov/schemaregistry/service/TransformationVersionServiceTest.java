package ru.vegorov.schemaregistry.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.vegorov.schemaregistry.dto.TransformationTemplateResponse;
import ru.vegorov.schemaregistry.entity.SchemaEntity;
import ru.vegorov.schemaregistry.entity.TransformationTemplateEntity;
import ru.vegorov.schemaregistry.exception.ConflictException;
import ru.vegorov.schemaregistry.exception.ResourceNotFoundException;
import ru.vegorov.schemaregistry.repository.TransformationTemplateRepository;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransformationVersionServiceTest {

    @Mock
    private TransformationTemplateRepository templateRepository;

    private TransformationVersionService versionService;

    @BeforeEach
    void setUp() {
        versionService = new TransformationVersionService(templateRepository);
    }

    @Test
    void activateVersion_shouldActivateSpecifiedVersion() {
        // Given
        SchemaEntity inputSchema = new SchemaEntity();
        inputSchema.setId(1L);

        SchemaEntity outputSchema = new SchemaEntity();
        outputSchema.setId(2L);

        TransformationTemplateEntity template = new TransformationTemplateEntity();
        template.setId(1L);
        template.setConsumerId("consumer-1");
        template.setSubject("test-subject");
        template.setVersion("2.0.0");
        template.setIsActive(false);
        template.setInputSchema(inputSchema);
        template.setOutputSchema(outputSchema);

        TransformationTemplateEntity savedTemplate = new TransformationTemplateEntity();
        savedTemplate.setId(1L);
        savedTemplate.setConsumerId("consumer-1");
        savedTemplate.setSubject("test-subject");
        savedTemplate.setVersion("2.0.0");
        savedTemplate.setIsActive(true);
        savedTemplate.setInputSchema(inputSchema);
        savedTemplate.setOutputSchema(outputSchema);

        when(templateRepository.findByConsumerIdAndSubjectAndVersion("consumer-1", "test-subject", "2.0.0"))
            .thenReturn(Optional.of(template));
        when(templateRepository.save(any(TransformationTemplateEntity.class))).thenReturn(savedTemplate);

        // When
        TransformationTemplateResponse response = versionService.activateVersion("consumer-1", "test-subject", "2.0.0");

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getVersion()).isEqualTo("2.0.0");
        assertThat(response.getIsActive()).isTrue();
        verify(templateRepository).save(any(TransformationTemplateEntity.class));
    }

    @Test
    void activateVersion_shouldThrowWhenVersionDoesNotExist() {
        // Given
        when(templateRepository.findByConsumerIdAndSubjectAndVersion("consumer-1", "test-subject", "2.0.0"))
            .thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> versionService.activateVersion("consumer-1", "test-subject", "2.0.0"))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("Transformation template version not found");
    }

    @Test
    void deactivateVersion_shouldDeactivateSpecifiedVersion() {
        // Given
        SchemaEntity inputSchema = new SchemaEntity();
        inputSchema.setId(1L);

        SchemaEntity outputSchema = new SchemaEntity();
        outputSchema.setId(2L);

        TransformationTemplateEntity template = new TransformationTemplateEntity();
        template.setId(1L);
        template.setConsumerId("consumer-1");
        template.setSubject("test-subject");
        template.setVersion("1.0.0");
        template.setIsActive(true);
        template.setInputSchema(inputSchema);
        template.setOutputSchema(outputSchema);

        TransformationTemplateEntity savedTemplate = new TransformationTemplateEntity();
        savedTemplate.setId(1L);
        savedTemplate.setConsumerId("consumer-1");
        savedTemplate.setSubject("test-subject");
        savedTemplate.setVersion("1.0.0");
        savedTemplate.setIsActive(false);
        savedTemplate.setInputSchema(inputSchema);
        savedTemplate.setOutputSchema(outputSchema);

        when(templateRepository.findByConsumerIdAndSubjectAndVersion("consumer-1", "test-subject", "1.0.0"))
            .thenReturn(Optional.of(template));
        when(templateRepository.countByConsumerIdAndSubject("consumer-1", "test-subject")).thenReturn(2L);
        when(templateRepository.save(any(TransformationTemplateEntity.class))).thenReturn(savedTemplate);

        // When
        TransformationTemplateResponse response = versionService.deactivateVersion("consumer-1", "test-subject", "1.0.0");

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getVersion()).isEqualTo("1.0.0");
        assertThat(response.getIsActive()).isFalse();
    }

    @Test
    void deactivateVersion_shouldThrowWhenVersionIsAlreadyInactive() {
        // Given
        TransformationTemplateEntity template = new TransformationTemplateEntity();
        template.setId(1L);
        template.setIsActive(false);

        when(templateRepository.findByConsumerIdAndSubjectAndVersion("consumer-1", "test-subject", "1.0.0"))
            .thenReturn(Optional.of(template));

        // When & Then
        assertThatThrownBy(() -> versionService.deactivateVersion("consumer-1", "test-subject", "1.0.0"))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("Version is already inactive");
    }

    @Test
    void deactivateVersion_shouldThrowWhenOnlyOneVersionExists() {
        // Given
        TransformationTemplateEntity template = new TransformationTemplateEntity();
        template.setId(1L);
        template.setIsActive(true);

        when(templateRepository.findByConsumerIdAndSubjectAndVersion("consumer-1", "test-subject", "1.0.0"))
            .thenReturn(Optional.of(template));
        when(templateRepository.countByConsumerIdAndSubject("consumer-1", "test-subject")).thenReturn(1L);

        // When & Then
        assertThatThrownBy(() -> versionService.deactivateVersion("consumer-1", "test-subject", "1.0.0"))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("Cannot deactivate the only version");
    }

    @Test
    void getActiveVersion_shouldReturnActiveVersion() {
        // Given
        SchemaEntity inputSchema = new SchemaEntity();
        inputSchema.setId(1L);

        SchemaEntity outputSchema = new SchemaEntity();
        outputSchema.setId(2L);

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
        TransformationTemplateResponse response = versionService.getActiveVersion("consumer-1", "test-subject");

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getVersion()).isEqualTo("1.0.0");
        assertThat(response.getIsActive()).isTrue();
    }

    @Test
    void getActiveVersion_shouldThrowWhenNoActiveVersion() {
        // Given
        when(templateRepository.findByConsumerIdAndSubjectAndIsActiveTrue("consumer-1", "test-subject"))
            .thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> versionService.getActiveVersion("consumer-1", "test-subject"))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("No active version found");
    }

    @Test
    void getVersionHistory_shouldReturnAllVersionsOrderedByVersionDesc() {
        // Given
        SchemaEntity inputSchema = new SchemaEntity();
        inputSchema.setId(1L);

        SchemaEntity outputSchema = new SchemaEntity();
        outputSchema.setId(2L);

        TransformationTemplateEntity template1 = new TransformationTemplateEntity();
        template1.setId(1L);
        template1.setVersion("2.0.0");
        template1.setInputSchema(inputSchema);
        template1.setOutputSchema(outputSchema);

        TransformationTemplateEntity template2 = new TransformationTemplateEntity();
        template2.setId(2L);
        template2.setVersion("1.0.0");
        template2.setInputSchema(inputSchema);
        template2.setOutputSchema(outputSchema);

        List<TransformationTemplateEntity> templates = Arrays.asList(template1, template2);

        when(templateRepository.findByConsumerIdAndSubject("consumer-1", "test-subject"))
            .thenReturn(templates);

        // When
        List<TransformationTemplateResponse> responses = versionService.getVersionHistory("consumer-1", "test-subject");

        // Then
        assertThat(responses).hasSize(2);
        assertThat(responses.get(0).getVersion()).isEqualTo("2.0.0");
        assertThat(responses.get(1).getVersion()).isEqualTo("1.0.0");
    }

    @Test
    void deleteVersion_shouldDeleteVersionWhenNotActive() {
        // Given
        TransformationTemplateEntity template = new TransformationTemplateEntity();
        template.setId(1L);
        template.setVersion("1.0.0");
        template.setIsActive(false);

        when(templateRepository.findByConsumerIdAndSubjectAndVersion("consumer-1", "test-subject", "1.0.0"))
            .thenReturn(Optional.of(template));
        when(templateRepository.countByConsumerIdAndSubject("consumer-1", "test-subject")).thenReturn(2L);

        // When
        versionService.deleteVersion("consumer-1", "test-subject", "1.0.0");

        // Then
        verify(templateRepository).delete(template);
    }

    @Test
    void deleteVersion_shouldThrowWhenVersionIsActive() {
        // Given
        TransformationTemplateEntity template = new TransformationTemplateEntity();
        template.setId(1L);
        template.setVersion("1.0.0");
        template.setIsActive(true);

        when(templateRepository.findByConsumerIdAndSubjectAndVersion("consumer-1", "test-subject", "1.0.0"))
            .thenReturn(Optional.of(template));

        // When & Then
        assertThatThrownBy(() -> versionService.deleteVersion("consumer-1", "test-subject", "1.0.0"))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("Cannot delete active version");
    }

    @Test
    void deleteVersion_shouldThrowWhenOnlyOneVersionExists() {
        // Given
        TransformationTemplateEntity template = new TransformationTemplateEntity();
        template.setId(1L);
        template.setVersion("1.0.0");
        template.setIsActive(false);

        when(templateRepository.findByConsumerIdAndSubjectAndVersion("consumer-1", "test-subject", "1.0.0"))
            .thenReturn(Optional.of(template));
        when(templateRepository.countByConsumerIdAndSubject("consumer-1", "test-subject")).thenReturn(1L);

        // When & Then
        assertThatThrownBy(() -> versionService.deleteVersion("consumer-1", "test-subject", "1.0.0"))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("Cannot delete the only version");
    }
}