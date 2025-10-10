# Deployment Guide

This guide covers deployment options for the JSON Schema Registry and Transformation Service, including Docker containerization, local development setup, and production considerations.

## Prerequisites

- **Java 17** or higher (for local development)
- **Maven 3.6+** (for building)
- **Docker and Docker Compose** (for containerized deployment)
- **PostgreSQL 15+** (when running outside Docker)

## Quick Start with Docker

The fastest way to get started is using Docker Compose:

```bash
# Clone the repository
git clone <repository-url>
cd simple-schema-registry

# Start all services
make start

# Or manually:
docker-compose up -d

# Check service health
curl http://localhost:8080/actuator/health

# Access API documentation
open http://localhost:8080/swagger-ui.html
```

## Development Setup

### Local PostgreSQL

For local development with your own PostgreSQL instance:

```bash
# Start PostgreSQL (using Docker for convenience)
docker run -d --name postgres-dev \
  -e POSTGRES_DB=schema_registry \
  -e POSTGRES_USER=schema_user \
  -e POSTGRES_PASSWORD=schema_password \
  -p 5432:5432 postgres:15-alpine

# Build and run the application
mvn clean spring-boot:run
```

### Using IDE

1. Import the project as a Maven project
2. Ensure Java 17 is configured
3. Set the active profile to `default` (or create a custom profile)
4. Run the main class: `SchemaRegistryApplication`

## Docker Deployment

### Docker Compose (Recommended)

The `docker-compose.yml` file provides a complete deployment stack:

```yaml
version: '3.8'

services:
  app:
    build: .
    ports:
      - "8080:8080"
    environment:
      - SPRING_PROFILES_ACTIVE=docker
      - SPRING_DATASOURCE_URL=jdbc:postgresql://db:5432/schema_registry
    depends_on:
      db:
        condition: service_healthy
    networks:
      - schema-registry-network

  db:
    image: postgres:15-alpine
    environment:
      - POSTGRES_DB=schema_registry
      - POSTGRES_USER=schema_user
      - POSTGRES_PASSWORD=schema_password
    volumes:
      - postgres_data:/var/lib/postgresql/data
      - ./src/main/resources/db/migration:/docker-entrypoint-initdb.d
    networks:
      - schema-registry-network
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U schema_user -d schema_registry"]
      interval: 10s
      timeout: 5s
      retries: 5
```

### Building Custom Docker Image

```bash
# Build the image
docker build -t schema-registry:latest .

# Run with external database
docker run -d \
  --name schema-registry \
  -p 8080:8080 \
  -e SPRING_DATASOURCE_URL=jdbc:postgresql://host:5432/schema_registry \
  -e SPRING_DATASOURCE_USERNAME=schema_user \
  -e SPRING_DATASOURCE_PASSWORD=schema_password \
  schema-registry:latest
```

### Multi-stage Dockerfile

The Dockerfile uses multi-stage builds for optimization:

```dockerfile
# Build stage
FROM eclipse-temurin:17-jdk-alpine AS builder
# ... build application

# Runtime stage
FROM eclipse-temurin:17-jre-alpine
# ... run application with minimal footprint
```

## Configuration

### Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `SPRING_PROFILES_ACTIVE` | `default` | Active Spring profile |
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://localhost:5432/schema_registry` | Database URL |
| `SPRING_DATASOURCE_USERNAME` | `schema_user` | Database username |
| `SPRING_DATASOURCE_PASSWORD` | `schema_password` | Database password |
| `SPRING_JPA_HIBERNATE_DDL_AUTO` | `validate` | Hibernate DDL mode |
| `SERVER_PORT` | `8080` | Server port |
| `LOGGING_LEVEL_RU_VEGOROV` | `INFO` | Application log level |

### Application Properties

Key configuration files:

- `application.properties` - Default configuration
- `application-docker.properties` - Docker-specific settings

### Database Configuration

The application uses Flyway for database migrations. Migrations are located in `src/main/resources/db/migration/`.

To run migrations manually:
```bash
mvn flyway:migrate
```

## Production Deployment

### Security Considerations

1. **Change default credentials**:
   ```bash
   # Use strong passwords for database
   export SPRING_DATASOURCE_PASSWORD="your-secure-password"
   ```

2. **Enable SSL/TLS**:
   ```properties
   server.ssl.key-store=classpath:keystore.p12
   server.ssl.key-store-password=your-password
   server.ssl.keyStoreType=PKCS12
   ```

3. **Network security**:
   - Run behind a reverse proxy (nginx, traefik)
   - Configure firewall rules
   - Use internal networks for database communication

### Scaling Considerations

1. **Horizontal scaling**:
   - Stateless application design supports multiple instances
   - Use load balancer for distribution
   - Session storage if authentication is added

2. **Database scaling**:
   - Connection pooling configured via HikariCP
   - Read replicas for read-heavy workloads
   - Database indexes optimized for query patterns

### Monitoring

1. **Health checks**:
   ```bash
   # Application health
   curl http://localhost:8080/actuator/health

   # Database connectivity
   curl http://localhost:8080/actuator/health/db
   ```

2. **Metrics**:
   - Spring Boot Actuator provides metrics endpoint
   - JVM metrics, HTTP request metrics, database connection pools

3. **Logging**:
   - Structured JSON logging
   - Configurable log levels
   - Log aggregation with ELK stack or similar

### Backup and Recovery

1. **Database backups**:
   ```bash
   # Using Docker volumes
   docker run --rm -v schema_registry_postgres_data:/data \
     -v $(pwd):/backup alpine tar czf /backup/backup.tar.gz -C /data .
   ```

2. **Application configuration**:
   - Store configuration in version control
   - Use environment-specific property files
   - Document all configuration changes

## Makefile Commands

The project includes a comprehensive Makefile for common operations:

```bash
# Development
make build          # Build JAR
make test           # Run tests
make run            # Run locally

# Docker operations
make up             # Start services
make down           # Stop services
make logs           # View logs
make restart        # Restart services

# Database
make db-migrate     # Run migrations
make db-info        # Migration status
make db-clean       # Clean database

# Utilities
make clean          # Clean artifacts
make health         # Health check
make docs           # Show documentation URLs
```

## Troubleshooting

### Common Issues

1. **Port conflicts**:
   ```bash
   # Check what's using port 8080
   lsof -i :8080

   # Change port in docker-compose.yml or application.properties
   ```

2. **Database connection issues**:
   ```bash
   # Test database connectivity
   docker-compose exec db pg_isready -U schema_user -d schema_registry

   # Check database logs
   make logs-db
   ```

3. **Application startup failures**:
   ```bash
   # Check application logs
   make logs-app

   # Verify Java version
   java -version
   ```

### Logs and Debugging

```bash
# View all logs
docker-compose logs -f

# View application logs only
docker-compose logs -f app

# Access container shell
docker-compose exec app sh

# Check database
docker-compose exec db psql -U schema_user -d schema_registry
```

## Cloud Deployment

### AWS

1. **ECS Fargate**:
   ```bash
   # Build and push image to ECR
   aws ecr get-login-password --region us-east-1 | docker login --username AWS --password-stdin <account>.dkr.ecr.us-east-1.amazonaws.com
   docker build -t schema-registry .
   docker tag schema-registry:latest <account>.dkr.ecr.us-east-1.amazonaws.com/schema-registry:latest
   docker push <account>.dkr.ecr.us-east-1.amazonaws.com/schema-registry:latest
   ```

2. **RDS PostgreSQL**:
   - Use Amazon RDS for managed PostgreSQL
   - Configure security groups for ECS to RDS communication

### Kubernetes

Sample deployment manifests:

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: schema-registry
spec:
  replicas: 3
  selector:
    matchLabels:
      app: schema-registry
  template:
    metadata:
      labels:
        app: schema-registry
    spec:
      containers:
      - name: schema-registry
        image: schema-registry:latest
        ports:
        - containerPort: 8080
        env:
        - name: SPRING_DATASOURCE_URL
          value: "jdbc:postgresql://postgres:5432/schema_registry"
        - name: SPRING_DATASOURCE_USERNAME
          valueFrom:
            secretKeyRef:
              name: db-secret
              key: username
        - name: SPRING_DATASOURCE_PASSWORD
          valueFrom:
            secretKeyRef:
              name: db-secret
              key: password
```

## Performance Tuning

### JVM Settings

```bash
# Production JVM settings
java -Xmx2g -Xms512m -XX:+UseG1GC -XX:MaxGCPauseMillis=200 \
  -jar app.jar
```

### Database Tuning

```sql
-- Connection pool settings (in application.properties)
spring.datasource.hikari.maximum-pool-size=20
spring.datasource.hikari.minimum-idle=5
spring.datasource.hikari.connection-timeout=30000
```

### Caching

Consider implementing caching for:
- Frequently accessed schemas
- Transformation templates
- Consumer metadata

## Maintenance

### Updates

1. **Application updates**:
   ```bash
   # Pull latest changes
   git pull

   # Rebuild and restart
   make up-build
   ```

2. **Database migrations**:
   ```bash
   # Check migration status
   make db-info

   # Apply new migrations
   make db-migrate
   ```

### Monitoring

Set up monitoring for:
- Application health and metrics
- Database performance
- Container resource usage
- Error rates and response times