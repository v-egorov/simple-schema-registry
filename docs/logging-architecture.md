# Logging Architecture and Implementation Guide

## Overview

This document describes the comprehensive logging architecture implemented in the Simple Schema Registry application. The logging system provides structured, configurable, and performance-aware logging capabilities for debugging, monitoring, and troubleshooting.

## Table of Contents

1. [Architecture Overview](#architecture-overview)
2. [Logging Principles](#logging-principles)
3. [Configuration](#configuration)
4. [Implementation Details](#implementation-details)
5. [Log Levels and Patterns](#log-levels-and-patterns)
6. [Performance Monitoring](#performance-monitoring)
7. [Error Handling and Correlation](#error-handling-and-correlation)
8. [Developer Guide](#developer-guide)
9. [Best Practices](#best-practices)
10. [Troubleshooting](#troubleshooting)

## Architecture Overview

### Components

The logging architecture consists of several integrated components:

- **Logback Configuration**: XML-based configuration with structured patterns
- **SLF4J API**: Logging facade for consistent API usage
- **MDC (Mapped Diagnostic Context)**: Request correlation and context tracking
- **Application Properties**: Runtime configuration of logging behavior
- **Service Layer Logging**: Business logic operation tracking
- **Controller Layer Logging**: HTTP request/response monitoring
- **Exception Handler Logging**: Structured error reporting

### Data Flow

```
HTTP Request → Controller (MDC Setup) → Service (Business Logic) → Repository → Response
       ↓              ↓              ↓              ↓              ↓
   Correlation ID  Request Logging  Operation Logs  SQL Logs   Response Logging
```

## Logging Principles

### 1. Structured Logging

All log messages follow consistent structured patterns using key-value pairs:

```java
logger.info("Operation completed: subject={}, version={}, duration={}ms",
    subject, version, duration);
```

**Benefits:**
- Machine-readable for log aggregation systems
- Consistent parsing and analysis
- Better searchability and filtering

### 2. Correlation IDs

Each HTTP request receives a unique correlation ID for end-to-end tracing:

```java
String correlationId = UUID.randomUUID().toString();
MDC.put("correlationId", correlationId);
```

**Benefits:**
- Request tracing across service boundaries
- Debugging complex request flows
- Performance analysis per request

### 3. Configurable Verbosity

Logging levels can be adjusted via application properties without code changes:

```properties
app.logging.business-operations.enabled=true
app.logging.performance.enabled=true
logging.level.ru.vegorov.schemaregistry.service=INFO
```

### 4. Performance Awareness

Automatic performance monitoring with configurable thresholds:

```java
if (duration > slowThresholdMs) {
    logger.warn("Slow operation detected: duration={}ms", duration);
}
```

### 5. Context Preservation

All logs include relevant business context (subject, consumer, operation type):

```java
MDC.put("operation", "registerCanonicalSchema");
MDC.put("subject", subject);
```

## Configuration

### Logback Configuration (`logback-spring.xml`)

```xml
<!-- Application business logic logs -->
<logger name="ru.vegorov.schemaregistry.service" level="INFO" additivity="false">
    <appender-ref ref="CONSOLE"/>
    <appender-ref ref="FILE"/>
</logger>

<!-- Application controllers logs -->
<logger name="ru.vegorov.schemaregistry.controller" level="INFO" additivity="false">
    <appender-ref ref="CONSOLE"/>
    <appender-ref ref="FILE"/>
</logger>

<!-- Application exceptions logs -->
<logger name="ru.vegorov.schemaregistry.exception" level="WARN" additivity="false">
    <appender-ref ref="CONSOLE"/>
    <appender-ref ref="FILE"/>
</logger>
```

### Application Properties (`application.properties`)

```properties
# Application Logging Configuration
logging.level.ru.vegorov.schemaregistry.service=INFO
logging.level.ru.vegorov.schemaregistry.controller=INFO
logging.level.ru.vegorov.schemaregistry.exception=WARN

# Business operation logging
app.logging.business-operations.enabled=true
app.logging.business-operations.level=INFO

# Request/Response logging
app.logging.requests.enabled=true
app.logging.requests.level=DEBUG
app.logging.requests.include-payload=false

# Performance logging
app.logging.performance.enabled=true
app.logging.performance.slow-threshold-ms=1000
```

### Environment-Specific Configuration

**Development:**
```properties
logging.level.ru.vegorov.schemaregistry=DEBUG
app.logging.requests.include-payload=true
```

**Production:**
```properties
logging.level.ru.vegorov.schemaregistry=WARN
app.logging.requests.include-payload=false
```

## Implementation Details

### Logger Declaration

Each class declares a logger instance:

```java
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SchemaRegistryService {
    private static final Logger logger = LoggerFactory.getLogger(SchemaRegistryService.class);

    @Value("${app.logging.business-operations.enabled:true}")
    private boolean businessLoggingEnabled;

    @Value("${app.logging.performance.enabled:true}")
    private boolean performanceLoggingEnabled;
}
```

### MDC Context Management

Controllers set up MDC context for each request:

```java
@RestController
public class SchemaRegistryController {
    @PostMapping("/schemas/{subject}")
    public ResponseEntity<SchemaResponse> registerCanonicalSchema(@PathVariable String subject, @RequestBody SchemaRegistrationRequest request) {
        String correlationId = UUID.randomUUID().toString();
        MDC.put("correlationId", correlationId);
        MDC.put("operation", "registerCanonicalSchema");
        MDC.put("subject", subject);

        try {
            // Business logic
            return ResponseEntity.ok(response);
        } finally {
            MDC.clear();
        }
    }

    // Fixed: All validation and transformation endpoints now set MDC context
    @PostMapping("/schemas/{subject}/validate")
    public ResponseEntity<SchemaValidationResponse> validateJsonAgainstCanonicalSchema(@PathVariable String subject, @RequestBody SchemaValidationRequest request) {
        String correlationId = UUID.randomUUID().toString();
        MDC.put("correlationId", correlationId);
        MDC.put("operation", "validateJsonAgainstCanonicalSchema");
        MDC.put("subject", subject);

        try {
            // validation logic
            return ResponseEntity.ok(response);
        } finally {
            MDC.clear();
        }
    }
}

@RestController
public class TransformationController {
    @PostMapping("/{consumerId}/subjects/{subject}/transform")
    public ResponseEntity<TransformationResponse> transform(@PathVariable String consumerId, @PathVariable String subject, @RequestBody TransformationRequest request) {
        String correlationId = UUID.randomUUID().toString();
        MDC.put("correlationId", correlationId);
        MDC.put("operation", "transform");
        MDC.put("consumerId", consumerId);
        MDC.put("subject", subject);

        try {
            // transformation logic
            return ResponseEntity.ok(response);
        } finally {
            MDC.clear();
        }
    }
}
```

### Performance Timing

Services include automatic performance monitoring:

```java
public SchemaResponse registerCanonicalSchema(SchemaRegistrationRequest request) {
    Instant start = performanceLoggingEnabled ? Instant.now() : null;

    try {
        // Business logic
        SchemaResponse response = performRegistration(request);

        if (performanceLoggingEnabled) {
            long duration = Duration.between(start, Instant.now()).toMillis();
            if (duration > slowThresholdMs) {
                logger.warn("Slow schema registration: subject={}, duration={}ms", request.getSubject(), duration);
            } else {
                logger.debug("Schema registration performance: subject={}, duration={}ms", request.getSubject(), duration);
            }
        }

        return response;
    } catch (Exception e) {
        if (performanceLoggingEnabled) {
            long duration = Duration.between(start, Instant.now()).toMillis();
            logger.error("Schema registration failed: subject={}, duration={}ms", request.getSubject(), duration, e);
        } else if (businessLoggingEnabled) {
            logger.error("Schema registration failed: subject={}", request.getSubject(), e);
        }
        throw e;
    }
}
```

## Log Levels and Patterns

### Log Levels

- **ERROR**: System failures, data corruption, unexpected errors
- **WARN**: Business rule violations, compatibility issues, performance warnings
- **INFO**: Successful business operations, state changes, important milestones
- **DEBUG**: Detailed operation flow, intermediate results, troubleshooting data
- **TRACE**: Full request/response payloads, internal state (use sparingly)

### Message Patterns

#### Business Operations
```java
logger.info("Operation completed: subject={}, version={}, result={}", operationType, subject, version, result);
```

#### Validation Operations
```java
// Fixed: Now logs actual JSON string length instead of JsonNode.size()
logger.info("Validating JSON against {} schema: subject={}, dataSize={}chars", schemaType, subject, jsonString.length());
logger.warn("JSON validation failed: subject={}, errors={}", subject, errorCount);
```

#### Validation Operations
```java
logger.info("Validating JSON against {} schema: subject={}, dataSize={}", schemaType, subject, jsonData.size());
logger.warn("JSON validation failed: subject={}, errors={}", subject, errorCount);
```

#### Transformation Operations
```java
logger.info("Transformation initiated: consumer={}, subject={}, engine={}", consumerId, subject, engine);
logger.debug("Applying transformation: inputSize={}, configSize={}", inputJson.size(), config.length());
```

#### Error Scenarios
```java
logger.error("Operation failed: context={}, error={}", operationContext, error.getMessage(), error);
logger.warn("Resource conflict: resource={}, reason={}", resourceId, conflictReason);
```

## Performance Monitoring

### Automatic Timing

All major operations include performance timing:

```java
Instant start = performanceLoggingEnabled ? Instant.now() : null;
// ... operation ...
long duration = Duration.between(start, Instant.now()).toMillis();
```

### Slow Operation Detection

Configurable thresholds for performance alerts:

```java
@Value("${app.logging.performance.slow-threshold-ms:1000}")
private long slowThresholdMs;

if (duration > slowThresholdMs) {
    logger.warn("Slow operation detected: operation={}, duration={}ms", operationName, duration);
}
```

### Performance Metrics

Track key performance indicators:

- Schema registration time
- Transformation execution time
- Validation processing time
- Database query performance

## Error Handling and Correlation

### Structured Error Logging

Exception handlers provide context-rich error information:

```java
@ExceptionHandler(ResourceNotFoundException.class)
public ResponseEntity<ErrorResponse> handleResourceNotFound(ResourceNotFoundException ex) {
    logger.warn("Resource not found: message={}, correlationId={}",
        ex.getMessage(), MDC.get("correlationId"));
    // Return error response
}
```

### Correlation ID Propagation

Correlation IDs are maintained throughout request processing:

```java
// Controller sets correlation ID
MDC.put("correlationId", correlationId);

// Service operations inherit correlation ID
logger.info("Processing operation: correlationId={}", MDC.get("correlationId"));

// Exception handlers include correlation ID
logger.error("Error occurred: correlationId={}", MDC.get("correlationId"), exception);
```

### Error Context

Errors include relevant business context:

```java
logger.error("Schema validation failed: subject={}, version={}, schemaType={}, correlationId={}",
    subject, version, schemaType, MDC.get("correlationId"), exception);
```

## Developer Guide

### Adding Logging to New Services

1. **Import Dependencies:**
```java
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
```

2. **Declare Logger and Configuration:**
```java
@Service
public class NewService {
    private static final Logger logger = LoggerFactory.getLogger(NewService.class);

    @Value("${app.logging.business-operations.enabled:true}")
    private boolean businessLoggingEnabled;

    @Value("${app.logging.performance.enabled:true}")
    private boolean performanceLoggingEnabled;

    @Value("${app.logging.performance.slow-threshold-ms:1000}")
    private long slowThresholdMs;
}
```

3. **Add Business Operation Logging:**
```java
public Result performOperation(Parameters params) {
    if (businessLoggingEnabled) {
        logger.info("Starting operation: param1={}, param2={}", params.getParam1(), params.getParam2());
    }

    Instant start = performanceLoggingEnabled ? Instant.now() : null;

    try {
        Result result = doBusinessLogic(params);

        if (businessLoggingEnabled) {
            logger.info("Operation completed successfully: result={}", result.getSummary());
        }

        if (performanceLoggingEnabled) {
            long duration = Duration.between(start, Instant.now()).toMillis();
            logger.debug("Operation performance: duration={}ms", duration);
        }

        return result;
    } catch (Exception e) {
        if (performanceLoggingEnabled) {
            long duration = Duration.between(start, Instant.now()).toMillis();
            logger.error("Operation failed: context, duration={}ms, error={}", context, duration, e.getMessage(), e);
        } else if (businessLoggingEnabled) {
            logger.error("Operation failed: context, error={}", context, e.getMessage(), e);
        }
        throw e;
    }
}
```

### Adding Logging to New Controllers

1. **Import MDC Dependencies:**
```java
import org.slf4j.MDC;
import java.util.UUID;
```

2. **Set Up MDC Context:**
```java
@RestController
public class NewController {

    @PostMapping("/new-endpoint/{param}")
    public ResponseEntity<ResponseType> newEndpoint(@PathVariable String param, @RequestBody RequestType request) {
        String correlationId = UUID.randomUUID().toString();
        MDC.put("correlationId", correlationId);
        MDC.put("operation", "newEndpoint");
        MDC.put("param", param);

        try {
            if (requestLoggingEnabled) {
                logger.info("Processing new endpoint request");
            }

            // Business logic
            ResponseType response = service.processRequest(request);

            if (requestLoggingEnabled) {
                logger.info("New endpoint request completed successfully");
            }

            return ResponseEntity.ok(response);
        } finally {
            MDC.clear();
        }
    }
}
```

### Adding Logging to New Exception Handlers

```java
@ExceptionHandler(NewCustomException.class)
public ResponseEntity<ErrorResponse> handleNewCustomException(NewCustomException ex) {
    if (businessLoggingEnabled) {
        logger.error("Custom error occurred: context={}, correlationId={}",
            ex.getContext(), MDC.get("correlationId"), ex);
    }

    ErrorResponse error = new ErrorResponse(
        HttpStatus.BAD_REQUEST.value(),
        "Custom Error",
        ex.getMessage(),
        LocalDateTime.now()
    );
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
}
```

### Logback Configuration for New Components

Add new logger configurations to `logback-spring.xml`:

```xml
<!-- New service logs -->
<logger name="com.example.newpackage.NewService" level="INFO" additivity="false">
    <appender-ref ref="CONSOLE"/>
    <appender-ref ref="FILE"/>
</logger>
```

### Testing Logging Implementation

1. **Unit Test Logging Behavior:**
```java
@Test
void shouldLogOperationStartAndCompletion() {
    // Given
    when(config.businessLoggingEnabled()).thenReturn(true);

    // When
    service.performOperation(params);

    // Then
    verify(logger).info("Starting operation: param1={}, param2={}", "value1", "value2");
    verify(logger).info("Operation completed successfully: result={}", "success");
}
```

2. **Integration Test with MDC:**
```java
@Test
void shouldIncludeCorrelationIdInLogs() {
    // Test that MDC context is properly set and cleared
    assertNotNull(MDC.get("correlationId"));
    // Verify logs contain correlation ID
}
```

## Best Practices

### 1. Log Message Consistency

- Use consistent key names: `subject`, `consumerId`, `version`, `operation`
- Follow established patterns for similar operations
- Include units for measurements: `duration=150ms`, `size=1024bytes`

### 2. Avoid Sensitive Data Logging

```java
// Good: Log structure without sensitive data
logger.info("User authentication: userId={}, success={}", userId, success);

// Bad: Log passwords or tokens
logger.info("User login: username={}, password={}", username, password);
```

### 3. Performance Considerations

- Use parameterized logging to avoid string concatenation overhead
- Check logging level before expensive operations
- Use appropriate log levels to control verbosity in production

### 4. Context Management

- Always clear MDC context in finally blocks
- Set operation context at the beginning of request processing
- Include relevant business identifiers in MDC

### 5. Error Logging

- **Always log business errors**: Use the pattern `if (performanceLoggingEnabled) { ... } else if (businessLoggingEnabled) { ... }` to ensure errors are logged even when performance monitoring is disabled
- Log exceptions with full stack traces at ERROR level
- Include business context in error messages
- Use WARN for recoverable errors, ERROR for system failures
- **Critical**: Business errors must be logged regardless of performance logging configuration

## Troubleshooting

### Common Issues

1. **Missing Correlation IDs** ✅ **FIXED**
   - **Issue**: MDC context not set in all controller endpoints
   - **Solution**: Added MDC setup to all validation and transformation endpoints
   - **Result**: All requests now have unique correlation IDs for tracing

2. **Incorrect Data Size Logging** ✅ **FIXED**
   - **Issue**: `JsonNode.size()` returned child element count (often 1) instead of data size
   - **Solution**: Changed to `objectMapper.writeValueAsString(jsonData).length()`
   - **Result**: Logs now show meaningful character counts (e.g., "9470chars")

3. **Performance Impact**
   - Check if performance logging is enabled in production
   - Adjust slow thresholds appropriately

4. **Log Level Configuration**
   - Verify property precedence: application.properties > environment variables
   - Check for profile-specific configurations

### Debug Commands

```bash
# Check current logging configuration
curl http://localhost:8080/actuator/loggers

# Change log level dynamically
curl -X POST http://localhost:8080/actuator/loggers/ru.vegorov.schemaregistry.service \
  -H "Content-Type: application/json" \
  -d '{"configuredLevel": "DEBUG"}'
```

### Log Analysis

```bash
# Find slow operations
grep "Slow.*detected" logs/schema-registry.log

# Find errors by correlation ID
grep "correlationId=abc-123" logs/schema-registry.log

# Count operations by type
grep "operation=" logs/schema-registry.log | cut -d'=' -f2 | sort | uniq -c
```

This logging architecture provides comprehensive observability while maintaining performance and configurability. Follow the developer guide when extending the codebase to ensure consistent logging patterns across the application.