# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

SmartOCR is a Java library for extracting structured data from documents using local LLMs via Ollama. It provides entity-agnostic document parsing that combines OCR with structured data extraction.

## Build Commands

```bash
mvn clean install          # Clean and build
mvn test                   # Run unit tests (no Ollama required)
mvn test -Dtest=ClassName  # Run specific test
mvn test -Dtest=*IntegrationTest  # Run integration tests (requires Ollama)
```

**Requirements:** Java 21, Ollama running locally with `llama3.2-vision` and `llama3.2` models.

## Architecture

Two-stage pipeline architecture:

```
Image → VisionOcrService → Plain Text → StructuredExtractionService → JsonNode
                                ↑
                          or direct text input
```

### Key Packages

- **api/** - Public interface (`DocumentParser`) and implementation (`DefaultDocumentParser`) with builder pattern
- **pipeline/** - `ParsingPipeline` orchestrates the two-stage extraction process using `ExtractionStrategy` interface
- **ollama/** - HTTP client for Ollama API (`OllamaClient`), `VisionOcrService` for OCR, `StructuredExtractionService` for JSON extraction
- **schema/** - `ExtractionSchema` opaque wrapper treating schemas as raw LLM instructions
- **util/** - `PromptTemplates` for centralized LLM prompts, `JsonSanitizer` for robust JSON extraction from LLM responses

### Design Patterns

- **Builder Pattern**: `DefaultDocumentParser.builder()` for fluent configuration
- **Strategy Pattern**: `ExtractionStrategy` interface decouples parsing logic from API
- **Constructor Injection**: Interface-based dependencies (`LlmClient`) enable mockable testing

### Exception Hierarchy

```
RuntimeException
└── ParserException
    ├── OcrException (image/vision model failures)
    └── ExtractionException (JSON extraction failures)
```

## Testing Notes

- Unit tests use Mockito to mock `LlmClient` - no Ollama needed
- Integration tests require running Ollama instance
- Tests run with `-Djava.awt.headless=true` and module opens for Java 21 compatibility
