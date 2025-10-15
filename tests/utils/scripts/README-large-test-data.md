# Large Investment Publications Test Data Generator

This directory contains a script for generating large JSON test files conforming to the `invest-publications.schema.json` schema. These files are designed for stress-testing the schema registry with large JSON documents containing embedded base64 images.

## Script: `generate-large-investment-publications.py`

### Purpose

The script generates realistic test data that simulates large investment research publications with:
- Hierarchical structure: Publications → Chapters → Blocks → Views
- All optional fields populated with investment/financial terminology
- Multiple base64-encoded images of configurable sizes
- Comprehensive metadata and notes at all levels
- All supported view types: text, image, chart, table

### Usage

```bash
# Basic usage - generate 1MB test file
python3 generate-large-investment-publications.py --size 1mb

# Generate different sizes
python3 generate-large-investment-publications.py --size 5mb
python3 generate-large-investment-publications.py --size 10mb
python3 generate-large-investment-publications.py --size 15mb

# Specify custom output location
python3 generate-large-investment-publications.py --size 10mb --output /path/to/my-test-data.json

# Generate with schema validation (requires ajv-cli)
python3 generate-large-investment-publications.py --size 5mb --validate

# Show help
python3 generate-large-investment-publications.py --help
```

### Options

- `--size {1mb,5mb,10mb,15mb}`: Target file size (default: 1mb)
- `--output FILE`: Output filename (default: `large-publication-{size}.json`)
- `--schema FILE`: Schema file path for validation
- `--validate`: Validate generated JSON against schema (requires `ajv-cli`)
- `--help`: Show help message

### File Size Specifications

| Size | Chapters | Blocks/Chapter | Total Blocks | Views | Images | Base64/Image |
|------|----------|----------------|--------------|-------|--------|--------------|
| 1MB  | 3        | 5              | 15           | ~45   | 6      | ~15KB        |
| 5MB  | 6        | 7              | 42           | ~168  | 12     | ~40KB        |
| 10MB | 8        | 9              | 72           | ~360  | 20     | ~80KB        |
| 15MB | 10       | 11             | 110          | ~605  | 25     | ~120KB       |

### Dependencies

- Python 3.6+
- `ajv-cli` (optional, for schema validation): `npm install -g ajv-cli`

### Schema Compliance

Generated files conform to `tests/examples/investment-research/publications/invest-publications.schema.json` and include:

- **Required fields**: All mandatory schema fields
- **Optional fields**: All optional fields populated with realistic data
- **Data types**: Proper types for all fields (strings, numbers, arrays, objects)
- **Constraints**: Respects length limits, enum values, and pattern requirements
- **Base64 images**: Properly formatted base64 strings representing large binary content

### Content Features

- **Realistic terminology**: Investment and financial terms throughout
- **Hierarchical notes**: Notes at publication, chapter, and block levels
- **Rich metadata**: Complete metadata objects with all enum values
- **Multiple view types**: Text, images, charts, and tables
- **Proper timestamps**: ISO 8601 formatted dates
- **Unique IDs**: Alphanumeric IDs following schema patterns

### Use Cases

1. **Performance testing**: Test schema registry performance with large JSON payloads
2. **Memory testing**: Verify handling of large documents in memory
3. **Parsing validation**: Ensure robust JSON parsing of complex structures
4. **Schema validation**: Test schema validation with complete, complex data
5. **Binary content handling**: Test processing of base64-encoded images

### Examples

```bash
# Generate test files for performance testing
cd tests/utils/scripts
python3 generate-large-investment-publications.py --size 1mb --output ../examples/investment-research/publications/stress-test-1mb.json
python3 generate-large-investment-publications.py --size 5mb --output ../examples/investment-research/publications/stress-test-5mb.json
python3 generate-large-investment-publications.py --size 10mb --output ../examples/investment-research/publications/stress-test-10mb.json

# Validate generated files
python3 generate-large-investment-publications.py --size 1mb --validate
```

### File Locations

Generated files are typically placed in:
- `tests/examples/investment-research/publications/` (for examples)
- Custom locations via `--output` parameter

### Troubleshooting

**Script not found**: Ensure you're running from the correct directory or use full path.

**Validation fails**: Install `ajv-cli` with `npm install -g ajv-cli`.

**Permission denied**: Make sure the output directory is writable.

**Memory issues**: For very large files (10MB+), ensure sufficient system memory.

### Integration with Tests

The generated files can be used in various test scenarios:

```bash
# Use in transformation tests
./tests/transform/test-transform-data.sh large-publication-1mb.json

# Use in schema validation tests
./tests/schemas/test-schema-validation.sh large-publication-5mb.json

# Use in performance benchmarks
./tests/utils/scripts/benchmark-json-processing.py large-publication-*.json
```