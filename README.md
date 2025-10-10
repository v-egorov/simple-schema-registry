# JSON Schema Registry and Transformation Service

A comprehensive Spring Boot application for managing JSON schemas and transforming data for different consumer applications. This service provides schema versioning, compatibility checking, and JSON transformation capabilities using JSLT (JSON Schema Language for Transformations).

## Features

- **Schema Registry**: Store, version, and manage JSON schemas with automatic compatibility checking
- **JSON Transformation**: Transform canonical JSON data into consumer-specific formats using JSLT templates
- **Multi-Consumer Support**: Handle different data format requirements for various applications (mobile, web, analytics)
- **RESTful API**: Complete REST API with OpenAPI 3.0 documentation and Swagger UI
- **Database Persistence**: PostgreSQL with Flyway migrations for reliable schema management
- **Docker Support**: Containerized deployment with Docker Compose
- **Extensible Design**: Pluggable transformation engines (currently supports JSLT)

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

The service provides three main API groups:

### Schema Registry API (`/api/schemas`)
- `POST /api/schemas` - Register a new schema or create a new version
- `GET /api/schemas/{subject}` - Get all versions of a schema
- `GET /api/schemas/{subject}/{version}` - Get specific schema version
- `GET /api/schemas/{subject}/latest` - Get latest schema version
- `POST /api/schemas/{subject}/compat` - Check schema compatibility
- `GET /api/schemas/subjects` - List all schema subjects

### Consumer Management API (`/api/consumers`)
- `POST /api/consumers` - Register a new consumer
- `GET /api/consumers` - List all consumers
- `GET /api/consumers/{consumerId}` - Get consumer details

### Transformation API (`/api/transform`)
- `POST /api/transform/{consumerId}` - Transform JSON data for a consumer
- `GET /api/transform/templates/{consumerId}` - Get transformation template
- `POST /api/transform/templates/{consumerId}` - Create/update transformation template
- `GET /api/transform/engines` - List available transformation engines

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

- [API Reference](docs/api-reference.md) - Detailed API documentation with examples
- [Architecture](docs/architecture.md) - System design and components
- [Getting Started](docs/getting-started.md) - Development setup guide
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