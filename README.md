# SmartOCR

A Java library for extracting structured data from documents using local LLMs via [Ollama](https://ollama.ai). SmartOCR provides entity-agnostic document parsing that combines OCR capabilities with structured data extraction, all running locally on your machine.

## Features

- **Local Processing**: All processing happens locally using Ollama - no data leaves your machine
- **OCR from Images**: Extract text from images using vision-capable models (e.g., LLaVA, llama3.2-vision)
- **Structured Extraction**: Extract structured JSON data from text according to your custom schema
- **Entity-Agnostic**: Works with any document type - invoices, receipts, forms, contracts, etc.
- **Flexible Pipeline**: Process images, raw text, or both
- **Zero Configuration**: Sensible defaults that work out of the box

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
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

### Gradle

```groovy
implementation 'fr.lapetina.smartocr:smartocr:1.0.0-SNAPSHOT'
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

## Configuration

### Custom Ollama URL

```java
DocumentParser parser = DefaultDocumentParser.builder()
    .ollamaBaseUrl("http://localhost:11434")
    .build();
```

### Custom Models

```java
DocumentParser parser = DefaultDocumentParser.builder()
    .visionModel("llava:13b")      // For OCR
    .textModel("mistral:latest")    // For extraction
    .build();
```

## Schema Definition

The extraction schema follows JSON Schema format. Key points:

- Use `type` to specify the data type (`string`, `number`, `integer`, `boolean`, `array`, `object`)
- Use `description` to guide the LLM on what to extract
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

## Acknowledgments

- [Ollama](https://ollama.ai) for making local LLMs accessible
- [Jackson](https://github.com/FasterXML/jackson) for JSON processing
