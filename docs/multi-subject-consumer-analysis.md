# Multi-Subject Consumer Support Analysis

## Overview
This document analyzes two approaches for enabling consumers to handle multiple subjects in the JSON Schema Registry and Transformation Service.

## Problem Statement
Currently, each consumer can have only one transformation template. The requirement is to allow consumers to consume multiple subjects, each potentially requiring different transformation logic.

## Approach 1: Subject-in-Request (Recommended for Simplicity)

### Architecture
- Add `subject` field to transformation requests and templates
- Allow multiple templates per consumer (one per subject)
- Database: Add `subject` column to `transformation_templates` with composite unique constraint on `(consumer_id, subject)`

### API Structure

#### Core Transformation Endpoint
```
POST /api/transform/{consumerId}
```

#### Template Management
```
GET /api/transform/templates/{consumerId}/{subject}
POST /api/transform/templates/{consumerId}/{subject}
```

### Call Flow
1. Client sends transformation request with subject specified
2. Service looks up template by consumerId + subject
3. Applies transformation and returns result

### Practical Examples

#### Transformation Request
```json
POST /api/transform/mobile-app
{
  "subject": "user-events",
  "canonicalJson": {
    "eventType": "user_created",
    "userId": "12345",
    "timestamp": "2024-01-15T10:30:00Z",
    "data": {
      "firstName": "John",
      "lastName": "Doe",
      "email": "john.doe@example.com"
    }
  }
}
```

#### Transformation Response
```json
{
  "transformedJson": {
    "id": "12345",
    "full_name": "John Doe",
    "email": "john.doe@example.com",
    "event_timestamp": "2024-01-15T10:30:00Z",
    "processed_by": "mobile-app"
  }
}
```

#### Template Creation
```json
POST /api/transform/templates/mobile-app/user-events
{
  "engine": "jslt",
  "expression": ". | {id: .userId, full_name: (.firstName + ' ' + .lastName), email: .email, event_timestamp: .timestamp}",
  "description": "User event transformation for mobile app"
}
```

### Multi-Subject Consumer Example
Consumer "mobile-app" handles three subjects:

```bash
# User events
curl -X POST /api/transform/mobile-app \
  -d '{"subject":"user-events", "canonicalJson":{...}}'

# Product events
curl -X POST /api/transform/mobile-app \
  -d '{"subject":"product-events", "canonicalJson":{...}}'

# Order events
curl -X POST /api/transform/mobile-app \
  -d '{"subject":"order-events", "canonicalJson":{...}}'
```

## Approach 2: Decoupled Transformations

### Architecture
- Transformation templates are standalone entities
- Many-to-many relationship between consumers and subjects via separate table
- Relationship table contains transformation ID as the link

### API Structure

#### Lookup Endpoint (New)
```
GET /api/consumers/{consumerId}/subjects/{subject}/transformation
```

#### Transformation Endpoint
```
POST /api/transform/{transformationId}
```

#### Relationship Management
```
POST /api/consumer-subjects
```

### Call Flow
1. Client queries which transformation applies to consumer+subject
2. Client calls transformation with the returned ID
3. Service applies transformation

### Practical Examples

#### Relationship Setup
```json
POST /api/consumer-subjects
{
  "consumerId": "mobile-app",
  "subject": "user-events",
  "transformationId": "123"
}
```

#### Lookup Call
```json
GET /api/consumers/mobile-app/subjects/user-events/transformation
Response: {
  "transformationId": "123",
  "engine": "jslt"
}
```

#### Transformation Call
```json
POST /api/transform/123
{
  "canonicalJson": {
    "userId": "12345",
    "firstName": "John",
    "lastName": "Doe"
  }
}
```

## Comparison Table

| Aspect | Subject-in-Request | Decoupled Transformations |
|--------|-------------------|---------------------------|
| **API Calls per Transformation** | 1 | 2 |
| **Client Complexity** | Low | Medium |
| **Flexibility** | Medium | High |
| **Performance** | Better | Worse |
| **Discoverability** | Clear | Complex |
| **Reusability** | Low | High |
| **Maintenance** | Simpler | More complex |
| **Evolution Support** | Good | Excellent |

## Recommendation
For the current requirements, **Subject-in-Request** provides the best balance of simplicity and functionality. It enables multi-subject consumers with minimal API changes and client complexity.

The **Decoupled Transformations** approach offers superior long-term flexibility for complex relationship management but increases operational complexity.

## Implementation Impact
- **Subject-in-Request**: Changes to DTOs, entities, repository, service, and controller
- **Decoupled**: More extensive changes including new endpoints and relationship table
- Both approaches require database migration and documentation updates</content>
</xai:function_call