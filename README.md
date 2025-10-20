# JSON Schema Registry and Transformation Service

A comprehensive Spring Boot application for managing JSON schemas and transforming data for different consumer applications. This service provides schema versioning, compatibility checking, and JSON transformation capabilities using JSLT (JSON Schema Language for Transformations).

## Features

- **Dual Schema Management**: Separate canonical schemas (source of truth) and consumer-specific output schemas
- **Schema Versioning**: Full semantic versioning support with compatibility checking for both schema types
- **Advanced JSON Transformation**: Transform data using multiple engines - JSLT, Router, and Pipeline
- **Router Engine**: Intelligent routing based on data characteristics and conditional logic
- **Pipeline Engine**: Sequential multi-step transformations with error handling and validation
- **Template Versioning**: Version-controlled transformation templates with activation/deactivation
- **Multi-Consumer Support**: Handle different data format requirements for various applications (mobile, web, analytics)
- **JSON Schema Validation**: Validate data against both canonical and consumer output schemas
- **Multi-Version JSON Schema Support**: Support for draft-04, draft-06, draft-07, draft-2019-09, and draft-2020-12 specifications
- **RESTful API**: Complete REST API with OpenAPI 3.0 documentation and Swagger UI
- **Database Persistence**: PostgreSQL with Flyway migrations for reliable schema management
- **Docker Support**: Containerized deployment with Docker Compose
- **Extensible Design**: Pluggable transformation engines with comprehensive error handling

## JSON Schema Version Support

This service supports multiple versions of the JSON Schema specification:

- **Draft-04** (default when no `$schema` field is specified)
- **Draft-06**
- **Draft-07**
- **Draft-2019-09**
- **Draft-2020-12**

The service automatically detects the schema version from the `$schema` field in the JSON schema and uses the appropriate validation engine. If no `$schema` field is present, it defaults to draft-04 for backward compatibility.

### Supported Schema URIs

- `http://json-schema.org/draft-04/schema#`
- `http://json-schema.org/draft-06/schema#`
- `http://json-schema.org/draft-07/schema#`
- `https://json-schema.org/draft/2019-09/schema`
- `https://json-schema.org/draft/2020-12/schema`

## Technology Stack

- **Java 17** - Programming language
- **Spring Boot 3.1.4** - Application framework
- **PostgreSQL 15** - Database
- **JSLT 0.1.14** - JSON transformation engine
- **Spring Data JPA** - Data access layer
- **Flyway** - Database migrations
- **SpringDoc OpenAPI** - API documentation
- **Docker** - Containerization

## Quick Start

### Prerequisites

- Java 17 or higher
- Maven 3.6+
- Docker and Docker Compose (for containerized deployment)

### Local Development Setup

1. **Clone the repository**
   ```bash
   git clone <repository-url>
   cd simple-schema-registry
   ```

2. **Start PostgreSQL database**
   ```bash
   docker run -d --name postgres-schema-registry \
     -e POSTGRES_DB=schema_registry \
     -e POSTGRES_USER=schema_user \
     -e POSTGRES_PASSWORD=schema_password \
     -p 5432:5432 postgres:15-alpine
   ```

3. **Run the application**
   ```bash
   mvn spring-boot:run
   ```

4. **Access the application**
   - API: http://localhost:8080
   - Swagger UI: http://localhost:8080/swagger-ui.html
   - API Docs: http://localhost:8080/api-docs

### Docker Deployment

```bash
# Build and start all services
docker-compose up -d

# View logs
docker-compose logs -f app

# Stop services
docker-compose down
```

## API Overview

The service provides four main API groups with a clear separation between canonical schemas and consumer-specific schemas:

### Schema Registry API

#### Canonical Schemas (`/api/schemas`)
- `POST /api/schemas/{subject}` - Register a new canonical schema or create a new version
- `GET /api/schemas/{subject}/versions` - Get all versions of a canonical schema
- `GET /api/schemas/{subject}/versions/{version}` - Get specific canonical schema version
- `GET /api/schemas/{subject}` - Get latest canonical schema version
- `POST /api/schemas/{subject}/compat` - Check canonical schema compatibility
- `POST /api/schemas/{subject}/validate` - Validate JSON against canonical schema
- `GET /api/schemas/subjects` - List all canonical schema subjects

#### Consumer Output Schemas (`/api/consumers/{consumerId}/schemas`)
- `POST /api/consumers/{consumerId}/schemas/{subject}` - Register a consumer output schema
- `GET /api/consumers/{consumerId}/schemas/{subject}/versions` - Get all versions of a consumer output schema
- `GET /api/consumers/{consumerId}/schemas/{subject}/versions/{version}` - Get specific consumer output schema version
- `GET /api/consumers/{consumerId}/schemas/{subject}` - Get latest consumer output schema version
- `POST /api/consumers/{consumerId}/schemas/{subject}/compat` - Check consumer output schema compatibility
- `POST /api/consumers/{consumerId}/schemas/{subject}/validate` - Validate JSON against consumer output schema
- `GET /api/consumers/{consumerId}/schemas/subjects` - List subjects for a consumer

### Consumer Management API (`/api/consumers`)
- `POST /api/consumers` - Register a new consumer
- `GET /api/consumers` - List all consumers
- `GET /api/consumers/{consumerId}` - Get consumer details

### Transformation API (`/api/consumers/{consumerId}/subjects/{subject}`)

#### Data Transformation
- `POST /api/consumers/{consumerId}/subjects/{subject}/transform` - Transform JSON data using active template
- `POST /api/consumers/{consumerId}/subjects/{subject}/transform/versions/{version}` - Transform with specific template version

#### Template Management
- `POST /api/consumers/{consumerId}/subjects/{subject}/templates` - Create new transformation template version
- `GET /api/consumers/{consumerId}/subjects/{subject}/templates` - Get all template versions
- `GET /api/consumers/{consumerId}/subjects/{subject}/templates/active` - Get active template
- `GET /api/consumers/{consumerId}/subjects/{subject}/templates/versions/{version}` - Get specific template version

#### Version Management
- `PUT /api/consumers/{consumerId}/subjects/{subject}/templates/versions/{version}/activate` - Activate template version
- `PUT /api/consumers/{consumerId}/subjects/{subject}/templates/versions/{version}/deactivate` - Deactivate template version
- `DELETE /api/consumers/{consumerId}/subjects/{subject}/templates/versions/{version}` - Delete template version

#### Utilities
- `GET /api/consumers/engines` - List available transformation engines

## Configuration

The application uses Spring profiles for different environments:

- **default**: Local development with H2 database
- **docker**: Production deployment with PostgreSQL

Key configuration properties in `application.properties`:

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

## Development

### Build Commands

```bash
# Compile
mvn clean compile

# Run tests
mvn test

# Build JAR
mvn clean package

# Run locally
mvn spring-boot:run
```

### Database Diagnostics

The project includes comprehensive database diagnostic tools via Makefile targets for monitoring, troubleshooting, and maintaining the PostgreSQL database. These tools help with performance monitoring, data analysis, and maintenance operations.

#### Basic Diagnostics
```bash
# Database status and connection info
make db-status

# Table sizes and operation counts
make db-tables

# Database and table size breakdown
make db-size
```

#### Schema Registry Diagnostics
```bash
# Schema counts by subject with version stats
make db-schemas-count

# Recently created schemas
make db-schemas-recent

# Schema subjects overview
make db-schemas-subjects

# Version distribution across subjects
make db-schemas-versions
```

#### Consumer & Transformation Diagnostics
```bash
# List consumers with template status
make db-consumers-list

# Recently active consumers
make db-consumers-active

# Consumer registration statistics
make db-consumers-stats

# List transformation templates
make db-templates-list

# Template engine distribution
make db-templates-engines

# Recently updated templates
make db-templates-recent
```

#### Performance & Health Monitoring
```bash
# Table indexes with usage statistics
make db-indexes

# Check for active database locks
make db-locks

# Active database connections
make db-connections
```

#### Maintenance Operations
```bash
# Run VACUUM ANALYZE for optimization
make db-vacuum-analyze

# Reindex all tables (use with caution)
make db-reindex

# Remove test data (subjects/consumers starting with 'test-')
make db-cleanup-test-data
```

#### Advanced Diagnostics
```bash
# Analyze schema evolution patterns
make db-schema-evolution

# Compatibility settings distribution
make db-compatibility-checks

# Find orphaned records
make db-orphaned-data

# Export current schema structure
make db-export-schema

# Create database backup
make db-backup

# Show current database activity
make db-activity
```

#### Usage Examples

**Monitor Database Health:**
```bash
# Quick health check
make db-status

# Check for performance issues
make db-indexes
make db-locks
```

**Analyze Schema Usage:**
```bash
# See most active subjects
make db-schemas-count

# Check compatibility distribution
make db-compatibility-checks
```

**Consumer Analytics:**
```bash
# Consumer adoption statistics
make db-consumers-stats

# Template usage overview
make db-templates-engines
```

**Maintenance Tasks:**
```bash
# Regular optimization
make db-vacuum-analyze

# Clean up test data after development
make db-cleanup-test-data
```

### Project Structure

```
src/
├── main/
│   ├── java/ru/vegorov/schemaregistry/
│   │   ├── config/          # Configuration classes
│   │   ├── controller/      # REST controllers
│   │   ├── dto/            # Data transfer objects
│   │   ├── entity/         # JPA entities
│   │   ├── exception/      # Custom exceptions
│   │   ├── repository/     # Data repositories
│   │   └── service/        # Business logic services
│   └── resources/
│       ├── db/migration/   # Flyway migrations
│       └── application.properties
└── test/                   # Test classes
```

## Documentation

- [Getting Started](docs/getting-started.md) - Quick start guide with examples
- [End-to-End Example](docs/end-to-end-example.md) - Complete workflow demonstration with investment publications
- [Testing Guide](docs/testing.md) - Comprehensive testing strategy and execution order
- [Advanced Transformations](docs/advanced-transformations.md) - Router and Pipeline engines implementation guide
- [API Reference](docs/api-reference.md) - Complete API documentation with examples
- [Architecture](docs/architecture.md) - System design and components
- [Logging Architecture](docs/logging-architecture.md) - Comprehensive logging system design and implementation guide
- [Exception Handling Guide](docs/exception-handling-guide.md) - Exception handling patterns and GlobalExceptionHandler implementation
- [Transformation Extension Plan](docs/transformation-extension-plan.md) - Router and Pipeline engines design
- [Schema Evolution Workflow](docs/schema-evolution-workflow.md) - Schema evolution with examples
- [Deployment](docs/deployment.md) - Production deployment instructions
- [Troubleshooting](docs/troubleshooting.md) - Common issues and solutions

## Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## License

This project is licensed under the MIT License - see the LICENSE file for details.