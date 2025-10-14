# End-to-End Example: Investment Publications Schema Registry and Transformation

This document provides a comprehensive end-to-end example demonstrating the complete workflow of the JSON Schema Registry and Transformation Service using investment publications data.

## Overview

In this example, we'll work with investment publications that contain internal notes and metadata. We'll set up a consumer (mobile app) that requires publications without the internal notes, and demonstrate the complete transformation pipeline.

## Prerequisites

- Running Schema Registry service on `http://localhost:8080`
- PostgreSQL database running (via Docker Compose)
- Sample data files in `tests/examples/investment-research/publications/`

## Step 1: Register Canonical Schema

First, we register the canonical schema for investment publications. This schema includes all fields including internal notes.

### Command

```bash
curl -X POST http://localhost:8080/api/schemas/invest-publications \
  -H "Content-Type: application/json" \
  -H "Accept: application/json" \
  -d @tests/examples/investment-research/publications/invest-publications.schema.json
```

### Expected Response (201 Created)

```json
{
  "subject": "invest-publications",
  "version": "1.0.0",
  "compatibility": "BACKWARD",
  "description": null,
  "createdAt": "2025-10-14T12:00:00Z",
  "updatedAt": "2025-10-14T12:00:00Z",
  "schema": {
    "$schema": "https://json-schema.org/draft/2020-12/schema",
    "$id": "https://example.com/schemas/invest-publications.schema.json",
    "title": "Investment Publications",
    "description": "Schema for investment analytics publications with hierarchy: publication → chapter → block → view",
    "type": "object",
    "additionalProperties": false,
    "properties": {
      "publications": {
        "type": "array",
        "minItems": 1,
        "items": { "$ref": "#/$defs/publication" }
      }
    },
    "required": ["publications"],
    "$defs": {
      // ... full schema definition
    }
  }
}
```

## Step 2: Register Consumer

Next, we register a consumer (mobile app) that will receive transformed publications without internal notes.

### Command

```bash
curl -X POST http://localhost:8080/api/consumers \
  -H "Content-Type: application/json" \
  -H "Accept: application/json" \
  -d '{
    "consumerId": "mobile-app",
    "name": "Mobile Application",
    "description": "Investment publications consumer for mobile app - requires data without internal notes"
  }'
```

### Expected Response (201 Created)

```json
{
  "consumerId": "mobile-app",
  "name": "Mobile Application",
  "description": "Investment publications consumer for mobile app - requires data without internal notes",
  "createdAt": "2025-10-14T12:00:01Z",
  "updatedAt": "2025-10-14T12:00:01Z"
}
```

## Step 3: Register Consumer Output Schema

Now we register the output schema for the mobile app consumer. This schema is the same as the canonical schema but without the "notes" fields.

### Command

```bash
curl -X POST http://localhost:8080/api/consumers/mobile-app/schemas/invest-publications \
  -H "Content-Type: application/json" \
  -H "Accept: application/json" \
  -d @tests/examples/investment-research/publications/invest-publications-no-notes.schema.json
```

### Expected Response (201 Created)

```json
{
  "subject": "invest-publications",
  "consumerId": "mobile-app",
  "version": "1.0.0",
  "compatibility": "BACKWARD",
  "description": null,
  "createdAt": "2025-10-14T12:00:02Z",
  "updatedAt": "2025-10-14T12:00:02Z",
  "schema": {
    "$id": "https://example.com/schemas/invest-publications.schema.json",
    "type": "object",
    "$defs": {
      // ... schema definition without notes fields
    },
    "title": "Investment Publications",
    "$schema": "https://json-schema.org/draft/2020-12/schema",
    "required": ["publications"],
    "properties": {
      "publications": {
        "type": "array",
        "items": {
          "$ref": "#/$defs/publication"
        },
        "minItems": 1
      }
    },
    "description": "Schema for investment analytics publications with hierarchy: publication → chapter → block → view",
    "additionalProperties": false
  }
}
```

## Step 4: Register JSLT Transformation Template

We register a JSLT transformation template that removes all "notes" fields from the publication data.

### Command

```bash
curl -X POST http://localhost:8080/api/consumers/mobile-app/subjects/invest-publications/templates \
  -H "Content-Type: application/json" \
  -H "Accept: application/json" \
  -d '{
    "version": "1.0.0",
    "engine": "jslt",
    "expression": "{\"publications\": [for (.publications) {\"id\": .id, \"title\": .title, \"subtitle\": .subtitle, \"summary\": .summary, \"type\": .type, \"targetAudience\": .targetAudience, \"status\": .status, \"version\": .version, \"language\": .language, \"authors\": .authors, \"createdAt\": .createdAt, \"updatedAt\": .updatedAt, \"publishedAt\": .publishedAt, \"metadata\": .metadata, \"chapters\": if (.chapters) [for (.chapters) {\"id\": .id, \"title\": .title, \"subtitle\": .subtitle, \"summary\": .summary, \"order\": .order, \"metadata\": .metadata, \"createdAt\": .createdAt, \"updatedAt\": .updatedAt, \"authors\": .authors, \"language\": .language, \"blocks\": if (.blocks) [for (.blocks) {\"id\": .id, \"title\": .title, \"subtitle\": .subtitle, \"summary\": .summary, \"type\": .type, \"metadata\": .metadata, \"createdAt\": .createdAt, \"updatedAt\": .updatedAt, \"authors\": .authors, \"language\": .language, \"views\": if (.views) [for (.views) {\"type\": .type, \"content\": .content, \"mediaType\": .mediaType, \"base64\": .base64, \"alt\": .alt, \"caption\": .caption, \"spec\": .spec, \"columns\": .columns, \"rows\": .rows}] else []}] else []}] else []}]}",
    "inputSchema": {
      "subject": "invest-publications"
    },
    "outputSchema": {
      "subject": "invest-publications",
      "consumerId": "mobile-app"
    },
    "description": "Remove all internal notes from investment publications for mobile app consumption"
  }'
```

### Expected Response (201 Created)

```json
{
  "consumerId": "mobile-app",
  "subject": "invest-publications",
  "version": "1.0.0",
  "engine": "jslt",
  "expression": "{\"publications\": [for (.publications) {\"id\": .id, \"title\": .title, \"subtitle\": .subtitle, \"summary\": .summary, \"type\": .type, \"targetAudience\": .targetAudience, \"status\": .status, \"version\": .version, \"language\": .language, \"authors\": .authors, \"createdAt\": .createdAt, \"updatedAt\": .updatedAt, \"publishedAt\": .publishedAt, \"metadata\": .metadata, \"chapters\": if (.chapters) [for (.chapters) {\"id\": .id, \"title\": .title, \"subtitle\": .subtitle, \"summary\": .summary, \"order\": .order, \"metadata\": .metadata, \"createdAt\": .createdAt, \"updatedAt\": .updatedAt, \"authors\": .authors, \"language\": .language, \"blocks\": if (.blocks) [for (.blocks) {\"id\": .id, \"title\": .title, \"subtitle\": .subtitle, \"summary\": .summary, \"type\": .type, \"metadata\": .metadata, \"createdAt\": .createdAt, \"updatedAt\": .updatedAt, \"authors\": .authors, \"language\": .language, \"views\": if (.views) [for (.views) {\"type\": .type, \"content\": .content, \"mediaType\": .mediaType, \"base64\": .base64, \"alt\": .alt, \"caption\": .caption, \"spec\": .spec, \"columns\": .columns, \"rows\": .rows}] else []}] else []}] else []}]}",
  "inputSchema": {
    "subject": "invest-publications"
  },
  "outputSchema": {
    "subject": "invest-publications",
    "consumerId": "mobile-app"
  },
  "description": "Remove all internal notes from investment publications for mobile app consumption",
  "createdAt": "2025-10-14T12:00:03Z",
  "updatedAt": "2025-10-14T12:00:03Z"
}
```

## Step 5: Validate Input Data Against Canonical Schema

Before transforming, we validate that our input data conforms to the canonical schema.

### Command

```bash
curl -X POST http://localhost:8080/api/schemas/invest-publications/validate \
  -H "Content-Type: application/json" \
  -H "Accept: application/json" \
  -d @tests/examples/investment-research/publications/all-elements-with-all-values.json
```

### Expected Response (200 OK)

```json
{
  "valid": true,
  "errors": []
}
```

## Step 6: Transform Data and Validate Output

Now we transform the data using the registered template and validate that the output conforms to the consumer schema.

### Command

```bash
curl -X POST http://localhost:8080/api/consumers/mobile-app/subjects/invest-publications/transform \
  -H "Content-Type: application/json" \
  -H "Accept: application/json" \
  -d '{
    "subject": "invest-publications",
    "canonicalJson": {
      "publications": [
        {
          "id": "pub_full_example_001",
          "title": "Полноценный пример публикации для тестирования",
          "subtitle": "Включены все поля и минимум 2 элемента в массивах",
          "summary": "Эта публикация создана для демонстрации всех полей схемы, с расширенными метаданными, примечаниями и полными данными.",
          "type": "research_note",
          "targetAudience": "institutional",
          "status": "draft",
          "version": "0.0.1",
          "language": "ru",
          "authors": ["Автор 1", "Автор 2"],
          "createdAt": "2025-01-01T10:00:00Z",
          "updatedAt": "2025-10-12T15:00:00Z",
          "publishedAt": null,
          "metadata": {
            "assetClasses": ["Equities", "Bonds"],
            "companies": ["Компания А", "Компания Б"],
            "instruments": ["Stocks", "Bonds"],
            "sectors": ["Information Technology", "Health Care"],
            "regions": ["US", "Europe"],
            "riskProfile": "Moderate",
            "timeHorizon": "Long-term",
            "tags": ["тест", "пример", "публикация"],
            "additionalField": "Произвольное значение"
          },
          "notes": [
            {
              "id": "note_pub_1",
              "author": "Редактор 1",
              "role": "editor",
              "timestamp": "2025-10-12T12:00:00Z",
              "status": "open",
              "text": "Первое примечание на уровне публикации."
            }
          ],
          "chapters": [
            {
              "id": "ch_001",
              "title": "Глава 1",
              "subtitle": "Подзаголовок главы 1",
              "summary": "Краткое содержание первой главы.",
              "order": 0,
              "metadata": {
                "assetClasses": ["Equities"],
                "companies": ["Компания А"],
                "instruments": ["Stocks"],
                "sectors": ["Information Technology"],
                "regions": ["US"],
                "riskProfile": "Moderate",
                "timeHorizon": "Long-term",
                "tags": ["глава1"]
              },
              "notes": [
                {
                  "id": "note_ch_1_1",
                  "author": "Автор 1",
                  "role": "author",
                  "timestamp": "2025-10-12T13:00:00Z",
                  "status": "open",
                  "text": "Примечание к главе 1, первый элемент."
                }
              ],
              "createdAt": "2025-10-10T10:00:00Z",
              "updatedAt": "2025-10-12T14:00:00Z",
              "authors": ["Автор 1", "Автор 2"],
              "language": "ru",
              "blocks": [
                {
                  "id": "block_001",
                  "title": "Блок 1",
                  "subtitle": "Подзаголовок блока 1",
                  "summary": "Краткое описание блока 1.",
                  "type": "textual",
                  "metadata": {
                    "assetClasses": ["Equities"],
                    "companies": ["Компания А"],
                    "instruments": ["Stocks"],
                    "sectors": ["Information Technology"],
                    "regions": ["US"],
                    "riskProfile": "Moderate",
                    "timeHorizon": "Long-term",
                    "tags": ["блок1"]
                  },
                  "notes": [
                    {
                      "id": "note_block_1_1",
                      "author": "Автор 1",
                      "role": "author",
                      "timestamp": "2025-10-12T13:45:00Z",
                      "status": "open",
                      "text": "Первое примечание к блоку 1."
                    }
                  ],
                  "createdAt": "2025-10-11T10:00:00Z",
                  "updatedAt": "2025-10-12T14:15:00Z",
                  "authors": ["Автор 1"],
                  "language": "ru",
                  "views": [
                    {
                      "type": "text",
                      "content": "Текстовый контент внутри блокa 1.",
                      "notes": [
                        {
                          "id": "note_view_1_1",
                          "author": "Рецензент А",
                          "role": "reviewer",
                          "timestamp": "2025-10-12T14:30:00Z",
                          "status": "open",
                          "text": "Примечание к текстовому view."
                        }
                      ]
                    }
                  ]
                }
              ]
            }
          ]
        }
      ]
    }
  }'
```

### Expected Response (200 OK)

```json
{
  "transformedJson": {
    "publications": [
      {
        "id": "pub_full_example_001",
        "title": "Полноценный пример публикации для тестирования",
        "subtitle": "Включены все поля и минимум 2 элемента в массивах",
        "summary": "Эта публикация создана для демонстрации всех полей схемы, с расширенными метаданными, примечаниями и полными данными.",
        "type": "research_note",
        "targetAudience": "institutional",
        "status": "draft",
        "version": "0.0.1",
        "language": "ru",
        "authors": ["Автор 1", "Автор 2"],
        "createdAt": "2025-01-01T10:00:00Z",
        "updatedAt": "2025-10-12T15:00:00Z",
        "publishedAt": null,
        "metadata": {
          "assetClasses": ["Equities", "Bonds"],
          "companies": ["Компания А", "Компания Б"],
          "instruments": ["Stocks", "Bonds"],
          "sectors": ["Information Technology", "Health Care"],
          "regions": ["US", "Europe"],
          "riskProfile": "Moderate",
          "timeHorizon": "Long-term",
          "tags": ["тест", "пример", "публикация"],
          "additionalField": "Произвольное значение"
        },
        "chapters": [
          {
            "id": "ch_001",
            "title": "Глава 1",
            "subtitle": "Подзаголовок главы 1",
            "summary": "Краткое содержание первой главы.",
            "order": 0,
            "metadata": {
              "assetClasses": ["Equities"],
              "companies": ["Компания А"],
              "instruments": ["Stocks"],
              "sectors": ["Information Technology"],
              "regions": ["US"],
              "riskProfile": "Moderate",
              "timeHorizon": "Long-term",
              "tags": ["глава1"]
            },
            "createdAt": "2025-10-10T10:00:00Z",
            "updatedAt": "2025-10-12T14:00:00Z",
            "authors": ["Автор 1", "Автор 2"],
            "language": "ru",
            "blocks": [
              {
                "id": "block_001",
                "title": "Блок 1",
                "subtitle": "Подзаголовок блока 1",
                "summary": "Краткое описание блока 1.",
                "type": "textual",
                "metadata": {
                  "assetClasses": ["Equities"],
                  "companies": ["Компания А"],
                  "instruments": ["Stocks"],
                  "sectors": ["Information Technology"],
                  "regions": ["US"],
                  "riskProfile": "Moderate",
                  "timeHorizon": "Long-term",
                  "tags": ["блок1"]
                },
                "createdAt": "2025-10-11T10:00:00Z",
                "updatedAt": "2025-10-12T14:15:00Z",
                "authors": ["Автор 1"],
                "language": "ru",
                "views": [
                  {
                    "type": "text",
                    "content": "Текстовый контент внутри блокa 1."
                  }
                ]
              }
            ]
          }
        ]
      }
    ]
  }
}
```

### Validation Against Consumer Schema

To validate that the transformed output conforms to the consumer schema:

```bash
curl -X POST http://localhost:8080/api/consumers/mobile-app/schemas/invest-publications/validate \
  -H "Content-Type: application/json" \
  -H "Accept: application/json" \
  -d '{"transformedJson": {"publications": [{"id": "pub_full_example_001", "title": "Полноценный пример публикации для тестирования", "type": "research_note", "chapters": [{"id": "ch_001", "title": "Глава 1", "blocks": [{"id": "block_001", "title": "Блок 1", "views": [{"type": "text", "content": "Текстовый контент внутри блокa 1."}]}]}]}]}}'
```

Expected Response:
```json
{
  "valid": true,
  "errors": []
}
```

## Automation Script

For convenience, you can use the provided automation script to run this entire example:

```bash
# Run the complete end-to-end example
./tests/utils/scripts/run-end-to-end-example.sh
```

This script will:
1. Start the required services (PostgreSQL)
2. Register all schemas, consumers, and templates
3. Validate and transform sample data
4. Verify the results

## Key Concepts Demonstrated

1. **Schema Evolution**: Registering canonical schemas with versioning and compatibility rules
2. **Consumer-Specific Schemas**: Different consumers can have different output schemas
3. **Transformation Templates**: JSLT expressions that transform data between schemas
4. **Data Validation**: Ensuring data conforms to schemas before and after transformation
5. **Multi-level Hierarchy**: Complex nested data structures (publications → chapters → blocks → views)

## Next Steps

- Explore other transformation engines (pipeline, router)
- Set up schema compatibility checking
- Implement consumer-specific metadata filtering
- Add authentication and authorization
- Monitor transformation performance and errors

## Troubleshooting

- Ensure PostgreSQL is running: `docker-compose up -d db`
- Check service health: `curl http://localhost:8080/actuator/health`
- View application logs for detailed error information
- Use the test scripts in `tests/` for individual component testing