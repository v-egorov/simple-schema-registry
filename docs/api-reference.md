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

## API Design Principles

### Parameter Location Strategy

This API follows a deliberate design pattern where **resource identifiers appear in both URL paths and request bodies**. This approach provides enhanced security, validation, and API clarity.

#### Why Parameters in Both Path and Body?

**Security & Validation Benefits:**

- **URL Tampering Prevention**: Path parameters can be manipulated by users or intermediaries. Cross-validation ensures the path parameter matches the validated request body.
- **Input Sanitization**: Request body parameters undergo full validation (including `@NotBlank`, `@Valid`, etc.) while path parameters are treated as potentially untrusted.
- **Consistency Checks**: Prevents processing operations on the wrong resource due to URL manipulation or copy-paste errors.

**API Design Benefits:**

- **Explicit Intent**: Makes the target resource clear in both routing (URL) and data (body) contexts.
- **Documentation Clarity**: Self-documenting API where the operation target is visible in multiple places.
- **Future Extensibility**: Allows for potential differences between URL identifiers and body data in future API versions.

#### REST API Best Practices Reference

This pattern aligns with several established REST API design guidelines:

- **OWASP API Security Guidelines**: "Validate all input from all sources including path parameters"
- **Microsoft REST API Guidelines**: "Resource identifiers should be validated from multiple sources when possible"
- **Google API Design Guide**: "Use path parameters for resource identification, but validate against request body for critical operations"

#### Example Implementation

```java
@PostMapping("/schemas/{subject}")
public ResponseEntity<SchemaResponse> registerSchema(
    @PathVariable String subject,  // From URL - untrusted
    @Valid @RequestBody SchemaRegistrationRequest request) {  // Fully validated

    // Cross-validation: Ensure they match
    if (!subject.equals(request.getSubject())) {
        return ResponseEntity.badRequest().build();
    }

    // Process with validated data
    return processSchema(request);
}
```

**Request Example:**

```http
POST /api/schemas/user-profile
Content-Type: application/json

{
  "subject": "user-profile",  // Must match URL path
  "schema": { ... },
  "compatibility": "BACKWARD"
}
```

**Error Response (400 Bad Request):**

```json
{
  "timestamp": "2024-01-15T10:30:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Subject in path 'user-profile' does not match subject in request body 'different-subject'",
  "path": "/api/schemas/user-profile"
}
```

#### Affected Endpoints

This pattern is used for critical resource operations where identifier validation is essential:

- Schema Registration: `POST /api/schemas/{subject}`
- Schema Compatibility Check: `POST /api/schemas/{subject}/compat`
- Schema Validation: `POST /api/schemas/{subject}/validate`
- Consumer Schema Operations: `POST /api/consumers/{consumerId}/schemas/{subject}`
- Transformation Operations: `POST /api/consumers/{consumerId}/subjects/{subject}/transform`

---

## Schema Registry API

### Register Schema

Register a new schema or create a new version of an existing schema.

**Endpoint**: `POST /api/schemas/{subject}`

**Supported JSON Schema Versions**: The service supports draft-04, draft-06, draft-07, and draft-2019-09. The schema version is automatically detected from the `$schema` field. If no `$schema` field is present, draft-04 is used as the default. Note: draft-2020-12 is not supported due to validation bugs in the NetworkNT library.

**Request Body**:

```json
{
  "subject": "user-profile",
  "schema": {
    "$schema": "http://json-schema.org/draft-07/schema#",
    "type": "object",
    "properties": {
      "id": { "type": "integer" },
      "name": { "type": "string" },
      "email": { "type": "string", "format": "email" }
    },
    "required": ["id", "name"]
  },
  "compatibility": "BACKWARD",
  "description": "User profile schema for version 1.0.0",
  "version": "1.0.0" // Optional: specify version for registration. If not provided, auto-assigns next patch version
}
```

**Response** (201 Created):

```json
{
  "id": 1,
  "subject": "user-profile",
  "version": "1.0.0",
  "schema": {
    "$schema": "http://json-schema.org/draft-07/schema#",
    "type": "object",
    "properties": {
      "id": { "type": "integer" },
      "name": { "type": "string" },
      "email": { "type": "string", "format": "email" }
    },
    "required": ["id", "name"]
  },
  "compatibility": "BACKWARD",
  "description": "User profile schema for version 1.0.0",
  "createdAt": "2024-01-15T10:30:00",
  "updatedAt": "2024-01-15T10:30:00"
}
```

### Get All Schema Versions

Retrieve all versions of a schema by subject.

**Endpoint**: `GET /api/schemas/{subject}/versions`

**Response** (200 OK):

```json
[
  {
    "id": 1,
    "subject": "user-profile",
    "version": "1.0.0",
    "schema": {...},
    "compatibility": "BACKWARD",
    "description": "User profile schema for version 1.0.0",
    "createdAt": "2024-01-15T10:30:00",
    "updatedAt": "2024-01-15T10:30:00"
  },
  {
    "id": 2,
    "subject": "user-profile",
    "version": "2.0.0",
    "schema": {...},
    "compatibility": "BACKWARD",
    "description": "User profile schema for version 2.0.0",
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
  "version": "2.0.0",
  "schema": {...},
  "compatibility": "BACKWARD",
  "description": "User profile schema for version 2.0.0",
  "createdAt": "2024-01-15T11:00:00",
  "updatedAt": "2024-01-15T11:00:00"
}
```

### Get Latest Schema Version

Retrieve the latest version of a schema.

**Endpoint**: `GET /api/schemas/{subject}`

**Response** (200 OK):

```json
{
  "id": 2,
  "subject": "user-profile",
  "version": "2.0.0",
  "schema": {...},
  "compatibility": "BACKWARD",
  "description": "User profile schema for version 2.0.0",
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
      "id": { "type": "integer" },
      "name": { "type": "string" },
      "email": { "type": "string", "format": "email" },
      "phone": { "type": "string" }
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
["user-profile", "product-catalog", "order-data"]
```

### Validate JSON Against Schema

Validate JSON data against the latest version of a schema or a specific version.

**Endpoint**: `POST /api/schemas/{subject}/validate`

**Request Body**:

```json
{
  "subject": "user-profile",
  "jsonData": {
    "id": 123,
    "name": "John Doe",
    "email": "john@example.com"
  },
  "version": "1.0.0" // Optional - if not provided, uses latest version
}
```

**Response** (200 OK) - Valid:

```json
{
  "valid": true,
  "subject": "user-profile",
  "schemaVersion": "1.0.0"
}
```

**Response** (200 OK) - Invalid:

```json
{
  "valid": false,
  "subject": "user-profile",
  "schemaVersion": "1.0.0",
  "errors": [
    "$.email: does not match the email pattern",
    "$.name: is missing but it is required"
  ]
}
```

**Error Responses**:

- `404 Not Found`: Schema subject not found
- `400 Bad Request`: Invalid request format or subject mismatch

---

## Consumer Schema API

### Register Consumer Schema

Register a new consumer output schema or create a new version for a specific consumer.

**Endpoint**: `POST /api/consumers/{consumerId}/schemas/{subject}`

**Request Body**:

```json
{
  "subject": "user-profile",
  "schema": {
    "$schema": "http://json-schema.org/draft-07/schema#",
    "type": "object",
    "properties": {
      "id": { "type": "integer" },
      "name": { "type": "string" },
      "email": { "type": "string", "format": "email" }
    },
    "required": ["id", "name"]
  },
  "compatibility": "BACKWARD",
  "description": "Consumer output schema for mobile app",
  "version": "1.0.0" // Optional: specify version for registration. If not provided, auto-assigns next patch version
}
```

**Response** (201 Created):

```json
{
  "id": 2,
  "subject": "user-profile",
  "version": "1.0.0",
  "schema": {
    "$schema": "http://json-schema.org/draft-07/schema#",
    "type": "object",
    "properties": {
      "id": { "type": "integer" },
      "name": { "type": "string" },
      "email": { "type": "string", "format": "email" }
    },
    "required": ["id", "name"]
  },
  "compatibility": "BACKWARD",
  "description": "Consumer output schema for mobile app",
  "createdAt": "2024-01-15T10:30:00",
  "updatedAt": "2024-01-15T10:30:00"
}
```

### Get All Consumer Schema Versions

Retrieve all versions of consumer output schemas for a subject and consumer.

**Endpoint**: `GET /api/consumers/{consumerId}/schemas/{subject}/versions`

**Response** (200 OK):

```json
[
  {
    "id": 2,
    "subject": "user-profile",
    "version": "1.0.0",
    "schema": {...},
    "compatibility": "BACKWARD",
    "description": "Consumer output schema for mobile app",
    "createdAt": "2024-01-15T10:30:00",
    "updatedAt": "2024-01-15T10:30:00"
  }
]
```

### Get Specific Consumer Schema Version

Retrieve a specific version of a consumer output schema.

**Endpoint**: `GET /api/consumers/{consumerId}/schemas/{subject}/versions/{version}`

**Response** (200 OK):

```json
{
  "id": 2,
  "subject": "user-profile",
  "version": "1.0.0",
  "schema": {...},
  "compatibility": "BACKWARD",
  "description": "Consumer output schema for mobile app",
  "createdAt": "2024-01-15T10:30:00",
  "updatedAt": "2024-01-15T10:30:00"
}
```

### Get Latest Consumer Schema Version

Retrieve the latest version of a consumer output schema.

**Endpoint**: `GET /api/consumers/{consumerId}/schemas/{subject}`

**Response** (200 OK):

```json
{
  "id": 2,
  "subject": "user-profile",
  "version": "1.0.0",
  "schema": {...},
  "compatibility": "BACKWARD",
  "description": "Consumer output schema for mobile app",
  "createdAt": "2024-01-15T10:30:00",
  "updatedAt": "2024-01-15T10:30:00"
}
```

### Check Consumer Schema Compatibility

Check if a new consumer output schema is compatible with existing versions.

**Endpoint**: `POST /api/consumers/{consumerId}/schemas/{subject}/compat`

**Request Body**:

```json
{
  "subject": "user-profile",
  "schema": {
    "$schema": "http://json-schema.org/draft-07/schema#",
    "type": "object",
    "properties": {
      "id": { "type": "integer" },
      "name": { "type": "string" },
      "email": { "type": "string", "format": "email" },
      "phone": { "type": "string" }
    },
    "required": ["id", "name"]
  }
}
```

**Response** (200 OK):

```json
{
  "compatible": true,
  "message": "Consumer output schema is compatible with all existing versions"
}
```

### Validate JSON Against Consumer Schema

Validate JSON data against the latest version or a specific version of a consumer output schema.

**Endpoint**: `POST /api/consumers/{consumerId}/schemas/{subject}/validate`

**Request Body**:

```json
{
  "subject": "user-profile",
  "jsonData": {
    "id": 123,
    "name": "John Doe",
    "email": "john@example.com"
  },
  "version": "1.0.0" // Optional - if not provided, uses latest version
}
```

**Response** (200 OK) - Valid:

```json
{
  "valid": true,
  "subject": "user-profile",
  "schemaVersion": "1.0.0"
}
```

**Response** (200 OK) - Invalid:

```json
{
  "valid": false,
  "subject": "user-profile",
  "schemaVersion": "1.0.0",
  "errors": [
    "$.email: does not match the email pattern",
    "$.name: is missing but it is required"
  ]
}
```

### List Consumer Schema Subjects

Retrieve all unique subjects that have consumer output schemas for a specific consumer.

**Endpoint**: `GET /api/consumers/{consumerId}/schemas/subjects`

**Response** (200 OK):

```json
["user-profile", "product-catalog", "order-data"]
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

Transform canonical JSON data for a specific consumer and subject using their active transformation template.

**Endpoint**: `POST /api/consumers/{consumerId}/subjects/{subject}/transform`

**Request Body**:

```json
{
  "subject": "user-profile",
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
  },
  "subject": "user-profile"
}
```

### Transform JSON Data with Specific Version

Transform canonical JSON data using a specific transformation template version.

**Endpoint**: `POST /api/consumers/{consumerId}/subjects/{subject}/transform/versions/{version}`

**Request Body**:

```json
{
  "subject": "user-profile",
  "transformationVersion": "1.0.0",
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
  },
  "subject": "user-profile"
}
```

### Create Transformation Template Version

Create a new version of a transformation template for a consumer and subject.

**Endpoint**: `POST /api/consumers/{consumerId}/subjects/{subject}/templates`

#### JSLT Engine Example

**Request Body**:

```json
{
  "version": "1.0.0",
  "engine": "jslt",
  "expression": ". | {id: .userId, name: .fullName, email: .emailAddress, registered: .registrationDate, status: .accountStatus}",
  "inputSchema": {
    "subject": "user-profile",
    "version": "1.0.0"
  },
  "outputSchema": {
    "subject": "user-profile",
    "consumerId": "mobile-app",
    "version": "1.0.0"
  },
  "description": "Simple field mapping for mobile app"
}
```

**Response** (201 Created):

```json
{
  "id": 1,
  "consumerId": "mobile-app",
  "subject": "user-profile",
  "version": "1.0.0",
  "engine": "jslt",
  "inputSchema": 1,
  "outputSchema": 2,
  "isActive": true,
  "templateExpression": ". | {id: .userId, name: .fullName, email: .emailAddress, registered: .registrationDate, status: .accountStatus}",
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
  "version": "1.0.0",
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
      "defaultTransformationId": "generic-transformation-v1",
    ],
    "validation": {
      "inputSchema": "canonical-data-schema-v1",
      "outputSchema": "consumer-data-schema-v1"
    }
  },
  "inputSchema": {
    "subject": "user-profile",
    "version": "1.0.0"
  },
  "outputSchema": {
    "subject": "user-profile",
    "consumerId": "multi-tenant-app",
    "version": "1.0.0"
  },
  "description": "Content-based routing for different data types"
}
```

#### Pipeline Engine Example

**Request Body**:

```json
{
  "version": "1.0.0",
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
  "inputSchema": {
    "subject": "user-profile",
    "version": "1.0.0"
  },
  "outputSchema": {
    "subject": "user-profile",
    "consumerId": "analytics-platform",
    "version": "1.0.0"
  },
  "description": "Multi-step data processing pipeline"
}
```

### Get All Template Versions

Retrieve all versions of transformation templates for a consumer and subject.

**Endpoint**: `GET /api/consumers/{consumerId}/subjects/{subject}/templates`

**Response** (200 OK):

```json
[
  {
  "id": 1,
  "consumerId": "mobile-app",
  "subject": "user-profile",
  "version": "1.0.0",
  "engine": "jslt",
  "inputSchema": {
    "subject": "user-profile",
    "version": "1.0.0"
  },
  "outputSchema": {
    "subject": "user-profile",
    "consumerId": "mobile-app",
    "version": "1.0.0"
  },
    "isActive": true,
    "templateExpression": ". | {id: .userId, name: .fullName, email: .emailAddress, registered: .registrationDate, status: .accountStatus}",
    "configuration": null,
    "description": "Simple field mapping for mobile app",
    "createdAt": "2024-01-15T10:35:00",
    "updatedAt": "2024-01-15T10:35:00"
  }
]
```

### Get Active Template

Retrieve the currently active transformation template for a consumer and subject.

**Endpoint**: `GET /api/consumers/{consumerId}/subjects/{subject}/templates/active`

**Response** (200 OK):

```json
{
  "id": 1,
  "consumerId": "mobile-app",
  "subject": "user-profile",
  "version": "1.0.0",
  "engine": "jslt",
  "inputSchema": 1,
  "outputSchema": 2,
  "isActive": true,
  "templateExpression": ". | {id: .userId, name: .fullName, email: .emailAddress, registered: .registrationDate, status: .accountStatus}",
  "configuration": null,
  "description": "Simple field mapping for mobile app",
  "createdAt": "2024-01-15T10:35:00",
  "updatedAt": "2024-01-15T10:35:00"
}
```

### Get Specific Template Version

Retrieve a specific version of a transformation template.

**Endpoint**: `GET /api/consumers/{consumerId}/subjects/{subject}/templates/versions/{version}`

**Response** (200 OK):

```json
{
  "id": 1,
  "consumerId": "mobile-app",
  "subject": "user-profile",
  "version": "1.0.0",
  "engine": "jslt",
  "inputSchema": 1,
  "outputSchema": 2,
  "isActive": true,
  "templateExpression": ". | {id: .userId, name: .fullName, email: .emailAddress, registered: .registrationDate, status: .accountStatus}",
  "configuration": null,
  "description": "Simple field mapping for mobile app",
  "createdAt": "2024-01-15T10:35:00",
  "updatedAt": "2024-01-15T10:35:00"
}
```

### Activate Template Version

Activate a specific version of a transformation template.

**Endpoint**: `PUT /api/consumers/{consumerId}/subjects/{subject}/templates/versions/{version}/activate`

#### Activation Logic and Flow

The activation process follows these steps:

1. **Version Validation**: Checks if the requested transformation template version exists for the specified consumer and subject pair. Returns 404 if not found.

2. **Active Status Check**: If the requested version is already active (`isActive = true`), returns the current template without changes.

3. **Current Version Deactivation**: If a different version is currently active:
   - Sets the currently active version's `isActive` flag to `false`
   - Uses `saveAndFlush()` to ensure immediate database commit
   - This prevents constraint violations during the activation step

4. **New Version Activation**: Sets the requested version's `isActive` flag to `true` and saves the changes.

5. **Database Constraints**: A unique partial index ensures only one version can be active per consumer-subject pair at the database level.

#### New Version Creation and Activation Flow

When creating transformation templates:

1. **First Template Auto-Activation**: The very first template created for a consumer-subject pair is automatically activated

2. **Subsequent Templates Inactive**: All additional versions for the same consumer-subject pair are created as inactive by default

3. **Manual Activation Required**: New versions (after the first) require explicit activation via the activate endpoint

4. **Reasoning for Manual Activation** (not Auto-Activation):
   - **Safety**: Prevents accidental activation of untested or incomplete transformations
   - **Control**: Allows operators to validate transformations before making them active
   - **Rollback Capability**: Easy to switch between versions without creating new ones
   - **Audit Trail**: Clear record of when versions were activated for compliance
   - **Testing**: Allows staging and testing of new versions before production activation

4. **Version Management**: Operators can maintain multiple versions and switch between them as needed for different scenarios (A/B testing, gradual rollouts, emergency rollbacks).

**Response** (200 OK):

```json
{
  "id": 2,
  "consumerId": "mobile-app",
  "subject": "user-profile",
  "version": "2.0.0",
  "engine": "jslt",
  "inputSchema": {
    "subject": "user-profile",
    "version": "1.0.0"
  },
  "outputSchema": {
    "subject": "user-profile",
    "consumerId": "mobile-app",
    "version": "1.0.0"
  },
  "isActive": true,
  "templateExpression": ". | {id: .userId, name: .fullName, email: .emailAddress, registered: .registrationDate, status: .accountStatus, lastLogin: .lastLoginDate}",
  "configuration": null,
  "description": "Updated field mapping with last login",
  "createdAt": "2024-01-15T11:00:00",
  "updatedAt": "2024-01-15T11:00:00"
}
```

**Error Responses**:

- **404 Not Found**: When the specified transformation template version doesn't exist
  ```json
  {
    "timestamp": "2024-01-15T11:00:00",
    "status": 404,
    "error": "Not Found",
    "message": "Transformation template version not found: consumer=mobile-app, subject=user-profile, version=3.0.0",
    "path": "/api/consumers/mobile-app/subjects/user-profile/templates/versions/3.0.0/activate"
  }
  ```

### Deactivate Template Version

Deactivate a specific version of a transformation template.

**Endpoint**: `PUT /api/consumers/{consumerId}/subjects/{subject}/templates/versions/{version}/deactivate`

**Response** (200 OK):

```json
{
  "id": 1,
  "consumerId": "mobile-app",
  "subject": "user-profile",
  "version": "1.0.0",
  "engine": "jslt",
  "inputSchema": 1,
  "outputSchema": 2,
  "isActive": false,
  "templateExpression": ". | {id: .userId, name: .fullName, email: .emailAddress, registered: .registrationDate, status: .accountStatus}",
  "configuration": null,
  "description": "Simple field mapping for mobile app",
  "createdAt": "2024-01-15T10:35:00",
  "updatedAt": "2024-01-15T11:00:00"
}
```

### Delete Template Version

Delete a specific version of a transformation template (only if not active).

**Endpoint**: `DELETE /api/consumers/{consumerId}/subjects/{subject}/templates/versions/{version}`

**Response** (204 No Content)

### List Available Transformation Engines

Get a list of supported transformation engines.

**Endpoint**: `GET /api/consumers/engines`

**Response** (200 OK):

```json
["jslt", "router", "pipeline"]
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
  "path": "/api/consumers/mobile-app/subjects/user-profile/transform"
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

| Use Case                    | Recommended Engine | Reason                                              |
| --------------------------- | ------------------ | --------------------------------------------------- |
| Simple field mapping        | `jslt`             | Most efficient for basic transformations            |
| Multi-tenant data routing   | `router`           | Routes different data types to appropriate handlers |
| Complex data processing     | `pipeline`         | Supports multi-step workflows with error handling   |
| Conditional transformations | `router`           | Evaluates conditions to select transformation paths |

### Configuration Validation

All engines include comprehensive configuration validation:

- **JSON Schema Validation**: Router and pipeline configurations are validated against predefined schemas
- **Expression Validation**: JSLT expressions are compiled and validated before storage
- **Structural Validation**: Ensures required fields and proper data types
- **Runtime Safety**: Prevents invalid configurations from being deployed

### Runtime Data Validation

Router and pipeline engines support optional runtime schema validation during data transformation to ensure data integrity and catch transformation errors early.

#### Router Engine Validation

The `validation` element enables schema validation at routing boundaries:

- `inputSchema`: Validates that incoming data conforms to the expected input schema before routing decisions are made
- `outputSchema`: Validates that the final transformed output matches the target consumer schema

#### Pipeline Engine Validation

The `validation` element provides validation checkpoints throughout the processing pipeline:

- `finalSchema`: Validates the end result of the entire pipeline against the expected output schema
- `intermediateSchemas`: Optional validation checkpoints after specific pipeline steps (e.g., "after-step-1", "after-step-2") to catch data integrity issues mid-process

This runtime validation prevents invalid data from propagating through transformations and provides early error detection in complex processing workflows.

For more information about JSLT syntax, visit: <https://github.com/schibsted/jslt>

