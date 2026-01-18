package fr.lapetina.smartocr.api;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.Optional;

/**
 * Entity-agnostic document parser that extracts structured data from images or text
 * according to a provided JSON schema.
 */
public interface DocumentParser {

    /**
     * Parses document content (image and/or text) and extracts structured data
     * according to the provided extraction schema.
     *
     * @param image               optional image data to parse
     * @param text                optional raw text to parse
     * @param extractionSchemaJson JSON schema defining the structure to extract
     * @return extracted data as a JsonNode conforming to the schema
     * @throws ParserException if parsing fails or both image and text are empty
     */
    JsonNode parse(Optional<byte[]> image, Optional<String> text, String extractionSchemaJson);

    /**
     * Parses an image and extracts structured data according to the provided schema.
     *
     * @param image                image data to parse
     * @param extractionSchemaJson JSON schema defining the structure to extract
     * @return extracted data as a JsonNode conforming to the schema
     * @throws OcrException if OCR fails
     * @throws ExtractionException if extraction fails
     */
    default JsonNode parseImage(byte[] image, String extractionSchemaJson) {
        return parse(Optional.of(image), Optional.empty(), extractionSchemaJson);
    }

    /**
     * Parses raw text and extracts structured data according to the provided schema.
     *
     * @param text                 raw text to parse
     * @param extractionSchemaJson JSON schema defining the structure to extract
     * @return extracted data as a JsonNode conforming to the schema
     * @throws ExtractionException if extraction fails
     */
    default JsonNode parseText(String text, String extractionSchemaJson) {
        return parse(Optional.empty(), Optional.of(text), extractionSchemaJson);
    }
}
