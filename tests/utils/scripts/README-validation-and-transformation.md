# JSON Validation and Transformation Scripts

This directory contains specialized scripts for validating and transforming large JSON files with the Schema Registry API. These scripts are designed to handle large files efficiently without dumping massive content to the terminal.

## Scripts Overview

### 1. `register-consumer.sh`

**Purpose**: Registers new consumers with the Schema Registry API.

**Key Features**:
- Simple consumer registration with ID, name, and description
- Validates consumer ID format (letters, numbers, hyphens, underscores)
- Provides clear success/error feedback
- Integrates with existing test infrastructure

**Usage**:
```bash
# Register a consumer with description
./register-consumer.sh mobile-app "Mobile Application" "Consumer for mobile app data"

# Register with minimal data
./register-consumer.sh web-client "Web Client"
```

**Output Example**:
```
[INFO] Registering consumer: mobile-app
[INFO] Name: Mobile Application
[INFO] Description: Consumer for mobile app data
[INFO] Sending registration request...
[PASS] Consumer registered successfully
Response:
{
  "id": 1,
  "consumerId": "mobile-app",
  "name": "Mobile Application",
  "description": "Consumer for mobile app data",
  "createdAt": "2025-10-16T14:47:05.914446156",
  "updatedAt": "2025-10-16T14:47:05.914446156"
}
```

### 2. `validate-json-against-schema.sh`

**Purpose**: Validates JSON data files against canonical schemas using the Schema Registry validation endpoint.

**Key Features**:
- Handles large JSON files efficiently
- Validates against latest or specific schema versions
- Shows validation results without dumping file content
- Progress indicators and file size reporting
- Comprehensive error reporting

**Usage**:
```bash
# Validate against latest canonical schema
./validate-json-against-schema.sh data.json user-profile

# Validate against specific schema version
./validate-json-against-schema.sh data.json user-profile 1.0.0

# Validate large test files
./validate-json-against-schema.sh ../examples/investment-research/publications/large-publication-1mb.json investment-publications
```

**Output Example**:
```
[INFO] Validating JSON file: data.json
[INFO] Subject: user-profile
[INFO] File size: 2MB
[INFO] Sending validation request to: http://localhost:8080/api/schemas/user-profile/validate
[PASS] ✅ JSON validation PASSED
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
File: data.json
Subject: user-profile
Size: 2MB
Status: Valid
Schema Version: Latest
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
```

### 3. `transform-json-to-file.sh`

**Purpose**: Transforms JSON data using consumer transformation templates and saves results to files instead of printing to terminal.

**Key Features**:
- Reads input JSON from files
- Saves transformed output directly to files
- Memory-efficient processing of large files
- Supports template versioning
- Progress indicators and size reporting
- Comprehensive error handling

**Usage**:
```bash
# Transform using active template
./transform-json-to-file.sh input.json consumer-123 user-profile output.json

# Transform using specific template version
./transform-json-to-file.sh input.json consumer-123 user-profile output.json 1.0.0

# Transform large test files
./transform-json-to-file.sh ../examples/investment-research/publications/large-publication-5mb.json consumer-123 investment-publications transformed-output.json
```

**Output Example**:
```
[INFO] Transforming JSON data
[INFO] Input file: input.json
[INFO] Consumer: consumer-123
[INFO] Subject: user-profile
[INFO] Output file: output.json
[INFO] Input size: 3MB
[INFO] Sending transformation request to: http://localhost:8080/api/consumers/consumer-123/subjects/user-profile/transform
[PASS] ✅ Transformation completed successfully
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Input file: input.json (3MB)
Output file: output.json (2MB)
Consumer: consumer-123
Subject: user-profile
Version: Active
Status: Transformed successfully
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
```

## Prerequisites

### Dependencies
- `jq` (JSON processor) - `apt install jq` or `brew install jq`
- `curl` (HTTP client) - usually pre-installed
- Bash shell

### Service Requirements
- Schema Registry service must be running
- For consumer registration: Service must be accessible
- For validation: Canonical schemas must be registered
- For transformation: Consumer and transformation templates must be set up

## Configuration

### Environment Variables
- `BASE_URL`: API base URL (default: `http://localhost:8080`)

### Examples
```bash
# Use custom base URL
BASE_URL=http://production-server:8080 ./validate-json-against-schema.sh data.json subject

# Transform with custom server
BASE_URL=http://staging-server:8080 ./transform-json-to-file.sh input.json consumer subject output.json
```

## Use Cases

### Large File Stress Testing
```bash
# Validate all generated large test files
cd tests/examples/investment-research/publications
for file in large-publication-*.json; do
    echo "Validating $file..."
    ../../../utils/scripts/validate-json-against-schema.sh "$file" investment-publications
done

# Transform large files with different consumers
for file in large-publication-*.json; do
    output="transformed-$(basename "$file")"
    echo "Transforming $file -> $output..."
    ../../../utils/scripts/transform-json-to-file.sh "$file" consumer-123 investment-publications "$output"
done
```

### Integration Testing
```bash
# Setup test environment
./setup.sh

# Register a consumer for testing
./register-consumer.sh test-consumer "Test Consumer" "Consumer for integration testing"

  # Register schema for the consumer
  ./register-schema-from-file.sh consumer-schema.json test-subject test-consumer BACKWARD "Test consumer schema"

  # Register schema with specific version
  ./register-schema-from-file.sh schema.json test-subject BACKWARD "Schema with version" "2.0.0"

# Validate test data
./validate-json-against-schema.sh test-data.json test-subject

# Transform and verify
./transform-json-to-file.sh test-data.json test-consumer test-subject transformed.json

# Verify output is valid JSON
jq empty transformed.json && echo "Output is valid JSON"
```

### Performance Benchmarking
```bash
# Time validation of different file sizes
time ./validate-json-against-schema.sh small-file.json subject
time ./validate-json-against-schema.sh large-file.json subject

# Time transformation operations
time ./transform-json-to-file.sh input.json consumer subject output.json
```

## Error Handling

### Validation Errors
- **Invalid JSON**: Script exits with clear error message
- **Schema not found**: HTTP 404 with subject details
- **Validation failure**: Shows specific validation errors from API
- **Network issues**: Connection errors with retry suggestions

### Transformation Errors
- **Consumer not found**: HTTP 404 with consumer details
- **Template not found**: HTTP 404 with template details
- **Invalid input**: HTTP 400 with validation details
- **Transformation failure**: Shows API error response

## File Size Handling

### Memory Efficiency
- Files are read and processed without loading entire content into memory for display
- JSON parsing uses `jq` for efficient streaming
- Output is streamed directly to files
- No terminal output of large JSON content

### Large File Support
- Tested with files up to 23MB (15MB input generating 23MB output)
- Progress indicators for files >1MB
- Size reporting in MB for easy monitoring
- Efficient HTTP transfer using curl

## Integration with Test Suite

### Existing Scripts
- Works alongside `transform-from-file.sh` (which prints to terminal)
- Uses same `common.sh` utilities as other test scripts
- Compatible with existing test setup and teardown

### Test Workflow
```bash
# Full test workflow example
./setup.sh
./register-consumer.sh test-consumer "Test Consumer" "Consumer for testing"
./register-schema-from-file.sh schema.json subject
./validate-json-against-schema.sh test-data.json subject
create_test_template "test-consumer" "subject" "template-expression"
./transform-json-to-file.sh test-data.json test-consumer subject output.json
./cleanup.sh
```

## Troubleshooting

### Common Issues

**"jq: command not found"**
```bash
# Install jq
sudo apt install jq  # Ubuntu/Debian
brew install jq      # macOS
```

**"Service not running"**
```bash
# Start the service
cd /path/to/project
mvn spring-boot:run
# or
docker-compose up -d
```

**"Schema/Consumer not found"**
```bash
# Check registered resources
curl http://localhost:8080/api/schemas
curl http://localhost:8080/api/consumers

# Register missing consumer
./register-consumer.sh missing-consumer "Missing Consumer" "Consumer that was not found"
```

**"File too large"**
- Scripts handle files up to tens of MB
- For extremely large files, consider splitting or using streaming approaches
- Monitor system memory usage

### Debug Mode
```bash
# Enable verbose output
set -x
./validate-json-against-schema.sh data.json subject

# Check API responses directly
curl -X POST http://localhost:8080/api/schemas/subject/validate \
  -H "Content-Type: application/json" \
  -d '{"subject":"subject","jsonData":{}}'
```

## Performance Characteristics

### Validation Performance
- Small files (<1MB): <1 second
- Large files (10MB): 2-5 seconds
- Very large files (23MB): 5-10 seconds

### Transformation Performance
- Depends on transformation complexity
- Simple JSLT transforms: 1-3 seconds for large files
- Complex transforms: 3-8 seconds for large files

### Memory Usage
- Minimal memory footprint
- Files processed in streaming fashion
- No large in-memory data structures

## Security Considerations

- Scripts validate JSON syntax before sending to API
- No sensitive data logging
- Safe file operations with proper error handling
- No arbitrary code execution

## Future Enhancements

Potential improvements:
- Parallel processing for multiple files
- Compression support for very large files
- Progress bars for long-running operations
- Batch processing capabilities
- Integration with CI/CD pipelines