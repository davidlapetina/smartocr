package fr.lapetina.smartocr.ollama;

import fr.lapetina.ollama.loadbalancer.PipelineFactory;
import fr.lapetina.ollama.loadbalancer.disruptor.DisruptorPipeline;
import fr.lapetina.ollama.loadbalancer.domain.model.InferenceRequest;
import fr.lapetina.ollama.loadbalancer.domain.model.InferenceResponse;

import java.util.Base64;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * LLM client implementation that uses the ollama-load-balancer pipeline
 * for pooled request handling with load balancing and circuit breaking.
 */
public class PooledLlmClient implements LlmClient, AutoCloseable {

    private static final long DEFAULT_TIMEOUT_SECONDS = 300;

    private final PipelineFactory factory;
    private final DisruptorPipeline pipeline;
    private final long timeoutSeconds;

    /**
     * Creates a pooled LLM client from a configuration file path.
     *
     * @param configPath path to the YAML configuration file
     */
    public PooledLlmClient(String configPath) {
        this(configPath, DEFAULT_TIMEOUT_SECONDS);
    }

    /**
     * Creates a pooled LLM client from a configuration file path with custom timeout.
     *
     * @param configPath     path to the YAML configuration file
     * @param timeoutSeconds timeout for requests in seconds
     */
    public PooledLlmClient(String configPath, long timeoutSeconds) {
        Objects.requireNonNull(configPath, "configPath must not be null");
        this.factory = PipelineFactory.create(configPath);
        this.factory.start();
        this.pipeline = factory.getPipeline();
        this.timeoutSeconds = timeoutSeconds;
    }

    /**
     * Creates a pooled LLM client with an existing factory.
     * The factory should already be started.
     *
     * @param factory the pipeline factory to use
     */
    public PooledLlmClient(PipelineFactory factory) {
        this(factory, DEFAULT_TIMEOUT_SECONDS);
    }

    /**
     * Creates a pooled LLM client with an existing factory and custom timeout.
     *
     * @param factory        the pipeline factory to use
     * @param timeoutSeconds timeout for requests in seconds
     */
    public PooledLlmClient(PipelineFactory factory, long timeoutSeconds) {
        this.factory = Objects.requireNonNull(factory, "factory must not be null");
        this.pipeline = factory.getPipeline();
        this.timeoutSeconds = timeoutSeconds;
    }

    @Override
    public String sendPrompt(String model, String prompt) {
        Objects.requireNonNull(model, "model must not be null");
        Objects.requireNonNull(prompt, "prompt must not be null");

        InferenceRequest request = InferenceRequest.ofPrompt(model, prompt);
        return executeRequest(request);
    }

    @Override
    public String sendVisionPrompt(String model, String prompt, byte[] image) {
        Objects.requireNonNull(model, "model must not be null");
        Objects.requireNonNull(prompt, "prompt must not be null");
        Objects.requireNonNull(image, "image must not be null");

        String base64Image = Base64.getEncoder().encodeToString(image);
        InferenceRequest request = InferenceRequest.ofVision(model, prompt, List.of(base64Image));
        return executeRequest(request);
    }

    private String executeRequest(InferenceRequest request) {
        try {
            CompletableFuture<InferenceResponse> future = pipeline.submit(request);
            InferenceResponse response = future.get(timeoutSeconds, TimeUnit.SECONDS);

            if (response.isError()) {
                throw new OllamaException("Request failed: " + response.errorType() +
                        " - " + response.errorMessage());
            }

            return response.response();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new OllamaException("Request interrupted", e);
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof OllamaException) {
                throw (OllamaException) cause;
            }
            throw new OllamaException("Request execution failed: " + cause.getMessage(), cause);
        } catch (TimeoutException e) {
            throw new OllamaException("Request timed out after " + timeoutSeconds + " seconds", e);
        }
    }

    /**
     * Returns the underlying pipeline factory.
     */
    public PipelineFactory getFactory() {
        return factory;
    }

    /**
     * Returns the underlying pipeline.
     */
    public DisruptorPipeline getPipeline() {
        return pipeline;
    }

    @Override
    public void close() {
        if (factory != null) {
            factory.close();
        }
    }
}
