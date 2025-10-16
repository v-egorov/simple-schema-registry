# Exception Handling Guide

## Overview

This guide provides comprehensive documentation for developers on exception handling patterns, the GlobalExceptionHandler implementation, and extending the service exception taxonomy in the Simple Schema Registry application.

## Table of Contents

1. [Exception Handling Principles](#exception-handling-principles)
2. [Global Exception Handler Implementation](#global-exception-handler-implementation)
3. [Custom Exception Taxonomy](#custom-exception-taxonomy)
4. [Extending Exception Taxonomy](#extending-exception-taxonomy)
5. [Exception Handling Patterns](#exception-handling-patterns)
6. [Best Practices](#best-practices)
7. [Testing Exception Handling](#testing-exception-handling)

## Exception Handling Principles

### 1. Consistent Error Responses

All exceptions are handled by the `GlobalExceptionHandler` to provide consistent error response format:

```json
{
  "status": 404,
  "error": "Resource Not Found",
  "message": "Schema with subject 'user' and version 1 not found",
  "timestamp": "2024-01-15T10:30:00"
}
```

### 2. Appropriate HTTP Status Codes

- **400 Bad Request**: Validation errors, invalid input
- **404 Not Found**: Resource not found
- **409 Conflict**: Resource conflicts (e.g., schema already exists)
- **415 Unsupported Media Type**: Wrong content type
- **500 Internal Server Error**: Unexpected server errors

### 3. Correlation ID Propagation

All exception handlers include correlation ID for request tracing:

```java
String correlationId = getCorrelationId();
MDC.put("correlationId", correlationId);
```

### 4. Structured Logging

Exception handlers use structured logging with business context:

```java
logger.warn("Resource not found: message={}", ex.getMessage());
```

## Global Exception Handler Implementation

### Core Components

The `GlobalExceptionHandler` is a `@RestControllerAdvice` class that centralizes exception handling:

```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @Value("${app.logging.business-operations.enabled:true}")
    private boolean businessLoggingEnabled;
}
```

### Correlation ID Resolution

The handler includes a robust correlation ID resolution method:

```java
private String getCorrelationId() {
    try {
        if (RequestContextHolder.getRequestAttributes() != null) {
            ServletRequestAttributes requestAttributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            String correlationId = (String) requestAttributes.getAttribute("correlationId", 0);
            if (correlationId != null) {
                return correlationId;
            }
        }
    } catch (Exception e) {
        // RequestContextHolder might not be available in some contexts
    }
    // Fallback to MDC
    return org.slf4j.MDC.get("correlationId");
}
```

### Exception Handler Pattern

Each exception handler follows this pattern:

```java
@ExceptionHandler(CustomException.class)
public ResponseEntity<ErrorResponse> handleCustom(CustomException ex) {
    String correlationId = getCorrelationId();

    // Set MDC context for structured logging
    org.slf4j.MDC.put("correlationId", correlationId);
    org.slf4j.MDC.put("operation", "exceptionHandler");

    try {
        if (businessLoggingEnabled) {
            logger.warn("Custom error: message={}", ex.getMessage());
        }
    } finally {
        org.slf4j.MDC.clear();
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

### Supported Exception Types

#### Custom Business Exceptions

- **ResourceNotFoundException** → 404 Not Found
- **ConflictException** → 409 Conflict
- **SchemaValidationException** → 400 Bad Request
- **TransformationException** → 500 Internal Server Error

#### Framework Exceptions

- **MethodArgumentNotValidException** → 400 Bad Request (with field errors)
- **HttpMessageNotReadableException** → 400 Bad Request
- **HttpMediaTypeNotSupportedException** → 415 Unsupported Media Type
- **HttpMediaTypeNotAcceptableException** → 406 Not Acceptable

#### Generic Exception

- **Exception** → 500 Internal Server Error (catch-all)

### Response Types

#### Standard Error Response

```java
public static class ErrorResponse {
    private int status;
    private String error;
    private String message;
    private LocalDateTime timestamp;

    // Constructor and getters/setters
}
```

#### Validation Error Response

```java
public static class ValidationErrorResponse extends ErrorResponse {
    private Map<String, String> fieldErrors;

    // Constructor and getters/setters
}
```

## Custom Exception Taxonomy

### Current Exception Hierarchy

```
RuntimeException (unchecked)
├── ResourceNotFoundException
├── ConflictException
└── SchemaValidationException

Exception (checked)
└── TransformationException
```

### Exception Categories

#### 1. Resource Exceptions

**ResourceNotFoundException**
- Thrown when requested resource doesn't exist
- Maps to HTTP 404
- Examples: Schema not found, Consumer not found

**ConflictException**
- Thrown when operation conflicts with existing state
- Maps to HTTP 409
- Examples: Schema already exists, Version conflict

#### 2. Validation Exceptions

**SchemaValidationException**
- Thrown when schema validation fails
- Maps to HTTP 400
- Examples: Invalid JSON schema, Schema compatibility issues

#### 3. Processing Exceptions

**TransformationException**
- Thrown when data transformation fails
- Maps to HTTP 500
- Examples: JSLT transformation errors, Pipeline failures

## Extending Exception Taxonomy

### When to Add New Exceptions

Add new custom exceptions when:

1. **Specific Error Handling Required**: Different HTTP status codes or error messages needed
2. **Business Logic Distinction**: Error type carries specific business meaning
3. **Logging Differentiation**: Different log levels or monitoring required

### Steps to Add New Exception

#### 1. Create Exception Class

```java
package ru.vegorov.schemaregistry.exception;

public class SubjectNotRegisteredException extends RuntimeException {

    public SubjectNotRegisteredException(String subject) {
        super("Subject '" + subject + "' is not registered for this consumer");
    }

    public SubjectNotRegisteredException(String subject, Throwable cause) {
        super("Subject '" + subject + "' is not registered for this consumer", cause);
    }
}
```

#### 2. Add Handler to GlobalExceptionHandler

```java
@ExceptionHandler(SubjectNotRegisteredException.class)
public ResponseEntity<ErrorResponse> handleSubjectNotRegistered(SubjectNotRegisteredException ex) {
    String correlationId = getCorrelationId();

    // Set MDC context for structured logging
    org.slf4j.MDC.put("correlationId", correlationId);
    org.slf4j.MDC.put("operation", "exceptionHandler");

    try {
        if (businessLoggingEnabled) {
            logger.warn("Subject not registered: message={}", ex.getMessage());
        }
    } finally {
        org.slf4j.MDC.clear();
    }

    ErrorResponse error = new ErrorResponse(
        HttpStatus.BAD_REQUEST.value(),
        "Subject Not Registered",
        ex.getMessage(),
        LocalDateTime.now()
    );
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
}
```

#### 3. Update Logback Configuration

Add logger configuration in `logback-spring.xml`:

```xml
<!-- New exception type logs -->
<logger name="ru.vegorov.schemaregistry.exception.SubjectNotRegisteredException" level="WARN" additivity="false">
    <appender-ref ref="CONSOLE"/>
    <appender-ref ref="FILE"/>
</logger>
```

#### 4. Update Application Properties

Add logging level configuration in `application.properties`:

```properties
logging.level.ru.vegorov.schemaregistry.exception.SubjectNotRegisteredException=WARN
```

#### 5. Update Documentation

Update this guide and `docs/architecture.md` to reflect the new exception.

### Exception Naming Conventions

- **Suffix**: End with "Exception"
- **Specificity**: Be specific about the error condition
- **Consistency**: Follow existing naming patterns

Examples:
- `ResourceNotFoundException` (not `NotFoundException`)
- `SchemaValidationException` (not `ValidationException`)
- `TransformationException` (not `TransformException`)

## Exception Handling Patterns

### Service Layer Exception Handling

Services should throw appropriate custom exceptions:

```java
@Service
public class SchemaRegistryService {

    public SchemaResponse getSchema(String subject, Integer version) {
        SchemaEntity schema = schemaRepository.findBySubjectAndVersion(subject, version);
        if (schema == null) {
            throw new ResourceNotFoundException(
                "Schema with subject '" + subject + "' and version " + version + " not found");
        }
        return mapToResponse(schema);
    }

    public void registerSchema(SchemaRegistrationRequest request) {
        // Check for conflicts
        if (schemaRepository.existsBySubjectAndVersion(request.getSubject(), 1)) {
            throw new ConflictException(
                "Schema with subject '" + request.getSubject() + "' already exists");
        }

        // Validate schema
        try {
            validateSchema(request.getSchema());
        } catch (Exception e) {
            throw new SchemaValidationException(
                "Invalid schema: " + e.getMessage(), e);
        }

        // Register schema
        // ...
    }
}
```

### Controller Layer Exception Handling

Controllers should let exceptions bubble up to GlobalExceptionHandler:

```java
@RestController
public class SchemaRegistryController {

    @GetMapping("/schemas/{subject}/versions/{version}")
    public ResponseEntity<SchemaResponse> getSchema(@PathVariable String subject, @PathVariable Integer version) {
        // No try-catch needed - let GlobalExceptionHandler handle ResourceNotFoundException
        SchemaResponse response = schemaService.getSchema(subject, version);
        return ResponseEntity.ok(response);
    }
}
```

### Transformation Exception Handling

Transformations should use checked exceptions:

```java
public JsonNode transform(JsonNode input, String jsltExpression) throws TransformationException {
    try {
        // Transformation logic
        return JsltTransformer.transform(input, jsltExpression);
    } catch (JsltException e) {
        throw new TransformationException("JSLT transformation failed: " + e.getMessage(), e);
    } catch (Exception e) {
        throw new TransformationException("Unexpected transformation error: " + e.getMessage(), e);
    }
}
```

## Best Practices

### 1. Exception Type Selection

- **Use RuntimeException for business logic errors**: ResourceNotFoundException, ConflictException
- **Use checked Exception for external failures**: TransformationException
- **Avoid generic exceptions**: Don't throw Exception or RuntimeException directly

### 2. Exception Message Quality

- **Be specific**: Include relevant identifiers (subject, version, consumerId)
- **Be user-friendly**: Messages should be understandable by API consumers
- **Include context**: Add business context for debugging

```java
// Good
throw new ResourceNotFoundException("Schema with subject 'user' and version 2 not found");

// Bad
throw new ResourceNotFoundException("Not found");
```

### 3. Exception Chaining

Always preserve the original exception:

```java
try {
    externalService.call();
} catch (ExternalServiceException e) {
    throw new TransformationException("External service failed: " + e.getMessage(), e);
}
```

### 4. Logging Levels

- **WARN**: Expected business errors (not found, conflicts)
- **ERROR**: Unexpected errors, system failures
- **DEBUG**: Detailed error context for troubleshooting

### 5. Performance Considerations

- Exception creation is expensive - use only for exceptional cases
- Avoid logging large objects in exception messages
- Use conditional logging based on configuration flags

## Testing Exception Handling

### Unit Testing Exception Handlers

```java
@SpringBootTest
class GlobalExceptionHandlerTest {

    @Autowired
    private GlobalExceptionHandler exceptionHandler;

    @Test
    void shouldHandleResourceNotFoundException() {
        // Given
        ResourceNotFoundException ex = new ResourceNotFoundException("Schema not found");

        // When
        ResponseEntity<GlobalExceptionHandler.ErrorResponse> response =
            exceptionHandler.handleResourceNotFound(ex);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().getError()).isEqualTo("Resource Not Found");
        assertThat(response.getBody().getMessage()).isEqualTo("Schema not found");
    }
}
```

### Integration Testing

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ExceptionHandlingIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void shouldReturn404ForNonExistentSchema() {
        // When
        ResponseEntity<ErrorResponse> response = restTemplate.getForEntity(
            "/schemas/nonexistent/versions/1", ErrorResponse.class);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().getError()).isEqualTo("Resource Not Found");
    }
}
```

### Testing Custom Exceptions

```java
@Test
void shouldThrowConflictExceptionWhenSchemaExists() {
    // Given
    SchemaRegistrationRequest request = createSchemaRequest("existing-subject");

    // When & Then
    assertThatThrownBy(() -> schemaService.registerSchema(request))
        .isInstanceOf(ConflictException.class)
        .hasMessageContaining("already exists");
}
```

This exception handling architecture ensures consistent error responses, proper logging, and maintainable error taxonomy across the application.