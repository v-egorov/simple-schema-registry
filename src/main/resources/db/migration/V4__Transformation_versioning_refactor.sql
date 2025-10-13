-- Transformation Versioning Refactor
-- Complete rewrite of transformation system to support versioning and proper relationships

-- Step 1: Enhance schemas table to support multiple schema types
ALTER TABLE schemas
ADD COLUMN schema_type VARCHAR(50) NOT NULL DEFAULT 'canonical' CHECK (schema_type IN ('canonical', 'consumer_output')),
ADD COLUMN consumer_id VARCHAR(255);

-- Add constraint ensuring consumer_id is set for consumer_output schemas
ALTER TABLE schemas
ADD CONSTRAINT check_consumer_output_has_consumer_id
CHECK (schema_type = 'canonical' OR consumer_id IS NOT NULL);

-- Add foreign key for consumer_id
ALTER TABLE schemas
ADD CONSTRAINT fk_schemas_consumer_id
FOREIGN KEY (consumer_id) REFERENCES consumers(consumer_id) ON DELETE CASCADE;

-- Update unique constraint to include new columns
-- Drop existing constraint first
ALTER TABLE schemas DROP CONSTRAINT IF EXISTS schemas_subject_version_key;
-- Add new composite unique constraint
ALTER TABLE schemas
ADD CONSTRAINT unique_subject_schema_type_consumer_version
UNIQUE (subject, schema_type, consumer_id, version);

-- Add indexes for new query patterns
CREATE INDEX idx_schemas_subject_type ON schemas(subject, schema_type);
CREATE INDEX idx_schemas_consumer ON schemas(consumer_id, subject, schema_type);

-- Step 2: Drop and recreate transformation_templates table
-- Drop existing table (no data migration needed)
DROP TABLE IF EXISTS transformation_templates CASCADE;

-- Create new transformation_templates table with proper relationships
CREATE TABLE transformation_templates (
    id BIGSERIAL PRIMARY KEY,
    consumer_id VARCHAR(255) NOT NULL,
    subject VARCHAR(255) NOT NULL,
    version VARCHAR(50) NOT NULL,
    engine VARCHAR(50) NOT NULL DEFAULT 'jslt',
    template_expression TEXT,  -- Nullable for router/pipeline engines
    configuration TEXT,       -- JSON configuration for router/pipeline engines
    input_schema_id BIGINT NOT NULL,
    output_schema_id BIGINT NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT FALSE,
    description TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Foreign key constraints
    FOREIGN KEY (consumer_id) REFERENCES consumers(consumer_id) ON DELETE CASCADE,
    FOREIGN KEY (input_schema_id) REFERENCES schemas(id) ON DELETE RESTRICT,
    FOREIGN KEY (output_schema_id) REFERENCES schemas(id) ON DELETE RESTRICT,

    -- Business logic constraints
    UNIQUE(consumer_id, subject, version),
    CHECK (is_active IN (TRUE, FALSE))
);

-- Create partial unique index to ensure only one active version per consumer-subject pair
CREATE UNIQUE INDEX idx_transformation_templates_active_only
ON transformation_templates(consumer_id, subject)
WHERE is_active = TRUE;

-- Performance indexes
CREATE INDEX idx_transformation_templates_consumer_subject
ON transformation_templates(consumer_id, subject);

CREATE INDEX idx_transformation_templates_active
ON transformation_templates(consumer_id, subject, is_active);

CREATE INDEX idx_transformation_templates_version
ON transformation_templates(consumer_id, subject, version);

-- Add trigger for updated_at timestamp
CREATE OR REPLACE FUNCTION update_transformation_templates_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER update_transformation_templates_updated_at
    BEFORE UPDATE ON transformation_templates
    FOR EACH ROW EXECUTE FUNCTION update_transformation_templates_updated_at();

-- Step 3: Update existing schemas to have proper schema_type
-- All existing schemas are canonical schemas
UPDATE schemas SET schema_type = 'canonical' WHERE schema_type = 'canonical';