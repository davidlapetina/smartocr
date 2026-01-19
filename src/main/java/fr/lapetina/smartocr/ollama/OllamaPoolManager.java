package fr.lapetina.smartocr.ollama;

import java.util.Objects;

/**
 * Manager for Ollama connection pools.
 * Provides separate pools for vision (llama3.2-vision) and text (llama3.2) models.
 *
 * <p>Usage:
 * <pre>{@code
 * try (OllamaPoolManager poolManager = OllamaPoolManager.createDefault()) {
 *     LlmClient visionClient = poolManager.getVisionClient();
 *     LlmClient textClient = poolManager.getTextClient();
 *     // use clients...
 * }
 * }</pre>
 */
public class OllamaPoolManager implements AutoCloseable {

    private static final String DEFAULT_VISION_CONFIG = "vision-pool-config.yaml";
    private static final String DEFAULT_TEXT_CONFIG = "text-pool-config.yaml";

    private final PooledLlmClient visionClient;
    private final PooledLlmClient textClient;

    /**
     * Creates a pool manager with custom configuration paths.
     *
     * @param visionConfigPath path to the vision pool configuration
     * @param textConfigPath   path to the text pool configuration
     */
    public OllamaPoolManager(String visionConfigPath, String textConfigPath) {
        Objects.requireNonNull(visionConfigPath, "visionConfigPath must not be null");
        Objects.requireNonNull(textConfigPath, "textConfigPath must not be null");
        this.visionClient = new PooledLlmClient(visionConfigPath);
        this.textClient = new PooledLlmClient(textConfigPath);
    }

    /**
     * Creates a pool manager with default configuration paths.
     * Expects vision-pool-config.yaml and text-pool-config.yaml on the classpath.
     *
     * @return new OllamaPoolManager instance
     */
    public static OllamaPoolManager createDefault() {
        return new OllamaPoolManager(DEFAULT_VISION_CONFIG, DEFAULT_TEXT_CONFIG);
    }

    /**
     * Creates a pool manager with custom configuration paths.
     *
     * @param visionConfigPath path to the vision pool configuration
     * @param textConfigPath   path to the text pool configuration
     * @return new OllamaPoolManager instance
     */
    public static OllamaPoolManager create(String visionConfigPath, String textConfigPath) {
        return new OllamaPoolManager(visionConfigPath, textConfigPath);
    }

    /**
     * Returns the LLM client for vision operations (llama3.2-vision model).
     * This client supports image-based prompts.
     *
     * @return the vision LLM client
     */
    public LlmClient getVisionClient() {
        return visionClient;
    }

    /**
     * Returns the LLM client for text operations (llama3.2 model).
     * This client is optimized for text-only prompts.
     *
     * @return the text LLM client
     */
    public LlmClient getTextClient() {
        return textClient;
    }

    /**
     * Returns the underlying pooled client for vision operations.
     * Provides access to the pipeline factory for advanced use cases.
     *
     * @return the pooled vision client
     */
    public PooledLlmClient getPooledVisionClient() {
        return visionClient;
    }

    /**
     * Returns the underlying pooled client for text operations.
     * Provides access to the pipeline factory for advanced use cases.
     *
     * @return the pooled text client
     */
    public PooledLlmClient getPooledTextClient() {
        return textClient;
    }

    @Override
    public void close() {
        Exception firstException = null;

        try {
            if (visionClient != null) {
                visionClient.close();
            }
        } catch (Exception e) {
            firstException = e;
        }

        try {
            if (textClient != null) {
                textClient.close();
            }
        } catch (Exception e) {
            if (firstException == null) {
                firstException = e;
            }
        }

        if (firstException != null) {
            throw new RuntimeException("Error closing pool manager", firstException);
        }
    }
}
