# Troubleshooting Guide

This guide helps you diagnose and resolve common issues with the JSON Schema Registry and Transformation Service.

## Quick Health Check

Before diving into specific issues, run these basic checks:

```bash
# Check if services are running
docker-compose ps

# Check application health
curl -f http://localhost:8080/actuator/health

# Check database connectivity
curl -f http://localhost:8080/actuator/health/db

# View recent logs
docker-compose logs --tail=50 app
```

## Startup Issues

### Application Won't Start

**Symptoms**: Container exits immediately or application fails to start.

**Possible Causes & Solutions**:

1. **Database not ready**:
   ```bash
   # Check database health
   docker-compose exec db pg_isready -U schema_user -d schema_registry

   # Wait for database to be ready
   docker-compose restart app
   ```

2. **Port conflict**:
   ```bash
   # Find what's using port 8080
   lsof -i :8080
   netstat -tulpn | grep :8080

   # Change port in docker-compose.yml
   # ports: - "8081:8080"
   ```

3. **Java version issues**:
   ```bash
   # Check Java version
   java -version

   # Should be Java 17 or higher
   ```

4. **Memory issues**:
   ```bash
   # Check container logs
   docker-compose logs app

   # Increase memory if needed
   # environment: - JAVA_OPTS=-Xmx1g
   ```

### Database Connection Issues

**Symptoms**: Application logs show connection refused or timeout errors.

**Solutions**:

1. **Database not started**:
   ```bash
   # Start database
   docker-compose up -d db

   # Wait for health check
   sleep 30
   ```

2. **Wrong connection string**:
   ```bash
   # Check environment variables
   docker-compose exec app env | grep SPRING_DATASOURCE

   # Verify docker-compose.yml configuration
   ```

3. **Network issues**:
   ```bash
   # Test network connectivity
   docker-compose exec app ping db

   # Check network
   docker network ls
   ```

## Runtime Issues

### API Returns 500 Errors

**Symptoms**: HTTP 500 Internal Server Error responses.

**Debug Steps**:

1. **Check application logs**:
   ```bash
   docker-compose logs -f app
   ```

2. **Check database connectivity**:
   ```bash
   docker-compose exec db psql -U schema_user -d schema_registry -c "SELECT 1"
   ```

3. **Verify request format**:
   ```bash
   # Test with minimal payload
   curl -X POST http://localhost:8080/api/consumers \
     -H "Content-Type: application/json" \
     -d '{"consumerId": "test", "name": "Test"}'
   ```

### Schema Validation Errors

**Symptoms**: Schema registration fails with validation errors.

**Common Issues**:

1. **Invalid JSON Schema**:
   ```json
   // Wrong - missing type
   {"properties": {"name": {"type": "string"}}}

   // Correct
   {"type": "object", "properties": {"name": {"type": "string"}}}
   ```

2. **Compatibility issues**:
   ```bash
   # Check existing schema
   curl http://localhost:8080/api/schemas/user-profile/latest

   # Test compatibility
   curl -X POST http://localhost:8080/api/schemas/user-profile/compat \
     -H "Content-Type: application/json" \
     -d '{"schema": {...}}'
   ```

### Transformation Errors

**Symptoms**: Data transformation fails or returns unexpected results.

**Debug Steps**:

1. **Validate template syntax**:
   ```bash
   # Get current template
   curl http://localhost:8080/api/transform/templates/mobile-app

   # Test with simple data
   curl -X POST http://localhost:8080/api/transform/mobile-app \
     -H "Content-Type: application/json" \
     -d '{"canonicalJson": {"test": "value"}}'
   ```

2. **Check JSLT syntax**:
   ```jslt
   // Correct JSLT
   . | {id: .userId, name: .fullName}

   // Wrong - missing dot
   {id: userId, name: fullName}
   ```

3. **Template not found**:
   ```bash
   # Ensure consumer exists
   curl http://localhost:8080/api/consumers/mobile-app

   # Create template if missing
   curl -X POST http://localhost:8080/api/transform/templates/mobile-app \
     -H "Content-Type: application/json" \
     -d '{"template": ".", "engine": "JSLT"}'
   ```

## Database Issues

### Migration Failures

**Symptoms**: Flyway migration errors during startup.

**Solutions**:

1. **Check migration status**:
   ```bash
   docker-compose exec app ./mvnw flyway:info
   ```

2. **Repair migrations**:
   ```bash
   # Clean and retry (WARNING: destroys data)
   docker-compose exec app ./mvnw flyway:clean
   docker-compose exec app ./mvnw flyway:migrate
   ```

3. **Manual migration**:
   ```bash
   # Access database
   docker-compose exec db psql -U schema_user -d schema_registry

   # Check tables
   \dt
   ```

### Data Corruption

**Symptoms**: Inconsistent data or foreign key violations.

**Recovery**:

```bash
# Backup current data (if valuable)
docker-compose exec db pg_dump -U schema_user schema_registry > backup.sql

# Clean and restart
make clean
make start
```

## Docker Issues

### Container Won't Start

**Symptoms**: Container fails to start or exits immediately.

**Debug**:

```bash
# Check container status
docker-compose ps

# View detailed logs
docker-compose logs app

# Try starting manually
docker run --rm schema-registry:latest
```

### Image Build Failures

**Symptoms**: Docker build fails.

**Solutions**:

1. **Check Dockerfile**:
   ```bash
   # Validate syntax
   docker build --no-cache -t test-build .
   ```

2. **Clean build**:
   ```bash
   # Remove old images
   docker system prune -f

   # Rebuild
   docker-compose build --no-cache
   ```

### Volume Issues

**Symptoms**: Data not persisting between container restarts.

**Check**:

```bash
# List volumes
docker volume ls

# Inspect volume
docker volume inspect schema_registry_postgres_data

# Check disk space
df -h
```

## Performance Issues

### Slow Response Times

**Symptoms**: API calls take longer than expected.

**Investigation**:

1. **Check system resources**:
   ```bash
   # Container stats
   docker stats

   # Database performance
   docker-compose exec db psql -U schema_user -d schema_registry -c "SELECT * FROM pg_stat_activity"
   ```

2. **Database query performance**:
   ```bash
   # Enable query logging
   # logging.level.org.hibernate.SQL=DEBUG
   # logging.level.org.hibernate.type.descriptor.sql.BasicBinder=TRACE
   ```

3. **Connection pool issues**:
   ```bash
   # Check HikariCP metrics
   curl http://localhost:8080/actuator/metrics/hikaricp.connections.active
   ```

### Memory Issues

**Symptoms**: OutOfMemoryError or container restarts.

**Solutions**:

1. **Increase memory limits**:
   ```yaml
   # docker-compose.yml
   services:
     app:
       deploy:
         resources:
           limits:
             memory: 1G
           reservations:
             memory: 512M
   ```

2. **JVM tuning**:
   ```bash
   # Set JVM options
   environment:
     - JAVA_OPTS=-Xmx512m -Xms256m -XX:+UseG1GC
   ```

## Networking Issues

### Cannot Access API

**Symptoms**: Cannot reach http://localhost:8080

**Checks**:

1. **Port mapping**:
   ```bash
   # Check port binding
   docker-compose ps
   netstat -tulpn | grep 8080
   ```

2. **Firewall**:
   ```bash
   # Check firewall rules
   sudo ufw status
   sudo iptables -L
   ```

3. **Network mode**:
   ```bash
   # Try host network
   docker-compose.yml:
     services:
       app:
         network_mode: host
   ```

### Service Discovery Issues

**Symptoms**: Services cannot communicate with each other.

**Debug**:

```bash
# Test service connectivity
docker-compose exec app ping db

# Check DNS resolution
docker-compose exec app nslookup db

# Verify network
docker network inspect schema_registry_schema-registry-network
```

## Logging and Monitoring

### Enable Debug Logging

```properties
# application.properties
logging.level.ru.vegorov.schemaregistry=DEBUG
logging.level.org.springframework.web=DEBUG
logging.level.org.hibernate.SQL=DEBUG
logging.level.org.hibernate.type.descriptor.sql.BasicBinder=TRACE
```

### Access Logs

```bash
# Application logs
docker-compose logs -f app

# Database logs
docker-compose logs -f db

# System logs
docker-compose exec app tail -f /app/logs/application.log
```

### Health Endpoints

```bash
# Overall health
curl http://localhost:8080/actuator/health

# Database health
curl http://localhost:8080/actuator/health/db

# Metrics
curl http://localhost:8080/actuator/metrics

# Info
curl http://localhost:8080/actuator/info
```

## Development Issues

### IDE Problems

**IntelliJ IDEA**:

1. **Maven import issues**:
   - File → Invalidate Caches / Restart
   - Reimport Maven project

2. **JDK configuration**:
   - File → Project Structure → Project SDK

**VS Code**:

1. **Extension issues**:
   - Reload window: Ctrl+Shift+P → "Developer: Reload Window"
   - Reinstall Java extensions

### Test Failures

**Common test issues**:

1. **Database not available**:
   ```bash
   # Use TestContainers properly
   # Check @TestContainers and @Container annotations
   ```

2. **Port conflicts in tests**:
   ```bash
   # Use random ports
   @SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
   ```

## Common Error Messages

### "Connection refused"

**Cause**: Database not running or wrong connection string.

**Solution**: Check database status and connection configuration.

### "Schema not compatible"

**Cause**: New schema breaks compatibility rules.

**Solution**: Review compatibility types and adjust schema or compatibility level.

### "Template not found"

**Cause**: Transformation template not created for consumer.

**Solution**: Create template using POST /api/transform/templates/{consumerId}

### "Validation failed"

**Cause**: Request doesn't meet validation requirements.

**Solution**: Check required fields and data types in API documentation.

## Getting Help

If you can't resolve an issue:

1. **Check the logs**: `docker-compose logs -f`
2. **Review configuration**: Compare with working examples
3. **Test with minimal setup**: Use the getting started guide
4. **Check GitHub issues**: Look for similar problems
5. **Create an issue**: Provide logs, configuration, and steps to reproduce

## Emergency Recovery

For critical issues where the service is completely down:

```bash
# Stop everything
docker-compose down -v

# Clean up
docker system prune -f

# Restart fresh
make start

# Verify
make health
```

This will give you a completely clean environment to work with.