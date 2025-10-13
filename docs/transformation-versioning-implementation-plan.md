# Transformation Versioning Implementation Plan

## Overview

This document provides a detailed implementation plan for the transformation versioning refactor based on the following decisions:

- **Database Schema**: Option 1 (Subject-Consumer-Versioned Templates) with partial unique index
- **API Changes**: Option B (Version in Path)
- **Database Migration**: Start from scratch - no data migration required
- **Implementation Approach**: Option 1 (Complete Refactor)
- **Rollback**: Not required

## Implementation Phases

### Phase 1: Database Schema Implementation
**Status**: ⏳ Not Started

#### 1.1 Enhanced Schemas Table
**Status**: ⏳ Not Started
**Description**: Modify the existing `schemas` table to support multiple schema types
**Tasks**:
- [ ] Add `schema_type` column (VARCHAR(50), NOT NULL, CHECK constraint for 'canonical'/'consumer_output')
- [ ] Add `consumer_id` column (VARCHAR(255), nullable)
- [ ] Add CHECK constraint: `schema_type = 'canonical' OR consumer_id IS NOT NULL`
- [ ] Update unique constraint: `UNIQUE(subject, schema_type, consumer_id, version)`
- [ ] Add foreign key: `consumer_id` → `consumers.consumer_id` (ON DELETE CASCADE)
- [ ] Add indexes: `idx_schemas_subject_type`, `idx_schemas_consumer`

#### 1.2 Transformation Templates Table (Complete Rewrite)
**Status**: ⏳ Not Started
**Description**: Drop and recreate transformation_templates table with proper relationships
**Tasks**:
- [ ] Drop existing `transformation_templates` table
- [ ] Create new `transformation_templates` table with columns:
  - `id` (BIGSERIAL PRIMARY KEY)
  - `consumer_id` (VARCHAR(255) NOT NULL, FK to consumers)
  - `subject` (VARCHAR(255) NOT NULL)
  - `version` (VARCHAR(50) NOT NULL, semver format)
  - `engine` (VARCHAR(50) NOT NULL DEFAULT 'jslt')
  - `template_expression` (TEXT, nullable for router/pipeline)
  - `configuration` (TEXT, nullable for router/pipeline)
  - `input_schema_id` (BIGINT NOT NULL, FK to schemas)
  - `output_schema_id` (BIGINT NOT NULL, FK to schemas)
  - `is_active` (BOOLEAN NOT NULL DEFAULT FALSE)
  - `description` (TEXT)
  - `created_at`, `updated_at` (TIMESTAMP WITH TIME ZONE)
- [ ] Add constraints:
  - `UNIQUE(consumer_id, subject, version)`
  - `CHECK (is_active IN (TRUE, FALSE))`
  - Foreign keys with RESTRICT on delete
- [ ] Create partial unique index for active versions
- [ ] Create performance indexes

#### 1.3 Database Migration Script
**Status**: ✅ Completed
**Description**: Create Flyway migration script for schema changes
**Tasks**:
- [x] Create V4__Transformation_versioning_refactor.sql
- [x] Include all schema changes in proper order
- [x] Add comments explaining each change
- [x] Test migration on clean database

### Phase 2: Entity and DTO Updates
**Status**: ⏳ Not Started

#### 2.1 SchemaEntity Updates
**Status**: ✅ Completed
**Description**: Update SchemaEntity to support new schema types
**Tasks**:
- [x] Add `schemaType` field (enum: CANONICAL, CONSUMER_OUTPUT)
- [x] Add `consumerId` field (nullable)
- [x] Update constructors and getters/setters
- [x] Update validation annotations
- [x] Update toString and equals/hashCode methods

#### 2.2 TransformationTemplateEntity (Complete Rewrite)
**Status**: ✅ Completed
**Description**: Rewrite TransformationTemplateEntity with new relationships
**Tasks**:
- [x] Remove old TransformationTemplateEntity
- [x] Create new entity with proper JPA mappings:
  - @ManyToOne relationships to SchemaEntity for input/output schemas
  - @Column mappings for all fields
  - @PrePersist/@PreUpdate for timestamps
- [x] Add validation annotations
- [x] Update repository interfaces

#### 2.3 New DTOs
**Status**: ⏳ Not Started
**Description**: Create DTOs for new API structure
**Tasks**:
- [ ] SchemaValidationRequest (existing, may need updates)
- [ ] SchemaValidationResponse (existing, may need updates)
- [ ] TransformationTemplateRequest (update for schema IDs)
- [ ] TransformationTemplateResponse (update for schema details)
- [ ] TransformationRequest (add version field)
- [ ] TransformationResponse (may need updates)

### Phase 3: Repository Layer Updates
**Status**: ⏳ Not Started

#### 3.1 SchemaRepository Updates
**Status**: ⏳ Not Started
**Description**: Update queries for new schema structure
**Tasks**:
- [ ] Update findBySubjectAndVersion to consider schema_type
- [ ] Add methods for finding canonical schemas
- [ ] Add methods for finding consumer output schemas
- [ ] Update existing query methods

#### 3.2 TransformationTemplateRepository (Complete Rewrite)
**Status**: ⏳ Not Started
**Description**: Rewrite repository for new table structure
**Tasks**:
- [ ] Remove old TransformationTemplateRepository
- [ ] Create new repository with methods:
  - `findByConsumerIdAndSubjectAndVersion`
  - `findActiveByConsumerIdAndSubject` (using @Query with JOIN)
  - `findByConsumerIdAndSubjectOrderByVersionDesc`
  - `existsByConsumerIdAndSubject`
- [ ] Add custom query methods for version management

### Phase 4: Service Layer Implementation
**Status**: ⏳ Not Started

#### 4.1 SchemaRegistryService Updates
**Status**: ⏳ Not Started
**Description**: Update for new schema types and relationships
**Tasks**:
- [ ] Update registerSchema to handle schema_type
- [ ] Add methods for registering consumer output schemas
- [ ] Update validation logic for schema types
- [ ] Update getLatestSchema to consider schema_type

#### 4.2 TransformationService (Major Rewrite)
**Status**: ⏳ Not Started
**Description**: Complete rewrite for new architecture
**Tasks**:
- [ ] Rewrite transform() method to handle version parameter
- [ ] Update template retrieval logic with foreign key joins
- [ ] Add version validation and active version selection
- [ ] Update error handling for referential integrity
- [ ] Implement historical processing with version specification

#### 4.3 New TransformationVersionService
**Status**: ⏳ Not Started
**Description**: Implement version management service
**Tasks**:
- [ ] Create TransformationVersionService class
- [ ] Implement createNewVersion() with schema validation
- [ ] Implement activateVersion() with transaction safety
- [ ] Implement getAvailableVersions()
- [ ] Add schema compatibility validation methods

### Phase 5: Controller Layer Updates
**Status**: ⏳ Not Started

#### 5.1 SchemaRegistryController Updates
**Status**: ⏳ Not Started
**Description**: Update for new schema types
**Tasks**:
- [ ] Update registerSchema endpoint to accept schema_type
- [ ] Add endpoints for consumer output schema management
- [ ] Update validation endpoints for new schema structure
- [ ] Update response DTOs

#### 5.2 TransformationController (Major Rewrite)
**Status**: ⏳ Not Started
**Description**: Implement new API structure (Option B: Version in Path)
**Tasks**:
- [ ] Remove old transformation endpoints
- [ ] Implement new endpoint structure:
  - `POST /api/consumers/{consumerId}/subjects/{subject}/transform`
  - `POST /api/consumers/{consumerId}/subjects/{subject}/transform/{version}`
- [ ] Update request/response handling
- [ ] Add version parameter validation
- [ ] Update error responses

#### 5.3 New Template Management Endpoints
**Status**: ⏳ Not Started
**Description**: Implement template CRUD with version management
**Tasks**:
- [ ] `POST /api/consumers/{consumerId}/subjects/{subject}/templates`
- [ ] `GET /api/consumers/{consumerId}/subjects/{subject}/templates`
- [ ] `PUT /api/consumers/{consumerId}/subjects/{subject}/templates/{version}/activate`
- [ ] Add proper validation and error handling

### Phase 6: Testing Implementation
**Status**: ⏳ Not Started

#### 6.1 Unit Tests
**Status**: ⏳ Not Started
**Description**: Update and create unit tests
**Tasks**:
- [ ] Update SchemaRegistryServiceTest for new schema types
- [ ] Rewrite TransformationServiceTest for new architecture
- [ ] Create TransformationVersionServiceTest
- [ ] Update entity tests for new relationships
- [ ] Test referential integrity constraints

#### 6.2 Integration Tests
**Status**: ⏳ Not Started
**Description**: Create comprehensive integration tests
**Tasks**:
- [ ] Test schema registration for both types
- [ ] Test transformation template CRUD operations
- [ ] Test version activation/deactivation
- [ ] Test transformation execution with version selection
- [ ] Test referential integrity (foreign key constraints)

#### 6.3 API Tests
**Status**: ⏳ Not Started
**Description**: Test new API endpoints
**Tasks**:
- [ ] Test new transformation endpoints with version parameters
- [ ] Test template management endpoints
- [ ] Test schema validation endpoints
- [ ] Test error scenarios and edge cases

### Phase 7: Documentation Updates
**Status**: ⏳ Not Started

#### 7.1 API Reference Updates
**Status**: ⏳ Not Started
**Description**: Update api-reference.md for new endpoints
**Tasks**:
- [ ] Document new transformation endpoints
- [ ] Document template management endpoints
- [ ] Update schema registration documentation
- [ ] Add examples for version management

#### 7.2 Getting Started Updates
**Status**: ⏳ Not Started
**Description**: Update getting-started.md with new workflows
**Tasks**:
- [ ] Update examples to use new API structure
- [ ] Add version management examples
- [ ] Update curl examples for new endpoints
- [ ] Add schema type registration examples

### Phase 8: Validation and Deployment
**Status**: ⏳ Not Started

#### 8.1 System Integration Testing
**Status**: ⏳ Not Started
**Description**: End-to-end testing of the complete system
**Tasks**:
- [ ] Test complete workflows: schema → template → transformation
- [ ] Test version management scenarios
- [ ] Test multi-subject consumer scenarios
- [ ] Performance testing with new schema

#### 8.2 Production Readiness
**Status**: ⏳ Not Started
**Description**: Final validation before deployment
**Tasks**:
- [ ] Database migration testing on production-like environment
- [ ] API compatibility verification
- [ ] Performance benchmarking
- [ ] Security review of new endpoints

## Progress Tracking

### Overall Progress: 0%

- [ ] Phase 1: Database Schema Implementation (0/3 tasks)
- [ ] Phase 2: Entity and DTO Updates (0/3 tasks)
- [ ] Phase 3: Repository Layer Updates (0/2 tasks)
- [ ] Phase 4: Service Layer Implementation (0/3 tasks)
- [ ] Phase 5: Controller Layer Updates (0/3 tasks)
- [ ] Phase 6: Testing Implementation (0/3 tasks)
- [ ] Phase 7: Documentation Updates (0/2 tasks)
- [ ] Phase 8: Validation and Deployment (0/2 tasks)

## Risk Mitigation

### Technical Risks
- **Schema Migration Complexity**: Starting from scratch eliminates migration risks
- **Referential Integrity Issues**: Foreign key constraints will catch issues early
- **API Breaking Changes**: Complete refactor allows clean API design

### Timeline Risks
- **Development Complexity**: Major architectural changes require careful implementation
- **Testing Coverage**: Comprehensive testing needed for complex relationships
- **Integration Issues**: New relationships may reveal unexpected interactions

## Success Criteria

- [ ] All database constraints properly enforced
- [ ] All foreign key relationships working correctly
- [ ] API endpoints returning expected responses
- [ ] Version management working as specified
- [ ] All tests passing (unit, integration, API)
- [ ] Documentation updated and accurate
- [ ] Performance meets requirements
- [ ] No data integrity issues in testing

## Next Steps

1. Begin with Phase 1: Database schema implementation
2. Create Flyway migration script
3. Test migration on clean database
4. Proceed to entity/DTO updates
5. Implement service layer changes
6. Update controllers and APIs
7. Comprehensive testing
8. Documentation updates
9. Final validation and deployment