package fr.lapetina.smartocr.pipeline;

import fr.lapetina.smartocr.api.ParserException;
import fr.lapetina.smartocr.ollama.StructuredExtractionService;
import fr.lapetina.smartocr.ollama.VisionOcrService;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.Objects;
import java.util.Optional;

/**
 * Orchestrates the document parsing pipeline.
 * <p>
 * Pipeline logic:
 * <ul>
 *   <li>If image is present → OCR → text extraction</li>
 *   <li>If text is present → direct extraction</li>
 *   <li>If both provided → image takes precedence</li>
 *   <li>If none provided → IllegalArgumentException</li>
 * </ul>
 */
public class ParsingPipeline implements ExtractionStrategy {

    private final VisionOcrService ocrService;
    private final StructuredExtractionService extractionService;

    /**
     * Creates a pipeline with the specified services.
     *
     * @param ocrService        the OCR service for image text extraction
     * @param extractionService the service for structured data extraction
     */
    public ParsingPipeline(VisionOcrService ocrService, StructuredExtractionService extractionService) {
        this.ocrService = Objects.requireNonNull(ocrService, "ocrService must not be null");
        this.extractionService = Objects.requireNonNull(extractionService, "extractionService must not be null");
    }

    @Override
    public JsonNode extract(Optional<byte[]> image, Optional<String> text, String extractionSchemaJson) {
        Objects.requireNonNull(image, "image must not be null");
        Objects.requireNonNull(text, "text must not be null");
        Objects.requireNonNull(extractionSchemaJson, "extractionSchemaJson must not be null");

        if (image.isEmpty() && text.isEmpty()) {
            throw new ParserException("At least one of image or text must be provided");
        }

        String textToExtract = resolveText(image, text);
        return extractionService.extract(textToExtract, extractionSchemaJson);
    }

    private String resolveText(Optional<byte[]> image, Optional<String> text) {
        // Image takes precedence if present
        if (image.isPresent()) {
            byte[] imageData = image.get();
            if (imageData.length == 0) {
                throw new ParserException("image must not be empty");
            }
            return ocrService.extractText(imageData);
        }

        // Fall back to provided text
        String providedText = text.get();
        if (providedText.isBlank()) {
            throw new ParserException("text must not be blank");
        }
        return providedText;
    }
}
