package fr.lapetina.smartocr.api;

import fr.lapetina.smartocr.ollama.LlmClient;
import fr.lapetina.smartocr.ollama.OllamaPoolManager;
import fr.lapetina.smartocr.ollama.StructuredExtractionService;
import fr.lapetina.smartocr.ollama.VisionOcrService;
import fr.lapetina.smartocr.pipeline.ExtractionStrategy;
import fr.lapetina.smartocr.pipeline.ParsingPipeline;
import fr.lapetina.smartocr.schema.ExtractionSchema;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.Objects;
import java.util.Optional;

/**
 * Default implementation of {@link DocumentParser} that delegates extraction
 * to a configured strategy.
 * <p>
 * Uses connection pooling with load balancing via ollama-load-balancer.
 * <p>
 * Usage:
 * <pre>{@code
 * // Simple usage with default pool configuration
 * DocumentParser parser = new DefaultDocumentParser();
 * JsonNode result = parser.parseImage(imageBytes, schemaJson);
 *
 * // Custom configuration
 * DocumentParser parser = DefaultDocumentParser.builder()
 *     .visionModel("llava")
 *     .textModel("mistral")
 *     .build();
 *
 * // With custom pool configuration files
 * DocumentParser parser = DefaultDocumentParser.builder()
 *     .poolConfig("my-vision-pool.yaml", "my-text-pool.yaml")
 *     .build();
 * }</pre>
 */
public class DefaultDocumentParser implements DocumentParser {

    private final ExtractionStrategy extractionStrategy;

    /**
     * Creates a new parser with the default pool configuration.
     * Uses vision-pool-config.yaml and text-pool-config.yaml from the classpath.
     */
    public DefaultDocumentParser() {
        this(builder().build().extractionStrategy);
    }

    /**
     * Creates a new parser with the specified extraction strategy.
     *
     * @param extractionStrategy strategy for performing the actual extraction
     */
    public DefaultDocumentParser(ExtractionStrategy extractionStrategy) {
        this.extractionStrategy = Objects.requireNonNull(extractionStrategy, "extractionStrategy must not be null");
    }

    /**
     * Creates a new builder for configuring the parser.
     *
     * @return a new builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    @Override
    public JsonNode parse(Optional<byte[]> image, Optional<String> text, String extractionSchemaJson) {
        Objects.requireNonNull(image, "image must not be null (use Optional.empty() for no image)");
        Objects.requireNonNull(text, "text must not be null (use Optional.empty() for no text)");
        Objects.requireNonNull(extractionSchemaJson, "extractionSchemaJson must not be null");

        if (image.isEmpty() && text.isEmpty()) {
            throw new ParserException("At least one of image or text must be provided");
        }

        if (extractionSchemaJson.isBlank()) {
            throw new ParserException("extractionSchemaJson must not be blank");
        }

        return extractionStrategy.extract(image, text, extractionSchemaJson);
    }

    /**
     * Parses an image using the provided extraction schema.
     *
     * @param image  the image data
     * @param schema the extraction schema
     * @return extracted data as JsonNode
     */
    public JsonNode parseImage(byte[] image, ExtractionSchema schema) {
        Objects.requireNonNull(schema, "schema must not be null");
        return parseImage(image, schema.rawJson());
    }

    /**
     * Parses text using the provided extraction schema.
     *
     * @param text   the text to parse
     * @param schema the extraction schema
     * @return extracted data as JsonNode
     */
    public JsonNode parseText(String text, ExtractionSchema schema) {
        Objects.requireNonNull(schema, "schema must not be null");
        return parseText(text, schema.rawJson());
    }

    /**
     * Builder for creating configured DefaultDocumentParser instances.
     * Uses connection pooling with load balancing via ollama-load-balancer.
     */
    public static class Builder {
        private String visionModel = "llama3.2-vision";
        private String textModel = "llama3.2";
        private String visionPoolConfig = "vision-pool-config.yaml";
        private String textPoolConfig = "text-pool-config.yaml";
        private OllamaPoolManager poolManager;

        private Builder() {
        }

        /**
         * Sets the vision model for OCR.
         *
         * @param model the model name (default: llama3.2-vision)
         * @return this builder
         */
        public Builder visionModel(String model) {
            this.visionModel = Objects.requireNonNull(model, "model must not be null");
            return this;
        }

        /**
         * Sets the text model for extraction.
         *
         * @param model the model name (default: llama3.2)
         * @return this builder
         */
        public Builder textModel(String model) {
            this.textModel = Objects.requireNonNull(model, "model must not be null");
            return this;
        }

        /**
         * Sets custom pool configuration files.
         *
         * @param visionPoolConfig path to the vision pool configuration YAML
         * @param textPoolConfig   path to the text pool configuration YAML
         * @return this builder
         */
        public Builder poolConfig(String visionPoolConfig, String textPoolConfig) {
            this.visionPoolConfig = Objects.requireNonNull(visionPoolConfig, "visionPoolConfig must not be null");
            this.textPoolConfig = Objects.requireNonNull(textPoolConfig, "textPoolConfig must not be null");
            return this;
        }

        /**
         * Uses an existing pool manager.
         * The pool manager will not be closed when the parser is no longer needed.
         *
         * @param poolManager the pool manager to use
         * @return this builder
         */
        public Builder poolManager(OllamaPoolManager poolManager) {
            this.poolManager = Objects.requireNonNull(poolManager, "poolManager must not be null");
            return this;
        }

        /**
         * Builds the configured parser.
         *
         * @return a new DefaultDocumentParser instance
         */
        public DefaultDocumentParser build() {
            LlmClient visionClient;
            LlmClient textClient;

            if (poolManager != null) {
                visionClient = poolManager.getVisionClient();
                textClient = poolManager.getTextClient();
            } else {
                OllamaPoolManager manager = OllamaPoolManager.create(visionPoolConfig, textPoolConfig);
                visionClient = manager.getVisionClient();
                textClient = manager.getTextClient();
            }

            VisionOcrService ocrService = new VisionOcrService(visionClient, visionModel);
            StructuredExtractionService extractionService = new StructuredExtractionService(textClient, textModel);
            ParsingPipeline pipeline = new ParsingPipeline(ocrService, extractionService);
            return new DefaultDocumentParser(pipeline);
        }
    }
}
