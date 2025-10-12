# JSLT Features and Typical Use Cases

## Overview

JSLT (JSON Schema Language for Transformations) is a complete query and transformation language for JSON, inspired by jq, XPath, and XQuery. It allows querying, filtering, and transforming JSON data using a declarative syntax.

Key features:
- Querying: Extract values with dot notation (e.g., `.name`)
- Filtering: Test conditions with `if` expressions
- Transformation: Construct new JSON structures
- Functions: Built-in functions for strings, numbers, arrays, objects
- Variables and functions: Define reusable logic

## Typical Use Cases with Examples

### 1. Removal of Attributes

To remove specific attributes from an object, use object matching with exclusion.

**Input:**
```json
{
  "name": "John",
  "age": 30,
  "city": "NYC"
}
```

**JSLT Template:**
```jslt
{
  * - age : .
}
```

**Output:**
```json
{
  "name": "John",
  "city": "NYC"
}
```

### 2. Removal of Attributes with Same Name on Several Levels

For nested structures, use recursive functions to remove keys at all levels.

**Input:**
```json
{
  "user": {
    "name": "John",
    "details": {
      "name": "alias",
      "age": 30
    }
  },
  "name": "root"
}
```

**JSLT Template:**
```jslt
def remove_name(obj)
  if (is-object($obj))
    {for ($obj) 
       if (.key != "name") 
         .key : remove_name(.value)
     }
  else
    $obj

remove_name(.)
```

**Output:**
```json
{
  "user": {
    "details": {
      "age": 30
    }
  }
}
```

### 3. Transformation of Elements: Renaming

Rename keys by mapping old keys to new ones.

**Input:**
```json
{
  "firstName": "John",
  "lastName": "Doe"
}
```

**JSLT Template:**
```jslt
{
  "fullName": .firstName + " " + .lastName
}
```

**Output:**
```json
{
  "fullName": "John Doe"
}
```

### 4. Transformation of Elements: Concatenation

Concatenate strings using the `+` operator.

**Input:**
```json
{
  "firstName": "John",
  "lastName": "Doe"
}
```

**JSLT Template:**
```jslt
{
  "fullName": .firstName + " " + .lastName
}
```

**Output:**
```json
{
  "fullName": "John Doe"
}
```

### 5. Transformation of Elements: Conversions from Numbers to Strings

Use the `string()` function to convert numbers to strings.

**Input:**
```json
{
  "age": 30,
  "score": 95.5
}
```

**JSLT Template:**
```jslt
{
  "ageStr": string(.age),
  "scoreStr": string(.score)
}
```

**Output:**
```json
{
  "ageStr": "30",
  "scoreStr": "95.5"
}
```

### 6. Shifting Elements Between Levels of Schema

Move elements from nested to top level or vice versa.

**From upper to below:**

**Input:**
```json
{
  "name": "John",
  "details": {
    "age": 30
  }
}
```

**JSLT Template:**
```jslt
{
  "user": {
    "name": .name,
    "age": .details.age
  }
}
```

**Output:**
```json
{
  "user": {
    "name": "John",
    "age": 30
  }
}
```

**From below to upper:**

**Input:**
```json
{
  "user": {
    "name": "John",
    "age": 30
  }
}
```

**JSLT Template:**
```jslt
{
  "name": .user.name,
  "age": .user.age
}
```

**Output:**
```json
{
  "name": "John",
  "age": 30
}
```

### 7. Dealing with Large Schemas

For large schemas where only small portions need changes, use `* : .` to copy unchanged parts and specify modifications.

**Input:**
```json
{
  "id": 1,
  "name": "Product A",
  "price": 100,
  "details": {
    "category": "Electronics",
    "specs": {
      "weight": "1kg",
      "color": "black"
    }
  },
  "tags": ["new", "popular"]
}
```

**JSLT Template (modify price and add discount):**
```jslt
{
  "price": .price * 0.9,
  "discount": true,
  * : .
}
```

**Output:**
```json
{
  "id": 1,
  "name": "Product A",
  "price": 90,
  "details": {
    "category": "Electronics",
    "specs": {
      "weight": "1kg",
      "color": "black"
    }
  },
  "tags": ["new", "popular"],
  "discount": true
}
```

This approach ensures only the changed parts are specified, while the rest is copied unchanged.

## References and Resources

- **Official Repository:** https://github.com/schibsted/jslt
- **Tutorial:** https://raw.githubusercontent.com/schibsted/jslt/master/tutorial.md
- **Functions Documentation:** https://raw.githubusercontent.com/schibsted/jslt/master/functions.md
- **Demo Playground:** http://www.garshol.priv.no/jslt-demo
- **Javadoc:** http://javadoc.io/doc/com.schibsted.spt.data/jslt
- **Talk on JSLT Development:** https://vimeo.com/289470470
- **Paper on JSLT Usage:** https://arxiv.org/abs/1908.10754

For more examples, see the examples directory in the repository: https://github.com/schibsted/jslt/tree/master/examples