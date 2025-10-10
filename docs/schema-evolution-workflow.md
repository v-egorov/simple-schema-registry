# Schema Evolution Workflow Guide

This guide provides a comprehensive walkthrough of the typical schema evolution workflow in the JSON Schema Registry and Transformation Service. It covers registering canonical schemas, managing consumers, creating transformation templates, and handling schema evolution scenarios.

## Table of Contents

- [Introduction to Schema Evolution](#introduction-to-schema-evolution)
- [Step 1: Register Canonical Schema](#step-1-register-canonical-schema)
- [Step 2: Register Consumers](#step-2-register-consumers)
- [Step 3: Create Transformation Templates](#step-3-create-transformation-templates)
- [Step 4: Schema Evolution Examples](#step-4-schema-evolution-examples)
- [Best Practices](#best-practices)
- [Resources and References](#resources-and-references)

## Introduction to Schema Evolution

Schema evolution is the process of managing changes to data structures over time while maintaining compatibility with existing systems. In a microservices architecture, different services may require different views of the same canonical data.

### Key Concepts

- **Canonical Schema**: The authoritative, source-of-truth schema for your data
- **Consumer**: An application or service that consumes transformed data
- **Transformation Template**: JSLT expressions that transform canonical data into consumer-specific formats
- **Compatibility**: Ensures new schema versions don't break existing consumers

### Workflow Overview

1. **Define Canonical Schema**: Register the master schema for your data
2. **Register Consumers**: Add applications that need transformed data
3. **Create Templates**: Define how to transform data for each consumer
4. **Evolve Schemas**: Update schemas while maintaining compatibility
5. **Monitor & Maintain**: Track usage and performance

## Step 1: Register Canonical Schema

The first step is registering your canonical (master) schema. This establishes the baseline structure for your data.

### API Endpoint

```
POST /api/schemas
```

### Example: User Profile Schema

**Request:**

```bash
curl -X POST http://localhost:8080/api/schemas \
  -H "Content-Type: application/json" \
  -d '{
    "subject": "user-profile",
    "schema": {
      "$schema": "https://json-schema.org/draft/2020-12/schema",
      "type": "object",
      "properties": {
        "userId": {
          "type": "string",
          "description": "Unique user identifier"
        },
        "fullName": {
          "type": "string",
          "description": "User full name"
        },
        "email": {
          "type": "string",
          "format": "email",
          "description": "User email address"
        },
        "createdAt": {
          "type": "string",
          "format": "date-time",
          "description": "Account creation timestamp"
        }
      },
      "required": ["userId", "email"]
    },
    "compatibility": "BACKWARD"
  }'
```

**Response:**

```json
{
  "subject": "user-profile",
  "version": 1,
  "id": 1,
  "schema": {
    "$schema": "https://json-schema.org/draft/2020-12/schema",
    "type": "object",
    "properties": {
      "userId": { "type": "string", "description": "Unique user identifier" },
      "fullName": { "type": "string", "description": "User full name" },
      "email": {
        "type": "string",
        "format": "email",
        "description": "User email address"
      },
      "createdAt": {
        "type": "string",
        "format": "date-time",
        "description": "Account creation timestamp"
      }
    },
    "required": ["userId", "email"]
  },
  "compatibility": "BACKWARD",
  "createdAt": "2025-10-10T19:54:43.123456Z"
}
```

### Compatibility Modes

- **BACKWARD**: New schema can read old data
- **FORWARD**: Old schema can read new data
- **FULL**: Both backward and forward compatible
- **NONE**: No compatibility guarantees

## Step 2: Register Consumers

Consumers represent applications or services that need data in specific formats. Each consumer can have its own transformation requirements.

### API Endpoint

```
POST /api/consumers
```

### Example: Mobile App Consumer

**Request:**

```bash
curl -X POST http://localhost:8080/api/consumers \
  -H "Content-Type: application/json" \
  -d '{
    "consumerId": "mobile-app-v1",
    "name": "Mobile Application v1.0",
    "description": "iOS/Android mobile app consumer"
  }'
```

**Response:**

```json
{
  "consumerId": "mobile-app-v1",
  "name": "Mobile Application v1.0",
  "description": "iOS/Android mobile app consumer",
  "createdAt": "2025-10-10T19:54:43.456789Z",
  "updatedAt": "2025-10-10T19:54:43.456789Z"
}
```

### Example: Analytics Consumer

**Request:**

```bash
curl -X POST http://localhost:8080/api/consumers \
  -H "Content-Type: application/json" \
  -d '{
    "consumerId": "analytics-service",
    "name": "Analytics Service",
    "description": "Data warehouse and analytics platform"
  }'
```

## Step 3: Create Transformation Templates

Transformation templates define how to convert canonical data into consumer-specific formats using JSLT expressions.

### API Endpoint

```
POST /api/transform/templates/{consumerId}
```

### Example: Mobile App Template

For mobile apps, we might want a simplified view with camelCase field names:

**Request:**

```bash
curl -X POST http://localhost:8080/api/transform/templates/mobile-app-v1 \
  -H "Content-Type: application/json" \
  -d '{
    "expression": "{ \"userId\": .userId, \"fullName\": .fullName, \"email\": .email }",
    "engine": "jslt"
  }'
```

**Response:**

```json
{
  "consumerId": "mobile-app-v1",
  "expression": "{ \"userId\": .userId, \"fullName\": .fullName, \"email\": .email }",
  "engine": "jslt",
  "createdAt": "2025-10-10T19:54:43.789012Z",
  "updatedAt": "2025-10-10T19:54:43.789012Z"
}
```

### Example: Analytics Template

For analytics, we might want to flatten and add metadata:

**Request:**

```bash
curl -X POST http://localhost:8080/api/transform/templates/analytics-service \
  -H "Content-Type: application/json" \
  -d '{
    "expression": "{ \"user_id\": .userId, \"name\": .fullName, \"email_domain\": (.email | split(\"@\") | [1]), \"signup_date\": .createdAt, \"data_source\": \"user_profile\" }",
    "engine": "jslt"
  }'
```

## Step 4: Schema Evolution Examples

### Example 1: Adding Optional Fields

**Scenario**: Add an optional `phoneNumber` field to the user profile.

**New Schema (Version 2):**

```bash
curl -X POST http://localhost:8080/api/schemas \
  -H "Content-Type: application/json" \
  -d '{
    "subject": "user-profile",
    "schema": {
      "$schema": "https://json-schema.org/draft/2020-12/schema",
      "type": "object",
      "properties": {
        "userId": {"type": "string", "description": "Unique user identifier"},
        "fullName": {"type": "string", "description": "User full name"},
        "email": {"type": "string", "format": "email", "description": "User email address"},
        "phoneNumber": {"type": "string", "description": "User phone number"},
        "createdAt": {"type": "string", "format": "date-time", "description": "Account creation timestamp"}
      },
      "required": ["userId", "email"]
    },
    "compatibility": "BACKWARD"
  }'
```

**Compatibility Check:**

```bash
curl -X POST http://localhost:8080/api/schemas/user-profile/compat \
  -H "Content-Type: application/json" \
  -d '{
    "schema": {
      "$schema": "https://json-schema.org/draft/2020-12/schema",
      "type": "object",
      "properties": {
        "userId": {"type": "string"},
        "fullName": {"type": "string"},
        "email": {"type": "string", "format": "email"},
        "phoneNumber": {"type": "string"},
        "createdAt": {"type": "string", "format": "date-time"}
      },
      "required": ["userId", "email"]
    }
  }'
```

### Example 2: Consumer-Specific Evolution

**Scenario**: Mobile app needs a new field in its transformed data.

**Update Mobile App Template:**

```bash
curl -X POST http://localhost:8080/api/transform/templates/mobile-app-v1 \
  -H "Content-Type: application/json" \
  -d '{
    "expression": "{ \"userId\": .userId, \"fullName\": .fullName, \"email\": .email, \"hasPhone\": (.phoneNumber != null) }",
    "engine": "jslt"
  }'
```

### Example 3: Breaking Changes with New Consumer

**Scenario**: Create a new consumer for a different mobile app version that requires different data structure.

**Register New Consumer:**

```bash
curl -X POST http://localhost:8080/api/consumers \
  -H "Content-Type: application/json" \
  -d '{
    "consumerId": "mobile-app-v2",
    "name": "Mobile Application v2.0",
    "description": "Updated mobile app with new data requirements"
  }'
```

**Create New Template:**

```bash
curl -X POST http://localhost:8080/api/transform/templates/mobile-app-v2 \
  -H "Content-Type: application/json" \
  -d '{
    "expression": "{ \"id\": .userId, \"displayName\": .fullName, \"contact\": { \"email\": .email, \"phone\": .phoneNumber } }",
    "engine": "jslt"
  }'
```

## Best Practices

### Schema Design

- Use semantic versioning for major changes
- Prefer backward-compatible changes
- Document schema changes thoroughly
- Validate schemas against JSON Schema specification

### Consumer Management

- Use descriptive consumer IDs
- Group related consumers logically
- Monitor consumer usage patterns
- Plan for consumer deprecation

### Transformation Templates

- Keep templates simple and readable
- Test transformations with sample data
- Use JSLT built-in functions effectively
- Document transformation logic

### Evolution Strategy

- Test compatibility before deployment
- Have rollback plans for breaking changes
- Communicate changes to consumer teams
- Monitor for data quality issues

## Resources and References

### JSON Schema

- [JSON Schema Specification](https://json-schema.org/specification.html) - Official specification
- [Understanding JSON Schema](https://json-schema.org/understanding-json-schema/) - Comprehensive guide
- [JSON Schema Validator](https://www.jsonschemavalidator.net/) - Online validation tool

### JSLT (JSON Schema Language for Transformations)

- [JSLT Documentation](https://github.com/schibsted/jslt) - Official documentation
- [JSLT Playground](https://jslt.schibsted.com/) - Interactive testing tool
- [JSLT Examples](https://github.com/schibsted/jslt/tree/master/examples) - Code examples

### Schema Registry Patterns

- [Confluent Schema Registry](https://docs.confluent.io/platform/current/schema-registry/index.html) - Industry standard
- [Schema Evolution Best Practices](https://www.confluent.io/blog/schema-evolution-in-apache-avro/) - Evolution strategies
- [Event-Driven Architecture](https://microservices.io/patterns/data/event-driven-architecture.html) - Architecture patterns

### Tools and Utilities

- [jq](https://stedolan.github.io/jq/) - JSON processing command-line tool
- [JSONPath Online Evaluator](https://jsonpath.com/) - Test JSON path expressions
- [Postman](https://www.postman.com/) - API testing and documentation

### Additional Reading

- [Designing Data-Intensive Applications](https://dataintensive.net/) - Book on data system design
- [Building Microservices](https://samnewman.io/books/building_microservices/) - Microservices architecture
- [Domain-Driven Design](https://dddcommunity.org/book/evans_2003/) - Strategic design patterns

