-- Add subjects support for consumers and change schema version to semver

-- Change schema version from INTEGER to VARCHAR for semver support
ALTER TABLE schemas ALTER COLUMN version TYPE VARCHAR(50);

-- Create consumer_subjects table for multi-subject support
CREATE TABLE consumer_subjects (
    consumer_id BIGINT NOT NULL,
    subject VARCHAR(255) NOT NULL,
    PRIMARY KEY (consumer_id, subject),
    FOREIGN KEY (consumer_id) REFERENCES consumers(id) ON DELETE CASCADE
);

-- Add index for subject queries
CREATE INDEX idx_consumer_subjects_subject ON consumer_subjects(subject);