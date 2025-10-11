# Multi-Subject Consumer Support Plan

## Overview

This document outlines the implementation plan for supporting multi-subject consumers in the Schema Registry using **Approach 1: Subject-in-Request**. In this approach, consumers can register for multiple subjects, and all API requests include a `subject` parameter to specify which subject the operation applies to. This allows for flexible consumer configurations while maintaining clear subject-specific operations.

## Approach Details

- **Consumer Registration**: Consumers register with a list of subjects they want to consume from.
- **Request Structure**: All consumer-related API calls (e.g., transform data, get configuration) include a `subject` query parameter or path variable.
- **Subject Validation**: The system validates that the consumer is registered for the specified subject before processing requests.
- **Backward Compatibility**: Existing single-subject consumers will be supported by treating them as consumers with one subject.

## Codebase Changes

### Entities
- [x] **ConsumerEntity**: Add a `subjects` field as `Set<String>` to store multiple subjects per consumer. Alternatively, create a new `ConsumerSubjectEntity` for a many-to-many relationship if more metadata is needed per subject.
- [x] **SchemaEntity**: Add a `version` field (String) to support semver versioning (e.g., "1.0.0") for evolution tracking.

### DTOs
- [x] **ConsumerRegistrationRequest**: Add `subjects` field as `List<String>` to allow specifying multiple subjects during registration.
- [x] **ConsumerResponse**: Include `subjects` in the response to show registered subjects.
- [x] **TransformationRequest**: Add `subject` field to specify which subject the transformation applies to.
- [x] **TransformationResponse**: Ensure responses include subject context.
- [x] **CompatibilityCheckRequest**: Update to include subject if needed for subject-specific checks.

### Repositories
- [x] **ConsumerRepository**: Update queries to support filtering by subject (e.g., find consumers by subject).
- [x] **SchemaRepository**: Add methods to retrieve schemas by subject and version.

### Services
- [x] **ConsumerService**:
  - Update `registerConsumer` to accept and validate multiple subjects.
  - Add method to validate consumer access to a specific subject.
  - Update consumer retrieval methods to support subject filtering.
- [x] **SchemaRegistryService**:
  - Add schema versioning logic (follow semver rules on updates, e.g., increment patch for backward-compatible changes).
  - Add methods to retrieve historical schemas by semver version.
- [x] **TransformationService**:
  - Update transformation logic to consider subject-specific configurations.
  - Add support for versioned transformations.
- [x] **ConfigurationValidator**: Update to validate multi-subject configurations and subject-specific rules.

### Controllers
- [x] **ConsumerController**:
  - Update registration endpoint to accept subjects list.
  - Modify all consumer endpoints to include `subject` as a required parameter (e.g., `/consumers/{consumerId}/transform?subject={subject}`).
  - Add validation to ensure consumer is registered for the subject.
- [x] **TransformationController**: Update endpoints to include subject parameter and apply subject-specific transformations.
- [x] **SchemaRegistryController**: Ensure schema operations consider subject context.

### Exception Handling
- Update `GlobalExceptionHandler` to handle subject-related errors (e.g., `SubjectNotRegisteredException`).

## Test Suite Changes

### Integration Tests
- [x] **tests/consumers/**:
  - Update `test-consumer-register.sh` to register consumers with multiple subjects.
  - Update `test-consumer-get.sh` and `test-consumer-list.sh` to test subject-specific operations.
  - Add new test scripts for multi-subject scenarios (e.g., `test-consumer-multi-subject.sh`).
- [x] **tests/transform/**:
  - Update transformation tests to include subject parameters.
  - Add tests for subject-specific transformations.
- [x] **tests/workflows/**:
  - Update `test-full-workflow.sh` and `test-schema-evolution.sh` to cover multi-subject consumer workflows.
- [x] **tests/error-handling/**:
  - Add tests for subject validation errors (e.g., consumer not registered for subject).

### Unit Tests
- [x] **ConsumerServiceTest**: Add tests for multi-subject registration, subject validation, and consumer retrieval by subject.
- [x] **SchemaRegistryServiceTest**: Add tests for schema versioning and historical schema retrieval.
- [x] **TransformationServiceTest**: Add tests for subject-specific and versioned transformations.
- [x] Update existing tests to accommodate multi-subject logic.

## Documentation Changes

### API Reference (api-reference.md)
- [x] Update all consumer-related endpoints to include `subject` parameter.
- [x] Document new request/response formats with subjects.
- [x] Add examples for multi-subject consumer registration and operations.

### Getting Started (getting-started.md)
- [x] Add section on registering multi-subject consumers.
- [x] Provide examples of subject-specific API calls.

### Architecture (architecture.md)
- [x] Update diagrams and descriptions to reflect multi-subject consumer support.
- [x] Explain subject-in-request approach and its benefits.

### Implementation Plan (implementation-plan.md)
- [x] Mark multi-subject consumer support as implemented.
- [x] Update any related implementation notes.

### Other Docs
- [x] Update troubleshooting.md with common issues related to multi-subject consumers.
- [x] Update testing.md to include multi-subject test scenarios.

## Handling Schema Evolution and Historical Re-processing

### Schema Versioning
- Each schema update follows semver rules (major.minor.patch, e.g., "1.0.0" → "1.1.0" for minor changes) within its subject.
- Schemas are stored with version history, allowing retrieval of past versions using semver comparisons.

### Transformation Versioning
- Transformation templates are linked to specific schema versions.
- When a consumer registers, they specify transformations per subject, which can be version-specific.

### Re-processing Historical JSONs
- **Scenario**: Historical JSON created with canonical schema v1, current schema is v2, consumer requests re-processing.
- **Approach**:
  1. **Version Detection**: Determine the original schema version used for the historical JSON (stored as metadata or inferred).
  2. **Transformation Selection**: For the consumer-subject pair, select the appropriate transformation based on the original version and current consumer schema.
  3. **Backward Compatibility**: If no specific transformation exists for the version, fall back to the latest transformation or apply a default mapping.
  4. **Re-processing API**: Add a new endpoint `/consumers/{consumerId}/reprocess` that accepts historical JSON, original schema version, and target subject.
  5. **Storage Considerations**: Store transformation history in `TransformationTemplateEntity` with version fields, or create a new entity for transformation versions.

### Consumer-Specific Transformations
- Consumers can have different transformation logic per subject.
- For historical re-processing, the system applies the consumer's current transformation for that subject, potentially adapting it based on schema version differences.

### Implementation Details
- **Metadata Storage**: Add fields to store schema version (semver string) with JSON payloads or in a separate audit table.
- **Version Management**: Integrate a semver library (e.g., Maven dependency like `com.github.zafarkhaja:java-semver`) for parsing, comparing, and incrementing versions.
- **Transformation Engine Updates**: Modify `JsltTransformationEngine`, `PipelineTransformationEngine`, and `RouterTransformationEngine` to accept version parameters and apply version-aware transformations.
- **Error Handling**: Handle cases where historical schema versions are incompatible with current transformations, using semver compatibility rules.

This plan ensures robust support for multi-subject consumers while addressing the complexities of schema evolution and historical data re-processing.