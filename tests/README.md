# API Test Suite

This directory contains a comprehensive suite of curl-based tests for the JSON Schema Registry and Transformation Service API.

## Overview

The test suite covers all API endpoints and includes:
- **Health checks** - Service availability and monitoring endpoints
- **Consumer management** - Registration and retrieval of consumer applications
- **Schema registry** - Schema registration, versioning, and compatibility checking
- **Data transformation** - JSON transformation using JSLT templates
- **Integration workflows** - End-to-end scenarios combining multiple APIs
- **Error handling** - Validation of error responses and edge cases

## Directory Structure

```
tests/
├── README.md                    # This file
├── run-all.sh                   # Master test runner
├── utils/
│   ├── common.sh               # Shared utilities and functions
│   ├── setup.sh                # Test data setup helpers
│   ├── cleanup.sh              # Test cleanup utilities
│   └── scripts/
│       ├── register-consumer.sh         # Consumer registration utility
│       ├── register-schema-from-file.sh # Schema registration from file
│       ├── register-jslt-template-from-file.sh # Template registration
│       ├── validate-json-against-schema.sh # JSON validation utility
│       ├── transform-json-to-file.sh    # JSON transformation utility
│       ├── transform-from-file.sh       # Terminal transformation
│       └── generate-large-investment-publications.py # Test data generator
├── health/
│   ├── test-health.sh          # Basic health checks
│   └── test-actuator.sh        # Spring Boot actuator tests
├── consumers/
│   ├── test-consumer-register.sh    # Consumer registration
│   ├── test-consumer-list.sh        # Consumer listing
│   └── test-consumer-get.sh         # Consumer retrieval
├── schemas/
│   ├── test-schema-register.sh      # Schema registration
│   ├── test-schema-get-all.sh       # Get all schema versions
│   ├── test-schema-get-specific.sh  # Get specific version
│   ├── test-schema-get-latest.sh    # Get latest version
│   ├── test-schema-compatibility.sh # Compatibility checking
│   └── test-schema-subjects.sh      # List all subjects
├── transform/
│   ├── test-transform-data.sh       # Data transformation
│   ├── test-transform-template-get.sh    # Get templates
│   ├── test-transform-template-create.sh # Create/update templates
│   └── test-transform-engines.sh    # List engines
├── workflows/
│   ├── test-full-workflow.sh        # Complete integration test
│   └── test-schema-evolution.sh     # Schema versioning workflow
└── error-handling/
    ├── test-errors-400.sh          # Bad request tests
    ├── test-errors-404.sh          # Not found tests
    └── test-errors-409.sh          # Conflict tests
```

## Quick Start

### Prerequisites

- **Service running**: The API service must be running (default: http://localhost:8080)
- **curl**: Command-line tool for making HTTP requests
- **bash**: Unix shell for running test scripts

### Run All Tests

```bash
# Make scripts executable (first time only)
chmod +x tests/run-all.sh tests/**/*.sh tests/utils/*.sh

# Run all tests
./tests/run-all.sh
```

### Run Specific Test Suite

```bash
# Run only health checks
./tests/run-all.sh --health-only

# Run only critical tests
./tests/run-all.sh --quick

# Run individual test script
./tests/health/test-health.sh
```

## Configuration

### Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `BASE_URL` | `http://localhost:8080` | API service base URL |
| `VERBOSE` | `false` | Enable verbose test output |
| `PARALLEL` | `false` | Run tests in parallel (experimental) |

### Command Line Options

```bash
./tests/run-all.sh [OPTIONS]

Options:
  -h, --help          Show help message
  -u, --url URL       API base URL
  -p, --parallel      Run tests in parallel
  -j, --junit FILE    Generate JUnit XML report
  -v, --verbose       Verbose output
  --health-only       Run only health checks
  --quick             Run only critical tests
```

### Examples

```bash
# Test staging environment
./tests/run-all.sh --url http://staging.example.com:8080

# Generate JUnit report for CI/CD
./tests/run-all.sh --junit test-results.xml

# Verbose output for debugging
./tests/run-all.sh --verbose

# Quick health check
./tests/run-all.sh --health-only
```

## Test Results

### Output Format

```
JSON Schema Registry Test Suite
================================
Base URL: http://localhost:8080
Parallel: false
Verbose:  false

[INFO] Service is healthy and ready for testing
[PASS] Service is healthy

>>> Running test-health
[PASS] Health endpoint should be accessible
[PASS] Service root endpoint is responsive (HTTP 404)
[PASS] Swagger UI should be accessible
[PASS] OpenAPI spec should be accessible
[PASS] test-health completed

=== Health Tests ===
Health: 1 tests, 1 passed, 0 failed

=== FINAL TEST SUMMARY ===
========================================
Total test suites: 6
Total tests run:   42
Tests passed:      40
Tests failed:      2

Suite Results:
  Health:               5 tests ( 5 passed,  0 failed)
  Consumers:            8 tests ( 8 passed,  0 failed)
  Schemas:             12 tests (10 passed,  2 failed)
  Transform:           10 tests (10 passed,  0 failed)
  Workflows:            5 tests ( 5 passed,  0 failed)
  Error Handling:       2 tests ( 2 passed,  0 failed)

❌ 2 TESTS FAILED (95% pass rate)
```

### Exit Codes

- **0**: All tests passed
- **1**: One or more tests failed
- **2**: Test execution error (service not available, etc.)

## Writing New Tests

### Test Script Template

```bash
#!/bin/bash

# Test description
# Tests the [endpoint] endpoint

source "$(dirname "$0")/../utils/common.sh"

echo "Running [Test Name]"
echo "==================="

# Test implementation
response=$(get_request "/api/endpoint")
http_code=$(echo "$response" | tail -n1)
response_body=$(echo "$response" | head -n -1)

assert_response "$http_code" 200 "Endpoint should be accessible"
assert_json_field "$response_body" "field" "expected_value"

echo
print_test_summary
```

### Available Utility Functions

#### Assertions
- `assert_response actual expected message` - Check HTTP status code
- `assert_json_field json field expected message` - Check JSON field value
- `assert_contains haystack needle message` - Check string contains substring
- `assert_not_empty value message` - Check value is not empty

#### HTTP Requests
- `get_request path` - GET request
- `post_request path data` - POST request with JSON data

#### Test Data Management
- `create_test_consumer id name [description]` - Create test consumer
- `create_test_schema subject schema` - Create test schema
- `create_test_template consumer template [engine]` - Create test template

#### Logging
- `log_info message` - Info message
- `log_success message` - Success message
- `log_error message` - Error message
- `log_warning message` - Warning message

### Test Organization

1. **Setup**: Create any required test data
2. **Test**: Execute API calls and assertions
3. **Cleanup**: Remove test data (if needed)
4. **Summary**: Print test results

## CI/CD Integration

### GitHub Actions Example

```yaml
name: API Tests
on: [push, pull_request]

jobs:
  test:
    runs-on: ubuntu-latest
    steps:
    - uses: actions/checkout@v2
    - name: Start services
      run: docker-compose up -d
    - name: Wait for services
      run: sleep 30
    - name: Run API tests
      run: ./tests/run-all.sh --junit test-results.xml
    - name: Upload test results
      uses: actions/upload-artifact@v2
      with:
        name: test-results
        path: test-results.xml
```

### Jenkins Pipeline Example

```groovy
pipeline {
    agent any
    stages {
        stage('Test') {
            steps {
                sh 'docker-compose up -d'
                sh 'sleep 30'
                sh './tests/run-all.sh --junit test-results.xml'
                junit 'test-results.xml'
            }
        }
    }
}
```

## Troubleshooting

### Service Not Available

**Error**: `Service is not healthy`

**Solution**:
```bash
# Start the service
docker-compose up -d

# Or run locally
mvn spring-boot:run

# Wait for startup
sleep 30

# Check health
curl http://localhost:8080/actuator/health
```

### Tests Failing

**Common issues**:

1. **Wrong base URL**:
   ```bash
   ./tests/run-all.sh --url http://localhost:8080
   ```

2. **Service not ready**:
   ```bash
   # Check service status
   docker-compose ps
   # Check logs
   docker-compose logs app
   ```

3. **Database issues**:
   ```bash
   # Check database
   docker-compose exec db pg_isready -U schema_user -d schema_registry
   ```

4. **Port conflicts**:
   ```bash
   # Find process using port 8080
   lsof -i :8080
   ```

### Test Script Issues

**Debug individual tests**:
```bash
# Run with verbose output
./tests/run-all.sh --verbose

# Run single test
./tests/health/test-health.sh

# Check script syntax
bash -n tests/health/test-health.sh
```

## Contributing

### Adding New Tests

1. Create test script in appropriate directory
2. Follow naming convention: `test-feature-action.sh`
3. Include comprehensive assertions
4. Add documentation comments
5. Test with both success and failure scenarios

### Test Categories

- **Unit tests**: Test individual endpoints
- **Integration tests**: Test workflows combining multiple endpoints
- **Error tests**: Test error conditions and edge cases
- **Performance tests**: Test response times and load handling

## Test Coverage

The test suite covers:

- ✅ All REST API endpoints
- ✅ JSON request/response validation
- ✅ HTTP status code validation
- ✅ Error response handling
- ✅ Data consistency and integrity
- ✅ Schema compatibility rules
- ✅ JSLT transformation functionality
- ✅ End-to-end integration scenarios
- ✅ Edge cases and boundary conditions

## Performance

- **Individual tests**: < 1 second each
- **Full test suite**: < 2 minutes
- **Memory usage**: Minimal (curl + bash)
- **Network traffic**: Lightweight JSON payloads

## Limitations

- **No UI testing**: Focuses on API endpoints only
- **No load testing**: Single-threaded request patterns
- **Database state**: Tests may leave test data (cleanup scripts available)
- **Authentication**: Not tested (service currently open)
- **Rate limiting**: Not tested (may need additional tools)

For comprehensive testing including UI, load, and security testing, consider additional testing frameworks and tools.