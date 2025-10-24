-- Squashed migration script combining V1-V5 into final schema
-- Optimized column order for schemas table, omitting unnecessary tables like consumer_subjects

-- Create function to update updated_at timestamp
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Create consumers table
CREATE TABLE consumers (
    id BIGSERIAL PRIMARY KEY,
    consumer_id VARCHAR(255) UNIQUE NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create schemas table with reordered columns (logical grouping)
CREATE TABLE schemas (
    id BIGSERIAL PRIMARY KEY,
    subject VARCHAR(255) NOT NULL,
    schema_type VARCHAR(50) NOT NULL DEFAULT 'canonical' CHECK (schema_type IN ('canonical', 'consumer_output')),
    consumer_id VARCHAR(255),
    version VARCHAR(50) NOT NULL,
    schema_json JSONB NOT NULL,
    compatibility VARCHAR(50) NOT NULL DEFAULT 'BACKWARD',
    description TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    UNIQUE(subject, schema_type, consumer_id, version),
    FOREIGN KEY (consumer_id) REFERENCES consumers(consumer_id) ON DELETE CASCADE,
    CHECK (schema_type = 'canonical' OR consumer_id IS NOT NULL)
);

-- Create indexes for schemas table
CREATE INDEX idx_schemas_subject ON schemas(subject);
CREATE INDEX idx_schemas_subject_version ON schemas(subject, version DESC);
CREATE INDEX idx_schemas_created_at ON schemas(created_at);
CREATE INDEX idx_schemas_subject_type ON schemas(subject, schema_type);
CREATE INDEX idx_schemas_consumer ON schemas(consumer_id, subject, schema_type);

-- Create transformation_templates table
CREATE TABLE transformation_templates (
    id BIGSERIAL PRIMARY KEY,
    consumer_id VARCHAR(255) NOT NULL,
    subject VARCHAR(255) NOT NULL,
    version VARCHAR(50) NOT NULL,
    engine VARCHAR(50) NOT NULL DEFAULT 'jslt',
    template_expression TEXT,
    configuration TEXT,
    input_schema_id BIGINT NOT NULL,
    output_schema_id BIGINT NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT FALSE,
    description TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    FOREIGN KEY (consumer_id) REFERENCES consumers(consumer_id) ON DELETE CASCADE,
    FOREIGN KEY (input_schema_id) REFERENCES schemas(id) ON DELETE RESTRICT,
    FOREIGN KEY (output_schema_id) REFERENCES schemas(id) ON DELETE RESTRICT,
    UNIQUE(consumer_id, subject, version),
    CHECK (is_active IN (TRUE, FALSE))
);

-- Create indexes for transformation_templates table
CREATE UNIQUE INDEX idx_transformation_templates_active_only
ON transformation_templates(consumer_id, subject)
WHERE is_active = TRUE;

CREATE INDEX idx_transformation_templates_consumer_subject
ON transformation_templates(consumer_id, subject);

CREATE INDEX idx_transformation_templates_active
ON transformation_templates(consumer_id, subject, is_active);

CREATE INDEX idx_transformation_templates_version
ON transformation_templates(consumer_id, subject, version);

-- Create triggers to automatically update updated_at columns
CREATE TRIGGER update_schemas_updated_at BEFORE UPDATE ON schemas
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_consumers_updated_at BEFORE UPDATE ON consumers
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_transformation_templates_updated_at BEFORE UPDATE ON transformation_templates
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();