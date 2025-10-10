# Getting Started

This guide will help you get the JSON Schema Registry and Transformation Service up and running in your development environment.

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

Response:
```json
{
  "consumerId": "mobile-app",
  "name": "Mobile Application",
  "description": "iOS and Android mobile app",
  "createdAt": "2024-01-15T10:30:00",
  "updatedAt": "2024-01-15T10:30:00"
}
```

### 2. Register a Schema

Register a JSON schema for user profiles:

```bash
curl -X POST http://localhost:8080/api/schemas \
  -H "Content-Type: application/json" \
  -d '{
    "subject": "user-profile",
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
    "description": "User profile schema"
  }'
```

Response:
```json
{
  "id": 1,
  "subject": "user-profile",
  "version": 1,
  "schema": {...},
  "compatibility": "BACKWARD",
  "description": "User profile schema",
  "createdAt": "2024-01-15T10:30:00",
  "updatedAt": "2024-01-15T10:30:00"
}
```

### 3. Create Transformation Templates

#### JSLT Engine Template
Create a simple transformation template for the mobile app:

```bash
curl -X POST http://localhost:8080/api/transform/templates/mobile-app \
  -H "Content-Type: application/json" \
  -d '{
    "engine": "jslt",
    "expression": ". | {id: .id, name: .name, email: .email}",
    "description": "Simple field mapping for mobile app"
  }'
```

#### Router Engine Template
Create a router template that routes different data types:

```bash
curl -X POST http://localhost:8080/api/transform/templates/multi-tenant-app \
  -H "Content-Type: application/json" \
  -d '{
    "engine": "router",
    "routerConfig": {
      "type": "router",
      "routes": [
        {
          "condition": "$.type == \"user\"",
          "transformationId": "user-normalization-v1",
          "description": "Normalize user data"
        },
        {
          "condition": "$.type == \"product\"",
          "transformationId": "product-enrichment-v1",
          "description": "Enrich product data"
        }
      ],
      "defaultTransformationId": "generic-transformation-v1",
      "validation": {
        "inputSchema": "canonical-data-schema-v1",
        "outputSchema": "consumer-data-schema-v1"
      }
    },
    "description": "Content-based routing for different data types"
  }'
```

#### Pipeline Engine Template
Create a pipeline template for multi-step processing:

```bash
curl -X POST http://localhost:8080/api/transform/templates/analytics-platform \
  -H "Content-Type: application/json" \
  -d '{
    "engine": "pipeline",
    "pipelineConfig": {
      "type": "pipeline",
      "steps": [
        {
          "name": "validate-input",
          "transformationId": "input-validation-v1",
          "continueOnError": false,
          "description": "Validate input data structure"
        },
        {
          "name": "normalize-data",
          "transformationId": "data-normalization-v1",
          "continueOnError": false,
          "description": "Normalize data format"
        },
        {
          "name": "enrich-data",
          "transformationId": "data-enrichment-v1",
          "continueOnError": true,
          "description": "Add computed fields"
        }
      ],
      "validation": {
        "finalSchema": "enriched-data-schema-v1",
        "intermediateSchemas": {
          "after-step-1": "validated-data-schema-v1",
          "after-step-2": "normalized-data-schema-v1"
        }
      }
    },
    "description": "Multi-step data processing pipeline"
  }'
```

### 4. Transform Data with Different Engines

#### JSLT Engine Transformation
Transform data using the simple JSLT template:

```bash
curl -X POST http://localhost:8080/api/transform/mobile-app \
  -H "Content-Type: application/json" \
  -d '{
    "canonicalJson": {
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
  "transformedJson": {
    "id": 123,
    "name": "John Doe",
    "email": "john@example.com"
  }
}
```

#### Router Engine Transformation - User Data
Transform user data using the router (routes to user-normalization-v1):

```bash
curl -X POST http://localhost:8080/api/transform/multi-tenant-app \
  -H "Content-Type: application/json" \
  -d '{
    "canonicalJson": {
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
  "transformedJson": {
    "user_id": 12345,
    "full_name": "John Doe",
    "email": "john.doe@example.com",
    "department": "engineering",
    "normalized_type": "user"
  }
}
```

#### Router Engine Transformation - Product Data
Transform product data using the router (routes to product-enrichment-v1):

```bash
curl -X POST http://localhost:8080/api/transform/multi-tenant-app \
  -H "Content-Type: application/json" \
  -d '{
    "canonicalJson": {
      "type": "product",
      "id": 67890,
      "name": "Wireless Headphones",
      "category": "electronics",
      "price": 199.99,
      "inStock": true
    }
  }'
```

Response:
```json
{
  "transformedJson": {
    "product_id": 67890,
    "product_name": "Wireless Headphones",
    "category": "electronics",
    "price_usd": 199.99,
    "availability": "in_stock",
    "enriched_category": "electronics"
  }
}
```

#### Pipeline Engine Transformation
Transform data through the multi-step pipeline:

```bash
curl -X POST http://localhost:8080/api/transform/analytics-platform \
  -H "Content-Type: application/json" \
  -d '{
    "canonicalJson": {
      "type": "user",
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
  "transformedJson": {
    "type": "user",
    "id": 12345,
    "firstName": "John",
    "lastName": "Doe",
    "email": "john.doe@example.com",
    "registrationDate": "2024-01-15T10:30:00Z",
    "validated": true,
    "normalized": true,
    "enriched": true,
    "timestamp": "2024-01-15T10:35:00Z"
  }
}
```

## Development Workflow

### Running Tests

```bash
# Run all tests
make test

# Run unit tests only
make test-unit

# Run integration tests only
make test-integration
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
curl -X POST http://localhost:8080/api/transform/templates/mobile-app \
  -H "Content-Type: application/json" \
  -d '{
    "engine": "jslt",
    "expression": ". | {user_id: .id, full_name: (.firstName + \" \" + .lastName), email: .email, member_since: .registrationDate}",
    "description": "Mobile-optimized user profile transformation"
  }'
```

**Web Dashboard Template (Router)**:
```bash
curl -X POST http://localhost:8080/api/transform/templates/web-dashboard \
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
curl -X POST http://localhost:8080/api/transform/templates/analytics-platform \
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
curl -X POST http://localhost:8080/api/transform/mobile-app \
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
curl -X POST http://localhost:8080/api/transform/web-dashboard \
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
curl -X POST http://localhost:8080/api/transform/analytics-platform \
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
curl -X POST http://localhost:8080/api/transform/test-consumer \
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