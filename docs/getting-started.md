# Getting Started

This guide will help you get the JSON Schema Registry and Transformation Service up and running in your development environment.

## Table of Contents

- [Prerequisites](#prerequisites)
- [Quick Start (5 minutes)](#quick-start-5-minutes)
- [Detailed Setup](#detailed-setup)
  - [Option 1: Docker Compose (Recommended)](#option-1-docker-compose-recommended)
  - [Option 2: Local Development](#option-2-local-development)
- [Multi-Subject Consumer Support](#multi-subject-consumer-support)
- [First API Calls](#first-api-calls)
  - [1. Register a Consumer](#1-register-a-consumer)
  - [2. Register a Schema](#2-register-a-schema)
  - [3. Create Transformation Templates](#3-create-transformation-templates)
  - [4. Transform Data with Different Engines](#4-transform-data-with-different-engines)
- [Curl Examples with Files](#curl-examples-with-files)
  - [Prerequisites](#prerequisites-1)
  - [Schema Registration from File](#schema-registration-from-file)
  - [Data Transformation from File](#data-transformation-from-file)
  - [JSLT Template Registration from File](#jslt-template-registration-from-file)
  - [Advanced Examples](#advanced-examples)
- [Development Workflow](#development-workflow)
  - [Running Tests](#running-tests)
  - [Building the Application](#building-the-application)
  - [Database Operations](#database-operations)
  - [Viewing Logs](#viewing-logs)
- [IDE Setup](#ide-setup)
- [Project Structure](#project-structure)
- [Common Issues](#common-issues)
- [Advanced Usage Examples](#advanced-usage-examples)
- [Next Steps](#next-steps)
- [Getting Help](#getting-help)

## Prerequisites

Before you begin, ensure you have the following installed:

- **Java 17** or higher
- **Maven 3.6+**
- **Docker and Docker Compose** (recommended)
- **Git** (for cloning the repository)
- **curl** or **Postman** (for testing APIs)

You can verify installations with:

```bash
java -version
mvn -version
docker --version
docker-compose --version
```

## Quick Start (5 minutes)

The fastest way to get started is using Docker Compose:

```bash
# 1. Clone the repository
git clone <repository-url>
cd simple-schema-registry

# 2. Start all services
make start

# Or manually:
docker-compose up -d

# 3. Wait for services to be ready (about 30 seconds)
sleep 30

# 4. Check service health
curl http://localhost:8080/actuator/health

# 5. Access API documentation
open http://localhost:8080/swagger-ui.html
```

That's it! The service is now running with a PostgreSQL database.

## Detailed Setup

### Option 1: Docker Compose (Recommended)

1. **Clone and navigate**:
   ```bash
   git clone <repository-url>
   cd simple-schema-registry
   ```

2. **Start services**:
   ```bash
   docker-compose up -d
   ```

3. **Verify startup**:
   ```bash
   # Check container status
   docker-compose ps

   # View logs
   docker-compose logs -f app
   ```

4. **Access the application**:
   - API: http://localhost:8080
   - Swagger UI: http://localhost:8080/swagger-ui.html
   - Health Check: http://localhost:8080/actuator/health

### Option 2: Local Development

For development with your own PostgreSQL instance:

1. **Start PostgreSQL**:
   ```bash
   # Using Docker for PostgreSQL
   docker run -d --name postgres-dev \
     -e POSTGRES_DB=schema_registry \
     -e POSTGRES_USER=schema_user \
     -e POSTGRES_PASSWORD=schema_password \
     -p 5432:5432 postgres:15-alpine
   ```

2. **Build the application**:
   ```bash
   mvn clean compile
   ```

3. **Run the application**:
   ```bash
   mvn spring-boot:run
   ```

4. **Verify**:
   ```bash
   curl http://localhost:8080/actuator/health
   ```

## Schema and Consumer Architecture

This service implements a dual-schema architecture with flexible consumer-subject relationships:

### Key Features:
- **Dual Schema Types**: Canonical schemas (source of truth) and consumer-specific output schemas
- **Subject-Based Organization**: Schemas and transformations are organized by subjects
- **Consumer-Subject Templates**: Each consumer-subject pair can have its own transformation template
- **Template Versioning**: Templates support multiple versions with activation/deactivation
- **Schema Validation**: Input validated against canonical schemas, output against consumer schemas

## First API Calls

Let's test the service with some basic API calls using curl.

### 1. Register a Consumer

First, register a consumer application:

```bash
curl -X POST http://localhost:8080/api/consumers \
  -H "Content-Type: application/json" \
  -d '{
    "consumerId": "mobile-app",
    "name": "Mobile Application",
    "description": "iOS and Android mobile app"
  }'
```

**Note**: Consumers no longer require pre-declaring subjects. Subject access is determined by the transformation templates registered for each consumer-subject pair.

Response:
```json
{
  "id": 1,
  "consumerId": "mobile-app",
  "name": "Mobile Application",
  "description": "iOS and Android mobile app",
  "createdAt": "2024-01-15T10:30:00",
  "updatedAt": "2024-01-15T10:30:00"
}
```

### 2. Register Schemas

Register both canonical and consumer output schemas:

#### Canonical Schema (Source of Truth)
```bash
curl -X POST http://localhost:8080/api/schemas/user-profile \
  -H "Content-Type: application/json" \
  -d '{
    "version": "1.0.0",
    "schema": {
      "$schema": "http://json-schema.org/draft-07/schema#",
      "type": "object",
      "properties": {
        "id": {"type": "integer"},
        "name": {"type": "string"},
        "email": {"type": "string", "format": "email"}
      },
      "required": ["id", "name"]
    },
    "compatibility": "BACKWARD",
    "description": "Canonical user profile schema"
  }'
```

#### Consumer Output Schema (Mobile App Format)
```bash
curl -X POST http://localhost:8080/api/consumers/mobile-app/schemas/user-profile \
  -H "Content-Type: application/json" \
  -d '{
    "version": "1.0.0",
    "schema": {
      "$schema": "http://json-schema.org/draft-07/schema#",
      "type": "object",
      "properties": {
        "userId": {"type": "integer"},
        "fullName": {"type": "string"},
        "email": {"type": "string", "format": "email"}
      },
      "required": ["userId", "fullName"]
    },
    "compatibility": "BACKWARD",
    "description": "Mobile app user profile schema"
  }'
```

### 3. Create Transformation Templates

Templates are created per consumer-subject pair and require input/output schema references:

#### JSLT Engine Template
```bash
curl -X POST http://localhost:8080/api/consumers/mobile-app/subjects/user-profile/templates \
  -H "Content-Type: application/json" \
  -d '{
    "version": "1.0.0",
    "engine": "jslt",
    "templateExpression": "{ \\"userId\\": .userId, \\"fullName\\": .fullName, \\"email\\": .email }",
    "inputSchema": {
      "subject": "user-profile"
    },
    "outputSchema": {
      "subject": "user-profile",
      "consumerId": "mobile-app"
    },
    "description": "Mobile app user profile transformation"
  }'
```

#### Router Engine Template
```bash
curl -X POST http://localhost:8080/api/consumers/multi-tenant-app/subjects/data/templates \
  -H "Content-Type: application/json" \
  -d '{
    "version": "1.0.0",
    "engine": "router",
    "configuration": {
      "routes": [
        {
          "condition": ".type == \\"user\\"",
          "transformation": ". | {\\"userId\\": .id, \\"name\\": .name}",
          "description": "User data transformation"
        },
        {
          "condition": ".type == \\"product\\"",
          "transformation": ". | {\\"productId\\": .id, \\"title\\": .name}",
          "description": "Product data transformation"
        }
      ],
      "defaultTransformation": ".",
      "description": "Content-based routing"
    },
    "inputSchema": {
      "subject": "data"
    },
    "outputSchema": {
      "subject": "data",
      "consumerId": "multi-tenant-app"
    },
    "description": "Multi-tenant data routing"
  }'
```

#### Pipeline Engine Template
```bash
curl -X POST http://localhost:8080/api/consumers/analytics-platform/subjects/user-profile/templates \
  -H "Content-Type: application/json" \
  -d '{
    "version": "1.0.0",
    "engine": "pipeline",
    "configuration": {
      "steps": [
        {
          "name": "validate",
          "transformation": ".",
          "continueOnError": false,
          "description": "Input validation"
        },
        {
          "name": "normalize",
          "transformation": ". | {\\"user_id\\": .id, \\"full_name\\": .name}",
          "continueOnError": false,
          "description": "Field normalization"
        },
        {
          "name": "enrich",
          "transformation": ". | . + {\\"processed_at\\": now()}",
          "continueOnError": true,
          "description": "Add metadata"
        }
      ],
      "description": "Analytics data pipeline"
    },
    "inputSchema": {
      "subject": "user-profile"
    },
    "outputSchema": {
      "subject": "user-profile",
      "consumerId": "analytics-platform"
    },
    "description": "Analytics data processing pipeline"
  }'
```

### 4. Transform Data with Different Engines

#### Template Versioning and Activation

Templates support versioning for gradual rollouts and rollbacks:

- **First Template**: Automatically activated when created
- **Additional Versions**: Created as inactive by default, requiring explicit activation

Create and activate a new version:

```bash
# Create a new version (inactive by default)
curl -X POST http://localhost:8080/api/consumers/mobile-app/subjects/user-profile/templates \
  -H "Content-Type: application/json" \
  -d '{
    "version": "2.0.0",
    "engine": "jslt",
    "templateExpression": "{ \"userId\": .userId, \"name\": .fullName, \"email\": .email, \"status\": .accountStatus }",
    "inputSchema": { "subject": "user-profile" },
    "outputSchema": { "subject": "user-profile", "consumerId": "mobile-app" },
    "description": "Enhanced mobile app transformation with status"
  }'

# Activate the new version (deactivates previous version automatically)
curl -X PUT http://localhost:8080/api/consumers/mobile-app/subjects/user-profile/templates/versions/2.0.0/activate

# Verify active version
curl http://localhost:8080/api/consumers/mobile-app/subjects/user-profile/templates/active
```

**Why Manual Activation?** Unlike some systems that auto-activate new versions, this design provides:
- **Safety**: Test transformations before production activation
- **Control**: Gradual rollouts and A/B testing capabilities
- **Rollback**: Quick switching between versions for emergencies
- **Audit**: Clear activation history for compliance

#### JSLT Engine Transformation
Transform canonical data for the mobile app consumer:

```bash
curl -X POST "http://localhost:8080/api/consumers/mobile-app/subjects/user-profile/transform" \
  -H "Content-Type: application/json" \
  -d '{
    "data": {
      "id": 123,
      "name": "John Doe",
      "email": "john@example.com",
      "internalId": "abc-123",
      "createdAt": "2024-01-15T10:30:00Z"
    }
  }'
```

Response:
```json
{
  "transformedData": {
    "userId": 123,
    "fullName": "John Doe",
    "email": "john@example.com"
  },
  "subject": "user-profile",
  "consumerId": "mobile-app",
  "templateVersion": "1.0.0"
}
```

**Note**: The transformation template `{"id": .id, "name": .name, "email": .email}` explicitly selects only the desired fields, effectively filtering out sensitive or unnecessary data like `internalId` and `createdAt`.

#### Router Engine Transformation
Transform data using content-based routing:

```bash
curl -X POST "http://localhost:8080/api/consumers/multi-tenant-app/subjects/data/transform" \
  -H "Content-Type: application/json" \
  -d '{
    "data": {
      "type": "user",
      "id": 12345,
      "firstName": "John",
      "lastName": "Doe",
      "email": "john.doe@example.com",
      "department": "engineering"
    }
  }'
```

Response:
```json
{
  "transformedData": {
    "userId": 12345,
    "name": "John Doe"
  },
  "subject": "data",
  "consumerId": "multi-tenant-app",
  "templateVersion": "1.0.0"
}
```

#### Pipeline Engine Transformation
Transform data through multi-step processing:

```bash
curl -X POST "http://localhost:8080/api/consumers/analytics-platform/subjects/user-profile/transform" \
  -H "Content-Type: application/json" \
  -d '{
    "data": {
      "id": 12345,
      "firstName": "John",
      "lastName": "Doe",
      "email": "john.doe@example.com",
      "registrationDate": "2024-01-15T10:30:00Z"
    }
  }'
```

Response:
```json
{
  "transformedData": {
    "user_id": 12345,
    "full_name": "John Doe",
    "processed_at": "2024-01-15T10:35:00Z"
  },
  "subject": "user-profile",
  "consumerId": "analytics-platform",
  "templateVersion": "1.0.0"
}
```

## Curl Examples with Files

For more advanced usage, you can work with separate files for schemas, data payloads, and JSLT transformation templates. These examples use `jq` to construct the API request payloads from files while maintaining the required JSON structure.

### Prerequisites

Install `jq` for JSON processing:

```bash
# Ubuntu/Debian
sudo apt-get install jq

# macOS
brew install jq

# Or download from https://stedolan.github.io/jq/
```

### Helper Scripts Overview

The project includes several helper scripts in `tests/utils/scripts/` for common operations:

#### `register-schema-from-file.sh`
Registers a JSON schema from a file with the Schema Registry.

**Arguments:**
- `schema_file`: Path to JSON schema file (must be valid JSON)
- `subject`: Schema subject name (e.g., "user-profile")
- `compatibility`: Schema compatibility mode ("BACKWARD", "FORWARD", "FULL", "NONE")
- `description`: Optional human-readable description

**Example:**
```bash
./tests/utils/scripts/register-schema-from-file.sh user-schema.json user-profile BACKWARD "User profile schema"
```

#### `register-jslt-template-from-file.sh`
Registers a JSLT transformation template from a file for a consumer and subject.

**Arguments:**
- `jslt_file`: Path to JSLT template file
- `consumer_id`: Consumer ID to register the template for (consumer must already exist)
- `subject`: Schema subject for the template
- `version`: Template version (default: "1.0.0")
- `input_subject`: Input schema subject (default: same as subject)
- `description`: Optional description (default: "JSLT template from file")

**Example:**
```bash
./tests/utils/scripts/register-jslt-template-from-file.sh remove-notes.jslt mobile-app user-profile 1.0.0 user-profile "Remove notes from publications"
```

#### `transform-from-file.sh`
Transforms JSON data from a file using a registered transformation template.

**Arguments:**
- `data_file`: Path to JSON data file to transform (must be valid JSON)
- `consumer_id`: Consumer ID with registered transformation template
- `subject`: Schema subject for the data (must match consumer's registered subjects)
- `version`: Optional template version (uses active version if not specified)

**Example:**
```bash
./tests/utils/scripts/transform-from-file.sh user-data.json mobile-app user-profile
./tests/utils/scripts/transform-from-file.sh user-data.json mobile-app user-profile 1.0.0
```

### Schema Registration from File

Save your JSON schema to a file (e.g., `user-schema.json`):

```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "type": "object",
  "properties": {
    "id": {"type": "integer"},
    "name": {"type": "string"},
    "email": {"type": "string", "format": "email"}
  },
  "required": ["id", "name"]
}
```

Register the schema using the helper script:

```bash
# Using the helper script
./tests/utils/scripts/register-schema-from-file.sh user-schema.json user-profile 1.0.0 BACKWARD "User profile schema"

# Or manually with curl and jq
jq -n \
  --arg subject "user-profile" \
  --arg compatibility "BACKWARD" \
  --arg description "User profile schema" \
  --argjson schema "$(cat user-schema.json)" \
  '{
    subject: $subject,
    schema: $schema,
    compatibility: $compatibility,
    description: $description
  }' | \
curl -X POST http://localhost:8080/api/schemas/user-profile \
  -H "Content-Type: application/json" \
  -d @-
```

### JSLT Template Registration from File

Save your JSLT transformation template to a file (e.g., `remove-notes.jslt`):

```jslt
{
  "publications": [
    .publications[]
    | {
        id: .id,
        title: .title,
        // notes field excluded
        createdAt: .createdAt
      }
  ]
}
```

Register the template using the helper script:

```bash
# Using the helper script
./tests/utils/scripts/register-jslt-template-from-file.sh remove-notes.jslt mobile-app user-profile 1.0.0 user-profile "Remove notes from publications"

# Or manually with curl
curl -X POST http://localhost:8080/api/consumers/mobile-app/subjects/user-profile/templates \
  -H "Content-Type: application/json" \
  -d '{
    "version": "1.0.0",
    "engine": "jslt",
    "templateExpression": "'"$(cat remove-notes.jslt)"'",
    "inputSchema": {
      "subject": "user-profile"
    },
    "outputSchema": {
      "subject": "user-profile",
      "consumerId": "mobile-app"
    },
    "description": "Remove notes from publications"
  }'
```

### Data Transformation from File

Save your data payload to a file (e.g., `user-data.json`):

```json
{
  "id": 123,
  "name": "John Doe",
  "email": "john@example.com",
  "internalId": "abc-123",
  "createdAt": "2024-01-15T10:30:00Z"
}
```

Transform the data using the helper script:

```bash
# Using the helper script
./tests/utils/scripts/transform-from-file.sh user-data.json mobile-app user-profile

# Or manually with curl and jq
jq -n \
  --argjson data "$(cat user-data.json)" \
  '{
    data: $data
  }' | \
curl -X POST "http://localhost:8080/api/consumers/mobile-app/subjects/user-profile/transform" \
  -H "Content-Type: application/json" \
  -d @-
```

### Advanced Examples

#### Complete Workflow: Register Template, Transform Data

```bash
# 1. Register JSLT template
./tests/utils/scripts/register-jslt-template-from-file.sh remove-notes.jslt mobile-app user-profile 1.0.0 user-profile "Remove notes from publications"

# 2. Transform data using the registered template
./tests/utils/scripts/transform-from-file.sh publication-data.json mobile-app user-profile
```

#### Register Multiple Schemas

```bash
# Product schema
./tests/utils/scripts/register-schema-from-file.sh product-schema.json product-catalog 1.0.0 FORWARD "Product catalog schema"

# Order schema
./tests/utils/scripts/register-schema-from-file.sh order-schema.json order-data 1.0.0 BACKWARD "Order data schema"
```

#### Batch Transformations

```bash
# Transform multiple data files
for data_file in data/user-*.json; do
  ./tests/utils/scripts/transform-from-file.sh "$data_file" mobile-app user-profile
done
```

#### Using with Make

Add to your `Makefile`:

```makefile
# Register schema from file
register-schema:
	./tests/utils/scripts/register-schema-from-file.sh $(SCHEMA_FILE) $(SUBJECT) $(VERSION) $(COMPATIBILITY) "$(DESCRIPTION)"

# Register JSLT template from file
register-jslt-template:
	./tests/utils/scripts/register-jslt-template-from-file.sh $(JSLT_FILE) $(CONSUMER) $(SUBJECT) $(VERSION) $(INPUT_SUBJECT) "$(DESCRIPTION)"

# Transform data from file
transform-data:
	./tests/utils/scripts/transform-from-file.sh $(DATA_FILE) $(CONSUMER) $(SUBJECT) $(VERSION)
```

Then use:

```bash
make register-schema SCHEMA_FILE=user-schema.json SUBJECT=user-profile COMPATIBILITY=BACKWARD DESCRIPTION="User schema"
make register-jslt-template JSLT_FILE=remove-notes.jslt CONSUMER=mobile-app SUBJECT=user-profile VERSION=1.0.0 INPUT_SUBJECT=user-profile DESCRIPTION="Remove notes"
make transform-data DATA_FILE=user-data.json CONSUMER=mobile-app SUBJECT=user-profile
```

## Development Workflow

### Running Tests

The project includes comprehensive integration tests that validate the entire system. Tests are organized into suites with specific execution order due to dependencies.

#### Test Execution Order

Tests must be run in the following sequence to ensure proper setup and data dependencies:

1. **Health Tests** - Basic service availability and health endpoints
2. **Consumers Tests** - Consumer registration and management
3. **Schemas Tests** - Schema registration, versioning, and compatibility
4. **Transform Tests** - Data transformation templates and engines
5. **Workflows Tests** - End-to-end workflow validation
6. **Error Handling Tests** - Error conditions and edge cases

#### Running All Tests

```bash
# Run all tests in correct order
make test

# Or use the test runner directly
./tests/run-all.sh
```

#### Running Individual Test Suites

If you need to run specific test suites (not recommended for normal development), ensure prerequisites are met:

```bash
# Health checks only
./tests/run-all.sh --health-only

# Critical tests only (health + basic consumer/schema operations)
./tests/run-all.sh --quick

# Individual suites (ensure dependencies are satisfied)
./tests/run-all.sh --health-only  # Run first
./tests/run-all.sh --consumers    # Requires health
./tests/run-all.sh --schemas      # Requires consumers
./tests/run-all.sh --transform    # Requires schemas
```

#### Test Dependencies

- **Health tests** have no dependencies
- **Consumer tests** require the service to be healthy
- **Schema tests** require consumers to exist for testing
- **Transform tests** require schemas and consumers
- **Workflow tests** require all previous components
- **Error handling tests** require full system setup

#### Test Data Management

Tests use unique identifiers (timestamps) to avoid conflicts between runs. Test data is persistent in the database, so:

- Tests can be run multiple times safely
- Each run creates fresh data with unique identifiers
- No manual cleanup is required between test runs

#### Debugging Test Failures

```bash
# Run with verbose output
./tests/run-all.sh --verbose

# Run specific failing test
bash tests/schemas/test-schema-register.sh

# Check service logs during test execution
docker-compose logs -f app
```

### Building the Application

```bash
# Build JAR file
make build

# Build Docker image
make build-docker
```

### Database Operations

```bash
# Run migrations
make db-migrate

# Check migration status
make db-info

# Clean database
make db-clean
```

### Viewing Logs

```bash
# All services
make logs

# Application only
make logs-app

# Database only
make logs-db
```

## IDE Setup

### IntelliJ IDEA

1. **Import Project**:
   - File → Open → Select `pom.xml`
   - Choose "Open as Project"

2. **Configure JDK**:
   - File → Project Structure → Project SDK → Java 17

3. **Run Configuration**:
   - Run → Edit Configurations
   - Add new "Spring Boot" configuration
   - Main class: `ru.vegorov.schemaregistry.SchemaRegistryApplication`

### VS Code

1. **Install Extensions**:
   - Java Extension Pack
   - Spring Boot Extension Pack

2. **Open Project**:
   - File → Open Folder → Select project directory

3. **Run/Debug**:
   - F5 or Run → Start Debugging
   - Select "Java" environment

## Project Structure

```
simple-schema-registry/
├── src/
│   ├── main/
│   │   ├── java/ru/vegorov/schemaregistry/
│   │   │   ├── controller/     # REST controllers
│   │   │   ├── dto/           # Data transfer objects
│   │   │   ├── entity/        # JPA entities
│   │   │   ├── exception/     # Custom exceptions
│   │   │   ├── repository/    # Data repositories
│   │   │   └── service/       # Business logic
│   │   └── resources/
│   │       ├── db/migration/  # Flyway migrations
│   │       └── application.properties
│   └── test/                  # Test classes
├── docs/                      # Documentation
├── Dockerfile                 # Docker image
├── docker-compose.yml         # Container orchestration
├── Makefile                   # Development commands
└── pom.xml                    # Maven configuration
```

## Common Issues

### Service Won't Start

**Problem**: Application fails to start with database connection errors.

**Solution**:
```bash
# Check if database is running
docker-compose ps

# Restart database
docker-compose restart db

# Check database logs
make logs-db
```

### Port Already in Use

**Problem**: Port 8080 is already occupied.

**Solution**:
```bash
# Find what's using the port
lsof -i :8080

# Change port in application.properties
# server.port=8081

# Or use a different port for Docker
# docker-compose up -d --scale app=1 -p 8081:8080
```

### Database Migration Errors

**Problem**: Flyway migration fails.

**Solution**:
```bash
# Check migration status
make db-info

# Clean and retry (WARNING: destroys data)
make db-clean
make db-migrate
```

## Advanced Usage Examples

### End-to-End Workflow: Multi-Tenant E-commerce Platform

Let's create a complete workflow for an e-commerce platform that handles different types of data:

#### 1. Register Consumers
```bash
# Mobile app consumer
curl -X POST http://localhost:8080/api/consumers \
  -H "Content-Type: application/json" \
  -d '{
    "consumerId": "mobile-app",
    "name": "Mobile Shopping App",
    "description": "iOS and Android e-commerce app"
  }'

# Web dashboard consumer
curl -X POST http://localhost:8080/api/consumers \
  -H "Content-Type: application/json" \
  -d '{
    "consumerId": "web-dashboard",
    "name": "Admin Dashboard",
    "description": "Internal admin dashboard"
  }'

# Analytics platform consumer
curl -X POST http://localhost:8080/api/consumers \
  -H "Content-Type: application/json" \
  -d '{
    "consumerId": "analytics-platform",
    "name": "Analytics Platform",
    "description": "Data analytics and reporting platform"
  }'
```

#### 2. Register Schemas
```bash
# User profile schema
curl -X POST http://localhost:8080/api/schemas \
  -H "Content-Type: application/json" \
  -d '{
    "subject": "user-profile",
    "schema": {
      "$schema": "http://json-schema.org/draft-07/schema#",
      "type": "object",
      "properties": {
        "id": {"type": "integer"},
        "firstName": {"type": "string"},
        "lastName": {"type": "string"},
        "email": {"type": "string", "format": "email"},
        "registrationDate": {"type": "string", "format": "date-time"}
      },
      "required": ["id", "firstName", "lastName", "email"]
    },
    "compatibility": "BACKWARD",
    "description": "User profile data schema"
  }'

# Product catalog schema
curl -X POST http://localhost:8080/api/schemas \
  -H "Content-Type: application/json" \
  -d '{
    "subject": "product-catalog",
    "schema": {
      "$schema": "http://json-schema.org/draft-07/schema#",
      "type": "object",
      "properties": {
        "id": {"type": "integer"},
        "name": {"type": "string"},
        "category": {"type": "string"},
        "price": {"type": "number"},
        "inStock": {"type": "boolean"}
      },
      "required": ["id", "name", "category", "price"]
    },
    "compatibility": "BACKWARD",
    "description": "Product catalog data schema"
  }'
```

#### 3. Create Advanced Transformation Templates

**Mobile App Template (JSLT)**:
```bash
curl -X POST http://localhost:8080/api/consumers/templates/mobile-app \
  -H "Content-Type: application/json" \
  -d '{
    "engine": "jslt",
    "expression": ". | {\"user_id\": .id, \"full_name\": (.firstName + \" \" + .lastName), \"email\": .email, \"member_since\": .registrationDate}",
    "description": "Mobile-optimized user profile transformation"
  }'
```

**Web Dashboard Template (Router)**:
```bash
curl -X POST http://localhost:8080/api/consumers/templates/web-dashboard \
  -H "Content-Type: application/json" \
  -d '{
    "engine": "router",
    "routerConfig": {
      "type": "router",
      "routes": [
        {
          "condition": "$.type == \"user\"",
          "transformationId": "user-admin-view",
          "description": "Admin view of user data"
        },
        {
          "condition": "$.type == \"product\"",
          "transformationId": "product-admin-view",
          "description": "Admin view of product data"
        },
        {
          "condition": "$.type == \"order\"",
          "transformationId": "order-admin-view",
          "description": "Admin view of order data"
        }
      ],
      "defaultTransformationId": "generic-admin-view",
      "validation": {
        "inputSchema": "canonical-data-schema-v1",
        "outputSchema": "admin-view-schema-v1"
      }
    },
    "description": "Admin dashboard data routing"
  }'
```

**Analytics Platform Template (Pipeline)**:
```bash
curl -X POST http://localhost:8080/api/consumers/templates/analytics-platform \
  -H "Content-Type: application/json" \
  -d '{
    "engine": "pipeline",
    "pipelineConfig": {
      "type": "pipeline",
      "steps": [
        {
          "name": "validate-schema",
          "transformationId": "schema-validation-v1",
          "continueOnError": false,
          "description": "Validate data against registered schemas"
        },
        {
          "name": "normalize-fields",
          "transformationId": "field-normalization-v1",
          "continueOnError": false,
          "description": "Normalize field names and formats"
        },
        {
          "name": "add-metadata",
          "transformationId": "metadata-enrichment-v1",
          "continueOnError": false,
          "description": "Add processing metadata and timestamps"
        },
        {
          "name": "anonymize-sensitive",
          "transformationId": "data-anonymization-v1",
          "continueOnError": true,
          "description": "Anonymize sensitive data for analytics"
        }
      ],
      "validation": {
        "finalSchema": "analytics-ready-schema-v1",
        "intermediateSchemas": {
          "after-step-1": "validated-schema-v1",
          "after-step-2": "normalized-schema-v1",
          "after-step-3": "enriched-schema-v1"
        }
      }
    },
    "description": "Analytics data processing pipeline"
  }'
```

#### 4. Test End-to-End Transformations

**Mobile App User Data**:
```bash
curl -X POST "http://localhost:8080/api/consumers/mobile-app/transform?subject=user-profile" \
  -H "Content-Type: application/json" \
  -d '{
    "canonicalJson": {
      "type": "user",
      "id": 12345,
      "firstName": "Alice",
      "lastName": "Johnson",
      "email": "alice.johnson@example.com",
      "registrationDate": "2024-01-15T09:30:00Z"
    }
  }'
```

**Web Dashboard Product Data**:
```bash
curl -X POST "http://localhost:8080/api/consumers/web-dashboard/transform?subject=product-catalog" \
  -H "Content-Type: application/json" \
  -d '{
    "canonicalJson": {
      "type": "product",
      "id": 67890,
      "name": "Bluetooth Speaker",
      "category": "electronics",
      "price": 79.99,
      "inStock": true,
      "supplier": "AudioTech Inc",
      "internalNotes": "High-quality audio, good reviews"
    }
  }'
```

**Analytics Platform Processing**:
```bash
curl -X POST "http://localhost:8080/api/consumers/analytics-platform/transform?subject=user-profile" \
  -H "Content-Type: application/json" \
  -d '{
    "canonicalJson": {
      "type": "user",
      "id": 12345,
      "firstName": "Alice",
      "lastName": "Johnson",
      "email": "alice.johnson@example.com",
      "phone": "+1-555-0123",
      "registrationDate": "2024-01-15T09:30:00Z",
      "lastLogin": "2024-01-20T14:22:00Z"
    }
  }'
```

### Java-Based Transformation Registration

For advanced use cases, you can register transformations programmatically:

```java
// Example: Register a router transformation via Java code
RouterConfiguration routerConfig = new RouterConfiguration(
    List.of(
        new RouterConfiguration.Route("$.type == 'order'", "order-processing-v1", "Process order data"),
        new RouterConfiguration.Route("$.type == 'payment'", "payment-processing-v1", "Process payment data")
    ),
    "default-processing-v1",
    new RouterConfiguration.ValidationConfig("input-schema-v1", "output-schema-v1")
);

TransformationTemplateRequest request = new TransformationTemplateRequest(
    "router", routerConfig, "Order processing router"
);

// Register via REST API or service call
transformationService.createOrUpdateTemplate("order-processor", request);
```

### Testing Advanced Transformations

#### Unit Testing Engines
```java
@SpringBootTest
class TransformationEngineTest {

    @Autowired
    private RouterTransformationEngine routerEngine;

    @Autowired
    private PipelineTransformationEngine pipelineEngine;

    @Test
    void routerEngine_shouldRouteBasedOnConditions() {
        // Test router logic
    }

    @Test
    void pipelineEngine_shouldExecuteStepsInOrder() {
        // Test pipeline execution
    }
}
```

#### Integration Testing
```bash
# Test complete workflows
make test-integration

# Test specific transformation scenarios
curl -X POST "http://localhost:8080/api/consumers/test-consumer/transform?subject=test-subject" \
  -H "Content-Type: application/json" \
  -d @test-data.json
```

## Next Steps

Now that you have the service running with advanced transformation engines, you can:

1. **Explore the API**: Use Swagger UI to test all endpoints
2. **Add More Schemas**: Register schemas for different data types
3. **Create Consumers**: Add more consumer applications
4. **Build Complex Transformations**: Combine router and pipeline engines
5. **Implement Custom Engines**: Extend the system with domain-specific engines
6. **Write Tests**: Add comprehensive unit and integration tests
7. **Monitor Performance**: Use the database diagnostics tools
8. **Customize Configuration**: Modify settings for your environment

## Getting Help

- **API Documentation**: http://localhost:8080/swagger-ui.html
- **Health Checks**: http://localhost:8080/actuator/health
- **Logs**: `make logs`
- **Database Diagnostics**: `make db-status`, `make db-schemas-count`
- **Issues**: Check the troubleshooting guide

For more detailed information, see:
- [API Reference](api-reference.md)
- [Architecture](architecture.md)
- [Transformation Extension Plan](transformation-extension-plan.md)
- [Schema Evolution Workflow](schema-evolution-workflow.md)
- [Deployment](deployment.md)
- [Troubleshooting](troubleshooting.md)