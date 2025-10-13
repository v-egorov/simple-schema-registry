# Transformation Versioning Refactor Plan

## Current Architecture Issues

### Problems Identified

1. **1:1 Consumer-Template Relationship**: Current design allows only one transformation template per consumer, contradicting multi-subject consumer requirements
2. **No Versioning**: Transformations cannot evolve with schema changes
3. **No Fallback Mechanism**: Cannot rollback to previous transformation versions during production issues
4. **Limited Flexibility**: Cannot process historical data with different schema versions

### Business Requirements

- **Multi-Subject Support**: Consumers can transform multiple subjects
- **Schema Evolution**: Support for evolving canonical schemas and consumer output schemas
- **Transformation Versioning**: Semver-based versioning linked to subject:consumer pairs
- **Active/Default Versions**: Only one transformation version active per subject:consumer pair at a time
- **Fallback Capability**: Ability to switch back to previous versions for operational issues
- **Historical Processing**: Ability to specify transformation version in API calls for reprocessing
- **Breaking Changes Allowed**: No existing users, development stage allows major changes

## Proposed New Architecture

### Core Concepts

1. **Transformation Templates**: Versioned transformation logic per subject:consumer pair
2. **Active Versions**: Each subject:consumer pair has one active transformation version
3. **Version Selection**: API can specify version or use active version
4. **Schema Compatibility**: Transformations linked to specific schema versions

### Important Constraint Correction

**Issue Identified**: Initial design proposed `UNIQUE(consumer_id, subject, is_active)` which would only allow two records per consumer-subject pair (true/false). This was incorrect.

**Solution**: Use partial unique indexes to ensure only one active version per consumer-subject pair while allowing multiple inactive versions:

```sql
-- For Option 1
CREATE UNIQUE INDEX idx_transformation_templates_active_only
ON transformation_templates(consumer_id, subject)
WHERE is_active = TRUE;

-- For Option 2
CREATE UNIQUE INDEX idx_template_versions_active_only
ON transformation_template_versions(template_id)
WHERE is_active = TRUE;
```

### Column Semantics Clarification

#### Subject Column
- **Purpose**: Identifies the data subject (domain/type) this transformation applies to
- **Usage**: Since consumers can consume multiple subjects, each transformation template is scoped to a specific subject
- **Example**: A consumer might have transformations for "user-profile" and "product-catalog" subjects
- **Relationship**: Links to the `subject` field in the `schemas` table for validation

#### Input Schema Version Column
- **Purpose**: Specifies which version of the subject's canonical schema this transformation is designed to handle
- **Usage**: When subject schemas evolve (e.g., from "1.0.0" to "1.1.0"), different transformation versions may be needed to handle different input formats
- **Semantics**: References the `version` field in the `schemas` table for the same subject
- **Example**: Transformation version "2.0.0" might be designed to handle subject schema version "1.1.0" while transformation "1.0.0" handles "1.0.0"

#### Output Schema Version Column
- **Purpose**: Specifies which version of the consumer's expected output schema this transformation produces
- **Usage**: As consumer requirements evolve, they may expect different output formats or additional fields
- **Semantics**: Tracks the consumer's output contract version, independent of the transformation version
- **Example**: Consumer might evolve from expecting simple user data (output v1.0) to enriched user data (output v2.0), requiring different transformations

### Practical Usage Examples

#### Scenario 1: Subject Schema Evolution
```
Subject: "user-profile"
Subject Schema Versions: 1.0.0 → 1.1.0 (added phone field)
Consumer: "mobile-app"
Output Schema Versions: 1.0 (simple), 2.0 (with phone)

Transformation Versions:
- v1.0.0: input_schema_version="1.0.0", output_schema_version="1.0"
- v1.1.0: input_schema_version="1.1.0", output_schema_version="1.0" (backwards compatible)
- v2.0.0: input_schema_version="1.1.0", output_schema_version="2.0" (includes phone)
```

#### Scenario 2: Consumer Evolution
```
Subject: "product-catalog"
Subject Schema Version: 1.0.0 (stable)
Consumer: "web-dashboard"
Output Schema Versions: 1.0 → 2.0 (added analytics fields)

Transformation Versions:
- v1.0.0: input_schema_version="1.0.0", output_schema_version="1.0"
- v2.0.0: input_schema_version="1.0.0", output_schema_version="2.0" (same input, enhanced output)
```

#### Version Selection Logic
When processing a transformation request:
1. **Subject** determines which transformation templates are eligible
2. **Input Schema Version** should match the current subject schema version being processed
3. **Active Flag** determines which version to use by default
4. **Output Schema Version** documents what the consumer receives

This design enables:
- **Historical Processing**: Specify exact transformation version for reprocessing old data
- **Gradual Rollout**: Test new transformations while keeping old versions available
- **Schema Evolution**: Handle both input and output schema changes independently

### Database Schema Changes

#### Option 1: Subject-Consumer-Versioned Templates

```sql
-- New transformation_templates table structure
CREATE TABLE transformation_templates (
    id BIGSERIAL PRIMARY KEY,
    consumer_id VARCHAR(255) NOT NULL,
    subject VARCHAR(255) NOT NULL,
    version VARCHAR(50) NOT NULL,  -- Semver format (e.g., "1.2.3")
    engine VARCHAR(50) NOT NULL DEFAULT 'jslt',
    template_expression TEXT,      -- Nullable for router/pipeline engines
    configuration TEXT,           -- JSON configuration for router/pipeline engines
    description TEXT,
    is_active BOOLEAN NOT NULL DEFAULT FALSE,  -- Only one active per consumer+subject
    input_schema_version VARCHAR(50),          -- Linked to subject schema version
    output_schema_version VARCHAR(50),         -- Consumer output schema version
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,

    FOREIGN KEY (consumer_id) REFERENCES consumers(consumer_id) ON DELETE CASCADE,
    UNIQUE(consumer_id, subject, version),     -- Unique version per consumer+subject
    CHECK (is_active IN (TRUE, FALSE))         -- Boolean constraint
);

-- Partial index to ensure only one active version per consumer+subject
CREATE UNIQUE INDEX idx_transformation_templates_active_only
ON transformation_templates(consumer_id, subject)
WHERE is_active = TRUE;

-- Indexes
CREATE INDEX idx_transformation_templates_consumer_subject ON transformation_templates(consumer_id, subject);
CREATE INDEX idx_transformation_templates_active ON transformation_templates(consumer_id, subject, is_active);
CREATE INDEX idx_transformation_templates_version ON transformation_templates(consumer_id, subject, version);
```

#### Option 2: Separate Template Versions Table

```sql
-- Keep base templates
CREATE TABLE transformation_templates (
    id BIGSERIAL PRIMARY KEY,
    consumer_id VARCHAR(255) NOT NULL,
    subject VARCHAR(255) NOT NULL,
    description TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,

    FOREIGN KEY (consumer_id) REFERENCES consumers(consumer_id) ON DELETE CASCADE,
    UNIQUE(consumer_id, subject)
);

-- Versioned instances
CREATE TABLE transformation_template_versions (
    id BIGSERIAL PRIMARY KEY,
    template_id BIGINT NOT NULL,
    version VARCHAR(50) NOT NULL,  -- Semver format
    engine VARCHAR(50) NOT NULL DEFAULT 'jslt',
    template_expression TEXT,
    configuration TEXT,
    is_active BOOLEAN NOT NULL DEFAULT FALSE,
    input_schema_version VARCHAR(50),
    output_schema_version VARCHAR(50),
    description TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,

    FOREIGN KEY (template_id) REFERENCES transformation_templates(id) ON DELETE CASCADE,
    UNIQUE(template_id, version),              -- Unique version per template
    CHECK (is_active IN (TRUE, FALSE))         -- Boolean constraint
);

-- Partial index to ensure only one active version per template
CREATE UNIQUE INDEX idx_template_versions_active_only
ON transformation_template_versions(template_id)
WHERE is_active = TRUE;
```

## API Changes

### Current API
```
POST /api/consumers/{consumerId}/transform?subject={subject}
```

### Proposed New API

#### Option A: Version Parameter
```
POST /api/consumers/{consumerId}/transform?subject={subject}&version={version}
```
- `version` parameter optional, defaults to active version

#### Option B: Version in Path
```
POST /api/consumers/{consumerId}/subjects/{subject}/transform
POST /api/consumers/{consumerId}/subjects/{subject}/transform/{version}
```

#### Option C: Version in Request Body
```json
{
  "canonicalJson": {...},
  "transformationVersion": "1.2.3"  // Optional
}
```

### Template Management API Changes

#### Current API
```
POST /api/consumers/templates/{consumerId}
```

#### Proposed New API
```
POST /api/consumers/{consumerId}/subjects/{subject}/templates
POST /api/consumers/{consumerId}/subjects/{subject}/templates/{version}
GET  /api/consumers/{consumerId}/subjects/{subject}/templates
PUT  /api/consumers/{consumerId}/subjects/{subject}/templates/{version}/activate
```

## Service Layer Changes

### TransformationService Updates

#### Current Logic
```java
public TransformationResponse transform(String consumerId, TransformationRequest request) {
    String subject = request.getSubject();
    consumerService.validateConsumerSubject(consumerId, subject);

    // Get single template for consumer
    TransformationTemplateEntity template = templateRepository.findByConsumerId(consumerId)
        .orElseThrow(...);

    // Apply transformation
    return applyTransformation(template, request);
}
```

#### Proposed New Logic
```java
public TransformationResponse transform(String consumerId, TransformationRequest request) {
    String subject = request.getSubject();
    String version = request.getTransformationVersion(); // Optional

    consumerService.validateConsumerSubject(consumerId, subject);

    // Get appropriate template version
    TransformationTemplateEntity template;
    if (version != null) {
        template = templateRepository.findByConsumerIdAndSubjectAndVersion(consumerId, subject, version)
            .orElseThrow(...);
    } else {
        template = templateRepository.findActiveByConsumerIdAndSubject(consumerId, subject)
            .orElseThrow(...);
    }

    // Apply transformation
    return applyTransformation(template, request);
}
```

### Version Management Service

New service for handling version lifecycle:

```java
public class TransformationVersionService {

    public void createNewVersion(String consumerId, String subject, String version,
                                TransformationTemplateRequest request) {
        // Validate version format (semver)
        // Check if version already exists
        // Create new version as inactive
    }

    public void activateVersion(String consumerId, String subject, String version) {
        // Deactivate current active version
        // Activate specified version
        // Update schema version links
    }

    public List<String> getAvailableVersions(String consumerId, String subject) {
        // Return all versions for consumer+subject pair
    }
}
```

## Migration Strategy

### Database Migration

#### Phase 1: Schema Changes
1. Create new table structure (Option 1 or 2)
2. **Important**: Ensure partial unique indexes are created to allow only one active version per consumer-subject pair
3. Migrate existing templates to new structure
4. Set all migrated templates as active (version "1.0.0")
5. Update foreign key references

#### Phase 2: Data Migration
1. For each existing consumer:
   - Create template entries for each subject they're registered for
   - Copy existing template logic to version "1.0.0"
   - Mark as active

#### Phase 3: API Migration
1. Update controllers to new API structure
2. Add version parameters
3. Update request/response DTOs

### Backward Compatibility

#### Option A: Gradual Migration
- Keep old API endpoints alongside new ones
- Deprecate old endpoints over time
- Provide migration guide for consumers

#### Option B: Breaking Change
- Implement new API structure immediately
- Update all tests and documentation
- Clear communication about breaking changes

## Implementation Options

### Option 1: Complete Refactor (Recommended)

**Pros:**
- Clean architecture aligned with requirements
- Full versioning support
- Future-proof design
- Clear separation of concerns

**Cons:**
- Major breaking changes
- Complex migration
- Significant development effort

**Timeline:** 4-6 weeks
**Risk Level:** High (major architectural change)

### Option 2: Incremental Enhancement

**Pros:**
- Smaller changes
- Easier to test and deploy
- Maintains some backward compatibility

**Cons:**
- Still has architectural compromises
- May require future refactoring
- Complex version management logic

**Timeline:** 2-3 weeks
**Risk Level:** Medium

### Option 3: Hybrid Approach

**Pros:**
- Balances clean design with practical constraints
- Allows phased rollout
- Reduces risk

**Cons:**
- More complex implementation
- Potential for technical debt

**Timeline:** 3-4 weeks
**Risk Level:** Medium-High

## Testing Strategy

### Unit Tests
- Test version selection logic
- Test activation/deactivation
- Test semver validation
- Test schema version linking

### Integration Tests
- Test full transformation workflows with versioning
- Test version switching scenarios
- Test historical data processing
- Test fallback mechanisms

### Migration Tests
- Test data migration from old to new schema
- Test API compatibility during transition
- Test rollback scenarios

## Rollback Plan

### Database Rollback
- Keep old table structure during migration
- Ability to restore from backup if needed
- Clear migration rollback scripts

### Code Rollback
- Feature flags for new functionality
- Ability to disable versioning features
- Gradual rollout with monitoring

## Success Criteria

1. **Functional Requirements:**
   - Consumers can have multiple subjects with different transformations
   - Transformation versioning works with semver
   - Active version switching works correctly
   - Historical processing with version specification works

2. **Non-Functional Requirements:**
   - API performance maintained (<100ms response times)
   - Backward compatibility for critical paths
   - Clear error messages for version conflicts
   - Comprehensive test coverage (>90%)

3. **Operational Requirements:**
   - Easy version management through API
   - Clear audit trail of version changes
   - Monitoring and alerting for version issues
   - Documentation for version management processes

## Next Steps

1. **Review and Approval:** Review this plan with stakeholders
2. **Option Selection:** Choose implementation approach (1, 2, or 3)
3. **Detailed Design:** Create detailed design documents for selected approach
4. **Implementation Planning:** Break down into specific tasks and timeline
5. **Risk Assessment:** Identify and mitigate high-risk areas
6. **Go/No-Go Decision:** Final approval before implementation begins