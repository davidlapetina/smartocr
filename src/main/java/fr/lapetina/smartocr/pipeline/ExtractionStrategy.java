package fr.lapetina.smartocr.pipeline;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.Optional;

/**
 * Internal strategy interface for extracting structured data from documents.
 * Implementations handle the actual extraction logic (e.g., LLM calls).
 */
public interface ExtractionStrategy {

    /**
     * Extracts structured data from the provided content according to the schema.
     *
     * @param image               optional image data
     * @param text                optional text content
     * @param extractionSchemaJson JSON schema defining the extraction structure
     * @return extracted data as JsonNode
     * @throws fr.lapetina.smartocr.api.OcrException if OCR fails
     * @throws fr.lapetina.smartocr.api.ExtractionException if extraction fails
     */
    JsonNode extract(Optional<byte[]> image, Optional<String> text, String extractionSchemaJson);
}
