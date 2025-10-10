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

### 3. Create a Transformation Template

Create a transformation template for the mobile app:

```bash
curl -X POST http://localhost:8080/api/transform/templates/mobile-app \
  -H "Content-Type: application/json" \
  -d '{
    "template": ". | {id: .id, name: .name, email: .email}",
    "engine": "JSLT"
  }'
```

### 4. Transform Data

Transform some canonical JSON data:

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

## Next Steps

Now that you have the service running, you can:

1. **Explore the API**: Use Swagger UI to test all endpoints
2. **Add More Schemas**: Register schemas for different data types
3. **Create Consumers**: Add more consumer applications
4. **Build Transformations**: Create complex JSLT templates
5. **Write Tests**: Add unit and integration tests
6. **Customize Configuration**: Modify settings for your environment

## Getting Help

- **API Documentation**: http://localhost:8080/swagger-ui.html
- **Health Checks**: http://localhost:8080/actuator/health
- **Logs**: `make logs`
- **Issues**: Check the troubleshooting guide

For more detailed information, see:
- [API Reference](api-reference.md)
- [Architecture](architecture.md)
- [Deployment](deployment.md)
- [Troubleshooting](troubleshooting.md)