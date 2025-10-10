-- Add configuration column to transformation_templates table
-- This supports router and pipeline transformation engines

ALTER TABLE transformation_templates
ADD COLUMN configuration TEXT;

-- Update template_expression to be nullable for router/pipeline engines
ALTER TABLE transformation_templates
ALTER COLUMN template_expression DROP NOT NULL;

-- Add comment to explain the new column
COMMENT ON COLUMN transformation_templates.configuration IS 'JSON configuration for router and pipeline transformation engines';
COMMENT ON COLUMN transformation_templates.template_expression IS 'JSLT expression for jslt engine (nullable for router/pipeline engines)';