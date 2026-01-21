# SmartOCR

A Java library for extracting structured data from documents using local LLMs via [Ollama](https://ollama.ai). SmartOCR provides entity-agnostic document parsing that combines OCR capabilities with structured data extraction, all running locally on your machine.

## Features

- **Local Processing**: All processing happens locally using Ollama - no data leaves your machine
- **OCR from Images**: Extract text from images using vision-capable models (e.g., LLaVA, llama3.2-vision)
- **Structured Extraction**: Extract structured JSON data from text according to your custom schema
- **Array Support**: Extract arrays and nested arrays for complete document scanning (e.g., line items, contact lists)
- **Entity-Agnostic**: Works with any document type - invoices, receipts, forms, contracts, etc.
- **Flexible Pipeline**: Process images, raw text, or both
- **Zero Configuration**: Sensible defaults that work out of the box
- **Pool Mode**: Optional connection pooling with load balancing via [ollama-load-balancer](https://github.com/ollama-pool/ollama-load-balancer)

## Requirements

- Java 21 or higher
- [Ollama](https://ollama.ai) running locally
- Vision model for OCR (e.g., `llama3.2-vision`)
- Text model for extraction (e.g., `llama3.2`)

## Installation

### Maven

```xml
<dependency>
    <groupId>fr.lapetina.smartocr</groupId>
    <artifactId>smartocr</artifactId>
    <version>1.2.0</version>
</dependency>
```

### Gradle

```groovy
implementation 'fr.lapetina.smartocr:smartocr:1.2.0'
```

## Quick Start

### 1. Start Ollama

Make sure Ollama is running and you have the required models:

```bash
ollama serve
ollama pull llama3.2-vision
ollama pull llama3.2
```

### 2. Parse a Document

```java
import fr.lapetina.smartocr.api.DocumentParser;
import fr.lapetina.smartocr.api.DefaultDocumentParser;
import com.fasterxml.jackson.databind.JsonNode;

// Create parser with default settings
DocumentParser parser = new DefaultDocumentParser();

// Define your extraction schema
String schema = """
    {
        "type": "object",
        "properties": {
            "invoiceNumber": {"type": "string", "description": "The invoice number"},
            "date": {"type": "string", "description": "Invoice date in ISO-8601 format"},
            "totalAmount": {"type": "number", "description": "Total amount as a number"},
            "vendorName": {"type": "string", "description": "Name of the vendor"}
        }
    }
    """;

// Parse an image
byte[] imageData = Files.readAllBytes(Path.of("invoice.png"));
JsonNode result = parser.parseImage(imageData, schema);

System.out.println("Invoice Number: " + result.get("invoiceNumber").asText());
System.out.println("Total: " + result.get("totalAmount").asDouble());
```

### 3. Parse Raw Text

```java
String text = """
    Customer: John Smith
    Email: john.smith@example.com
    Order Date: 2024-03-15
    Total: $99.99
    """;

String schema = """
    {
        "type": "object",
        "properties": {
            "customerName": {"type": "string"},
            "email": {"type": "string"},
            "orderDate": {"type": "string"},
            "total": {"type": "number"}
        }
    }
    """;

JsonNode result = parser.parseText(text, schema);
```

### 4. Extract Arrays from Documents

For complete document scanning, you can extract top-level arrays or nested arrays:

```java
String document = """
    CONTACT LIST

    Name: John Smith
    Email: john@example.com
    Phone: 555-1234

    Name: Jane Doe
    Email: jane@example.com
    Phone: 555-5678
    """;

String schema = """
    {
        "type": "array",
        "description": "List of contacts",
        "items": {
            "type": "object",
            "properties": {
                "name": {"type": "string"},
                "email": {"type": "string"},
                "phone": {"type": "string"}
            }
        }
    }
    """;

JsonNode contacts = parser.parseText(document, schema);
// Returns: [{"name": "John Smith", ...}, {"name": "Jane Doe", ...}]

for (JsonNode contact : contacts) {
    System.out.println(contact.get("name").asText());
}
```

## Configuration

SmartOCR uses connection pooling with load balancing via [ollama-load-balancer](https://github.com/ollama-pool/ollama-load-balancer). This provides circuit breakers, health monitoring, and efficient connection management.

### Custom Models

```java
DocumentParser parser = DefaultDocumentParser.builder()
    .visionModel("llava:13b")      // For OCR
    .textModel("mistral:latest")    // For extraction
    .build();
```

### Custom Pool Configuration

```java
// Use custom pool configuration files
DocumentParser parser = DefaultDocumentParser.builder()
    .poolConfig("my-vision-pool.yaml", "my-text-pool.yaml")
    .build();
```

### Shared Pool Manager

Share a pool manager across multiple parsers for efficient resource usage:

```java
try (OllamaPoolManager poolManager = OllamaPoolManager.createDefault()) {
    DocumentParser parser = DefaultDocumentParser.builder()
        .poolManager(poolManager)
        .build();
    // use parser...
}
```

### Pool Architecture

SmartOCR uses two separate connection pools:
- **Vision Pool**: For OCR operations using `llama3.2-vision`
- **Text Pool**: For structured extraction using `llama3.2`

Configuration files are YAML-based. See `src/main/resources/vision-pool-config.yaml` and `text-pool-config.yaml` for examples.

## Schema Definition

The extraction schema follows JSON Schema format. Key points:

- Use `type` to specify the data type (`string`, `number`, `integer`, `boolean`, `array`, `object`)
- Use `description` to guide the LLM on what to extract
- Top-level arrays are supported for extracting lists of items from documents
- Nested arrays (one level deep) are supported for complex structures
- Dates are automatically normalized to ISO-8601 format (YYYY-MM-DD)
- Numbers are extracted without currency symbols
- Missing values are returned as `null`

### Example: Receipt Schema

```json
{
    "type": "object",
    "properties": {
        "storeName": {"type": "string"},
        "date": {"type": "string", "description": "Receipt date in ISO-8601 format"},
        "total": {"type": "number"},
        "items": {
            "type": "array",
            "items": {
                "type": "object",
                "properties": {
                    "name": {"type": "string"},
                    "price": {"type": "number"}
                }
            }
        }
    }
}
```

## Architecture

SmartOCR uses a two-stage pipeline:

1. **OCR Stage** (optional): If an image is provided, the vision model extracts all readable text
2. **Extraction Stage**: The text model extracts structured data according to your schema

```
Image → [Vision Model] → Raw Text → [Text Model] → Structured JSON
                              ↑
                         or direct text input
```

## Error Handling

SmartOCR provides specific exceptions for different failure modes:

- `ParserException`: Base exception for all parsing errors
- `OcrException`: OCR-specific failures (image processing, vision model errors)
- `ExtractionException`: Extraction failures (invalid JSON, schema mismatches)

```java
try {
    JsonNode result = parser.parseImage(imageData, schema);
} catch (OcrException e) {
    // Handle OCR failure
} catch (ExtractionException e) {
    // Handle extraction failure
}
```

## Building from Source

```bash
git clone https://github.com/dlapetina/smartocr.git
cd smartocr
mvn clean install
```

### Running Tests

Unit tests (no Ollama required):
```bash
mvn test
```

Integration tests (requires Ollama):
```bash
ollama serve &
mvn test -Dtest=*IntegrationTest
```

## License

This project is licensed under the Apache License 2.0 - see the [LICENSE](LICENSE) file for details.

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## Dependencies

SmartOCR uses the following key dependencies:

- [ollama-load-balancer](https://github.com/ollama-pool/ollama-load-balancer) (1.1.0) - High-performance connection pooling and load balancing for Ollama
- [Jackson](https://github.com/FasterXML/jackson) - JSON processing

## Acknowledgments

- [Ollama](https://ollama.ai) for making local LLMs accessible
- [Jackson](https://github.com/FasterXML/jackson) for JSON processing
- [ollama-load-balancer](https://github.com/ollama-pool/ollama-load-balancer) for connection pooling
