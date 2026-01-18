package fr.lapetina.smartocr.ollama;

import fr.lapetina.smartocr.api.ExtractionException;
import fr.lapetina.smartocr.util.JsonSanitizer;
import fr.lapetina.smartocr.util.JsonSanitizer.JsonSanitizationException;
import fr.lapetina.smartocr.util.PromptTemplates;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.Objects;

/**
 * Service that extracts structured JSON data from text using an LLM.
 */
public class StructuredExtractionService {

    private static final String DEFAULT_MODEL = "llama3.2";

    private final LlmClient client;
    private final String model;

    /**
     * Creates a StructuredExtractionService with the specified LLM client and model.
     *
     * @param client the LLM client to use
     * @param model  the text model name
     */
    public StructuredExtractionService(LlmClient client, String model) {
        this.client = Objects.requireNonNull(client, "client must not be null");
        this.model = Objects.requireNonNull(model, "model must not be null");
    }

    /**
     * Creates a StructuredExtractionService with the specified LLM client and default model.
     *
     * @param client the LLM client to use
     */
    public StructuredExtractionService(LlmClient client) {
        this(client, DEFAULT_MODEL);
    }

    /**
     * Creates a StructuredExtractionService with a default Ollama client and model.
     */
    public StructuredExtractionService() {
        this(new OllamaClient(), DEFAULT_MODEL);
    }

    /**
     * Extracts structured data from text according to the provided schema.
     *
     * @param text                 the text to extract from
     * @param extractionSchemaJson the JSON schema defining the extraction structure
     * @return the extracted data as a JsonNode
     * @throws ExtractionException if extraction fails or response is not valid JSON
     */
    public JsonNode extract(String text, String extractionSchemaJson) {
        Objects.requireNonNull(text, "text must not be null");
        Objects.requireNonNull(extractionSchemaJson, "extractionSchemaJson must not be null");

        if (text.isBlank()) {
            throw new ExtractionException("text must not be blank");
        }

        if (extractionSchemaJson.isBlank()) {
            throw new ExtractionException("extractionSchemaJson must not be blank");
        }

        try {
            String prompt = PromptTemplates.buildExtractionPrompt(extractionSchemaJson, text);
            String response = client.sendPrompt(model, prompt);
            return JsonSanitizer.sanitize(response);
        } catch (OllamaException e) {
            throw new ExtractionException("Extraction failed: " + e.getMessage(), e);
        } catch (JsonSanitizationException e) {
            throw new ExtractionException("Failed to extract valid JSON: " + e.getMessage(), e);
        }
    }
}
