package fr.lapetina.smartocr.ollama;

/**
 * Interface for LLM client operations.
 * Enables mocking in tests without bytecode manipulation.
 */
public interface LlmClient {

    /**
     * Sends a text prompt to the specified model.
     *
     * @param model  the model name to use
     * @param prompt the text prompt
     * @return the raw text response from the model
     * @throws OllamaException if the request fails
     */
    String sendPrompt(String model, String prompt);

    /**
     * Sends a vision prompt with an image to the specified model.
     *
     * @param model  the model name to use (must support vision)
     * @param prompt the text prompt
     * @param image  the image data
     * @return the raw text response from the model
     * @throws OllamaException if the request fails
     */
    String sendVisionPrompt(String model, String prompt, byte[] image);
}
