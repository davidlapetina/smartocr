package fr.lapetina.smartocr.ollama;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Base64;
import java.util.Objects;

/**
 * Low-level HTTP client for communicating with Ollama API.
 */
public class OllamaClient implements LlmClient {

    private static final String DEFAULT_BASE_URL = "http://localhost:11434";
    private static final Duration DEFAULT_TIMEOUT = Duration.ofMinutes(5);

    private final String baseUrl;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    /**
     * Creates a client with the default base URL (http://localhost:11434).
     */
    public OllamaClient() {
        this(DEFAULT_BASE_URL);
    }

    /**
     * Creates a client with the specified base URL.
     *
     * @param baseUrl the Ollama server base URL
     */
    public OllamaClient(String baseUrl) {
        this.baseUrl = Objects.requireNonNull(baseUrl, "baseUrl must not be null").replaceAll("/+$", "");
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(DEFAULT_TIMEOUT)
                .build();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Sends a text prompt to the specified model.
     *
     * @param model  the model name to use
     * @param prompt the text prompt
     * @return the raw text response from the model
     * @throws OllamaException if the request fails
     */
    public String sendPrompt(String model, String prompt) throws OllamaException {
        Objects.requireNonNull(model, "model must not be null");
        Objects.requireNonNull(prompt, "prompt must not be null");

        ObjectNode requestBody = objectMapper.createObjectNode();
        requestBody.put("model", model);
        requestBody.put("prompt", prompt);
        requestBody.put("stream", false);

        return executeRequest(requestBody);
    }

    /**
     * Sends a vision prompt with an image to the specified model.
     *
     * @param model  the model name to use (must support vision)
     * @param prompt the text prompt
     * @param image  the image data
     * @return the raw text response from the model
     * @throws OllamaException if the request fails
     */
    public String sendVisionPrompt(String model, String prompt, byte[] image) throws OllamaException {
        Objects.requireNonNull(model, "model must not be null");
        Objects.requireNonNull(prompt, "prompt must not be null");
        Objects.requireNonNull(image, "image must not be null");

        ObjectNode requestBody = objectMapper.createObjectNode();
        requestBody.put("model", model);
        requestBody.put("prompt", prompt);
        requestBody.put("stream", false);

        ArrayNode images = requestBody.putArray("images");
        images.add(Base64.getEncoder().encodeToString(image));

        return executeRequest(requestBody);
    }

    private String executeRequest(ObjectNode requestBody) throws OllamaException {
        try {
            String json = objectMapper.writeValueAsString(requestBody);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/api/generate"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .timeout(DEFAULT_TIMEOUT)
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new OllamaException("Ollama request failed with status " + response.statusCode()
                        + ": " + response.body());
            }

            JsonNode responseNode = objectMapper.readTree(response.body());
            JsonNode responseText = responseNode.get("response");

            if (responseText == null || responseText.isNull()) {
                throw new OllamaException("No response field in Ollama response: " + response.body());
            }

            return responseText.asText();

        } catch (IOException e) {
            throw new OllamaException("Failed to communicate with Ollama: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new OllamaException("Request interrupted", e);
        }
    }
}
