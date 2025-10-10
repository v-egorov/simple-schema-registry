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

- **SchemaRegistryService**: Handles schema versioning, compatibility validation, and retrieval
- **TransformationService**: Manages transformation templates and executes transformations
- **ConsumerService**: Manages consumer application registrations
- **Transformation Engines**: Pluggable engines for different transformation languages

**Key Features**:
- Transaction management with Spring's `@Transactional`
- Business rule validation
- Engine abstraction for extensibility

### 3. Data Access Layer (Repositories)

Spring Data JPA repositories provide data access:

- **SchemaRepository**: CRUD operations for schema entities
- **ConsumerRepository**: CRUD operations for consumer entities
- **TransformationTemplateRepository**: CRUD operations for transformation templates

### 4. Data Model (Entities)

JPA entities representing the domain model:

- **SchemaEntity**: Stores schema definitions with versioning
- **ConsumerEntity**: Represents consumer applications
- **TransformationTemplateEntity**: Stores transformation logic per consumer

## Data Flow

### Schema Registration Flow

1. Client sends schema registration request
2. Controller validates request data
3. Service checks compatibility with existing versions
4. Repository saves new schema version
5. Response contains schema metadata

### Data Transformation Flow

1. Client sends transformation request with canonical JSON
2. Controller validates request
3. Service retrieves consumer's transformation template
4. Appropriate transformation engine processes the data
5. Transformed JSON is returned to client

## Database Schema

### Tables

```sql
-- Schema storage with versioning
CREATE TABLE schemas (
    id BIGSERIAL PRIMARY KEY,
    subject VARCHAR(255) NOT NULL,
    version INTEGER NOT NULL,
    schema_json JSONB NOT NULL,
    compatibility VARCHAR(50) NOT NULL DEFAULT 'BACKWARD',
    description TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    UNIQUE(subject, version)
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

-- Transformation templates per consumer
CREATE TABLE transformation_templates (
    id BIGSERIAL PRIMARY KEY,
    consumer_id VARCHAR(255) NOT NULL,
    engine VARCHAR(50) NOT NULL DEFAULT 'jslt',
    template_expression TEXT,  -- Nullable for router/pipeline engines
    configuration TEXT,        -- JSON configuration for router/pipeline engines
    description TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    FOREIGN KEY (consumer_id) REFERENCES consumers(consumer_id) ON DELETE CASCADE,
    UNIQUE(consumer_id)
);
```

### Indexes

- `idx_schemas_subject` - Fast subject-based queries
- `idx_schemas_subject_version` - Optimized version retrieval
- `idx_transformation_templates_consumer_id` - Fast template lookups

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