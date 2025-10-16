package ru.vegorov.schemaregistry.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import ru.vegorov.schemaregistry.exception.ConflictException;
import ru.vegorov.schemaregistry.service.TransformationException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @Value("${app.logging.business-operations.enabled:true}")
    private boolean businessLoggingEnabled;

    /**
     * Get correlationId from request attributes, fallback to MDC
     */
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

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFound(ResourceNotFoundException ex) {
        String correlationId = getCorrelationId();

        // Set MDC context for structured logging
        org.slf4j.MDC.put("correlationId", correlationId);
        org.slf4j.MDC.put("operation", "exceptionHandler");

        try {
            if (businessLoggingEnabled) {
                logger.warn("Resource not found: message={}", ex.getMessage());
            }
        } finally {
            org.slf4j.MDC.clear();
        }

        ErrorResponse error = new ErrorResponse(
            HttpStatus.NOT_FOUND.value(),
            "Resource Not Found",
            ex.getMessage(),
            LocalDateTime.now()
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ErrorResponse> handleConflict(ConflictException ex) {
        String correlationId = getCorrelationId();

        // Set MDC context for structured logging
        org.slf4j.MDC.put("correlationId", correlationId);
        org.slf4j.MDC.put("operation", "exceptionHandler");

        try {
            if (businessLoggingEnabled) {
                logger.warn("Resource conflict: message={}", ex.getMessage());
            }
        } finally {
            org.slf4j.MDC.clear();
        }

        ErrorResponse error = new ErrorResponse(
            HttpStatus.CONFLICT.value(),
            "Conflict",
            ex.getMessage(),
            LocalDateTime.now()
        );
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
        String correlationId = getCorrelationId();

        // Set MDC context for structured logging
        org.slf4j.MDC.put("correlationId", correlationId);
        org.slf4j.MDC.put("operation", "exceptionHandler");

        try {
            if (businessLoggingEnabled) {
                logger.warn("Illegal argument: message={}", ex.getMessage());
            }
        } finally {
            org.slf4j.MDC.clear();
        }

        ErrorResponse error = new ErrorResponse(
            HttpStatus.BAD_REQUEST.value(),
            "Bad Request",
            ex.getMessage(),
            LocalDateTime.now()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(SchemaValidationException.class)
    public ResponseEntity<ErrorResponse> handleSchemaValidation(SchemaValidationException ex) {
        String correlationId = getCorrelationId();

        // Set MDC context for structured logging
        org.slf4j.MDC.put("correlationId", correlationId);
        org.slf4j.MDC.put("operation", "exceptionHandler");

        try {
            if (businessLoggingEnabled) {
                logger.error("Schema validation error: message={}", ex.getMessage(), ex);
            }
        } finally {
            org.slf4j.MDC.clear();
        }

        ErrorResponse error = new ErrorResponse(
            HttpStatus.BAD_REQUEST.value(),
            "Schema Validation Error",
            ex.getMessage(),
            LocalDateTime.now()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(TransformationException.class)
    public ResponseEntity<ErrorResponse> handleTransformation(TransformationException ex) {
        String correlationId = getCorrelationId();

        // Set MDC context for structured logging
        org.slf4j.MDC.put("correlationId", correlationId);
        org.slf4j.MDC.put("operation", "exceptionHandler");

        try {
            if (businessLoggingEnabled) {
                logger.error("Transformation error: message={}", ex.getMessage(), ex);
            }
        } finally {
            org.slf4j.MDC.clear();
        }

        ErrorResponse error = new ErrorResponse(
            HttpStatus.INTERNAL_SERVER_ERROR.value(),
            "Transformation Error",
            ex.getMessage(),
            LocalDateTime.now()
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ValidationErrorResponse> handleValidationErrors(MethodArgumentNotValidException ex) {
        String correlationId = getCorrelationId();

        // Set MDC context for structured logging
        org.slf4j.MDC.put("correlationId", correlationId);
        org.slf4j.MDC.put("operation", "exceptionHandler");

        try {
            if (businessLoggingEnabled) {
                logger.warn("Validation error: message={}", "Input validation failed");
            }
        } finally {
            org.slf4j.MDC.clear();
        }

        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
            errors.put(error.getField(), error.getDefaultMessage()));

        ValidationErrorResponse error = new ValidationErrorResponse(
            HttpStatus.BAD_REQUEST.value(),
            "Validation Failed",
            "Input validation failed",
            errors,
            LocalDateTime.now()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(HttpMediaTypeNotAcceptableException.class)
    public ResponseEntity<Void> handleMediaTypeNotAcceptable(HttpMediaTypeNotAcceptableException ex) {
        String correlationId = getCorrelationId();

        // Set MDC context for structured logging
        org.slf4j.MDC.put("correlationId", correlationId);
        org.slf4j.MDC.put("operation", "exceptionHandler");

        try {
            if (businessLoggingEnabled) {
                logger.warn("Media type not acceptable: message={}", ex.getMessage());
            }
        } finally {
            org.slf4j.MDC.clear();
        }

        return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).build();
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleMessageNotReadable(HttpMessageNotReadableException ex) {
        String correlationId = getCorrelationId();

        // Set MDC context for structured logging
        org.slf4j.MDC.put("correlationId", correlationId);
        org.slf4j.MDC.put("operation", "exceptionHandler");

        try {
            if (businessLoggingEnabled) {
                logger.warn("Message not readable: message={}", ex.getMessage());
            }
        } finally {
            org.slf4j.MDC.clear();
        }

        ErrorResponse error = new ErrorResponse(
            HttpStatus.BAD_REQUEST.value(),
            "Bad Request",
            "Invalid JSON format in request body",
            LocalDateTime.now()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMediaTypeNotSupported(HttpMediaTypeNotSupportedException ex) {
        String correlationId = getCorrelationId();

        // Set MDC context for structured logging
        org.slf4j.MDC.put("correlationId", correlationId);
        org.slf4j.MDC.put("operation", "exceptionHandler");

        try {
            if (businessLoggingEnabled) {
                logger.warn("Media type not supported: message={}", ex.getMessage());
            }
        } finally {
            org.slf4j.MDC.clear();
        }

        ErrorResponse error = new ErrorResponse(
            HttpStatus.UNSUPPORTED_MEDIA_TYPE.value(),
            "Unsupported Media Type",
            "Content-Type not supported. Expected application/json",
            LocalDateTime.now()
        );
        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE).body(error);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
        String correlationId = getCorrelationId();

        // Set MDC context for structured logging
        org.slf4j.MDC.put("correlationId", correlationId);
        org.slf4j.MDC.put("operation", "exceptionHandler");

        try {
            logger.error("Unexpected error occurred: message={}", ex.getMessage(), ex);
        } finally {
            org.slf4j.MDC.clear();
        }

        ErrorResponse error = new ErrorResponse(
            HttpStatus.INTERNAL_SERVER_ERROR.value(),
            "Internal Server Error",
            "An unexpected error occurred",
            LocalDateTime.now()
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }

    public static class ErrorResponse {
        private int status;
        private String error;
        private String message;
        private LocalDateTime timestamp;

        public ErrorResponse(int status, String error, String message, LocalDateTime timestamp) {
            this.status = status;
            this.error = error;
            this.message = message;
            this.timestamp = timestamp;
        }

        // Getters and setters
        public int getStatus() { return status; }
        public void setStatus(int status) { this.status = status; }
        public String getError() { return error; }
        public void setError(String error) { this.error = error; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public LocalDateTime getTimestamp() { return timestamp; }
        public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    }

    public static class ValidationErrorResponse extends ErrorResponse {
        private Map<String, String> fieldErrors;

        public ValidationErrorResponse(int status, String error, String message,
                                     Map<String, String> fieldErrors, LocalDateTime timestamp) {
            super(status, error, message, timestamp);
            this.fieldErrors = fieldErrors;
        }

        public Map<String, String> getFieldErrors() { return fieldErrors; }
        public void setFieldErrors(Map<String, String> fieldErrors) { this.fieldErrors = fieldErrors; }
    }
}