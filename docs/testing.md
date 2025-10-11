# Testing Guide

This guide explains the testing strategy, execution order, and best practices for the JSON Schema Registry and Transformation Service.

## Test Overview

The project uses a comprehensive integration testing approach that validates the entire system end-to-end. Tests are organized into logical suites with specific execution dependencies.

## Test Architecture

### Test Types

- **Integration Tests**: Full system validation with real database and HTTP calls
- **API Tests**: REST endpoint validation with various scenarios
- **Workflow Tests**: End-to-end business process validation

### Test Organization

Tests are organized in the `tests/` directory:

```
tests/
├── run-all.sh              # Main test runner
├── utils/
│   ├── common.sh          # Shared utilities and assertions
│   └── setup.sh           # Test environment setup
├── health/                # Service health validation
├── consumers/             # Consumer management tests
├── schemas/               # Schema registry tests
├── transform/             # Data transformation tests
├── workflows/             # End-to-end workflow tests
└── error-handling/        # Error condition tests
```

## Test Execution Order

Tests **MUST** be executed in the following sequence due to data dependencies:

### 1. Health Tests (`tests/health/`)
**Purpose**: Validate basic service availability and health endpoints
**Dependencies**: None
**Tests**:
- `test-health.sh` - Basic health checks and service endpoints
- `test-actuator.sh` - Spring Boot Actuator endpoints

### 2. Consumer Tests (`tests/consumers/`)
**Purpose**: Test consumer registration and management
**Dependencies**: Service must be healthy
**Tests**:
- `test-consumer-register.sh` - Consumer registration
- `test-consumer-get.sh` - Consumer retrieval
- `test-consumer-list.sh` - Consumer listing

### 3. Schema Tests (`tests/schemas/`)
**Purpose**: Test schema registration, versioning, and compatibility
**Dependencies**: Consumers must exist for testing
**Tests**:
- `test-schema-register.sh` - Schema registration with versioning
- `test-schema-get-specific.sh` - Retrieve specific schema versions
- `test-schema-get-latest.sh` - Get latest schema versions
- `test-schema-get-all.sh` - List all schema versions
- `test-schema-subjects.sh` - Schema subject management
- `test-schema-compatibility.sh` - Compatibility checking

### 4. Transform Tests (`tests/transform/`)
**Purpose**: Test data transformation templates and engines
**Dependencies**: Schemas and consumers must exist
**Tests**:
- `test-transform-template-create.sh` - Template creation/update
- `test-transform-template-get.sh` - Template retrieval
- `test-transform-data.sh` - Data transformation execution
- `test-transform-engines.sh` - Transformation engine validation
- `test-transform-router.sh` - Router engine functionality
- `test-transform-pipeline.sh` - Pipeline engine functionality

### 5. Workflow Tests (`tests/workflows/`)
**Purpose**: End-to-end business process validation
**Dependencies**: All previous components must be functional
**Tests**:
- `test-full-workflow.sh` - Complete user journey
- `test-schema-evolution.sh` - Schema evolution scenarios

### 6. Error Handling Tests (`tests/error-handling/`)
**Purpose**: Validate error conditions and edge cases
**Dependencies**: Full system setup required
**Tests**:
- `test-errors-400.sh` - Bad request validation
- `test-errors-404.sh` - Not found scenarios
- `test-errors-409.sh` - Conflict resolution

## Running Tests

### Full Test Suite

```bash
# Run all tests in correct order
make test

# Or use the test runner directly
./tests/run-all.sh
```

### Partial Test Execution

For development and debugging, you can run specific test suites:

```bash
# Health checks only
./tests/run-all.sh --health-only

# Critical functionality (health + consumers + schemas)
./tests/run-all.sh --quick

# Individual suites (ensure dependencies are met)
./tests/run-all.sh --consumers
./tests/run-all.sh --schemas
./tests/run-all.sh --transform
./tests/run-all.sh --workflows
./tests/run-all.sh --error-handling
```

### Verbose Output

```bash
# Run with detailed output
./tests/run-all.sh --verbose

# Run individual test with verbose output
bash tests/schemas/test-schema-register.sh
```

## Test Data Management

### Unique Identifiers

All tests use timestamp-based unique identifiers to prevent conflicts:

```bash
# Example: Consumer ID generation
timestamp=$(date +%s)
consumer_id="test-consumer-$timestamp"
```

### Data Persistence

- Test data persists in the database between runs
- Each test run creates fresh data with unique identifiers
- No manual cleanup required
- Tests are safe to run multiple times

### Test Isolation

- Tests use unique identifiers to avoid interference
- Database state is maintained between test suites
- Dependencies are explicitly managed through execution order

## Test Utilities

### Common Functions (`tests/utils/common.sh`)

Shared utilities for all tests:

- `make_request` - HTTP request helper with timeout
- `assert_response` - Response code validation
- `assert_contains` - Content validation
- `assert_json_field` - JSON field validation
- `create_test_consumer` - Consumer creation helper
- `create_test_schema` - Schema creation helper
- `create_test_template` - Template creation helper

### Assertions

Tests use comprehensive assertions:

```bash
# Response code validation
assert_response "$http_code" 201 "Schema registration should succeed"

# JSON field validation
assert_json_field "$response_body" "subject" "test-user-profile"
assert_json_field "$response_body" "version" 1

# Content validation
assert_contains "$response_body" '"createdAt"' "Should contain timestamp"
```

## Debugging Test Failures

### Common Issues

1. **Service Not Ready**
   ```bash
   # Check service health
   curl http://localhost:8080/actuator/health

   # Wait for startup
   sleep 30
   ```

2. **Database Connection Issues**
   ```bash
   # Check database
   docker-compose exec db pg_isready -U schema_user -d schema_registry

   # Restart services
   docker-compose restart
   ```

3. **Test Order Violations**
   ```bash
   # Always run in dependency order
   ./tests/run-all.sh --health-only  # First
   ./tests/run-all.sh --consumers    # Second
   ./tests/run-all.sh --schemas      # Third
   ```

4. **Data Conflicts**
   ```bash
   # Tests use unique timestamps - no cleanup needed
   # If issues persist, restart with clean database
   make clean && make start
   ```

### Debugging Steps

1. **Check Service Logs**
   ```bash
   docker-compose logs -f app
   ```

2. **Run Individual Tests**
   ```bash
   bash tests/health/test-health.sh
   bash tests/schemas/test-schema-register.sh
   ```

3. **Verify Prerequisites**
   ```bash
   # Ensure service is healthy
   curl http://localhost:8080/actuator/health

   # Check database connectivity
   docker-compose exec db psql -U schema_user -d schema_registry -c "SELECT 1"
   ```

4. **Test API Manually**
   ```bash
   # Test consumer creation
   curl -X POST http://localhost:8080/api/consumers \
     -H "Content-Type: application/json" \
     -d '{"consumerId": "debug-test", "name": "Debug Test"}'
   ```

## Test Development

### Adding New Tests

1. **Create Test File**
   ```bash
   touch tests/new-feature/test-new-feature.sh
   chmod +x tests/new-feature/test-new-feature.sh
   ```

2. **Follow Test Structure**
   ```bash
   #!/bin/bash
   source "$(dirname "$0")/../utils/common.sh"

   echo "Running New Feature Tests"
   echo "========================="

   # Generate unique IDs
   timestamp=$(date +%s)
   test_id="test-new-feature-$timestamp"

   # Test implementation
   # ...

   print_test_summary
   ```

3. **Add to Test Runner**
   Update `tests/run-all.sh` to include the new test suite in the appropriate position.

### Test Best Practices

- Use unique identifiers for all test data
- Include comprehensive assertions
- Test both success and failure scenarios
- Document test dependencies
- Keep tests focused and atomic
- Use descriptive test names and messages

## Performance Testing

### Load Testing

```bash
# Basic load test
for i in {1..100}; do
  curl -s -X POST http://localhost:8080/api/schemas \
    -H "Content-Type: application/json" \
    -d "{\"subject\": \"load-test-$i\", \"schema\": {\"type\": \"object\"}}" &
done
wait
```

### Memory and Resource Testing

```bash
# Monitor resource usage
docker stats

# Check database connections
docker-compose exec db psql -U schema_user -d schema_registry \
  -c "SELECT count(*) FROM pg_stat_activity"
```

## Continuous Integration

### CI Pipeline

The test suite is designed for CI/CD pipelines:

- Tests are self-contained and don't require external setup
- Unique identifiers prevent conflicts between parallel runs
- Comprehensive error reporting for debugging
- Fast execution with `--quick` option for PR validation

### CI Configuration Example

```yaml
# .github/workflows/test.yml
name: Test
on: [push, pull_request]
jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - name: Set up JDK 17
        uses: actions/setup-java@v3
        with:
          java-version: '17'
          distribution: 'temurin'
      - name: Run tests
        run: make test
```

## Troubleshooting

See the [Troubleshooting Guide](troubleshooting.md) for detailed solutions to common test issues.

## Related Documentation

- [Getting Started](getting-started.md) - Basic setup and first API calls
- [API Reference](api-reference.md) - Complete API documentation
- [Troubleshooting](troubleshooting.md) - Common issues and solutions
- [Architecture](architecture.md) - System design and components</content>
</xai:function_call<parameter name="filePath">docs/testing.md