# API Reference

This document provides detailed API documentation for the JSON Schema Registry and Transformation Service. All endpoints return JSON responses and use standard HTTP status codes.

## Base URL
```
http://localhost:8080
```

## Authentication
Currently, no authentication is required. All endpoints are publicly accessible.

## Content Types
- **Request**: `application/json`
- **Response**: `application/json`

---

## Schema Registry API

### Register Schema
Register a new schema or create a new version of an existing schema.

**Endpoint**: `POST /api/schemas`

**Request Body**:
```json
{
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
  "description": "User profile schema for version 1"
}
```

**Response** (201 Created):
```json
{
  "id": 1,
  "subject": "user-profile",
  "version": 1,
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
  "description": "User profile schema for version 1",
  "createdAt": "2024-01-15T10:30:00",
  "updatedAt": "2024-01-15T10:30:00"
}
```

### Get All Schema Versions
Retrieve all versions of a schema by subject.

**Endpoint**: `GET /api/schemas/{subject}`

**Response** (200 OK):
```json
[
  {
    "id": 1,
    "subject": "user-profile",
    "version": 1,
    "schema": {...},
    "compatibility": "BACKWARD",
    "description": "User profile schema for version 1",
    "createdAt": "2024-01-15T10:30:00",
    "updatedAt": "2024-01-15T10:30:00"
  },
  {
    "id": 2,
    "subject": "user-profile",
    "version": 2,
    "schema": {...},
    "compatibility": "BACKWARD",
    "description": "User profile schema for version 2",
    "createdAt": "2024-01-15T11:00:00",
    "updatedAt": "2024-01-15T11:00:00"
  }
]
```

### Get Specific Schema Version
Retrieve a specific version of a schema.

**Endpoint**: `GET /api/schemas/{subject}/{version}`

**Response** (200 OK):
```json
{
  "id": 2,
  "subject": "user-profile",
  "version": 2,
  "schema": {...},
  "compatibility": "BACKWARD",
  "description": "User profile schema for version 2",
  "createdAt": "2024-01-15T11:00:00",
  "updatedAt": "2024-01-15T11:00:00"
}
```

### Get Latest Schema Version
Retrieve the latest version of a schema.

**Endpoint**: `GET /api/schemas/{subject}/latest`

**Response** (200 OK):
```json
{
  "id": 2,
  "subject": "user-profile",
  "version": 2,
  "schema": {...},
  "compatibility": "BACKWARD",
  "description": "User profile schema for version 2",
  "createdAt": "2024-01-15T11:00:00",
  "updatedAt": "2024-01-15T11:00:00"
}
```

### Check Schema Compatibility
Check if a new schema is compatible with existing versions.

**Endpoint**: `POST /api/schemas/{subject}/compat`

**Request Body**:
```json
{
  "schema": {
    "$schema": "http://json-schema.org/draft-07/schema#",
    "type": "object",
    "properties": {
      "id": {"type": "integer"},
      "name": {"type": "string"},
      "email": {"type": "string", "format": "email"},
      "phone": {"type": "string"}
    },
    "required": ["id", "name"]
  }
}
```

**Response** (200 OK):
```json
{
  "compatible": true,
  "message": "Schema is compatible with all existing versions"
}
```

### List All Schema Subjects
Retrieve all unique schema subjects.

**Endpoint**: `GET /api/schemas/subjects`

**Response** (200 OK):
```json
[
  "user-profile",
  "product-catalog",
  "order-data"
]
```

---

## Consumer Management API

### Register Consumer
Register a new consumer application.

**Endpoint**: `POST /api/consumers`

**Request Body**:
```json
{
  "consumerId": "mobile-app",
  "name": "Mobile Application",
  "description": "iOS and Android mobile app consumer"
}
```

**Response** (201 Created):
```json
{
  "consumerId": "mobile-app",
  "name": "Mobile Application",
  "description": "iOS and Android mobile app consumer",
  "createdAt": "2024-01-15T10:30:00",
  "updatedAt": "2024-01-15T10:30:00"
}
```

### List All Consumers
Retrieve all registered consumers.

**Endpoint**: `GET /api/consumers`

**Response** (200 OK):
```json
[
  {
    "consumerId": "mobile-app",
    "name": "Mobile Application",
    "description": "iOS and Android mobile app consumer",
    "createdAt": "2024-01-15T10:30:00",
    "updatedAt": "2024-01-15T10:30:00"
  },
  {
    "consumerId": "web-dashboard",
    "name": "Web Dashboard",
    "description": "Internal web dashboard for analytics",
    "createdAt": "2024-01-15T10:35:00",
    "updatedAt": "2024-01-15T10:35:00"
  }
]
```

### Get Consumer Details
Retrieve details of a specific consumer.

**Endpoint**: `GET /api/consumers/{consumerId}`

**Response** (200 OK):
```json
{
  "consumerId": "mobile-app",
  "name": "Mobile Application",
  "description": "iOS and Android mobile app consumer",
  "createdAt": "2024-01-15T10:30:00",
  "updatedAt": "2024-01-15T10:30:00"
}
```

---

## Transformation API

### Transform JSON Data
Transform canonical JSON data for a specific consumer using their transformation template.

**Endpoint**: `POST /api/transform/{consumerId}`

#### JSLT Engine Example
**Request Body**:
```json
{
  "canonicalJson": {
    "userId": 12345,
    "fullName": "John Doe",
    "emailAddress": "john.doe@example.com",
    "registrationDate": "2024-01-15T10:30:00Z",
    "accountStatus": "active"
  }
}
```

**Response** (200 OK):
```json
{
  "transformedJson": {
    "id": 12345,
    "name": "John Doe",
    "email": "john.doe@example.com",
    "registered": "2024-01-15T10:30:00Z",
    "status": "active"
  }
}
```

#### Router Engine Example
**Request Body** (User Data):
```json
{
  "canonicalJson": {
    "type": "user",
    "id": 12345,
    "firstName": "John",
    "lastName": "Doe",
    "email": "john.doe@example.com",
    "department": "engineering"
  }
}
```

**Response** (200 OK):
```json
{
  "transformedJson": {
    "user_id": 12345,
    "full_name": "John Doe",
    "email": "john.doe@example.com",
    "department": "engineering",
    "normalized_type": "user"
  }
}
```

**Request Body** (Product Data):
```json
{
  "canonicalJson": {
    "type": "product",
    "id": 67890,
    "name": "Wireless Headphones",
    "category": "electronics",
    "price": 199.99,
    "inStock": true
  }
}
```

**Response** (200 OK):
```json
{
  "transformedJson": {
    "product_id": 67890,
    "product_name": "Wireless Headphones",
    "category": "electronics",
    "price_usd": 199.99,
    "availability": "in_stock",
    "enriched_category": "electronics"
  }
}
```

#### Pipeline Engine Example
**Request Body**:
```json
{
  "canonicalJson": {
    "type": "user",
    "id": 12345,
    "firstName": "John",
    "lastName": "Doe",
    "email": "john.doe@example.com",
    "registrationDate": "2024-01-15T10:30:00Z"
  }
}
```

**Response** (200 OK):
```json
{
  "transformedJson": {
    "type": "user",
    "id": 12345,
    "firstName": "John",
    "lastName": "Doe",
    "email": "john.doe@example.com",
    "registrationDate": "2024-01-15T10:30:00Z",
    "validated": true,
    "normalized": true,
    "enriched": true,
    "timestamp": "2024-01-15T10:35:00Z"
  }
}
```

### Get Transformation Template
Retrieve the transformation template for a consumer.

**Endpoint**: `GET /api/transform/templates/{consumerId}`

**Response** (200 OK):
```json
{
  "consumerId": "mobile-app",
  "template": ". | {id: .userId, name: .fullName, email: .emailAddress, registered: .registrationDate, status: .accountStatus}",
  "engine": "JSLT",
  "createdAt": "2024-01-15T10:35:00",
  "updatedAt": "2024-01-15T10:35:00"
}
```

### Create/Update Transformation Template
Create or update a transformation template for a consumer.

**Endpoint**: `POST /api/transform/templates/{consumerId}`

#### JSLT Engine Example
**Request Body**:
```json
{
  "engine": "jslt",
  "expression": ". | {id: .userId, name: .fullName, email: .emailAddress, registered: .registrationDate, status: .accountStatus}",
  "description": "Simple field mapping for mobile app"
}
```

**Response** (200 OK):
```json
{
  "id": 1,
  "consumerId": "mobile-app",
  "engine": "jslt",
  "expression": ". | {id: .userId, name: .fullName, email: .emailAddress, registered: .registrationDate, status: .accountStatus}",
  "configuration": null,
  "description": "Simple field mapping for mobile app",
  "createdAt": "2024-01-15T10:35:00",
  "updatedAt": "2024-01-15T10:35:00"
}
```

#### Router Engine Example
**Request Body**:
```json
{
  "engine": "router",
  "routerConfig": {
    "type": "router",
    "routes": [
      {
        "condition": "$.type == 'user'",
        "transformationId": "user-normalization-v1",
        "description": "Normalize user data"
      },
      {
        "condition": "$.type == 'product'",
        "transformationId": "product-enrichment-v1",
        "description": "Enrich product data"
      }
    ],
    "defaultTransformationId": "generic-transformation-v1",
    "validation": {
      "inputSchema": "canonical-data-schema-v1",
      "outputSchema": "consumer-data-schema-v1"
    }
  },
  "description": "Content-based routing for different data types"
}
```

**Response** (200 OK):
```json
{
  "id": 2,
  "consumerId": "multi-tenant-app",
  "engine": "router",
  "expression": "{\"type\":\"router\",\"routes\":[{\"condition\":\"$.type == 'user'\",\"transformationId\":\"user-normalization-v1\",\"description\":\"Normalize user data\"},{\"condition\":\"$.type == 'product'\",\"transformationId\":\"product-enrichment-v1\",\"description\":\"Enrich product data\"}],\"defaultTransformationId\":\"generic-transformation-v1\",\"validation\":{\"inputSchema\":\"canonical-data-schema-v1\",\"outputSchema\":\"consumer-data-schema-v1\"}}",
  "configuration": "{\"type\":\"router\",\"routes\":[{\"condition\":\"$.type == 'user'\",\"transformationId\":\"user-normalization-v1\",\"description\":\"Normalize user data\"},{\"condition\":\"$.type == 'product'\",\"transformationId\":\"product-enrichment-v1\",\"description\":\"Enrich product data\"}],\"defaultTransformationId\":\"generic-transformation-v1\",\"validation\":{\"inputSchema\":\"canonical-data-schema-v1\",\"outputSchema\":\"consumer-data-schema-v1\"}}",
  "description": "Content-based routing for different data types",
  "createdAt": "2024-01-15T10:40:00",
  "updatedAt": "2024-01-15T10:40:00"
}
```

#### Pipeline Engine Example
**Request Body**:
```json
{
  "engine": "pipeline",
  "pipelineConfig": {
    "type": "pipeline",
    "steps": [
      {
        "name": "validate-input",
        "transformationId": "input-validation-v1",
        "continueOnError": false,
        "description": "Validate input data structure"
      },
      {
        "name": "normalize-data",
        "transformationId": "data-normalization-v1",
        "continueOnError": false,
        "description": "Normalize data format"
      },
      {
        "name": "enrich-data",
        "transformationId": "data-enrichment-v1",
        "continueOnError": true,
        "description": "Add computed fields"
      }
    ],
    "validation": {
      "finalSchema": "enriched-data-schema-v1",
      "intermediateSchemas": {
        "after-step-1": "validated-data-schema-v1",
        "after-step-2": "normalized-data-schema-v1"
      }
    }
  },
  "description": "Multi-step data processing pipeline"
}
```

**Response** (200 OK):
```json
{
  "id": 3,
  "consumerId": "analytics-platform",
  "engine": "pipeline",
  "expression": "{\"type\":\"pipeline\",\"steps\":[{\"name\":\"validate-input\",\"transformationId\":\"input-validation-v1\",\"continueOnError\":false,\"description\":\"Validate input data structure\"},{\"name\":\"normalize-data\",\"transformationId\":\"data-normalization-v1\",\"continueOnError\":false,\"description\":\"Normalize data format\"},{\"name\":\"enrich-data\",\"transformationId\":\"data-enrichment-v1\",\"continueOnError\":true,\"description\":\"Add computed fields\"}],\"validation\":{\"finalSchema\":\"enriched-data-schema-v1\",\"intermediateSchemas\":{\"after-step-1\":\"validated-data-schema-v1\",\"after-step-2\":\"normalized-data-schema-v1\"}}}",
  "configuration": "{\"type\":\"pipeline\",\"steps\":[{\"name\":\"validate-input\",\"transformationId\":\"input-validation-v1\",\"continueOnError\":false,\"description\":\"Validate input data structure\"},{\"name\":\"normalize-data\",\"transformationId\":\"data-normalization-v1\",\"continueOnError\":false,\"description\":\"Normalize data format\"},{\"name\":\"enrich-data\",\"transformationId\":\"data-enrichment-v1\",\"continueOnError\":true,\"description\":\"Add computed fields\"}],\"validation\":{\"finalSchema\":\"enriched-data-schema-v1\",\"intermediateSchemas\":{\"after-step-1\":\"validated-data-schema-v1\",\"after-step-2\":\"normalized-data-schema-v1\"}}}",
  "description": "Multi-step data processing pipeline",
  "createdAt": "2024-01-15T10:45:00",
  "updatedAt": "2024-01-15T10:45:00"
}
```

### List Available Transformation Engines
Get a list of supported transformation engines.

**Endpoint**: `GET /api/transform/engines`

**Response** (200 OK):
```json
[
  "jslt",
  "router",
  "pipeline"
]
```

---

## Error Responses

All endpoints may return the following error responses:

### 400 Bad Request
Invalid request data or validation errors.

```json
{
  "timestamp": "2024-01-15T10:30:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed for argument [0] in public...",
  "path": "/api/schemas"
}
```

### 404 Not Found
Resource not found.

```json
{
  "timestamp": "2024-01-15T10:30:00",
  "status": 404,
  "error": "Not Found",
  "message": "Schema not found for subject: user-profile and version: 999",
  "path": "/api/schemas/user-profile/999"
}
```

### 409 Conflict
Schema compatibility conflict.

```json
{
  "timestamp": "2024-01-15T10:30:00",
  "status": 409,
  "error": "Conflict",
  "message": "Schema is not compatible with existing versions",
  "path": "/api/schemas/user-profile/compat"
}
```

### 500 Internal Server Error
Unexpected server error.

```json
{
  "timestamp": "2024-01-15T10:30:00",
  "status": 500,
  "error": "Internal Server Error",
  "message": "An unexpected error occurred",
  "path": "/api/transform/mobile-app"
}
```

---

## Compatibility Types

The following compatibility types are supported:

- `BACKWARD`: New schema can read data written by old schema
- `FORWARD`: Old schema can read data written by new schema
- `FULL`: Both backward and forward compatibility
- `NONE`: No compatibility checking

---

## Transformation Engines

The service supports multiple transformation engines for different use cases:

### JSLT Engine (`jslt`)
- **Purpose**: Simple JSON-to-JSON transformations using JSLT expressions
- **Use Case**: Field mapping, data normalization, simple transformations
- **Configuration**: Single JSLT expression string
- **Example**: `{ "user_id": .id, "full_name": (.firstName + " " + .lastName) }`

### Router Engine (`router`)
- **Purpose**: Intelligent routing based on input data characteristics
- **Use Case**: Multi-tenant applications, content-based routing, conditional processing
- **Configuration**: JSON object with routing rules and conditions
- **Features**:
  - JSON path-based condition evaluation
  - Multiple routing rules with priorities
  - Default fallback transformation
  - Schema validation support

### Pipeline Engine (`pipeline`)
- **Purpose**: Sequential execution of multiple transformations
- **Use Case**: Complex multi-step processing, data enrichment workflows
- **Configuration**: JSON object with ordered transformation steps
- **Features**:
  - Configurable error handling per step
  - Result accumulation across steps
  - Intermediate schema validation
  - Step-by-step processing control

### Engine Selection Guide

| Use Case | Recommended Engine | Reason |
|----------|-------------------|---------|
| Simple field mapping | `jslt` | Most efficient for basic transformations |
| Multi-tenant data routing | `router` | Routes different data types to appropriate handlers |
| Complex data processing | `pipeline` | Supports multi-step workflows with error handling |
| Conditional transformations | `router` | Evaluates conditions to select transformation paths |

### Configuration Validation

All engines include comprehensive configuration validation:

- **JSON Schema Validation**: Router and pipeline configurations are validated against predefined schemas
- **Expression Validation**: JSLT expressions are compiled and validated before storage
- **Structural Validation**: Ensures required fields and proper data types
- **Runtime Safety**: Prevents invalid configurations from being deployed

For more information about JSLT syntax, visit: https://github.com/schibsted/jslt