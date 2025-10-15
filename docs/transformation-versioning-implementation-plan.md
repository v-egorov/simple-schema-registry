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
**Status**: ✅ Completed

#### 1.1 Enhanced Schemas Table
**Status**: ✅ Completed
**Description**: Modify the existing `schemas` table to support multiple schema types
**Tasks**:
- [x] Add `schema_type` column (VARCHAR(50), NOT NULL, CHECK constraint for 'canonical'/'consumer_output')
- [x] Add `consumer_id` column (VARCHAR(255), nullable)
- [x] Add CHECK constraint: `schema_type = 'canonical' OR consumer_id IS NOT NULL`
- [x] Update unique constraint: `UNIQUE(subject, schema_type, consumer_id, version)`
- [x] Add foreign key: `consumer_id` → `consumers.consumer_id` (ON DELETE CASCADE)
- [x] Add indexes: `idx_schemas_subject_type`, `idx_schemas_consumer`

#### 1.2 Transformation Templates Table (Complete Rewrite)
**Status**: ✅ Completed
**Description**: Drop and recreate transformation_templates table with proper relationships
**Tasks**:
- [x] Drop existing `transformation_templates` table
- [x] Create new `transformation_templates` table with columns:
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
- [x] Add constraints:
  - `UNIQUE(consumer_id, subject, version)`
  - `CHECK (is_active IN (TRUE, FALSE))`
  - Foreign keys with RESTRICT on delete
- [x] Create partial unique index for active versions
- [x] Create performance indexes

#### 1.3 Database Migration Script
**Status**: ✅ Completed
**Description**: Create Flyway migration script for schema changes
**Tasks**:
- [x] Create V4__Transformation_versioning_refactor.sql
- [x] Include all schema changes in proper order
- [x] Add comments explaining each change
- [x] Test migration on clean database

### Phase 2: Entity and DTO Updates
**Status**: ✅ Completed

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
**Status**: ✅ Completed
**Description**: Create DTOs for new API structure
**Tasks**:
- [x] SchemaValidationRequest (existing, may need updates)
- [x] SchemaValidationResponse (existing, may need updates)
- [x] TransformationTemplateRequest (update for schema IDs)
- [x] TransformationTemplateResponse (update for schema details)
- [x] TransformationRequest (add version field)
- [x] TransformationResponse (may need updates)

### Phase 3: Repository Layer Updates
**Status**: ✅ Completed

#### 3.1 SchemaRepository Updates
**Status**: ✅ Completed
**Description**: Update queries for new schema structure
**Tasks**:
- [x] Update findBySubjectAndVersion to consider schema_type
- [x] Add methods for finding canonical schemas
- [x] Add methods for finding consumer output schemas
- [x] Update existing query methods

#### 3.2 TransformationTemplateRepository (Complete Rewrite)
**Status**: ✅ Completed
**Description**: Rewrite repository for new table structure
**Tasks**:
- [x] Remove old TransformationTemplateRepository
- [x] Create new repository with methods:
  - `findByConsumerIdAndSubjectAndVersion`
  - `findActiveByConsumerIdAndSubject` (using @Query with JOIN)
  - `findByConsumerIdAndSubjectOrderByVersionDesc`
  - `existsByConsumerIdAndSubject`
- [x] Add custom query methods for version management

### Phase 4: Service Layer Implementation
**Status**: ✅ Completed

#### 4.1 SchemaRegistryService Updates
**Status**: ✅ Completed
**Description**: Update for new schema types and relationships
**Tasks**:
- [x] Update registerSchema to handle schema_type
- [x] Add methods for registering consumer output schemas
- [x] Update validation logic for schema types
- [x] Update getLatestSchema to consider schema_type

#### 4.2 TransformationService (Major Rewrite)
**Status**: ✅ Completed
**Description**: Complete rewrite for new architecture
**Tasks**:
- [x] Rewrite transform() method to handle version parameter
- [x] Update template retrieval logic with foreign key joins
- [x] Add version validation and active version selection
- [x] Update error handling for referential integrity
- [x] Implement historical processing with version specification

#### 4.3 New TransformationVersionService
**Status**: ✅ Completed
**Description**: Implement version management service
**Tasks**:
- [x] Create TransformationVersionService class
- [x] Implement createNewVersion() with schema validation
- [x] Implement activateVersion() with transaction safety
- [x] Implement getAvailableVersions()
- [x] Add schema compatibility validation methods

##### activateVersion() Implementation Details

The `activateVersion()` method implements atomic version switching with the following logic:

1. **Version Existence Check**: Validates the requested version exists for the consumer-subject pair
2. **Active Status Check**: Returns early if the version is already active (no-op operation)
3. **Current Version Deactivation**: Finds and deactivates any currently active version using `saveAndFlush()` for immediate database commit
4. **New Version Activation**: Activates the requested version and saves changes
5. **Database Constraints**: Relies on partial unique index ensuring only one active version per consumer-subject pair

**Why Manual Activation (No Auto-Activation)**:
- **Safety First**: Prevents untested transformations from being automatically activated
- **Operational Control**: Allows validation and testing before production deployment
- **Rollback Capability**: Enables quick switching between versions for emergency scenarios
- **Audit Compliance**: Maintains clear activation history for regulatory requirements
- **Staged Rollouts**: Supports gradual deployment strategies and A/B testing

**Note**: This is NOT auto-versioning (automatic version number assignment). Users must explicitly specify semantic version numbers for each template.

### Phase 5: Controller Layer Updates
**Status**: ✅ Completed

#### 5.1 SchemaRegistryController Updates
**Status**: ✅ Completed
**Description**: Update for new schema types
**Tasks**:
- [x] Update registerSchema endpoint to accept schema_type
- [x] Add endpoints for consumer output schema management
- [x] Update validation endpoints for new schema structure
- [x] Update response DTOs

#### 5.2 TransformationController (Major Rewrite)
**Status**: ✅ Completed
**Description**: Implement new API structure (Option B: Version in Path)
**Tasks**:
- [x] Remove old transformation endpoints
- [x] Implement new endpoint structure:
  - `POST /api/consumers/{consumerId}/subjects/{subject}/transform`
  - `POST /api/consumers/{consumerId}/subjects/{subject}/transform/{version}`
- [x] Update request/response handling
- [x] Add version parameter validation
- [x] Update error responses

#### 5.3 New Template Management Endpoints
**Status**: ✅ Completed
**Description**: Implement template CRUD with version management
**Tasks**:
- [x] `POST /api/consumers/{consumerId}/subjects/{subject}/templates`
- [x] `GET /api/consumers/{consumerId}/subjects/{subject}/templates`
- [x] `PUT /api/consumers/{consumerId}/subjects/{subject}/templates/{version}/activate`
- [x] Add proper validation and error handling

### Phase 6: Testing Implementation
**Status**: ✅ Completed

#### 6.1 Unit Tests
**Status**: ✅ Completed
**Description**: Update and create unit tests
**Tasks**:
- [x] Update SchemaRegistryServiceTest for new schema types
- [x] Rewrite TransformationServiceTest for new architecture
- [x] Create TransformationVersionServiceTest
- [x] Update entity tests for new relationships
- [x] Test referential integrity constraints

#### 6.2 Integration Tests
**Status**: ✅ Completed
**Description**: Create comprehensive integration tests
**Tasks**:
- [x] Test schema registration for both types
- [x] Test transformation template CRUD operations
- [x] Test version activation/deactivation
- [x] Test transformation execution with version selection
- [x] Test referential integrity (foreign key constraints)

#### 6.3 API Tests
**Status**: ✅ Completed
**Description**: Test new API endpoints
**Tasks**:
- [x] Test new transformation endpoints with version parameters
- [x] Test template management endpoints
- [x] Test schema validation endpoints
- [x] Test error scenarios and edge cases

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

### Overall Progress: 75% (6/8 phases completed)

- [x] Phase 1: Database Schema Implementation (3/3 tasks)
- [x] Phase 2: Entity and DTO Updates (3/3 tasks)
- [x] Phase 3: Repository Layer Updates (2/2 tasks)
- [x] Phase 4: Service Layer Implementation (3/3 tasks)
- [x] Phase 5: Controller Layer Updates (3/3 tasks)
- [x] Phase 6: Testing Implementation (3/3 tasks)
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

✅ **COMPLETED PHASES (1-5):**
1. Database schema implementation - V4 migration script created and tested
2. Entity and DTO updates - All entities and DTOs updated for new architecture
3. Repository layer updates - SchemaRepository and TransformationTemplateRepository rewritten
4. Service layer implementation - SchemaRegistryService, TransformationService, and TransformationVersionService completed
5. Controller layer updates - New "Version in Path" API structure implemented

🔄 **REMAINING PHASES (6-8):**
6. **Testing Implementation** - Update unit tests and create comprehensive integration tests
7. **Documentation Updates** - Update API reference and getting started guides
8. **Validation and Deployment** - End-to-end testing and production readiness validation

**Immediate Next Steps:**
1. ✅ Update existing unit tests to work with new architecture - COMPLETED
2. ✅ Create integration tests for new API endpoints - COMPLETED
3. Update API documentation with new endpoints
4. Perform end-to-end testing of complete workflows