# Transformation Versioning Refactor Plan

## Current Architecture Issues

### Problems Identified

1. **Incorrect Consumer-Template Relationship**: Current design enforces 1:1 relationship between consumers and transformation templates, but consumers can register for multiple subjects requiring different transformations

2. **Lack of Transformation Versioning**: No support for versioning transformations as schemas evolve, preventing safe schema evolution and rollback capabilities

3. **No Fallback Mechanism**: Cannot rollback to previous transformation versions during production issues or gradual rollout scenarios

4. **Limited Historical Processing**: Cannot reprocess historical data with different schema versions using appropriate transformation logic

5. **Schema Table Design Issues**: Current `schemas` table only supports canonical schemas, but the system needs to track consumer-specific output schemas as well

6. **Database Normalization Problems**: Transformation templates store redundant schema version information instead of proper foreign key relationships

7. **Lack of Referential Integrity**: No foreign key constraints between transformation templates and schemas, allowing orphaned or invalid references

8. **Missing Schema Type Support**: No way to distinguish between canonical schemas (authoritative) and consumer output schemas (contracts)

9. **Inflexible Subject-Consumer Binding**: Current design doesn't properly support consumers having different transformation logic for different subjects

### Business Requirements

- **Multi-Subject Consumer Support**: Each consumer can transform multiple subjects with subject-specific transformation logic
- **Schema Evolution Handling**: Support for evolving canonical schemas while maintaining backward compatibility for consumers
- **Independent Consumer Evolution**: Consumers can evolve their output requirements independently of canonical schema changes
- **Transformation Versioning**: Semver-based versioning for transformations linked to subject:consumer pairs
- **Active Version Management**: Only one transformation version active per subject:consumer pair at any time
- **Fallback and Rollback**: Ability to switch back to previous transformation versions for operational safety
- **Historical Data Processing**: Support for reprocessing historical data using specific transformation versions
- **Schema Contract Management**: Track both input (canonical) and output (consumer) schema contracts
- **Referential Integrity**: Proper database relationships ensuring data consistency
- **Breaking Changes Allowed**: Development stage allows major architectural changes without backward compatibility concerns

## Proposed New Architecture

### Core Concepts

1. **Multi-Subject Consumer Support**: Consumers can register for multiple subjects and have subject-specific transformation logic

2. **Unified Schema Management**: Single `schemas` table supporting both canonical schemas (authoritative) and consumer output schemas (contracts)

3. **Versioned Transformations**: Semver-based versioning for transformation templates linked to subject:consumer pairs

4. **Active Version Management**: Each subject:consumer pair has exactly one active transformation version at any time

5. **Referential Integrity**: Foreign key relationships ensure transformations reference valid schemas and prevent orphaned data

6. **Schema Evolution Support**: Independent evolution of canonical schemas and consumer output contracts

7. **Fallback Capabilities**: Ability to rollback to previous transformation versions for operational safety

8. **Historical Processing**: Support for reprocessing data using specific transformation versions via API parameters



### Practical Usage Examples

#### Scenario 1: Subject Schema Evolution
```
Subject: "user-profile"
Canonical Schemas:
- ID 1: subject="user-profile", type="canonical", version="1.0.0" (basic fields)
- ID 2: subject="user-profile", type="canonical", version="1.1.0" (added phone field)

Consumer Output Schemas:
- ID 3: subject="user-profile", type="consumer_output", consumer="mobile-app", version="1.0" (simple view)
- ID 4: subject="user-profile", type="consumer_output", consumer="mobile-app", version="2.0" (with phone)

Transformation Versions:
- v1.0.0: input_schema_id=1, output_schema_id=3 (handles basic input, produces simple output)
- v1.1.0: input_schema_id=2, output_schema_id=3 (handles enhanced input, maintains simple output)
- v2.0.0: input_schema_id=2, output_schema_id=4 (handles enhanced input, produces enhanced output)
```

#### Scenario 2: Consumer Evolution
```
Subject: "product-catalog"
Canonical Schema:
- ID 5: subject="product-catalog", type="canonical", version="1.0.0" (stable)

Consumer Output Schemas:
- ID 6: subject="product-catalog", type="consumer_output", consumer="web-dashboard", version="1.0" (basic view)
- ID 7: subject="product-catalog", type="consumer_output", consumer="web-dashboard", version="2.0" (with analytics)

Transformation Versions:
- v1.0.0: input_schema_id=5, output_schema_id=6 (stable input, basic output)
- v2.0.0: input_schema_id=5, output_schema_id=7 (same input, enhanced output with analytics)
```

#### Version Selection Logic
When processing a transformation request:
1. **Subject** determines which transformation templates are eligible
2. **Active Flag** determines which version to use by default (or explicit version if specified)
3. **Foreign Keys** ensure input/output schemas are properly linked and validated
4. **Referential Integrity** guarantees that referenced schemas exist and are appropriate types

### Benefits of Normalized Design

**Data Integrity**:
- Foreign key constraints prevent orphaned references
- Schema type validation ensures proper usage
- Referential integrity guarantees transformation validity

**Maintainability**:
- Single source of truth for all schemas
- Clear relationships between transformations and contracts
- Easier debugging with explicit schema links

**Extensibility**:
- Easy addition of new schema types
- Support for consumer-specific schema evolution
- Flexible schema relationship modeling

This design enables:
- **Historical Processing**: Specify exact transformation version for reprocessing old data
- **Gradual Rollout**: Test new transformations while maintaining referential integrity
- **Schema Evolution**: Handle both input and output schema changes with proper relationships
- **Data Consistency**: Foreign key constraints prevent orphaned or invalid references

### Database Schema Changes

#### Enhanced Schemas Table (Unified Design)

```sql
-- Enhanced schemas table supporting multiple schema types
CREATE TABLE schemas (
    id BIGSERIAL PRIMARY KEY,
    subject VARCHAR(255) NOT NULL,
    schema_type VARCHAR(50) NOT NULL,  -- 'canonical' or 'consumer_output'
    consumer_id VARCHAR(255),          -- NULL for canonical, populated for consumer_output
    version VARCHAR(50) NOT NULL,      -- Semver format
    schema_json JSONB NOT NULL,
    compatibility VARCHAR(50) NOT NULL DEFAULT 'BACKWARD',
    description TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,

    -- Constraints
    UNIQUE(subject, schema_type, consumer_id, version),
    CHECK (schema_type IN ('canonical', 'consumer_output')),
    CHECK (schema_type = 'canonical' OR consumer_id IS NOT NULL),
    FOREIGN KEY (consumer_id) REFERENCES consumers(consumer_id) ON DELETE CASCADE
);

-- Indexes
CREATE INDEX idx_schemas_subject_type ON schemas(subject, schema_type);
CREATE INDEX idx_schemas_consumer ON schemas(consumer_id, subject, schema_type);
```

#### Option 1: Subject-Consumer-Versioned Templates

```sql
-- Normalized transformation_templates with proper referential integrity
CREATE TABLE transformation_templates (
    id BIGSERIAL PRIMARY KEY,
    consumer_id VARCHAR(255) NOT NULL,
    subject VARCHAR(255) NOT NULL,
    version VARCHAR(50) NOT NULL,      -- Transformation version (semver)
    engine VARCHAR(50) NOT NULL DEFAULT 'jslt',
    template_expression TEXT,          -- Nullable for router/pipeline engines
    configuration TEXT,                -- JSON configuration for router/pipeline engines
    input_schema_id BIGINT NOT NULL,   -- References canonical schema
    output_schema_id BIGINT NOT NULL,  -- References consumer output schema
    is_active BOOLEAN NOT NULL DEFAULT FALSE,
    description TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,

    FOREIGN KEY (consumer_id) REFERENCES consumers(consumer_id) ON DELETE CASCADE,
    FOREIGN KEY (input_schema_id) REFERENCES schemas(id) ON DELETE RESTRICT,
    FOREIGN KEY (output_schema_id) REFERENCES schemas(id) ON DELETE RESTRICT,
    UNIQUE(consumer_id, subject, version),
    CHECK (is_active IN (TRUE, FALSE))
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

-- Versioned instances with proper referential integrity
CREATE TABLE transformation_template_versions (
    id BIGSERIAL PRIMARY KEY,
    template_id BIGINT NOT NULL,
    version VARCHAR(50) NOT NULL,      -- Transformation version (semver)
    engine VARCHAR(50) NOT NULL DEFAULT 'jslt',
    template_expression TEXT,
    configuration TEXT,
    input_schema_id BIGINT NOT NULL,   -- References canonical schema
    output_schema_id BIGINT NOT NULL,  -- References consumer output schema
    is_active BOOLEAN NOT NULL DEFAULT FALSE,
    description TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,

    FOREIGN KEY (template_id) REFERENCES transformation_templates(id) ON DELETE CASCADE,
    FOREIGN KEY (input_schema_id) REFERENCES schemas(id) ON DELETE RESTRICT,
    FOREIGN KEY (output_schema_id) REFERENCES schemas(id) ON DELETE RESTRICT,
    UNIQUE(template_id, version),
    CHECK (is_active IN (TRUE, FALSE))
);

-- Partial index to ensure only one active version per template
CREATE UNIQUE INDEX idx_template_versions_active_only
ON transformation_template_versions(template_id)
WHERE is_active = TRUE;
```

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

### Schema Relationships and Referential Integrity

#### Unified Schema Management
The `schemas` table now stores all schema types with proper relationships:

- **Canonical Schemas**: `schema_type = 'canonical'`, `consumer_id = NULL`
  - Authoritative schemas defining subject structure
  - Used for input validation and documentation

- **Consumer Output Schemas**: `schema_type = 'consumer_output'`, `consumer_id ≠ NULL`
  - Consumer-specific output contracts
  - Define what each consumer expects as output
  - Enable consumer evolution independent of canonical schemas

#### Transformation Template Relationships

**Foreign Key References**:
- `input_schema_id` → `schemas.id` (canonical schema for the subject)
- `output_schema_id` → `schemas.id` (consumer output schema)
- `consumer_id` → `consumers.consumer_id`
- `subject` (denormalized for query performance and API clarity)

**Referential Integrity Benefits**:
- **Data Consistency**: Foreign keys prevent orphaned references
- **Cascade Protection**: `RESTRICT` on schema deletion prevents accidental data loss
- **Audit Trail**: Clear links between transformations and their input/output contracts
- **Query Performance**: Indexed foreign key relationships enable efficient joins

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

New service for handling version lifecycle with schema validation:

```java
public class TransformationVersionService {

    public void createNewVersion(String consumerId, String subject, String version,
                                Long inputSchemaId, Long outputSchemaId,
                                TransformationTemplateRequest request) {
        // Validate version format (semver)
        // Verify input schema exists and is canonical type for subject
        // Verify output schema exists and is consumer_output type for consumer+subject
        // Check if transformation version already exists
        // Create new version as inactive
    }

    public void activateVersion(String consumerId, String subject, String version) {
        // Validate transformation version exists
        // Deactivate current active version for consumer+subject
        // Activate specified version
        // Log version change for audit trail
    }

    public List<TransformationTemplateResponse> getAvailableVersions(String consumerId, String subject) {
        // Return all versions for consumer+subject pair with schema details
    }

    public void validateSchemaCompatibility(Long inputSchemaId, Long outputSchemaId) {
        // Ensure input schema is canonical and matches subject
        // Ensure output schema is consumer_output and matches consumer+subject
        // Validate schema compatibility rules
    }
}
```

## Migration Strategy

### Database Migration

#### Phase 1: Schema Changes
1. **Enhance schemas table**: Add `schema_type`, `consumer_id` columns with proper constraints
2. **Create transformation_templates**: Implement chosen option (1 or 2) with foreign key relationships
3. **Migrate existing data**:
   - Convert existing schemas to canonical type with `consumer_id = NULL`
   - Create consumer output schemas based on existing consumer expectations
   - Link transformation templates to appropriate schema IDs
4. **Set initial active versions**: Mark migrated transformations as active (version "1.0.0")
5. **Validate referential integrity**: Ensure all foreign key relationships are valid

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
- Test version selection logic with foreign key relationships
- Test activation/deactivation with referential integrity
- Test semver validation
- Test schema type validation (canonical vs consumer_output)
- Test foreign key constraint enforcement
- Test schema compatibility validation

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
   - Data integrity with referential constraints enforced
   - Schema consistency validation (100% valid references)
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