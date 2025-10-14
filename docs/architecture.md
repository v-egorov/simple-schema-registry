# System Architecture

This document describes the architecture and design of the JSON Schema Registry and Transformation Service.

## Overview

The JSON Schema Registry and Transformation Service is a Spring Boot application that provides centralized schema management and JSON data transformation capabilities for multiple consumer applications. It follows a layered architecture pattern with clear separation of concerns.

## Architecture Principles

- **Layered Architecture**: Clear separation between presentation, business logic, and data layers
- **RESTful Design**: Stateless HTTP APIs following REST principles
- **Extensible Design**: Pluggable transformation engines and modular components
- **Data Integrity**: ACID transactions and referential integrity
- **API Documentation**: OpenAPI 3.0 specification with Swagger UI

## System Components

### 1. Presentation Layer (Controllers)

The presentation layer handles HTTP requests and responses, providing RESTful APIs for:

- **SchemaRegistryController**: Schema registration, versioning, and compatibility checking
- **ConsumerController**: Consumer application management
- **TransformationController**: JSON data transformation operations

**Key Features**:

- Request validation using Jakarta Validation
- OpenAPI annotations for automatic documentation
- Global exception handling with consistent error responses
- Content negotiation (JSON)

### 2. Business Logic Layer (Services)

The service layer contains the core business logic:

- **SchemaRegistryService**: Handles schema versioning, compatibility validation, and retrieval for both canonical and consumer output schemas
- **TransformationService**: Manages transformation template creation, retrieval, and executes transformations
- **TransformationVersionService**: Handles template version activation, deactivation, and lifecycle management
- **ConsumerService**: Manages consumer application registrations
- **ConfigurationValidator**: Validates transformation engine configurations
- **Transformation Engines**: Pluggable engines for different transformation languages (JSLT, Router, Pipeline)

**Key Features**:

- Transaction management with Spring's `@Transactional`
- Business rule validation and schema relationships
- Template versioning with active version management
- Engine abstraction for extensibility
- Schema validation integration

### 3. Data Access Layer (Repositories)

Spring Data JPA repositories provide data access:

- **SchemaRepository**: CRUD operations for schema entities
- **ConsumerRepository**: CRUD operations for consumer entities
- **TransformationTemplateRepository**: CRUD operations for transformation templates

### 4. Data Model (Entities)

JPA entities representing the domain model:

- **SchemaEntity**: Stores schema definitions with versioning, supporting both canonical and consumer output schema types
- **ConsumerEntity**: Represents consumer applications
- **TransformationTemplateEntity**: Stores transformation templates with versioning, linked to input/output schemas
- **SchemaType**: Enum defining schema types (CANONICAL, CONSUMER_OUTPUT)

## Data Flow

### Schema Registration Flow

1. Client sends schema registration request
2. Controller validates request data
3. Service checks compatibility with existing versions
4. Repository saves new schema version
5. Response contains schema metadata

### Data Transformation Flow

#### High-Level Flow

1. Client sends transformation request with canonical JSON data
2. Controller validates request parameters and consumer/subject existence
3. Service retrieves the active transformation template version for the consumer-subject pair
4. Appropriate transformation engine processes the data according to the template
5. Transformed JSON is validated against the consumer output schema (if configured)
6. Transformed JSON is returned to client

#### Detailed Sequence Diagram

```
Client Application                    TransformationController              TransformationService              TransformationEngine
        |                                           |                              |                              |
        |  POST /api/consumers/{consumerId}/subjects/{subject}/transform           |                              |
        |  Content-Type: application/json                                          |                              |
        |  Body: {"data": {...}}                                                   |                              |
        |------------------------------------------>|                              |                              |
        |                                           |                              |                              |
        |                                           | 1. Validate request params   |                              |
        |                                           |    - consumerId exists       |                              |
        |                                           |    - subject exists          |                              |
        |                                           |    - JSON payload structure  |                              |
        |                                           |                              |                              |
        |                                           | 2. Call service.transform()  |                              |
        |                                           |----------------------------->|                              |
        |                                           |                              |                              |
        |                                           |                              | 3. Retrieve active template  |
        |                                           |                              |    version for consumer+subject|
        |                                           |                              |                              |
        |                                           |                              | 4. Validate input against    |
        |                                           |                              |    canonical schema          |
        |                                           |                              |                              |
        |                                           |                              | 5. Determine engine type     |
        |                                           |                              |    (jslt/router/pipeline)    |
        |                                           |                              |                              |
        |                                           |                              | 6. Instantiate appropriate   |
        |                                           |                              |    engine                    |
        |                                           |                              |                              |
        |                                           |                              | 7. Call engine.transform()   |
        |                                           |                              |----------------------------->|
        |                                           |                              |                              |
        |                                           |                              |                              | 8. Execute transformation
        |                                           |                              |                              |    - JSLT: Apply expression
        |                                           |                              |                              |    - Router: Evaluate conditions
        |                                           |                              |                              |    - Pipeline: Execute steps
        |                                           |                              |                              |
        |                                           |                              | 9. Validate output against   |
        |                                           |                              |    consumer output schema    |
        |                                           |                              |                              |
        |                                           |                              | 10. Return transformed JSON  |
        |                                           |                              |<-----------------------------|
        |                                           |                              |                              |
        |                                           | 11. Return success response  |                              |
        |                                           |    {"transformedData": {...}}|                              |
        |<------------------------------------------|                              |                              |
```

#### Key Points About Current Flow

- **Schema Validation**: Input JSON is validated against canonical schemas, output against consumer schemas
- **Template Versioning**: Uses active template version for consumer-subject pairs (many templates per consumer)
- **Subject-Based Organization**: Templates are organized by consumer-subject combinations
- **Engine Selection**: Template metadata determines which transformation engine to use
- **Version Management**: Templates support multiple versions with activation/deactivation

#### Engine-Specific Flows

##### JSLT Engine Flow

```
Input JSON → Parse JSLT Expression → Apply Transformation → Output JSON
```

##### Router Engine Flow

```
Input JSON → Evaluate Route Conditions → Select Transformation → Apply Selected Transform → Output JSON
```

##### Pipeline Engine Flow

```
Input JSON → Step 1 Transform → Step 2 Transform → ... → Step N Transform → Output JSON
              ↓                     ↓                           ↓
        Continue on Error?   Continue on Error?         Continue on Error?
```

## Database Schema

### Tables

```sql
-- Schema storage with versioning and type support
CREATE TABLE schemas (
    id BIGSERIAL PRIMARY KEY,
    subject VARCHAR(255) NOT NULL,
    schema_type VARCHAR(50) NOT NULL DEFAULT 'canonical' CHECK (schema_type IN ('canonical', 'consumer_output')),
    consumer_id VARCHAR(255),  -- NULL for canonical schemas, required for consumer_output
    version VARCHAR(50) NOT NULL,  -- Semver format (e.g., "1.0.0")
    schema_json JSONB NOT NULL,
    compatibility VARCHAR(50) NOT NULL DEFAULT 'BACKWARD',
    description TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,

    -- Constraints
    FOREIGN KEY (consumer_id) REFERENCES consumers(consumer_id) ON DELETE CASCADE,
    UNIQUE(subject, schema_type, consumer_id, version),
    CHECK (schema_type = 'canonical' OR consumer_id IS NOT NULL)
);

-- Consumer applications
CREATE TABLE consumers (
    id BIGSERIAL PRIMARY KEY,
    consumer_id VARCHAR(255) UNIQUE NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

-- Note: Subjects are no longer stored in consumers.
-- Subject access is determined by registered transformation templates.

-- Transformation templates with versioning and schema relationships
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
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,

    -- Constraints
    FOREIGN KEY (consumer_id) REFERENCES consumers(consumer_id) ON DELETE CASCADE,
    FOREIGN KEY (input_schema_id) REFERENCES schemas(id) ON DELETE RESTRICT,
    FOREIGN KEY (output_schema_id) REFERENCES schemas(id) ON DELETE RESTRICT,
    UNIQUE(consumer_id, subject, version)
);
```

### Indexes

- `idx_schemas_subject_type` - Fast queries by subject and schema type
- `idx_schemas_consumer` - Optimized consumer-specific schema queries
- `idx_transformation_templates_consumer_subject` - Fast template lookups by consumer and subject
- `idx_transformation_templates_active` - Optimized active template retrieval
- `idx_transformation_templates_version` - Fast version-specific queries
- `unique_active_template` - Ensures only one active version per consumer-subject pair

### Subject Management

Subjects are not pre-declared in consumer registrations. Instead, consumers can register transformation templates for any subject, and the available subjects for a consumer are determined by the templates that have been registered.

## Transformation Engine Architecture

### Engine Interface

```java
public interface TransformationEngine {
    String getName();
    Map<String, Object> transform(Map<String, Object> inputJson, String expression) throws TransformationException;
    boolean validateExpression(String expression);
}
```

### Available Transformation Engines

#### 1. JSLT Engine

- **Implementation**: `JsltTransformationEngine`
- **Purpose**: Simple JSON-to-JSON transformations using JSLT expressions
- **Use Case**: Declarative transformations, data normalization, field mapping
- **Example Expression**: `{ "user_id": .id, "full_name": (.firstName + " " + .lastName) }`

#### 2. Router Engine

- **Implementation**: `RouterTransformationEngine`
- **Purpose**: Intelligent routing of transformations based on input data characteristics
- **Use Case**: Conditional processing, multi-tenant data handling, content-based routing
- **Configuration**: JSON-based routing rules with conditions and transformation mappings

#### 3. Pipeline Engine

- **Implementation**: `PipelineTransformationEngine`
- **Purpose**: Sequential execution of multiple transformations
- **Use Case**: Complex multi-step processing, data enrichment workflows, validation chains
- **Configuration**: Ordered list of transformation steps with error handling options

### Engine Architecture Details

#### Router Engine Architecture

```
Input JSON → Condition Evaluation → Route Selection → JSLT Transformation → Output JSON
```

**Key Components**:

- **ConditionEvaluator**: Evaluates JSON path expressions against input data
- **RouteSelector**: Selects appropriate transformation based on first matching condition
- **RouterTransformationEngine**: Main coordinator with fallback to default transformation

#### Pipeline Engine Architecture

```
Input JSON → Step 1 → Step 2 → ... → Step N → Output JSON
```

**Key Components**:

- **PipelineStep**: Individual transformation step with error handling configuration
- **PipelineExecutor**: Manages step execution and result accumulation
- **PipelineTransformationEngine**: Coordinator with configurable error propagation

### Configuration Validation

All transformation engines include configuration validation:

- **JSON Schema Validation**: Router and pipeline configurations are validated against JSON schemas
- **Structural Validation**: Ensures required fields and proper data types
- **Expression Validation**: Validates transformation expressions before execution
- **Error Reporting**: Detailed validation errors for debugging configuration issues

### Extensibility

The engine abstraction allows for easy addition of new transformation engines:

1. Implement the `TransformationEngine` interface
2. Add configuration validation if needed
3. Register the engine as a Spring bean
4. Add engine name to the service's available engines list
5. Update database schema if configuration storage is required

## Configuration Management

### Application Profiles

- **default**: Local development with H2 database
- **docker**: Production deployment with PostgreSQL

### Externalized Configuration

Key configuration properties:

```properties
# Database
spring.datasource.url=jdbc:postgresql://localhost:5432/schema_registry
spring.datasource.username=schema_user
spring.datasource.password=schema_password

# JPA
spring.jpa.hibernate.ddl-auto=validate
spring.jpa.show-sql=false

# OpenAPI
springdoc.swagger-ui.path=/swagger-ui.html
```

## Security Considerations

### Current State

- No authentication/authorization implemented
- All endpoints are publicly accessible
- Suitable for internal networks or development environments

### Future Enhancements

- OAuth 2.0 / JWT authentication
- Role-based access control (RBAC)
- API rate limiting
- Audit logging

## Monitoring and Observability

### Spring Boot Actuator

Endpoints available at `/actuator/*`:

- `/actuator/health` - Application health status
- `/actuator/info` - Application information
- `/actuator/metrics` - Application metrics
- `/actuator/loggers` - Logging configuration

### Logging

- Structured logging with SLF4J
- Configurable log levels per package
- SQL query logging for debugging

## Deployment Architecture

### Docker Containerization

- Multi-stage Dockerfile for optimized image size
- Non-root user execution
- Health checks for container orchestration

### Docker Compose

Services:

- **app**: Spring Boot application
- **db**: PostgreSQL database with persistent volumes

### Production Considerations

- Database connection pooling
- Horizontal scaling capabilities
- Load balancer configuration
- Backup and recovery strategies

## Performance Characteristics

### Database Performance

- Indexed queries for fast schema retrieval
- JSONB storage for flexible schema storage
- Connection pooling with HikariCP

### Caching Opportunities

- Schema caching for frequently accessed schemas
- Template compilation caching
- Consumer metadata caching

### Scalability

- Stateless application design
- Database read replicas for read-heavy workloads
- CDN for static API documentation

## Error Handling

### Global Exception Handler

- Consistent error response format
- Appropriate HTTP status codes
- Detailed error messages for debugging

### Custom Exceptions

- `ResourceNotFoundException` - 404 responses
- `SchemaValidationException` - 400 responses for invalid schemas
- `TransformationException` - 500 responses for transformation errors

## Testing Strategy

### Unit Tests

- Service layer testing with mocked repositories
- Transformation engine testing
- Utility class testing

### Integration Tests

- Controller testing with TestContainers
- Database integration testing
- End-to-end API testing

### Test Coverage

- Target: >80% code coverage
- Focus on business logic and edge cases
